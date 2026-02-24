package com.chatutils.config;

import com.chatutils.ChatUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class ChatUtilsConfigGui extends GuiScreen {

    private final GuiScreen parent;

    private GuiButton hoveredButton;
    private int lastMouseX;
    private int lastMouseY;
    private long hoverStartTime;

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
        int startY = this.height / 5;

        int leftX = centerX - 155;
        int rightX = centerX + 5;

        int leftY = startY;
        int rightY = startY;

        GuiButton compactToggle = new GuiButton(0, leftX, leftY, 150, 20,
                "Compact Chat: " + onOff(ChatUtils.Config.compactingEnabled));
        leftY += 24;

        String expireText = ChatUtils.Config.expireTimeSeconds == -1
                ? "Never"
                : ChatUtils.Config.expireTimeSeconds + "s";

        GuiButton expireBtn = new GuiButton(1, leftX, leftY, 150, 20,
                "Expire Time: " + expireText);
        leftY += 24;

        GuiButton consecutiveBtn = new GuiButton(2, leftX, leftY, 150, 20,
                "Consecutive Mode: " + onOff(ChatUtils.Config.consecutiveOnly));
        leftY += 24;

        GuiButton copyBtn = new GuiButton(3, leftX, leftY, 150, 20,
                "Stacked Copy: " + onOff(ChatUtils.Config.stackedMessageCopyEnabled));

        GuiButton timestampToggle = new GuiButton(4, rightX, rightY, 150, 20,
                "Timestamps: " + onOff(ChatUtils.Config.timestampsEnabled));
        rightY += 24;

        GuiButton formatBtn = new GuiButton(5, rightX, rightY, 150, 20,
                "24 Hour: " + onOff(ChatUtils.Config.timestamp24Hour));
        rightY += 24;

        GuiButton secondsBtn = new GuiButton(6, rightX, rightY, 150, 20,
                "Show Seconds: " + onOff(ChatUtils.Config.timestampShowSeconds));
        rightY += 24;

        String style = ChatUtils.Config.timestampStyle == 0 ? "[HH:mm]" : "<HH:mm>";
        GuiButton styleBtn = new GuiButton(7, rightX, rightY, 150, 20,
                "Style: " + style);

        GuiButton done = new GuiButton(99, centerX - 100,
                Math.max(leftY, rightY) + 40,
                200, 20,
                "Done");

        this.buttonList.add(compactToggle);
        this.buttonList.add(expireBtn);
        this.buttonList.add(consecutiveBtn);
        this.buttonList.add(copyBtn);

        this.buttonList.add(timestampToggle);
        this.buttonList.add(formatBtn);
        this.buttonList.add(secondsBtn);
        this.buttonList.add(styleBtn);

        this.buttonList.add(done);

        boolean compact = ChatUtils.Config.compactingEnabled;
        expireBtn.enabled = compact;
        consecutiveBtn.enabled = compact;
        copyBtn.enabled = compact;

        boolean timestamps = ChatUtils.Config.timestampsEnabled;
        formatBtn.enabled = timestamps;
        secondsBtn.enabled = timestamps;
        styleBtn.enabled = timestamps;
    }

    private String onOff(boolean b) {
        return b ? "ON" : "OFF";
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {

        switch (button.id) {

            case 0:
                ChatUtils.Config.compactingEnabled = !ChatUtils.Config.compactingEnabled;
                break;

            case 1:
                int time = ChatUtils.Config.expireTimeSeconds;
                if (time == 30) ChatUtils.Config.expireTimeSeconds = 60;
                else if (time == 60) ChatUtils.Config.expireTimeSeconds = 120;
                else if (time == 120) ChatUtils.Config.expireTimeSeconds = 300;
                else if (time == 300) ChatUtils.Config.expireTimeSeconds = -1;
                else ChatUtils.Config.expireTimeSeconds = 30;
                break;

            case 2:
                ChatUtils.Config.consecutiveOnly = !ChatUtils.Config.consecutiveOnly;
                break;

            case 3:
                ChatUtils.Config.stackedMessageCopyEnabled = !ChatUtils.Config.stackedMessageCopyEnabled;
                break;

            case 4:
                ChatUtils.Config.timestampsEnabled = !ChatUtils.Config.timestampsEnabled;
                break;

            case 5:
                ChatUtils.Config.timestamp24Hour = !ChatUtils.Config.timestamp24Hour;
                break;

            case 6:
                ChatUtils.Config.timestampShowSeconds = !ChatUtils.Config.timestampShowSeconds;
                break;

            case 7:
                ChatUtils.Config.timestampStyle =
                        ChatUtils.Config.timestampStyle == 0 ? 1 : 0;
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

        int centerX = this.width / 2;
        int startY = this.height / 5;

        this.drawCenteredString(this.fontRendererObj,
                "Chat Utils Config",
                centerX,
                20,
                0xFFFFFF);

        this.drawString(this.fontRendererObj,
                "Compact Chat",
                centerX - 155,
                startY - 12,
                0xAAAAAA);

        this.drawString(this.fontRendererObj,
                "Timestamps",
                centerX + 5,
                startY - 12,
                0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);

        GuiButton currentHover = null;

        for (GuiButton button : this.buttonList) {
            if (button.isMouseOver()) {
                currentHover = button;
                break;
            }
        }

        if (currentHover != null) {
            if (currentHover != hoveredButton || mouseX != lastMouseX || mouseY != lastMouseY) {
                hoveredButton = currentHover;
                hoverStartTime = System.currentTimeMillis();
                lastMouseX = mouseX;
                lastMouseY = mouseY;
                return;
            }

            if (System.currentTimeMillis() - hoverStartTime >= 600) {
                List<String> tooltip = null;

                switch (hoveredButton.id) {
                    case 0:
                        tooltip = Arrays.asList("Merge identical chat messages.");
                        break;
                    case 1:
                        tooltip = Arrays.asList(
                                "30s / 60s / 120s / 300s – Only stack within that time.",
                                "",
                                "Never – No time limit for stacking."
                        );
                        break;
                    case 2:
                        tooltip = Arrays.asList("Only stack consecutive messages.");
                        break;
                    case 3:
                        tooltip = Arrays.asList("Copy Compacted messages.");
                        break;
                    case 4:
                        tooltip = Arrays.asList("Show time before messages.");
                        break;
                    case 5:
                        tooltip = Arrays.asList("Use 24h or 12h format.");
                        break;
                    case 6:
                        tooltip = Arrays.asList("Display seconds in time.");
                        break;
                    case 7:
                        tooltip = Arrays.asList("Change timestamp brackets.");
                        break;
                }

                if (tooltip != null) {
                    this.drawHoveringText(tooltip, mouseX, mouseY);
                }
            }
        } else {
            hoveredButton = null;
        }
    }
}