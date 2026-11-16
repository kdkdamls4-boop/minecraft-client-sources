package tokyo.ghoul.client.features.modules.render;

import com.mojang.math.Axis;
import lombok.Getter;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.features.modules.combat.AntiBotModule;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@ModuleRegister(name = "Arrows", description = "Стрелки к игрокам вне экрана", category = Category.RENDER)
public class ArrowsModule extends Module {
    @Getter private static final ArrowsModule instance = new ArrowsModule();
    private static final float ARROW_SIZE = 46f;
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final MultiBooleanSetting rendering = new MultiBooleanSetting("Отображать").value(
            new BooleanSetting("Игроков").value(true),
            new BooleanSetting("Друзей").value(true),
            new BooleanSetting("Голых").value(false),
            new BooleanSetting("Предметы").value(false)
    );
    private final ColorSetting playerColor = new ColorSetting("Цвет игроков").value(Color.WHITE)
            .setVisible(() -> rendering.isEnabled("Игроков") || rendering.isEnabled("Голых"));
    private final ColorSetting friendColor = new ColorSetting("Цвет друзей").value(new Color(0, 255, 0))
            .setVisible(() -> rendering.isEnabled("Друзей"));
    private final ColorSetting itemColor = new ColorSetting("Цвет предметов").value(new Color(255, 0, 0))
            .setVisible(() -> rendering.isEnabled("Предметы"));
    private final SliderSetting radius = new SliderSetting("Радиус").value(40f).range(30f, 80f).step(5f);
    private final BooleanSetting onlyIfNotVisible = new BooleanSetting("Показывать только если не видишь").value(false);

    public ArrowsModule() {
        addSettings(rendering, playerColor, friendColor, itemColor, onlyIfNotVisible, radius);
    }

    @Override
    public void onEvent() {
        EventListener render = Render2DEvent.getInstance().subscribe(new Listener<>(2, this::onRender));
        addEvents(render);
    }

    private void onRender(Render2DEvent.Render2DEventData event) {
        if (mc.player == null || mc.level == null || mc.getDebugOverlay().showDebugScreen()) return;
        Camera camera = mc.gameRenderer.mainCamera();
        float camYaw = camera.yRot();
        if (mc.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) camYaw += 180f;
        double cos = Math.cos(Math.toRadians(camYaw));
        double sin = Math.sin(Math.toRadians(camYaw));
        double baseRadius = radius.getValue();
        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        Vec3 cameraPos = camera.position();

        for (Entity entity : collectTargets()) {
            if (!isValidEntity(entity)) continue;
            double adjustedRadius = (entity instanceof ItemEntity) ? Math.max(baseRadius - 20, 0) : baseRadius;
            double xOffset = (width / 2F) - adjustedRadius;
            double yOffset = (height / 2F) - adjustedRadius;
            double xWay = (MathUtil.interpolate(entity.xOld, entity.getX()) - cameraPos.x) * 0.01D;
            double zWay = (MathUtil.interpolate(entity.zOld, entity.getZ()) - cameraPos.z) * 0.01D;
            double rotationY = -(zWay * cos - xWay * sin);
            double rotationX = -(xWay * cos + zWay * sin);
            if (!isValidRotation(rotationX, rotationY, adjustedRadius)) continue;

            double angle = Math.toDegrees(Math.atan2(rotationY, rotationX));
            double x = (adjustedRadius * Math.cos(Math.toRadians(angle))) + xOffset + adjustedRadius;
            double y = (adjustedRadius * Math.sin(Math.toRadians(angle))) + yOffset + adjustedRadius;

            var matrices = event.matrixStack();
            matrices.pushPose();
            matrices.translate(x, y, 0D);
            matrices.mulPose(Axis.ZP.rotationDegrees((float) angle));
            RenderUtil.TEXTURE_RECT.draw(matrices, -ARROW_SIZE / 2f, -ARROW_SIZE / 2f, ARROW_SIZE, ARROW_SIZE, 0f,
                    determineEntityColor(entity), 0f, 0f, 1f, 1f, FileUtil.getImage("arrows/triangle"));
            matrices.popPose();
        }

    }

    private List<Entity> collectTargets() {
        List<Entity> out = new ArrayList<>(mc.level.players());
        if (rendering.isEnabled("Предметы")) {
            AABB box = mc.player.getBoundingBox().inflate(64.0);
            out.addAll(mc.level.getEntitiesOfClass(ItemEntity.class, box, item -> true));
        }
        return out;
    }

    private boolean isValidEntity(Entity entity) {
        if (entity == mc.player || !entity.isAlive()) return false;
        if (entity instanceof Player player) {
            if (AntiBotModule.isBot(player)) return false;
            if (!isPlayerTarget(player)) return false;
        } else if (!(entity instanceof ItemEntity) || !rendering.isEnabled("Предметы")) {
            return false;
        }
        return !onlyIfNotVisible.getValue() || !isEntityVisible(entity);
    }

    private boolean isPlayerTarget(Player player) {
        boolean friend = FriendManager.getInstance().contains(player.getGameProfile().name());
        boolean naked = isNaked(player);
        if (friend) return rendering.isEnabled("Друзей");
        if (naked) return rendering.isEnabled("Голых");
        return rendering.isEnabled("Игроков");
    }

    private static boolean isNaked(Player player) {
        for (EquipmentSlot slot : ARMOR) {
            if (!player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }

    private boolean isEntityVisible(Entity entity) {
        Vec3 start = mc.player.getEyePosition(mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
        Vec3 end = entity.position().add(0, entity.getBbHeight() / 2, 0);
        HitResult result = mc.level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        return result.getType() == HitResult.Type.MISS;
    }

    private Color determineEntityColor(Entity entity) {
        if (entity instanceof Player player) {
            if (FriendManager.getInstance().contains(player.getGameProfile().name())) {
                return friendColor.getValue();
            }
            return playerColor.getValue();
        }
        if (entity instanceof ItemEntity) return itemColor.getValue();
        return playerColor.getValue();
    }

    private boolean isValidRotation(double rotationX, double rotationY, double radius) {
        final double mrotY = -rotationY;
        final double mrotX = -rotationX;
        return Mth.sqrt((float) (mrotX * mrotX + mrotY * mrotY)) < radius;
    }
}
