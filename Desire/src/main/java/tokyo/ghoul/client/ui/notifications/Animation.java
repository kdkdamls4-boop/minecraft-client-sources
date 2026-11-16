package tokyo.ghoul.client.ui.notifications;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.utils.animation.Easing;

@Getter
@Setter
public class Animation {
    private double value;
    private double startValue;
    private double target;
    private long duration;
    private Easing easing;
    private long startTime;
    private boolean finished = true;

    public Animation(long duration, Easing easing) {
        this.duration = duration;
        this.easing = easing;
        this.value = 0.0;
        this.target = 0.0;
        this.startValue = 0.0;
    }

    public Animation(long duration) {
        this(duration, Easing.EXPO_OUT);
    }

    public void animate(double target) {
        if (this.target != target) {
            this.startValue = this.value;
            this.target = target;
            this.startTime = System.currentTimeMillis();
            this.finished = false;
        }
    }

    public void animate(boolean target) {
        animate(target ? 1.0 : 0.0);
    }

    public void update() {
        if (finished) return;

        long timeAlive = System.currentTimeMillis() - startTime;
        double progress = Math.min(1.0, (double) timeAlive / Math.max(1L, duration));

        if (progress >= 1.0) {
            value = target;
            finished = true;
        } else {
            double eased = easing.apply(progress);
            value = startValue + (target - startValue) * eased;
        }
    }
}
