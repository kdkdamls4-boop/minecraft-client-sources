package deluxe.client.util;

import deluxe.client.MinecraftAccess;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Vector3f;

public final class PlayerUtils implements MinecraftAccess {
   public static double method4069() {
      return 1488.0;
   }

   public static PlayerInput method4070(PlayerInput playerInput, double local6831_1, float local6831_3) {
      boolean local6831_4 = playerInput.forward();
      boolean local6831_5 = playerInput.backward();
      boolean local6831_6 = playerInput.left();
      boolean local6831_7 = playerInput.right();
      if (local6831_1 >= -90.0F + local6831_3 && local6831_1 <= 90.0F - local6831_3) {
         local6831_4 = true;
      } else if (local6831_1 < -90.0F - local6831_3 || local6831_1 > 90.0F + local6831_3) {
         local6831_5 = true;
      }

      if (local6831_1 >= 0.0F + local6831_3 && local6831_1 <= 180.0F - local6831_3) {
         local6831_7 = true;
      } else if (local6831_1 >= -180.0F + local6831_3 && local6831_1 <= 0.0F - local6831_3) {
         local6831_6 = true;
      }

      return new PlayerInput(local6831_4, local6831_5, local6831_6, local6831_7, playerInput.jump(), playerInput.sneak(), playerInput.sprint());
   }

   private PlayerUtils() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static float method4071(
      float local6833_0, float local6833_1, double local6833_2, double local6833_4, double local6833_6, double local6833_8, float local6833_10
   ) {
      double local6833_11 = local6833_6 - local6833_2;
      double local6833_13 = local6833_8 - local6833_4;
      float local6833_15 = (float)(local6833_11 * local6833_11 + local6833_13 * local6833_13);
      float local6833_16 = local6833_1;
      float local6833_17 = mc.player.handSwingProgress;
      if (local6833_15 > 0.0025000002F) {
         float local6833_18 = (float)MathHelper.atan2(local6833_13, local6833_11) * (180.0F / (float)Math.PI) - 90.0F;
         float local6833_19 = MathHelper.abs(MathHelper.wrapDegrees(local6833_0) - local6833_18);
         if (95.0F < local6833_19 && local6833_19 < 265.0F) {
            local6833_16 = local6833_18 - 180.0F;
         } else {
            local6833_16 = local6833_18;
         }
      }

      if (mc.player != null && mc.player.handSwingProgress - 0.3F > 0.0F) {
         local6833_16 = local6833_0;
      }

      float local6833_18 = MathHelper.wrapDegrees(local6833_16 - local6833_1);
      local6833_16 = local6833_1 + local6833_18 * 0.3F;
      float local6833_19 = MathHelper.wrapDegrees(local6833_0 - local6833_16);
      float local6833_20 = 52.0F;
      if (Math.abs(local6833_19) > local6833_20) {
         local6833_16 += local6833_19 - MathHelper.sign(local6833_19) * local6833_20;
      }

      return local6833_16;
   }

   public static String method4072() {
      return "lunarclient:v2.21.5-2540    ";
   }

   public static double method4073(Vec3d vec3d, float local6835_1) {
      float local6835_2 = (float)Math.atan2(-vec3d.x, vec3d.z);
      double local6835_3 = Math.toRadians(MathHelper.wrapDegrees(local6835_1));
      return Math.toDegrees(MathHelper.wrapDegrees(local6835_2 - local6835_3));
   }

   public static final boolean method4074(int local6836_0) {
      boolean isPressed = mc.options.forwardKey.isPressed();
      boolean isPressed2 = mc.options.leftKey.isPressed();
      boolean isPressed3 = mc.options.backKey.isPressed();
      boolean isPressed4 = mc.options.rightKey.isPressed();
      return local6836_0 == 0 ? isPressed : (local6836_0 == 1 ? isPressed2 : (local6836_0 == 2 ? isPressed3 : local6836_0 == 3 && isPressed4));
   }

   public static final boolean method4075() {
      return method4074(0);
   }

   public static PlayerInput method4076(PlayerInput playerInput, double local6838_1) {
      return method4070(playerInput, local6838_1, 20.0F);
   }

   public static final boolean method4077() {
      return method4074(3);
   }

   public static double method4078(Entity entity) {
      return Math.sqrt(entity.squaredDistanceTo(new Vec3d(entity.lastX, entity.lastY, entity.lastZ)));
   }

   public static boolean method4079() {
      return mc.player.input.getMovementInput().lengthSquared() > 0.0F;
   }

   public static final boolean method4080() {
      return method4074(1);
   }

   public static final boolean method4081() {
      return method4074(2);
   }

   public static float method4082(int local6844_0) {
      Vector3f vector3f = new Vector3f(0.0F, (float)mc.player.getVelocity().y, 0.0F);
      if (vector3f.y != 0.0F && !mc.player.isOnGround()) {
         float local6844_2 = 0.0F;
         double local6844_3 = 0.08;
         boolean local6844_5 = vector3f.y <= 0.0;
         if (local6844_5 && mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            local6844_3 = 0.01;
         }

         for (int index = 0; index < local6844_0 + 1; index++) {
            double local6844_7 = vector3f.y;
            local6844_7 -= local6844_3;
            vector3f.y = (float)(local6844_7 * 0.98F);
            if (vector3f.y > 0.0F) {
               local6844_2 = 0.0F;
            } else {
               local6844_2 -= vector3f.y;
            }
         }

         return local6844_2;
      } else {
         return 0.0F;
      }
   }

   public static PlayerUtils.Nested0517 method4083(PlayerEntity playerEntity, int local6845_1) {
      PlayerUtils.Nested0517 local6845_2 = new PlayerUtils.Nested0517();
      local6845_2.field13978 = playerEntity.getEntityPos().toVector3f();
      local6845_2.field13979 = playerEntity.getVelocity().toVector3f();
      double local6845_3 = playerEntity.getEntityPos().y;

      for (int index = 0; index < local6845_1; index++) {
         double local6845_6 = 0.08;
         boolean local6845_8 = playerEntity.getVelocity().y <= 0.0;
         if (local6845_8 && playerEntity.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            local6845_6 = 0.01;
         }

         local6845_2.field13979 = local6845_2.field13979.add(0.0F, (float)(-local6845_6 / 4.0), 0.0F);
         BlockHitResult blockHitResult = playerEntity.getEntityWorld()
            .raycast(
               new RaycastContext(
                  new Vec3d(local6845_2.field13978),
                  new Vec3d(local6845_2.field13978.add(local6845_2.field13979)),
                  ShapeType.OUTLINE,
                  FluidHandling.WATER,
                  playerEntity
               )
            );
         local6845_2.field13978 = local6845_2.field13978.add(local6845_2.field13979);
      }

      local6845_2.field13977 = method4082(local6845_1);
      return local6845_2;
   }

   public static final float method4084(float local6846_0) {
      return local6846_0
         + (
            !method4080() || !method4077() || method4075() && method4081() || !method4075() && !method4081()
               ? (
                  !method4075() || !method4081() || method4080() && method4077() || !method4080() && !method4077()
                     ? (
                        (!method4080() || !method4077() || method4075() && method4081()) && (!method4075() || !method4081() || method4080() && method4077())
                           ? (
                              !method4080() && !method4077() && !method4081()
                                 ? 0
                                 : (
                                       method4075() && !method4081()
                                          ? 45
                                          : (
                                             !method4081() || method4075()
                                                ? (!method4075() && !method4081() || method4075() && method4081() ? 90 : 0)
                                                : (!method4080() && !method4077() ? 180 : 135)
                                          )
                                    )
                                    * (method4080() ? -1 : 1)
                           )
                           : 0
                     )
                     : (method4080() ? -90 : (method4077() ? 90 : 0))
               )
               : (method4075() ? 0 : (method4081() ? 180 : 0))
         );
   }

   public static class Nested0517 {
      public boolean field13976 = false;
      public float field13977 = 0.0F;
      public Vector3f field13978 = new Vector3f();
      public Vector3f field13979 = new Vector3f();
   }
}
