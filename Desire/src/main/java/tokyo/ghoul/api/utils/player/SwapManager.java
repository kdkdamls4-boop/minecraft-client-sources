package tokyo.ghoul.api.utils.player;

import lombok.Getter;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.inventory.ContainerInput;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.move.SprintEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.other.NetworkUtil;

public class SwapManager implements QuickImports {
    @Getter
    private static final SwapManager instance = new SwapManager();

    @Getter
    private float ticksCondition;

    public SwapManager() {
        UpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            if (ticksCondition > 0) ticksCondition--;
        }));

        SprintEvent.getInstance().subscribe(new Listener<>(-10, event -> {
            if (mc.player == null) return;
            if (ticksCondition > 0) event.setSprint(false);
        }));
    }

    public void grimSwap(int syncId, int slotId, int button, ContainerInput actionType) {
        if (mc.player == null || mc.player.connection == null || mc.gameMode == null) return;

        mc.player.connection.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
        if (PlayerHandler.getInstance().isServerSprinting()) {
            ticksCondition = 2;
            mc.player.setSprinting(false);
            mc.player.connection.send(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
        }
        mc.gameMode.handleContainerInput(syncId, slotId, button, actionType, mc.player);
        NetworkUtil.sendSilentPacket(new ServerboundContainerClosePacket(0));
        mc.player.connection.send(new ServerboundPlayerInputPacket(mc.player.input.keyPresses));
    }

    public void load() {}
}
