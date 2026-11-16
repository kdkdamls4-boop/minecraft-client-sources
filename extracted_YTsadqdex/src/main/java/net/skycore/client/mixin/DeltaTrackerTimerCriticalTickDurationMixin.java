package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker.Timer;
import net.skycore.client.module.combat.CriticalsModule;
import net.skycore.client.util.ResetSpeedHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Timer.class)
public abstract class DeltaTrackerTimerCriticalTickDurationMixin {
   @Shadow
   private float deltaTicks;
   @Shadow
   @Final
   private float msPerTick;

   @ModifyExpressionValue(method = "advanceGameTime", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"))
   private float skycore$criticalTickDuration(float value) {
      float value2 = CriticalsModule.INSTANCE.timerSpeedOverride();
      return Float.isFinite(value2) ? this.msPerTick / value2 : value;
   }

   @Inject(
      method = "advanceGameTime",
      at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", opcode = 181, shift = Shift.AFTER)
   )
   private void skycore$scaleDeltaTicks(long timestamp, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
      float speed = ResetSpeedHelper.getSpeed();
      float value = CriticalsModule.INSTANCE.timerSpeedOverride();
      if (Float.isFinite(value)) {
         speed = 1.0F;
      }

      if (Math.abs(speed - 1.0F) >= 0.001F) {
         this.deltaTicks *= speed;
      }
   }
}
