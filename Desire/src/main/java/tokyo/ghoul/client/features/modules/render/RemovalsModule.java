package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;

@ModuleRegister(name = "Removals", description = "Удаление лишних эффектов", category = Category.RENDER)
public class RemovalsModule extends Module {
    @Getter private static final RemovalsModule instance = new RemovalsModule();

    private final MultiBooleanSetting overlay = new MultiBooleanSetting("Overlay").value(
            new BooleanSetting("Fire").value(true),
            new BooleanSetting("Bad Effects").value(true),
            new BooleanSetting("Shaking").value(false),
            new BooleanSetting("Totem Overlay").value(false),
            new BooleanSetting("Scoreboard").value(false),
            new BooleanSetting("Boss Bar").value(false)
    );
    private final MultiBooleanSetting world = new MultiBooleanSetting("World").value(
            new BooleanSetting("Weather").value(true),
            new BooleanSetting("Totem Particles").value(false),
            new BooleanSetting("Effects").value(false)
    );
    private final MultiBooleanSetting sounds = new MultiBooleanSetting("Sounds").value(
            new BooleanSetting("Hit").value(false),
            new BooleanSetting("Hurt").value(false),
            new BooleanSetting("Totem").value(false),
            new BooleanSetting("Step").value(false),
            new BooleanSetting("Eat").value(false),
            new BooleanSetting("Firework").value(false),
            new BooleanSetting("Warden").value(false)
    );

    public RemovalsModule() {
        addSettings(overlay, world, sounds);
    }

    public boolean isFireOverlay() {
        return selected(overlay, "Fire");
    }

    public boolean isHurtCamera() {
        return selected(overlay, "Shaking");
    }

    public boolean isBadEffects() {
        return selected(overlay, "Bad Effects");
    }

    public boolean isTotemOverlay() {
        return selected(overlay, "Totem Overlay");
    }

    public boolean isScoreboard() {
        return selected(overlay, "Scoreboard");
    }

    public boolean isBossBar() {
        return selected(overlay, "Boss Bar");
    }

    public boolean isWaterOverlay() {
        return false;
    }

    public boolean isInwallOverlay() {
        return false;
    }

    public boolean isGlowEffect() {
        return false;
    }

    public boolean isWeather() {
        return selected(world, "Weather");
    }

    public static boolean shouldRemoveParticle(ParticleOptions options) {
        RemovalsModule module = instance;
        if (!module.isEnabled() || options == null) return false;
        if (module.world.isEnabled("Totem Particles") && options.getType() == ParticleTypes.TOTEM_OF_UNDYING) {
            return true;
        }
        if (module.world.isEnabled("Effects")) {
            return options.getType() == ParticleTypes.ENTITY_EFFECT;
        }
        return false;
    }

    public static boolean shouldRemoveSound(SoundEvent soundEvent) {
        RemovalsModule module = instance;
        if (!module.isEnabled() || soundEvent == null) return false;
        String soundPath = soundEvent.location().getPath();
        if (module.sounds.isEnabled("Totem") && soundPath.contains("totem")) return true;
        if (module.sounds.isEnabled("Firework") && soundPath.contains("firework")) return true;
        if (module.sounds.isEnabled("Step") && soundPath.contains("step")) return true;
        if (module.sounds.isEnabled("Eat") && (soundPath.contains("eat") || soundPath.contains("drink") || soundPath.contains("burp"))) return true;
        if (module.sounds.isEnabled("Hit") && (soundPath.contains("attack") || soundPath.contains("hit"))) return true;
        if (module.sounds.isEnabled("Warden") && (soundPath.contains("warden") || soundPath.contains("sculk_sensor")
                || soundPath.contains("sculk_shrieker") || soundPath.contains("shriek"))) {
            return true;
        }
        return module.sounds.isEnabled("Hurt") && soundPath.contains("hurt");
    }

    private boolean selected(MultiBooleanSetting setting, String name) {
        return isEnabled() && setting.isEnabled(name);
    }

    @Override
    public void onEvent() {
    }
}
