package tokyo.ghoul.client.features.modules.movement.speed;

import lombok.Getter;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.move.MotionEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.client.TimerManager;
import tokyo.ghoul.api.utils.player.MoveUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ModuleRegister(name = "Speed", description = "Увеличение скорости передвижения", category = Category.MOVEMENT)
public class SpeedModule extends Module {
    @Getter private static final SpeedModule instance = new SpeedModule();

    private static final float TIMER_SLOW_SPEED = 0.65F;
    private static final float TIMER_FAST_SPEED = 1.35F;
    private static final int TIMER_SLOW_TICKS = 2;
    private static final int TIMER_FAST_BURST_TICKS = 3;
    private static final int TIMER_RECOVERY_TICKS = 20;
    private static final float TIMER2_SLOW_SPEED = 0.10F;
    private static final float TIMER2_FAST_SPEED = 1.35F;
    private static final long TIMER2_SLOW_TIME_MS = 1_650L;
    private static final long TIMER2_FAST_TIME_MS = 2_200L;

    private final ModeSetting mode = new ModeSetting("Mode").value("Grim Timer")
            .values("Grim Timer", "Grim Timer2", "LonyGrief", "Collision", "HolyWorld", "Vanilla");

    private final SliderSetting holyWorldSpeed = new SliderSetting("Скорость").value(8.0F).range(1.0F, 20.0F).step(0.1F)
            .setVisible(() -> mode.is("HolyWorld"));
    private final SliderSetting holyWorldRange = new SliderSetting("Дальность").value(3.0F).range(0.5F, 10.0F).step(0.1F)
            .setVisible(() -> mode.is("HolyWorld"));
    private final SliderSetting holyWorldExpand = new SliderSetting("Расширение").value(0.5F).range(0.1F, 2.0F).step(0.1F)
            .setVisible(() -> mode.is("HolyWorld"));
    private final BooleanSetting holyWorldOnlyPlayers = new BooleanSetting("Только игроки").value(true)
            .setVisible(() -> mode.is("HolyWorld"));
    private final BooleanSetting holyWorldRequireMoving = new BooleanSetting("Требуется движение").value(true)
            .setVisible(() -> mode.is("HolyWorld"));
    private final SliderSetting vanillaSpeed = new SliderSetting("Скорость").value(10.0F).range(1.0F, 30.0F).step(0.5F)
            .setVisible(() -> mode.is("Vanilla"));

    private int ticks;
    private int groundTicks;
    private int timerTicks;
    private int timerRecoveryTicks;
    private boolean timerModeActive;
    private boolean timer2FastPhase;
    private long timer2PhaseStartedAt;
    private String activeMode = "";
    private final Map<Entity, Vec3> previousPositions = new HashMap<>();

    public SpeedModule() {
        addSettings(mode, holyWorldSpeed, holyWorldRange, holyWorldExpand,
                holyWorldOnlyPlayers, holyWorldRequireMoving, vanillaSpeed);
    }

    @Override
    public void onEnable() {
        activeMode = "";
        resetMovementState(false);
        resetTimerState(true);
        syncSelectedMode();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetMovementState(false);
        resetTimerState(true);
        activeMode = "";
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        EventListener input = MovementInputEvent.getInstance().subscribe(new Listener<>(this::onMoveInput));
        EventListener motion = MotionEvent.getInstance().subscribe(new Listener<>(event -> onPostMotion()));
        EventListener packet = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
        addEvents(update, input, motion, packet);
    }

    private void onUpdate() {
        syncSelectedMode();

        if (mode.is("Grim Timer")) {
            onTimerUpdate();
            return;
        }
        if (mode.is("Grim Timer2")) {
            onTimer2Update();
            return;
        }
        if (timerModeActive) {
            resetTimerState(true);
        }
        if (mode.is("Collision")) {
            onCollisionUpdate();
            return;
        }
        if (mode.is("HolyWorld")) {
            onHolyWorldEntityUpdate();
            return;
        }
        if (mode.is("Vanilla")) {
            onVanillaUpdate();
            return;
        }
        if (mode.is("LonyGrief")) {
            onLonyGriefMove();
        }
    }

    private void onVanillaUpdate() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null
                || mc.player.isPassenger() || mc.player.getAbilities().flying) return;

        double motionY = mc.player.getDeltaMovement().y;
        if (!MoveUtil.isMoving()) {
            mc.player.setDeltaMovement(0.0D, motionY, 0.0D);
            return;
        }

        double targetSpeed = vanillaSpeed.getValue() / 20.0D;
        double yaw = MoveUtil.direction(mc.player.getYRot(), moveForward(), moveStrafe());
        mc.player.setDeltaMovement(-Math.sin(yaw) * targetSpeed, motionY, Math.cos(yaw) * targetSpeed);
    }

    private void onHolyWorldEntityUpdate() {
        if (mc.player == null || mc.level == null) return;

        int collisions = 0;
        AABB expandedBox = mc.player.getBoundingBox().inflate(holyWorldExpand.getValue());
        Entity nearest = null;
        double bestSq = Double.MAX_VALUE;
        double maxRangeSq = holyWorldRange.getValue() * holyWorldRange.getValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!isHolyWorldEntity(entity)) continue;
            if (expandedBox.intersects(entity.getBoundingBox())) collisions++;
            double dx = entity.getX() - mc.player.getX();
            double dz = entity.getZ() - mc.player.getZ();
            double distanceSq = dx * dx + dz * dz;
            if (distanceSq <= maxRangeSq && distanceSq < bestSq) {
                bestSq = distanceSq;
                nearest = entity;
            }
        }

        double finalSpeed = holyWorldSpeed.getValue() * 0.01D * collisions;
        if (finalSpeed <= 0.0D || collisions <= 0) return;
        if (holyWorldRequireMoving.getValue() && !MoveUtil.isMoving()) return;

        if (nearest != null) {
            double[] direction = directionToPoint(mc.player.position(), nearest.position(), finalSpeed);
            addVelocity(direction[0], 0.0D, direction[1]);
        }
    }

    private boolean isHolyWorldEntity(Entity entity) {
        if (entity == mc.player) return false;
        if (holyWorldOnlyPlayers.getValue() && !(entity instanceof Player)) return false;
        return entity instanceof LivingEntity || entity instanceof AbstractBoat;
    }

    private static double[] directionToPoint(Vec3 from, Vec3 to, double speed) {
        double dx = to.x - from.x;
        double dz = to.z - from.z;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length == 0.0D) return new double[]{0.0D, 0.0D};
        return new double[]{dx / length * speed, dz / length * speed};
    }

    private void onTimerUpdate() {
        timerModeActive = true;
        if (!canUseTimer()) {
            resetTimerCycle(true);
            return;
        }

        if (timerRecoveryTicks > 0) {
            TimerManager.getInstance().resetSpeed();
            timerRecoveryTicks--;
            return;
        }

        int cycleLength = TIMER_SLOW_TICKS + TIMER_FAST_BURST_TICKS * 2 + 2;
        int cycleTick = timerTicks % cycleLength;
        if (cycleTick < TIMER_SLOW_TICKS) {
            TimerManager.getInstance().setSpeed(TIMER_SLOW_SPEED);
        } else {
            int burstTick = cycleTick - TIMER_SLOW_TICKS;
            boolean normalTick = burstTick == TIMER_FAST_BURST_TICKS
                    || burstTick == TIMER_FAST_BURST_TICKS * 2 + 1;
            TimerManager.getInstance().setSpeed(normalTick ? 1.0F : TIMER_FAST_SPEED);
        }
        timerTicks++;
    }

    private void onTimer2Update() {
        timerModeActive = true;
        if (!canUseTimer()) {
            resetTimer2Cycle(true);
            return;
        }

        long now = System.currentTimeMillis();
        if (timer2PhaseStartedAt == 0L) {
            timer2PhaseStartedAt = now;
            timer2FastPhase = false;
        }

        long phaseDuration = timer2FastPhase ? TIMER2_FAST_TIME_MS : TIMER2_SLOW_TIME_MS;
        if (now - timer2PhaseStartedAt >= phaseDuration) {
            timer2FastPhase = !timer2FastPhase;
            timer2PhaseStartedAt = now;
        }

        TimerManager.getInstance().setSpeed(timer2FastPhase ? TIMER2_FAST_SPEED : TIMER2_SLOW_SPEED);
    }

    private void onCollisionUpdate() {
        if (!MoveUtil.isMoving() || mc.level == null) return;

        double scanRadius = 1.25;
        List<Entity> nearbyEntities = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(scanRadius));
        for (Entity entity : nearbyEntities) {
            if (!(entity instanceof Player) || entity == mc.player) continue;

            double distanceX = Math.abs(mc.player.getX() - entity.getX());
            double distanceZ = Math.abs(mc.player.getZ() - entity.getZ());
            if (distanceX > 2.25 || distanceZ > 1.5) continue;

            double entitySpeed = getEntitySpeed(entity);
            if (entitySpeed < 5) {
                int collisions = 0;
                for (Entity collisionEntity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(0.1))) {
                    if (collisionEntity != mc.player) collisions++;
                }
                if (collisions > 0) {
                    double[] motion = MoveUtil.forward(0.02);
                    addVelocity(motion[0], 0.0, motion[1]);
                }
            } else {
                double checkRadius = 1.25;
                int collisions = 0;
                for (Entity collisionEntity : mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(checkRadius))) {
                    if (collisionEntity != mc.player && mc.player.distanceTo(collisionEntity) <= checkRadius) {
                        collisions++;
                    }
                }
                if (collisions > 0) {
                    double[] motion = MoveUtil.forward(0.0335);
                    addVelocity(motion[0], 0.0, motion[1]);
                }
            }
            break;
        }
    }

    private void onLonyGriefMove() {
        if (!canUseMovementSpeed()) {
            resetMovementState(true);
            return;
        }

        TimerManager.getInstance().setSpeed(1.825F);
        if (ticks > 3) {
            double bst = 0.03;
            if (ticks % 2 == 0) {
                addVelocity(0.0D, 0.03D, 0.0D);
                bst = mc.player.onGround() ? 0.085 : 0.03;
            }
            double yaw = MoveUtil.direction(mc.player.getYRot(), moveForward(), moveStrafe());
            double xt = MoveUtil.isMoving() ? -Math.sin(yaw) : 0.0;
            double zt = MoveUtil.isMoving() ? Math.cos(yaw) : 0.0;
            addVelocity(xt * bst, 0.0D, zt * bst);
        }
        ticks++;
    }

    private void onMoveInput(MovementInputEvent.MovementInputEventData event) {
        if (mc.player == null) return;
        if (mode.is("Grim Timer")) {
            if (canUseTimer() && mc.player.onGround()) mc.player.jumpFromGround();
            return;
        }
        if (mode.is("Grim Timer2")) {
            if (timer2FastPhase && canUseTimer() && mc.player.onGround()) mc.player.jumpFromGround();
            return;
        }
        if (mode.is("LonyGrief")) {
            if (!canUseMovementSpeed()) return;
            if (mc.player.verticalCollision) groundTicks++;
            else groundTicks = 0;
            if (groundTicks >= 1) mc.player.jumpFromGround();
        }
    }

    private void onPostMotion() {
        if (!mode.is("LonyGrief") || !canUseMovementSpeed()) return;
        if (ticks % 2 == 0) {
            TimerManager.getInstance().setSpeed(0.3F);
            sendSilentPacket(new ServerboundPlayerCommandPacket(mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
        }
    }

    private void onPacket(PacketEvent.PacketEventData event) {
        if (mc.player == null || !event.isReceive()) return;
        if (!(event.packet() instanceof ClientboundPlayerPositionPacket)) return;
        if (mode.is("Grim Timer")) {
            timerTicks = 0;
            timerRecoveryTicks = TIMER_RECOVERY_TICKS;
            TimerManager.getInstance().resetSpeed();
        }
        if (mode.is("LonyGrief")) {
            if (ticks % 2 == 1) ticks++;
            TimerManager.getInstance().setSpeed(1.0F);
        }
    }

    private double getEntitySpeed(Entity entity) {
        Vec3 currentPos = entity.position();
        Vec3 previousPos = previousPositions.getOrDefault(entity, currentPos);
        double dx = currentPos.x - previousPos.x;
        double dz = currentPos.z - previousPos.z;
        previousPositions.put(entity, currentPos);
        return Math.sqrt(dx * dx + dz * dz) * 20.0;
    }

    private void addVelocity(double x, double y, double z) {
        Vec3 vel = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(vel.x + x, vel.y + y, vel.z + z);
    }

    private boolean canUseMovementSpeed() {
        return mc.player != null
                && mc.level != null
                && mc.getConnection() != null
                && MoveUtil.isMoving()
                && !mc.player.isPassenger()
                && !mc.player.getAbilities().flying;
    }

    private boolean canUseTimer() {
        return canUseMovementSpeed()
                && !mc.player.isFallFlying()
                && !mc.player.isInWater()
                && !mc.player.isInLava()
                && !mc.player.onClimbable();
    }

    private void syncSelectedMode() {
        String selectedMode = mode.getValue();
        if (selectedMode.equals(activeMode)) return;

        resetMovementState(false);
        resetTimerState(true);
        activeMode = selectedMode;

        if (mode.is("Grim Timer")) {
            timerModeActive = true;
            TimerManager.getInstance().setSpeed(TIMER_SLOW_SPEED);
        } else if (mode.is("Grim Timer2")) {
            timerModeActive = true;
            timer2PhaseStartedAt = System.currentTimeMillis();
            timer2FastPhase = false;
            TimerManager.getInstance().setSpeed(TIMER2_SLOW_SPEED);
        }
    }

    private void resetMovementState(boolean resetTimer) {
        ticks = 0;
        groundTicks = 0;
        if (resetTimer) TimerManager.getInstance().resetSpeed();
    }

    private void resetTimerCycle(boolean resetTimer) {
        timerTicks = 0;
        timerRecoveryTicks = 0;
        if (resetTimer) TimerManager.getInstance().resetSpeed();
    }

    private void resetTimer2Cycle(boolean resetTimer) {
        timer2FastPhase = false;
        timer2PhaseStartedAt = 0L;
        if (resetTimer) TimerManager.getInstance().resetSpeed();
    }

    private void resetTimerState(boolean resetTimer) {
        timerModeActive = false;
        resetTimerCycle(false);
        resetTimer2Cycle(false);
        if (resetTimer) TimerManager.getInstance().resetSpeed();
    }

    private float moveForward() {
        return mc.player.input.getMoveVector().y;
    }

    private float moveStrafe() {
        return mc.player.input.getMoveVector().x;
    }
}
