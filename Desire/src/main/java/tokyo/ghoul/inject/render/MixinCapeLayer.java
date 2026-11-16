package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.render.AutoCape;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;

@Mixin(CapeLayer.class)
public abstract class MixinCapeLayer {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void desire$hideCapeOnElytra(PoseStack pose, SubmitNodeCollector collector, int light,
                                         AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (AutoCape.hidesCapeForChest(state.chestEquipment, state.isFallFlying)
                || CosmeticsModule.getInstance().appliesCustomModel(state)) {
            ci.cancel();
        }
    }
}
