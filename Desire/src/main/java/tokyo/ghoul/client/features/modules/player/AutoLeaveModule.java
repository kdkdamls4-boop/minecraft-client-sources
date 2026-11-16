package tokyo.ghoul.client.features.modules.player;

import lombok.Getter;
import net.minecraft.client.player.AbstractClientPlayer;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.system.configs.FriendManager;

@ModuleRegister(name = "Auto Leave", description = "Автоматический выход при опасности", category = Category.PLAYER)
public class AutoLeaveModule extends Module {
    @Getter private static final AutoLeaveModule instance = new AutoLeaveModule();

    private final SliderSetting distance = new SliderSetting("Distance").value(50f).range(1f, 100f).step(1f);
    private final ModeSetting action = new ModeSetting("Action").value("Spawn").values("Hub", "Spawn", "Home");

    public AutoLeaveModule() {
        addSettings(distance, action);
    }

    @Override
    public void onEvent() {
        EventListener updateEvent = UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            handleUpdateEvent();
        }));

        addEvents(updateEvent);
    }

    private void handleUpdateEvent() {
        for (AbstractClientPlayer player : mc.level.players()) {
            if (mc.player == player) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;

            if (player.position().distanceTo(mc.player.position()) <= distance.getValue()) {
                handleLeave();
                toggle();
                break;
            }
        }
    }

    private void handleLeave() {
        switch (action.getValue()) {
            case "Hub" -> mc.player.connection.sendCommand("hub");
            case "Spawn" -> mc.player.connection.sendCommand("spawn");
            case "Home" -> mc.player.connection.sendCommand("home home");
        }
    }
}
