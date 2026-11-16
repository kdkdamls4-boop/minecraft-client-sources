package net.skycore.client.module.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.util.ResetSpeedHelper;
import net.skycore.client.setting.BooleanSetting;

public final class TimerModule extends ClientModule {
   private float lastPitch;
   private final BooleanSetting smart;
   private static final float SMART_THRESHOLD = 10.0F;
   private static float charge = 0.0F;
   private float appliedSpeed;
   private double lastX;
   private double lastY;
   private static final float CHARGE_MAX = 100.0F;
   private final NumberSetting speed = this.add(new NumberSetting("Скорость", "", 1.8, 1.0, 5.0, 0.1, ""));
   public static final TimerModule INSTANCE = new TimerModule();
   private double lastZ;
   private float lastYaw;
   private final NumberSetting decaySpeed;

   @Override
   protected void onDisable() {
      this.appliedSpeed = 1.0F;
      ResetSpeedHelper.resetSpeed();
   }

   public float getCharge() {
      return charge;
   }

   private void tickSpeed() {
      if (this.smart.get()) {
         float speed2;
         if (charge > 10.0F) {
            speed2 = this.speed.get().floatValue();
         } else {
            speed2 = 1.0F;
         }

         this.appliedSpeed = speed2;
      } else {
         Double value = this.speed.get();
         float speed3 = value.floatValue();
         this.appliedSpeed = speed3;
      }

      if (this.isEnabled()) {
         ResetSpeedHelper.setSpeed(this.appliedSpeed);
      }

      if (charge < 10.0F) {
         if (this.smart.get()) {
            if (this.isEnabled()) {
               this.setEnabled(false);
            }
         }
      }
   }

   private boolean sameAsLast(double value, double value2, double value3, float yaw, float pitch) {
      if (value == this.lastX && value2 == this.lastY && value3 == this.lastZ) {
         if (yaw == this.lastYaw && pitch == this.lastPitch) {
            return true;
         }
      }

      return false;
   }

   private TimerModule() {
      super("Timer", "Client tick timer", ModuleCategory.MOVEMENT);
      this.smart = this.add(new BooleanSetting("Умный", true));
      this.decaySpeed = this.add(new NumberSetting("Скорость убывания", "", 1.0, 0.5, 3.0, 0.1, ""));
      this.appliedSpeed = 1.0F;
      this.decaySpeed.visibleWhen(this.smart::get);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onPreTick);
   }

   private void onPreTick(PlayerTickEndEvent event) {
      LocalPlayer player = event.player();
      if (player != null) {
         float yRot = player.getYRot();
         float xRot = player.getXRot();
         double x = player.getX();
         double y = player.getY();
         double z = player.getZ();
         this.updateCharge(yRot, xRot, x, y, z, false);
         this.tickSpeed();
      }
   }

   public float getAppliedSpeed() {
      return this.appliedSpeed;
   }

   public void updateCharge(float lastYaw2, float lastPitch2, double lastX2, double lastY2, double lastZ2, boolean condition) {
      if (this.sameAsLast(lastX2, lastY2, lastZ2, lastYaw2, lastPitch2) || condition) {
         charge++;
         charge++;
      } else if (this.isEnabled()) {
         charge = charge - (float)(this.speed.get() * 2.0 * this.decaySpeed.get());
      }

      charge = (float)Mth.clamp(charge, 0.0, 100.0);
      this.lastX = lastX2;
      this.lastY = lastY2;
      this.lastZ = lastZ2;
      this.lastYaw = lastYaw2;
      this.lastPitch = lastPitch2;
   }
}
