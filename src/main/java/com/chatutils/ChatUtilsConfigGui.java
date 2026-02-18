package com.chatutils;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatUtilsConfigGui extends GuiScreen {

    private final GuiScreen parent;
    private final List<GuiButton> optionButtons = new ArrayList<>();

    private GuiButton toggleCompacting;
    private GuiButton timeButton;
    private GuiButton consecutiveButton;

    public ChatUtilsConfigGui(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        buttonList.clear();
        optionButtons.clear();

        int centerX = width / 2;
        int y = height / 4;

        toggleCompacting = new GuiButton(0, centerX - 100, y, 200, 20, "");
        buttonList.add(toggleCompacting);

        timeButton = new GuiButton(1, centerX - 100, y + 24, 200, 20, "");
        consecutiveButton = new GuiButton(2, centerX - 100, y + 48, 200, 20, "");

        buttonList.add(timeButton);
        buttonList.add(consecutiveButton);

        optionButtons.add(timeButton);
        optionButtons.add(consecutiveButton);

        buttonList.add(new GuiButton(99, centerX - 100, y + 96, 200, 20, "Done"));

        refreshButtons();
    }

    private void refreshButtons() {
        toggleCompacting.displayString = "Compacting: " + (ChatUtils.Config.compactingEnabled ? "ON" : "OFF");
        timeButton.displayString = "Expire Time: " +
                (ChatUtils.Config.expireTimeSeconds == 0 ? "Never" : ChatUtils.Config.expireTimeSeconds + "s");
        consecutiveButton.displayString = "Consecutive Only: " +
                (ChatUtils.Config.consecutiveOnly ? "ON" : "OFF");

        boolean enabled = ChatUtils.Config.compactingEnabled;
        for (GuiButton button : optionButtons) {
            button.enabled = enabled;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                ChatUtils.Config.compactingEnabled = !ChatUtils.Config.compactingEnabled;
                break;

            case 1:
                if (ChatUtils.Config.expireTimeSeconds == 0) {
                    ChatUtils.Config.expireTimeSeconds = 30;
                } else if (ChatUtils.Config.expireTimeSeconds == 30) {
                    ChatUtils.Config.expireTimeSeconds = 60;
                } else if (ChatUtils.Config.expireTimeSeconds == 60) {
                    ChatUtils.Config.expireTimeSeconds = 120;
                } else if (ChatUtils.Config.expireTimeSeconds == 120) {
                    ChatUtils.Config.expireTimeSeconds = 300;
                } else {
                    ChatUtils.Config.expireTimeSeconds = 0;
                }
                break;

            case 2:
                ChatUtils.Config.consecutiveOnly = !ChatUtils.Config.consecutiveOnly;
                break;

            case 99:
                mc.displayGuiScreen(parent);
                return;
        }

        refreshButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "ChatUtils Config", width / 2, height / 4 - 20, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
