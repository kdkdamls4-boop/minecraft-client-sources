package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.event.PlayerInputEvent;

public final class MovementHelper {
   public static boolean isMoving() {
      return MovementInput.hasPlayerMovement();
   }

   public static void fixMovement(PlayerInputEvent playerInputEvent, float value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && playerInputEvent != null) {
         Vec2 vec2 = playerInputEvent.moveVector();
         float y = vec2.y;
         float x = vec2.x;
         if (y != 0.0F || x != 0.0F) {
            float yRot = instance.getYRot();
            float value2 = instance.isFallFlying() ? yRot : value;
            double computedValue = Mth.wrapDegrees((float)Math.toDegrees(direction(value2, y, x)));
            float value3 = 0.0F;
            float value4 = 0.0F;
            float mAX_VALUE = Float.MAX_VALUE;

            for (float index = -1.0F; index <= 1.0F; index++) {
               for (float index2 = -1.0F; index2 <= 1.0F; index2++) {
                  if (index2 != 0.0F || index != 0.0F) {
                     double computedValue2 = Mth.wrapDegrees((float)Math.toDegrees(direction(yRot, index, index2)));
                     float computedValue3 = Math.abs(Mth.wrapDegrees((float)(computedValue - computedValue2)));
                     if (computedValue3 < mAX_VALUE) {
                        mAX_VALUE = computedValue3;
                        value3 = index;
                        value4 = index2;
                     }
                  }
               }
            }

            playerInputEvent.setMoveVector(new Vec2(value4, value3));
            Input input = playerInputEvent.keyPresses();
            playerInputEvent.setKeyPresses(new Input(value3 > 0.0F, value3 < 0.0F, value4 > 0.0F, value4 < 0.0F, input.jump(), input.shift(), input.sprint()));
         }
      }
   }

   private MovementHelper() {
   }

   private static double direction(float value, double value2, double value3) {
      if (value2 < 0.0) {
         value += 180.0F;
      }

      float value4 = 1.0F;
      if (value2 < 0.0) {
         value4 = -0.5F;
      } else if (value2 > 0.0) {
         value4 = 0.5F;
      }

      if (value3 > 0.0) {
         value -= 90.0F * value4;
      }

      if (value3 < 0.0) {
         value += 90.0F * value4;
      }

      return Math.toRadians(value);
   }

   public static void setMotion(double motion) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null && instance.input != null) {
         Input input = instance.input.keyPresses;
         float computedValue = input.forward() == input.backward() ? 0.0F : (input.forward() ? 1.0F : -1.0F);
         float computedValue2 = input.left() == input.right() ? 0.0F : (input.left() ? 1.0F : -1.0F);
         if (computedValue != 0.0F || computedValue2 != 0.0F) {
            double yRot = direction(instance.getYRot(), computedValue, computedValue2);
            Vec3 deltaMovement = instance.getDeltaMovement();
            instance.setDeltaMovement(-Math.sin(yRot) * motion, deltaMovement.y, Math.cos(yRot) * motion);
         }
      }
   }
}
