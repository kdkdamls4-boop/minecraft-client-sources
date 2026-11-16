package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.player.PlayerUtil;
import tokyo.ghoul.api.utils.render.GlowLine;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import com.mojang.blaze3d.vertex.PoseStack;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleRegister(name = "Predictions", description = "Предсказание траекторий", category = Category.RENDER)
public class PredictionsModule extends Module {
    @Getter private static final PredictionsModule instance = new PredictionsModule();

    private final MultiBooleanSetting render = new MultiBooleanSetting("Render").value(
            new BooleanSetting("Ender pearl").value(true),
            new BooleanSetting("Trident").value(false),
            new BooleanSetting("Arrow").value(false)
    );
    private final BooleanSetting walls = new BooleanSetting("Through walls").value(true);
    private final BooleanSetting friend = new BooleanSetting("Friendly indicator").value(false);
    private final ColorSetting friendColor = new ColorSetting("Color").value(new Color(0, 255, 0));
    private final BooleanSetting glow = new BooleanSetting("Glow").value(true);
    private final SliderSetting glowSize = new SliderSetting("Glow size").value(1.0F).range(0.4F, 2.5F).step(0.1F)
            .setVisible(glow::getValue);
    private final SliderSetting glowStrength = new SliderSetting("Glow strength").value(1.0F).range(0.2F, 2.0F).step(0.1F)
            .setVisible(glow::getValue);

    private static final float GLOW_BASE_SIZE = 0.32F;
    private static final float GLOW_BASE_ALPHA = 0.2F;
    private static final int MAX_GLOW_SPRITES = 6000;

    private final List<Points> points = new ArrayList<>();
    private final List<GlowSegment> glowSegments = new ArrayList<>();

    private final String UNKNOWN = "Неизвестный";

    public PredictionsModule() {
        addSettings(render, walls, friend, friendColor, glow, glowSize, glowStrength);
    }

    @Override
    public void onEvent() {
        EventListener render3DEvent = Render3DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender3D(event);
        }));

        EventListener render2DEvent = Render2DEvent.getInstance().subscribe(new Listener<>(event -> {
            handleRender2D(event);
        }));

        addEvents(render3DEvent, render2DEvent);
    }

    private void handleRender2D(Render2DEvent.Render2DEventData event) {
        Font font = Fonts.SF_MEDIUM;
        PoseStack matrixStack = event.matrixStack();

        for (Points point : points) {
            Vector2f project = ProjectionUtil.project((float)point.position.x, (float)point.position.y, (float)point.position.z);
            if (project.x == Float.MAX_VALUE && project.y == Float.MAX_VALUE) continue;

            String text = String.format("%s (%.1f сек)", point.itemName, point.ticks * 50 / 1000.0);
            String ownerText = "От " + point.ownerName;
            float offset = 3f, fontSize = 7f;
            float textWidth = font.getWidth(text, fontSize);
            float ownerWidth = font.getWidth(ownerText, fontSize);
            float textHeight = fontSize + offset * 2f;
            float addRectWidth = ownerWidth + offset * 2f;
            float posX = project.x - textWidth / 2f - offset;
            float posY = project.y;

            Color bgColor = point.isFriend && friend.getValue() ? friendColor.getValue() : UIColors.backgroundBlur();
            Color textColor = UIColors.textColor();

            RenderUtil.BLUR_RECT.draw(matrixStack, posX, posY, textWidth + offset * 2f, textHeight, 2f, bgColor);
            font.drawText(matrixStack, text, posX + offset, posY + offset, fontSize, textColor);

            if (!ownerText.contains(UNKNOWN)) {
                RenderUtil.BLUR_RECT.draw(matrixStack, project.x - addRectWidth / 2f, posY + textHeight + 2f, addRectWidth, textHeight, 2f, bgColor);
                font.drawText(matrixStack, ownerText, project.x - ownerWidth / 2f, posY + textHeight + 2f + offset, fontSize, textColor);
            }
        }
    }

    private void handleRender3D(Render3DEvent.Render3DEventData event) {
        PoseStack matrixStack = event.matrixStack();
        Vec3 renderOffset = mc.getEntityRenderDispatcher().camera.position();
        Matrix4f framePose = new Matrix4f(matrixStack.last().pose());
        points.clear();
        glowSegments.clear();

        matrixStack.pushPose();
        matrixStack.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);

        for (var entity : mc.level.entitiesForRendering()) {
            String name = entity instanceof ThrownEnderpearl ? ((ThrowableItemProjectile) entity).getItem().getHoverName().getString() : entity.getName().getString();

            boolean isPearl = entity instanceof ThrownEnderpearl;
            boolean isTrident = entity instanceof ThrownTrident trident && !trident.isNoPhysics() && !trident.verticalCollisionBelow;
            boolean isArrow = entity instanceof Arrow;

            if ((isPearl && render.isEnabled("Ender pearl")) || (isTrident && render.isEnabled("Trident")) || (isArrow && render.isEnabled("Arrow"))) {

                if ((isArrow || isTrident) && (entity.getDeltaMovement().lengthSqr() < 0.001)) continue;

                var ownerRef = ((Projectile) entity).owner;
                UUID ownerUuid = ownerRef != null ? ownerRef.getUUID() : null;
                var owner = ownerUuid != null ? mc.level.getPlayerByUUID(ownerUuid) : null;
                boolean isFriend = owner != null && (FriendManager.getInstance().contains(owner.getName().getString()) || owner.getName().getString().equals(mc.getUser().getName()));
                String ownerName = owner != null ? mc.getUser().getName().equals(owner.getName().getString()) ? "Вас" : owner.getName().getString() : UNKNOWN;

                predictTrajectory(entity, isFriend, name, ownerName, matrixStack);
            }

        }

        renderGlow(event, framePose, renderOffset);
        RenderUtil.WORLD.endRender(matrixStack);
    }

    private void renderGlow(Render3DEvent.Render3DEventData event, Matrix4f framePose, Vec3 cameraPos) {
        if (!glow.getValue() || glowSegments.isEmpty()) return;
        var camera = event.cameraState();
        Quaternionf orientation = camera == null ? null : camera.orientation;
        if (orientation == null) return;

        float size = GLOW_BASE_SIZE * glowSize.getValue();
        int alpha = Math.min(255, Math.round(255.0F * GLOW_BASE_ALPHA * glowStrength.getValue()));
        double spacing = size * 0.3;
        VertexConsumer buffer = RenderUtil.WORLD.textured(FileUtil.getImage("particles/glow"));

        int budget = MAX_GLOW_SPRITES;
        for (GlowSegment segment : glowSegments) {
            if (budget <= 0) break;
            int argb = alpha << 24 | segment.rgb & 0xFFFFFF;
            budget -= GlowLine.segment(buffer, framePose, segment.from.subtract(cameraPos), segment.to.subtract(cameraPos),
                    orientation, size, argb, spacing);
        }
    }


    private void predictTrajectory(Entity entity, boolean isFriend, String itemName, String ownerName, PoseStack matrixStack) {
        Color color = isFriend && friend.getValue() ? Color.GREEN : UIColors.gradient(entity.tickCount % 360);

        Vec3 motion = entity.getDeltaMovement();
        Vec3 pos = entity.position();
        Vec3 prevPos;
        int ticks = 0;

        for (int i = 0; i <= 149; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = getMotion(entity, motion);

            boolean canSee = walls.getValue() || PlayerUtil.canSee(pos);

            boolean thinLine = canSee && !glow.getValue();
            var matrix = matrixStack.last().pose();
            var buffer = thinLine ? RenderUtil.WORLD.lines() : null;
            if (thinLine) buffer.addVertex(matrix, (float)prevPos.x, (float)prevPos.y, (float)prevPos.z)
                    .setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());

            var hit = mc.level.clip(new ClipContext(prevPos, pos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() == HitResult.Type.BLOCK) pos = hit.getLocation();

            if (thinLine) buffer.addVertex(matrix, (float)pos.x, (float)pos.y, (float)pos.z).setColor(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
            if (canSee && glow.getValue()) glowSegments.add(new GlowSegment(prevPos, pos, color.getRGB()));

            if (hit.getType() == HitResult.Type.BLOCK || pos.y < -128) {
                points.add(new Points(pos, ticks, isFriend, itemName, ownerName));
                break;
            }
            ticks++;
        }
    }


    private Vec3 getMotion(Entity entity, Vec3 motion) {
        Vec3 motion2 = motion;
        motion2 = entity.isInWater() ? motion2.scale(0.8) : motion2.scale(0.99);

        if (!entity.isNoGravity()) {
            motion2 = motion2.subtract(0, 0.03, 0);
        }

        return motion2;
    }

    private record Points(Vec3 position, int ticks, boolean isFriend, String itemName, String ownerName) {}

    private record GlowSegment(Vec3 from, Vec3 to, int rgb) {}
}
