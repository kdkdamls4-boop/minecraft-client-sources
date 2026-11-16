package tokyo.ghoul.client.features.modules.render.pets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.render.pets.PetDogController;

public final class PetDogModel implements QuickImports {
    private final ModelPart head;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart leftBackLeg;
    private final ModelPart rightBackLeg;
    private final ModelPart tail;

    public PetDogModel(ModelPart root) {
        this.head = root.getChild("head");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.frontLeftLeg = root.getChild("front_left_leg");
        this.frontRightLeg = root.getChild("front_right_leg");
        this.leftBackLeg = root.getChild("left_back_leg");
        this.rightBackLeg = root.getChild("right_back_leg");
        this.tail = root.getChild("tail");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        CubeListBuilder headBuilder = CubeListBuilder.create().texOffs(0, 0)
                .addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 4.0F, CubeDeformation.NONE)
                .texOffs(21, 0)
                .addBox(-1.5F, 0.0F, -7.0F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE);
        PartDefinition head = root.addOrReplaceChild("head", headBuilder, PartPose.offset(0.0F, 10.5F, -6.8F));

        CubeListBuilder leftEarBuilder = CubeListBuilder.create().texOffs(32, 4)
                .addBox(0.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(34, 1)
                .addBox(0.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, CubeDeformation.NONE);
        head.addOrReplaceChild("left_ear", leftEarBuilder, PartPose.offset(3.0F, 3.0F, -2.0F));

        CubeListBuilder rightEarBuilder = CubeListBuilder.create().texOffs(32, 4)
                .addBox(-1.0F, -5.0F, -1.5F, 1.0F, 3.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(34, 1)
                .addBox(-1.0F, -5.5F, -0.75F, 1.0F, 1.0F, 1.5F, CubeDeformation.NONE);
        head.addOrReplaceChild("right_ear", rightEarBuilder, PartPose.offset(-3.0F, 3.0F, -2.0F));

        CubeListBuilder neckBuilder = CubeListBuilder.create().texOffs(15, 7)
                .addBox(-2.95F, -1.0F, -4.0F, 5.9F, 5.0F, 6.0F, CubeDeformation.NONE);
        root.addOrReplaceChild("neck", neckBuilder, PartPose.offset(0.0F, 10.5F, -5.0F));

        CubeListBuilder chestBuilder = CubeListBuilder.create().texOffs(32, 13)
                .addBox(-4.0F, -3.5F, -3.0F, 8.0F, 7.0F, 6.0F, CubeDeformation.NONE);
        CubeListBuilder backBuilder = CubeListBuilder.create().texOffs(3, 19)
                .addBox(-3.0F, -3.0F, -0.5F, 6.0F, 6.0F, 11.0F, CubeDeformation.NONE);

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 13.5F, -5.0F));
        body.addOrReplaceChild("chest", chestBuilder, PartPose.offset(0.0F, 0.0F, 3.0F));
        body.addOrReplaceChild("back", backBuilder, PartPose.offset(0.0F, -0.5F, 5.5F));

        CubeListBuilder frontLeftLegBuilder = CubeListBuilder.create().texOffs(42, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE);
        CubeListBuilder frontRightLegBuilder = CubeListBuilder.create().texOffs(42, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE);
        CubeListBuilder leftBackLegBuilder = CubeListBuilder.create().texOffs(52, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE);
        CubeListBuilder rightBackLegBuilder = CubeListBuilder.create().texOffs(52, 0)
                .addBox(-1.0F, 0.0F, -1.0F, 2.0F, 5.0F, 2.0F, CubeDeformation.NONE);

        root.addOrReplaceChild("front_left_leg", frontLeftLegBuilder, PartPose.offset(1.5F, 16.0F, -3.0F));
        root.addOrReplaceChild("front_right_leg", frontRightLegBuilder, PartPose.offset(-1.5F, 16.0F, -3.0F));
        root.addOrReplaceChild("left_back_leg", leftBackLegBuilder, PartPose.offset(1.5F, 16.0F, 9.0F));
        root.addOrReplaceChild("right_back_leg", rightBackLegBuilder, PartPose.offset(-1.5F, 16.0F, 9.0F));

        CubeListBuilder tailBuilder = CubeListBuilder.create().texOffs(2, 12)
                .addBox(-1.0F, 2.0F, -1.0F, 2.0F, 8.0F, 2.0F, CubeDeformation.NONE);
        root.addOrReplaceChild("tail", tailBuilder, PartPose.offset(0.0F, 9.0F, 10.0F));

        return LayerDefinition.create(mesh, 60, 36);
    }

    public void setRotationAngles(float ageInTicks, PetDogController brain) {
        float layProgress = brain.getLayProgress();
        float wakeProgress = brain.getTrickWakeProgress();

        this.neck.xRot = -0.43633232F;
        this.head.yRot = brain.getYaw() * 0.017453292F + (float) Math.sin(ageInTicks * 3.8F) * 0.16F * wakeProgress;
        this.head.xRot = brain.getPitch() * 0.017453292F;

        this.frontLeftLeg.xRot = (float) Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;
        this.frontRightLeg.xRot = (float) Math.cos(brain.limbSwing * 0.6662F + 3.1415927F) * 1.4F * brain.limbSwingAmount;
        this.leftBackLeg.xRot = (float) Math.cos(brain.limbSwing * 0.6662F + 3.1415927F) * 1.4F * brain.limbSwingAmount;
        this.rightBackLeg.xRot = (float) Math.cos(brain.limbSwing * 0.6662F) * 1.4F * brain.limbSwingAmount;

        this.frontLeftLeg.xRot = lerp(this.frontLeftLeg.xRot, (float) Math.toRadians(-90.0D), layProgress);
        this.frontRightLeg.xRot = lerp(this.frontRightLeg.xRot, (float) Math.toRadians(-90.0D), layProgress);
        this.leftBackLeg.xRot = lerp(this.leftBackLeg.xRot, (float) Math.toRadians(90.0D), layProgress);
        this.rightBackLeg.xRot = lerp(this.rightBackLeg.xRot, (float) Math.toRadians(90.0D), layProgress);

        this.frontLeftLeg.yRot = lerp(0.0F, (float) Math.toRadians(-22.0D), layProgress);
        this.frontRightLeg.yRot = lerp(0.0F, (float) Math.toRadians(22.0D), layProgress);
        this.leftBackLeg.yRot = lerp(0.0F, (float) Math.toRadians(22.0D), layProgress);
        this.rightBackLeg.yRot = lerp(0.0F, (float) Math.toRadians(-22.0D), layProgress);

        this.tail.xRot = (float) Math.toRadians(lerp(22.0F, 45.0F, layProgress));
        this.tail.zRot = (float) (Math.toRadians(-22.5D) + Math.toRadians(22.5D) + Math.cos(ageInTicks * 0.15F) * 0.3F);
    }

    public void render(PoseStack matrices, VertexConsumer consumer, Identifier texture, PetDogController brain, float ageInTicks) {
        int light = 15728880;
        int overlay = OverlayTexture.NO_OVERLAY;

        matrices.pushPose();
        matrices.translate(0.0D, 1.2F - brain.getLayProgress() * 0.3F, 0.0D);
        matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(brain.getBody()));

        this.head.render(matrices, consumer, light, overlay, -1);
        this.neck.render(matrices, consumer, light, overlay, -1);
        this.body.render(matrices, consumer, light, overlay, -1);
        this.frontLeftLeg.render(matrices, consumer, light, overlay, -1);
        this.frontRightLeg.render(matrices, consumer, light, overlay, -1);
        this.leftBackLeg.render(matrices, consumer, light, overlay, -1);
        this.rightBackLeg.render(matrices, consumer, light, overlay, -1);
        this.tail.render(matrices, consumer, light, overlay, -1);

        matrices.popPose();
    }

    public void renderHat(PoseStack matrices, VertexConsumer consumer, Identifier texture, PetDogController brain, PropellerHatModel hat, float ageInTicks) {
        int light = 15728880;

        matrices.pushPose();
        matrices.translate(0.0D, 1.2F - brain.getLayProgress() * 0.3F, 0.0D);
        matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(brain.getBody()));

        this.head.translateAndRotate(matrices);
        matrices.translate(0.0F, 0.0F, -0.09F);
        matrices.scale(0.55F, 0.55F, 0.55F);

        hat.render(matrices, consumer, texture, light, ageInTicks, brain.isFlying());

        matrices.popPose();
    }

    private static float lerp(float from, float to, float delta) { return from + (to - from) * delta; }
}
