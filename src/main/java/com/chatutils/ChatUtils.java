package com.chatutils;

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

    public static final String MODID = "chatutils";
    public static final String NAME = "ChatUtils";
    public static final String VERSION = "1.2.0";

    @Mod.Instance(MODID)
    public static ChatUtils instance;

    public static KeyBinding openGuiKey;
    private static Configuration configFile;

    private int ticks = 0;
    private static boolean pendingGuiOpen = false;

    public static class Config {
        public static boolean compactingEnabled = true;
        public static int expireTimeSeconds = 60;
        public static boolean consecutiveOnly = false;
        public static boolean resetOnWorldChange = true;
        public static boolean stackedMessageCopyEnabled = true;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        File configPath = new File(event.getModConfigurationDirectory(), "chatutils.cfg");
        configFile = new Configuration(configPath);
        loadConfig();
    }

    public static void loadConfig() {
        if (configFile == null) {
            return;
        }

        configFile.load();

        Config.compactingEnabled = configFile.getBoolean("compactingEnabled", Configuration.CATEGORY_GENERAL, true,
                "Enable compact chat stacking.");
        Config.expireTimeSeconds = configFile.getInt("expireTimeSeconds", Configuration.CATEGORY_GENERAL, 60, -1, Integer.MAX_VALUE,
                "Stack expiry time in seconds. Use -1 for never.");
        Config.consecutiveOnly = configFile.getBoolean("consecutiveOnly", Configuration.CATEGORY_GENERAL, false,
                "Only stack consecutive messages.");
        Config.resetOnWorldChange = configFile.getBoolean("resetOnWorldChange", Configuration.CATEGORY_GENERAL, true,
                "Reset compact stacks when changing worlds.");
        Config.stackedMessageCopyEnabled = configFile.getBoolean("stackedMessageCopyEnabled", Configuration.CATEGORY_GENERAL, true,
                "Show copy icon for stacked messages.");

        if (configFile.hasChanged()) {
            configFile.save();
        }
    }

    public static void saveConfig() {
        if (configFile == null) {
            return;
        }

        configFile.get(Configuration.CATEGORY_GENERAL, "compactingEnabled", true).set(Config.compactingEnabled);
        configFile.get(Configuration.CATEGORY_GENERAL, "expireTimeSeconds", 60).set(Config.expireTimeSeconds);
        configFile.get(Configuration.CATEGORY_GENERAL, "consecutiveOnly", false).set(Config.consecutiveOnly);
        configFile.get(Configuration.CATEGORY_GENERAL, "resetOnWorldChange", true).set(Config.resetOnWorldChange);
        configFile.get(Configuration.CATEGORY_GENERAL, "stackedMessageCopyEnabled", true).set(Config.stackedMessageCopyEnabled);
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
        if (ChatUtils.Config.resetOnWorldChange) {
            ChatCompactHandler.reset();
        }
    }

    private static class ChatUtilsCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "chatutils";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/chatutils";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            pendingGuiOpen = true;
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }

    private static class CopyToClipboardCommand extends CommandBase {

        @Override
        public String getCommandName() {
            return "copytoclipboard";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/copytoclipboard <text>";
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args == null || args.length == 0) {
                return;
            }

            String joined = String.join(" ", args);
            try {
                GuiScreen.setClipboardString(joined);
            } catch (Exception ignored) {
            }
        }

        @Override
        public int getRequiredPermissionLevel() {
            return 0;
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }
    }
}
