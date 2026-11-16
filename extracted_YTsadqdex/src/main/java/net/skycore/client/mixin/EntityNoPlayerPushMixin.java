package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.skycore.client.module.movement.NoPushModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityNoPlayerPushMixin {
   @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void skycore$noPlayerPush(Entity entity, CallbackInfo callbackInfo) {
      if (NoPushModule.playersPush()) {
         LocalPlayer instance = Minecraft.getInstance().player;
         if (instance != null) {
            Entity entity2 = (Entity)(Object)this;
            if (entity2 == instance || entity == instance) {
               callbackInfo.cancel();
            }
         }
      }
   }
}
