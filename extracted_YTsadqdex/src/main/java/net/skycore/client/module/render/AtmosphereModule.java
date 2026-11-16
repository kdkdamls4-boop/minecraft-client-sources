package net.skycore.client.module.render;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.EnsureFogUniformsHelper;
import net.skycore.client.setting.BooleanSetting;

public final class AtmosphereModule extends ClientModule {
   private final NumberSetting distanceBlur;
   private final BooleanSetting themeFogColor;
   private final BooleanSetting rainSplashes;
   private final ColorSetting customFogColor;
   private final NumberSetting wetBlocks;
   private final NumberSetting wetSlope;
   private final NumberSetting softness;
   private final NumberSetting rainIntensity;
   private final BooleanSetting duskVeil;
   private final NumberSetting rainSplashSize;
   private final NumberSetting fogEnd;
   private final NumberSetting duskStrength;
   public static final AtmosphereModule INSTANCE = new AtmosphereModule();
   private final NumberSetting wetWater;
   private final NumberSetting fogStart;
   private final BooleanSetting themeRain;
   private final BooleanSetting rainAlways;
   private final NumberSetting density;
   private final NumberSetting height;
   private final NumberSetting rainStreakSize;
   private final EnsureFogUniformsHelper renderer;
   private final BooleanSetting volumetricFog = this.add(new BooleanSetting("Volumetric Fog", true));
   private final BooleanSetting skyReflections;
   private final BooleanSetting wetReflections;

   public float duskStrength() {
      Double value = this.duskStrength.get();
      return value.floatValue();
   }

   @Override
   protected void onEnable() {
      this.renderer.resetFailure();
   }

   public boolean needsWorldCapture() {
      if (this.isEnabled()) {
         Boolean get = this.volumetricFog.get();
         if (get) {
            return true;
         }

         Boolean get2 = this.duskVeil.get();
         if (get2) {
            return true;
         }

         if (this.wetReflections.get() || this.themeRain.get()) {
            return true;
         }
      }

      return false;
   }

   public float rainIntensity() {
      Double value = this.rainIntensity.get();
      return value.floatValue();
   }

   public void render(CameraRenderState cameraRenderState) {
      this.renderer.render(cameraRenderState);
   }

   public float wetWater() {
      return this.wetWater.get().floatValue();
   }

   public boolean shouldRenderCustomRain() {
      if (this.isEnabled() && this.themeRain.get()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level == null) {
            return false;
         }

         Boolean get = this.rainAlways.get();
         if (get) {
            return true;
         }

         return instance.level.getRainLevel(1.0F) > 0.02F;
      } else {
         return false;
      }
   }

   public float softness() {
      return this.softness.get().floatValue();
   }

   public boolean wetReflections() {
      return this.wetReflections.get();
   }

   public boolean rainAlways() {
      return this.rainAlways.get();
   }

   public float fogStart() {
      Double value = this.fogStart.get();
      return value.floatValue();
   }

   public float density() {
      return this.density.get().floatValue();
   }

   public boolean themeFogColor() {
      return this.themeFogColor.get();
   }

   public boolean shouldHideVanillaWeather() {
      return this.shouldRenderCustomRain();
   }

   public boolean ownsFog() {
      if (this.isEnabled()) {
         Boolean get = this.volumetricFog.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public boolean rainSplashes() {
      Boolean get = this.rainSplashes.get();
      return get;
   }

   @Override
   protected void onTick() {
      if (this.shouldHideVanillaWeather()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            instance.level.setRainLevel(0.0F);
            instance.level.setThunderLevel(0.0F);
         }
      }
   }

   public float rainSplashSize() {
      Double value = this.rainSplashSize.get();
      return value.floatValue();
   }

   private AtmosphereModule() {
      super("Atmosphere", "Fog, dusk lighting, reflections, and rain", ModuleCategory.RENDER);
      this.duskVeil = this.add(new BooleanSetting("Dusk Layer", true));
      this.wetReflections = this.add(new BooleanSetting("Reflections", true));
      this.skyReflections = this.add(new BooleanSetting("Sky Reflections", true));
      this.themeRain = this.add(new BooleanSetting("Rain", true));
      this.rainAlways = this.add(new BooleanSetting("Always", true));
      this.rainIntensity = this.add(new NumberSetting("Intensity", 0.55, 0.0, 1.0, 0.05));
      this.rainStreakSize = this.add(new NumberSetting("Drops", 1.1, 0.4, 2.0, 0.05));
      this.rainSplashes = this.add(new BooleanSetting("Splashes", true));
      this.rainSplashSize = this.add(new NumberSetting("Ripples", 0.5, 0.15, 1.0, 0.05));
      this.wetBlocks = this.add(new NumberSetting("Block Reflections", 0.7, 0.0, 1.0, 0.05));
      this.wetWater = this.add(new NumberSetting("Water Reflections", 0.7, 0.0, 1.0, 0.05));
      this.wetSlope = this.add(new NumberSetting("Surface Slope", 0.42, 0.2, 0.9, 0.05));
      this.density = this.add(new NumberSetting("Density", 1.1, 0.2, 3.0, 0.05));
      this.height = this.add(new NumberSetting("Layer Height", 48.0, 8.0, 120.0, 1.0));
      this.softness = this.add(new NumberSetting("Softness", 1.0, 0.25, 2.5, 0.05));
      this.fogStart = this.add(new NumberSetting("Fog Start", 12.0, 1.0, 96.0, 1.0));
      this.fogEnd = this.add(new NumberSetting("Fog End", 96.0, 24.0, 220.0, 1.0));
      this.duskStrength = this.add(new NumberSetting("Layer Strength", 0.7, 0.1, 1.5, 0.05));
      this.distanceBlur = this.add(new NumberSetting("Distant Blur", 0.4, 0.0, 1.0, 0.05));
      this.themeFogColor = this.add(new BooleanSetting("Theme Color", true));
      this.customFogColor = this.add(new ColorSetting("Fog Color", -4667160, false));
      this.renderer = new EnsureFogUniformsHelper(this);
      this.rainAlways.visibleWhen(this.themeRain::get);
      this.rainIntensity.visibleWhen(this.themeRain::get);
      this.rainStreakSize.visibleWhen(this.themeRain::get);
      this.rainSplashes.visibleWhen(this.themeRain::get);
      this.rainSplashSize.visibleWhen(() -> {
         Boolean get = this.themeRain.get();
         if (get) {
            if (this.rainSplashes.get()) {
               return true;
            }
         }

         return false;
      });
      this.wetBlocks.visibleWhen(this.wetReflections::get);
      this.skyReflections.visibleWhen(this.wetReflections::get);
      this.wetWater.visibleWhen(this.wetReflections::get);
      this.wetSlope.visibleWhen(this.wetReflections::get);
      this.density.visibleWhen(this.volumetricFog::get);
      this.height.visibleWhen(this.volumetricFog::get);
      this.softness.visibleWhen(this.volumetricFog::get);
      this.fogStart.visibleWhen(this.volumetricFog::get);
      this.fogEnd.visibleWhen(this.volumetricFog::get);
      this.duskStrength.visibleWhen(this.duskVeil::get);
      this.distanceBlur.visibleWhen(this.volumetricFog::get);
      this.customFogColor.visibleWhen(() -> {
         Boolean get2 = this.themeFogColor.get();
         return !get2;
      });
   }

   public float distanceBlur() {
      Double value = this.distanceBlur.get();
      return value.floatValue();
   }

   public int blurSteps() {
      return 1;
   }

   public boolean needsBlurCapture() {
      if (!this.isEnabled()) {
         return false;
      }

      if (this.wetReflections.get()) {
         if (!this.shouldRenderCustomRain()) {
            return true;
         }
      }

      Double value = this.distanceBlur.get();
      if (value.floatValue() > 0.01F) {
         Boolean get = this.volumetricFog.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public boolean duskVeil() {
      Boolean get = this.duskVeil.get();
      return get;
   }

   public float wetSlope() {
      Double value = this.wetSlope.get();
      return value.floatValue();
   }

   public boolean needsFogPass() {
      if (!this.isEnabled()) {
         return false;
      }

      if (!this.volumetricFog.get()) {
         if (!this.duskVeil.get()) {
            Boolean get = this.wetReflections.get();
            if (get) {
               if (!this.shouldRenderCustomRain()) {
                  return true;
               }
            }

            return this.distanceBlur.get().floatValue() > 0.01F;
         }
      }

      return true;
   }

   public static AtmosphereModule enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(AtmosphereModule.class);
      Predicate<net.skycore.client.module.render.AtmosphereModule> predicate = ClientModule::isEnabled;
      var optional2 = optional.filter(predicate);
      return (AtmosphereModule)optional2.orElse(null);
   }

   @Override
   protected void onDisable() {
      this.renderer.release();
   }

   public boolean skyReflections() {
      Boolean get = this.wetReflections.get();
      if (get) {
         Boolean get2 = this.skyReflections.get();
         if (get2) {
            return true;
         }
      }

      return false;
   }

   public boolean rainOwnsWet() {
      return this.shouldRenderCustomRain() && this.wetReflections.get();
   }

   public int resolveFogColor() {
      Boolean get = this.themeFogColor.get();
      if (get) {
         ThemeManager themeManager = SkycoreClient.themes();
         return themeManager.accent();
      } else {
         return this.customFogColor.get();
      }
   }

   public float wetBlocks() {
      return this.wetBlocks.get().floatValue();
   }

   public boolean volumetricFog() {
      return this.volumetricFog.get();
   }

   public float rainStreakSize() {
      Double value = this.rainStreakSize.get();
      return value.floatValue();
   }

   public float fogEnd() {
      return this.fogEnd.get().floatValue();
   }

   public float height() {
      return this.height.get().floatValue();
   }
}
