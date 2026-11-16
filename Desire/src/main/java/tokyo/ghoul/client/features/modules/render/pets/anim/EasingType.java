package tokyo.ghoul.client.features.modules.render.pets.anim;

public enum EasingType {
    LINEAR(progress -> progress),
    BOTH_SINE(progress -> -(Math.cos(Math.PI * progress) - 1.0D) / 2.0D),
    OUT_CUBIC(progress -> {
        double inverse = 1.0D - progress;
        return 1.0D - inverse * inverse * inverse;
    }),
    IN_OUT_CUBIC(progress -> progress < 0.5D ? 4.0D * progress * progress * progress : 1.0D - Math.pow(-2.0D * progress + 2.0D, 3.0D) / 2.0D);

    private final EasingFunction function;
    EasingType(EasingFunction function) { this.function = function; }
    public double apply(double progress) { return function.apply(progress); }
}
