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
    private int       lastMouseX;
    private int       lastMouseY;
    private long      hoverStartTime;

    public ChatUtilsConfigGui(GuiScreen parent) {
        this.parent = parent;
    }



    @Override
    public void initGui() {
        refreshButtons();
    }

    private void refreshButtons() {
        buttonList.clear();

        int cx     = width / 2;
        int startY = height / 6;

        int col1X = cx - 235;
        int col2X = cx - 80;
        int col3X = cx + 5;   // wait – we have 4 columns, adjust


        int leftX  = cx - 160;
        int rightX = cx + 5;

        int leftY  = startY;
        int rightY = startY;

        // Column 1: Compact Chat
        GuiButton compactToggle = new GuiButton(0, leftX, leftY, 150, 20,
                "Compact Chat: " + onOff(ChatUtils.Config.compactingEnabled));
        leftY += 24;

        String expireText = ChatUtils.Config.expireTimeSeconds == -1
                ? "Never" : ChatUtils.Config.expireTimeSeconds + "s";
        GuiButton expireBtn = new GuiButton(1, leftX, leftY, 150, 20,
                "Expire Time: " + expireText);
        leftY += 24;

        GuiButton consecutiveBtn = new GuiButton(2, leftX, leftY, 150, 20,
                "Consecutive Mode: " + onOff(ChatUtils.Config.consecutiveOnly));
        leftY += 24;

        GuiButton copyBtn = new GuiButton(3, leftX, leftY, 150, 20,
                "Stacked Copy: " + onOff(ChatUtils.Config.stackedMessageCopyEnabled));
        leftY += 24;

        //  Column 2: Timestamps
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
        rightY += 24;

        //  Second row: Chat Heads and Visual
        // Push both rows below the longer of the two first-row columns.
        int secondRowY = Math.max(leftY, rightY) + 12;

        int headsX  = leftX;
        int visualX = rightX;
        int headsY  = secondRowY;
        int visualY = secondRowY;

        //  Column 3: Chat Heads
        GuiButton headsToggle = new GuiButton(10, headsX, headsY, 150, 20,
                "Chat Heads: " + onOff(ChatUtils.Config.chatHeads));
        headsY += 24;

        GuiButton offsetBtn = new GuiButton(11, headsX, headsY, 150, 20,
                "Offset Others: " + onOff(ChatUtils.Config.offsetNonPlayerMessages));
        headsY += 24;

        GuiButton hideConsecutiveBtn = new GuiButton(12, headsX, headsY, 150, 20,
                "Hide Repeat Head: " + onOff(ChatUtils.Config.hideHeadOnConsecutive));
        headsY += 24;

        // Column 4: Visual
        GuiButton transparentBtn = new GuiButton(20, visualX, visualY, 150, 20,
                "Transparent Chat: " + onOff(ChatUtils.Config.transparentChat));
        visualY += 24;

        GuiButton animatedBtn = new GuiButton(21, visualX, visualY, 150, 20,
                "Animated Chat: " + onOff(ChatUtils.Config.animatedChat));
        visualY += 24;

        //  Done
        int doneY = Math.max(headsY, visualY) + 12;
        GuiButton done = new GuiButton(99, cx - 100, doneY, 200, 20, "Done");

        // Register
        buttonList.add(compactToggle);
        buttonList.add(expireBtn);
        buttonList.add(consecutiveBtn);
        buttonList.add(copyBtn);

        buttonList.add(timestampToggle);
        buttonList.add(formatBtn);
        buttonList.add(secondsBtn);
        buttonList.add(styleBtn);

        buttonList.add(headsToggle);
        buttonList.add(offsetBtn);
        buttonList.add(hideConsecutiveBtn);

        buttonList.add(transparentBtn);
        buttonList.add(animatedBtn);

        buttonList.add(done);

        //  Dynamic buttons
        boolean compact = ChatUtils.Config.compactingEnabled;
        expireBtn.enabled      = compact;
        consecutiveBtn.enabled = compact;
        copyBtn.enabled        = compact;

        boolean timestamps = ChatUtils.Config.timestampsEnabled;
        formatBtn.enabled  = timestamps;
        secondsBtn.enabled = timestamps;
        styleBtn.enabled   = timestamps;

        boolean heads    = ChatUtils.Config.chatHeads;
        offsetBtn.enabled          = heads;
        hideConsecutiveBtn.enabled = heads;
    }

    private String onOff(boolean b) { return b ? "ON" : "OFF"; }


    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {

            // Compact Chat
            case 0: ChatUtils.Config.compactingEnabled = !ChatUtils.Config.compactingEnabled; break;
            case 1:
                int t = ChatUtils.Config.expireTimeSeconds;
                if      (t == 30)  ChatUtils.Config.expireTimeSeconds = 60;
                else if (t == 60)  ChatUtils.Config.expireTimeSeconds = 120;
                else if (t == 120) ChatUtils.Config.expireTimeSeconds = 300;
                else if (t == 300) ChatUtils.Config.expireTimeSeconds = -1;
                else               ChatUtils.Config.expireTimeSeconds = 30;
                break;
            case 2: ChatUtils.Config.consecutiveOnly           = !ChatUtils.Config.consecutiveOnly;           break;
            case 3: ChatUtils.Config.stackedMessageCopyEnabled = !ChatUtils.Config.stackedMessageCopyEnabled; break;

            // Timestamps
            case 4: ChatUtils.Config.timestampsEnabled   = !ChatUtils.Config.timestampsEnabled;   break;
            case 5: ChatUtils.Config.timestamp24Hour      = !ChatUtils.Config.timestamp24Hour;      break;
            case 6: ChatUtils.Config.timestampShowSeconds = !ChatUtils.Config.timestampShowSeconds; break;
            case 7: ChatUtils.Config.timestampStyle = ChatUtils.Config.timestampStyle == 0 ? 1 : 0; break;

            //  Chat Heads
            case 10: ChatUtils.Config.chatHeads               = !ChatUtils.Config.chatHeads;               break;
            case 11: ChatUtils.Config.offsetNonPlayerMessages = !ChatUtils.Config.offsetNonPlayerMessages; break;
            case 12: ChatUtils.Config.hideHeadOnConsecutive   = !ChatUtils.Config.hideHeadOnConsecutive;   break;

            //  Visual
            case 20: ChatUtils.Config.transparentChat = !ChatUtils.Config.transparentChat; break;
            case 21: ChatUtils.Config.animatedChat    = !ChatUtils.Config.animatedChat;    break;

            //  Done
            case 99:
                mc.displayGuiScreen(parent);
                return;
        }

        ChatUtils.saveConfig();
        refreshButtons();
    }


    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();

        int cx     = width / 2;
        int startY = height / 6;

        drawCenteredString(fontRendererObj, "Chat Utils Config", cx, 10, 0xFFFFFF);

        // Column headers
        drawString(fontRendererObj, "Compact Chat",  cx - 160, startY - 12, 0xAAAAAA);
        drawString(fontRendererObj, "Timestamps",    cx +   5, startY - 12, 0xAAAAAA);

        // Second-row headers – replicate the Y calculation from refreshButtons().
        int leftY  = startY + 4 * 24;   // 4 compact buttons
        int rightY = startY + 4 * 24;   // 4 timestamp buttons
        int secondRowY = Math.max(leftY, rightY) + 12;

        drawString(fontRendererObj, "Chat Heads", cx - 160, secondRowY - 12, 0xAAAAAA);
        drawString(fontRendererObj, "Visual",     cx +   5, secondRowY - 12, 0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks);

        // Tooltip logic
        GuiButton currentHover = null;
        for (GuiButton b : buttonList) {
            if (b.isMouseOver()) { currentHover = b; break; }
        }

        if (currentHover != null) {
            if (currentHover != hoveredButton
                    || mouseX != lastMouseX
                    || mouseY != lastMouseY) {
                hoveredButton  = currentHover;
                hoverStartTime = System.currentTimeMillis();
                lastMouseX     = mouseX;
                lastMouseY     = mouseY;
                return;
            }

            if (System.currentTimeMillis() - hoverStartTime >= 600) {
                List<String> tooltip = getTooltip(hoveredButton.id);
                if (tooltip != null) drawHoveringText(tooltip, mouseX, mouseY);
            }
        } else {
            hoveredButton = null;
        }
    }

    private List<String> getTooltip(int id) {
        switch (id) {
            // Compact
            case 0:  return Arrays.asList("Merge identical chat messages.");
            case 1:  return Arrays.asList("30s / 60s / 120s / 300s – only stack within that time.", "", "Never – no time limit.");
            case 2:  return Arrays.asList("Only stack consecutive messages.");
            case 3:  return Arrays.asList("Copy compacted messages to clipboard.");
            // Timestamps
            case 4:  return Arrays.asList("Show time before messages.");
            case 5:  return Arrays.asList("Use 24-hour or 12-hour format.");
            case 6:  return Arrays.asList("Display seconds in the timestamp.");
            case 7:  return Arrays.asList("Change timestamp bracket style.");
            // Chat Heads
            case 10: return Arrays.asList("Draw the sender's skin face beside each chat line.");
            case 11: return Arrays.asList("Shift non-player messages right so all lines stay aligned.");
            case 12: return Arrays.asList("(BREAKS WHILE COMPACT CHAT IS ENABLED)", "", "Hide the head on back-to-back messages from the same player.");
            // Visual
            case 20: return Arrays.asList("Makes chat background transparent.");
            case 21: return Arrays.asList("Aminates chat messages.");
            default: return null;
        }
    }
}