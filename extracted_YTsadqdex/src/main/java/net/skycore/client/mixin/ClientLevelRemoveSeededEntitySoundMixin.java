package net.skycore.client.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.skycore.client.module.render.NoRenderModule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelRemoveSeededEntitySoundMixin {
   @Inject(
      method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void skycore$removeSeededEntitySound(
      Entity entity, Entity entity2, Holder<SoundEvent> holder, SoundSource soundSource, float value, float value2, long timestamp, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.shouldRemoveSound((SoundEvent)holder.value())) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void skycore$removeSeededPositionedSound(
      Entity entity, double value, double value2, double value3, Holder<SoundEvent> holder, SoundSource soundSource, float value4, float value5, long timestamp, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.shouldRemoveSound((SoundEvent)holder.value())) {
         callbackInfo.cancel();
      }
   }

   @Inject(
      method = "playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
      at = @At("HEAD"),
      cancellable = true
   )
   private void skycore$removeLocalEntitySound(Entity entity, SoundEvent soundEvent, SoundSource soundSource, float value, float value2, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveSound(soundEvent)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "playPlayerSound", at = @At("HEAD"), cancellable = true)
   private void skycore$removePlayerSound(SoundEvent soundEvent, SoundSource soundSource, float value, float value2, CallbackInfo callbackInfo) {
      if (NoRenderModule.shouldRemoveSound(soundEvent)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V", at = @At("HEAD"), cancellable = true)
   private void skycore$removeLocalPositionedSound(
      double value, double value2, double value3, SoundEvent soundEvent, SoundSource soundSource, float value4, float value5, boolean condition, CallbackInfo callbackInfo
   ) {
      if (NoRenderModule.shouldRemoveSound(soundEvent)) {
         callbackInfo.cancel();
      }
   }
}
