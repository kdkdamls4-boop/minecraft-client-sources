package tokyo.ghoul.api.system.interfaces;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.network.protocol.Packet;
import tokyo.ghoul.api.utils.other.NetworkUtil;
import tokyo.ghoul.api.utils.other.TextUtil;

public interface QuickImports {
    Minecraft mc = Minecraft.getInstance();
    tokyo.ghoul.api.utils.player.SwapManager swapManager = tokyo.ghoul.api.utils.player.SwapManager.getInstance();
    tokyo.ghoul.api.utils.player.PlayerHandler playerHandler = tokyo.ghoul.api.utils.player.PlayerHandler.getInstance();

    default void print(String message) {
        TextUtil.sendMessage(message);
    }

    default void sendPacket(PredictiveAction packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(PredictiveAction packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
    default void sendPacket(Packet<?> packet) {
        NetworkUtil.sendPacket(packet);
    }
    default void sendSilentPacket(Packet<?> packet) {
        NetworkUtil.sendSilentPacket(packet);
    }
}
