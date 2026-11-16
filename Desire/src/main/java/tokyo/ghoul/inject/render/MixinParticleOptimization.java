package tokyo.ghoul.inject.render;

import net.minecraft.client.particle.Particle;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(Particle.class)
public abstract class MixinParticleOptimization {
    @Shadow protected double x;
    @Shadow protected double y;
    @Shadow protected double z;
    @Shadow protected int age;

    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract void setBoundingBox(AABB box);
    @Shadow protected abstract void setLocationFromBoundingbox();

    @Unique private int desire$lightAge = -1;
    @Unique private int desire$lightValue;

    @Inject(method = "move", at = @At("HEAD"), cancellable = true)
    private void desire$simplePhysics(double dx, double dy, double dz, CallbackInfo ci) {
        if (!OptimizationModule.shouldSimplifyParticlePhysics(x, y, z)) return;
        setBoundingBox(getBoundingBox().move(dx, dy, dz));
        setLocationFromBoundingbox();
        ci.cancel();
    }

    @Inject(method = "getLightCoords", at = @At("HEAD"), cancellable = true)
    private void desire$lightCached(float partialTick, CallbackInfoReturnable<Integer> cir) {
        if (OptimizationModule.isParticleLightCache() && desire$lightAge == age) {
            cir.setReturnValue(desire$lightValue);
        }
    }

    @Inject(method = "getLightCoords", at = @At("RETURN"))
    private void desire$lightStore(float partialTick, CallbackInfoReturnable<Integer> cir) {
        desire$lightAge = age;
        desire$lightValue = cir.getReturnValue();
    }
}
