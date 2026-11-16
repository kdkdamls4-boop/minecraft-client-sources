package net.skycore.client.combat;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.combat.AntiBotModule;

public final class CrystalCandidates {
   private static int digCacheTick = Integer.MIN_VALUE;
   private static final Random AIM_RANDOM = new Random();
   private static final Minecraft MC = Minecraft.getInstance();
   private static final Map<BlockState, Float> DIG_TICK_CACHE = new HashMap<>();
   private static final int[][] OFFSETS = createOffsets();

   public static CrystalCandidates.Goal bestBreak(CrystalCandidates.Options options) {
      if (MC.player != null && MC.level != null) {
         LocalPlayer player = MC.player;
         Vec3 eyePosition = player.getEyePosition();
         var players = targets(options, player, eyePosition);
         if (players.isEmpty()) {
            return null;
         }

         var players2 = friends(player, eyePosition, options.targetRange() + 8.0);
         float health = options.antiSuicide ? Math.max(0.0F, player.getHealth() + player.getAbsorptionAmount() - 1.0F) : Float.MAX_VALUE;
         return selectBreak(players, players2, eyePosition, health, options);
      } else {
         return null;
      }
   }

   public static Vec3 crystalAim(BlockPos blockPos) {
      Vec3 position = Vec3.atCenterOf(blockPos);
      double computedValue = jitter(-0.03F, 0.03F);
      double computedValue2 = jitter(-0.03F, 0.03F);
      return position.add(computedValue, computedValue2, jitter(-0.03F, 0.03F));
   }

   private static float maxFriendDamage(Vec3 position, List<Player> players) {
      List<BlockPos> blockPoss = List.of();
      return maxFriendDamage(position, players, blockPoss, null);
   }

   public static boolean actionAllowedBeforeCooldown(CrystalCandidates.Goal goal, boolean condition) {
      if (goal != null) {
         if (condition) {
            return true;
         }

         CrystalCandidates.Action action = goal.action();
         if (action == CrystalCandidates.Action.DIG) {
            return true;
         }
      }

      return false;
   }

   public static CrystalCandidates.Goal bestPlacement(CrystalCandidates.Options options) {
      return best(options, false);
   }

   private static CrystalCandidates.Goal best(CrystalCandidates.Options options, boolean condition) {
      if (MC.player != null && MC.level != null) {
         LocalPlayer player = MC.player;
         Vec3 eyePosition = player.getEyePosition();
         var players = targets(options, player, eyePosition);
         if (players.isEmpty()) {
            return null;
         }

         var players2 = players.stream().sorted(Comparator.comparingDouble(player2 -> {
            return sourceEyeDistance(player, player2);
         })).limit(6L).toList();
         var players3 = friends(player, eyePosition, options.targetRange() + 8.0);
         float health = options.antiSuicide ? Math.max(0.0F, player.getHealth() + player.getAbsorptionAmount() - 1.0F) : Float.MAX_VALUE;
         if (condition) {
            CrystalCandidates.Goal goal = selectBreak(players, players3, eyePosition, health, options);
            if (goal != null) {
               return goal;
            }
         }

         byte flag = 0;

         for (int value = 0; value < 9; value++) {
            if (MC.player.getInventory().getItem(value).is(Items.END_CRYSTAL)) {
               flag = 1;
               break;
            }
         }

         if (flag == 0) {
            return null;
         }

         ExplosionDamage.beginPlanningPass();

         try {
            CrystalCandidates.Goal goal2 = null;
            CrystalCandidates.Goal goal3 = null;
            int value2 = 0;
            int value3 = 0;
            int obsidian = findObsidian();
            double distance = options.placeRange * options.placeRange;
            double distance2 = options.breakRange * options.breakRange;

            for (Player player3 : players2) {
               AABB boundingBox = options.predictTicks > 0 && !hasCobweb(player3.getBoundingBox())
                  ? InputPredictor.simulateOtherPlayer(player3, options.predictTicks).boundingBox()
                  : player3.getBoundingBox();
               BlockPos blockPos = BlockPos.containing((boundingBox.minX + boundingBox.maxX) * 0.5, boundingBox.minY, (boundingBox.minZ + boundingBox.maxZ) * 0.5);

               for (int[] intValues : OFFSETS) {
                  if (value2 >= 150) {
                     return goal2 != null ? goal2 : goal3;
                  }

                  BlockPos blockPos2 = blockPos.offset(intValues[0], intValues[1], intValues[2]);
                  BlockState blockState = MC.level.getBlockState(blockPos2);
                  int value4 = !blockState.is(Blocks.OBSIDIAN) && !blockState.is(Blocks.BEDROCK) ? 0 : 1;
                  int value5 = value4 == 0
                        && !blockState.canBeReplaced()
                        && options.allowDig
                        && options.allowObsidian
                        && obsidian >= 0
                        && mayExcavate(blockPos2, blockState, eyePosition, options.placeRange, options.failedBases())
                     ? 1
                     : 0;
                  int value6 = value4 != 0 || !options.allowObsidian || obsidian < 0 || !blockState.canBeReplaced() && value5 == 0 ? 0 : 1;
                  if (value4 != 0 || value6 != 0) {
                     Vec3 position = Vec3.atBottomCenterOf(blockPos2.above());
                     if (!(distanceToBoxSqr(eyePosition, new AABB(blockPos2)) > distance) && !(distanceToBoxSqr(eyePosition, blastReachBox(position)) > distance2)) {
                        BlockPos blockPos3 = blockPos2.above();
                        BlockState blockState2 = MC.level.getBlockState(blockPos3);
                        BlockPos blockPos4 = !blockState2.isAir() && options.allowDig && mayExcavate(blockPos3, blockState2, eyePosition, options.placeRange, options.failedBases()) ? blockPos3 : null;
                        if ((blockState2.isAir() || blockPos4 != null)
                           && (blockPos4 == null || !(distanceToBoxSqr(eyePosition, new AABB(blockPos4)) > distance))
                           && !occupied(crystalSpace(blockPos3))
                           && (value6 == 0 || value5 != 0 || !occupied(crystalSpace(blockPos2)))
                           && !(ExplosionDamage.maximumDamage(position, player3, boundingBox) < options.minDamage())) {
                           BlockPos blockPos5 = value6 != 0 ? blockPos2 : null;
                           ArrayList<BlockPos> blockPoss = new ArrayList(4);
                           if (blockPos4 != null) {
                              blockPoss.add(blockPos4);
                           }

                           if (value5 != 0) {
                              blockPoss.add(blockPos2);
                           }

                           int size = blockPoss.size();
                           float damage = candidateDamage(position, player3, boundingBox, blockPoss, blockPos5);
                           if (damage < options.minDamage && options.allowDig && goal2 == null && value3 < 16 && blockPoss.size() < 4) {
                              value3++;
                              findDigChain(position, boundingBox, eyePosition, distance, blockPoss, blockPos5, blockPos2, options.failedBases(), 4);
                              damage = candidateDamage(position, player3, boundingBox, blockPoss, blockPos5);
                           }

                           if (!(damage < options.minDamage)) {
                              CrystalCandidates.PlacementFace placementFace = value6 != 0 ? placementFace(blockPos2, eyePosition, options.placeRange, blockPoss) : null;
                              if (value6 == 0 || placementFace != null) {
                                 if (blockPoss.size() > size) {
                                    ArrayList elements = new ArrayList(blockPoss.subList(size, blockPoss.size()));
                                    elements.addAll(blockPoss.subList(0, size));
                                    blockPoss = elements;
                                 }

                                 value2++;
                                 float boundingBox2 = ExplosionDamage.damage(position, player, player.getBoundingBox(), blockPoss, blockPos5);
                                 if (!(boundingBox2 >= health) && (!(boundingBox2 > options.maxSelfDamage) || !(boundingBox2 >= damage))) {
                                    float damage2 = maxFriendDamage(position, players3);
                                    if (!(damage2 > options.maxSelfDamage) || !(damage2 >= damage)) {
                                       double x = blockPos2.getX() + 0.5 - (boundingBox.minX + boundingBox.maxX) * 0.5;
                                       double z = blockPos2.getZ() + 0.5 - (boundingBox.minZ + boundingBox.maxZ) * 0.5;
                                       double squareRoot = Math.sqrt(x * x + z * z);
                                       float computedValue = damage - boundingBox2 * 2.0F - damage2 * 2.0F - (float)(squareRoot * 3.0);
                                       if (value6 != 0) {
                                          computedValue -= 3.0F;
                                       }

                                       if (blockPos2.getY() + 1 > boundingBox.minY + 0.5 && (player3.onGround() || !(player3.getDeltaMovement().y > 0.08))) {
                                          computedValue -= 3.0F;
                                       }

                                       if (intValues[0] == 0 && intValues[2] == 0) {
                                          computedValue += 2.0F;
                                       }

                                       if (blockPos2.equals(options.stickyBase)) {
                                          computedValue += 4.0F;
                                       }

                                       if (!blockPoss.isEmpty()) {
                                          float index = 0.0F;

                                          for (BlockPos blockPos6 : blockPoss) {
                                             float progress = estimatedDigTicks(blockPos6);
                                             if (blockPos6.equals(options.currentDig())) {
                                                progress *= 1.0F - Mth.clamp(options.currentDigProgress(), 0.0F, 1.0F);
                                             }

                                             index += progress;
                                          }

                                          computedValue -= index * 0.1F;
                                          computedValue -= 3 * (blockPoss.size() - 1);
                                          if (options.currentDig() != null && blockPoss.contains(options.currentDig())) {
                                             computedValue += 8.0F;
                                          }
                                       }

                                       BlockPos blockPos7 = blockPoss.isEmpty() ? null : (BlockPos)blockPoss.get(0);
                                       CrystalCandidates.Goal goal4 = new CrystalCandidates.Goal(
                                          blockPos7 != null
                                             ? CrystalCandidates.Action.DIG
                                             : (value6 != 0 ? CrystalCandidates.Action.OBSIDIAN : CrystalCandidates.Action.CRYSTAL),
                                          blockPos2,
                                          null,
                                          placementFace == null ? null : placementFace.neighbor,
                                          placementFace == null ? null : placementFace.face,
                                          blockPos7,
                                          placementFace == null ? null : placementFace.aim,
                                          damage,
                                          boundingBox2,
                                          damage2,
                                          computedValue
                                       );
                                       if (blockPos7 == null) {
                                          if (goal2 == null || goal4.score > goal2.score) {
                                             goal2 = goal4;
                                          }
                                       } else if (goal3 == null || goal4.score > goal3.score) {
                                          goal3 = goal4;
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

            return goal2 != null ? goal2 : goal3;
         } finally {
            ExplosionDamage.endPlanningPass();
         }
      } else {
         return null;
      }
   }

   public static BlockPos stickyCandidate(BlockPos blockPos, BlockPos blockPos2, int value, int value2) {
      return blockPos != null ? blockPos : (blockPos2 != null && value >= value2 && value - value2 <= 20 ? blockPos2 : null);
   }

   private static List<Player> friends(Player player, Vec3 position, double distance) {
      var stream = StreamSupport.stream(MC.level.entitiesForRendering().spliterator(), false);
      Predicate<net.minecraft.world.entity.Entity> predicate = entity -> {
         return entity instanceof Player;
      };
      var stream2 = stream.filter(predicate).map(entity2 -> {
         return (Player)entity2;
      });
      var gameProfile = stream2.filter(player2 -> {
         if (player2 != player) {
            if (player2.isAlive()) {
               if (!player2.isDeadOrDying() && !player2.isSpectator()) {
                  if (FriendManager.INSTANCE.isFriend(player2.getGameProfile().name())) {
                     return true;
                  }
               }
            }
         }

         return false;
      }).filter(player3 -> {
         return sourceEyeDistance(player, player3) <= distance;
      });
      return gameProfile.map(player4 -> {
         return (Player)player4;
      }).toList();
   }

   private static List<Player> targets(CrystalCandidates.Options options, Player player, Vec3 position) {
      var entitys = MC.level.entitiesForRendering();
      var stream = StreamSupport.stream(entitys.spliterator(), false);
      Predicate<net.minecraft.world.entity.Entity> predicate = entity -> {
         return entity instanceof Player;
      };
      var stream2 = stream.filter(predicate);
      Function<net.minecraft.world.entity.Entity, net.minecraft.world.entity.player.Player> function = entity2 -> {
         return (Player)entity2;
      };
      var gameProfile = stream2.<Player>map(function).filter(player2 -> {
         if (player2 != player && player2.isAlive()) {
            if (!player2.isDeadOrDying() && !player2.isSpectator()) {
               return true;
            }
         }

         return false;
      }).filter(player3 -> {
         FriendManager iNSTANCE = FriendManager.INSTANCE;
         GameProfile gameProfile2 = player3.getGameProfile();
         String text = gameProfile2.name();
         return !iNSTANCE.isFriend(text);
      });
      Predicate<net.minecraft.world.entity.player.Player> predicate2 = player4 -> {
         return !AntiBotModule.isBot(player4);
      };
      var stream3 = gameProfile.filter(predicate2);
      Predicate<net.minecraft.world.entity.Entity> predicate3 = entity3 -> {
         double value = sourceEyeDistance(player, entity3);
         return value <= options.targetRange;
      };
      var stream4 = stream3.filter(predicate3).map(player5 -> {
         return (Player)player5;
      });
      return stream4.toList();
   }

   private static void findDigChain(
      Vec3 position, AABB bounds, Vec3 position2, double index, List<BlockPos> blockPoss, BlockPos blockPos, BlockPos blockPos2, Set<BlockPos> blockPoss2, int index2
   ) {
      for (int value = 0; value < 9 && blockPoss.size() < index2; value++) {
         Vec3 center = value == 8
            ? bounds.getCenter()
            : new Vec3(
               (value & 1) == 0 ? bounds.minX + bounds.getXsize() * 0.15 : bounds.maxX - bounds.getXsize() * 0.15,
               (value & 2) == 0 ? bounds.minY + bounds.getYsize() * 0.05 : bounds.maxY - bounds.getYsize() * 0.05,
               (value & 4) == 0 ? bounds.minZ + bounds.getZsize() * 0.15 : bounds.maxZ - bounds.getZsize() * 0.15
            );
         if (blockPos == null || !new AABB(blockPos).deflate(1.0E-4).clip(position, center).isPresent()) {
            Vec3 position3 = center;

            for (int value2 = 0; value2 < 3 && blockPoss.size() < index2; value2++) {
               BlockPos blockPos3 = WorldCache.rayBlock(position3, position);
               if (blockPos3 == null) {
                  break;
               }

               if (!blockPoss.contains(blockPos3) && !blockPos3.equals(blockPos2)) {
                  if (!mayExcavate(blockPos3, WorldCache.blockState(blockPos3), position2, Math.sqrt(index), blockPoss2)) {
                     break;
                  }

                  blockPoss.add(blockPos3.immutable());
               }

               Vec3 position4 = ExplosionDamage.afterVoxel(position3, position, blockPos3);
               if (position4 == null) {
                  break;
               }

               position3 = position4;
            }
         }
      }
   }

   private static double jitter(float value, float value2) {
      return value + AIM_RANDOM.nextFloat() * (value2 - value);
   }

   private static boolean breakable(BlockPos blockPos, BlockState blockState) {
      return blockState.getBlock().defaultDestroyTime() >= 0.0F;
   }

   public static float sourceEyeDistance(Player player, Entity entity) {
      Vec3 eyePosition = player.getEyePosition();
      float bbWidth = entity.getBbWidth() / 2.0F;
      double x = player.getX();
      double x2 = x - entity.getX();
      double computedValue = -bbWidth;
      double value = bbWidth;
      float clampedValue = (float)Mth.clamp(x2, computedValue, value);
      double y = eyePosition.y - entity.getY();
      float bbHeight = (float)Mth.clamp(y, 0.0, entity.getBbHeight());
      double z = player.getZ();
      double z2 = z - entity.getZ();
      double computedValue2 = -bbWidth;
      double value2 = bbWidth;
      float clampedValue2 = (float)Mth.clamp(z2, computedValue2, value2);
      Vec3 position = entity.position();
      double value3 = clampedValue;
      double value4 = bbHeight;
      double value5 = clampedValue2;
      return (float)eyePosition.distanceTo(position.add(value3, value4, value5));
   }

   public static Rotation combatRotation(Vec3 position, Vec3 position2) {
      Rotation rotation = aimRotation(position, position2);
      float yaw = rotation.getYaw();
      ThreadLocalRandom threadLocalRandom = ThreadLocalRandom.current();
      yaw += (float)threadLocalRandom.nextDouble();
      float pitch = rotation.getPitch();
      ThreadLocalRandom threadLocalRandom2 = ThreadLocalRandom.current();
      return new Rotation(yaw, pitch + (float)threadLocalRandom2.nextDouble());
   }

   public static CrystalCandidates.Goal best(CrystalCandidates.Options options) {
      return best(options, true);
   }

   public static boolean protectedPlayerColumn(BlockPos blockPos, BlockPos blockPos2) {
      int value = 0;
      value = (byte)0;
      value = blockPos.getX();
      if (value == blockPos2.getX()) {
         if (blockPos.getZ() == blockPos2.getZ()) {
            value = blockPos2.getY();
            value -= blockPos.getY();
            if (Math.abs(value) <= 1) {
               return true;
            }
         }
      }

      return false;
   }

   public static AABB blastReachBox(Vec3 position) {
      double x = position.x - 1.0;
      double z = position.z - 1.0;
      double x2 = position.x + 1.0;
      double y = position.y + 2.0;
      double z2 = position.z + 1.0;
      return new AABB(x, position.y, z, x2, y, z2);
   }

   public static boolean sameClientTick(int value, int value2) {
      return value2 == value || value2 == value + 1;
   }

   private static boolean occupied(AABB bounds) {
      ClientLevel level = MC.level;
      Predicate<net.minecraft.world.entity.Entity> predicate = entity -> {
         return entity.isAlive() && !entity.isSpectator();
      };
      return !level.getEntitiesOfClass(Entity.class, bounds, predicate).isEmpty();
   }

   public static CrystalCandidates.PlacementFace placementFace(BlockPos blockPos, Vec3 position, double value) {
      return placementFace(blockPos, position, value, List.of());
   }

   private static float estimatedDigTicks(BlockPos blockPos) {
      int value = MC.player.tickCount;
      if (value != digCacheTick) {
         DIG_TICK_CACHE.clear();
         digCacheTick = value;
      }

      BlockState blockState = MC.level.getBlockState(blockPos);
      Float value2 = DIG_TICK_CACHE.get(blockState);
      if (value2 != null) {
         return value2;
      }

      float destroyProgress = blockState.getDestroyProgress(MC.player, MC.level, blockPos);
      if (!(destroyProgress > 0.0F)) {
         return Float.MAX_VALUE;
      }

      ItemStack mainHandItem = MC.player.getMainHandItem();
      float value3 = MiningTools.rate(mainHandItem, blockState);
      int value4 = MiningTools.bestHotbarSlot(blockState);
      ItemStack item = value4 < 0 ? mainHandItem : MC.player.getInventory().getItem(value4);
      float value5 = MiningTools.rate(item, blockState);
      if (value3 > 0.0F && value5 > 0.0F) {
         destroyProgress *= value5 / value3;
      }

      float result = 1.0F / destroyProgress;
      DIG_TICK_CACHE.put(blockState, result);
      return result;
   }

   public static BlockPos stickyBaseAfterCommit(BlockPos blockPos, CrystalCandidates.Goal goal, boolean condition) {
      if (condition && goal != null) {
         return switch (goal.action()) {
            case BREAK -> blockPos;
            case CRYSTAL -> null;
            case OBSIDIAN, DIG -> goal.base();
         };
      } else {
         return blockPos;
      }
   }

   public static double distanceToBoxSqr(Vec3 position, AABB bounds) {
      double clampedValue = clamp(position.x, bounds.minX, bounds.maxX);
      double clampedValue2 = clamp(position.y, bounds.minY, bounds.maxY);
      return position.distanceToSqr(clampedValue, clampedValue2, clamp(position.z, bounds.minZ, bounds.maxZ));
   }

   public static AABB crystalSpace(BlockPos blockPos) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1;
      return new AABB(x, y, z, x2, blockPos.getY() + 2, blockPos.getZ() + 1);
   }

   private static CrystalCandidates.Goal selectBreak(List<Player> players, List<Player> players2, Vec3 position, float value, CrystalCandidates.Options options) {
      LocalPlayer player = MC.player;
      CrystalCandidates.Goal goal = null;
      double distance = options.breakRange * options.breakRange;

      for (Entity entity : MC.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal endCrystal) {
            AABB boundingBox = endCrystal.getBoundingBox();
            if (!(distanceToBoxSqr(position, boundingBox) > distance)) {
               Vec3 position2 = endCrystal.position();
               float damage = 0.0F;

               for (Player player2 : players) {
                  damage = Math.max(damage, ExplosionDamage.damage(position2, player2));
               }

               if (!(damage < options.minDamage)) {
                  float damage2 = ExplosionDamage.damage(position2, player);
                  if (!(damage2 >= value) && (!(damage2 > options.maxSelfDamage) || !(damage2 >= damage))) {
                     float damage3 = maxFriendDamage(position2, players2);
                     if (!(damage3 > options.maxSelfDamage) || !(damage3 >= damage)) {
                        float computedValue = damage - damage2 * 2.0F - damage3 * 2.0F;
                        if (goal == null || computedValue > goal.score) {
                           goal = new CrystalCandidates.Goal(
                              CrystalCandidates.Action.BREAK, endCrystal.blockPosition(), endCrystal, null, null, null, null, damage, damage2, damage3, computedValue
                           );
                        }
                     }
                  }
               }
            }
         }
      }

      return goal == null
         ? null
         : new CrystalCandidates.Goal(
            CrystalCandidates.Action.BREAK,
            goal.base(),
            goal.crystal(),
            null,
            null,
            null,
            breakAim(goal.crystal(), position, options.breakRange),
            goal.enemyDamage(),
            goal.selfDamage(),
            goal.friendDamage(),
            goal.score()
         );
   }

   public static List<Player> eligibleTargets(CrystalCandidates.Options options) {
      if (MC.player != null) {
         if (MC.level != null) {
            LocalPlayer player = MC.player;
            Vec3 eyePosition = MC.player.getEyePosition();
            return targets(options, player, eyePosition);
         }
      }

      return List.of();
   }

   public static Vec3 faceAim(BlockState blockState, BlockPos blockPos, Direction direction) {
      VoxelShape collisionShape = blockState.getCollisionShape(MC.level, blockPos);
      AABB bounds = collisionShape.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : collisionShape.bounds();
      double x = blockPos.getX() + (bounds.minX + bounds.maxX) * 0.5;
      double y = blockPos.getY() + (bounds.minY + bounds.maxY) * 0.5;
      double z = blockPos.getZ() + (bounds.minZ + bounds.maxZ) * 0.5;
      switch (direction) {
         case UP:
            y = blockPos.getY() + bounds.maxY;
            break;
         case DOWN:
            y = blockPos.getY() + bounds.minY;
            break;
         case NORTH:
            z = blockPos.getZ() + bounds.minZ;
            break;
         case SOUTH:
            z = blockPos.getZ() + bounds.maxZ;
            break;
         case WEST:
            x = blockPos.getX() + bounds.minX;
            break;
         case EAST:
            x = blockPos.getX() + bounds.maxX;
      }
      return switch (direction.getAxis()) {
         case X -> new Vec3(x, y + jitter(-0.03F, 0.03F), z + jitter(-0.03F, 0.03F));
         case Y -> new Vec3(x + jitter(-0.03F, 0.03F), y, z + jitter(-0.03F, 0.03F));
         case Z -> new Vec3(x + jitter(-0.03F, 0.03F), y + jitter(-0.03F, 0.03F), z);
      };
   }

   public static boolean cooldownElapsed(long timestamp, long timestamp2, int value) {
      return timestamp - timestamp2 > value * 50L;
   }

   static int[][] createOffsets() {
      long[] longValues = new long[294];
      int value = 0;

      for (int index = -3; index <= 3; index++) {
         for (int index2 = -3; index2 <= 3; index2++) {
            for (int value2 = 1; value2 >= -4; value2--) {
               int computedValue = (index * index + index2 * index2) * 64 + Math.abs(value2 + 1) * 8 + (1 - value2);
               int computedValue2 = index + 8 << 16 | value2 + 8 << 8 | index2 + 8;
               longValues[value++] = (long)computedValue << 32 | computedValue2;
            }
         }
      }

      Arrays.sort(longValues);
      int[][] intArrayValues = new int[longValues.length][3];

      for (int value3 = 0; value3 < longValues.length; value3++) {
         int value4 = (int)(longValues[value3] & 4294967295L);
         intArrayValues[value3] = new int[]{(value4 >>> 16 & 0xFF) - 8, (value4 >>> 8 & 0xFF) - 8, (value4 & 0xFF) - 8};
      }

      return intArrayValues;
   }

   private static float maxFriendDamage(Vec3 position, List<Player> players, List<BlockPos> blockPoss, BlockPos blockPos) {
      float damage = 0.0F;

      for (Player player : players) {
         damage = Math.max(damage, ExplosionDamage.damage(position, player, player.getBoundingBox(), blockPoss, blockPos));
      }

      return damage;
   }

   public static boolean hasTargets(CrystalCandidates.Options options) {
      if (MC.player != null) {
         if (MC.level != null) {
            LocalPlayer player = MC.player;
            Vec3 eyePosition = MC.player.getEyePosition();
            if (!targets(options, player, eyePosition).isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private static boolean mayExcavate(BlockPos blockPos, BlockState blockState, Vec3 position, double value, Set<BlockPos> blockPoss) {
      if (!blockState.isAir()) {
         if (!blockState.is(Blocks.COBWEB)) {
            if (!blockState.is(Blocks.SWEET_BERRY_BUSH)) {
               if (!(blockState.getBlock() instanceof LiquidBlock)) {
                  if (!protectedPlayerColumn(MC.player.blockPosition(), blockPos) && !blockPoss.contains(blockPos)) {
                     if (distanceToBoxSqr(position, new AABB(blockPos)) <= value * value && breakable(blockPos, blockState)) {
                        return true;
                     }
                  }
               }
            }
         }
      }

      return false;
   }

   public static Rotation aimRotation(Vec3 position, Vec3 position2) {
      Vec3 position3 = position2.subtract(position);
      double x = Math.hypot(position3.x, position3.z);
      float x2 = (float)(Mth.atan2(position3.z, position3.x) * 180.0 / Math.PI) - 90.0F;
      float y = (float)(-Mth.atan2(position3.y, x) * 180.0 / Math.PI);
      return new Rotation(Mth.wrapDegrees(x2), Mth.clamp(y, -90.0F, 90.0F));
   }

   private static boolean hasCobweb(AABB bounds) {
      for (int roundedDown = Mth.floor(bounds.minX); roundedDown <= Mth.floor(bounds.maxX); roundedDown++) {
         for (int roundedDown2 = Mth.floor(bounds.minY); roundedDown2 <= Mth.floor(bounds.maxY); roundedDown2++) {
            for (int roundedDown3 = Mth.floor(bounds.minZ); roundedDown3 <= Mth.floor(bounds.maxZ); roundedDown3++) {
               if (MC.level.getBlockState(new BlockPos(roundedDown, roundedDown2, roundedDown3)).is(Blocks.COBWEB)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   public static Vec3 breakAim(EndCrystal endCrystal, Vec3 position, double value) {
      Vec3 position2 = endCrystal.position().add(jitter(-0.03F, 0.03F), 0.5 + jitter(-0.03F, 0.03F), jitter(-0.03F, 0.03F));
      if (clearBlockLine(position, position2)) {
         return position2;
      }

      AABB boundingBox = endCrystal.getBoundingBox();
      Vec3 position3 = null;
      double mAX_VALUE = Double.MAX_VALUE;

      for (double index : new double[]{0.5, 0.05, 0.95}) {
         double index2 = Mth.lerp(index + jitter(-0.04F, 0.04F), boundingBox.minX, boundingBox.maxX);

         for (double index3 : new double[]{0.5, 0.05, 0.95}) {
            double index4 = Mth.lerp(index3 + jitter(-0.04F, 0.04F), boundingBox.minY, boundingBox.maxY);

            for (double index5 : new double[]{0.5, 0.05, 0.95}) {
               Vec3 position4 = new Vec3(index2, index4, Mth.lerp(index5 + jitter(-0.04F, 0.04F), boundingBox.minZ, boundingBox.maxZ));
               double distanceSquared = position4.distanceToSqr(position2);
               if (position.distanceToSqr(position4) <= value * value && distanceSquared < mAX_VALUE && clearBlockLine(position, position4)) {
                  mAX_VALUE = distanceSquared;
                  position3 = position4;
               }
            }
         }
      }

      return position3 == null ? position2 : position3;
   }

   public static CrystalCandidates.PlacementFace placementFace(BlockPos blockPos, Vec3 position, double index, List<BlockPos> blockPoss) {
      CrystalCandidates.PlacementFace placementFace = null;
      double index2 = index * index;

      for (Direction direction : Direction.values()) {
         BlockPos opposite = blockPos.relative(direction.getOpposite());
         if (!blockPoss.contains(opposite)) {
            BlockState blockState = MC.level.getBlockState(opposite);
            if (!blockState.canBeReplaced() && blockState.isFaceSturdy(MC.level, opposite, direction)) {
               Vec3 position2 = faceAim(blockState, opposite, direction);
               if (!(position.subtract(position2).dot(new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ())) <= 0.0)) {
                  double distanceSquared = position.distanceToSqr(position2);
                  if (distanceSquared < index2) {
                     index2 = distanceSquared;
                     placementFace = new CrystalCandidates.PlacementFace(opposite, direction, position2);
                  }
               }
            }
         }
      }

      return placementFace;
   }

   private CrystalCandidates() {
   }

   public static boolean rayIntersectsBox(Vec3 position, Vec3 position2, double value, AABB bounds) {
      if (!bounds.contains(position)) {
         Vec3 position3 = position2.scale(value);
         Vec3 position4 = position.add(position3);
         var optional = bounds.clip(position, position4);
         if (!optional.isPresent()) {
            return false;
         }
      }

      return true;
   }

   private static double clamp(double value, double value2, double value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private static float candidateDamage(Vec3 position, Player player, AABB bounds, List<BlockPos> blockPoss, BlockPos blockPos) {
      float value = ExplosionDamage.damage(position, player, bounds, blockPoss, blockPos);
      float result;
      if (bounds != player.getBoundingBox() && (!blockPoss.isEmpty() || blockPos != null)) {
         AABB boundingBox = player.getBoundingBox();
         float value2 = ExplosionDamage.damage(position, player, boundingBox, blockPoss, blockPos);
         result = Math.min(value, value2);
      } else {
         result = value;
      }

      return result;
   }

   private static boolean clearBlockLine(Vec3 position, Vec3 position2) {
      return MC.level.clip(new ClipContext(position, position2, Block.COLLIDER, Fluid.NONE, MC.player)).getType() == Type.MISS;
   }

   public static boolean retainCurrentDig(float value, int value2, boolean condition) {
      return condition && (value >= 0.2F || value2 < 6);
   }

   private static int findObsidian() {
      if (MC.player == null) {
         return -1;
      }

      for (int value = 0; value < 9; value++) {
         Inventory inventory = MC.player.getInventory();
         ItemStack item = inventory.getItem(value);
         if (item.is(Blocks.OBSIDIAN.asItem())) {
            return value;
         }
      }

      return -1;
   }

   public enum Action {
      BREAK,
      CRYSTAL,
      OBSIDIAN,
      DIG;
   }

   public record Goal(
      CrystalCandidates.Action action,
      BlockPos base,
      EndCrystal crystal,
      BlockPos neighbor,
      Direction face,
      BlockPos dig,
      Vec3 aim,
      float enemyDamage,
      float selfDamage,
      float friendDamage,
      float score
   ) {
      public EndCrystal crystal() {
         return this.crystal;
      }

      public float friendDamage() {
         return this.friendDamage;
      }

      public Direction face() {
         return this.face;
      }

      public float score() {
         return this.score;
      }

      public BlockPos neighbor() {
         return this.neighbor;
      }

      public CrystalCandidates.Action action() {
         return this.action;
      }

      public float selfDamage() {
         return this.selfDamage;
      }

      public Vec3 aim() {
         return this.aim;
      }

      public BlockPos base() {
         return this.base;
      }

      public BlockPos dig() {
         return this.dig;
      }

      public float enemyDamage() {
         return this.enemyDamage;
      }
   }

   public record Options(
      double targetRange,
      double placeRange,
      double breakRange,
      double minDamage,
      double maxSelfDamage,
      int predictTicks,
      boolean allowObsidian,
      boolean allowDig,
      boolean antiSuicide,
      BlockPos stickyBase,
      Set<BlockPos> failedBases,
      BlockPos currentDig,
      float currentDigProgress
   ) {
      public double minDamage() {
         return this.minDamage;
      }

      public double placeRange() {
         return this.placeRange;
      }

      public boolean allowDig() {
         return this.allowDig;
      }

      public double breakRange() {
         return this.breakRange;
      }

      public double maxSelfDamage() {
         return this.maxSelfDamage;
      }

      public float currentDigProgress() {
         return this.currentDigProgress;
      }

      public boolean allowObsidian() {
         return this.allowObsidian;
      }

      public double targetRange() {
         return this.targetRange;
      }

      public BlockPos stickyBase() {
         return this.stickyBase;
      }

      public boolean antiSuicide() {
         return this.antiSuicide;
      }

      public int predictTicks() {
         return this.predictTicks;
      }

      public Set<BlockPos> failedBases() {
         return this.failedBases;
      }

      public BlockPos currentDig() {
         return this.currentDig;
      }
   }

   public record PlacementFace(BlockPos neighbor, Direction face, Vec3 aim) {
      public Vec3 aim() {
         return this.aim;
      }

      public Direction face() {
         return this.face;
      }

      public BlockPos neighbor() {
         return this.neighbor;
      }
   }
}
