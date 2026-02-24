package com.chatutils.mixin;

import com.chatutils.ChatCompactHandler;
import net.minecraft.client.gui.ChatLine;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.util.IChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(GuiNewChat.class)
public class MixinGuiNewChat {

    @Shadow @Final private List<ChatLine> chatLines;
    @Shadow @Final private List<ChatLine> drawnChatLines;

    @ModifyVariable(
            method = "setChatLine",
            at = @At("HEAD"),
            ordinal = 0
    )
    private IChatComponent injectTimestamp(IChatComponent component) {
        return ChatCompactHandler.applyTimestamp(component);
    }

    @Inject(method = "setChatLine", at = @At("HEAD"))
    private void beforeSetChatLine(IChatComponent component, int chatLineId, int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatCompactHandler.handleChatMessage(component, refresh, chatLines, drawnChatLines);
    }

    @Inject(method = "setChatLine", at = @At("TAIL"))
    private void afterSetChatLine(IChatComponent component, int chatLineId, int updateCounter, boolean refresh, CallbackInfo ci) {
        ChatCompactHandler.resetMessageHash();
    }

    @ModifyArg(
            method = "setChatLine",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;add(ILjava/lang/Object;)V",
                    remap = false
            )
    )
    private Object trackChatLine(Object line) {
        if (line instanceof ChatLine) {
            ChatCompactHandler.trackChatLine((ChatLine) line);
        }
        return line;
    }
}