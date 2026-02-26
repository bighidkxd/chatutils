package com.chatutils;

import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.util.IChatComponent;

/**
 * Shared mutable state between MixinGuiNewChat and MixinChatLine
 *
 * MixinGuiNewChat writes {@code currentFullMessage} at the top of setChatLine()
 * so that MixinChatLine can detect whether a freshly-constructed ChatLine is the
 * first word-wrapped fragment of a message or a continuation fragment.
 */
public final class ChatUtilsState {

    private ChatUtilsState() {}

    /** The IChatComponent that is currently being split into drawn lines */
    public static IChatComponent currentFullMessage = null;

    /** The last full-message component that was processed (for continuation detection) */
    public static IChatComponent lastFullMessage = null;

    /**
     * The last player successfully detected in a chat message
     * Not reset when a non-player message arrives, so "consecutive" means
     * "same player as the last player-attributed line",Ported from Chatting
     */
    public static NetworkPlayerInfo lastDetectedPlayer = null;
}