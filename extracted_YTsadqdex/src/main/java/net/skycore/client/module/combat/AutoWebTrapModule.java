package net.skycore.client.module.combat;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.FakePlayerRegistry;
import net.skycore.client.combat.InputPredictor;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationClaim;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.BlockStatePredictionHandlerPendingBlockStatesAccessor;
import net.skycore.client.mixin.ClientLevelGetDestroyingBlocksAccessor;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.util.CrystalPlacementAimHelper;
import net.skycore.client.util.MovementHelper;
import net.skycore.client.util.QueuePendingHelper;
import net.skycore.client.util.SupportChainHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.WaitingHelper;
import org.joml.Vector2f;

public final class AutoWebTrapModule extends ClientModule implements MinecraftAccess {
   private int crystalDeferTick;
   private LivingEntity target;
   private int swapReadyTick;
   private static final int MAX_CANDIDATES = 24;
   private int swapSource;
   private static final int ROTATION_PRIORITY = 50;
   private ClientLevel lastWorld;
   private static final Direction[] DIRECTIONS = new Direction[]{
      Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP
   };
   private int lastPlanningTick;
   public static final AutoWebTrapModule INSTANCE = new AutoWebTrapModule();
   private long nextPlanId;
   private static final int MAX_CHAIN = 3;
   private AutoWebTrapModule.TrapPlan plan;
   private int preparedTick;
   private final Map<BlockPos, Integer> blockedUntil;
   private final Map<BlockPos, Integer> failures;
   private final Map<BlockPos, Integer> pending;
   private AutoWebTrapModule.Placement preparedPlacement;
   private final WaitingHelper handoff = new WaitingHelper();
   private boolean rotationReady;
   private static final double PLACE_RANGE = 4.5;
   private int lastAttemptTick;
   private double confirmationTicks;
   private int aimTick;
   private static final double TARGET_RANGE = 8.0;
   private final BooleanSetting matrixBypass;

   private void clearSwap() {
      this.swapSource = -1;
      this.swapReadyTick = -1;
   }

   private void aimPreparedPlacement() {
      if (this.preparedPlacement != null && this.isValidTarget(this.target)) {
         if (this.aimTick != mc.player.tickCount) {
            byte flag;
            targetValidation: {
               this.aimTick = mc.player.tickCount;
               InputPredictor inputPredictor = InputPredictor.simulateLocalPlayer(1);
               Vec3 pos = inputPredictor.pos;
               double eyeHeight = mc.player.getEyeHeight();
               Vec3 position = pos.add(0.0, eyeHeight, 0.0);
               Rotation rotation = Rotation.lookingAt(this.preparedPlacement.hit(), position);
               Vector2f angles = new Vector2f(rotation.getYaw(), rotation.getPitch());
               if (RotationClaim.claim(this, angles)) {
                  if (RotationClaim.hasSentRotation(angles)) {
                     flag = 1;
                     break targetValidation;
                  }
               }

               flag = 0;
            }

            this.rotationReady = (flag != 0);
         }
      }
   }

   private AutoWebTrapModule.Placement findPlacement(AutoWebTrapModule.TrapPlan trapPlan) {
      for (BlockPos blockPos : trapPlan.positions()) {
         AutoWebTrapModule.PlanState planState = trapPlan.state(blockPos);
         if (planState != AutoWebTrapModule.PlanState.CONFIRMED
            && planState != AutoWebTrapModule.PlanState.WAITING_SERVER
            && planState != AutoWebTrapModule.PlanState.SKIPPED
            && planState != AutoWebTrapModule.PlanState.TIMED_OUT) {
            if (this.confirmedWeb(blockPos)) {
               trapPlan.setState(blockPos, AutoWebTrapModule.PlanState.CONFIRMED, mc.player.tickCount);
            } else {
               AutoWebTrapModule.Placement placement = this.getPlacement(blockPos);
               if (placement != null) {
                  return placement;
               }
            }
         }
      }

      return null;
   }

   public boolean isFullyTrapped(LivingEntity entity) {
      if (entity != null) {
         if (mc.level != null) {
            AABB boundingBox = entity.getBoundingBox();
            double ysize = boundingBox.minY + Math.min(0.2, boundingBox.getYsize() * 0.2);
            double ysize2 = boundingBox.minY + boundingBox.getYsize() * 0.8;
            return this.hasWebAtHeight(boundingBox, ysize) && this.hasWebAtHeight(boundingBox, ysize2);
         }
      }

      return false;
   }

   @Override
   protected void onDisable() {
      this.restoreSwap(true);
      this.clearSwap();
      this.crystalDeferTick = -100;
      this.lastPlanningTick = -100;
      this.lastAttemptTick = -1;
      this.pending.clear();
      this.blockedUntil.clear();
      this.failures.clear();
      this.lastWorld = null;
      this.resetTrapCycle();
      super.onDisable();
   }

   public void runCombatTick() {
      if (!this.canRun()) {
         this.restoreSwap(true);
         this.clearPrepared();
         RotationClaim.release(this);
      } else {
         int value = mc.player.tickCount;
         if (this.lastWorld != mc.level || value < this.lastPlanningTick) {
            this.restoreSwap(true);
            this.resetTrapCycle();
            this.pending.clear();
            this.blockedUntil.clear();
            this.failures.clear();
            this.confirmationTicks = 2.0;
            this.lastPlanningTick = -100;
            this.lastWorld = mc.level;
         }

         if (value == this.lastPlanningTick) {
            this.aimPreparedPlacement();
         } else {
            this.lastPlanningTick = value;
            this.updatePending(value);
            this.updatePlanFromWorld(value);
            LivingEntity target2 = this.findTarget();
            if (target2 == null) {
               this.restoreSwap(true);
               this.resetTrapCycle();
            } else {
               if (this.target != target2) {
                  this.plan = null;
                  this.handoff.reset();
                  this.clearPrepared();
               }

               this.target = target2;
               boolean fullyTrapped = this.isFullyTrapped(target2);
               this.handoff.update(target2.getUUID(), value, fullyTrapped, this.hasFeetWeb(target2));
               if (!fullyTrapped && !this.handoff.ready(target2.getUUID())) {
                  this.refreshPlan(target2, value);
                  if (this.plan == null) {
                     this.clearPrepared();
                     this.restoreSwap(true);
                     RotationClaim.release(this);
                  } else {
                     if (this.preparedPlacement != null && value - this.preparedTick > 20) {
                        this.reject(this.preparedPlacement.placePos(), value);
                        this.plan.setState(this.preparedPlacement.placePos(), AutoWebTrapModule.PlanState.TIMED_OUT, value);
                        this.clearPrepared();
                     }

                     AutoWebTrapModule.Placement placement = this.findPlacement(this.plan);
                     if (placement == null) {
                        this.clearPrepared();
                        this.restoreSwap(true);
                        RotationClaim.release(this);
                     } else {
                        if (this.preparedPlacement == null || !this.preparedPlacement.sameFace(placement)) {
                           this.preparedPlacement = placement;
                           this.preparedTick = value;
                           this.plan.setState(placement.placePos(), AutoWebTrapModule.PlanState.PREPARED, value);
                        }

                        this.aimPreparedPlacement();
                     }
                  }
               } else {
                  this.clearPrepared();
                  this.restoreSwap(true);
                  RotationClaim.release(this);
               }
            }
         }
      }
   }

   private BlockHitResult sentRayHit(AutoWebTrapModule.Placement placement) {
      float angle = QueuePendingHelper.lastSentYaw();
      float angle2 = QueuePendingHelper.lastSentPitch();
      if (Float.isFinite(angle) && Float.isFinite(angle2)) {
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 position = placement.hit().subtract(eyePosition);
         double angle3 = Math.min(4.5, mc.player.blockInteractionRange());
         if (position.lengthSqr() > angle3 * angle3) {
            return null;
         }

         Vec3 position2 = eyePosition.add(Vec3.directionFromRotation(angle2, angle).scale(angle3));
         return mc.level.clip(new ClipContext(eyePosition, position2, Block.OUTLINE, Fluid.NONE, mc.player)) instanceof BlockHitResult blockHitResult
               && blockHitResult.getType() == Type.BLOCK
               && blockHitResult.getBlockPos().equals(placement.supportPos())
               && blockHitResult.getBlockPos().relative(blockHitResult.getDirection()).equals(placement.placePos())
            ? blockHitResult
            : null;
      } else {
         return null;
      }
   }

   public void applyFinalMovementCorrection(PlayerInputEvent playerInputEvent) {
      if (this.isEnabled() && mc.player != null) {
         if (RotationManager.INSTANCE.isAimingAtPriority(50)) {
            MovementHelper.fixMovement(playerInputEvent, mc.player.getYRot());
            return;
         }
      }
   }

   public boolean blocksReallyWorldAura() {
      if (this.canRun() && this.target instanceof Player player && this.isValidTarget(player)) {
         if (!this.handoff.ready(player.getUUID())) {
            if (this.preparedPlacement != null) {
               return true;
            }

            if (!this.pending.isEmpty()) {
               return true;
            }

            WaitingHelper handoff2 = this.handoff;
            UUID uUID = player.getUUID();
            if (handoff2.waiting(uUID)) {
               return true;
            }
         }
      }

      return false;
   }

   private void reject(BlockPos blockPos, int value) {
      var failures2 = this.failures;
      BlockPos blockPos2 = blockPos.immutable();
      Integer value2 = 1;
      Integer value3 = failures2.merge(blockPos2, value2, Integer::sum);
      int value4 = value3;
      var blockedUntil2 = this.blockedUntil;
      BlockPos blockPos3 = blockPos.immutable();
      int computedValue = value + (value4 >= 3 ? 40 : 6);
      blockedUntil2.put(blockPos3, computedValue);
   }

   private void updatePlanFromWorld(int index) {
      if (this.plan != null) {
         for (BlockPos blockPos : this.plan.positions()) {
            AutoWebTrapModule.PlanState planState = this.plan.state(blockPos);
            if (this.confirmedWeb(blockPos)) {
               this.plan.setState(blockPos, AutoWebTrapModule.PlanState.CONFIRMED, index);
            } else if (planState == AutoWebTrapModule.PlanState.WAITING_SERVER && !this.pending.containsKey(blockPos)) {
               this.plan.setState(blockPos, AutoWebTrapModule.PlanState.REJECTED, index);
            }
         }
      }
   }

   private LivingEntity findTarget() {
      if (this.target != null && this.isValidTarget(this.target)) {
         return this.target;
      }

      Player player = null;
      double index = 64.0;

      for (Player player2 : mc.level.players()) {
         if (this.isValidTarget(player2)) {
            double eyePosition = distanceToBoxSqr(mc.player.getEyePosition(), player2.getBoundingBox());
            if (eyePosition < index) {
               player = player2;
               index = eyePosition;
            }
         }
      }

      return player;
   }

   private List<BlockPos> predictedColumns(LivingEntity entity) {
      if (!(entity instanceof Player player)) {
         return List.of();
      } else {
         double clampedValue = Math.clamp(this.confirmationTicks, 0.0, 8.0);
         double computedValue = clampedValue + 0.5 + (this.matrixBypass.get() ? 1.0 : 0.0);
         double computedValue2 = Math.max(1.0, Math.ceil(clampedValue)) + 1.0 + (this.matrixBypass.get() ? 1.0 : 0.0);
         double computedValue3 = computedValue + 2.0 * computedValue2 + 3.0;
         var aABBs = InputPredictor.predictOtherPlayerBoxes(player, (int)Math.ceil(computedValue3 + 1.0));
         ArrayList<Entry<BlockPos, Double>> blockPoss = new ArrayList<>();

         for (BlockPos blockPos : SupportChainHelper.candidates(aABBs, computedValue, computedValue3)) {
            if (this.canReplace(blockPos)) {
               SupportChainHelper.Interval interval = SupportChainHelper.arrival(aABBs, blockPos, computedValue, computedValue3);
               if (interval != null) {
                  blockPoss.add(Map.entry(blockPos, interval.enter()));
               }
            }
         }

         blockPoss.sort(Comparator.<Entry<BlockPos, Double>>comparingDouble(Entry::getValue));
         LinkedHashMap lookup = new LinkedHashMap();
         List<BlockPos> blockPoss2 = List.of();
         double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;
         int value = 0;

         for (Entry entry : blockPoss) {
            if (value++ >= 24) {
               break;
            }

            var key = SupportChainHelper.supportChain((BlockPos)entry.getKey(), 3, this::canReplace, blockPos2 -> {
               if (!lookup.containsKey(blockPos2)) {
                  lookup.put(blockPos2, this.getPlacement(blockPos2));
               }

               return lookup.get(blockPos2) != null;
            });
            if (!key.isEmpty()) {
               double computedValue4 = computedValue + (key.size() - 1) * computedValue2;
               SupportChainHelper.Interval key2 = SupportChainHelper.arrival(aABBs, (BlockPos)entry.getKey(), computedValue4, computedValue4 + 3.0);
               if (key2 != null && !(key2.exit() - key2.enter() < 0.25)) {
                  double eyePosition = key2.enter()
                     - computedValue
                     + (key.size() - 1) * 0.75
                     - Math.min(2.0, key2.exit() - key2.enter()) * 0.5
                     + mc.player.getEyePosition().distanceToSqr(Vec3.atCenterOf((Vec3i)key.getFirst())) * 0.02;
                  if (eyePosition < pOSITIVE_INFINITY) {
                     pOSITIVE_INFINITY = eyePosition;
                     blockPoss2 = key;
                  }
               }
            }
         }

         return blockPoss2;
      }
   }

   public boolean shouldDeferCrystalAura(List<Player> players, boolean condition, boolean condition2, boolean condition3, int crystalDeferTick2, boolean condition4) {
      if (!this.canRun()) {
         return false;
      }

      if (condition4) {
         this.clearPrepared();
      }

      if (this.target == null || this.handoff.ready(this.target.getUUID())) {
         return this.crystalDeferTick == crystalDeferTick2;
      }

      if (!this.isValidTarget(this.target)) {
         return false;
      }

      if (this.preparedPlacement == null && this.pending.isEmpty() && !this.handoff.waiting(this.target.getUUID()) && this.lastAttemptTick != crystalDeferTick2) {
         return false;
      }

      this.crystalDeferTick = crystalDeferTick2;
      return true;
   }

   private AutoWebTrapModule() {
      super("AutoWebTrap", "Traps the target in cobwebs and hands it to CrystalAura", ModuleCategory.COMBAT);
      this.pending = new LinkedHashMap<>();
      this.blockedUntil = new LinkedHashMap<>();
      this.failures = new LinkedHashMap<>();
      this.confirmationTicks = 2.0;
      this.matrixBypass = this.add(new BooleanSetting("Matrix Bypass", false));
      this.preparedTick = -1;
      this.aimTick = -1;
      this.lastAttemptTick = -1;
      this.crystalDeferTick = -100;
      this.lastPlanningTick = -100;
      this.swapSource = -1;
      this.swapReadyTick = -1;
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> {
         this.runCombatTick();
      });
   }

   private void restoreSwap(boolean condition) {
      if (condition && mc.player != null) {
         if (mc.gameMode != null && this.swapSource != -1) {
            if (this.swapSource >= 0 && this.swapSource <= 8) {
               mc.player.getInventory().setSelectedSlot(this.swapSource);
               if (mc.gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor) {
                  multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$ensureHasSentCarriedItem();
               }
            }

            this.clearSwap();
            return;
         }
      }
   }

   private static double distanceToBoxSqr(Vec3 position, AABB bounds) {
      double clampedValue = Mth.clamp(position.x, bounds.minX, bounds.maxX);
      double clampedValue2 = Mth.clamp(position.y, bounds.minY, bounds.maxY);
      double clampedValue3 = Mth.clamp(position.z, bounds.minZ, bounds.maxZ);
      return position.distanceToSqr(clampedValue, clampedValue2, clampedValue3);
   }

   private boolean isValidTarget(LivingEntity entity) {
      if (!(
         entity instanceof Player player
            && player != mc.player
            && player.isAlive()
            && !player.isSpectator()
            && !player.isRemoved()
            && !player.isCreative()
            && player.tickCount >= 3
            && player.level() == mc.level
            && !FakePlayerRegistry.isFake(player)
            && !AntiBotModule.isBot(player)
      )) {
         return false;
      } else {
         return distanceToBoxSqr(mc.player.getEyePosition(), player.getBoundingBox()) > 64.0
            ? false
            : !FriendManager.INSTANCE.isFriend(player.getGameProfile().name());
      }
   }

   private Vec3 snapToFace(Vec3 position, BlockPos blockPos, Direction direction, AABB bounds) {
      double x = position.x;
      double y = position.y;
      double z = position.z;
      Axis axis = direction.getAxis();
      if (axis == Axis.X) {
         double x2 = blockPos.getX();
         double value;
         if (direction == Direction.EAST) {
            value = bounds.maxX;
         } else {
            value = bounds.minX;
         }

         x = x2 + value;
      } else {
         Axis axis2 = direction.getAxis();
         if (axis2 == Axis.Y) {
            double y2 = blockPos.getY();
            double value2;
            if (direction == Direction.UP) {
               value2 = bounds.maxY;
            } else {
               value2 = bounds.minY;
            }

            y = y2 + value2;
         } else {
            z = blockPos.getZ() + (direction == Direction.SOUTH ? bounds.maxZ : bounds.minZ);
         }
      }

      return new Vec3(x, y, z);
   }

   private int findWebSlot() {
      if (mc.player == null) {
         return -1;
      }

      for (int value = 0; value < 9; value++) {
         if (mc.player.getInventory().getItem(value).is(Items.COBWEB)) {
            return value;
         }
      }

      return -1;
   }

   private void clearPrepared() {
      this.preparedPlacement = null;
      this.preparedTick = -1;
      this.aimTick = -1;
      this.rotationReady = false;
   }

   private boolean confirmedWeb(BlockPos blockPos) {
      if (mc.level != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (blockState.is(Blocks.COBWEB)) {
            if (!this.predictedBlock(blockPos)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean armWebSlot(int slot, int swapReadyTick2) {
      int selectedSlot = mc.player.getInventory().getSelectedSlot();
      if (this.swapSource == -1) {
         if (selectedSlot != slot && !mc.player.getMainHandItem().is(Items.COBWEB)) {
            this.swapSource = selectedSlot;
            mc.player.getInventory().setSelectedSlot(slot);
            if (mc.gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor) {
               multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$ensureHasSentCarriedItem();
            }

            this.swapReadyTick = swapReadyTick2;
            return !this.matrixBypass.get();
         } else {
            if (mc.gameMode instanceof MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor2) {
               multiPlayerGameModeEnsureHasSentCarriedItemAccessor2.skycore$ensureHasSentCarriedItem();
            }

            return true;
         }
      } else {
         return swapReadyTick2 >= this.swapReadyTick && mc.player.getMainHandItem().is(Items.COBWEB);
      }
   }

   public boolean hasFeetWeb(LivingEntity entity) {
      if (entity != null) {
         if (mc.level != null) {
            AABB boundingBox = entity.getBoundingBox().deflate(1.0E-5);
            BlockPos blockPos = BlockPos.containing(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
            BlockPos blockPos2 = BlockPos.containing(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);
            var blockPoss = BlockPos.betweenClosed(blockPos, blockPos2);
            Iterator iterator = blockPoss.iterator();

            BlockPos blockPos3;
            do {
               if (!iterator.hasNext()) {
                  return false;
               }

               blockPos3 = (BlockPos)iterator.next();
            } while (!this.confirmedWeb(blockPos3));

            return true;
         }
      }

      return false;
   }

   public void afterMovementPacket() {
      if (this.canRun() && this.preparedPlacement != null && this.plan != null) {
         int slot = mc.player.tickCount;
         if (this.isValidTarget(this.target) && this.rotationReady && RotationClaim.claimedRecently(this, 1) && RotationManager.INSTANCE.isAimingAtPriority(50)
            )
          {
            AutoWebTrapModule.Placement preparedPlacement2 = this.preparedPlacement;
            BlockHitResult blockHitResult = this.sentRayHit(preparedPlacement2);
            if (slot != this.lastAttemptTick && blockHitResult != null) {
               int webSlot = this.findWebSlot();
               if (webSlot != -1 && this.armWebSlot(webSlot, slot)) {
                  this.lastAttemptTick = slot;
                  if (this.canReplace(preparedPlacement2.placePos()) && this.canClick(preparedPlacement2.supportPos())) {
                     BlockPlaceContext blockPlaceContext = new BlockPlaceContext(mc.player, InteractionHand.MAIN_HAND, mc.player.getMainHandItem(), blockHitResult);
                     if (blockPlaceContext.getClickedPos().equals(preparedPlacement2.placePos()) && blockPlaceContext.canPlace()) {
                        boolean condition = CrystalPlacementAimHelper.placementOk(mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult));
                        if (condition && mc.level.getBlockState(preparedPlacement2.placePos()).is(Blocks.COBWEB)) {
                           this.pending.putIfAbsent(preparedPlacement2.placePos(), slot);
                           mc.player.swing(InteractionHand.MAIN_HAND);
                        } else {
                           condition = false;
                           this.reject(preparedPlacement2.placePos(), slot);
                        }

                        this.plan
                           .setState(preparedPlacement2.placePos(), condition ? AutoWebTrapModule.PlanState.WAITING_SERVER : AutoWebTrapModule.PlanState.REJECTED, slot);
                        if (condition) {
                           RotationClaim.markAction();
                        }

                        this.restoreSwap(true);
                        this.clearPrepared();
                     } else {
                        this.reject(preparedPlacement2.placePos(), slot);
                        this.plan.setState(preparedPlacement2.placePos(), AutoWebTrapModule.PlanState.REJECTED, slot);
                        this.restoreSwap(true);
                        this.clearPrepared();
                     }
                  } else {
                     this.reject(preparedPlacement2.placePos(), slot);
                     this.plan.setState(preparedPlacement2.placePos(), AutoWebTrapModule.PlanState.REJECTED, slot);
                     this.restoreSwap(true);
                     this.clearPrepared();
                  }
               }
            }
         }
      }
   }

   public Player crystalAuraTarget() {
      return this.isEnabled() && this.target instanceof Player player && this.isValidTarget(player) && this.handoff.ready(player.getUUID()) && this.hasFeetWeb(player)
         ? player
         : null;
   }

   private void refreshPlan(LivingEntity entity, int value) {
      if (this.plan != null) {
         if (this.plan.matches(entity, value) && this.plan.hasPending()) {
            if (this.findPlacement(this.plan) != null) {
               return;
            }
         }
      }

      var blockPoss = this.predictedColumns(entity);
      if (blockPoss.isEmpty()) {
         this.plan = null;
      } else {
         this.plan = new AutoWebTrapModule.TrapPlan(++this.nextPlanId, entity, blockPoss, value, value + 20);
         this.clearPrepared();
      }
   }

   public Direction nextTrapFace(LivingEntity entity) {
      if (entity != null) {
         if (mc.level != null) {
            if (mc.player != null) {
               AutoWebTrapModule.TrapPlan trapPlan;
               placementValidation: {
                  if (this.plan != null) {
                     UUID uUID = this.plan.targetId;
                     UUID uUID2 = entity.getUUID();
                     if (uUID.equals(uUID2)) {
                        trapPlan = this.plan;
                        break placementValidation;
                     }
                  }

                  var blockPoss = this.predictedColumns(entity);
                  trapPlan = new AutoWebTrapModule.TrapPlan(-1L, entity, blockPoss, mc.player.tickCount, mc.player.tickCount + 8);
               }

               AutoWebTrapModule.TrapPlan trapPlan2 = trapPlan;
               AutoWebTrapModule.Placement placement = this.findPlacement(trapPlan2);
               Direction direction;
               if (placement == null) {
                  direction = null;
               } else {
                  BlockHitResult blockHitResult = placement.hitResult();
                  direction = blockHitResult.getDirection();
               }

               return direction;
            }
         }
      }

      return null;
   }

   private boolean canClick(BlockPos blockPos) {
      BlockState blockState = mc.level.getBlockState(blockPos);
      if (!blockState.isAir()) {
         if (!blockState.getShape(mc.level, blockPos).isEmpty()) {
            return true;
         }
      }

      return false;
   }

   private AutoWebTrapModule.Placement getPlacement(BlockPos blockPos) {
      if (!this.canReplace(blockPos)) {
         return null;
      }

      AutoWebTrapModule.Placement placement = null;
      double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;
      double computedValue = Math.min(4.5, mc.player.blockInteractionRange());
      Vec3 eyePosition = mc.player.getEyePosition();

      for (Direction direction : DIRECTIONS) {
         BlockPos opposite = blockPos.relative(direction.getOpposite());
         if (this.canClick(opposite)) {
            AABB blockState = mc.level.getBlockState(opposite).getShape(mc.level, opposite).bounds();
            Vec3 x = this.snapToFace(
               new Vec3(
                  opposite.getX() + (blockState.minX + blockState.maxX) * 0.5,
                  opposite.getY() + (blockState.minY + blockState.maxY) * 0.5,
                  opposite.getZ() + (blockState.minZ + blockState.maxZ) * 0.5
               ),
               opposite,
               direction,
               blockState
            );
            double distanceSquared = eyePosition.distanceToSqr(x);
            Vec3 position = x.subtract(eyePosition).normalize();
            if (mc.level.clip(new ClipContext(eyePosition, x.add(position.scale(0.001)), Block.OUTLINE, Fluid.NONE, mc.player)) instanceof BlockHitResult blockHitResult
               && blockHitResult.getType() == Type.BLOCK
               && blockHitResult.getBlockPos().equals(opposite)
               && blockHitResult.getDirection() == direction
               && distanceSquared <= computedValue * computedValue
               && distanceSquared < pOSITIVE_INFINITY) {
               pOSITIVE_INFINITY = distanceSquared;
               placement = new AutoWebTrapModule.Placement(blockPos.immutable(), opposite.immutable(), blockHitResult, blockHitResult.getLocation());
            }
         }
      }

      return placement;
   }

   private boolean canReplace(BlockPos blockPos) {
      if (!this.pending.containsKey(blockPos) && this.blockedUntil.getOrDefault(blockPos, -1) <= mc.player.tickCount) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (blockState.canBeReplaced() && !blockState.is(Blocks.COBWEB)) {
            if (mc.player.getBoundingBox().inflate(0.05).intersects(new AABB(blockPos))) {
               return false;
            }

            double computedValue = Math.min(4.5, mc.player.blockInteractionRange());
            return distanceToBoxSqr(mc.player.getEyePosition(), new AABB(blockPos)) <= computedValue * computedValue;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean predictedBlock(BlockPos blockPos) {
      if (mc.level instanceof ClientLevelGetDestroyingBlocksAccessor clientLevelGetDestroyingBlocksAccessor) {
         if (clientLevelGetDestroyingBlocksAccessor.skycore$getBlockStatePredictionHandler() instanceof BlockStatePredictionHandlerPendingBlockStatesAccessor blockStatePredictionHandlerPendingBlockStatesAccessor) {
            Long2ObjectOpenHashMap lookup = blockStatePredictionHandlerPendingBlockStatesAccessor.skycore$pendingBlockStates();
            if (lookup.containsKey(blockPos.asLong())) {
               return true;
            }
         }
      }

      return false;
   }

   public BlockPos nextTrapPos(LivingEntity entity) {
      if (entity != null) {
         if (mc.level != null) {
            if (mc.player != null) {
               AutoWebTrapModule.TrapPlan trapPlan;
               placementValidation: {
                  if (this.plan != null) {
                     UUID uUID = this.plan.targetId;
                     UUID uUID2 = entity.getUUID();
                     if (uUID.equals(uUID2)) {
                        trapPlan = this.plan;
                        break placementValidation;
                     }
                  }

                  var blockPoss = this.predictedColumns(entity);
                  int count = -1;
                  count = mc.player.tickCount + 8;
                  trapPlan = new AutoWebTrapModule.TrapPlan(-1L, entity, blockPoss, mc.player.tickCount, count);
               }

               AutoWebTrapModule.TrapPlan trapPlan2 = trapPlan;
               AutoWebTrapModule.Placement placement = this.findPlacement(trapPlan2);
               return placement == null ? null : placement.placePos();
            }
         }
      }

      return null;
   }

   private void updatePending(int value) {
      this.blockedUntil.entrySet().removeIf(entry -> {
         Integer value2 = entry.getValue();
         return value2 <= value;
      });
      Iterator iterator = this.pending.entrySet().iterator();

      while (iterator.hasNext()) {
         Entry element = (Entry)iterator.next();
         BlockPos key = (BlockPos)element.getKey();
         int value3 = value - (Integer)element.getValue();
         if (this.confirmedWeb(key)) {
            double clampedValue = Math.clamp(value3, 1, 8);
            this.confirmationTicks = this.confirmationTicks + (clampedValue - this.confirmationTicks) * (clampedValue > this.confirmationTicks ? 0.5 : 0.15);
            this.failures.remove(key);
            iterator.remove();
         } else if (value3 > 20 || value3 >= 2 && !this.predictedBlock(key)) {
            this.reject(key, value);
            iterator.remove();
         }
      }
   }

   private boolean canRun() {
      if (this.isEnabled()) {
         if (mc.player != null) {
            if (mc.level != null) {
               if (mc.gameMode != null) {
                  if (!mc.player.isSpectator() && !mc.player.isUsingItem()) {
                     return this.findWebSlot() != -1;
                  }
               }
            }
         }
      }

      return false;
   }

   private void resetTrapCycle() {
      this.handoff.reset();
      this.target = null;
      this.plan = null;
      this.clearPrepared();
      RotationClaim.release(this);
   }

   private boolean hasWebAtHeight(AABB bounds, double value) {
      BlockPos blockPos = BlockPos.containing(bounds.minX + 1.0E-5, value, bounds.minZ + 1.0E-5);
      double computedValue = bounds.maxX - 1.0E-5;
      double computedValue2 = bounds.maxZ - 1.0E-5;
      BlockPos blockPos2 = BlockPos.containing(computedValue, value, computedValue2);
      var blockPoss = BlockPos.betweenClosed(blockPos, blockPos2);

      for (BlockPos blockPos3 : blockPoss) {
         if (this.confirmedWeb(blockPos3)) {
            return true;
         }
      }

      return false;
   }

   public boolean canTakeReallyWorldPriority() {
      return this.canRun();
   }

   public record Placement(BlockPos placePos, BlockPos supportPos, BlockHitResult hitResult, Vec3 hit) {
      public Vec3 hit() {
         return this.hit;
      }

      public BlockPos placePos() {
         return this.placePos;
      }

      private boolean sameFace(AutoWebTrapModule.Placement placement) {
         if (placement != null) {
            if (this.placePos.equals(placement.placePos)) {
               if (this.supportPos.equals(placement.supportPos)) {
                  Direction direction = this.hitResult.getDirection();
                  if (direction == placement.hitResult.getDirection()) {
                     return true;
                  }
               }
            }
         }

         return false;
      }

      public BlockPos supportPos() {
         return this.supportPos;
      }

      public BlockHitResult hitResult() {
         return this.hitResult;
      }
   }

   enum PlanState {
      PENDING,
      PREPARED,
      WAITING_SERVER,
      CONFIRMED,
      REJECTED,
      SKIPPED,
      TIMED_OUT;
   }

   static final class TrapPlan {
      private final Map<Long, AutoWebTrapModule.PlanState> states = new LinkedHashMap<>();
      private final Map<Long, Integer> stateTicks = new LinkedHashMap<>();
      private final List<BlockPos> positions;
      private final int expiresTick;
      private final List<BlockPos> columns;
      private final Vec3 targetOrigin;
      private final long id;
      private final UUID targetId;

      private List<BlockPos> positions() {
         return this.positions;
      }

      private int stateTick(BlockPos blockPos) {
         var stateTicks2 = this.stateTicks;
         long timestamp = blockPos.asLong();
         Long element = timestamp;
         return stateTicks2.getOrDefault(element, 0);
      }

      private boolean matches(LivingEntity entity, int value) {
         if (this.targetId.equals(entity.getUUID()) && value <= this.expiresTick) {
            Vec3 targetOrigin2 = this.targetOrigin;
            Vec3 position = entity.position();
            if (targetOrigin2.distanceToSqr(position) <= 0.25) {
               return true;
            }
         }

         return false;
      }

      private boolean waitingForServer() {
         return this.states.containsValue(AutoWebTrapModule.PlanState.WAITING_SERVER);
      }

      private AutoWebTrapModule.PlanState state(BlockPos blockPos) {
         var states2 = this.states;
         long timestamp = blockPos.asLong();
         Long element = timestamp;
         return states2.getOrDefault(element, AutoWebTrapModule.PlanState.PENDING);
      }

      private boolean hasPending() {
         Iterator iterator = this.states.values().iterator();

         AutoWebTrapModule.PlanState planState;
         do {
            if (!iterator.hasNext()) {
               return false;
            }

            planState = (AutoWebTrapModule.PlanState)iterator.next();
            if (planState == AutoWebTrapModule.PlanState.PENDING) {
               break;
            }

         } while (
            planState != AutoWebTrapModule.PlanState.PREPARED && planState != AutoWebTrapModule.PlanState.WAITING_SERVER && planState != AutoWebTrapModule.PlanState.REJECTED
         );

         return true;
      }

      private TrapPlan(long id2, LivingEntity targetId2, List<BlockPos> blockPoss, int value, int expiresTick2) {
         this.id = id2;
         this.targetId = targetId2.getUUID();
         this.targetOrigin = targetId2.position();
         this.columns = List.copyOf(blockPoss);
         this.expiresTick = expiresTick2;
         LinkedHashSet entries = new LinkedHashSet();

         for (BlockPos blockPos : blockPoss) {
            entries.add(blockPos.immutable());
         }

         this.positions = List.copyOf(entries);

         for (BlockPos blockPos2 : this.positions) {
            this.states.put(blockPos2.asLong(), AutoWebTrapModule.PlanState.PENDING);
            this.stateTicks.put(blockPos2.asLong(), value);
         }
      }

      private void setState(BlockPos state, AutoWebTrapModule.PlanState state2, int state3) {
         var states2 = this.states;
         Long element = state.asLong();
         states2.put(element, state2);
         var stateTicks2 = this.stateTicks;
         long timestamp = state.asLong();
         Long element2 = timestamp;
         Integer value = state3;
         stateTicks2.put(element2, value);
      }
   }
}
