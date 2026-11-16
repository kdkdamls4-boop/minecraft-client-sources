package tokyo.ghoul.api.utils.rotation.rotations;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationMode;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroControl;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroFeatures;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroManager;
import tokyo.ghoul.api.utils.rotation.neuro.NeuroModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

public class NeuroRotation extends RotationMode {
    private static final float MAX_YAW_STEP = 50f;
    private static final float MAX_PITCH_STEP = 35f;
    private static final double NOISE_ACCEL_KEEP = 0.5;
    private static final double NOISE_VEL_KEEP = 0.7;
    private static final double NOISE_NORM = noiseNorm();

    private final Supplier<Float> temperature;
    private final Random random = new Random();
    private final SmoothRotation fallback = new SmoothRotation();

    private final List<double[]> targetTrail = new ArrayList<>();

    private NeuroModel stateModel;
    private float[] state;
    private boolean initialized;
    private int lastTick;
    private Rotation cached;
    private Entity trackedEntity;
    private int reaction;
    private int lastAttackTick = -NeuroFeatures.MAX_SINCE_ATTACK;
    private boolean warned;

    private double accelNoiseYaw, accelNoisePitch, velNoiseYaw, velNoisePitch;
    private float residualYaw, residualPitch;

    public NeuroRotation(Supplier<Float> temperature) {
        super("Neuro");
        this.temperature = temperature;
    }

    public void reset() {
        initialized = false;
        trackedEntity = null;
        targetTrail.clear();
        accelNoiseYaw = accelNoisePitch = velNoiseYaw = velNoisePitch = 0;
        residualYaw = residualPitch = 0;
        if (state != null) Arrays.fill(state, 0f);
    }

    public void onAttack() {
        if (mc.player != null) lastAttackTick = mc.player.tickCount;
    }

    @Override
    public Rotation process(Rotation currentRotation, Rotation targetRotation, Vec3 vec3d, Entity entity) {
        NeuroModel model = NeuroManager.getInstance().getActiveModel();
        if (model == null || mc.player == null) {
            if (!warned) {
                print("Нейро-модель не выбрана: §e.neuro play <модель>§r, пока Smooth");
                warned = true;
            }
            return fallback.process(currentRotation, targetRotation, vec3d, entity);
        }
        warned = false;

        if (model != stateModel) {
            stateModel = model;
            state = model.newState();
            initialized = false;
        }

        int tick = mc.player.tickCount;
        if (initialized && tick == lastTick && cached != null) {
            return cached;
        }
        if (!initialized || tick - lastTick > 2 || tick < lastTick) {
            reset();
            initialized = true;
        }
        lastTick = tick;

        float[] features = buildFeatures(currentRotation, targetRotation, entity, model, tick);
        NeuroModel.Prediction prediction;
        try {
            prediction = model.predict(features, state);
        } catch (Exception e) {
            print("Нейро-модель упала: " + e.getMessage());
            cached = currentRotation;
            return cached;
        }

        stepNoise();
        float temp = temperature.get();
        boolean observed = features[7] > 0.5f;
        float yawStep = NeuroControl.step(prediction.muYaw(), prediction.sigmaYaw(), velNoiseYaw * NOISE_NORM, temp,
                features[0], features[5], observed);
        float pitchStep = NeuroControl.step(prediction.muPitch(), prediction.sigmaPitch(), velNoisePitch * NOISE_NORM, temp,
                features[1], features[5] * 2, observed);
        yawStep = Mth.clamp(yawStep, -MAX_YAW_STEP, MAX_YAW_STEP);
        pitchStep = Mth.clamp(pitchStep, -MAX_PITCH_STEP, MAX_PITCH_STEP);

        float gcd = MouseUtil.getGCD();
        float wantYaw = yawStep + residualYaw;
        float wantPitch = pitchStep + residualPitch;
        float quantYaw = Math.round(wantYaw / gcd) * gcd;
        float quantPitch = Math.round(wantPitch / gcd) * gcd;
        residualYaw = wantYaw - quantYaw;
        residualPitch = wantPitch - quantPitch;

        float pitch = currentRotation.getXRot() + quantPitch;
        if (pitch > 90f || pitch < -90f) {
            pitch = Mth.clamp(pitch, -90f, 90f);
            residualPitch = 0;
        }
        cached = new Rotation(currentRotation.getYRot() + quantYaw, pitch);
        return cached;
    }

    private float[] buildFeatures(Rotation current, Rotation targetRotation, Entity entity, NeuroModel model, int tick) {
        if (entity != trackedEntity) {
            trackedEntity = entity;
            targetTrail.clear();
            int base = model.getReaction();
            int roll = random.nextInt(10);
            reaction = Math.max(2, roll < 6 ? base : roll < 9 ? base + 1 : base - 1);
        }

        Vec3 eye = mc.player.getEyePosition();
        boolean observed;
        float errYaw = 0, errPitch = 0, velYaw = 0, velPitch = 0, distance = 0, size = 0;

        if (entity != null) {
            AABB box = entity.getBoundingBox();
            Vec3 center = box.getCenter();
            targetTrail.add(new double[]{center.x, center.y, center.z, box.getXsize() / 2.0});
            if (targetTrail.size() > reaction + 2) targetTrail.remove(0);

            int seenIndex = targetTrail.size() - 1 - reaction;
            observed = seenIndex >= 0;
            if (observed) {
                double[] seen = targetTrail.get(seenIndex);
                float[] aim = NeuroFeatures.angles(eye.x, eye.y, eye.z, seen[0], seen[1], seen[2]);
                errYaw = NeuroFeatures.wrap(aim[0] - current.getYRot());
                errPitch = aim[1] - current.getXRot();
                distance = (float) Math.sqrt(sq(seen[0] - eye.x) + sq(seen[1] - eye.y) + sq(seen[2] - eye.z));
                size = (float) Math.toDegrees(Math.atan2(seen[3], Math.max(0.1, distance)));
                if (seenIndex >= 1) {
                    double[] prev = targetTrail.get(seenIndex - 1);
                    float[] prevAim = NeuroFeatures.angles(eye.x, eye.y, eye.z, prev[0], prev[1], prev[2]);
                    velYaw = NeuroFeatures.wrap(aim[0] - prevAim[0]);
                    velPitch = aim[1] - prevAim[1];
                }
            }
        } else {
            observed = targetRotation != null;
            if (observed) {
                errYaw = NeuroFeatures.wrap(targetRotation.getYRot() - current.getYRot());
                errPitch = targetRotation.getXRot() - current.getXRot();
                distance = 3f;
                size = (float) Math.toDegrees(Math.atan2(0.3, 3.0));
            }
        }

        return NeuroFeatures.build(errYaw, errPitch, velYaw, velPitch, distance, size,
                tick - lastAttackTick, observed);
    }

    private void stepNoise() {
        double keep = Math.sqrt(1 - NOISE_ACCEL_KEEP * NOISE_ACCEL_KEEP);
        accelNoiseYaw = NOISE_ACCEL_KEEP * accelNoiseYaw + keep * random.nextGaussian();
        accelNoisePitch = NOISE_ACCEL_KEEP * accelNoisePitch + keep * random.nextGaussian();
        velNoiseYaw = NOISE_VEL_KEEP * velNoiseYaw + accelNoiseYaw;
        velNoisePitch = NOISE_VEL_KEEP * velNoisePitch + accelNoisePitch;
    }

    private static double noiseNorm() {
        Random rng = new Random(7);
        double keep = Math.sqrt(1 - NOISE_ACCEL_KEEP * NOISE_ACCEL_KEEP);
        double accel = 0, vel = 0, sum = 0;
        int steps = 50_000;
        for (int i = 0; i < steps; i++) {
            accel = NOISE_ACCEL_KEEP * accel + keep * rng.nextGaussian();
            vel = NOISE_VEL_KEEP * vel + accel;
            sum += vel * vel;
        }
        return 1.0 / Math.sqrt(sum / steps);
    }

    private static double sq(double v) {
        return v * v;
    }
}
