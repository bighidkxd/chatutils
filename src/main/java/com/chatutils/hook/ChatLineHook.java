package com.chatutils.hook;

import net.minecraft.client.network.NetworkPlayerInfo;


public interface ChatLineHook {

    /** True if the line's prefix matched a player name (even if head is suppressed). */
    boolean chatutils$hasDetected();

    /** The NetworkPlayerInfo to render a head for, or null if suppressed / not found. */
    NetworkPlayerInfo chatutils$getPlayerInfo();

    /** Monotonically-increasing ID assigned at construction – used for deletion. */
    long chatutils$getUniqueId();
}