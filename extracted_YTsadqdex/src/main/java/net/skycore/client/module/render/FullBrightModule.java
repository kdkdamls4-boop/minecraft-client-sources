package net.skycore.client.module.render;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.LightLayer;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.BooleanSetting;

public final class FullBrightModule extends ClientModule {
   private float renderGamma;
   private final NumberSetting brightness;
   private final BooleanSetting adaptiveBrightness;
   private final ModeSetting mode = this.add(new ModeSetting("Mode", "Gamma", "Gamma", "Potion"));
   private double savedGamma;

   public static FullBrightModule enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(FullBrightModule.class);
      Predicate<net.skycore.client.module.render.FullBrightModule> predicate = ClientModule::isEnabled;
      var optional2 = optional.filter(predicate);
      return (FullBrightModule)optional2.orElse(null);
   }

   public static float getBrightnessForRendering(float coordinate) {
      FullBrightModule fullBrightModule = enabled();
      if (fullBrightModule == null) {
         return coordinate;
      } else {
         if (fullBrightModule.mode.is("Gamma")) {
            return fullBrightModule.renderGamma;
         } else if (fullBrightModule.mode.is("Potion")) {
            return Math.max(coordinate, fullBrightModule.renderGamma);
         } else {
            return coordinate;
         }
      }
   }

   @Override
   protected void onEnable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.options != null) {
         Double value = instance.options.gamma().get();
         double value2 = value;
         this.savedGamma = value2;
         this.renderGamma = (float)this.savedGamma;
      }
   }

   @Override
   protected void onDisable() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.options != null) {
         OptionInstance optionInstance = instance.options.gamma();
         optionInstance.set(this.savedGamma);
      }

      this.renderGamma = 0.0F;
      LocalPlayer player = instance.player;
      if (player != null) {
         player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }

   public FullBrightModule() {
      super("Full Bright", "Brightens dark areas of the world", ModuleCategory.RENDER);
      this.adaptiveBrightness = this.add(new BooleanSetting("Adaptive Brightness", false));
      this.brightness = this.add(new NumberSetting("Brightness", "", 8.0, 0.5, 20.0, 0.1, ""));
      this.adaptiveBrightness.visibleWhen(() -> {
         return this.mode.is("Gamma");
      });
      this.brightness.visibleWhen(() -> {
         if (this.adaptiveBrightness.get()) {
            if (!this.mode.is("Potion")) {
               return false;
            }
         }

         return true;
      });
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null && instance.level != null && instance.options != null) {
         if (this.mode.is("Gamma")) {
            if (this.adaptiveBrightness.get()) {
               BlockPos blockPos = player.blockPosition();
               int brightness2 = Math.max(instance.level.getBrightness(LightLayer.BLOCK, blockPos), instance.level.getBrightness(LightLayer.SKY, blockPos));
               double computedValue = 1.2 + Math.pow(1.0 - brightness2 / 15.0, 2.0) * 10.8;
               float computedValue2 = Mth.clamp((float)computedValue, 1.2F, 12.0F);
               this.renderGamma = this.renderGamma + (computedValue2 - this.renderGamma) * 0.3F;
            } else {
               this.renderGamma = this.brightness.get().floatValue() / 5.0F;
            }
         } else if (this.mode.is("Potion")) {
            this.renderGamma = this.brightness.get().floatValue() / 5.0F;
         }

         if (this.mode.is("Potion")) {
            MobEffectInstance effect = player.getEffect(MobEffects.NIGHT_VISION);
            if (effect == null || effect.getDuration() < 220) {
               player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1337, 1, false, false, false));
            }
         } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
         }
      }
   }
}
