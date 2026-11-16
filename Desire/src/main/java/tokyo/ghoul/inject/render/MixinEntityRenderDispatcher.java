package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private <E extends Entity> void desire$cullFarEntities(E entity, Frustum frustum, double camX, double camY, double camZ,
                                                           CallbackInfoReturnable<Boolean> cir) {
        if (OptimizationModule.shouldCullEntity(entity, camX, camY, camZ)) {
            cir.setReturnValue(false);
        }
    }
}
