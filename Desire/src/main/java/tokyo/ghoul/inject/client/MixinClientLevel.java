package tokyo.ghoul.inject.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {
    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void hideSeededEntitySound(Entity except, Entity sourceEntity, Holder<SoundEvent> sound,
                                       SoundSource source, float volume, float pitch, long seed, CallbackInfo ci) {
        if (sound != null && RemovalsModule.shouldRemoveSound(sound.value())) {
            ci.cancel();
        }
    }

    @Inject(method = "playSeededSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FFJ)V",
            at = @At("HEAD"), cancellable = true)
    private void hideSeededPosSound(Entity except, double x, double y, double z, Holder<SoundEvent> sound,
                                    SoundSource source, float volume, float pitch, long seed, CallbackInfo ci) {
        if (sound != null && RemovalsModule.shouldRemoveSound(sound.value())) {
            ci.cancel();
        }
    }

    @Inject(method = "playLocalSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void hideLocalEntitySound(Entity sourceEntity, SoundEvent sound, SoundSource source,
                                      float volume, float pitch, CallbackInfo ci) {
        if (RemovalsModule.shouldRemoveSound(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playPlayerSound", at = @At("HEAD"), cancellable = true)
    private void hidePlayerSound(SoundEvent sound, SoundSource source, float volume, float pitch, CallbackInfo ci) {
        if (RemovalsModule.shouldRemoveSound(sound)) {
            ci.cancel();
        }
    }

    @Inject(method = "playLocalSound(DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
            at = @At("HEAD"), cancellable = true)
    private void hideLocalPosSound(double x, double y, double z, SoundEvent sound, SoundSource source,
                                   float volume, float pitch, boolean distanceDelay, CallbackInfo ci) {
        if (RemovalsModule.shouldRemoveSound(sound)) {
            ci.cancel();
        }
    }

}
