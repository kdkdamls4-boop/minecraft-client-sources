package net.skycore.client.util;

import it.unimi.dsi.fastutil.HashCommon;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class InitializeHelper implements BlockGetter {
   private CollisionContext cachedCollisionContext;
   private final long[] blockPositionKeys = new long[8192];
   private final long[] shapePositionKeys;
   private final BlockState[] blockStates = new BlockState[8192];
   private int shapeGeneration;
   private Vec3 rayEnd;
   private Object cachedLevel;
   public static InitializeHelper INSTANCE;
   private CollisionContext rayCollisionContext;
   private Vec3 rayStart;
   private final VoxelShape[] collisionShapes;
   private final int[] shapeGenerations;
   private int cacheTick = Integer.MIN_VALUE;

   @Override
   public BlockEntity getBlockEntity(BlockPos blockPos) {
      return Minecraft.getInstance().level == null ? null : Minecraft.getInstance().level.getBlockEntity(blockPos);
   }

   public static void initialize() {
      InitializeHelper initializeHelper = new InitializeHelper();
      INSTANCE = initializeHelper;
   }

   private InitializeHelper() {
      this.shapePositionKeys = new long[8192];
      this.collisionShapes = new VoxelShape[8192];
      this.shapeGenerations = new int[8192];
   }

   public void invalidate() {
      int intermediate = -1;
      intermediate = (byte)-1;
      intermediate = (byte)-1;
      Arrays.fill(this.blockStates, null);
      intermediate = this.shapeGeneration + 1;
      this.shapeGeneration = intermediate;
   }

   @Override
   public BlockState getBlockState(BlockPos blockPos) {
      if (Minecraft.getInstance().level == null) {
         return Blocks.AIR.defaultBlockState();
      }

      int instance = Minecraft.getInstance().player == null ? Integer.MIN_VALUE : Minecraft.getInstance().player.tickCount;
      if (instance != this.cacheTick || this.cachedLevel != Minecraft.getInstance().level) {
         this.cacheTick = instance;
         this.cachedLevel = Minecraft.getInstance().level;
         this.invalidate();
      }

      long timestamp = blockPos.asLong();
      int value = (int)HashCommon.mix(timestamp) & 8191;
      BlockState blockState = this.blockStates[value];
      if (blockState != null && this.blockPositionKeys[value] == timestamp) {
         return blockState;
      }

      BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos);
      this.blockPositionKeys[value] = timestamp;
      this.blockStates[value] = blockState2;
      return blockState2;
   }

   @Override
   public int getHeight() {
      int value = 0;
      value = (byte)0;
      Minecraft instance = Minecraft.getInstance();
      if (instance.level == null) {
         value = 0;
      } else {
         value = Minecraft.getInstance().level.getHeight();
      }

      return value;
   }

   @Override
   public int getMinY() {
      int value = 0;
      if (Minecraft.getInstance().level == null) {
         value = 0;
      } else {
         value = Minecraft.getInstance().level.getMinY();
      }

      return value;
   }

   static {
      initialize();
   }

   private static BlockPos firstHit(InitializeHelper initializeHelper, BlockPos blockPos) {
      VoxelShape voxelShape = initializeHelper.collision(blockPos);
      if (voxelShape.isEmpty()) {
         return null;
      }

      BlockHitResult blockHitResult = voxelShape.clip(initializeHelper.rayStart, initializeHelper.rayEnd, blockPos);
      BlockPos blockPos2;
      if (blockHitResult == null) {
         blockPos2 = null;
      } else {
         blockPos2 = blockPos.immutable();
      }

      return blockPos2;
   }

   @Override
   public FluidState getFluidState(BlockPos blockPos) {
      return this.getBlockState(blockPos).getFluidState();
   }

   private VoxelShape collision(BlockPos blockPos) {
      long timestamp = blockPos.asLong();
      int computedValue = (int)HashCommon.mix(timestamp ^ -7046029254386353131L) & 8191;
      if (this.shapeGenerations[computedValue] == this.shapeGeneration && this.shapePositionKeys[computedValue] == timestamp) {
         if (this.collisionShapes[computedValue] != null) {
            return this.collisionShapes[computedValue];
         }
      }

      BlockState blockState = this.getBlockState(blockPos);
      VoxelShape collisionShape = blockState.getCollisionShape(this, blockPos, this.rayCollisionContext);
      this.shapePositionKeys[computedValue] = timestamp;
      this.collisionShapes[computedValue] = collisionShape;
      this.shapeGenerations[computedValue] = this.shapeGeneration;
      return collisionShape;
   }

   public BlockPos rayBlock(Vec3 rayStart2, Vec3 rayEnd2, CollisionContext cachedCollisionContext2) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level == null) {
         return null;
      }

      if (this.cachedCollisionContext != cachedCollisionContext2) {
         this.cachedCollisionContext = cachedCollisionContext2;
         this.shapeGeneration++;
      }

      this.rayStart = rayStart2;
      this.rayEnd = rayEnd2;
      this.rayCollisionContext = cachedCollisionContext2;
      BiFunction<net.skycore.client.util.InitializeHelper, net.minecraft.core.BlockPos, net.minecraft.core.BlockPos> biFunction = InitializeHelper::firstHit;
      Function<net.skycore.client.util.InitializeHelper, net.minecraft.core.BlockPos> function = initializeHelper -> {
         return null;
      };
      return BlockGetter.traverseBlocks(rayStart2, rayEnd2, this, biFunction, function);
   }
}
