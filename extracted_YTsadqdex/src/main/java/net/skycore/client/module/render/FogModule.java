package net.skycore.client.module.render;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.renderer.fog.FogData;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.FogBlurRenderer;
import net.skycore.client.setting.BooleanSetting;

public final class FogModule extends ClientModule {
   private final NumberSetting blurStrength;
   private final BooleanSetting changeColor;
   private final NumberSetting fogStart = this.add(new NumberSetting("Fog Start", 1.0, 0.1, 2.5, 0.05));
   private final NumberSetting fogEnd = this.add(new NumberSetting("Fog End", 1.2, 0.1, 2.5, 0.05));
   private final ColorSetting fogColor;
   private final NumberSetting blurDistance;
   private final BooleanSetting fogBlur;
   private final BooleanSetting blurTint;
   private final FogBlurRenderer blurRenderer;

   @Override
   protected void onDisable() {
      this.blurRenderer.release();
   }

   public Number blurStrength() {
      return this.blurStrength.get();
   }

   public FogBlurRenderer blurRenderer() {
      return this.blurRenderer;
   }

   public BooleanSetting blurTint() {
      return this.blurTint;
   }

   public static FogModule enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(FogModule.class);
      Predicate<net.skycore.client.module.render.FogModule> predicate = ClientModule::isEnabled;
      return (FogModule)optional.filter(predicate).orElse(null);
   }

   public void applyFog(FogData fogData) {
      float renderDistanceEnd = fogData.renderDistanceEnd;
      float distance = renderDistanceEnd * this.fogStart.get().floatValue();
      float computedValue = distance + 1.0F;
      Double value = this.fogEnd.get();
      float computedValue2 = renderDistanceEnd * value.floatValue();
      float maximum = Math.max(computedValue, computedValue2);
      fogData.renderDistanceStart = distance;
      fogData.environmentalStart = distance;
      fogData.skyEnd = maximum;
      fogData.renderDistanceEnd = maximum;
      fogData.environmentalEnd = maximum;
      Boolean get = this.changeColor.get();
      if (get) {
         int value2 = this.fogColor.get();
         fogData.color.set((value2 >> 16 & 0xFF) / 255.0F, (value2 >> 8 & 0xFF) / 255.0F, (value2 & 0xFF) / 255.0F, (value2 >>> 24) / 255.0F);
      }
   }

   public boolean usesFogBlur() {
      if (this.isEnabled()) {
         if (this.fogBlur.get()) {
            return true;
         }
      }

      return false;
   }

   public FogModule() {
      super("Fog", "Hex distance fog and optional depth blur", ModuleCategory.RENDER);
      this.changeColor = this.add(new BooleanSetting("Change Color", false));
      this.fogColor = this.add(new ColorSetting("Fog Color", -1, true));
      this.fogBlur = this.add(new BooleanSetting("Fog Blur", false));
      this.blurStrength = this.add(new NumberSetting("Blur Strength", "", 6.0, 1.0, 20.0, 0.5, ""));
      this.blurDistance = this.add(new NumberSetting("Blur Distance", "", 40.0, 8.0, 128.0, 1.0, " blocks"));
      this.blurTint = this.add(new BooleanSetting("Blur Tint", false));
      this.blurRenderer = new FogBlurRenderer(this);
      this.fogColor.visibleWhen(this.changeColor::get);
      this.blurStrength.visibleWhen(this.fogBlur::get);
      this.blurDistance.visibleWhen(this.fogBlur::get);
      this.blurTint.visibleWhen(this.fogBlur::get);
   }

   public Number blurDistance() {
      return this.blurDistance.get();
   }

   public int resolveBlurTint() {
      ThemeManager themeManager = SkycoreClient.themes();
      return themeManager.accent();
   }
}
