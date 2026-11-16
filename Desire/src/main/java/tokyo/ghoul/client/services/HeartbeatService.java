package tokyo.ghoul.client.services;


import net.minecraft.client.gui.screens.ChatScreen;
import lombok.Getter;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.other.ScreenEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.system.client.GpsManager;
import tokyo.ghoul.api.system.configs.ConfigSkin;
import tokyo.ghoul.api.system.configs.MacroManager;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.other.ScreenUtil;
import tokyo.ghoul.api.utils.other.SlownessManager;

public class HeartbeatService implements QuickImports {
    @Getter private static final HeartbeatService instance = new HeartbeatService();

    public void load() {
        keyEvent();
        render2dEvent();
        tickEvent();
        screenEvent();
    }

    private void screenEvent() {
        ScreenEvent.getInstance().subscribe(new Listener<>(event -> {
            ScreenUtil.drawButton(event);
        }));
    }

    private void tickEvent() {
        TickEvent.getInstance().subscribe(new Listener<>(event -> {
            SlownessManager.tick();

            ConfigSkin.getInstance().fetchSkin();
        }));
    }

    private void render2dEvent() {
        Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.gui.screen() instanceof ChatScreen) {
                DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
                    if (draggable.getModule().isEnabled()) {
                        draggable.onDraw();
                    }
                });
            }

            GpsManager.getInstance().update(event.context());
        }));
    }

    private void keyEvent() {
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.action() != 1 || event.key() == -999 || event.key() == -1) return;

            int action = event.action();
            int key = event.key() + (event.mouse() ? -100 : 0);

            if (mc.gui.screen() == null) {
                ModuleManager.getInstance().getModules().forEach(module -> {
                    int bind = module.getBind();
                    if (bind == key && module.hasBind()) {
                        module.toggle();
                    }
                });

                MacroManager.getInstance().onKeyPressed(key);
            }
        }));
    }
}
