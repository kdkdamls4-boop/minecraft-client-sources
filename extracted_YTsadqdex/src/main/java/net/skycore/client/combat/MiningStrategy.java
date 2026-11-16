package net.skycore.client.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import net.skycore.client.util.CrystalPlacementAimHelper;
import net.skycore.client.util.EfficiencyHelper;
import net.skycore.client.util.QueuePendingHelper;
import net.skycore.client.util.VisualSlotHelper;
import org.joml.Vector2f;

public enum MiningStrategy {
   INSTANCE;

   private int lastClaimTick = -100;
   private boolean holdStarted;
   private int lastProgressTick = -100;
   private boolean forceStop;
   private BlockPos targetBlock;
   private float breakProgress;
   private int airSinceTick;
   private Direction targetFace;
   private MiningStrategy.State state;
   private static final int MAX_BREAK_ATTEMPTS = 3;
   private int breakAttempts;
   private int startTick = -100;
   private Object owner;

   private Direction findFacingDirection(BlockPos blockPos) {
      Vec3 instance = Minecraft.getInstance().player.getEyePosition().subtract(Vec3.atCenterOf(blockPos));
      double x = Math.abs(instance.x);
      double y = Math.abs(instance.y);
      double z = Math.abs(instance.z);
      if (y >= x && y >= z) {
         return instance.y > 0.0 ? Direction.UP : Direction.DOWN;
      } else if (x >= z) {
         return instance.x > 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         return instance.z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }
   }

   public boolean shouldKeepDestroy() {
      if (!this.forceStop) {
         if (this.targetBlock != null && this.holdStarted) {
            return true;
         }
      }

      return false;
   }

   public void release(Object progress) {
      if (this.owner != null) {
         if (this.owner != progress) {
            return;
         }
      }

      this.abortHold();
      this.reset();
   }

   private boolean rotationReady() {
      if (Minecraft.getInstance().player != null) {
         if (this.targetBlock != null && this.targetFace != null) {
            Vec3 position = Vec3.atCenterOf(this.targetBlock);
            double stepX = this.targetFace.getStepX() * 0.5;
            double stepY = this.targetFace.getStepY() * 0.5;
            double stepZ = this.targetFace.getStepZ() * 0.5;
            Vec3 position2 = position.add(stepX, stepY, stepZ);
            Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();
            Vector2f angles = CrystalPlacementAimHelper.aim(eyePosition, position2);
            if (!RotationClaim.claimMining(this, angles)) {
               return false;
            }

            return RotationClaim.hasSentRotation(angles);
         }
      }

      return false;
   }

   private static int elapsedTicks(int value, int value2) {
      int computedValue = value - value2;
      return computedValue < 0 ? Integer.MAX_VALUE : computedValue;
   }

   public boolean isProgressStalled() {
      int value = 1;
      value = (byte)-1;
      value = (byte)-1;
      Minecraft instance = Minecraft.getInstance();
      int intermediate = -1;
      if (instance.player != null) {
         intermediate = (byte)-1;
         if (this.targetBlock != null) {
            float value2 = EfficiencyHelper.ticks(this.targetBlock);
            intermediate = Mth.ceil(value2 * 2.5F);
            value = Math.min(240, intermediate);
            int maximum = Math.max(40, value);
            return elapsedTicks(Minecraft.getInstance().player.tickCount, this.lastProgressTick) > maximum;
         }
      }

      return false;
   }

   public int getMiningAge() {
      int value = 0;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && this.targetBlock != null) {
         value = elapsedTicks(Minecraft.getInstance().player.tickCount, this.startTick);
      } else {
         value = 0;
      }

      return value;
   }

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   private void abortHold() {
      this.holdStarted = false;
      if (this.targetBlock != null && Minecraft.getInstance().gameMode != null) {
         if (Minecraft.getInstance().level != null) {
            Minecraft instance = Minecraft.getInstance();
            if (instance.gameMode.isDestroying()) {
               BlockState blockState = Minecraft.getInstance().level.getBlockState(this.targetBlock);
               if (!isSolidBlock(blockState)) {
                  BlockPos targetBlock2 = this.targetBlock;
                  MultiPlayerGameModeEnsureHasSentCarriedItemAccessor instance2 = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode;
                  if (!targetBlock2.equals(instance2.skycore$getDestroyBlockPos())) {
                     return;
                  }
               }

               this.forceStop = true;
               boolean condition = false /* VF: Semaphore variable */;

               try {
                  condition = true;
                  Minecraft.getInstance().gameMode.stopDestroyBlock();
                  condition = false;
               } finally {
                  if (condition) {
                     this.forceStop = false;
                  }
               }

               this.forceStop = false;
            }
         }
      }
   }

   public boolean hasTarget() {
      return this.targetBlock != null;
   }

   private void reset() {
      RotationClaim.release(this);
      this.owner = null;
      this.targetBlock = null;
      this.targetFace = null;
      this.lastClaimTick = -100;
      this.lastProgressTick = -100;
      this.startTick = -100;
      this.airSinceTick = -100;
      this.breakAttempts = 0;
      this.breakProgress = 0.0F;
      this.holdStarted = false;
      this.state = MiningStrategy.State.IDLE;
   }

   private void updateBreakProgress() {
      MultiPlayerGameModeEnsureHasSentCarriedItemAccessor instance = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode;
      float computedValue = this.targetBlock.equals(instance.skycore$getDestroyBlockPos()) ? Mth.clamp(instance.skycore$getDestroyProgress(), 0.0F, 1.0F) : 0.0F;
      if (computedValue > this.breakProgress + 1.0E-6F) {
         this.breakProgress = computedValue;
         this.lastProgressTick = Minecraft.getInstance().player.tickCount;
      }
   }

   public BlockPos getTargetBlock() {
      return this.targetBlock;
   }

   public float getBreakProgress() {
      return this.breakProgress;
   }

   private boolean isTargetInReach() {
      Minecraft instance = Minecraft.getInstance();
      double attributeValue = instance.player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);
      double minimum = Math.min(3.0, attributeValue);
      Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();
      return CrystalSimulation.distanceSquared(eyePosition, this.targetBlock) <= minimum * minimum;
   }

   public boolean tickMining() {
      if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null || Minecraft.getInstance().gameMode == null) {
         this.reset();
         return false;
      }

      if (this.targetBlock == null) {
         return false;
      }

      if (Minecraft.getInstance().level.getBlockState(this.targetBlock).is(BlockTags.LEAVES)) {
         this.abortHold();
         this.reset();
         RotationClaim.release(this);
         return false;
      }

      if (Minecraft.getInstance().options.keyAttack.isDown()) {
         this.abortHold();
         this.reset();
         return false;
      }

      int instance = elapsedTicks(Minecraft.getInstance().player.tickCount, this.lastClaimTick);
      if (instance > 4 && this.breakProgress <= 0.0F && !this.holdStarted) {
         this.abortHold();
         this.reset();
         return false;
      }

      if (!isSolidBlock(Minecraft.getInstance().level.getBlockState(this.targetBlock))) {
         this.state = this.confirmedOpen() ? MiningStrategy.State.BROKEN : MiningStrategy.State.MINING;
         if (this.state == MiningStrategy.State.BROKEN) {
            this.holdStarted = false;
         }

         return true;
      } else {
         this.airSinceTick = -100;
         if (!this.isTargetInReach()) {
            this.state = MiningStrategy.State.STUCK;
            this.abortHold();
            this.reset();
            QueuePendingHelper.abandon();
            return false;
         }

         if (!this.rotationReady()) {
            this.state = MiningStrategy.State.AIMING;
            if (this.getMiningAge() >= 4) {
               this.abortHold();
               RotationClaim.release(this);
               QueuePendingHelper.abandon();
               this.state = MiningStrategy.State.STUCK;
               return false;
            } else {
               return true;
            }
         } else if (!this.isProgressStalled()) {
            ActionReservation.action();
            MultiPlayerGameModeEnsureHasSentCarriedItemAccessor instance2 = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)Minecraft.getInstance().gameMode;
            int value = this.targetBlock.equals(instance2.skycore$getDestroyBlockPos()) && instance2.skycore$isDestroying() ? 1 : 0;
            this.holdStarted = true;
            if (value == 0) {
               if (this.breakAttempts >= 3) {
                  this.state = MiningStrategy.State.STUCK;
                  return true;
               }

               this.breakAttempts++;
               Minecraft.getInstance().gameMode.startDestroyBlock(this.targetBlock, this.targetFace);
               Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
            } else if (Minecraft.getInstance().gameMode.continueDestroyBlock(this.targetBlock, this.targetFace)) {
               Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
            }

            this.updateBreakProgress();
            if (!isSolidBlock(Minecraft.getInstance().level.getBlockState(this.targetBlock))) {
               this.state = this.confirmedOpen() ? MiningStrategy.State.BROKEN : MiningStrategy.State.MINING;
               if (this.state == MiningStrategy.State.BROKEN) {
                  this.holdStarted = false;
               }
            } else {
               this.airSinceTick = -100;
               this.state = MiningStrategy.State.MINING;
            }

            return true;
         } else if (this.breakAttempts >= 3) {
            this.state = MiningStrategy.State.STUCK;
            return true;
         } else {
            this.abortHold();
            this.lastProgressTick = Minecraft.getInstance().player.tickCount;
            this.breakProgress = 0.0F;
            this.state = MiningStrategy.State.AIMING;
            return true;
         }
      }
   }

   private static boolean isSolidBlock(BlockState blockState) {
      if (!blockState.isAir()) {
         if (!(blockState.getBlock() instanceof LiquidBlock)) {
            return true;
         }
      }

      return false;
   }

   MiningStrategy() {
      this.airSinceTick = -100;
      this.state = MiningStrategy.State.IDLE;
   }

   private boolean confirmedOpen() {
      Minecraft instance = Minecraft.getInstance();
      int value = instance.player.tickCount;
      if (this.airSinceTick != -100 && value >= this.airSinceTick) {
         return value - this.airSinceTick >= 1;
      } else {
         this.airSinceTick = value;
         return false;
      }
   }

   public void keepAlive(Object value) {
      if (this.owner == value) {
         if (this.targetBlock != null && Minecraft.getInstance().player != null) {
            Minecraft instance = Minecraft.getInstance();
            this.lastClaimTick = instance.player.tickCount;
         }
      }
   }

   public boolean claimTarget(Object owner2, BlockPos targetBlock2) {
      if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null || targetBlock2 == null) {
         return false;
      }

      if (this.owner != null && this.owner != owner2) {
         return false;
      }

      if (Minecraft.getInstance().options.keyAttack.isDown()) {
         return false;
      }

      if (!targetBlock2.equals(this.targetBlock)) {
         this.abortHold();
         this.targetBlock = targetBlock2.immutable();
         this.breakProgress = 0.0F;
         this.lastProgressTick = Minecraft.getInstance().player.tickCount;
         this.startTick = Minecraft.getInstance().player.tickCount;
         this.airSinceTick = -100;
         this.breakAttempts = 0;
         this.state = MiningStrategy.State.IDLE;
         this.targetFace = this.findFacingDirection(this.targetBlock);
         this.holdStarted = false;
         int instance = VisualSlotHelper.findBestHotbarToolSlot(Minecraft.getInstance().level.getBlockState(targetBlock2));
         if (instance >= 0 && Minecraft.getInstance().player.getInventory().getSelectedSlot() != instance) {
            Minecraft.getInstance().player.getInventory().setSelectedSlot(instance);
         }
      } else if (this.targetFace == null) {
         this.targetFace = this.findFacingDirection(this.targetBlock);
      }

      this.owner = owner2;
      this.lastClaimTick = Minecraft.getInstance().player.tickCount;
      return true;
   }

   public MiningStrategy.State getState() {
      return this.state;
   }

   public enum State {
      IDLE,
      AIMING,
      MINING,
      BROKEN,
      STUCK;
   }
}
