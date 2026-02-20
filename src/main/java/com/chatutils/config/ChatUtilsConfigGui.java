package com.chatutils;

import com.chatutils.ChatUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

public class ChatUtilsConfigGui extends GuiScreen {

    private final GuiScreen parent;

    public ChatUtilsConfigGui(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        refreshButtons();
    }

    private void refreshButtons() {
        this.buttonList.clear();

        int centerX = this.width / 2;
        int y = this.height / 4;

        // Compact toggle
        this.buttonList.add(new GuiButton(
                0,
                centerX - 100,
                y,
                200,
                20,
                "Compact Chat: " + (ChatUtils.Config.compactingEnabled ? "ON" : "OFF")
        ));

        // Expiry cycle
        String expireText;
        if (ChatUtils.Config.expireTimeSeconds == -1) {
            expireText = "Never";
        } else {
            expireText = ChatUtils.Config.expireTimeSeconds + "s";
        }

        this.buttonList.add(new GuiButton(
                1,
                centerX - 100,
                y + 24,
                200,
                20,
                "Expire Time: " + expireText
        ));

        // Consecutive only
        this.buttonList.add(new GuiButton(
                2,
                centerX - 100,
                y + 48,
                200,
                20,
                "Consecutive Mode: " + (ChatUtils.Config.consecutiveOnly ? "ON" : "OFF")
        ));

        this.buttonList.add(new GuiButton(
                3,
                centerX - 100,
                y + 72,
                200,
                20,
                "Reset On World Change: " + (ChatUtils.Config.resetOnWorldChange ? "ON" : "OFF")
        ));

        this.buttonList.add(new GuiButton(
                4,
                centerX - 100,
                y + 96,
                200,
                20,
                "Stacked Message Copy: " + (ChatUtils.Config.stackedMessageCopyEnabled ? "ON" : "OFF")
        ));

        // Done
        this.buttonList.add(new GuiButton(
                99,
                centerX - 100,
                y + 148,
                200,
                20,
                "Done"
        ));

        setCompactingDependentButtonsEnabled(ChatUtils.Config.compactingEnabled);
    }

    private void setCompactingDependentButtonsEnabled(boolean enabled) {
        for (Object obj : this.buttonList) {
            GuiButton button = (GuiButton) obj;
            if (button.id == 1 || button.id == 2 || button.id == 3 || button.id == 4) {
                button.enabled = enabled;
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {

        switch (button.id) {

            case 0:
                ChatUtils.Config.compactingEnabled =
                        !ChatUtils.Config.compactingEnabled;
                break;

            case 1:
                int time = ChatUtils.Config.expireTimeSeconds;

                if (time == 30) {
                    ChatUtils.Config.expireTimeSeconds = 60;
                } else if (time == 60) {
                    ChatUtils.Config.expireTimeSeconds = 120;
                } else if (time == 120) {
                    ChatUtils.Config.expireTimeSeconds = 300;
                } else if (time == 300) {
                    ChatUtils.Config.expireTimeSeconds = -1; // Never
                } else {
                    ChatUtils.Config.expireTimeSeconds = 30;
                }
                break;

            case 2:
                ChatUtils.Config.consecutiveOnly =
                        !ChatUtils.Config.consecutiveOnly;
                break;

            case 3:
                ChatUtils.Config.resetOnWorldChange =
                        !ChatUtils.Config.resetOnWorldChange;
                break;

            case 4:
                ChatUtils.Config.stackedMessageCopyEnabled =
                        !ChatUtils.Config.stackedMessageCopyEnabled;
                break;

            case 99:
                mc.displayGuiScreen(parent);
                return;
        }

        ChatUtils.saveConfig();
        refreshButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        this.drawCenteredString(
                this.fontRendererObj,
                "Chat Utils Config",
                this.width / 2,
                20,
                0xFFFFFF
        );

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
