package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import tokyo.ghoul.api.system.interfaces.IRenderStatePool;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderStatePool {
    @Redirect(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;createRenderState()Lnet/minecraft/client/renderer/entity/state/EntityRenderState;"))
    private EntityRenderState desire$pooledState(EntityRenderer<?, ?> renderer, Entity entity, float partialTick) {
        if (!OptimizationModule.isPoolingEntityStates()) return renderer.createRenderState();
        IRenderStatePool pool = (IRenderStatePool) entity;
        if (pool.desire$pooledState(renderer) instanceof EntityRenderState state) return state;
        EntityRenderState state = renderer.createRenderState();
        pool.desire$poolState(renderer, state);
        return state;
    }
}
