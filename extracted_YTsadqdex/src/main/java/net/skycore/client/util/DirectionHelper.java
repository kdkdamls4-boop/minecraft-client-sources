package net.skycore.client.util;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.event.PlayerInputEvent;

public final class DirectionHelper {
   public static void apply(PlayerInputEvent playerInputEvent, float value, float value2) {
      float y = playerInputEvent.moveVector().y;
      float x = playerInputEvent.moveVector().x;
      if (y != 0.0F || x != 0.0F) {
         double index = Mth.wrapDegrees(Math.toDegrees(direction(value2, y, x)));
         float mAX_VALUE = Float.MAX_VALUE;
         float index2 = 0.0F;
         float index3 = 0.0F;

         for (float index4 = -1.0F; index4 <= 1.0F; index4++) {
            for (float index5 = -1.0F; index5 <= 1.0F; index5++) {
               if (index4 != 0.0F || index5 != 0.0F) {
                  double computedValue = Mth.wrapDegrees(Math.toDegrees(direction(value, index4, index5)));
                  float computedValue2 = Math.abs(Mth.wrapDegrees((float)(index - computedValue)));
                  if (computedValue2 < mAX_VALUE) {
                     mAX_VALUE = computedValue2;
                     index2 = index4;
                     index3 = index5;
                  }
               }
            }
         }

         playerInputEvent.setMoveVector(new Vec2(index3, index2).normalized());
         Input input = playerInputEvent.keyPresses();
         playerInputEvent.setKeyPresses(new Input(index2 > 0.0F, index2 < 0.0F, index3 > 0.0F, index3 < 0.0F, input.jump(), input.shift(), input.sprint()));
      }
   }

   public static void apply(PlayerInputEvent playerInputEvent, float yaw) {
      apply(playerInputEvent, yaw, SnapBodyToFreeLookHelper.getYaw());
   }

   private DirectionHelper() {
   }

   private static double direction(float value, float value2, float value3) {
      if (value2 < 0.0F) {
         value += 180.0F;
      }

      float computedValue = value2 < 0.0F ? -0.5F : (value2 > 0.0F ? 0.5F : 1.0F);
      if (value3 > 0.0F) {
         value -= 90.0F * computedValue;
      }

      if (value3 < 0.0F) {
         value += 90.0F * computedValue;
      }

      double value4 = value;
      return Math.toRadians(value4);
   }
}
