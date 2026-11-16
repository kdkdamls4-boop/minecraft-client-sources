package net.skycore.client.combat;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.util.QuantizeHelper;

public final class AuraRotation {
   private static int holyWorldAimIndex;
   private static float holyWorldHeldLateral;
   private float funtimeOffsetYaw;
   private static float holyWorldCamYaw;
   private float funtimeJitterCurYaw;
   private static float holyWorldHeldPitchSmooth;
   private static final float[] HOLY_WORLD_RECENT_YAW = new float[8];
   private float lastPitch;
   private static float holyWorldLastLateral = Float.NaN;
   private float funtimeOffsetPitch;
   private static float holyWorldCamPitch;
   private float lonyLastPitch;
   private boolean holyBack;
   private static final float[] HOLY_WORLD_RECENT_PITCH = new float[8];
   private float lonySmooth;
   private static int holyWorldRecentYawCount;
   private int funtimeJitterTick;
   private static float holyWorldHeldYawSmooth;
   private static boolean holyWorldCamValid;
   private float lonyLastYaw;
   private float funtimeJitterPitch;
   private LivingEntity lonyLastTarget;
   private boolean funtimeOffsetValid;
   private LivingEntity lastTarget;
   private int funtimeTicksSinceHit;
   private boolean lonyReverseSmooth;
   private float funtimeJitterYaw;
   private float holySpeedAcceleration;
   private float lerpPitch;
   private static final float[] HOLY_WORLD_LATERALS = new float[512];
   private float funtimeJitterCurPitch;
   private static final SecureRandom HOLY_WORLD_RNG = new SecureRandom();
   private static final SecureRandom RANDOM = new SecureRandom();
   private float lerpYaw;
   private boolean lerpReady;
   private static boolean holyWorldHoldingSnap;
   private float lastYaw;
   private static int holyWorldRecentPitchCount;

   private void updateHolyWorldRotation(LocalPlayer player, LivingEntity entity, boolean condition) {
      long now = System.currentTimeMillis();
      double randomValue = Math.random();
      double cosine = Math.sin(now / 1000.0) * Math.cos(now / 450.0);
      Vec3 deltaMovement = player.getDeltaMovement();
      int moveVector = player.input == null || player.input.getMoveVector().x == 0.0F && player.input.getMoveVector().y == 0.0F ? 0 : 1;
      Vec3 center = entity.getBoundingBox().getCenter();
      if (moveVector != 0) {
         double computedValue = 0.045 + randomValue * 0.055;
         center = center.add(deltaMovement.x * (0.3 + cosine * 0.2) + (randomValue - 0.5) * computedValue, (randomValue - 0.7) * 0.04, deltaMovement.z * (0.35 + cosine * 0.2) + (randomValue - 0.6) * computedValue);
      }

      if (entity.isFallFlying()) {
         center = center.add(entity.getDeltaMovement().scale(0.5 + cosine * 0.05));
      }

      Vec3 eyePosition = center.subtract(player.getEyePosition(1.0F));
      float yaw = yawTo(eyePosition);
      float pitch = pitchTo(eyePosition);
      int attackStrengthScale = player.getAttackStrengthScale(1.0F) > 0.93F ? 1 : 0;
      float computedValue2 = Math.abs(Mth.wrapDegrees(yaw - this.lastYaw));
      if (!this.holyBack) {
         float computedValue3 = 0.0065F + (float)randomValue * 0.0075F;
         if (computedValue2 > 30.0F) {
            computedValue3 += 0.015F;
         }

         if (computedValue2 > 10.0F && attackStrengthScale != 0) {
            computedValue3 += 0.022F;
         }

         if (moveVector != 0) {
            computedValue3 *= 0.95F + (float)randomValue * 0.18F;
         }

         if (attackStrengthScale != 0) {
            computedValue3 += 0.025F * (float)randomValue;
         }

         this.holySpeedAcceleration += computedValue3 * (0.96F + (float)cosine * 0.14F);
         float speed = moveVector != 0 ? 0.26F + (float)randomValue * 0.05F : 0.29F + (float)randomValue * 0.04F;
         if (this.holySpeedAcceleration >= speed) {
            this.holyBack = true;
         }
      } else {
         this.holySpeedAcceleration -= (attackStrengthScale != 0 ? 0.028F : 0.014F) * (1.0F + (float)randomValue * 0.5F);
         if (this.holySpeedAcceleration <= -0.042F) {
            this.holyBack = false;
         }
      }

      float speed2 = player.isFallFlying() ? 0.62F : (moveVector != 0 ? 0.45F : 0.35F);
      float clampedValue = Mth.clamp(this.holySpeedAcceleration, 0.032F, speed2);
      if (attackStrengthScale != 0 && randomValue > 0.75) {
         clampedValue = Math.min(clampedValue + (float)(randomValue * 0.08), speed2 + 0.06F);
      }

      float yaw2 = (player.isFallFlying() ? 85.0F : (attackStrengthScale != 0 ? 48.0F : 32.0F)) + (float)randomValue * 12.0F;
      float pitch2 = (player.isFallFlying() ? 45.0F : (attackStrengthScale != 0 ? 28.0F : 20.0F)) + (float)randomValue * 10.0F;
      if (moveVector != 0 && attackStrengthScale == 0) {
         yaw2 *= 0.85F;
         pitch2 *= 0.85F;
      }

      float computedValue4 = Mth.clamp(Mth.wrapDegrees(yaw - this.lastYaw), -yaw2, yaw2);
      float clampedValue2 = Mth.clamp(pitch - this.lastPitch, -pitch2, pitch2);
      float yaw3 = this.lastYaw + computedValue4 * clampedValue;
      float pitch3 = Mth.clamp(
         this.lastPitch + clampedValue2 * clampedValue * (attackStrengthScale != 0 ? 0.92F : 0.82F) * (float)(0.88 + randomValue * 0.28) * (0.7F + (float)randomValue * 0.15F), -89.2F, 89.2F
      );
      float gCDValue = MouseSensitivity.getGCDValue();
      if (gCDValue > 0.0F) {
         yaw3 = this.lastYaw + Math.round((yaw3 - this.lastYaw) / gCDValue) * gCDValue;
         pitch3 = this.lastPitch + Math.round((pitch3 - this.lastPitch) / gCDValue) * gCDValue;
      }

      float angle = entity.isFallFlying() ? 360.0F : (moveVector != 0 ? 64.0F + (float)randomValue * 28.0F : 75.0F + (float)randomValue * 35.0F);
      RotationManager.update(new Rotation(yaw3, pitch3), angle, angle, angle, angle, 0, 1, condition);
      this.lastYaw = yaw3;
      this.lastPitch = pitch3;
      this.lerpYaw = yaw3;
      this.lerpPitch = pitch3;
   }

   public void applyFuntimeSnapBeforeAttack(AttackAuraModule attackAuraModule, LocalPlayer player, LivingEntity entity, boolean condition) {
      holyWorldHoldingSnap = false;
      this.applyFuntimeRotation(player, entity, true, condition);
   }

   public void pinCameraForFuntime(LocalPlayer player) {
      this.pinHolyWorldCamera(player);
   }

   private void pinHolyWorldCamera(LocalPlayer player) {
      this.lockHolyWorldCamera(player);
      FreeLookController.setFreeYaw(holyWorldCamYaw);
      FreeLookController.setFreePitch(holyWorldCamPitch);
      FreeLookController.setActive(true);
   }

   private static void rebuildHolyWorldAimOrder() {
      float[] floatValues = new float[512];

      for (int value = 0; value < floatValues.length; value++) {
         float randomValue = HOLY_WORLD_RNG.nextFloat();
         if (randomValue < 0.3F) {
            floatValues[value] = 0.03F + HOLY_WORLD_RNG.nextFloat() * 0.13F;
         } else if (randomValue < 0.45F) {
            floatValues[value] = 0.2F + HOLY_WORLD_RNG.nextFloat() * 0.12F;
         } else if (randomValue < 0.55F) {
            floatValues[value] = 0.44F + HOLY_WORLD_RNG.nextFloat() * 0.12F;
         } else if (randomValue < 0.7F) {
            floatValues[value] = 0.68F + HOLY_WORLD_RNG.nextFloat() * 0.12F;
         } else {
            floatValues[value] = 0.84F + HOLY_WORLD_RNG.nextFloat() * 0.13F;
         }
      }

      for (int index = floatValues.length - 1; index > 0; index--) {
         int computedValue = HOLY_WORLD_RNG.nextInt(index + 1);
         float value2 = floatValues[index];
         floatValues[index] = floatValues[computedValue];
         floatValues[computedValue] = value2;
      }

      System.arraycopy(floatValues, 0, HOLY_WORLD_LATERALS, 0, floatValues.length);
      holyWorldAimIndex = 0;
   }

   private void lockHolyWorldCamera(LocalPlayer player) {
      if (FreeLookController.isActive()) {
         float freeYaw = FreeLookController.getFreeYaw();
         holyWorldCamYaw = freeYaw;
         freeYaw = FreeLookController.getFreePitch();
         holyWorldCamPitch = freeYaw;
      } else {
         float yRot = player.getYRot();
         holyWorldCamYaw = yRot;
         holyWorldCamPitch = player.getXRot();
      }

      holyWorldCamValid = true;
      FreeLookController.setFreeYaw(holyWorldCamYaw);
      FreeLookController.setFreePitch(holyWorldCamPitch);
      FreeLookController.setActive(true);
   }

   private float lonyMouseGcd() {
      Minecraft instance = Minecraft.getInstance();
      OptionInstance optionInstance = instance.options.sensitivity();
      Double value = (Double)optionInstance.get();
      double value2 = value;
      double computedValue = value2 * 0.6 + 0.2;
      return (float)(Math.pow(computedValue, 3.0) * 0.8) * 0.15F;
   }

   private Vec2 nextFuntimeJitter(float yaw, float pitch) {
      int elapsed = this.funtimeJitterTick + 1;
      this.funtimeJitterTick = elapsed;
      if (elapsed >= 2) {
         this.funtimeJitterTick = 0;
         ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
         this.funtimeJitterYaw = threadLocalRandom.nextFloat(-20.0F, 20.0F);
         float randomValue = threadLocalRandom.nextFloat(-10.0F, 10.0F);
         this.funtimeJitterPitch = randomValue;
      }

      this.funtimeJitterCurYaw = Mth.lerp(0.6F, this.funtimeJitterCurYaw, this.funtimeJitterYaw);
      this.funtimeJitterCurPitch = Mth.lerp(0.6F, this.funtimeJitterCurPitch, this.funtimeJitterPitch);
      float computedValue = yaw + this.funtimeJitterCurYaw;
      float clampedValue = Mth.clamp(pitch + this.funtimeJitterCurPitch, -90.0F, 90.0F);
      return new Vec2(computedValue, clampedValue);
   }

   private void applySkycoreRotation(LocalPlayer player, Vec3 position, boolean condition) {
      Vec3 eyePosition = position.subtract(player.getEyePosition()).normalize();
      float x = (float)Math.toDegrees(Math.atan2(-eyePosition.x, eyePosition.z));
      float y = -((float)Math.toDegrees(Math.asin(eyePosition.y)));
      float value = this.serverYaw(player);
      float value2 = this.serverPitch(player);
      float angle = value + (float)Math.max(Math.min(Mth.wrapDegrees(x - value), 999.0), -999.0);
      float angle2 = Mth.clamp(value2 + (float)Math.max(Math.min(y - value2, 999.0), -999.0), -90.0F, 90.0F);
      Minecraft instance = Minecraft.getInstance();
      int cameraType = !instance.options.smoothCamera && instance.options.getCameraType().isFirstPerson() && player.isScoping() ? 1 : 0;
      double value3 = instance.options.sensitivity().get();
      double computedValue = value3 * 0.6F + 0.2F;
      double computedValue2 = computedValue * computedValue * computedValue * (cameraType != 0 ? 1.0 : 8.0);
      angle = QuantizeHelper.quantize(angle, computedValue2);
      angle2 = QuantizeHelper.quantize(angle2, computedValue2);
      RotationManager.updateExact(new Rotation(angle, angle2), 10, 2);
      if (condition) {
         player.setYRot(angle);
         player.setXRot(angle2);
      }
   }

   private void ensureHolyWorldSnapTarget() {
      if (!holyWorldHoldingSnap) {
         float value = nextHolyWorldLateral();
         holyWorldHeldLateral = value;
         holyWorldHeldYawSmooth = nextHolyWorldUniqueSpeed(72.0F, 148.0F, true);
         float pitch = nextHolyWorldUniqueSpeed(64.0F, 132.0F, false);
         holyWorldHeldPitchSmooth = pitch;
         if (Math.abs(holyWorldHeldYawSmooth - holyWorldHeldPitchSmooth) < 7.0F) {
            holyWorldHeldPitchSmooth = nextHolyWorldUniqueSpeed(64.0F, 132.0F, false);
         }

         holyWorldHoldingSnap = true;
      }
   }

   private void resetFuntimeState() {
      this.funtimeOffsetValid = false;
      this.funtimeTicksSinceHit = 0;
      this.resetFuntimeJitter();
   }

   private Vec3 getHolyWorldAimPoint(LocalPlayer player, LivingEntity entity, float value) {
      AABB boundingBox = entity.getBoundingBox();
      Vec3 eyePosition = player.getEyePosition();
      double computedValue = (boundingBox.minX + boundingBox.maxX) * 0.5;
      double computedValue2 = (boundingBox.minZ + boundingBox.maxZ) * 0.5;
      double x = computedValue - eyePosition.x;
      double z = computedValue2 - eyePosition.z;
      double computedValue3 = Math.hypot(x, z);
      if (computedValue3 < 1.0E-4) {
         return new Vec3(computedValue, (boundingBox.minY + boundingBox.maxY) * 0.5, computedValue2);
      }

      double computedValue4 = -z / computedValue3;
      double computedValue5 = x / computedValue3;
      double xsize = boundingBox.getXsize() * 0.5 - 0.012;
      double zsize = boundingBox.getZsize() * 0.5 - 0.012;
      double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;
      if (Math.abs(computedValue4) > 1.0E-6) {
         pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, xsize / Math.abs(computedValue4));
      }

      if (Math.abs(computedValue5) > 1.0E-6) {
         pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, zsize / Math.abs(computedValue5));
      }

      double computedValue6 = (value - 0.5) * 2.0 * pOSITIVE_INFINITY;
      return new Vec3(computedValue + computedValue4 * computedValue6, (boundingBox.minY + boundingBox.maxY) * 0.5, computedValue2 + computedValue5 * computedValue6);
   }

   public void tick(AttackAuraModule attackAuraModule, LocalPlayer player, LivingEntity lastTarget2, String text, String text2, boolean condition, boolean condition2, boolean condition3, double angle) {
      if (player != null && lastTarget2 != null && lastTarget2.isAlive()) {
         if (this.lastTarget != lastTarget2) {
            this.seed(player);
            this.lastTarget = lastTarget2;
         }

         if (ElytraTargetModule.INSTANCE.isEnabled() && player.isFallFlying()) {
            this.applySkycoreRotation(player, ElytraTargetModule.INSTANCE.predictDonor(lastTarget2), condition3);
            this.lastYaw = this.serverYaw(player);
            this.lastPitch = this.serverPitch(player);
         } else {
            boolean condition4 = "Snaps".equals(text2);
            if ("Holy World".equals(text)
               || "HolyWorldLegit".equals(text)
               || "Funtime".equals(text)
               || !condition4
               || !(player.getAttackStrengthScale(1.0F) < 0.95F)
               || condition2) {
               switch (text) {
                  case "HvH":
                     this.sunwayRotation(player, lastTarget2);
                     break;
                  case "HolyWorldLegit":
                     this.updateHolyWorldLegitRotation(player, lastTarget2);
                     break;
                  case "Funtime":
                     this.updateFuntimeRotation(attackAuraModule, player, lastTarget2, condition4, condition2, condition3);
                     break;
                  default:
                     this.updateReallyWorldRotation(player, lastTarget2, condition, angle, attackAuraModule);
               }

               this.lastYaw = this.serverYaw(player);
               this.lastPitch = this.serverPitch(player);
            }
         }
      }
   }

   private void updateFuntimeRotation(AttackAuraModule attackAuraModule, LocalPlayer player, LivingEntity entity, boolean condition, boolean condition2, boolean condition3) {
      int elapsed = -1;
      int intermediate = -1;
      this.lockHolyWorldCamera(player);
      intermediate = (byte)-1;
      elapsed = this.funtimeTicksSinceHit + 1;
      intermediate = (byte)-1;
      this.funtimeTicksSinceHit = elapsed;
      intermediate = !condition && condition2 ? 1 : 0;
      this.applyFuntimeRotation(player, entity, (intermediate != 0), condition3);
   }

   public void reset() {
      this.lerpReady = false;
      this.resetFuntimeState();
      this.lonyLastTarget = null;
      this.lastTarget = null;
      this.holySpeedAcceleration = 0.0F;
      this.holyBack = false;
      this.lonyReverseSmooth = false;
      this.lonySmooth = 0.0F;
      holyWorldHoldingSnap = false;
      holyWorldCamValid = false;
      RotationManager.INSTANCE.stopRotation();
   }

   private float serverYaw(LocalPlayer player) {
      return RotationController.isActive() ? RotationController.getServerYaw() : player.getYRot();
   }

   private float wrapLerp(float value, float value2, float value3) {
      float intermediate = value3 - value2;
      return value2 + value * Mth.wrapDegrees(intermediate);
   }

   static {
      rebuildHolyWorldAimOrder();
   }

   private void resetLony(float lonyLastYaw2, float lonyLastPitch2) {
      this.lonyReverseSmooth = false;
      this.lonySmooth = 0.0F;
      this.lonyLastYaw = lonyLastYaw2;
      this.lonyLastPitch = lonyLastPitch2;
   }

   private void resetFuntimeJitter() {
      this.funtimeJitterTick = 0;
      this.funtimeJitterYaw = 0.0F;
      this.funtimeJitterPitch = 0.0F;
      this.funtimeJitterCurYaw = 0.0F;
      this.funtimeJitterCurPitch = 0.0F;
   }

   private static float nextHolyWorldLateral() {
      if (holyWorldAimIndex >= HOLY_WORLD_LATERALS.length) {
         rebuildHolyWorldAimOrder();
      }

      float result = HOLY_WORLD_LATERALS[holyWorldAimIndex++];
      int value = 0;

      while (true) {
         if (Float.isNaN(holyWorldLastLateral)) {
            break;
         }

         float computedValue = result - holyWorldLastLateral;
         if (!(Math.abs(computedValue) < 0.38F) || value++ >= 48) {
            break;
         }

         if (holyWorldAimIndex >= HOLY_WORLD_LATERALS.length) {
            rebuildHolyWorldAimOrder();
         }

         result = HOLY_WORLD_LATERALS[holyWorldAimIndex++];
      }

      holyWorldLastLateral = result;
      return result;
   }

   private float randomLerp(float value, float value2) {
      float randomValue = RANDOM.nextFloat();
      return Mth.lerp(randomValue, value, value2);
   }

   private void updateHolyWorldLegitRotation(LocalPlayer player, LivingEntity entity) {
      if (RotationController.isActive() && !RotationController.isClientLook()) {
         player.setYRot(FreeLookController.getFreeYaw());
         player.setXRot(FreeLookController.getFreePitch());
         RotationController.deactivateKeepingAim();
      }

      Vec3 closestVec = CriticalHelper.getClosestVec(entity);
      float value = yawTo(closestVec);
      float value2 = pitchTo(closestVec);
      float yRot = player.getYRot();
      float xRot = player.getXRot();
      float computedValue = Math.abs(Mth.wrapDegrees(value - yRot));
      float computedValue2 = Math.abs(value2 - xRot);
      float computedValue3 = (float)Math.hypot(computedValue, computedValue2);
      AABB boundingBox = entity.getBoundingBox();
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 strictDistance = eyePosition.add(Vec3.directionFromRotation(xRot, yRot).scale(Math.max(CriticalHelper.getStrictDistance(entity) + 1.0, 6.0)));
      boolean present = boundingBox.inflate(0.08).clip(eyePosition, strictDistance).isPresent();
      float angle;
      float angle2;
      if (present || computedValue3 < 2.5F) {
         angle = 2.2F + RANDOM.nextFloat() * 2.4F;
         angle2 = 1.6F + RANDOM.nextFloat() * 1.8F;
      } else if (computedValue3 < 18.0F) {
         angle = 7.5F + RANDOM.nextFloat() * 5.5F;
         angle2 = 5.0F + RANDOM.nextFloat() * 4.0F;
      } else {
         angle = 14.0F + RANDOM.nextFloat() * 9.0F;
         angle2 = 9.0F + RANDOM.nextFloat() * 7.0F;
      }

      float computedValue4 = (RANDOM.nextFloat() - 0.5F) * 0.4F;
      float computedValue5 = (RANDOM.nextFloat() - 0.5F) * 0.28F;
      float yaw = value + computedValue4;
      float clampedValue = Mth.clamp(value2 + computedValue5, -90.0F, 90.0F);
      float gCDValue = MouseSensitivity.getGCDValue();
      if (gCDValue > 0.0F) {
         yaw = yRot + Math.round((yaw - yRot) / gCDValue) * gCDValue;
         clampedValue = xRot + Math.round((clampedValue - xRot) / gCDValue) * gCDValue;
         clampedValue = Mth.clamp(clampedValue, -90.0F, 90.0F);
      }

      this.lerpYaw = yaw;
      this.lerpPitch = clampedValue;
      RotationManager.update(new Rotation(yaw, clampedValue), angle, angle2, angle, angle2, 0, 1, true);
   }

   private static float yawTo(Vec3 position) {
      double x = Math.toDegrees(Math.atan2(position.z, position.x)) - 90.0;
      return (float)Mth.wrapDegrees(x);
   }

   private void updateLonyGriefRotation(LocalPlayer player, LivingEntity lonyLastTarget2) {
      if (this.lonyLastTarget != lonyLastTarget2) {
         this.resetLony(this.serverYaw(player), this.serverPitch(player));
         this.lonyLastTarget = lonyLastTarget2;
      }

      AABB boundingBox = lonyLastTarget2.getBoundingBox();
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 center = boundingBox.getCenter().subtract(eyePosition);
      float yaw = yawTo(center);
      float pitch = pitchTo(center);
      float angle = this.serverYaw(player);
      float angle2 = this.serverPitch(player);
      Vec3 position = eyePosition.add(Vec3.directionFromRotation(angle2, angle).scale(999.0));
      boolean present = boundingBox.inflate(-0.5).clip(eyePosition, position).isPresent();
      if (this.lonyReverseSmooth) {
         if (this.lonySmooth >= -0.01F) {
            this.lonySmooth = this.lonySmooth - (Math.abs(Mth.wrapDegrees(yaw - this.lonyLastYaw)) > 80.0F ? 0.1F : 0.01F);
         }

         if (this.lonySmooth <= -0.01F) {
            this.lonyReverseSmooth = false;
         }
      } else {
         this.lonySmooth += 0.005F;
         if (this.lonySmooth >= 0.22F || present) {
            this.lonyReverseSmooth = true;
         }
      }

      float maximum = Math.max(this.lonySmooth, 0.0F);
      float clampedValue = this.lonyLastYaw + Mth.wrapDegrees(yaw - this.lonyLastYaw) * Mth.clamp(maximum * 10.3F, 0.0F, 1.0F);
      float clampedValue2 = this.lonyLastPitch + (pitch - this.lonyLastPitch) * Mth.clamp(maximum / 10.7F, 0.0F, 1.0F);
      float yaw2 = this.lonyMouseGcd();
      if (yaw2 != 0.0F) {
         clampedValue -= (clampedValue - this.lonyLastYaw) % yaw2;
         clampedValue2 -= (clampedValue2 - this.lonyLastPitch) % yaw2;
      }

      clampedValue2 = Mth.clamp(clampedValue2, -89.0F, 89.0F);
      this.lonyLastYaw = clampedValue;
      this.lonyLastPitch = clampedValue2;
      this.lerpYaw = clampedValue;
      this.lerpPitch = clampedValue2;
      RotationManager.update(new Rotation(clampedValue, clampedValue2), 360.0F, 360.0F, 0.0F, 5.0F, 1, 5, false);
   }

   private void reallyWorldFastRotation(LocalPlayer player) {
      float yaw;
      if (RotationController.isClientLook()) {
         yaw = player.getYRot();
      } else {
         yaw = this.serverYaw(player);
      }

      float yaw2 = yaw;
      float pitch;
      if (RotationController.isClientLook()) {
         pitch = player.getXRot();
      } else {
         pitch = this.serverPitch(player);
      }

      float pitch2 = pitch;
      float computedValue = Mth.wrapDegrees(this.lerpYaw - yaw2);
      float computedValue2 = Mth.wrapDegrees(this.lerpPitch - pitch2);
      float angle = yaw2 + computedValue;
      float angle2 = pitch2 + computedValue2;
      RotationManager.update(new Rotation(angle, angle2), 2500.0F, 2500.0F, 2500.0F, 2500.0F, 2, 5, RotationController.isClientLook());
   }

   private void applyFuntimeRotation(LocalPlayer player, LivingEntity entity, boolean condition, boolean condition2) {
      if (!player.isFallFlying()) {
         this.pinHolyWorldCamera(player);
         float holyWorldCamYaw2 = holyWorldCamYaw;
         float holyWorldCamPitch2 = holyWorldCamPitch;
         Vec2 vec2 = this.computeFuntimeTargetAngle(player, entity);
         if (vec2 != null) {
            float yaw;
            float pitch;
            if (condition) {
               this.storeFuntimeOffset(vec2.x, vec2.y);
               yaw = vec2.x;
               pitch = vec2.y;
               this.funtimeTicksSinceHit = 0;
            } else {
               float elapsed = this.funtimeOffsetValid && this.funtimeTicksSinceHit < 9 ? this.funtimeOffsetYaw : this.serverYaw(player);
               float elapsed2 = this.funtimeOffsetValid && this.funtimeTicksSinceHit < 9 ? this.funtimeOffsetPitch : this.serverPitch(player);
               Vec2 vec22 = this.nextFuntimeJitter(elapsed, elapsed2);
               yaw = vec22.x;
               pitch = vec22.y;
            }

            RotationManager.update(new Rotation(yaw, pitch), 360.0F, 360.0F, 360.0F, 360.0F, condition ? 1 : 2, 6, condition2);
            FreeLookController.setFreeYaw(holyWorldCamYaw2);
            FreeLookController.setFreePitch(holyWorldCamPitch2);
            FreeLookController.setActive(true);
            this.lastYaw = yaw;
            this.lastPitch = pitch;
         }
      }
   }

   private void seed(LocalPlayer player) {
      float yaw = this.serverYaw(player);
      float pitch = this.serverPitch(player);
      this.lerpYaw = yaw;
      this.lerpPitch = pitch;
      this.lerpReady = true;
      this.resetFuntimeState();
      this.resetLony(yaw, pitch);
      this.lonyLastTarget = null;
      this.holySpeedAcceleration = 0.0F;
      this.holyBack = false;
      this.lastYaw = yaw;
      this.lastPitch = pitch;
   }

   private static float nextHolyWorldUniqueSpeed(float value, float value2, boolean condition) {
      float[] floatValues = condition ? HOLY_WORLD_RECENT_YAW : HOLY_WORLD_RECENT_PITCH;
      int index = condition ? holyWorldRecentYawCount : holyWorldRecentPitchCount;
      float yaw = value;

      for (int value3 = 0; value3 < 64; value3++) {
         yaw = value + HOLY_WORLD_RNG.nextFloat() * (value2 - value);
         byte flag = 1;
         int minimum = Math.min(index, floatValues.length);

         for (int value4 = 0; value4 < minimum; value4++) {
            if (Math.abs(yaw - floatValues[value4]) < 7.0F) {
               flag = 0;
               break;
            }
         }

         if (flag != 0) {
            break;
         }
      }

      if (condition) {
         HOLY_WORLD_RECENT_YAW[holyWorldRecentYawCount % HOLY_WORLD_RECENT_YAW.length] = yaw;
         holyWorldRecentYawCount++;
      } else {
         HOLY_WORLD_RECENT_PITCH[holyWorldRecentPitchCount % HOLY_WORLD_RECENT_PITCH.length] = yaw;
         holyWorldRecentPitchCount++;
      }

      return yaw;
   }

   private float serverPitch(LocalPlayer player) {
      float pitch;
      if (RotationController.isActive()) {
         pitch = RotationController.getServerPitch();
      } else {
         pitch = player.getXRot();
      }

      return pitch;
   }

   public void onAttack() {
      holyWorldHoldingSnap = false;
      this.funtimeTicksSinceHit = 0;
   }

   private void storeFuntimeOffset(float funtimeOffsetYaw2, float value) {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      float randomValue = (threadLocalRandom.nextBoolean() ? 1.0F : -1.0F) * threadLocalRandom.nextFloat(18.0F, 24.0F);
      float randomValue2 = threadLocalRandom.nextFloat(12.0F, 16.0F);
      int computedValue = value + randomValue2 <= 90.0F ? 1 : 0;
      int computedValue2 = value - randomValue2 >= -90.0F ? 1 : 0;
      float computedValue3 = computedValue != 0 && computedValue2 != 0 ? (threadLocalRandom.nextBoolean() ? 1.0F : -1.0F) : (computedValue2 != 0 ? -1.0F : 1.0F);
      this.funtimeOffsetYaw = funtimeOffsetYaw2 + randomValue;
      this.funtimeOffsetPitch = Mth.clamp(value + computedValue3 * randomValue2, -90.0F, 90.0F);
      this.funtimeOffsetValid = true;
      this.resetFuntimeJitter();
   }

   private void updateReallyWorldRotation(LocalPlayer player, LivingEntity entity, boolean condition, double value, AttackAuraModule attackAuraModule) {
      double maximum = CriticalHelper.getStrictDistance(entity) / Math.max(value, 0.1);
      Vec3 eyePosition = player.getEyePosition(1.0F);
      Vec3 y = entity.position().add(0.0, Mth.clamp(eyePosition.y - entity.getY(), 0.0, maximum), 0.0).subtract(eyePosition).normalize();
      float x = (float)Math.toDegrees(Math.atan2(-y.x, y.z));
      float x2 = (float)Mth.clamp(-Math.toDegrees(Math.atan2(y.y, Math.hypot(y.x, y.z))), -90.0, 90.0);
      float computedValue = (float)Math.cos(System.currentTimeMillis() / 70.0);
      float computedValue2 = (float)Math.sin(System.currentTimeMillis() / 115.0);
      float computedValue3 = (float)Math.ceil(this.randomLerp(1.0F, 3.0F) * computedValue);
      float computedValue4 = (float)Math.ceil(this.randomLerp(1.0F, 2.0F) * computedValue2);
      if (!this.lerpReady) {
         this.lerpYaw = this.serverYaw(player);
         this.lerpPitch = this.serverPitch(player);
         this.lerpReady = true;
      }

      this.lerpYaw = this.wrapLerp(1.0F, this.lerpYaw, x + computedValue3);
      this.lerpPitch = this.wrapLerp(1.0F, this.lerpPitch, Mth.clamp(x2 + computedValue4, -90.0F, 90.0F));
      if (condition && !CriticalHelper.canSeeEntity(entity)) {
         Vec3 closestVec = CriticalHelper.getClosestVec(entity);
         float yaw = yawTo(closestVec);
         float pitch = pitchTo(closestVec);
         this.lerpYaw = yaw;
         this.lerpPitch = pitch;
         this.reallyWorldFastRotation(player);
      } else {
         this.reallyWorldFastRotation(player);
      }
   }

   private Vec2 computeFuntimeTargetAngle(LocalPlayer player, LivingEntity entity) {
      Vec3 eyePosition = player.getEyePosition();
      Vec3 closestPoint = CriticalHelper.getClosestPoint(eyePosition, entity.getBoundingBox());
      Vec3 position = closestPoint.subtract(eyePosition);
      double x = Math.hypot(position.x, position.z);
      if (x < 1.0E-4) {
         return null;
      }

      double angleRadians = Math.atan2(position.z, position.x);
      double computedValue = Math.toDegrees(angleRadians) - 90.0;
      float computedValue2 = (float)Mth.wrapDegrees(computedValue);
      double angleRadians2 = Math.atan2(position.y, x);
      float computedValue3 = Mth.clamp((float)(-Math.toDegrees(angleRadians2)), -90.0F, 90.0F);
      return new Vec2(computedValue2, computedValue3);
   }

   private static float pitchTo(Vec3 position) {
      double x = Math.atan2(position.y, Math.hypot(position.x, position.z));
      return Mth.clamp((float)(-Math.toDegrees(x)), -90.0F, 90.0F);
   }

   private void sunwayRotation(LocalPlayer player, LivingEntity entity) {
      Vec3 closestVec = CriticalHelper.getClosestVec(entity);
      float value = yawTo(closestVec);
      float value2 = pitchTo(closestVec);
      float value3 = (float)player.fallDistance;
      float computedValue = (RANDOM.nextFloat() - 0.5F) * (2.0F + value3 * 0.5F);
      float computedValue2 = (RANDOM.nextFloat() - 0.5F) * (1.5F + value3 * 0.3F);
      float angle = 4.3543536E13F + RANDOM.nextFloat() * 4.1243535E13F + value3 * 1.3453453E9F;
      float angle2 = 4.1345345E14F + RANDOM.nextFloat() * 1.1345346E10F + value3 * 1.2345345E9F;
      if (value3 > 0.5F) {
         computedValue += (RANDOM.nextFloat() - 0.5F) * 3.0F;
         computedValue2 += (RANDOM.nextFloat() - 0.5F) * 2.0F;
      }

      if (value3 > 1.0F) {
         angle += RANDOM.nextFloat() * 5.0F;
         angle2 += RANDOM.nextFloat() * 3.0F;
      }

      float value4 = this.serverYaw(player);
      float value5 = this.serverPitch(player);
      float computedValue3 = Mth.wrapDegrees(value + computedValue - value4);
      float computedValue4 = Mth.wrapDegrees(value2 + computedValue2 - value5);
      float gCDValue = MouseSensitivity.getGCDValue();
      float yaw = value4 + computedValue3;
      float clampedValue = Mth.clamp(value5 + computedValue4, -90.0F, 90.0F);
      if (gCDValue != 0.0F) {
         yaw -= (yaw - value4) % gCDValue;
         clampedValue -= (clampedValue - value5) % gCDValue;
      }

      this.lerpYaw = yaw;
      this.lerpPitch = clampedValue;
      RotationManager.update(new Rotation(yaw, clampedValue), angle, angle2, angle, angle2, 5, 5, false);
   }
}
