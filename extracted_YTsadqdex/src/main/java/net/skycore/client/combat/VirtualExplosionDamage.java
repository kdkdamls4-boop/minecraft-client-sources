package net.skycore.client.combat;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.skycore.client.util.MinecraftAccess;

public final class VirtualExplosionDamage implements MinecraftAccess {
   public static boolean intersectsEntityBox(BlockPos blockPos, Entity entity) {
      double x = blockPos.getX();
      double y = blockPos.getY();
      double z = blockPos.getZ();
      double x2 = blockPos.getX() + 1.0;
      double y2 = blockPos.getY() + 2.0;
      double z2 = blockPos.getZ() + 1.0;
      AABB bounds = new AABB(x, y, z, x2, y2, z2);
      return entity.getBoundingBox().intersects(bounds);
   }

   public static float getDamageOfGhostBlock(Vec3 position, Player player, BlockPos blockPos) {
      if (mc.level != null && mc.level.getDifficulty() != Difficulty.PEACEFUL && player != null) {
         double value = 12.0;
         if (!new AABB(
               Mth.floor(position.x - value - 1.0),
               Mth.floor(position.y - value - 1.0),
               Mth.floor(position.z - value - 1.0),
               Mth.floor(position.x + value + 1.0),
               Mth.floor(position.y + value + 1.0),
               Mth.floor(position.z + value + 1.0)
            )
            .intersects(player.getBoundingBox())) {
            return 0.0F;
         }

         if (player.isInvulnerable()) {
            return 0.0F;
         }

         double computedValue = player.distanceToSqr(position) / 144.0;
         if (computedValue > 1.0) {
            return 0.0F;
         }

         double exposureGhost = getExposureGhost(position, player, blockPos);
         double computedValue2 = (1.0 - computedValue) * exposureGhost;
         return applyDamageReductions(player, (float)Math.floor((computedValue2 * computedValue2 + computedValue2) / 2.0 * 7.0 * value + 1.0));
      } else {
         return 0.0F;
      }
   }

   public static float getDamageIgnoringBlock(Vec3 position, Player player, BlockPos blockPos) {
      if (mc.level != null && mc.level.getDifficulty() != Difficulty.PEACEFUL && player != null && blockPos != null) {
         double value = 12.0;
         if (!new AABB(
               Mth.floor(position.x - value - 1.0),
               Mth.floor(position.y - value - 1.0),
               Mth.floor(position.z - value - 1.0),
               Mth.floor(position.x + value + 1.0),
               Mth.floor(position.y + value + 1.0),
               Mth.floor(position.z + value + 1.0)
            )
            .intersects(player.getBoundingBox())) {
            return 0.0F;
         }

         if (player.isInvulnerable()) {
            return 0.0F;
         }

         double computedValue = player.distanceToSqr(position) / 144.0;
         if (computedValue > 1.0) {
            return 0.0F;
         }

         double exposureIgnoreBlock = getExposureIgnoreBlock(position, player, blockPos);
         double computedValue2 = (1.0 - computedValue) * exposureIgnoreBlock;
         return applyDamageReductions(player, (float)Math.floor((computedValue2 * computedValue2 + computedValue2) / 2.0 * 7.0 * value + 1.0));
      } else {
         return 0.0F;
      }
   }

   private static Type raycast(Vec3 position, Vec3 position2) {
      BiFunction<java.lang.Object, net.minecraft.core.BlockPos, net.minecraft.world.phys.HitResult.Type> blockState = (value, blockPos) -> {
         BlockState blockState2 = mc.level.getBlockState(blockPos);
         VoxelShape collisionShape = blockState2.getCollisionShape(mc.level, blockPos);
         BlockHitResult blockHitResult = collisionShape.clip(position, position2, blockPos);
         return blockHitResult == null ? null : blockHitResult.getType();
      };
      Function<java.lang.Object, net.minecraft.world.phys.HitResult.Type> function = value2 -> {
         return Type.MISS;
      };
      return BlockGetter.traverseBlocks(position, position2, null, blockState, function);
   }

   private static BlockHitResult raycastGhost(ClipContext clipContext, BlockPos blockPos) {
      Vec3 from = clipContext.getFrom();
      Vec3 to = clipContext.getTo();
      BiFunction<net.minecraft.world.level.ClipContext, net.minecraft.core.BlockPos, net.minecraft.world.phys.BlockHitResult> blockState = (clipContext2, blockPos2) -> {
         BlockState blockState2;
         if (blockPos2.equals(blockPos)) {
            blockState2 = Blocks.OBSIDIAN.defaultBlockState();
         } else {
            blockState2 = mc.level.getBlockState(blockPos2);
         }

         BlockState blockState3 = blockState2;
         VoxelShape collisionShape = blockState3.getCollisionShape(mc.level, blockPos2);
         Vec3 from2 = clipContext.getFrom();
         return collisionShape.clip(from2, clipContext.getTo(), blockPos2);
      };
      return BlockGetter.traverseBlocks(from, to, clipContext, blockState, clipContext3 -> {
         Vec3 to2 = clipContext3.getTo();
         Direction uP = Direction.UP;
         BlockPos to3 = BlockPos.containing(clipContext3.getTo());
         return BlockHitResult.miss(to2, uP, to3);
      });
   }

   private static float getExposureGhost(Vec3 position, AABB bounds, Entity entity, BlockPos blockPos) {
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
                  if (raycastGhost(new ClipContext(position2, position, Block.COLLIDER, Fluid.NONE, entity), blockPos).getType() == Type.MISS) {
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

   private static int getEnchantLevel(ItemStack itemStack, String text) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            ItemEnchantments enchantments = itemStack.getEnchantments();
            var keys = enchantments.keySet();

            for (Holder<net.minecraft.world.item.enchantment.Enchantment> holder : keys) {
               var optional = holder.unwrapKey();
               var path = optional.map(resourceKey -> {
                  return resourceKey.identifier().getPath();
               });
               String text2 = path.orElse("");
               if (text.equals(text2)) {
                  return enchantments.getLevel(holder);
               }
            }

            return 0;
         }
      }

      return 0;
   }

   public static float getExplosionDamage(Vec3 position, Player player, boolean condition) {
      if (mc.level != null && mc.level.getDifficulty() != Difficulty.PEACEFUL && player != null) {
         if (!new AABB(
               Mth.floor(position.x - 11.0),
               Mth.floor(position.y - 11.0),
               Mth.floor(position.z - 11.0),
               Mth.floor(position.x + 13.0),
               Mth.floor(position.y + 13.0),
               Mth.floor(position.z + 13.0)
            )
            .intersects(player.getBoundingBox())) {
            return 0.0F;
         }

         if (player.isInvulnerable()) {
            return 0.0F;
         }

         double computedValue = player.distanceToSqr(position) / 144.0;
         if (computedValue > 1.0) {
            return 0.0F;
         }

         double boundingBox = getExposure(position, player.getBoundingBox(), condition);
         double computedValue2 = (1.0 - computedValue) * boundingBox;
         return applyDamageReductions(player, (float)Math.floor((computedValue2 * computedValue2 + computedValue2) / 2.0 * 7.0 * 12.0 + 1.0));
      } else {
         return 0.0F;
      }
   }

   private static float getExposureGhost(Vec3 position, Entity entity, BlockPos blockPos) {
      AABB boundingBox = entity.getBoundingBox();
      return getExposureGhost(position, boundingBox, entity, blockPos);
   }

   private static float getProtectionAmount(ItemStack... itemStackValues) {
      float index = 0.0F;

      for (ItemStack itemStack : itemStackValues) {
         index += getEnchantLevel(itemStack, "protection");
         index += getEnchantLevel(itemStack, "blast_protection") * 2.0F;
      }

      return Math.min(index, 20.0F);
   }

   private static float getExposure(Vec3 position, AABB bounds) {
      double index = 0.4545454446934474;
      double index2 = 0.21739130885479366;
      double index3 = 0.4545454446934474;
      int value = 0;
      int value2 = 0;

      for (double index4 = 0.0; index4 <= 1.0; index4 += index) {
         for (double index5 = 0.0; index5 <= 1.0; index5 += index2) {
            for (double index6 = 0.0; index6 <= 1.0; index6 += index3) {
               Vec3 position2 = new Vec3(
                  Mth.lerp(index4, bounds.minX, bounds.maxX) + 0.045454555306552624,
                  Mth.lerp(index5, bounds.minY, bounds.maxY),
                  Mth.lerp(index6, bounds.minZ, bounds.maxZ) + 0.045454555306552624
               );
               if (raycast(position2, position) == Type.MISS) {
                  value++;
               }

               value2++;
            }
         }
      }

      return value2 == 0 ? 0.0F : (float)value / value2;
   }

   private static float applyDamageReductions(Player player, float damage) {
      if (mc.level.getDifficulty() == Difficulty.EASY) {
         damage = Math.min(damage / 2.0F + 1.0F, damage);
      } else if (mc.level.getDifficulty() == Difficulty.HARD) {
         damage = damage * 3.0F / 2.0F;
      }

      DamageSource damageSource = mc.level.damageSources().explosion(null, null);
      damage = CombatRules.getDamageAfterAbsorb(player, damage, damageSource, player.getArmorValue(), (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
      if (player.hasEffect(MobEffects.RESISTANCE)) {
         int effect = 25 - (player.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
         damage = Math.max(damage * effect / 25.0F, 0.0F);
      }

      if (damage <= 0.0F) {
         return 0.0F;
      }

      float itemBySlot = getProtectionAmount(
         player.getItemBySlot(EquipmentSlot.HEAD),
         player.getItemBySlot(EquipmentSlot.CHEST),
         player.getItemBySlot(EquipmentSlot.LEGS),
         player.getItemBySlot(EquipmentSlot.FEET)
      );
      if (itemBySlot > 0.0F) {
         damage = CombatRules.getDamageAfterMagicAbsorb(damage, itemBySlot);
      }

      return damage;
   }

   private static BlockHitResult raycastIgnoreBlock(ClipContext clipContext, BlockPos blockPos) {
      Vec3 from = clipContext.getFrom();
      Vec3 to = clipContext.getTo();
      BiFunction<net.minecraft.world.level.ClipContext, net.minecraft.core.BlockPos, net.minecraft.world.phys.BlockHitResult> blockState = (clipContext2, blockPos2) -> {
         BlockState blockState2;
         if (blockPos2.equals(blockPos)) {
            blockState2 = Blocks.AIR.defaultBlockState();
         } else {
            blockState2 = mc.level.getBlockState(blockPos2);
         }

         BlockState blockState3 = blockState2;
         VoxelShape collisionShape = blockState3.getCollisionShape(mc.level, blockPos2);
         Vec3 from2 = clipContext.getFrom();
         Vec3 to2 = clipContext.getTo();
         return collisionShape.clip(from2, to2, blockPos2);
      };
      Function<net.minecraft.world.level.ClipContext, net.minecraft.world.phys.BlockHitResult> to3 = clipContext3 -> {
         Vec3 to4 = clipContext3.getTo();
         double from3 = clipContext3.getFrom().x;
         Vec3 to5 = clipContext3.getTo();
         double x = from3 - to5.x;
         Vec3 from4 = clipContext3.getFrom();
         double to6 = from4.y - clipContext3.getTo().y;
         Direction approximateNearest = Direction.getApproximateNearest(x, to6, clipContext3.getFrom().z - clipContext3.getTo().z);
         BlockPos to7 = BlockPos.containing(clipContext3.getTo());
         return BlockHitResult.miss(to4, approximateNearest, to7);
      };
      return BlockGetter.traverseBlocks(from, to, clipContext, blockState, to3);
   }

   private static float getExposureIgnoreBlock(Vec3 position, Entity entity, BlockPos blockPos) {
      AABB boundingBox = entity.getBoundingBox();
      double computedValue = 1.0 / ((boundingBox.maxX - boundingBox.minX) * 2.0 + 1.0);
      double computedValue2 = 1.0 / ((boundingBox.maxY - boundingBox.minY) * 2.0 + 1.0);
      double computedValue3 = 1.0 / ((boundingBox.maxZ - boundingBox.minZ) * 2.0 + 1.0);
      double computedValue4 = (1.0 - Math.floor(1.0 / computedValue) * computedValue) / 2.0;
      double computedValue5 = (1.0 - Math.floor(1.0 / computedValue3) * computedValue3) / 2.0;
      if (!(computedValue < 0.0) && !(computedValue2 < 0.0) && !(computedValue3 < 0.0)) {
         int value = 0;
         int value2 = 0;

         for (double index = 0.0; index <= 1.0; index += computedValue) {
            for (double index2 = 0.0; index2 <= 1.0; index2 += computedValue2) {
               for (double index3 = 0.0; index3 <= 1.0; index3 += computedValue3) {
                  Vec3 position2 = new Vec3(
                     Mth.lerp(index, boundingBox.minX, boundingBox.maxX) + computedValue4, Mth.lerp(index2, boundingBox.minY, boundingBox.maxY), Mth.lerp(index3, boundingBox.minZ, boundingBox.maxZ) + computedValue5
                  );
                  if (raycastIgnoreBlock(new ClipContext(position2, position, Block.COLLIDER, Fluid.NONE, entity), blockPos).getType() == Type.MISS) {
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

   private VirtualExplosionDamage() {
   }

   private static float getExposure(Vec3 position, AABB bounds, boolean condition) {
      if (!condition) {
         return getExposure(position, bounds);
      }

      int value = 0;
      int value2 = 0;

      for (int value3 = 0; value3 <= 1; value3++) {
         for (int value4 = 0; value4 <= 1; value4++) {
            for (int value5 = 0; value5 <= 1; value5++) {
               Vec3 position2 = new Vec3(Mth.lerp(value3, bounds.minX, bounds.maxX), Mth.lerp(value4, bounds.minY, bounds.maxY), Mth.lerp(value5, bounds.minZ, bounds.maxZ));
               if (raycast(position2, position) == Type.MISS) {
                  value++;
               }

               value2++;
            }
         }
      }

      return value2 == 0 ? 0.0F : (float)value / value2;
   }
}
