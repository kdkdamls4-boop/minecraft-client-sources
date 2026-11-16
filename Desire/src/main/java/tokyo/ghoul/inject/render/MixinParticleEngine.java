package tokyo.ghoul.inject.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

import java.util.Map;
import java.util.Queue;

@Mixin(ParticleEngine.class)
public abstract class MixinParticleEngine {
    @Shadow @Final private Map<ParticleRenderType, ParticleGroup<?>> particles;
    @Shadow @Final private Queue<Particle> particlesToAdd;

    @Unique private int desire$particleCount;

    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void hideParticles(ParticleOptions options, double x, double y, double z,
                               double xa, double ya, double za, CallbackInfoReturnable<Particle> cir) {
        if (BotContext.isBackgroundContextActive()) {
            cir.setReturnValue(null);
            return;
        }
        if (options == null || options.getType() == ParticleTypes.FIREWORK || options.getType() == ParticleTypes.FLASH) {
            return;
        }
        if (OptimizationModule.shouldCullParticle(x, y, z)) {
            cir.setReturnValue(null);
            return;
        }
        if (OptimizationModule.shouldThinParticle(x, y, z)) {
            cir.setReturnValue(null);
            return;
        }
        if (RemovalsModule.shouldRemoveParticle(options)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = {"add", "createTrackingEmitter"}, at = @At("HEAD"), cancellable = true)
    private void hideBotParticles(CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void desire$particleLimit(Particle particle, CallbackInfo ci) {
        int limit = OptimizationModule.maxParticles();
        if (limit <= 0) return;
        if (desire$particleCount >= limit) {
            ci.cancel();
            return;
        }
        desire$particleCount++;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void desire$countParticles(CallbackInfo ci) {
        int count = particlesToAdd.size();
        for (ParticleGroup<?> group : particles.values()) count += group.size();
        desire$particleCount = count;
    }

    @Inject(method = {"clearParticles", "setLevel"}, at = @At("TAIL"))
    private void desire$resetParticleCount(CallbackInfo ci) {
        desire$particleCount = 0;
    }
}
