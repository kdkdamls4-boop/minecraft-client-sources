package net.skycore.client.combat;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class WorldCache implements BlockGetter {
   private static final int MASK = 8191;
   private static final long SHAPE_SALT = -7046029254386353131L;
   private static final WorldCache INSTANCE = new WorldCache();
   private final VoxelShape[] shapeValues;
   private final BlockState[] stateValues;
   private final long[] shapeKeys;
   private int generation;
   private int tick;
   private static final Minecraft MC = Minecraft.getInstance();
   private final int[] shapeGenerations;
   private CollisionContext context;
   private final long[] stateKeys = new long[8192];
   private static final CollisionContext EMPTY = CollisionContext.empty();
   private ClientLevel level;

   private BlockPos trace(Vec3 position, Vec3 position2, CollisionContext context2) {
      int value = 1;
      value = (byte)1;
      this.checkWorld();
      if (this.level == null) {
         return null;
      }

      if (this.context != context2) {
         this.context = context2;
         value = this.generation + 1;
         this.generation = value;
      }

      BiFunction<net.skycore.client.combat.WorldCache, net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> biFunction = (worldCache, blockPos) -> {
         VoxelShape voxelShape = worldCache.shape(blockPos);
         if (!voxelShape.isEmpty()) {
            if (voxelShape.clip(position, position2, blockPos) != null) {
               return blockPos.immutable();
            }
         }

         return null;
      };
      Function<net.skycore.client.combat.WorldCache, net.minecraft.core.BlockPos> function = worldCache2 -> {
         return null;
      };
      return BlockGetter.traverseBlocks(position, position2, this, biFunction, function);
   }

   private void checkWorld() {
      int value = 0;
      ClientLevel level2 = MC.level;
      if (MC.player == null) {
         value = Integer.MIN_VALUE;
      } else {
         value = MC.player.tickCount;
      }

      int value2 = value;
      if (level2 == this.level) {
         if (value2 == this.tick) {
            return;
         }
      }

      this.level = level2;
      this.tick = value2;
      this.invalidateInternal();
   }

   public static void invalidate() {
      INSTANCE.invalidateInternal();
   }

   public static BlockState blockState(BlockPos blockPos) {
      return INSTANCE.getBlockState(blockPos);
   }

   private void invalidateInternal() {
      Arrays.fill(this.stateValues, null);
      this.generation++;
   }

   @Override
   public FluidState getFluidState(BlockPos blockPos) {
      BlockState blockState = this.getBlockState(blockPos);
      return blockState.getFluidState();
   }

   public static BlockPos firstHit(BlockGetter blockGetter, Vec3 position, Vec3 position2) {
      BiFunction<net.minecraft.world.level.BlockGetter, net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> blockState = (blockGetter2, blockPos) -> {
         BlockState blockState2 = blockGetter2.getBlockState(blockPos);
         VoxelShape collisionShape = blockState2.getCollisionShape(blockGetter2, blockPos, EMPTY);
         if (!collisionShape.isEmpty()) {
            if (collisionShape.clip(position, position2, blockPos) != null) {
               return blockPos.immutable();
            }
         }

         return null;
      };
      Function<net.minecraft.world.level.BlockGetter, net.minecraft.core.BlockPos> function = blockGetter3 -> {
         return null;
      };
      return BlockGetter.traverseBlocks(position, position2, blockGetter, blockState, function);
   }

   @Override
   public BlockState getBlockState(BlockPos blockPos) {
      this.checkWorld();
      if (this.level == null) {
         return Blocks.AIR.defaultBlockState();
      }

      long timestamp = blockPos.asLong();
      int value = (int)HashCommon.mix(timestamp) & 8191;
      BlockState blockState = this.stateValues[value];
      if (blockState != null && this.stateKeys[value] == timestamp) {
         return blockState;
      }

      blockState = this.level.getBlockState(blockPos);
      this.stateKeys[value] = timestamp;
      this.stateValues[value] = blockState;
      return blockState;
   }

   @Override
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return MC.level == null ? null : MC.level.getBlockEntity(blockPos);
   }

   public static BlockPos rayBlock(Vec3 position, Vec3 position2) {
      return INSTANCE.trace(position, position2, EMPTY);
   }

   @Override
   public int getMinY() {
      int value = 0;
      value = (byte)0;
      if (MC.level == null) {
         value = 0;
      } else {
         value = MC.level.getMinY();
      }

      return value;
   }

   @Override
   public int getHeight() {
      int value = 0;
      if (MC.level == null) {
         value = 0;
      } else {
         value = MC.level.getHeight();
      }

      return value;
   }

   private VoxelShape shape(BlockPos blockPos) {
      long timestamp = blockPos.asLong();
      int computedValue = (int)HashCommon.mix(timestamp ^ -7046029254386353131L) & 8191;
      VoxelShape voxelShape = this.shapeValues[computedValue];
      if (this.shapeGenerations[computedValue] == this.generation) {
         if (this.shapeKeys[computedValue] == timestamp && voxelShape != null) {
            return voxelShape;
         }
      }

      BlockState blockState = this.getBlockState(blockPos);
      voxelShape = blockState.getCollisionShape(this, blockPos, this.context);
      this.shapeValues[computedValue] = voxelShape;
      this.shapeKeys[computedValue] = timestamp;
      this.shapeGenerations[computedValue] = this.generation;
      return voxelShape;
   }

   private WorldCache() {
      this.stateValues = new BlockState[8192];
      this.shapeKeys = new long[8192];
      this.shapeValues = new VoxelShape[8192];
      this.shapeGenerations = new int[8192];
      this.tick = Integer.MIN_VALUE;
   }
}
