package tokyo.ghoul.client.features.modules.render.custommodels;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

final class CustomModelsMeshes {
    private CustomModelsMeshes() {}

    static CustomModelsModel crazyRabbit() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition bone = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(28, 45).addBox(-5.0F, -13.0F, -5.0F, 10.0F, 11.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        bone.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-3.0F, -2.0F, -1.0F));
        bone.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(3.0F, -2.0F, -1.0F));
        bone.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(0.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, -13.0F, -1.0F, 0.0F, 0.0F, -0.0873F));
        bone.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, 0.0F, -2.0F, 2.0F, 8.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, -13.0F, -1.0F, 0.0F, 0.0F, 0.0873F));
        bone.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.0F, 0.0F, -4.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(56, 0).addBox(-5.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(56, 0).mirror().addBox(3.0F, -9.0F, -5.0F, 2.0F, 3.0F, 2.0F, CubeDeformation.NONE).mirror(false)
                        .texOffs(0, 45).addBox(-4.0F, -11.0F, -4.0F, 8.0F, 11.0F, 8.0F, CubeDeformation.NONE)
                        .texOffs(46, 0).addBox(1.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F, CubeDeformation.NONE)
                        .texOffs(46, 0).addBox(-4.0F, -20.0F, 0.0F, 3.0F, 9.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -14.0F, -1.0F));

        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        ModelPart body = baked.getChild("body");
        return CustomModelsModel.fromParts(baked,
                body.getChild("head"), body,
                body.getChild("left_arm"), body.getChild("right_arm"),
                body.getChild("left_leg"), body.getChild("right_leg"),
                false, 1.25F, 1.25F, 1.25F, body, 24.0F - 4.8F, 24.0F - 2.0F);
    }

    static CustomModelsModel demon() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -4.0F, -3.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.3F)),
                PartPose.offset(0.0F, -6.0F, -1.0F));
        head.addOrReplaceChild("left_horn", CubeListBuilder.create()
                        .texOffs(32, 8).addBox(13.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 0).addBox(17.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(-8.0F, 8.0F, 0.0F, -0.3927F, 0.3927F, -0.5236F));
        head.addOrReplaceChild("right_horn", CubeListBuilder.create()
                        .texOffs(32, 8).mirror().addBox(-19.4346F, -5.2071F, 2.7071F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.1F))
                        .texOffs(0, 0).mirror().addBox(-19.4346F, -10.4071F, 2.7071F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.offsetAndRotation(8.0F, 8.0F, 0.0F, -0.3927F, -0.3927F, 0.5236F));

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 16).addBox(-4.5F, -1.7028F, 1.4696F, 8.0F, 12.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.5F, -0.1F, -3.5F, 0.1745F, 0.0F, 0.0F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create()
                        .texOffs(40, 12).addBox(-7.0072F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.01F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(8.25F, -2.0F, 10.0F, 0.0873F, -0.829F, 0.1745F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create()
                        .texOffs(40, 12).mirror().addBox(-4.9928F, -0.5972F, 0.7515F, 12.0F, 13.0F, 0.01F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-9.25F, -2.0F, 10.0F, 0.0873F, 0.829F, -0.1745F));

        root.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(24, 16).addBox(-1.1F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.4F, -1.25F, -2.0F, 0.0F, 0.0F, -0.2182F));
        root.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(24, 16).mirror().addBox(-2.9F, -1.05F, 0.0F, 4.0F, 14.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.4F, -1.25F, -2.0F, 0.0F, 0.0F, 0.2182F));

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(48, 22).addBox(-3.25F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(3.0F, 10.0F, 0.0F));
        PartDefinition leftLower = leftLeg.addOrReplaceChild("lower", CubeListBuilder.create()
                        .texOffs(34, 34).addBox(0.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));
        leftLower.addOrReplaceChild("foot", CubeListBuilder.create()
                        .texOffs(26, 0).addBox(-0.7F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(40, 0).addBox(-0.7F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));
        PartDefinition leftSide = leftLower.addOrReplaceChild("side", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-1.0F, 0.0F, -2.0F, 0.0F, -0.0873F, -0.2618F));
        leftSide.addOrReplaceChild("side_detail", CubeListBuilder.create()
                        .texOffs(16, 34).addBox(-0.7911F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(0, 32).addBox(-0.7911F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(1.9F, 12.0F, 0.25F));

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(48, 22).mirror().addBox(-0.75F, -2.25F, -1.0F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-3.0F, 10.0F, 0.0F));
        PartDefinition rightLower = rightLeg.addOrReplaceChild("lower", CubeListBuilder.create()
                        .texOffs(34, 34).mirror().addBox(-3.95F, 4.6F, 8.0511F, 3.0F, 5.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.7F, -0.1F, -3.55F, -0.5236F, 0.0F, 0.0F));
        rightLower.addOrReplaceChild("foot", CubeListBuilder.create()
                        .texOffs(26, 0).mirror().addBox(-3.3F, -1.15F, 9.3F, 4.0F, 2.0F, 4.0F, CubeDeformation.NONE)
                        .texOffs(40, 0).mirror().addBox(-3.3F, -1.15F, 7.3F, 4.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.4F, 15.0F, 0.25F, 0.5236F, 0.0F, 0.0F));
        PartDefinition rightSide = rightLower.addOrReplaceChild("side", CubeListBuilder.create(),
                PartPose.offsetAndRotation(1.0F, 0.0F, -2.0F, 0.0F, 0.0873F, 0.2618F));
        rightSide.addOrReplaceChild("side_detail", CubeListBuilder.create()
                        .texOffs(16, 34).mirror().addBox(-3.2089F, -10.1159F, 8.0029F, 4.0F, 4.0F, 5.0F, CubeDeformation.NONE)
                        .texOffs(0, 32).mirror().addBox(-3.2089F, -15.1159F, 4.0029F, 4.0F, 9.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(-1.9F, 12.0F, 0.25F));

        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        return CustomModelsModel.fromParts(baked,
                baked.getChild("head"), baked.getChild("body"),
                baked.getChild("left_arm"), baked.getChild("right_arm"),
                baked.getChild("left_leg"), baked.getChild("right_leg"),
                false, 1.0F, 1.0F, 1.0F, null, 0.0F, 0.0F);
    }

    static CustomModelsModel amogus() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(34, 8).addBox(-4.0F, 6.0F, -3.0F, 8.0F, 12.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(15, 10).addBox(-3.0F, 9.0F, 3.0F, 6.0F, 8.0F, 3.0F, CubeDeformation.NONE)
                        .texOffs(26, 0).addBox(-3.0F, 5.0F, -3.0F, 6.0F, 1.0F, 6.0F, CubeDeformation.NONE)
                        .texOffs(0, 10).addBox(-3.0F, 7.0F, -4.0F, 6.0F, 4.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        body.addOrReplaceChild("left_leg", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(2.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-2.0F, 18.0F, 0.0F));
        body.addOrReplaceChild("right_leg", CubeListBuilder.create()
                        .texOffs(13, 0).addBox(-5.9F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(2.0F, 18.0F, 0.0F));

        ModelPart baked = LayerDefinition.create(mesh, 64, 64).bakeRoot();
        ModelPart bodyPart = baked.getChild("body");
        return CustomModelsModel.fromParts(baked,
                null, bodyPart, null, null,
                bodyPart.getChild("left_leg"), bodyPart.getChild("right_leg"),
                false, 1.0F, 1.0F, 1.0F, bodyPart, -8.0F, -5.0F);
    }
}
