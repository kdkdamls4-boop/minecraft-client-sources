package tokyo.ghoul.api.utils.other;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;

/** Следит за порядком пакетов в тике: «чистый» тик — после пакета движения ещё не было действий. */
public final class TickPacketOrderTracker {
    private static volatile boolean movementSent;
    private static volatile boolean teleportConfirmed;
    private static volatile boolean actionSent;

    private TickPacketOrderTracker() {
    }

    public static void onPacketSend(Packet<?> packet) {
        if (packet instanceof ServerboundAcceptTeleportationPacket) {
            teleportConfirmed = true;
        } else if (packet instanceof ServerboundMovePlayerPacket) {
            if (!teleportConfirmed) {
                actionSent = false;
                movementSent = true;
            }
            teleportConfirmed = false;
        } else if (packet instanceof ServerboundClientTickEndPacket) {
            if (!movementSent) {
                actionSent = false;
            }
            movementSent = false;
        } else if (isActionPacket(packet)) {
            actionSent = true;
        }
    }

    public static boolean isTickClean() {
        return !actionSent && !movementSent;
    }

    private static boolean isActionPacket(Packet<?> packet) {
        if (packet instanceof ServerboundPlayerActionPacket action) {
            return action.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND;
        }
        return packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundAttackPacket
                || packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundPickItemFromBlockPacket
                || packet instanceof ServerboundPickItemFromEntityPacket
                || packet instanceof ServerboundPlayerCommandPacket;
    }
}
