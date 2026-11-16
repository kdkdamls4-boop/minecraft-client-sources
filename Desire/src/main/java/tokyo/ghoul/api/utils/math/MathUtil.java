package tokyo.ghoul.api.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.world.entity.Entity;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.math.BigDecimal;
import java.math.RoundingMode;

@UtilityClass
public class MathUtil implements QuickImports {
    private final RandomUtil randomUtil = new RandomUtil();

    public double getEntityBPS(Entity entity) {
        return Math.hypot(entity.xOld - entity.getX(), entity.zOld - entity.getZ()) * 20;
    }

    public float interpolate(double oldValue, double newValue) {
        return (float) (oldValue + (newValue - oldValue) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    public double interpolate(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public float interpolate(float start, float end, float delta) {
        return start + (end - start) * delta;
    }

    public int interpolate(int oldValue, int newValue, float interpolationValue) {
        return (int)(oldValue + (newValue - oldValue) * interpolationValue);
    }

    public double round(double value, double step) {
        double v = (double) Math.round(value / step) * step;
        int scale = Math.min(4, Math.max(2, getDecimalPlaces(step)));
        BigDecimal bd = BigDecimal.valueOf(v);
        bd = bd.setScale(scale, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    public float round(float value, float step) {
        double v = (double) Math.round(value / step) * step;
        int scale = Math.min(4, Math.max(2, getDecimalPlaces(step)));
        BigDecimal bd = BigDecimal.valueOf(v);
        bd = bd.setScale(scale, RoundingMode.HALF_UP);
        return bd.floatValue();
    }

    private int getDecimalPlaces(double step) {
        try {
            return BigDecimal.valueOf(step).stripTrailingZeros().scale();
        } catch (Exception e) {
            return 2;
        }
    }

    public int randomInRange(int min, int max) {
        return randomUtil.randomInRange(min, max);
    }

    public float randomInRange(float min, float max) {
        return randomUtil.randomInRange(min, max);
    }

    public double randomInRange(double min, double max) {
        return randomUtil.randomInRange(min, max);
    }
}