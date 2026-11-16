package tokyo.ghoul.client.features.modules.player;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.other.RotationUpdateEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.world.HandleBreakingEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.player.HotbarSupply;
import tokyo.ghoul.api.utils.render.pipeline.EvaLayers;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.rotations.InstantRotation;
import tokyo.ghoul.api.utils.task.TaskPriority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

@ModuleRegister(name = "Blocker", description = "Блокировка нежелательных действий", category = Category.PLAYER)
public class BlockerModule extends Module {
    @Getter private static final BlockerModule instance = new BlockerModule();

    private final ModeSetting levitation = new ModeSetting("Левитация")
            .values("Выкл", "При эффекте", "Заготовка", "Всегда крыша").value("Заготовка");
    private final BooleanSetting jumpForPillar = new BooleanSetting("Прыгать для столба").value(true)
            .setVisible(() -> !levitation.is("Выкл"));
    private final BooleanSetting dig = new BooleanSetting("Раскапывать блоки").value(true);
    private final BooleanSetting digFloor = new BooleanSetting("Менять пол").value(false).setVisible(dig::getValue);
    private final BooleanSetting waitCenter = new BooleanSetting("Ждать центр").value(true);
    private final SliderSetting centerMargin = new SliderSetting("Отступ от края").range(0.05F, 0.25F).step(0.01F).value(0.10F).setVisible(waitCenter::getValue);
    private final ModeSetting rotate = new ModeSetting("Ротация").values("Vanilla", "Без ротации").value("Vanilla");
    @Getter private final BooleanSetting clientLook = new BooleanSetting("Client look").value(false).setVisible(() -> rotate.is("Vanilla"));
    private final SliderSetting blocksPerTick = new SliderSetting("Блоков за тик").range(1.0F, 8.0F).step(1.0F).value(8.0F)
            .setVisible(() -> rotate.is("Без ротации"));
    private final BooleanSetting fromInventory = new BooleanSetting("Брать из инвентаря").value(true);
    private final SliderSetting obsidianSlot = new SliderSetting("Слот обсидиана").range(1.0F, 9.0F).step(1.0F).value(9.0F)
            .setVisible(fromInventory::getValue);
    private final BooleanSetting render = new BooleanSetting("Подсветка").value(true);

    private static final Direction[] HORIZONTALS = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST
    };
    private static final float STRONG_RESISTANCE = 600.0F;
    private static final double DIG_REACH = 4.5;
    private static final double PLACE_REACH = 4.5;
    private static final Direction[] PLACE_ORDER = {
            Direction.DOWN, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP
    };

    private enum PlaceResult { PLACED, AIMING, FAILED }

    private float serverYaw;
    private float serverPitch;
    private boolean placedInWindow;
    private boolean jumpWanted;
    private static final double JUMP_EYE_GAIN = 1.2;

    private BlockPos basePos;
    private Direction pillarDir;
    private BlockPos miningPos;
    private Direction miningFace;
    private int lastSlot = -1;
    private int ticksToBack = 0;
    @Getter private Rotation rotation;

    private final Set<BlockPos> placedThisTick = new HashSet<>();
    private final ConcurrentLinkedQueue<VisualData> visualDatas = new ConcurrentLinkedQueue<>();

    public BlockerModule() {
        addSettings(levitation, jumpForPillar, dig, digFloor, waitCenter, centerMargin, rotate, clientLook,
                blocksPerTick, fromInventory, obsidianSlot, render);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (mc.player == null || mc.level == null) return;

        serverYaw = mc.player.getYRot();
        serverPitch = mc.player.getXRot();
        placedInWindow = false;
        setBase(mc.player.blockPosition());
        rebuild();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        stopMining();
        restoreSlot();
        basePos = null;
        pillarDir = null;
        rotation = null;
        placedThisTick.clear();
        visualDatas.clear();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener rotUpdate = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> {
            if (rotation != null && rotate.is("Vanilla")) {
                applyRotation(rotation);
            }
        }));
        EventListener breaking = HandleBreakingEvent.getInstance().subscribe(new Listener<>(event -> {
            if (miningPos != null) {
                HandleBreakingEvent.getInstance().setCancel(true);
            }
        }));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(this::onWorldRender));

        addEvents(update, rotUpdate, breaking, packet, renderEvent);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;

        if (ticksToBack > 0) {
            ticksToBack--;
        }

        if (ticksToBack == 0) {
            if (lastSlot != -1) {
                restoreSlot();
            }
            rotation = null;
        }

        BlockPos currentPos = mc.player.blockPosition();
        if (basePos == null) {
            setBase(currentPos);
        } else if (!currentPos.equals(basePos)) {
            boolean liftedInPlace = (isLevitating() || !mc.player.onGround())
                    && currentPos.getX() == basePos.getX() && currentPos.getZ() == basePos.getZ()
                    && currentPos.getY() > basePos.getY() && currentPos.getY() <= basePos.getY() + 1;
            if (!liftedInPlace) {
                stopMining();
                setBase(currentPos);
            }
        }

        jumpWanted = false;
        int placed = rebuild();
        if (placed == 0) {
            digStep();
            if (jumpWanted && jumpForPillar.getValue() && miningPos == null && mc.player.onGround()
                    && !isLevitating() && rotation == null) {
                mc.player.jumpFromGround();
            }
        }
    }

    private void setBase(BlockPos pos) {
        basePos = pos;
        pillarDir = choosePillarDir();
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (mc.player == null || mc.level == null || !isEnabled()) return;

        if (event.isSend()) {
            if (event.packet() instanceof ServerboundMovePlayerPacket move && !PacketEvent.getInstance().isCancel()) {
                if (move.hasRotation()) {
                    serverYaw = move.getYRot(serverYaw);
                    serverPitch = move.getXRot(serverPitch);
                }
                placedInWindow = false;
            }
            return;
        }
        if (!event.isReceive()) return;

        if (event.packet() instanceof ClientboundBlockUpdatePacket blockPacket) {
            BlockPos brokenPos = blockPacket.getPos();
            if (isBoxPosition(brokenPos) && blockPacket.getBlockState().canBeReplaced()) {
                mc.execute(() -> {
                    if (isEnabled()) {
                        rebuild();
                    }
                });
            }
        } else if (event.packet() instanceof ClientboundSectionBlocksUpdatePacket sectionPacket) {
            boolean[] hasBrokenBoxBlock = new boolean[]{false};
            sectionPacket.runUpdates((pos, state) -> {
                if (isBoxPosition(pos) && state.canBeReplaced()) {
                    hasBrokenBoxBlock[0] = true;
                }
            });
            if (hasBrokenBoxBlock[0]) {
                mc.execute(() -> {
                    if (isEnabled()) {
                        rebuild();
                    }
                });
            }
        }
    }

    public int rebuild() {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return 0;

        placedThisTick.clear();

        if (basePos == null) {
            setBase(mc.player.blockPosition());
        }

        boolean legit = rotate.is("Vanilla");
        if (legit && placedInWindow) return 0;

        int placedCount = 0;
        int maxPerTick = legit ? 1 : blocksPerTick.getValue().intValue();

        for (BlockPos targetPos : getTargetPositions()) {
            if (placedCount >= maxPerTick) break;
            if (placedThisTick.contains(targetPos)) continue;
            if (!mc.level.getBlockState(targetPos).canBeReplaced()) continue;

            PlaceResult result = tryPlaceBlock(targetPos, legit);
            if (result == PlaceResult.PLACED) {
                placedCount++;
                placedThisTick.add(targetPos);
                if (legit) placedInWindow = true;
            } else if (result == PlaceResult.AIMING) {
                break;
            }
        }
        return placedCount;
    }

    private List<BlockPos> getTargetPositions() {
        List<BlockPos> positions = new ArrayList<>();
        if (basePos == null || mc.level == null) return positions;

        boolean levitating = isLevitating();
        boolean buildPillar = levitation.is("Заготовка") || levitation.is("Всегда крыша")
                || (levitation.is("При эффекте") && levitating);
        boolean buildRoof = levitation.is("Всегда крыша") || (!levitation.is("Выкл") && levitating);

        List<BlockPos> levitationPart = new ArrayList<>();
        if (buildPillar && pillarDir != null) {
            BlockPos side = basePos.relative(pillarDir);
            levitationPart.add(side.above(1));
            levitationPart.add(side.above(2));
        }
        if (buildRoof) {
            levitationPart.add(basePos.above(2));
        }

        if (levitating) positions.addAll(levitationPart);

        BlockPos belowFeet = basePos.below();
        positions.add(belowFeet);

        for (Direction dir : HORIZONTALS) {
            BlockPos feetPos = basePos.relative(dir);
            BlockPos underFeet = feetPos.below();
            if (mc.level.getBlockState(underFeet).canBeReplaced() && !underFeet.equals(belowFeet)) {
                positions.add(underFeet);
            }
            positions.add(feetPos);
        }

        if (!levitating) positions.addAll(levitationPart);

        return positions;
    }

    private Direction choosePillarDir() {
        if (mc.player == null || mc.level == null || basePos == null) return null;
        for (Direction dir : HORIZONTALS) {
            if (isStrong(mc.level.getBlockState(basePos.relative(dir).above(2)))) return dir;
        }
        for (Direction dir : HORIZONTALS) {
            if (isStrong(mc.level.getBlockState(basePos.relative(dir).above(1)))) return dir;
        }
        return mc.player.getDirection().getOpposite();
    }

    private boolean isLevitating() {
        return mc.player != null && mc.player.hasEffect(MobEffects.LEVITATION);
    }

    private boolean isBoxPosition(BlockPos pos) {
        if (pos == null || basePos == null) return false;
        return getTargetPositions().contains(pos);
    }


    private static boolean isStrong(BlockState state) {
        return state.getBlock().getExplosionResistance() >= STRONG_RESISTANCE;
    }

    private boolean isDiggable(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()) return false;
        if (isStrong(state) || state.getDestroySpeed(mc.level, pos) < 0) return false;
        return mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf(pos)) <= DIG_REACH * DIG_REACH;
    }

    private BlockPos findDigTarget() {
        if (basePos == null) return null;
        BlockPos floor = basePos.below();
        for (BlockPos pos : getTargetPositions()) {
            if (pos.equals(floor) && !digFloor.getValue()) continue;
            if (isDiggable(pos)) return pos;
        }
        return null;
    }

    private void digStep() {
        if (!dig.getValue() || !HotbarSupply.has(Items.OBSIDIAN, fromInventory.getValue())) {
            stopMining();
            return;
        }

        if (miningPos != null && (!isDiggable(miningPos) || !isBoxPosition(miningPos))) {
            stopMining();
        }

        BlockPos target = miningPos != null ? miningPos : findDigTarget();
        if (target == null) return;

        Direction face = target.equals(miningPos) && miningFace != null ? miningFace : findDigFace(target);
        BlockState state = mc.level.getBlockState(target);

        int tool = findBestTool(state);
        if (lastSlot == -1) {
            lastSlot = mc.player.getInventory().getSelectedSlot();
        }
        ticksToBack = 2;
        if (tool != -1) {
            mc.player.getInventory().setSelectedSlot(tool);
        }

        Rotation rot = calculateRotation(getFacePoint(target, face));
        rotation = rot;
        if (rotate.is("Vanilla")) {
            applyRotation(rot);
        }

        miningPos = target;
        miningFace = face;
        if (mc.gameMode.continueDestroyBlock(target, face)) {
            mc.player.swing(InteractionHand.MAIN_HAND);
        }

        if (render.getValue() && !hasVisual(target)) {
            VisualData visualData = new VisualData(target, 0.85F, 0.15F, 0.15F);
            visualData.animation.setValue(1);
            visualDatas.add(visualData);
        }
    }

    private void stopMining() {
        if (miningPos != null && mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
        }
        miningPos = null;
        miningFace = null;
    }

    private Direction findDigFace(BlockPos pos) {
        Vec3 eyes = mc.player.getEyePosition();
        Direction best = null;
        double bestDist = Double.MAX_VALUE;
        for (Direction face : Direction.values()) {
            if (!mc.level.getBlockState(pos.relative(face)).canBeReplaced()) continue;
            double dist = eyes.distanceToSqr(getFacePoint(pos, face));
            if (dist < bestDist) {
                bestDist = dist;
                best = face;
            }
        }
        if (best != null) return best;

        Vec3 diff = eyes.subtract(Vec3.atCenterOf(pos));
        double ax = Math.abs(diff.x), ay = Math.abs(diff.y), az = Math.abs(diff.z);
        if (ax >= ay && ax >= az) return diff.x > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return diff.y > 0 ? Direction.UP : Direction.DOWN;
        return diff.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private int findBestTool(BlockState state) {
        int bestSlot = -1;
        float bestSpeed = 1.0F;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            float speed = stack.getDestroySpeed(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }


    private boolean isPlayerNearTarget(BlockPos placePos) {
        if (mc.player == null) return false;

        if (basePos != null && placePos.equals(basePos.below())) {
            return false;
        }

        AABB targetBox = new AABB(placePos);

        if (basePos != null && placePos.getX() == basePos.getX() && placePos.getZ() == basePos.getZ()) {
            return mc.player.getBoundingBox().intersects(targetBox);
        }

        float margin = centerMargin.getValue();
        AABB playerBox = mc.player.getBoundingBox().inflate(margin, 0.0, margin);

        boolean horizOverlap = playerBox.minX < targetBox.maxX && playerBox.maxX > targetBox.minX &&
                               playerBox.minZ < targetBox.maxZ && playerBox.maxZ > targetBox.minZ;

        if (horizOverlap) {
            return targetBox.maxY > mc.player.getY() - 0.5 && targetBox.minY < mc.player.getY() + mc.player.getBbHeight() + 0.5;
        }

        return false;
    }

    private PlaceResult tryPlaceBlock(BlockPos placePos, boolean legit) {
        if (!mc.level.getBlockState(placePos).canBeReplaced()) return PlaceResult.FAILED;

        AABB box = new AABB(placePos);

        if (waitCenter.getValue()) {
            if (isPlayerNearTarget(placePos)) {
                return PlaceResult.FAILED;
            }
        } else if (!placePos.equals(basePos.below()) && mc.player.getBoundingBox().intersects(box)) {
            return PlaceResult.FAILED;
        }

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == null || !entity.isAlive() || entity instanceof ItemEntity || entity == mc.player) continue;
            if (entity.getBoundingBox().intersects(box)) {
                return PlaceResult.FAILED;
            }
        }

        PlaceData placeData = findPlaceData(placePos);
        if (placeData == null) return PlaceResult.FAILED;

        if (!HotbarSupply.has(Items.OBSIDIAN, fromInventory.getValue())) return PlaceResult.FAILED;

        Rotation rot = calculateRotation(placeData.hitVec);
        this.rotation = rot;
        ticksToBack = Math.max(ticksToBack, 2);

        Vec3 hitVec = placeData.hitVec;
        if (rotate.is("Vanilla")) {
            applyRotation(rot);
        }
        if (legit) {
            hitVec = serverRayHit(placeData);
            if (hitVec == null) return PlaceResult.AIMING;
        }

        int obsSlot = HotbarSupply.ensure(Items.OBSIDIAN, fromInventory.getValue(), obsidianSlot.getValue().intValue() - 1);
        if (obsSlot == -1) return PlaceResult.FAILED;

        if (lastSlot == -1) {
            lastSlot = mc.player.getInventory().getSelectedSlot();
        }
        ticksToBack = 2;
        mc.player.getInventory().setSelectedSlot(obsSlot);

        BlockHitResult hitResult = new BlockHitResult(
                hitVec,
                placeData.side,
                placeData.neighbourPos,
                false
        );

        InteractionResult result = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hitResult);
        if (result.consumesAction()) {
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (render.getValue() && !hasVisual(placePos)) {
                VisualData visualData = new VisualData(placePos, 0.45F, 0.2F, 0.9F);
                visualData.animation.setValue(1);
                visualDatas.add(visualData);
            }
            return PlaceResult.PLACED;
        }

        return PlaceResult.FAILED;
    }

    private Vec3 serverRayHit(PlaceData placeData) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 look = Vec3.directionFromRotation(serverPitch, serverYaw);
        Vec3 end = eyes.add(look.scale(PLACE_REACH));
        Optional<Vec3> hit = new AABB(placeData.neighbourPos).clip(eyes, end);
        if (hit.isEmpty()) return null;
        Vec3 point = hit.get();
        return hitSide(point, placeData.neighbourPos) == placeData.side ? point : null;
    }

    private static Direction hitSide(Vec3 hit, BlockPos pos) {
        double dx = hit.x - (pos.getX() + 0.5);
        double dy = hit.y - (pos.getY() + 0.5);
        double dz = hit.z - (pos.getZ() + 0.5);
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax > ay && ax > az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay > az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static boolean isFaceVisible(BlockPos pos, Direction face, Vec3 eyes) {
        return switch (face) {
            case UP -> eyes.y > pos.getY() + 1;
            case DOWN -> eyes.y < pos.getY();
            case EAST -> eyes.x > pos.getX() + 1;
            case WEST -> eyes.x < pos.getX();
            case SOUTH -> eyes.z > pos.getZ() + 1;
            case NORTH -> eyes.z < pos.getZ();
        };
    }

    private void applyRotation(Rotation rot) {
        if (rotate.is("Vanilla")) {
            if (clientLook.getValue() && mc.player != null) {
                mc.player.setYRot(rot.getYaw());
                mc.player.setXRot(rot.getPitch());
            }
            RotationStrategy strategy = new RotationStrategy(new InstantRotation(), clientLook.getValue())
                    .clientLook(clientLook.getValue());
            strategy.ticksUntilReset(2);
            RotationManager.getInstance().addRotation(rot, strategy, TaskPriority.HIGH, this);
        }
    }

    private PlaceData findPlaceData(BlockPos targetPos) {
        Vec3 eyes = mc.player.getEyePosition();
        for (Direction dir : PLACE_ORDER) {
            BlockPos neighbour = targetPos.relative(dir);
            if (!isValidSupport(neighbour, mc.level.getBlockState(neighbour))) continue;

            Direction face = dir.getOpposite();
            if (!isFaceVisible(neighbour, face, eyes)) {
                if (face == Direction.UP && neighbour.getY() + 1 < eyes.y + JUMP_EYE_GAIN) {
                    jumpWanted = true;
                }
                continue;
            }

            Vec3 hitVec = getFacePoint(neighbour, face);
            if (eyes.distanceToSqr(hitVec) > PLACE_REACH * PLACE_REACH) continue;
            return new PlaceData(neighbour, face, hitVec);
        }
        return null;
    }

    private boolean isValidSupport(BlockPos pos, BlockState state) {
        if (placedThisTick.contains(pos)) return true;
        if (pos.equals(miningPos)) return false;
        if (state.isAir()) return false;
        if (state.canBeReplaced()) return false;
        return state.getFluidState().isEmpty();
    }

    private void restoreSlot() {
        if (lastSlot != -1 && mc.player != null) {
            mc.player.getInventory().setSelectedSlot(lastSlot);
            lastSlot = -1;
        }
    }

    private static Vec3 getFacePoint(BlockPos pos, Direction face) {
        return new Vec3(
                pos.getX() + 0.5 + face.getStepX() * 0.5,
                pos.getY() + 0.5 + face.getStepY() * 0.5,
                pos.getZ() + 0.5 + face.getStepZ() * 0.5
        );
    }

    private Rotation calculateRotation(Vec3 toVec) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 diff = toVec.subtract(eyes);
        double distance = Math.hypot(diff.x, diff.z);
        float yaw = (float) (Mth.atan2(diff.z, diff.x) * 180.0 / Math.PI) - 90.0F;
        float pitch = (float) (-(Mth.atan2(diff.y, distance) * 180.0 / Math.PI));
        return new Rotation(Mth.wrapDegrees(yaw), Mth.clamp(pitch, -90.0F, 90.0F));
    }

    private boolean hasVisual(BlockPos blockPos) {
        for (VisualData visualData : visualDatas) {
            if (visualData.blockPos.equals(blockPos)) {
                return true;
            }
        }
        return false;
    }

    private void onWorldRender(Render3DEvent.Render3DEventData event) {
        if (mc.level == null || mc.player == null || visualDatas.isEmpty() || !render.getValue()) return;

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
            line(buffer, matrix, x2, y1, z2, x2, y2, z2, argb);
            line(buffer, matrix, x1, y1, z2, x1, y2, z2, argb);
        } else {
            quad(buffer, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, argb);
            quad(buffer, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, argb);
            quad(buffer, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, argb);
            quad(buffer, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, argb);
            quad(buffer, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, argb);
            quad(buffer, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, argb);
        }
    }

    private static void line(VertexConsumer b, Matrix4f m, float x1, float y1, float z1,
                             float x2, float y2, float z2, int argb) {
        b.addVertex(m, x1, y1, z1).setColor(argb);
        b.addVertex(m, x2, y2, z2).setColor(argb);
    }

    private static void quad(VertexConsumer b, Matrix4f m,
                             float x1, float y1, float z1,
                             float x2, float y2, float z2,
                             float x3, float y3, float z3,
                             float x4, float y4, float z4, int argb) {
        b.addVertex(m, x1, y1, z1).setColor(argb);
        b.addVertex(m, x2, y2, z2).setColor(argb);
        b.addVertex(m, x3, y3, z3).setColor(argb);
        b.addVertex(m, x4, y4, z4).setColor(argb);
    }

    private record PlaceData(BlockPos neighbourPos, Direction side, Vec3 hitVec) {}

    private static class VisualData {
        private final BlockPos blockPos;
        private final Fade animation = new Fade();
        private final float r;
        private final float g;
        private final float b;

        public VisualData(BlockPos blockPos, float r, float g, float b) {
            this.blockPos = blockPos;
            this.r = r;
            this.g = g;
            this.b = b;
        }
    }

    private static class Fade {
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
