package net.skycore.client.util;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.LivingEntityGetNoJumpDelayAccessor;

public class PredictHelper {
   private int simulatedTicks;
   public boolean sprinting;
   private boolean edgeClipped;
   public AABB box;
   public boolean onGround;
   public float fallDistance;
   public boolean verticalCollision;
   public boolean swimming;
   public boolean horizontalCollision;
   public final PredictedPlayerInput input;
   public boolean jumping;
   public final Player player;
   private final HashSet<TagKey<Fluid>> eyeFluidTags;
   public Vec3 velocity;
   public boolean fallFlying;
   public boolean inWater;
   public boolean underWater;
   public int jumpDelay;
   public float pitch;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeights;
   public float yaw;
   public float movementSpeedOverride = Float.NaN;
   public Vec3 position;

   private Vec3 movementInputToVelocity(Vec3 position2, float value, float value2) {
      double lengthSquared = position2.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return Vec3.ZERO;
      }

      Vec3 position3;
      if (lengthSquared > 1.0) {
         position3 = position2.normalize();
      } else {
         position3 = position2;
      }

      Vec3 position4 = position3.scale(value);
      float computedValue = Mth.sin(value2 * (float) (Math.PI / 180.0));
      double computedValue2 = value2 * (float) (Math.PI / 180.0);
      float cosine = Mth.cos(computedValue2);
      return new Vec3(position4.x * cosine - position4.z * computedValue, position4.y, position4.z * cosine + position4.x * computedValue);
   }

   private void updateEyeFluidTags() {
      this.underWater = this.eyeFluidTags.contains(FluidTags.WATER);
      this.eyeFluidTags.clear();
      double computedValue = this.getEyeY() - 0.11111111F;
      if (!(this.player.getVehicle() instanceof Boat boat && !boat.isUnderWater() && boat.getBoundingBox().maxY >= computedValue && boat.getBoundingBox().minY <= computedValue)) {
         BlockPos blockPos = BlockPos.containing(this.position.x, computedValue, this.position.z);
         FluidState fluidState = this.player.level().getFluidState(blockPos);
         double y = blockPos.getY() + fluidState.getHeight(this.player.level(), blockPos);
         if (y > computedValue) {
            this.eyeFluidTags.addAll(fluidState.typeHolder().tags().toList());
         }
      }
   }

   private Vec3 clampClimbingVelocity(Vec3 position2) {
      if (!this.isClimbing()) {
         return position2;
      }

      this.resetFallDistance();
      double clampedValue = Mth.clamp(position2.x, -0.15F, 0.15F);
      double clampedValue2 = Mth.clamp(position2.z, -0.15F, 0.15F);
      double maximum = Math.max(position2.y, -0.15F);
      if (maximum < 0.0) {
         BlockPos blockPos = this.blockPosition(this.position);
         BlockState blockState = this.getBlockState(blockPos);
         if (!blockState.is(Blocks.SCAFFOLDING)) {
            if (this.player.isSuppressingSlidingDownLadder()) {
               maximum = 0.0;
            }
         }
      }

      return new Vec3(clampedValue, maximum, clampedValue2);
   }

   private Vec3 clipMovementAtEdge(Vec3 position2) {
      if (position2.y <= 0.0 && this.isAboveGround()) {
         double x = position2.x;
         double z = position2.z;

         double index;
         for (index = 0.05; x != 0.0 && this.player.level().noCollision(this.player, this.box.move(x, -0.5, 0.0)); x += x > 0.0 ? -index : index) {
            if (x < index && x >= -index) {
               x = 0.0;
               break;
            }
         }

         while (z != 0.0 && this.player.level().noCollision(this.player, this.box.move(0.0, -0.5, z))) {
            if (z < index && z >= -index) {
               z = 0.0;
               break;
            }

            z += z > 0.0 ? -index : index;
         }

         while (x != 0.0 && z != 0.0 && this.player.level().noCollision(this.player, this.box.move(x, -0.5, z))) {
            x = x < index && x >= -index ? 0.0 : (x > 0.0 ? x - index : x + index);
            if (z < index && z >= -index) {
               z = 0.0;
               break;
            }

            z += z > 0.0 ? -index : index;
         }

         if (position2.x != x || position2.z != z) {
            this.edgeClipped = true;
         }

         if (this.isSneaking()) {
            position2 = new Vec3(x, position2.y, z);
         }
      }

      return position2;
   }

   private double getFluidHeight(TagKey tagKey) {
      return this.fluidHeights.getDouble(tagKey);
   }

   private BlockPos getBlockBelow() {
      double intermediate = this.box.minY - 0.5000001;
      return BlockPos.containing(this.position.x, intermediate, this.position.z);
   }

   private boolean updateFluidHeightAndPush(TagKey tagKey, double value) {
      if (this.areSurroundingChunksMissing()) {
         return false;
      }

      AABB bounds = this.box.deflate(0.001);
      int roundedDown = Mth.floor(bounds.minX);
      int roundedUp = Mth.ceil(bounds.maxX);
      int roundedDown2 = Mth.floor(bounds.minY);
      int roundedUp2 = Mth.ceil(bounds.maxY);
      int roundedDown3 = Mth.floor(bounds.minZ);
      int roundedUp3 = Mth.ceil(bounds.maxZ);
      double value2 = 0.0;
      byte flag = 0;
      Vec3 zERO = Vec3.ZERO;
      int value3 = 0;
      MutableBlockPos mutablePos = new MutableBlockPos();

      for (int index = roundedDown; index < roundedUp; index++) {
         for (int index2 = roundedDown2; index2 < roundedUp2; index2++) {
            for (int index3 = roundedDown3; index3 < roundedUp3; index3++) {
               mutablePos.set(index, index2, index3);
               FluidState fluidState = this.player.level().getFluidState(mutablePos);
               if (fluidState.is(tagKey)) {
                  double height = index2 + fluidState.getHeight(this.player.level(), mutablePos);
                  if (height >= bounds.minY) {
                     flag = 1;
                     value2 = Math.max(height - bounds.minY, value2);
                     Vec3 flow = fluidState.getFlow(this.player.level(), mutablePos);
                     if (value2 < 0.4) {
                        flow = flow.scale(value2);
                     }

                     zERO = zERO.add(flow);
                     value3++;
                  }
               }
            }
         }
      }

      if (zERO.length() > 0.0) {
         if (value3 > 0) {
            zERO = zERO.scale(1.0 / value3);
         }

         zERO = zERO.scale(value);
         if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && zERO.length() < 0.0045) {
            zERO = zERO.normalize().scale(0.0045);
         }

         this.velocity = this.velocity.add(zERO);
      }

      this.fluidHeights.put(tagKey, value2);
      return (flag != 0);
   }

   private boolean canMoveTo(double value, double value2, double value3) {
      AABB bounds = this.box.move(value, value2, value3);
      return this.isSpaceFree(bounds);
   }

   private boolean isAboveGround() {
      if (!this.onGround) {
         if (this.fallDistance < 0.5) {
            Level level = this.player.level();
            Player player2 = this.player;
            AABB bounds = this.box.move(0.0, this.fallDistance - 0.5, 0.0);
            if (!level.noCollision(player2, bounds)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private void updateWaterState() {
      if (this.player.getVehicle() instanceof Boat) {
         Boat vehicle = (Boat)this.player.getVehicle();
         if (!vehicle.isUnderWater()) {
            this.inWater = false;
            return;
         }
      }

      if (this.updateFluidHeightAndPush(FluidTags.WATER, 0.014)) {
         this.resetFallDistance();
         this.inWater = true;
      } else {
         this.inWater = false;
      }
   }

   public boolean isClimbing() {
      BlockPos blockPos = this.blockPosition(this.position);
      BlockState blockState = this.getBlockState(blockPos);
      if (blockState.is(BlockTags.CLIMBABLE)) {
         return true;
      }

      if (blockState.getBlock() instanceof TrapDoorBlock) {
         if (this.isTrapdoorClimbable(blockPos, blockState)) {
            return true;
         }
      }

      return false;
   }

   private float getMovementSpeed() {
      float result;
      if (Float.isNaN(this.movementSpeedOverride)) {
         result = 0.1F;
      } else {
         result = this.movementSpeedOverride;
      }

      return result;
   }

   public void jump() {
      Vec3 velocity2 = this.velocity;
      double jumpVelocity = this.getJumpVelocity();
      double y = jumpVelocity - this.velocity.y;
      Vec3 position2 = velocity2.add(0.0, y, 0.0);
      this.velocity = position2;
      if (this.isSprinting()) {
         double yaw2 = this.yaw;
         float angleRadians = (float)Math.toRadians(yaw2);
         Vec3 velocity3 = this.velocity;
         double value = angleRadians;
         double computedValue = -Mth.sin(value) * 0.2;
         double value2 = angleRadians;
         double computedValue2 = Mth.cos(value2) * 0.2;
         this.velocity = velocity3.add(computedValue, 0.0, computedValue2);
      }
   }

   private Vec3 directionFromRotation(float value, float value2) {
      float computedValue = (float)(value * Math.PI / 180.0);
      float computedValue2 = (float)(-value2 * Math.PI / 180.0);
      double intermediate = computedValue2;
      float cosine = Mth.cos(intermediate);
      intermediate = computedValue2;
      float sine = Mth.sin(intermediate);
      float cosine2 = Mth.cos(computedValue);
      intermediate = computedValue;
      float sine2 = Mth.sin(intermediate);
      double computedValue3 = sine * cosine2;
      double intermediate2 = -sine2;
      double intermediate3 = cosine * cosine2;
      return new Vec3(computedValue3, intermediate2, intermediate3);
   }

   public double getAttributeValue(Holder holder) {
      return this.player.getAttributes().getValue(holder);
   }

   private boolean isSprinting() {
      return this.sprinting;
   }

   private void jumpInFluid(TagKey tagKey) {
      this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
   }

   public static PredictHelper predictLocalPlayer(int ticks) {
      PredictHelper instance = fromLocalPlayer(PredictedPlayerInput.from(Minecraft.getInstance().player.input.keyPresses));

      for (int value = 0; value < ticks; value++) {
         instance.tick();
      }

      return instance;
   }

   private Vec3 getLookDirection() {
      return this.directionFromRotation(this.pitch, this.yaw);
   }

   private float getJumpVelocity() {
      return 0.42F * this.getBlockJumpFactor() + this.getJumpBoost();
   }

   private void move(Vec3 position2) {
      Vec3 position3 = this.clipMovementAtEdge(position2);
      Vec3 position4 = this.collideMovement(position3);
      if (position4.lengthSqr() > 1.0E-7) {
         this.position = this.position.add(position4);
         this.box = this.player.getDimensions(this.player.getPose()).makeBoundingBox(this.position);
      }

      int x = !Mth.equal(position2.x, position4.x) ? 1 : 0;
      int z = !Mth.equal(position2.z, position4.z) ? 1 : 0;
      this.horizontalCollision = ((x == 0 && z == 0 ? 0 : 1) != 0);
      this.verticalCollision = ((position2.y != position4.y ? 1 : 0) != 0);
      this.onGround = ((this.verticalCollision && position2.y < 0.0 ? 1 : 0) != 0);
      if (!this.isInWater()) {
         this.updateWaterState();
      }

      if (this.onGround) {
         this.resetFallDistance();
      } else if (position2.y < 0.0) {
         this.fallDistance = this.fallDistance - (float)position2.y;
      }

      Vec3 velocity2 = this.velocity;
      if (this.horizontalCollision || this.verticalCollision) {
         this.velocity = new Vec3(x != 0 ? 0.0 : velocity2.x, this.onGround ? 0.0 : velocity2.y, z != 0 ? 0.0 : velocity2.z);
      }
   }

   protected boolean isSneaking() {
      if (!this.input.buttons.shift()) {
         if (!this.input.forceSneaking) {
            return false;
         }
      }

      return true;
   }

   private boolean isTrapdoorClimbable(BlockPos blockPos, BlockState blockState) {
      Boolean value = blockState.getValue(TrapDoorBlock.OPEN);
      if (!value) {
         return false;
      }

      BlockState blockState2 = this.player.level().getBlockState(blockPos.below());
      if (blockState2.is(Blocks.LADDER)) {
         Direction value2 = blockState2.getValue(LadderBlock.FACING);
         if (value2.equals(blockState.getValue(TrapDoorBlock.FACING))) {
            return true;
         }
      }

      return false;
   }

   private double getFluidJumpThreshold() {
      return this.player.getEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   private void updateSwimming() {
      byte flag = -1;
      if (this.swimming) {
         continueSwimmingDecision: {
            if (this.isSprinting()) {
               if (this.isInWater()) {
                  if (!this.player.isPassenger()) {
                     flag = 1;
                     break continueSwimmingDecision;
                  }
               }
            }

            flag = 0;
         }

         this.swimming = (flag != 0);
      } else {
         startSwimmingDecision: {
            if (this.isSprinting()) {
               if (this.isUnderWater()) {
                  if (!this.player.isPassenger()) {
                     Level level = this.player.level();
                     BlockPos blockPos = this.blockPosition(this.position);
                     FluidState fluidState = level.getFluidState(blockPos);
                     if (fluidState.is(FluidTags.WATER)) {
                        flag = 1;
                        break startSwimmingDecision;
                     }
                  }
               }
            }

            flag = 0;
         }

         this.swimming = (flag != 0);
      }
   }

   private float getFrictionAdjustedSpeed(float value) {
      float result;
      if (this.onGround) {
         result = this.getMovementSpeed() * (0.21600002F / (value * value * value));
      } else {
         result = this.getAirAcceleration();
      }

      return result;
   }

   private float getBlockJumpFactor() {
      float value = 0.0F;
      Block block = this.getBlockState(this.blockPosition(this.position)).getBlock();
      if (block != null) {
         value = block.getJumpFactor();
      }

      float value2 = 0.0F;
      Block block2 = this.getBlockState(this.getBlockBelow()).getBlock();
      if (block2 != null) {
         value2 = block2.getJumpFactor();
      }

      return value == 1.0F ? value2 : value;
   }

   public static boolean willStopSprinting(int ticks) {
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.input != null && ticks > 0) {
         PredictHelper instance = fromLocalPlayer(PredictedPlayerInput.from(Minecraft.getInstance().player.input.keyPresses));
         if (!instance.sprinting) {
            return true;
         }

         for (int value = 0; value < ticks; value++) {
            instance.tick();
            if (!instance.sprinting) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void updateSprinting() {
      if (this.sprinting && this.shouldStopSprinting()) {
         this.sprinting = false;
      }

      if (!this.sprinting) {
         if (this.canStartSprinting()) {
            this.sprinting = true;
         }
      }
   }

   private static HashSet collectEyeFluidTags(Player player2) {
      HashSet entries = new HashSet();
      if (player2.isEyeInFluid(FluidTags.WATER)) {
         entries.add(FluidTags.WATER);
      }

      if (player2.isEyeInFluid(FluidTags.LAVA)) {
         entries.add(FluidTags.LAVA);
      }

      return entries;
   }

   public PredictHelper copy() {
      Player player2 = this.player;
      PredictedPlayerInput input2 = this.input;
      Vec3 position2 = this.position;
      Vec3 velocity2 = this.velocity;
      AABB box2 = this.box;
      float yaw2 = this.yaw;
      float pitch2 = this.pitch;
      int intermediate = -1;
      boolean sprinting2 = this.sprinting;
      float fallDistance2 = this.fallDistance;
      intermediate = this.jumpDelay;
      boolean jumping2 = this.jumping;
      boolean fallFlying2 = this.fallFlying;
      boolean onGround2 = this.onGround;
      boolean horizontalCollision2 = this.horizontalCollision;
      boolean verticalCollision2 = this.verticalCollision;
      boolean inWater2 = this.inWater;
      boolean swimming2 = this.swimming;
      boolean underWater2 = this.underWater;
      Object2DoubleArrayMap lookup = new Object2DoubleArrayMap<>(this.fluidHeights);
      HashSet entries = new HashSet<>(this.eyeFluidTags);
      return new PredictHelper(
         player2,
         input2,
         position2,
         velocity2,
         box2,
         yaw2,
         pitch2,
         sprinting2,
         fallDistance2,
         intermediate,
         jumping2,
         fallFlying2,
         onGround2,
         horizontalCollision2,
         verticalCollision2,
         inWater2,
         swimming2,
         underWater2,
         lookup,
         entries
      );
   }

   public PredictHelper(
      Player player2,
      PredictedPlayerInput input2,
      Vec3 position2,
      Vec3 velocity2,
      AABB box2,
      float yaw2,
      float pitch2,
      boolean sprinting2,
      float fallDistance2,
      int jumpDelay2,
      boolean jumping2,
      boolean fallFlying2,
      boolean onGround2,
      boolean horizontalCollision2,
      boolean verticalCollision2,
      boolean inWater2,
      boolean swimming2,
      boolean underWater2,
      Object2DoubleMap fluidHeights2,
      HashSet eyeFluidTags2
   ) {
      this.simulatedTicks = 0;
      this.edgeClipped = false;
      this.player = player2;
      this.input = input2;
      this.position = position2;
      this.velocity = velocity2;
      this.box = box2;
      this.yaw = yaw2;
      this.pitch = pitch2;
      this.sprinting = sprinting2;
      this.fallDistance = fallDistance2;
      this.jumpDelay = jumpDelay2;
      this.jumping = jumping2;
      this.fallFlying = fallFlying2;
      this.onGround = onGround2;
      this.horizontalCollision = horizontalCollision2;
      this.verticalCollision = verticalCollision2;
      this.inWater = inWater2;
      this.swimming = swimming2;
      this.underWater = underWater2;
      this.fluidHeights = fluidHeights2;
      this.eyeFluidTags = eyeFluidTags2;
   }

   public Vec3 getOriginalPosition() {
      return this.player.position();
   }

   private double getEyeY() {
      return this.position.y + this.player.getEyeHeight();
   }

   public static boolean willKeepSprintingWithoutInput(int ticks) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         Minecraft instance2 = Minecraft.getInstance();
         if (instance2.player.input != null) {
            PredictedPlayerInput instance3 = PredictedPlayerInput.from(Minecraft.getInstance().player.input.keyPresses);
            PredictHelper predictHelper = fromLocalPlayer(instance3);
            Input input2 = predictHelper.input.buttons;
            PredictedPlayerInput input3 = predictHelper.input;
            boolean jump = input2.jump();
            Input input4 = new Input(false, false, false, false, jump, input2.shift(), false);
            input3.buttons = input4;

            for (int value = 0; value < ticks; value++) {
               predictHelper.tick();
            }

            return predictHelper.sprinting;
         }
      }

      return false;
   }

   private void travel(Vec3 position2) {
      if (this.swimming && !this.player.isPassenger()) {
         double y = this.getLookDirection().y;
         double computedValue = y < -0.2 ? 0.085 : 0.06;
         BlockPos blockPos = new BlockPos(Mth.floor(this.position.x), Mth.floor(this.position.y + 1.0 - 0.1), Mth.floor(this.position.z));
         if (y <= 0.0 || this.input.buttons.jump() || !this.player.level().getBlockState(blockPos).getFluidState().isEmpty()) {
            this.velocity = this.velocity.add(0.0, (y - this.velocity.y) * computedValue, 0.0);
         }
      }

      double y2 = this.velocity.y;
      double value = 0.08;
      int y3 = this.velocity.y <= 0.0 ? 1 : 0;
      if (this.velocity.y <= 0.0 && this.hasEffect(MobEffects.SLOW_FALLING)) {
         value = 0.01;
         this.resetFallDistance();
      }

      if (this.isInWater() && this.player.isAffectedByFluids()) {
         double y4 = this.position.y;
         float value2 = this.isSprinting() ? 0.9F : 0.8F;
         float value3 = 0.02F;
         float attributeValue = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.onGround) {
            attributeValue *= 0.5F;
         }

         if (attributeValue > 0.0F) {
            value2 += (0.54600006F - value2) * attributeValue / 3.0F;
            value3 += (this.getMovementSpeed() - value3) * attributeValue / 3.0F;
         }

         if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
            value2 = 0.96F;
         }

         this.moveRelative(value3, position2);
         this.move(this.velocity);
         Vec3 velocity2 = this.velocity;
         if (this.horizontalCollision && this.isClimbing()) {
            velocity2 = new Vec3(velocity2.x, 0.2, velocity2.z);
         }

         this.velocity = velocity2.multiply(value2, 0.8, value2);
         Vec3 fluidFallingAdjustedMovement = this.player.getFluidFallingAdjustedMovement(value, (y3 != 0), this.velocity);
         this.velocity = fluidFallingAdjustedMovement;
         if (this.horizontalCollision && this.canMoveTo(fluidFallingAdjustedMovement.x, fluidFallingAdjustedMovement.y + 0.6 - this.position.y + y4, fluidFallingAdjustedMovement.z)) {
            this.velocity = new Vec3(fluidFallingAdjustedMovement.x, 0.3, fluidFallingAdjustedMovement.z);
         }
      } else if (this.isInLava() && this.player.isAffectedByFluids()) {
         double y5 = this.position.y;
         this.moveRelative(0.02F, position2);
         this.move(this.velocity);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
            this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
            this.velocity = this.player.getFluidFallingAdjustedMovement(value, (y3 != 0), this.velocity);
         } else {
            this.velocity = this.velocity.scale(0.5);
         }

         if (!this.player.isNoGravity()) {
            this.velocity = this.velocity.add(0.0, -value / 4.0, 0.0);
         }

         if (this.horizontalCollision && this.canMoveTo(this.velocity.x, this.velocity.y + 0.6 - this.position.y + y5, this.velocity.z)) {
            this.velocity = new Vec3(this.velocity.x, 0.3, this.velocity.z);
         }
      } else if (this.fallFlying) {
         Vec3 velocity3 = this.velocity;
         Vec3 lookDirection = this.getLookDirection();
         float computedValue2 = this.pitch * (float) (Math.PI / 180.0);
         double squareRoot = Math.sqrt(lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z);
         double value4 = this.velocity.horizontalDistance();
         double length = lookDirection.length();
         float cosine = Mth.cos(computedValue2);
         cosine = (float)((double)cosine * cosine * Math.min(1.0, length / 0.4));
         velocity3 = this.velocity.add(0.0, value * (-1.0 + cosine * 0.75), 0.0);
         if (velocity3.y < 0.0 && squareRoot > 0.0) {
            double y6 = velocity3.y * -0.1 * cosine;
            velocity3 = velocity3.add(lookDirection.x * y6 / squareRoot, y6, lookDirection.z * y6 / squareRoot);
         }

         if (computedValue2 < 0.0F && squareRoot > 0.0) {
            double computedValue3 = value4 * -Mth.sin(computedValue2) * 0.04;
            velocity3 = velocity3.add(-lookDirection.x * computedValue3 / squareRoot, computedValue3 * 3.2, -lookDirection.z * computedValue3 / squareRoot);
         }

         if (squareRoot > 0.0) {
            velocity3 = velocity3.add((lookDirection.x / squareRoot * value4 - velocity3.x) * 0.1, 0.0, (lookDirection.z / squareRoot * value4 - velocity3.z) * 0.1);
         }

         this.velocity = velocity3.multiply(0.99, 0.98, 0.99);
         this.move(this.velocity);
      } else {
         BlockPos blockBelow = this.getBlockBelow();
         float friction = this.player.level().getBlockState(blockBelow).getBlock().getFriction();
         float computedValue4 = this.onGround ? friction * 0.91F : 0.91F;
         Vec3 position3 = this.travelWithFriction(position2, friction);
         double y7 = position3.y;
         if (this.hasEffect(MobEffects.LEVITATION)) {
            MobEffectInstance effect = this.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
               y7 += (0.05 * (effect.getAmplifier() + 1) - position3.y) * 0.2;
            }
         } else if (this.player.level().isClientSide() && !this.player.level().isLoaded(blockBelow)) {
            y7 = this.position.y > this.player.level().getMaxY() ? -0.1 : 0.0;
         } else if (!this.player.isNoGravity()) {
            y7 -= value;
         }

         if (this.player.shouldDiscardFriction()) {
            this.velocity = new Vec3(position3.x, y7, position3.z);
         } else {
            this.velocity = new Vec3(position3.x * computedValue4, y7 * 0.98F, position3.z * computedValue4);
         }
      }

      if (this.player.getAbilities().flying && !this.player.isPassenger()) {
         this.velocity = new Vec3(this.velocity.x, y2 * 0.6, this.velocity.z);
         this.resetFallDistance();
      }
   }

   private Vec3 collideMovement(Vec3 position2) {
      AABB bounds = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).move(this.position);
      List<net.minecraft.world.phys.shapes.VoxelShape> voxelShapes = Collections.emptyList();
      Vec3 position3;
      if (position2.lengthSqr() == 0.0) {
         position3 = position2;
      } else {
         position3 = Entity.collideBoundingBox(this.player, position2, bounds, this.player.level(), voxelShapes);
      }

      int x = position2.x != position3.x ? 1 : 0;
      int y = position2.y != position3.y ? 1 : 0;
      int z = position2.z != position3.z ? 1 : 0;
      int y2 = !this.onGround && (y == 0 || !(position2.y < 0.0)) ? 0 : 1;
      if (this.player.maxUpStep() > 0.0F && y2 != 0 && (x != 0 || z != 0)) {
         Vec3 position4 = Entity.collideBoundingBox(this.player, new Vec3(position2.x, this.player.maxUpStep(), position2.z), bounds, this.player.level(), voxelShapes);
         Vec3 position5 = Entity.collideBoundingBox(
            this.player, new Vec3(0.0, this.player.maxUpStep(), 0.0), bounds.expandTowards(position2.x, 0.0, position2.z), this.player.level(), voxelShapes
         );
         Vec3 position6 = Entity.collideBoundingBox(this.player, new Vec3(position2.x, 0.0, position2.z), bounds.move(position5), this.player.level(), voxelShapes).add(position5);
         if (position5.y < this.player.maxUpStep() && position6.horizontalDistanceSqr() > position4.horizontalDistanceSqr()) {
            position4 = position6;
         }

         if (position4.horizontalDistanceSqr() > position3.horizontalDistanceSqr()) {
            return position4.add(Entity.collideBoundingBox(this.player, new Vec3(0.0, -position4.y + position2.y, 0.0), bounds.move(position4), this.player.level(), voxelShapes));
         }
      }

      return position3;
   }

   public void tick() {
      this.simulatedTicks++;
      this.edgeClipped = false;
      if (!(this.position.y <= -70.0)) {
         this.input.tick();
         this.updateWaterState();
         this.updateEyeFluidTags();
         this.updateSprinting();
         this.updateSwimming();
         if (this.jumpDelay > 0) {
            this.jumpDelay--;
         }

         this.jumping = this.input.buttons.jump();
         double x = this.velocity.x;
         double y = this.velocity.y;
         double z = this.velocity.z;
         if (Math.abs(this.velocity.x) < 0.003) {
            x = 0.0;
         }

         if (Math.abs(this.velocity.y) < 0.003) {
            y = 0.0;
         }

         if (Math.abs(this.velocity.z) < 0.003) {
            z = 0.0;
         }

         if (this.onGround) {
            this.fallFlying = false;
         }

         this.velocity = new Vec3(x, y, z);
         if (this.jumping) {
            double fluidHeight = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            int value = this.isInWater() && fluidHeight > 0.0 ? 1 : 0;
            double fluidJumpThreshold = this.getFluidJumpThreshold();
            if (value == 0 || this.onGround && !(fluidHeight > fluidJumpThreshold)) {
               if (!this.isInLava() || this.onGround && !(fluidHeight > fluidJumpThreshold)) {
                  if ((this.onGround || value != 0 && fluidHeight <= fluidJumpThreshold) && this.jumpDelay == 0) {
                     this.jump();
                     this.jumpDelay = 10;
                  }
               } else {
                  this.jumpInFluid(FluidTags.LAVA);
               }
            } else {
               this.jumpInFluid(FluidTags.WATER);
            }
         }

         float computedValue = this.input.sideways * 0.98F;
         float computedValue2 = this.input.forward * 0.98F;
         float value2 = 0.0F;
         if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
         }

         this.travel(new Vec3(computedValue, value2, computedValue2));
      }
   }

   public static PredictHelper fromPlayer(Player player2, PredictedPlayerInput predictedPlayerInput) {
      Vec3 position2 = player2.position();
      Vec3 position3 = player2.position();
      int intermediate = -1;
      position3 = position3.subtract(new Vec3(player2.xo, player2.yo, player2.zo));
      AABB boundingBox = player2.getBoundingBox();
      float yRot = player2.getYRot();
      float xRot = player2.getXRot();
      intermediate = (byte)-1;
      boolean sprinting2 = player2.isSprinting();
      float intermediate2 = (float)player2.fallDistance;
      LivingEntityGetNoJumpDelayAccessor livingEntityGetNoJumpDelayAccessor = (LivingEntityGetNoJumpDelayAccessor)player2;
      intermediate = livingEntityGetNoJumpDelayAccessor.skycore$getNoJumpDelay();
      boolean jumping2 = ((LivingEntityGetNoJumpDelayAccessor)player2).skycore$isJumping();
      boolean fallFlying2 = player2.isFallFlying();
      boolean onGround2 = player2.onGround();
      boolean horizontalCollision2 = player2.horizontalCollision;
      boolean verticalCollision2 = player2.verticalCollision;
      boolean inWater2 = player2.isInWater();
      boolean swimming2 = player2.isSwimming();
      boolean underWater2 = player2.isUnderWater();
      Object2DoubleMap fluidHeights2 = collectFluidHeights(player2);
      var eyeFluidTags2 = collectEyeFluidTags(player2);
      return new PredictHelper(
         player2,
         predictedPlayerInput,
         position2,
         position3,
         boundingBox,
         yRot,
         xRot,
         sprinting2,
         intermediate2,
         intermediate,
         jumping2,
         fallFlying2,
         onGround2,
         horizontalCollision2,
         verticalCollision2,
         inWater2,
         swimming2,
         underWater2,
         fluidHeights2,
         eyeFluidTags2
      );
   }

   public boolean isInLava() {
      return this.fluidHeights.getDouble(FluidTags.LAVA) > 0.0;
   }

   private static Object2DoubleMap collectFluidHeights(Player player2) {
      Object2DoubleArrayMap lookup = new Object2DoubleArrayMap();
      double fluidHeight = player2.getFluidHeight(FluidTags.WATER);
      if (fluidHeight > 0.0) {
         lookup.put(FluidTags.WATER, fluidHeight);
      }

      double fluidHeight2 = player2.getFluidHeight(FluidTags.LAVA);
      if (fluidHeight2 > 0.0) {
         lookup.put(FluidTags.LAVA, fluidHeight2);
      }

      return lookup;
   }

   public BlockPos blockPosition(Vec3 position2) {
      int value = 1;
      int roundedDown = Mth.floor(position2.x);
      int roundedDown2 = Mth.floor(position2.y);
      value = Mth.floor(position2.z);
      return new BlockPos(roundedDown, roundedDown2, value);
   }

   private boolean canStartSprinting() {
      if (this.input.buttons.sprint() && this.input.forward > 0.8F) {
         if (!this.input.buttons.shift()) {
            if (!this.horizontalCollision) {
               if (!this.player.isUsingItem() && !this.player.isPassenger()) {
                  if (!this.isInWater()) {
                     return true;
                  }

                  if (this.isUnderWater()) {
                     return true;
                  }
               }
            }
         }
      }

      return false;
   }

   public boolean hasEffect(Holder holder) {
      MobEffectInstance effect = this.player.getEffect(holder);
      if (effect != null) {
         if (effect.getDuration() >= this.simulatedTicks) {
            return true;
         }
      }

      return false;
   }

   public static PredictHelper fromLocalPlayer(PredictedPlayerInput predictedPlayerInput) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player2 = instance.player;
      Vec3 position2 = player2.position();
      Vec3 deltaMovement = player2.getDeltaMovement();
      AABB boundingBox = player2.getBoundingBox();
      float yRot = player2.getYRot();
      float xRot = player2.getXRot();
      int intermediate = -1;
      boolean sprinting2 = player2.isSprinting();
      float value = (float)player2.fallDistance;
      LivingEntityGetNoJumpDelayAccessor livingEntityGetNoJumpDelayAccessor = (LivingEntityGetNoJumpDelayAccessor)player2;
      intermediate = livingEntityGetNoJumpDelayAccessor.skycore$getNoJumpDelay();
      LivingEntityGetNoJumpDelayAccessor livingEntityGetNoJumpDelayAccessor2 = (LivingEntityGetNoJumpDelayAccessor)player2;
      boolean jumping2 = livingEntityGetNoJumpDelayAccessor2.skycore$isJumping();
      boolean fallFlying2 = player2.isFallFlying();
      boolean onGround2 = player2.onGround();
      boolean horizontalCollision2 = player2.horizontalCollision;
      boolean verticalCollision2 = player2.verticalCollision;
      boolean inWater2 = player2.isInWater();
      boolean swimming2 = player2.isSwimming();
      boolean underWater2 = player2.isUnderWater();
      Object2DoubleMap fluidHeights2 = collectFluidHeights(player2);
      var eyeFluidTags2 = collectEyeFluidTags(player2);
      return new PredictHelper(
         player2,
         predictedPlayerInput,
         position2,
         deltaMovement,
         boundingBox,
         yRot,
         xRot,
         sprinting2,
         value,
         intermediate,
         jumping2,
         fallFlying2,
         onGround2,
         horizontalCollision2,
         verticalCollision2,
         inWater2,
         swimming2,
         underWater2,
         fluidHeights2,
         eyeFluidTags2
      );
   }

   private boolean isSpaceFree(AABB bounds) {
      Level level = this.player.level();
      return level.noCollision(this.player, bounds) && !this.player.level().containsAnyLiquid(bounds);
   }

   private boolean areSurroundingChunksMissing() {
      AABB bounds = this.box.inflate(1.0);
      int roundedDown = Mth.floor(bounds.minX);
      int roundedUp = Mth.ceil(bounds.maxX);
      int roundedDown2 = Mth.floor(bounds.minZ);
      int roundedUp2 = Mth.ceil(bounds.maxZ);
      Level level = this.player.level();
      return !level.hasChunksAt(roundedDown, roundedDown2, roundedUp, roundedUp2);
   }

   public static PredictHelper predict(Player player2, int ticks) {
      PredictHelper predictHelper = fromPlayer(player2, PredictedPlayerInput.infer(player2));

      for (int value = 0; value < ticks; value++) {
         predictHelper.tick();
      }

      return predictHelper;
   }

   private void resetFallDistance() {
      this.fallDistance = 0.0F;
   }

   private MobEffectInstance getEffect(Holder holder) {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      MobEffectInstance effect = this.player.getEffect(holder);
      if (effect != null) {
         value = effect.getDuration();
         if (value >= this.simulatedTicks) {
            return effect;
         }
      }

      return null;
   }

   private void moveRelative(float value, Vec3 position2) {
      Vec3 position3 = this.movementInputToVelocity(position2, value, this.yaw);
      this.velocity = this.velocity.add(position3);
   }

   private boolean shouldStopSprinting() {
      if (!(this.input.forward <= 0.0F)) {
         if (!this.input.buttons.shift() && !this.horizontalCollision && (!this.isInWater() || this.isUnderWater())) {
            return false;
         }
      }

      return true;
   }

   private float getAirAcceleration() {
      float value = 0.02F;
      return this.input.buttons.sprint() ? value + 0.006F : value;
   }

   public boolean isUnderWater() {
      if (this.underWater) {
         if (this.isInWater()) {
            return true;
         }
      }

      return false;
   }

   private boolean isInWater() {
      return this.inWater;
   }

   private Vec3 travelWithFriction(Vec3 position2, float value) {
      this.moveRelative(this.getFrictionAdjustedSpeed(value), position2);
      Vec3 position3 = this.clampClimbingVelocity(this.velocity);
      this.velocity = position3;
      this.move(this.velocity);
      Vec3 velocity2 = this.velocity;
      BlockPos blockPos = this.blockPosition(this.position);
      BlockState blockState = this.getBlockState(blockPos);
      if (this.horizontalCollision || this.jumping) {
         if (!this.isClimbing()) {
            if (blockState == null) {
               return velocity2;
            }

            if (!blockState.is(Blocks.POWDER_SNOW)) {
               return velocity2;
            }

            if (!PowderSnowBlock.canEntityWalkOnPowderSnow(this.player)) {
               return velocity2;
            }
         }

         velocity2 = new Vec3(velocity2.x, 0.2, velocity2.z);
      }

      return velocity2;
   }

   public BlockState getBlockState(BlockPos blockPos) {
      return this.player.level().getBlockState(blockPos);
   }

   private float getJumpBoost() {
      if (this.hasEffect(MobEffects.JUMP_BOOST)) {
         MobEffectInstance effect = this.getEffect(MobEffects.JUMP_BOOST);
         return 0.1F * (effect.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }
}
