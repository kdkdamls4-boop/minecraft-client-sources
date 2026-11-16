package net.skycore.client.mixin;

import java.util.Map;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.client.sounds.SoundEngine.PlayResult;
import net.skycore.client.module.render.DynamicSurroundingsModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineDynamicSoundTickMixin {
   @Shadow
   @Final
   private Map<SoundInstance, ChannelHandle> instanceToChannel;
   @Shadow
   @Final
   private ChannelAccess channelAccess;

   @Inject(method = "tickInGameSound", at = @At("RETURN"))
   private void skycore$dynamicSoundTick(CallbackInfo callbackInfo) {
      DynamicSurroundingsModule.INSTANCE.onSoundTick(this.instanceToChannel, this.channelAccess);
   }

   @Inject(method = "play", at = @At("RETURN"))
   private void skycore$dynamicSoundPlay(SoundInstance soundInstance, CallbackInfoReturnable<PlayResult> callbackInfoReturnable) {
      DynamicSurroundingsModule.INSTANCE.onSoundStarted(soundInstance, this.instanceToChannel.get(soundInstance));
   }
}
