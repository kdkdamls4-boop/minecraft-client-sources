package tokyo.ghoul.client.features.modules.combat;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.RotationUpdateEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.combat.ExplosionUtil;
import tokyo.ghoul.api.utils.player.DirectionUtil;
import tokyo.ghoul.api.utils.player.SimulatedPlayer;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleRegister(name = "Anti Crystal", description = "Защита от урона кристаллов", category = Category.COMBAT)
public class AntiCrystalModule extends Module {
    @Getter private static final AntiCrystalModule instance = new AntiCrystalModule();

    private final ModeSetting rotate = new ModeSetting("Ротация").values("Vanilla", "Без ротации").value("Vanilla");

    @Getter private Rotation rotation;
    private int ticksToBack;
    private int lastSlot = -1;
    @Getter private PlaceData lastData;

    private final ConcurrentLinkedQueue<VisualData> visualDatas = new ConcurrentLinkedQueue<>();

    public AntiCrystalModule() {
        addSettings(rotate);
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ticksToBack = 0;
        rotation = null;
        lastData = null;

        if (lastSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(lastSlot);
        }

        lastSlot = -1;
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener rotUpdate = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (rotation != null && rotate.is("Vanilla")) {
                applyRotation(rotation);
            }
        }));
        EventListener render = Render3DEvent.getInstance().subscribe(new Listener<>(this::onWorldRender));
        addEvents(update, rotUpdate, render);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (ticksToBack > 0) {
            ticksToBack--;
        }

        if (ticksToBack == 0 && lastSlot != -1) {
            mc.player.getInventory().setSelectedSlot(lastSlot);
            lastSlot = -1;
        }

        Player target = findTarget();
        int obsSlot = findHotbarItem(Items.OBSIDIAN);

        if (obsSlot == -1) {
            lastData = null;
            rotation = null;
            return;
        }

        final Player self = mc.player;

        if (target != null) {
            final Level world = mc.level;
            final Iterable<Entity> entities = mc.level.entitiesForRendering();
            lastData = findDangerPos(world, self, target, entities);
        }

        if (lastData == null) {
            rotation = null;
            return;
        }

        BlockPos blockPos = lastData.blockPos();
        if (blockPos == null) {
            lastData = null;
            rotation = null;
            return;
        }

        Direction direction = lastData.face();
        if (direction == null) {
            lastData = null;
            rotation = null;
            return;
        }

        Rotation rot = calculateRotation(getFacePoint(blockPos, direction));

        if (raycastBlock(rot, blockPos, 4.5F) != null) {
            {
                int slot = findHotbarItem(Items.OBSIDIAN);

                if (slot != -1) {
                    if (lastSlot == -1) lastSlot = self.getInventory().getSelectedSlot();
                    ticksToBack = 3;
                    self.getInventory().setSelectedSlot(slot);
                    applyRotation(rot);
                    mc.gameMode.useItemOn(
                            mc.player,
                            InteractionHand.MAIN_HAND,
                            new BlockHitResult(
                                    Vec3.atCenterOf(blockPos),
                                    DirectionUtil.getFaceDirection(rot, self, blockPos),
                                    blockPos,
                                    false
                            )
                    );
                    self.swing(InteractionHand.MAIN_HAND);
                    BlockPos visualPos = blockPos.relative(direction);
                    if (!hasVisual(visualPos)) {
                        VisualData visualData = new VisualData(visualPos, 0.2F, 0.8F, 0.2F);
                        visualData.animation.setValue(1);
                        visualDatas.add(visualData);
                    }
                    lastData = null;
                }
            }

            rotation = rot;
            if (rotation != null && rotate.is("Vanilla")) {
                applyRotation(rotation);
            }
        } else {
            rotation = null;
            lastData = null;
        }
    }

    private void applyRotation(Rotation rot) {
        if (rotate.is("Vanilla")) {
            RotationStrategy strategy = new RotationStrategy(new InstantRotation(), false).clientLook(false);
            strategy.ticksUntilReset(2);
            RotationManager.getInstance().addRotation(rot, strategy, TaskPriority.HIGH, this);
        }
    }

    private PlaceData findDangerPos(
            Level world,
            Player self,
            Player target,
            Iterable<Entity> entities
    ) {
        BlockPos bestPanPos = null;
        double bestPanDamage = 0;
        Direction bestFace = null;

        BlockPos basePos = self.blockPosition();
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int x = -5; x <= 5; x++) {
            int px = basePos.getX() + x;

            for (int y = -5; y <= 5; y++) {
                int py = basePos.getY() + y;

                for (int z = -5; z <= 5; z++) {
                    checkPos.set(px, py, basePos.getZ() + z);

                    if (checkPos.getY() >= ((int) mc.player.getY())) continue;

                    BlockState state = world.getBlockState(checkPos);
                    if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) continue;

                    Direction face = getBestVisibleFace(checkPos, self);
                    if (face == null) continue;

                    BlockPos placePos = checkPos.relative(face);

                    if (!world.getBlockState(placePos).isAir()) continue;

                    Vec3 rotatePoint = getFacePoint(checkPos, face);
                    Rotation rot = calculateRotation(rotatePoint);

                    BlockHitResult raycast = raycastBlock(rot, checkPos, 4.5F);

                    if (raycast == null || raycast.getDirection() != face || raycast.getDirection() != Direction.UP) continue;

                    Rotation rotTarget = calculateRotation(target.getEyePosition(), rotatePoint);

                    BlockHitResult raycastTarget = raycastBlock(rotTarget, target, checkPos, 6.0F);

                    if (raycastTarget == null) continue;

                    if (isBlockedByEntitiesForCrystal(entities, placePos)) continue;

                    SimulatedPlayer sim = SimulatedPlayer.simulateLocalPlayer(1);
                    AABB predictBox = (sim != null && sim.boundingBox != null) ? sim.boundingBox : self.getBoundingBox();
                    double selfDamage = ExplosionUtil.getExplosionDamageWPredict(Vec3.atCenterOf(checkPos.above()), self, predictBox);
                    if (selfDamage <= bestPanDamage || selfDamage < 22) continue;

                    bestPanDamage = selfDamage;
                    bestPanPos = checkPos.immutable();
                    bestFace = face;
                }
            }
        }

        return new PlaceData(bestPanPos, bestFace);
    }

    private boolean isBlockedByEntitiesForCrystal(Iterable<Entity> entities, BlockPos placePos) {
        if (entities == null) return false;
        for (Entity entity : entities) {
            if (!(entity instanceof ItemEntity) && ExplosionUtil.intersectsEntityBox(placePos, entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasVisual(BlockPos blockPos) {
        for (VisualData visualData : visualDatas) {
            if (visualData.getBlockPos().equals(blockPos)) {
                return true;
            }
        }
        return false;
    }

    private Direction getBestVisibleFace(BlockPos pos, Player entity) {
        Vec3 eye = entity.getEyePosition();
        Direction bestFace = null;
        double bestDist = Double.MAX_VALUE;

        for (Direction face : Direction.values()) {
            Vec3 point = getFacePoint(pos, face);
            Rotation rot = calculateRotation(point);

            if (raycastBlock(rot, pos, 4.5F) == null) {
                continue;
            }

            double dist = eye.distanceToSqr(point);
            if (dist < bestDist) {
                bestDist = dist;
                bestFace = face;
            }
        }

        return bestFace;
    }

    private Vec3 getFacePoint(BlockPos pos, Direction face) {
        return new Vec3(
                pos.getX() + 0.5 + face.getStepX() * 0.5001,
                pos.getY() + 0.5 + face.getStepY() * 0.5001,
                pos.getZ() + 0.5 + face.getStepZ() * 0.5001
        );
    }

    private Rotation calculateRotation(Vec3 toVec) {
        return calculateRotation(mc.player.getEyePosition(), toVec);
    }

    private Rotation calculateRotation(Vec3 fromVec, Vec3 toVec) {
        Vec3 diff = toVec.subtract(fromVec);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Mth.atan2(diff.y, distance) * 180.0 / Math.PI));
        return new Rotation(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F));
    }

    private BlockHitResult raycastBlock(Rotation rotation, BlockPos targetPos, double maxDistance) {
        if (mc.player == null) return null;
        return raycastBlock(rotation, mc.player, targetPos, maxDistance);
    }

    private BlockHitResult raycastBlock(Rotation rotation, Player player, BlockPos targetPos, double maxDistance) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = Vec3.directionFromRotation(rotation.getPitch(), rotation.getYaw());
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        AABB blockBox = new AABB(targetPos);
        Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
        if (hitOpt.isEmpty()) {
            return null;
        }

        Vec3 hitVec = hitOpt.get();
        Direction side = getHitSide(hitVec, targetPos);
        return new BlockHitResult(hitVec, side, targetPos, false);
    }

    private BlockHitResult raycastBlock(BlockPos targetPos, double maxDistance) {
        if (mc.player == null) return null;
        Vec3 eyePos = mc.player.getEyePosition();
        Vec3 lookVec = mc.player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

        AABB blockBox = new AABB(targetPos);
        Optional<Vec3> hitOpt = blockBox.clip(eyePos, endPos);
        if (hitOpt.isEmpty()) {
            return null;
        }

        Vec3 hitVec = hitOpt.get();
        Direction side = getHitSide(hitVec, targetPos);
        return new BlockHitResult(hitVec, side, targetPos, false);
    }

    private Direction getHitSide(Vec3 hitVec, BlockPos blockPos) {
        double dx = hitVec.x - (blockPos.getX() + 0.5);
        double dy = hitVec.y - (blockPos.getY() + 0.5);
        double dz = hitVec.z - (blockPos.getZ() + 0.5);

        double absDx = Math.abs(dx);
        double absDy = Math.abs(dy);
        double absDz = Math.abs(dz);

        if (absDx > absDy && absDx > absDz) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else if (absDy > absDz) {
            return dy > 0 ? Direction.UP : Direction.DOWN;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    private Player findTarget() {
        if (mc.player == null || mc.level == null) return null;
        Player bestTarget = null;
        double bestDist = 8.0 * 8.0;

        for (Player player : mc.level.players()) {
            if (player == mc.player || !player.isAlive()) continue;
            if (FriendManager.getInstance().contains(player.getName().getString())) continue;
            if (AntiBotModule.isBot(player)) continue;

            double dist = mc.player.distanceToSqr(player);
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = player;
            }
        }

        return bestTarget;
    }

    private int findHotbarItem(Item item) {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getItem(i).is(item)) {
                return i;
            }
        }
        return -1;
    }

    private void onWorldRender(Render3DEvent.Render3DEventData event) {
        if (mc.level == null || mc.player == null || visualDatas.isEmpty()) return;

        visualDatas.removeIf(visualData -> visualData.animation.getValue() == 0);

        var camera = event.cameraState();
        if (camera == null || !camera.initialized) return;
        double camX = camera.pos.x, camY = camera.pos.y, camZ = camera.pos.z;
        PoseStack matrices = event.matrixStack();

        for (VisualData visualData : visualDatas) {
            visualData.animation.animate(0);
            visualData.animation.update();
        }

        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.QUADS, (pose, buffer) -> {
            for (VisualData visualData : visualDatas) {
                int argb = argb(visualData.r, visualData.g, visualData.b, (float) visualData.animation.getValue() * 0.45f);
                emitBlockBox(pose.pose(), buffer, visualData.blockPos, camX, camY, camZ, argb, false);
            }
        });
        event.collector().order(1000).submitCustomGeometry(matrices, EvaLayers.DEBUG_LINES, (pose, buffer) -> {
            for (VisualData visualData : visualDatas) {
                int argb = argb(visualData.r, visualData.g, visualData.b, (float) visualData.animation.getValue());
                emitBlockBox(pose.pose(), buffer, visualData.blockPos, camX, camY, camZ, argb, true);
            }
        });
    }

    private static int argb(float r, float g, float b, float a) {
        int ai = Math.max(0, Math.min(255, (int) (a * 255)));
        int ri = Math.max(0, Math.min(255, (int) (r * 255)));
        int gi = Math.max(0, Math.min(255, (int) (g * 255)));
        int bi = Math.max(0, Math.min(255, (int) (b * 255)));
        return (ai << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private static void emitBlockBox(Matrix4f matrix, VertexConsumer buffer, BlockPos pos,
                                     double camX, double camY, double camZ, int argb, boolean outline) {
        float x1 = (float) (pos.getX() - camX);
        float y1 = (float) (pos.getY() - camY);
        float z1 = (float) (pos.getZ() - camZ);
        float x2 = x1 + 1.0f, y2 = y1 + 1.0f, z2 = z1 + 1.0f;
        if (outline) {
            line(buffer, matrix, x1, y1, z1, x2, y1, z1, argb);
            line(buffer, matrix, x2, y1, z1, x2, y1, z2, argb);
            line(buffer, matrix, x2, y1, z2, x1, y1, z2, argb);
            line(buffer, matrix, x1, y1, z2, x1, y1, z1, argb);
            line(buffer, matrix, x1, y2, z1, x2, y2, z1, argb);
            line(buffer, matrix, x2, y2, z1, x2, y2, z2, argb);
            line(buffer, matrix, x2, y2, z2, x1, y2, z2, argb);
            line(buffer, matrix, x1, y2, z2, x1, y2, z1, argb);
            line(buffer, matrix, x1, y1, z1, x1, y2, z1, argb);
            line(buffer, matrix, x2, y1, z1, x2, y2, z1, argb);
            line(buffer, matrix, x1, y1, z2, x1, y2, z2, argb);
            line(buffer, matrix, x2, y1, z2, x2, y2, z2, argb);
            return;
        }
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, argb);
        quad(buffer, matrix, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, argb);
        quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, argb);
        quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, argb);
        quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, argb);
        quad(buffer, matrix, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, argb);
    }

    private static void line(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix,
                             float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, int argb) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(argb);
        buffer.addVertex(matrix, x2, y2, z2).setColor(argb);
        buffer.addVertex(matrix, x3, y3, z3).setColor(argb);
        buffer.addVertex(matrix, x4, y4, z4).setColor(argb);
    }

    public record PlaceData(BlockPos blockPos, Direction face) {}

    @Getter
    @AllArgsConstructor
    public static class VisualData {
        private final BlockPos blockPos;
        private final Fade animation = new Fade();
        private float r;
        private float g;
        private float b;
    }

    public static class Fade {
        private double value;
        private double startValue;
        private double target;
        private long startTime;
        private boolean finished = true;

        public void animate(double next) {
            if (this.target != next) {
                this.startValue = this.value;
                this.target = next;
                this.startTime = System.currentTimeMillis();
                this.finished = false;
            }
        }

        public void update() {
            if (finished) return;
            double progress = Math.min(1.0, (System.currentTimeMillis() - startTime) / 300.0);
            if (progress >= 1.0) {
                value = target;
                finished = true;
            } else {
                value = startValue + (target - startValue) * Easing.CUBIC_OUT.apply(progress);
            }
        }

        public void setValue(double value) {
            this.value = value;
            this.startValue = value;
            this.target = value;
            this.finished = true;
        }

        public double getValue() {
            return value;
        }
    }
}
