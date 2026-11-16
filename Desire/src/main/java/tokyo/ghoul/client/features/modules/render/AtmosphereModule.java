package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.render.ColorMode;

import java.awt.Color;

@ModuleRegister(name = "Atmosphere", description = "Атмосферные эффекты мира", category = Category.RENDER)
public final class AtmosphereModule extends Module {
    public static final String STORM_MANUAL = "Manual";
    public static final String STORM_WEATHER = "Weather";
    public static final String RAIN = "Rain";
    public static final String SNOW = "Snow";

    @Getter private static final AtmosphereModule instance = new AtmosphereModule();

    public final BooleanSetting fog = new BooleanSetting("Fog").value(true);
    public final BooleanSetting nightHaze = new BooleanSetting("Night Haze").value(false);
    public final BooleanSetting rain = new BooleanSetting("Rain").value(true);
    public final ModeSetting rainType = new ModeSetting("Precipitation").values(RAIN, SNOW).value(RAIN)
            .setVisible(rain::getValue);
    public final BooleanSetting lightning = new BooleanSetting("Lightning").value(true);
    public final BooleanSetting advanced = new BooleanSetting("Advanced").value(false);

    public final ModeSetting fogColorMode = ColorMode.setting()
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final ColorSetting fogColor = new ColorSetting("Fog Color").value(new Color(0xFF9AA5C8, true))
            .setVisible(() -> fog.getValue() && advanced.getValue() && ColorMode.isCustom(fogColorMode));
    public final SliderSetting fogDensity = new SliderSetting("Density").value(0.55F).range(0.05F, 2.0F).step(0.01F)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final SliderSetting fogHeight = new SliderSetting("Height").value(48.0F).range(8.0F, 160.0F).step(1.0F)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final SliderSetting ambientStrength = new SliderSetting("Ambient").value(0.5F).range(0.0F, 2.0F).step(0.05F)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final BooleanSetting syncWithSky = new BooleanSetting("Sync With Sky").value(true)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final SliderSetting skySync = new SliderSetting("Sync Amount").value(0.65F).range(0.0F, 1.0F).step(0.05F)
            .setVisible(() -> fog.getValue() && advanced.getValue() && syncWithSky.getValue());
    public final SliderSetting sunsetBoost = new SliderSetting("Sunset Boost").value(1.4F).range(0.0F, 3.0F).step(0.05F)
            .setVisible(() -> fog.getValue() && advanced.getValue() && syncWithSky.getValue());
    public final SliderSetting turbulence = new SliderSetting("Turbulence").value(0.35F).range(0.0F, 1.0F).step(0.05F)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final SliderSetting sunScatter = new SliderSetting("Sun Scatter").value(0.8F).range(0.0F, 2.0F).step(0.05F)
            .setVisible(() -> fog.getValue() && advanced.getValue());
    public final BooleanSetting stormFog = new BooleanSetting("Storm Fog").value(false)
            .setVisible(() -> fog.getValue() && advanced.getValue());

    public final ModeSetting hazeColorMode = ColorMode.setting()
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final ColorSetting hazeColor = new ColorSetting("Haze Color").value(new Color(0xFFB4C4E0, true))
            .setVisible(() -> nightHaze.getValue() && advanced.getValue() && ColorMode.isCustom(hazeColorMode));
    public final SliderSetting hazeAmount = new SliderSetting("Haze Amount").value(0.6F).range(0.05F, 2.0F).step(0.05F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final SliderSetting hazeThickness = new SliderSetting("Haze Thickness").value(5.0F).range(1.0F, 40.0F).step(0.5F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final BooleanSetting hazeFollowGround = new BooleanSetting("Follow Ground").value(true)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final SliderSetting hazeOffset = new SliderSetting("Haze Offset").value(1.5F).range(-8.0F, 24.0F).step(0.5F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue() && hazeFollowGround.getValue());
    public final SliderSetting hazeLevel = new SliderSetting("Haze Level").value(64.0F).range(-64.0F, 320.0F).step(1.0F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue() && !hazeFollowGround.getValue());
    public final SliderSetting hazeBanks = new SliderSetting("Haze Banks").value(0.6F).range(0.0F, 1.0F).step(0.05F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final SliderSetting hazeGlow = new SliderSetting("Haze Glow").value(0.85F).range(0.0F, 2.0F).step(0.05F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final SliderSetting hazeMoonScatter = new SliderSetting("Moon Scatter").value(1.0F).range(0.0F, 3.0F).step(0.05F)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());
    public final BooleanSetting hazeNightOnly = new BooleanSetting("Night Only").value(true)
            .setVisible(() -> nightHaze.getValue() && advanced.getValue());

    public final SliderSetting rainDensity = new SliderSetting("Rain Density").value(1.0F).range(0.1F, 2.0F).step(0.05F)
            .setVisible(() -> rain.getValue() && advanced.getValue());
    public final SliderSetting rainSpeed = new SliderSetting("Rain Speed").value(1.0F).range(0.25F, 3.0F).step(0.05F)
            .setVisible(() -> rain.getValue() && advanced.getValue());
    public final ColorSetting rainColor = new ColorSetting("Rain Color").value(new Color(0xFFC2D2E8, true))
            .setVisible(() -> rain.getValue() && advanced.getValue());
    public final SliderSetting rainOpacity = new SliderSetting("Rain Opacity").value(0.55F).range(0.1F, 1.0F).step(0.05F)
            .setVisible(() -> rain.getValue() && advanced.getValue());
    public final BooleanSetting ripples = new BooleanSetting("Ripples").value(false)
            .setVisible(() -> rain.getValue() && advanced.getValue() && !usesSnow());
    public final SliderSetting rippleStrength = new SliderSetting("Ripple Strength").value(0.5F).range(0.1F, 1.5F).step(0.05F)
            .setVisible(() -> rain.getValue() && advanced.getValue() && !usesSnow() && ripples.getValue());

    public final ModeSetting stormSource = new ModeSetting("Storm Source").values(STORM_MANUAL, STORM_WEATHER).value(STORM_MANUAL)
            .setVisible(() -> rain.getValue() || lightning.getValue() || (fog.getValue() && stormFog.getValue()));
    public final SliderSetting stormPower = new SliderSetting("Storm Power").value(0.5F).range(0.0F, 1.0F).step(0.05F)
            .setVisible(() -> STORM_MANUAL.equals(stormSource.getValue())
                    && (rain.getValue() || lightning.getValue() || (fog.getValue() && stormFog.getValue())));
    public final SliderSetting strikeRate = new SliderSetting("Strike Rate").value(1.0F).range(0.25F, 4.0F).step(0.05F)
            .setVisible(() -> lightning.getValue() && advanced.getValue());
    public final SliderSetting strikeDistance = new SliderSetting("Strike Distance").value(220.0F).range(60.0F, 500.0F).step(10.0F)
            .setVisible(() -> lightning.getValue() && advanced.getValue());
    public final ColorSetting boltColor = new ColorSetting("Bolt Color").value(new Color(0xFFCBDCFF, true))
            .setVisible(() -> lightning.getValue() && advanced.getValue());
    public final SliderSetting boltWidth = new SliderSetting("Bolt Width").value(1.6F).range(0.5F, 5.0F).step(0.1F)
            .setVisible(() -> lightning.getValue() && advanced.getValue());
    public final SliderSetting boltBranches = new SliderSetting("Branches").value(3.0F).range(0.0F, 4.0F).step(1.0F)
            .setVisible(() -> lightning.getValue() && advanced.getValue());
    public final SliderSetting flashStrength = new SliderSetting("Flash").value(1.0F).range(0.0F, 2.0F).step(0.05F)
            .setVisible(() -> lightning.getValue() && advanced.getValue());

    public AtmosphereModule() {
        addSettings(
                fog, nightHaze, rain, rainType, lightning, advanced,
                fogColorMode, fogColor, fogDensity, fogHeight, ambientStrength,
                syncWithSky, skySync, sunsetBoost, turbulence, sunScatter, stormFog,
                hazeColorMode, hazeColor, hazeAmount, hazeThickness, hazeFollowGround,
                hazeOffset, hazeLevel, hazeBanks, hazeGlow, hazeMoonScatter, hazeNightOnly,
                rainDensity, rainSpeed, rainColor, rainOpacity, ripples, rippleStrength,
                stormSource, stormPower, strikeRate, strikeDistance, boltColor, boltWidth,
                boltBranches, flashStrength
        );
    }

    @Override
    public void onEvent() {
    }

    public boolean usesFog() {
        return fog.getValue();
    }

    public boolean usesLightning() {
        return lightning.getValue();
    }

    public boolean usesRain() {
        return rain.getValue();
    }

    public boolean usesSnow() {
        return SNOW.equals(rainType.getValue());
    }

    public boolean usesNightHaze() {
        return nightHaze.getValue();
    }

    public boolean usesAtmosphere() {
        return usesFog() || usesLightning() || usesRain() || usesNightHaze();
    }
}
