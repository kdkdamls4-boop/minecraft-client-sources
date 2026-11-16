package net.skycore.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.AuraMarkerRenderer;
import net.skycore.client.util.TargetArcParticles;
import net.skycore.client.util.TargetScanRenderer;
import net.skycore.client.util.TargetOrbitParticles;
import net.skycore.client.util.TargetSpiralParticles;
import net.skycore.client.util.WriteHexByteHelper;

public final class TargetESPModule extends ClientModule {
   private final ColorSetting customColor;
   private String lastEffect;
   private final TargetScanRenderer scan;
   private final TargetArcParticles newGhosts;
   private final AuraMarkerRenderer marker;
   private final TargetOrbitParticles circle;
   private final ModeSetting palette;
   private final TargetSpiralParticles ghosts;
   private final ModeSetting effect = this.add(new ModeSetting("Effect", "Marker", "Marker", "Ghosts", "New Ghosts", "Circle", "Scan"));
   private final ColorSetting newGhostsFirstColor;
   private final ColorSetting newGhostsSecondColor;

   public void renderWorld() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null && instance.level != null && instance.gameRenderer != null) {
            String text = this.effect.get();
            if (!text.equals(this.lastEffect)) {
               this.resetRenderers();
               this.lastEffect = text;
            }

            AttackAuraModule attackAuraModule = SkycoreClient.modules().get(AttackAuraModule.class).orElse(null);
            LivingEntity entity = attackAuraModule != null && attackAuraModule.isEnabled() ? attackAuraModule.currentTarget() : null;
            float gameTimeDeltaPartialTick = instance.getDeltaTracker().getGameTimeDeltaPartialTick(true);
            int color = this.palette.is("Custom") ? this.customColor.get() : SkycoreClient.themes().accent();
            if (this.effect.is("Ghosts")) {
               this.ghosts.render(entity, gameTimeDeltaPartialTick, color);
            } else if (this.effect.is("New Ghosts")) {
               int value = this.palette.is("Custom") ? this.newGhostsFirstColor.get() : color;
               int value2 = this.palette.is("Custom") ? this.newGhostsSecondColor.get() : gradientPartner(color);
               this.newGhosts.render(entity, gameTimeDeltaPartialTick, value, value2);
            } else if (this.effect.is("Circle")) {
               this.circle.render(entity, gameTimeDeltaPartialTick, color);
            } else if (this.effect.is("Scan")) {
               this.scan.render(entity, gameTimeDeltaPartialTick, color);
            } else {
               this.marker.render(entity, gameTimeDeltaPartialTick, color);
            }
         } else {
            this.resetRenderers();
         }
      }
   }

   public TargetESPModule() {
      super("Target ESP", "Highlights the current AttackAura target", ModuleCategory.RENDER);
      this.palette = this.add(new ModeSetting("Palette", "Theme", "Theme", "Custom"));
      this.customColor = this.add(new ColorSetting("Custom Color", -16711800, false));
      this.newGhostsFirstColor = this.add(new ColorSetting("New Ghosts Color 1", -10773761, false));
      this.newGhostsSecondColor = this.add(new ColorSetting("New Ghosts Color 2", -3908353, false));
      this.marker = new AuraMarkerRenderer();
      this.ghosts = new TargetSpiralParticles();
      this.newGhosts = new TargetArcParticles();
      this.circle = new TargetOrbitParticles();
      this.scan = new TargetScanRenderer();
      this.lastEffect = "";
      this.customColor.visibleWhen(() -> {
         if (this.palette.is("Custom")) {
            if (!this.effect.is("New Ghosts")) {
               return true;
            }
         }

         return false;
      });
      this.newGhostsFirstColor.visibleWhen(() -> {
         if (this.effect.is("New Ghosts")) {
            if (this.palette.is("Custom")) {
               return true;
            }
         }

         return false;
      });
      this.newGhostsSecondColor.visibleWhen(() -> {
         if (this.effect.is("New Ghosts")) {
            if (this.palette.is("Custom")) {
               return true;
            }
         }

         return false;
      });
   }

   @Override
   protected void onDisable() {
      this.resetRenderers();
      this.marker.release();
      this.scan.release();
      this.lastEffect = "";
   }

   @Override
   protected void onEnable() {
      this.lastEffect = "";
   }

   private void resetRenderers() {
      this.marker.reset();
      this.ghosts.reset();
      this.newGhosts.reset();
      this.circle.reset();
      this.scan.reset();
   }

   private static int gradientPartner(int value) {
      float[] floatValues = WriteHexByteHelper.hsv(value);
      float computedValue = (floatValues[0] + 0.14F) % 1.0F;
      float computedValue2 = floatValues[1] * 0.72F + 0.08F;
      float clampedValue = Mth.clamp(computedValue2, 0.2F, 0.9F);
      float value2 = floatValues[2];
      float computedValue3 = Mth.clamp(Math.max(value2, 0.45F) * 1.18F, 0.55F, 1.0F);
      int value3 = WriteHexByteHelper.fromHsv(computedValue, clampedValue, computedValue3, 255);
      return WriteHexByteHelper.lerp(value3, -1, 0.22F);
   }
}
