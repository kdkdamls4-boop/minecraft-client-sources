package tokyo.ghoul.api.utils.rotation.misc;

import lombok.experimental.UtilityClass;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.api.utils.rotation.manager.RotationPlan;
import tokyo.ghoul.client.features.modules.combat.AuraModule;

import java.util.Arrays;

/**
 * Замеряет реальные движения мыши игрока в отсчётах за тик (DPI * скорость руки при текущей сенсе).
 * Пока замеров мало, отдаёт дефолт ~ 800 DPI при 3 дюйм/с.
 */
@UtilityClass
public class MouseProfile implements QuickImports {
    private static final int CAPACITY = 1200;
    private static final int MIN_SAMPLES = 80;
    private static final float DEFAULT_PEAK = 120f;
    private static final float MIN_COUNTS = 2f;

    private final float[] samples = new float[CAPACITY];
    private int size;
    private int head;
    private int sinceRecalc;
    private float cachedPeak = DEFAULT_PEAK;

    private float lastYaw;
    private float lastPitch;
    private boolean hasLast;

    public void tick() {
        if (mc.player == null || mc.gui.screen() != null) {
            hasLast = false;
            return;
        }

        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        if (hasLast && !isRotationForced()) {
            float dyDeg = Mth.wrapDegrees(yaw - lastYaw);
            float dpDeg = pitch - lastPitch;
            if (Math.abs(dyDeg) < 90f && Math.abs(dpDeg) < 90f) {
                float gcd = MouseUtil.getGCD();
                float counts = (float) Math.hypot(dyDeg / gcd, dpDeg / gcd);
                if (counts >= MIN_COUNTS) add(counts);
            }
        }
        lastYaw = yaw;
        lastPitch = pitch;
        hasLast = true;
    }

    /** Типичная быстрая скорость игрока (90-й перцентиль) в отсчётах за тик. */
    public float getPeakCounts() {
        if (size < MIN_SAMPLES) return DEFAULT_PEAK;
        if (sinceRecalc >= 40 || cachedPeak == DEFAULT_PEAK) {
            float[] sorted = Arrays.copyOf(samples, size);
            Arrays.sort(sorted);
            cachedPeak = Mth.clamp(sorted[(int) (size * 0.9f)], 20f, 2000f);
            sinceRecalc = 0;
        }
        return cachedPeak;
    }

    public boolean isCalibrated() {
        return size >= MIN_SAMPLES;
    }

    private void add(float counts) {
        samples[head] = counts;
        head = (head + 1) % CAPACITY;
        if (size < CAPACITY) size++;
        sinceRecalc++;
    }

    // поворот игрока выставляет не мышь, а клиент — такие тики не считаем
    private boolean isRotationForced() {
        RotationPlan plan = RotationManager.getInstance().getCurrentRotationPlan();
        if (plan != null && plan.clientLook()) return true;
        AuraModule aura = AuraModule.getInstance();
        return aura.isEnabled() && aura.target != null && aura.getAimMode().is("Legit");
    }
}
