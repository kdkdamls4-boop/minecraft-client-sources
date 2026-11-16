package tokyo.ghoul.inject.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.command.CommandManager;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.IBotListener;

@Mixin(ClientPacketListener.class)
public class MixinClientPlayNetworkHandler {
    @Inject(method = "sendChat", at = @At("HEAD"), cancellable = true)
    private void sendChatMessage(String content, CallbackInfo ci) {
        CommandManager.getInstance().executeCommands(content, ci);
    }

    @WrapOperation(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;packetProcessor()Lnet/minecraft/network/PacketProcessor;"))
    private PacketProcessor ghoul$botPacketProcessor(Minecraft minecraft, Operation<PacketProcessor> original) {
        BotSession session = ((IBotListener) this).ghoul$getBotSession();
        if (session != null && session.getPacketProcessor() != null) {
            return session.getPacketProcessor();
        }
        return original.call(minecraft);
    }

    @WrapOperation(method = "handleConfigurationStart", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;clearClientLevel(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void ghoul$botClearLevel(Minecraft minecraft, Screen screen, Operation<Void> original) {
        if (!BotContext.isBackgroundContextActive()) {
            original.call(minecraft, screen);
            return;
        }
        ((ClientPacketListener) (Object) this).clearLevel();
        minecraft.level = null;
        minecraft.player = null;
        minecraft.gameMode = null;
    }

    @Inject(method = "setActionBarText", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void ghoul$botActionBar(ClientboundSetActionBarTextPacket packet, CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            BotContext.current().setActionBar(packet.text().getString());
            ci.cancel();
        }
    }

    @Inject(method = {"setTitleText", "setSubtitleText", "setTitlesAnimation", "handleTitlesClear", "handleTabListCustomisation", "handleBossUpdate"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void ghoul$botHudPackets(CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }
}
