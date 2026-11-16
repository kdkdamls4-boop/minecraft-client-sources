package tokyo.ghoul.client.features.modules.render;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.ModeSetting;
import tokyo.ghoul.api.module.setting.MultiBooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.system.backend.Choice;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.FogBlurRenderer;
import tokyo.ghoul.api.utils.render.WetShaderRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.api.utils.render.pipeline.SkyPipelines;

import java.awt.Color;

@ModuleRegister(name = "Ambience", description = "Изменение времени и погоды в мире", category = Category.RENDER)
public class AmbienceModule extends Module {
    @Getter private static final AmbienceModule instance = new AmbienceModule();

    private static boolean skyHookRegistered;

    @AllArgsConstructor
    private enum WorldTime implements ModeSetting.NamedChoice {
        NO_CHANGE("No change"),
        DAWN("Dawn"),
        DAY("Day"),
        NOON("Noon"),
        DUSK("Dusk"),
        NIGHT("Night"),
        MID_NIGHT("Mid Night");

        private final String name;

        @Override
        public String getName() {
            return name;
        }
    }

    private final ModeSetting time = new ModeSetting("Time").value(WorldTime.DAY).values(WorldTime.values());

    private final BooleanSetting customFog = new BooleanSetting("Custom fog").value(false);
    private final ColorSetting fogColor = new ColorSetting("Fog color").value(new Color(200, 200, 200)).setVisible(customFog::getValue);
    private final SliderSetting fogDistance = new SliderSetting("Fog distance").value(-8f).range(-8f, 25f).step(1f).setVisible(customFog::getValue);
    private final SliderSetting fogDensity = new SliderSetting("Fog density").value(100f).range(0f, 100f).step(1f).setVisible(customFog::getValue);
    private final BooleanSetting fogBlur = new BooleanSetting("Fog blur").value(false).setVisible(customFog::getValue);
    private final SliderSetting fogBlurStrength = new SliderSetting("Blur strength").value(5f).range(1f, 10f).step(0.5f)
            .setVisible(() -> customFog.getValue() && fogBlur.getValue());

    private float lastFogStart = 0f, lastFogEnd = 64f;

    private final BooleanSetting saturation = new BooleanSetting("Saturation").value(false);
    private final SliderSetting saturationAmount = new SliderSetting("Saturation amount").value(1.15f).range(0f, 2.5f).step(0.05f)
            .setVisible(saturation::getValue);

    private final BooleanSetting wetWorld = new BooleanSetting("WetWorld").value(false);
    private final BooleanSetting customSky = new BooleanSetting("Custom Sky").value(false);
    private final MultiBooleanSetting sections = new MultiBooleanSetting("Sections").value(wetWorld, customSky);

    public final ModeSetting wetMode = new ModeSetting("Wet Mode").value("Solution").values("Solution", "Solution New")
            .setVisible(wetWorld::getValue);
    public final SliderSetting wetness = new SliderSetting("Wetness").value(0.85f).range(0.1f, 2f).step(0.05f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution"));
    public final SliderSetting puddles = new SliderSetting("Puddles").value(0.55f).range(0.05f, 1f).step(0.05f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution"));
    public final SliderSetting reflection = new SliderSetting("Reflection").value(0.95f).range(0.1f, 2f).step(0.05f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution"));
    public final SliderSetting rain = new SliderSetting("Rain").value(1f).range(0.1f, 3f).step(0.1f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution"));
    public final SliderSetting newWetness = new SliderSetting("New Wetness").value(0.62f).range(0f, 1.2f).step(0.05f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New"));
    public final SliderSetting newReflection = new SliderSetting("New Reflection").value(0.72f).range(0f, 1.2f).step(0.05f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New"));
    public final SliderSetting newRain = new SliderSetting("New Rain").value(0.75f).range(0f, 2f).step(0.1f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New"));
    public final ModeSetting quality = new ModeSetting("Quality").value("High").values("Low", "Medium", "High", "Ultra")
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New"));
    public final BooleanSetting ripples = new BooleanSetting("Ripples").value(true)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New"));
    public final SliderSetting rippleSpeed = new SliderSetting("Ripple Speed").value(1f).range(0.1f, 3f).step(0.1f)
            .setVisible(() -> wetWorld.getValue() && wetMode.is("Solution New") && ripples.getValue());

    public final ModeSetting skyMode = new ModeSetting("Sky Mode").value("Aurora").values(
            "Aurora", "aurora2", "Sakura", "Plasma", "Plasma 2", "Northern Lights",
            "Night", "Summer Sky", "Caustic", "Thunder")
            .setVisible(customSky::getValue);
    public final SliderSetting speed = new SliderSetting("Speed").value(1f).range(0.1f, 5f).step(0.01f)
            .setVisible(customSky::getValue);
    public final SliderSetting scale = new SliderSetting("Scale").value(5f).range(1f, 20f).step(0.01f)
            .setVisible(customSky::getValue);
    public final SliderSetting intensity = new SliderSetting("Intensity").value(0.1f).range(0.1f, 1f).step(0.01f)
            .setVisible(customSky::getValue);
    public final SliderSetting alpha = new SliderSetting("Opacity").value(1f).range(0.3f, 1f).step(0.05f)
            .setVisible(customSky::getValue);
    public final BooleanSetting useThemeColor = new BooleanSetting("Theme color").value(false)
            .setVisible(customSky::getValue);
    public final SliderSetting thunderInterval = new SliderSetting("Thunder Interval").value(4f).range(1f, 10f).step(0.5f)
            .setVisible(() -> customSky.getValue() && skyMode.is("Thunder"));
    public final SliderSetting thunderChance = new SliderSetting("Lightning Chance").value(0.65f).range(0f, 1f).step(0.05f)
            .setVisible(() -> customSky.getValue() && skyMode.is("Thunder"));
    public final SliderSetting thunderGlow = new SliderSetting("Lightning Glow").value(1f).range(0.1f, 3f).step(0.1f)
            .setVisible(() -> customSky.getValue() && skyMode.is("Thunder"));

    private long startMillis = -1;

    public AmbienceModule() {
        addSettings(
                time,
                customFog, fogColor, fogDistance, fogDensity, fogBlur, fogBlurStrength,
                saturation, saturationAmount,
                sections,
                wetMode, wetness, puddles, reflection, rain,
                newWetness, newReflection, newRain, quality, ripples, rippleSpeed,
                skyMode, speed, scale, intensity, alpha, useThemeColor,
                thunderInterval, thunderChance, thunderGlow
        );
        SkyPipelines.register();
        customSky.onAction(this::resetSkyTime);
        if (!skyHookRegistered) {
            skyHookRegistered = true;
            LevelRenderEvents.END_MAIN.register(context -> {
                if (!isCustomSkyEnabled() || mc.level == null || mc.player == null || mc.gameRenderer == null) return;
                try {
                    SkyPipelines.copyDepth();
                    renderSkyShader();
                } catch (Throwable t) {
                    customSky.setValue(false);
                    print("Custom Sky disabled: " + t);
                }
            });
        }
    }

    public boolean isWetWorldEnabled() {
        return isEnabled() && wetWorld.getValue();
    }

    public boolean isCustomSkyEnabled() {
        return isEnabled() && customSky.getValue();
    }

    public static float getSaturation() {
        AmbienceModule module = instance;
        if (module == null || !module.isEnabled() || !module.saturation.getValue()) return 1.0f;
        return module.saturationAmount.getValue();
    }

    public long getTime(long original) {
        if (mc.level == null || !isEnabled()) return original;

        WorldTime selected = Choice.getChoiceByName(time.getValue(), WorldTime.values());

        return switch (selected) {
            case NO_CHANGE -> original;
            case DAWN -> 23041L;
            case DAY -> 1000L;
            case NOON -> 6000L;
            case DUSK -> 12610L;
            case NIGHT -> 13000L;
            case MID_NIGHT -> 18000L;
        };
    }

    public void applyCustomFog(FogData fog) {
        if (!isEnabled() || !customFog.getValue()) return;

        float viewDistance = Math.max(fog.renderDistanceEnd, 0f);
        float start = Mth.clamp(fogDistance.getValue(), -8f, viewDistance);
        float end = Mth.clamp(fogDistance.getValue() + fogDensity.getValue(), 0f, viewDistance);
        Color color = fogColor.getValue();

        fog.color.set(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        fog.environmentalStart = start;
        fog.environmentalEnd = end;
        fog.renderDistanceStart = start;
        fog.renderDistanceEnd = end;
        lastFogStart = start;
        lastFogEnd = end;
    }

    public boolean isFogOnlySky() {
        return isEnabled() && customFog.getValue();
    }

    public int fogSkyColor() {
        Color color = fogColor.getValue();
        return 0xFF000000 | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
    }

    public float fogStart() {
        return lastFogStart;
    }

    public float fogEnd() {
        return lastFogEnd;
    }

    public float fogBlurStrength() {
        return fogBlurStrength.getValue();
    }

    public void renderFogBlur(CameraRenderState camera) {
        if (!isFogOnlySky() || !fogBlur.getValue()) return;
        FogBlurRenderer.getInstance().render(this, camera);
    }

    public float wetQualityIndex() {
        if (quality.is("Low")) return 0f;
        if (quality.is("Medium")) return 1f;
        if (quality.is("Ultra")) return 3f;
        return 2f;
    }

    public void renderWetWorld(CameraRenderState camera) {
        if (!isWetWorldEnabled()) return;
        WetShaderRenderer.getInstance().render(this, camera);
    }

    @Override
    public void onEnable() {
        resetSkyTime();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        startMillis = -1;
        FogBlurRenderer.getInstance().release();
        super.onDisable();
    }

    private void resetSkyTime() {
        startMillis = System.currentTimeMillis();
    }

    private float timeSeconds() {
        if (startMillis < 0) startMillis = System.currentTimeMillis();
        return (System.currentTimeMillis() - startMillis) / 1000f;
    }

    private Color themeColor() {
        return useThemeColor.getValue() ? UIColors.primary() : Color.WHITE;
    }

    private Color themeSecondary() {
        return useThemeColor.getValue() ? UIColors.secondary() : Color.WHITE;
    }

    public void renderSkyShader() {
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) return;

        Camera camera = mc.gameRenderer.mainCamera();
        float yaw = (float) Math.toRadians(-camera.yaw());
        float pitch = (float) Math.toRadians(camera.xRot());
        float fov = mc.options.fov().get().floatValue();
        float time = timeSeconds();
        boolean preMultipliedTime = !skyMode.is("Caustic") && !skyMode.is("Aurora") && !skyMode.is("aurora2")
                && !skyMode.is("Night") && !skyMode.is("Summer Sky") && !skyMode.is("Thunder");
        float shaderTime = preMultipliedTime ? time * speed.getValue() : time;

        Color first = themeColor();
        Color second = themeSecondary();
        if ((skyMode.is("aurora2") || skyMode.is("Thunder")) && !useThemeColor.getValue()) {
            first = new Color(0x79, 0x86, 0xCB);
            second = new Color(0x79, 0x86, 0xCB);
        }
        float rainLevel = mc.level.getRainLevel(1.0f);
        float thunderLevel = mc.level.getThunderLevel(1.0f);

        float night = 0f;
        if (skyMode.is("Sakura")) {
            long dayTime = Math.floorMod(mc.level.getOverworldClockTime(), 24000L);
            night = dayTime >= 13000L && dayTime <= 23000L ? 1f : 0f;
        }

        float packedIntensity = skyMode.is("Thunder") ? intensity.getValue() * 0.1f : intensity.getValue();

        EvaUniform uniform = EvaUniform.of(
                mc.getWindow().getWidth(), mc.getWindow().getHeight(), 0f, 0f,
                shaderTime, yaw, pitch, scale.getValue(),
                first.getRed() / 255f, first.getGreen() / 255f, first.getBlue() / 255f, alpha.getValue(),
                second.getRed() / 255f, second.getGreen() / 255f, second.getBlue() / 255f, 0f,
                fov, packedIntensity, speed.getValue(), night,
                rainLevel, thunderLevel, skyMode.is("Night") || useThemeColor.getValue() ? 1f : 0f, 0f,
                thunderInterval.getValue(), thunderChance.getValue(), thunderGlow.getValue(), 0f
        );

        SkyPipelines.render(switch (skyMode.getValue()) {
            case "Plasma" -> SkyPipelines.SKY_PLASMA;
            case "Plasma 2" -> SkyPipelines.PLASMA_2;
            case "Caustic" -> SkyPipelines.CAUSTIC;
            case "Northern Lights" -> SkyPipelines.AURORA;
            case "aurora2" -> SkyPipelines.AURORA_2;
            case "Night" -> SkyPipelines.NIGHT;
            case "Summer Sky" -> SkyPipelines.SUMMER;
            case "Sakura" -> SkyPipelines.SKY_SAKURA;
            case "Thunder" -> SkyPipelines.THUNDER;
            default -> SkyPipelines.SKY_AURORA;
        }, uniform);
    }

    @Override public void onEvent() {}
}
