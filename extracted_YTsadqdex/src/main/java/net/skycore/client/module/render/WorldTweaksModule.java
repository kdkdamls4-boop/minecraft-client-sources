package net.skycore.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.DayTimeAnimation;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.RenderSkyHelper;
import net.skycore.client.util.RenderSkyOverlayHelper;
import net.skycore.client.util.SecondarySkyColorHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WriteHexByteHelper;

public final class WorldTweaksModule extends ClientModule implements SecondarySkyColorHelper {
   private final NumberSetting siyanieCoverage;
   private final NumberSetting siyanieSpeed;
   private final BooleanSetting endSkyOverride;
   private final ModeSetting worldColorMode;
   private static final long TIME_TRANSITION_MILLIS = 5000L;
   private final BooleanSetting changeTemperature;
   private final NumberSetting customTime;
   private final NumberSetting cycleFrom;
   private final NumberSetting timeSpeed;
   private long animationStartedAt;
   private float animationStart;
   private final ModeSetting skyEffect;
   private final BooleanSetting changeSky;
   private final NumberSetting temperatureAmount;
   public static final String FOG_DISABLE = "Disable";
   private final ModeSetting fogMode;
   private long timeReferenceNanos;
   private final NumberSetting sourceSpeed;
   public static final String SKY_SOURCE_SUMMER = "SkyCore Summer";
   private final NumberSetting celestialRotation;
   private RenderSkyOverlayHelper renderer;
   private final NumberSetting skySpeed;
   public static final String SKY_NEBULA = "Nebula";
   private final ColorSetting skyColor1;
   private final BooleanSetting changeTime = this.add(new BooleanSetting("Change Time", false));
   private final NumberSetting sourceFill;
   public static final String FOG_VANILLA = "Vanilla";
   private final BooleanSetting smoothTimeCycle;
   private final NumberSetting fogDistance;
   public static final String SKY_CLOUDS = "Clouds";
   private final NumberSetting cycleDuration;
   private final ModeSetting weatherMode;
   private final BooleanSetting changeWorldColor;
   public static final String SKY_SOURCE_PLASMA = "SkyCore Plasma";
   private final NumberSetting saturationAmount;
   private final NumberSetting siyanieSoftness;
   public static final String SKY_PLASMA = "Plasma";
   private final NumberSetting sourceScale;
   private final NumberSetting skyIntensity;
   private final NumberSetting sourceAlpha;
   private RenderSkyHelper sourceRenderer;
   private final ColorSetting skyColor2;
   private final BooleanSetting changeSaturation;
   public static final String SKY_SIYANIE = "Aurora";
   private final BooleanSetting weatherControl;
   private final ColorSetting worldColor;
   public static final String FOG_CUSTOM = "Custom";
   private final ModeSetting timeOfDay = this.add(new ModeSetting("Time of Day", "Day", "Day", "Night", "Sunset", "Midnight", "Sunrise", "Custom"));
   private final NumberSetting cycleTo;
   private float animationTarget;
   private final ModeSetting summerMode;
   private final NumberSetting sourceGlow;
   private String lastSelectedTime;
   private final NumberSetting siyanieIntensity;
   private final ModeSetting skyColorMode;
   public static final String SKY_DEEP_SPACE = "Deep Space";
   private final NumberSetting sourceOutline;

   public void renderWorldEnd(CameraRenderState cameraRenderState) {
      this.applyWeather();
      if (!this.usesColorGrade()) {
         if (!this.shouldRenderSky()) {
            this.releaseRenderer();
         }
      } else {
         if (this.renderer == null) {
            RenderSkyOverlayHelper renderSkyOverlayHelper = new RenderSkyOverlayHelper();
            this.renderer = renderSkyOverlayHelper;
         }

         this.renderer.renderColorGrade(this);
      }
   }

   private float currentAnimatedTime() {
      if (this.animationStart == this.animationTarget) {
         return this.animationTarget;
      } else {
         long nowNanos = System.nanoTime();
         float computedValue = (float)(nowNanos - this.animationStartedAt) / 1000000.0F;
         float computedValue2 = computedValue / 5000.0F;
         float clampedValue = Math.clamp(computedValue2, 0.0F, 1.0F);
         double computedValue3 = 1.0F - clampedValue;
         float computedValue4 = 1.0F - (float)Math.pow(computedValue3, 3.0);
         return this.animationStart + (this.animationTarget - this.animationStart) * computedValue4;
      }
   }

   @Override
   public float skyIntensity() {
      if (this.skyEffect.is("Aurora")) {
         return this.siyanieIntensity.get().floatValue();
      } else {
         Double value = this.skyIntensity.get();
         return value.floatValue();
      }
   }

   public String skyEffect() {
      return this.skyEffect.get();
   }

   @Override
   public int primarySkyColor() {
      int value = 0;
      value = (byte)0;
      if (this.skyEffect.is("Aurora")) {
         return SkycoreClient.themes().accent();
      }

      if (this.skyColorMode.is("Sync")) {
         ThemeManager themeManager = SkycoreClient.themes();
         value = themeManager.accent();
      } else {
         Integer value2 = this.skyColor1.get();
         value = value2;
      }

      return value;
   }

   @Override
   public int secondarySkyColor() {
      if (this.skyEffect.is("Aurora")) {
         return WriteHexByteHelper.lerp(SkycoreClient.themes().accent(), -1, 0.34F);
      } else {
         if (!this.skyColorMode.is("Sync")) {
            Integer value = this.skyColor2.get();
            return value;
         } else {
            return WriteHexByteHelper.lerp(SkycoreClient.themes().accent(), -1, 0.34F);
         }
      }
   }

   public static WorldTweaksModule enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      WorldTweaksModule worldTweaksModule = moduleManager.get(WorldTweaksModule.class).orElse(null);
      if (worldTweaksModule != null) {
         if (worldTweaksModule.isEnabled()) {
            return worldTweaksModule;
         }
      }

      return null;
   }

   private void resetTimeAnimation() {
      String text = this.timeOfDay.get();
      float value = this.targetTime(text);
      this.animationStart = value;
      this.animationTarget = value;
      this.animationStartedAt = System.nanoTime();
      this.timeReferenceNanos = this.animationStartedAt;
      String text2 = this.timeOfDay.get();
      this.lastSelectedTime = text2;
   }

   public int resolvedWorldColor() {
      int value = 0;
      if (this.worldColorMode.is("Custom")) {
         value = this.worldColor.get();
      } else {
         ThemeManager themeManager = SkycoreClient.themes();
         value = themeManager.accent();
      }

      return value;
   }

   private void applyWeather() {
      if (this.weatherControl.get()) {
         ClientLevel instance = Minecraft.getInstance().level;
         if (instance != null) {
            if (this.weatherMode.is("Thunder")) {
               instance.setRainLevel(1.0F);
               instance.setThunderLevel(1.0F);
            } else if (this.weatherMode.is("Rain")) {
               instance.setRainLevel(1.0F);
               instance.setThunderLevel(0.0F);
            } else {
               instance.setRainLevel(0.0F);
               instance.setThunderLevel(0.0F);
            }
         }
      }
   }

   public float siyanieCoverage() {
      return this.siyanieCoverage.get().floatValue();
   }

   private boolean sourcePlasmaVisible() {
      if (this.changeSky.get()) {
         if (this.skyEffect.is("SkyCore Plasma")) {
            return true;
         }
      }

      return false;
   }

   public RenderSkyHelper.Settings sourceSkySettings() {
      int intermediate = this.primarySkyColor();
      Double value = this.sourceSpeed.get();
      float value2 = value.floatValue();
      Double value3 = this.sourceScale.get();
      float value4 = value3.floatValue();
      Double value5 = this.sourceOutline.get();
      float value6 = value5.floatValue();
      float value7 = this.sourceGlow.get().floatValue();
      Double value8 = this.sourceFill.get();
      float value9 = value8.floatValue();
      Double value10 = this.sourceAlpha.get();
      return new RenderSkyHelper.Settings(intermediate, value2, value4, value6, value7, value9, value10.floatValue() + 1.0F, this.summerMode.is("Night"));
   }

   @Override
   protected void onDisable() {
      this.releaseRenderer();
   }

   private boolean isSourceSky() {
      if (!this.skyEffect.is("SkyCore Plasma")) {
         if (!this.skyEffect.is("SkyCore Summer")) {
            return false;
         }
      }

      return true;
   }

   public void applyVanillaFog(FogData fogData, float value) {
      if (this.fogMode.is("Disable")) {
         float maximum = Math.max(value * 4.0F, 1024.0F);
         fogData.environmentalStart = maximum;
         fogData.environmentalEnd = maximum;
         fogData.renderDistanceStart = maximum;
         fogData.renderDistanceEnd = maximum;
         fogData.skyEnd = maximum;
         fogData.cloudEnd = maximum;
      } else if (this.fogMode.is("Custom")) {
         float distance = Mth.clamp(this.fogDistance.get().floatValue(), 1.0F, value * 0.82F);
         float computedValue = Mth.clamp(Math.max(distance * 0.45F, value * 0.28F), 6.0F, value * 0.55F);
         float maximum2 = Math.max(0.0F, distance - computedValue);
         int computedValue2 = 0xFF000000 | SkycoreClient.themes().accent() & 16777215;
         fogData.color.set(WriteHexByteHelper.red(computedValue2) / 255.0F, WriteHexByteHelper.green(computedValue2) / 255.0F, WriteHexByteHelper.blue(computedValue2) / 255.0F, 1.0F);
         fogData.environmentalStart = maximum2;
         fogData.environmentalEnd = distance;
         fogData.renderDistanceStart = maximum2;
         fogData.renderDistanceEnd = distance;
         fogData.skyEnd = Math.max(fogData.skyEnd, distance);
         fogData.cloudEnd = Math.max(fogData.cloudEnd, distance);
      }
   }

   @Override
   public float skySpeed() {
      if (this.skyEffect.is("Aurora")) {
         Double value = this.siyanieSpeed.get();
         return value.floatValue();
      } else {
         Double value2 = this.skySpeed.get();
         return value2.floatValue();
      }
   }

   public boolean reflectsSky() {
      return this.isEnabled() && this.shouldRenderSky();
   }

   public float temperatureAmount() {
      return this.temperatureAmount.get().floatValue();
   }

   public boolean usesSaturation() {
      Boolean get = this.changeSaturation.get();
      if (get) {
         Double value = this.saturationAmount.get();
         if (Math.abs(value) > 1.0E-4) {
            return true;
         }
      }

      return false;
   }

   public float siyanieSoftness() {
      return this.siyanieSoftness.get().floatValue();
   }

   private boolean shouldRenderSky() {
      if (!this.usesSky()) {
         return false;
      }

      ClientLevel instance = Minecraft.getInstance().level;
      if (instance != null) {
         if (instance.dimension() == Level.END) {
            if (!this.endSkyOverride.get()) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean siyanieVisible() {
      Boolean get = this.changeSky.get();
      if (get) {
         if (this.skyEffect.is("Aurora")) {
            return true;
         }
      }

      return false;
   }

   public boolean usesWorldColor() {
      Boolean get = this.changeWorldColor.get();
      return get;
   }

   private float targetTime(String text) {
      return switch (text) {
         case "Night" -> 13000.0F;
         case "Sunset" -> 12000.0F;
         case "Midnight" -> 18000.0F;
         case "Sunrise" -> 23000.0F;
         case "Custom" -> this.customTime.get().floatValue();
         default -> 1000.0F;
      };
   }

   private void releaseRenderer() {
      if (this.sourceRenderer != null) {
         this.sourceRenderer.close();
         this.sourceRenderer = null;
      }

      if (this.renderer != null) {
         this.renderer.release();
         this.renderer = null;
      }
   }

   public void renderSkyOverlay(CameraRenderState cameraRenderState) {
      if (this.shouldRenderSky()) {
         if (this.isSourceSky()) {
            if (this.sourceRenderer == null) {
               this.sourceRenderer = new RenderSkyHelper();
            }

            this.sourceRenderer
               .renderSky(
                  cameraRenderState,
                  new RenderSkyHelper.Settings(
                     this.primarySkyColor(),
                     this.sourceSpeed.get().floatValue(),
                     this.sourceScale.get().floatValue(),
                     this.sourceOutline.get().floatValue(),
                     this.sourceGlow.get().floatValue(),
                     this.sourceFill.get().floatValue(),
                     this.sourceAlpha.get().floatValue() + 1.0F,
                     this.summerMode.is("Night")
                  ),
                  this.skyEffect.is("SkyCore Summer")
               );
         } else {
            if (this.sourceRenderer != null) {
               this.sourceRenderer.close();
               this.sourceRenderer = null;
            }

            if (this.renderer == null) {
               this.renderer = new RenderSkyOverlayHelper();
            }

            this.renderer.renderSky(this, cameraRenderState);
         }
      }
   }

   private boolean timeControlsVisible() {
      Boolean get = this.changeTime.get();
      if (get) {
         Boolean get2 = this.changeSky.get();
         if (!get2) {
            return true;
         }

         if (!this.skyEffect.is("SkyCore Summer")) {
            return true;
         }
      }

      return false;
   }

   public WorldTweaksModule() {
      super("World Tweaks", "Time, weather, sky, fog, grading, and world lighting", ModuleCategory.RENDER);
      this.customTime = this.add(new NumberSetting("Custom Time", "", 6000.0, 0.0, 23999.0, 1.0, " ticks"));
      this.smoothTimeCycle = this.add(new BooleanSetting("Smooth Time Cycle", false));
      this.cycleFrom = this.add(new NumberSetting("Cycle From", "", 12000.0, 0.0, 23999.0, 1.0, " ticks"));
      this.cycleTo = this.add(new NumberSetting("Cycle To", "", 23000.0, 0.0, 23999.0, 1.0, " ticks"));
      this.cycleDuration = this.add(new NumberSetting("Cycle Duration", "", 30.0, 2.0, 300.0, 1.0, "s"));
      this.celestialRotation = this.add(new NumberSetting("Celestial Rotation", "", 0.0, -180.0, 180.0, 1.0, "°"));
      this.timeSpeed = this.add(new NumberSetting("Time Speed", "", 0.0, 0.0, 10.0, 0.1, "x"));
      this.weatherControl = this.add(new BooleanSetting("Weather Control", false));
      this.weatherMode = this.add(new ModeSetting("Weather", "Clear", "Clear", "Rain", "Thunder"));
      this.changeSky = this.add(new BooleanSetting("Change Sky", false));
      this.skyEffect = this.add(
         new ModeSetting("Effect", "Deep Space", "Deep Space", "Nebula", "Plasma", "Clouds", "Aurora", "SkyCore Plasma", "SkyCore Summer")
      );
      this.skyColorMode = this.add(new ModeSetting("Sky Color Mode", "Sync", "Sync", "Custom Color"));
      this.skyColor1 = this.add(new ColorSetting("Primary Color", -8812853, false));
      this.skyColor2 = this.add(new ColorSetting("Secondary Color", -8812853, false));
      this.endSkyOverride = this.add(new BooleanSetting("End Sky Override", true));
      this.skySpeed = this.add(new NumberSetting("Speed", "", 4.0, 0.1, 8.0, 0.1, "x"));
      this.skyIntensity = this.add(new NumberSetting("Intensity", "", 3.0, 0.1, 5.0, 0.1, "x"));
      this.siyanieSpeed = this.add(new NumberSetting("Aurora Speed", 1.2, 0.1, 5.0, 0.1));
      this.siyanieIntensity = this.add(new NumberSetting("Aurora Intensity", 1.5, 0.1, 4.0, 0.1));
      this.siyanieSoftness = this.add(new NumberSetting("Softness", 0.4, 0.0, 1.0, 0.01));
      this.siyanieCoverage = this.add(new NumberSetting("Coverage", 0.5, 0.0, 1.0, 0.01));
      this.summerMode = this.add(new ModeSetting("Summer Mode", "Day", "Day", "Night"));
      this.sourceSpeed = this.add(new NumberSetting("Shader Speed", 1.2, 0.1, 5.0, 0.1));
      this.sourceScale = this.add(new NumberSetting("Shader Scale", 1.0, 0.2, 4.0, 0.1));
      this.sourceOutline = this.add(new NumberSetting("Shader Outline", 0.1, 0.01, 1.0, 0.01));
      this.sourceGlow = this.add(new NumberSetting("Shader Glow", 10.0, 0.0, 30.0, 0.5));
      this.sourceFill = this.add(new NumberSetting("Shader Fill", 0.6, 0.0, 2.0, 0.05));
      this.sourceAlpha = this.add(new NumberSetting("Shader Alpha", 1.0, 0.0, 2.0, 0.05));
      this.changeSaturation = this.add(new BooleanSetting("Change Saturation", false));
      this.saturationAmount = this.add(new NumberSetting("Saturation", "", 0.5, -1.0, 2.0, 0.05, ""));
      this.changeTemperature = this.add(new BooleanSetting("Temperature", false));
      this.temperatureAmount = this.add(new NumberSetting("Temperature Amount", "", 0.0, -1.0, 1.0, 0.05, ""));
      this.changeWorldColor = this.add(new BooleanSetting("Change World Color", false));
      this.worldColorMode = this.add(new ModeSetting("World Color Mode", "Sync", "Sync", "Custom"));
      this.worldColor = this.add(new ColorSetting("World Color", -1, false));
      this.fogMode = this.add(new ModeSetting("Fog", "Vanilla", "Vanilla", "Disable", "Custom"));
      this.fogDistance = this.add(new NumberSetting("Fog Distance", "", 90.0, 10.0, 200.0, 1.0, " blocks"));
      this.timeOfDay.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            Boolean get = this.smoothTimeCycle.get();
            if (!get) {
               return true;
            }
         }

         return false;
      });
      this.customTime.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            if (!this.smoothTimeCycle.get()) {
               if (this.timeOfDay.is("Custom")) {
                  return true;
               }
            }
         }

         return false;
      });
      this.smoothTimeCycle.visibleWhen(this::timeControlsVisible);
      this.cycleFrom.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            if (this.smoothTimeCycle.get()) {
               return true;
            }
         }

         return false;
      });
      this.cycleTo.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            Boolean get2 = this.smoothTimeCycle.get();
            if (get2) {
               return true;
            }
         }

         return false;
      });
      this.cycleDuration.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            Boolean get3 = this.smoothTimeCycle.get();
            if (get3) {
               return true;
            }
         }

         return false;
      });
      this.celestialRotation.visibleWhen(this::timeControlsVisible);
      this.timeSpeed.visibleWhen(() -> {
         if (this.timeControlsVisible()) {
            Boolean get4 = this.smoothTimeCycle.get();
            if (get4) {
               return true;
            }
         }

         return false;
      });
      this.weatherMode.visibleWhen(this.weatherControl::get);
      this.skyEffect.visibleWhen(this.changeSky::get);
      this.skyColorMode.visibleWhen(() -> {
         Boolean get5 = this.changeSky.get();
         if (get5) {
            if (!this.skyEffect.is("Aurora")) {
               return true;
            }
         }

         return false;
      });
      this.skyColor1.visibleWhen(() -> {
         Boolean get6 = this.changeSky.get();
         if (get6) {
            if (!this.skyEffect.is("Aurora")) {
               if (this.skyColorMode.is("Custom Color")) {
                  return true;
               }
            }
         }

         return false;
      });
      this.skyColor2.visibleWhen(() -> {
         Boolean get7 = this.changeSky.get();
         if (get7) {
            if (!this.isSourceSky()) {
               if (!this.skyEffect.is("Aurora") && this.skyColorMode.is("Custom Color")) {
                  return true;
               }
            }
         }

         return false;
      });
      this.endSkyOverride.visibleWhen(this.changeSky::get);
      this.skySpeed.visibleWhen(() -> {
         Boolean get8 = this.changeSky.get();
         if (get8) {
            if (!this.isSourceSky()) {
               if (!this.skyEffect.is("Aurora")) {
                  return true;
               }
            }
         }

         return false;
      });
      this.skyIntensity.visibleWhen(() -> {
         Boolean get9 = this.changeSky.get();
         if (get9 && !this.isSourceSky()) {
            if (!this.skyEffect.is("Aurora")) {
               return true;
            }
         }

         return false;
      });
      this.siyanieSpeed.visibleWhen(this::siyanieVisible);
      this.siyanieIntensity.visibleWhen(this::siyanieVisible);
      this.siyanieSoftness.visibleWhen(this::siyanieVisible);
      this.siyanieCoverage.visibleWhen(this::siyanieVisible);
      this.summerMode.visibleWhen(() -> {
         Boolean get10 = this.changeSky.get();
         if (get10) {
            if (this.skyEffect.is("SkyCore Summer")) {
               return true;
            }
         }

         return false;
      });
      this.sourceScale.visibleWhen(() -> {
         return this.changeSky.get() && this.isSourceSky();
      });
      this.sourceSpeed.visibleWhen(this::sourcePlasmaVisible);
      this.sourceOutline.visibleWhen(this::sourcePlasmaVisible);
      this.sourceGlow.visibleWhen(this::sourcePlasmaVisible);
      this.sourceFill.visibleWhen(this::sourcePlasmaVisible);
      this.sourceAlpha.visibleWhen(this::sourcePlasmaVisible);
      this.saturationAmount.visibleWhen(this.changeSaturation::get);
      this.temperatureAmount.visibleWhen(this.changeTemperature::get);
      this.worldColorMode.visibleWhen(this.changeWorldColor::get);
      this.worldColor.visibleWhen(() -> {
         Boolean get11 = this.changeWorldColor.get();
         if (get11) {
            if (this.worldColorMode.is("Custom")) {
               return true;
            }
         }

         return false;
      });
      this.fogDistance.visibleWhen(() -> {
         return this.fogMode.is("Custom");
      });
      this.resetTimeAnimation();
   }

   public boolean usesSky() {
      return this.changeSky.get();
   }

   public boolean usesTemperature() {
      Boolean get = this.changeTemperature.get();
      if (get) {
         Double value = this.temperatureAmount.get();
         double value2 = value;
         if (Math.abs(value2) > 1.0E-4) {
            return true;
         }
      }

      return false;
   }

   public boolean usesColorGrade() {
      if (!this.usesSaturation()) {
         if (!this.usesTemperature()) {
            return false;
         }
      }

      return true;
   }

   public boolean modifiesVanillaFog() {
      return !this.fogMode.is("Vanilla");
   }

   public boolean reflectsSiyanie() {
      return this.isEnabled() && this.skyEffect.is("Aurora") && this.shouldRenderSky();
   }

   public boolean changesTime() {
      if (!this.changeTime.get()) {
         if (this.changeSky.get()) {
            if (this.skyEffect.is("SkyCore Summer")) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   public float saturationAmount() {
      Double value = this.saturationAmount.get();
      return value.floatValue();
   }

   @Override
   protected void onEnable() {
      this.resetTimeAnimation();
   }

   public long getCustomTime() {
      if (this.changeSky.get() && this.skyEffect.is("SkyCore Summer")) {
         return 18000L;
      }

      double elapsed = Math.max(0.0, (System.nanoTime() - this.timeReferenceNanos) / 1.0E9);
      double elapsed2;
      if (this.smoothTimeCycle.get()) {
         elapsed2 = DayTimeAnimation.cycle(this.cycleFrom.get(), this.cycleTo.get(), elapsed, this.cycleDuration.get(), this.timeSpeed.get());
      } else {
         String text = this.timeOfDay.get();
         float value = this.targetTime(text);
         if (!text.equals(this.lastSelectedTime) || Math.abs(DayTimeAnimation.shortestDifference(this.animationTarget, value)) > 0.5) {
            float value2 = this.currentAnimatedTime();
            float value3 = value;
            this.animationStart = value2;
            this.animationTarget = value2 + (float)DayTimeAnimation.shortestDifference(value2, value3);
            this.animationStartedAt = System.nanoTime();
            this.timeReferenceNanos = this.animationStartedAt;
            this.lastSelectedTime = text;
            elapsed = 0.0;
         }

         elapsed2 = this.currentAnimatedTime();
      }

      return DayTimeAnimation.withCelestialRotation(elapsed2, this.celestialRotation.get());
   }
}
