package tokyo.ghoul.api.utils.rotation.manager;


import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.*;
import tokyo.ghoul.api.event.events.other.RotationUpdateEvent;
import tokyo.ghoul.api.event.events.player.other.MovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.PostRotationMovementInputEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.player.move.VelocityEvent;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.misc.MouseProfile;
import tokyo.ghoul.api.utils.task.TaskPriority;
import tokyo.ghoul.api.utils.task.TaskProcessor;

@Getter
@Setter
public class RotationManager implements QuickImports {
    @Getter private final static RotationManager instance = new RotationManager();

    private RotationPlan lastRotationPlan;
    private final TaskProcessor<RotationPlan> rotationPlanRequestProcessor = new TaskProcessor<>();
    private Rotation currentRotation;
    private Rotation previousRotation;
    private Rotation serverRotation = Rotation.DEFAULT;
    private long lastRotationUpdateMs;
    private long rotationUpdateIntervalMs;

    public void load() {
        VelocityEvent.getInstance().subscribe(new Listener<>(event -> {
            if (getCurrentRotationPlan() != null && getCurrentRotationPlan().moveCorrection()) {
                var input = event.getMovementInput();
                double length = input.lengthSqr();
                if (length < 1.0E-7) {
                    event.setVelocity(net.minecraft.world.phys.Vec3.ZERO);
                } else {
                    var movement = (length > 1.0 ? input.normalize() : input).scale(event.getSpeed());
                    float radians = getRotation().getYRot() * ((float) Math.PI / 180.0f);
                    float sin = Mth.sin(radians);
                    float cos = Mth.cos(radians);
                    event.setVelocity(new net.minecraft.world.phys.Vec3(movement.x * cos - movement.z * sin, movement.y, movement.z * cos + movement.x * sin));
                }
            }
        }));

        PacketEvent.getInstance().subscribe(new Listener<>(event -> {
            if (event.isSend()) {
                Rotation rotation;

                if (event.packet() instanceof ServerboundMovePlayerPacket packet) {
                    if (packet.hasRotation()) {
                        rotation = new Rotation(packet.getYRot(1f), packet.getXRot(1f));
                    } else {
                        return;
                    }
                } else if (event.packet() instanceof ClientboundPlayerPositionPacket packet) {
                    rotation = new Rotation(packet.change().yRot(), packet.change().xRot());
                } else {
                    return;
                }

                if (!PacketEvent.getInstance().isCancel()) {
                    serverRotation = rotation;
                }
            }
        }));

        UpdateEvent.getInstance().subscribe(new Listener<>(event -> MouseProfile.tick()));

        MovementInputEvent.getInstance().subscribe(new Listener<>(event -> {
            PostRotationMovementInputEvent.getInstance().call();
        }));

        GameLoopEvent.getInstance().subscribe(new Listener<>(event -> {
            if (mc.player == null) return;
            RotationUpdateEvent.getInstance().call();
            update();
        }));
    }

    private void setRotation(Rotation value) {
        previousRotation = (value == null) ? (currentRotation != null ? currentRotation : mc.player != null ? new Rotation(mc.player.getYRot(), mc.player.getXRot()) : Rotation.DEFAULT) : currentRotation;
        currentRotation = value;
    }

    public Rotation getRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return currentRotation != null ? currentRotation : RotationUtil.fromVec2f(mc.player.getRotationVector());
    }

    public Rotation getPreviousRotation() {
        if (mc.player == null) return Rotation.DEFAULT;
        return previousRotation != null ? previousRotation : RotationUtil.fromVec2f(mc.player.getRotationVector());
    }

    public RotationPlan getCurrentRotationPlan() {
        return rotationPlanRequestProcessor.fetchActiveTaskValue() != null ? rotationPlanRequestProcessor.fetchActiveTaskValue() : lastRotationPlan;
    }

    public void addRotation(Rotation.VecRotation vecRotation, LivingEntity entity, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(vecRotation.rotation(), vecRotation.vec(), entity, provider), requestPriority, provider);
    }

    public void addRotation(Rotation rotation, RotationStrategy configurable, TaskPriority requestPriority, Module provider) {
        addRotation(configurable.createRotationPlan(rotation, provider), requestPriority, provider);
    }

    private void addRotation(RotationPlan plan, TaskPriority requestPriority, Module provider) {
        rotationPlanRequestProcessor.addTask(new TaskProcessor.Task<>(plan.ticksUntilReset(), requestPriority.getPriority(), provider, plan));
    }

    public void updateNow() {
        if (mc.player == null) return;
        update();
    }

    private void update() {
        long now = System.currentTimeMillis();
        if (lastRotationUpdateMs != 0L) {
            rotationUpdateIntervalMs = Math.max(2L, Math.min(100L, now - lastRotationUpdateMs));
        }
        lastRotationUpdateMs = now;

        RotationPlan activePlan = getCurrentRotationPlan();
        if (activePlan == null) {
            return;
        }

        Rotation playerRotation = RotationUtil.fromVec2f(mc.player.getRotationVector());

        if (rotationPlanRequestProcessor.fetchActiveTaskValue() == null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(serverRotation, playerRotation);
            if (differenceFromCurrentToPlayer < activePlan.resetThreshold()) {
                if (activePlan.clientLook() && currentRotation != null) {
                    mc.player.setYRot(currentRotation.getYRot() + computeAngleDifference(mc.player.getYRot(), currentRotation.getYRot()));
                    mc.player.setXRot(currentRotation.getXRot() + computeAngleDifference(mc.player.getXRot(), currentRotation.getXRot()));
                }
                setRotation(null);
                lastRotationPlan = null;
                return;
            }
        }
        Rotation newRotation = activePlan.nextRotation(currentRotation != null ? currentRotation : playerRotation, rotationPlanRequestProcessor.fetchActiveTaskValue() == null).adjustSensitivity();
        setRotation(newRotation);
        
        if (activePlan.clientLook()) {
            mc.player.setYRot(newRotation.getYRot());
            mc.player.setXRot(newRotation.getXRot());
            mc.player.yRotO = newRotation.getYRot();
            mc.player.xRotO = newRotation.getXRot();
        }

        lastRotationPlan = activePlan;

        rotationPlanRequestProcessor.tick(1);
    }

    public void applyClientLookFrame() {
        if (Minecraft.getInstance() == null || Minecraft.getInstance().player == null || currentRotation == null) return;
        RotationPlan activePlan = getCurrentRotationPlan();
        if (activePlan == null || !activePlan.clientLook()) return;

        Rotation previous = getPreviousRotation();
        float t = rotationUpdateIntervalMs <= 0L ? 1.0f
                : (float) Math.min(1.0, (double) (System.currentTimeMillis() - lastRotationUpdateMs) / rotationUpdateIntervalMs);

        float yaw = currentRotation.getYRot();
        float pitch = currentRotation.getXRot();
        float renderYaw = previous.getYRot() + Mth.wrapDegrees(yaw - previous.getYRot()) * t;
        float renderPitch = previous.getXRot() + (pitch - previous.getXRot()) * t;

        mc.player.setYRot(renderYaw);
        mc.player.setXRot(renderPitch);
        mc.player.yRotO = renderYaw;
        mc.player.xRotO = renderPitch;
    }

    private double computeRotationDifference(Rotation a, Rotation b) {
        return Math.hypot(Mth.abs(computeAngleDifference(a.getXRot(), b.getXRot())), Mth.abs((a.getXRot() - b.getXRot())));
    }

    private float computeAngleDifference(float a, float b) {
        return Mth.wrapDegrees(a - b);
    }
}
