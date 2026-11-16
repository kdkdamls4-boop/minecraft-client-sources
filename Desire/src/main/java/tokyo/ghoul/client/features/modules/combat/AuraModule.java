package tokyo.ghoul.client.features.modules.combat;

import lombok.Getter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ambient.AmbientCreature;
import net.minecraft.world.entity.animal.fish.AbstractFish;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.events.other.RotationUpdateEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.configs.FriendManager;
import tokyo.ghoul.api.utils.combat.CriticalHitHandler;
import tokyo.ghoul.api.utils.combat.SprintManager;
import tokyo.ghoul.api.utils.combat.TpsSync;
import tokyo.ghoul.api.utils.math.TimerUtil;
import tokyo.ghoul.api.utils.player.MoveUtil;
import tokyo.ghoul.api.utils.player.CarriedItemSync;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.api.utils.rotation.manager.RotationStrategy;
import tokyo.ghoul.api.utils.rotation.misc.AuraUtil;
import tokyo.ghoul.api.utils.rotation.misc.LegitAim;
import tokyo.ghoul.api.utils.rotation.misc.MultipointUtil;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroManager;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroModel;
import tokyo.ghoul.api.utils.rotation.rotations.*;
import tokyo.ghoul.api.utils.task.TaskPriority;
import tokyo.ghoul.api.utils.predict.PredictUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.features.modules.movement.SprintModule;

import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;


@ModuleRegister(name = "Aura", description = "Автоматическая атака вокруг игрока", category = Category.COMBAT)
public class AuraModule extends Module {
    @Getter private static final AuraModule instance = new AuraModule();

    private static final EquipmentSlot[] ARMOR_SLOTS = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET };

    private final MultiBooleanSetting targetTypeSetting = new MultiBooleanSetting("Attack").value(
            new BooleanSetting("Players").value(true),
            new BooleanSetting("Mobs").value(true),
            new BooleanSetting("Animals").value(true),
            new BooleanSetting("Friends").value(true),
            new BooleanSetting("Naked").value(false),
            new BooleanSetting("Invisible").value(false)
    );
    private final ModeSetting sortMode = new ModeSetting("Sort").value("Auto").values(
            "Auto", "Distance", "Health", "Armor", "All", "Crosshair");

    @Getter private final ModeSetting aimMode = new ModeSetting("Aim mode").value("LonyGrief").values(
            "LonyGrief", "ReallyWorld", "MlsAC", "HolyWorld", "Legit", "Neuro", "Shard"
    );
    private final HolyWorldSensRotation holyWorldRotation = new HolyWorldSensRotation();
    private final ShardRotation shardRotation = new ShardRotation();
    private int hwReadyTicks;
    private int hwHitDelay;
    private int hwLastHitDelay = -1;
    private int hwHitDelayRepeats;
    private float hwStrengthThreshold = 0.95f;
    private final SliderSetting neuroNoise = new SliderSetting("Neuro noise").value(0.35f).range(0f, 1.5f).step(0.05f)
            .setVisible(() -> aimMode.is("Neuro"));
    private final NeuroRotation neuroRotation = new NeuroRotation(() -> neuroNoise.getValue());
    private int lastAttackTick;
    private int neuroAttackGap;

    private static final float LEGIT_SPEED = 20f;
    private static final float PRE_DISTANCE = 0.3f;
    private final LegitAim legitAim = new LegitAim();

    public final SliderSetting distance = new SliderSetting("Distance").value(3f).range(0.5f, 6f).step(0.1f);
    private final SliderSetting distanceRotation = new SliderSetting("Rotate distance").value(1f).range(1f, 6f).step(0.1f);

    public final BooleanSetting elytraTarget = new BooleanSetting("Предикт на элитрах").value(true);
    @Getter private final ModeSetting elytraPredictMode = new ModeSetting("Предикт").value("Обычный").values("Обычный", "Легитный")
            .setVisible(elytraTarget::getValue);
    public final BooleanSetting elytraTargetRender = new BooleanSetting("Показать наводку элитр").value(true)
            .setVisible(elytraTarget::getValue);
    public final SliderSetting rotateElytra = new SliderSetting("Дистанция элитр").value(60f).range(10f, 100f).step(5f)
            .setVisible(elytraTarget::getValue);

    private final ModeSetting correctionMode = new ModeSetting("Move fix").value("Focus").values("Focus", "Free");
    private final BooleanSetting ignoreWalls = new BooleanSetting("Ignore walls RW").value(false);
    private final BooleanSetting onlyOnAim = new BooleanSetting("Only on aim").value(false);
    private final BooleanSetting smartCriticals = new BooleanSetting("Smart crits").value(false);
    private final BooleanSetting tpsSync = new BooleanSetting("TPS sync").value(false);
    private final BooleanSetting shieldBreak = new BooleanSetting("Break shield").value(true);
    private final BooleanSetting legitSwap = new BooleanSetting("Legit shield swap").value(true)
            .setVisible(shieldBreak::getValue);
    private final BooleanSetting clientLook = new BooleanSetting("Client look").value(false);

    public final SliderSetting hwKp = new SliderSetting("HW Kp").value(0.42f).range(0.05f, 1.0f).step(0.01f)
            .setVisible(() -> aimMode.is("MlsAC"));
    public final SliderSetting hwKi = new SliderSetting("HW Ki").value(0.03f).range(0.0f, 0.2f).step(0.005f)
            .setVisible(() -> aimMode.is("MlsAC"));
    public final SliderSetting hwKd = new SliderSetting("HW Kd").value(0.08f).range(0.0f, 0.5f).step(0.01f)
            .setVisible(() -> aimMode.is("MlsAC"));
    public final SliderSetting hwPitchBias = new SliderSetting("HW Pitch bias").value(6.8f).range(1.0f, 15.0f).step(0.2f)
            .setVisible(() -> aimMode.is("MlsAC"));

    public LivingEntity target;

    private final TimerUtil hurtTimer = new TimerUtil();
    private final CriticalHitHandler criticalHitHandler = new CriticalHitHandler();
    public float lastYaw;
    public float lastPitch;
    private boolean attacking;
    public boolean isAttacking;
    public boolean blockPos;
    private boolean sprintStop;
    private boolean sprintRestorePending;

    private int rotationAttackTicks;
    private int hitCount;
    private int criticalSprintTicks;
    private LivingEntity pendingCriticalTarget;
    @Getter private Vec3 predictedElytraPos;
    @Getter private LivingEntity elytraTargetEntity;

    public AuraModule() {
        addSettings(aimMode, neuroNoise, distance, distanceRotation,
                targetTypeSetting, sortMode,
                elytraTarget, elytraPredictMode, elytraTargetRender, rotateElytra,
                correctionMode, ignoreWalls, onlyOnAim, smartCriticals, tpsSync, shieldBreak, legitSwap,
                clientLook
        );
    }


    public static boolean isMoveFix() {
        return getInstance().shouldControlMovementCorrection();
    }

    public static boolean isMoveFixFree() {
        return getInstance().correctionMode.is("Free");
    }

    public static int getWhiteRiseTicksToAttack() {
        if (mc.player == null) return 0;
        float progress = mc.player.getAttackStrengthScale(0.5f);
        return progress >= 1.0f ? 0 : 1;
    }

    public boolean isAttackingFlag() {
        return attacking;
    }

    public int getRotationAttackTicks() {
        return rotationAttackTicks;
    }

    public boolean canSnap(LivingEntity entity) {
        return entity != null && entity == target && isCanAttack();
    }

    public float getAttackDistance() {
        return distance.getValue();
    }

    public float getPreDistance() {
        return PRE_DISTANCE;
    }


    @Override
    public void onEnable() {
        target = null;
        hitCount = 0;
        clearPendingCriticalAttack();
        rotationAttackTicks = 0;
        criticalHitHandler.onEnable();
        GrimRotation.reset();
        UniversalRotation.reset();
        HolyWorldRotation.reset();
        neuroRotation.reset();
        neuroAttackGap = 0;
        holyWorldRotation.reset();
        shardRotation.reset();
        hwReadyTicks = 0;
        rollHolyWorldHit();
        legitAim.reset();
        resetElytraOvertake();
        blockPos = false;
        isAttacking = false;
    }

    public void onLegitFrame() {
        if (!isEnabled() || !aimMode.is("Legit") || mc.player == null || mc.level == null
                || mc.gui.screen() != null || target == null || !isValid(target)) {
            legitAim.reset();
            return;
        }
        legitAim.update(target, LEGIT_SPEED);
    }

    @Override
    public void onDisable() {
        legitAim.disable();
        hitCount = 0;
        clearPendingCriticalAttack();
        rotationAttackTicks = 0;
        criticalHitHandler.onDisable();
        GrimRotation.reset();
        UniversalRotation.reset();
        HolyWorldRotation.reset();
        neuroRotation.reset();
        holyWorldRotation.reset();
        shardRotation.reset();
        resetElytraOvertake();
        predictedElytraPos = null;
        elytraTargetEntity = null;
        blockPos = false;
        isAttacking = false;
    }

    @Override
    public void onEvent() {
        EventListener eventUpdate = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener rotationUpdateEvent = RotationUpdateEvent.getInstance().subscribe(new Listener<>(event -> onRotationUpdate()));
        EventListener attackEvent = AttackEvent.getInstance().subscribe(new Listener<>(event -> AuraUtil.onAttack(aimMode.getValue())));
        EventListener renderEvent = Render3DEvent.getInstance().subscribe(new Listener<>(this::onRender3D));
        addEvents(eventUpdate, rotationUpdateEvent, attackEvent, renderEvent);
    }


    private void onTick() {
        if (mc.player == null || mc.level == null) return;

        if (criticalSprintTicks > 0) criticalSprintTicks--;
        if (rotationAttackTicks > 0) rotationAttackTicks--;
        if (sprintRestorePending) {
            sprintRestorePending = false;
            if (!SprintModule.getInstance().mode.getValue().equals("None")) {
                mc.player.setSprinting(true);
            }
        }

        if (target == null || !isValid(target)) {
            target = updateTarget();
        }
        updateBlockPos();
        updateElytraTargetState();

        if (target != null) {
            if (aimMode.is("MlsAC") || aimMode.is("ReallyWorld")) {
                tryAttackHolyWorld();
            } else {
                long attackDelay = tpsSync.getValue()
                        ? TpsSync.getAdjustedCooldown(458L, true)
                        : 458L;
                boolean holyWorldHitReady = isHolyWorldHitReady();
                boolean moduleDelayReady = hurtTimer.finished(attackDelay) && isNeuroIntervalReady()
                        && holyWorldHitReady;

                if (pendingCriticalTarget != null) {
                    if (pendingCriticalTarget != target || !isFallingCriticalWindow()) {
                        clearPendingCriticalAttack();
                    } else if (isCanAttack() && moduleDelayReady) {
                        if (resetSprintForAttack()) return;
                        attackTarget();
                        pendingCriticalTarget = null;
                        hitCount++;
                        hurtTimer.reset();
                    }
                    return;
                }

                if (isCanAttack() && moduleDelayReady) {
                    if (deferCriticalAttack()) return;
                    if (resetSprintForAttack()) return;
                    attackTarget();
                    hitCount++;
                    hurtTimer.reset();
                }
            }
        } else {
            clearPendingCriticalAttack();
            hitCount = 0;
        }
    }


    private void onRotationUpdate() {
        if (aimMode.is("Legit")) return;
        if (target == null) return;

        Vec3 aimPoint = getVec3d(target);
        Rotation angle = RotationUtil.fromVec3(aimPoint.subtract(mc.player.getEyePosition()));

        boolean moveCorrection = shouldControlMovementCorrection();
        RotationStrategy strategy = new RotationStrategy(getRotationMode(), moveCorrection, correctionMode.is("Free"))
                .clientLook(clientLook.getValue());

        RotationManager.getInstance().addRotation(new Rotation.VecRotation(angle, aimPoint), target, strategy, TaskPriority.HIGH, this);
    }

    private RotationMode getRotationMode() {
        return switch (aimMode.getValue()) {
            case "ReallyWorld" -> new UniversalRotation();
            case "MlsAC" -> new HolyWorldRotation();
            case "Neuro" -> neuroRotation;
            case "HolyWorld" -> holyWorldRotation;
            case "Shard" -> shardRotation;
            default -> new GrimRotation();
        };
    }


    private void attackTarget() {
        if (shieldBreak.getValue() && isShieldBlocking(target) && attackWithAxe()) return;
        performAttack();
    }

    private boolean isShieldBlocking(LivingEntity entity) {
        return entity != null && entity.isBlocking()
                && entity.isUsingItem()
                && entity.getUseItem().is(Items.SHIELD);
    }

    private boolean attackWithAxe() {
        int axeSlot = findAxeSlot(0, 8);
        if (axeSlot == -1) {
            axeSlot = findAxeSlot(9, 35);
        }
        if (axeSlot == -1) return false;

        int previousSlot = mc.player.getInventory().getSelectedSlot();
        boolean inventorySwap = axeSlot > 8;
        int sourceScreenSlot = inventorySwap ? axeSlot : -1;
        ItemStack originalHeldStack = mc.player.getMainHandItem();

        if (inventorySwap) {
            if (mc.player.containerMenu.containerId != mc.player.inventoryMenu.containerId) {
                return false;
            }
            swapAxeWithHotbar(sourceScreenSlot, previousSlot);
            if (!isAxe(mc.player.getMainHandItem())) {
                return false;
            }
            axeSlot = previousSlot;
        }

        try {
            selectHotbarSlot(axeSlot);
            performAttack();
        } finally {
            if (inventorySwap) {
                swapAxeWithHotbar(sourceScreenSlot, previousSlot);
                if (!mc.player.getMainHandItem().is(originalHeldStack.getItem())) {
                    mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            sourceScreenSlot, previousSlot, net.minecraft.world.inventory.ContainerInput.SWAP, mc.player);
                }
            }
            selectHotbarSlot(previousSlot);
        }
        return true;
    }

    private int findAxeSlot(int start, int end) {
        for (int slot = end; slot >= start; slot--) {
            if (isAxe(mc.player.getInventory().getItem(slot))) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isAxe(ItemStack stack) {
        return stack.is(Items.WOODEN_AXE)
                || stack.is(Items.STONE_AXE)
                || stack.is(Items.IRON_AXE)
                || stack.is(Items.GOLDEN_AXE)
                || stack.is(Items.DIAMOND_AXE)
                || stack.is(Items.NETHERITE_AXE);
    }

    private void swapAxeWithHotbar(int sourceScreenSlot, int hotbarSlot) {
        Runnable swap = () -> mc.gameMode.handleContainerInput(
                mc.player.inventoryMenu.containerId,
                sourceScreenSlot,
                hotbarSlot,
                net.minecraft.world.inventory.ContainerInput.SWAP,
                mc.player
        );
        if (legitSwap.getValue()) {
            swap.run();
        } else {
            swap.run();
        }
    }

    private void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        CarriedItemSync.send(slot);
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private void performAttack() {
        if (blockPos && elytraPredictMode.is("Легитный") && target != null) {
            Rotation snap = RotationUtil.rotationAt(target.getEyePosition(1.0f));
            mc.player.setYRot(snap.getYRot());
            mc.player.setXRot(snap.getXRot());
        }
        attacking = true;
        try {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);

            if (ignoreWalls.getValue()) {
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        } finally {
            attacking = false;
        }
        isAttacking = true;
        onAttackCompleted();
    }


    private boolean resetSprintForAttack() {
        boolean criticalWindow = isFallingCriticalWindow();
        if (criticalWindow) {
            criticalSprintTicks = Math.max(criticalSprintTicks, 1);
        }

        if (mc.player.isFallFlying()) {
            if (mc.player.isSprinting()) {
                mc.player.setSprinting(false);
            }
            return false;
        }

        switch (SprintModule.getInstance().mode.getValue()) {
            case "Legit" -> {
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    SprintManager.markAttackImminent();
                    return true;
                }
            }
            case "Packet" -> {
                if (mc.player.isSprinting()) {
                    mc.player.setSprinting(false);
                    sprintStop = true;
                }
            }
            default -> {
            }
        }

        return false;
    }

    private void onAttackCompleted() {
        SprintManager.onAttack();
        SprintManager.reset();

        if (aimMode.is("MlsAC")) {
            HolyWorldRotation.onAttack();
        }
        if (aimMode.is("Neuro")) {
            neuroRotation.onAttack();
            lastAttackTick = mc.player.tickCount;
            NeuroModel model = NeuroManager.getInstance().getActiveModel();
            neuroAttackGap = model != null ? model.sampleAttackInterval(ThreadLocalRandom.current()) : -1;
        }
        if (aimMode.is("Shard")) {
            shardRotation.onAttack();
        }
        if (aimMode.is("HolyWorld")) {
            holyWorldRotation.onAttack();
            hwReadyTicks = 0;
            rollHolyWorldHit();
        }

        String sprintMode = SprintModule.getInstance().mode.getValue();
        if (sprintStop) {
            sprintRestorePending = true;
        } else if (!sprintMode.equals("None")) {
            mc.player.setSprinting(true);
        }

        sprintStop = false;
    }

    private boolean isNeuroIntervalReady() {
        if (!aimMode.is("Neuro") || neuroAttackGap <= 0 || mc.player == null) return true;
        return mc.player.tickCount - lastAttackTick >= neuroAttackGap;
    }

    // вызывается раз в тик: удар идёт не сразу после кулдауна, а через случайную паузу
    private boolean isHolyWorldHitReady() {
        if (!aimMode.is("HolyWorld") || mc.player == null) return true;
        if (mc.player.getAttackStrengthScale(0.5F) >= hwStrengthThreshold) {
            hwReadyTicks++;
        } else {
            hwReadyTicks = 0;
        }
        return hwReadyTicks > hwHitDelay;
    }

    // пауза после кулдауна: лог-нормальная (чаще 0-1 тик, иногда дольше), без длинных серий одинаковых значений
    private void rollHolyWorldHit() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int delay = Mth.clamp((int) Math.round(Math.exp(rng.nextGaussian() * 0.6) * 1.4 - 0.6), 0, 5);

        if (delay == hwLastHitDelay && ++hwHitDelayRepeats >= 2) {
            delay = (delay + 1 + rng.nextInt(2)) % 4;
            hwHitDelayRepeats = 0;
        } else if (delay != hwLastHitDelay) {
            hwHitDelayRepeats = 0;
        }
        hwLastHitDelay = delay;

        // изредка "задумался" перед ударом
        if (rng.nextInt(100) < 6) {
            delay += 2 + rng.nextInt(3);
        }
        hwHitDelay = delay;
        hwStrengthThreshold = Mth.clamp(0.96f + (float) rng.nextGaussian() * 0.025f, 0.9f, 1.0f);
    }

    private boolean isFloating(LivingEntity entity) {
        return !entity.isFallFlying() && (entity.isVisuallySwimming() || entity.isSwimming());
    }

    private void clearPendingCriticalAttack() {
        pendingCriticalTarget = null;
        criticalSprintTicks = 0;
        sprintStop = false;
        SprintManager.reset();
    }

    private boolean deferCriticalAttack() {
        if (pendingCriticalTarget != null) {
            return true;
        }
        if (!isFallingCriticalWindow() || !mc.player.isSprinting()) {
            return false;
        }
        pendingCriticalTarget = target;
        criticalSprintTicks = 2;
        mc.player.setSprinting(false);
        SprintManager.markAttackImminent();
        return true;
    }

    private boolean isFallingCriticalWindow() {
        return !mc.player.onGround()
                && mc.player.fallDistance > 0.0F
                && mc.player.getDeltaMovement().y < -1.0E-4
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable()
                && !mc.player.isPassenger()
                && !mc.player.getAbilities().flying;
    }

    private boolean isCanAttack() {
        if (target == null || mc.player == null) return false;

        if (mc.player.getAttackStrengthScale(0.5F) < 0.9F) {
            return false;
        }
        if (!criticalHitHandler.canAttack(0, smartCriticals.getValue())) {
            return false;
        }
        if (mc.player.isFallFlying() && target.isFallFlying() && elytraTarget.getValue()) {
            Vec3 point = PredictUtil.realPredict(target);
            if (mc.player.getEyePosition().distanceTo(point) > 3.2F) {
                return false;
            }
            return true;
        }

        if (aimMode.is("HolyWorld") && (isFloating(mc.player) || isFloating(target))) {
            return false;
        }
        if (onlyOnAim.getValue() && !isRotationAimedAtTarget()) {
            return false;
        }
        if (aimMode.is("MlsAC") || aimMode.is("ReallyWorld")) {
            if (!isAimOnTargetHitbox(target)) {
                return false;
            }
        } else if (getRaytraceHit() != target) {
            return false;
        }
        double range = getAttackDistance();
        return mc.player.getEyePosition().distanceTo(MultipointUtil.getNearestPoint(target, range)) <= range;
    }

    public boolean isAimOnTargetHitbox(LivingEntity entity) {
        if (entity == null || mc.player == null) return false;
        if (getRaytraceHit() == entity) return true;

        Rotation current = RotationManager.getInstance().getRotation();
        if (HolyWorldRotation.isAimOnHitbox(current, entity)) {
            return ignoreWalls.getValue() || !isTargetBehindWall();
        }

        Rotation server = RotationManager.getInstance().getServerRotation();
        if (HolyWorldRotation.isAimOnHitbox(server, entity)) {
            return ignoreWalls.getValue() || !isTargetBehindWall();
        }

        return false;
    }

    public void tryAttackHolyWorld() {
        if (!isEnabled() || target == null || mc.player == null) return;
        if (!aimMode.is("MlsAC") && !aimMode.is("ReallyWorld")) return;

        double attackDist = getAttackDistance();
        if (mc.player.getEyePosition().distanceTo(MultipointUtil.getNearestPoint(target, attackDist)) > attackDist) {
            return;
        }

        if (!isAimOnTargetHitbox(target)) {
            return;
        }

        long attackDelay = tpsSync.getValue()
                ? TpsSync.getAdjustedCooldown(458L, true)
                : 458L;
        if (!hurtTimer.finished(attackDelay)) {
            return;
        }

        if (mc.player.getAttackStrengthScale(0.5F) < 0.9F) {
            return;
        }

        if (!criticalHitHandler.canAttack(0, smartCriticals.getValue())) {
            return;
        }

        if (pendingCriticalTarget != null) {
            if (pendingCriticalTarget != target || !isFallingCriticalWindow()) {
                clearPendingCriticalAttack();
            } else {
                if (resetSprintForAttack()) return;
                attackTarget();
                pendingCriticalTarget = null;
                hitCount++;
                hurtTimer.reset();
            }
            return;
        }

        if (deferCriticalAttack()) return;
        if (resetSprintForAttack()) return;

        attackTarget();
        hitCount++;
        hurtTimer.reset();
    }

    private Entity getRaytraceHit() {
        net.minecraft.world.phys.EntityHitResult hit = RaytracingUtil.raytraceEntity(
                getAttackDistance(),
                RotationManager.getInstance().getRotation(),
                ignoreWalls.getValue());
        return hit != null ? hit.getEntity() : null;
    }

    private boolean isTargetBehindWall() {
        if (target == null || mc.player == null || mc.level == null) return false;

        Vec3 eye = mc.player.getEyePosition();
        Vec3 targetEye = target.position().add(0, target.getEyeHeight(), 0);

        HitResult result = mc.level.clip(new ClipContext(
                eye, targetEye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));

        return result.getType() == HitResult.Type.BLOCK;
    }

    private boolean isRotationAimedAtTarget() {
        if (target == null || mc.player == null) {
            return false;
        }

        Rotation serverRotation = RotationManager.getInstance().getServerRotation();
        double range = mc.player.isFallFlying()
                ? Math.max(rotateElytra.getValue(), getAttackDistance() + distanceRotation.getValue())
                : getAttackDistance() + distanceRotation.getValue();

        var currentBox = target.getBoundingBox().inflate(mc.player.isFallFlying() ? 0.35 : 0.18);
        return RaytracingUtil.rayTraceBox(serverRotation, range + 0.5, currentBox);
    }


    private LivingEntity updateTarget() {
        if (!isEnabled()) return null;

        LivingEntity best = null;
        double bestScore = Double.POSITIVE_INFINITY;

        Iterator<Entity> iterator = mc.level.entitiesForRendering().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity instanceof LivingEntity living && isValid(living)) {
                double score = getSortScore(living);
                if (score < bestScore) {
                    bestScore = score;
                    best = living;
                }
            }
        }
        return best;
    }

    private double getSortScore(LivingEntity entity) {
        double baseScore;
        if (sortMode.is("Distance")) {
            baseScore = entity.distanceToSqr(mc.player);
        } else if (sortMode.is("Health")) {
            baseScore = entity.getHealth() + entity.getAbsorptionAmount();
        } else if (sortMode.is("Armor")) {
            baseScore = -getArmorScore(entity);
        } else if (sortMode.is("All")) {
            baseScore = getMixedScore(entity);
        } else if (sortMode.is("Crosshair")) {
            baseScore = getCrosshairAngle(entity);
        } else {
            baseScore = getAngleScore(entity);
        }

        if (mc.player != null && mc.player.isFallFlying() && entity.isFallFlying()) {
            baseScore -= 10000.0;
        }
        return baseScore;
    }

    private double getAngleScore(LivingEntity entity) {
        Rotation vec = RotationUtil.fromVec3(entity.getBoundingBox().getCenter().subtract(mc.player.getEyePosition()));
        Rotation current = RotationManager.getInstance().getRotation();
        double dy = Math.abs(Mth.wrapDegrees(vec.getYRot() - current.getYRot()));
        double dp = Math.abs(Mth.wrapDegrees(vec.getXRot() - current.getXRot()));
        return dy + dp;
    }

    private double getCrosshairAngle(LivingEntity entity) {
        Vec3 eyes = mc.player.getEyePosition();
        Vec3 look = mc.player.getViewVector(1.0f).normalize();
        Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eyes).normalize();
        double dot = Mth.clamp(look.dot(toEntity), -1.0, 1.0);
        return Math.toDegrees(Math.acos(dot));
    }

    private double getMixedScore(LivingEntity entity) {
        double maxRange = Math.max(1.0, distance.getValue() + distanceRotation.getValue());
        double distanceNorm = Mth.clamp(
                mc.player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) / maxRange, 0.0, 1.0);
        double healthNorm = Mth.clamp(
                (entity.getHealth() + entity.getAbsorptionAmount()) / 36.0, 0.0, 1.0);
        double armorNorm = 1.0 - Mth.clamp(getArmorScore(entity) / 28.0, 0.0, 1.0);
        double angleNorm = Mth.clamp(getAngleScore(entity) / 180.0, 0.0, 1.0);

        return distanceNorm * 0.4 + healthNorm * 0.25 + armorNorm * 0.2 + angleNorm * 0.15;
    }

    private float getArmorScore(LivingEntity entity) {
        float score = entity.getArmorValue();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!entity.getItemBySlot(slot).isEmpty()) {
                score += 2.0F;
            }
        }
        return score;
    }

    public boolean isValid(LivingEntity entity) {
        if (entity == mc.player) return false;
        if (!entity.isAlive() || entity.getHealth() <= 0.0F) return false;
        if (!mc.player.isAlive() || mc.player.getHealth() <= 0.0F) return false;

        if (entity instanceof Player player) {
            boolean invisible = player.isInvisible();
            boolean naked = isNaked(player);

            if (invisible) {
                if (!targetTypeSetting.isEnabled("Invisible")) return false;
            } else if (naked) {
                if (!targetTypeSetting.isEnabled("Naked")) return false;
            } else if (!targetTypeSetting.isEnabled("Players")) {
                return false;
            }

            if (!targetTypeSetting.isEnabled("Friends")
                    && FriendManager.getInstance().contains(player.getName().getString())) {
                return false;
            }

            if (AntiBotModule.isBot(player)) {
                return false;
            }
        }

        if (entity instanceof AgeableMob || entity instanceof AbstractFish) {
            if (!targetTypeSetting.isEnabled("Animals")) return false;
        } else if (entity instanceof Monster || entity instanceof AmbientCreature) {
            if (!targetTypeSetting.isEnabled("Mobs")) return false;
        }

        double validRange = (mc.player.isFallFlying() || entity.isFallFlying())
                ? Math.max(rotateElytra.getValue(), getAttackDistance() + distanceRotation.getValue())
                : getAttackDistance() + distanceRotation.getValue();
        if (mc.player.getEyePosition().distanceTo(entity.getBoundingBox().getCenter()) > validRange) {
            return false;
        }
        return !(entity instanceof ArmorStand);
    }

    private boolean isNaked(Player player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!player.getItemBySlot(slot).isEmpty()) return false;
        }
        return true;
    }


    public Vec3 getVec3d(LivingEntity entity) {
        if (entity == null) return Vec3.ZERO;

        if (entity.isFallFlying() && elytraTarget.getValue()) {
            double speed = Math.hypot(entity.getX() - entity.xo, entity.getZ() - entity.zo) * 20.0;
            double verticalSpeed = (entity.getY() - entity.yo) * 20.0;
            if (speed > 0.05 || Math.abs(verticalSpeed) > 0.05) {
                return PredictUtil.realPredict(entity);
            }
            return entity.getBoundingBox().getCenter();
        }

        double scanDistance = mc.player != null && mc.player.isFallFlying()
                ? Math.max(rotateElytra.getValue(), getAttackDistance() + distanceRotation.getValue())
                : getAttackDistance();

        return MultipointUtil.getNearestPoint(entity, scanDistance);
    }

    private void updateElytraTargetState() {
        if (target != null && target.isFallFlying() && elytraTarget.getValue()) {
            predictedElytraPos = PredictUtil.realPredict(target);
            elytraTargetEntity = target;
        } else {
            predictedElytraPos = null;
            elytraTargetEntity = null;
        }
    }

    private void resetElytraOvertake() {
        predictedElytraPos = null;
        elytraTargetEntity = null;
    }


    public boolean shouldControlMovementCorrection() {
        return isEnabled()
                && target != null
                && mc.player != null
                && !mc.player.isFallFlying()
                && !aimMode.is("Legit");
    }

    private void updateBlockPos() {
        if (elytraTarget.getValue() && actuallyElytraFlying() && target != null && target.isFallFlying()) {
            blockPos = true;
            return;
        }
        blockPos = false;
    }

    private boolean actuallyElytraFlying() {
        return mc.player != null && mc.player.isFallFlying() && !mc.player.getAbilities().flying;
    }

    public boolean shouldFlipModelVisual() {
        if (!isEnabled() || !elytraTarget.getValue()) return false;
        if (mc.player == null || !mc.player.isFallFlying()) return false;
        if (target == null || !target.isAlive() || !target.isFallFlying()) return false;
        Vec3 predict = predictedElytraPos != null ? predictedElytraPos : PredictUtil.realPredict(target);
        return mc.player.getEyePosition().distanceTo(predict) < 6.0;
    }

    private void onRender3D(Render3DEvent.Render3DEventData event) {
        if (!elytraTarget.getValue() || !elytraTargetRender.getValue() || predictedElytraPos == null || elytraTargetEntity == null || mc.player == null) return;
        if (!mc.player.isFallFlying() && !elytraTargetEntity.isFallFlying()) return;

        Vec3 cam = mc.gameRenderer.mainCamera().position();
        event.matrixStack().pushPose();
        event.matrixStack().translate(-cam.x, -cam.y, -cam.z);

        net.minecraft.world.phys.AABB baseBox = elytraTargetEntity.getBoundingBox();
        float width = (float) ((baseBox.maxX - baseBox.minX) * 0.8);
        float height = (float) ((baseBox.maxY - baseBox.minY) * 0.8);

        double x = predictedElytraPos.x;
        double y = predictedElytraPos.y;
        double z = predictedElytraPos.z;

        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
                x - width / 2.0, y, z - width / 2.0,
                x + width / 2.0, y + height, z + width / 2.0
        );

        float pulse = (float) (Math.sin(System.currentTimeMillis() / 500.0) * 0.3 + 0.7);
        int alpha = Math.max(0, Math.min(255, (int) (pulse * 0.75f * 255)));
        int cyan = (alpha << 24) | 0x00FFFF;

        var matrix = event.matrixStack().last().pose();
        var buffer = RenderUtil.WORLD.lines();
        drawBox(buffer, matrix, box, cyan);

        event.matrixStack().popPose();
    }

    private void drawBox(com.mojang.blaze3d.vertex.VertexConsumer buffer, org.joml.Matrix4f matrix, net.minecraft.world.phys.AABB box, int color) {
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        line(buffer, matrix, x1, y1, z1, x2, y1, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y1, z2, color);
        line(buffer, matrix, x2, y1, z2, x1, y1, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y1, z1, color);

        line(buffer, matrix, x1, y2, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y2, z1, x2, y2, z2, color);
        line(buffer, matrix, x2, y2, z2, x1, y2, z2, color);
        line(buffer, matrix, x1, y2, z2, x1, y2, z1, color);

        line(buffer, matrix, x1, y1, z1, x1, y2, z1, color);
        line(buffer, matrix, x2, y1, z1, x2, y2, z1, color);
        line(buffer, matrix, x2, y1, z2, x2, y2, z2, color);
        line(buffer, matrix, x1, y1, z2, x1, y2, z2, color);
    }

    private void line(com.mojang.blaze3d.vertex.VertexConsumer b, org.joml.Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
        b.addVertex(m, x1, y1, z1).setColor(color);
        b.addVertex(m, x2, y2, z2).setColor(color);
    }
}
