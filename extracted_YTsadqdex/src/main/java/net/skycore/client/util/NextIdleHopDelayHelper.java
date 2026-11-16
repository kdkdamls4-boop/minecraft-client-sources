package net.skycore.client.util;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.module.combat.AttackAuraModule;

public final class NextIdleHopDelayHelper {
   private LegacyRigHelper type;
   private static final double GROUND_EPSILON = 0.08;
   private static final double FOLLOW_START = 2.85;
   private static final double SAFE_DROP = 1.65;
   private static final double GROUND_CATCHUP_SPEED = 0.255;
   private static final double GRAVITY = 0.055;
   private static final double TARGET_START = 1.75;
   private Vec3 position;
   private float yaw;
   private boolean followingOwner;
   private static final double TARGET_REANCHOR = 0.75;
   private static final double OWNER_REANCHOR = 0.85;
   private double verticalVelocity;
   private Vec3 anchorTargetPosition;
   private int targetId;
   private static final double TARGET_STOP = 1.12;
   private static final double FOLLOW_STOP = 1.55;
   private static final double GROUND_MAX_SPEED = 0.185;
   private int jumpCooldown;
   private float landingAmount;
   private static final double JUMP_VELOCITY = 0.36;
   private static final double IDLE_HOP_VELOCITY = 0.245;
   private Vec3 anchor;
   private Vec3 velocity = Vec3.ZERO;
   private static final double TERMINAL_VELOCITY = -0.72;
   private boolean onGround;
   private static final double TELEPORT_DISTANCE = 12.0;
   private boolean pursuingTarget;
   private int landingPauseTicks;
   private float movementAmount;
   private int idleHopTicks;
   private Vec3 previousPosition;
   private static final double FLY_MAX_SPEED = 0.225;
   private Vec3 anchorOwnerPosition;
   private float previousYaw;

   private static int nextIdleHopDelay() {
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      return threadLocalRandom.nextInt(105, 205);
   }

   private static Vec3 ownerAnchor(LocalPlayer player, LegacyRigHelper legacyRigHelper, boolean condition) {
      double yRot = Math.toRadians(player.getYRot());
      double value = condition ? 1.15 : 1.3;
      double value2 = condition ? 0.7 : 0.92;
      double computedValue = Math.cos(yRot) * value;
      double computedValue2 = Math.sin(yRot) * value;
      double computedValue3 = Math.sin(yRot) * value2;
      double computedValue4 = -Math.cos(yRot) * value2;
      double y = player.getY() + (legacyRigHelper.flying() ? 1.55 : 0.0);
      return new Vec3(player.getX() + computedValue + computedValue3, y, player.getZ() + computedValue2 + computedValue4);
   }

   private void integrateGround(Level level, Vec3 position2) {
      Vec3 position3 = this.anchor.subtract(this.position);
      Vec3 position4 = new Vec3(position3.x, 0.0, position3.z);
      double length = position4.length();
      double value = horizontalDistance(this.position, position2);
      int value2 = !(length >= 0.035) || !this.followingOwner && !this.pursuingTarget ? 0 : 1;
      if (value2 == 0) {
         this.velocity = new Vec3(this.velocity.x * 0.2, 0.0, this.velocity.z * 0.2);
         if (this.velocity.x * this.velocity.x + this.velocity.z * this.velocity.z < 2.0E-5) {
            this.velocity = Vec3.ZERO;
         }

         this.movementAmount = this.movementAmount + (0.0F - this.movementAmount) * 0.38F;
         this.tickIdleHop(level);
      } else {
         this.idleHopTicks = Math.max(this.idleHopTicks, 45);
         double computedValue = Mth.clamp((value - 4.0) / 4.0, 0.0, 1.0);
         double computedValue2 = 0.185 + 0.07 * computedValue;
         Vec3 position5 = position4.lengthSqr() < 1.0E-6 ? Vec3.ZERO : position4.normalize().scale(computedValue2);
         Vec3 position6 = new Vec3(this.velocity.x, 0.0, this.velocity.z);
         position6 = approach(position6, position5, 0.048 + computedValue * 0.018).scale(0.88);
         if (this.landingPauseTicks > 0) {
            position6 = position6.scale(0.52);
         }

         this.velocity = new Vec3(position6.x, 0.0, position6.z);
      }

      double x = groundBelow(level, this.position.x, this.position.z, this.position.y + 0.2, 2.25);
      this.onGround = ((!Double.isNaN(x) && Math.abs(this.position.y - x) <= 0.08 && this.verticalVelocity <= 0.0 ? 1 : 0) != 0);
      if (this.onGround) {
         this.position = new Vec3(this.position.x, x, this.position.z);
         this.verticalVelocity = 0.0;
      }

      if (!this.onGround) {
         this.verticalVelocity = Math.max(-0.72, this.verticalVelocity - 0.055);
      }

      Vec3 position7 = new Vec3(this.velocity.x, 0.0, this.velocity.z);
      Vec3 position8 = this.position.add(position7);
      int x2 = position7.lengthSqr() > 5.0E-5 && !bodyFree(level, new Vec3(position8.x, this.position.y, position8.z)) ? 1 : 0;
      if (x2 != 0 && this.onGround && this.jumpCooldown == 0 && bodyFree(level, new Vec3(position8.x, this.position.y + 1.05, position8.z))) {
         this.jump(0.36);
      }

      if (this.onGround && position7.lengthSqr() > 5.0E-5 && x2 == 0) {
         double x3 = groundBelow(level, position8.x, position8.z, this.position.y + 0.35, 2.4);
         if (Double.isNaN(x3) || this.position.y - x3 > 1.65) {
            this.velocity = new Vec3(0.0, 0.0, 0.0);
            position7 = Vec3.ZERO;
            position8 = this.position;
         }
      }

      double y = this.position.y + this.verticalVelocity;
      if (x2 != 0) {
         position8 = new Vec3(this.position.x, y, this.position.z);
      } else {
         position8 = new Vec3(position8.x, y, position8.z);
      }

      if (!bodyFree(level, position8)) {
         if (this.verticalVelocity > 0.0) {
            this.verticalVelocity = 0.0;
            position8 = new Vec3(position8.x, this.position.y, position8.z);
         } else {
            position8 = new Vec3(this.position.x, y, this.position.z);
         }
      }

      double x4 = groundBelow(level, position8.x, position8.z, Math.max(position8.y, this.position.y) + 0.15, 3.0);
      if (this.verticalVelocity <= 0.0 && !Double.isNaN(x4) && position8.y <= x4 + 0.08) {
         double computedValue3 = -this.verticalVelocity;
         int value3 = !this.onGround && computedValue3 > 0.08 ? 1 : 0;
         this.position = new Vec3(position8.x, x4, position8.z);
         this.verticalVelocity = 0.0;
         this.onGround = true;
         if (value3 != 0) {
            this.landingAmount = (float)Mth.clamp(computedValue3 * 1.85, 0.18, 1.0);
            this.landingPauseTicks = computedValue3 > 0.38 ? 3 : 1;
            this.velocity = this.velocity.scale(computedValue3 > 0.38 ? 0.48 : 0.72);
         }
      } else {
         this.position = position8;
         this.onGround = false;
      }

      if (value2 != 0) {
         float clampedValue = (float)Mth.clamp(length / 0.8, 0.0, 1.0);
         this.movementAmount = this.movementAmount + (clampedValue - this.movementAmount) * 0.32F;
      }
   }

   private void integrate(Level level, Vec3 position2, LegacyRigHelper legacyRigHelper) {
      if (this.anchor != null) {
         if (legacyRigHelper.flying()) {
            this.integrateFlying(level);
         } else {
            this.integrateGround(level, position2);
         }
      }
   }

   private void updateTargetIntent(LivingEntity targetId2, LegacyRigHelper legacyRigHelper, Level level) {
      Vec3 position2 = targetId2.position();
      double value = horizontalDistance(this.position, position2);
      if (targetId2.getId() != this.targetId) {
         this.targetId = targetId2.getId();
         this.anchorTargetPosition = null;
         this.pursuingTarget = true;
      } else if (!this.pursuingTarget && value > 1.75) {
         this.pursuingTarget = true;
      } else if (this.pursuingTarget && value < 1.12) {
         this.pursuingTarget = false;
      }

      if (this.anchorTargetPosition == null || horizontalDistance(this.anchorTargetPosition, position2) > 0.75) {
         this.anchorTargetPosition = position2;
         this.anchor = targetAnchor(targetId2, legacyRigHelper);
         if (!legacyRigHelper.flying()) {
            this.anchor = withGroundY(level, this.anchor, position2.y);
         }
      }

      this.followingOwner = false;
      if (!this.pursuingTarget) {
         this.anchor = this.position;
         this.velocity = this.velocity.multiply(0.3, legacyRigHelper.flying() ? 0.3 : 1.0, 0.3);
      }
   }

   private static boolean headRoom(Level level, Vec3 position2) {
      BlockPos blockPos = BlockPos.containing(position2.x, position2.y + 1.05, position2.z);
      double y = position2.y + 1.55;
      BlockPos blockPos2 = BlockPos.containing(position2.x, y, position2.z);
      VoxelShape collisionShape = level.getBlockState(blockPos).getCollisionShape(level, blockPos);
      if (collisionShape.isEmpty()) {
         VoxelShape collisionShape2 = level.getBlockState(blockPos2).getCollisionShape(level, blockPos2);
         if (collisionShape2.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   public void tick(LegacyRigHelper type2) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      ClientLevel level = instance.level;
      if (player != null && level != null) {
         if (type2 != this.type) {
            this.type = type2;
            this.reset();
         }

         Vec3 position2 = player.position();
         if (this.position == null) {
            Vec3 position3 = ownerAnchor(player, type2, true);
            this.position = type2.flying() ? position3 : withGroundY(level, position3, position2.y);
            this.previousPosition = this.position;
            this.anchor = this.position;
            this.anchorOwnerPosition = position2;
            this.yaw = player.getYRot();
            this.previousYaw = this.yaw;
            this.onGround = ((!type2.flying() ? 1 : 0) != 0);
            this.idleHopTicks = nextIdleHopDelay();
         } else {
            this.previousPosition = this.position;
            this.previousYaw = this.yaw;
            this.landingAmount = this.landingAmount + (0.0F - this.landingAmount) * 0.22F;
            if (this.jumpCooldown > 0) {
               this.jumpCooldown--;
            }

            if (this.landingPauseTicks > 0) {
               this.landingPauseTicks--;
            }

            if (this.position.distanceTo(position2) > 12.0) {
               Vec3 position4 = ownerAnchor(player, type2, true);
               this.position = type2.flying() ? position4 : withGroundY(level, position4, position2.y);
               this.previousPosition = this.position;
               this.velocity = Vec3.ZERO;
               this.verticalVelocity = 0.0;
               this.onGround = ((!type2.flying() ? 1 : 0) != 0);
               this.anchor = this.position;
               this.anchorOwnerPosition = position2;
               this.anchorTargetPosition = null;
               this.targetId = Integer.MIN_VALUE;
               this.followingOwner = false;
               this.pursuingTarget = false;
               this.idleHopTicks = nextIdleHopDelay();
            } else {
               LivingEntity entity = currentAuraTarget();
               if (validTarget(entity, player)) {
                  this.updateTargetIntent(entity, type2, level);
               } else {
                  this.updateOwnerIntent(player, type2, level);
               }

               this.integrate(level, position2, type2);
               this.updateFacing(player, entity);
            }
         }
      } else {
         this.reset();
      }
   }

   private static boolean validTarget(LivingEntity entity, LocalPlayer player) {
      if (entity != null && entity != player) {
         if (entity.isAlive() && !entity.isRemoved()) {
            return true;
         }
      }

      return false;
   }

   public void reset() {
      this.position = null;
      this.previousPosition = null;
      this.velocity = Vec3.ZERO;
      this.anchor = null;
      this.anchorOwnerPosition = null;
      this.anchorTargetPosition = null;
      this.targetId = Integer.MIN_VALUE;
      this.followingOwner = false;
      this.pursuingTarget = false;
      this.onGround = false;
      this.verticalVelocity = 0.0;
      this.jumpCooldown = 0;
      this.idleHopTicks = nextIdleHopDelay();
      this.landingPauseTicks = 0;
      this.landingAmount = 0.0F;
      this.movementAmount = 0.0F;
   }

   private void updateOwnerIntent(LocalPlayer player, LegacyRigHelper legacyRigHelper, Level level) {
      Vec3 position2 = player.position();
      double value = horizontalDistance(this.position, position2);
      this.targetId = Integer.MIN_VALUE;
      this.anchorTargetPosition = null;
      this.pursuingTarget = false;
      if (!this.followingOwner && value > 2.85) {
         this.followingOwner = true;
         this.anchorOwnerPosition = null;
      } else if (this.followingOwner && value < 1.55) {
         this.followingOwner = false;
         this.anchor = this.position;
         this.velocity = this.velocity.multiply(0.24, legacyRigHelper.flying() ? 0.24 : 1.0, 0.24);
      }

      int value2 = this.anchorOwnerPosition != null && !(horizontalDistance(this.anchorOwnerPosition, position2) > 0.85) ? 0 : 1;
      if (this.followingOwner && (value2 != 0 || this.anchor == null)) {
         this.anchorOwnerPosition = position2;
         this.anchor = ownerAnchor(player, legacyRigHelper, false);
         if (!legacyRigHelper.flying()) {
            this.anchor = withGroundY(level, this.anchor, position2.y);
         }
      }

      if (!this.followingOwner) {
         this.anchorOwnerPosition = position2;
         this.anchor = this.position;
      }
   }

   private static double groundY(Level level, double value, double value2, double value3, double value4) {
      double intermediate = Math.max(value3, value4) + 2.0;
      double value5 = groundBelow(level, value, value2, intermediate, 8.0);
      return Double.isNaN(value5) ? value4 : value5;
   }

   private void integrateFlying(Level level) {
      Vec3 position2 = this.anchor.subtract(this.position);
      double squareRoot = Math.sqrt(position2.x * position2.x + position2.z * position2.z);
      if (position2.lengthSqr() < 0.0012) {
         this.velocity = this.velocity.scale(0.2);
         if (this.velocity.lengthSqr() < 2.0E-5) {
            this.velocity = Vec3.ZERO;
         }

         this.movementAmount = this.movementAmount + (0.0F - this.movementAmount) * 0.38F;
      } else {
         Vec3 position3 = position2.normalize().scale(0.225);
         this.velocity = approach(this.velocity, position3, 0.055).scale(0.9);
         Vec3 position4 = this.position.add(this.velocity);
         if (spaceFree(level, position4)) {
            this.position = position4;
         } else {
            this.velocity = this.velocity.scale(0.25);
         }

         float clampedValue = (float)Mth.clamp(squareRoot / 0.8, 0.0, 1.0);
         this.movementAmount = this.movementAmount + (clampedValue - this.movementAmount) * 0.32F;
      }
   }

   private void tickIdleHop(Level level) {
      if (this.onGround) {
         if (!this.pursuingTarget && !this.followingOwner && this.landingPauseTicks <= 0) {
            if (this.idleHopTicks > 0) {
               int computedValue = this.idleHopTicks - 1;
               this.idleHopTicks = computedValue;
               return;
            }

            if (headRoom(level, this.position)) {
               this.jump(0.245);
            }

            int intermediate = nextIdleHopDelay();
            this.idleHopTicks = intermediate;
            return;
         }
      }
   }

   public boolean moving() {
      double x = this.velocity.x * this.velocity.x;
      return x + this.velocity.z * this.velocity.z > 3.5E-4;
   }

   public boolean onGround() {
      return this.onGround;
   }

   public float landingAmount() {
      return this.landingAmount;
   }

   private static float approachDegrees(float value, float value2, float value3) {
      float computedValue = Mth.wrapDegrees(value2 - value);
      float computedValue2 = -value3;
      computedValue = Mth.clamp(computedValue, computedValue2, value3);
      return value + computedValue;
   }

   private static Vec3 withGroundY(Level level, Vec3 position2, double value) {
      double x = position2.x;
      double x2 = groundY(level, position2.x, position2.z, position2.y, value);
      return new Vec3(x, x2, position2.z);
   }

   private void jump(double verticalVelocity2) {
      if (this.onGround && this.jumpCooldown <= 0) {
         this.verticalVelocity = verticalVelocity2;
         this.onGround = false;
         this.jumpCooldown = 8;
      }
   }

   private static boolean bodyFree(Level level, Vec3 position2) {
      double y = position2.y + 0.08;
      BlockPos blockPos = BlockPos.containing(position2.x, y, position2.z);
      double y2 = position2.y + 0.5;
      BlockPos blockPos2 = BlockPos.containing(position2.x, y2, position2.z);
      double y3 = position2.y + 0.92;
      BlockPos blockPos3 = BlockPos.containing(position2.x, y3, position2.z);
      if (level.getBlockState(blockPos).getCollisionShape(level, blockPos).isEmpty()) {
         VoxelShape collisionShape = level.getBlockState(blockPos2).getCollisionShape(level, blockPos2);
         if (collisionShape.isEmpty()) {
            VoxelShape collisionShape2 = level.getBlockState(blockPos3).getCollisionShape(level, blockPos3);
            if (collisionShape2.isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private static LivingEntity currentAuraTarget() {
      ModuleManager moduleManager = SkycoreClient.modules();
      AttackAuraModule attackAuraModule = moduleManager.get(AttackAuraModule.class).orElse(null);
      if (attackAuraModule != null) {
         if (attackAuraModule.isEnabled()) {
            return attackAuraModule.currentTarget();
         }
      }

      return null;
   }

   private void updateFacing(LocalPlayer player, LivingEntity entity) {
      Vec3 position2 = new Vec3(this.velocity.x, 0.0, this.velocity.z);
      float yaw2;
      if (position2.lengthSqr() > 3.5E-4) {
         yaw2 = (float)Math.toDegrees(Math.atan2(-position2.x, position2.z));
      } else {
         Vec3 position3 = validTarget(entity, player) ? entity.position().subtract(this.position) : player.position().subtract(this.position);
         yaw2 = position3.x * position3.x + position3.z * position3.z < 0.01 ? this.yaw : (float)Math.toDegrees(Math.atan2(-position3.x, position3.z));
      }

      float yaw3 = position2.lengthSqr() > 3.5E-4 ? 18.0F : 4.2F;
      this.yaw = approachDegrees(this.yaw, yaw2, yaw3);
   }

   public NextIdleHopDelayHelper() {
      this.targetId = Integer.MIN_VALUE;
      this.type = LegacyRigHelper.DOG;
   }

   public float interpolatedYaw(float value) {
      float y = this.yaw - this.previousYaw;
      float computedValue = Mth.wrapDegrees(y);
      return this.previousYaw + computedValue * Mth.clamp(value, 0.0F, 1.0F);
   }

   private static Vec3 approach(Vec3 position2, Vec3 position3, double value) {
      Vec3 position4 = position3.subtract(position2);
      double length = position4.length();
      return !(length <= value) && !(length < 1.0E-8) ? position2.add(position4.scale(value / length)) : position3;
   }

   public float movementAmount() {
      return this.movementAmount;
   }

   private static double groundBelow(Level level, double index, double index2, double index3, double index4) {
      int roundedDown = Mth.floor(index3);
      int index5 = Mth.floor(index3 - index4) - 1;

      for (int index6 = roundedDown; index6 >= index5; index6--) {
         BlockPos blockPos = BlockPos.containing(index, index6, index2);
         BlockState blockState = level.getBlockState(blockPos);
         if (!blockState.getCollisionShape(level, blockPos).isEmpty()) {
            return blockPos.getY() + 1.0;
         }
      }

      return Double.NaN;
   }

   public Vec3 interpolatedPosition(float value) {
      if (this.position == null) {
         return null;
      }

      Vec3 position2;
      if (this.previousPosition == null) {
         position2 = this.position;
      } else {
         position2 = this.previousPosition;
      }

      Vec3 position3 = position2;
      double clampedValue = Mth.clamp(value, 0.0F, 1.0F);
      double x = position3.x + (this.position.x - position3.x) * clampedValue;
      double y = position3.y + (this.position.y - position3.y) * clampedValue;
      return new Vec3(x, y, position3.z + (this.position.z - position3.z) * clampedValue);
   }

   private static boolean spaceFree(Level level, Vec3 position2) {
      double y = position2.y + 0.3;
      BlockPos blockPos = BlockPos.containing(position2.x, y, position2.z);
      BlockPos blockPos2 = BlockPos.containing(position2.x, position2.y + 1.05, position2.z);
      VoxelShape collisionShape = level.getBlockState(blockPos).getCollisionShape(level, blockPos);
      if (collisionShape.isEmpty()) {
         VoxelShape collisionShape2 = level.getBlockState(blockPos2).getCollisionShape(level, blockPos2);
         if (collisionShape2.isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private static double horizontalDistance(Vec3 position2, Vec3 position3) {
      double x = position2.x - position3.x;
      double z = position2.z - position3.z;
      double intermediate = x * x + z * z;
      return Math.sqrt(intermediate);
   }

   private static Vec3 targetAnchor(LivingEntity entity, LegacyRigHelper legacyRigHelper) {
      Vec3 deltaMovement = entity.getDeltaMovement();
      double x = -deltaMovement.x;
      double z = -deltaMovement.z;
      Vec3 position2 = new Vec3(x, 0.0, z);
      if (position2.lengthSqr() < 0.0025) {
         double yRot = entity.getYRot();
         float angleRadians = (float)Math.toRadians(yRot);
         double sine = Math.sin(angleRadians);
         z = angleRadians;
         position2 = new Vec3(sine, 0.0, -Math.cos(z));
      }

      Vec3 position3 = position2.normalize();
      double value = legacyRigHelper.flying() ? 1.35 : 0.95;
      position2 = position3.scale(value);
      Vec3 position4 = entity.position();
      double x2 = position2.x;
      double value2 = legacyRigHelper.flying() ? 1.35 : 0.0;
      return position4.add(x2, value2, position2.z);
   }
}
