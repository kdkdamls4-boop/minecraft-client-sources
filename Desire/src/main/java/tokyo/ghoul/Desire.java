package tokyo.ghoul;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.command.CommandManager;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.system.DiscordRpc;
import tokyo.ghoul.api.system.configs.ConfigManager;
import tokyo.ghoul.api.system.configs.ConfigSkin;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.system.configs.MacroManager;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.api.system.files.FileManager;
import tokyo.ghoul.api.utils.other.SoundUtil;
import tokyo.ghoul.api.utils.render.AmbienceSaturationProgram;
import tokyo.ghoul.api.utils.render.KawaseBlurProgram;
import tokyo.ghoul.api.utils.render.MagnifierProgram;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.inventory.DelayedInventoryActionManager;
import tokyo.ghoul.api.utils.inventory.InventoryActionManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroManager;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.AutoBuyEngine;
import tokyo.ghoul.client.autobuy.ItemDatabase;
import tokyo.ghoul.client.services.HeartbeatService;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.account.AccountSwitcher;
import tokyo.ghoul.client.ui.theme.ThemeEditor;
import tokyo.ghoul.client.ui.widget.WidgetManager;
import tokyo.ghoul.api.utils.player.PlayerHandler;
import tokyo.ghoul.api.utils.player.SwapManager;

public class Desire implements ClientModInitializer {
	@Getter private static Desire instance = new Desire();

    @Override
	public void onInitializeClient() {
        instance = this;

        EvaPipelines.register();
        EvaLayers.register();

        LevelRenderEvents.COLLECT_SUBMITS.register(context -> {
            if (context.poseStack() == null || context.submitNodeCollector() == null) return;
            RenderUtil.WORLD.beginFrame(context.submitNodeCollector());
            Render3DEvent.getInstance().call(new Render3DEvent.Render3DEventData(
                    context.poseStack(),
                    net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false),
                    context.submitNodeCollector(), context.levelState().cameraRenderState));
            RenderUtil.WORLD.finishFrame();
        });

        SoundUtil.load();

        ItemDatabase.reload();
        AutoBuyConfig.load();

        loadManagers();
        loadServices();
        loadFiles();
    }

    public void postLoad() {
        rebindMinecraft();

        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isEnabled()) {
                module.applyEnabledState();
            }
        }

        ModuleManager.getInstance().getModules().sort((a, b) -> Float.compare(
                Fonts.PS_MEDIUM.getWidth(b.getName(), 7f),
                Fonts.PS_MEDIUM.getWidth(a.getName(), 7f)
        ));

        KawaseBlurProgram.load();
        MagnifierProgram.load();
        AmbienceSaturationProgram.load();
        AccountSwitcher.applyStartupAccount();
    }

    private void loadFiles() {
        ConfigManager.getInstance().load("autoConfig");
        ConfigManager.getInstance().enableAutosave();
        DraggableManager.getInstance().load();
        FriendManager.getInstance().load();
        MacroManager.getInstance().load();
    }

    private void loadManagers() {
        PlayerHandler.getInstance().load();
        SwapManager.getInstance().load();
        WidgetManager.getInstance().load();
        RotationManager.getInstance().load();
        InventoryActionManager.INSTANCE.load();
        DelayedInventoryActionManager.INSTANCE.load();
        NeuroManager.getInstance().load();

        ModuleManager.getInstance().load();
        CommandManager.getInstance().load();
        AutoBuyEngine.INSTANCE.register();

        ThemeEditor.getInstance().load();
    }

    private void loadServices() {
        HeartbeatService.getInstance().load();
        RenderService.getInstance().load();
        ConfigSkin.getInstance().load();
        DiscordRpc.startRPC();
    }

    public void onClose() {
        ConfigManager.getInstance().save("autoConfig");
        FileManager.getInstance().save();
        ThemeEditor.getInstance().save(true);
        DraggableManager.getInstance().save();
        MacroManager.getInstance().save();
        AutoBuyConfig.save();

        DiscordRpc.stop();
    }

    private static void rebindMinecraft() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        try {
            java.lang.reflect.Field unsafeField = Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            java.lang.reflect.Field mcField = null;
            for (java.lang.reflect.Field f : QuickImports.class.getDeclaredFields()) {
                if (f.getType() == Minecraft.class) {
                    mcField = f;
                    break;
                }
            }
            if (mcField == null) mcField = QuickImports.class.getDeclaredField("mc");
            mcField.setAccessible(true);
            Object base = unsafe.getClass().getMethod("staticFieldBase", java.lang.reflect.Field.class).invoke(unsafe, mcField);
            long offset = (Long) unsafe.getClass().getMethod("staticFieldOffset", java.lang.reflect.Field.class).invoke(unsafe, mcField);
            unsafe.getClass().getMethod("putObject", Object.class, long.class, Object.class).invoke(unsafe, base, offset, client);
        } catch (Throwable t) {
            System.err.println("Failed to rebind Minecraft: " + t);
        }
    }
}
