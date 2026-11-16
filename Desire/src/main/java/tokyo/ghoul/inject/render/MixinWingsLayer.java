package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WingsLayer.class)
public abstract class MixinWingsLayer {
    @Inject(method = "getPlayerElytraTexture", at = @At("HEAD"), cancellable = true)
    private static void desire$keepDefaultElytraTexture(HumanoidRenderState state, CallbackInfoReturnable<Identifier> cir) {
        cir.setReturnValue(null);
    }
}
