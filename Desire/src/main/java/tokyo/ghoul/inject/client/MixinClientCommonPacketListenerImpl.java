package tokyo.ghoul.inject.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.IBotConnection;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.IBotListener;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinClientCommonPacketListenerImpl implements IBotListener {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    @Final
    protected Connection connection;

    @Override
    public BotSession ghoul$getBotSession() {
        return ((IBotConnection) (Object) this.connection).ghoul$getBotSession();
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;packetProcessor()Lnet/minecraft/network/PacketProcessor;"))
    private PacketProcessor ghoul$botPacketProcessor(Minecraft minecraft, Operation<PacketProcessor> original) {
        BotSession session = ghoul$getBotSession();
        if (session != null && session.getPacketProcessor() != null) {
            return session.getPacketProcessor();
        }
        return original.call(minecraft);
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
    private void ghoul$botOnDisconnect(DisconnectionDetails details, CallbackInfo ci) {
        BotSession session = ghoul$getBotSession();
        if (session == null) {
            return;
        }
        BotSessionManager manager = BotSessionManager.getInstance();
        if (session == manager.getMainSession()) {
            if (manager.isForeground(session)) {
                return;
            }
            ci.cancel();
            ClientCommonPacketListenerImpl self = (ClientCommonPacketListenerImpl) (Object) this;
            this.minecraft.execute(() -> {
                manager.switchToMain();
                self.onDisconnect(details);
            });
            return;
        }
        if (details != null && details.reason() != null) {
            session.setDisconnectReason(details.reason().getString());
        }
        this.minecraft.execute(() -> manager.handleBotDisconnect(session));
        ci.cancel();
    }

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void ghoul$botResourcePack(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        BotSession session = ghoul$getBotSession();
        if (session == null || session == BotSessionManager.getInstance().getMainSession()) {
            return;
        }
        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        ci.cancel();
    }
}
