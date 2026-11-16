package net.skycore.client.combat;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.module.combat.AutoWebTrapModule;
import net.skycore.client.util.MiningPlanOptions;
import net.skycore.client.util.EntityExposureKey;
import net.skycore.client.util.BlockAimTarget;
import net.skycore.client.util.EntityCollisionSnapshot;
import net.skycore.client.util.EfficiencyHelper;
import net.skycore.client.util.FirstDigHelper;
import net.skycore.client.util.InitializeHelper;
import net.skycore.client.util.VisibleHelper;

public final class CrystalSimulation {
   private static Int2IntOpenHashMap protectionCache;
   private static int[] candidateOffsetIndices;
   private static CollisionContext collisionContext;
   private static EquipmentSlot[] armorSlots;
   private static boolean ignoreTerrain;
   private static double[] exposureSampleFractions;
   private static Map<EntityExposureKey, Float> exposureCache;
   private static int cacheTick;

   public static double distanceSquared(Vec3 position, AABB bounds) {
      double clampedValue = Mth.clamp(position.x, bounds.minX, bounds.maxX);
      double clampedValue2 = Mth.clamp(position.y, bounds.minY, bounds.maxY);
      return position.distanceToSqr(clampedValue, clampedValue2, Mth.clamp(position.z, bounds.minZ, bounds.maxZ));
   }

   public static BlockAimTarget placementFace(BlockPos blockPos, Vec3 position, float value) {
      return placementFace(blockPos, position, value, (List)null);
   }

   public static Vec3 jitterFace(Vec3 position, Direction direction) {
      Axis axis = direction.getAxis();
      Vec3 position2;
      double x = axis == Axis.X ? position.x : position.x + jitter();
      double intermediate;
      if (axis == Axis.Y) {
         intermediate = position.y;
      } else {
         intermediate = position.y + jitter();
      }

      double intermediate2;
      if (axis == Axis.Z) {
         intermediate2 = position.z;
      } else {
         intermediate2 = position.z + jitter();
      }

      position2 = new Vec3(x, intermediate, intermediate2);
      return position2;
   }

   public static float damageWithVirtualSupport(Vec3 position, LivingEntity entity, AABB bounds, BlockPos blockPos, List<BlockPos> blockPoss) {
      if (entity != null && bounds != null && blockPos != null) {
         var objects = blockPoss == null ? List.of() : blockPoss;
         AABB bounds2 = new AABB(blockPos);
         AABB bounds3 = bounds2.deflate(1.0E-4);
         return damage(position, entity, bounds, exposure(position, bounds, bounds3, objects));
      } else {
         return 0.0F;
      }
   }

   public static Vec3 breakAim(Entity entity, Vec3 position, float value) {
      Vec3 position2 = entity.position().add(jitter(), 0.5 + jitter(), jitter());
      if (VisibleHelper.visible(position2)) {
         return position2;
      }

      AABB boundingBox = entity.getBoundingBox();
      double distance = value * value;
      Vec3 position3 = null;
      double mAX_VALUE = Double.MAX_VALUE;

      for (double index : exposureSampleFractions) {
         double index2 = Mth.lerp(index + sampleJitter(), boundingBox.minX, boundingBox.maxX);

         for (double index3 : exposureSampleFractions) {
            double index4 = Mth.lerp(index3 + sampleJitter(), boundingBox.minY, boundingBox.maxY);

            for (double index5 : exposureSampleFractions) {
               Vec3 position4 = new Vec3(index2, index4, Mth.lerp(index5 + sampleJitter(), boundingBox.minZ, boundingBox.maxZ));
               if (!(position.distanceToSqr(position4) > distance)) {
                  double distanceSquared = position4.distanceToSqr(position2);
                  if (!(distanceSquared >= mAX_VALUE) && VisibleHelper.visible(position4)) {
                     mAX_VALUE = distanceSquared;
                     position3 = position4;
                  }
               }
            }
         }
      }

      return position3 == null ? position2 : position3;
   }

   private static float exposure(Vec3 position, AABB bounds) {
      AABB bounds2 = (AABB)null;
      var objects = List.of();
      return exposure(position, bounds, bounds2, objects);
   }

   private static float resistance(LivingEntity entity, float result) {
      MobEffectInstance effect = entity.getEffect(MobEffects.RESISTANCE);
      if (effect == null) {
         return result;
      }

      float amplifier = Mth.clamp((effect.getAmplifier() + 1) * 0.2F, 0.0F, 1.0F);
      return result * (1.0F - amplifier);
   }

   public static boolean supported(BlockPos blockPos) {
      BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
      return isSupport(blockState);
   }

   private static Vec3 digSample(AABB bounds, int value) {
      if (value >= 8) {
         return bounds.getCenter();
      }

      double computedValue = (bounds.maxX - bounds.minX) * 0.15;
      double computedValue2 = (bounds.maxY - bounds.minY) * 0.05;
      double computedValue3 = (bounds.maxZ - bounds.minZ) * 0.15;
      Vec3 position;
      double value2;
      if ((value & 1) == 0) {
         value2 = bounds.minX + computedValue;
      } else {
         value2 = bounds.maxX - computedValue;
      }

      double value3;
      if ((value & 2) == 0) {
         value3 = bounds.minY + computedValue2;
      } else {
         value3 = bounds.maxY - computedValue2;
      }

      double value4;
      if ((value & 4) == 0) {
         value4 = bounds.minZ + computedValue3;
      } else {
         value4 = bounds.maxZ - computedValue3;
      }

      position = new Vec3(value2, value3, value4);
      return position;
   }

   public static boolean occupied(BlockPos blockPos) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1.0;
      double y2 = blockPos.getY() + 2.0;
      double z2 = blockPos.getZ() + 1.0;
      AABB bounds = new AABB(x, y, z, x2, y2, z2);
      Minecraft instance = Minecraft.getInstance();
      ClientLevel level = instance.level;
      Predicate<net.minecraft.world.entity.Entity> predicate = CrystalSimulation::blockingEntity;
      return !level.getEntitiesOfClass(Entity.class, bounds, predicate).isEmpty();
   }

   private static int[] candidateOffsets() {
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
      int[] intValues = new int[longValues.length * 3];

      for (int value3 = 0; value3 < longValues.length; value3++) {
         int value4 = (int)(longValues[value3] & 4294967295L);
         intValues[value3 * 3] = (value4 >> 16 & 0xFF) - 8;
         intValues[value3 * 3 + 1] = (value4 >> 8 & 0xFF) - 8;
         intValues[value3 * 3 + 2] = (value4 & 0xFF) - 8;
      }

      return intValues;
   }

   public static float digTicks(BlockPos blockPos) {
      return EfficiencyHelper.ticks(blockPos);
   }

   public static void initialize() {
      candidateOffsetIndices = candidateOffsets();
      double[] doubleValues = new double[]{0.5, 0.05, 0.95};
      exposureSampleFractions = doubleValues;
      EquipmentSlot[] equipmentSlotValues = new EquipmentSlot[4];
      equipmentSlotValues[0] = EquipmentSlot.HEAD;
      equipmentSlotValues[1] = EquipmentSlot.CHEST;
      equipmentSlotValues[2] = EquipmentSlot.LEGS;
      equipmentSlotValues[3] = EquipmentSlot.FEET;
      armorSlots = equipmentSlotValues;
      CollisionContext collisionContext2 = CollisionContext.empty();
      collisionContext = collisionContext2;
      protectionCache = new Int2IntOpenHashMap();
      cacheTick = Integer.MIN_VALUE;
      protectionCache.defaultReturnValue(-1);
      exposureCache = new HashMap<>();
      ignoreTerrain = false;
      new LinkedHashMap();
      new LinkedHashMap();
   }

   public static float damage(Vec3 position, LivingEntity entity) {
      float damage;
      if (entity == null) {
         damage = 0.0F;
      } else {
         AABB boundingBox = entity.getBoundingBox();
         damage = damage(position, entity, boundingBox);
      }

      return damage;
   }

   public static boolean freeCell(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      BlockState blockState = instance.level.getBlockState(blockPos);
      if (blockState.isAir()) {
         if (!occupied(blockPos)) {
            return true;
         }
      }

      return false;
   }

   public static boolean mineable(BlockState blockState) {
      if (blockState.is(BlockTags.LEAVES)) {
         return false;
      }

      if (blockState.is(Blocks.COBWEB)) {
         if (AutoWebTrapModule.INSTANCE.isEnabled()) {
            return false;
         }
      }

      if (breakable(blockState)) {
         if (!blockState.is(Blocks.SWEET_BERRY_BUSH)) {
            return true;
         }
      }

      return false;
   }

   private static boolean blockingEntity(Entity entity) {
      return entity.isAlive() && !entity.isSpectator();
   }

   public static float damageWithDig(Vec3 position, LivingEntity entity, AABB bounds, List<BlockPos> blockPoss) {
      if (entity == null || bounds == null) {
         return 0.0F;
      }

      if (blockPoss != null && !blockPoss.isEmpty()) {
         return damage(position, entity, bounds, exposure(position, bounds, null, blockPoss));
      }

      return damage(position, entity, bounds);
   }

   private static float digCost(List elements, MiningPlanOptions miningPlanOptions) {
      float index = 0.0F;

      for (int value = 0; value < elements.size(); value++) {
         BlockPos blockPos = (BlockPos)elements.get(value);
         float progress = digTicks(blockPos);
         if (miningPlanOptions != null && blockPos.equals(miningPlanOptions.currentBlock)) {
            progress *= 1.0F - Mth.clamp(miningPlanOptions.progress, 0.0F, 1.0F);
         }

         index += progress;
      }

      return index;
   }

   public static BlockPos standingBlock(LivingEntity entity) {
      if (entity == null) {
         return BlockPos.ZERO;
      }

      double x = entity.getX();
      BlockPos y = BlockPos.containing(x, entity.getY() - 0.2, entity.getZ());
      if (Minecraft.getInstance().level != null) {
         if (Minecraft.getInstance().level.getBlockState(y).isAir()) {
            y = y.below();
         }
      }

      return y;
   }

   public static double blastDistanceSquared(Vec3 position, Vec3 position2) {
      double x = position2.x - 1.0;
      double z = position2.z - 1.0;
      double x2 = position2.x + 1.0;
      AABB bounds = new AABB(x, position2.y, z, x2, position2.y + 2.0, position2.z + 1.0);
      return distanceSquared(position, bounds);
   }

   private static float planningExposure(Vec3 position, AABB bounds, AABB bounds2, List elements, MiningPlanOptions miningPlanOptions) {
      if (miningPlanOptions != null && miningPlanOptions.fastExposure) {
         int value = 0;

         for (int value2 = 0; value2 < 9; value2++) {
            Vec3 position2 = digSample(bounds, value2);
            if (clearRay(position2, position, bounds2, elements)) {
               value++;
            }
         }

         return value / 9.0F;
      } else {
         return exposure(position, bounds, bounds2, elements);
      }
   }

   public static FirstDigHelper bestPlacement(
      List<EntityCollisionSnapshot> entityCollisionSnapshots, Vec3 position, float value, float value2, float value3, float value4, boolean condition, float value5, List elements, MiningPlanOptions miningPlanOptions
   ) {
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && !entityCollisionSnapshots.isEmpty()) {
         exposureCache.clear();
         ignoreTerrain = true;
         AABB boundingBox = Minecraft.getInstance().player.getBoundingBox();
         FirstDigHelper firstDigHelper = null;
         FirstDigHelper firstDigHelper2 = null;
         FirstDigHelper firstDigHelper3 = null;
         FirstDigHelper firstDigHelper4 = null;
         float computedValue = -Float.MAX_VALUE;
         float computedValue2 = -Float.MAX_VALUE;
         float mAX_VALUE = Float.MAX_VALUE;
         float computedValue3 = -Float.MAX_VALUE;
         float computedValue4 = -Float.MAX_VALUE;
         double computedValue5 = value * value;
         double computedValue6 = value2 * value2;
         int value6 = miningPlanOptions != null && miningPlanOptions.enabled ? 1 : 0;
         int value7 = value6 != 0 ? miningPlanOptions.maxAttempts : 0;
         int value8 = 0;
         ArrayList elements2 = new ArrayList(4);
         ArrayList elements3 = new ArrayList(4);

         for (EntityCollisionSnapshot entityCollisionSnapshot : entityCollisionSnapshots) {
            BlockPos blockPos = BlockPos.containing(entityCollisionSnapshot.feet.x, entityCollisionSnapshot.feet.y, entityCollisionSnapshot.feet.z);
            int deltaMovement = !entityCollisionSnapshot.entity.onGround() && entityCollisionSnapshot.entity.getDeltaMovement().y > 0.08 ? 1 : 0;
            AABB boundingBox2 = entityCollisionSnapshot.entity.getBoundingBox();
            int instance = standingBlock(Minecraft.getInstance().player).getY() == standingBlock(entityCollisionSnapshot.entity).getY() ? 1 : 0;
            int instance2 = standingBlock(Minecraft.getInstance().player).getY() > standingBlock(entityCollisionSnapshot.entity).getY() ? 1 : 0;

            for (int offsetIndex = 0; offsetIndex + 2 < candidateOffsetIndices.length; offsetIndex += 3) {
               int value9 = candidateOffsetIndices[offsetIndex];
               int computedValue7 = candidateOffsetIndices[offsetIndex + 1];
               int computedValue8 = candidateOffsetIndices[offsetIndex + 2];
               BlockPos blockPos2 = blockPos.offset(value9, computedValue7, computedValue8);
               if (miningPlanOptions == null || miningPlanOptions.blacklist == null || !miningPlanOptions.blacklist.contains(blockPos2)) {
                  double value10 = distanceSquared(position, blockPos2);
                  if (!(value10 > computedValue5)) {
                     Vec3 position2 = blast(blockPos2);
                     double value11 = blastDistanceSquared(position, position2);
                     if (!(value11 > computedValue6)) {
                        BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos2);
                        BlockPos blockPos3 = blockPos2.above();
                        BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos3);
                        byte flag2 = 0;
                        byte flag3;
                        if (isSupport(blockState)) {
                           flag3 = 0;
                        } else if (condition && blockState.canBeReplaced()) {
                           flag3 = 1;
                        } else {
                           if (!condition || !mayDig(blockPos2, blockState, position, computedValue5, miningPlanOptions)) {
                              String text = "нетОпоры";
                              continue;
                           }

                           flag3 = 1;
                           flag2 = 1;
                        }

                        byte flag4 = 0;
                        if (!blockState2.isAir()) {
                           if (!mayDig(blockPos3, blockState2, position, computedValue5, miningPlanOptions)) {
                              String text2 = "местоЗанято";
                              continue;
                           }

                           flag4 = 1;
                        }

                        if (occupied(blockPos3)) {
                           String text3 = "сущностьВКлетке";
                        } else if (flag3 != 0 && flag2 == 0 && obsidianObstructed(blockPos2)) {
                           String text4 = "сущностьНаОпоре";
                        } else {
                           elements2.clear();
                           elements3.clear();
                           if (flag4 != 0) {
                              elements2.add(blockPos3);
                           }

                           if (flag2 != 0) {
                              elements2.add(blockPos2);
                           }

                           float value12 = damage(position2, entityCollisionSnapshot.entity, entityCollisionSnapshot.box, 1.0F);
                           if (value12 < value4) {
                              String text5 = "потолокУронаНиже";
                           } else {
                              AABB bounds = flag3 != 0 ? new AABB(blockPos2).deflate(1.0E-4) : null;
                              int value13 = flag3 == 0 && elements2.isEmpty() ? 0 : 1;
                              float damage = damage(position2, entityCollisionSnapshot.entity, entityCollisionSnapshot.box, planningExposure(position2, entityCollisionSnapshot.box, bounds, elements2, miningPlanOptions));
                              if (damage < value4 && firstDigHelper == null && value7 > 0 && elements2.size() < miningPlanOptions.maxBlocks) {
                                 value7--;
                                 findDigChain(position2, entityCollisionSnapshot.box, bounds, elements2, elements3, position, computedValue5, miningPlanOptions, miningPlanOptions.maxBlocks - elements2.size());
                                 if (!elements3.isEmpty()) {
                                    elements2.addAll(elements3);
                                    value13 = 1;
                                    damage = damage(position2, entityCollisionSnapshot.entity, entityCollisionSnapshot.box, planningExposure(position2, entityCollisionSnapshot.box, bounds, elements2, miningPlanOptions));
                                 }
                              }

                              if (damage < value4) {
                                 String text6 = "малоУрона";
                              } else {
                                 if (value13 != 0 && entityCollisionSnapshot.box != boundingBox2) {
                                    float value14 = damage(position2, entityCollisionSnapshot.entity, boundingBox2, planningExposure(position2, boundingBox2, bounds, elements2, miningPlanOptions));
                                    damage = Math.min(damage, value14);
                                    if (damage < value4) {
                                       String text7 = "малоУронаПоЖивому";
                                       continue;
                                    }
                                 }

                                 if (flag3 != 0 && placementFace(blockPos2, position, value, elements2) == null) {
                                    String text8 = "нетГраниДляОбсидиана";
                                 } else {
                                    value8++;
                                    float instance3 = damage(
                                       position2, Minecraft.getInstance().player, boundingBox, value13 != 0 ? exposure(position2, boundingBox, bounds, elements2) : -1.0F
                                    );
                                    int value15 = instance2 != 0 && !elements2.isEmpty() ? 1 : 0;
                                    if (value15 != 0 ? !(instance3 >= value5) && !(damage < value4) : !CrystalFailureTracker.rejected(instance3, 0.0F, damage, value3, value5)) {
                                       float value16 = friendDamage(position2, elements);
                                       if (value15 == 0 && CrystalFailureTracker.rejected(instance3, value16, damage, value3, value5)) {
                                          String text9 = "заденетДруга";
                                       } else if (value15 != 0 && value16 >= value5) {
                                          String text10 = "заденетДруга";
                                       } else {
                                          double x = blockPos2.getX() + 0.5 - entityCollisionSnapshot.feet.x;
                                          double z = blockPos2.getZ() + 0.5 - entityCollisionSnapshot.feet.z;
                                          double squareRoot = Math.sqrt(x * x + z * z);
                                          int y = blockPos2.getY() + 1.0 > entityCollisionSnapshot.box.minY + 0.5 && deltaMovement == 0 ? 1 : 0;
                                          float computedValue9 = (value5 - instance3) * 25.0F + damage - value16 * 2.0F - (float)(squareRoot * 1.5);
                                          if (instance != 0 && blockPos2.getY() == standingBlock(entityCollisionSnapshot.entity).getY()) {
                                             float instance4 = planningExposure(position2, entityCollisionSnapshot.box, null, List.of(), miningPlanOptions)
                                                - cachedExposure(position2, boundingBox, Minecraft.getInstance().player);
                                             if (instance4 > 0.15F || hasFaceCover(position, blockPos2)) {
                                                computedValue9 += 35.0F + instance4 * 40.0F;
                                             } else if (value13 == 0) {
                                                computedValue9 -= 12.0F;
                                             }
                                          }

                                          if (flag3 != 0) {
                                             computedValue9 -= 3.0F;
                                          }

                                          if (y != 0) {
                                             computedValue9 -= 3.0F;
                                          }

                                          if (value9 == 0 && computedValue8 == 0) {
                                             computedValue9 += 2.0F;
                                          }

                                          if (miningPlanOptions != null && blockPos2.equals(miningPlanOptions.stickyBase)) {
                                             computedValue9 += 4.0F;
                                          }

                                          if (miningPlanOptions != null && miningPlanOptions.preferredBase != null) {
                                             double computedValue10 = Math.sqrt(blockPos2.distSqr(miningPlanOptions.preferredBase));
                                             float computedValue11 = Math.abs(damage - miningPlanOptions.preferredDamage);
                                             computedValue9 += Math.max(0.0F, 64.0F - (float)computedValue10 * 16.0F - computedValue11 * 7.0F);
                                          }

                                          if (elements2.isEmpty()) {
                                             float computedValue12 = damage * 12.0F - instance3 * 8.0F - value16 * 5.0F - (float)Math.sqrt(value10) * 3.0F - (float)squareRoot * 4.0F;
                                             int value17 = miningPlanOptions != null && blockPos2.equals(miningPlanOptions.stickyBase) ? 1 : 0;
                                             if (value17 != 0) {
                                                computedValue12 += 10.0F;
                                             }

                                             BlockPos blockPos4 = standingBlock(entityCollisionSnapshot.entity);
                                             int x2 = Math.abs(blockPos2.getX() - blockPos4.getX());
                                             int z2 = Math.abs(blockPos2.getZ() - blockPos4.getZ());
                                             int y2 = blockPos2.getY() == blockPos4.getY() && x2 + z2 == 1 ? 1 : 0;
                                             if (y2 != 0) {
                                                computedValue12 += 12.0F;
                                             } else if (blockPos2.getY() < blockPos4.getY()) {
                                                computedValue12 -= 12.0F * (blockPos4.getY() - blockPos2.getY());
                                             }

                                             if (flag3 != 0) {
                                                if (computedValue12 > computedValue4) {
                                                   computedValue4 = computedValue12;
                                                   firstDigHelper4 = new FirstDigHelper(blockPos2, position2, true, List.of(), damage, instance3);
                                                }
                                             } else {
                                                computedValue9 += 8.0F;
                                                int value18 = miningPlanOptions != null && miningPlanOptions.preferMaximumDamage ? 1 : 0;
                                                int value19 = firstDigHelper3 != null && miningPlanOptions != null && firstDigHelper3.base().equals(miningPlanOptions.stickyBase) ? 1 : 0;
                                                int damage2;
                                                if (value18 == 0) {
                                                   damage2 = firstDigHelper3 != null && !(computedValue12 > computedValue3) ? 0 : 1;
                                                } else if (firstDigHelper3 == null) {
                                                   damage2 = 1;
                                                } else if (value17 != value19) {
                                                   damage2 = value17 != 0
                                                      ? (damage + 0.75F >= firstDigHelper3.enemyDamage() ? 1 : 0)
                                                      : (damage > firstDigHelper3.enemyDamage() + 0.75F ? 1 : 0);
                                                } else {
                                                   damage2 = !(damage > firstDigHelper3.enemyDamage() + 0.01F)
                                                         && (!(Math.abs(damage - firstDigHelper3.enemyDamage()) <= 0.01F) || !(computedValue12 > computedValue3))
                                                      ? 0
                                                      : 1;
                                                }

                                                if (damage2 != 0) {
                                                   computedValue3 = computedValue12;
                                                   firstDigHelper3 = new FirstDigHelper(blockPos2, position2, false, List.of(), damage, instance3);
                                                }
                                             }

                                             if (computedValue9 > computedValue) {
                                                computedValue = computedValue9;
                                                firstDigHelper = new FirstDigHelper(blockPos2, position2, (flag3 != 0), List.of(), damage, instance3);
                                             }
                                          } else {
                                             float value20 = digCost(elements2, miningPlanOptions);
                                             computedValue9 -= value20 * 0.1F;
                                             computedValue9 -= 3.0F * (elements2.size() - 1);
                                             if (miningPlanOptions.currentBlock != null && elements2.contains(miningPlanOptions.currentBlock)) {
                                                computedValue9 += 8.0F;
                                             }

                                             if (value20 < mAX_VALUE - 0.25F || Math.abs(value20 - mAX_VALUE) <= 0.25F && computedValue9 > computedValue2) {
                                                mAX_VALUE = value20;
                                                computedValue2 = computedValue9;
                                                firstDigHelper2 = new FirstDigHelper(
                                                   blockPos2,
                                                   position2,
                                                   (flag3 != 0),
                                                   orderedChain(elements3, flag4 != 0 ? blockPos3 : null, flag2 != 0 ? blockPos2 : null),
                                                   damage,
                                                   instance3
                                                );
                                             }
                                          }
                                       }
                                    } else {
                                       String text11 = "убьётМеня";
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

         if (firstDigHelper3 != null) {
            firstDigHelper = firstDigHelper3;
         } else if (firstDigHelper4 != null) {
            firstDigHelper = firstDigHelper4;
         } else {
            firstDigHelper = firstDigHelper2;
         }

         ignoreTerrain = false;
         return firstDigHelper;
      } else {
         return null;
      }
   }

   public static Vec3 feet(AABB bounds) {
      return new Vec3((bounds.minX + bounds.maxX) / 2.0, bounds.minY, (bounds.minZ + bounds.maxZ) / 2.0);
   }

   public static float sampleExposure(Vec3 position, AABB bounds) {
      return exposure(position, bounds);
   }

   public static BlockPos standingBlock(Vec3 position) {
      double y = position.y - 0.2;
      BlockPos blockPos = BlockPos.containing(position.x, y, position.z);
      if (Minecraft.getInstance().level != null) {
         if (Minecraft.getInstance().level.getBlockState(blockPos).isAir()) {
            blockPos = blockPos.below();
         }
      }

      return blockPos;
   }

   private CrystalSimulation() {
   }

   public static boolean breakable(BlockState blockState) {
      if (blockState.isAir()) {
         return false;
      }

      if (blockState.getBlock() instanceof LiquidBlock) {
         return false;
      }

      Block block = blockState.getBlock();
      return block.defaultDestroyTime() >= 0.0F;
   }

   private static boolean protectedColumn(BlockPos blockPos) {
      int value = 0;
      value = (byte)0;
      BlockPos instance = Minecraft.getInstance().player.blockPosition();
      value = blockPos.getX();
      if (value == instance.getX()) {
         value = blockPos.getZ();
         if (value == instance.getZ()) {
            value = blockPos.getY();
            int y = value - instance.getY();
            return y >= -1 && y <= 1;
         }
      }

      return false;
   }

   public static float coverAdvantage(Vec3 position, AABB bounds, AABB bounds2) {
      return exposure(position, bounds2) - exposure(position, bounds);
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds, float damage) {
      return damage(position, entity, bounds, damage, 12.0);
   }

   public static float entityExposure(Vec3 position, Entity entity) {
      return cachedExposure(position, entity.getBoundingBox(), entity);
   }

   public static Vec3 blast(BlockPos blockPos) {
      double x = blockPos.getX() + 0.5;
      double y = blockPos.getY() + 1.0;
      double z = blockPos.getZ() + 0.5;
      return new Vec3(x, y, z);
   }

   private static double sampleJitter() {
      return VisibleHelper.random(-0.04F, 0.04F);
   }

   private static Vec3 closestFacePoint(BlockState blockState, BlockPos blockPos, Direction direction, Vec3 position) {
      VoxelShape collisionShape = blockState.getCollisionShape(Minecraft.getInstance().level, blockPos);
      AABB bounds = collisionShape.isEmpty() ? new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0) : collisionShape.bounds();
      double value = 0.001;
      double x = blockPos.getX() + bounds.minX;
      double y = blockPos.getY() + bounds.minY;
      double z = blockPos.getZ() + bounds.minZ;
      double x2 = blockPos.getX() + bounds.maxX;
      double y2 = blockPos.getY() + bounds.maxY;
      double z2 = blockPos.getZ() + bounds.maxZ;
      double clampedValue = Mth.clamp(position.x, x + value, x2 - value);
      double clampedValue2 = Mth.clamp(position.y, y + value, y2 - value);
      double clampedValue3 = Mth.clamp(position.z, z + value, z2 - value);
      switch (direction) {
         case UP:
            clampedValue2 = y2;
            break;
         case DOWN:
            clampedValue2 = y;
            break;
         case NORTH:
            clampedValue3 = z;
            break;
         case SOUTH:
            clampedValue3 = z2;
            break;
         case WEST:
            clampedValue = x;
            break;
         case EAST:
            clampedValue = x2;
      }

      return new Vec3(clampedValue, clampedValue2, clampedValue3);
   }

   public static float cachedExposure(Vec3 position, AABB bounds, Entity entity) {
      if (!ignoreTerrain) {
         return exposure(position, bounds);
      }

      int id = entity.getId();
      double x = position.x / 1.0;
      long roundedDown = (long)Math.floor(x);
      double y = position.y / 1.0;
      long roundedDown2 = (long)Math.floor(y);
      EntityExposureKey entityExposureKey = new EntityExposureKey(id, roundedDown, roundedDown2, (long)Math.floor(position.z / 1.0));
      Float value = exposureCache.get(entityExposureKey);
      if (value != null) {
         return value;
      }

      float result = exposure(position, bounds);
      exposureCache.put(entityExposureKey, result);
      return result;
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds, float value, double value2) {
      if (entity == null || bounds == null || Minecraft.getInstance().level == null) {
         return 0.0F;
      }

      if (entity.isDeadOrDying()) {
         return 0.0F;
      }

      Vec3 position2 = feet(bounds);
      double distance = position2.distanceTo(position);
      if (distance > value2) {
         return 0.0F;
      }

      double value3 = value >= 0.0F ? value : cachedExposure(position, bounds, entity);
      double computedValue = (1.0 - distance / value2) * value3;
      float computedValue2 = (float)((computedValue * computedValue + computedValue) / 2.0 * 7.0 * value2 + 1.0);
      computedValue2 = armor(entity, computedValue2);
      computedValue2 = enchantments(entity, computedValue2);
      computedValue2 = resistance(entity, computedValue2);
      return Math.max(0.0F, computedValue2);
   }

   private static double jitter() {
      return VisibleHelper.random(-0.03F, 0.03F);
   }

   public static CrystalDamageResult bestBreak(List<? extends LivingEntity> livingEntitys, Vec3 position, float value, float value2, float value3, float value4, List elements) {
      if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null) {
         CrystalDamageResult crystalDamageResult = null;
         float index = -Float.MAX_VALUE;

         for (Entity entity : Minecraft.getInstance().level.entitiesForRendering()) {
            if (entity instanceof EndCrystal endCrystal) {
               double boundingBox = distanceSquared(position, endCrystal.getBoundingBox());
               if (boundingBox > value * value) {
                  String text = "реач";
               } else {
                  Vec3 position2 = endCrystal.position();
                  float damage = 0.0F;

                  for (LivingEntity entity2 : livingEntitys) {
                     damage = Math.max(damage, damage(position2, entity2));
                  }

                  if (damage < value3) {
                     String text2 = "малоУрона";
                  } else {
                     float instance = damage(position2, Minecraft.getInstance().player);
                     if (CrystalFailureTracker.rejected(instance, 0.0F, damage, value2, value4)) {
                        String text3 = "убьётМеня";
                     } else {
                        float value5 = friendDamage(position2, elements);
                        if (CrystalFailureTracker.rejected(instance, value5, damage, value2, value4)) {
                           String text4 = "заденетДруга";
                        } else {
                           float computedValue = (value4 - instance) * 25.0F + damage - value5 * 2.0F;
                           if (computedValue > index) {
                              index = computedValue;
                              crystalDamageResult = new CrystalDamageResult(endCrystal, damage, instance);
                           }
                        }
                     }
                  }
               }
            }
         }

         return crystalDamageResult;
      } else {
         return null;
      }
   }

   public static double distanceSquared(Vec3 position, BlockPos blockPos) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1.0;
      double y2 = blockPos.getY() + 1.0;
      double z2 = blockPos.getZ() + 1.0;
      AABB bounds = new AABB(x, y, z, x2, y2, z2);
      return distanceSquared(position, bounds);
   }

   private static int protection(LivingEntity entity) {
      int instance = Minecraft.getInstance().player == null ? Integer.MIN_VALUE : Minecraft.getInstance().player.tickCount;
      if (instance != cacheTick) {
         protectionCache.clear();
         cacheTick = instance;
      }

      int id = protectionCache.get(entity.getId());
      if (id != -1) {
         return id;
      }

      int value = 0;

      for (EquipmentSlot equipmentSlot : armorSlots) {
         ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);
         if (!itemBySlot.isEmpty()) {
            value += VisibleHelper.enchantment(itemBySlot, Enchantments.PROTECTION);
            value += VisibleHelper.enchantment(itemBySlot, Enchantments.BLAST_PROTECTION) * 2;
         }
      }

      value = Mth.clamp(value, 0, 20);
      protectionCache.put(entity.getId(), value);
      return value;
   }

   private static double exitAxis(double result, double value, double value2, double value3) {
      if (value > 1.0E-8) {
         double result2 = value3 / value;
         if (result2 >= 0.0 && result2 < result) {
            return result2;
         }
      } else if (value < -1.0E-8) {
         double result3 = value2 / value;
         if (result3 >= 0.0 && result3 < result) {
            return result3;
         }
      }

      return result;
   }

   private static void findDigChain(Vec3 position, AABB bounds, AABB bounds2, List elements, List elements2, Vec3 position2, double index, MiningPlanOptions miningPlanOptions, int index2) {
      if (index2 > 0) {
         for (int value = 0; value < 9; value++) {
            if (elements2.size() >= index2) {
               return;
            }

            Vec3 position3 = digSample(bounds, value);
            if (bounds2 == null || !bounds2.clip(position3, position).isPresent()) {
               Vec3 position4 = position3;

               for (int value2 = 0; value2 < 3; value2++) {
                  BlockPos blockPos = InitializeHelper.INSTANCE.rayBlock(position4, position, collisionContext);
                  if (blockPos == null) {
                     break;
                  }

                  if (!elements.contains(blockPos) && !elements2.contains(blockPos)) {
                     if (elements2.size() >= index2) {
                        return;
                     }

                     if (!mayDig(blockPos, InitializeHelper.INSTANCE.getBlockState(blockPos), position2, index, miningPlanOptions)) {
                        break;
                     }

                     elements2.add(blockPos);
                  }

                  Vec3 position5 = afterVoxel(position4, position, blockPos);
                  if (position5 == null) {
                     break;
                  }

                  position4 = position5;
               }
            }
         }
      }
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds) {
      return damage(position, entity, bounds, -1.0F);
   }

   public static BlockAimTarget placementFace(BlockPos blockPos, Vec3 position, float index, List elements) {
      BlockAimTarget blockAimTarget = null;
      double mAX_VALUE = Double.MAX_VALUE;

      for (Direction direction : Direction.values()) {
         BlockPos opposite = blockPos.relative(direction.getOpposite());
         if (elements == null || !elements.contains(opposite)) {
            BlockState blockState = Minecraft.getInstance().level.getBlockState(opposite);
            if (!blockState.canBeReplaced() && blockState.isFaceSturdy(Minecraft.getInstance().level, opposite, direction)) {
               Vec3 position2 = closestFacePoint(blockState, opposite, direction, position);
               Vec3 position3 = new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
               if (position.subtract(position2).dot(position3) > 0.0) {
                  double distanceSquared = position.distanceToSqr(position2);
                  if (!(distanceSquared > index * index) && distanceSquared < mAX_VALUE) {
                     mAX_VALUE = distanceSquared;
                     blockAimTarget = new BlockAimTarget(opposite, direction, position2);
                  }
               }
            }
         }
      }

      if (blockAimTarget == null) {
         BlockPos blockPos2 = blockPos.below();
         if (elements == null || !elements.contains(blockPos2)) {
            BlockState blockState2 = Minecraft.getInstance().level.getBlockState(blockPos2);
            if (!blockState2.canBeReplaced() && blockState2.isFaceSturdy(Minecraft.getInstance().level, blockPos2, Direction.UP)) {
               Vec3 position4 = closestFacePoint(blockState2, blockPos2, Direction.UP, position);
               if (position.distanceToSqr(position4) <= index * index) {
                  blockAimTarget = new BlockAimTarget(blockPos2, Direction.UP, position4);
               }
            }
         }
      }

      return blockAimTarget;
   }

   private static float enchantments(LivingEntity entity, float value) {
      return value * (1.0F - protection(entity) / 25.0F);
   }

   static {
      initialize();
   }

   public static boolean obsidianObstructed(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      ClientLevel level = instance.level;
      BlockState blockState = Blocks.OBSIDIAN.defaultBlockState();
      return !level.isUnobstructed(blockState, blockPos, CollisionContext.empty());
   }

   public static Vec3 crystalAim(BlockPos blockPos) {
      Vec3 position = Vec3.atCenterOf(blockPos);
      double value = jitter();
      return position.add(value, jitter(), jitter());
   }

   private static float armor(LivingEntity entity, float result) {
      float armorValue = entity.getArmorValue();
      if (armorValue <= 0.0F) {
         return result;
      }

      float attributeValue = (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float computedValue = 2.0F + attributeValue / 4.0F;
      float clampedValue = Mth.clamp(armorValue - result / computedValue, armorValue * 0.2F, 20.0F);
      return result * (1.0F - clampedValue / 25.0F);
   }

   private static List orderedChain(List elements, BlockPos blockPos, BlockPos blockPos2) {
      if (elements.isEmpty() && blockPos == null && blockPos2 == null) {
         return List.of();
      }

      int intermediate = elements.size() + 2;
      ArrayList elements2 = new ArrayList(intermediate);
      elements2.addAll(elements);
      if (blockPos != null) {
         elements2.add(blockPos);
      }

      if (blockPos2 != null) {
         elements2.add(blockPos2);
      }

      return elements2;
   }

   public static boolean isSupport(BlockState blockState) {
      return blockState.is(Blocks.OBSIDIAN) || blockState.is(Blocks.BEDROCK);
   }

   public static boolean hasFaceCover(Vec3 position, BlockPos blockPos) {
      if (Minecraft.getInstance().level != null && position != null && blockPos != null) {
         Vec3 position2 = blast(blockPos);
         ClipContext clipContext = new ClipContext(position, position2, net.minecraft.world.level.ClipContext.Block.COLLIDER, Fluid.NONE, Minecraft.getInstance().player);
         BlockHitResult instance = Minecraft.getInstance().level.clip(clipContext);
         if (instance instanceof BlockHitResult blockHitResult && instance.getType() == Type.BLOCK) {
            BlockPos blockPos2 = blockHitResult.getBlockPos();
            return !blockPos2.equals(blockPos) && !blockPos2.equals(blockPos.above());
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private static boolean clearRay(Vec3 position, Vec3 position2, AABB bounds, List elements) {
      if (bounds != null && bounds.clip(position, position2).isPresent()) {
         return false;
      }

      Vec3 position3 = position;

      for (int value = 0; value <= elements.size(); value++) {
         BlockPos blockPos = InitializeHelper.INSTANCE.rayBlock(position3, position2, collisionContext);
         if (blockPos == null) {
            return true;
         }

         if (!elements.contains(blockPos)) {
            return false;
         }

         Vec3 position4 = afterVoxel(position3, position2, blockPos);
         if (position4 == null) {
            return true;
         }

         position3 = position4;
      }

      return false;
   }

   private static float exposure(Vec3 position, AABB bounds, AABB bounds2, List elements) {
      double computedValue = 1.0 / ((bounds.maxX - bounds.minX) * 2.0 + 1.0);
      double computedValue2 = 1.0 / ((bounds.maxY - bounds.minY) * 2.0 + 1.0);
      double computedValue3 = 1.0 / ((bounds.maxZ - bounds.minZ) * 2.0 + 1.0);
      double computedValue4 = (1.0 - Math.floor(1.0 / computedValue) * computedValue) / 2.0;
      double computedValue5 = (1.0 - Math.floor(1.0 / computedValue3) * computedValue3) / 2.0;
      if (!(computedValue < 0.0) && !(computedValue2 < 0.0) && !(computedValue3 < 0.0)) {
         int value = 0;
         int value2 = 0;

         for (double index = 0.0; index <= 1.0; index += computedValue) {
            for (double index2 = 0.0; index2 <= 1.0; index2 += computedValue2) {
               for (double index3 = 0.0; index3 <= 1.0; index3 += computedValue3) {
                  Vec3 position2 = new Vec3(
                     Mth.lerp(index, bounds.minX, bounds.maxX) + computedValue4, Mth.lerp(index2, bounds.minY, bounds.maxY), Mth.lerp(index3, bounds.minZ, bounds.maxZ) + computedValue5
                  );
                  if (clearRay(position2, position, bounds2, elements)) {
                     value++;
                  }

                  value2++;
               }
            }
         }

         return value2 == 0 ? 0.0F : (float)value / value2;
      } else {
         return 0.0F;
      }
   }

   public static boolean mayDig(BlockPos blockPos, BlockState blockState, Vec3 position, double distance, MiningPlanOptions miningPlanOptions) {
      if (miningPlanOptions == null || !miningPlanOptions.enabled) {
         return false;
      }

      if (!mineable(blockState)) {
         return false;
      }

      if (protectedColumn(blockPos)) {
         return false;
      }

      if (miningPlanOptions.standBlock != null) {
         int x = Math.abs(blockPos.getX() - miningPlanOptions.standBlock.getX());
         int z = Math.abs(blockPos.getZ() - miningPlanOptions.standBlock.getZ());
         int y = blockPos.getY() - miningPlanOptions.standBlock.getY();
         if (x + z != 1 || y != 0 && y != 1) {
            return false;
         }
      } else if (miningPlanOptions.footY != Integer.MIN_VALUE && blockPos.getY() != miningPlanOptions.footY) {
         return false;
      }

      return miningPlanOptions.blacklist != null && miningPlanOptions.blacklist.contains(blockPos) ? false : distanceSquared(position, blockPos) <= distance;
   }

   public static String describe(List elements) {
      if (elements.isEmpty()) {
         return "-";
      }

      StringBuilder stringBuilder = new StringBuilder();

      for (int value = 0; value < elements.size(); value++) {
         BlockPos blockPos = (BlockPos)elements.get(value);
         if (value > 0) {
            stringBuilder.append('>');
         }

         StringBuilder stringBuilder2 = stringBuilder.append(blockPos.toShortString());
         StringBuilder stringBuilder3 = stringBuilder2.append('[');
         float value2 = digTicks(blockPos);
         stringBuilder3.append(Math.round(value2)).append("t]");
      }

      return stringBuilder.toString();
   }

   public static boolean occupiedExceptItems(BlockPos blockPos) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1.0;
      double y2 = blockPos.getY() + 2.0;
      double z2 = blockPos.getZ() + 1.0;
      AABB bounds = new AABB(x, y, z, x2, y2, z2);
      Minecraft instance = Minecraft.getInstance();
      ClientLevel level = instance.level;
      Predicate<net.minecraft.world.entity.Entity> predicate = entity -> {
         return !(entity instanceof ItemEntity) && blockingEntity(entity);
      };
      var entitiesOfClass = level.getEntitiesOfClass(Entity.class, bounds, predicate);
      return !entitiesOfClass.isEmpty();
   }

   public static boolean isPlaceable(BlockPos blockPos, Vec3 position, float value) {
      Minecraft instance = Minecraft.getInstance();
      BlockState blockState = instance.level.getBlockState(blockPos);
      if (!blockState.canBeReplaced()) {
         return false;
      } else {
         return obsidianObstructed(blockPos) ? false : placementFace(blockPos, position, value) != null;
      }
   }

   private static Vec3 afterVoxel(Vec3 position, Vec3 position2, BlockPos blockPos) {
      Vec3 position3 = position2.subtract(position);
      double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;
      double x = position3.x;
      double x2 = blockPos.getX();
      x2 -= position.x;
      double x3 = blockPos.getX() + 1.0 - position.x;
      pOSITIVE_INFINITY = exitAxis(pOSITIVE_INFINITY, x, x2, x3);
      double y = position3.y;
      x2 = blockPos.getY() - position.y;
      pOSITIVE_INFINITY = exitAxis(pOSITIVE_INFINITY, y, x2, blockPos.getY() + 1.0 - position.y);
      double z = position3.z;
      x2 = blockPos.getZ();
      x2 -= position.z;
      pOSITIVE_INFINITY = exitAxis(pOSITIVE_INFINITY, z, x2, blockPos.getZ() + 1.0 - position.z);
      if (!Double.isFinite(pOSITIVE_INFINITY)) {
         return null;
      }

      double computedValue = pOSITIVE_INFINITY + 1.0E-4;
      Vec3 position4;
      if (computedValue >= 1.0) {
         position4 = null;
      } else {
         Vec3 position5 = position3.scale(computedValue);
         position4 = position.add(position5);
      }

      return position4;
   }

   public static float friendDamage(Vec3 position, List<? extends LivingEntity> livingEntitys) {
      if (livingEntitys != null && !livingEntitys.isEmpty()) {
         float result = 0.0F;
         Iterator iterator = livingEntitys.iterator();

         while (true) {
            if (!iterator.hasNext()) {
               return result;
            }

            LivingEntity element = (LivingEntity)iterator.next();
            if (element != null) {
               Minecraft instance = Minecraft.getInstance();
               if (element != instance.player) {
                  float boundingBox = damage(position, element, element.getBoundingBox());
                  result = Math.max(result, boundingBox);
               }
            }
         }
      } else {
         return 0.0F;
      }
   }
}
