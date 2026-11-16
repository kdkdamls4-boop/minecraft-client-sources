package tokyo.ghoul.client.features.modules.render.pets.anim;

import tokyo.ghoul.client.features.modules.render.pets.anim.MSTimer;

public class EasedAnimation {
    private int speed;
    private final MSTimer timer = new MSTimer();
    private double size = 1.0D;
    private boolean forward;
    private EasingType easing = EasingType.LINEAR;

    public void reset() { timer.reset(); }
    public boolean finished() { return timer.finished((long) speed) && forward; }
    public EasedAnimation setEasing(EasingType easing) { this.easing = easing; return this; }
    public EasedAnimation setForward(boolean forward) {
        if (this.forward != forward) {
            this.forward = forward;
            timer.setMillis((long) (System.currentTimeMillis() - size - Math.min(size, (double) timer.getElapsedTime())));
        }
        return this;
    }
    public EasedAnimation snap(boolean forward) {
        this.forward = forward;
        timer.setMillis(System.currentTimeMillis() - (long) speed - 1L);
        return this;
    }
    public boolean finished(boolean direction) { return timer.finished((long) speed) && (direction ? forward : !forward); }
    public float get() {
        if (forward) {
            if (timer.finished((long) speed)) return (float) size;
            return (float) (easing.apply((double) timer.getElapsedTime() / (double) speed) * size);
        }
        if (timer.finished((long) speed)) return 0.0F;
        return (float) ((1.0D - easing.apply((double) timer.getElapsedTime() / (double) speed)) * size);
    }
    public EasedAnimation setSpeed(int speed) { this.speed = speed; return this; }
    public EasedAnimation setSize(float size) { this.size = size; return this; }
}
