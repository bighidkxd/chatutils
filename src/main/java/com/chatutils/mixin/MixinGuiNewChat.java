package com.chatutils.mixin;

import com.chatutils.ChatCompactHandler;
import com.chatutils.ChatUtils;
import com.chatutils.ChatUtilsState;
import com.chatutils.hook.ChatLineHook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.List;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    @Shadow
    @Final
    private List<ChatLine> chatLines;
    @Shadow
    @Final
    private List<ChatLine> drawnChatLines;
    @Shadow
    @Final
    private Minecraft mc;

    // ─────────────────────────────────────────────────────────────────────────
    //  Shared state for chat-head rendering
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Updated by {@link #chatutils$captureDrawLine} whenever the draw-loop
     * assigns a new ChatLine local, so the drawStringWithShadow redirect can
     * reference it without fragile local-capture indices.
     */
    @Unique
    private ChatLine chatutils$renderLine = null;

    // ─────────────────────────────────────────────────────────────────────────
    //  Timestamp injection (unchanged)
    // ─────────────────────────────────────────────────────────────────────────

    @ModifyVariable(
            method = "setChatLine",
            at = @At("HEAD"),
            ordinal = 0
    )
    private IChatComponent injectTimestamp(IChatComponent component) {
        return ChatCompactHandler.applyTimestamp(component);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Compacting + currentFullMessage tracking
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Fires at the top of setChatLine().
     * <p>
     * We store {@code component} in {@link ChatUtilsState#currentFullMessage}
     * BEFORE any ChatLine objects are constructed so that MixinChatLine can
     * detect continuation (word-wrapped) lines and skip re-detection on them.
     */
    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void beforeSetChatLine(IChatComponent component, int chatLineId,
                                   int updateCounter, boolean refresh, CallbackInfo ci) {
        // Let MixinChatLine know which source message is being processed.
        ChatUtilsState.currentFullMessage = component;

        ChatCompactHandler.handleChatMessage(component, refresh, chatLines, drawnChatLines);
    }

    @Inject(method = "setChatLine", at = @At("TAIL"))
    private void afterSetChatLine(IChatComponent component, int chatLineId,
                                  int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatCompactHandler.resetMessageHash();
        ChatUtilsState.currentFullMessage = null; // done splitting

        // Guard against unbounded growth.
        while (chatLines.size() > 10000) chatLines.remove(chatLines.size() - 1);
        while (drawnChatLines.size() > 10000) drawnChatLines.remove(drawnChatLines.size() - 1);
    }

    @ModifyArgs(
            method = "setChatLine",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    remap = false
            )
    )
    private void trackChatLine(Args args) {
        Object line = args.get(1);
        if (line instanceof ChatLine) {
            ChatCompactHandler.trackChatLine((ChatLine) line);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Animation  (toggle-controlled)
    // ─────────────────────────────────────────────────────────────────────────

    @Unique
    private long animationStart = 0L;

    /**
     * Reset the animation clock whenever a new message arrives.
     */
    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void chatutils$resetAnimation(IChatComponent component, int chatLineId,
                                          int updateCounter, boolean refresh, CallbackInfo ci) {
        if (ChatUtils.Config.animatedChat && !refresh) {
            animationStart = System.currentTimeMillis();
        }
    }

    /**
     * Slide the new message in from below.
     * Only active while {@link ChatUtils.Config#animatedChat} is true.
     */
    @Inject(method = "drawChat", at = @At("HEAD"))
    private void applyAnimation(int updateCounter, CallbackInfo ci) {
        if (!ChatUtils.Config.animatedChat || animationStart == 0L) return;

        double speed = 20.0D;
        float lineHeight = 9.0F;
        double shift = ((double) System.currentTimeMillis()
                - (double) lineHeight * speed
                - animationStart) / speed;
        if (shift > 0.0D) shift = 0.0D;
        GlStateManager.translate(0.0D, -shift, 0.0D);
    }

    //  Transparent background

    /**
     * Forces the chat background rect to fully transparent when the option is on.
     * When off, the vanilla colour argument is passed through unchanged.
     */
    @ModifyArgs(
            method = "drawChat",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiNewChat;drawRect(IIIII)V",
                    ordinal = 0)
    )
    private void chatutils$maybeClearBackground(Args args) {
        if (ChatUtils.Config.transparentChat) {
            args.set(4, 0x00000000);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Chat heads – step 1: capture the current ChatLine during drawChat loop
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Every iteration of the drawChat loop assigns a ChatLine to a local.
     * We intercept that store via @ModifyVariable so we always know which line
     * is about to be rendered when our drawStringWithShadow redirect fires.
     */
    @ModifyVariable(method = "drawChat", at = @At("STORE"))
    private ChatLine chatutils$captureDrawLine(ChatLine line) {
        chatutils$renderLine = line;
        return line;
    }

    //  Chat heads – step 2: redirect the font-renderer call to draw the head

    /**
     * Replaces the vanilla {@code FontRenderer.drawStringWithShadow} call inside
     * drawChat with our own implementation that:
     *
     * <ol>
     *   <li>Draws an 8×8 player-skin face to the left of the message when the
     *       line was attributed to a known player.</li>
     *   <li>Shifts the text 10 px right to make room for the head (or just for
     *       alignment when {@link ChatUtils.Config#offsetNonPlayerMessages} is
     *       enabled).</li>
     *   <li>Falls through to the vanilla draw call for the (shifted) text.</li>
     * </ol>
     * <p>
     * The skin face is rendered at the same alpha as the chat line itself so it
     * fades out naturally with the message.
     */
    @Redirect(
            method = "drawChat",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/FontRenderer;drawStringWithShadow(Ljava/lang/String;FFI)I")
    )
    private int chatutils$redirectDrawString(FontRenderer fr,
                                             String text, float x, float y, int color) {
        float drawX = x;

        if (ChatUtils.Config.chatHeads && chatutils$renderLine instanceof ChatLineHook) {
            ChatLineHook hook = (ChatLineHook) chatutils$renderLine;
            NetworkPlayerInfo info = hook.chatutils$getPlayerInfo();

            if (info != null) {
                // ── draw the player skin face ───────────────────────────────
                // Extract the chat-line alpha so the head fades in sync.
                int alpha = (color >> 24) & 0xFF;
                float headAlpha = (alpha == 0) ? 1.0f : alpha / 255f;

                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableTexture2D();
                mc.getTextureManager().bindTexture(info.getLocationSkin());
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                GlStateManager.color(1.0f, 1.0f, 1.0f, headAlpha);

                // Base skin layer
                Gui.drawScaledCustomSizeModalRect(
                        (int) x, (int) (y - 1f),
                        8.0f, 8.0f,
                        8, 8,
                        8, 8,
                        64.0f, 64.0f
                );
                // Hat / overlay layer  (u=40, v=8)
                Gui.drawScaledCustomSizeModalRect(
                        (int) x, (int) (y - 1f),
                        40.0f, 8.0f,
                        8, 8,
                        8, 8,
                        64.0f, 64.0f
                );

                GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);

                drawX += 10f; // shift text

            } else if (hook.chatutils$hasDetected() || ChatUtils.Config.offsetNonPlayerMessages) {
                // Player was detected but head is suppressed (e.g. consecutive),
                drawX += 10f;
            }
        }

        return fr.drawStringWithShadow(text, drawX, y, color);
    }

    /**
     * When chat heads are active, all text is shifted 10px right.
     * Click detection must be shifted to match, otherwise click targets
     * hover events are 10px to the left of where they appear.
     */
    @ModifyVariable(
            method = "getChatComponent",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private int chatutils$offsetClickX(int mouseX) {
        if (ChatUtils.Config.chatHeads) {
            return mouseX - 10;
        }
        return mouseX;
    }
}