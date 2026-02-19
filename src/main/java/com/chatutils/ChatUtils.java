package com.chatutils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
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

@Mod(modid = ChatUtils.MODID, name = ChatUtils.NAME, version = ChatUtils.VERSION)
public class ChatUtils {

    public static final String MODID = "chatutils";
    public static final String NAME = "ChatUtils";
    public static final String VERSION = "1.2.0";

    @Mod.Instance(MODID)
    public static ChatUtils instance;

    public static KeyBinding openGuiKey;

    private int ticks = 0;
    private static boolean pendingGuiOpen = false;

    public static class Config {
        public static boolean compactingEnabled = true;
        public static int expireTimeSeconds = 60;
        public static boolean consecutiveOnly = false;
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        openGuiKey = new KeyBinding("key.chatutils.opengui", Keyboard.KEY_B, "key.categories.chatutils");
        ClientRegistry.registerKeyBinding(openGuiKey);

        ClientCommandHandler.instance.registerCommand(new ChatUtilsCommand());
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
        ChatCompactHandler.reset();
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
}