package tokyo.ghoul.client.features.modules.render.cosmetics.imported;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticModelRenderer.AttachmentPoses;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticModelRenderer.BonePose;

public final class ImportedCosmeticRenderer {
    private static final float ARM_OFFSET = 0.625F;
    private static final float LEG_OFFSET = 0.75F;
    private static final long ANIMATION_CYCLE_NANOS = 60_000_000_000L;

    private ImportedCosmeticRenderer() {
    }

    public static RenderType layer(ImportedCosmetics.Imported cosmetic) {
        return RenderTypes.entityTranslucent(cosmetic.texture());
    }

    public static RenderType outlineLayer(ImportedCosmetics.Imported cosmetic) {
        return RenderTypes.outline(cosmetic.texture());
    }

    public static void render(ImportedCosmetics.Imported cosmetic, AttachmentPoses poses, PoseStack stack,
                              VertexConsumer consumer, int light, int color) {
        ImportedModel model = cosmetic.model();
        if (model == null) return;
        switch (cosmetic.attach()) {
            case HEAD -> upright(cosmetic, model, poses.head(), stack, consumer, light, color);
            case BODY -> upright(cosmetic, model, poses.body(), stack, consumer, light, color);
            case PAWS -> {
                limb(cosmetic, model, poses.rightArm(), ARM_OFFSET, false, stack, consumer, light, color);
                limb(cosmetic, model, poses.leftArm(), ARM_OFFSET, false, stack, consumer, light, color);
                limb(cosmetic, model, poses.rightLeg(), LEG_OFFSET, true, stack, consumer, light, color);
                limb(cosmetic, model, poses.leftLeg(), LEG_OFFSET, true, stack, consumer, light, color);
            }
            case STOCKINGS -> {
                limb(cosmetic, model, poses.rightLeg(), 0.0F, false, stack, consumer, light, color);
                limb(cosmetic, model, poses.leftLeg(), 0.0F, false, stack, consumer, light, color);
            }
        }
    }

    public static void renderIsolated(ImportedCosmetics.Imported cosmetic, PoseStack stack, VertexConsumer consumer, int light) {
        ImportedModel model = cosmetic.model();
        if (model == null) return;
        ImportedModel.Bounds bounds = model.bounds();
        float fit = 0.95F / Math.max(bounds.maxExtent(), 0.05F);
        float spin = (float) ((System.nanoTime() / 1_000_000_000.0) * 72.0 % 360.0);

        stack.pushPose();
        stack.translate(0.0F, 1.0F, 0.0F);
        stack.mulPose(Axis.YP.rotationDegrees(spin));
        stack.mulPose(Axis.XP.rotationDegrees(-10.0F));
        stack.scale(fit, fit, fit);
        if (cosmetic.attach().limb()) stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        stack.translate(-bounds.centerX(), -bounds.centerY(), -bounds.centerZ());
        model.render(stack, consumer, light, -1, animation(cosmetic), seconds());
        stack.popPose();
    }

    private static void upright(ImportedCosmetics.Imported cosmetic, ImportedModel model, BonePose part,
                                PoseStack stack, VertexConsumer consumer, int light, int color) {
        stack.pushPose();
        part.apply(stack);
        stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        stack.scale(cosmetic.scale(), cosmetic.scale(), cosmetic.scale());
        stack.translate(cosmetic.x(), cosmetic.y(), cosmetic.z());
        stack.translate(0.5F, 0.51F, 0.5F);
        model.render(stack, consumer, light, color, animation(cosmetic), seconds());
        stack.popPose();
    }

    private static void limb(ImportedCosmetics.Imported cosmetic, ImportedModel model, BonePose part, float offset, boolean pointForward,
                             PoseStack stack, VertexConsumer consumer, int light, int color) {
        stack.pushPose();
        part.apply(stack);
        stack.translate(0.0F, offset, 0.0F);
        if (pointForward) stack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        stack.scale(cosmetic.scale(), cosmetic.scale(), cosmetic.scale());
        stack.translate(cosmetic.x(), cosmetic.y(), cosmetic.z());
        model.render(stack, consumer, light, color, animation(cosmetic), seconds());
        stack.popPose();
    }

    private static ImportedAnimation animation(ImportedCosmetics.Imported cosmetic) {
        return cosmetic.animated() ? cosmetic.animation() : null;
    }

    private static float seconds() {
        return (float) (System.nanoTime() % ANIMATION_CYCLE_NANOS) / 1.0E9F;
    }
}
