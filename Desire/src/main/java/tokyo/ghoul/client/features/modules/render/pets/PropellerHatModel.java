package tokyo.ghoul.client.features.modules.render.pets;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class PropellerHatModel {
    private final ModelPart cap;
    private final ModelPart propeller;

    public PropellerHatModel(ModelPart root) {
        this.cap = root.getChild("cap");
        this.propeller = root.getChild("propeller");
    }

    public static LayerDefinition getTexturedModelData() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("cap", CubeListBuilder.create().texOffs(0, 15).addBox(-5.0F, -4.0F, -5.0F, 10.0F, 4.0F, 10.0F, CubeDeformation.NONE), PartPose.offset(0.0F, -5.0F, 0.0F));
        root.addOrReplaceChild("propeller", CubeListBuilder.create().texOffs(0, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F, CubeDeformation.NONE)
                        .texOffs(0, 3).addBox(-7.5F, -3.0F, -1.5F, 15.0F, 1.0F, 3.0F, CubeDeformation.NONE)
                        .texOffs(0, 29).addBox(-1.5F, -3.0F, -7.5F, 3.0F, 1.0F, 15.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -9.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    public void render(PoseStack matrices, VertexConsumer consumer, Identifier texture, int light, float age, boolean flying) {
        this.propeller.yRot = flying ? age * 1.35F : 0.0F;
        this.cap.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY, -1);
        this.propeller.render(matrices, consumer, light, OverlayTexture.NO_OVERLAY, -1);
    }
}
