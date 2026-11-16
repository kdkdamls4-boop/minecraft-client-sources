package net.skycore.client.module.combat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.ActionReservation;
import net.skycore.client.combat.CrystalDamageResult;
import net.skycore.client.combat.CrystalFailureTracker;
import net.skycore.client.combat.CrystalSimulation;
import net.skycore.client.combat.MiningStrategy;
import net.skycore.client.combat.RotationClaim;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.movement.WebIgnoreModule;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.AllowsHelper;
import net.skycore.client.util.MiningPlanOptions;
import net.skycore.client.util.BlockAimTarget;
import net.skycore.client.util.EntityCollisionSnapshot;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.CooldownTimer;
import net.skycore.client.util.CrystalPlacementAimHelper;
import net.skycore.client.util.FirstDigHelper;
import net.skycore.client.util.FixInputHelper;
import net.skycore.client.util.InitializeHelper;
import net.skycore.client.util.MovementHelper;
import net.skycore.client.util.PredictHelper;
import net.skycore.client.util.QueuePendingHelper;
import net.skycore.client.util.ShouldDropFailedUseOnHelper;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.VisualSlotHelper;
import org.joml.Vector2f;

public final class CrystalAuraModule extends ClientModule implements MinecraftAccess {
   private final SliderSetting maxSelfDamage;
   private BlockPos footDigUpper;
   private final BooleanSetting legit;
   private final net.skycore.client.module.player.CrystalAuraModule vanillaAura;
   private final HashMap<BlockPos, Integer> failedDig;
   private final MiningStrategy miner;
   private boolean yieldingToWebTrap;
   private float displacedDamage;
   public static final CrystalAuraModule INSTANCE = new CrystalAuraModule();
   private int pendingTick;
   private long miningPulseTime;
   private int aimedCrystalTick;
   private long noPlaceSinceMs;
   private boolean supportAwaitingConfirmation;
   private float miningProgress;
   private float breakAfterMovementRange;
   private final SliderSetting breakRange;
   private BlockPos stickyBase;
   private int crystalAttemptCount;
   private int emptyTicks;
   private final HashMap<BlockPos, Integer> failedCrystalBases;
   private static final int PREDICT_TICKS = 2;
   private Item pendingItem;
   private int breakAfterMovementId;
   private BlockPos footDigBase;
   private BlockPos miningPulse;
   private final SliderSetting placeRange;
   private BlockPos forceDigPos;
   private BlockPos crystalAfterSupport;
   private static final int FOCUS_FAIL_LIMIT = 2;
   private final CooldownTimer timer;
   private final BooleanSetting rage;
   private BlockPos lockedFootBase;
   private BlockPos supportStackTarget;
   private BlockPos lastPlacedSupport;
   private BlockPos crystalAfterMovement;
   private BlockPos burrowLegsPos;
   private BlockPos displacedBase;
   private static final float SAFE_INTERACTION_RANGE = 3.0F;
   private BlockPos pendingBase;
   private int requestedSupportTick;
   private final HashMap<BlockPos, Integer> brokenDig;
   private BlockPos crystalAttemptBase;
   private static final float HIGH_VALUE_SUPPORT_DAMAGE = 7.5F;
   private static final float RAGE_INTERACTION_RANGE = 6.0F;
   private final SliderSetting minDamage;
   private final BooleanSetting obsidian;
   private int aimedCrystalId;
   private int legitSlot;
   private int displacedTick;
   private int ownCrystalTick;
   private BlockPos ownCrystal;
   private int focusFails;
   private boolean pendingCrystal;
   private final List<CrystalAuraModule.PlacementPulse> pulses;
   private CrystalAuraModule.FootSpot refreshedFootSpot;
   private final MiningPlanOptions digOptions;
   private static final int[][] FOOT_NEIGHBORS = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
   private BlockPos focusSpot;
   private final ModeSetting auraMode = this.add(new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Vannila"));
   private int supportTick;
   private final AllowsHelper interaction;
   private final BooleanSetting autoDig;
   private BlockPos footCrystalAfter;
   private BlockPos lastRequestedSupport;
   private final BooleanSetting hiding;
   private boolean crystalAwaitingSpawn;
   private BlockPos cycleBase;
   private int lastCrystalActionTick;
   private final SliderSetting delay;
   private final SliderSetting targetRange = this.add(new SliderSetting("Радиус цели", 8.0, 4.0, 12.0, 0.5));
   private static final long NO_PLACE_DIG_MS = 100L;
   private boolean burrowFlattenActive;
   private int crystalClaimFailures;
   private final ModeSetting movementCorrection;

   private boolean preserveDig(BlockPos blockPos) {
      BlockPos blockPos2 = this.miner.getTargetBlock();
      if (blockPos2 != null) {
         if (!blockPos2.equals(blockPos)) {
            this.miner.release(this);
            QueuePendingHelper.finish();
            return false;
         }
      }

      return false;
   }

   private boolean commitCrystal(BlockPos blockPos) {
      return this.commitCrystal(blockPos, true);
   }

   private static int miningAlpha(float value, float value2) {
      return (int)((0.45F + 0.4F * value) * value2 * 255.0F);
   }

   private boolean equipLegit() {
      int item = CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true);
      if (item == -1) {
         return false;
      }

      this.miner.release(this);
      this.switchActionSlot(item);
      return true;
   }

   private boolean isFootDigCell(BlockPos blockPos) {
      if (blockPos != null) {
         if (blockPos.equals(this.footDigBase)) {
            return true;
         }

         if (blockPos.equals(this.footDigUpper)) {
            return true;
         }
      }

      return false;
   }

   public void afterMovementPacket() {
      BlockPos crystalAfterMovement2 = this.crystalAfterMovement;
      this.crystalAfterMovement = null;
      int breakAfterMovementId2 = this.breakAfterMovementId;
      float breakAfterMovementRange2 = this.breakAfterMovementRange;
      this.breakAfterMovementId = -1;
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && this.auraMode.is("ReallyWorld") && !this.yieldingToWebTrap && instance.player != null && instance.level != null && instance.gameMode != null) {
         if (this.pendingItem != null) {
            BlockPos pendingBase2 = this.pendingBase;
            Item pendingItem2 = this.pendingItem;
            boolean pendingCrystal2 = this.pendingCrystal;
            int pendingTick2 = this.pendingTick;
            this.clearPending();
            int value = instance.player.tickCount;
            if (pendingBase2 != null && value >= pendingTick2 && value <= pendingTick2 + 2) {
               int angle;
               if (pendingCrystal2) {
                  Vector2f eyePosition = CrystalPlacementAimHelper.aim(instance.player.getEyePosition(), CrystalPlacementAimHelper.crystalPlacementAim(pendingBase2));
                  angle = !RotationClaim.hasSentRotation(eyePosition) && CrystalPlacementAimHelper.tryCrystalPick(pendingBase2, this.actionRange()) == null ? 0 : 1;
               } else {
                  BlockAimTarget eyePosition2 = CrystalSimulation.placementFace(pendingBase2, instance.player.getEyePosition(), this.actionRange());
                  angle = eyePosition2 == null
                        || !RotationClaim.hasSentRotation(CrystalPlacementAimHelper.aim(instance.player.getEyePosition(), eyePosition2.aim()))
                           && CrystalPlacementAimHelper.trySupportPick(pendingBase2, eyePosition2.block(), eyePosition2.aim(), this.actionRange()) == null
                     ? 0
                     : 1;
               }

               if (angle != 0) {
                  if (pendingCrystal2) {
                     this.commitCrystal(pendingBase2, false);
                  } else {
                     this.commitSupport(pendingBase2, pendingItem2);
                  }

                  this.miner.tickMining();
                  return;
               }
            }
         }

         if (breakAfterMovementId2 != -1 && instance.level.getEntity(breakAfterMovementId2) instanceof EndCrystal endCrystal && endCrystal.isAlive()) {
            Vec3 eyePosition3 = instance.player.getEyePosition();
            Vec3 position = CrystalSimulation.breakAim(endCrystal, eyePosition3, breakAfterMovementRange2);
            Vector2f angles = CrystalPlacementAimHelper.aim(eyePosition3, position);
            double distance = (double)breakAfterMovementRange2 * breakAfterMovementRange2;
            if ((
                  RotationClaim.hasSentRotation(angles)
                     || CrystalPlacementAimHelper.attackRay(RotationClaim.previousYaw(), RotationClaim.previousPitch(), breakAfterMovementRange2, endCrystal)
               )
               && CrystalSimulation.distanceSquared(eyePosition3, endCrystal.getBoundingBox()) <= distance
               && (!this.legit.get() || this.equipLegit())) {
               instance.gameMode.attack(instance.player, endCrystal);
               instance.player.swing(InteractionHand.MAIN_HAND);
               RotationClaim.markAction();
               this.aimedCrystalId = -1;
               this.aimedCrystalTick = -100;
               return;
            }
         }

         if (crystalAfterMovement2 != null && this.crystalCycleReady()) {
            Vector2f eyePosition4 = CrystalPlacementAimHelper.aim(instance.player.getEyePosition(), CrystalPlacementAimHelper.crystalPlacementAim(crystalAfterMovement2));
            if ((RotationClaim.hasSentRotation(eyePosition4) || CrystalPlacementAimHelper.tryCrystalPick(crystalAfterMovement2, this.actionRange()) != null)
               && CrystalSimulation.isSupport(instance.level.getBlockState(crystalAfterMovement2))
               && !CrystalSimulation.occupied(crystalAfterMovement2.above())
               && !this.awaitingSupport(crystalAfterMovement2)) {
               float value2 = this.actionRange();
               if (!(CrystalSimulation.distanceSquared(instance.player.getEyePosition(), crystalAfterMovement2) > value2 * value2)) {
                  this.commitCrystal(crystalAfterMovement2, false);
               }
            }
         }
      } else {
         this.clearPending();
      }
   }

   private void clearPending() {
      this.pendingBase = null;
      this.pendingItem = null;
      this.pendingCrystal = false;
      this.pendingTick = -100;
   }

   private boolean tryCrystalImmediatelyAfterSupport() {
      if (this.crystalAfterSupport != null && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
         BlockPos crystalAfterSupport2 = this.crystalAfterSupport;
         if (this.releasePlayerOccupiedSupport(crystalAfterSupport2)) {
            return false;
         }

         if (this.releaseItemBlockedCrystal(crystalAfterSupport2)) {
            return false;
         }

         if (!CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(crystalAfterSupport2))) {
            if (!this.supportAwaitingConfirmation && Minecraft.getInstance().player.tickCount - this.supportTick > 3) {
               this.crystalAfterSupport = null;
               return false;
            } else {
               this.primeCrystalRotation(crystalAfterSupport2);
               return true;
            }
         } else {
            double value = this.actionRange();
            if (CrystalSimulation.distanceSquared(Minecraft.getInstance().player.getEyePosition(), crystalAfterSupport2) > value * value) {
               this.crystalAfterSupport = null;
               return false;
            }

            EndCrystal endCrystal = this.findOwnCrystal(crystalAfterSupport2.above());
            if (endCrystal != null) {
               this.crystalAfterSupport = null;
               this.confirmCrystalSpawn(crystalAfterSupport2);
               return false;
            }

            if (this.crystalAwaitingSpawn && crystalAfterSupport2.above().equals(this.ownCrystal)) {
               if (Minecraft.getInstance().player.tickCount - this.ownCrystalTick < 3) {
                  return true;
               }

               this.crystalAwaitingSpawn = false;
               this.ownCrystal = null;
               this.crystalAttemptBase = null;
               this.crystalAttemptCount = 0;
               RotationClaim.release(this);
            }

            this.lockedFootBase = crystalAfterSupport2.immutable();
            this.stickyBase = crystalAfterSupport2.immutable();
            this.footCrystalAfter = crystalAfterSupport2.immutable();
            this.miner.release(this);
            if (!this.queueCrystal(crystalAfterSupport2)) {
               this.primeCrystalRotation(crystalAfterSupport2);
            }

            return true;
         }
      } else {
         return false;
      }
   }

   private MiningPlanOptions readyOnlyDigOptions() {
      this.digOptions.enabled = false;
      this.digOptions.currentBlock = null;
      this.digOptions.progress = 0.0F;
      this.digOptions.blacklist = this.failedDig.keySet();
      this.digOptions.stickyBase = this.stickyBase != null ? this.stickyBase : this.recentSupport();
      this.applyReplacementPreference();
      this.digOptions.preferMaximumDamage = this.rage.get();
      this.digOptions.fastExposure = true;
      this.digOptions.maxBlocks = 0;
      this.digOptions.maxAttempts = 0;
      this.digOptions.footY = Integer.MIN_VALUE;
      this.digOptions.standBlock = null;
      return this.digOptions;
   }

   private AABB pulseBox(BlockPos blockPos, float value) {
      double x = blockPos.getX() + 0.5;
      double y = blockPos.getY() + 0.5;
      double z = blockPos.getZ() + 0.5;
      double clampedValue = 0.5 * Mth.clamp(value, 0.05F, 1.0F);
      return new AABB(x - clampedValue, y - clampedValue, z - clampedValue, x + clampedValue, y + clampedValue, z + clampedValue);
   }

   private boolean isOccupyingSolid(BlockPos blockPos, BlockState blockState) {
      if (blockState != null) {
         if (!blockState.isAir()) {
            if (!blockState.canBeReplaced()) {
               if (blockState.getBlock() instanceof LiquidBlock) {
                  return false;
               }

               Minecraft instance = Minecraft.getInstance();
               VoxelShape collisionShape = blockState.getCollisionShape(instance.level, blockPos);
               return !collisionShape.isEmpty() || CrystalSimulation.mineable(blockState);
            }
         }
      }

      return false;
   }

   private BlockPos pickStickyReadyBase() {
      if (this.footCrystalAfter != null) {
         Minecraft instance = Minecraft.getInstance();
         if (CrystalSimulation.isSupport(instance.level.getBlockState(this.footCrystalAfter))) {
            return this.footCrystalAfter;
         }
      }

      if (this.stickyBase != null) {
         Minecraft instance2 = Minecraft.getInstance();
         if (CrystalSimulation.isSupport(instance2.level.getBlockState(this.stickyBase))) {
            return this.stickyBase;
         }
      }

      if (this.lastPlacedSupport != null) {
         Minecraft instance3 = Minecraft.getInstance();
         if (instance3.player.tickCount - this.supportTick <= 40) {
            instance3 = Minecraft.getInstance();
            if (CrystalSimulation.isSupport(instance3.level.getBlockState(this.lastPlacedSupport))) {
               return this.lastPlacedSupport;
            }
         }
      }

      return null;
   }

   private boolean releasePlayerOccupiedSupport(BlockPos displacedBase2) {
      if (!this.playerOccupiesSupport(displacedBase2)) {
         return false;
      }

      this.displacedBase = displacedBase2.immutable();
      this.displacedDamage = this.supportDamage(displacedBase2);
      this.displacedTick = Minecraft.getInstance().player.tickCount;
      if (displacedBase2.equals(this.pendingBase)) {
         this.clearPending();
      }

      if (displacedBase2.equals(this.lockedFootBase)) {
         this.lockedFootBase = null;
      }

      this.clearStickyIf(displacedBase2);
      if (displacedBase2.equals(this.crystalAfterSupport)) {
         this.crystalAfterSupport = null;
      }

      if (displacedBase2.equals(this.cycleBase)) {
         this.cycleBase = null;
      }

      if (displacedBase2.equals(this.supportStackTarget)) {
         this.supportStackTarget = null;
      }

      if (displacedBase2.equals(this.lastPlacedSupport)) {
         this.lastPlacedSupport = null;
         this.supportAwaitingConfirmation = false;
         this.supportTick = -100;
      }

      if (displacedBase2.equals(this.forceDigPos) || displacedBase2.above().equals(this.forceDigPos) || displacedBase2.equals(this.footDigBase)) {
         this.abortFootDig();
      }

      return true;
   }

   private boolean recordCrystalPlacementFailure(BlockPos crystalAttemptBase2) {
      if (crystalAttemptBase2 != null && Minecraft.getInstance().player != null) {
         if (this.crystalAttemptBase != null && this.crystalAttemptBase.equals(crystalAttemptBase2)) {
            this.crystalAttemptCount++;
         } else {
            this.crystalAttemptBase = crystalAttemptBase2.immutable();
            this.crystalAttemptCount = 1;
         }

         if (this.crystalAttemptCount < 2) {
            return false;
         }

         this.crystalAttemptBase = null;
         this.crystalAttemptCount = 0;
         this.crystalAwaitingSpawn = false;
         this.ownCrystal = null;
         this.armCycleBase(crystalAttemptBase2);
         RotationClaim.release(this);
         return false;
      } else {
         return false;
      }
   }

   private void finishFootDig(BlockPos blockPos, boolean condition) {
      if (blockPos.equals(this.footDigUpper)) {
         BlockPos blockPos2 = this.footDigBase != null ? this.footDigBase : blockPos.below();
         this.footDigUpper = null;
         BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos2);
         if (CrystalSimulation.mineable(blockState)) {
            this.footDigBase = blockPos2.immutable();
            this.forceDigPos = blockPos2.immutable();
         } else if (blockState.canBeReplaced() && condition) {
            this.footDigBase = null;
            this.forceDigPos = null;
            this.placeAfterForceDig(blockPos2);
         } else {
            this.footDigBase = null;
            this.forceDigPos = null;
         }
      } else if (blockPos.equals(this.footDigBase) || this.forceDigPos != null && this.forceDigPos.equals(blockPos)) {
         BlockPos blockPos3 = blockPos.above();
         BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos3);
         this.footDigBase = null;
         this.footDigUpper = null;
         this.forceDigPos = null;
         if (blockState2.isAir() && condition) {
            this.placeAfterForceDig(blockPos);
         } else {
            this.forceDigPos = null;
         }
      } else {
         if (condition) {
            this.placeAfterForceDig(blockPos);
         } else {
            this.forceDigPos = null;
         }
      }
   }

   private void requestDig(BlockPos blockPos) {
      this.rememberActionSlot();
      if (!this.recentlyBroken(blockPos) || this.forceDigPos != null && this.forceDigPos.equals(blockPos)) {
         ActionReservation.touch();
         ActionReservation.reserve(blockPos);
         BlockPos blockPos2 = this.miner.getTargetBlock();
         MiningStrategy.State state = this.miner.getState();
         if (state == MiningStrategy.State.BROKEN && blockPos.equals(blockPos2)) {
            this.brokenDig.put(blockPos.immutable(), Minecraft.getInstance().player.tickCount);
            BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
            int value = !blockState.isAir() && !blockState.canBeReplaced() ? 0 : 1;
            this.miner.release(this);
            if (value == 0) {
               return;
            }

            this.noteSpotSuccess(blockPos);
            if (this.burrowFlattenActive && blockPos.equals(this.burrowLegsPos)) {
               this.burrowFlattenActive = false;
               this.burrowLegsPos = null;
               this.forceDigPos = null;
            } else if (this.forceDigPos != null && this.forceDigPos.equals(blockPos)) {
               this.finishFootDig(blockPos, true);
            }
         } else if (state == MiningStrategy.State.STUCK) {
            if (this.miner.getBreakProgress() > 0.0F) {
               this.blacklistDig(blockPos);
            }

            this.noteSpotFailure(blockPos);
            if (this.burrowFlattenActive && blockPos.equals(this.burrowLegsPos)) {
               this.burrowFlattenActive = false;
               this.burrowLegsPos = null;
            }

            if (this.forceDigPos != null && this.forceDigPos.equals(blockPos)) {
               this.forceDigPos = null;
            }

            this.miner.release(this);
            RotationClaim.release(this.miner);
            QueuePendingHelper.finish();
         } else {
            boolean condition = this.miner.claimTarget(this, blockPos);
            if (condition || blockPos.equals(this.miner.getTargetBlock()) && this.miner.getState() == MiningStrategy.State.MINING) {
               this.showMiningPulse(blockPos);
            }
         }
      } else {
         this.blacklistDig(blockPos);
         this.noteSpotFailure(blockPos);
         this.miner.release(this);
      }
   }

   private boolean tryQueueFootCrystal() {
      Minecraft instance = Minecraft.getInstance();
      float health = instance.player.getHealth();
      Minecraft instance2 = Minecraft.getInstance();
      float absorptionAmount = health + instance2.player.getAbsorptionAmount() - 1.0F;
      float maximum = Math.max(0.0F, absorptionAmount);
      Double value = this.targetRange.get();
      return this.tryStickyReadyExplode(maximum, CrystalPlacementAimHelper.friends(value.floatValue() + 8.0F), this.crystalCycleReady());
   }

   private boolean noteSpotFailure(BlockPos blockPos) {
      return this.noteSpotFailure(blockPos, 2);
   }

   private boolean resolveSupportAttempt() {
      if (this.supportAwaitingConfirmation && this.lastPlacedSupport != null && Minecraft.getInstance().player != null && Minecraft.getInstance().level != null
         )
       {
         BlockPos lastPlacedSupport2 = this.lastPlacedSupport;
         BlockState blockState = Minecraft.getInstance().level.getBlockState(lastPlacedSupport2);
         if (!CrystalSimulation.isSupport(blockState)) {
            if (Minecraft.getInstance().player.tickCount - this.supportTick < 2) {
               return true;
            }

            this.supportAwaitingConfirmation = false;
            this.lastPlacedSupport = null;
            this.supportTick = -100;
            boolean noteSpotFailure = this.noteSpotFailure(lastPlacedSupport2, 3);
            if (!noteSpotFailure && blockState.canBeReplaced()) {
               this.forceDigPos = lastPlacedSupport2.immutable();
               this.footDigBase = lastPlacedSupport2.immutable();
               this.footDigUpper = null;
            }

            return true;
         } else {
            this.supportAwaitingConfirmation = false;
            this.supportTick = -100;
            BlockPos supportStackTarget2 = this.supportStackTarget;
            int value = supportStackTarget2 != null && !lastPlacedSupport2.equals(supportStackTarget2) ? 1 : 0;
            this.resetAfterOwnSupport(lastPlacedSupport2);
            if (value != 0) {
               this.lastPlacedSupport = null;
               this.crystalAfterSupport = null;
               this.cycleBase = null;
               this.stickyBase = supportStackTarget2.immutable();
               this.footCrystalAfter = supportStackTarget2.immutable();
               if (this.queueSupport(supportStackTarget2, Items.OBSIDIAN)) {
                  this.lastRequestedSupport = supportStackTarget2.immutable();
                  this.requestedSupportTick = Minecraft.getInstance().player.tickCount;
               }

               return true;
            } else {
               this.supportStackTarget = null;
               if (CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true) != -1) {
                  if (!this.queueCrystal(lastPlacedSupport2) && !this.playerOccupiesSupport(lastPlacedSupport2) && !this.onlyDroppedItemsBlockCrystal(lastPlacedSupport2)) {
                     this.crystalAfterSupport = lastPlacedSupport2.immutable();
                     this.primeCrystalRotation(lastPlacedSupport2);
                  }

                  return true;
               } else {
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   private boolean noteBlockedSpot(BlockPos blockPos) {
      return this.noteSpotFailure(blockPos, 1);
   }

   private void restoreLegit() {
      if (this.legitSlot != -1) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            Minecraft instance2 = Minecraft.getInstance();
            Inventory inventory = instance2.player.getInventory();
            inventory.setSelectedSlot(this.legitSlot);
            Boolean get = this.hiding.get();
            if (get) {
               SpoofElytraAsChestplateHelper.refresh(4);
            }
         }
      }
   }

   public void applyFinalMovementCorrection(PlayerInputEvent playerInputEvent) {
      if (this.focusedMovementCorrection()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            float yRot = instance.player.getYRot();
            MovementHelper.fixMovement(playerInputEvent, yRot);
         }
      }
   }

   private CrystalAuraModule.FootSpot findReadyFootCrystal(List elements, Vec3 position, float damage, List elements2) {
      Player player = this.nearestTarget(elements);
      if (player != null && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
         BlockPos blockPos = CrystalSimulation.standingBlock(player);
         double index = this.actionRange();
         double distance = index * index;
         float index2 = -Float.MAX_VALUE;
         CrystalAuraModule.FootSpot footSpot = null;

         for (int[] intValues : FOOT_NEIGHBORS) {
            BlockPos blockPos2 = blockPos.offset(intValues[0], 0, intValues[1]);
            if (!(CrystalSimulation.distanceSquared(position, blockPos2) > distance)
               && CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(blockPos2))
               && Minecraft.getInstance().level.getBlockState(blockPos2.above()).isAir()
               && !this.crystalCellBlocked(blockPos2)) {
               Vec3 position2 = CrystalSimulation.blast(blockPos2);
               float boundingBox = CrystalSimulation.damage(position2, player, player.getBoundingBox());
               if (!(boundingBox < this.minDamage.get().floatValue())) {
                  float instance = CrystalSimulation.damage(position2, Minecraft.getInstance().player, Minecraft.getInstance().player.getBoundingBox());
                  float damage2 = 0.0F;
                  if (elements2 != null) {
                     for (Object value : elements2) {
                        if (value instanceof LivingEntity entity && entity != Minecraft.getInstance().player) {
                           damage2 = Math.max(damage2, CrystalSimulation.damage(position2, entity, entity.getBoundingBox()));
                        }
                     }
                  }

                  if (!CrystalFailureTracker.rejected(instance, damage2, boundingBox, this.maxSelfDamage.get().floatValue(), damage)) {
                     float computedValue = boundingBox * 10.0F - instance - damage2 * 4.0F;
                     if (computedValue > index2) {
                        this.failedCrystalBases.remove(blockPos2);
                        index2 = computedValue;
                        footSpot = new CrystalAuraModule.FootSpot(blockPos, blockPos2.immutable(), boundingBox, CrystalAuraModule.FootAction.CRYSTAL);
                     }
                  }
               }
            }
         }

         return footSpot;
      } else {
         return null;
      }
   }

   private boolean continuePreviousDig() {
      BlockPos blockPos = this.miner.getTargetBlock();
      if (blockPos != null && !this.isBlacklisted(blockPos)) {
         BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
         if (!CrystalSimulation.mineable(blockState)) {
            return false;
         }

         float value = this.actionRange();
         Minecraft instance = Minecraft.getInstance();
         if (CrystalSimulation.distanceSquared(instance.player.getEyePosition(), blockPos) > value * value) {
            return false;
         }

         this.requestDig(blockPos);
         return true;
      } else {
         return false;
      }
   }

   private boolean tryReplaceAfterExplode(boolean condition) {
      if (condition && this.cycleBase != null && Minecraft.getInstance().level != null) {
         BlockPos cycleBase2 = this.cycleBase;
         double value = this.actionRange();
         if (CrystalSimulation.distanceSquared(Minecraft.getInstance().player.getEyePosition(), cycleBase2) > value * value) {
            this.clearStickyIf(cycleBase2);
            if (cycleBase2.equals(this.lockedFootBase)) {
               this.lockedFootBase = null;
            }

            this.cycleBase = null;
            return false;
         } else {
            if (!CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(cycleBase2))) {
               this.cycleBase = null;
               return false;
            }

            if (this.releasePlayerOccupiedSupport(cycleBase2)) {
               return false;
            }

            if (this.releaseItemBlockedCrystal(cycleBase2)) {
               return false;
            }

            if (!this.supportDealsEnough(cycleBase2)) {
               this.clearStickyIf(cycleBase2);
               this.cycleBase = null;
               return false;
            }

            if (this.findOwnCrystal(cycleBase2.above()) == null && !CrystalSimulation.occupied(cycleBase2.above())) {
               Vec3 position = CrystalSimulation.blast(cycleBase2);
               float instance = Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F);
               if (CrystalFailureTracker.rejected(
                  CrystalSimulation.damage(position, Minecraft.getInstance().player),
                  CrystalSimulation.friendDamage(position, CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F)),
                  0.0F,
                  this.maxSelfDamage.get().floatValue(),
                  instance
               )) {
                  return false;
               } else if (this.pendingItem == null && !this.awaitingSupport(cycleBase2)) {
                  this.miner.release(this);
                  this.armCycleBase(cycleBase2);
                  return this.queueCrystal(cycleBase2);
               } else {
                  this.armCycleBase(cycleBase2);
                  return true;
               }
            } else {
               return false;
            }
         }
      } else {
         return false;
      }
   }

   private boolean runFootSideUndercut(List elements, Vec3 position, boolean condition, boolean condition2, CrystalAuraModule.FootSpot footSpot) {
      if (this.pendingItem != null) {
         this.keepMiner();
         return true;
      }

      if (this.lockedFootBase != null) {
         BlockPos lockedFootBase2 = this.lockedFootBase;
         double value = this.actionRange();
         boolean releasePlayerOccupiedSupport = this.releasePlayerOccupiedSupport(lockedFootBase2);
         if (releasePlayerOccupiedSupport || !this.isFootLevelBase(lockedFootBase2, elements) || CrystalSimulation.distanceSquared(position, lockedFootBase2) > value * value || this.isBlacklisted(lockedFootBase2)) {
            this.clearStickyIf(lockedFootBase2);
            this.lockedFootBase = null;
            if (lockedFootBase2.equals(this.forceDigPos) || lockedFootBase2.above().equals(this.forceDigPos) || lockedFootBase2.equals(this.footDigBase)) {
               this.abortFootDig();
            }
         }
      }

      BlockPos blockPos = this.pickStickyReadyBase();
      if (blockPos != null && this.releasePlayerOccupiedSupport(blockPos)) {
         blockPos = null;
      }

      if (blockPos != null && this.lockedFootBase != null && !blockPos.equals(this.lockedFootBase)) {
         blockPos = null;
      }

      if (blockPos != null && !this.isFootLevelBase(blockPos, elements)) {
         this.clearStickyIf(blockPos);
         if (blockPos.equals(this.cycleBase)) {
            this.cycleBase = null;
         }

         blockPos = null;
      }

      if (condition2 && blockPos != null) {
         return this.tryStickyReadyExplode(
            Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F),
            CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F),
            condition
         );
      }

      if (footSpot == null && condition2 && this.lockedFootBase == null) {
         CrystalAuraModule.FootSpot instance = this.findReadyFootCrystal(
            elements,
            position,
            Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F),
            CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F)
         );
         if (instance != null) {
            this.abortFootDig();
            this.lockedFootBase = instance.pos().immutable();
            this.footCrystalAfter = instance.pos().immutable();
            this.stickyBase = instance.pos().immutable();
            return this.tryStickyReadyExplode(
               Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F),
               CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F),
               condition
            );
         }
      }

      if (this.finishForceDig(CrystalPlacementAimHelper.findItem(Items.OBSIDIAN, false) != -1)) {
         return true;
      }

      if (this.forceDigPos != null) {
         BlockPos forceDigPos2 = this.forceDigPos;
         BlockState blockState = Minecraft.getInstance().level.getBlockState(forceDigPos2);
         if (CrystalSimulation.isSupport(blockState) && !this.isFootDigCell(forceDigPos2)) {
            this.miner.release(this);
            this.forceDigPos = null;
            this.footCrystalAfter = forceDigPos2.immutable();
            this.stickyBase = forceDigPos2.immutable();
            return true;
         } else {
            this.requestDig(forceDigPos2);
            return true;
         }
      } else {
         CrystalAuraModule.FootSpot footSpot2 = footSpot;
         if (footSpot2 == null && this.lockedFootBase != null) {
            footSpot2 = this.footSpotForLockedBase(elements);
            if (footSpot2 == null) {
               this.lockedFootBase = null;
            }
         }

         if (footSpot2 == null) {
            footSpot2 = this.findFootSideSpot(
               elements,
               position,
               Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F),
               CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F)
            );
         }

         if (footSpot2 == null) {
            return false;
         }

         if (this.lockedFootBase == null) {
            this.lockedFootBase = footSpot2.pos().immutable();
         }

         if (footSpot2.action() == CrystalAuraModule.FootAction.CRYSTAL) {
            this.footCrystalAfter = footSpot2.pos().immutable();
            this.stickyBase = footSpot2.pos().immutable();
            return this.tryStickyReadyExplode(
               Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F),
               CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F),
               condition
            );
         }

         this.emptyTicks = 0;
         this.noPlaceSinceMs = 0L;
         this.digOptions.footY = footSpot2.stand().getY();
         BlockState instance2 = Minecraft.getInstance().level.getBlockState(footSpot2.pos());
         if (footSpot2.action() != CrystalAuraModule.FootAction.DIG && footSpot2.action() != CrystalAuraModule.FootAction.DIG_SUPPORT) {
            if (footSpot2.action() != CrystalAuraModule.FootAction.PLACE) {
               return false;
            }

            if (condition && CrystalPlacementAimHelper.findItem(Items.OBSIDIAN, false) != -1) {
               BlockPos blockPos2 = footSpot2.pos().above();
               BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos2);
               if (blockState2.isAir()) {
                  this.footDigBase = null;
                  this.footDigUpper = null;
                  this.forceDigPos = null;
                  this.placeAfterForceDig(footSpot2.pos());
                  return true;
               }

               if (!this.isFootDigTarget(blockState2, blockPos2)) {
                  return false;
               }

               this.footDigBase = footSpot2.pos().immutable();
               this.footDigUpper = blockPos2.immutable();
               this.forceDigPos = blockPos2.immutable();
               this.requestDig(blockPos2);
               return true;
            } else {
               return true;
            }
         } else {
            boolean condition3 = footSpot2.action() == CrystalAuraModule.FootAction.DIG_SUPPORT
               ? instance2.is(Blocks.OBSIDIAN) && CrystalSimulation.mineable(instance2)
               : this.isFootDigTarget(instance2, footSpot2.pos());
            if (!condition3) {
               return false;
            }

            BlockPos blockPos3 = footSpot2.pos().immutable();
            BlockPos blockPos4 = blockPos3.above().immutable();
            BlockState blockState3 = Minecraft.getInstance().level.getBlockState(blockPos4);
            this.footDigBase = blockPos3;
            if (CrystalSimulation.mineable(blockState3)) {
               this.footDigUpper = blockPos4;
               this.forceDigPos = blockPos4;
               this.requestDig(blockPos4);
            } else {
               if (!blockState3.isAir()) {
                  this.footDigBase = null;
                  return false;
               }

               this.footDigUpper = null;
               this.forceDigPos = blockPos3;
               this.requestDig(blockPos3);
            }

            return true;
         }
      }
   }

   private void resetReallyWorldRuntime() {
      this.yieldingToWebTrap = false;
      this.clearLegitSlot();
      this.miner.release(this);
      this.interaction.end();
      RotationClaim.release(this);
      this.pulses.clear();
      this.miningPulse = null;
      this.ownCrystal = null;
      this.lastRequestedSupport = null;
      this.stickyBase = null;
      this.emptyTicks = 0;
      this.noPlaceSinceMs = 0L;
      this.forceDigPos = null;
      this.footDigBase = null;
      this.footDigUpper = null;
      this.lockedFootBase = null;
      this.refreshedFootSpot = null;
      this.footCrystalAfter = null;
      this.focusSpot = null;
      this.focusFails = 0;
      this.burrowLegsPos = null;
      this.burrowFlattenActive = false;
      this.cycleBase = null;
      this.displacedBase = null;
      this.displacedDamage = 0.0F;
      this.displacedTick = -100;
      this.lastCrystalActionTick = -100;
      this.crystalClaimFailures = 0;
      this.aimedCrystalId = -1;
      this.aimedCrystalTick = -100;
      this.supportAwaitingConfirmation = false;
      this.crystalAfterSupport = null;
      this.supportStackTarget = null;
      this.failedDig.clear();
      this.brokenDig.clear();
      this.failedCrystalBases.clear();
      this.crystalAttemptBase = null;
      this.crystalAttemptCount = 0;
      this.crystalAwaitingSpawn = false;
      this.clearPending();
      InitializeHelper.INSTANCE.invalidate();
      RotationClaim.reset();
      ActionReservation.reset();
      VisualSlotHelper.INSTANCE.restoreAndReset();
      QueuePendingHelper.hardReset();
      this.lastPlacedSupport = null;
      this.supportTick = -100;
   }

   private boolean crystalCellBlocked(BlockPos blockPos) {
      if (blockPos != null) {
         if (Minecraft.getInstance().level != null) {
            BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
            BlockPos blockPos2 = blockPos.above();
            if (!CrystalSimulation.occupied(blockPos2)) {
               if (CrystalSimulation.isSupport(blockState) || !CrystalSimulation.obsidianObstructed(blockPos)) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private boolean isFootLevelPlan(FirstDigHelper firstDigHelper, List elements) {
      Player player = this.nearestTarget(elements);
      if (player != null && firstDigHelper != null) {
         BlockPos blockPos = firstDigHelper.base();
         return this.isFootLevelBase(blockPos, elements);
      } else {
         return false;
      }
   }

   private void applyReplacementPreference() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && this.displacedBase != null && Minecraft.getInstance().player.tickCount - this.displacedTick <= 40) {
         this.digOptions.preferredBase = this.displacedBase;
         this.digOptions.preferredDamage = this.displacedDamage;
      } else {
         this.displacedBase = null;
         this.digOptions.preferredBase = null;
         this.digOptions.preferredDamage = 0.0F;
      }
   }

   @Override
   protected void onDisable() {
      if (this.vanillaAura.isEnabled()) {
         ClientModule.runSilently(() -> {
            this.vanillaAura.setEnabled(false);
         });
      }

      this.resetReallyWorldRuntime();
   }

   public boolean isBusy() {
      return this.isEnabled() && (this.pendingItem != null || this.miner.hasTarget());
   }

   private void clearStickyIf(BlockPos blockPos) {
      if (blockPos != null) {
         if (blockPos.equals(this.footCrystalAfter)) {
            this.footCrystalAfter = null;
         }

         if (blockPos.equals(this.stickyBase)) {
            this.stickyBase = null;
         }

         if (blockPos.equals(this.cycleBase)) {
            this.cycleBase = null;
         }
      }
   }

   private boolean playerOccupiesSupport(BlockPos blockPos) {
      if (blockPos != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            double x = blockPos.getX();
            double y = blockPos.getY() + 0.75;
            double z = blockPos.getZ();
            double x2 = blockPos.getX() + 1.0;
            double y2 = blockPos.getY() + 3.0;
            double z2 = blockPos.getZ() + 1.0;
            AABB bounds = new AABB(x, y, z, x2, y2, z2);
            Minecraft instance2 = Minecraft.getInstance();
            var entitiesOfClass = instance2.level.getEntitiesOfClass(Player.class, bounds, player -> {
               Minecraft instance3 = Minecraft.getInstance();
               return player != instance3.player && player.isAlive() && !player.isSpectator();
            });
            return !entitiesOfClass.isEmpty();
         }
      }

      return false;
   }

   private void abortFootDig() {
      if (this.burrowFlattenActive) {
         this.footDigBase = null;
         this.footDigUpper = null;
         if (this.forceDigPos != null && !this.forceDigPos.equals(this.burrowLegsPos)) {
            this.forceDigPos = this.burrowLegsPos;
         }
      } else {
         if (this.forceDigPos != null || this.footDigBase != null || this.footDigUpper != null || this.miner.hasTarget()) {
            boolean condition = this.miner.hasTarget();
            this.forceDigPos = null;
            this.footDigBase = null;
            this.footDigUpper = null;
            this.miner.release(this);
            if (condition) {
               QueuePendingHelper.finish();
            }
         }
      }
   }

   private boolean isFootLevelBase(BlockPos blockPos, List elements) {
      int value = 0;
      Player player = this.nearestTarget(elements);
      if (player != null && blockPos != null) {
         BlockPos blockPos2 = CrystalSimulation.standingBlock(player);
         value = blockPos.getX() - blockPos2.getX();
         int computedValue = Math.abs(value);
         value = blockPos.getZ();
         value -= blockPos2.getZ();
         int computedValue2 = Math.abs(value);
         value = blockPos.getY();
         return value == blockPos2.getY() && computedValue + computedValue2 == 1;
      } else {
         return false;
      }
   }

   private void refreshFootSupport(List elements, Vec3 position, float value, List elements2) {
      if (!this.miner.hasTarget() && Minecraft.getInstance().level != null) {
         this.failedDig.keySet().removeIf(blockPos -> {
            Minecraft instance = Minecraft.getInstance();
            BlockState blockState = instance.level.getBlockState(blockPos);
            return blockState.canBeReplaced() || CrystalSimulation.isSupport(blockState);
         });
         this.failedCrystalBases.keySet().removeIf(blockPos2 -> {
            Minecraft instance2 = Minecraft.getInstance();
            return CrystalSimulation.isSupport(instance2.level.getBlockState(blockPos2));
         });
         this.brokenDig.keySet().removeIf(blockPos3 -> {
            BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos3);
            return blockState2.canBeReplaced();
         });
      }

      CrystalAuraModule.FootSpot footSideSpot = this.findFootSideSpot(elements, position, value, elements2);
      this.refreshedFootSpot = footSideSpot;
      int value2 = this.forceDigPos == null || this.footDigBase == null && this.footDigUpper == null ? 0 : 1;
      if (value2 != 0 && (footSideSpot == null || this.lockedFootBase == null || !footSideSpot.pos().equals(this.lockedFootBase))) {
         BlockPos lockedFootBase2 = this.lockedFootBase;
         this.abortFootDig();
         if (lockedFootBase2 != null) {
            this.clearStickyIf(lockedFootBase2);
            if (lockedFootBase2.equals(this.cycleBase)) {
               this.cycleBase = null;
            }
         }

         this.lockedFootBase = footSideSpot == null ? null : footSideSpot.pos().immutable();
      }

      if (footSideSpot != null
         && !this.miner.hasTarget()
         && this.forceDigPos == null
         && this.pendingItem == null
         && !this.supportAwaitingConfirmation
         && this.crystalAfterSupport == null
         && !this.crystalAwaitingSpawn
         && this.ownCrystal == null) {
         BlockPos blockPos4 = footSideSpot.pos().immutable();
         if (this.lockedFootBase != null && !this.lockedFootBase.equals(blockPos4)) {
            BlockPos lockedFootBase3 = this.lockedFootBase;
            this.clearStickyIf(lockedFootBase3);
            if (lockedFootBase3.equals(this.cycleBase)) {
               this.cycleBase = null;
            }

            this.focusSpot = null;
            this.focusFails = 0;
         }

         this.lockedFootBase = blockPos4;
         this.failedCrystalBases.remove(blockPos4);
      }
   }

   public CrystalAuraModule() {
      super("Crystal Aura Combat", "Automates crystal targeting, placement and attacks", ModuleCategory.COMBAT);
      this.placeRange = this.add(new SliderSetting("Дальность установки", 4.5, 3.0, 6.0, 0.1));
      this.breakRange = this.add(new SliderSetting("Дальность взрыва", 3.0, 1.0, 6.0, 0.1));
      this.minDamage = this.add(new SliderSetting("Мин. урон врагу", 3.0, 1.0, 20.0, 0.5));
      this.maxSelfDamage = this.add(new SliderSetting("Макс. урон себе", 8.0, 0.0, 20.0, 0.5));
      this.delay = this.add(new SliderSetting("Задержка в тиках", 0.0, 0.0, 5.0, 1.0));
      this.rage = this.add(new BooleanSetting("Rage", false));
      this.hiding = this.add(new BooleanSetting("Скрытие", false));
      this.obsidian = this.add(new BooleanSetting("Ставить обсидиан", true));
      this.legit = this.add(new BooleanSetting("Легитный", false));
      this.movementCorrection = this.add(new ModeSetting("Коррекция движения", "Сфокусированная", "Выкл", "Сфокусированная"));
      this.autoDig = this.add(new BooleanSetting("Авто подкоп", true));
      this.vanillaAura = net.skycore.client.module.player.CrystalAuraModule.INSTANCE;
      this.supportTick = -100;
      this.pendingTick = -100;
      this.breakAfterMovementId = -1;
      this.aimedCrystalId = -1;
      this.aimedCrystalTick = -100;
      this.timer = new CooldownTimer();
      this.interaction = new AllowsHelper();
      this.lastCrystalActionTick = -100;
      this.displacedTick = -100;
      this.rage.visibleWhen(() -> {
         return this.auraMode.is("ReallyWorld");
      });
      this.hiding.visibleWhen(() -> {
         return this.auraMode.is("ReallyWorld");
      });
      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         this.onDisable();
         FixInputHelper.reset();
      });
      SkycoreClient.events().subscribe(PlayerInputEvent.class, FixInputHelper::fixInput);
      this.miner = MiningStrategy.INSTANCE;
      this.digOptions = new MiningPlanOptions();
      this.legitSlot = -1;
      this.failedDig = new HashMap<>();
      this.brokenDig = new HashMap<>();
      this.failedCrystalBases = new HashMap<>();
      this.ownCrystalTick = -100;
      this.requestedSupportTick = -100;
      this.pulses = new ArrayList<>();
   }

   private void expireDigState() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         Minecraft instance2 = Minecraft.getInstance();
         int value = instance2.player.tickCount;
         if (!this.failedDig.isEmpty()) {
            this.failedDig.values().removeIf(value2 -> {
               return value > value2;
            });
         }

         if (!this.brokenDig.isEmpty()) {
            var values = this.brokenDig.values();
            values.removeIf(value3 -> {
               return value - value3 > 60;
            });
         }

         if (!this.failedCrystalBases.isEmpty()) {
            var values2 = this.failedCrystalBases.values();
            Predicate<java.lang.Integer> predicate = value4 -> {
               return value > value4;
            };
            values2.removeIf(predicate);
         }
      }
   }

   private void resetAfterOwnSupport(BlockPos stickyBase2) {
      this.miner.release(this);
      this.clearPending();
      RotationClaim.release(this);
      this.crystalClaimFailures = 0;
      this.aimedCrystalId = -1;
      this.aimedCrystalTick = -100;
      this.failedDig.remove(stickyBase2);
      this.failedDig.remove(stickyBase2.above());
      this.brokenDig.remove(stickyBase2);
      this.brokenDig.remove(stickyBase2.above());
      this.focusSpot = null;
      this.focusFails = 0;
      this.emptyTicks = 0;
      this.noPlaceSinceMs = 0L;
      this.forceDigPos = null;
      this.footDigBase = null;
      this.footDigUpper = null;
      this.ownCrystal = null;
      this.crystalAttemptBase = null;
      this.crystalAttemptCount = 0;
      this.crystalAwaitingSpawn = false;
      this.stickyBase = stickyBase2.immutable();
      this.footCrystalAfter = stickyBase2.immutable();
      this.cycleBase = stickyBase2.immutable();
      this.lastCrystalActionTick = -100;
      this.noteSpotSuccess(stickyBase2);
   }

   private CrystalAuraModule.FootSpot footSpotForLockedBase(List elements) {
      if (this.lockedFootBase != null && Minecraft.getInstance().level != null) {
         Player player = this.nearestTarget(elements);
         if (player != null && this.isFootLevelBase(this.lockedFootBase, elements)) {
            BlockState blockState = Minecraft.getInstance().level.getBlockState(this.lockedFootBase);
            CrystalAuraModule.FootAction footAction;
            if (CrystalSimulation.isSupport(blockState)) {
               footAction = CrystalAuraModule.FootAction.CRYSTAL;
            } else if (blockState.canBeReplaced()) {
               footAction = CrystalAuraModule.FootAction.PLACE;
            } else {
               if (!this.isFootDigTarget(blockState, this.lockedFootBase)) {
                  return null;
               }

               footAction = CrystalAuraModule.FootAction.DIG;
            }

            return new CrystalAuraModule.FootSpot(CrystalSimulation.standingBlock(player), this.lockedFootBase.immutable(), 0.0F, footAction);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private void executePlacement(FirstDigHelper stickyBase2, boolean condition) {
      if (stickyBase2 != null) {
         if (!this.releasePlayerOccupiedSupport(stickyBase2.base())) {
            this.stickyBase = stickyBase2.base();
            if (this.pendingItem != null) {
               this.keepMiner();
            } else if (!condition) {
               this.keepMiner();
            } else {
               ActionReservation.touch();
               ActionReservation.reserve(stickyBase2.base());
               ActionReservation.reserve(stickyBase2.base().above());
               boolean condition2;
               if (stickyBase2.obsidian()) {
                  this.miner.release(this);
                  condition2 = this.queueSupport(stickyBase2.base(), Items.OBSIDIAN);
                  if (condition2) {
                     this.lastRequestedSupport = stickyBase2.base();
                     this.requestedSupportTick = Minecraft.getInstance().player.tickCount;
                     this.footCrystalAfter = stickyBase2.base().immutable();
                  } else {
                     this.noteSpotFailure(stickyBase2.base(), 3);
                  }
               } else {
                  this.miner.release(this);
                  condition2 = this.queueCrystal(stickyBase2.base());
               }

               if (condition2 && stickyBase2.obsidian()) {
                  this.timer.reset();
               }

               this.keepMiner();
            }
         }
      }
   }

   public void onSourcePostTick() {
      if (this.isEnabled()) {
         if (!this.auraMode.is("Vannila")) {
            if (!this.yieldingToWebTrap) {
               if (this.pendingItem == null) {
                  this.miner.tickMining();
               }

               VisualSlotHelper.INSTANCE.tick();
            }
         }
      }
   }

   private boolean recentlyBroken(BlockPos blockPos) {
      Integer value = this.brokenDig.get(blockPos);
      if (value != null) {
         if (Minecraft.getInstance().player.tickCount - value <= 60) {
            return true;
         }
      }

      return false;
   }

   private boolean awaitingSupport(BlockPos blockPos) {
      return CrystalFailureTracker.sameSupportCoolingDown(blockPos, this.lastPlacedSupport, Minecraft.getInstance().player.tickCount, this.supportTick);
   }

   private void clearLegitSlot() {
      this.restoreLegit();
      this.legitSlot = -1;
   }

   private float safeInteractionRange() {
      LocalPlayer instance = Minecraft.getInstance().player;
      return instance == null
         ? 3.0F
         : (float)Math.min(instance.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE), instance.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE));
   }

   private boolean footDigInProgressAt(BlockPos blockPos) {
      if (blockPos != null) {
         if (blockPos.equals(this.footDigBase)) {
            return true;
         }

         if (blockPos.equals(this.forceDigPos)) {
            MiningStrategy.State state = this.miner.getState();
            if (state != MiningStrategy.State.BROKEN) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean claimCrystalRotation(Vector2f angles) {
      if (!RotationClaim.claim(this, angles)) {
         if (++this.crystalClaimFailures >= 3) {
            RotationClaim.release(this);
            this.crystalClaimFailures = 0;
         }

         return false;
      } else {
         this.crystalClaimFailures = 0;
         float value = QueuePendingHelper.lastSentYaw();
         float value2 = QueuePendingHelper.lastSentPitch();
         if (Float.isFinite(value) && Float.isFinite(value2)) {
            float x = Math.abs(Mth.wrapDegrees(angles.x - value));
            float y = Math.abs(angles.y - value2);
            return x <= 3.0F && y <= 3.0F;
         } else {
            return false;
         }
      }
   }

   private void queuePlacement(BlockPos pendingBase2, Item pendingItem2, boolean pendingCrystal2) {
      this.pendingBase = pendingBase2;
      this.pendingItem = pendingItem2;
      this.pendingCrystal = pendingCrystal2;
      this.pendingTick = Minecraft.getInstance().player.tickCount;
   }

   private boolean noteSpotFailure(BlockPos blockPos, int value) {
      if (blockPos == null) {
         return false;
      }

      BlockPos blockPos2 = blockPos.immutable();
      if (!blockPos2.equals(this.focusSpot)) {
         this.focusSpot = blockPos2;
         this.focusFails = 0;
      }

      this.focusFails++;
      if (this.focusFails < Math.max(1, value)) {
         return false;
      }

      LocalPlayer instance = Minecraft.getInstance().player;
      this.failedCrystalBases.put(blockPos2, instance == null ? 0 : instance.tickCount);
      this.focusSpot = null;
      this.focusFails = 0;
      return true;
   }

   private Player nearestTarget(List elements) {
      if (elements != null && !elements.isEmpty()) {
         Player player = null;
         double mAX_VALUE = Double.MAX_VALUE;

         for (Object value : elements) {
            if (value instanceof Player player2) {
               double value2 = CrystalPlacementAimHelper.eyeDistance(player2);
               if (value2 < mAX_VALUE) {
                  mAX_VALUE = value2;
                  player = player2;
               }
            }
         }

         return player;
      } else {
         return null;
      }
   }

   private boolean tryStickyReadyExplode(float value, List elements, boolean condition) {
      BlockPos blockPos = this.pickStickyReadyBase();
      if (blockPos == null) {
         return false;
      }

      if (this.releasePlayerOccupiedSupport(blockPos)) {
         return false;
      }

      if (this.releaseItemBlockedCrystal(blockPos)) {
         return false;
      }

      if (!CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(blockPos))) {
         this.clearStickyIf(blockPos);
         return false;
      }

      float damage = this.supportDamage(blockPos);
      if (this.isBlacklisted(blockPos) && damage < 7.5F) {
         this.clearStickyIf(blockPos);
         if (blockPos.equals(this.lockedFootBase)) {
            this.lockedFootBase = null;
         }

         return false;
      } else {
         if (damage >= 7.5F) {
            this.failedDig.remove(blockPos);
            this.failedDig.remove(blockPos.above());
         }

         if (!blockPos.equals(this.lockedFootBase) && damage < this.minDamage.get().floatValue()) {
            this.clearStickyIf(blockPos);
            return false;
         }

         if (this.forceDigPos != null && !blockPos.equals(this.forceDigPos) && !blockPos.above().equals(this.forceDigPos)) {
            this.abortFootDig();
         }

         BlockPos blockPos2 = blockPos.above();
         BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos2);
         if (CrystalSimulation.occupied(blockPos2)) {
            this.displacedBase = blockPos.immutable();
            this.displacedDamage = damage;
            this.displacedTick = Minecraft.getInstance().player.tickCount;
            this.noteBlockedSpot(blockPos);
            this.clearStickyIf(blockPos);
            if (blockPos.equals(this.lockedFootBase)) {
               this.lockedFootBase = null;
            }

            return false;
         } else if (this.crystalCellBlocked(blockPos)) {
            this.noteBlockedSpot(blockPos);
            this.clearStickyIf(blockPos);
            if (blockPos.equals(this.lockedFootBase)) {
               this.lockedFootBase = null;
            }

            return false;
         } else if (!blockState.isAir()) {
            if (this.isFootDigTarget(blockState, blockPos2)) {
               this.footCrystalAfter = blockPos.immutable();
               this.stickyBase = blockPos.immutable();
               this.forceDigPos = blockPos2.immutable();
               this.footDigBase = blockPos.immutable();
               this.footDigUpper = blockPos2.immutable();
               this.requestDig(blockPos2);
               return true;
            }

            this.noteSpotFailure(blockPos);
            this.clearStickyIf(blockPos);
            if (blockPos.equals(this.lockedFootBase)) {
               this.lockedFootBase = null;
            }

            return false;
         } else if (this.pendingItem != null) {
            return true;
         } else if (this.awaitingSupport(blockPos)) {
            this.footCrystalAfter = blockPos.immutable();
            return true;
         } else if (!condition) {
            this.armCycleBase(blockPos);
            return true;
         } else {
            this.miner.release(this);
            this.footCrystalAfter = blockPos.immutable();
            this.stickyBase = blockPos.immutable();
            this.cycleBase = blockPos.immutable();
            return this.queueCrystal(blockPos) ? true : true;
         }
      }
   }

   public int visualActionSlot() {
      int value = 0;
      if (this.isVisualHidingEnabled()) {
         if (this.legitSlot >= 0) {
            if (Minecraft.getInstance().player != null) {
               Minecraft instance = Minecraft.getInstance();
               Inventory inventory = instance.player.getInventory();
               if (inventory.getSelectedSlot() == this.legitSlot) {
                  value = -1;
               } else {
                  value = this.legitSlot;
               }

               return value;
            }
         }
      }

      return -1;
   }

   private boolean hasAnyReadySupport(List elements, Vec3 position, float value, List elements2) {
      BlockPos blockPos = this.pickStickyReadyBase();
      if (blockPos != null && this.supportDealsEnough(blockPos)) {
         return true;
      }

      CrystalAuraModule.FootSpot readyFootCrystal = this.findReadyFootCrystal(elements, position, value, elements2);
      return readyFootCrystal != null;
   }

   private boolean isFootDigTarget(BlockState blockState, BlockPos blockPos) {
      if (CrystalSimulation.mineable(blockState) && !CrystalSimulation.isSupport(blockState)) {
         if (this.stickyBase != null) {
            if (this.stickyBase.equals(blockPos)) {
               return false;
            }
         }

         if (this.lastPlacedSupport != null && this.lastPlacedSupport.equals(blockPos)) {
            return false;
         }

         if (this.footCrystalAfter != null) {
            if (this.footCrystalAfter.equals(blockPos)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private void placeAfterForceDig(BlockPos lastRequestedSupport2) {
      this.noPlaceSinceMs = 0L;
      this.emptyTicks = 0;
      if (!this.releasePlayerOccupiedSupport(lastRequestedSupport2)) {
         if (Minecraft.getInstance().level.getBlockState(lastRequestedSupport2).canBeReplaced()) {
            if (this.queueSupport(lastRequestedSupport2, Items.OBSIDIAN)) {
               this.forceDigPos = null;
               this.lastRequestedSupport = lastRequestedSupport2;
               this.requestedSupportTick = Minecraft.getInstance().player.tickCount;
               this.stickyBase = lastRequestedSupport2.immutable();
               this.footCrystalAfter = lastRequestedSupport2.immutable();
               this.timer.reset();
            } else {
               this.forceDigPos = lastRequestedSupport2.immutable();
               this.footDigBase = lastRequestedSupport2.immutable();
            }
         } else if (CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(lastRequestedSupport2))) {
            this.forceDigPos = null;
            this.stickyBase = lastRequestedSupport2.immutable();
            this.footCrystalAfter = lastRequestedSupport2.immutable();
            this.noteSpotSuccess(lastRequestedSupport2);
         } else {
            this.forceDigPos = null;
         }
      }
   }

   private void noteSpotSuccess(BlockPos blockPos) {
      if (blockPos != null) {
         if (this.focusSpot != null) {
            if (this.focusSpot.equals(blockPos)) {
               this.focusSpot = null;
               this.focusFails = 0;
               return;
            }
         }
      }

      if (this.focusSpot == null) {
         this.focusFails = 0;
      }
   }

   private boolean crystalCycleReady() {
      if (Minecraft.getInstance().player == null) {
         return false;
      }

      Double value = this.delay.get();
      int value2 = value.intValue();
      int count = value2 == 2 ? 1 : value2;
      Minecraft instance = Minecraft.getInstance();
      return instance.player.tickCount - this.lastCrystalActionTick >= count;
   }

   private boolean tryBurrowFlatten(List elements, Vec3 position) {
      Player player = this.nearestTarget(elements);
      if (player == null || Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
         return false;
      }

      if (this.burrowFlattenActive && this.burrowLegsPos != null) {
         BlockState blockState = Minecraft.getInstance().level.getBlockState(this.burrowLegsPos);
         if (!blockState.isAir() && !blockState.canBeReplaced()) {
            if (MiningStrategy.State.BROKEN == this.miner.getState() && this.burrowLegsPos.equals(this.miner.getTargetBlock())) {
               this.burrowFlattenActive = false;
               this.burrowLegsPos = null;
               this.forceDigPos = null;
               this.miner.release(this);
               return false;
            } else {
               this.forceDigPos = this.burrowLegsPos;
               this.requestDig(this.burrowLegsPos);
               return true;
            }
         } else {
            if (this.forceDigPos != null && this.forceDigPos.equals(this.burrowLegsPos)) {
               this.forceDigPos = null;
            }

            this.burrowFlattenActive = false;
            this.burrowLegsPos = null;
            return false;
         }
      } else {
         if (!this.isFullyInsideBlocks(player)) {
            return false;
         }

         BlockPos blockPos = this.legsBlock(player);
         if (!this.isBlacklisted(blockPos) && !this.recentlyBroken(blockPos)) {
            BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos);
            if (blockState2.isAir() || blockState2.canBeReplaced() || CrystalSimulation.isSupport(blockState2)) {
               return false;
            }

            if (!CrystalSimulation.mineable(blockState2)) {
               return false;
            }

            double value = this.actionRange();
            if (CrystalSimulation.distanceSquared(position, blockPos) > value * value) {
               return false;
            }

            this.burrowFlattenActive = true;
            this.burrowLegsPos = blockPos.immutable();
            this.forceDigPos = blockPos.immutable();
            this.footDigBase = null;
            this.footDigUpper = null;
            this.requestDig(blockPos);
            return true;
         } else {
            return false;
         }
      }
   }

   private void showMiningPulse(BlockPos miningPulse2) {
      this.miningPulse = miningPulse2.immutable();
      float progress = Mth.clamp(this.miner.getBreakProgress(), 0.0F, 1.0F);
      this.miningProgress = progress;
      this.miningPulseTime = System.currentTimeMillis();
   }

   private boolean isBlacklisted(BlockPos blockPos) {
      Integer value = this.failedDig.get(blockPos);
      if (value == null) {
         return false;
      }

      if (Minecraft.getInstance().player != null) {
         if (Minecraft.getInstance().player.tickCount <= value) {
            return true;
         }
      }

      this.failedDig.remove(blockPos);
      return false;
   }

   public void onSourcePreTick() {
      if (this.isEnabled()) {
         this.crystalAfterMovement = null;
         this.breakAfterMovementId = -1;
         WebIgnoreModule.INSTANCE.requestTemp(20);
         if (this.auraMode.is("Vannila")) {
            if (!this.vanillaAura.isEnabled()) {
               this.resetReallyWorldRuntime();
               ClientModule.runSilently(() -> {
                  this.vanillaAura.setEnabled(true);
               });
            }
         } else {
            if (this.vanillaAura.isEnabled()) {
               ClientModule.runSilently(() -> {
                  this.vanillaAura.setEnabled(false);
               });
            }

            this.yieldingToWebTrap = false;
            if (Minecraft.getInstance().player != null
               && Minecraft.getInstance().level != null
               && Minecraft.getInstance().gameMode != null
               && !Minecraft.getInstance().player.isSpectator()) {
               this.expireDigState();
               AutoWebTrapModule.INSTANCE.runCombatTick();
               Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();
               var players = this.combatTargets();
               if (players.isEmpty()) {
                  AutoWebTrapModule.INSTANCE.runCombatTick();
                  this.stickyBase = null;
                  this.emptyTicks = 0;
                  this.noPlaceSinceMs = 0L;
                  this.forceDigPos = null;
                  this.footDigBase = null;
                  this.footDigUpper = null;
                  this.lockedFootBase = null;
                  this.refreshedFootSpot = null;
                  this.footCrystalAfter = null;
                  this.focusSpot = null;
                  this.focusFails = 0;
                  this.burrowLegsPos = null;
                  this.burrowFlattenActive = false;
                  this.cycleBase = null;
                  this.displacedBase = null;
                  this.displacedDamage = 0.0F;
                  this.displacedTick = -100;
                  this.supportAwaitingConfirmation = false;
                  this.crystalAfterSupport = null;
                  this.restoreLegit();
                  this.miner.release(this);
                  RotationClaim.release(this);
                  QueuePendingHelper.abandon();
                  return;
               }

               float instance = Math.max(0.0F, Minecraft.getInstance().player.getHealth() + Minecraft.getInstance().player.getAbsorptionAmount() - 1.0F);
               var players2 = CrystalPlacementAimHelper.friends(this.targetRange.get().floatValue() + 8.0F);
               float damage = this.actionRange();
               boolean crystalCycleReady = this.crystalCycleReady();
               int computedValue = CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, false) != -1 ? 1 : 0;
               int computedValue2 = CrystalPlacementAimHelper.findItem(Items.OBSIDIAN, false) != -1 ? 1 : 0;
               if (AutoWebTrapModule.INSTANCE.shouldDeferCrystalAura(players, crystalCycleReady, (computedValue != 0), (computedValue2 != 0), Minecraft.getInstance().player.tickCount, false)
                  )
                {
                  this.yieldingToWebTrap = true;
                  this.clearPending();
                  this.miner.release(this);
                  this.forceDigPos = null;
                  this.footDigBase = null;
                  this.footDigUpper = null;
                  this.lockedFootBase = null;
                  this.refreshedFootSpot = null;
                  this.focusSpot = null;
                  this.crystalAfterSupport = null;
                  this.supportAwaitingConfirmation = false;
                  this.supportStackTarget = null;
                  this.cycleBase = null;
                  this.stickyBase = null;
                  this.footCrystalAfter = null;
                  RotationClaim.release(this);
                  QueuePendingHelper.finish();
                  AutoWebTrapModule.INSTANCE.runCombatTick();
                  return;
               }

               if (this.resolveSupportAttempt()) {
                  ActionReservation.touch();
                  return;
               }

               if (this.tryCrystalImmediatelyAfterSupport()) {
                  ActionReservation.touch();
                  return;
               }

               CrystalDamageResult crystalDamageResult = CrystalSimulation.bestBreak(
                  players, eyePosition, damage, this.maxSelfDamage.get().floatValue(), this.minDamage.get().floatValue(), instance, players2
               );
               if (crystalDamageResult != null) {
                  this.abortFootDig();
                  this.miner.release(this);
                  ActionReservation.touch();
                  BlockPos x = BlockPos.containing(crystalDamageResult.crystal().getX(), crystalDamageResult.crystal().getY() - 0.2, crystalDamageResult.crystal().getZ());
                  if (CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(x))) {
                     this.armCycleBase(x);
                  }

                  if (this.attackCrystal(crystalDamageResult.crystal()) && computedValue != 0 && this.tryReplaceAfterExplode(this.crystalCycleReady())) {
                     return;
                  }

                  this.armCycleBase(x);
                  ActionReservation.touch();
                  return;
               }

               if (this.finishOwnCrystal(instance)) {
                  this.abortFootDig();
                  this.miner.release(this);
                  ActionReservation.touch();
                  if (computedValue != 0 && this.tryReplaceAfterExplode(this.crystalCycleReady())) {
                     return;
                  }

                  return;
               }

               if (computedValue != 0 && this.tryReplaceAfterExplode(crystalCycleReady)) {
                  ActionReservation.touch();
                  return;
               }

               FirstDigHelper firstDigHelper = null;
               if (computedValue != 0 && this.forceDigPos == null && this.footDigBase == null) {
                  var entityCollisionSnapshots = this.predictTargets(players);
                  float value = this.maxSelfDamage.get().floatValue();
                  float value2 = this.minDamage.get().floatValue();
                  firstDigHelper = CrystalSimulation.bestPlacement(
                     entityCollisionSnapshots, eyePosition, damage, damage, value, value2, this.obsidian.get() && computedValue2 != 0, instance, players2, this.digOptions(players)
                  );
               }

               if (computedValue != 0 && this.forceDigPos == null && this.footDigBase == null && firstDigHelper != null && this.runWorldPlan(firstDigHelper, crystalCycleReady)) {
                  ActionReservation.touch();
                  return;
               }

               this.emptyTicks++;
               if (this.emptyTicks > 2 || !this.continuePreviousDig()) {
                  this.stickyBase = null;
                  this.emptyTicks = 0;
                  this.miner.release(this);
                  RotationClaim.release(this);
                  QueuePendingHelper.abandon();
               }
            } else {
               this.miner.release(this);
            }
         }
      }
   }

   private EndCrystal findOwnCrystal(BlockPos blockPos) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1.0;
      double y2 = blockPos.getY() + 2.0;
      AABB bounds = new AABB(x, y, z, x2, y2, blockPos.getZ() + 1.0);
      ClientLevel instance = Minecraft.getInstance().level;
      Predicate<net.minecraft.world.entity.boss.enderdragon.EndCrystal> predicate = endCrystal -> {
         return true;
      };
      var entitiesOfClass = instance.getEntitiesOfClass(EndCrystal.class, bounds, predicate);
      return entitiesOfClass.isEmpty() ? null : (EndCrystal)entitiesOfClass.get(0);
   }

   private boolean commitCrystal(BlockPos ownCrystal2, boolean condition) {
      if (CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true) == -1) {
         return false;
      }

      boolean get = this.legit.get();
      if (get) {
         if (!this.equipLegit()) {
            return false;
         }
      } else {
         this.switchActionSlot(CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true));
      }

      this.interaction.begin(ownCrystal2);
      byte flag = 0;

      try {
         float value = this.actionRange();
         Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();
         CrystalPlacementAimHelper.aim(eyePosition, CrystalPlacementAimHelper.crystalPlacementAim(ownCrystal2));
         BlockHitResult blockHitResult = CrystalPlacementAimHelper.tryCrystalPick(ownCrystal2, value);
         if (blockHitResult == null) {
            blockHitResult = CrystalPlacementAimHelper.crystalPlacementHit(ownCrystal2);
         }

         if (blockHitResult != null
            && CrystalPlacementAimHelper.placementOk(ShouldDropFailedUseOnHelper.useItemOn(Minecraft.getInstance().player, InteractionHand.MAIN_HAND, blockHitResult))) {
            Minecraft.getInstance().player.swing(InteractionHand.MAIN_HAND);
            ActionReservation.action();
            RotationClaim.markAction();
            flag = 1;
         }
      } finally {
         this.interaction.end();
      }

      if (flag != 0) {
         this.displacedBase = null;
         this.displacedDamage = 0.0F;
         this.displacedTick = -100;
         this.ownCrystal = ownCrystal2.above();
         this.ownCrystalTick = Minecraft.getInstance().player.tickCount;
         if (this.crystalAttemptBase != null && this.crystalAttemptBase.equals(ownCrystal2)) {
            this.crystalAttemptCount++;
         } else {
            this.crystalAttemptBase = ownCrystal2.immutable();
            this.crystalAttemptCount = 1;
         }

         this.crystalAwaitingSpawn = true;
         this.cycleBase = ownCrystal2.immutable();
         this.stickyBase = ownCrystal2.immutable();
         this.footCrystalAfter = ownCrystal2.immutable();
         this.markCrystalAction();
         this.pulses.add(new CrystalAuraModule.PlacementPulse(ownCrystal2.above(), true, System.currentTimeMillis()));
         EndCrystal ownCrystal3 = this.findOwnCrystal(this.ownCrystal);
         if (ownCrystal3 != null && condition) {
            this.confirmCrystalSpawn(ownCrystal2);
            this.attackCrystal(ownCrystal3, this.breakRange.get().floatValue());
         }
      } else {
         this.recordCrystalPlacementFailure(ownCrystal2);
      }

      return (flag != 0);
   }

   private boolean releaseItemBlockedCrystal(BlockPos progress) {
      if (!this.onlyDroppedItemsBlockCrystal(progress)) {
         return false;
      }

      if (progress.equals(this.lockedFootBase)) {
         this.lockedFootBase = null;
      }

      this.clearStickyIf(progress);
      if (progress.equals(this.crystalAfterSupport)) {
         this.crystalAfterSupport = null;
      }

      if (progress.equals(this.cycleBase)) {
         this.cycleBase = null;
      }

      return true;
   }

   private boolean isFailedCrystalBase(BlockPos blockPos) {
      if (blockPos != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null) {
            Integer value = this.failedCrystalBases.get(blockPos);
            if (value != null) {
               Minecraft instance2 = Minecraft.getInstance();
               if (instance2.player.tickCount <= value) {
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   private boolean runWorldPlan(FirstDigHelper firstDigHelper, boolean condition) {
      if (firstDigHelper == null || firstDigHelper.base() == null) {
         return false;
      } else if (firstDigHelper.firstDig() != null) {
         this.requestDig(firstDigHelper.firstDig());
         return true;
      } else {
         this.executePlacement(firstDigHelper, condition);
         return true;
      }
   }

   private void primeCrystalRotation(BlockPos blockPos) {
      if (blockPos != null && CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true) != -1) {
         if (Minecraft.getInstance().player != null) {
            Vec3 position = CrystalPlacementAimHelper.crystalPlacementAim(blockPos);
            Vector2f instance = CrystalPlacementAimHelper.aim(Minecraft.getInstance().player.getEyePosition(), position);
            RotationClaim.reopenClaimForTick();
            RotationClaim.claim(this, instance);
            return;
         }
      }
   }

   private List<Player> combatTargets() {
      Double value = this.targetRange.get();
      var players = CrystalPlacementAimHelper.targets(value.floatValue());
      Player player = AutoWebTrapModule.INSTANCE.crystalAuraTarget();
      if (player != null) {
         if (players.contains(player)) {
            return List.of(player);
         }
      }

      return players;
   }

   private boolean commitSupport(BlockPos lastPlacedSupport2, Item item) {
      Minecraft instance = Minecraft.getInstance();
      if (lastPlacedSupport2 != null && item != null && instance.player != null && instance.level != null && instance.gameMode != null) {
         int item2 = CrystalPlacementAimHelper.findItem(item, false);
         if (item2 < 0) {
            return false;
         }

         BlockAimTarget eyePosition = CrystalSimulation.placementFace(lastPlacedSupport2, instance.player.getEyePosition(), this.actionRange());
         if (eyePosition == null) {
            return false;
         }

         Vector2f eyePosition2 = CrystalPlacementAimHelper.aim(instance.player.getEyePosition(), eyePosition.aim());
         if (!this.claimCrystalRotation(eyePosition2)) {
            return false;
         }

         BlockHitResult eyePosition3 = CrystalPlacementAimHelper.trySupportPick(lastPlacedSupport2, eyePosition.block(), instance.player.getEyePosition(), this.actionRange());
         if (eyePosition3 == null) {
            return false;
         }

         this.switchActionSlot(item2);
         this.interaction.begin(lastPlacedSupport2);

         boolean condition;
         try {
            condition = CrystalPlacementAimHelper.placementOk(ShouldDropFailedUseOnHelper.useItemOn(instance.player, InteractionHand.MAIN_HAND, eyePosition3));
         } finally {
            this.interaction.end();
         }

         if (!condition) {
            return false;
         }

         instance.player.swing(InteractionHand.MAIN_HAND);
         ActionReservation.action();
         RotationClaim.markAction();
         this.lastPlacedSupport = lastPlacedSupport2.immutable();
         this.supportTick = instance.player.tickCount;
         this.noteSpotSuccess(lastPlacedSupport2);
         this.markCrystalAction();
         return true;
      } else {
         return false;
      }
   }

   private void blacklistDig(BlockPos blockPos) {
      if (blockPos != null && Minecraft.getInstance().player != null) {
         var failedDig2 = this.failedDig;
         BlockPos blockPos2 = blockPos.immutable();
         int instance = Minecraft.getInstance().player.tickCount + 8;
         failedDig2.put(blockPos2, instance);
      }
   }

   public void renderWorld() {
      if (this.isVanillaModeActive()) {
         this.vanillaAura.renderWorld();
      } else if (this.isEnabled() && mc.level != null) {
         long now = System.currentTimeMillis();
         AddQuadHelper.beginFrame();
         this.pulses.removeIf(placementPulse -> {
            return now - placementPulse.at() >= 300L;
         });

         for (CrystalAuraModule.PlacementPulse placementPulse2 : this.pulses) {
            float computedValue = (float)(now - placementPulse2.at()) / 300.0F;
            int value = placementPulse2.crystal() ? 11814655 : 2981375;
            int computedValue2 = (int)((1.0F - computedValue) * 200.0F) << 24 | value;
            AddQuadHelper.addFilledBox(this.pulseBox(placementPulse2.pos(), 0.4F + 0.6F * computedValue), computedValue2);
         }

         if (this.miningPulse != null) {
            float alpha = Math.max(0.0F, 1.0F - (float)Math.max(0L, now - this.miningPulseTime - 140L) / 160.0F);
            if (alpha == 0.0F) {
               this.miningPulse = null;
            } else {
               AddQuadHelper.addFilledBox(
                  this.pulseBox(this.miningPulse, 0.55F + 0.45F * this.miningProgress), miningAlpha(this.miningProgress, alpha) << 24 | 16725815
               );
            }
         }

         AddQuadHelper.endFrame();
      } else {
         this.pulses.clear();
         this.miningPulse = null;
      }
   }

   private CrystalAuraModule.FootSpot findFootSideSpot(List elements, Vec3 position, float value, List elements2) {
      Player player = this.nearestTarget(elements);
      if (player != null && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
         BlockPos blockPos = CrystalSimulation.standingBlock(player);
         int y = blockPos.getY();
         BlockPos instance = CrystalSimulation.standingBlock(Minecraft.getInstance().player);
         int y2 = instance.getY() == y ? 1 : 0;
         double value2 = this.actionRange();
         double distance = value2 * value2;
         float value3 = this.maxSelfDamage.get().floatValue();
         float value4 = this.minDamage.get().floatValue();
         AABB boundingBox = player.getBoundingBox();
         AABB boundingBox2 = Minecraft.getInstance().player.getBoundingBox();
         CrystalAuraModule.FootSpot footSpot = null;
         float computedValue = -Float.MAX_VALUE;
         float mAX_VALUE = Float.MAX_VALUE;
         CrystalAuraModule.FootSpot footSpot2 = null;
         float computedValue2 = -Float.MAX_VALUE;
         CrystalAuraModule.FootSpot footSpot3 = null;
         float computedValue3 = -Float.MAX_VALUE;
         Iterator iterator = List.of(
               new CrystalAuraModule.DirectionalFootChoice(blockPos.north(), false),
               new CrystalAuraModule.DirectionalFootChoice(blockPos.south(), false),
               new CrystalAuraModule.DirectionalFootChoice(blockPos.west(), false),
               new CrystalAuraModule.DirectionalFootChoice(blockPos.east(), false)
            )
            .iterator();

         while (true) {
            BlockPos blockPos2;
            CrystalAuraModule.FootAction footAction;
            while (true) {
               if (!iterator.hasNext()) {
                  if (footSpot2 != null && footSpot3 != null) {
                     return computedValue2 >= computedValue3 ? footSpot2 : footSpot3;
                  }

                  if (footSpot2 != null) {
                     return footSpot2;
                  }

                  if (footSpot3 != null) {
                     return footSpot3;
                  }

                  return footSpot;
               }

               CrystalAuraModule.DirectionalFootChoice element = (CrystalAuraModule.DirectionalFootChoice)iterator.next();
               blockPos2 = element.pos();
               if (!this.isBlacklisted(blockPos2)
                  && !(CrystalSimulation.distanceSquared(position, blockPos2) > distance)
                  && !CrystalSimulation.occupiedExceptItems(blockPos2.above())) {
                  BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos2);
                  if (element.digSupport()) {
                     footAction = CrystalAuraModule.FootAction.DIG_SUPPORT;
                     break;
                  }

                  if (CrystalSimulation.isSupport(blockState)) {
                     footAction = CrystalAuraModule.FootAction.CRYSTAL;
                     break;
                  }

                  if (blockState.canBeReplaced()) {
                     footAction = CrystalAuraModule.FootAction.PLACE;
                     break;
                  }

                  if (this.isFootDigTarget(blockState, blockPos2)) {
                     footAction = CrystalAuraModule.FootAction.DIG;
                     break;
                  }
               }
            }

            Vec3 position2 = CrystalSimulation.blast(blockPos2);
            BlockPos blockPos3 = blockPos2.above();
            BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos3);
            boolean air = blockState2.isAir();
            ArrayList elements3 = new ArrayList(2);
            if (footAction == CrystalAuraModule.FootAction.DIG) {
               elements3.add(blockPos2);
            }

            if (!air && this.isFootDigTarget(blockState2, blockPos3)) {
               elements3.add(blockPos3);
            }

            int value5 = footAction != CrystalAuraModule.FootAction.CRYSTAL ? 1 : 0;
            float damage = value5 != 0
               ? CrystalSimulation.damageWithVirtualSupport(position2, player, boundingBox, blockPos2, elements3)
               : CrystalSimulation.damageWithDig(position2, player, boundingBox, elements3);
            float instance2 = value5 != 0
               ? CrystalSimulation.damageWithVirtualSupport(position2, Minecraft.getInstance().player, boundingBox2, blockPos2, elements3)
               : CrystalSimulation.damageWithDig(position2, Minecraft.getInstance().player, boundingBox2, elements3);
            float value6 = 0.0F;
            if (elements2 != null) {
               for (Object value7 : elements2) {
                  if (value7 instanceof LivingEntity entity && entity != Minecraft.getInstance().player) {
                     float boundingBox3 = value5 != 0
                        ? CrystalSimulation.damageWithVirtualSupport(position2, entity, entity.getBoundingBox(), blockPos2, elements3)
                        : CrystalSimulation.damageWithDig(position2, entity, entity.getBoundingBox(), elements3);
                     value6 = Math.max(value6, boundingBox3);
                  }
               }
            }

            int y3 = blockPos.getY() < instance.getY() ? 1 : 0;
            if (y3 != 0
                  && (
                     footAction == CrystalAuraModule.FootAction.DIG
                        || footAction == CrystalAuraModule.FootAction.DIG_SUPPORT
                        || footAction == CrystalAuraModule.FootAction.PLACE
                  )
               ? !(instance2 >= value) && !(damage < value4)
               : !CrystalFailureTracker.rejected(instance2, value6, damage, value3, value) && !(damage < value4)) {
               float computedValue4 = value - instance2;
               float computedValue5 = computedValue4 * 25.0F + damage;
               if (this.displacedBase != null && Minecraft.getInstance().player.tickCount - this.displacedTick <= 40) {
                  double computedValue6 = Math.sqrt(blockPos2.distSqr(this.displacedBase));
                  float computedValue7 = Math.abs(damage - this.displacedDamage);
                  computedValue5 += Math.max(0.0F, 64.0F - (float)computedValue6 * 16.0F - computedValue7 * 7.0F);
               }

               boolean faceCover = CrystalSimulation.hasFaceCover(position, blockPos2);
               float value8 = CrystalSimulation.coverAdvantage(position2, boundingBox2, boundingBox);
               if (y2 != 0) {
                  if (!faceCover && !(value8 > 0.15F)) {
                     computedValue5 -= 10.0F;
                  } else {
                     computedValue5 += 40.0F + value8 * 45.0F;
                  }
               }

               if (y3 != 0) {
                  computedValue5 += 20.0F;
               }

               if (footAction == CrystalAuraModule.FootAction.CRYSTAL) {
                  computedValue5 += 0.05F;
                  if (air && damage >= value4 && computedValue5 > computedValue2) {
                     computedValue2 = computedValue5;
                     footSpot2 = new CrystalAuraModule.FootSpot(blockPos, blockPos2.immutable(), damage, footAction);
                  } else if (!air && damage >= value4 && this.isFootDigTarget(blockState2, blockPos3)) {
                     float value9 = CrystalSimulation.digTicks(blockPos3);
                     computedValue5 += 50.0F - value9 * 0.15F;
                     if (computedValue5 > computedValue2) {
                        computedValue2 = computedValue5;
                        footSpot2 = new CrystalAuraModule.FootSpot(blockPos, blockPos2.immutable(), damage, footAction);
                     }
                  }
               } else if (footAction == CrystalAuraModule.FootAction.PLACE) {
                  computedValue5 += 0.02F;
                  if (!air && this.isFootDigTarget(blockState2, blockPos3)) {
                     computedValue5 -= CrystalSimulation.digTicks(blockPos3) * 0.15F;
                  }

                  if (damage >= value4 && computedValue5 > computedValue3) {
                     computedValue3 = computedValue5;
                     footSpot3 = new CrystalAuraModule.FootSpot(blockPos, blockPos2.immutable(), damage, footAction);
                  }
               } else {
                  float value10 = CrystalSimulation.digTicks(blockPos2);
                  if (!air && this.isFootDigTarget(blockState2, blockPos3)) {
                     value10 += CrystalSimulation.digTicks(blockPos3);
                  }

                  computedValue5 -= value10 * 0.25F;
                  if (value10 < mAX_VALUE - 0.25F || Math.abs(value10 - mAX_VALUE) <= 0.25F && computedValue5 > computedValue) {
                     mAX_VALUE = value10;
                     computedValue = computedValue5;
                     footSpot = new CrystalAuraModule.FootSpot(blockPos, blockPos2.immutable(), damage, footAction);
                  }
               }
            }
         }
      } else {
         return null;
      }
   }

   private boolean queueCrystal(BlockPos crystalAfterMovement2) {
      if (this.releasePlayerOccupiedSupport(crystalAfterMovement2)) {
         return false;
      }

      if (this.releaseItemBlockedCrystal(crystalAfterMovement2)) {
         return false;
      }

      int item = CrystalPlacementAimHelper.findItem(Items.END_CRYSTAL, true);
      if (item == -1) {
         String text = "кристалл";
         String text2 = "нет в инвентаре";
         return false;
      }

      if (this.awaitingSupport(crystalAfterMovement2)) {
         String text3 = "кристалл";
         String text4 = "опора ещё не подтверждена сервером";
         return false;
      }

      Vec3 position = CrystalPlacementAimHelper.crystalPlacementAim(crystalAfterMovement2);
      Vector2f instance = CrystalPlacementAimHelper.aim(Minecraft.getInstance().player.getEyePosition(), position);
      if (!this.claimCrystalRotation(instance)) {
         if (RotationClaim.claimedRecently(this, 0)) {
            this.crystalAfterMovement = crystalAfterMovement2.immutable();
            return true;
         } else {
            return false;
         }
      } else {
         return this.commitCrystal(crystalAfterMovement2);
      }
   }

   private boolean queueSupport(BlockPos supportStackTarget2, Item item) {
      if (this.pendingItem != null) {
         return false;
      }

      if (this.releasePlayerOccupiedSupport(supportStackTarget2)) {
         return false;
      }

      if (this.supportStackTarget != null && !supportStackTarget2.equals(this.supportStackTarget)) {
         this.supportStackTarget = null;
      }

      float value = this.actionRange();
      BlockAimTarget instance = CrystalSimulation.placementFace(supportStackTarget2, Minecraft.getInstance().player.getEyePosition(), value);
      if (instance == null) {
         BlockPos lowestVanillaStackCell = this.findLowestVanillaStackCell(supportStackTarget2, value);
         if (lowestVanillaStackCell == null || lowestVanillaStackCell.equals(supportStackTarget2)) {
            return false;
         }

         this.supportStackTarget = supportStackTarget2.immutable();
         supportStackTarget2 = lowestVanillaStackCell;
         instance = CrystalSimulation.placementFace(supportStackTarget2, Minecraft.getInstance().player.getEyePosition(), value);
         if (instance == null) {
            return false;
         }
      }

      int item2 = CrystalPlacementAimHelper.findItem(item, true);
      if (item2 == -1) {
         String text = "обсидиан";
         return false;
      } else {
         Vector2f instance2 = CrystalPlacementAimHelper.aim(Minecraft.getInstance().player.getEyePosition(), instance.aim());
         if (this.claimCrystalRotation(instance2)) {
            this.queuePlacement(supportStackTarget2, item, false);
            return true;
         } else if (RotationClaim.claimedRecently(this, 0)) {
            this.queuePlacement(supportStackTarget2, item, false);
            return true;
         } else {
            return false;
         }
      }
   }

   private boolean supportDealsEnough(BlockPos blockPos) {
      float damage = this.supportDamage(blockPos);
      return damage >= this.minDamage.get().floatValue();
   }

   private List<EntityCollisionSnapshot> predictTargets(List<Player> players) {
      ArrayList elements = new ArrayList(players);
      elements.sort(Comparator.<Player>comparingDouble(player -> {
         return CrystalPlacementAimHelper.eyeDistance(player);
      }).thenComparing((player2, player3) -> {
         float absorptionAmount = player3.getHealth() + player3.getAbsorptionAmount();
         float absorptionAmount2 = player2.getHealth() + player2.getAbsorptionAmount();
         return Float.compare(absorptionAmount, absorptionAmount2);
      }));
      ArrayList elements2 = new ArrayList();
      int computedValue = Math.min(elements.size(), 6);

      for (int value = 0; value < computedValue; value++) {
         Player player4 = (Player)elements.get(value);
         AABB bounds;
         if (!CrystalPlacementAimHelper.containsBlock(player4.getBoundingBox(), Blocks.COBWEB)) {
            PredictHelper predictHelper = PredictHelper.predict(player4, 2);
            bounds = predictHelper.box != null ? predictHelper.box : player4.getBoundingBox();
         } else {
            bounds = player4.getBoundingBox();
         }

         elements2.add(new EntityCollisionSnapshot(player4, bounds));
      }

      return elements2;
   }

   private void confirmCrystalSpawn(BlockPos blockPos) {
      this.crystalAwaitingSpawn = false;
      this.crystalAttemptBase = null;
      this.crystalAttemptCount = 0;
      if (blockPos != null) {
         this.failedCrystalBases.remove(blockPos);
         if (blockPos.equals(this.crystalAfterSupport)) {
            this.crystalAfterSupport = null;
         }

         this.noteSpotSuccess(blockPos);
      }
   }

   private void armCycleBase(BlockPos blockPos) {
      if (blockPos != null) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null) {
            BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
            if (!CrystalSimulation.isSupport(blockState)) {
               return;
            }

            BlockPos blockPos2 = blockPos.immutable();
            this.cycleBase = blockPos2;
            BlockPos blockPos3 = blockPos.immutable();
            this.stickyBase = blockPos3;
            BlockPos blockPos4 = blockPos.immutable();
            this.footCrystalAfter = blockPos4;
            return;
         }
      }
   }

   private boolean onlyDroppedItemsBlockCrystal(BlockPos blockPos) {
      if (blockPos != null) {
         if (CrystalSimulation.occupied(blockPos.above()) && !CrystalSimulation.occupiedExceptItems(blockPos.above())) {
            return true;
         }
      }

      return false;
   }

   private void markCrystalAction() {
      if (Minecraft.getInstance().player != null) {
         this.lastCrystalActionTick = Minecraft.getInstance().player.tickCount;
      }

      this.timer.reset();
   }

   private FirstDigHelper bestWorldSupport(List<EntityCollisionSnapshot> entityCollisionSnapshots, Vec3 position, float value, List elements) {
      if (Minecraft.getInstance().level != null && Minecraft.getInstance().player != null && entityCollisionSnapshots != null && !entityCollisionSnapshots.isEmpty()) {
         float value2 = this.actionRange();
         float value3 = value2;
         double distance = value2 * value2;
         double distance2 = value3 * value3;
         float value4 = this.minDamage.get().floatValue();
         float value5 = this.maxSelfDamage.get().floatValue();
         int computedValue = Mth.ceil(value2) + 1;
         BlockPos instance = Minecraft.getInstance().player.blockPosition();
         float computedValue2 = -Float.MAX_VALUE;
         FirstDigHelper firstDigHelper = null;
         float computedValue3 = -Float.MAX_VALUE;

         for (int x = instance.getX() - computedValue; x <= instance.getX() + computedValue; x++) {
            for (int y = instance.getY() - computedValue; y <= instance.getY() + computedValue; y++) {
               for (int z = instance.getZ() - computedValue; z <= instance.getZ() + computedValue; z++) {
                  BlockPos blockPos = new BlockPos(x, y, z);
                  if (!this.isFailedCrystalBase(blockPos)
                     && CrystalSimulation.isSupport(Minecraft.getInstance().level.getBlockState(blockPos))
                     && !(CrystalSimulation.distanceSquared(position, blockPos) > distance)) {
                     Vec3 position2 = CrystalSimulation.blast(blockPos);
                     if (!(position.distanceToSqr(position2) > distance2)
                        && Minecraft.getInstance().level.getBlockState(blockPos.above()).isAir()
                        && !CrystalSimulation.occupied(blockPos.above())) {
                        float damage = 0.0F;
                        double mAX_VALUE = Double.MAX_VALUE;
                        byte flag = 0;

                        for (EntityCollisionSnapshot entityCollisionSnapshot : entityCollisionSnapshots) {
                           float value6 = CrystalSimulation.damage(position2, entityCollisionSnapshot.entity, entityCollisionSnapshot.box);
                           if (value6 > damage) {
                              damage = value6;
                           }

                           mAX_VALUE = Math.min(mAX_VALUE, position2.distanceTo(entityCollisionSnapshot.feet));
                           BlockPos blockPos2 = CrystalSimulation.standingBlock(entityCollisionSnapshot.entity);
                           int x2 = Math.abs(blockPos.getX() - blockPos2.getX());
                           int z2 = Math.abs(blockPos.getZ() - blockPos2.getZ());
                           if (blockPos.getY() == blockPos2.getY() && x2 + z2 == 1) {
                              flag = 1;
                           }
                        }

                        if (!(damage < value4)) {
                           float instance2 = CrystalSimulation.damage(position2, Minecraft.getInstance().player, Minecraft.getInstance().player.getBoundingBox());
                           float value7 = CrystalSimulation.friendDamage(position2, elements);
                           if (!CrystalFailureTracker.rejected(instance2, value7, damage, value5, value)) {
                              double computedValue4 = Math.sqrt(CrystalSimulation.distanceSquared(position, blockPos));
                              float damage2 = damage * 12.0F - instance2 * 8.0F - value7 * 5.0F - (float)computedValue4 * 3.0F - (float)mAX_VALUE * 4.0F;
                              if (this.displacedBase != null && Minecraft.getInstance().player.tickCount - this.displacedTick <= 40) {
                                 double computedValue5 = Math.sqrt(blockPos.distSqr(this.displacedBase));
                                 damage2 += Math.max(0.0F, 48.0F - (float)computedValue5 * 12.0F - Math.abs(damage - this.displacedDamage) * 5.0F);
                              }

                              if (flag != 0 && damage2 > computedValue3) {
                                 computedValue3 = damage2;
                                 firstDigHelper = new FirstDigHelper(blockPos.immutable(), position2, false, List.of(), damage, instance2);
                              } else if (flag == 0 && damage2 > computedValue2) {
                                 computedValue2 = damage2;
                                 new FirstDigHelper(blockPos.immutable(), position2, false, List.of(), damage, instance2);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return firstDigHelper;
      } else {
         return null;
      }
   }

   public boolean isRotating() {
      byte flag = 0;
      flag = 0;
      if (this.isVanillaModeActive()) {
         return this.vanillaAura.isVanillaRotating();
      }

      targetSelection: {
         if (!RotationClaim.claimedRecently(this, 2)) {
            if (!RotationClaim.claimedRecently(this.miner, 2)) {
               flag = 0;
               break targetSelection;
            }
         }

         flag = 1;
      }

      byte flag2 = flag;
      return this.isReallyWorldModeActive() && (this.isBusy() || flag2 != 0 || QueuePendingHelper.ownsRotationLoop());
   }

   private boolean attackCrystal(EndCrystal endCrystal) {
      return this.attackCrystal(endCrystal, this.actionRange());
   }

   private boolean attackCrystal(EndCrystal breakAfterMovementId2, float breakAfterMovementRange2) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && breakAfterMovementId2 != null && breakAfterMovementId2.isAlive() && instance.player != null && instance.level != null && instance.gameMode != null && !(breakAfterMovementRange2 <= 0.0F)) {
         if (instance.player.getEyePosition().distanceToSqr(breakAfterMovementId2.position()) > breakAfterMovementRange2 * breakAfterMovementRange2) {
            return false;
         } else {
            Vector2f eyePosition = CrystalPlacementAimHelper.aim(instance.player.getEyePosition(), breakAfterMovementId2.position());
            if (!this.claimCrystalRotation(eyePosition)) {
               return false;
            } else if (!RotationClaim.hasSentRotation(eyePosition)) {
               this.breakAfterMovementId = breakAfterMovementId2.getId();
               this.breakAfterMovementRange = breakAfterMovementRange2;
               return true;
            } else {
               instance.gameMode.attack(instance.player, breakAfterMovementId2);
               instance.player.swing(InteractionHand.MAIN_HAND);
               RotationClaim.markAction();
               this.aimedCrystalId = breakAfterMovementId2.getId();
               this.aimedCrystalTick = instance.player.tickCount;
               this.markCrystalAction();
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private boolean finishOwnCrystal(float damage) {
      if (this.ownCrystal == null) {
         return false;
      }

      if (Minecraft.getInstance().player.tickCount - this.ownCrystalTick > 20) {
         this.armCycleBase(this.ownCrystal.below());
         this.ownCrystal = null;
         return false;
      }

      EndCrystal ownCrystal2 = this.findOwnCrystal(this.ownCrystal);
      if (ownCrystal2 == null) {
         int instance = Minecraft.getInstance().player.tickCount - this.ownCrystalTick;
         if (this.crystalAwaitingSpawn) {
            if (instance < 3) {
               return true;
            }

            BlockPos blockPos = this.ownCrystal.below();
            this.crystalAwaitingSpawn = false;
            this.ownCrystal = null;
            if (this.crystalAttemptCount >= 2) {
               this.crystalAttemptBase = null;
               this.crystalAttemptCount = 0;
               RotationClaim.release(this);
            }

            this.armCycleBase(blockPos);
            return true;
         } else {
            this.armCycleBase(this.ownCrystal.below());
            this.ownCrystal = null;
            return false;
         }
      } else {
         this.confirmCrystalSpawn(this.ownCrystal.below());
         float value = this.actionRange();
         if (CrystalSimulation.distanceSquared(Minecraft.getInstance().player.getEyePosition(), ownCrystal2.getBoundingBox()) > value * value) {
            this.ownCrystal = null;
            return false;
         } else if (CrystalFailureTracker.lethal(CrystalSimulation.damage(ownCrystal2.position(), Minecraft.getInstance().player), damage)) {
            this.ownCrystal = null;
            return false;
         } else {
            this.armCycleBase(this.ownCrystal.below());
            return this.attackCrystal(ownCrystal2, value);
         }
      }
   }

   private void rememberActionSlot() {
      int slot = -1;
      slot = (byte)1;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (this.legitSlot == -1) {
            Minecraft instance2 = Minecraft.getInstance();
            Inventory inventory = instance2.player.getInventory();
            slot = inventory.getSelectedSlot();
            this.legitSlot = slot;
         }

         Boolean get = this.hiding.get();
         if (get) {
            SpoofElytraAsChestplateHelper.hold(this.legitSlot, 8);
         }
      }
   }

   private MiningPlanOptions digOptions(List elements) {
      this.digOptions.enabled = this.autoDig.get();
      this.digOptions.currentBlock = this.miner.getTargetBlock();
      this.digOptions.progress = this.miner.getBreakProgress();
      this.digOptions.blacklist = this.failedDig.keySet();
      this.digOptions.stickyBase = this.stickyBase != null ? this.stickyBase : this.recentSupport();
      this.applyReplacementPreference();
      this.digOptions.preferMaximumDamage = this.rage.get();
      this.digOptions.fastExposure = true;
      this.digOptions.maxBlocks = 4;
      this.digOptions.maxAttempts = 16;
      this.digOptions.footY = Integer.MIN_VALUE;
      this.digOptions.standBlock = null;
      return this.digOptions;
   }

   private float actionRange() {
      return Boolean.TRUE.equals(this.rage.get()) ? 6.0F : this.safeInteractionRange();
   }

   public boolean focusedMovementCorrection() {
      if (this.isReallyWorldModeActive()) {
         if (this.movementCorrection.is("Сфокусированная")) {
            return true;
         }
      }

      return false;
   }

   private void abandonFocusSpot() {
      BlockPos focusSpot2 = this.focusSpot;
      this.focusSpot = null;
      this.focusFails = 0;
      if (focusSpot2 != null) {
         this.blacklistDig(focusSpot2);
         this.blacklistDig(focusSpot2.above());
         this.clearStickyIf(focusSpot2);
         if (focusSpot2.equals(this.lockedFootBase)) {
            this.lockedFootBase = null;
         }

         if (focusSpot2.equals(this.forceDigPos) || focusSpot2.equals(this.footDigBase) || focusSpot2.equals(this.footDigUpper)) {
            this.forceDigPos = null;
            this.footDigBase = null;
            this.footDigUpper = null;
         }
      }

      this.abortFootDig();
      this.clearPending();
      this.emptyTicks = 0;
   }

   private BlockPos legsBlock(Player player) {
      double x = player.getX();
      double y = player.getY() + 0.05;
      double z = player.getZ();
      return BlockPos.containing(x, y, z);
   }

   public boolean isVisualHidingEnabled() {
      if (this.isReallyWorldModeActive()) {
         Boolean get = this.hiding.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public boolean isReallyWorldModeActive() {
      if (this.isEnabled()) {
         if (this.auraMode.is("ReallyWorld")) {
            return true;
         }
      }

      return false;
   }

   private void suppressFailedCrystalBase(BlockPos blockPos) {
      if (blockPos != null && Minecraft.getInstance().player != null) {
         BlockPos blockPos2 = blockPos.immutable();
         this.failedCrystalBases.put(blockPos2, Minecraft.getInstance().player.tickCount + 40);
         this.failedDig.put(blockPos2, Minecraft.getInstance().player.tickCount + 40);
         this.crystalAttemptBase = null;
         this.crystalAttemptCount = 0;
         this.crystalAwaitingSpawn = false;
         this.ownCrystal = null;
         this.clearStickyIf(blockPos2);
         if (blockPos2.equals(this.lockedFootBase)) {
            this.lockedFootBase = null;
         }

         if (blockPos2.equals(this.lastPlacedSupport)) {
            this.lastPlacedSupport = null;
            this.supportTick = -100;
         }

         if (blockPos2.equals(this.focusSpot)) {
            this.focusSpot = null;
            this.focusFails = 0;
         }

         this.aimedCrystalId = -1;
         this.aimedCrystalTick = -100;
         RotationClaim.release(this);
      }
   }

   private boolean finishForceDig(boolean condition) {
      if (this.forceDigPos == null) {
         return false;
      }

      if (this.burrowFlattenActive && this.forceDigPos.equals(this.burrowLegsPos)) {
         BlockPos forceDigPos2 = this.forceDigPos;
         BlockState blockState = Minecraft.getInstance().level.getBlockState(forceDigPos2);
         if (!blockState.isAir() && !blockState.canBeReplaced() && (MiningStrategy.State.BROKEN != this.miner.getState() || !forceDigPos2.equals(this.miner.getTargetBlock()))) {
            if (!CrystalSimulation.mineable(blockState)) {
               this.miner.release(this);
               this.burrowFlattenActive = false;
               this.burrowLegsPos = null;
               this.forceDigPos = null;
               return true;
            } else {
               return false;
            }
         } else {
            this.brokenDig.put(forceDigPos2.immutable(), Minecraft.getInstance().player.tickCount);
            this.miner.release(this);
            this.burrowFlattenActive = false;
            this.burrowLegsPos = null;
            this.forceDigPos = null;
            return true;
         }
      } else {
         BlockPos forceDigPos3 = this.forceDigPos;
         BlockState blockState2 = Minecraft.getInstance().level.getBlockState(forceDigPos3);
         if ((blockState2.isAir() || blockState2.canBeReplaced()) && forceDigPos3.equals(this.miner.getTargetBlock()) && this.miner.getState() != MiningStrategy.State.BROKEN) {
            return true;
         }

         if (CrystalSimulation.isSupport(blockState2) && !this.isFootDigCell(forceDigPos3)) {
            this.miner.release(this);
            this.forceDigPos = null;
            this.footCrystalAfter = forceDigPos3.immutable();
            return true;
         }

         if (MiningStrategy.State.BROKEN == this.miner.getState() && forceDigPos3.equals(this.miner.getTargetBlock())) {
            this.brokenDig.put(forceDigPos3.immutable(), Minecraft.getInstance().player.tickCount);
            this.miner.release(this);
            if (!blockState2.isAir() && !blockState2.canBeReplaced()) {
               return true;
            }

            this.finishFootDig(forceDigPos3, condition);
            return true;
         } else if (!CrystalSimulation.mineable(blockState2) || CrystalSimulation.isSupport(blockState2) && !this.isFootDigCell(forceDigPos3)) {
            this.miner.release(this);
            if (!blockState2.canBeReplaced() || !forceDigPos3.equals(this.footDigBase) && !forceDigPos3.equals(this.footDigUpper)) {
               if (blockState2.canBeReplaced() && condition) {
                  this.placeAfterForceDig(forceDigPos3);
               } else if (CrystalSimulation.isSupport(blockState2)) {
                  this.forceDigPos = null;
                  this.footCrystalAfter = forceDigPos3.immutable();
               } else {
                  this.forceDigPos = null;
               }
            } else {
               this.finishFootDig(forceDigPos3, condition);
            }

            return true;
         } else {
            return false;
         }
      }
   }

   private void keepMiner() {
      if (this.miner.hasTarget()) {
         if (this.miner.isProgressStalled()) {
            this.miner.release(this);
         } else {
            this.miner.keepAlive(this);
         }
      }
   }

   public boolean isVanillaModeActive() {
      if (this.isEnabled()) {
         if (this.auraMode.is("Vannila")) {
            return true;
         }
      }

      return false;
   }

   private void switchActionSlot(int slot) {
      if (slot >= 0 && slot <= 8 && Minecraft.getInstance().player != null) {
         this.rememberActionSlot();
         if (Minecraft.getInstance().player.getInventory().getSelectedSlot() != slot) {
            Minecraft.getInstance().player.getInventory().setSelectedSlot(slot);
            if (this.hiding.get()) {
               SpoofElytraAsChestplateHelper.refresh(8);
            }
         }
      }
   }

   private MiningPlanOptions existingSupportOptions() {
      this.digOptions.enabled = false;
      this.digOptions.currentBlock = null;
      this.digOptions.progress = 0.0F;
      this.digOptions.blacklist = null;
      BlockPos blockPos;
      if (this.stickyBase != null) {
         blockPos = this.stickyBase;
      } else {
         blockPos = this.recentSupport();
      }

      this.digOptions.stickyBase = blockPos;
      this.applyReplacementPreference();
      MiningPlanOptions digOptions2 = this.digOptions;
      boolean get = this.rage.get();
      digOptions2.preferMaximumDamage = get;
      this.digOptions.fastExposure = true;
      this.digOptions.maxBlocks = 0;
      this.digOptions.maxAttempts = 0;
      this.digOptions.footY = Integer.MIN_VALUE;
      this.digOptions.standBlock = null;
      return this.digOptions;
   }

   private BlockPos findLowestVanillaStackCell(BlockPos blockPos, float value) {
      if (blockPos != null && Minecraft.getInstance().level != null && Minecraft.getInstance().player != null) {
         Vec3 eyePosition = Minecraft.getInstance().player.getEyePosition();

         for (int value2 = 1; value2 <= 4; value2++) {
            BlockPos blockPos2 = blockPos.below(value2);
            BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos2);
            if (!blockState.canBeReplaced()) {
               if (!blockState.isFaceSturdy(Minecraft.getInstance().level, blockPos2, Direction.UP)) {
                  return null;
               }

               BlockPos blockPos3 = blockPos2.above();
               if (!Minecraft.getInstance().level.getBlockState(blockPos3).canBeReplaced()) {
                  return null;
               }

               Vec3 position = new Vec3(blockPos2.getX() + 0.5, blockPos2.getY() + 1.0, blockPos2.getZ() + 0.5);
               return eyePosition.distanceToSqr(position) <= value * value ? blockPos3.immutable() : null;
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private BlockPos recentSupport() {
      if (this.lastRequestedSupport != null) {
         if (Minecraft.getInstance().player.tickCount - this.requestedSupportTick <= 20) {
            return this.lastRequestedSupport;
         }
      }

      return null;
   }

   private float supportDamage(BlockPos blockPos) {
      if (blockPos != null && Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
         var players = this.combatTargets();
         Player player = this.nearestTarget(players);
         if (player == null) {
            return 0.0F;
         }

         Vec3 position = CrystalSimulation.blast(blockPos);
         AABB boundingBox = player.getBoundingBox();
         return CrystalSimulation.damage(position, player, boundingBox);
      } else {
         return 0.0F;
      }
   }

   private boolean isFullyInsideBlocks(Player player) {
      if (player != null && Minecraft.getInstance().level != null) {
         BlockPos blockPos = this.legsBlock(player);
         double x = player.getX();
         double y = player.getY() + 0.95;
         BlockPos z = BlockPos.containing(x, y, player.getZ());
         Minecraft instance = Minecraft.getInstance();
         if (this.isOccupyingSolid(blockPos, instance.level.getBlockState(blockPos))) {
            instance = Minecraft.getInstance();
            if (this.isOccupyingSolid(z, instance.level.getBlockState(z))) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public record DirectionalFootChoice(BlockPos pos, boolean digSupport) {
      public boolean digSupport() {
         return this.digSupport;
      }

      public BlockPos pos() {
         return this.pos;
      }
   }

   enum FootAction {
      CRYSTAL,
      PLACE,
      DIG,
      DIG_SUPPORT;
   }

   public record FootSpot(BlockPos stand, BlockPos pos, float damage, CrystalAuraModule.FootAction action) {
      public BlockPos pos() {
         return this.pos;
      }

      public CrystalAuraModule.FootAction action() {
         return this.action;
      }

      public float damage() {
         return this.damage;
      }

      public BlockPos stand() {
         return this.stand;
      }
   }

   public record PlacementPulse(BlockPos pos, boolean crystal, long at) {
      public boolean crystal() {
         return this.crystal;
      }

      public long at() {
         return this.at;
      }

      public BlockPos pos() {
         return this.pos;
      }
   }
}
