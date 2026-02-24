package com.chatutils;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.*;
import net.minecraft.util.StringUtils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatCompactHandler {

    private static final Map<Integer, ChatEntry> chatMessageMap = new HashMap<>();
    private static final Map<Integer, Set<ChatLine>> messagesForHash = new HashMap<>();
    private static final DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private static final String TIMESTAMP_REGEX = "^(?:\\[\\d\\d:\\d\\d(:\\d\\d)?(?: AM| PM)?]|<\\d\\d:\\d\\d(:\\d\\d)?>) ";

    public static int currentMessageHash = -1;

    public static IChatComponent applyTimestamp(IChatComponent original) {
        if (!ChatUtils.Config.timestampsEnabled) return original;
        if (original.getUnformattedText().trim().isEmpty()) return original;

        String pattern;

        if (ChatUtils.Config.timestamp24Hour) {
            pattern = ChatUtils.Config.timestampShowSeconds ? "HH:mm:ss" : "HH:mm";
        } else {
            pattern = ChatUtils.Config.timestampShowSeconds ? "hh:mm:ss a" : "hh:mm a";
        }

        String time = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern(pattern));

        String formatted = ChatUtils.Config.timestampStyle == 0
                ? "[" + time + "] "
                : "<" + time + "> ";

        ChatComponentIgnored wrapper = new ChatComponentIgnored(
                net.minecraft.util.EnumChatFormatting.GRAY
                        + formatted
                        + net.minecraft.util.EnumChatFormatting.RESET
        );

        wrapper.appendSibling(original);

        return wrapper;
    }

    public static void handleChatMessage(IChatComponent component, boolean refresh, List<ChatLine> chatLines, List<ChatLine> drawnChatLines) {
        if (!ChatUtils.Config.compactingEnabled || refresh) return;

        String clear = cleanColor(component.getFormattedText()).trim();
        if (clear.isEmpty() || isDivider(clear)) return;

        int hash = getChatComponentHash(component);
        currentMessageHash = hash;

        long now = System.currentTimeMillis();
        ChatEntry entry = chatMessageMap.get(hash);

        if (entry == null) {
            chatMessageMap.put(hash, new ChatEntry(1, now));
            return;
        }

        if (ChatUtils.Config.expireTimeSeconds != -1 &&
                (now - entry.lastSeenMessageMillis) > ChatUtils.Config.expireTimeSeconds * 1000L) {
            chatMessageMap.put(hash, new ChatEntry(1, now));
            return;
        }

        boolean removed = deleteMessageByHash(hash, chatLines, drawnChatLines);
        if (!removed) {
            chatMessageMap.put(hash, new ChatEntry(1, now));
            return;
        }

        entry.messageCount++;
        entry.lastSeenMessageMillis = now;

        String cleanUnformatted = StringUtils.stripControlCodes(component.getUnformattedText())
                .replaceAll(TIMESTAMP_REGEX, "")
                .trim();

        component.appendSibling(
                new ChatComponentText(
                        EnumChatFormatting.GRAY + " (" + decimalFormat.format(entry.messageCount) + ")"
                )
        );

        if (ChatUtils.Config.stackedMessageCopyEnabled && !cleanUnformatted.isEmpty()) {
            component.appendSibling(new ChatComponentText(EnumChatFormatting.RESET + " "));
            component.appendSibling(createCopyIcon(cleanUnformatted + " (" + entry.messageCount + ")"));
        }
    }

    private static IChatComponent createCopyIcon(String text) {
        String runCommand = "/copytoclipboard " + text;

        ChatComponentText copyIcon = new ChatComponentText(
                EnumChatFormatting.DARK_GRAY + Character.toString((char) Integer.parseInt("270D", 16))
        );

        ChatStyle style = new ChatStyle()
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new ChatComponentText(EnumChatFormatting.GRAY + "Copy message")))
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, runCommand));

        copyIcon.setChatStyle(style);
        return copyIcon;
    }

    public static void trackChatLine(ChatLine line) {
        if (currentMessageHash == -1) return;

        messagesForHash.computeIfAbsent(currentMessageHash, k -> new HashSet<>()).add(line);
    }

    public static void resetMessageHash() {
        currentMessageHash = -1;
    }

    private static boolean deleteMessageByHash(int hash, List<ChatLine> chatLines, List<ChatLine> drawnChatLines) {
        Set<ChatLine> tracked = messagesForHash.remove(hash);
        if (tracked == null || tracked.isEmpty()) return false;

        boolean removed = false;

        for (int i = 0; i < chatLines.size() && i < 100; i++) {
            if (tracked.contains(chatLines.get(i))) {
                chatLines.remove(i);
                i--;
                removed = true;
            } else if (ChatUtils.Config.consecutiveOnly) {
                break;
            }
        }

        for (int i = 0; i < drawnChatLines.size() && i < 300; i++) {
            if (tracked.contains(drawnChatLines.get(i))) {
                drawnChatLines.remove(i);
                i--;
                removed = true;
            } else if (ChatUtils.Config.consecutiveOnly) {
                break;
            }
        }

        return removed;
    }

    public static void cleanupExpired() {
        if (ChatUtils.Config.expireTimeSeconds == -1) return;

        long now = System.currentTimeMillis();

        chatMessageMap.entrySet().removeIf(e -> {
            if ((now - e.getValue().lastSeenMessageMillis) > ChatUtils.Config.expireTimeSeconds * 1000L) {
                messagesForHash.remove(e.getKey());
                return true;
            }
            return false;
        });
    }

    public static void reset() {
        chatMessageMap.clear();
        messagesForHash.clear();
        currentMessageHash = -1;
    }

    private static int getChatStyleHash(ChatStyle style) {
        HoverEvent hover = style.getChatHoverEvent();
        HoverEvent.Action action = null;
        int hoverHash = 0;

        if (hover != null) {
            action = hover.getAction();
            hoverHash = getChatComponentHash(hover.getValue());
        }

        return Objects.hash(
                style.getColor(),
                style.getBold(),
                style.getItalic(),
                style.getUnderlined(),
                style.getStrikethrough(),
                style.getObfuscated(),
                action,
                hoverHash,
                style.getChatClickEvent(),
                style.getInsertion()
        );
    }

    private static int getChatComponentHash(IChatComponent component) {
        List<Integer> siblings = new ArrayList<>();

        for (IChatComponent sibling : component.getSiblings()) {
            if (!(sibling instanceof ChatComponentIgnored)) {
                siblings.add(getChatComponentHash(sibling));
            }
        }

        if (component instanceof ChatComponentIgnored) {
            return Objects.hash(siblings);
        }

        String cleaned = component.getUnformattedText()
                .replaceAll(TIMESTAMP_REGEX, "")
                .trim();

        return Objects.hash(cleaned, siblings, getChatStyleHash(component.getChatStyle()));
    }

    private static boolean isDivider(String text) {
        text = text.replaceAll(TIMESTAMP_REGEX, "").trim();
        if (text.length() < 5) return false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != '-' && c != '=' && c != '\u25AC') {
                return false;
            }
        }
        return true;
    }

    private static String cleanColor(String text) {
        return text.replaceAll("§.", "");
    }

    private static class ChatEntry {
        int messageCount;
        long lastSeenMessageMillis;

        ChatEntry(int count, long time) {
            this.messageCount = count;
            this.lastSeenMessageMillis = time;
        }
    }
}