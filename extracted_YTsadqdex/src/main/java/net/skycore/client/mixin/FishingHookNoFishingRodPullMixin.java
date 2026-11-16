package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.skycore.client.module.movement.NoPushModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FishingHook.class)
public abstract class FishingHookNoFishingRodPullMixin {
   @Inject(method = "pullEntity", at = @At("HEAD"), cancellable = true)
   private void skycore$noFishingRodPull(Entity entity, CallbackInfo callbackInfo) {
      if (NoPushModule.fishingRodPush()) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null && entity == instance) {
            callbackInfo.cancel();
         }
      }
   }
}
