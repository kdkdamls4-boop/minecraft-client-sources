package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityOptimization {
    @ModifyVariable(method = "submitMultipleFromCount", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static int desire$singleCopy(int count) {
        return OptimizationModule.isSingleItemCopy() ? 1 : count;
    }
}
