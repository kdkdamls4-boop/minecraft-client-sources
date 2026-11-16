package tokyo.ghoul.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.render.EntityColorEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;
import tokyo.ghoul.client.features.modules.movement.AirStuckModule;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
    @Shadow
    @Nullable
    protected abstract RenderType getRenderType(S state, boolean showBody, boolean translucent, boolean showOutline);

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float updateVisalPitch(float original, T entity, S state, float tickDelta) {
        if (entity != SharedClass.player()) {
            return original;
        }

        RotationManager rotationManager = RotationManager.getInstance();
        Rotation rotation = rotationManager.getRotation();
        Rotation rotationPrev = rotationManager.getPreviousRotation();
        RotationPlan currentRotationPlan = rotationManager.getCurrentRotationPlan();

        if (currentRotationPlan == null) {
            return original;
        }

        return Mth.rotLerp(tickDelta, rotationPrev.getXRot(), rotation.getXRot());
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("TAIL"))
    private void airStuckRenderState(T entity, S state, float tickDelta, CallbackInfo ci) {
        AirStuckModule airStuck = AirStuckModule.getInstance();
        if (entity == SharedClass.player() && airStuck.isEnabled() && airStuck.isStateSaved()) {
            state.xRot = airStuck.getSavedPitch();
            state.bodyRot = airStuck.getSavedBodyYaw();
            state.yRot = Mth.wrapDegrees(airStuck.getSavedYaw() - airStuck.getSavedBodyYaw());
        }
    }

    @Redirect(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private RenderType renderHook(LivingEntityRenderer<T, S, M> instance, S state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.boundingBoxWidth == 0.6F) {
            int defaultColor = -1;
            EntityColorEvent.EntityColorEventData eventData = new EntityColorEvent.EntityColorEventData(defaultColor);
            boolean modified = EntityColorEvent.getInstance().call(eventData);
            if (modified && eventData.color() != defaultColor) {
                translucent = true;
            }
        }

        return this.getRenderType(state, showBody, translucent, showOutline);
    }

    @ModifyArg(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"), index = 6)
    private int renderModelHook(int color, @Local(argsOnly = true) S state) {
        EntityColorEvent.EntityColorEventData event = new EntityColorEvent.EntityColorEventData(color);
        if (state.isInvisibleToPlayer) EntityColorEvent.getInstance().call(event);
        return event.color();
    }
}
