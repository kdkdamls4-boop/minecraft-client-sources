package tokyo.ghoul.inject.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientConfigurationPacketListenerImpl;
import net.minecraft.network.PacketProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.IBotListener;

@Mixin(ClientConfigurationPacketListenerImpl.class)
public class MixinClientConfigurationPacketListenerImpl {

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;packetProcessor()Lnet/minecraft/network/PacketProcessor;"))
    private PacketProcessor ghoul$botPacketProcessor(Minecraft minecraft, Operation<PacketProcessor> original) {
        BotSession session = ((IBotListener) this).ghoul$getBotSession();
        if (session != null && session.getPacketProcessor() != null) {
            return session.getPacketProcessor();
        }
        return original.call(minecraft);
    }
}
