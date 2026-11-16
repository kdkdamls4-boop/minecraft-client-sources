package tokyo.ghoul.api.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;

public final class CarriedItemSync {
    private static int lastServerSlot = -1;
    private static boolean useInWindow;

    static {
        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isSend()) return;
            if (event.packet() instanceof ServerboundSetCarriedItemPacket packet) {
                if (!PacketEvent.getInstance().isCancel()) {
                    lastServerSlot = packet.getSlot();
                }
            } else if (event.packet() instanceof ServerboundMovePlayerPacket) {
                useInWindow = false;
            } else if (event.packet() instanceof ServerboundUseItemPacket
                    || event.packet() instanceof ServerboundUseItemOnPacket
                    || event.packet() instanceof ServerboundInteractPacket) {
                useInWindow = true;
            }
        }));
    }

    private CarriedItemSync() {
    }

    public static void hook() {
    }

    public static boolean send(int slot) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return false;
        if (slot < 0 || slot > 8 || slot == lastServerSlot) return false;

        int previous = lastServerSlot;
        mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
        if (PacketEvent.getInstance().isCancel()) {
            lastServerSlot = previous;
            return false;
        }
        lastServerSlot = slot;
        return true;
    }

    public static boolean isUseInWindow() {
        return useInWindow;
    }
}
