package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMoveMixin {
   @Shadow
   private ClientLevel level;

   @Inject(method = "handleMoveEntity", at = @At("TAIL"))
   private void skycore$move(ClientboundMoveEntityPacket clientboundMoveEntityPacket, CallbackInfo callbackInfo) {
      Entity entity = clientboundMoveEntityPacket.getEntity(this.level);
      if (entity != null) {
         InterpolationHandler interpolation = entity.getInterpolation();
         float yRot = clientboundMoveEntityPacket.hasRotation() ? clientboundMoveEntityPacket.getYRot() : (interpolation != null ? interpolation.yRot() : entity.getYRot());
         float xRot = clientboundMoveEntityPacket.hasRotation() ? clientboundMoveEntityPacket.getXRot() : (interpolation != null ? interpolation.xRot() : entity.getXRot());
         EntityGetResolvedYawAccessor entityGetResolvedYawAccessor = (EntityGetResolvedYawAccessor)entity;
         entityGetResolvedYawAccessor.skycore$setResolvedYaw(yRot);
         entityGetResolvedYawAccessor.skycore$setResolvedPitch(xRot);
      }
   }

   @Inject(method = "handleEntityPositionSync", at = @At("TAIL"))
   private void skycore$sync(ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket, CallbackInfo callbackInfo) {
      Entity entity = this.level.getEntity(clientboundEntityPositionSyncPacket.id());
      if (entity instanceof Player) {
         EntityGetResolvedYawAccessor entityGetResolvedYawAccessor = (EntityGetResolvedYawAccessor)entity;
         entityGetResolvedYawAccessor.skycore$setResolvedYaw(clientboundEntityPositionSyncPacket.values().yRot());
         entityGetResolvedYawAccessor.skycore$setResolvedPitch(clientboundEntityPositionSyncPacket.values().xRot());
      }
   }
}
