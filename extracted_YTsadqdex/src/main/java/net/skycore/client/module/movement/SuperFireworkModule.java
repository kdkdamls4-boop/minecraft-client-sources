package net.skycore.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.BooleanSetting;

public final class SuperFireworkModule extends ClientModule {
   private static final float[] REALLY_WORLD_1211_XZ = new float[]{1.5F, 1.51F, 1.53F, 1.57F, 1.62F, 1.64F, 1.79F, 1.88F, 1.98F};
   private static final float[] BRAVO_GRIEF_XZ = new float[]{1.67F, 1.68F, 1.69F, 1.72F, 1.72F, 1.73F, 1.74F, 1.75F, 1.76F};
   public static final SuperFireworkModule INSTANCE = new SuperFireworkModule();
   private final SliderSetting[] xzSliders;
   private static final float[] BRAVO_GRIEF_Y = new float[]{1.72F, 1.72F, 1.72F, 1.72F, 1.72F, 1.73F, 1.74F, 1.76F, 1.8F};
   private static final float[] REALLY_WORLD_Y = new float[]{1.6F, 1.62F, 1.6F, 1.62F, 1.64F, 1.68F, 1.75F, 1.9F, 2.16F};
   private static final float[] REALLY_WORLD_XZ = new float[]{1.6F, 1.63F, 1.66F, 1.71F, 1.73F, 1.81F, 1.81F, 1.81F, 1.81F};
   private final SliderSetting[] yGroupSliders;
   private static final float[] REALLY_WORLD_1211_Y = new float[]{1.5F, 1.52F, 1.53F, 1.56F, 1.64F, 1.68F, 1.77F, 1.84F, 2.01F};
   private final BooleanSetting boost = this.add(new BooleanSetting("Boost", true));
   private static final String REALLY_WORLD_1211_MODE = "RW 1.21.11";
   private final ModeSetting mode = this.add(new ModeSetting("Boost Mode", "ReallyWorld", "ReallyWorld", "Bravo", "BravoGrief", "RW 1.21.11", "Custom"));

   public boolean shouldBoost() {
      if (this.isEnabled()) {
         Boolean get = this.boost.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public float[] resolveBoost(int value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && this.shouldBoost()) {
         float yRot = Mth.wrapDegrees(instance.getYRot());
         float xRot = instance.getXRot();
         if (this.mode.is("Custom")) {
            float customSpeed = this.getCustomSpeed(yRot, this.xzSliders);
            float customSpeed2 = this.getCustomSpeed(xRot, this.yGroupSliders);
            if (customSpeed2 > customSpeed) {
               customSpeed = customSpeed2;
            }

            return new float[]{customSpeed, customSpeed2};
         } else {
            if (this.mode.is("Bravo")) {
               return new float[]{this.getBravoSpeedXZ(xRot, yRot), this.getBravoSpeedY(xRot)};
            }

            if (this.mode.is("BravoGrief")) {
               return new float[]{getTableSpeed(BRAVO_GRIEF_XZ, yRot, 9), getTableSpeed(BRAVO_GRIEF_Y, xRot, 9)};
            }

            if (this.mode.is("RW 1.21.11")) {
               float reallyWorld1211SpeedXZ = getReallyWorld1211SpeedXZ(xRot, yRot);
               float reallyWorld1211SpeedY = getReallyWorld1211SpeedY(xRot);
               return new float[]{reallyWorld1211SpeedXZ, reallyWorld1211SpeedY};
            }

            float tableSpeed = getTableSpeed(REALLY_WORLD_XZ, yRot, 9);
            float tableSpeed2 = getTableSpeed(REALLY_WORLD_Y, xRot, 9);
            if (tableSpeed2 > tableSpeed) {
               tableSpeed = tableSpeed2;
            }

            return new float[]{tableSpeed, tableSpeed2};
         }
      } else {
         return new float[]{1.5F, 1.5F};
      }
   }

   private float getCustomSpeed(float angle, SliderSetting[] clientComponent021Values) {
      float value = convertAngle(angle);
      int computedValue = (int)(value / 5.0F);
      if (computedValue >= clientComponent021Values.length) {
         computedValue = clientComponent021Values.length - 1;
      }

      if (computedValue < 0) {
         computedValue = 0;
      }

      SliderSetting sliderSetting = clientComponent021Values[computedValue];
      return sliderSetting.get().floatValue();
   }

   private static float getReallyWorld1211SpeedY(float speed) {
      float value = convertAngle(speed);
      if (value >= 36.0F && value < 37.0F) {
         return 1.81F;
      }

      return getTableSpeed(REALLY_WORLD_1211_Y, speed, 9);
   }

   private static float getReallyWorld1211SpeedXZ(float speed, float speed2) {
      float tableSpeed = getTableSpeed(REALLY_WORLD_1211_XZ, speed2, 9);
      float reallyWorld1211SpeedY = getReallyWorld1211SpeedY(speed);
      return Math.max(tableSpeed, reallyWorld1211SpeedY);
   }

   private float getBravoSpeedY(float value) {
      float computedValue = Math.abs(value);
      if (computedValue >= 37.0F && computedValue <= 38.0F) {
         return 2.03F;
      } else if (computedValue >= 25.0F && computedValue <= 30.0F) {
         return 2.0F;
      } else if (computedValue >= 35.0F && computedValue <= 45.0F) {
         return 1.99F;
      } else if (computedValue >= 40.0F && computedValue <= 50.0F) {
         return 1.97F;
      } else if (computedValue >= 50.0F && computedValue <= 60.0F) {
         return 1.96F;
      } else if (computedValue >= 51.0F && computedValue <= 61.0F) {
         return 1.85F;
      } else {
         return computedValue >= 52.0F && computedValue <= 65.0F ? 1.8F : 1.59F;
      }
   }

   private float getBravoSpeedXZ(float value, float value2) {
      float computedValue = Math.abs(value);
      float computedValue2 = Math.abs(Mth.wrapDegrees(value2) % 90.0F);
      float value3;
      if (computedValue >= 38.0F && computedValue <= 52.0F) {
         value3 = 2.0F;
      } else if (computedValue >= 32.0F && computedValue <= 58.0F) {
         value3 = 1.96F;
      } else if (computedValue >= 28.0F && computedValue <= 62.0F) {
         value3 = 1.95F;
      } else if ((!(computedValue2 >= 29.0F) || !(computedValue2 <= 61.0F)) && (!(computedValue >= 29.0F) || !(computedValue <= 61.0F))) {
         if ((!(computedValue2 >= 28.0F) || !(computedValue2 <= 60.0F)) && (!(computedValue >= 28.0F) || !(computedValue <= 60.0F))) {
            if ((!(computedValue2 >= 26.0F) || !(computedValue2 <= 64.0F)) && (!(computedValue >= 26.0F) || !(computedValue <= 64.0F))) {
               if ((!(computedValue2 >= 24.0F) || !(computedValue2 <= 66.0F)) && (!(computedValue >= 24.0F) || !(computedValue <= 66.0F))) {
                  if ((!(computedValue2 >= 15.0F) || !(computedValue2 <= 75.0F)) && (!(computedValue >= 15.0F) || !(computedValue <= 75.0F))) {
                     if ((!(computedValue2 >= 13.0F) || !(computedValue2 <= 77.0F)) && (!(computedValue >= 13.0F) || !(computedValue <= 77.0F))) {
                        if ((!(computedValue2 >= 12.0F) || !(computedValue2 <= 78.0F)) && (!(computedValue >= 12.0F) || !(computedValue <= 78.0F))) {
                           if ((!(computedValue2 >= 8.0F) || !(computedValue2 <= 82.0F)) && (!(computedValue >= 11.0F) || !(computedValue <= 79.0F))) {
                              if ((!(computedValue2 >= 5.0F) || !(computedValue2 <= 85.0F)) && (!(computedValue >= 8.0F) || !(computedValue <= 82.0F))) {
                                 if (!(computedValue2 <= 90.0F) && !(computedValue <= 90.0F)) {
                                    value3 = 1.66F;
                                 } else {
                                    value3 = 1.67F;
                                 }
                              } else {
                                 value3 = 1.67F;
                              }
                           } else {
                              value3 = 1.75F;
                           }
                        } else {
                           value3 = 1.75F;
                        }
                     } else {
                        value3 = 1.75F;
                     }
                  } else {
                     value3 = 1.75F;
                  }
               } else {
                  value3 = 1.75F;
               }
            } else {
               value3 = 1.874F;
            }
         } else {
            value3 = 1.954F;
         }
      } else {
         value3 = 1.963F;
      }

      return value > 15.0F ? value3 - 0.068F : value3;
   }

   private static float convertAngle(float value) {
      float result = Math.abs(value);
      if (result > 90.0F) {
         result = 180.0F - result;
      }

      if (result > 45.0F) {
         result = 90.0F - result;
      }

      return result;
   }

   private SuperFireworkModule() {
      super("Super Firework", "Boosts elytra flight from fireworks", ModuleCategory.MOVEMENT);
      this.xzSliders = new SliderSetting[]{
         slider("XZ 0-5", 1.52F),
         slider("XZ 5-10", 1.53F),
         slider("XZ 10-15", 1.54F),
         slider("XZ 15-20", 1.55F),
         slider("XZ 20-25", 1.56F),
         slider("XZ 25-30", 1.57F),
         slider("XZ 30-35", 1.58F),
         slider("XZ 35-40", 1.59F),
         slider("XZ 40-45", 1.6F)
      };
      this.yGroupSliders = new SliderSetting[]{
         slider("Y 0-5", 1.51F),
         slider("Y 5-10", 1.52F),
         slider("Y 10-15", 1.53F),
         slider("Y 15-20", 1.54F),
         slider("Y 20-25", 1.55F),
         slider("Y 25-30", 1.56F),
         slider("Y 30-35", 1.57F),
         slider("Y 35-40", 1.58F),
         slider("Y 40-45", 1.59F),
         slider("Y 45-50", 1.6F),
         slider("Y 50-55", 1.61F)
      };
      this.mode.visibleWhen(this.boost::get);

      for (SliderSetting sliderSetting : this.xzSliders) {
         this.add(sliderSetting);
         sliderSetting.visibleWhen(() -> {
            if (this.boost.get()) {
               if (this.mode.is("Custom")) {
                  return true;
               }
            }

            return false;
         });
      }

      for (SliderSetting sliderSetting2 : this.yGroupSliders) {
         this.add(sliderSetting2);
         sliderSetting2.visibleWhen(() -> {
            if (this.boost.get()) {
               if (this.mode.is("Custom")) {
                  return true;
               }
            }

            return false;
         });
      }
   }

   private static float getTableSpeed(float[] floatValues, float angle, int value) {
      int computedValue = (int)(convertAngle(angle) / 5.0F);
      if (computedValue >= value) {
         computedValue = value - 1;
      }

      if (computedValue < 0) {
         computedValue = 0;
      }

      return floatValues[computedValue];
   }

   private static SliderSetting slider(String text, float value) {
      double intermediate = value;
      return new SliderSetting(text, intermediate, 1.5, 3.0, 0.01);
   }

   public float responseSpeed() {
      return this.resolveBoost(0)[0];
   }
}
