package com.chatutils;

import net.minecraft.client.gui.ChatLine;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;

import java.text.DecimalFormat;
import java.util.*;

public class ChatCompactHandler {

    private static final Map<Integer, ChatEntry> chatMessageMap = new HashMap<>();
    private static final Map<Integer, Set<ChatLine>> messagesForHash = new HashMap<>();
    private static final DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private static final String TIMESTAMP_REGEX = "^(?:\\[\\d\\d:\\d\\d(:\\d\\d)?(?: AM| PM|)]|<\\d\\d:\\d\\d>) ";

    public static int currentMessageHash = -1;

    public static void handleChatMessage(IChatComponent component, boolean refresh, List<ChatLine> chatLines, List<ChatLine> drawnChatLines) {
        if (!ChatUtils.Config.compactingEnabled || refresh) {
            return;
        }

        String clear = cleanColor(component.getFormattedText()).trim();
        if (clear.isEmpty() || isDivider(clear)) {
            return;
        }

        int hash = getChatComponentHash(component);
        currentMessageHash = hash;

        long now = System.currentTimeMillis();

        ChatEntry entry = chatMessageMap.get(hash);
        if (entry == null) {
            chatMessageMap.put(hash, new ChatEntry(1, now));
            return;
        }

        if (ChatUtils.Config.expireTimeSeconds > 0 &&
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

        component.appendSibling(
                new ChatComponentText(
                        EnumChatFormatting.GRAY + " (" + decimalFormat.format(entry.messageCount) + ")"
                )
        );
    }

    public static void trackChatLine(ChatLine line) {
        if (currentMessageHash == -1) {
            return;
        }

        messagesForHash
                .computeIfAbsent(currentMessageHash, k -> new HashSet<>())
                .add(line);
    }

    public static void resetMessageHash() {
        currentMessageHash = -1;
    }

    private static boolean deleteMessageByHash(int hash, List<ChatLine> chatLines, List<ChatLine> drawnChatLines) {
        Set<ChatLine> tracked = messagesForHash.remove(hash);
        if (tracked == null || tracked.isEmpty()) {
            return false;
        }

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
        if (ChatUtils.Config.expireTimeSeconds <= 0) {
            return;
        }

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
            siblings.add(getChatComponentHash(sibling));
        }

        String cleaned = component.getUnformattedText()
                .replaceAll(TIMESTAMP_REGEX, "")
                .trim();

        return Objects.hash(cleaned, siblings, getChatStyleHash(component.getChatStyle()));
    }

    private static String cleanColor(String text) {
        return text.replaceAll("§.", "");
    }

    private static boolean isDivider(String text) {
        text = text.replaceAll(TIMESTAMP_REGEX, "").trim();
        if (text.length() < 5) return false;
        int symbols = 0;
        for (char c : text.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                symbols++;
            }
        }
        return symbols > text.length() * 0.6;
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
