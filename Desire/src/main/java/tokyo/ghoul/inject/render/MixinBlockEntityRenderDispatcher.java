package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.api.system.interfaces.IRenderStatePool;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinBlockEntityRenderDispatcher {
    @Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void desire$cullFarBlockEntities(
            E blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumbling, boolean flag,
            CallbackInfoReturnable<S> cir) {
        if (OptimizationModule.shouldCullBlockEntity(blockEntity)) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(method = "tryExtractRenderState", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;createRenderState()Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;"))
    private BlockEntityRenderState desire$pooledState(BlockEntityRenderer<?, ?> renderer, BlockEntity blockEntity,
                                                      float partialTick, ModelFeatureRenderer.CrumblingOverlay crumbling, boolean flag) {
        if (!OptimizationModule.isPoolingBlockEntityStates()) return renderer.createRenderState();
        IRenderStatePool pool = (IRenderStatePool) blockEntity;
        if (pool.desire$pooledState(renderer) instanceof BlockEntityRenderState state) return state;
        BlockEntityRenderState state = renderer.createRenderState();
        pool.desire$poolState(renderer, state);
        return state;
    }
}
