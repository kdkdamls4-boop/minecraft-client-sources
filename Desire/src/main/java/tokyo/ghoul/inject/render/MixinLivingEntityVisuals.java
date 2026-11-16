package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticModelRenderer;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;
import tokyo.ghoul.client.features.modules.render.custommodels.CustomModelsModel;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityVisuals {
    @Shadow
    protected EntityModel model;

    @Unique
    private EntityModel static$originalModel;
    @Unique
    private boolean static$customModelActive;

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"), cancellable = true)
    private void desire$tileItemOnly(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!((Object) this instanceof AvatarRenderer) || !(state instanceof AvatarRenderState avatar)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || avatar.id != minecraft.player.getId()) return;

        Integer tileEntry = CosmeticsGuiPreview.peekBound(state);
        if (tileEntry == null || tileEntry == CosmeticsGuiPreview.PREVIEW) return;

        RenderType layer = CosmeticModelRenderer.layer(tileEntry);
        if (layer != null) {
            collector.submitCustomGeometry(matrices, layer, (pose, consumer) -> {
                PoseStack local = new PoseStack();
                local.last().pose().set(pose.pose());
                local.last().normal().set(pose.normal());
                CosmeticModelRenderer.renderIsolated(tileEntry, local, consumer, 15728880);
            });
        }
        ci.cancel();
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("HEAD"))
    private void swapToCustomModel(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        if (!((Object) this instanceof AvatarRenderer) || !(state instanceof AvatarRenderState avatarState)) return;
        CosmeticsModule feature = CosmeticsModule.getInstance();
        if (!feature.appliesCustomModel(avatarState)) return;
        this.static$originalModel = this.model;
        this.model = CustomModelsModel.create(feature.currentModelType(avatarState));
        this.static$customModelActive = true;
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER,
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"),
            require = 0)
    private void desire$submitCosmetics(LivingEntityRenderState state, PoseStack matrices, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        try {
            if (!((Object) this instanceof AvatarRenderer) || !(state instanceof AvatarRenderState avatar)) return;

            Minecraft minecraft = Minecraft.getInstance();
            CosmeticsModule cosmetics = CosmeticsModule.getInstance();
            if (CosmeticsGuiPreview.forcedModelFor(avatar) != null) {
                return;
            }
            if (cosmetics.customModels.getValue()) {
                return;
            }
            if (!CosmeticsGuiPreview.shouldDrawCosmetics(cosmetics)
                    || minecraft.player == null
                    || avatar.id != minecraft.player.getId()) {
                return;
            }

            CosmeticModelRenderer.AttachmentPoses poses;
            if (this.model instanceof HumanoidModel<?> humanoid) {
                ((HumanoidModel) humanoid).setupAnim(avatar);
                poses = CosmeticModelRenderer.AttachmentPoses.of(humanoid);
            } else if (this.model instanceof CustomModelsModel custom) {
                ((EntityModel) custom).setupAnim(avatar);
                poses = custom.attachmentPoses();
            } else {
                return;
            }

            for (int entry : CosmeticsGuiPreview.resolveEntries(cosmetics)) {
                if (entry < 0) continue;
                RenderType layer = CosmeticModelRenderer.layer(entry);
                if (layer == null) continue;
                final int entryIndex = entry;
                collector.submitCustomGeometry(matrices, layer, (pose, consumer) -> {
                    PoseStack local = new PoseStack();
                    local.last().pose().set(pose.pose());
                    local.last().normal().set(pose.normal());
                    CosmeticModelRenderer.render(entryIndex, poses, local, consumer, 15728880,
                            avatar.walkAnimationPos, avatar.walkAnimationSpeed,
                            avatar.xRot, avatar.yRot - avatar.bodyRot);
                });
                RenderType outline = avatar.outlineColor != 0 ? CosmeticModelRenderer.outlineLayer(entryIndex) : null;
                if (outline != null) {
                    final int outlineColor = avatar.outlineColor;
                    collector.submitCustomGeometry(matrices, outline, (pose, consumer) -> {
                        PoseStack local = new PoseStack();
                        local.last().pose().set(pose.pose());
                        local.last().normal().set(pose.normal());
                        CosmeticModelRenderer.renderOutline(entryIndex, poses, local, consumer, outlineColor,
                                avatar.walkAnimationPos, avatar.walkAnimationSpeed,
                                avatar.xRot, avatar.yRot - avatar.bodyRot);
                    });
                }
            }
        } finally {
            restoreCustomModel();
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V", at = @At("TAIL"))
    private void restoreModelAtEnd(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        restoreCustomModel();
    }

    @Unique
    private void restoreCustomModel() {
        if (this.static$customModelActive) {
            this.model = this.static$originalModel;
            this.static$customModelActive = false;
            this.static$originalModel = null;
        }
    }
}
