package deluxe.client;

import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.util.MathUtils;
import deluxe.client.event.MovementInputEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.EntityUtils;
import deluxe.client.util.MouseSensitivity;
import deluxe.client.util.Stopwatch;

import net.minecraft.util.math.MathHelper;

public final class RotationManager implements MinecraftAccess {
   private static RotationManager rotationManager;
   private float field00944;
   private float field00945;
   private float field00946;
   private float field00947;
   private float field00948;
   private float field00949;
   private float field00950;
   private float field00951;
   private RotationAngles rotationAngles;
   private RotationSpeed rotationSpeed;
   private boolean field00954;
   private long field00955;
   private long field00956;
   private long field00957;
   private long field00958 = -1L;
   private final Stopwatch stopwatch = new Stopwatch();
   private RotationManager.ReturnMode returnMode = RotationManager.ReturnMode.Smooth;
   private final EventListener<PlayerTickEvent> field00961 = playerTickEvent -> {
      if (mc.player != null) {
         this.field00955++;
         this.field00948 = this.field00948 + (this.field00950 - this.field00948) * 0.4F;
         this.field00949 = this.field00949 + (this.field00951 - this.field00949) * 0.4F;
         this.field00950 = this.field00951 = 0.0F;
         if (this.field00955 - this.field00956 <= 1L && this.rotationAngles != null && this.rotationSpeed != null) {
            if (!this.field00954) {
               this.field00946 = mc.player.getYaw();
               this.field00947 = mc.player.getPitch();
               this.field00954 = true;
            }

            this.method0663(this.rotationAngles, this.rotationSpeed.method2283(), this.rotationSpeed.method2285());
         } else if (this.field00954 && this.rotationAngles != null && !this.stopwatch.hasExceededMillis(this.field00957)) {
            this.method0663(this.rotationAngles, this.rotationSpeed.method2283(), this.rotationSpeed.method2285());
         } else if (this.field00954) {
            if (this.returnMode == RotationManager.ReturnMode.SmoothFast) {
               float local854_2 = MathHelper.wrapDegrees(this.field00946 - mc.player.getYaw());
               float local854_3 = this.field00947 - mc.player.getPitch();
               if (Math.abs(local854_2) <= this.rotationSpeed.method2282() && Math.abs(local854_3) <= this.rotationSpeed.method2284()) {
                  mc.player.setYaw(mc.player.headYaw = this.field00946);
                  mc.player.setPitch(this.field00947);
                  this.field00958 = -1L;
                  this.field00954 = false;
                  return;
               }

               float local854_4 = (float)Math.hypot(local854_2, local854_3);
               float local854_5 = mc.player.getAttackCooldownProgress(1.0F) >= 1.0F ? MathUtils.randomFloat(35.0F, 45.0F) : 0.0F;
               float local854_6 = Math.abs(local854_2 / local854_4) * local854_5;
               float local854_7 = Math.abs(local854_3 / local854_4) * local854_5;
               float local854_8 = MathUtils.randomFloat(-0.4F, 0.4F);
               float local854_9 = MathUtils.randomFloat(-0.2F, 0.2F);
               float local854_10 = MathHelper.lerp(
                  0.85F, mc.player.getYaw(), mc.player.getYaw() + MathHelper.clamp(local854_2, -local854_6, local854_6) + local854_8
               );
               float local854_11 = MathHelper.lerp(
                  0.85F, mc.player.getPitch(), mc.player.getPitch() + MathHelper.clamp(local854_3, -local854_7, local854_7) + local854_9
               );
               mc.player
                  .setYaw(
                     mc.player.headYaw = mc.player.headYaw
                        + MouseSensitivity.method4410(MathHelper.wrapDegrees(local854_10 - mc.player.getYaw()))
                  );
               mc.player
                  .setPitch(
                     MathHelper.clamp(mc.player.getPitch() + MouseSensitivity.method4410(local854_11 - mc.player.getPitch()), -90.0F, 90.0F)
                  );
               return;
            }

            if (this.returnMode == RotationManager.ReturnMode.Empty) {
               this.field00954 = false;
               return;
            }

            float local854_2 = MathHelper.wrapDegrees(this.field00946 - mc.player.getYaw());
            float local854_3 = this.field00947 - mc.player.getPitch();
            float local854_4 = Math.min(Math.abs(local854_2), this.rotationSpeed.method2282());
            float local854_5 = Math.min(Math.abs(local854_3), this.rotationSpeed.method2284());
            mc.player
               .setYaw(
                  mc.player.headYaw = mc.player.headYaw + MouseSensitivity.method4410(MathHelper.clamp(local854_2, -local854_4, local854_4))
               );
            mc.player
               .setPitch(
                  MathHelper.clamp(
                     mc.player.getPitch() + MouseSensitivity.method4410(MathHelper.clamp(local854_3, -local854_5, local854_5)), -90.0F, 90.0F
                  )
               );
            if (Math.abs(local854_2) <= this.rotationSpeed.method2282() && Math.abs(local854_3) <= this.rotationSpeed.method2284()) {
               mc.player.setYaw(mc.player.headYaw = this.field00946);
               mc.player.setPitch(this.field00947);
               this.field00954 = false;
            }
         }
      }
   };

   public void method0648(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, long local837_3) {
      this.method0665(newRotationAngles, newRotationSpeed, local837_3, RotationManager.ReturnMode.Smooth);
   }

   public void method0649() {
      if (mc.player != null && this.field00954) {
         this.field00946 = mc.player.getYaw();
         this.field00947 = mc.player.getPitch();
      }
   }

   public static void method0650(MovementInputEvent movementInputEvent, float local839_1) {
      float local839_2 = movementInputEvent.method4441();
      float local839_3 = movementInputEvent.method4447();
      double local839_4 = MathHelper.wrapDegrees(Math.toDegrees(method0656(local839_1, local839_2, local839_3)));
      if (local839_2 != 0.0F || local839_3 != 0.0F) {
         float local839_6 = 0.0F;
         float local839_7 = 0.0F;
         float local839_8 = Float.MAX_VALUE;

         for (float local839_9 = -1.0F; local839_9 <= 1.0F; local839_9++) {
            for (float local839_10 = -1.0F; local839_10 <= 1.0F; local839_10++) {
               if (local839_10 != 0.0F || local839_9 != 0.0F) {
                  double local839_11 = MathHelper.wrapDegrees(Math.toDegrees(method0656(mc.player.getYaw(), local839_9, local839_10)));
                  double local839_13 = Math.abs(local839_4 - local839_11);
                  if (local839_13 < local839_8) {
                     local839_8 = (float)local839_13;
                     local839_6 = local839_9;
                     local839_7 = local839_10;
                  }
               }
            }
         }

         movementInputEvent.method4446(local839_6);
         movementInputEvent.method4449(local839_7);
      }
   }

   public float method0651() {
      return this.field00948;
   }

   public void method0652(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed) {
      this.method0648(newRotationAngles, newRotationSpeed, 0L);
   }

   public void method0653() {
      this.field00954 = false;
      this.rotationAngles = null;
      this.field00958 = -1L;
   }

   public boolean method0654(double local843_1, double local843_3) {
      if (!this.field00954) {
         return false;
      }

      this.field00946 += (float)local843_1 * 0.15F;
      this.field00947 = MathHelper.clamp(this.field00947 + (float)local843_3 * 0.15F, -90.0F, 90.0F);
      this.field00950 = this.field00950 + Math.abs((float)local843_1 * 0.15F);
      this.field00951 = this.field00951 + Math.abs((float)local843_3 * 0.15F);
      return true;
   }

   public float method0655() {
      return this.field00947;
   }

   private RotationManager() {
   }

   private static double method0656(float local846_0, float local846_1, float local846_2) {
      return Math.atan2(
         local846_1 * Math.cos(Math.toRadians(local846_0)) + local846_2 * Math.sin(Math.toRadians(local846_0)),
         local846_2 * Math.cos(Math.toRadians(local846_0)) - local846_1 * Math.sin(Math.toRadians(local846_0))
      );
   }

   public float method0657() {
      return this.field00944;
   }

   public static RotationManager method0658() {
      if (rotationManager == null) {
         rotationManager = new RotationManager();
         EventBus.getInstance().subscribe(rotationManager);
      }

      return rotationManager;
   }

   public static void method0659(MovementInputEvent movementInputEvent, float local849_1, float local849_2) {
      if (movementInputEvent.method4441() != 0.0F || movementInputEvent.method4447() != 0.0F) {
         double local849_3 = MathHelper.wrapDegrees(
            (float)Math.toDegrees(method0656(mc.player.isGliding() ? local849_1 : local849_2, movementInputEvent.method4441(), movementInputEvent.method4447()))
         );
         float local849_5 = 0.0F;
         float local849_6 = 0.0F;
         float local849_7 = Float.MAX_VALUE;

         for (float local849_8 = -1.0F; local849_8 <= 1.0F; local849_8++) {
            for (float local849_9 = -1.0F; local849_9 <= 1.0F; local849_9++) {
               if ((local849_9 != 0.0F || local849_8 != 0.0F)
                  && (float)Math.abs(local849_3 - MathHelper.wrapDegrees((float)Math.toDegrees(method0656(local849_1, local849_8, local849_9)))) < local849_7) {
                  local849_7 = (float)Math.abs(local849_3 - MathHelper.wrapDegrees((float)Math.toDegrees(method0656(local849_1, local849_8, local849_9))));
                  local849_5 = local849_8;
                  local849_6 = local849_9;
               }
            }
         }

         movementInputEvent.method4446(local849_5);
         movementInputEvent.method4449(local849_6);
      }
   }

   public void method0660(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, RotationManager.ReturnMode newReturnMode) {
      this.method0665(newRotationAngles, newRotationSpeed, 0L, newReturnMode);
   }

   public float method0661() {
      return this.field00949;
   }

   public float method0662() {
      return this.field00945;
   }

   private boolean method0663(RotationAngles newRotationAngles, float local853_2, float local853_3) {
      float local853_4 = MathHelper.wrapDegrees(newRotationAngles.method4362() - mc.player.getYaw());
      float local853_5 = newRotationAngles.method4365() - mc.player.getPitch();
      float local853_6 = Math.min(Math.abs(local853_4), local853_2);
      float local853_7 = Math.min(Math.abs(local853_5), local853_3);
      mc.player
         .setYaw(mc.player.headYaw = mc.player.headYaw + MouseSensitivity.method4410(MathHelper.clamp(local853_4, -local853_6, local853_6)));
      mc.player
         .setPitch(
            MathHelper.clamp(mc.player.getPitch() + MouseSensitivity.method4410(MathHelper.clamp(local853_5, -local853_7, local853_7)), -90.0F, 90.0F)
         );
      return Math.abs(local853_4) <= local853_2 && Math.abs(local853_5) <= local853_3;
   }

   public void method0665(RotationAngles newRotationAngles, RotationSpeed newRotationSpeed, long local855_3, RotationManager.ReturnMode newReturnMode) {
      RotationAngles currentRotationAngles = EntityUtils.method2815(newRotationAngles);
      if (currentRotationAngles != newRotationAngles) {
         newRotationAngles = currentRotationAngles;
         newRotationSpeed = new RotationSpeed(
            Math.max(newRotationSpeed.method2283(), 15.0F), Math.max(newRotationSpeed.method2285(), 15.0F), newRotationSpeed.method2282(), newRotationSpeed.method2284()
         );
      }

      this.returnMode = newReturnMode;
      this.rotationAngles = newRotationAngles;
      this.rotationSpeed = newRotationSpeed;
      this.field00956 = this.field00955;
      this.field00957 = local855_3;
      this.field00958 = -1L;
      this.stopwatch.reset();
   }

   public float method0666() {
      return this.field00946;
   }

   public boolean method0667() {
      return this.field00954;
   }

   public enum ReturnMode {
      Empty,
      Smooth,
      SmoothFast;
   }
}
