package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.module.combat.AntiThornsModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerThornsDamageMixin {
   @Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
   private void skycore$thornsDamage(ClientboundDamageEventPacket clientboundDamageEventPacket, CallbackInfo callbackInfo) {
      skycore$dispatchThornsPacket(clientboundDamageEventPacket, callbackInfo);
   }

   @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
   private void skycore$thornsMotion(ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket, CallbackInfo callbackInfo) {
      skycore$dispatchThornsPacket(clientboundSetEntityMotionPacket, callbackInfo);
   }

   @Unique
   private static void skycore$dispatchThornsPacket(Packet<?> packet, CallbackInfo callbackInfo) {
      if (Minecraft.getInstance().isSameThread() && AntiThornsModule.INSTANCE.isEnabled()) {
         PacketReceiveEvent packetReceiveEvent = new PacketReceiveEvent(packet);
         SkycoreClient.events().post(packetReceiveEvent);
         if (packetReceiveEvent.isCancelled()) {
            callbackInfo.cancel();
         }
      }
   }
}
