package tokyo.ghoul.client.features.modules.render.ghosthit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Ghost Hit", description = "Эффект призрачного удара", category = Category.RENDER)
public class GhostHitModule extends Module {
    @Getter private static final GhostHitModule instance = new GhostHitModule();
    public static final float MODEL_SCALE = 0.9375F;
    public static final float VERTICAL_OFFSET = -1.501F;
    private static final int TOTEM_POP_STATUS = 35;
    private static final int FULL_BRIGHT = 0x00F000F0;

    public final SliderSetting lifetime = new SliderSetting("Lifetime").value(1450f).range(550f, 3000f).step(50f);
    public final SliderSetting riseHeight = new SliderSetting("Rise").value(1.85f).range(0.45f, 4f).step(0.05f);
    public final SliderSetting ghostAlpha = new SliderSetting("Opacity").value(0.58f).range(0.15f, 1f).step(0.01f);
    public final SliderSetting glowPower = new SliderSetting("Glow").value(1f).range(0.2f, 2.2f).step(0.05f);
    public final SliderSetting maxGhosts = new SliderSetting("Max Ghosts").value(7f).range(1f, 16f).step(1f);
    public final SliderSetting particles = new SliderSetting("Particles").value(7f).range(0f, 24f).step(1f);
    public final BooleanSetting onHit = new BooleanSetting("Hit").value(true);
    public final BooleanSetting onTotem = new BooleanSetting("Totem").value(true);
    public final BooleanSetting rainbow = new BooleanSetting("Rainbow").value(false);

    private final List<Ghost> ghosts = new ArrayList<>();
    private PlayerModel wideModel;
    private PlayerModel slimModel;

    public GhostHitModule() {
        addSettings(lifetime, riseHeight, ghostAlpha, glowPower, maxGhosts, particles, onHit, onTotem, rainbow);
    }

    @Override
    public void onDisable() {
        ghosts.clear();
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener attack = AttackEvent.getInstance().subscribe(new Listener<>(event -> {
            if (onHit.getValue()) capture(event.entity());
        }));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.isReceive() || !onTotem.getValue() || mc.level == null) return;
            if (event.packet() instanceof ClientboundEntityEventPacket status
                    && status.getEventId() == TOTEM_POP_STATUS) {
                capture(status.getEntity(mc.level));
            }
        }));
        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(this::render));
        addEvents(attack, packet, render);
    }

    private void render(Render3DEvent.Render3DEventData event) {
        if (event.cameraState() == null) return;
        long now = System.currentTimeMillis();
        ghosts.removeIf(ghost -> ghost.rawProgress(now) >= 1f);
        if (ghosts.isEmpty()) return;
        ensureModels();
        if (wideModel == null || slimModel == null) return;

        Vec3 cam = event.cameraState().pos;
        float glow = glowPower.getValue();
        Identifier bloom = FileUtil.getImage("particles/glow");
        PoseStack matrices = event.matrixStack();

        for (Ghost ghost : List.copyOf(ghosts)) {
            float raw = ghost.rawProgress(now);
            float eased = ghost.easedProgress(raw);
            if (eased < 0.01f) continue;
            float rise = riseHeight(raw);
            int tint = (Mth.clamp((int) (eased * ghostAlpha.getValue() * 255f), 0, 255) << 24) | 0xFFFFFF;

            PlayerModel model = ghost.slim ? slimModel : wideModel;
            AvatarRenderState state = buildState(ghost, raw);
            model.resetPose();
            model.setupAnim(state);
            hideExtras(model);

            float scale = MODEL_SCALE * (ghost.baby ? 0.5F : 1.0F);
            float yawWobble = Mth.sin(raw * 3.1415927F) * 7.0F + raw * 18.0F;
            matrices.pushPose();
            matrices.translate(ghost.pos.x - cam.x, ghost.pos.y + rise - cam.y, ghost.pos.z - cam.z);
            matrices.mulPose(Axis.YP.rotationDegrees(180.0F - ghost.bodyYaw + yawWobble));
            matrices.scale(-scale, -scale, scale);
            matrices.translate(0.0F, VERTICAL_OFFSET, 0.0F);
            event.collector().submitCustomGeometry(matrices, RenderTypes.entityTranslucent(ghost.texture), (pose, consumer) -> {
                PoseStack local = new PoseStack();
                local.last().pose().set(pose.pose());
                local.last().normal().set(pose.normal());
                model.root().render(local, consumer, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, tint);
            });
            matrices.popPose();
        }

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.textured(bloom), (pose, buffer) -> {
            for (Ghost ghost : List.copyOf(ghosts)) {
                float raw = ghost.rawProgress(now);
                float eased = ghost.easedProgress(raw);
                if (eased < 0.01f) continue;
                float rise = riseHeight(raw);
                float pulse = 0.86F + Mth.sin(raw * 6.2831855F * 2.0F + ghost.seed * 6.2831855F) * 0.14F;
                Vec3 risen = ghost.pos.add(0.0D, rise, 0.0D).subtract(cam);
                sprite(pose, buffer, event, risen.add(0, ghost.height * 0.56, 0), 2.55F * pulse * 0.5F, ghostColor(ghost, raw, (int) (158f * eased * glow)));
                sprite(pose, buffer, event, risen.add(0, ghost.height * 0.95, 0), 1.35F * pulse * 0.5F, ghostColor(ghost, raw + 0.05f, (int) (138f * eased * glow)));
                sprite(pose, buffer, event, risen.add(0, 0.18, 0), 1.85F * (1f + raw * 0.35f) * 0.5F, ghostColor(ghost, raw + 0.12f, (int) (120f * eased * glow)));
                int count = Math.round(particles.getValue());
                for (int index = 0; index < count; index++) {
                    float angle = ghost.seed * 6.2831855F + raw * 6.2831855F * 2.25F + index * 6.2831855F / Math.max(1, count);
                    float radius = 0.48F + 0.12F * Mth.sin(raw * 6.2831855F + index);
                    float yOff = 0.25F + ghost.height * (index / (float) Math.max(1, count)) + Mth.sin(raw * 6.2831855F * 1.5F + index) * 0.12F;
                    Vec3 point = risen.add(Mth.cos(angle) * radius, yOff, Mth.sin(angle) * radius);
                    float size = 0.32F + 0.12F * Mth.sin(angle + raw * 6.2831855F);
                    sprite(pose, buffer, event, point, size * 0.5F, ghostColor(ghost, raw + index * 0.03f, (int) (112f * eased * glow)));
                }
            }
        });

        int trailCount = Math.max(0, Math.min(6, Math.round(particles.getValue() / 2.5F)));
        if (trailCount <= 0) return;
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) -> {
            Matrix4f matrix = pose.pose();
            for (Ghost ghost : List.copyOf(ghosts)) {
                float raw = ghost.rawProgress(now);
                float eased = ghost.easedProgress(raw);
                if (eased < 0.01f) continue;
                float rise = riseHeight(raw);
                Vec3 risen = ghost.pos.add(0.0D, rise, 0.0D).subtract(cam);
                for (int trail = 0; trail < trailCount; trail++) {
                    Vec3 previous = null;
                    for (int step = 0; step <= 48; step++) {
                        float t = step / 48.0F;
                        float angle = ghost.seed * 6.2831855F + raw * 6.2831855F * 2.8F + t * 6.2831855F * 1.28F + trail * 6.2831855F / Math.max(1, trailCount);
                        float radius = (0.34F + 0.16F * Mth.sin(t * 3.1415927F)) * (1.0F + raw * 0.42F);
                        Vec3 point = risen.add(Mth.cos(angle) * radius, 0.2D + ghost.height * t + raw * 0.36D, Mth.sin(angle) * radius);
                        if (previous != null) {
                            int color = ghostColor(ghost, raw + t * 0.08F, (int) ((1.0F - t * 0.35F) * eased * 160.0F * glow));
                            buffer.addVertex(matrix, (float) previous.x, (float) previous.y, (float) previous.z).setColor(color);
                            buffer.addVertex(matrix, (float) point.x, (float) point.y, (float) point.z).setColor(color);
                        }
                        previous = point;
                    }
                }
            }
        });
    }

    private void sprite(PoseStack.Pose pose, com.mojang.blaze3d.vertex.VertexConsumer buffer, Render3DEvent.Render3DEventData event, Vec3 pos, float half, int color) {
        if (((color >>> 24) & 255) <= 0) return;
        Matrix4f matrix = new Matrix4f(pose.pose());
        matrix.translate((float) pos.x, (float) pos.y, (float) pos.z);
        if (event.cameraState().orientation != null) {
            matrix.rotate(event.cameraState().orientation);
        }
        buffer.addVertex(matrix, -half, -half, 0f).setUv(0f, 1f).setColor(color);
        buffer.addVertex(matrix, half, -half, 0f).setUv(1f, 1f).setColor(color);
        buffer.addVertex(matrix, half, half, 0f).setUv(1f, 0f).setColor(color);
        buffer.addVertex(matrix, -half, half, 0f).setUv(0f, 0f).setColor(color);
    }

    private void capture(Entity target) {
        if (mc.player == null || mc.level == null) return;
        if (!(target instanceof AbstractClientPlayer player) || player == mc.player) return;
        var skin = player.getSkin();
        if (skin == null || skin.body() == null || skin.body().texturePath() == null) return;
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 pos = new Vec3(
                Mth.lerp(tickDelta, player.xo, player.getX()),
                Mth.lerp(tickDelta, player.yo, player.getY()),
                Mth.lerp(tickDelta, player.zo, player.getZ()));
        ghosts.add(new Ghost(
                pos,
                skin.body().texturePath(),
                skin.model() == PlayerModelType.SLIM,
                Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot),
                Mth.rotLerp(tickDelta, player.yHeadRotO, player.yHeadRot) - Mth.rotLerp(tickDelta, player.yBodyRotO, player.yBodyRot),
                Mth.lerp(tickDelta, player.xRotO, player.getXRot()),
                player.walkAnimation.position(),
                Math.min(player.walkAnimation.speed(), 1.0F),
                player.getAttackAnim(tickDelta),
                player.isBaby(),
                player.getBbHeight(),
                System.currentTimeMillis(),
                (float) Math.random()
        ));
        while (ghosts.size() > Math.round(maxGhosts.getValue())) {
            ghosts.removeFirst();
        }
    }

    private float riseHeight(float progress) {
        return riseHeight.getValue() * (1.0F - (float) Math.pow(1.0F - progress, 2.4D));
    }

    private int ghostColor(Ghost ghost, float progress, int alpha) {
        alpha = Mth.clamp(alpha, 0, 255);
        if (rainbow.getValue()) {
            float hue = (ghost.seed + progress * 0.35f + (System.currentTimeMillis() % 4000L) / 4000f) % 1f;
            Color color = Color.getHSBColor(hue, 0.85f, 1f);
            return (alpha << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        }
        Color accent = UIColors.primary();
        Color secondary = UIColors.secondary();
        float blend = (float) (Math.sin(progress * Math.PI * 2.0 + ghost.seed * 6.0) + 1.0) * 0.5f;
        int r = (int) (accent.getRed() + (secondary.getRed() - accent.getRed()) * blend);
        int g = (int) (accent.getGreen() + (secondary.getGreen() - accent.getGreen()) * blend);
        int b = (int) (accent.getBlue() + (secondary.getBlue() - accent.getBlue()) * blend);
        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }

    private AvatarRenderState buildState(Ghost ghost, float raw) {
        AvatarRenderState state = new AvatarRenderState();
        float limbSpeed = Math.clamp(ghost.limbSpeed * (1.0F - raw * 0.65F), 0.0F, 1.0F);
        state.bodyRot = ghost.bodyYaw;
        state.yRot = ghost.headYaw;
        state.xRot = ghost.pitch - raw * raw * (3f - 2f * raw) * 14.0F;
        state.walkAnimationPos = ghost.limbPos + raw * 2.0F;
        state.walkAnimationSpeed = limbSpeed;
        state.speedValue = limbSpeed > 1.0E-4F ? 1.0F / limbSpeed : 1.0F;
        state.isCrouching = false;
        state.isBaby = ghost.baby;
        state.pose = Pose.STANDING;
        state.ageInTicks = raw * 20.0F;
        state.scale = 1.0F;
        state.ageScale = ghost.baby ? 0.5F : 1.0F;
        state.mainArm = HumanoidArm.RIGHT;
        state.attackArm = HumanoidArm.RIGHT;
        state.useItemHand = InteractionHand.MAIN_HAND;
        state.swingAnimationType = SwingAnimationType.WHACK;
        state.attackTime = Mth.clamp(ghost.swing * (1.0F - raw) + Mth.sin(raw * 3.1415927F) * 0.32F, 0.0F, 1.0F);
        state.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        state.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        state.headEquipment = ItemStack.EMPTY;
        state.chestEquipment = ItemStack.EMPTY;
        state.legsEquipment = ItemStack.EMPTY;
        state.feetEquipment = ItemStack.EMPTY;
        state.showHat = false;
        state.showJacket = false;
        state.showLeftSleeve = false;
        state.showRightSleeve = false;
        state.showLeftPants = false;
        state.showRightPants = false;
        return state;
    }

    private void hideExtras(PlayerModel model) {
        model.head.visible = true;
        model.body.visible = true;
        model.leftArm.visible = true;
        model.rightArm.visible = true;
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        model.hat.visible = false;
        model.jacket.visible = false;
        model.leftSleeve.visible = false;
        model.rightSleeve.visible = false;
        model.leftPants.visible = false;
        model.rightPants.visible = false;
        ModelPart root = model.root();
        if (root.hasChild("ear")) root.getChild("ear").visible = false;
        if (root.hasChild("cloak")) root.getChild("cloak").visible = false;
    }

    private void ensureModels() {
        Minecraft minecraft = Minecraft.getInstance();
        if (wideModel == null) {
            wideModel = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
        }
        if (slimModel == null) {
            slimModel = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
        }
    }

    private record Ghost(Vec3 pos, Identifier texture, boolean slim, float bodyYaw, float headYaw, float pitch,
                         float limbPos, float limbSpeed, float swing, boolean baby, float height, long birthMs, float seed) {
        float rawProgress(long now) {
            return Mth.clamp((now - birthMs) / Math.max(1f, instance.lifetime.getValue()), 0f, 1f);
        }

        float easedProgress(float progress) {
            float fadeIn = smooth(Mth.clamp(progress / 0.16f, 0f, 1f));
            float fadeOut = 1f - smooth(Mth.clamp((progress - 0.22f) / 0.78f, 0f, 1f));
            return fadeIn * fadeOut;
        }

        private static float smooth(float value) {
            return value * value * (3f - 2f * value);
        }
    }
}
