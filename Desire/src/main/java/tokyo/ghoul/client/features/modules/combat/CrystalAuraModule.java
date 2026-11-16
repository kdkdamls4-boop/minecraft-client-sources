package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector2f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.move.MotionEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.event.events.player.world.HandleBreakingEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.client.features.modules.combat.crystal.BlockBreaker;
import tokyo.ghoul.client.features.modules.combat.crystal.CachedBlockView;
import tokyo.ghoul.client.features.modules.combat.crystal.CrystalPlanner;
import tokyo.ghoul.client.features.modules.combat.crystal.CrystalRotations;
import tokyo.ghoul.client.features.modules.combat.crystal.CrystalUtil;
import tokyo.ghoul.client.features.modules.combat.crystal.CrystalVisuals;
import tokyo.ghoul.client.features.modules.combat.crystal.ExplosionDamage;
import tokyo.ghoul.client.features.modules.combat.crystal.KnockbackPrediction;
import tokyo.ghoul.client.features.modules.combat.crystal.MatrixPlacement;
import tokyo.ghoul.client.features.modules.combat.crystal.TargetMovementTracker;
import tokyo.ghoul.client.features.modules.combat.crystal.TargetSimulation;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ModuleRegister(name = "Crystal Aura", description = "Автоматическая установка и взрыв кристаллов", category = Category.COMBAT)
public class CrystalAuraModule extends Module {
    @Getter private static final CrystalAuraModule instance = new CrystalAuraModule();

    private final SliderSetting targetRange = new SliderSetting("Радиус цели").range(4.0F, 12.0F).step(0.5F).value(8.0F);
    private final SliderSetting placeRange = new SliderSetting("Дальность установки").range(3.0F, 6.0F).step(0.1F).value(4.5F);
    private final SliderSetting explodeRange = new SliderSetting("Дальность взрыва").range(1.0F, 6.0F).step(0.1F).value(3.0F);
    private final SliderSetting minDamage = new SliderSetting("Мин. урон врагу").range(1.0F, 20.0F).step(0.5F).value(3.0F);
    private final SliderSetting maxSelfDamage = new SliderSetting("Макс. урон себе").range(0.0F, 20.0F).step(0.5F).value(8.0F);
    private final SliderSetting delay = new SliderSetting("Задержка в тиках").range(0.0F, 5.0F).step(1.0F).value(0.0F);
    private final SliderSetting predictTicks = new SliderSetting("Предикт тиков").range(0.0F, 4.0F).step(1.0F).value(2.0F);
    private final BooleanSetting placeObsidian = new BooleanSetting("Ставить обсидиан").value(true);
    private final BooleanSetting legit = new BooleanSetting("Легитный").value(false);
    private final BooleanSetting antiSuicide = new BooleanSetting("Анти-суицид").value(true);
    private final BooleanSetting autoDig = new BooleanSetting("Авто подкоп").value(true);
    private final BooleanSetting digThroughWalls = new BooleanSetting("Копать сквозь стены").value(true);
    private final BooleanSetting placeThroughWalls = new BooleanSetting("Ставить сквозь стены").value(true);
    private final BooleanSetting explodeThroughWalls = new BooleanSetting("Взрывать сквозь стены").value(true);
    private final BooleanSetting matrix = new BooleanSetting("Обход Matrix").value(false);
    private final BooleanSetting render = new BooleanSetting("Визуализация").value(true);
    private final BooleanSetting saveSelf = new BooleanSetting("Сейвить себя").value(false);
    private final BooleanSetting emulateDamage = new BooleanSetting("Эмуляция урона").value(false);

    private static final float PLACE_OVER_EXPLODE_FACTOR = 1.5F;

    private final BlockBreaker breaker = BlockBreaker.getInstance();
    private final CrystalVisuals visuals = new CrystalVisuals();
    private final TargetMovementTracker movementTracker = new TargetMovementTracker();

    private final Map<BlockPos, Integer> blacklist = new HashMap<>();
    private final Map<BlockPos, Integer> ownObsidian = new HashMap<>();
    private final Map<BlockPos, Integer> reappeared = new HashMap<>();
    private final Map<BlockPos, Integer> pendingBroken = new ConcurrentHashMap<>();
    private final Map<BlockPos, BlockState> serverStates = new ConcurrentHashMap<>();
    private final Deque<PendingDamage> pendingSelfDamage = new ArrayDeque<>();

    private int awaitTicks;
    private int nonAwaitStreak;
    private BlockPos committedBase;
    private int committedTick = -100;
    private BlockPos stallPos;
    private int stallCount;
    private BlockPos lastPlacedPos;
    private int lastPlacedTick = -100;
    private int savedSlot = -1;
    private int idleTicks;
    private int digConfirm;
    private BlockPos digConfirmPos;
    private long lastActionTime;
    private int lastExplodeTick = -100;
    private volatile int lastHealthTick = -100;
    private int lastUseTick = -100;

    private BlockPos placedCrystal;
    private int placedCrystalTick = -100;

    private BlockPos queuedPos;
    private Item queuedItem;
    private ExplosionDamage.PlaceFace queuedFace;
    private int queuedTick = -100;

    private TargetSimulation knockback;
    private int knockbackStartTick;
    private int knockbackLastTick;
    private double knockbackSpeed;
    private double knockbackDirX;
    private double knockbackDirZ;

    public CrystalAuraModule() {
        addSettings(targetRange, placeRange, explodeRange, minDamage, maxSelfDamage, delay, predictTicks, placeObsidian,
                legit, antiSuicide, autoDig, digThroughWalls, placeThroughWalls, explodeThroughWalls, matrix, render,
                saveSelf, emulateDamage);
    }

    public static boolean isActive() {
        return instance.isEnabled();
    }

    public BlockPos getMiningPos() {
        return isEnabled() && breaker.isOwnedBy(this) ? breaker.getPos() : null;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        ExplosionDamage.setEmulateDamage(false);
        MatrixPlacement.setActive(false);
        restoreSlot();
        breaker.release(this);
        CrystalRotations.reset();
        visuals.clear();
        placedCrystal = null;
        clearQueue();
        movementTracker.reset();
        knockback = null;
        idleTicks = 0;
        digConfirm = 0;
        blacklist.clear();
        ownObsidian.clear();
        reappeared.clear();
        awaitTicks = 0;
        pendingBroken.clear();
        serverStates.clear();
        pendingSelfDamage.clear();
        committedBase = null;
        stallPos = null;
        stallCount = 0;
    }

    @Override
    public void onEvent() {
        EventListener tick = TickEvent.getInstance().subscribe(new Listener<>(event -> breaker.onClientTick()));
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(event -> onInput()));
        EventListener motion = MotionEvent.getInstance().subscribe(new Listener<>(event -> onPostInput()));
        EventListener attack = AttackEvent.getInstance().subscribe(new Listener<>(event -> onAttack(event.entity())));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        EventListener breaking = HandleBreakingEvent.getInstance().subscribe(new Listener<>(event -> {
            if (breaker.isOwnedBy(this)) HandleBreakingEvent.getInstance().setCancel(true);
        }));
        EventListener render3d = Render3DEvent.getInstance().subscribe(new Listener<>(this::onRender));
        addEvents(tick, input, motion, attack, packet, breaking, render3d);
    }


    private void onInput() {
        ExplosionDamage.setEmulateDamage(emulateDamage.getValue());
        MatrixPlacement.setActive(matrix.getValue());
        if (mc.player == null || mc.level == null || mc.gameMode == null || mc.player.isSpectator()) return;

        breaker.updateState();
        if (render.getValue()) {
            visuals.tick(targetRange.getValue(), true);
        } else {
            visuals.clear();
        }

        if (WebTrapModule.getInstance().isPlacing()) return;
        if (!legit.getValue()) restoreSlot();

        pruneExpired();
        handleBrokenBlocks();
        updateKnockback();

        List<Player> targets = targets();
        if (targets.isEmpty()) {
            movementTracker.reset();
            knockback = null;
            idle();
            return;
        }

        Vec3 eye = mc.player.getEyePosition();
        CrystalPlanner.Settings settings = settings();
        boolean canAct = (knockback != null || System.currentTimeMillis() - lastActionTime > delay.getValue().intValue() * 50L)
                && !breaker.sentDestroyPacketThisTick();

        Player target = null;
        AABB targetBox = null;
        CrystalPlanner.Plan explosion = null;
        CrystalPlanner.Plan placement = null;

        int candidates = Math.min(targets.size(), 3);
        for (int i = 0; i < candidates; i++) {
            Player candidate = targets.get(i);
            boolean tracked = movementTracker.isTracking(candidate);
            if (tracked) movementTracker.track(candidate);

            AABB box = predictedBox(candidate);
            CrystalPlanner.Plan candidateExplosion = canAct ? CrystalPlanner.findExplosion(candidate, box, eye, settings) : null;
            CrystalPlanner.DigContext dig = candidateExplosion == null
                    ? digContext(tracked ? movementTracker.speed() : 0.0F, !isKnockbackTarget(candidate))
                    : null;
            CrystalPlanner.Plan candidatePlacement = dig == null ? null : CrystalPlanner.findPlacement(candidate, box, eye, settings, dig);

            boolean found = candidateExplosion != null || candidatePlacement != null;
            if (i == 0 || found) {
                target = candidate;
                targetBox = box;
                explosion = candidateExplosion;
                placement = candidatePlacement;
            }
            if (found) break;
        }

        if (!movementTracker.isTracking(target)) movementTracker.track(target);

        if (canAct) {
            if (explosion != null) {
                CrystalPlanner.DigContext dig = digContext(movementTracker.speed(), !isKnockbackTarget(target));
                placement = CrystalPlanner.findPlacement(target, targetBox, eye, settings, dig);
                if (shouldPlaceInstead(placement, explosion, eye)) {
                    idleTicks = 0;
                    stopMining();
                    if (executePlan(placement, true)) return;
                }

                stopMining();
                if (explode(explosion.crystal())) lastActionTime = System.currentTimeMillis();
                return;
            }

            if (explodePlacedCrystal(target, targetBox, settings) || placeSafetyWall(target)) {
                stopMining();
                lastActionTime = System.currentTimeMillis();
                return;
            }
        }

        if (placement == null) {
            onNoPlan(target);
            return;
        }

        if (placement.action() == CrystalPlanner.Action.DIG) {
            CrystalPlanner.Plan combo = comboPlacement(target, eye, settings);
            if (combo != null) {
                idleTicks = 0;
                stopMining();
                restoreSlot();
                if (combo.action() == CrystalPlanner.Action.PLACE_OBSIDIAN) executePlan(combo, canAct);
                return;
            }
        }

        idleTicks = 0;
        if (placement.action() == CrystalPlanner.Action.AWAIT) {
            awaitTicks++;
            nonAwaitStreak = 0;
        } else if (++nonAwaitStreak >= 20) {
            awaitTicks = 0;
        }

        if (placement.clears() != null) {
            committedBase = placement.clears();
            committedTick = mc.player.tickCount;
        } else if (placement.base() != null) {
            committedBase = placement.base();
            committedTick = mc.player.tickCount;
        }

        if (placement.action() == CrystalPlanner.Action.DIG) {
            restoreSlot();
            if (confirmDig(placement.pos())) startMining(placement.pos());
        } else {
            stopMining();
            if (placement.action() == CrystalPlanner.Action.PLACE_OBSIDIAN || placement.action() == CrystalPlanner.Action.PLACE_CRYSTAL) {
                executePlan(placement, canAct);
            }
        }
    }

    private void onPostInput() {
        if (queuedItem == null) return;

        BlockPos pos = queuedPos;
        Item item = queuedItem;
        ExplosionDamage.PlaceFace face = queuedFace;
        int tick = queuedTick;
        clearQueue();

        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.player.tickCount != tick) return;

        if (face == null) {
            placeQueuedCrystal(pos);
        } else {
            placeQueuedBlock(pos, item, face);
        }
    }


    private boolean executePlan(CrystalPlanner.Plan plan, boolean canAct) {
        if (!canAct) return false;

        BlockPos pos = plan.pos();
        if (isPendingBroken(pos)) {
            recordStall(pos, false);
            return false;
        }
        if (plan.action() == CrystalPlanner.Action.PLACE_CRYSTAL && isCrystalJustPlaced(pos)) return false;

        boolean queued;
        if (plan.action() == CrystalPlanner.Action.PLACE_OBSIDIAN) {
            restoreSlot();
            queued = queueBlock(pos, Items.OBSIDIAN);
        } else {
            queued = queueCrystal(pos);
        }

        recordStall(pos, queued);
        if (queued) lastActionTime = System.currentTimeMillis();
        return queued;
    }

    private boolean queueCrystal(BlockPos base) {
        if (CrystalUtil.hotbarSlot(Items.END_CRYSTAL) == -1) return false;
        if (base.equals(lastPlacedPos) && mc.player.tickCount - lastPlacedTick < 1) return false;

        Vector2f rotation = matrix.getValue()
                ? MatrixPlacement.crystalRotation(base, placeRange.getValue(), placeThroughWalls.getValue())
                : crystalPlaceRotation(base);
        if (rotation == null || !CrystalRotations.request(this, rotation)) return false;

        queue(base, Items.END_CRYSTAL, null);
        return true;
    }

    private boolean queueBlock(BlockPos pos, Item item) {
        if (CrystalUtil.hotbarSlot(item) == -1) return false;

        BlockPlacement placement = blockPlacement(pos);
        if (placement == null || !CrystalRotations.request(this, placement.rotation())) return false;

        queue(pos, item, placement.face());
        return true;
    }

    private void placeQueuedCrystal(BlockPos base) {
        if (mc.player.tickCount == lastUseTick) return;
        int slot = CrystalUtil.hotbarSlot(Items.END_CRYSTAL);
        if (slot == -1) return;

        BlockHitResult hit = placementHit(base, null, Vec3.atCenterOf(base));
        if (hit == null) return;

        boolean holding = legit.getValue();
        int previous = mc.player.getInventory().getSelectedSlot();
        if (holding) {
            holdCrystal();
        } else {
            mc.player.getInventory().setSelectedSlot(slot);
        }

        boolean placed;
        try {
            placed = use(hit);
        } finally {
            if (!holding) mc.player.getInventory().setSelectedSlot(previous);
        }

        if (placed) {
            placedCrystal = base.above();
            if (base.equals(committedBase)) committedBase = null;
            placedCrystalTick = mc.player.tickCount;
        }
    }

    private void placeQueuedBlock(BlockPos pos, Item item, ExplosionDamage.PlaceFace face) {
        if (mc.player.tickCount == lastUseTick) return;
        int slot = CrystalUtil.hotbarSlot(item);
        if (slot == -1) return;

        BlockHitResult hit = placementHit(face.neighbor(), face.side(), face.aim());
        if (hit == null || !hit.getBlockPos().relative(hit.getDirection()).equals(pos)) {
            recordStall(pos, false);
            return;
        }

        int previous = mc.player.getInventory().getSelectedSlot();
        mc.player.getInventory().setSelectedSlot(slot);
        boolean placed;
        try {
            placed = use(hit);
        } finally {
            mc.player.getInventory().setSelectedSlot(previous);
        }

        if (!placed) {
            recordStall(pos, false);
            return;
        }

        CachedBlockView.INSTANCE.invalidate();
        lastPlacedPos = pos;
        lastPlacedTick = mc.player.tickCount;
        if (item == Items.OBSIDIAN) ownObsidian.put(pos.immutable(), mc.player.tickCount + 200);
        if (render.getValue()) visuals.onBlockPlaced(pos);
    }

    private boolean use(BlockHitResult hit) {
        if (mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit) != InteractionResult.SUCCESS) return false;
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastUseTick = mc.player.tickCount;
        return true;
    }

    private BlockHitResult placementHit(BlockPos neighbor, Direction side, Vec3 aim) {
        if (matrix.getValue() && !placeThroughWalls.getValue()) return MatrixPlacement.hitFromAim(neighbor, side);

        Vec3 eye = mc.player.getEyePosition();
        double distance = Math.min(eye.distanceTo(aim) + 1.0, mc.player.blockInteractionRange());
        Vec3 end = eye.add(CrystalUtil.direction(CrystalRotations.aimYaw(), CrystalRotations.aimPitch()).scale(distance));
        VoxelShape shape = mc.level.getBlockState(neighbor).getShape(mc.level, neighbor);
        if (shape.isEmpty()) return null;

        BlockHitResult hit = shape.clip(eye, end, neighbor);
        return hit != null && hit.getBlockPos().equals(neighbor) ? hit : null;
    }

    private boolean explode(EndCrystal crystal) {
        return attackCrystal(crystal, explodeRange.getValue());
    }

    private boolean attackCrystal(EndCrystal crystal, float range) {
        if (crystal == null || crystal.isRemoved()) return false;
        if (mc.player.tickCount == lastExplodeTick) return false;
        if (isTotemGuarded(crystal)) return false;

        Vector2f rotation = crystalAttackRotation(crystal, range);
        if (rotation == null || !CrystalRotations.request(this, rotation)) return false;
        if (!aimHitsCrystal(crystal, range)) return false;

        if (legit.getValue()) holdCrystal();
        mc.gameMode.attack(mc.player, crystal);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastExplodeTick = mc.player.tickCount;
        addPendingSelfDamage(ExplosionDamage.damage(crystal.position(), mc.player));
        return true;
    }

    private boolean explodePlacedCrystal(Player target, AABB targetBox, CrystalPlanner.Settings settings) {
        if (placedCrystal == null) return false;

        if (mc.player.tickCount - placedCrystalTick > 20) {
            placedCrystal = null;
            return false;
        }

        EndCrystal crystal = crystalAt(placedCrystal);
        if (crystal == null || crystal.isRemoved()) {
            int age = mc.player.tickCount - placedCrystalTick;
            if (age < 0 || age >= 4) placedCrystal = null;
            return false;
        }

        float range = explodeRange.getValue();
        if (ExplosionDamage.distanceSq(mc.player.getEyePosition(), crystal.getBoundingBox()) > range * range) {
            placedCrystal = null;
            return false;
        }

        float self = ExplosionDamage.damage(crystal.position(), mc.player);
        float enemy = ExplosionDamage.damage(crystal.position(), target, targetBox);
        if (!CrystalPlanner.isSelfDamageAcceptable(self, enemy, settings, placedCrystal.below())) {
            placedCrystal = null;
            return false;
        }

        boolean exploded = attackCrystal(crystal, range);
        if (exploded) placedCrystal = null;
        return exploded;
    }

    private boolean placeSafetyWall(Player target) {
        if (!saveSelf.getValue() || CrystalUtil.hotbarSlot(Items.OBSIDIAN) == -1) return false;

        double dx = target.getX() - mc.player.getX();
        double dz = target.getZ() - mc.player.getZ();
        if (Math.sqrt(dx * dx + dz * dz) > placeRange.getValue() + 1.5) return false;
        if (Math.abs(target.getY() - mc.player.getY()) > 1.0) return false;

        Direction side = Math.abs(dx) > Math.abs(dz)
                ? (dx > 0.0 ? Direction.EAST : Direction.WEST)
                : (dz > 0.0 ? Direction.SOUTH : Direction.NORTH);
        BlockPos pos = mc.player.blockPosition().relative(side);
        if (!mc.level.getBlockState(pos).canBeReplaced() || ExplosionDamage.obsidianObstructed(pos)) return false;

        restoreSlot();
        queueBlock(pos, Items.OBSIDIAN);
        return true;
    }


    private void startMining(BlockPos pos) {
        if (!breaker.mine(this, pos)) return;

        BlockBreaker.State state = breaker.getState();
        if (state == BlockBreaker.State.STUCK) {
            blacklist(pos);
            breaker.release(this);
        } else if (state == BlockBreaker.State.MINING && render.getValue()) {
            visuals.onMining(pos, breaker.getProgress());
        }
    }

    private void stopMining() {
        digConfirm = 0;
        digConfirmPos = null;
        if (!breaker.isOwnedBy(this)) return;

        if (breaker.willFinish()) {
            breaker.keepAlive(this);
        } else {
            breaker.release(this);
        }
    }

    private boolean confirmDig(BlockPos pos) {
        if (breaker.isOwnedBy(this)) return true;
        if (!pos.equals(digConfirmPos)) {
            digConfirmPos = pos;
            digConfirm = 1;
        } else {
            digConfirm++;
        }
        return digConfirm >= 2;
    }

    private boolean continueUsefulMining() {
        if (!breaker.isOwnedBy(this)) return false;

        BlockPos pos = breaker.getPos();
        if (pos == null || isBlacklisted(pos)) return false;
        if (!ExplosionDamage.isBreakable(mc.level.getBlockState(pos)) || ExplosionDamage.nearFluid(pos)) return false;

        float range = placeRange.getValue();
        if (ExplosionDamage.distanceSq(mc.player.getEyePosition(), pos) > range * range) return false;

        startMining(pos);
        return true;
    }

    private void onNoPlan(Player target) {
        idleTicks++;
        if (idleTicks <= 6 && !isKnockbackTarget(target)) {
            if (continueUsefulMining()) return;

            BlockPos mining = breaker.isOwnedBy(this) ? breaker.getPos() : null;
            if (mining != null && ExplosionDamage.isBreakable(mc.level.getBlockState(mining))) {
                breaker.keepAlive(this);
                return;
            }
        }
        idle();
    }

    private void idle() {
        idleTicks = 0;
        digConfirm = 0;
        restoreSlot();
        breaker.release(this);
    }

    private void handleBrokenBlocks() {
        if (breaker.isOwnedBy(this) && breaker.getState() == BlockBreaker.State.BROKEN) {
            BlockPos pos = breaker.getPos().immutable();
            breaker.release(this);
            if (render.getValue() && visuals.isMining(pos)) {
                visuals.onBlockBroken(pos);
                visuals.clearMining();
            }

            BlockState serverState = serverStates.remove(pos);
            if (serverState == null || ExplosionDamage.isBreakable(serverState)) {
                pendingBroken.put(pos, mc.player.tickCount);
                if (serverState != null) serverStates.put(pos, serverState);
            }
        }

        if (pendingBroken.isEmpty()) return;

        int now = mc.player.tickCount;
        Iterator<Map.Entry<BlockPos, Integer>> iterator = pendingBroken.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Integer> entry = iterator.next();
            BlockPos pos = entry.getKey();
            BlockState state = serverStates.remove(pos);
            if (state == null) state = reappearedState(pos, now);

            int age = now - entry.getValue();
            if (state != null || age < 0 || age > 20) {
                iterator.remove();
                reappeared.remove(pos);
                if (state != null && ExplosionDamage.isBreakable(state)) blacklist(pos);
            }
        }
    }

    private BlockState reappearedState(BlockPos pos, int now) {
        BlockState state = mc.level.getBlockState(pos);
        if (!ExplosionDamage.isBreakable(state)) {
            reappeared.remove(pos);
            return null;
        }
        int firstSeen = reappeared.computeIfAbsent(pos, key -> now);
        return now - firstSeen >= 2 ? state : null;
    }

    private void onServerBlock(BlockPos pos, BlockState state) {
        if (pendingBroken.containsKey(pos) || pos.equals(breaker.getPos())) {
            serverStates.put(pos.immutable(), state);
        }
    }


    private CrystalPlanner.Settings settings() {
        prunePendingSelfDamage();
        float selfLimit = antiSuicide.getValue()
                ? Math.max(0.0F, mc.player.getHealth() + mc.player.getAbsorptionAmount() - 1.0F - pendingSelfDamage())
                : Float.MAX_VALUE;

        return new CrystalPlanner.Settings(
                placeRange.getValue(),
                explodeRange.getValue(),
                minDamage.getValue(),
                maxSelfDamage.getValue(),
                selfLimit,
                CrystalUtil.hotbarSlot(Items.END_CRYSTAL) != -1,
                placeObsidian.getValue() && CrystalUtil.hotbarSlot(Items.OBSIDIAN) != -1,
                placeThroughWalls.getValue(),
                digThroughWalls.getValue() && (!matrix.getValue() || MatrixPlacement.hasFastPickaxe()),
                explodeThroughWalls.getValue(),
                CrystalUtil.friends(targetRange.getValue() + 8.0F),
                matrix.getValue(),
                committedBase());
    }

    private CrystalPlanner.DigContext digContext(float targetStep, boolean allowed) {
        boolean owned = breaker.isOwnedBy(this);
        return new CrystalPlanner.DigContext(
                allowed && autoDig.getValue(),
                blacklist.keySet(),
                ownObsidian.keySet(),
                owned ? breaker.getPos() : null,
                owned ? breaker.getProgress() : 0.0F,
                targetStep,
                committedBase(),
                pendingBroken.keySet(),
                awaitTicks);
    }

    private boolean shouldPlaceInstead(CrystalPlanner.Plan placement, CrystalPlanner.Plan explosion, Vec3 eye) {
        if (placement == null || placement.action() != CrystalPlanner.Action.PLACE_CRYSTAL) return false;

        float gain = placement.enemyDamage() - explosion.enemyDamage();
        if (gain < 2.0F || placement.enemyDamage() < explosion.enemyDamage() * PLACE_OVER_EXPLODE_FACTOR) return false;

        float range = explodeRange.getValue();
        return ExplosionDamage.distanceSqToCrystal(eye, ExplosionDamage.crystalPos(placement.pos())) <= range * range;
    }

    private CrystalPlanner.Plan comboPlacement(Player target, Vec3 eye, CrystalPlanner.Settings settings) {
        if (!isAuraTarget(target) || target.isBlocking() || isKnockbackTarget(target)) return null;
        if (CrystalUtil.eyeDistance(target) > AuraModule.getInstance().getAttackDistance()) return null;
        if (isInCobweb(target.getBoundingBox())) return null;

        Vec3 position = mc.player.position();
        TargetSimulation simulation = TargetSimulation.of(target, TargetSimulation.GuessedInput.guess(target));
        simulation.velocity = KnockbackPrediction.apply(target, simulation.velocity, simulation.onGround, position,
                KnockbackPrediction.yawTowards(position, target), KnockbackPrediction.strength(mc.player.isSprinting()));
        for (int i = 0; i < knockbackTicks(); i++) simulation.tick();

        CrystalPlanner.Plan plan = CrystalPlanner.findPlacement(target, simulation.box, eye, settings, digContext(0.0F, false));
        if (plan == null) return null;
        return plan.action() == CrystalPlanner.Action.PLACE_CRYSTAL || plan.action() == CrystalPlanner.Action.PLACE_OBSIDIAN ? plan : null;
    }

    private BlockPos committedBase() {
        return mc.player.tickCount - committedTick > 40 ? null : committedBase;
    }


    private List<Player> targets() {
        List<Player> players = CrystalUtil.enemies(targetRange.getValue());
        players.sort(Comparator.comparingDouble(CrystalUtil::eyeDistance));
        return players;
    }

    private AABB predictedBox(Player target) {
        if (isInCobweb(target.getBoundingBox())) return target.getBoundingBox();

        if (knockback != null && knockback.player == target) {
            TargetSimulation copy = knockback.copy();
            for (int i = 0; i < knockbackTicks(); i++) copy.tick();
            return copy.box != null ? copy.box : target.getBoundingBox();
        }

        int ticks = predictTicks.getValue().intValue();
        if (ticks <= 0) return target.getBoundingBox();
        TargetSimulation simulation = TargetSimulation.predict(target, ticks);
        return simulation.box != null ? simulation.box : target.getBoundingBox();
    }

    private int knockbackTicks() {
        return Math.max(predictTicks.getValue().intValue(), 3);
    }

    private boolean isKnockbackTarget(Player player) {
        return knockback != null && knockback.player == player;
    }

    private static boolean isAuraTarget(Player player) {
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.target == player;
    }

    private void onAttack(Entity entity) {
        MatrixPlacement.onAttack();
        if (mc.player == null || !(entity instanceof Player target)) return;

        Vec3 attackerPos = mc.player.position();
        float attackerYaw = mc.player.getYRot();
        float strength = KnockbackPrediction.currentStrength();
        boolean protectedTarget = target.hurtTime > 0 || target.isBlocking();
        if (protectedTarget || !isAuraTarget(target)) return;

        TargetSimulation simulation = TargetSimulation.of(target, TargetSimulation.GuessedInput.guess(target));
        Vec3 before = simulation.velocity;
        simulation.velocity = KnockbackPrediction.apply(target, before, simulation.onGround, attackerPos, attackerYaw, strength);

        double dx = simulation.velocity.x - before.x / 2.0;
        double dz = simulation.velocity.z - before.z / 2.0;
        knockbackSpeed = Math.sqrt(dx * dx + dz * dz);
        knockbackDirX = knockbackSpeed > 0.0 ? dx / knockbackSpeed : 0.0;
        knockbackDirZ = knockbackSpeed > 0.0 ? dz / knockbackSpeed : 0.0;
        knockback = simulation;
        knockbackStartTick = mc.player.tickCount;
        knockbackLastTick = knockbackStartTick;
    }

    private void updateKnockback() {
        TargetSimulation simulation = knockback;
        if (simulation == null) return;

        int tick = mc.player.tickCount;
        int age = tick - knockbackStartTick;
        if (age < 0 || age > 10 || simulation.player.isRemoved()) {
            knockback = null;
            return;
        }
        if (tick == knockbackLastTick) return;
        knockbackLastTick = tick;

        Player player = simulation.player;
        double along = (player.getX() - player.xo) * knockbackDirX + (player.getZ() - player.zo) * knockbackDirZ;
        if (along < knockbackSpeed * 0.5 && (age <= 1 || !simulation.onGround)) {
            simulation.tick();
        } else {
            knockback = null;
        }
    }


    private Vector2f crystalAttackRotation(EndCrystal crystal, float range) {
        Vec3 eye = mc.player.getEyePosition();
        if (matrix.getValue()) {
            Vec3 aim = ExplosionDamage.crystalAim(crystal, eye, range, true);
            return aim == null ? null : MatrixPlacement.attackRotation(crystal, aim, range);
        }

        Vec3 aim = explodeThroughWalls.getValue()
                ? ExplosionDamage.crystalAimOrCenter(crystal, eye, range)
                : ExplosionDamage.crystalAim(crystal, eye, range);
        return aim == null ? null : rotation(eye, aim);
    }

    private boolean aimHitsCrystal(EndCrystal crystal, float range) {
        if (matrix.getValue()) return MatrixPlacement.aimHitsEntity(crystal, range);
        return CrystalUtil.rayHitsBox(mc.player.getEyePosition(), CrystalRotations.aimYaw(), CrystalRotations.aimPitch(),
                range, !explodeThroughWalls.getValue(), crystal.getBoundingBox());
    }

    private Vector2f crystalPlaceRotation(BlockPos base) {
        Vec3 eye = mc.player.getEyePosition();
        if (placeThroughWalls.getValue()) return rotation(eye, ExplosionDamage.clampedAim(base, eye));

        Vec3 aim = ExplosionDamage.crystalPlaceAim(base, eye, placeRange.getValue());
        return aim == null ? null : rotation(eye, aim);
    }

    private BlockPlacement blockPlacement(BlockPos pos) {
        float range = placeRange.getValue();
        if (matrix.getValue()) {
            MatrixPlacement.Placement placement = MatrixPlacement.placeRotation(pos, range, placeThroughWalls.getValue());
            return placement == null ? null : new BlockPlacement(placement.rotation(),
                    new ExplosionDamage.PlaceFace(placement.neighbor(), placement.side(), placement.aim()));
        }

        Vec3 eye = mc.player.getEyePosition();
        ExplosionDamage.PlaceFace face = ExplosionDamage.findPlaceFace(pos, eye, range, placeThroughWalls.getValue());
        return face == null ? null : new BlockPlacement(rotation(eye, face.aim()), face);
    }

    private Vector2f rotation(Vec3 eye, Vec3 point) {
        Vector2f rotation = CrystalUtil.rotationTo(eye, point);
        return matrix.getValue() ? MatrixPlacement.snapToGcd(rotation) : rotation;
    }


    private void queue(BlockPos pos, Item item, ExplosionDamage.PlaceFace face) {
        queuedPos = pos;
        queuedItem = item;
        queuedFace = face;
        queuedTick = mc.player.tickCount;
    }

    private void clearQueue() {
        queuedPos = null;
        queuedItem = null;
        queuedFace = null;
        queuedTick = -100;
    }

    private void holdCrystal() {
        int slot = CrystalUtil.hotbarSlot(Items.END_CRYSTAL);
        if (slot == -1) return;

        breaker.release(this);
        if (savedSlot == -1) savedSlot = mc.player.getInventory().getSelectedSlot();
        if (mc.player.getInventory().getSelectedSlot() != slot) mc.player.getInventory().setSelectedSlot(slot);
    }

    private void restoreSlot() {
        if (savedSlot == -1) return;
        if (mc.player != null) mc.player.getInventory().setSelectedSlot(savedSlot);
        savedSlot = -1;
    }

    private void recordStall(BlockPos pos, boolean success) {
        if (!success && pos.equals(stallPos)) {
            if (++stallCount >= 5) {
                blacklist(pos);
                if (pos.equals(committedBase)) committedBase = null;
                stallPos = null;
                stallCount = 0;
            }
            return;
        }
        stallPos = success ? null : pos;
        stallCount = success ? 0 : 1;
    }

    private void blacklist(BlockPos pos) {
        if (pos != null && mc.player != null) blacklist.put(pos.immutable(), mc.player.tickCount + 40);
    }

    private boolean isBlacklisted(BlockPos pos) {
        Integer until = blacklist.get(pos);
        if (until == null) return false;
        if (mc.player != null && mc.player.tickCount <= until) return true;
        blacklist.remove(pos);
        return false;
    }

    private void pruneExpired() {
        int tick = mc.player.tickCount;
        blacklist.values().removeIf(until -> tick > until);
        ownObsidian.values().removeIf(until -> tick > until);
    }

    private boolean isPendingBroken(BlockPos pos) {
        return pendingBroken.containsKey(pos) || pendingBroken.containsKey(pos.above());
    }

    private boolean isCrystalJustPlaced(BlockPos base) {
        if (placedCrystal == null || !placedCrystal.equals(base.above())) return false;
        int age = mc.player.tickCount - placedCrystalTick;
        return age >= 0 && age < 4 && crystalAt(placedCrystal) == null;
    }

    private EndCrystal crystalAt(BlockPos pos) {
        List<EndCrystal> crystals = mc.level.getEntitiesOfClass(EndCrystal.class, ExplosionDamage.crystalSpace(pos), crystal -> true);
        return crystals.isEmpty() ? null : crystals.get(0);
    }

    private boolean isTotemGuarded(EndCrystal crystal) {
        return AutoTotemModule.getInstance().isTotemPending() && ExplosionDamage.damage(crystal.position(), mc.player) > 0.0F;
    }

    private static boolean isInCobweb(AABB box) {
        int minX = Mth.floor(box.minX), minY = Mth.floor(box.minY), minZ = Mth.floor(box.minZ);
        int maxX = Mth.floor(box.maxX), maxY = Mth.floor(box.maxY), maxZ = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (mc.level.getBlockState(cursor.set(x, y, z)).is(Blocks.COBWEB)) return true;
                }
            }
        }
        return false;
    }


    private void addPendingSelfDamage(float damage) {
        if (damage > 0.0F) pendingSelfDamage.add(new PendingDamage(mc.player.tickCount, damage));
    }

    private void prunePendingSelfDamage() {
        if (pendingSelfDamage.isEmpty()) return;
        int tick = mc.player.tickCount;
        int latency = CrystalUtil.latencyTicks();
        pendingSelfDamage.removeIf(entry -> lastHealthTick > entry.tick() + latency || tick - entry.tick() > latency + 5);
    }

    private float pendingSelfDamage() {
        float total = 0.0F;
        for (PendingDamage entry : pendingSelfDamage) total += entry.damage();
        return total;
    }


    private void onPacket(PacketEvent.PacketEventData event) {
        if (event.isSend()) {
            breaker.onPacketSend(event.packet());
            if (event.packet() instanceof ServerboundMovePlayerPacket move && move.hasRotation()) {
                MatrixPlacement.onRotationSent(move.getYRot(0.0F), move.getXRot(0.0F));
            }
            return;
        }

        if (event.packet() instanceof ClientboundSetHealthPacket) {
            lastHealthTick = mc.player == null ? -100 : mc.player.tickCount;
        } else if (!pendingBroken.isEmpty() || breaker.getPos() != null) {
            if (event.packet() instanceof ClientboundBlockUpdatePacket update) {
                onServerBlock(update.getPos(), update.getBlockState());
            } else if (event.packet() instanceof ClientboundSectionBlocksUpdatePacket section) {
                section.runUpdates(this::onServerBlock);
            }
        }
    }

    private void onRender(Render3DEvent.Render3DEventData event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (!render.getValue()) {
            visuals.clearMining();
            return;
        }
        visuals.render(event);
    }

    private record PendingDamage(int tick, float damage) {}

    private record BlockPlacement(Vector2f rotation, ExplosionDamage.PlaceFace face) {}
}
