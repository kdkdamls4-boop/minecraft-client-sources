package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractorOptimization {
    @Inject(method = "extractEntity", at = @At("HEAD"))
    private void desire$beginWorldEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        OptimizationModule.setWorldEntityExtract(true);
    }

    @Inject(method = "extractEntity", at = @At("RETURN"))
    private void desire$endWorldEntity(Entity entity, float partialTick, CallbackInfoReturnable<EntityRenderState> cir) {
        OptimizationModule.setWorldEntityExtract(false);
    }
}
