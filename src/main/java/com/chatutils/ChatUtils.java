package com.chatutils;

import com.chatutils.config.ChatUtilsConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import org.lwjgl.input.Keyboard;

import java.io.File;

@Mod(modid = ChatUtils.MODID, name = ChatUtils.NAME, version = ChatUtils.VERSION)
public class ChatUtils {

    public static final String MODID   = "chatutils";
    public static final String NAME    = "ChatUtils";
    public static final String VERSION = "2.2.0";

    @Mod.Instance(MODID)
    public static ChatUtils instance;

    public static KeyBinding openGuiKey;
    private static Configuration configFile;

    private int ticks = 0;
    private static boolean pendingGuiOpen = false;

    public static class Config {

        // Compacting
        public static boolean compactingEnabled         = true;
        public static int     expireTimeSeconds         = -1;
        public static boolean consecutiveOnly           = false;
        public static boolean stackedMessageCopyEnabled = true;

        // Timestamps
        public static boolean timestampsEnabled    = false;
        public static boolean timestamp24Hour      = true;
        public static boolean timestampShowSeconds = false;
        public static int     timestampStyle       = 0;

        // Chat Heads
        public static boolean chatHeads               = true;
        public static boolean offsetNonPlayerMessages = true;
        public static boolean hideHeadOnConsecutive   = false;

        // Visual
        public static boolean transparentChat = false;
        public static boolean animatedChat    = false;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configPath = new File(event.getModConfigurationDirectory(), "chatutils.cfg");
        configFile = new Configuration(configPath);
        loadConfig();
    }

    public static void loadConfig() {
        if (configFile == null) return;
        configFile.load();

        // Compacting
        Config.compactingEnabled         = configFile.getBoolean("compactingEnabled",         Configuration.CATEGORY_GENERAL, true,  "");
        Config.expireTimeSeconds         = configFile.getInt    ("expireTimeSeconds",          Configuration.CATEGORY_GENERAL, 60, -1, Integer.MAX_VALUE, "");
        Config.consecutiveOnly           = configFile.getBoolean("consecutiveOnly",            Configuration.CATEGORY_GENERAL, false, "");
        Config.stackedMessageCopyEnabled = configFile.getBoolean("stackedMessageCopyEnabled",  Configuration.CATEGORY_GENERAL, true,  "");

        // Timestamps
        Config.timestampsEnabled    = configFile.getBoolean("timestampsEnabled",    Configuration.CATEGORY_GENERAL, false, "");
        Config.timestamp24Hour      = configFile.getBoolean("timestamp24Hour",      Configuration.CATEGORY_GENERAL, true,  "");
        Config.timestampShowSeconds = configFile.getBoolean("timestampShowSeconds", Configuration.CATEGORY_GENERAL, false, "");
        Config.timestampStyle       = configFile.getInt    ("timestampStyle",       Configuration.CATEGORY_GENERAL, 0, 0, 1, "");

        // Chat Heads
        Config.chatHeads               = configFile.getBoolean("chatHeads",               Configuration.CATEGORY_GENERAL, true,  "");
        Config.offsetNonPlayerMessages = configFile.getBoolean("offsetNonPlayerMessages", Configuration.CATEGORY_GENERAL, true,  "");
        Config.hideHeadOnConsecutive   = configFile.getBoolean("hideHeadOnConsecutive",   Configuration.CATEGORY_GENERAL, false, "");

        // Visual
        Config.transparentChat = configFile.getBoolean("transparentChat", Configuration.CATEGORY_GENERAL, false, "");
        Config.animatedChat    = configFile.getBoolean("animatedChat",    Configuration.CATEGORY_GENERAL, false, "");

        if (configFile.hasChanged()) configFile.save();
    }

    public static void saveConfig() {
        if (configFile == null) return;

        // Compacting
        configFile.get(Configuration.CATEGORY_GENERAL, "compactingEnabled",         true ).set(Config.compactingEnabled);
        configFile.get(Configuration.CATEGORY_GENERAL, "expireTimeSeconds",         60   ).set(Config.expireTimeSeconds);
        configFile.get(Configuration.CATEGORY_GENERAL, "consecutiveOnly",           false).set(Config.consecutiveOnly);
        configFile.get(Configuration.CATEGORY_GENERAL, "stackedMessageCopyEnabled", true ).set(Config.stackedMessageCopyEnabled);

        // Timestamps
        configFile.get(Configuration.CATEGORY_GENERAL, "timestampsEnabled",    false).set(Config.timestampsEnabled);
        configFile.get(Configuration.CATEGORY_GENERAL, "timestamp24Hour",      true ).set(Config.timestamp24Hour);
        configFile.get(Configuration.CATEGORY_GENERAL, "timestampShowSeconds", false).set(Config.timestampShowSeconds);
        configFile.get(Configuration.CATEGORY_GENERAL, "timestampStyle",       0    ).set(Config.timestampStyle);

        // Chat Heads
        configFile.get(Configuration.CATEGORY_GENERAL, "chatHeads",               true ).set(Config.chatHeads);
        configFile.get(Configuration.CATEGORY_GENERAL, "offsetNonPlayerMessages", true ).set(Config.offsetNonPlayerMessages);
        configFile.get(Configuration.CATEGORY_GENERAL, "hideHeadOnConsecutive",   false).set(Config.hideHeadOnConsecutive);

        // Visual
        configFile.get(Configuration.CATEGORY_GENERAL, "transparentChat", false).set(Config.transparentChat);
        configFile.get(Configuration.CATEGORY_GENERAL, "animatedChat",    false).set(Config.animatedChat);

        configFile.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        openGuiKey = new KeyBinding("key.chatutils.opengui", Keyboard.KEY_B, "key.categories.chatutils");
        ClientRegistry.registerKeyBinding(openGuiKey);

        ClientCommandHandler.instance.registerCommand(new ChatUtilsCommand());
        ClientCommandHandler.instance.registerCommand(new CopyToClipboardCommand());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGuiKey.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new ChatUtilsConfigGui(null));
        }
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            if (pendingGuiOpen) {
                Minecraft.getMinecraft().displayGuiScreen(new ChatUtilsConfigGui(null));
                pendingGuiOpen = false;
            }

            if (++ticks >= 12000) {
                ChatCompactHandler.cleanupExpired();
                ticks = 0;
            }
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        ticks = 0;
    }

    private static class ChatUtilsCommand extends CommandBase {
        public String  getCommandName()                                     { return "chatutils"; }
        public String  getCommandUsage(ICommandSender sender)               { return "/chatutils"; }
        public void    processCommand(ICommandSender sender, String[] args) { pendingGuiOpen = true; }
        public int     getRequiredPermissionLevel()                         { return 0; }
        public boolean canCommandSenderUseCommand(ICommandSender s)         { return true; }
    }

    private static class CopyToClipboardCommand extends CommandBase {
        public String  getCommandName()                                     { return "copytoclipboard"; }
        public String  getCommandUsage(ICommandSender sender)               { return "/copytoclipboard <text>"; }
        public void    processCommand(ICommandSender sender, String[] args) {
            if (args == null || args.length == 0) return;
            try { GuiScreen.setClipboardString(String.join(" ", args)); } catch (Exception ignored) {}
        }
        public int     getRequiredPermissionLevel()                         { return 0; }
        public boolean canCommandSenderUseCommand(ICommandSender s)         { return true; }
    }
}