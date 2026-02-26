package com.chatutils.mixin;

import com.chatutils.ChatUtils;
import com.chatutils.ChatUtilsState;
import com.chatutils.hook.ChatLineHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Ported directly from Chatting's ChatLineMixin.
 *
 * On every ChatLine construction we scan the text before the first colon for a
 * player name (by username or display-name / nickname).  If found, we store the
 * player's NetworkPlayerInfo so the render mixin can draw their skin face.
 *
 * Continuation lines (word-wrapped fragments of the same message) are detected
 * via {@link ChatUtilsState#currentFullMessage} and skip detection so only the
 * first fragment gets a head.
 */
@Mixin(ChatLine.class)
public class MixinChatLine implements ChatLineHook {


    @Unique private boolean chatutils$detected = false;

    @Unique private NetworkPlayerInfo chatutils$playerInfo = null;

    // The detected player, preserved even when chatutils$playerInfo is suppressed
    @Unique private NetworkPlayerInfo chatutils$detectedPlayerInfo = null;

    @Unique private long chatutils$uniqueId = 0L;


    @Unique private static long chatutils$lastUniqueId = 0L;

    /**
     * Strips formatting codes and non-word characters so each remaining token
     * can be tested as a potential player name.
     */
    @Unique private static final Pattern SPLIT_PATTERN = Pattern.compile("(§.)|\\W");


    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(int updateCounter, IChatComponent lineString, int chatLineID, CallbackInfo ci) {

        // Assign a monotonically-increasing unique ID for deletion tracking.
        chatutils$uniqueId = ++chatutils$lastUniqueId;

        IChatComponent fullMsg = ChatUtilsState.currentFullMessage;
        if (fullMsg != null && fullMsg == ChatUtilsState.lastFullMessage) {
            // Continuation fragment – inherit nothing; no head on this line.
            return;
        }
        ChatUtilsState.lastFullMessage = fullMsg;

        //  player detection
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler == null) return;

        String beforeColon = StringUtils.substringBefore(lineString.getFormattedText(), ":");

        Map<String, NetworkPlayerInfo> nicknameCache = new HashMap<>();

        try {
            for (String word : SPLIT_PATTERN.split(beforeColon)) {
                if (word.isEmpty()) continue;

                // Try exact username match first
                NetworkPlayerInfo info = netHandler.getPlayerInfo(word);

                // Fall back to display-name / nickname lookup
                if (info == null) {
                    info = chatutils$getPlayerFromNickname(word, netHandler, nicknameCache);
                }

                if (info != null) {
                    chatutils$detected          = true;
                    chatutils$detectedPlayerInfo = info;

                    // Consecutive-message
                    // If this is the same player as the last detected player
                    // optionally null out chatutils$playerInfo so no head is drawn
                    if (ChatUtilsState.lastDetectedPlayer != null &&
                            info.getGameProfile() == ChatUtilsState.lastDetectedPlayer.getGameProfile()
                            && ChatUtils.Config.hideHeadOnConsecutive) {
                        chatutils$playerInfo = null; // suppressed, but chatutils$detected stays true
                    } else {
                        chatutils$playerInfo = info;
                    }

                    ChatUtilsState.lastDetectedPlayer = info;
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  nickname helper

    @Unique
    @Nullable
    private static NetworkPlayerInfo chatutils$getPlayerFromNickname(
            String word,
            NetHandlerPlayClient connection,
            Map<String, NetworkPlayerInfo> cache) {

        if (cache.isEmpty()) {
            for (NetworkPlayerInfo p : connection.getPlayerInfoMap()) {
                IChatComponent displayName = p.getDisplayName();
                if (displayName == null) continue;

                String nickname = displayName.getUnformattedTextForChat();
                if (word.equals(nickname)) {
                    cache.clear(); // signal "already found, no need to keep"
                    return p;
                }
                cache.put(nickname, p);
            }
            return null;
        } else {
            return cache.get(word);
        }
    }

    //  ChatLineHook interface ─

    @Override
    public boolean chatutils$hasDetected() {
        return chatutils$detected;
    }

    @Override
    public NetworkPlayerInfo chatutils$getPlayerInfo() {
        return chatutils$playerInfo;
    }

    @Override
    public long chatutils$getUniqueId() {
        return chatutils$uniqueId;
    }
}