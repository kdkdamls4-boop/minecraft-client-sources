package tokyo.ghoul.client.features.modules.render.custommodels;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.SwingAnimationType;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CustomModelsModel extends EntityModel<AvatarRenderState> implements HeadedModel {

    public enum ModelType {
        SAHUR,
        CRAZY_RABBIT,
        WHITE_DEMON,
        RED_DEMON,
        AMOGUS,
        CHIMPANZINI_BANANINI,
        FISH_BEAR,
        COCONUT_CAPYBARA,
        TRALALERO_TRALALA,
        BOMBARDIRO_CROCODILO,
        BALLERINA_CAPPUCCINA,
        LIRILI_LARILA,
        BRR_BRR_PATAPIM
    }

    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final boolean armsFollowBody;

    private final float rootScaleX;
    private final float rootScaleY;
    private final float rootScaleZ;

    private final ModelPart offsetPart;
    private final float offsetStanding;
    private final float offsetCrouching;
    private float headPitchScale = 1.0F;

    private static final EnumMap<ModelType, CustomModelsModel> CACHE = new EnumMap<>(ModelType.class);

    private CustomModelsModel(ModelPart root, ModelPart head, ModelPart body, ModelPart leftArm, ModelPart rightArm, ModelPart leftLeg, ModelPart rightLeg, boolean armsFollowBody,
                              float rootScaleX, float rootScaleY, float rootScaleZ,
                              ModelPart offsetPart, float offsetStanding, float offsetCrouching) {
        super(root);
        this.head = head;
        this.body = body;
        this.leftArm = leftArm;
        this.rightArm = rightArm;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
        this.armsFollowBody = armsFollowBody;
        this.rootScaleX = rootScaleX;
        this.rootScaleY = rootScaleY;
        this.rootScaleZ = rootScaleZ;
        this.offsetPart = offsetPart;
        this.offsetStanding = offsetStanding;
        this.offsetCrouching = offsetCrouching;
    }

    static CustomModelsModel fromParts(ModelPart root, ModelPart head, ModelPart body, ModelPart leftArm, ModelPart rightArm,
                                       ModelPart leftLeg, ModelPart rightLeg, boolean armsFollowBody,
                                       float rootScaleX, float rootScaleY, float rootScaleZ,
                                       ModelPart offsetPart, float offsetStanding, float offsetCrouching) {
        return new CustomModelsModel(root, head, body, leftArm, rightArm, leftLeg, rightLeg, armsFollowBody,
                rootScaleX, rootScaleY, rootScaleZ, offsetPart, offsetStanding, offsetCrouching);
    }

    public static CustomModelsModel create(ModelType type) {
        return CACHE.computeIfAbsent(type, CustomModelsModel::build);
    }

    @Override
    public ModelPart getHead() {
        return this.head != null ? this.head : this.root();
    }

    public tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticModelRenderer.AttachmentPoses attachmentPoses() {
        return tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticModelRenderer.AttachmentPoses.ofParts(
                this.root(), this.head, this.body, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg);
    }

    private static CustomModelsModel build(ModelType type) {
        return switch (type) {
            case CRAZY_RABBIT -> CustomModelsMeshes.crazyRabbit();
            case WHITE_DEMON, RED_DEMON -> CustomModelsMeshes.demon();
            case AMOGUS -> CustomModelsMeshes.amogus();
            case CHIMPANZINI_BANANINI -> blender("chimpanzini_bananini");
            case FISH_BEAR -> blender("fish_bear");
            case COCONUT_CAPYBARA -> blender("coconut_capybara");
            case TRALALERO_TRALALA -> blender("tralalero_tralala");
            case BOMBARDIRO_CROCODILO -> blender("bombardiro_crocodilo");
            case BALLERINA_CAPPUCCINA -> blender("ballerina_cappuccina");
            case LIRILI_LARILA -> blender("lirili_larila");
            case BRR_BRR_PATAPIM -> blender("brr_brr_patapim");
            default -> sahur();
        };
    }

    private static CustomModelsModel blender(String name) {
        CustomMesh.Baked baked = CustomMesh.bake(name);
        ModelPart root = baked.root();
        CustomModelsModel model = new CustomModelsModel(root, optional(root, "head"), optional(root, "body"),
                optional(root, "left_arm"), optional(root, "right_arm"),
                optional(root, "left_leg"), optional(root, "right_leg"), false,
                1.0F, 1.0F, 1.0F, null, 0.0F, 0.0F);
        model.headPitchScale = baked.headPitch();
        return model;
    }

    private static ModelPart optional(ModelPart root, String name) {
        return root.hasChild(name) ? root.getChild(name) : null;
    }

    @Override
    public void setupAnim(AvatarRenderState state) {
        super.setupAnim(state);
        this.root().xScale = this.rootScaleX;
        this.root().yScale = this.rootScaleY;
        this.root().zScale = this.rootScaleZ;
        if (this.offsetPart != null) {
            this.offsetPart.y = state.isCrouching ? this.offsetCrouching : this.offsetStanding;
        }
        float speedValue = state.speedValue;
        float walkPos = state.walkAnimationPos;
        float walkSpeed = state.walkAnimationSpeed;

        if (this.head != null) {
            this.head.xRot = state.xRot * ((float) Math.PI / 180) * this.headPitchScale;
            this.head.yRot = state.yRot * ((float) Math.PI / 180);
            if (state.isFallFlying) {
                this.head.xRot = -0.7853982f;
            }
        }
        if (this.rightArm != null) {
            this.rightArm.xRot = Mth.cos(walkPos * 0.6662f + (float) Math.PI) * 2.0f * walkSpeed * 0.5f / speedValue;
        }
        if (this.leftArm != null) {
            this.leftArm.xRot = Mth.cos(walkPos * 0.6662f) * 2.0f * walkSpeed * 0.5f / speedValue;
        }
        if (this.rightLeg != null) {
            this.rightLeg.xRot = Mth.cos(walkPos * 0.6662f) * 1.4f * walkSpeed / speedValue;
        }
        if (this.leftLeg != null) {
            this.leftLeg.xRot = Mth.cos(walkPos * 0.6662f + (float) Math.PI) * 1.4f * walkSpeed / speedValue;
        }
        this.applyAttackAnimation(state);
        if (this.rightArm != null) {
            AnimationUtils.bobModelPart(this.rightArm, state.ageInTicks, 1.0F);
        }
        if (this.leftArm != null) {
            AnimationUtils.bobModelPart(this.leftArm, state.ageInTicks, -1.0F);
        }
    }

    private void applyAttackAnimation(AvatarRenderState state) {
        if (this.rightArm == null && this.leftArm == null) {
            return;
        }
        float attackTime = state.attackTime;
        if (attackTime <= 0.0F) {
            return;
        }
        float bodyYRot = Mth.sin(Mth.sqrt(attackTime) * (float) Math.PI * 2.0F) * 0.2F;
        if (state.attackArm == HumanoidArm.LEFT) {
            bodyYRot *= -1.0F;
        }
        if (this.body != null) {
            this.body.yRot = bodyYRot;
            if (this.armsFollowBody && this.head != null) {
                this.head.yRot -= bodyYRot;
            }
        }
        if (!this.armsFollowBody) {
            if (this.rightArm != null) {
                this.rightArm.yRot += bodyYRot;
            }
            if (this.leftArm != null) {
                this.leftArm.yRot += bodyYRot;
                this.leftArm.xRot += bodyYRot;
            }
        }
        if (state.swingAnimationType != SwingAnimationType.WHACK) {
            return;
        }
        ModelPart attackArm = state.attackArm == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
        if (attackArm == null) {
            attackArm = this.rightArm != null ? this.rightArm : this.leftArm;
        }
        float swing = Ease.outQuart(attackTime);
        float aa = Mth.sin(swing * (float) Math.PI);
        float bb = Mth.sin(attackTime * (float) Math.PI) * -((this.head != null ? this.head.xRot : 0.0F) - 0.7F) * 0.75F;
        attackArm.xRot -= aa * 1.2F + bb;
        if (!this.armsFollowBody) {
            attackArm.yRot += bodyYRot * 2.0F;
        }
        attackArm.zRot += Mth.sin(attackTime * (float) Math.PI) * -0.4F;
    }

    private static Map<String, ModelPart> child(String name, ModelPart part) {
        Map<String, ModelPart> children = new HashMap<>();
        if (part != null) {
            children.put(name, part);
        }
        return children;
    }

    private static Map<String, ModelPart> children(ModelPart... parts) {
        Map<String, ModelPart> children = new HashMap<>();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i] != null) {
                children.put("part" + i, parts[i]);
            }
        }
        return children;
    }

    private static CustomModelsModel sahur() {
        List<ModelPart> parts = List.of(
                sahurPart(10.42857F, 7.94286F, 8.42857F, 11.0F, 9.08571F, 11.28571F, new int[]{70, 29, 69, 24}, new int[]{71, 24, 70, 29}, new int[]{69, 29, 70, 31}, new int[]{75, 29, 76, 31}, new int[]{64, 29, 69, 31}, new int[]{70, 29, 75, 31}),
                sahurPart(10.0F, 8.0F, 9.42857F, 12.28572F, 9.71429F, 13.42857F, new int[]{47, 7, 43, 0}, new int[]{51, 0, 47, 7}, new int[]{43, 7, 47, 10}, new int[]{54, 7, 58, 10}, new int[]{36, 7, 43, 10}, new int[]{47, 7, 54, 10}),
                sahurPart(11.14286F, 7.94286F, 8.28571F, 11.71429F, 9.08571F, 10.0F, new int[]{40, 31, 39, 28}, new int[]{41, 28, 40, 31}, new int[]{39, 31, 40, 33}, new int[]{43, 31, 44, 33}, new int[]{36, 31, 39, 33}, new int[]{40, 31, 43, 33}),
                sahurPart(11.85714F, 7.94286F, 8.28571F, 12.42857F, 9.08571F, 10.57143F, new int[]{17, 60, 16, 56}, new int[]{18, 56, 17, 60}, new int[]{16, 60, 17, 62}, new int[]{21, 60, 22, 62}, new int[]{12, 60, 16, 62}, new int[]{17, 60, 21, 62}),
                sahurPart(9.71429F, 7.94286F, 8.71429F, 10.28571F, 9.08571F, 11.57143F, new int[]{6, 61, 5, 56}, new int[]{7, 56, 6, 61}, new int[]{5, 61, 6, 63}, new int[]{11, 61, 12, 63}, new int[]{0, 61, 5, 63}, new int[]{6, 61, 11, 63}),
                sahurPart(8.42857F, 16.0F, 11.42857F, 10.14286F, 23.42857F, 13.14286F, new int[]{50, 23, 47, 20}, new int[]{53, 20, 50, 23}, new int[]{47, 23, 50, 36}, new int[]{53, 23, 56, 36}, new int[]{44, 23, 47, 36}, new int[]{50, 23, 53, 36}),
                sahurPart(15.0F, 16.0F, 11.42857F, 16.71429F, 23.42857F, 13.14286F, new int[]{47, 23, 50, 20}, new int[]{50, 20, 53, 23}, new int[]{50, 23, 47, 36}, new int[]{56, 23, 53, 36}, new int[]{53, 23, 50, 36}, new int[]{47, 23, 44, 36}),
                sahurPart(15.42857F, 16.57143F, 9.71429F, 16.57143F, 17.71429F, 14.28572F, new int[]{46, 18, 44, 10}, new int[]{48, 10, 46, 18}, new int[]{44, 18, 46, 20}, new int[]{54, 18, 56, 20}, new int[]{36, 18, 44, 20}, new int[]{46, 18, 54, 20}),
                sahurPart(14.85714F, 16.0F, 0.0F, 17.14286F, 18.28572F, 10.28571F, new int[]{22, 52, 18, 34}, new int[]{26, 34, 22, 52}, new int[]{18, 52, 22, 56}, new int[]{40, 52, 44, 56}, new int[]{0, 52, 18, 56}, new int[]{22, 52, 40, 56}),
                sahurPart(10.28571F, 9.14286F, 11.42857F, 12.0F, 16.57143F, 13.14286F, new int[]{50, 39, 47, 36}, new int[]{53, 36, 50, 39}, new int[]{47, 39, 50, 52}, new int[]{53, 39, 56, 52}, new int[]{44, 39, 47, 52}, new int[]{50, 39, 53, 52}),
                sahurPart(12.85714F, 8.0F, 9.42857F, 15.14286F, 9.71429F, 13.42857F, new int[]{43, 7, 47, 0}, new int[]{47, 0, 51, 7}, new int[]{47, 7, 43, 10}, new int[]{58, 7, 54, 10}, new int[]{54, 7, 47, 10}, new int[]{43, 7, 36, 10}),
                sahurPart(13.14286F, 9.14286F, 11.42857F, 14.85714F, 16.57143F, 13.14286F, new int[]{47, 39, 50, 36}, new int[]{50, 36, 53, 39}, new int[]{50, 39, 47, 52}, new int[]{56, 39, 53, 52}, new int[]{53, 39, 50, 52}, new int[]{47, 39, 44, 52}),
                sahurPart(13.42857F, 7.94286F, 8.28571F, 14.0F, 9.08571F, 10.0F, new int[]{39, 31, 40, 28}, new int[]{40, 28, 41, 31}, new int[]{40, 31, 39, 33}, new int[]{44, 31, 43, 33}, new int[]{43, 31, 40, 33}, new int[]{39, 31, 36, 33}),
                sahurPart(14.14286F, 7.94286F, 8.42857F, 14.71429F, 9.08571F, 11.28571F, new int[]{69, 29, 70, 24}, new int[]{70, 24, 71, 29}, new int[]{70, 29, 69, 31}, new int[]{76, 29, 75, 31}, new int[]{75, 29, 70, 31}, new int[]{69, 29, 64, 31}),
                sahurPart(14.85714F, 7.94286F, 8.71429F, 15.42857F, 9.08571F, 11.57143F, new int[]{5, 61, 6, 56}, new int[]{6, 56, 7, 61}, new int[]{6, 61, 5, 63}, new int[]{12, 61, 11, 63}, new int[]{11, 61, 6, 63}, new int[]{5, 61, 0, 63}),
                sahurPart(12.71429F, 7.94286F, 8.28571F, 13.28572F, 9.08571F, 10.57143F, new int[]{16, 60, 17, 56}, new int[]{17, 56, 18, 60}, new int[]{17, 60, 16, 62}, new int[]{22, 60, 21, 62}, new int[]{21, 60, 17, 62}, new int[]{16, 60, 12, 62}),
                sahurPart(10.0F, 15.42857F, 9.71429F, 15.14286F, 29.71429F, 14.85714F, new int[]{18, 9, 9, 0}, new int[]{27, 0, 18, 9}, new int[]{9, 9, 18, 34}, new int[]{27, 9, 36, 34}, new int[]{0, 9, 9, 34}, new int[]{18, 9, 27, 34}),
                sahurPart(9.71429F, 21.14286F, 9.54286F, 15.42857F, 24.0F, 9.54286F, new int[]{54, 52, 44, 52}, new int[]{64, 52, 54, 52}, new int[]{44, 52, 54, 57}, new int[]{54, 52, 64, 57}, new int[]{44, 52, 44, 57}, new int[]{54, 52, 54, 57}),
                sahurPart(9.71429F, 25.14286F, 9.14286F, 12.0F, 27.42857F, 10.28571F, new int[]{62, 12, 58, 10}, new int[]{66, 10, 62, 12}, new int[]{58, 12, 62, 16}, new int[]{64, 12, 68, 16}, new int[]{56, 12, 58, 16}, new int[]{62, 12, 64, 16}),
                sahurPart(10.0F, 25.28572F, 9.02857F, 11.71429F, 27.0F, 9.02857F, new int[]{59, 28, 56, 28}, new int[]{62, 28, 59, 28}, new int[]{56, 28, 59, 31}, new int[]{59, 28, 62, 31}, new int[]{56, 28, 56, 31}, new int[]{59, 28, 59, 31}),
                sahurPart(9.42857F, 27.37143F, 9.08571F, 12.28572F, 27.94286F, 9.65714F, new int[]{62, 22, 57, 21}, new int[]{67, 21, 62, 22}, new int[]{57, 22, 62, 23}, new int[]{63, 22, 68, 23}, new int[]{56, 22, 57, 23}, new int[]{62, 22, 63, 23}),
                sahurPart(12.0F, 24.14286F, 8.85714F, 13.14286F, 27.57143F, 10.0F, new int[]{40, 22, 38, 20}, new int[]{42, 20, 40, 22}, new int[]{38, 22, 40, 28}, new int[]{42, 22, 44, 28}, new int[]{36, 22, 38, 28}, new int[]{40, 22, 42, 28}),
                sahurPart(9.57143F, 24.0F, 9.0F, 11.28571F, 25.14286F, 10.14286F, new int[]{27, 58, 24, 56}, new int[]{30, 56, 27, 58}, new int[]{24, 58, 27, 60}, new int[]{29, 58, 32, 60}, new int[]{22, 58, 24, 60}, new int[]{27, 58, 29, 60}),
                sahurPart(13.85714F, 24.0F, 9.0F, 15.57143F, 25.14286F, 10.14286F, new int[]{24, 58, 27, 56}, new int[]{27, 56, 30, 58}, new int[]{27, 58, 24, 60}, new int[]{32, 58, 29, 60}, new int[]{29, 58, 27, 60}, new int[]{24, 58, 22, 60}),
                sahurPart(12.85714F, 27.37143F, 9.08571F, 15.71429F, 27.94286F, 9.65714F, new int[]{57, 22, 62, 21}, new int[]{62, 21, 67, 22}, new int[]{62, 22, 57, 23}, new int[]{68, 22, 63, 23}, new int[]{63, 22, 62, 23}, new int[]{57, 22, 56, 23}),
                sahurPart(13.14286F, 25.14286F, 9.14286F, 15.42857F, 27.42857F, 10.28571F, new int[]{58, 12, 62, 10}, new int[]{62, 10, 66, 12}, new int[]{62, 12, 58, 16}, new int[]{68, 12, 64, 16}, new int[]{64, 12, 62, 16}, new int[]{58, 12, 56, 16}),
                sahurPart(13.42857F, 25.28572F, 9.02857F, 15.14286F, 27.0F, 9.02857F, new int[]{59, 28, 56, 28}, new int[]{62, 28, 59, 28}, new int[]{56, 28, 59, 31}, new int[]{59, 28, 62, 31}, new int[]{56, 28, 56, 31}, new int[]{59, 28, 59, 31}),
                sahurPart(11.71429F, 20.85714F, 9.14286F, 13.42857F, 22.57143F, 10.28571F, new int[]{61, 18, 58, 16}, new int[]{64, 16, 61, 18}, new int[]{58, 18, 61, 21}, new int[]{63, 18, 66, 21}, new int[]{56, 18, 58, 21}, new int[]{61, 18, 63, 21})
        );

        ModelPart pose = new ModelPart(List.of(), children(parts.toArray(new ModelPart[0])));
        pose.setPos(0.0F, -3.0F, 0.0F);

        ModelPart root = new ModelPart(List.of(), child("pose", pose));

        return new CustomModelsModel(root, null, null, null, null, null, null, false,
                -1.25F, -1.25F, 1.25F, pose, -3.0F, -1.72F);
    }

    private static ModelPart sahurPart(float px1, float py1, float pz1, float px2, float py2, float pz2,
                                       int[] up, int[] down, int[] north, int[] south, int[] east, int[] west) {
        ModelPart.Cube cube = new UvCube(
                px1 - 12.0F, py1 - 24.0F, pz1 - 12.0F,
                px2 - 12.0F, py2 - 24.0F, pz2 - 12.0F,
                new int[][]{up, down, north, south, east, west}
        );
        return new ModelPart(List.of(cube), Map.of());
    }

    private static final class UvCube extends ModelPart.Cube {
        private static final float TEX_SIZE = 128.0F;

        private final ModelPart.Polygon[] faces;

        private UvCube(float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int[][] uv) {
            super(0, 0, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false,
                    TEX_SIZE, TEX_SIZE, EnumSet.noneOf(Direction.class));

            ModelPart.Vertex t0 = new ModelPart.Vertex(minX, minY, minZ, 0.0F, 0.0F);
            ModelPart.Vertex t1 = new ModelPart.Vertex(maxX, minY, minZ, 0.0F, 8.0F);
            ModelPart.Vertex t2 = new ModelPart.Vertex(maxX, maxY, minZ, 8.0F, 8.0F);
            ModelPart.Vertex t3 = new ModelPart.Vertex(minX, maxY, minZ, 8.0F, 0.0F);
            ModelPart.Vertex l0 = new ModelPart.Vertex(minX, minY, maxZ, 0.0F, 0.0F);
            ModelPart.Vertex l1 = new ModelPart.Vertex(maxX, minY, maxZ, 0.0F, 8.0F);
            ModelPart.Vertex l2 = new ModelPart.Vertex(maxX, maxY, maxZ, 8.0F, 8.0F);
            ModelPart.Vertex l3 = new ModelPart.Vertex(minX, maxY, maxZ, 8.0F, 0.0F);

            this.faces = new ModelPart.Polygon[]{
                    face(uv[1], l1, l0, t0, t1, Direction.DOWN),
                    face(uv[0], t2, t3, l3, l2, Direction.UP),
                    face(uv[5], t0, l0, l3, t3, Direction.WEST),
                    face(uv[2], t1, t0, t3, t2, Direction.NORTH),
                    face(uv[4], l1, t1, t2, l2, Direction.EAST),
                    face(uv[3], l0, l1, l2, l3, Direction.SOUTH)
            };
        }

        private static ModelPart.Polygon face(int[] rect,
                                              ModelPart.Vertex a,
                                              ModelPart.Vertex b,
                                              ModelPart.Vertex c,
                                              ModelPart.Vertex d,
                                              Direction direction) {
            return new ModelPart.Polygon(
                    new ModelPart.Vertex[]{a, b, c, d},
                    rect[0], rect[1], rect[2], rect[3],
                    TEX_SIZE, TEX_SIZE,
                    false,
                    direction
            );
        }

        @Override
        public void compile(PoseStack.Pose pose, VertexConsumer consumer, int lightCoords, int overlayCoords, int color) {
            Matrix4f matrix = pose.pose();
            Vector3f scratchVector = new Vector3f();

            for (ModelPart.Polygon polygon : this.faces) {
                Vector3f normal = pose.transformNormal(polygon.normal(), scratchVector);
                float nx = normal.x();
                float ny = normal.y();
                float nz = normal.z();

                for (ModelPart.Vertex vertex : polygon.vertices()) {
                    float x = vertex.worldX();
                    float y = vertex.worldY();
                    float z = vertex.worldZ();
                    Vector3f pos = matrix.transformPosition(x, y, z, scratchVector);
                    consumer.addVertex(pos.x(), pos.y(), pos.z(), color, vertex.u(), vertex.v(),
                            overlayCoords, lightCoords, nx, ny, nz);
                }
            }
        }
    }
}
