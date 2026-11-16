package tokyo.ghoul.api.utils.math;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Mth;

@Getter
@Setter
public class PIDController {
    private float kp;
    private float ki;
    private float kd;

    private float integral;
    private float prevError;
    private float prevDerivative;

    private float maxIntegral;
    private float integralDecay;
    private float derivativeFilterAlpha;
    private boolean angleWrap;

    private long lastUpdateTimeMs;

    public PIDController(float kp, float ki, float kd, float maxIntegral, boolean angleWrap) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
        this.maxIntegral = maxIntegral;
        this.integralDecay = 0.96f;
        this.derivativeFilterAlpha = 0.65f;
        this.angleWrap = angleWrap;
        this.reset();
    }

    public PIDController(float kp, float ki, float kd, float maxIntegral) {
        this(kp, ki, kd, maxIntegral, false);
    }

    public void reset() {
        this.integral = 0.0f;
        this.prevError = 0.0f;
        this.prevDerivative = 0.0f;
        this.lastUpdateTimeMs = 0L;
    }

    public void setGains(float kp, float ki, float kd) {
        this.kp = kp;
        this.ki = ki;
        this.kd = kd;
    }

    public float compute(float current, float target) {
        float error = angleWrap ? Mth.wrapDegrees(target - current) : (target - current);

        long now = System.currentTimeMillis();
        float dt;
        if (lastUpdateTimeMs <= 0L) {
            dt = 1.0f;
        } else {
            long deltaMs = now - lastUpdateTimeMs;
            dt = Mth.clamp((float) deltaMs / 16.667f, 0.2f, 3.0f);
        }
        lastUpdateTimeMs = now;

        float pTerm = kp * error;

        if (Math.signum(error) != Math.signum(prevError) && prevError != 0.0f) {
            integral *= 0.45f;
        }
        integral = integral * integralDecay + error * dt;
        integral = Mth.clamp(integral, -maxIntegral, maxIntegral);
        float iTerm = ki * integral;

        float rawDerivative = (error - prevError) / dt;
        float filteredDerivative = prevDerivative + derivativeFilterAlpha * (rawDerivative - prevDerivative);
        prevDerivative = filteredDerivative;
        prevError = error;
        float dTerm = kd * filteredDerivative;

        return pTerm + iTerm + dTerm;
    }
}
