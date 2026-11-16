package net.skycore.client.combat;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ExplosionDamage {
   private static final Map<Integer, Integer> PROTECTION_CACHE = new HashMap<>();
   private static final Minecraft MC = Minecraft.getInstance();
   private static boolean planningPass;
   private static final Map<ExplosionDamage.ExposureKey, Float> EXPOSURE_CACHE = new HashMap<>();
   private static int protectionTick = Integer.MIN_VALUE;

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds, List<BlockPos> blockPoss, BlockPos blockPos) {
      if (MC.level != null && !entity.isDeadOrDying()) {
         Vec3 position2 = new Vec3((bounds.minX + bounds.maxX) * 0.5, bounds.minY, (bounds.minZ + bounds.maxZ) * 0.5);
         double distance = position.distanceTo(position2);
         if (distance > 12.0) {
            return 0.0F;
         }

         float value;
         if (planningPass && blockPoss.isEmpty() && blockPos == null) {
            ExplosionDamage.ExposureKey exposureKey = new ExplosionDamage.ExposureKey(
               entity.getId(), (long)Math.floor(position.x), (long)Math.floor(position.y), (long)Math.floor(position.z)
            );
            Float value2 = EXPOSURE_CACHE.get(exposureKey);
            if (value2 == null) {
               value2 = exposure(position, entity, bounds, blockPoss, null);
               EXPOSURE_CACHE.put(exposureKey, value2);
            }

            value = value2;
         } else {
            value = exposure(position, entity, bounds, blockPoss, blockPos);
         }

         return mitigate((1.0 - distance / 12.0) * value, entity);
      } else {
         return 0.0F;
      }
   }

   public static void beginPlanningPass() {
      EXPOSURE_CACHE.clear();
      planningPass = true;
   }

   public static float rawExplosionDamage(double value) {
      return (float)((value * value + value) * 0.5 * 7.0 * 12.0 + 1.0);
   }

   public static void endPlanningPass() {
      planningPass = false;
      EXPOSURE_CACHE.clear();
   }

   private static int protection(LivingEntity entity) {
      int value = MC.player == null ? Integer.MIN_VALUE : MC.player.tickCount;
      if (value != protectionTick) {
         PROTECTION_CACHE.clear();
         protectionTick = value;
      }

      Integer id = PROTECTION_CACHE.get(entity.getId());
      if (id != null) {
         return id;
      }

      int value2 = 0;

      for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
         ItemStack itemBySlot = entity.getItemBySlot(equipmentSlot);

         for (Entry entry : itemBySlot.getEnchantments().entrySet()) {
            Holder key = (Holder)entry.getKey();
            if (key.is(Enchantments.PROTECTION)) {
               value2 += entry.getIntValue();
            }

            if (key.is(Enchantments.BLAST_PROTECTION)) {
               value2 += entry.getIntValue() * 2;
            }
         }
      }

      value2 = Mth.clamp(value2, 0, 20);
      PROTECTION_CACHE.put(entity.getId(), value2);
      return value2;
   }

   private static float exposure(Vec3 position, LivingEntity entity, AABB bounds, List<BlockPos> blockPoss, BlockPos blockPos) {
      double computedValue = 1.0 / ((bounds.maxX - bounds.minX) * 2.0 + 1.0);
      double computedValue2 = 1.0 / ((bounds.maxY - bounds.minY) * 2.0 + 1.0);
      double computedValue3 = 1.0 / ((bounds.maxZ - bounds.minZ) * 2.0 + 1.0);
      double computedValue4 = (1.0 - Math.floor(1.0 / computedValue) * computedValue) * 0.5;
      double computedValue5 = (1.0 - Math.floor(1.0 / computedValue3) * computedValue3) * 0.5;
      AABB bounds2 = blockPos == null ? null : new AABB(blockPos).deflate(1.0E-4);
      int value = 0;
      int value2 = 0;

      for (double index = 0.0; index <= 1.0; index += computedValue) {
         for (double index2 = 0.0; index2 <= 1.0; index2 += computedValue2) {
            for (double index3 = 0.0; index3 <= 1.0; index3 += computedValue3) {
               Vec3 position2 = new Vec3(
                  Mth.lerp(index, bounds.minX, bounds.maxX) + computedValue4, Mth.lerp(index2, bounds.minY, bounds.maxY), Mth.lerp(index3, bounds.minZ, bounds.maxZ) + computedValue5
               );
               value2++;
               if (!blockedByPlannedSupport(position, position2, bounds2) && clearThroughDigChain(position2, position, entity, blockPoss)) {
                  value++;
               }
            }
         }
      }

      return value2 == 0 ? 0.0F : (float)value / value2;
   }

   public static float damage(Vec3 position, LivingEntity entity) {
      AABB boundingBox = entity.getBoundingBox();
      List<BlockPos> blockPoss = List.of();
      return damage(position, entity, boundingBox, blockPoss, null);
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds) {
      return damage(position, entity, bounds, List.of(), null);
   }

   private ExplosionDamage() {
   }

   public static float maximumDamage(Vec3 position, LivingEntity entity, AABB bounds) {
      if (MC.level != null) {
         if (!entity.isDeadOrDying()) {
            double computedValue = (bounds.minX + bounds.maxX) * 0.5;
            double computedValue2 = (bounds.minZ + bounds.maxZ) * 0.5;
            Vec3 position2 = new Vec3(computedValue, bounds.minY, computedValue2);
            double distance = position.distanceTo(position2);
            float result;
            if (distance > 12.0) {
               result = 0.0F;
            } else {
               double computedValue3 = 1.0 - distance / 12.0;
               result = mitigate(computedValue3, entity);
            }

            return result;
         }
      }

      return 0.0F;
   }

   public static Vec3 afterVoxel(Vec3 position, Vec3 position2, BlockPos blockPos) {
      Vec3 position3 = position2.subtract(position);
      double pOSITIVE_INFINITY = Double.POSITIVE_INFINITY;

      for (int value = 0; value < 3; value++) {
         double x = value == 0 ? position.x : (value == 1 ? position.y : position.z);
         double x2 = value == 0 ? position3.x : (value == 1 ? position3.y : position3.z);
         int x3 = value == 0 ? blockPos.getX() : (value == 1 ? blockPos.getY() : blockPos.getZ());
         if (Math.abs(x2) <= 1.0E-8) {
            if (x < x3 || x > x3 + 1) {
               return null;
            }
         } else {
            pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, Math.max((x3 - x) / x2, (x3 + 1.0 - x) / x2));
         }
      }

      return Double.isFinite(pOSITIVE_INFINITY) && !(pOSITIVE_INFINITY + 1.0E-4 >= 1.0) ? position.add(position3.scale(Math.max(0.0, pOSITIVE_INFINITY) + 1.0E-4)) : null;
   }

   private static boolean clearThroughDigChain(Vec3 position, Vec3 position2, LivingEntity entity, List<BlockPos> blockPoss) {
      Vec3 position3 = position;

      for (int value = 0; value <= blockPoss.size(); value++) {
         BlockPos blockPos = WorldCache.rayBlock(position3, position2);
         if (blockPos == null) {
            return true;
         }

         if (!blockPoss.contains(blockPos)) {
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

   private static float mitigate(double damage, LivingEntity entity) {
      float value = rawExplosionDamage(damage);
      float armorValue = entity.getArmorValue();
      if (armorValue > 0.0F) {
         float attributeValue = (float)entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
         float computedValue = Mth.clamp(armorValue - value / (2.0F + attributeValue / 4.0F), armorValue * 0.2F, 20.0F);
         value *= 1.0F - computedValue / 25.0F;
      }

      int value2 = protection(entity);
      value *= 1.0F - Mth.clamp(value2, 0, 20) / 25.0F;
      MobEffectInstance effect = entity.getEffect(MobEffects.RESISTANCE);
      if (effect != null) {
         value *= 1.0F - Mth.clamp((effect.getAmplifier() + 1) * 0.2F, 0.0F, 1.0F);
      }

      return Math.max(0.0F, value);
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds, BlockPos blockPos) {
      return damage(position, entity, bounds, blockPos, null);
   }

   public static float damage(Vec3 position, LivingEntity entity, AABB bounds, BlockPos blockPos, BlockPos blockPos2) {
      List elements;
      if (blockPos == null) {
         elements = List.of();
      } else {
         elements = List.of(blockPos);
      }

      return damage(position, entity, bounds, elements, blockPos2);
   }

   public static boolean blockedByPlannedSupport(Vec3 position, Vec3 position2, AABB bounds) {
      if (bounds != null) {
         if (bounds.clip(position, position2).isPresent()) {
            return true;
         }
      }

      return false;
   }

   public record ExposureKey(int entityId, long x, long y, long z) {
      public long x() {
         return this.x;
      }

      public long y() {
         return this.y;
      }

      public int entityId() {
         return this.entityId;
      }

      public long z() {
         return this.z;
      }
   }
}
