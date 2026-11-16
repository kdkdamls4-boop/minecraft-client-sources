package net.skycore.client.combat;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class InputPredictor {
   private boolean eyeInWater;
   private boolean swimming;
   public Vec3 pos;
   private int simulatedTicks;
   private boolean jumping;
   private boolean fallFlying;
   private boolean inWater;
   private Vec3 velocity;
   private float yaw;
   private final Input input;
   private double lavaHeight;
   private boolean horizontalCollision;
   private AABB boundingBox;
   private int jumpingCooldown;
   private boolean onGround;
   private boolean guessedSprint;
   private float simulatedFallDistance;
   private float movementForward;
   private double waterHeight;
   private float movementSideways;
   private float pitch;
   private final Player player;
   private boolean sprinting;

   private void travelWater(Vec3 position) {
      double y = this.pos.y;
      double value = this.fluidGravity();
      int y2 = this.velocity.y <= 0.0 ? 1 : 0;
      float value2 = this.sprinting ? 0.9F : 0.8F;
      float yaw2 = 0.02F;
      float attributeValue = (float)this.player.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
      if (!this.onGround) {
         attributeValue *= 0.5F;
      }

      if (attributeValue > 0.0F) {
         value2 += (0.54600006F - value2) * attributeValue / 3.0F;
         yaw2 += (0.1F - yaw2) * attributeValue / 3.0F;
      }

      if (this.effectStillActive(MobEffects.DOLPHINS_GRACE)) {
         value2 = 0.96F;
      }

      this.velocity = this.velocity.add(inputToVelocity(position, yaw2, this.yaw));
      this.velocity = this.capClimbable(this.velocity);
      this.move(this.velocity);
      Vec3 velocity2 = this.velocity;
      if (this.horizontalCollision && this.isClimbable()) {
         velocity2 = new Vec3(velocity2.x, 0.2, velocity2.z);
      }

      this.velocity = velocity2.multiply(value2, 0.8, value2);
      this.velocity = this.player.getFluidFallingAdjustedMovement(value, (y2 != 0), this.velocity);
      Vec3 velocity3 = this.velocity;
      if (this.horizontalCollision && this.player.level().noCollision(this.player, this.boundingBox.move(velocity3.x, velocity3.y + 0.6 - this.pos.y + y, velocity3.z))) {
         this.velocity = new Vec3(velocity3.x, 0.3, velocity3.z);
      }
   }

   public static List<AABB> predictOtherPlayerBoxes(Player player2, int ticks) {
      InputPredictor inputPredictor = simulateOtherPlayer(player2, 0);
      int index = ticks + 1;
      ArrayList elements = new ArrayList(index);
      elements.add(inputPredictor.boundingBox());

      for (int value = 0; value < ticks; value++) {
         inputPredictor.tickOther();
         AABB bounds = inputPredictor.boundingBox();
         elements.add(bounds);
      }

      return List.copyOf(elements);
   }

   private boolean isClimbable() {
      if (this.player == null) {
         return false;
      } else {
         Level level = this.player.level();
         BlockPos blockPos = BlockPos.containing(this.pos);
         BlockState blockState = level.getBlockState(blockPos);
         if (blockState.is(BlockTags.CLIMBABLE)) {
            return true;
         } else if (blockState.getBlock() instanceof TrapDoorBlock && blockState.getValue(TrapDoorBlock.OPEN)) {
            BlockState blockState2 = level.getBlockState(blockPos.below());
            return blockState2.is(Blocks.LADDER) && blockState2.getValue(LadderBlock.FACING).equals(blockState.getValue(TrapDoorBlock.FACING));
         } else {
            return false;
         }
      }
   }

   private void tickLocal() {
      if (this.player != null) {
         if (this.onGround) {
            this.fallFlying = false;
         }

         if (this.input.jump() && this.onGround) {
            double jumpBoostPower = 0.42 + this.player.getJumpBoostPower();
            this.velocity = new Vec3(this.velocity.x, jumpBoostPower, this.velocity.z);
            if (this.player.isSprinting()) {
               float yRot = (float)Math.toRadians(this.player.getYRot());
               this.velocity = this.velocity.add(-Math.sin(yRot) * 0.2, 0.0, Math.cos(yRot) * 0.2);
            }
         }

         if (this.fallFlying) {
            this.updateFallFlyingVelocity();
         }

         Vec3 velocity2 = this.velocity;
         double y = velocity2.y;
         if (!this.player.level().noCollision(this.player, this.boundingBox.move(0.0, y, 0.0))) {
            this.onGround = ((y < 0.0 ? 1 : 0) != 0);
            y = 0.0;
         } else {
            this.onGround = false;
         }

         double x = velocity2.x;
         if (!this.player.level().noCollision(this.player, this.boundingBox.move(x, y, 0.0))) {
            x = 0.0;
         }

         double z = velocity2.z;
         if (!this.player.level().noCollision(this.player, this.boundingBox.move(x, y, z))) {
            z = 0.0;
         }

         this.pos = this.pos.add(x, y, z);
         this.boundingBox = this.boundingBox.move(x, y, z);
         if (!this.fallFlying) {
            double y2 = this.player.hasEffect(MobEffects.SLOW_FALLING) && velocity2.y <= 0.0 ? 0.01 : 0.08;
            double y3 = y == 0.0 && this.onGround ? 0.0 : (velocity2.y - y2) * 0.98;
            this.velocity = new Vec3(x * 0.91, y3, z * 0.91);
         }
      }
   }

   private Vec3 capClimbable(Vec3 position) {
      if (!this.isClimbable()) {
         return position;
      }

      this.simulatedFallDistance = 0.0F;
      double maximum = Math.max(position.y, -0.15F);
      BlockState blockState = this.player.level().getBlockState(BlockPos.containing(this.pos));
      if (maximum < 0.0) {
         if (!blockState.is(Blocks.SCAFFOLDING)) {
            if (this.player.isSuppressingSlidingDownLadder()) {
               maximum = 0.0;
            }
         }
      }

      double clampedValue = Mth.clamp(position.x, -0.15F, 0.15F);
      return new Vec3(clampedValue, maximum, Mth.clamp(position.z, -0.15F, 0.15F));
   }

   private void move(Vec3 position) {
      if (this.player != null && this.player.level() != null) {
         AABB bounds = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).move(this.pos);
         Vec3 position2 = this.collideLikeSkycoreClient(this.clipSneakingEdge(position), bounds);
         int x = !Mth.equal(position.x, position2.x) ? 1 : 0;
         int y = position.y != position2.y ? 1 : 0;
         int z = !Mth.equal(position.z, position2.z) ? 1 : 0;
         if (position2.lengthSqr() > 1.0E-7) {
            this.pos = this.pos.add(position2);
            this.boundingBox = this.boundingBox.move(position2);
         }

         this.onGround = ((y != 0 && position.y < 0.0 ? 1 : 0) != 0);
         this.horizontalCollision = ((x == 0 && z == 0 ? 0 : 1) != 0);
         if (this.onGround) {
            this.simulatedFallDistance = 0.0F;
         } else if (position.y < 0.0) {
            this.simulatedFallDistance = this.simulatedFallDistance - (float)position.y;
         }

         if (x != 0 || y != 0 || z != 0) {
            this.velocity = new Vec3(x != 0 ? 0.0 : this.velocity.x, this.onGround ? 0.0 : this.velocity.y, z != 0 ? 0.0 : this.velocity.z);
         }
      } else {
         Vec3 position3 = this.onGround && position.y < 0.0 ? new Vec3(position.x, 0.0, position.z) : position;
         this.pos = this.pos.add(position3);
         this.boundingBox = this.boundingBox.move(position3);
      }
   }

   private static InputPredictor.GuessedInput guessInput(Player player2, Vec3 position) {
      double value = position.horizontalDistanceSqr();
      int value2 = !player2.onGround() ? 1 : 0;
      boolean shiftKeyDown = player2.isShiftKeyDown();
      int value3 = value >= 0.014641 ? 1 : 0;
      float value4 = 0.0F;
      float value5 = 0.0F;
      if (value > 0.0025000000000000005) {
         double yRot = getDegreesRelativeToView(position, player2.getYRot());
         double computedValue = Mth.wrapDegrees(yRot);
         boolean[] booleanValues = directionalInputForDegrees(computedValue, 20.0F);
         if (booleanValues[0] != booleanValues[1]) {
            value4 = booleanValues[0] ? 1.0F : -1.0F;
         }

         if (booleanValues[2] != booleanValues[3]) {
            value5 = booleanValues[2] ? 1.0F : -1.0F;
         }

         if (shiftKeyDown) {
            value4 *= 0.3F;
            value5 *= 0.3F;
         }
      }

      return new InputPredictor.GuessedInput(value4, value5, (value2 != 0), (value3 != 0));
   }

   private Vec3 collideLikeSkycoreClient(Vec3 position, AABB bounds) {
      if (position.lengthSqr() == 0.0) {
         return position;
      }

      Vec3 position2 = Entity.collideBoundingBox(this.player, position, bounds, this.player.level(), List.of());
      int x = position.x == position2.x && position.z == position2.z ? 0 : 1;
      int y = !this.onGround && (position.y == position2.y || !(position.y < 0.0)) ? 0 : 1;
      double value = this.player.maxUpStep();
      if (!(value <= 0.0) && y != 0 && x != 0) {
         Vec3 position3 = Entity.collideBoundingBox(this.player, new Vec3(position.x, value, position.z), bounds, this.player.level(), List.of());
         Vec3 position4 = Entity.collideBoundingBox(this.player, new Vec3(0.0, value, 0.0), bounds.expandTowards(position.x, 0.0, position.z), this.player.level(), List.of());
         Vec3 position5 = Entity.collideBoundingBox(this.player, new Vec3(position.x, 0.0, position.z), bounds.move(position4), this.player.level(), List.of()).add(position4);
         if (position4.y < value && position5.horizontalDistanceSqr() > position3.horizontalDistanceSqr()) {
            position3 = position5;
         }

         return position3.horizontalDistanceSqr() > position2.horizontalDistanceSqr()
            ? position3.add(Entity.collideBoundingBox(this.player, new Vec3(0.0, -position3.y + position.y, 0.0), bounds.move(position3), this.player.level(), List.of()))
            : position2;
      } else {
         return position2;
      }
   }

   private boolean effectStillActive(Holder<MobEffect> holder) {
      int value = 0;
      value = (byte)0;
      MobEffectInstance mobEffectInstance;
      if (this.player == null) {
         mobEffectInstance = null;
      } else {
         mobEffectInstance = this.player.getEffect(holder);
      }

      MobEffectInstance mobEffectInstance2 = mobEffectInstance;
      if (mobEffectInstance2 != null) {
         value = mobEffectInstance2.getDuration();
         if (value >= this.simulatedTicks) {
            return true;
         }
      }

      return false;
   }

   private void travel(Vec3 position) {
      double y = this.velocity.y;
      if (this.inWater && this.player.isAffectedByFluids()) {
         this.travelWater(position);
      } else if (this.lavaHeight > 0.0 && this.player.isAffectedByFluids()) {
         this.travelLava(position);
      } else if (this.fallFlying) {
         this.updateFallFlyingVelocity();
         this.move(this.velocity);
      } else {
         float friction = this.player == null
            ? 0.6F
            : this.player.level().getBlockState(BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z)).getBlock().getFriction();
         float computedValue = this.onGround ? friction * 0.91F : 0.91F;
         float yaw2 = this.onGround ? 0.1F * (0.21600002F / (friction * friction * friction)) : (this.guessedSprint ? 0.026F : 0.02F);
         this.velocity = this.velocity.add(inputToVelocity(position, yaw2, this.yaw));
         this.velocity = this.capClimbable(this.velocity);
         this.move(this.velocity);
         double y2 = this.velocity.y;
         if ((this.horizontalCollision || this.jumping) && this.isClimbable()) {
            y2 = 0.2;
         }

         if (this.effectStillActive(MobEffects.LEVITATION)) {
            MobEffectInstance effect = this.player.getEffect(MobEffects.LEVITATION);
            if (effect != null) {
               y2 += (0.05 * (effect.getAmplifier() + 1) - this.velocity.y) * 0.2;
            }
         } else if (this.player.level().isClientSide()
            && !this.player.level().isLoaded(BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z))) {
            y2 = this.pos.y > this.player.level().getMaxY() ? -0.1 : 0.0;
         } else if (!this.player.isNoGravity()) {
            double y3 = this.effectStillActive(MobEffects.SLOW_FALLING) && this.velocity.y <= 0.0 ? 0.01 : 0.08;
            y2 -= y3;
         }

         this.velocity = this.player.shouldDiscardFriction()
            ? new Vec3(this.velocity.x, y2, this.velocity.z)
            : new Vec3(this.velocity.x * computedValue, y2 * 0.98F, this.velocity.z * computedValue);
         if (this.player.getAbilities().flying && !this.player.isPassenger()) {
            this.velocity = new Vec3(this.velocity.x, y * 0.6, this.velocity.z);
            this.simulatedFallDistance = 0.0F;
         }
      }
   }

   private InputPredictor(
      Player player2,
      Input input2,
      Vec3 pos2,
      Vec3 velocity2,
      AABB boundingBox2,
      boolean onGround2,
      boolean fallFlying2,
      float yaw2,
      float pitch2,
      boolean sprinting2,
      boolean jumping2,
      int jumpingCooldown2,
      float movementForward2,
      float movementSideways2
   ) {
      this.player = player2;
      this.input = input2;
      this.pos = pos2;
      this.velocity = velocity2;
      this.boundingBox = boundingBox2;
      this.onGround = onGround2;
      this.fallFlying = fallFlying2;
      this.yaw = yaw2;
      this.pitch = pitch2;
      this.sprinting = sprinting2;
      this.jumping = jumping2;
      this.jumpingCooldown = jumpingCooldown2;
      this.movementForward = movementForward2;
      this.movementSideways = movementSideways2;
   }

   public boolean onGround() {
      return this.onGround;
   }

   private void refreshWaterState() {
      if (this.player != null && this.player.level() != null) {
         Level level = this.player.level();
         if (this.player.getVehicle() instanceof Boat boat && !boat.isUnderWater()) {
            this.inWater = false;
            this.waterHeight = 0.0;
         } else {
            AABB bounds = this.boundingBox.deflate(0.001);
            int roundedDown = Mth.floor(bounds.minX);
            int roundedUp = Mth.ceil(bounds.maxX);
            int roundedDown2 = Mth.floor(bounds.minY);
            int roundedUp2 = Mth.ceil(bounds.maxY);
            int roundedDown3 = Mth.floor(bounds.minZ);
            int roundedUp3 = Mth.ceil(bounds.maxZ);
            this.inWater = false;
            this.waterHeight = 0.0;
            if (level.hasChunksAt(roundedDown, roundedDown3, roundedUp, roundedUp3)) {
               Vec3 zERO = Vec3.ZERO;
               int value = 0;
               MutableBlockPos mutablePos = new MutableBlockPos();

               for (int index = roundedDown; index < roundedUp; index++) {
                  for (int index2 = roundedDown2; index2 < roundedUp2; index2++) {
                     for (int index3 = roundedDown3; index3 < roundedUp3; index3++) {
                        mutablePos.set(index, index2, index3);
                        FluidState fluidState = level.getFluidState(mutablePos);
                        if (fluidState.is(FluidTags.WATER)) {
                           double height = index2 + fluidState.getHeight(level, mutablePos);
                           if (!(height < bounds.minY)) {
                              this.inWater = true;
                              this.waterHeight = Math.max(height - bounds.minY, this.waterHeight);
                              Vec3 flow = fluidState.getFlow(level, mutablePos);
                              if (this.waterHeight < 0.4) {
                                 flow = flow.scale(this.waterHeight);
                              }

                              zERO = zERO.add(flow);
                              value++;
                           }
                        }
                     }
                  }
               }

               if (value > 0 && zERO.lengthSqr() > 0.0) {
                  zERO = zERO.scale(1.0 / value).scale(0.014);
                  if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && zERO.length() < 0.0045) {
                     zERO = zERO.normalize().scale(0.0045);
                  }

                  this.velocity = this.velocity.add(zERO);
               }
            }

            if (this.inWater) {
               this.simulatedFallDistance = 0.0F;
            }
         }

         double eyeHeight = this.pos.y + this.player.getEyeHeight() - 0.11111111F;
         BlockPos blockPos = BlockPos.containing(this.pos.x, eyeHeight, this.pos.z);
         FluidState fluidState2 = level.getFluidState(blockPos);
         this.eyeInWater = ((fluidState2.is(FluidTags.WATER) && blockPos.getY() + fluidState2.getHeight(level, blockPos) > eyeHeight ? 1 : 0) != 0);
      }
   }

   private static Vec3 inputToVelocity(Vec3 position, float value, float value2) {
      double lengthSquared = position.lengthSqr();
      if (lengthSquared < 1.0E-7) {
         return Vec3.ZERO;
      } else {
         Vec3 position2 = lengthSquared > 1.0 ? position.normalize() : position;
         double value3 = value;
         Vec3 position3 = position2.scale(value3);
         double computedValue = value2 * (float) (Math.PI / 180.0);
         float sine = Mth.sin(computedValue);
         float cosine = Mth.cos(computedValue);
         double x = position3.x * cosine - position3.z * sine;
         double x2 = position3.z * cosine + position3.x * sine;
         return new Vec3(x, position3.y, x2);
      }
   }

   private void updateFallFlyingVelocity() {
      if (this.player != null) {
         float y = (float)(this.yaw * Math.PI / 180.0);
         float computedValue = (float)(this.pitch * Math.PI / 180.0);
         float y2 = (float)(-this.yaw * Math.PI / 180.0);
         float cosine = Mth.cos(y2);
         float sine = Mth.sin(y2);
         float cosine2 = Mth.cos(computedValue);
         float sine2 = Mth.sin(computedValue);
         Vec3 position = new Vec3(sine * cosine2, -sine2, cosine * cosine2);
         double squareRoot = Math.sqrt(position.x * position.x + position.z * position.z);
         double value = this.velocity.horizontalDistance();
         double length = position.length();
         float computedValue2 = (float)(Math.cos(computedValue) * Math.cos(computedValue) * Math.min(1.0, length / 0.4));
         Vec3 position2 = this.velocity.add(0.0, 0.08 * (-1.0 + computedValue2 * 0.75), 0.0);
         if (position2.y < 0.0 && squareRoot > 0.0) {
            double y3 = position2.y * -0.1 * computedValue2;
            position2 = position2.add(position.x * y3 / squareRoot, y3, position.z * y3 / squareRoot);
         }

         if (computedValue < 0.0F && squareRoot > 0.0) {
            double computedValue3 = value * -Math.sin(computedValue) * 0.04;
            position2 = position2.add(-position.x * computedValue3 / squareRoot, computedValue3 * 3.2, -position.z * computedValue3 / squareRoot);
         }

         if (squareRoot > 0.0) {
            position2 = position2.add((position.x / squareRoot * value - position2.x) * 0.1, 0.0, (position.z / squareRoot * value - position2.z) * 0.1);
         }

         this.velocity = position2.multiply(0.99, 0.98, 0.99);
      }
   }

   private double fluidGravity() {
      if (this.velocity.y <= 0.0) {
         if (this.effectStillActive(MobEffects.SLOW_FALLING)) {
            return 0.01;
         }
      }

      return 0.08;
   }

   private static InputPredictor empty() {
      return new InputPredictor(
         null, Input.EMPTY, Vec3.ZERO, Vec3.ZERO, new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0), false, false, 0.0F, 0.0F, false, false, 0, 0.0F, 0.0F
      );
   }

   public AABB boundingBox() {
      return this.boundingBox;
   }

   public static InputPredictor simulateOtherPlayer(Player player2, int value) {
      if (player2 == null) {
         return empty();
      }

      Vec3 position = player2.position().subtract(player2.xo, player2.yo, player2.zo);
      InputPredictor.GuessedInput guessedInput = guessInput(player2, position);
      InputPredictor inputPredictor = new InputPredictor(
         player2,
         Input.EMPTY,
         player2.position(),
         position,
         player2.getBoundingBox(),
         player2.onGround(),
         player2.isFallFlying(),
         player2.getYRot(),
         player2.getXRot(),
         player2.isSprinting(),
         guessedInput.jump,
         0,
         guessedInput.forward,
         guessedInput.sideways
      );
      inputPredictor.guessedSprint = guessedInput.sprint;
      inputPredictor.horizontalCollision = player2.horizontalCollision;
      inputPredictor.simulatedFallDistance = (float)player2.fallDistance;
      inputPredictor.inWater = player2.isInWater();
      inputPredictor.swimming = player2.isSwimming();
      inputPredictor.lavaHeight = player2.getFluidHeight(FluidTags.LAVA);

      for (int value2 = 0; value2 < value; value2++) {
         inputPredictor.tickOther();
      }

      return inputPredictor;
   }

   private static double getDegreesRelativeToView(Vec3 position, float value) {
      float angleRadians = (float)Math.atan2(-position.x, position.z);
      double computedValue = Mth.wrapDegrees(value);
      double angleRadians2 = Math.toRadians(computedValue);
      double computedValue2 = Mth.wrapDegrees(angleRadians - angleRadians2);
      return Math.toDegrees(computedValue2);
   }

   public static InputPredictor simulateLocalPlayer(int value) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return empty();
      }

      Input input2 = instance.input == null ? Input.EMPTY : instance.input.keyPresses;
      float delta = 0.0F;
      float delta2 = 0.0F;
      if (instance.input != null) {
         delta = instance.input.getMoveVector().y;
         delta2 = instance.input.getMoveVector().x;
      }

      InputPredictor inputPredictor = new InputPredictor(
         instance,
         input2,
         instance.position(),
         instance.getDeltaMovement(),
         instance.getBoundingBox(),
         instance.onGround(),
         instance.isFallFlying(),
         instance.getYRot(),
         instance.getXRot(),
         instance.isSprinting(),
         input2.jump(),
         0,
         delta,
         delta2
      );

      for (int value2 = 0; value2 < value; value2++) {
         inputPredictor.tickLocal();
      }

      return inputPredictor;
   }

   private static boolean[] directionalInputForDegrees(double value, float value2) {
      byte flag = 0;
      byte flag2 = 0;
      byte flag3 = 0;
      byte flag4 = 0;
      if (value >= -90.0F + value2 && value <= 90.0F - value2) {
         flag = 1;
      } else if (value < -90.0F - value2 || value > 90.0F + value2) {
         flag2 = 1;
      }

      if (value >= 0.0F + value2 && value <= 180.0F - value2) {
         flag4 = 1;
      } else if (value >= -180.0F + value2 && value <= 0.0F - value2) {
         flag3 = 1;
      }

      return new boolean[]{(flag != 0), (flag2 != 0), (flag3 != 0), (flag4 != 0)};
   }

   private void tickOther() {
      this.simulatedTicks++;
      if (!(this.pos.y <= -70.0)) {
         this.refreshWaterState();
         boolean swimming2 = this.swimming;
         int value = !this.player.isShiftKeyDown()
               && !this.player.isUsingItem()
               && !this.player.isPassenger()
               && !this.horizontalCollision
               && (!this.inWater || this.eyeInWater)
            ? 0
            : 1;
         if (!this.sprinting || !(this.movementForward <= 0.0F) && value == 0) {
            if (!this.sprinting && this.guessedSprint && this.movementForward > 0.8F && value == 0) {
               this.sprinting = true;
            }
         } else {
            this.sprinting = false;
         }

         this.swimming = this.sprinting && this.inWater && !this.player.isPassenger()
            && (swimming2 || this.eyeInWater && this.player.level().getFluidState(BlockPos.containing(this.pos)).is(FluidTags.WATER));
         if (this.jumpingCooldown > 0) {
            this.jumpingCooldown--;
         }

         if (Math.abs(this.velocity.x) < 0.003) {
            this.velocity = new Vec3(0.0, this.velocity.y, this.velocity.z);
         }

         if (Math.abs(this.velocity.y) < 0.003) {
            this.velocity = new Vec3(this.velocity.x, 0.0, this.velocity.z);
         }

         if (Math.abs(this.velocity.z) < 0.003) {
            this.velocity = new Vec3(this.velocity.x, this.velocity.y, 0.0);
         }

         if (this.onGround) {
            this.fallFlying = false;
         }

         if (this.jumping && this.inWater && this.waterHeight > (this.player.getEyeHeight() < 0.4F ? 0.0 : 0.4)) {
            this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
         } else if (this.jumping && this.lavaHeight > (this.player.getEyeHeight() < 0.4F ? 0.0 : 0.4)) {
            this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
         } else if (this.jumping && this.onGround && this.jumpingCooldown == 0) {
            float value2 = 1.0F;
            if (this.player != null) {
               Level level = this.player.level();
               float jumpFactor = level.getBlockState(BlockPos.containing(this.pos)).getBlock().getJumpFactor();
               float jumpFactor2 = level.getBlockState(BlockPos.containing(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z)).getBlock().getJumpFactor();
               value2 = jumpFactor == 1.0F ? jumpFactor2 : jumpFactor;
            }

            double computedValue = 0.42F * value2;
            if (this.effectStillActive(MobEffects.JUMP_BOOST)) {
               MobEffectInstance effect = this.player.getEffect(MobEffects.JUMP_BOOST);
               computedValue += 0.1F * (effect.getAmplifier() + 1);
            }

            this.velocity = new Vec3(this.velocity.x, computedValue, this.velocity.z);
            if (this.sprinting) {
               float angleRadians = (float)Math.toRadians(this.yaw);
               this.velocity = this.velocity.add(-Mth.sin(angleRadians) * 0.2, 0.0, Mth.cos(angleRadians) * 0.2);
            }

            this.jumpingCooldown = 10;
            this.onGround = false;
         }

         float computedValue2 = this.movementSideways * 0.98F;
         float computedValue3 = this.movementForward * 0.98F;
         this.travel(new Vec3(computedValue2, 0.0, computedValue3));
      }
   }

   private void travelLava(Vec3 position) {
      double y = this.pos.y;
      double value = this.fluidGravity();
      int y2 = this.velocity.y <= 0.0 ? 1 : 0;
      this.velocity = this.capClimbable(this.velocity.add(inputToVelocity(position, 0.02F, this.yaw)));
      this.move(this.velocity);
      if (this.lavaHeight <= (this.player.getEyeHeight() < 0.4F ? 0.0 : 0.4)) {
         this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
         this.velocity = this.player.getFluidFallingAdjustedMovement(value, (y2 != 0), this.velocity);
      } else {
         this.velocity = this.velocity.scale(0.5);
      }

      if (!this.player.isNoGravity()) {
         this.velocity = this.velocity.add(0.0, -value / 4.0, 0.0);
      }

      Vec3 velocity2 = this.velocity;
      if (this.horizontalCollision && this.player.level().noCollision(this.player, this.boundingBox.move(velocity2.x, velocity2.y + 0.6 - this.pos.y + y, velocity2.z))) {
         this.velocity = new Vec3(velocity2.x, 0.3, velocity2.z);
      }
   }

   private Vec3 clipSneakingEdge(Vec3 position) {
      if (this.player != null
         && this.player.isShiftKeyDown()
         && !(position.y > 0.0)
         && (
            this.onGround
               || this.simulatedFallDistance < 0.5F
                  && !this.player.level().noCollision(this.player, this.boundingBox.move(0.0, this.simulatedFallDistance - 0.5, 0.0))
         )) {
         double x = position.x;
         double z = position.z;

         Level level;
         for (level = this.player.level();
            x != 0.0 && level.noCollision(this.player, this.boundingBox.move(x, -0.5, 0.0));
            x += x > 0.0 ? -0.05 : 0.05
         ) {
            if (x < 0.05 && x >= -0.05) {
               x = 0.0;
               break;
            }
         }

         while (z != 0.0 && level.noCollision(this.player, this.boundingBox.move(0.0, -0.5, z))) {
            if (z < 0.05 && z >= -0.05) {
               z = 0.0;
               break;
            }

            z += z > 0.0 ? -0.05 : 0.05;
         }

         while (x != 0.0 && z != 0.0 && level.noCollision(this.player, this.boundingBox.move(x, -0.5, z))) {
            x = x < 0.05 && x >= -0.05 ? 0.0 : (x > 0.0 ? x - 0.05 : x + 0.05);
            if (z < 0.05 && z >= -0.05) {
               z = 0.0;
               break;
            }

            z += z > 0.0 ? -0.05 : 0.05;
         }

         return x == position.x && z == position.z ? position : new Vec3(x, position.y, z);
      } else {
         return position;
      }
   }

   public record GuessedInput(float forward, float sideways, boolean jump, boolean sprint) {
      public boolean jump() {
         return this.jump;
      }

      public boolean sprint() {
         return this.sprint;
      }

      public float forward() {
         return this.forward;
      }

      public float sideways() {
         return this.sideways;
      }
   }
}
