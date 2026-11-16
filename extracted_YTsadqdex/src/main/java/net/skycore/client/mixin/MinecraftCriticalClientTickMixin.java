package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.module.combat.CriticalsModule;
import net.skycore.client.util.ApplyWindowIconHelper;
import net.skycore.client.util.ScheduleHelper;
import net.skycore.client.util.SynchronizeHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftCriticalClientTickMixin {
   @Inject(method = "tick", at = @At("HEAD"))
   private void skycore$criticalClientTick(CallbackInfo callbackInfo) {
      ScheduleHelper.onClientTickStart();
      CriticalsModule.INSTANCE.onClientTickStart();
      if (SynchronizeHelper.isActive()) {
         SkycoreClient.modules().get(AttackAuraModule.class).ifPresent(AttackAuraModule::onSlowFallingClientTick);
      }
   }

   @Inject(method = "tick", at = @At("RETURN"))
   private void skycore$actionsEnd(CallbackInfo callbackInfo) {
      ScheduleHelper.onClientTickEnd();
   }

   @Inject(method = "updateTitle", at = @At("TAIL"))
   private void skycore$applyWindowBranding(CallbackInfo callbackInfo) {
      ApplyWindowIconHelper.apply((Minecraft)(Object)this);
   }
}
