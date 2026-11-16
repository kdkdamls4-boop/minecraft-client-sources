package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.BrushstrokeTrailRenderer;

public final class TrailsModule extends ClientModule {
   private final List<BrushstrokeTrailRenderer.Stroke> strokes;
   private Vec3 previousPosition;
   private static final double MIN_MOVEMENT = 0.018;
   private long strokeSerial;
   private final NumberSetting length;
   private static final int MAX_STROKES = 28;
   private final NumberSetting speed;
   private final NumberSetting width;
   public static final TrailsModule INSTANCE = new TrailsModule();
   private final BrushstrokeTrailRenderer renderer;
   private final Random random;
   private double spawnBalance;
   private Object previousLevel;
   private final NumberSetting intensity = this.add(
      new NumberSetting("Intensity", "Number of individual hand-painted motion strokes", 1.4, 0.3, 3.5, 0.1, "x")
   );

   private TrailsModule() {
      super("Trails", "Cartoonish black-and-white 3D brushstrokes behind the moving player", ModuleCategory.RENDER);
      this.length = this.add(new NumberSetting("Length", "Maximum length of each painted streak", 2.55, 1.0, 4.0, 0.1, " blocks"));
      this.width = this.add(new NumberSetting("Width", "Brushstroke thickness", 1.0, 0.35, 2.0, 0.05, "x"));
      this.speed = this.add(new NumberSetting("Speed", "Brush drawing and disappearance speed", 1.0, 0.4, 2.5, 0.1, "x"));
      this.strokes = new ArrayList<>();
      this.renderer = new BrushstrokeTrailRenderer();
      this.random = new Random();
   }

   @Override
   protected void onDisable() {
      this.reset();
      this.renderer.release();
   }

   @Override
   protected void onEnable() {
      this.reset();
      Minecraft instance = Minecraft.getInstance();
      this.previousLevel = instance.level;
      this.previousPosition = instance.player == null ? null : instance.player.position();
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null && instance.player != null) {
         if (this.previousLevel != instance.level) {
            this.reset();
            this.previousLevel = instance.level;
         }

         Vec3 position = instance.player.position();
         if (this.previousPosition == null) {
            this.previousPosition = position;
         } else {
            Vec3 position2 = position.subtract(this.previousPosition);
            this.previousPosition = position;
            double x = Math.hypot(position2.x, position2.z);
            long nowNanos = System.nanoTime();
            double elapsed = 1.0 / this.speed.get();
            this.strokes.removeIf(stroke -> {
               long timestamp = nowNanos - stroke.bornNanos();
               if (timestamp < (long)(stroke.lifetimeNanos() * elapsed)) {
                  Vec3 position3 = stroke.origin();
                  if (!(position3.distanceToSqr(position) > 12.25)) {
                     return false;
                  }
               }

               return true;
            });
            if (!instance.options.getCameraType().isFirstPerson() && !(x < 0.018) && !(x > 1.8)) {
               Vec3 position4 = new Vec3(-position2.x / x, 0.0, -position2.z / x);
               Vec3 position5 = new Vec3(-position4.z, 0.0, position4.x);
               double minimum = Math.min(1.0, x / 0.21);
               this.spawnBalance = this.spawnBalance + (0.18 + minimum * 0.58) * this.intensity.get();
               int computedValue = Math.min(2, (int)this.spawnBalance);
               this.spawnBalance = Math.min(1.0, this.spawnBalance - computedValue);

               for (int value = 0; value < computedValue; value++) {
                  if (this.strokes.size() >= 28) {
                     this.strokes.remove(0);
                  }

                  int computedValue2 = (int)((this.strokeSerial + this.random.nextInt(2)) % 5L);

                  double computedValue3 = switch (computedValue2) {
                     case 0 -> 1.52;
                     case 1 -> 1.3;
                     case 2 -> 1.04;
                     case 3 -> 0.73;
                     default -> 0.41;
                  };
                  computedValue3 += (this.random.nextDouble() - 0.5) * 0.18;

                  double computedValue4 = switch (computedValue2) {
                     case 0, 1 -> 0.27;
                     case 2 -> 0.22;
                     default -> 0.16;
                  };
                  double computedValue5 = (this.random.nextDouble() * 2.0 - 1.0) * computedValue4;
                  Vec3 position6 = position.add(0.0, computedValue3, 0.0).add(position5.scale(computedValue5)).add(position4.scale(0.23 + this.random.nextDouble() * 0.17));
                  float computedValue6 = (float)(this.length.get() * (0.39 + this.random.nextDouble() * 0.26) * (0.68 + 0.32 * minimum));
                  float computedValue7 = (float)(this.width.get() * (0.135 + this.random.nextDouble() * 0.085) * (0.75 + 0.25 * minimum));
                  long timestamp2 = (long)((0.22 + this.random.nextDouble() * 0.13) * 1.0E9);
                  int value2 = this.random.nextFloat() < 0.19F ? 1 : 0;
                  this.strokes
                     .add(
                        new BrushstrokeTrailRenderer.Stroke(
                           position6,
                           position4,
                           position5,
                           computedValue6,
                           computedValue7,
                           timestamp2,
                           nowNanos - (long)(this.random.nextFloat() * 2.0E7F),
                           this.random.nextFloat() * 1000.0F,
                           (float)minimum,
                           (value2 != 0),
                           ++this.strokeSerial
                        )
                     );
               }
            } else {
               this.spawnBalance = 0.0;
               if (instance.options.getCameraType().isFirstPerson() || x > 1.8) {
                  this.strokes.clear();
               }
            }
         }
      } else {
         this.reset();
      }
   }

   private void reset() {
      this.strokes.clear();
      this.previousLevel = null;
      this.previousPosition = null;
      this.spawnBalance = 0.0;
      this.strokeSerial = 0L;
   }

   public void renderWorld() {
      if (this.isEnabled()) {
         if (!this.strokes.isEmpty()) {
            Minecraft instance = Minecraft.getInstance();
            if (instance.level != null) {
               if (instance.level == this.previousLevel && instance.player != null) {
                  if (instance.options.getCameraType().isFirstPerson()) {
                     return;
                  }

                  BrushstrokeTrailRenderer renderer2 = this.renderer;
                  var strokes2 = this.strokes;
                  float value = this.speed.get().floatValue();
                  long nowNanos = System.nanoTime();
                  renderer2.render(strokes2, value, nowNanos);
                  return;
               }
            }

            this.reset();
            return;
         }
      }
   }
}
