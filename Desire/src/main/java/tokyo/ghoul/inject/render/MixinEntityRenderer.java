package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;
import tokyo.ghoul.client.features.modules.render.GlowEspModule;
import tokyo.ghoul.client.features.modules.render.nametags.NameTagsModule;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int glowEspOutlineColor(Entity entity, Operation<Integer> original) {
        GlowEspModule glow = GlowEspModule.getInstance();
        return glow.shouldGlow(entity) ? glow.color(entity) : original.call(entity);
    }

    @Inject(method = "getNameTag", at = @At("RETURN"), cancellable = true)
    private void hideVanillaNameTag(T entity, CallbackInfoReturnable<Component> cir) {
        if (entity instanceof Player && NameTagsModule.shouldHideVanillaTag()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",
            at = @At("HEAD"), cancellable = true)
    private void desire$cullFarNameTags(T entity, S state, float partialTick, double nameRange, double scoreRange, CallbackInfo ci) {
        if (!OptimizationModule.shouldCullNameTag(entity)) return;
        state.nameTag = null;
        state.scoreText = null;
        state.nameTagAttachment = null;
        ci.cancel();
    }

    @Inject(method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"), cancellable = true)
    private void renderLabelIfPresent(S state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        if (NameTagsModule.shouldHideVanillaTag()) {
            ci.cancel();
        }
    }
}
