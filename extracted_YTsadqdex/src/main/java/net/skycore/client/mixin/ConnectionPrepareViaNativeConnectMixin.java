package net.skycore.client.mixin;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import java.net.InetSocketAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.module.combat.CriticalsModule;
import net.skycore.client.util.BeginSuppressViaPosRotHelper;
import net.skycore.client.util.ConsumeSilentHelper;
import net.skycore.client.util.NeedsPacketOrderSafeHelper;
import net.skycore.client.util.ShouldDropFailedUseOnHelper;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Connection.class)
public abstract class ConnectionPrepareViaNativeConnectMixin {
   @Inject(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;", at = @At("HEAD"), require = 0)
   private static void skycore$prepareViaNativeConnect(InetSocketAddress inetSocketAddress, net.minecraft.server.network.EventLoopGroupHolder eventLoopGroupHolder, Connection connection, CallbackInfoReturnable<ChannelFuture> callbackInfoReturnable) {
      NeedsPacketOrderSafeHelper.applyNativeBypassOnConnection(connection);
   }

   @Inject(method = "disconnect", at = @At("HEAD"), require = 0)
   private void skycore$resetViaBypass(CallbackInfo callbackInfo) {
      NeedsPacketOrderSafeHelper.resetNativeBypass();
   }

   @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
   private void skycore$onContainerClose(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
      if (packet instanceof ClientboundContainerClosePacket) {
         PacketReceiveEvent packetReceiveEvent = new PacketReceiveEvent(packet);
         SkycoreClient.events().post(packetReceiveEvent);
         if (packetReceiveEvent.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
   private void skycore$onSend(Packet<?> packet, ChannelFutureListener elements, boolean condition, CallbackInfo callbackInfo) {
      if (BeginSuppressViaPosRotHelper.shouldDropViaPosRot(packet)) {
         callbackInfo.cancel();
      } else if (ShouldDropFailedUseOnHelper.shouldDropFailedUseOn(packet)) {
         callbackInfo.cancel();
      } else if (!ConsumeSilentHelper.consumeSilent(packet)) {
         if (SynchronizeHelper.isActive()) {
            PacketSendEvent packetSendEvent = new PacketSendEvent(packet);
            SkycoreClient.events().post(packetSendEvent);
            CriticalsModule.INSTANCE.onPacketSend(packetSendEvent);
            if (packetSendEvent.isCancelled()) {
               callbackInfo.cancel();
            }
         }
      }
   }
}
