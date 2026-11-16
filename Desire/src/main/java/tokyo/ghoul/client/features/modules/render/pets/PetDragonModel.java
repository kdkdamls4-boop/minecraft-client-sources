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
import tokyo.ghoul.client.features.modules.render.pets.PetDragonController;

public final class PetDragonModel implements QuickImports {
    private static final CubeDeformation THIN_PLANE = new CubeDeformation(0.015F);

    private final ModelPart dragon;
    private final ModelPart torso;
    private final ModelPart waist;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftRearLeg;
    private final ModelPart rightRearLeg;
    private final ModelPart segment1;
    private final ModelPart segment2;
    private final ModelPart segment3;
    private final ModelPart segment4;

    public PetDragonModel(ModelPart root) {
        this.dragon = root.getChild("dragon");
        this.torso = this.dragon.getChild("torso");
        this.waist = this.torso.getChild("waist");
        ModelPart tail = this.waist.getChild("tail");
        this.segment1 = tail.getChild("segment_1");
        this.segment2 = this.segment1.getChild("segment_2");
        this.segment3 = this.segment2.getChild("segment_3");
        this.segment4 = this.segment3.getChild("segment_4");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftWing = this.torso.getChild("left_wing");
        this.rightWing = this.torso.getChild("right_wing");
        this.leftRearLeg = this.waist.getChild("left_rearleg");
        this.rightRearLeg = this.waist.getChild("right_rearleg");
        this.leftForearm = this.torso.getChild("left_forearm");
        this.rightForearm = this.torso.getChild("right_forearm");
    }

    public void setRotationAngles(float ageInTicks, PetDragonController brain) {
        this.head.yRot = 0.0F;
        this.head.xRot = (float) Math.toRadians(27.5D);
        this.neck.xRot = (float) Math.toRadians(12.5D);
        this.torso.xRot = (float) Math.toRadians(-30.0D);
        this.waist.xRot = (float) Math.toRadians(-20.0D);

        if (!brain.isPlayerMoving()) {
            float idle = ageInTicks * 0.05F;
            this.head.yRot += (float) Math.sin(idle * 0.8F) * 0.1F;
            this.head.xRot += (float) Math.sin(idle * 0.6F) * 0.1F;
            this.torso.zRot = (float) Math.sin(idle * 0.4F) * 0.1F;
            float sway = (float) Math.sin(idle * 1.2F) * 0.6F;
            this.setTailAngles(sway, 1.0F);
            float flapStrength = (float) Math.sin(ageInTicks * 0.4F) * 1.05F;
            this.setWingAngles(flapStrength, 0.9F);
            this.leftRearLeg.xRot = (float) Math.toRadians(20.0D + Math.sin(idle * 0.5F) * 5.0D);
            this.rightRearLeg.xRot = (float) Math.toRadians(20.0D - Math.sin(idle * 0.5F) * 5.0D);
            this.leftForearm.xRot = (float) Math.toRadians(15.0D + Math.sin(idle * 0.4F) * 8.0D);
            this.rightForearm.xRot = (float) Math.toRadians(15.0D - Math.sin(idle * 0.4F) * 8.0D);
        } else {
            float sway = (float) Math.sin(ageInTicks * 0.2F) * 0.4F;
            this.setTailAngles(sway, 0.7F);
            float flapStrength = (float) Math.sin(ageInTicks * 0.4F) * 1.2F;
            this.setWingAngles(flapStrength, 1.0F);
            float stride = (float) Math.sin(ageInTicks * 0.1F) * 0.2F;
            this.leftRearLeg.xRot = (float) Math.toRadians(37.5D + stride * 10.0D);
            this.rightRearLeg.xRot = (float) Math.toRadians(37.5D - stride * 10.0D);
            this.leftForearm.xRot = (float) Math.toRadians(30.0D + stride * 15.0D);
            this.rightForearm.xRot = (float) Math.toRadians(30.0D - stride * 15.0D);
        }
        this.jaw.xRot = (float) Math.toRadians(2.5D + Math.sin(ageInTicks * 0.3F) * 2.0D);
    }

    public void render(PoseStack matrices, VertexConsumer consumer, Identifier texture, PetDragonController brain, float ageInTicks) {
        int overlay = OverlayTexture.NO_OVERLAY;
        float hover = (float) Math.sin(ageInTicks * 0.2F) * 0.3F;
        matrices.pushPose();
        matrices.translate(0.0D, 0.8F + hover, 0.0D);
        matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
        matrices.mulPose(Axis.YP.rotationDegrees(brain.getBody()));
        matrices.scale(0.5F, 0.5F, 0.5F);
        this.dragon.render(matrices, consumer, 15728880, overlay, -1);
        matrices.popPose();
    }

    private void setTailAngles(float sway, float strength) {
        this.segment1.xRot = (float) Math.toRadians(-15.0D);
        this.segment1.yRot = sway;
        this.segment2.xRot = (float) Math.toRadians(-10.0D);
        this.segment2.yRot = sway * 0.8F * strength;
        this.segment3.xRot = (float) Math.toRadians(-7.5D);
        this.segment3.yRot = sway * 0.6F * strength;
        this.segment4.xRot = (float) Math.toRadians(-5.0D);
        this.segment4.yRot = sway * 0.4F * strength;
    }

    private void setWingAngles(float strength, float flap) {
        this.leftWing.xRot = (float) Math.toRadians(5.0D + strength * 10.0D * flap);
        this.leftWing.yRot = (float) Math.toRadians(-15.0D + strength * 5.0D * flap);
        this.leftWing.zRot = (float) Math.toRadians(45.0D + strength * 60.0D * flap);
        this.rightWing.xRot = (float) Math.toRadians(5.0D + strength * 10.0D * flap);
        this.rightWing.yRot = (float) Math.toRadians(15.0D - strength * 5.0D * flap);
        this.rightWing.zRot = (float) Math.toRadians(-45.0D - strength * 60.0D * flap);
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition dragon = root.addOrReplaceChild("dragon", CubeListBuilder.create(), PartPose.offset(0.0F, 17.0F, 0.0F));
        PartDefinition torso = dragon.addOrReplaceChild("torso", CubeListBuilder.create()
                        .texOffs(45, 83).addBox(0.0F, -6.5F, -2.25F, 0.0F, 3.0F, 4.0F, THIN_PLANE)
                        .texOffs(0, 35).addBox(-3.5F, -3.5F, -4.0F, 7.0F, 7.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -3.5F, -1.0F));
        PartDefinition waist = torso.addOrReplaceChild("waist", CubeListBuilder.create()
                        .texOffs(60, 35).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(26, 84).addBox(0.0F, -5.0F, 0.75F, 0.0F, 2.0F, 4.0F, THIN_PLANE),
                PartPose.offset(0.0F, 0.0F, 3.0F));
        PartDefinition tail = waist.addOrReplaceChild("tail", CubeListBuilder.create(), PartPose.offset(0.0F, 0.5F, 6.025F));
        PartDefinition segment1 = tail.addOrReplaceChild("segment_1", CubeListBuilder.create()
                        .texOffs(0, 65).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(59, 85).addBox(0.0F, -4.5F, 0.75F, 0.0F, 2.0F, 3.0F, THIN_PLANE),
                PartPose.offset(0.0F, 0.0F, -0.025F));
        PartDefinition segment2 = segment1.addOrReplaceChild("segment_2", CubeListBuilder.create()
                        .texOffs(42, 65).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(66, 85).addBox(0.0F, -4.0F, 0.5F, 0.0F, 2.0F, 3.0F, THIN_PLANE),
                PartPose.offset(0.0F, 0.0F, 5.0F));
        PartDefinition segment3 = segment2.addOrReplaceChild("segment_3", CubeListBuilder.create()
                        .texOffs(21, 76).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(73, 85).addBox(0.0F, -3.5F, 0.5F, 0.0F, 2.0F, 3.0F, THIN_PLANE),
                PartPose.offset(0.0F, 0.0F, 4.0F));
        segment3.addOrReplaceChild("segment_4", CubeListBuilder.create().texOffs(59, 78).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(0.0F, 0.0F, 4.0F));
        PartDefinition neck = torso.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(42, 74).addBox(-2.0F, -2.0F, -4.0F, 4.0F, 4.0F, 4.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -0.5F, -4.0F));
        PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(60, 48).addBox(-3.0F, -2.0F, -9.0F, 0.0F, 6.0F, 9.0F, THIN_PLANE)
                        .texOffs(62, 0).addBox(3.0F, -2.0F, -9.0F, 0.0F, 6.0F, 9.0F, THIN_PLANE)
                        .texOffs(72, 78).addBox(-3.0F, -2.0F, -9.0F, 6.0F, 6.0F, 0.0F, THIN_PLANE)
                        .texOffs(29, 35).addBox(-3.0F, -2.02F, -9.0F, 6.0F, 0.08F, 9.0F, CubeDeformation.NONE)
                        .texOffs(29, 45).addBox(-3.0F, 4.0F, -9.0F, 6.0F, 0.0F, 9.0F, THIN_PLANE)
                        .texOffs(0, 55).addBox(-3.0F, 3.94F, -9.0F, 6.0F, 0.08F, 9.0F, CubeDeformation.NONE)
                        .texOffs(0, 50).addBox(-2.75F, -0.65F, -9.04F, 5.5F, 2.5F, 0.08F, CubeDeformation.NONE)
                        .texOffs(79, 48).addBox(-3.0F, -2.0F, 0.0F, 6.0F, 6.0F, 0.0F, THIN_PLANE),
                PartPose.offset(0.0F, 0.0F, -4.0F));
        head.addOrReplaceChild("jaw", CubeListBuilder.create()
                        .texOffs(31, 55).addBox(-2.5F, 1.95F, -8.48F, 5.0F, 0.0F, 9.0F, THIN_PLANE)
                        .texOffs(0, 50).addBox(-2.5F, 1.9F, -8.52F, 5.0F, 0.1F, 9.0F, CubeDeformation.NONE)
                        .texOffs(11, 50).addBox(-2.5F, -0.3F, -8.52F, 5.0F, 2.2F, 0.08F, CubeDeformation.NONE)
                        .texOffs(62, 16).addBox(-2.5F, -2.05F, -8.48F, 0.0F, 4.0F, 9.0F, THIN_PLANE)
                        .texOffs(60, 64).addBox(2.5F, -2.05F, -8.48F, 0.0F, 4.0F, 9.0F, THIN_PLANE),
                PartPose.offsetAndRotation(0.0F, 3.4F, -0.75F, -0.0873F, 0.0F, 0.0F));
        waist.addOrReplaceChild("left_rearleg", legBuilder(62, 30, 81, 8), PartPose.offset(3.5F, 1.7F, 5.75F));
        waist.addOrReplaceChild("right_rearleg", legBuilder(71, 30, 81, 15), PartPose.offset(-3.5F, 1.7F, 5.75F));
        torso.addOrReplaceChild("left_forearm", forearmBuilder(79, 62, 81, 0), PartPose.offset(5.0F, 1.0F, -0.25F));
        torso.addOrReplaceChild("right_forearm", forearmBuilder(79, 55, 79, 69), PartPose.offset(-5.0F, 1.0F, -0.25F));
        torso.addOrReplaceChild("left_wing", CubeListBuilder.create()
                        .texOffs(85, 30).addBox(-0.875F, -3.0F, -0.975F, 2.0F, 4.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(54, 83).addBox(-0.875F, -13.0F, -0.975F, 1.0F, 10.0F, 1.0F, CubeDeformation.NONE)
                        .texOffs(21, 65).addBox(-0.875F, -1.0F, 1.025F, 1.0F, 1.0F, 9.0F, CubeDeformation.NONE)
                        .texOffs(0, 0).addBox(-0.375F, -19.0F, -0.975F, 0.0F, 19.0F, 15.0F, THIN_PLANE),
                PartPose.offset(3.375F, 0.0F, -2.025F));
        torso.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(85, 37).addBox(-1.375F, -3.0F, -0.975F, 2.0F, 4.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(21, 84).addBox(-0.375F, -13.0F, -0.975F, 1.0F, 10.0F, 1.0F, CubeDeformation.NONE)
                        .texOffs(0, 76).addBox(-0.375F, -1.0F, 1.025F, 1.0F, 1.0F, 9.0F, CubeDeformation.NONE)
                        .texOffs(31, 0).addBox(0.125F, -19.0F, -0.975F, 0.0F, 19.0F, 15.0F, THIN_PLANE),
                PartPose.offset(-3.125F, 0.0F, -2.025F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    private static CubeListBuilder legBuilder(int jointU, int jointV, int limbU, int limbV) {
        return CubeListBuilder.create()
                .texOffs(jointU, jointV).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(limbU, limbV).addBox(-1.5F, 1.0F, -1.0F, 3.0F, 4.0F, 2.0F, CubeDeformation.NONE)
                .texOffs(22, 50).addBox(-1.2F, 4.5F, -1.8F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(36, 76).addBox(-0.5F, 4.5F, -2.0F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(80, 30).addBox(0.2F, 4.5F, -1.8F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE);
    }

    private static CubeListBuilder forearmBuilder(int jointU, int jointV, int limbU, int limbV) {
        return CubeListBuilder.create()
                .texOffs(jointU, jointV).addBox(-1.5F, -1.5F, -1.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(limbU, limbV).addBox(-1.0F, 1.0F, -1.5F, 2.0F, 4.0F, 3.0F, CubeDeformation.NONE)
                .texOffs(5, 87).addBox(-1.2F, 4.5F, -2.1F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(10, 87).addBox(-0.5F, 4.5F, -2.3F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                .texOffs(15, 87).addBox(0.2F, 4.5F, -2.1F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE);
    }
}
