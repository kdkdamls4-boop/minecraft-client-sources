package net.skycore.client.module.player;

import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.BlockFaces;
import net.skycore.client.combat.BlockRayTrace;
import net.skycore.client.combat.CriticalHelper;
import net.skycore.client.combat.FakePlayerRegistry;
import net.skycore.client.combat.LookHelper;
import net.skycore.client.combat.Rotation;
import net.skycore.client.combat.RotationController;
import net.skycore.client.combat.RotationManager;
import net.skycore.client.combat.VirtualExplosionDamage;
import net.skycore.client.config.FriendManager;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.ClientLevelGetDestroyingBlocksAccessor2;
import net.skycore.client.mixin.MultiPlayerGameModeEnsureHasSentCarriedItemAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AutoWebTrapModule;
import net.skycore.client.util.AddQuadHelper;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.util.ItemUseCooldown;
import net.skycore.client.util.MinecraftAccess;
import net.skycore.client.util.EasingCurve;
import net.skycore.client.util.EntityAimPoint;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.util.DirectionHelper;
import net.skycore.client.util.RequestTempHelper;
import net.skycore.client.util.RestartHelper;
import net.skycore.client.setting.BooleanSetting;
import net.skycore.client.util.UseItemHelper;
import net.skycore.client.util.WriteHexByteHelper;

public final class CrystalAuraModule extends ClientModule implements MinecraftAccess {
   private BlockPos correctionStandPos;
   private static final long NEW_OBSIDIAN_PLACE_DELAY_MS = 100L;
   private static final int WEB_IGNORE_TICKS = 20;
   private BlockPos pendingObsidianPos;
   private BlockPos crystalPlaceBase;
   private static final double ANTI_EXPLOSION_DAMAGE = 0.0;
   private static final long OBSIDIAN_PLACE_DELAY_MS = 500L;
   private final Map<Long, Integer> failedGoals;
   private final BooleanSetting antiExplosion;
   private final ModeSetting correctionType;
   public static final CrystalAuraModule INSTANCE = new CrystalAuraModule();
   private BlockPos correctionBasePos;
   private static final int FAILED_GOAL_TTL = 40;
   private long lastCrystalPlaceMs;
   private Rotation rotation;
   private static final float[] RGB_BREAK = new float[]{1.0F, 0.24F, 0.24F};
   private static final long CRYSTAL_PLACE_WAIT_MS = 300L;
   private int crystalHotbarSlot;
   private static final double ANTI_EXPLOSION_MIN_SELF_THREAT = 14.0;
   private BlockPos lastTargetFeet;
   private static final int COLOR_CRYSTAL = -1263251969;
   private static final float MOVING_PITCH_SPEED = 35.0F;
   private static final float[] RGB_CRYSTAL = new float[]{0.7F, 0.3F, 1.0F};
   private final ConcurrentLinkedQueue<CrystalAuraModule.VisualData> visualDatas;
   private final SliderSetting minHealth;
   private static final int COLOR_OBSIDIAN = -1271019265;
   private static final float[] RGB_OBSIDIAN = new float[]{0.24F, 0.78F, 1.0F};
   private int savedHotbarSlot;
   private CrystalAuraModule.PlaceData goalData;
   private final SliderSetting placeRange;
   private static final long PLACE_FAIL_TIMEOUT_MS = 100L;
   private final ModeSetting mode = new ModeSetting("Mode", "Default", "Default", "New");
   private BlockPos correctionBreakPos;
   private static final int TARGET_RANGE = 8;
   private static final String SAFETY_RESOURCES = "Ресурсы";
   private static final float PLACE_LOOK_ANGLE = 8.0F;
   private final BooleanSetting autoKrot;
   private long lastCrystalInteractMs;
   private static final double BOX_RANGE = 3.0;
   private static final float IDLE_ROTATION_SPEED = 180.0F;
   private final SliderSetting crystalsPerSecond;
   private static final int ROTATION_PRIORITY = 999;
   private BlockPos breakingBlock;
   private static final int ROTATION_TIMEOUT = 100;
   private static final int COLOR_BREAK = -1258341316;
   private final SliderSetting maxSelfDamage;
   private final BooleanSetting rage;
   private static final long ANTI_EXPLOSION_OBSIDIAN_DELAY_MS = 50L;
   private BlockPos pendingCrystalBase;
   private static final long VISUAL_HOLD_MS = 450L;
   private final ModeSetting rotate = new ModeSetting("Ротация", "Vanilla", "Vanilla", "Без ротации");
   private static final float MOVING_YAW_SPEED = 55.0F;
   private static final int DEFAULT_SCAN_RADIUS = 5;
   private static final double ATTACK_RANGE = 3.0;
   private final MultiSelectSetting safety;
   private static final String SAFETY_FRIENDS = "Друзей";
   private boolean crystalPlaceBlocked;
   private long crystalGoalSinceMs;
   private CrystalAuraModule.PlaceData lastData;
   private static final int NEW_SCAN_RADIUS = 8;
   private static final float NEW_REACH = 6.0F;
   private long lastObsidianPlaceMs;
   private static final float REACH = 5.5F;
   private static final double ANTI_EXPLOSION_RANGE = 6.5;
   private static final double NEW_BOX_RANGE = 5.0;
   private Direction breakingDirection;
   private static final int NEW_MAX_BREAK_CHAIN = 8;

   private CrystalAuraModule.PlaceData resolveExecutableAction(CrystalAuraModule.PlaceData placeData) {
      if (placeData != null && placeData.blockPos() != null && placeData.face() != null) {
         BlockPos blockPos = placeData.blockPos();
         Direction direction = placeData.face();
         float reach = this.getReach();
         this.requestWebIgnore();
         if (this.canExecutePlaceThroughWalls(placeData)) {
            if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
               && this.isCrystalBase(mc.level.getBlockState(blockPos))
               && mc.level.getBlockState(blockPos.above()).isAir()) {
               this.stopBreaking();
               return new CrystalAuraModule.PlaceData(blockPos.immutable(), Direction.UP, placeData.damage(), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null);
            }

            if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
               BlockPos blockPos2 = blockPos.relative(direction);
               BlockState blockState = mc.level.getBlockState(blockPos2);
               BlockState blockState2 = mc.level.getBlockState(blockPos);
               int value = !blockState.isAir() && (!blockState.canBeReplaced() || blockState.is(Blocks.COBWEB)) ? 0 : 1;
               int fluidState = !blockState2.isAir() && !blockState2.canBeReplaced() && !blockState2.is(Blocks.COBWEB) && blockState2.getFluidState().isEmpty() ? 1 : 0;
               if (value != 0 && fluidState != 0) {
                  this.stopBreaking();
                  return placeData;
               }
            }
         }

         if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
            && this.isCrystalBase(mc.level.getBlockState(blockPos))
            && mc.level.getBlockState(blockPos.above()).isAir()
            && mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(blockPos)) <= reach + 0.75) {
            return new CrystalAuraModule.PlaceData(blockPos.immutable(), Direction.UP, placeData.damage(), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null);
         }

         if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
            BlockPos blockPos3 = blockPos.relative(direction);
            BlockState blockState3 = mc.level.getBlockState(blockPos3);
            BlockState blockState4 = mc.level.getBlockState(blockPos);
            int value2 = !blockState3.isAir() && (!blockState3.canBeReplaced() || blockState3.is(Blocks.COBWEB)) ? 0 : 1;
            int fluidState2 = !blockState4.isAir() && !blockState4.canBeReplaced() && !blockState4.is(Blocks.COBWEB) && blockState4.getFluidState().isEmpty() ? 1 : 0;
            if (value2 != 0 && fluidState2 != 0 && mc.player.getEyePosition().distanceTo(BlockFaces.getFacePoint(blockPos, direction)) <= reach + 0.75) {
               return placeData;
            }
         }

         Rotation facePoint = LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos, direction));
         if (BlockRayTrace.raycastBlock(facePoint, blockPos, reach) != null) {
            return placeData;
         } else if (placeData.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
            return !this.shouldNotBreak(blockPos) && this.findBestHotbarToolSlot(blockPos) != -1 ? placeData : null;
         } else if ((placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL || placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN)
            && this.canExecutePlaceThroughWalls(placeData)) {
            this.stopBreaking();
            return placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
               ? new CrystalAuraModule.PlaceData(blockPos.immutable(), Direction.UP, placeData.damage(), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null)
               : placeData;
         } else if (!this.shouldDig()) {
            return null;
         } else {
            BlockPos firstBreakableOnPath = this.findFirstBreakableOnPath(blockPos, direction);
            if (firstBreakableOnPath == null) {
               return null;
            } else if ((placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL || placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN)
               && !this.isCrystalBase(mc.level.getBlockState(firstBreakableOnPath))
               && !this.isUnbreakable(firstBreakableOnPath)
               && this.canExecutePlaceThroughWalls(placeData)) {
               this.stopBreaking();
               return placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
                  ? new CrystalAuraModule.PlaceData(blockPos.immutable(), Direction.UP, placeData.damage(), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null)
                  : placeData;
            } else {
               BlockHitResult obstruction = BlockRayTrace.findObstruction(blockPos, direction);
               Direction blockPos4 = obstruction != null && obstruction.getBlockPos().equals(firstBreakableOnPath) ? obstruction.getDirection() : Direction.UP;
               return new CrystalAuraModule.PlaceData(firstBreakableOnPath, blockPos4, placeData.damage(), CrystalAuraModule.PlaceResult.BREAK, null);
            }
         }
      } else {
         return placeData;
      }
   }

   private BlockHitResult clipWalk(Vec3 position, Vec3 position2) {
      if (mc.level != null) {
         if (mc.player != null) {
            return mc.level.clip(new ClipContext(position, position2, Block.COLLIDER, Fluid.NONE, mc.player));
         }
      }

      return null;
   }

   private boolean isUnsafeBoomForFriends(Vec3 position) {
      if (mc.level != null && position != null) {
         double distance = 4.0;

         for (Player player : mc.level.players()) {
            if (player != mc.player && player.isAlive() && this.isFriend(player) && player.position().distanceTo(position) < distance) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public boolean isAutoDigging() {
      return this.shouldDig();
   }

   private boolean isBlockedByEntitiesForBreak(Iterable<Entity> entitys, BlockPos blockPos, Player player) {
      Iterator iterator = entitys.iterator();

      Entity entity;
      do {
         if (!iterator.hasNext()) {
            return false;
         }

         entity = (Entity)iterator.next();
      } while (entity == player || !VirtualExplosionDamage.intersectsEntityBox(blockPos, entity) || entity instanceof ItemEntity);

      return true;
   }

   private BlockPos findCorrectionStandPos(BlockPos blockPos, Player player) {
      if (mc.player != null && mc.level != null) {
         Direction approximateNearest = Direction.getApproximateNearest(blockPos.getX() - player.getX(), 0.0, blockPos.getZ() - player.getZ());
         if (approximateNearest.getAxis().isVertical()) {
            approximateNearest = Direction.fromYRot(mc.player.getYRot()).getOpposite();
         }

         Direction[] directionValues = new Direction[]{approximateNearest, approximateNearest.getClockWise(), approximateNearest.getCounterClockWise(), approximateNearest.getOpposite()};
         BlockPos blockPos2 = null;
         double mAX_VALUE = Double.MAX_VALUE;

         for (int value = 0; value < directionValues.length; value++) {
            BlockPos blockPos3 = blockPos.relative(directionValues[value]);
            if (this.canUseCorrectionStand(blockPos3, player)) {
               double computedValue = mc.player.distanceToSqr(Vec3.atCenterOf(blockPos3)) + value * 2.0;
               BlockState blockState = mc.level.getBlockState(blockPos3);
               if (!blockState.isAir() && !blockState.canBeReplaced() && !blockState.is(Blocks.COBWEB)) {
                  computedValue -= 12.0;
               }

               if (computedValue < mAX_VALUE) {
                  mAX_VALUE = computedValue;
                  blockPos2 = blockPos3.immutable();
               }
            }
         }

         return blockPos2;
      } else {
         return null;
      }
   }

   private boolean isCorrectionBreakable(BlockPos blockPos, Player player) {
      if (blockPos != null) {
         if (!blockPos.equals(this.correctionBasePos) && !player.blockPosition().equals(blockPos)) {
            if (!player.blockPosition().above().equals(blockPos)) {
               BlockState blockState = mc.level.getBlockState(blockPos);
               if (this.isCrystalBase(blockState)) {
                  return false;
               }

               if (!blockState.isAir() && !blockState.canBeReplaced() && !blockState.is(Blocks.COBWEB) && !this.shouldNotBreak(blockPos)) {
                  if (this.findBestHotbarToolSlot(blockPos) != -1) {
                     return true;
                  }
               }

               return false;
            }
         }
      }

      return false;
   }

   private void drawFilledBlock(BlockPos blockPos, int alpha, float coordinate) {
      int alpha2 = -1;
      double x = blockPos.getX() + 0.5;
      double y = blockPos.getY() + 0.5;
      double z = blockPos.getZ() + 0.5;
      float computedValue = 0.4F + coordinate * 0.6F;
      double clampedValue = 0.5 * Mth.clamp(computedValue, 0.05F, 1.0F);
      alpha2 = (byte)-1;
      AABB bounds = new AABB(x - clampedValue, y - clampedValue, z - clampedValue, x + clampedValue, y + clampedValue, z + clampedValue);
      float clampedValue2 = Mth.clamp(coordinate * 0.78F, 0.0F, 1.0F);
      alpha2 = WriteHexByteHelper.multiplyAlpha(alpha, clampedValue2);
      AddQuadHelper.addFilledBox(bounds, alpha2);
   }

   @Override
   protected void onDisable() {
      this.rotation = null;
      this.goalData = null;
      this.lastData = null;
      this.stopBreaking();
      this.restoreSavedHotbarSlot();
      this.crystalHotbarSlot = -1;
      this.lastObsidianPlaceMs = 0L;
      this.lastCrystalPlaceMs = 0L;
      this.lastCrystalInteractMs = 0L;
      this.crystalGoalSinceMs = 0L;
      this.pendingObsidianPos = null;
      this.pendingCrystalBase = null;
      this.crystalPlaceBlocked = false;
      this.crystalPlaceBase = null;
      this.clearFullCorrection();
      this.failedGoals.clear();
      this.lastTargetFeet = null;
      this.stopRotation();
      super.onDisable();
   }

   private void collectAntiExplosionCandidates(Player player, Player player2, List<CrystalAuraModule.PlaceData> placeDatas) {
      HashSet entries = new HashSet();
      if (mc.level instanceof ClientLevelGetDestroyingBlocksAccessor2 clientLevelGetDestroyingBlocksAccessor2) {
         Int2ObjectMap<BlockDestructionProgress> lookup = clientLevelGetDestroyingBlocksAccessor2.skycore$getDestroyingBlocks();

         for (BlockDestructionProgress blockDestructionProgress : lookup.values()) {
            if (mc.level.getEntity(blockDestructionProgress.getId()) instanceof Player player3 && player3 != player2 && !this.isFriend(player3)) {
               this.addAntiExplosionCandidate(blockDestructionProgress.getPos(), player3, player, player2, placeDatas, entries);
            }
         }
      }

      for (Player player4 : mc.level.players()) {
         if (player4 != player2 && !this.isFriend(player4) && player4.swinging) {
            BlockPos enemyLookedBlock = this.getEnemyLookedBlock(player4, this.getReach());
            if (enemyLookedBlock != null) {
               this.addAntiExplosionCandidate(enemyLookedBlock, player4, player, player2, placeDatas, entries);
            }
         }
      }
   }

   private void collectBreakCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      HashSet entries;
      if (this.isNewMode()) {
         entries = new HashSet();
      } else {
         entries = null;
      }

      var entries2 = entries;
      this.collectBreakAround(player, player2, entitys, placeDatas, player2.blockPosition(), entries2);
      if (this.isNewMode()) {
         BlockPos blockPos = player.blockPosition();
         if (!blockPos.equals(player2.blockPosition())) {
            this.collectBreakAround(player, player2, entitys, placeDatas, blockPos, entries2);
         }
      }
   }

   private double getMinHealth() {
      return this.minHealth.get().floatValue();
   }

   private double heightPenalty(BlockPos blockPos, Player player) {
      if (blockPos != null && player != null) {
         double y = blockPos.getY() + 0.5;
         double y2 = y - player.getY();
         return y2 <= 0.25 ? 0.0 : y2 * 18.0;
      } else {
         return 0.0;
      }
   }

   private boolean canExecutePlaceThroughWalls(CrystalAuraModule.PlaceData placeData) {
      if (placeData != null && placeData.blockPos() != null) {
         if (placeData.face() != null) {
            CrystalAuraModule.PlaceResult placeResult = placeData.placeResult();
            if (placeResult != CrystalAuraModule.PlaceResult.PLACE_CRYSTAL) {
               CrystalAuraModule.PlaceResult placeResult2 = placeData.placeResult();
               if (placeResult2 != CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
                  return false;
               }
            }

            if (this.canPlaceThroughSoftWalls(placeData.blockPos())) {
               return true;
            }

            CrystalAuraModule.PlaceResult placeResult3 = placeData.placeResult();
            if (placeResult3 == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
               BlockPos blockPos = placeData.blockPos();
               return this.canPlaceThroughSoftWalls(blockPos.relative(placeData.face()));
            }

            BlockPos blockPos2 = placeData.blockPos();
            return this.canPlaceThroughSoftWalls(blockPos2.above());
         }
      }

      return false;
   }

   private boolean holdHotbarSlot(int slot) {
      if (slot >= 0 && slot <= 8) {
         if (mc.player != null) {
            this.rememberSavedHotbarSlot();
            if (mc.player.getInventory().getSelectedSlot() != slot) {
               UseItemHelper.switchToHotbarSlot(slot);
            }

            return true;
         }
      }

      return false;
   }

   private boolean canClearCorrectionCell(BlockPos blockPos) {
      BlockState blockState = mc.level.getBlockState(blockPos);
      if (!blockState.isAir() && !blockState.canBeReplaced()) {
         if (!blockState.is(Blocks.COBWEB)) {
            return !this.shouldNotBreak(blockPos) && this.findBestHotbarToolSlot(blockPos) != -1;
         }
      }

      return true;
   }

   private BlockPos traceCorrectionObstruction(Vec3 position, Vec3 position2, Player player) {
      BlockHitResult blockHitResult = mc.level.clip(new ClipContext(position, position2, Block.COLLIDER, Fluid.NONE, mc.player));
      Type type = blockHitResult.getType();
      if (type != Type.BLOCK) {
         return null;
      } else {
         BlockPos blockPos = blockHitResult.getBlockPos();
         if (this.isCorrectionBreakable(blockPos, player) && this.isCorrectionBlockInReach(blockPos)) {
            return blockPos.immutable();
         } else {
            return null;
         }
      }
   }

   private boolean collectObsidianCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      var entries = this.isNewMode() ? new HashSet() : null;
      if (this.collectObsidianAround(player, player2, entitys, placeDatas, player2.blockPosition(), entries)) {
         return true;
      }

      if (this.isNewMode()) {
         BlockPos blockPos = player.blockPosition();
         if (!blockPos.equals(player2.blockPosition()) && this.collectObsidianAround(player, player2, entitys, placeDatas, blockPos, entries)) {
            return true;
         }
      }

      for (Entity entity : entitys) {
         if (entity instanceof EndCrystal endCrystal) {
            Rotation nearestPoint = LookHelper.calculateAngle(EntityAimPoint.getNearestPoint(endCrystal, 3.0));
            double explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), player, false) + 30.0;
            BlockPos blockPos2 = endCrystal.blockPosition().below();
            Direction uP = Direction.UP;
            if (!this.shouldNotExplodeCrystal(endCrystal)
               && (
                  BlockRayTrace.isLookingAtEntity(nearestPoint, endCrystal, 3.0)
                     || BlockRayTrace.raycastBlock(LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos2, uP)), blockPos2, this.getReach()) != null
               )) {
               placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2, uP, explosionDamage, CrystalAuraModule.PlaceResult.EXPLOSION, endCrystal));
            }
         }
      }

      return false;
   }

   private EndCrystal findCrystalAt(BlockPos blockPos) {
      if (mc.level == null) {
         return null;
      }


      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal endCrystal) {
            BlockPos blockPos2 = endCrystal.blockPosition();
            if (blockPos2.equals(blockPos)) {
               return endCrystal;
            }
         }
      }

      return null;
   }

   private boolean isAntiExplosionGoal(CrystalAuraModule.PlaceData placeData) {
      return placeData != null && placeData.damage() >= 0.0;
   }

   private void promoteObsidianToCrystal(BlockPos crystalPlaceBase2) {
      this.crystalPlaceBase = crystalPlaceBase2.immutable();
      this.crystalPlaceBlocked = false;
      this.pendingCrystalBase = null;
      long now = System.currentTimeMillis();
      this.crystalGoalSinceMs = now;
      double damage;
      if (this.goalData != null) {
         damage = this.goalData.damage();
      } else {
         damage = 40.0;
      }

      double value = damage;
      BlockPos blockPos = crystalPlaceBase2.immutable();
      Direction uP = Direction.UP;
      double maximum = Math.max(value, 80.0);
      this.goalData = new CrystalAuraModule.PlaceData(blockPos, uP, maximum, CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null);
      this.lastData = this.goalData;
   }

   private void upsertVisual(BlockPos blockPos, float index, float index2, float index3) {
      long timestamp = System.currentTimeMillis() + 450L;

      for (CrystalAuraModule.VisualData visualData : this.visualDatas) {
         if (visualData.blockPos.equals(blockPos)) {
            visualData.r = index;
            visualData.g = index2;
            visualData.b = index3;
            visualData.fadeTarget = 1.0F;
            if (!visualData.goalHighlight) {
               visualData.holdUntilMs = timestamp;
            }

            return;
         }
      }

      CrystalAuraModule.VisualData visualData2 = new CrystalAuraModule.VisualData(blockPos, index, index2, index3, false);
      visualData2.fadeTarget = 1.0F;
      visualData2.holdUntilMs = timestamp;
      this.visualDatas.add(visualData2);
   }

   private int resolveItemSlot(Item item) {
      int slot;
      if (item == Items.END_CRYSTAL) {
         slot = this.crystalHotbarSlot;
      } else {
         slot = -1;
      }

      int slot2 = slot;
      int hotbarSlot = UseItemHelper.findHotbarSlot(item, slot2);
      if (hotbarSlot != -1) {
         if (item == Items.END_CRYSTAL) {
            this.crystalHotbarSlot = hotbarSlot;
         }
      }

      return hotbarSlot;
   }

   private boolean isBreakingThroughBlocks() {
      if (this.breakingBlock == null) {
         if (this.correctionBreakPos == null) {
            if (this.lastData != null) {
               if (this.lastData.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
                  return true;
               }
            }

            if (this.goalData != null) {
               if (this.goalData.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
                  return true;
               }
            }

            return false;
         }
      }

      return true;
   }

   private boolean isFailedGoal(CrystalAuraModule.PlaceData placeData) {
      var failedGoals2 = this.failedGoals;
      Long goalKey = this.getGoalKey(placeData);
      return failedGoals2.containsKey(goalKey);
   }

   private CrystalAuraModule.PlacementHit findSolidNeighborHit(BlockPos blockPos) {
      if (mc.player != null && mc.level != null) {
         Direction[] directionValues = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
         CrystalAuraModule.PlacementHit placementHit = null;
         double mAX_VALUE = Double.MAX_VALUE;
         Vec3 eyePosition = mc.player.getEyePosition();

         for (Direction direction : directionValues) {
            BlockPos opposite = blockPos.relative(direction.getOpposite());
            BlockState blockState = mc.level.getBlockState(opposite);
            if (!blockState.isAir() && !blockState.canBeReplaced() && blockState.getFluidState().isEmpty()) {
               Vec3 highPlaceHit = this.getHighPlaceHit(opposite, direction, blockPos);
               double distanceSquared = eyePosition.distanceToSqr(highPlaceHit);
               double computedValue = direction == Direction.UP ? -2.0 : (direction == Direction.DOWN ? 8.0 : 0.0);
               double computedValue2 = distanceSquared + computedValue;
               if (computedValue2 < mAX_VALUE) {
                  mAX_VALUE = computedValue2;
                  placementHit = new CrystalAuraModule.PlacementHit(opposite, direction, new BlockHitResult(highPlaceHit, direction, opposite, false), highPlaceHit);
               }
            }
         }

         return placementHit;
      } else {
         return null;
      }
   }

   private boolean hasReachedCorrectionPosition(BlockPos blockPos) {
      if (mc.player != null && blockPos != null) {
         double x = blockPos.getX() + 0.5;
         double x2 = x - mc.player.getX();
         double z = blockPos.getZ() + 0.5;
         double z2 = z - mc.player.getZ();
         if (x2 * x2 + z2 * z2 <= 0.2) {
            double y = mc.player.getY() - blockPos.getY();
            if (Math.abs(y) <= 0.75) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean holdHotbarItem(Item item) {
      int slot = this.resolveItemSlot(item);
      return this.holdHotbarSlot(slot);
   }

   private boolean hasGoodThroughWallDamage(double value) {
      return value >= 4.0;
   }

   private boolean isLookingReady(BlockPos blockPos, Direction direction, Vec3 position) {
      if (mc.player == null) {
         return false;
      }

      Vec3 facePoint = position != null ? position : BlockFaces.getFacePoint(blockPos, direction);
      if (!this.rotate.is("Без ротации")) {
         Rotation eyePosition = Rotation.lookingAt(facePoint, mc.player.getEyePosition());
         Rotation rotation2 = new Rotation(mc.player.getYRot(), mc.player.getXRot());
         if (rotation2.getDelta(eyePosition) > 8.0F) {
            return false;
         }
      }

      return this.canPlaceThroughSoftWalls(blockPos) ? true : this.raycastPlaceHit(blockPos, direction, facePoint) != null;
   }

   private boolean isEnemyBreakingThreat(BlockPos blockPos, Player player, Player player2) {
      if (mc.level != null && player != null && player2 != null) {
         BlockPos blockPos2 = blockPos.below();
         if (this.isCrystalBase(mc.level.getBlockState(blockPos2))) {
            return VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos), player2, false) >= 14.0;
         }

         double damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos.above()), player2, blockPos);
         return damageOfGhostBlock >= 14.0
            ? true
            : player.distanceToSqr(Vec3.atCenterOf(blockPos)) <= 36.0
               && VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos.above()), player2, false) >= 14.0;
      } else {
         return false;
      }
   }

   private void handleExplosion() {
      if (mc.player != null && this.goalData != null) {
         LocalPlayer player = mc.player;
         EndCrystal endCrystal = this.goalData.entity() instanceof EndCrystal endCrystal2 ? endCrystal2 : null;
         if (endCrystal == null || !endCrystal.isAlive()) {
            this.goalData = null;
            this.lastData = null;
            this.rotation = null;
         } else if (this.shouldNotExplodeCrystal(endCrystal)) {
            this.markGoalFailed(this.goalData);
            this.goalData = null;
            this.lastData = null;
            this.rotation = null;
         } else if (!this.isAntiExplosionGoal(this.goalData)
            && !this.isSelfDamageAllowed(player, VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), player, false))) {
            this.markGoalFailed(this.goalData);
            this.goalData = null;
            this.lastData = null;
            this.rotation = null;
         } else {
            Rotation crystalExplodeRotation = this.getCrystalExplodeRotation(endCrystal);
            this.rotation = crystalExplodeRotation;
            if (this.rotate.is("Vanilla")) {
               this.setRotation(crystalExplodeRotation);
            }

            int boundingBox = !BlockRayTrace.canReachBox(endCrystal.getBoundingBox(), 3.0) && !(endCrystal.distanceToSqr(mc.player) <= 9.0) ? 0 : 1;
            if (boundingBox == 0) {
               this.applyRotation();
            } else {
               boolean lookingAtEntity = BlockRayTrace.isLookingAtEntity(endCrystal, 3.0);
               boolean boundingBox2 = BlockRayTrace.rayTrace(crystalExplodeRotation, 3.0, endCrystal.getBoundingBox());
               int value = this.canPlaceThroughSoftWalls(endCrystal.blockPosition()) && this.hasGoodThroughWallDamage(this.goalData.damage()) ? 1 : 0;
               int value2 = !this.rotate.is("Без ротации") && !lookingAtEntity && !boundingBox2 && value == 0 ? 0 : 1;
               if (value2 == 0) {
                  this.applyRotation();
               } else {
                  mc.gameMode.attack(mc.player, endCrystal);
                  mc.player.swing(InteractionHand.MAIN_HAND);
                  this.clearCrystalPending();
                  this.pendingObsidianPos = null;
                  this.failedGoals.clear();
                  this.goalData = null;
                  this.lastData = null;
                  this.rotation = null;
               }
            }
         }
      }
   }

   private boolean isUnbreakable(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (!blockState.is(Blocks.BEDROCK) && !blockState.is(Blocks.BARRIER)) {
            if (!blockState.is(Blocks.END_PORTAL_FRAME)) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   private CrystalAuraModule.PlaceData selectGoal(Player player, Player player2, Iterable<Entity> entitys) {
      ArrayList<CrystalAuraModule.PlaceData> placeDatas = new ArrayList();
      if (this.antiExplosion.get()) {
         this.collectAntiExplosionCandidates(player, player2, placeDatas);
      }

      this.collectExplosionCandidates(player, entitys, placeDatas);
      this.collectSideCrystalCandidates(player, player2, entitys, placeDatas);
      byte flag = 0;

      for (CrystalAuraModule.PlaceData placeData : placeDatas) {
         if (placeData.placeResult() == CrystalAuraModule.PlaceResult.EXPLOSION) {
            flag = 1;
            break;
         }

         if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL && !this.isCrystalTooHigh(placeData.blockPos(), player) && placeData.damage() >= 6.0) {
            flag = 1;
            break;
         }
      }

      if (flag == 0) {
         this.collectSideObsidianCandidates(player, player2, entitys, placeDatas, true);
         byte flag2 = 0;

         for (CrystalAuraModule.PlaceData placeData2 : placeDatas) {
            if (placeData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
               flag2 = 1;
               break;
            }
         }

         if (this.shouldDig()) {
            this.collectRecessDigCandidates(player, player2, entitys, placeDatas);
         }

         byte flag3 = 0;

         for (CrystalAuraModule.PlaceData placeData3 : placeDatas) {
            if (placeData3.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
               flag3 = 1;
               break;
            }
         }

         if (flag2 == 0 && flag3 == 0) {
            this.collectSideObsidianCandidates(player, player2, entitys, placeDatas, false);
         }
      }

      placeDatas.sort((placeData4, placeData5) -> {
         double damage = placeData5.damage();
         int result = Double.compare(damage, placeData4.damage());
         double computedValue = placeData4.damage() - placeData5.damage();
         if (Math.abs(computedValue) > 1.5) {
            return result;
         }

         CrystalAuraModule.PlaceResult placeResult = placeData4.placeResult();
         return Integer.compare(this.actionPriority(placeResult), this.actionPriority(placeData5.placeResult()));
      });

      for (CrystalAuraModule.PlaceData placeData6 : placeDatas) {
         if (!this.isFailedGoal(placeData6)
            && this.isGoalExecutable(placeData6, player)
            && (this.isAntiExplosionGoal(placeData6) || this.isFullCorrectionActive() || this.isSafeSelfDamage(player2, placeData6))) {
            return placeData6;
         }
      }

      return null;
   }

   public boolean appliesMovementCorrection() {
      if (this.isEnabled()) {
         if (net.skycore.client.module.combat.CrystalAuraModule.INSTANCE.isVanillaModeActive()) {
            if (mc.player != null) {
               if (this.correctionType.is("Нет")) {
                  Boolean get = this.rage.get();
                  if (!get) {
                     return false;
                  }
               }

               if (this.isFullCorrectionActive()) {
                  return true;
               }

               if (RotationManager.INSTANCE.isRotating() || this.rotation != null && this.goalData != null) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void handleBreak(BlockPos breakingBlock2, Direction breakingDirection2) {
      if (mc.player != null && mc.gameMode != null && mc.level != null) {
         this.requestWebIgnore();
         BlockState blockState = mc.level.getBlockState(breakingBlock2);
         if (!blockState.isAir() && !blockState.is(Blocks.COBWEB) && (blockState.getFluidState().isEmpty() || !blockState.getCollisionShape(mc.level, breakingBlock2).isEmpty())) {
            if (this.shouldNotBreak(breakingBlock2)) {
               this.stopBreaking();
            } else {
               int bestHotbarToolSlot = this.findBestHotbarToolSlot(breakingBlock2);
               if (bestHotbarToolSlot == -1) {
                  this.stopBreaking();
               } else if (!this.holdHotbarSlot(bestHotbarToolSlot)) {
                  this.stopBreaking();
               } else {
                  if (this.breakingBlock != null && !this.breakingBlock.equals(breakingBlock2)) {
                     BlockState blockState2 = mc.level.getBlockState(this.breakingBlock);
                     if (!blockState2.isAir()) {
                        mc.gameMode.stopDestroyBlock();
                     }

                     this.breakingBlock = null;
                     this.breakingDirection = null;
                  }

                  if (this.breakingBlock != null && this.breakingBlock.equals(breakingBlock2)) {
                     mc.gameMode.continueDestroyBlock(breakingBlock2, breakingDirection2);
                  } else {
                     mc.gameMode.startDestroyBlock(breakingBlock2, breakingDirection2);
                     this.breakingBlock = breakingBlock2.immutable();
                     this.breakingDirection = breakingDirection2;
                  }

                  MultiPlayerGameModeEnsureHasSentCarriedItemAccessor multiPlayerGameModeEnsureHasSentCarriedItemAccessor = (MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)mc.gameMode;
                  if (multiPlayerGameModeEnsureHasSentCarriedItemAccessor.skycore$getDestroyProgress() > 0.0F) {
                     mc.level.addBreakingBlockEffect(breakingBlock2, breakingDirection2);
                  }

                  this.upsertVisual(breakingBlock2, RGB_BREAK[0], RGB_BREAK[1], RGB_BREAK[2]);
                  if (mc.level.getBlockState(breakingBlock2).isAir()) {
                     boolean condition = breakingBlock2.equals(this.correctionBreakPos);
                     this.stopBreaking();
                     this.correctionBreakPos = null;
                     if (!condition) {
                        this.tryPromoteDigToCrystal(breakingBlock2);
                     }
                  }
               }
            }
         } else {
            if (breakingBlock2.equals(this.correctionBreakPos)) {
               this.correctionBreakPos = null;
            }

            this.stopBreaking();
         }
      }
   }

   private void requestWebIgnore() {
      RequestTempHelper.INSTANCE.requestTemp(20);
   }

   private boolean isNewMode() {
      return this.mode.is("New");
   }

   private long getCrystalPlaceDelayMs() {
      Double value = this.crystalsPerSecond.get();
      float value2 = value.floatValue();
      float maximum = Math.max(0.5F, value2);
      long timestamp = (long)(1000.0F / maximum);
      return Math.max(1L, timestamp);
   }

   private void syncGoalVisual() {
      CrystalAuraModule.PlaceData placeData = this.lastData != null ? this.lastData : this.goalData;
      BlockPos blockPos = null;
      float[] rGB_OBSIDIAN = RGB_OBSIDIAN;
      int value = this.breakingBlock != null
            && mc.gameMode != null
            && mc.gameMode.isDestroying()
            && this.breakingBlock.equals(((MultiPlayerGameModeEnsureHasSentCarriedItemAccessor)mc.gameMode).skycore$getDestroyBlockPos())
         ? 1
         : 0;
      if (value != 0) {
         blockPos = this.breakingBlock;
         rGB_OBSIDIAN = RGB_BREAK;
      } else if (placeData != null && placeData.blockPos() != null && placeData.face() != null && placeData.placeResult() != CrystalAuraModule.PlaceResult.BREAK) {
         blockPos = switch (placeData.placeResult()) {
            case PLACE_OBSIDIAN -> placeData.blockPos().relative(placeData.face());
            case PLACE_CRYSTAL, EXPLOSION -> placeData.blockPos().above();
            case BREAK -> placeData.blockPos();
            default -> placeData.blockPos();
         };

         rGB_OBSIDIAN = switch (placeData.placeResult()) {
            case PLACE_OBSIDIAN -> RGB_OBSIDIAN;
            case PLACE_CRYSTAL, EXPLOSION -> RGB_CRYSTAL;
            case BREAK -> RGB_BREAK;
            default -> RGB_OBSIDIAN;
         };
      }

      if (value == 0) {
         this.visualDatas.removeIf(visualData -> {
            if (visualData.r == RGB_BREAK[0]) {
               if (visualData.g == RGB_BREAK[1]) {
                  if (visualData.b == RGB_BREAK[2]) {
                     return true;
                  }
               }
            }

            return false;
         });
      }

      for (CrystalAuraModule.VisualData visualData2 : this.visualDatas) {
         if (blockPos != null && visualData2.blockPos.equals(blockPos)) {
            visualData2.r = rGB_OBSIDIAN[0];
            visualData2.g = rGB_OBSIDIAN[1];
            visualData2.b = rGB_OBSIDIAN[2];
            visualData2.goalHighlight = true;
            visualData2.fadeTarget = 1.0F;
            visualData2.holdUntilMs = Long.MAX_VALUE;
         } else if (visualData2.goalHighlight) {
            visualData2.goalHighlight = false;
            visualData2.fadeTarget = 0.0F;
         }
      }

      if (blockPos != null) {
         byte flag = 0;

         for (CrystalAuraModule.VisualData visualData3 : this.visualDatas) {
            if (visualData3.blockPos.equals(blockPos)) {
               flag = 1;
               break;
            }
         }

         if (flag == 0) {
            CrystalAuraModule.VisualData visualData4 = new CrystalAuraModule.VisualData(blockPos, rGB_OBSIDIAN[0], rGB_OBSIDIAN[1], rGB_OBSIDIAN[2], true);
            visualData4.fadeTarget = 1.0F;
            visualData4.holdUntilMs = Long.MAX_VALUE;
            this.visualDatas.add(visualData4);
         }
      }
   }

   private boolean shouldNotExplodeCrystal(Entity entity) {
      if (entity == null) {
         return true;
      }

      AABB boundingBox = entity.getBoundingBox();
      Vec3 center = boundingBox.getCenter();
      if (this.safety.isSelected("Друзей")) {
         if (this.isUnsafeBoomForFriends(center)) {
            return true;
         }
      }

      return this.shouldNotExplodeBecauseOfResources(entity);
   }

   public boolean isVanillaRotating() {
      if (this.isEnabled()) {
         if (net.skycore.client.module.combat.CrystalAuraModule.INSTANCE.isVanillaModeActive()) {
            if (this.rotate.is("Vanilla")) {
               return true;
            }
         }
      }

      return false;
   }

   public void renderWorld() {
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         AddQuadHelper.beginFrame();
         this.syncGoalVisual();
         long now = System.currentTimeMillis();
         this.visualDatas.removeIf(visualData -> {
            if (!visualData.goalHighlight && visualData.fadeTarget >= 1.0F && now >= visualData.holdUntilMs) {
               visualData.fadeTarget = 0.0F;
            }

            visualData.animation.update(visualData.fadeTarget);
            if (visualData.fadeTarget <= 0.0F) {
               if (visualData.animation.getValue() <= 0.01F) {
                  if (visualData.animation.isDone()) {
                     return true;
                  }
               }
            }

            return false;
         });

         for (CrystalAuraModule.VisualData visualData2 : this.visualDatas) {
            float value = visualData2.animation.getValue();
            if (!(value <= 0.01F)) {
               int computedValue = (int)(visualData2.r * 255.0F) << 16 | (int)(visualData2.g * 255.0F) << 8 | (int)(visualData2.b * 255.0F);
               this.drawFilledBlock(visualData2.blockPos, 0xFF000000 | computedValue, value);
            }
         }

         AddQuadHelper.endFrame();
      }
   }

   private boolean hasMovementInput() {
      if (mc.player == null) {
         return false;
      }

      if (mc.player.input != null) {
         Vec2 moveVector = mc.player.input.getMoveVector();
         if (moveVector.lengthSquared() > 1.0E-4F) {
            return true;
         }
      }

      if (!mc.options.keyUp.isDown()) {
         if (!mc.options.keyDown.isDown()) {
            if (!mc.options.keyLeft.isDown()) {
               if (!mc.options.keyRight.isDown()) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private double getBoxRange() {
      double reach = this.getReach();
      double value = this.isNewMode() ? 5.0 : 3.0;
      return Math.max(reach, value);
   }

   private void resetCombatState() {
      this.goalData = null;
      this.lastData = null;
      this.rotation = null;
      this.pendingObsidianPos = null;
      this.clearCrystalPending();
      this.stopBreaking();
      this.failedGoals.clear();
      this.lastTargetFeet = null;
      this.clearFullCorrection();
      this.restoreSavedHotbarSlot();
      this.stopRotation();
   }

   private void clearFullCorrection() {
      this.correctionStandPos = null;
      this.correctionBasePos = null;
      this.correctionBreakPos = null;
   }

   private boolean hasObsidianInHotbar() {
      return UseItemHelper.findHotbarSlot(Items.OBSIDIAN, -1) != -1;
   }

   private void stopBreaking() {
      if (this.breakingBlock != null) {
         if (mc.gameMode != null) {
            mc.gameMode.stopDestroyBlock();
         }
      }

      this.breakingBlock = null;
      this.breakingDirection = null;
   }

   private boolean isSolidWalk(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (!blockState.isAir()) {
            if (!blockState.is(Blocks.COBWEB) && !blockState.canBeReplaced()) {
               VoxelShape collisionShape = blockState.getCollisionShape(mc.level, blockPos);
               return !collisionShape.isEmpty();
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private boolean isSelfDamageAllowed(Player player, double damage) {
      if (player != null && !player.isCreative()) {
         if (!player.isSpectator()) {
            if (damage > this.getMaxSelfDamage()) {
               return false;
            }

            float health = player.getHealth() + player.getAbsorptionAmount() - (float)damage;
            return health >= this.getMinHealth();
         }
      }

      return true;
   }

   private CrystalAuraModule.PlaceData resolveObstructionBreak(BlockPos blockPos, Direction direction, double value) {
      if (!this.shouldDig()) {
         return null;
      }

      BlockHitResult obstruction = BlockRayTrace.findObstruction(blockPos, direction);
      if (obstruction == null) {
         return null;
      }

      BlockPos blockPos2 = obstruction.getBlockPos();
      if (blockPos2.equals(blockPos)) {
         return null;
      }

      if (!this.isCobweb(blockPos2)) {
         if (!this.shouldNotBreak(blockPos2)) {
            if (this.findBestHotbarToolSlot(blockPos2) != -1) {
               return new CrystalAuraModule.PlaceData(blockPos2, obstruction.getDirection(), value, CrystalAuraModule.PlaceResult.BREAK, null);
            }
         }
      }

      return null;
   }

   private boolean withinThroughWallRange(BlockPos blockPos) {
      return this.canPlaceThroughSoftWalls(blockPos);
   }

   private double getMinBreakDamage() {
      return this.isNewMode() ? 14.0 : 19.0;
   }

   private boolean prepareFullCorrection(CrystalAuraModule.PlaceData placeData, Player player) {
      if (mc.player != null && mc.level != null && placeData != null && player != null) {
         if (this.correctionBreakPos != null && mc.level.getBlockState(this.correctionBreakPos).isAir()) {
            this.stopBreaking();
            this.correctionBreakPos = null;
         }

         BlockPos correctionBase = this.getCorrectionBase(placeData, player);
         if (correctionBase == null) {
            return true;
         }

         if (this.correctionBasePos == null || !this.correctionBasePos.equals(correctionBase) || this.correctionStandPos == null) {
            this.correctionBasePos = correctionBase.immutable();
            this.correctionStandPos = this.findCorrectionStandPos(correctionBase, player);
            this.correctionBreakPos = null;
         }

         if (this.correctionStandPos == null) {
            return !this.rage.get();
         } else {
            BlockPos correctionObstruction = this.findCorrectionObstruction(this.correctionStandPos, player);
            if (correctionObstruction != null) {
               Direction closestFace = BlockFaces.getClosestFace(correctionObstruction, mc.player);
               CrystalAuraModule.PlaceData placeData2 = new CrystalAuraModule.PlaceData(
                  correctionObstruction.immutable(), closestFace, Math.max(placeData.damage(), 1.0), CrystalAuraModule.PlaceResult.BREAK, null
               );
               this.correctionBreakPos = correctionObstruction.immutable();
               this.lastData = placeData2;
               this.rotation = LookHelper.calculateAngle(BlockFaces.getFacePoint(correctionObstruction, closestFace));
               this.applyRotation();
               this.handleBreak(correctionObstruction, closestFace);
               this.applyRotation();
               return false;
            } else {
               this.correctionBreakPos = null;
               if (!this.hasReachedCorrectionPosition(this.correctionStandPos)) {
                  this.updateGoalRotation(placeData);
                  this.applyRotation();
                  return false;
               } else {
                  return true;
               }
            }
         }
      } else {
         return true;
      }
   }

   private long getGoalKey(CrystalAuraModule.PlaceData placeData) {
      int value = 0;
      value = (byte)0;
      if (placeData != null) {
         if (placeData.blockPos() != null && placeData.placeResult() != null) {
            if (placeData.face() != null) {
               Direction direction = placeData.face();
               value = direction.ordinal();
            } else {
               value = 0;
            }

            int value2 = value;
            BlockPos blockPos = placeData.blockPos();
            long timestamp = blockPos.asLong();
            CrystalAuraModule.PlaceResult placeResult = placeData.placeResult();
            return timestamp ^ (long)placeResult.ordinal() << 32 ^ value2;
         }
      }

      return 0L;
   }

   private void handleCrystalPlacement(BlockPos crystalPlaceBase2) {
      this.requestWebIgnore();
      BlockPos blockPos = crystalPlaceBase2.above();
      if (!this.isCrystalBase(mc.level.getBlockState(crystalPlaceBase2))) {
         this.clearCrystalPending();
         this.lastData = null;
      } else if (!mc.level.getBlockState(blockPos).isAir()) {
         if (this.isCobweb(blockPos)) {
            this.requestWebIgnore();
         } else if (this.shouldDig() && !this.shouldNotBreak(blockPos) && this.findBestHotbarToolSlot(blockPos) != -1) {
            this.handleBreak(blockPos, BlockFaces.getClosestFace(blockPos, mc.player));
         } else {
            this.clearCrystalPending();
            this.lastData = null;
         }
      } else if (this.hasCrystalAt(blockPos)) {
         EndCrystal crystalAt = this.findCrystalAt(blockPos);
         this.clearCrystalPending();
         if (crystalAt != null) {
            this.goalData = new CrystalAuraModule.PlaceData(
               crystalPlaceBase2.immutable(),
               Direction.UP,
               Math.max(this.goalData != null ? this.goalData.damage() : 40.0, 40.0),
               CrystalAuraModule.PlaceResult.EXPLOSION,
               crystalAt
            );
            this.lastData = this.goalData;
            this.handleExplosion();
         }
      } else {
         long now = System.currentTimeMillis();
         if (this.crystalGoalSinceMs <= 0L) {
            this.crystalGoalSinceMs = now;
         }

         if (now - this.crystalGoalSinceMs >= 100L) {
            if (this.goalData != null) {
               this.markGoalFailed(this.goalData);
            }

            this.clearCrystalPending();
            this.pendingObsidianPos = null;
            this.goalData = null;
            this.lastData = null;
            this.rotation = null;
         } else {
            if (this.pendingCrystalBase != null && this.pendingCrystalBase.equals(crystalPlaceBase2)) {
               if (now - this.lastCrystalInteractMs < 300L) {
                  return;
               }

               if (!this.canPlaceCrystalNow()) {
                  return;
               }
            } else if (!this.canPlaceCrystalNow()) {
               return;
            }

            if (this.holdHotbarItem(Items.END_CRYSTAL)) {
               Vec3 eyePosition = mc.player.getEyePosition();
               Direction uP = Direction.UP;
               Vec3 position = new Vec3(crystalPlaceBase2.getX() + 0.5, crystalPlaceBase2.getY() + 1.0, crystalPlaceBase2.getZ() + 0.5);
               if (!this.canPlaceThroughSoftWalls(crystalPlaceBase2)) {
                  position = CriticalHelper.getClosestVec(eyePosition, new AABB(crystalPlaceBase2));
                  Vec3 position2 = position.subtract(eyePosition).reverse();
                  uP = Direction.getApproximateNearest(position2.x, position2.y, position2.z);
               }

               Vec3 position3 = new Vec3(crystalPlaceBase2.getX() + 0.5, crystalPlaceBase2.getY() + 1.35, crystalPlaceBase2.getZ() + 0.5);
               this.rotation = Rotation.lookingAt(position3, eyePosition);
               this.applyRotation();
               this.stopBreaking();
               ItemUseCooldown.setItemUseCooldown(0);
               mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(position, uP, crystalPlaceBase2, false));
               mc.player.swing(InteractionHand.MAIN_HAND);
               this.lastCrystalPlaceMs = now;
               this.lastCrystalInteractMs = now;
               this.crystalPlaceBase = crystalPlaceBase2.immutable();
               this.pendingCrystalBase = crystalPlaceBase2.immutable();
               this.crystalPlaceBlocked = true;
               this.upsertVisual(blockPos, RGB_CRYSTAL[0], RGB_CRYSTAL[1], RGB_CRYSTAL[2]);
               EndCrystal crystalAt2 = this.findCrystalAt(blockPos);
               if (crystalAt2 != null) {
                  this.clearCrystalPending();
                  this.goalData = new CrystalAuraModule.PlaceData(crystalPlaceBase2.immutable(), Direction.UP, 80.0, CrystalAuraModule.PlaceResult.EXPLOSION, crystalAt2);
                  this.lastData = this.goalData;
                  this.handleExplosion();
               }
            }
         }
      }
   }

   private CrystalAuraModule.ReturnResult isBlockedByEntitiesForObsidian(Iterable<Entity> entitys, BlockPos blockPos, Player player) {
      for (Entity entity : entitys) {
         if (entity != player) {
            AABB bounds = new AABB(blockPos.getX(), blockPos.getY() - 0.2F, blockPos.getZ(), blockPos.getX() + 1.0, blockPos.getY() + 2.2, blockPos.getZ() + 1.0);
            if (entity instanceof ItemEntity && !entity.onGround() && entity.getBoundingBox().intersects(bounds)) {
               return CrystalAuraModule.ReturnResult.ITEM;
            }

            if ((entity instanceof Player || entity instanceof EndCrystal) && VirtualExplosionDamage.intersectsEntityBox(blockPos, entity)) {
               return CrystalAuraModule.ReturnResult.PLAYER;
            }
         }
      }

      return CrystalAuraModule.ReturnResult.NONE;
   }

   private Rotation getCrystalExplodeRotation(EndCrystal endCrystal) {
      AABB boundingBox = endCrystal.getBoundingBox();
      Vec3 eyePosition = mc.player.getEyePosition();
      double computedValue = (boundingBox.minY + boundingBox.maxY) * 0.5;
      double value;
      if (eyePosition.y < boundingBox.minY) {
         value = computedValue + (boundingBox.maxY - boundingBox.minY) * 0.15;
      } else if (eyePosition.y > boundingBox.maxY) {
         value = computedValue - (boundingBox.maxY - boundingBox.minY) * 0.1;
      } else {
         value = Math.min(boundingBox.maxY - 0.05, computedValue + 0.35);
      }

      Vec3 position = new Vec3((boundingBox.minX + boundingBox.maxX) * 0.5, value, (boundingBox.minZ + boundingBox.maxZ) * 0.5);
      return Rotation.lookingAt(position, eyePosition);
   }

   private void collectExplosionCandidates(Player player, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      for (Entity entity : entitys) {
         if (entity instanceof EndCrystal endCrystal && !this.shouldNotExplodeCrystal(endCrystal) && !(endCrystal.distanceToSqr(player) > 36.0)) {
            Rotation nearestPoint = LookHelper.calculateAngle(EntityAimPoint.getNearestPoint(endCrystal, 3.0));
            double explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), player, false);
            double height = explosionDamage + 30.0;
            height -= this.heightPenalty(endCrystal.blockPosition(), player);
            BlockPos blockPos = endCrystal.blockPosition().below();
            Direction uP = Direction.UP;
            int value = this.canPlaceThroughSoftWalls(endCrystal.blockPosition()) && this.hasGoodThroughWallDamage(explosionDamage) ? 1 : 0;
            if (BlockRayTrace.isLookingAtEntity(nearestPoint, endCrystal, 3.0)
               || BlockRayTrace.raycastBlock(LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos, uP)), blockPos, this.getReach()) != null
               || endCrystal.distanceToSqr(mc.player) <= 9.0
               || value != 0
               || this.withinThroughWallRange(endCrystal.blockPosition())) {
               placeDatas.add(new CrystalAuraModule.PlaceData(blockPos, uP, height, CrystalAuraModule.PlaceResult.EXPLOSION, endCrystal));
            }
         }
      }
   }

   private double movementDirection(float value, float value2, float value3) {
      if (value2 < 0.0F) {
         value += 180.0F;
      }

      float computedValue = value2 < 0.0F ? -0.5F : (value2 > 0.0F ? 0.5F : 1.0F);
      if (value3 > 0.0F) {
         value -= 90.0F * computedValue;
      }

      if (value3 < 0.0F) {
         value += 90.0F * computedValue;
      }

      double value4 = value;
      return Math.toRadians(value4);
   }

   private void collectSideCrystalCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      if (mc.level != null) {
         BlockPos blockPos = player.blockPosition();

         for (int index = -2; index <= 2; index++) {
            for (int index2 = -2; index2 <= 2; index2++) {
               if (index != 0 || index2 != 0) {
                  for (int index3 = -2; index3 <= 0; index3++) {
                     BlockPos blockPos2 = blockPos.offset(index, index3, index2);
                     if (this.isCrystalBase(mc.level.getBlockState(blockPos2))
                        && mc.level.getBlockState(blockPos2.above()).isAir()
                        && !this.isBlockedByEntitiesForCrystal(entitys, blockPos2.above(), player)
                        && (BlockRayTrace.canReachBox(new AABB(blockPos2), this.getReach() + 0.5) || this.canPlaceThroughSoftWalls(blockPos2))) {
                        float explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos2.above()), player, false);
                        if (!(explosionDamage < 2.0F) && (BlockRayTrace.canReachBox(new AABB(blockPos2), this.getReach() + 0.5) || this.hasGoodThroughWallDamage(explosionDamage))) {
                           int computedValue = Math.abs(index) + Math.abs(index2);
                           double height = explosionDamage + 90.0 - blockPos2.distSqr(blockPos) * 2.0 - computedValue * 3.0;
                           height -= this.heightPenalty(blockPos2.above(), player);
                           if (this.isCrystalTooHigh(blockPos2, player)) {
                              height -= 35.0;
                           }

                           if (computedValue == 1) {
                              height += 20.0;
                           }

                           if (index3 <= -1) {
                              height += 15.0;
                           }

                           placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2.immutable(), Direction.UP, height, CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void driveToCorrectionPosition(PlayerInputEvent playerInputEvent, BlockPos blockPos) {
      if (mc.player != null && playerInputEvent != null && blockPos != null) {
         if (this.hasReachedCorrectionPosition(blockPos)) {
            playerInputEvent.clearMovement(true, playerInputEvent.keyPresses().shift());
         } else {
            Vec3 position = new Vec3(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
            Vec3 position2 = this.steerAroundObstacles(position);
            double x = position2.x - mc.player.getX();
            double z = position2.z - mc.player.getZ();
            float computedValue = (float)(Math.toDegrees(Math.atan2(z, x)) - 90.0);
            float yRot = mc.player.getYRot();
            float value = 0.0F;
            float value2 = 0.0F;
            float mAX_VALUE = Float.MAX_VALUE;

            for (float index = -1.0F; index <= 1.0F; index++) {
               for (float index2 = -1.0F; index2 <= 1.0F; index2++) {
                  if (index != 0.0F || index2 != 0.0F) {
                     float computedValue2 = (float)Math.toDegrees(this.movementDirection(yRot, index, index2));
                     float computedValue3 = Math.abs(Mth.wrapDegrees(computedValue - computedValue2));
                     if (computedValue3 < mAX_VALUE) {
                        mAX_VALUE = computedValue3;
                        value = index;
                        value2 = index2;
                     }
                  }
               }
            }

            double x2 = Math.hypot(blockPos.getX() + 0.5 - mc.player.getX(), blockPos.getZ() + 0.5 - mc.player.getZ());
            int value3 = x2 > 2.0 ? 1 : 0;
            boolean jump = playerInputEvent.keyPresses().jump();
            playerInputEvent.setMoveVector(new Vec2(value2, value));
            playerInputEvent.setKeyPresses(new Input(value > 0.0F, value < 0.0F, value2 > 0.0F, value2 < 0.0F, jump, false, (value3 != 0)));
         }
      }
   }

   private float getReach() {
      return this.placeRange.get().floatValue();
   }

   private boolean canPlaceCrystalNow() {
      long timestamp = System.currentTimeMillis() - this.lastCrystalPlaceMs;
      return timestamp >= this.getCrystalPlaceDelayMs();
   }

   private boolean isSafeSelfDamage(Player player, CrystalAuraModule.PlaceData placeData) {
      return this.isSelfDamageAllowed(player, this.getSelfDamage(player, placeData));
   }

   private double getMaxSelfDamage() {
      Double value = this.maxSelfDamage.get();
      return value.floatValue();
   }

   private void collectBreakAround(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas, BlockPos blockPos, Set<Long> longs) {
      int scanRadius = this.getScanRadius();
      MutableBlockPos mutablePos = new MutableBlockPos();

      for (int index = -scanRadius; index <= scanRadius; index++) {
         for (int index2 = -scanRadius; index2 <= scanRadius; index2++) {
            for (int index3 = -scanRadius; index3 <= scanRadius; index3++) {
               mutablePos.set(blockPos.getX() + index, blockPos.getY() + index2, blockPos.getZ() + index3);
               if ((longs == null || longs.add(mutablePos.asLong()))
                  && mutablePos.getY() < (int)player.getY()
                  && mc.level.getBlockState(mutablePos).isRedstoneConductor(mc.level, mutablePos)
                  && (!this.isUnbreakable(mutablePos) || !mc.level.getBlockState(mutablePos.above()).isAir())) {
                  Direction closestFace = this.isNewMode() ? BlockFaces.getBestVisibleFace(mutablePos, player2) : BlockFaces.getClosestFace(mutablePos, player2);
                  BlockPos blockPos2 = mutablePos.above();
                  if (!this.isBlockedByEntitiesForBreak(entitys, blockPos2, player)
                     && BlockRayTrace.canReachBox(
                        new AABB(blockPos2.getX() - 0.5, blockPos2.getY(), blockPos2.getZ() - 0.5, blockPos2.getX() + 2.0, blockPos2.getY() + 1.5, blockPos2.getZ() + 1.5),
                        this.getBoxRange()
                     )) {
                     if (!mc.level.getBlockState(blockPos2).isAir()) {
                        if (!this.shouldNotBreak(blockPos2)) {
                           int bestHotbarToolSlot = this.findBestHotbarToolSlot(blockPos2);
                           int bestHotbarToolSlot2 = this.findBestHotbarToolSlot(mutablePos);
                           if (bestHotbarToolSlot != -1 && (!mc.level.getBlockState(mutablePos).is(Blocks.OBSIDIAN) || bestHotbarToolSlot2 != -1)) {
                              double damageIgnoringBlock = VirtualExplosionDamage.getDamageIgnoringBlock(Vec3.atCenterOf(blockPos2), player, blockPos2) + 19.0 - 12.0;
                              if (!(damageIgnoringBlock < this.getMinBreakDamage())) {
                                 placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2.immutable(), closestFace, damageIgnoringBlock, CrystalAuraModule.PlaceResult.BREAK, null));
                              }
                           }
                        }
                     } else if (this.findBestHotbarToolSlot(mutablePos) != -1 && !this.shouldNotBreak(mutablePos)) {
                        double explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos2), player, false) + 19.0 - 12.0;
                        if (!(explosionDamage < this.getMinBreakDamage())) {
                           placeDatas.add(new CrystalAuraModule.PlaceData(mutablePos.immutable(), closestFace, explosionDamage, CrystalAuraModule.PlaceResult.BREAK, null));
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private LivingEntity findTarget() {
      Player player = AutoWebTrapModule.INSTANCE.crystalAuraTarget();
      if (player != null && mc.player.distanceToSqr(player) <= 64.0) {
         return player;
      }

      if (mc.player != null && mc.level != null) {
         Player player2 = null;
         double index = 64.0;

         for (Player player3 : mc.level.players()) {
            if (this.isValidTarget(player3)) {
               double distanceSquared = mc.player.distanceToSqr(player3);
               if (distanceSquared <= index) {
                  index = distanceSquared;
                  player2 = player3;
               }
            }
         }

         for (Player player4 : FakePlayerRegistry.INSTANCE.getFakePlayers()) {
            if (this.isValidTarget(player4)) {
               double distanceSquared2 = mc.player.distanceToSqr(player4);
               if (distanceSquared2 <= index) {
                  index = distanceSquared2;
                  player2 = player4;
               }
            }
         }

         return player2;
      } else {
         return null;
      }
   }

   private void applyRotation() {
      if (this.rotation != null) {
         if (this.rotate.is("Vanilla")) {
            float angle = this.rotationYawSpeed();
            float angle2 = this.rotationPitchSpeed();
            RotationManager.update(this.rotation, angle, angle2, angle, angle2, 100, 999, false);
         }
      }
   }

   private double getTargetDamage(CrystalAuraModule.PlaceData placeData, Player player) {
      if (placeData != null && placeData.blockPos() != null && placeData.face() != null && player != null && mc.level != null) {
         BlockPos blockPos = placeData.blockPos();
         Direction direction = placeData.face();

         return switch (placeData.placeResult()) {
            case PLACE_OBSIDIAN -> {
               BlockPos blockPos2 = blockPos.relative(direction);
               yield VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
            }
            case PLACE_CRYSTAL -> VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos.above()), player, false);
            case EXPLOSION -> placeData.entity() instanceof EndCrystal endCrystal
               ? VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), player, false)
               : 0.0;
            case BREAK -> {
               BlockPos blockState = mc.level.getBlockState(blockPos.above()).isAir() ? blockPos.above() : blockPos;
               if (!mc.level.getBlockState(blockState).isAir() && !this.isCrystalBase(mc.level.getBlockState(blockPos))) {
                  yield VirtualExplosionDamage.getDamageIgnoringBlock(Vec3.atCenterOf(blockState), player, blockState);
               } else {
                  BlockPos blockState2 = mc.level.getBlockState(blockPos.above()).isAir() ? blockPos : blockPos.below();
                  yield VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockState2.above()), player, false);
               }
            }
            default -> 0.0;
         };
      } else {
         return 0.0;
      }
   }

   private boolean canFastCrystalChain() {
      if (!this.isAntiExplosionGoal(this.goalData)) {
         if (!this.isNewMode()) {
            return false;
         }
      }

      return true;
   }

   private int getScanRadius() {
      return this.isNewMode() ? 8 : 5;
   }

   private long getObsidianPlaceDelayMs() {
      return this.isNewMode() ? 100L : 500L;
   }

   private BlockPos getEnemyLookedBlock(Player player, double value) {
      if (mc.level != null && player != null) {
         Vec3 eyePosition = player.getEyePosition();
         Vec3 viewVector = player.getViewVector(1.0F);
         Vec3 position = viewVector.scale(value);
         Vec3 position2 = eyePosition.add(position);
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePosition, position2, Block.OUTLINE, Fluid.NONE, player));
         Type type = blockHitResult.getType();
         return type != Type.BLOCK ? null : blockHitResult.getBlockPos().immutable();
      } else {
         return null;
      }
   }

   private boolean shouldNotExplodeBecauseOfResources(Entity entity) {
      if (this.safety.isSelected("Ресурсы")) {
         if (mc.level != null && entity != null) {
            ClientLevel level = mc.level;
            AABB boundingBox = entity.getBoundingBox();
            AABB bounds = boundingBox.inflate(5.0);
            Predicate<net.minecraft.world.entity.item.ItemEntity> predicate = itemEntity -> {
               return true;
            };
            var stream = level.getEntitiesOfClass(ItemEntity.class, bounds, predicate).stream();
            Function<net.minecraft.world.entity.item.ItemEntity, net.minecraft.world.item.Item> item = itemEntity2 -> {
               ItemStack item2 = itemEntity2.getItem();
               return item2.getItem();
            };
            var stream2 = stream.map(item);
            Predicate<net.minecraft.world.item.Item> predicate2 = item3 -> {
               if (item3 != Items.TOTEM_OF_UNDYING && item3 != Items.ENCHANTED_GOLDEN_APPLE) {
                  if (item3 != Items.NETHERITE_HELMET && item3 != Items.NETHERITE_CHESTPLATE) {
                     if (item3 != Items.NETHERITE_LEGGINGS && item3 != Items.NETHERITE_BOOTS && item3 != Items.NETHERITE_SWORD) {
                        if (item3 != Items.DIAMOND_SWORD) {
                           if (item3 != Items.ELYTRA && item3 != Items.TRIDENT) {
                              return false;
                           }
                        }
                     }
                  }
               }

               return true;
            };
            return stream2.anyMatch(predicate2);
         }
      }

      return false;
   }

   private Vec3 getHighPlaceHit(BlockPos blockPos, Direction direction, BlockPos blockPos2) {
      double x = blockPos.getX() + 0.5;
      double stepX = x + direction.getStepX() * 0.49;
      double z = blockPos.getZ() + 0.5 + direction.getStepZ() * 0.49;
      double value;
      if (direction == Direction.UP) {
         value = blockPos.getY() + 1.0;
      } else {
         if (direction == Direction.DOWN) {
            value = blockPos.getY();
         } else {
            double y = blockPos.getY() + 0.92;
            double y2 = blockPos2.getY() + 0.85;
            value = Math.max(y, y2);
         }
      }

      return new Vec3(stepX, value, z);
   }

   private void collectRecessDigCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         BlockPos blockPos = player.blockPosition();

         for (int index = -2; index <= 2; index++) {
            for (int index2 = -2; index2 <= 2; index2++) {
               if (index != 0 || index2 != 0) {
                  for (int index3 = -2; index3 <= -1; index3++) {
                     BlockPos blockPos2 = blockPos.offset(index, index3, index2);
                     BlockState blockState = mc.level.getBlockState(blockPos2);
                     if (!blockState.isAir()
                        && !blockState.canBeReplaced()
                        && !blockState.is(Blocks.COBWEB)
                        && !this.shouldNotBreak(blockPos2)
                        && this.findBestHotbarToolSlot(blockPos2) != -1) {
                        BlockState blockState2 = mc.level.getBlockState(blockPos2.above());
                        if (blockState2.isAir()) {
                           byte flag = 0;

                           for (Direction direction : Direction.values()) {
                              BlockPos blockPos3 = blockPos2.relative(direction);
                              BlockState blockState3 = mc.level.getBlockState(blockPos3);
                              if (!blockState3.isAir() && !blockState3.canBeReplaced() && !blockState3.is(Blocks.COBWEB) && blockState3.getFluidState().isEmpty()) {
                                 flag = 1;
                                 break;
                              }
                           }

                           if (flag != 0 && (BlockRayTrace.canReachBox(new AABB(blockPos2), this.getReach() + 0.5) || this.withinThroughWallRange(blockPos2))) {
                              float damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
                              if (!(damageOfGhostBlock < 2.0F)) {
                                 Direction closestFace = BlockFaces.getClosestFace(blockPos2, player2);
                                 int computedValue = Math.abs(index) + Math.abs(index2);
                                 double height = damageOfGhostBlock + 45.0 - blockPos2.distSqr(blockPos) - computedValue * 2.0;
                                 height -= this.heightPenalty(blockPos2.above(), player);
                                 if (computedValue == 1) {
                                    height += 20.0;
                                 }

                                 if (index3 == -2) {
                                    height += 10.0;
                                 }

                                 placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2.immutable(), closestFace, height, CrystalAuraModule.PlaceResult.BREAK, null));
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void stopRotation() {
      RotationManager.INSTANCE.stopRotation();
   }

   private boolean isFullCorrectionActive() {
      if (!this.correctionType.is("Полная")) {
         Boolean get = this.rage.get();
         if (!get) {
            return false;
         }
      }

      return true;
   }

   private boolean isGoalLocked() {
      long now = System.currentTimeMillis();
      if (this.breakingBlock != null) {
         return true;
      }

      if (this.isFullCorrectionActive()
         && this.correctionStandPos != null
         && !this.isBreakingThroughBlocks()
         && !this.hasReachedCorrectionPosition(this.correctionStandPos)) {
         return true;
      }

      if (this.pendingObsidianPos != null) {
         if (mc.level == null || !this.isCrystalBase(mc.level.getBlockState(this.pendingObsidianPos))) {
            if (now - this.lastObsidianPlaceMs >= 100L) {
               if (this.goalData != null) {
                  this.markGoalFailed(this.goalData);
               }

               this.pendingObsidianPos = null;
               this.goalData = null;
               this.lastData = null;
               this.clearCrystalPending();
               return false;
            } else {
               return true;
            }
         }

         BlockPos blockPos = this.pendingObsidianPos.immutable();
         this.pendingObsidianPos = null;
         this.promoteObsidianToCrystal(blockPos);
      }

      if (this.goalData != null
         && this.goalData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
         && this.crystalPlaceBase != null
         && this.crystalGoalSinceMs > 0L) {
         BlockPos blockPos2 = this.crystalPlaceBase.above();
         if (this.hasCrystalAt(blockPos2)) {
            return false;
         } else if (now - this.crystalGoalSinceMs >= 100L) {
            this.markGoalFailed(this.goalData);
            this.clearCrystalPending();
            this.goalData = null;
            this.lastData = null;
            return false;
         } else {
            return true;
         }
      } else {
         return this.pendingCrystalBase != null && now - this.lastCrystalInteractMs < 350L;
      }
   }

   private void collectCrystalCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas) {
      HashSet entries = new HashSet();
      BlockPos blockPos = player2.blockPosition();
      this.collectCrystalCandidatesAround(player, player2, entitys, placeDatas, blockPos, entries);
      BlockPos blockPos2 = player.blockPosition();
      BlockPos blockPos3 = player2.blockPosition();
      if (!blockPos2.equals(blockPos3)) {
         this.collectCrystalCandidatesAround(player, player2, entitys, placeDatas, blockPos2, entries);
      }
   }

   private int actionPriority(CrystalAuraModule.PlaceResult placeResult) {
      return switch (placeResult) {
         case PLACE_OBSIDIAN -> 2;
         case PLACE_CRYSTAL -> 1;
         case EXPLOSION -> 0;
         case BREAK -> 3;
         default -> 4;
      };
   }

   private void collectSideObsidianCandidates(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas, boolean condition) {
      if (this.isEnabled() && mc.level != null && mc.player != null) {
         BlockPos blockPos = player.blockPosition();
         int index = condition ? -2 : 0;
         int index2 = condition ? -1 : 0;

         for (int index3 = -2; index3 <= 2; index3++) {
            for (int index4 = -2; index4 <= 2; index4++) {
               if (index3 != 0 || index4 != 0) {
                  for (int index5 = index; index5 <= index2; index5++) {
                     BlockPos blockPos2 = blockPos.offset(index3, index5, index4);
                     if (!blockPos2.equals(blockPos) && !blockPos2.equals(blockPos.above())) {
                        BlockState blockState = mc.level.getBlockState(blockPos2);
                        if (!blockState.is(Blocks.COBWEB) && (blockState.isAir() || blockState.canBeReplaced())) {
                           BlockState blockState2 = mc.level.getBlockState(blockPos2.above());
                           if (blockState2.isAir()) {
                              if (!condition && this.shouldDig()) {
                                 BlockPos blockPos3 = blockPos2.below();
                                 BlockState blockState3 = mc.level.getBlockState(blockPos3);
                                 if (!blockState3.isAir()
                                    && !blockState3.canBeReplaced()
                                    && !blockState3.is(Blocks.COBWEB)
                                    && !this.shouldNotBreak(blockPos3)
                                    && this.findBestHotbarToolSlot(blockPos3) != -1) {
                                    continue;
                                 }
                              }

                              CrystalAuraModule.PlacementHit solidNeighborHit = this.findSolidNeighborHit(blockPos2);
                              if (solidNeighborHit != null && this.isBlockedByEntitiesForObsidian(entitys, blockPos2, player) != CrystalAuraModule.ReturnResult.PLAYER) {
                                 BlockPos blockPos4 = blockPos2.above();
                                 if (this.isBlockedByEntitiesForObsidian(entitys, blockPos4, player) != CrystalAuraModule.ReturnResult.PLAYER
                                    && (BlockRayTrace.canReachBox(new AABB(blockPos2), this.getReach() + 0.5) || this.canPlaceThroughSoftWalls(blockPos2))) {
                                    float damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos4), player, blockPos2);
                                    if (!(damageOfGhostBlock < 2.0F)
                                       && (BlockRayTrace.canReachBox(new AABB(blockPos2), this.getReach() + 0.5) || this.hasGoodThroughWallDamage(damageOfGhostBlock))) {
                                       int computedValue = Math.abs(index3) + Math.abs(index4);
                                       double value = blockPos2.distSqr(blockPos);
                                       double height = damageOfGhostBlock + (condition ? 60.0 : 20.0) - value * 3.0 - computedValue * 4.0;
                                       height -= this.heightPenalty(blockPos4, player);
                                       if (computedValue == 1) {
                                          height += 15.0;
                                       }

                                       if (index5 == -2) {
                                          height += 8.0;
                                       } else if (index5 == -1) {
                                          height += 12.0;
                                       }

                                       placeDatas.add(
                                          new CrystalAuraModule.PlaceData(
                                             solidNeighborHit.neighbor().immutable(), solidNeighborHit.face(), height, CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN, null
                                          )
                                       );
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private double getGoalScore(CrystalAuraModule.PlaceData placeData, Player player, Player player2) {
      if (this.isAntiExplosionGoal(placeData)) {
         return placeData.damage();
      } else {
         double selfDamage = this.getSelfDamage(player, placeData);
         double targetDamage = this.getTargetDamage(placeData, player2);
         return this.isNewMode() ? targetDamage * 3.0 + placeData.damage() - selfDamage * 2.5 : placeData.damage() - selfDamage;
      }
   }

   private boolean shouldDig() {
      if (!this.autoKrot.get()) {
         if (!this.isNewMode()) {
            if (!this.rage.get()) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean isCrystalTooHigh(BlockPos blockPos, Player player) {
      return blockPos != null && player != null ? blockPos.getY() + 1.0 > player.getY() + 0.35 : false;
   }

   private boolean canPlaceThroughSoftWalls(BlockPos blockPos) {
      if (mc.player != null) {
         if (mc.level != null && blockPos != null) {
            double reach = this.getReach() + 0.75;
            Vec3 eyePosition = mc.player.getEyePosition();
            Vec3 position = Vec3.atCenterOf(blockPos);
            if (eyePosition.distanceToSqr(position) > reach * reach) {
               return false;
            }

            return !this.pathBlockedByHardBase(eyePosition, position, blockPos);
         }
      }

      return false;
   }

   public void onUpdate(PlayerTickEndEvent playerTickEndEvent) {
      if (this.isEnabled()
         && net.skycore.client.module.combat.CrystalAuraModule.INSTANCE.isVanillaModeActive()
         && mc.player != null
         && mc.level != null
         && !mc.player.isUsingItem()) {
         AutoWebTrapModule.INSTANCE.runCombatTick();
         LivingEntity target = this.findTarget();
         if (!(target instanceof Player player && target.isAlive())) {
            this.resetCombatState();
         } else if (AutoWebTrapModule.INSTANCE.shouldDeferCrystalAura(List.of(player), true, true, true, mc.player.tickCount, false)) {
            this.rotation = null;
            RotationManager.INSTANCE.stopRotationIfPriority(999);
            this.clearFullCorrection();
            this.restoreSavedHotbarSlot();
            AutoWebTrapModule.INSTANCE.runCombatTick();
         } else {
            BlockPos blockPos = player.blockPosition();
            if (this.lastTargetFeet == null || !this.lastTargetFeet.equals(blockPos)) {
               this.lastTargetFeet = blockPos.immutable();
               this.onTargetMoved();
            }

            int hotbarSlot = UseItemHelper.findHotbarSlot(Items.OBSIDIAN, -1);
            int hotbarSlot2 = UseItemHelper.findHotbarSlot(Items.END_CRYSTAL, this.crystalHotbarSlot);
            if (hotbarSlot2 != -1) {
               this.crystalHotbarSlot = hotbarSlot2;
            }

            if (hotbarSlot != -1 && hotbarSlot2 != -1) {
               LocalPlayer player2 = mc.player;
               var entitys = mc.level.entitiesForRendering();
               CrystalAuraModule.PlaceData placeData = this.selectGoal(player, player2, entitys);
               if (placeData == null && this.rage.get()) {
                  placeData = this.createRageGoal(player);
               }

               if (!this.isGoalLocked()) {
                  if (placeData != null) {
                     if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
                        && (this.crystalPlaceBase == null || !this.crystalPlaceBase.equals(placeData.blockPos()))) {
                        this.crystalPlaceBase = placeData.blockPos().immutable();
                        this.crystalGoalSinceMs = System.currentTimeMillis();
                        this.pendingCrystalBase = null;
                     }

                     this.goalData = placeData;
                  } else {
                     this.goalData = null;
                     this.clearCrystalPending();
                  }
               } else if (placeData != null
                  && placeData.placeResult() == CrystalAuraModule.PlaceResult.EXPLOSION
                  && (this.goalData == null || this.goalData.placeResult() != CrystalAuraModule.PlaceResult.EXPLOSION)) {
                  this.clearCrystalPending();
                  this.pendingObsidianPos = null;
                  this.goalData = placeData;
               }

               if (this.goalData != null
                  && !this.isAntiExplosionGoal(this.goalData)
                  && !this.isFullCorrectionActive()
                  && !this.isSafeSelfDamage(player2, this.goalData)) {
                  this.markGoalFailed(this.goalData);
                  this.goalData = null;
                  this.clearCrystalPending();
                  this.pendingObsidianPos = null;
                  this.stopBreaking();
               }

               this.tickFailedGoals();
               CrystalAuraModule.PlaceData goalData2 = this.goalData;
               if (goalData2 == null || goalData2.placeResult() == CrystalAuraModule.PlaceResult.AWAITING) {
                  this.lastData = null;
                  this.rotation = null;
                  this.clearFullCorrection();
                  this.stopBreaking();
                  this.restoreSavedHotbarSlot();
               } else if (goalData2.damage() == 0.0) {
                  this.goalData = null;
                  this.lastData = null;
                  this.rotation = null;
                  this.clearFullCorrection();
                  this.stopBreaking();
                  this.restoreSavedHotbarSlot();
               } else {
                  this.lastData = goalData2;
                  this.requestWebIgnore();
                  if (this.isFullCorrectionActive()) {
                     if (this.isBreakingThroughBlocks()) {
                        this.correctionStandPos = null;
                     } else if (!this.prepareFullCorrection(goalData2, player)) {
                        return;
                     }
                  } else {
                     this.clearFullCorrection();
                  }

                  if (goalData2.placeResult() == CrystalAuraModule.PlaceResult.EXPLOSION) {
                     this.handleExplosion();
                  } else {
                     CrystalAuraModule.PlaceData placeData2 = this.resolveExecutableAction(goalData2);
                     if (placeData2 == null) {
                        placeData2 = this.recoverStuckGoal(goalData2);
                     }

                     if (placeData2 == null) {
                        int value = this.isNewMode()
                              && this.shouldDig()
                              && (
                                 goalData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN
                                    || goalData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
                              )
                              && this.findFirstBreakableOnPath(goalData2.blockPos(), goalData2.face()) != null
                           ? 1
                           : 0;
                        if (value != 0
                           || goalData2.placeResult() != CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN
                              && goalData2.placeResult() != CrystalAuraModule.PlaceResult.PLACE_CRYSTAL) {
                           this.updateGoalRotation(goalData2);
                           this.applyRotation();
                        } else if (goalData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL && !this.canPlaceCrystalNow()) {
                           this.updateGoalRotation(goalData2);
                           this.applyRotation();
                        } else {
                           this.goalData = null;
                           this.lastData = null;
                           this.clearCrystalPending();
                           this.rotation = null;
                           this.clearFullCorrection();
                           this.stopBreaking();
                        }
                     } else {
                        this.lastData = placeData2;
                        BlockPos blockPos2 = placeData2.blockPos();
                        Direction direction = placeData2.face();
                        if (blockPos2 != null && direction != null) {
                           Rotation facePoint = LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos2, direction));
                           if (placeData2.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
                              int reach = this.rotate.is("Без ротации") && BlockRayTrace.raycastBlock(blockPos2, this.getReach()) == null ? 0 : 1;
                              if (reach != 0) {
                                 this.rotation = facePoint;
                                 this.applyRotation();
                                 this.handleBreak(blockPos2, direction);
                              } else {
                                 this.rotation = facePoint;
                                 this.applyRotation();
                              }
                           } else {
                              Vec3 position;
                              if (placeData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL) {
                                 position = new Vec3(blockPos2.getX() + 0.5, blockPos2.getY() + 1.35, blockPos2.getZ() + 0.5);
                                 direction = Direction.UP;
                              } else if (placeData2.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
                                 BlockPos blockPos3 = blockPos2.relative(direction);
                                 position = this.getHighPlaceHit(blockPos2, direction, blockPos3);
                              } else {
                                 position = BlockFaces.getFacePoint(blockPos2, direction);
                              }

                              Rotation eyePosition = Rotation.lookingAt(position, mc.player.getEyePosition());
                              this.rotation = eyePosition;
                              this.applyRotation();
                              BlockHitResult blockHitResult = new BlockHitResult(position, direction, blockPos2, false);
                              this.handlePlacement(blockPos2, direction, eyePosition, blockHitResult);
                           }

                           this.applyRotation();
                        } else {
                           this.rotation = null;
                        }
                     }
                  }
               }
            } else {
               this.lastData = null;
               this.rotation = null;
               this.clearFullCorrection();
               this.restoreSavedHotbarSlot();
            }
         }
      }
   }

   private float rotationYawSpeed() {
      return this.hasMovementInput() ? 55.0F : 180.0F;
   }

   private boolean isCrystalBase(BlockState blockState) {
      if (!blockState.is(Blocks.OBSIDIAN)) {
         if (!blockState.is(Blocks.BEDROCK)) {
            return false;
         }
      }

      return true;
   }

   private CrystalAuraModule() {
      super("Crystal Aura", "", ModuleCategory.COMBAT);
      this.correctionType = new ModeSetting("Коррекция движения", "Свободная", "Свободная", "Сфокусированная", "Полная", "Нет");
      this.crystalsPerSecond = new SliderSetting("Кристаллов/сек", 8.0, 1.0, 20.0, 0.5);
      this.placeRange = new SliderSetting("Дистанция постановки", 5.5, 3.0, 6.0, 0.1F);
      this.maxSelfDamage = new SliderSetting("Макс. урон себе", 6.0, 1.0, 20.0, 0.5);
      this.minHealth = new SliderSetting("Мин. HP", 4.0, 1.0, 20.0, 0.5);
      this.autoKrot = new BooleanSetting("Делать подкоп", true);
      this.rage = new BooleanSetting("Rage", false);
      this.antiExplosion = new BooleanSetting("AntiExplosion", false);
      this.safety = new MultiSelectSetting("Не взрывать", List.of("Друзей", "Ресурсы"), Set.of("Друзей", "Ресурсы"));
      this.savedHotbarSlot = -1;
      this.crystalHotbarSlot = -1;
      this.failedGoals = new HashMap<>();
      this.visualDatas = new ConcurrentLinkedQueue<>();
      this.add(this.mode);
      this.add(this.rotate);
      this.add(this.correctionType);
      this.add(this.crystalsPerSecond);
      this.add(this.placeRange);
      this.add(this.maxSelfDamage);
      this.add(this.minHealth);
      this.add(this.autoKrot);
      this.add(this.rage);
      this.add(this.antiExplosion);
      this.add(this.safety);
      this.autoKrot.visibleWhen(() -> {
         if (!this.isNewMode()) {
            Boolean get = this.rage.get();
            if (!get) {
               return true;
            }
         }

         return false;
      });
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, this::onUpdate);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onMoveInput);
   }

   public boolean ownsWebTrap() {
      return false;
   }

   private BlockPos getCorrectionBase(CrystalAuraModule.PlaceData placeData, Player player) {
      int value = 0;
      if (placeData.blockPos() == null) {
         return null;
      }

      return switch (placeData.placeResult()) {
         case PLACE_OBSIDIAN -> {
            if (placeData.face() != null) {
               BlockPos blockPos = placeData.blockPos();
               yield blockPos.relative(placeData.face());
            } else {
               yield null;
            }
         }
         case PLACE_CRYSTAL, EXPLOSION -> {
            yield placeData.blockPos();
         }
         case BREAK -> {
            BlockPos blockPos2 = placeData.blockPos();
            value = blockPos2.getY();
            yield value > player.blockPosition().getY() ? blockPos2.below() : blockPos2;
         }
         default -> null;
      };
   }

   private void collectCrystalCandidatesAround(
      Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas, BlockPos blockPos, Set<Long> longs
   ) {
      int scanRadius = this.getScanRadius();
      MutableBlockPos mutablePos = new MutableBlockPos();

      for (int index = -scanRadius; index <= scanRadius; index++) {
         int x = blockPos.getX() + index;

         for (int index2 = -scanRadius; index2 <= scanRadius; index2++) {
            int y = blockPos.getY() + index2;

            for (int index3 = -scanRadius; index3 <= scanRadius; index3++) {
               mutablePos.set(x, y, blockPos.getZ() + index3);
               if (longs.add(mutablePos.asLong()) && mutablePos.getY() < (int)player.getY()) {
                  BlockState blockState = mc.level.getBlockState(mutablePos);
                  if (this.isCrystalBase(blockState)) {
                     BlockPos blockPos2 = mutablePos.above();
                     BlockState blockState2 = mc.level.getBlockState(blockPos2);
                     if (!blockState2.isAir()) {
                        if (this.isProtectedVegetation(blockPos2)) {
                        }
                     } else {
                        Direction closestFace = this.isNewMode() ? BlockFaces.getBestVisibleFace(mutablePos, player2) : BlockFaces.getClosestFace(mutablePos, player2);
                        if (BlockRayTrace.raycastBlock(LookHelper.calculateAngle(BlockFaces.getFacePoint(mutablePos, Direction.UP)), mutablePos, this.getReach()) != null) {
                           closestFace = Direction.UP;
                        }

                        if (!this.isBlockedByEntitiesForCrystal(entitys, blockPos2, player)
                           && BlockRayTrace.canReachBox(
                              new AABB(blockPos2.getX() - 0.5, blockPos2.getY(), blockPos2.getZ() - 0.5, blockPos2.getX() + 2.0, blockPos2.getY() + 1.5, blockPos2.getZ() + 1.5),
                              this.getBoxRange()
                           )) {
                           double explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos2), player, false) + 19.0;
                           if (!(explosionDamage < this.getMinCrystalDamage())) {
                              BlockPos blockPos3 = player.blockPosition();
                              if (mutablePos.equals(blockPos3.below()) || mutablePos.distManhattan(blockPos3.below()) <= 2) {
                                 explosionDamage += 12.0;
                              }

                              placeDatas.add(new CrystalAuraModule.PlaceData(mutablePos.immutable(), closestFace, explosionDamage, CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null));
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public BlockPos getBreakPos() {
      if (this.breakingBlock != null) {
         return this.breakingBlock;
      }

      BlockPos blockPos;
      if (this.lastData != null && this.lastData.placeResult() == CrystalAuraModule.PlaceResult.BREAK) {
         blockPos = this.lastData.blockPos();
      } else {
         blockPos = null;
      }

      return blockPos;
   }

   private int findBestHotbarToolSlot(BlockPos blockPos) {
      if (mc.player != null && mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (this.shouldNotBreak(blockPos)) {
            return -1;
         }

         if (this.isCobweb(blockPos)) {
            int selectedSlot = mc.player.getInventory().getSelectedSlot();
            float destroySpeed = mc.player.getInventory().getItem(selectedSlot).getDestroySpeed(blockState);

            for (int value = 0; value < 9; value++) {
               ItemStack item = mc.player.getInventory().getItem(value);
               if (!item.isEmpty()) {
                  float destroySpeed2 = item.getDestroySpeed(blockState);
                  if (destroySpeed2 >= destroySpeed) {
                     destroySpeed = destroySpeed2;
                     selectedSlot = value;
                  }
               }
            }

            return selectedSlot;
         } else {
            int index = -1;
            float index2 = 1.0F;

            for (int value2 = 0; value2 < 9; value2++) {
               ItemStack item2 = mc.player.getInventory().getItem(value2);
               if (!item2.isEmpty()) {
                  float destroySpeed3 = item2.getDestroySpeed(blockState);
                  if (destroySpeed3 > index2) {
                     index2 = destroySpeed3;
                     index = value2;
                  }
               }
            }

            return index;
         }
      } else {
         return -1;
      }
   }

   private boolean canUseCorrectionStand(BlockPos blockPos, Player player) {
      if (mc.level != null && blockPos != null && player != null) {
         AABB bounds = new AABB(blockPos.getX() + 0.05, blockPos.getY(), blockPos.getZ() + 0.05, blockPos.getX() + 0.95, blockPos.getY() + 1.85, blockPos.getZ() + 0.95);
         if (bounds.intersects(player.getBoundingBox())) {
            return false;
         }

         BlockPos blockPos2 = blockPos.below();
         BlockState blockState = mc.level.getBlockState(blockPos2);
         int collisionShape = !blockState.isAir() && !blockState.canBeReplaced() && !blockState.getCollisionShape(mc.level, blockPos2).isEmpty() ? 1 : 0;
         return collisionShape == 0 ? false : this.canClearCorrectionCell(blockPos) && this.canClearCorrectionCell(blockPos.above());
      } else {
         return false;
      }
   }

   private void restoreSavedHotbarSlot() {
      int value = 0;
      if (this.savedHotbarSlot != -1) {
         if (mc.player != null) {
            value = mc.player.getInventory().getSelectedSlot();
            if (value != this.savedHotbarSlot) {
               UseItemHelper.switchToHotbarSlot(this.savedHotbarSlot);
            }

            this.savedHotbarSlot = -1;
            return;
         }
      }

      this.savedHotbarSlot = -1;
   }

   private float rotationPitchSpeed() {
      return this.hasMovementInput() ? 35.0F : 180.0F;
   }

   private void updateGoalRotation(CrystalAuraModule.PlaceData placeData) {
      if (placeData != null && placeData.blockPos() != null && placeData.face() != null) {
         if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL) {
            BlockPos blockPos = placeData.blockPos();
            this.rotation = Rotation.lookingAt(new Vec3(blockPos.getX() + 0.5, blockPos.getY() + 1.0, blockPos.getZ() + 0.5), mc.player.getEyePosition());
         } else if (placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
            BlockPos blockPos2 = placeData.blockPos().relative(placeData.face());
            this.rotation = Rotation.lookingAt(this.getHighPlaceHit(placeData.blockPos(), placeData.face(), blockPos2), mc.player.getEyePosition());
         } else {
            this.rotation = LookHelper.calculateAngle(BlockFaces.getFacePoint(placeData.blockPos(), placeData.face()));
         }
      } else {
         this.rotation = null;
      }
   }

   private void markGoalFailed(CrystalAuraModule.PlaceData placeData) {
      long goalKey = this.getGoalKey(placeData);
      if (goalKey != 0L) {
         var failedGoals2 = this.failedGoals;
         Long element = goalKey;
         failedGoals2.put(element, 40);
      }
   }

   private BlockPos findFirstBreakableOnPath(BlockPos blockPos, Direction direction) {
      if (mc.player != null && mc.level != null && blockPos != null && direction != null) {
         this.requestWebIgnore();
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 facePoint = BlockFaces.getFacePoint(blockPos, direction);
         BlockPos blockPos2 = blockPos.relative(direction);
         int value = this.isNewMode() ? 8 : 4;
         Vec3 position = eyePosition;

         for (int value2 = 0; value2 < value; value2++) {
            BlockHitResult blockHitResult = mc.level.clip(new ClipContext(position, facePoint, Block.OUTLINE, Fluid.NONE, mc.player));
            if (blockHitResult.getType() != Type.BLOCK) {
               return null;
            }

            BlockPos blockPos3 = blockHitResult.getBlockPos();
            if (blockPos3.equals(blockPos) || blockPos3.equals(blockPos2)) {
               return null;
            }

            if (!this.isCobweb(blockPos3)) {
               if (!this.shouldNotBreak(blockPos3) && this.findBestHotbarToolSlot(blockPos3) != -1) {
                  return blockPos3;
               }

               return null;
            }

            position = blockHitResult.getLocation().add(facePoint.subtract(eyePosition).normalize().scale(0.05));
         }

         return null;
      } else {
         return null;
      }
   }

   private double getSelfDamage(Player player, CrystalAuraModule.PlaceData placeData) {
      if (placeData != null && placeData.blockPos() != null && placeData.face() != null && player != null && mc.level != null) {
         BlockPos blockPos = placeData.blockPos();
         Direction direction = placeData.face();

         return switch (placeData.placeResult()) {
            case PLACE_OBSIDIAN -> {
               BlockPos blockPos2 = blockPos.relative(direction);
               yield VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos2.above()), player, blockPos2);
            }
            case PLACE_CRYSTAL -> VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos.above()), player, false);
            case EXPLOSION -> placeData.entity() instanceof EndCrystal endCrystal
               ? VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), player, false)
               : 0.0;
            case BREAK -> {
               BlockPos blockState = mc.level.getBlockState(blockPos.above()).isAir() ? blockPos.above() : blockPos;
               if (!mc.level.getBlockState(blockState).isAir() && !this.isCrystalBase(mc.level.getBlockState(blockPos))) {
                  yield VirtualExplosionDamage.getDamageIgnoringBlock(Vec3.atCenterOf(blockState), player, blockState);
               } else {
                  BlockPos blockState2 = mc.level.getBlockState(blockPos.above()).isAir() ? blockPos : blockPos.below();
                  yield VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockState2.above()), player, false);
               }
            }
            default -> 0.0;
         };
      } else {
         return 0.0;
      }
   }

   private boolean isGoalExecutable(CrystalAuraModule.PlaceData placeData, Player player) {
      if (placeData == null || placeData.damage() <= 0.0) {
         return false;
      }

      if (placeData.placeResult() == CrystalAuraModule.PlaceResult.EXPLOSION) {
         return placeData.entity() != null && placeData.entity().isAlive();
      }

      if (!this.isPlacementStateValid(placeData)) {
         return false;
      }

      if (this.isAntiExplosionGoal(placeData) && placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN) {
         BlockPos blockPos = placeData.blockPos().relative(placeData.face());
         if (!mc.level.getBlockState(blockPos).isAir() && this.isActivelyBrokenByEnemy(blockPos)) {
            return true;
         }
      }

      return this.resolveExecutableAction(placeData) != null;
   }

   private CrystalAuraModule.PlaceData createRageGoal(Player player) {
      if (this.rage.get() && mc.player != null && mc.level != null && player != null) {
         Direction[] directionValues = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
         CrystalAuraModule.PlaceData placeData = null;
         double damage = -Double.MAX_VALUE;
         BlockPos blockPos = player.blockPosition();

         for (int value = 1; value <= 2; value++) {
            for (Direction direction : directionValues) {
               BlockPos blockPos2 = blockPos.relative(direction).below(value);
               BlockPos blockPos3 = blockPos2.above();
               if (!new AABB(blockPos3).intersects(player.getBoundingBox())) {
                  double computedValue = 120.0 - mc.player.distanceToSqr(Vec3.atCenterOf(blockPos2)) * 0.5 - (value - 1) * 8.0;
                  EndCrystal crystalAt = this.findCrystalAt(blockPos3);
                  CrystalAuraModule.PlaceData placeData2 = null;
                  if (crystalAt != null && crystalAt.isAlive() && !this.shouldNotExplodeCrystal(crystalAt)) {
                     placeData2 = new CrystalAuraModule.PlaceData(blockPos2.immutable(), Direction.UP, computedValue + 80.0, CrystalAuraModule.PlaceResult.EXPLOSION, crystalAt);
                  } else {
                     BlockState blockState = mc.level.getBlockState(blockPos2);
                     BlockState blockState2 = mc.level.getBlockState(blockPos3);
                     if (!blockState2.isAir() && !blockState2.canBeReplaced() && !blockState2.is(Blocks.COBWEB)) {
                        if (!this.shouldNotBreak(blockPos3) && this.findBestHotbarToolSlot(blockPos3) != -1) {
                           Direction closestFace = BlockFaces.getClosestFace(blockPos3, mc.player);
                           placeData2 = new CrystalAuraModule.PlaceData(blockPos3.immutable(), closestFace, computedValue, CrystalAuraModule.PlaceResult.BREAK, null);
                        }
                     } else if (this.isCrystalBase(blockState)) {
                        placeData2 = new CrystalAuraModule.PlaceData(
                           blockPos2.immutable(), Direction.UP, computedValue + 45.0, CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null
                        );
                     } else if (blockState.isAir() || blockState.canBeReplaced() && !blockState.is(Blocks.COBWEB)) {
                        CrystalAuraModule.PlacementHit solidNeighborHit = this.findSolidNeighborHit(blockPos2);
                        if (solidNeighborHit != null) {
                           placeData2 = new CrystalAuraModule.PlaceData(
                              solidNeighborHit.neighbor().immutable(), solidNeighborHit.face(), computedValue + 20.0, CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN, null
                           );
                        }
                     } else if (!this.shouldNotBreak(blockPos2) && this.findBestHotbarToolSlot(blockPos2) != -1) {
                        Direction closestFace2 = BlockFaces.getClosestFace(blockPos2, mc.player);
                        placeData2 = new CrystalAuraModule.PlaceData(blockPos2.immutable(), closestFace2, computedValue, CrystalAuraModule.PlaceResult.BREAK, null);
                     }
                  }

                  if (placeData2 != null && placeData2.damage() > damage) {
                     placeData = placeData2;
                     damage = placeData2.damage();
                  }
               }
            }
         }

         return placeData;
      } else {
         return null;
      }
   }

   private BlockPos findCorrectionObstruction(BlockPos blockPos, Player player) {
      if (mc.player != null && mc.level != null) {
         BlockPos[] blockPosValues = new BlockPos[]{blockPos, blockPos.above()};

         for (BlockPos blockPos2 : blockPosValues) {
            if (this.isCorrectionBreakable(blockPos2, player) && this.isCorrectionBlockInReach(blockPos2)) {
               return blockPos2.immutable();
            }
         }

         Vec3 position = mc.player.position().add(0.0, 0.55, 0.0);
         Vec3 position2 = Vec3.atLowerCornerWithOffset(blockPos, 0.5, 0.55, 0.5);
         BlockPos blockPos3 = this.traceCorrectionObstruction(position, position2, player);
         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 position3 = Vec3.atLowerCornerWithOffset(blockPos, 0.5, 1.55, 0.5);
         BlockPos blockPos4 = this.traceCorrectionObstruction(eyePosition, position3, player);
         if (blockPos3 == null) {
            return blockPos4;
         } else if (blockPos4 == null) {
            return blockPos3;
         } else {
            return mc.player.distanceToSqr(Vec3.atCenterOf(blockPos3)) <= mc.player.distanceToSqr(Vec3.atCenterOf(blockPos4)) ? blockPos3 : blockPos4;
         }
      } else {
         return null;
      }
   }

   private double getMinObsidianDamage() {
      return this.isNewMode() ? 16.0 : 25.0;
   }

   private boolean isValidTarget(Player player) {
      if (player != null) {
         if (player != mc.player && !player.isRemoved() && player.isAlive() && !this.isFriend(player)) {
            return true;
         }
      }

      return false;
   }

   private boolean collectObsidianAround(Player player, Player player2, Iterable<Entity> entitys, List<CrystalAuraModule.PlaceData> placeDatas, BlockPos blockPos, Set<Long> longs) {
      int scanRadius = this.getScanRadius();
      MutableBlockPos mutablePos = new MutableBlockPos();
      byte flag = 0;

      for (int index = -scanRadius; index <= scanRadius; index++) {
         for (int index2 = -scanRadius; index2 <= scanRadius; index2++) {
            for (int index3 = -scanRadius; index3 <= scanRadius; index3++) {
               mutablePos.set(blockPos.getX() + index, blockPos.getY() + index2, blockPos.getZ() + index3);
               if (longs == null || longs.add(mutablePos.asLong())) {
                  BlockState blockState = mc.level.getBlockState(mutablePos);
                  if (blockState.isRedstoneConductor(mc.level, mutablePos)) {
                     Direction bestFaceToObsidian = this.isNewMode() ? BlockFaces.getBestVisibleFaceToObsidian(mutablePos, player) : BlockFaces.getBestFaceToObsidian(mutablePos, player);
                     if (bestFaceToObsidian != null) {
                        BlockPos blockPos2 = mutablePos.relative(bestFaceToObsidian);
                        if (blockPos2.getY() < (int)player.getY()) {
                           BlockPos blockPos3 = blockPos2.above();
                           if (mc.level.getBlockState(blockPos2).isAir()
                              && this.isBlockedByEntitiesForObsidian(entitys, blockPos2, player) != CrystalAuraModule.ReturnResult.PLAYER
                              && this.isBlockedByEntitiesForObsidian(entitys, blockPos3, player) != CrystalAuraModule.ReturnResult.PLAYER
                              && BlockRayTrace.canReachBox(
                                 new AABB(blockPos2.getX() - 0.5, blockPos2.getY(), blockPos2.getZ() - 0.5, blockPos2.getX() + 2.0, blockPos2.getY() + 1.5, blockPos2.getZ() + 1.5),
                                 this.getBoxRange()
                              )) {
                              double damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos3), player, blockPos2) + 19.0 - 6.0;
                              if (!(damageOfGhostBlock < this.getMinObsidianDamage())) {
                                 if (this.isBlockedByEntitiesForObsidian(entitys, blockPos3, player) == CrystalAuraModule.ReturnResult.ITEM) {
                                    flag = 1;
                                 } else {
                                    placeDatas.add(
                                       new CrystalAuraModule.PlaceData(mutablePos.immutable(), bestFaceToObsidian, damageOfGhostBlock, CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN, null)
                                    );
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }

      return (flag != 0);
   }

   private void handlePlacement(BlockPos blockPos, Direction direction, Rotation rotation2, BlockHitResult blockHitResult) {
      if (mc.player != null) {
         if (mc.level != null) {
            if (mc.gameMode != null && this.goalData != null) {
               CrystalAuraModule.PlaceResult placeResult = this.goalData.placeResult();
               if (placeResult == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL) {
                  this.handleCrystalPlacement(blockPos);
                  return;
               }

               this.handleObsidianPlacement(blockPos, direction, blockHitResult);
               return;
            }
         }
      }
   }

   private boolean isPlacementStateValid(CrystalAuraModule.PlaceData placeData) {
      BlockPos blockPos = placeData.blockPos();
      Direction direction = placeData.face();
      if (blockPos != null && direction != null) {
         return (switch (placeData.placeResult()) {
            case PLACE_OBSIDIAN -> {
               BlockPos blockPos2 = blockPos.relative(direction);
               BlockState blockState = mc.level.getBlockState(blockPos2);
               yield !blockState.isAir() && (!blockState.canBeReplaced() || blockState.is(Blocks.COBWEB))
                  ? (this.isAntiExplosionGoal(placeData) && this.isActivelyBrokenByEnemy(blockPos2) ? 1 : 0)
                  : 1;
            }
            case PLACE_CRYSTAL -> this.isCrystalBase(mc.level.getBlockState(blockPos)) && mc.level.getBlockState(blockPos.above()).isAir() ? 1 : 0;
            default -> 1;
            case BREAK -> !this.shouldNotBreak(blockPos) && this.findBestHotbarToolSlot(blockPos) != -1 ? 1 : 0;
         }) != 0;
      } else {
         return false;
      }
   }

   private boolean isActivelyBrokenByEnemy(BlockPos blockPos) {
      if (mc.level != null && mc.levelRenderer != null) {
         if (mc.level instanceof ClientLevelGetDestroyingBlocksAccessor2 clientLevelGetDestroyingBlocksAccessor2) {
            for (BlockDestructionProgress blockDestructionProgress : clientLevelGetDestroyingBlocksAccessor2.skycore$getDestroyingBlocks().values()) {
               if (blockDestructionProgress.getPos().equals(blockPos) && mc.level.getEntity(blockDestructionProgress.getId()) instanceof Player player && player != mc.player && !this.isFriend(player)) {
                  return true;
               }
            }

            return false;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private boolean shouldNotBreak(BlockPos blockPos) {
      if (!this.isUnbreakable(blockPos)) {
         if (!this.isProtectedVegetation(blockPos)) {
            if (!this.isCobweb(blockPos)) {
               return false;
            }
         }
      }

      return true;
   }

   private boolean pathBlockedByHardBase(Vec3 position, Vec3 position2, BlockPos blockPos) {
      Vec3 position3 = position;
      Vec3 position4 = position2.subtract(position);
      double length = position4.length();
      if (length < 1.0E-6) {
         return false;
      }

      Vec3 position5 = position4.normalize().scale(0.08);
      int value = 0;

      while (value < 16) {
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(position3, position2, Block.COLLIDER, Fluid.NONE, mc.player));
         if (blockHitResult.getType() != Type.BLOCK) {
            return false;
         }

         BlockPos blockPos2 = blockHitResult.getBlockPos();
         if (!blockPos2.equals(blockPos) && !blockPos2.equals(blockPos.above()) && !blockPos2.equals(blockPos.below()) && !blockPos2.closerThan(blockPos, 1.1)) {
            BlockState blockState = mc.level.getBlockState(blockPos2);
            if (!this.isCrystalBase(blockState) && !this.isUnbreakable(blockPos2)) {
               position3 = blockHitResult.getLocation().add(position5);
               if (position3.distanceToSqr(position2) < 1.0E-4) {
                  return false;
               }

               value++;
               continue;
            }

            return true;
         }

         return false;
      }

      return false;
   }

   private boolean isProtectedVegetation(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (!blockState.is(BlockTags.FLOWERS)) {
            if (!blockState.is(Blocks.SHORT_GRASS)) {
               if (!blockState.is(Blocks.TALL_GRASS)) {
                  if (!blockState.is(Blocks.FERN)) {
                     if (!blockState.is(Blocks.LARGE_FERN)) {
                        if (!blockState.is(Blocks.SUNFLOWER)) {
                           if (!blockState.is(Blocks.LILAC)) {
                              if (!blockState.is(Blocks.ROSE_BUSH)) {
                                 if (!blockState.is(Blocks.PEONY)) {
                                    return false;
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean hasCrystalAt(BlockPos blockPos) {
      return this.findCrystalAt(blockPos) != null;
   }

   private CrystalAuraModule.PlaceData recoverStuckGoal(CrystalAuraModule.PlaceData placeData) {
      if (placeData != null && placeData.blockPos() != null && mc.player != null) {
         BlockPos blockPos = placeData.blockPos();
         float reach = this.getReach();
         Direction closestFace = BlockFaces.getClosestFace(blockPos, mc.player);
         Rotation facePoint = LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos, closestFace));
         if (BlockRayTrace.raycastBlock(facePoint, blockPos, reach) != null || this.canExecutePlaceThroughWalls(placeData)) {
            return new CrystalAuraModule.PlaceData(blockPos.immutable(), closestFace, placeData.damage(), placeData.placeResult(), placeData.entity());
         }

         if (!this.shouldDig()) {
            return null;
         }

         if (this.canExecutePlaceThroughWalls(placeData)) {
            return placeData.placeResult() == CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
               ? new CrystalAuraModule.PlaceData(blockPos.immutable(), Direction.UP, placeData.damage(), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null)
               : new CrystalAuraModule.PlaceData(blockPos.immutable(), closestFace, placeData.damage(), placeData.placeResult(), placeData.entity());
         }

         CrystalAuraModule.PlaceData placeData2 = this.resolveObstructionBreak(blockPos, closestFace, placeData.damage());
         if (placeData2 != null) {
            return placeData2;
         }

         BlockPos firstBreakableOnPath = this.findFirstBreakableOnPath(blockPos, closestFace);
         if (firstBreakableOnPath == null) {
            return null;
         }

         BlockHitResult obstruction = BlockRayTrace.findObstruction(blockPos, closestFace);
         Direction closestFace2 = obstruction != null && obstruction.getBlockPos().equals(firstBreakableOnPath) ? obstruction.getDirection() : BlockFaces.getClosestFace(firstBreakableOnPath, mc.player);
         return new CrystalAuraModule.PlaceData(firstBreakableOnPath.immutable(), closestFace2, placeData.damage(), CrystalAuraModule.PlaceResult.BREAK, null);
      } else {
         return null;
      }
   }

   private void setRotation(Rotation rotation2) {
      if (this.rotate.is("Vanilla")) {
         float angle = this.rotationYawSpeed();
         float angle2 = this.rotationPitchSpeed();
         RotationManager.update(rotation2, angle, angle2, angle, angle2, 100, 999, false);
      }
   }

   private void tickFailedGoals() {
      var entries = this.failedGoals.entrySet();
      Predicate<java.util.Map.Entry> value = entry -> {
         int value2 = (Integer)entry.getValue() - 1;
         if (value2 <= 0) {
            return true;
         }

         entry.setValue(value2);
         return false;
      };
      entries.removeIf(value);
   }

   private boolean isCobweb(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         if (mc.level.getBlockState(blockPos).is(Blocks.COBWEB)) {
            return true;
         }
      }

      return false;
   }

   private void clearCrystalPending() {
      this.crystalPlaceBase = null;
      this.crystalPlaceBlocked = false;
      this.pendingCrystalBase = null;
      this.crystalGoalSinceMs = 0L;
   }

   private boolean isFriend(Player player) {
      FriendManager iNSTANCE = FriendManager.INSTANCE;
      GameProfile gameProfile = player.getGameProfile();
      return iNSTANCE.isFriend(gameProfile.name());
   }

   private double getMinCrystalDamage() {
      return this.isNewMode() ? 22.0 : 31.0;
   }

   private boolean isCorrectionBlockInReach(BlockPos blockPos) {
      double reach = this.getReach() + 0.5;
      Vec3 eyePosition = mc.player.getEyePosition();
      Vec3 position = Vec3.atCenterOf(blockPos);
      return eyePosition.distanceToSqr(position) <= reach * reach;
   }

   private void addAntiExplosionCandidate(BlockPos blockPos, Player player, Player player2, Player player3, List<CrystalAuraModule.PlaceData> placeDatas, Set<Long> longs) {
      if (longs.add(blockPos.asLong())) {
         if (blockPos.closerThan(player3.blockPosition(), 6.5)) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            BlockPos blockPos2 = blockPos.below();
            if (!this.isCrystalBase(mc.level.getBlockState(blockPos2))) {
               if (this.isEnemyBreakingThreat(blockPos, player, player3)) {
                  if (mc.level.getBlockState(blockPos2).isRedstoneConductor(mc.level, blockPos2)) {
                     Direction uP = Direction.UP;
                     BlockPos blockPos3 = blockPos2.relative(uP);
                     Rotation facePoint = LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos2, uP));
                     if (BlockRayTrace.raycastBlock(facePoint, blockPos2, this.getReach()) != null || this.isActivelyBrokenByEnemy(blockPos3)) {
                        double damageOfGhostBlock = VirtualExplosionDamage.getDamageOfGhostBlock(Vec3.atCenterOf(blockPos3.above()), player2, blockPos3);
                        placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2.immutable(), uP, 0.0 + damageOfGhostBlock, CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN, null));
                     }
                  }
               }
            } else {
               if (!this.hasCrystalAt(blockPos) && (blockState.isAir() || this.isActivelyBrokenByEnemy(blockPos))) {
                  double explosionDamage = VirtualExplosionDamage.getExplosionDamage(Vec3.atCenterOf(blockPos), player2, false);
                  if (explosionDamage < 8.0) {
                     return;
                  }

                  Direction uP2 = Direction.UP;
                  if (BlockRayTrace.raycastBlock(LookHelper.calculateAngle(BlockFaces.getFacePoint(blockPos2, uP2)), blockPos2, this.getReach()) == null) {
                     return;
                  }

                  placeDatas.add(new CrystalAuraModule.PlaceData(blockPos2.immutable(), uP2, 0.0 + explosionDamage, CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null));
               }
            }
         }
      }
   }

   public void onMoveInput(PlayerInputEvent playerInputEvent) {
      if (this.appliesMovementCorrection()) {
         if (this.isFullCorrectionActive() && this.correctionStandPos != null) {
            this.driveToCorrectionPosition(playerInputEvent, this.correctionStandPos);
         } else {
            float yaw;
            if (RotationController.isActive() && !RotationController.isClientLook()) {
               yaw = RotationController.getFreeYaw();
            } else {
               yaw = mc.player.getYRot();
            }

            float value = yaw;
            float yRot = mc.player.getYRot();
            DirectionHelper.apply(playerInputEvent, yRot, value);
         }
      }
   }

   private BlockHitResult raycastPlaceHit(BlockPos blockPos, Direction direction, Vec3 position) {
      if (mc.player != null && mc.level != null && blockPos != null) {
         Vec3 facePoint = position != null ? position : BlockFaces.getFacePoint(blockPos, direction != null ? direction : Direction.UP);
         if (this.canPlaceThroughSoftWalls(blockPos)) {
            Direction direction2 = direction != null ? direction : Direction.UP;
            return new BlockHitResult(facePoint, direction2, blockPos, false);
         }

         BlockHitResult reach = BlockRayTrace.raycastBlock(blockPos, this.getReach());
         if (reach != null && reach.getBlockPos().equals(blockPos)) {
            return reach;
         }

         Vec3 eyePosition = mc.player.getEyePosition();
         Vec3 viewVector = eyePosition.add(mc.player.getViewVector(1.0F).scale(this.getReach()));
         BlockHitResult blockHitResult = mc.level.clip(new ClipContext(eyePosition, viewVector, Block.OUTLINE, Fluid.NONE, mc.player));
         return blockHitResult.getType() != Type.BLOCK || !blockHitResult.getBlockPos().equals(blockPos) || direction != null && blockHitResult.getDirection() != direction && direction != Direction.UP
            ? null
            : blockHitResult;
      } else {
         return null;
      }
   }

   private void tryPromoteDigToCrystal(BlockPos blockPos) {
      if (mc.level != null && this.goalData != null) {
         BlockPos blockPos2 = blockPos;
         if (this.isCrystalBase(mc.level.getBlockState(blockPos2))) {
            if (mc.level.getBlockState(blockPos2.above()).isAir()) {
               this.goalData = new CrystalAuraModule.PlaceData(
                  blockPos2.immutable(), Direction.UP, Math.max(this.goalData.damage(), 45.0), CrystalAuraModule.PlaceResult.PLACE_CRYSTAL, null
               );
               this.lastData = this.goalData;
               this.crystalPlaceBase = null;
               this.crystalPlaceBlocked = false;
            }
         } else {
            BlockState blockState = mc.level.getBlockState(blockPos2);
            if (blockState.isAir() || blockState.canBeReplaced() && !blockState.is(Blocks.COBWEB)) {
               if (!mc.level.getBlockState(blockPos2.above()).isAir()) {
                  return;
               }

               CrystalAuraModule.PlacementHit solidNeighborHit = this.findSolidNeighborHit(blockPos2);
               if (solidNeighborHit != null && this.hasObsidianInHotbar()) {
                  this.goalData = new CrystalAuraModule.PlaceData(
                     solidNeighborHit.neighbor().immutable(), solidNeighborHit.face(), Math.max(this.goalData.damage(), 45.0), CrystalAuraModule.PlaceResult.PLACE_OBSIDIAN, null
                  );
                  this.lastData = this.goalData;
               }
            }
         }
      }
   }

   private void handleObsidianPlacement(BlockPos blockPos, Direction direction, BlockHitResult blockHitResult) {
      this.requestWebIgnore();
      BlockPos blockPos2 = blockPos.relative(direction);
      BlockState blockState = mc.level.getBlockState(blockPos2);
      int value = !blockState.isAir() && (!blockState.canBeReplaced() || blockState.is(Blocks.COBWEB)) ? 0 : 1;
      if (value == 0) {
         if (this.isCrystalBase(blockState)) {
            this.pendingObsidianPos = null;
            this.promoteObsidianToCrystal(blockPos2);
         }
      } else {
         long now = System.currentTimeMillis();
         if (this.pendingObsidianPos != null && this.pendingObsidianPos.equals(blockPos2)) {
            BlockState blockState2 = mc.level.getBlockState(blockPos2);
            if (this.isCrystalBase(blockState2)) {
               this.pendingObsidianPos = null;
               this.promoteObsidianToCrystal(blockPos2);
               return;
            }

            if (now - this.lastObsidianPlaceMs < 120L) {
               return;
            }

            this.pendingObsidianPos = null;
         }

         Direction direction2 = direction;
         BlockPos blockPos3 = blockPos;
         BlockState blockState3 = mc.level.getBlockState(blockPos3);
         if (blockState3.isAir() || blockState3.canBeReplaced() || !blockState3.getFluidState().isEmpty()) {
            CrystalAuraModule.PlacementHit solidNeighborHit = this.findSolidNeighborHit(blockPos2);
            if (solidNeighborHit == null) {
               return;
            }

            blockPos3 = solidNeighborHit.neighbor();
            direction2 = solidNeighborHit.face();
         }

         int value2 = !this.canPlaceThroughSoftWalls(blockPos2) && !this.canPlaceThroughSoftWalls(blockPos3) ? 0 : 1;
         if (value2 == 0) {
            BlockPos blockPos4 = blockPos2.below();
            BlockState blockState4 = mc.level.getBlockState(blockPos4);
            if (!blockState4.isAir() && !blockState4.canBeReplaced() && blockState4.getFluidState().isEmpty()) {
               blockPos3 = blockPos4;
               direction2 = Direction.UP;
            }
         } else if (direction2 != Direction.UP) {
            BlockPos blockPos5 = blockPos2.below();
            BlockState blockState5 = mc.level.getBlockState(blockPos5);
            if (!blockState5.isAir() && !blockState5.canBeReplaced() && blockState5.getFluidState().isEmpty()) {
               blockPos3 = blockPos5;
               direction2 = Direction.UP;
            }
         }

         Vec3 highPlaceHit = this.getHighPlaceHit(blockPos3, direction2, blockPos2);
         if (this.holdHotbarItem(Items.OBSIDIAN)) {
            this.rotation = Rotation.lookingAt(highPlaceHit, mc.player.getEyePosition());
            this.applyRotation();
            this.stopBreaking();
            ItemUseCooldown.setItemUseCooldown(0);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(highPlaceHit, direction2, blockPos3, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
            this.pendingObsidianPos = blockPos2.immutable();
            this.lastObsidianPlaceMs = now;
            this.upsertVisual(blockPos2, RGB_OBSIDIAN[0], RGB_OBSIDIAN[1], RGB_OBSIDIAN[2]);
            if (mc.level.getBlockState(blockPos2).is(Blocks.OBSIDIAN)) {
               this.pendingObsidianPos = null;
               this.promoteObsidianToCrystal(blockPos2);
            }
         }
      }
   }

   private void onTargetMoved() {
      this.failedGoals.clear();
      if (this.goalData == null
         || this.goalData.placeResult() != CrystalAuraModule.PlaceResult.EXPLOSION
            && this.goalData.placeResult() != CrystalAuraModule.PlaceResult.PLACE_CRYSTAL
            && this.pendingObsidianPos == null
            && this.pendingCrystalBase == null) {
         this.pendingCrystalBase = null;
         this.crystalPlaceBlocked = false;
         this.pendingObsidianPos = null;
         this.clearFullCorrection();
         if (this.goalData != null && this.goalData.placeResult() != CrystalAuraModule.PlaceResult.EXPLOSION) {
            this.goalData = null;
            this.lastData = null;
         }

         this.stopBreaking();
      }
   }

   private void rememberSavedHotbarSlot() {
      if (this.savedHotbarSlot == -1) {
         if (mc.player != null) {
            Inventory inventory = mc.player.getInventory();
            this.savedHotbarSlot = inventory.getSelectedSlot();
         }
      }
   }

   private boolean isBlockedByEntitiesForCrystal(Iterable<Entity> entitys, BlockPos blockPos, Player player) {
      Iterator iterator = entitys.iterator();

      Entity entity;
      do {
         if (!iterator.hasNext()) {
            return false;
         }

         entity = (Entity)iterator.next();
      } while (entity == player || !VirtualExplosionDamage.intersectsEntityBox(blockPos, entity));

      return true;
   }

   public void onGameUpdate() {
      if (this.isEnabled()) {
         if (mc.player != null) {
            if (mc.level != null) {
               this.applyRotation();
            }
         }
      }
   }

   private boolean hasVisual(BlockPos blockPos) {
      Iterator iterator = this.visualDatas.iterator();

      CrystalAuraModule.VisualData visualData;
      do {
         if (!iterator.hasNext()) {
            return false;
         }

         visualData = (CrystalAuraModule.VisualData)iterator.next();
      } while (!visualData.blockPos.equals(blockPos));

      return true;
   }

   private boolean correctionSideIsClear(Vec3 position, Vec3 position2) {
      BlockPos blockPos = BlockPos.containing(position2.x, position.y + 0.01, position2.z);
      if (!this.isSolidWalk(blockPos)) {
         BlockPos blockPos2 = blockPos.above();
         if (!this.isSolidWalk(blockPos2)) {
            BlockPos blockPos3 = blockPos.below();
            if (!this.isSolidWalk(blockPos3)) {
               return false;
            }

            Vec3 position3 = position.add(0.0, 0.51, 0.0);
            double y = position.y + 0.51;
            Vec3 position4 = new Vec3(position2.x, y, position2.z);
            BlockHitResult blockHitResult = this.clipWalk(position3, position4);
            if (blockHitResult != null && blockHitResult.getType() == Type.BLOCK) {
               Vec3 location = blockHitResult.getLocation();
               if (!(position.distanceTo(location) > 0.9)) {
                  return false;
               }
            }

            return true;
         }
      }

      return false;
   }

   private Vec3 steerAroundObstacles(Vec3 position) {
      Vec3 position2 = mc.player.position();
      BlockHitResult blockHitResult = this.clipWalk(position2.add(0.0, 0.51, 0.0), new Vec3(position.x, position2.y + 0.51, position.z));
      if (blockHitResult != null && blockHitResult.getType() == Type.BLOCK) {
         BlockPos blockPos = blockHitResult.getBlockPos();
         BlockPos blockPos2 = BlockPos.containing(position);
         if (!blockPos.equals(blockPos2) && !blockPos.equals(blockPos2.above()) && this.isSolidWalk(blockPos)) {
            Vec3 position3 = Vec3.atCenterOf(blockPos).subtract(position2);
            Vec3 position4 = new Vec3(-position3.z, 0.0, position3.x);
            if (position4.lengthSqr() < 1.0E-6) {
               position4 = new Vec3(1.0, 0.0, 0.0);
            }

            position4 = position4.normalize().scale(1.45);
            Vec3 position5 = Vec3.atBottomCenterOf(blockPos).add(position4);
            Vec3 position6 = Vec3.atBottomCenterOf(blockPos).subtract(position4);
            boolean correctionSideIsClear = this.correctionSideIsClear(position2, position5);
            boolean correctionSideIsClear2 = this.correctionSideIsClear(position2, position6);
            if (correctionSideIsClear != correctionSideIsClear2) {
               return correctionSideIsClear ? position5 : position6;
            } else {
               return position.distanceToSqr(position5) <= position.distanceToSqr(position6) ? position5 : position6;
            }
         } else {
            return position;
         }
      } else {
         return position;
      }
   }

   public record PlaceData(BlockPos blockPos, Direction face, double damage, CrystalAuraModule.PlaceResult placeResult, Entity entity) {
      public Entity entity() {
         return this.entity;
      }

      public BlockPos blockPos() {
         return this.blockPos;
      }

      public CrystalAuraModule.PlaceResult placeResult() {
         return this.placeResult;
      }

      public double damage() {
         return this.damage;
      }

      public Direction face() {
         return this.face;
      }
   }

   enum PlaceResult {
      PLACE_OBSIDIAN,
      PLACE_CRYSTAL,
      EXPLOSION,
      BREAK,
      AWAITING;
   }

   public record PlacementHit(BlockPos neighbor, Direction face, BlockHitResult hitResult, Vec3 hit) {
      public BlockPos neighbor() {
         return this.neighbor;
      }

      public BlockHitResult hitResult() {
         return this.hitResult;
      }

      public Direction face() {
         return this.face;
      }

      public Vec3 hit() {
         return this.hit;
      }
   }

   enum ReturnResult {
      ITEM,
      PLAYER,
      NONE;
   }

   static final class VisualData {
      private float g;
      private final RestartHelper animation = new RestartHelper(280L, EasingCurve.CUBIC_OUT);
      private long holdUntilMs;
      private float r;
      private float b;
      private float fadeTarget;
      private boolean goalHighlight;
      private final BlockPos blockPos;

      private VisualData(BlockPos blockPos2, float r2, float g2, float b2, boolean goalHighlight2) {
         this.blockPos = blockPos2;
         this.r = r2;
         this.g = g2;
         this.b = b2;
         this.goalHighlight = goalHighlight2;
         this.fadeTarget = 0.0F;
         this.holdUntilMs = goalHighlight2 ? Long.MAX_VALUE : 0L;
      }
   }
}
