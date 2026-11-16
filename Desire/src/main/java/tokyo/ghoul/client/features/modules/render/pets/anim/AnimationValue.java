package tokyo.ghoul.client.features.modules.render.pets.anim;

public class AnimationValue {
    private EasedAnimation animation;
    private float output;
    private EasingType easing = EasingType.LINEAR;
    private float endpoint;

    public AnimationValue() {
        animation = new EasedAnimation().setSize(0.0F).setSpeed(0).setForward(false).setEasing(easing);
    }
    public float animate(float endpoint, int speed) {
        speed = Math.max(1, speed);
        output = this.endpoint - animation.get();
        this.endpoint = endpoint;
        if (output != this.endpoint - endpoint) {
            animation = new EasedAnimation().setSize(this.endpoint - output).setSpeed(speed).setForward(false).setEasing(easing);
        }
        return output;
    }
    public float get() {
        output = endpoint - animation.get();
        return output;
    }
    public AnimationValue easing(EasingType easing) { this.easing = easing; return this; }
}
