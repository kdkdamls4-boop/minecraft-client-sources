package tokyo.ghoul.api.utils.player;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;

@Getter
public class PlayerHandler implements QuickImports {
    private static final PlayerHandler instance = new PlayerHandler();

    public static PlayerHandler getInstance() {
        return instance;
    }

    private boolean serverSprinting;
    private int sprintingChangeTicks;

    public PlayerHandler() {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            sprintingChangeTicks++;
        }));

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;

            if (event.isSend() && event.packet() instanceof ServerboundPlayerCommandPacket command) {
                if (command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
                    if (serverSprinting) {
                        PacketEvent.getInstance().setCancel(true);
                    }
                    if (!PacketEvent.getInstance().isCancel()) {
                        sprintingChangeTicks = 0;
                    }
                    serverSprinting = true;
                } else if (command.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
                    if (!serverSprinting) {
                        PacketEvent.getInstance().setCancel(true);
                    }
                    if (!PacketEvent.getInstance().isCancel()) {
                        sprintingChangeTicks = 0;
                    }
                    serverSprinting = false;
                }
            }
        }));
    }

    public void load() {}
}
