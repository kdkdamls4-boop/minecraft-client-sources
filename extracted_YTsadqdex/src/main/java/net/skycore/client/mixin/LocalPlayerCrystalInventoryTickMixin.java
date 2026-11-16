package net.skycore.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.combat.RotationClaim;
import net.skycore.client.module.combat.AutoWebTrapModule;
import net.skycore.client.module.combat.CrystalAuraModule;
import net.skycore.client.util.FixInputHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayer.class, priority = 1200)
public abstract class LocalPlayerCrystalInventoryTickMixin {
   @Inject(method = "tick", at = @At("HEAD"), order = 0)
   private void skycore$crystalInventoryTick(CallbackInfo callbackInfo) {
      FixInputHelper.tick();
   }

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
   private void skycore$crystalPre(CallbackInfo callbackInfo) {
      RotationClaim.onClientTick();
      AutoWebTrapModule.INSTANCE.runCombatTick();
      CrystalAuraModule.INSTANCE.onSourcePreTick();
      CrystalAuraModule.INSTANCE.onSourcePostTick();
   }

   @Inject(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isControlledCamera()Z", shift = Shift.AFTER))
   private void skycore$crystalPacketRotation(CallbackInfo callbackInfo) {
      FixInputHelper.beforePacket();
      RotationClaim.beforeMovementPacket();
   }

   @Inject(method = "sendPosition", at = @At("TAIL"))
   private void skycore$crystalRotationSent(CallbackInfo callbackInfo) {
      RotationClaim.captureRotation();
      AutoWebTrapModule.INSTANCE.afterMovementPacket();
      CrystalAuraModule.INSTANCE.afterMovementPacket();
   }
}
