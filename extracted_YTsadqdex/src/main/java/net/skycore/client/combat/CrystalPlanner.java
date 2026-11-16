package net.skycore.client.combat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.combat.AntiBotModule;
import net.skycore.client.util.MinecraftAccess;

public final class CrystalPlanner implements MinecraftAccess {
   public static final float FRIEND_MAX_DAMAGE = 6.0F;
   public static final double ELYTRA_BOOM_DIST = 5.0;
   public static final float DIG_ADVANTAGE = 6.0F;
   public static final float ELYTRA_BOOM_DAMAGE = 14.0F;
   private static final Direction[] CARDINALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
   public static final float FACEPLACE_ARMOR = 8.0F;
   public static final int MAX_BREAKS = 2;

   private static void addSurgicalAround(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, AABB bounds, boolean condition) {
      addUnderAndCardinals(context, crystalActions, blockPos, bounds, condition);
      if (allowFaceplace(context, bounds)) {

         for (Direction direction : CARDINALS) {
            BlockPos blockPos2 = blockPos.relative(direction);
            tryAddObsidianOrCrystal(context, crystalActions, blockPos2, 0, null, bounds, true);
         }
      }
   }

   private static boolean isEmptyForCrystal(BlockState blockState) {
      return blockState != null && blockState.isAir();
   }

   private static void addHoleLedge(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, AABB bounds) {
      int index = Math.min(8, (int)Math.ceil(context.placeRange()));

      for (int value = 0; value <= index; value++) {
         for (Direction direction : CARDINALS) {
            BlockPos blockPos2 = blockPos.relative(direction).above(value);
            if (withinPlace(context, blockPos2) && isSupport(blockPos2)) {
               BlockPos blockPos3 = blockPos2.above();
               tryAddObsidianOrCrystal(context, crystalActions, blockPos3, 0, null, bounds, false);
            }
         }
      }
   }

   private static boolean unbreakable(BlockPos blockPos) {
      BlockState blockState = mc.level.getBlockState(blockPos);
      if (!blockState.is(Blocks.BEDROCK) && !blockState.is(Blocks.BARRIER)) {
         if (!blockState.is(Blocks.END_PORTAL_FRAME)) {
            return false;
         }
      }

      return true;
   }

   private static void collectExistingBases(CrystalPlanner.Context context, List<CrystalAction> crystalActions) {
      Player player = context.self();
      int index = (int)Math.ceil(context.placeRange());
      BlockPos eyePosition = BlockPos.containing(player.getEyePosition());

      for (int index2 = -index; index2 <= index; index2++) {
         for (int index3 = -index; index3 <= index; index3++) {
            for (int index4 = -index; index4 <= index; index4++) {
               BlockPos blockPos = eyePosition.offset(index2, index3, index4);
               tryAddCrystalBase(context, crystalActions, blockPos, 0, null, null, false);
            }
         }
      }
   }

   private static boolean isAboveHead(BlockPos blockPos, Player player, AABB bounds) {
      int value = 0;
      value = (byte)0;
      if (blockPos != null && player != null) {
         BlockPos blockPos2 = player.blockPosition();
         value = blockPos.getX();
         if (value == blockPos2.getX() && blockPos.getZ() == blockPos2.getZ()) {
            double value2;
            if (bounds != null) {
               value2 = bounds.minY;
            } else {
               value2 = player.getY();
            }

            double value3 = value2;
            return blockPos.getY() + 1.0 > value3 + 1.55;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean isPocket(BlockPos blockPos, Player player) {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      value = blockPos.getY();
      BlockPos blockPos2 = player.blockPosition();
      return value < blockPos2.getY();
   }

   private static boolean stillValid(CrystalPlanner.Context context, CrystalAction crystalAction) {
      if (crystalAction == null || context.target() == null || mc.level == null) {
         return false;
      }

      if (!isFailed(context, crystalAction.base()) && !isFailed(context, crystalAction.actionPos())) {
         return (switch (crystalAction.kind()) {
            case EXPLODE -> {
               if (crystalAction.crystal() != null && crystalAction.crystal().isAlive()) {
                  CrystalPlanner.DamageSnapshot damageSnapshot = snapshot(context, Vec3.atCenterOf(crystalAction.crystal().blockPosition()), null, null, predictedBox(context));
                  yield usable(context, damageSnapshot, crystalAction.pocket()) && worthExploding(context, damageSnapshot, crystalAction.crystal()) ? 1 : 0;
               } else {
                  yield 0;
               }
            }
            case PLACE_CRYSTAL -> CrystalDamage.isCrystalBase(mc.level.getBlockState(crystalAction.base()))
                  && crystalAir(crystalAction.base())
                  && !crystalBlocked(context, crystalAction.base().above(), predictedBox(context))
                  && nearTarget(context, crystalAction)
               ? 1
               : 0;
            case PLACE_OBSIDIAN -> canReplace(crystalAction.actionPos()) && context.hasObsidian() && nearTarget(context, crystalAction) ? 1 : 0;
            case DIG -> crystalAction.actionPos() != null
                  && !mc.level.getBlockState(crystalAction.actionPos()).isAir()
                  && !mc.level.getBlockState(crystalAction.actionPos()).is(Blocks.COBWEB)
                  && !unbreakable(crystalAction.actionPos())
                  && nearTarget(context, crystalAction)
               ? 1
               : 0;
            case PROTECT -> canReplace(crystalAction.actionPos()) && context.hasObsidian() ? 1 : 0;
         }) != 0;
      } else {
         return false;
      }
   }

   private static Direction placeFace(BlockPos blockPos) {
      if (mc.player == null) {
         return null;
      }

      Direction[] directionValues = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN};
      Direction direction = null;
      double mAX_VALUE = Double.MAX_VALUE;
      Vec3 eyePosition = mc.player.getEyePosition();

      for (Direction direction2 : directionValues) {
         BlockPos opposite = blockPos.relative(direction2.getOpposite());
         BlockState blockState = mc.level.getBlockState(opposite);
         if (!blockState.isAir() && !blockState.canBeReplaced() && blockState.getFluidState().isEmpty()) {
            Vec3 facePoint = CrystalPlacement.getFacePoint(opposite, direction2);
            double computedValue = eyePosition.distanceToSqr(facePoint) + (direction2 == Direction.DOWN ? 8.0 : 0.0);
            if (computedValue < mAX_VALUE) {
               mAX_VALUE = computedValue;
               direction = direction2;
            }
         }
      }

      return direction == null ? null : direction;
   }

   private static boolean usable(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot, boolean condition) {
      if (damageSnapshot.friend > 6.0F) {
         return false;
      }

      if (!selfLives(context, damageSnapshot)) {
         return false;
      }

      if (CrystalDamage.wouldKill(context.target(), damageSnapshot.target) || CrystalDamage.wouldPopTotem(context.target(), damageSnapshot.target)) {
         return damageSnapshot.target > 0.5F;
      }

      if (damageSnapshot.self > damageSnapshot.target + 0.01F) {
         return false;
      }

      float value = context.minTargetDamage();
      if (condition && CrystalDamage.hasTotem(context.target())) {
         value = Math.min(value, 4.0F);
      }

      return damageSnapshot.target >= value;
   }

   private static boolean crystalBlockedAssumingAir(CrystalPlanner.Context context, BlockPos blockPos, AABB bounds, BlockPos blockPos2) {
      return crystalBlocked(context, blockPos, bounds);
   }

   private static void tryAddObsidianOrCrystal(
      CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, int value, BlockPos blockPos2, AABB bounds, boolean condition
   ) {
      if (mc.level != null && blockPos != null && withinPlace(context, blockPos)) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (CrystalDamage.isCrystalBase(blockState)) {
            tryAddCrystalBase(context, crystalActions, blockPos, value, blockPos2, bounds, condition);
         } else if (context.hasObsidian() && canReplace(blockPos)) {
            if (airForCrystalOn(blockPos) && !CrystalDamage.isFluidAtCrystal(blockPos.above())) {
               if (!crystalBlocked(context, blockPos.above(), bounds)) {
                  if (!isAboveHead(blockPos, context.target(), bounds)) {
                     if (!isStandFloor(context, blockPos)) {
                        Direction direction = placeFace(blockPos);
                        if (direction != null) {
                           if (!isFailed(context, blockPos)) {
                              Vec3 position = boomOn(blockPos);
                              CrystalPlanner.DamageSnapshot damageSnapshot = snapshot(context, position, blockPos2, blockPos, bounds != null ? bounds : predictedBox(context));
                              int y = blockPos.getY() < context.target().blockPosition().getY() ? 1 : 0;
                              if (!condition || allowFaceplace(context, damageSnapshot, (y != 0))) {
                                 if (usable(context, damageSnapshot, (y != 0))) {
                                    if (value <= 0 || worthBreaking(context, damageSnapshot, value, (y != 0), blockPos)) {
                                       double computedValue = score(context, damageSnapshot, value, false, (y != 0), condition, position) - 12.0;
                                       if (CrystalPlacement.placeAgainstHit(blockPos, direction) == null) {
                                          computedValue -= 12.0;
                                       }

                                       crystalActions.add(
                                          new CrystalAction(
                                             CrystalAction.Kind.PLACE_OBSIDIAN,
                                             blockPos.immutable(),
                                             blockPos.immutable(),
                                             direction,
                                             null,
                                             damageSnapshot.target,
                                             damageSnapshot.self,
                                             damageSnapshot.splash,
                                             damageSnapshot.friend,
                                             value,
                                             (y != 0),
                                             condition,
                                             false,
                                             computedValue
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

   private static void tryAddSidePocket(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, AABB bounds) {
      if (context.canDig() && mc.level != null && blockPos != null && withinPlace(context, blockPos)) {
         if (!isStandFloor(context, blockPos) && !isFailed(context, blockPos)) {
            BlockState blockState = mc.level.getBlockState(blockPos);
            if (CrystalDamage.isCrystalBase(blockState) && crystalAir(blockPos)) {
               tryAddCrystalBase(context, crystalActions, blockPos, 0, null, bounds, isFaceplace(blockPos, context.target()));
            } else {
               ArrayList elements = new ArrayList(3);
               if (!CrystalDamage.isCrystalBase(blockState) && !canReplace(blockPos)) {
                  if (unbreakable(blockPos)) {
                     return;
                  }

                  elements.add(blockPos.immutable());
               }

               BlockPos blockPos2 = blockPos.above();
               BlockPos blockPos3 = blockPos.above(2);
               if (!isEmptyForCrystal(blockPos2)) {
                  if (unbreakable(blockPos2) || CrystalDamage.isCrystalBase(mc.level.getBlockState(blockPos2))) {
                     return;
                  }

                  elements.add(blockPos2.immutable());
               }

               if (!isEmptyForCrystal(blockPos3)) {
                  if (unbreakable(blockPos3) || CrystalDamage.isCrystalBase(mc.level.getBlockState(blockPos3))) {
                     return;
                  }

                  elements.add(blockPos3.immutable());
               }

               if (!elements.isEmpty() && elements.size() <= 2) {
                  BlockPos blockPos4 = (BlockPos)elements.get(0);
                  if (!isFailed(context, blockPos4)) {
                     Vec3 position = boomOn(blockPos);
                     BlockPos blockPos5 = !context.hasObsidian() && !CrystalDamage.isCrystalBase(blockState) ? null : blockPos;
                     CrystalPlanner.DamageSnapshot damageSnapshot = snapshot(context, position, blockPos4, blockPos5, bounds != null ? bounds : predictedBox(context));
                     boolean condition = isPocket(blockPos, context.target());
                     boolean condition2 = isFaceplace(blockPos, context.target());
                     if (!condition2 || allowFaceplace(context, damageSnapshot, condition) || condition) {
                        if (usable(context, damageSnapshot, condition) && worthBreaking(context, damageSnapshot, elements.size(), condition, blockPos4)) {
                           if (!crystalBlockedAssumingAir(context, blockPos.above(), bounds, blockPos4)) {
                              Direction closestFace = CrystalPlacement.getClosestFace(blockPos4, context.self());
                              double value = score(context, damageSnapshot, elements.size(), false, condition, condition2, position);
                              if (isSlow(mc.level.getBlockState(blockPos4))) {
                                 value -= 40.0;
                              }

                              crystalActions.add(
                                 new CrystalAction(
                                    CrystalAction.Kind.DIG,
                                    blockPos.immutable(),
                                    blockPos4.immutable(),
                                    closestFace,
                                    null,
                                    damageSnapshot.target,
                                    damageSnapshot.self,
                                    damageSnapshot.splash,
                                    damageSnapshot.friend,
                                    elements.size(),
                                    condition,
                                    condition2,
                                    false,
                                    value
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

   private static boolean worthBreaking(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot, int value, boolean condition, BlockPos blockPos) {
      if (value <= 0) {
         return true;
      }

      int value2 = !CrystalDamage.wouldKill(context.target(), damageSnapshot.target) && !CrystalDamage.wouldPopTotem(context.target(), damageSnapshot.target) ? 0 : 1;
      if (value2 != 0) {
         return true;
      }

      if (damageSnapshot.target < context.minTargetDamage() + 3.0F) {
         return false;
      }

      int blockState = blockPos != null && isSlow(mc.level.getBlockState(blockPos)) ? 1 : 0;
      return blockState != 0 && damageSnapshot.target < context.minTargetDamage() + 6.0F ? false : damageSnapshot.target >= context.minTargetDamage();
   }

   private static void addUnderAndCardinals(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, AABB bounds, boolean condition) {
      for (Direction direction : CARDINALS) {
         for (int value = 1; value <= 2; value++) {
            BlockPos blockPos2 = blockPos.relative(direction).below(value);
            tryAddObsidianOrCrystal(context, crystalActions, blockPos2, 0, null, bounds, false);
            tryAddSidePocket(context, crystalActions, blockPos2, bounds);
         }

         BlockPos blockPos3 = blockPos.relative(direction);
         tryAddObsidianOrCrystal(context, crystalActions, blockPos3, 0, null, bounds, true);
         tryAddSidePocket(context, crystalActions, blockPos3, bounds);
      }

      addUndercut(context, crystalActions, blockPos, bounds);
      addHoleLedge(context, crystalActions, blockPos, bounds);
   }

   private static double score(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot, int value, boolean condition, boolean condition2, boolean condition3, Vec3 position) {
      double distance = damageSnapshot.target * 5.0 + damageSnapshot.splash * 1.6 - damageSnapshot.self * 4.0 - damageSnapshot.friend * 8.0;
      if (CrystalDamage.wouldKill(context.target(), damageSnapshot.target)) {
         distance += 250.0;
      }

      if (CrystalDamage.wouldPopTotem(context.target(), damageSnapshot.target) && condition2) {
         distance += 130.0;
      }

      if (condition2) {
         distance += 32.0;
      }

      if (condition3) {
         distance += isWebbed(context.target()) ? 22.0 : -55.0;
      } else if (isWebbed(context.target())) {
         distance += 16.0;
      }

      if (isWebbed(context.target()) && position != null && nextToTarget(BlockPos.containing(position.x, position.y - 1.0, position.z), context.target())) {
         distance += 36.0;
      }

      if (condition) {
         distance += 20.0;
      }

      distance -= value * 28.0;
      if (damageSnapshot.self > context.maxSelfDamage() * 0.5F) {
         distance -= 25.0;
      }

      if (damageSnapshot.self > damageSnapshot.target) {
         distance -= 80.0;
      }

      if (position != null) {
         distance -= context.target().position().distanceToSqr(position) * 1.1;
         distance += Math.min(36.0, context.self().position().distanceToSqr(position)) * 0.35;
         if (isAboveTarget(context)) {
            distance += (context.self().getY() - position.y) * 10.0;
         }
      }

      return distance;
   }

   private static AABB predictedBox(CrystalPlanner.Context context) {
      Player player = context.target();
      int damage = player.isFallFlying() ? 2 : (player.onGround() ? 1 : 1);
      AABB bounds = CrystalDamage.predictBox(player, damage);
      return !player.onGround() && !player.isFallFlying() ? bounds : bounds;
   }

   public static BlockPos placeAnchor(BlockPos blockPos, Direction direction) {
      return blockPos.relative(direction.getOpposite());
   }

   private static boolean selfLives(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot) {
      Player player = context.self();
      if (!player.isCreative() && !player.isSpectator()) {
         float computedValue = CrystalDamage.remainingHp(player) - damageSnapshot.self;
         if (computedValue < 0.5F) {
            return false;
         } else {
            boolean condition = CrystalDamage.wouldKill(context.target(), damageSnapshot.target);
            if (condition && computedValue >= 0.5F) {
               return true;
            } else {
               return damageSnapshot.self > context.maxSelfDamage() + 0.01F && !condition ? false : computedValue >= context.minHealth() || condition;
            }
         }
      } else {
         return true;
      }
   }

   private static void tryAddCrystalBase(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, int value, BlockPos blockPos2, AABB bounds, boolean condition) {
      if (mc.level != null && blockPos != null && withinPlace(context, blockPos)) {
         if (!isFailed(context, blockPos) && CrystalDamage.isCrystalBase(mc.level.getBlockState(blockPos))) {
            if (crystalAir(blockPos) && !CrystalDamage.isFluidAtCrystal(blockPos.above())) {
               if (!crystalBlocked(context, blockPos.above(), bounds)) {
                  if (!isAboveHead(blockPos, context.target(), bounds)) {
                     if (!isStandFloor(context, blockPos)) {
                        Vec3 position = boomOn(blockPos);
                        CrystalPlanner.DamageSnapshot damageSnapshot = snapshot(context, position, blockPos2, null, bounds != null ? bounds : predictedBox(context));
                        boolean condition2 = isPocket(blockPos, context.target());
                        if (!condition || allowFaceplace(context, damageSnapshot, condition2)) {
                           if (usable(context, damageSnapshot, condition2)) {
                              if (value <= 0 || worthBreaking(context, damageSnapshot, value, condition2, blockPos)) {
                                 double value2 = score(context, damageSnapshot, value, false, condition2, condition, position);
                                 if (!CrystalPlacement.hasVisibleFace(blockPos)) {
                                    value2 -= 12.0;
                                 }

                                 crystalActions.add(
                                    new CrystalAction(
                                       CrystalAction.Kind.PLACE_CRYSTAL,
                                       blockPos.immutable(),
                                       blockPos.immutable(),
                                       Direction.UP,
                                       null,
                                       damageSnapshot.target,
                                       damageSnapshot.self,
                                       damageSnapshot.splash,
                                       damageSnapshot.friend,
                                       value,
                                       condition2,
                                       condition || isFaceplace(blockPos, context.target()),
                                       false,
                                       value2 + 45.0
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

   public static boolean shouldRetreat(CrystalPlanner.Context context) {
      if (context.self() != null && context.target() != null) {
         Player player = context.self();
         Player player2 = context.target();
         double distance = player.distanceTo(player2);
         if (distance > 2.8) {
            return false;
         }

         BlockPos blockPos = player.blockPosition();
         BlockPos blockPos2 = player2.blockPosition();
         int x = blockPos.getX() == blockPos2.getX() && blockPos.getZ() == blockPos2.getZ() ? 1 : 0;
         int value = !isOneBlockHole(blockPos) && !isOneBlockHole(blockPos2) ? 0 : 1;
         return x != 0 && Math.abs(player.getY() - player2.getY()) < 2.2 || value != 0 && distance < 2.4 || distance < 1.35;
      } else {
         return false;
      }
   }

   private CrystalPlanner() {
   }

   private static boolean isOneBlockHole(BlockPos blockPos) {
      if (mc.level == null) {
         return false;
      }

      int value = 0;

      for (Direction direction : CARDINALS) {
         BlockState blockState = mc.level.getBlockState(blockPos.relative(direction));
         if (!blockState.isAir() && !blockState.canBeReplaced() && blockState.getFluidState().isEmpty()) {
            value++;
         }
      }

      return value >= 3 && (mc.level.getBlockState(blockPos.above()).isAir() || mc.level.getBlockState(blockPos.above()).canBeReplaced());
   }

   private static boolean isAboveTarget(CrystalPlanner.Context context) {
      if (context.self() != null) {
         if (context.target() != null) {
            Player player = context.self();
            double y = player.getY();
            Player player2 = context.target();
            if (y > player2.getY() + 0.45) {
               return true;
            }
         }
      }

      return false;
   }

   private static List<Player> nearbyPlayers(Player player) {
      ArrayList elements = new ArrayList();
      var abstractClientPlayers = mc.level.players();
      Iterator iterator = abstractClientPlayers.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return elements;
         }

         Player element = (Player)iterator.next();
         if (element != player) {
            if (element.isAlive()) {
               if (element.distanceTo(player) <= 16.0F) {
                  elements.add(element);
               }
            }
         }
      }
   }

   public static CrystalAction stickOrSwitch(CrystalPlanner.Context context, CrystalAction crystalAction, CrystalAction crystalAction2) {
      if (crystalAction2 == null) {
         return stillValid(context, crystalAction) ? crystalAction : null;
      }

      if (crystalAction != null && stillValid(context, crystalAction) && nearTarget(context, crystalAction)) {
         if (crystalAction.kind() == CrystalAction.Kind.DIG && crystalAction2.kind() != CrystalAction.Kind.EXPLODE && crystalAction2.kind() != CrystalAction.Kind.PLACE_CRYSTAL) {
            return crystalAction;
         }

         if (crystalAction2.pocket() && !crystalAction.pocket()) {
            return crystalAction2;
         }

         if (!crystalAction.faceplace() || crystalAction.pocket() || !crystalAction2.pocket() && crystalAction2.faceplace()) {
            if (crystalAction2.sameAnchor(crystalAction) && crystalAction2.kind() == crystalAction.kind()) {
               return crystalAction2;
            } else {
               boolean condition = CrystalDamage.wouldKill(context.target(), crystalAction2.targetDamage());
               boolean condition2 = CrystalDamage.wouldKill(context.target(), crystalAction.targetDamage());
               if (condition && !condition2) {
                  return crystalAction2;
               } else {
                  return crystalAction2.score() > crystalAction.score() + 8.0 ? crystalAction2 : crystalAction;
               }
            }
         } else {
            return crystalAction2;
         }
      } else {
         return crystalAction2;
      }
   }

   private static void addUndercut(CrystalPlanner.Context context, List<CrystalAction> crystalActions, BlockPos blockPos, AABB bounds) {
      if (context.canDig() && blockPos != null) {
         BlockPos blockPos2 = blockPos.below();
         if (withinPlace(context, blockPos2) && !isFailed(context, blockPos2) && !unbreakable(blockPos2)) {
            BlockState blockState = mc.level.getBlockState(blockPos2);
            if (!isEmptyForCrystal(blockState) && !CrystalDamage.isCrystalBase(blockState)) {
               CrystalPlanner.DamageSnapshot damageSnapshot = null;

               for (Direction direction : CARDINALS) {
                  BlockPos blockPos3 = blockPos.relative(direction).below(2);
                  Vec3 position = boomOn(blockPos3);
                  BlockPos blockPos4 = context.hasObsidian() ? blockPos3 : null;
                  CrystalPlanner.DamageSnapshot damageSnapshot2 = snapshot(context, position, blockPos2, blockPos4, bounds != null ? bounds : predictedBox(context));
                  if (damageSnapshot == null || damageSnapshot2.target > damageSnapshot.target) {
                     damageSnapshot = damageSnapshot2;
                  }
               }

               if (damageSnapshot != null && usable(context, damageSnapshot, true) && worthBreaking(context, damageSnapshot, 1, true, blockPos2)) {
                  Direction closestFace = CrystalPlacement.getClosestFace(blockPos2, context.self());
                  crystalActions.add(
                     new CrystalAction(
                        CrystalAction.Kind.DIG,
                        blockPos2.immutable(),
                        blockPos2.immutable(),
                        closestFace,
                        null,
                        damageSnapshot.target,
                        damageSnapshot.self,
                        damageSnapshot.splash,
                        damageSnapshot.friend,
                        1,
                        true,
                        false,
                        false,
                        score(context, damageSnapshot, 1, false, true, false, boomOn(blockPos2)) + 55.0
                     )
                  );
               }
            } else {
               for (Direction direction2 : CARDINALS) {
                  tryAddObsidianOrCrystal(context, crystalActions, blockPos.relative(direction2).below(2), 0, blockPos2, bounds, false);
                  tryAddSidePocket(context, crystalActions, blockPos.relative(direction2).below(2), bounds);
               }
            }
         }
      }
   }

   private static boolean allowFaceplace(CrystalPlanner.Context context, AABB bounds) {
      Player player = context.target();
      if (isWebbed(player)) {
         return true;
      }

      if (CrystalDamage.hasTotem(player) && context.canDig()) {
         return false;
      }

      if (!(player.getArmorValue() < 8.0F)) {
         ItemStack itemBySlot = player.getItemBySlot(EquipmentSlot.CHEST);
         if (!itemBySlot.isEmpty()) {
            if (!(CrystalDamage.remainingHp(player) <= 6.0F)) {
               return false;
            }
         }
      }

      return true;
   }

   private static List<CrystalAction> collectCombat(CrystalPlanner.Context context) {
      ArrayList elements = new ArrayList();
      if (mc.level != null && context.self() != null && context.target() != null) {
         collectExplosions(context, elements);
         collectExistingBases(context, elements);
         Player player = context.target();
         addSurgicalAround(context, elements, player.blockPosition(), player.getBoundingBox(), false);
         AABB bounds = CrystalDamage.predictBox(player, player.isFallFlying() ? 2 : 1);
         BlockPos center = BlockPos.containing(bounds.getCenter().x, bounds.minY + 0.01, bounds.getCenter().z);
         if (!center.equals(player.blockPosition()) || !player.onGround()) {
            addSurgicalAround(context, elements, center, bounds, true);
         }

         return elements;
      } else {
         return elements;
      }
   }

   private static boolean allowFaceplace(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot, boolean condition) {
      if (!condition && !isWebbed(context.target())) {
         Player player = context.target();
         if (CrystalDamage.wouldKill(player, damageSnapshot.target)) {
            return true;
         }

         if (CrystalDamage.hasTotem(context.target())) {
            if (context.canDig()) {
               return false;
            }
         }

         AABB bounds = predictedBox(context);
         return allowFaceplace(context, bounds);
      } else {
         return true;
      }
   }

   private static boolean canReplace(BlockPos blockPos) {
      BlockState blockState = mc.level.getBlockState(blockPos);
      if (!blockState.isAir()) {
         if (blockState.canBeReplaced()) {
            if (!blockState.is(Blocks.COBWEB)) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private static BlockPos shieldInFront(Player player, Vec3 position) {
      BlockPos blockPos = player.blockPosition();
      double x = position.x - player.getX();
      double z = position.z - player.getZ();
      Direction direction;
      if (Math.abs(x) < 0.001 && Math.abs(z) < 0.001) {
         direction = player.getDirection();
      } else if (Math.abs(x) > Math.abs(z)) {
         direction = x > 0.0 ? Direction.EAST : Direction.WEST;
      } else {
         direction = z > 0.0 ? Direction.SOUTH : Direction.NORTH;
      }

      BlockPos blockPos2 = blockPos.relative(direction);
      if (canReplace(blockPos2) && !blockPos2.equals(BlockPos.containing(position))) {
         return blockPos2.immutable();
      }

      BlockPos blockPos3 = blockPos.above().relative(direction);
      return canReplace(blockPos3) && !blockPos3.equals(BlockPos.containing(position)) ? blockPos3.immutable() : null;
   }

   private static boolean airForCrystalOn(BlockPos blockPos) {
      return crystalAir(blockPos);
   }

   private static CrystalPlanner.DamageSnapshot snapshot(CrystalPlanner.Context context, Vec3 position, BlockPos blockPos, BlockPos blockPos2, AABB bounds) {
      AABB bounds2 = bounds != null ? bounds : predictedBox(context);
      float explosionDamage = CrystalDamage.getExplosionDamage(position, context.target(), bounds2, blockPos, blockPos2);
      float explosionDamage2 = CrystalDamage.getExplosionDamage(position, context.self(), context.self().getBoundingBox(), blockPos, blockPos2);
      float damage = 0.0F;
      float damage2 = 0.0F;
      if (mc.level != null) {
         for (Player player : nearbyPlayers(context.self())) {
            if (player != context.self() && player != context.target()) {
               float explosionDamage3 = CrystalDamage.getExplosionDamage(position, player, player.getBoundingBox(), blockPos, blockPos2);
               if (FriendManager.INSTANCE.isFriend(player.getGameProfile().name())) {
                  damage2 = Math.max(damage2, explosionDamage3);
               } else if (isValidEnemy(player)) {
                  damage += explosionDamage3;
               }
            }
         }

         for (Player player2 : FakePlayerRegistry.INSTANCE.getFakePlayers()) {
            if (player2 != context.target() && isValidEnemy(player2)) {
               damage += CrystalDamage.getExplosionDamage(position, player2, player2.getBoundingBox(), blockPos, blockPos2);
            }
         }
      }

      if (context.safetyFriends() && damage2 > 6.0F) {
         explosionDamage = 0.0F;
      }

      return new CrystalPlanner.DamageSnapshot(explosionDamage, explosionDamage2, damage, damage2);
   }

   private static CrystalAction protectGoal(BlockPos blockPos, Direction direction, double value) {
      CrystalAction.Kind kind = CrystalAction.Kind.PROTECT;
      BlockPos blockPos2 = blockPos.immutable();
      BlockPos blockPos3 = blockPos.immutable();
      return new CrystalAction(kind, blockPos2, blockPos3, direction, null, 0.0F, 0.0F, 0.0F, 0.0F, 0, false, false, false, value);
   }

   private static boolean worthExploding(CrystalPlanner.Context context, CrystalPlanner.DamageSnapshot damageSnapshot, EndCrystal endCrystal) {
      if (endCrystal != null && context.target() != null) {
         if (CrystalDamage.wouldKill(context.target(), damageSnapshot.target) || CrystalDamage.wouldPopTotem(context.target(), damageSnapshot.target)) {
            return damageSnapshot.self < CrystalDamage.remainingHp(context.self()) - 0.5F;
         } else {
            return damageSnapshot.self > damageSnapshot.target + 0.01F ? false : !(endCrystal.distanceTo(context.self()) + 0.75 < endCrystal.distanceTo(context.target()));
         }
      } else {
         return false;
      }
   }

   private static boolean isStandFloor(CrystalPlanner.Context context, BlockPos blockPos) {
      if (blockPos != null) {
         if (context.target() != null) {
            return blockPos.equals(context.target().blockPosition().below());
         }
      }

      return false;
   }

   public static CrystalAction bestCombat(CrystalPlanner.Context context) {
      var combat = collectCombat(context);
      byte flag = 0;

      for (CrystalAction crystalAction : combat) {
         if (crystalAction.pocket() && crystalAction.kind() != CrystalAction.Kind.EXPLODE) {
            flag = 1;
            break;
         }
      }

      CrystalAction crystalAction2 = null;

      for (CrystalAction crystalAction3 : combat) {
         if ((flag == 0 || !crystalAction3.faceplace() || crystalAction3.pocket()) && (crystalAction2 == null || crystalAction3.score() > crystalAction2.score())) {
            crystalAction2 = crystalAction3;
         }
      }

      return crystalAction2;
   }

   private static boolean isEmptyForCrystal(BlockPos blockPos) {
      if (blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (isEmptyForCrystal(blockState)) {
            return true;
         }
      }

      return false;
   }

   public static CrystalAction bestProtect(CrystalPlanner.Context context) {
      CrystalAction crystalAction = null;
      Iterator iterator = collectProtect(context).iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return crystalAction;
         }

         CrystalAction element = (CrystalAction)iterator.next();
         if (crystalAction == null || element.score() > crystalAction.score()) {
            crystalAction = element;
         }
      }
   }

   private static boolean hasToolFor(BlockPos blockPos) {
      return true;
   }

   private static boolean crystalBlocked(CrystalPlanner.Context context, BlockPos blockPos, AABB bounds) {
      AABB bounds2 = CrystalDamage.crystalOccupancy(blockPos);
      AABB bounds3 = bounds2.inflate(0.05);

      for (Entity entity : mc.level.getEntities(null, bounds3)) {
         if (entity != null && entity != context.self() && entity.isAlive()) {
            if (entity instanceof ItemEntity) {
               return true;
            }

            if (entity instanceof EndCrystal) {
               return true;
            }

            if (entity.getBoundingBox().intersects(bounds2)) {
               if (entity == context.target()) {
                  int value = !context.target().onGround() ? 1 : 0;
                  AABB boundingBox = bounds != null ? bounds : context.target().getBoundingBox();
                  if (value == 0 || boundingBox.intersects(bounds2)) {
                     return true;
                  }
               } else if (entity instanceof Player) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static List<CrystalAction> collectProtect(CrystalPlanner.Context context) {
      ArrayList elements = new ArrayList(6);
      if (mc.level != null && context.self() != null) {
         Player player = context.self();
         BlockPos blockPos = player.blockPosition();
         boolean oneBlockHole = isOneBlockHole(blockPos);
         int blockState = !mc.level.getBlockState(blockPos.below()).isAir() && !mc.level.getBlockState(blockPos).canBeReplaced() ? 0 : 1;
         EndCrystal endCrystal = nearestThreateningCrystal(context);
         int value = player.hurtTime > 0 && endCrystal != null ? 1 : 0;
         if (blockState != 0 && context.hasObsidian()) {
            BlockPos blockPos2 = blockPos.below();
            if (canReplace(blockPos2) && !CrystalDamage.isFluid(blockPos2)) {
               Direction direction = placeFace(blockPos2);
               if (direction != null) {
                  elements.add(protectGoal(blockPos2, direction, 40.0));
               }
            }
         }

         if ((oneBlockHole || value != 0 || endCrystal != null) && context.hasObsidian()) {
            if (oneBlockHole) {
               for (Direction direction2 : CARDINALS) {
                  BlockPos blockPos3 = blockPos.relative(direction2);
                  if (canReplace(blockPos3)) {
                     Direction direction3 = placeFace(blockPos3);
                     if (direction3 != null) {
                        elements.add(protectGoal(blockPos3, direction3, 36.0));
                     }
                  }
               }
            }

            if (endCrystal != null && endCrystal.getY() > player.getEyeY() - 0.2) {
               BlockPos blockPos4 = blockPos.above(2);
               if (canReplace(blockPos4) && mc.level.getBlockState(blockPos.above()).isAir()) {
                  Direction direction4 = placeFace(blockPos4);
                  if (direction4 != null) {
                     elements.add(protectGoal(blockPos4, direction4, 32.0));
                  }
               }
            }

            if (endCrystal != null && !oneBlockHole) {
               BlockPos blockPos5 = coverBetween(player.blockPosition(), endCrystal.blockPosition());
               if (blockPos5 != null && canReplace(blockPos5)) {
                  Direction direction5 = placeFace(blockPos5);
                  if (direction5 != null) {
                     elements.add(protectGoal(blockPos5, direction5, 34.0));
                  }
               }
            }
         }

         return elements;
      } else {
         return elements;
      }
   }

   private static void collectExplosions(CrystalPlanner.Context context, List<CrystalAction> crystalActions) {
      Player player = context.self();
      AABB boundingBox = player.getBoundingBox().inflate(Math.max(8.0, context.breakRange() + 2.0));

      for (EndCrystal endCrystal : mc.level.getEntitiesOfClass(EndCrystal.class, boundingBox, Entity::isAlive)) {
         if (!(endCrystal.distanceTo(player) > context.breakRange())
            && !CrystalDamage.isFluidAtCrystal(endCrystal.blockPosition())
            && (!context.safetyResources() || !damagesValuableDrops(endCrystal))) {
            BlockPos blockPos = endCrystal.blockPosition().below();
            if (!isFailed(context, blockPos)) {
               CrystalPlanner.DamageSnapshot damageSnapshot = snapshot(context, Vec3.atCenterOf(endCrystal.blockPosition()), null, null, predictedBox(context));
               if (usable(context, damageSnapshot, false) && worthExploding(context, damageSnapshot, endCrystal)) {
                  int value = context.target().isFallFlying() && player.onGround() ? 1 : 0;
                  int value2 = value != 0 && endCrystal.distanceTo(context.target()) > 5.0 && damageSnapshot.target < 14.0F ? 1 : 0;
                  if (value2 == 0) {
                     double value3 = score(context, damageSnapshot, 0, true, isPocket(blockPos, context.target()), false, endCrystal.position());
                     if (value != 0 && damageSnapshot.target >= 14.0F) {
                        value3 += 90.0;
                     }

                     crystalActions.add(
                        new CrystalAction(
                           CrystalAction.Kind.EXPLODE,
                           blockPos.immutable(),
                           endCrystal.blockPosition().immutable(),
                           Direction.UP,
                           endCrystal,
                           damageSnapshot.target,
                           damageSnapshot.self,
                           damageSnapshot.splash,
                           damageSnapshot.friend,
                           0,
                           isPocket(blockPos, context.target()),
                           isFaceplace(blockPos, context.target()),
                           false,
                           value3
                        )
                     );
                  }
               }
            }
         }
      }
   }

   private static boolean isSupport(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         BlockState blockState = mc.level.getBlockState(blockPos);
         return !blockState.isAir() && !blockState.canBeReplaced() && blockState.getFluidState().isEmpty();
      } else {
         return false;
      }
   }

   public static CrystalAction blastShield(CrystalPlanner.Context context, CrystalAction crystalAction) {
      if (context.self() != null && crystalAction != null && context.hasObsidian() && mc.level != null) {
         if (crystalAction.kind() != CrystalAction.Kind.EXPLODE && crystalAction.kind() != CrystalAction.Kind.PLACE_CRYSTAL) {
            return null;
         }

         if (isOneBlockHole(context.self().blockPosition())) {
            return null;
         }

         if (crystalAction.selfDamage() < 6.0F) {
            return null;
         }

         Vec3 position = crystalAction.crystal() != null ? crystalAction.crystal().position() : (crystalAction.base() != null ? boomOn(crystalAction.base()) : null);
         if (position == null) {
            return null;
         }

         BlockPos blockPos = shieldInFront(context.self(), position);
         if (blockPos != null && canReplace(blockPos) && !CrystalDamage.isFluid(blockPos)) {
            if (crystalAction.base() == null || !blockPos.equals(crystalAction.base()) && !blockPos.equals(crystalAction.base().above())) {
               if (crystalAction.actionPos() != null && blockPos.equals(crystalAction.actionPos())) {
                  return null;
               }

               Direction direction = placeFace(blockPos);
               if (direction == null) {
                  return null;
               }

               float explosionDamage = CrystalDamage.getExplosionDamage(position, context.self(), context.self().getBoundingBox(), null, blockPos);
               return explosionDamage > crystalAction.selfDamage() - 2.0F ? null : protectGoal(blockPos, direction, 90.0);
            } else {
               return null;
            }
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static boolean isProtectThreat(CrystalPlanner.Context context) {
      if (context.self() == null) {
         return false;
      }

      EndCrystal endCrystal = nearestThreateningCrystal(context);
      if (endCrystal == null) {
         return context.self().hurtTime > 0 && isOneBlockHole(context.self().blockPosition());
      }

      float explosionDamage = CrystalDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal.blockPosition()), context.self());
      float value = CrystalDamage.remainingHp(context.self());
      return explosionDamage >= 8.0F || explosionDamage >= value * 0.3F || context.self().hurtTime > 0;
   }

   private static Vec3 boomOn(BlockPos blockPos) {
      double x = blockPos.getX() + 0.5;
      double y = blockPos.getY() + 1.0;
      double z = blockPos.getZ() + 0.5;
      return new Vec3(x, y, z);
   }

   private static boolean damagesValuableDrops(EndCrystal endCrystal) {
      ClientLevel level = mc.level;
      AABB boundingBox = endCrystal.getBoundingBox();
      boundingBox = boundingBox.inflate(5.0);
      Predicate<net.minecraft.world.entity.item.ItemEntity> predicate = itemEntity -> {
         return true;
      };
      if (!level.getEntitiesOfClass(ItemEntity.class, boundingBox, predicate).isEmpty()) {
         ClientLevel level2 = mc.level;
         boundingBox = endCrystal.getBoundingBox().inflate(5.0);
         var entitiesOfClass = level2.getEntitiesOfClass(
            ItemEntity.class,
            boundingBox,
            itemEntity2 -> {
               Item item = itemEntity2.getItem().getItem();
               if (item != Items.TOTEM_OF_UNDYING) {
                  if (item != Items.ENCHANTED_GOLDEN_APPLE) {
                     if (item != Items.NETHERITE_HELMET) {
                        if (item != Items.NETHERITE_CHESTPLATE
                           && item != Items.NETHERITE_LEGGINGS
                           && item != Items.NETHERITE_BOOTS
                           && item != Items.NETHERITE_SWORD
                           && item != Items.DIAMOND_SWORD) {
                           if (item != Items.ELYTRA) {
                              if (item != Items.TRIDENT) {
                                 return false;
                              }
                           }
                        }
                     }
                  }
               }

               return true;
            }
         );
         var stream = entitiesOfClass.stream();
         if (stream.findAny().isPresent()) {
            return true;
         }
      }

      return false;
   }

   public static boolean isValidEnemy(Player player) {
      if (player == null || mc.player == null || player == mc.player || !player.isAlive() || player.isRemoved()) {
         return false;
      } else if (player.isCreative() || player.isSpectator() || player.isInvulnerable()) {
         return false;
      } else if (player.isPassenger()) {
         return false;
      } else {
         return FriendManager.INSTANCE.isFriend(player.getGameProfile().name()) ? false : !AntiBotModule.isBot(player);
      }
   }

   public static boolean nearTarget(CrystalPlanner.Context context, CrystalAction crystalAction) {
      int value = 1;
      int value2 = 0;
      value = (byte)1;
      value2 = (byte)0;
      if (crystalAction != null && context.target() != null) {
         BlockPos blockPos;
         if (crystalAction.actionPos() != null) {
            blockPos = crystalAction.actionPos();
         } else {
            blockPos = crystalAction.base();
         }

         BlockPos blockPos2 = blockPos;
         if (blockPos2 == null) {
            return true;
         }

         Player player = context.target();
         BlockPos blockPos3 = player.blockPosition();
         value2 = blockPos2.getX() - blockPos3.getX();
         value2 = Math.abs(value2);
         value = blockPos2.getZ();
         value2 += Math.abs(value - blockPos3.getZ());
         value = blockPos2.getY();
         value -= blockPos3.getY();
         int computedValue = value2 + Math.abs(value);
         return computedValue <= 6;
      } else {
         return false;
      }
   }

   private static boolean crystalAir(BlockPos blockPos) {
      BlockState blockState = mc.level.getBlockState(blockPos.above());
      if (isEmptyForCrystal(blockState)) {
         if (isEmptyForCrystal(mc.level.getBlockState(blockPos.above(2)))) {
            return true;
         }
      }

      return false;
   }

   private static boolean nextToTarget(BlockPos blockPos, Player player) {
      if (blockPos != null && player != null) {
         BlockPos blockPos2 = player.blockPosition();
         int x = Math.abs(blockPos.getX() - blockPos2.getX());
         int z = blockPos.getZ();
         int z2 = Math.abs(z - blockPos2.getZ());
         z = blockPos.getY();
         int y = z - blockPos2.getY();
         return x + z2 == 1 && y >= -2 && y <= 1;
      } else {
         return false;
      }
   }

   private static BlockPos coverBetween(BlockPos blockPos, BlockPos blockPos2) {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      value = blockPos2.getX() - blockPos.getX();
      int value2 = Integer.signum(value);
      value = blockPos2.getZ();
      int z = Integer.signum(value - blockPos.getZ());
      if (value2 == 0 && z == 0) {
         return blockPos.above(2);
      } else {
         return blockPos.offset(value2, 0, z);
      }
   }

   private static boolean isFailed(CrystalPlanner.Context context, BlockPos blockPos) {
      if (blockPos != null) {
         var longs = context.failedBases();
         if (longs.contains(blockPos.asLong())) {
            return true;
         }
      }

      return false;
   }

   private static boolean isFaceplace(BlockPos blockPos, Player player) {
      int value = 0;
      value = (byte)0;
      value = blockPos.getY();
      BlockPos blockPos2 = player.blockPosition();
      if (value != blockPos2.getY()) {
         value = blockPos.getY() - player.blockPosition().getY();
         if (Math.abs(value) != 0) {
            return false;
         }
      }

      return true;
   }

   private static EndCrystal nearestThreateningCrystal(CrystalPlanner.Context context) {
      EndCrystal endCrystal = null;
      float index = 0.0F;
      AABB boundingBox = context.self().getBoundingBox().inflate(6.5);

      for (EndCrystal endCrystal2 : mc.level.getEntitiesOfClass(EndCrystal.class, boundingBox, Entity::isAlive)) {
         float explosionDamage = CrystalDamage.getExplosionDamage(Vec3.atCenterOf(endCrystal2.blockPosition()), context.self());
         if (explosionDamage > index) {
            index = explosionDamage;
            endCrystal = endCrystal2;
         }
      }

      return index >= 4.0F ? endCrystal : null;
   }

   private static boolean isSlow(BlockState blockState) {
      if (!blockState.is(Blocks.OBSIDIAN)) {
         if (!blockState.is(Blocks.SPAWNER)) {
            if (!blockState.is(Blocks.CRYING_OBSIDIAN)) {
               if (!blockState.is(Blocks.ENDER_CHEST)) {
                  if (!blockState.is(Blocks.ANCIENT_DEBRIS)) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   private static boolean withinPlace(CrystalPlanner.Context context, BlockPos blockPos) {
      return context.self().getEyePosition().distanceTo(Vec3.atCenterOf(blockPos)) <= context.placeRange();
   }

   private static boolean isWebbed(Player player) {
      if (player != null && mc.level != null) {
         BlockPos blockPos = player.blockPosition();
         BlockState blockState = mc.level.getBlockState(blockPos);
         if (!blockState.is(Blocks.COBWEB)) {
            ClientLevel level = mc.level;
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();
            BlockState blockState2 = level.getBlockState(BlockPos.containing(x, y, z));
            if (!blockState2.is(Blocks.COBWEB)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public record Context(
      Player self,
      Player target,
      float placeRange,
      float breakRange,
      float minTargetDamage,
      float maxSelfDamage,
      float minHealth,
      boolean canDig,
      boolean hasObsidian,
      boolean safetyFriends,
      boolean safetyResources,
      Set<Long> failedBases,
      Set<Long> ownObsidian,
      BlockPos stickyBase
   ) {
      public Player target() {
         return this.target;
      }

      public boolean safetyFriends() {
         return this.safetyFriends;
      }

      public boolean safetyResources() {
         return this.safetyResources;
      }

      public float placeRange() {
         return this.placeRange;
      }

      public BlockPos stickyBase() {
         return this.stickyBase;
      }

      public Player self() {
         return this.self;
      }

      public float maxSelfDamage() {
         return this.maxSelfDamage;
      }

      public boolean hasObsidian() {
         return this.hasObsidian;
      }

      public Set<Long> ownObsidian() {
         return this.ownObsidian;
      }

      public boolean canDig() {
         return this.canDig;
      }

      public float breakRange() {
         return this.breakRange;
      }

      public float minHealth() {
         return this.minHealth;
      }
   }

   public record DamageSnapshot(float target, float self, float splash, float friend) {
      public float self() {
         return this.self;
      }

      public float splash() {
         return this.splash;
      }

      public float friend() {
         return this.friend;
      }

      public float target() {
         return this.target;
      }
   }
}
