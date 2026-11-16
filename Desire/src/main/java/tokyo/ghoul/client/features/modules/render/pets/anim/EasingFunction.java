package tokyo.ghoul.client.features.modules.render.pets.anim;

@FunctionalInterface
public interface EasingFunction {
    double apply(double progress);
}
