package deluxe.client.module.movement;

import deluxe.client.util.PlayerUtils;

import deluxe.mixin.EntityMixin2;
import deluxe.mixin.LivingEntityMixin;
import deluxe.client.MinecraftAccess;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;

public class SimulatedPlayerInput implements SimulatedMovementAccess, MinecraftAccess {
   public final PlayerEntity field04604;
   public final SimulatedPlayerInput.Nested0387 field04605;
   public Vec3d field04606;
   public Vec3d field04607;
   public Box field04608;
   public float field04609;
   public float field04610;
   public boolean field04611;
   public float field04612;
   public int field04613;
   public boolean field04614;
   public boolean field04615;
   public boolean field04616;
   public boolean field04617;
   public boolean field04618;
   public boolean field04619;
   public boolean field04620;
   public boolean field04621;
   private final Object2DoubleMap<TagKey<Fluid>> field04622;
   private final HashSet<TagKey<Fluid>> field04623;
   private int field04624 = 0;
   private boolean field04625 = false;

   private Vec3d method1484(Vec3d vec3d) {
      Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.field04606);
      List<VoxelShape> local2188_3 = Collections.emptyList();
      Vec3d vec3d2;
      if (vec3d.lengthSquared() == 0.0) {
         vec3d2 = vec3d;
      } else {
         vec3d2 = Entity.adjustMovementForCollisions(this.field04604, vec3d, box, this.field04604.getEntityWorld(), local2188_3);
      }

      boolean local2188_5 = vec3d.x != vec3d2.x;
      boolean local2188_6 = vec3d.y != vec3d2.y;
      boolean local2188_7 = vec3d.z != vec3d2.z;
      boolean local2188_8 = this.field04616 || local2188_6 && vec3d.y < 0.0;
      if (this.field04604.getStepHeight() > 0.0F && local2188_8 && (local2188_5 || local2188_7)) {
         Vec3d vec3d3 = Entity.adjustMovementForCollisions(
            this.field04604,
            new Vec3d(vec3d.x, this.field04604.getStepHeight(), vec3d.z),
            box,
            this.field04604.getEntityWorld(),
            local2188_3
         );
         Vec3d vec3d4 = Entity.adjustMovementForCollisions(
            this.field04604,
            new Vec3d(0.0, this.field04604.getStepHeight(), 0.0),
            box.stretch(vec3d.x, 0.0, vec3d.z),
            this.field04604.getEntityWorld(),
            local2188_3
         );
         Vec3d vec3d5 = Entity.adjustMovementForCollisions(
               this.field04604, new Vec3d(vec3d.x, 0.0, vec3d.z), box.offset(vec3d4), this.field04604.getEntityWorld(), local2188_3
            )
            .add(vec3d4);
         if (vec3d4.y < this.field04604.getStepHeight() && vec3d5.horizontalLengthSquared() > vec3d3.horizontalLengthSquared()) {
            vec3d3 = vec3d5;
         }

         if (vec3d3.horizontalLengthSquared() > vec3d2.horizontalLengthSquared()) {
            return vec3d3.add(
               Entity.adjustMovementForCollisions(
                  this.field04604,
                  new Vec3d(0.0, -vec3d3.y + vec3d.y, 0.0),
                  box.offset(vec3d3),
                  this.field04604.getEntityWorld(),
                  local2188_3
               )
            );
         }
      }

      return vec3d2;
   }

   private void method1485(Vec3d vec3d) {
      if (this.field04620 && !this.field04604.hasVehicle()) {
         double local2189_2 = this.method1525().y;
         double local2189_4 = local2189_2 < -0.2 ? 0.085 : 0.06;
         BlockPos blockPos = new BlockPos(
            MathHelper.floor(this.field04606.x), MathHelper.floor(this.field04606.y + 1.0 - 0.1), MathHelper.floor(this.field04606.z)
         );
         if (local2189_2 <= 0.0 || this.field04605.field04729.jump() || !this.field04604.getEntityWorld().getBlockState(blockPos).getFluidState().isEmpty()) {
            this.field04607 = this.field04607.add(0.0, (local2189_2 - this.field04607.y) * local2189_4, 0.0);
         }
      }

      double local2189_2 = this.field04607.y;
      double local2189_4 = 0.08;
      boolean local2189_6 = this.field04607.y <= 0.0;
      if (this.field04607.y <= 0.0 && this.method1516(StatusEffects.SLOW_FALLING)) {
         local2189_4 = 0.01;
         this.method1510();
      }

      if (this.method1529() && this.field04604.shouldSwimInFluids()) {
         double local2189_7 = this.field04606.y;
         float local2189_9 = this.method1512() ? 0.9F : 0.8F;
         float local2189_10 = 0.02F;
         float local2189_11 = (float)this.method1513(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.field04616) {
            local2189_11 *= 0.5F;
         }

         if (local2189_11 > 0.0F) {
            local2189_9 += (0.54600006F - local2189_9) * local2189_11 / 3.0F;
            local2189_10 += (this.method1515() - local2189_10) * local2189_11 / 3.0F;
         }

         if (this.method1516(StatusEffects.DOLPHINS_GRACE)) {
            local2189_9 = 0.96F;
         }

         this.method1486(local2189_10, vec3d);
         this.method1488(this.field04607);
         Vec3d vec3d2 = this.field04607;
         if (this.field04617 && this.method1517()) {
            vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
         }

         this.field04607 = vec3d2.multiply(local2189_9, 0.8, local2189_9);
         Vec3d vec3d3 = this.field04604.applyFluidMovingSpeed(local2189_4, local2189_6, this.field04607);
         this.field04607 = vec3d3;
         if (this.field04617 && this.method1499(vec3d3.x, vec3d3.y + 0.6 - this.field04606.y + local2189_7, vec3d3.z)) {
            this.field04607 = new Vec3d(vec3d3.x, 0.3, vec3d3.z);
         }
      } else if (this.method1494() && this.field04604.shouldSwimInFluids()) {
         double local2189_7 = this.field04606.y;
         this.method1486(0.02F, vec3d);
         this.method1488(this.field04607);
         if (this.method1519(FluidTags.LAVA) <= this.method1506()) {
            this.field04607 = this.field04607.multiply(0.5, 0.8, 0.5);
            this.field04607 = this.field04604.applyFluidMovingSpeed(local2189_4, local2189_6, this.field04607);
         } else {
            this.field04607 = this.field04607.multiply(0.5);
         }

         if (!this.field04604.hasNoGravity()) {
            this.field04607 = this.field04607.add(0.0, -local2189_4 / 4.0, 0.0);
         }

         if (this.field04617 && this.method1499(this.field04607.x, this.field04607.y + 0.6 - this.field04606.y + local2189_7, this.field04607.z)) {
            this.field04607 = new Vec3d(this.field04607.x, 0.3, this.field04607.z);
         }
      } else if (this.field04615) {
         Vec3d vec3d4 = this.field04607;
         if (vec3d4.y > -0.5) {
            this.field04612 = 1.0F;
         }

         Vec3d vec3d5 = this.method1525();
         float local2189_11 = this.field04610 * (float) (Math.PI / 180.0);
         double local2189_12 = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
         double local2189_14 = this.field04607.horizontalLength();
         double local2189_16 = vec3d5.length();
         float local2189_18 = MathHelper.cos(local2189_11);
         local2189_18 = (float)(local2189_18 * (local2189_18 * Math.min(1.0, local2189_16 / 0.4)));
         vec3d4 = this.field04607.add(0.0, local2189_4 * (-1.0 + local2189_18 * 0.75), 0.0);
         if (vec3d4.y < 0.0 && local2189_12 > 0.0) {
            double local2189_7 = vec3d4.y * -0.1 * local2189_18;
            vec3d4 = vec3d4.add(vec3d5.x * local2189_7 / local2189_12, local2189_7, vec3d5.z * local2189_7 / local2189_12);
         }

         if (local2189_11 < 0.0F && local2189_12 > 0.0) {
            double local2189_7 = local2189_14 * -MathHelper.sin(local2189_11) * 0.04;
            vec3d4 = vec3d4.add(-vec3d5.x * local2189_7 / local2189_12, local2189_7 * 3.2, -vec3d5.z * local2189_7 / local2189_12);
         }

         if (local2189_12 > 0.0) {
            vec3d4 = vec3d4.add(
               (vec3d5.x / local2189_12 * local2189_14 - vec3d4.x) * 0.1, 0.0, (vec3d5.z / local2189_12 * local2189_14 - vec3d4.z) * 0.1
            );
         }

         this.field04607 = vec3d4.multiply(0.99, 0.98, 0.99);
         this.method1488(this.field04607);
      } else {
         BlockPos blockPos2 = this.method1495();
         float slipperiness = this.field04604.getEntityWorld().getBlockState(blockPos2).getBlock().getSlipperiness();
         float local2189_9 = this.field04616 ? slipperiness * 0.91F : 0.91F;
         Vec3d vec3d6 = this.method1489(vec3d, slipperiness);
         double local2189_11 = vec3d6.y;
         if (this.method1516(StatusEffects.LEVITATION)) {
            StatusEffectInstance statusEffectInstance = this.method1487(StatusEffects.LEVITATION);
            if (statusEffectInstance != null) {
               local2189_11 += (0.05 * (statusEffectInstance.getAmplifier() + 1) - vec3d6.y) * 0.2;
            }
         } else if (this.field04604.getEntityWorld().isClient() && !this.field04604.getEntityWorld().isChunkLoaded(blockPos2)) {
            local2189_11 = this.field04606.y > this.field04604.getEntityWorld().getBottomY() ? -0.1 : 0.0;
         } else if (!this.field04604.hasNoGravity()) {
            local2189_11 -= local2189_4;
         }

         if (this.field04604.hasNoDrag()) {
            this.field04607 = new Vec3d(vec3d6.x, local2189_11, vec3d6.z);
         } else {
            this.field04607 = new Vec3d(vec3d6.x * local2189_9, local2189_11 * 0.98F, vec3d6.z * local2189_9);
         }
      }

      if (this.field04604.getAbilities().flying && !this.field04604.hasVehicle()) {
         this.field04607 = new Vec3d(this.field04607.x, local2189_2 * 0.6, this.field04607.z);
         this.method1510();
      }
   }

   public SimulatedPlayerInput(
      PlayerEntity playerEntity,
      SimulatedPlayerInput.Nested0387 local2190_2,
      Vec3d vec3d,
      Vec3d vec3d2,
      Box box,
      float local2190_6,
      float local2190_7,
      boolean local2190_8,
      float local2190_9,
      int local2190_10,
      boolean local2190_11,
      boolean local2190_12,
      boolean local2190_13,
      boolean local2190_14,
      boolean local2190_15,
      boolean local2190_16,
      boolean local2190_17,
      boolean local2190_18,
      Object2DoubleMap<TagKey<Fluid>> local2190_19,
      HashSet<TagKey<Fluid>> local2190_20
   ) {
      this.field04604 = playerEntity;
      this.field04605 = local2190_2;
      this.field04606 = vec3d;
      this.field04607 = vec3d2;
      this.field04608 = box;
      this.field04609 = local2190_6;
      this.field04610 = local2190_7;
      this.field04611 = local2190_8;
      this.field04612 = local2190_9;
      this.field04613 = local2190_10;
      this.field04614 = local2190_11;
      this.field04615 = local2190_12;
      this.field04616 = local2190_13;
      this.field04617 = local2190_14;
      this.field04618 = local2190_15;
      this.field04619 = local2190_16;
      this.field04620 = local2190_17;
      this.field04621 = local2190_18;
      this.field04622 = local2190_19;
      this.field04623 = local2190_20;
   }

   private void method1486(float local2191_1, Vec3d vec3d) {
      if (!(vec3d.lengthSquared() < 1.0E-7)) {
         Vec3d vec3d2 = (vec3d.lengthSquared() > 1.0 ? vec3d.normalize() : vec3d).multiply(local2191_1);
         this.field04607 = this.field04607
            .add(
               vec3d2.x * MathHelper.cos(this.field04609 * (float) (Math.PI / 180.0))
                  - vec3d2.z * MathHelper.sin(this.field04609 * (float) (Math.PI / 180.0)),
               vec3d2.y,
               vec3d2.z * MathHelper.cos(this.field04609 * (float) (Math.PI / 180.0))
                  + vec3d2.x * MathHelper.sin(this.field04609 * (float) (Math.PI / 180.0))
            );
      }
   }

   private StatusEffectInstance method1487(RegistryEntry<StatusEffect> local2192_1) {
      StatusEffectInstance statusEffect = this.field04604.getStatusEffect(local2192_1);
      return statusEffect != null && statusEffect.getDuration() >= this.field04624 ? statusEffect : null;
   }

   private void method1488(Vec3d vec3d) {
      Vec3d vec3d2 = vec3d;
      vec3d2 = this.method1492(vec3d2);
      Vec3d vec3d3 = this.method1484(vec3d2);
      if (vec3d3.lengthSquared() > 1.0E-7) {
         this.field04606 = this.field04606.add(vec3d3);
         this.field04608 = this.field04604.getDimensions(this.field04604.getPose()).getBoxAt(this.field04606);
      }

      boolean local2193_4 = !MathHelper.approximatelyEquals(vec3d.x, vec3d3.x);
      boolean local2193_5 = !MathHelper.approximatelyEquals(vec3d.z, vec3d3.z);
      this.field04617 = local2193_4 || local2193_5;
      this.field04618 = vec3d.y != vec3d3.y;
      this.field04616 = this.field04618 && vec3d.y < 0.0;
      if (!this.method1529()) {
         this.method1508();
      }

      if (this.field04616) {
         this.method1510();
      } else if (vec3d.y < 0.0) {
         this.field04612 = this.field04612 - (float)vec3d.y;
      }

      Vec3d vec3d4 = this.field04607;
      if (this.field04617 || this.field04618) {
         this.field04607 = new Vec3d(local2193_4 ? 0.0 : vec3d4.x, this.field04616 ? 0.0 : vec3d4.y, local2193_5 ? 0.0 : vec3d4.z);
      }
   }

   private Vec3d method1489(Vec3d vec3d, float local2194_2) {
      this.method1486(this.method1514(local2194_2), vec3d);
      this.field04607 = this.method1518(this.field04607);
      this.method1488(this.field04607);
      Vec3d vec3d2 = this.field04607;
      BlockPos blockPos = this.method1505(this.field04606);
      BlockState blockState = this.method1504(blockPos);
      if ((this.field04617 || this.field04614)
         && (this.method1517() || blockState != null && blockState.isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this.field04604))) {
         vec3d2 = new Vec3d(vec3d2.x, 0.2, vec3d2.z);
      }

      return vec3d2;
   }

   private float method1490() {
      return 0.42F * this.method1502() + this.method1507();
   }

   public static SimulatedPlayerInput method1491(int local2196_0) {
      SimulatedPlayerInput simulatedPlayerInput = method1522(SimulatedPlayerInput.Nested0387.fromClientPlayer(mc.player.input.playerInput));

      for (int index = 0; index < local2196_0; index++) {
         simulatedPlayerInput.method1509();
      }

      return simulatedPlayerInput;
   }

   private Vec3d method1492(Vec3d vec3d) {
      if (vec3d.y <= 0.0 && this.method1524()) {
         double local2197_2 = vec3d.x;
         double local2197_4 = vec3d.z;

         double local2197_6;
         for (local2197_6 = 0.05;
            local2197_2 != 0.0 && this.field04604.getEntityWorld().isSpaceEmpty(this.field04604, this.field04608.offset(local2197_2, -0.5, 0.0));
            local2197_2 += local2197_2 > 0.0 ? -local2197_6 : local2197_6
         ) {
            if (local2197_2 < local2197_6 && local2197_2 >= -local2197_6) {
               local2197_2 = 0.0;
               break;
            }
         }

         while (local2197_4 != 0.0 && this.field04604.getEntityWorld().isSpaceEmpty(this.field04604, this.field04608.offset(0.0, -0.5, local2197_4))) {
            if (local2197_4 < local2197_6 && local2197_4 >= -local2197_6) {
               local2197_4 = 0.0;
               break;
            }

            local2197_4 += local2197_4 > 0.0 ? -local2197_6 : local2197_6;
         }

         while (
            local2197_2 != 0.0
               && local2197_4 != 0.0
               && this.field04604.getEntityWorld().isSpaceEmpty(this.field04604, this.field04608.offset(local2197_2, -0.5, local2197_4))
         ) {
            local2197_2 = local2197_2 < local2197_6 && local2197_2 >= -local2197_6
               ? 0.0
               : (local2197_2 > 0.0 ? local2197_2 - local2197_6 : local2197_2 + local2197_6);
            if (local2197_4 < local2197_6 && local2197_4 >= -local2197_6) {
               local2197_4 = 0.0;
               break;
            }

            local2197_4 += local2197_4 > 0.0 ? -local2197_6 : local2197_6;
         }

         if (vec3d.x != local2197_2 || vec3d.z != local2197_4) {
            this.field04625 = true;
         }

         if (this.method1530()) {
            vec3d = new Vec3d(local2197_2, vec3d.y, local2197_4);
         }
      }

      return vec3d;
   }

   private boolean method1493(TagKey<Fluid> local2198_1, double local2198_2) {
      if (this.method1521()) {
         return false;
      }

      Box box = this.field04608.contract(0.001);
      int local2198_5 = MathHelper.floor(box.minX);
      int local2198_6 = MathHelper.ceil(box.maxX);
      int local2198_7 = MathHelper.floor(box.minY);
      int local2198_8 = MathHelper.ceil(box.maxY);
      int local2198_9 = MathHelper.floor(box.minZ);
      int local2198_10 = MathHelper.ceil(box.maxZ);
      double local2198_11 = 0.0;
      boolean local2198_13 = true;
      boolean local2198_14 = false;
      Vec3d vec3d = Vec3d.ZERO;
      int local2198_16 = 0;
      Mutable mutable = new Mutable();

      for (int index = local2198_5; index < local2198_6; index++) {
         for (int index2 = local2198_7; index2 < local2198_8; index2++) {
            for (int index3 = local2198_9; index3 < local2198_10; index3++) {
               mutable.set(index, index2, index3);
               FluidState fluidState = this.field04604.getEntityWorld().getFluidState(mutable);
               if (fluidState.isIn(local2198_1)) {
                  double local2198_22 = index2 + fluidState.getHeight(this.field04604.getEntityWorld(), mutable);
                  if (local2198_22 >= box.minY) {
                     local2198_14 = true;
                     local2198_11 = Math.max(local2198_22 - box.minY, local2198_11);
                     if (local2198_13) {
                        Vec3d vec3d2 = fluidState.getVelocity(this.field04604.getEntityWorld(), mutable);
                        if (local2198_11 < 0.4) {
                           vec3d2 = vec3d2.multiply(local2198_11);
                        }

                        vec3d = vec3d.add(vec3d2);
                        local2198_16++;
                     }
                  }
               }
            }
         }
      }

      if (vec3d.length() > 0.0) {
         if (local2198_16 > 0) {
            vec3d = vec3d.multiply(1.0 / local2198_16);
         }

         vec3d = vec3d.multiply(local2198_2);
         if (Math.abs(this.field04607.x) < 0.003 && Math.abs(this.field04607.z) < 0.003 && vec3d.length() < 0.0045) {
            vec3d = vec3d.normalize().multiply(0.0045);
         }

         this.field04607 = this.field04607.add(vec3d);
      }

      this.field04622.put(local2198_1, local2198_11);
      return local2198_14;
   }

   public boolean method1494() {
      return this.field04622.getDouble(FluidTags.LAVA) > 0.0;
   }

   private BlockPos method1495() {
      return BlockPos.ofFloored(this.field04606.x, this.field04608.minY - 0.5000001, this.field04606.z);
   }

   private void method1496() {
      this.field04621 = this.field04623.contains(FluidTags.WATER);
      this.field04623.clear();
      double local2201_1 = this.method1523() - 0.11111111F;
      if (!(
         this.field04604.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity
            && !abstractBoatEntity.isSubmergedInWater()
            && abstractBoatEntity.getBoundingBox().maxY >= local2201_1
            && abstractBoatEntity.getBoundingBox().minY <= local2201_1
      )) {
         BlockPos blockPos = BlockPos.ofFloored(this.field04606.x, local2201_1, this.field04606.z);
         FluidState fluidState = this.field04604.getEntityWorld().getFluidState(blockPos);
         double local2201_5 = blockPos.getY() + fluidState.getHeight(this.field04604.getEntityWorld(), blockPos);
         if (local2201_5 > local2201_1) {
            this.field04623.addAll(fluidState.streamTags().toList());
         }
      }
   }

   public static SimulatedPlayerInput method1497(PlayerEntity playerEntity, int local2202_1) {
      SimulatedPlayerInput simulatedPlayerInput = method1520(playerEntity, SimulatedPlayerInput.Nested0387.guessInput(playerEntity));

      for (int index = 0; index < local2202_1; index++) {
         simulatedPlayerInput.method1509();
      }

      return simulatedPlayerInput;
   }

   @Override
   public Vec3d method1498() {
      return this.field04604.getEntityPos();
   }

   private boolean method1499(double local2204_1, double local2204_3, double local2204_5) {
      return this.method1532(this.field04608.offset(local2204_1, local2204_3, local2204_5));
   }

   private boolean method1500(BlockPos blockPos, BlockState blockState) {
      if (!blockState.get(TrapdoorBlock.OPEN)) {
         return false;
      }

      BlockState blockState2 = this.field04604.getEntityWorld().getBlockState(blockPos.down());
      return blockState2.isOf(Blocks.LADDER) && blockState2.get(LadderBlock.FACING).equals(blockState.get(TrapdoorBlock.FACING));
   }

   private void method1501(TagKey<Fluid> local2206_1) {
      this.field04607 = this.field04607.add(0.0, 0.04F, 0.0);
   }

   private float method1502() {
      float local2207_1 = 0.0F;
      Block block = this.method1504(this.method1505(this.field04606)).getBlock();
      if (block != null) {
         local2207_1 = block.getJumpVelocityMultiplier();
      }

      float local2207_3 = 0.0F;
      Block block2 = this.method1504(this.method1495()).getBlock();
      if (block2 != null) {
         local2207_3 = block2.getJumpVelocityMultiplier();
      }

      return local2207_1 == 1.0F ? local2207_3 : local2207_1;
   }

   private Vec3d method1503(float local2208_1, float local2208_2) {
      float local2208_3 = (float)(local2208_1 * Math.PI / 180.0);
      float local2208_4 = (float)(-local2208_2 * Math.PI / 180.0);
      float local2208_5 = MathHelper.cos(local2208_4);
      float local2208_6 = MathHelper.sin(local2208_4);
      float local2208_7 = MathHelper.cos(local2208_3);
      float local2208_8 = MathHelper.sin(local2208_3);
      return new Vec3d(local2208_6 * local2208_7, -local2208_8, local2208_5 * local2208_7);
   }

   public BlockState method1504(BlockPos blockPos) {
      return this.field04604.getEntityWorld().getBlockState(blockPos);
   }

   public BlockPos method1505(Vec3d vec3d) {
      return new BlockPos(MathHelper.floor(vec3d.x), MathHelper.floor(vec3d.y), MathHelper.floor(vec3d.z));
   }

   private double method1506() {
      return this.field04604.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   private float method1507() {
      if (this.method1516(StatusEffects.JUMP_BOOST)) {
         StatusEffectInstance statusEffectInstance = this.method1487(StatusEffects.JUMP_BOOST);
         return 0.1F * (statusEffectInstance.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private void method1508() {
      if (this.field04604.getVehicle() instanceof AbstractBoatEntity abstractBoatEntity && !abstractBoatEntity.isSubmergedInWater()) {
         this.field04619 = false;
      } else {
         if (this.method1493(FluidTags.WATER, 0.014)) {
            this.method1510();
            this.field04619 = true;
         } else {
            this.field04619 = false;
         }
      }
   }

   @Override
   public void method1509() {
      this.field04624++;
      this.field04625 = false;
      if (!(this.field04606.y <= -70.0)) {
         this.field04605.update();
         this.method1508();
         this.method1496();
         this.method1511();
         if (this.field04613 > 0) {
            this.field04613--;
         }

         this.field04614 = this.field04605.field04729.jump();
         double local2214_1 = this.field04607.x;
         double local2214_3 = this.field04607.y;
         double local2214_5 = this.field04607.z;
         if (Math.abs(this.field04607.x) < 0.003) {
            local2214_1 = 0.0;
         }

         if (Math.abs(this.field04607.y) < 0.003) {
            local2214_3 = 0.0;
         }

         if (Math.abs(this.field04607.z) < 0.003) {
            local2214_5 = 0.0;
         }

         if (this.field04616) {
            this.field04615 = false;
         }

         this.field04607 = new Vec3d(local2214_1, local2214_3, local2214_5);
         if (this.field04614) {
            double local2214_7 = this.method1494() ? this.method1519(FluidTags.LAVA) : this.method1519(FluidTags.WATER);
            boolean local2214_9 = this.method1529() && local2214_7 > 0.0;
            double local2214_10 = this.method1506();
            if (!local2214_9 || this.field04616 && !(local2214_7 > local2214_10)) {
               if (!this.method1494() || this.field04616 && !(local2214_7 > local2214_10)) {
                  if ((this.field04616 || local2214_9 && local2214_7 <= local2214_10) && this.field04613 == 0) {
                     this.method1527();
                     if (this.field04604.equals(mc.player)) {
                        this.field04613 = 10;
                     }
                  }
               } else {
                  this.method1501(FluidTags.LAVA);
               }
            } else {
               this.method1501(FluidTags.WATER);
            }
         }

         float local2214_7 = this.field04605.field04728 * 0.98F;
         float local2214_8 = this.field04605.field04727 * 0.98F;
         float local2214_9 = 0.0F;
         if (this.method1516(StatusEffects.SLOW_FALLING) || this.method1516(StatusEffects.LEVITATION)) {
            this.method1510();
         }

         this.method1485(new Vec3d(local2214_7, local2214_9, local2214_8));
      }
   }

   private void method1510() {
      this.field04612 = 0.0F;
   }

   private void method1511() {
      if (this.field04620) {
         this.field04620 = this.method1512() && this.method1529() && !this.field04604.hasVehicle();
      } else {
         this.field04620 = this.method1512()
            && this.method1531()
            && !this.field04604.hasVehicle()
            && this.field04604.getEntityWorld().getFluidState(this.method1505(this.field04606)).isIn(FluidTags.WATER);
      }
   }

   private boolean method1512() {
      return this.field04611;
   }

   public double method1513(RegistryEntry<EntityAttribute> local2218_1) {
      return this.field04604.getAttributes().getValue(local2218_1);
   }

   private float method1514(float local2219_1) {
      return this.field04616 ? this.method1515() * (0.21600002F / (local2219_1 * local2219_1 * local2219_1)) : this.method1526();
   }

   private float method1515() {
      return 0.1F;
   }

   public boolean method1516(RegistryEntry<StatusEffect> local2221_1) {
      StatusEffectInstance statusEffect = this.field04604.getStatusEffect(local2221_1);
      return statusEffect != null && statusEffect.getDuration() >= this.field04624;
   }

   public boolean method1517() {
      BlockPos blockPos = this.method1505(this.field04606);
      BlockState blockState = this.method1504(blockPos);
      return blockState.isIn(BlockTags.CLIMBABLE) ? true : blockState.getBlock() instanceof TrapdoorBlock && this.method1500(blockPos, blockState);
   }

   private Vec3d method1518(Vec3d vec3d) {
      if (!this.method1517()) {
         return vec3d;
      }

      this.method1510();
      double local2223_2 = MathHelper.clamp(vec3d.x, -0.15F, 0.15F);
      double local2223_4 = MathHelper.clamp(vec3d.z, -0.15F, 0.15F);
      double local2223_6 = Math.max(vec3d.y, -0.15F);
      if (local2223_6 < 0.0 && !this.method1504(this.method1505(this.field04606)).isOf(Blocks.SCAFFOLDING) && this.field04604.isHoldingOntoLadder()) {
         local2223_6 = 0.0;
      }

      return new Vec3d(local2223_2, local2223_6, local2223_4);
   }

   private double method1519(TagKey<Fluid> local2224_1) {
      return this.field04622.getDouble(local2224_1);
   }

   public static SimulatedPlayerInput method1520(PlayerEntity playerEntity, SimulatedPlayerInput.Nested0387 local2225_1) {
      return new SimulatedPlayerInput(
         playerEntity,
         local2225_1,
         playerEntity.getEntityPos(),
         playerEntity.getEntityPos().subtract(new Vec3d(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ)),
         playerEntity.getBoundingBox(),
         playerEntity.getYaw(),
         playerEntity.getPitch(),
         playerEntity.isSprinting(),
         (float)playerEntity.fallDistance,
         ((LivingEntityMixin)playerEntity).getNoJumpDelay(),
         ((LivingEntityMixin)playerEntity).getJumping(),
         playerEntity.isGliding(),
         playerEntity.isOnGround(),
         playerEntity.horizontalCollision,
         playerEntity.verticalCollision,
         playerEntity.isTouchingWater(),
         playerEntity.isSwimming(),
         playerEntity.isSubmergedInWater(),
         new Object2DoubleArrayMap(((EntityMixin2)playerEntity).getFluidHeight()),
         new HashSet<>(((EntityMixin2)playerEntity).getFluidOnEyes())
      );
   }

   private boolean method1521() {
      Box box = this.field04608.expand(1.0);
      int local2226_2 = MathHelper.floor(box.minX);
      int local2226_3 = MathHelper.ceil(box.maxX);
      int local2226_4 = MathHelper.floor(box.minZ);
      int local2226_5 = MathHelper.ceil(box.maxZ);
      return !this.field04604.getEntityWorld().isRegionLoaded(local2226_2, local2226_4, local2226_3, local2226_5);
   }

   public static SimulatedPlayerInput method1522(SimulatedPlayerInput.Nested0387 local2227_0) {
      ClientPlayerEntity playerEntity = mc.player;
      return new SimulatedPlayerInput(
         playerEntity,
         local2227_0,
         playerEntity.getEntityPos(),
         playerEntity.getVelocity(),
         playerEntity.getBoundingBox(),
         playerEntity.getYaw(),
         playerEntity.getPitch(),
         playerEntity.isSprinting(),
         (float)playerEntity.fallDistance,
         ((LivingEntityMixin)playerEntity).getNoJumpDelay(),
         ((LivingEntityMixin)playerEntity).getJumping(),
         playerEntity.isGliding(),
         playerEntity.isOnGround(),
         playerEntity.horizontalCollision,
         playerEntity.verticalCollision,
         playerEntity.isTouchingWater(),
         playerEntity.isSwimming(),
         playerEntity.isSubmergedInWater(),
         new Object2DoubleArrayMap(((EntityMixin2)playerEntity).getFluidHeight()),
         new HashSet<>(((EntityMixin2)playerEntity).getFluidOnEyes())
      );
   }

   private double method1523() {
      return this.field04606.y + this.field04604.getStandingEyeHeight();
   }

   private boolean method1524() {
      return this.field04616
         || this.field04612 < 0.5 && !this.field04604.getEntityWorld().isSpaceEmpty(this.field04604, this.field04608.offset(0.0, this.field04612 - 0.5, 0.0));
   }

   private Vec3d method1525() {
      return this.method1503(this.field04610, this.field04609);
   }

   private float method1526() {
      float local2231_1 = 0.02F;
      return this.field04605.field04729.sprint() ? local2231_1 + 0.006F : local2231_1;
   }

   public void method1527() {
      this.field04607 = this.field04607.add(0.0, this.method1490() - this.field04607.y, 0.0);
      if (this.method1512()) {
         float local2232_1 = (float)Math.toRadians(this.field04609);
         this.field04607 = this.field04607.add(-MathHelper.sin(local2232_1) * 0.2, 0.0, MathHelper.cos(local2232_1) * 0.2);
      }
   }

   public SimulatedPlayerInput clone() {
      return new SimulatedPlayerInput(
         this.field04604,
         this.field04605,
         this.field04606,
         this.field04607,
         this.field04608,
         this.field04609,
         this.field04610,
         this.field04611,
         this.field04612,
         this.field04613,
         this.field04614,
         this.field04615,
         this.field04616,
         this.field04617,
         this.field04618,
         this.field04619,
         this.field04620,
         this.field04621,
         new Object2DoubleArrayMap(this.field04622),
         new HashSet<>(this.field04623)
      );
   }

   private boolean method1529() {
      return this.field04619;
   }

   protected boolean method1530() {
      return this.field04605.field04729.sneak() || this.field04605.field04726;
   }

   public boolean method1531() {
      return this.field04621 && this.method1529();
   }

   private boolean method1532(Box box) {
      return this.field04604.getEntityWorld().isSpaceEmpty(this.field04604, box) && !this.field04604.getEntityWorld().containsFluid(box);
   }

   public static class Nested0387 extends Input {
      public boolean field04726 = false;
      public float field04727;
      public float field04728;
      public PlayerInput field04729;
      public static final double field04730 = 0.121;

      public void update() {
         if (this.field04729.forward() != this.field04729.backward()) {
            this.field04727 = this.field04729.forward() ? 1.0F : -1.0F;
         } else {
            this.field04727 = 0.0F;
         }

         if (this.field04729.left() == this.field04729.right()) {
            this.field04728 = 0.0F;
         } else {
            this.field04728 = this.field04729.left() ? 1.0F : -1.0F;
         }

         if (this.field04729.sneak()) {
            this.field04728 *= 0.3F;
            this.field04727 *= 0.3F;
         }
      }

      public Nested0387(PlayerInput playerInput) {
         this.field04729 = playerInput;
      }

      public static SimulatedPlayerInput.Nested0387 guessInput(PlayerEntity playerEntity) {
         Vec3d vec3d = playerEntity.getEntityPos().subtract(new Vec3d(playerEntity.lastX, playerEntity.lastY, playerEntity.lastZ));
         double local2240_2 = vec3d.horizontalLengthSquared();
         PlayerInput playerInput = new PlayerInput(false, false, false, false, !playerEntity.isOnGround(), playerEntity.isSneaking(), local2240_2 >= 0.014641);
         if (local2240_2 > 0.0025000000000000005) {
            double local2240_5 = PlayerUtils.method4073(vec3d, playerEntity.getYaw());
            double local2240_7 = MathHelper.wrapDegrees(local2240_5);
            playerInput = PlayerUtils.method4076(playerInput, local2240_7);
         }

         return new SimulatedPlayerInput.Nested0387(playerInput);
      }

      public static SimulatedPlayerInput.Nested0387 fromClientPlayer(PlayerInput playerInput) {
         return new SimulatedPlayerInput.Nested0387(playerInput);
      }

      @Override
      public String toString() {
         return "SimulatedPlayerInput(forwards={"
            + this.field04729.forward()
            + "}, backwards={"
            + this.field04729.backward()
            + "}, left={"
            + this.field04729.left()
            + "}, right={"
            + this.field04729.right()
            + "}, jumping={"
            + this.field04729.jump()
            + "}, sprinting="
            + this.field04729.sprint()
            + ", slowDown="
            + this.field04729.sneak()
            + ")";
      }
   }
}
