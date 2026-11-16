package net.skycore.client.combat;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.skycore.client.util.MinecraftAccess;

public final class CrystalDamage implements MinecraftAccess {
   public static final float EXPLOSION_POWER = 6.0F;
   public static final float EXPLOSION_DIAMETER = 12.0F;

   public static boolean hasTotem(Player player) {
      if (player == null) {
         return false;
      }

      if (!isTotem(player.getOffhandItem())) {
         ItemStack mainHandItem = player.getMainHandItem();
         if (!isTotem(mainHandItem)) {
            return false;
         }
      }

      return true;
   }

   private static int enchantLevel(ItemStack itemStack, ResourceKey<Enchantment> resourceKey) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (mc.level != null) {
            RegistryAccess registryAccess = mc.level.registryAccess();
            var optional = registryAccess.lookupOrThrow(Registries.ENCHANTMENT).get(resourceKey);
            Holder holder = (Holder)optional.orElse(null);
            if (holder == null) {
               return 0;
            }

            ItemEnchantments orDefault = itemStack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            return orDefault.getLevel(holder);
         }
      }

      return 0;
   }

   private static BlockHitResult clip(Vec3 position, Vec3 position2) {
      return mc.level.clip(new ClipContext(position, position2, Block.COLLIDER, Fluid.NONE, mc.player));
   }

   public static boolean wouldPopTotem(Player player, float value) {
      if (hasTotem(player)) {
         if (value >= remainingHp(player) && value > 0.0F) {
            return true;
         }
      }

      return false;
   }

   private static float getExposure(Vec3 position, AABB bounds, BlockPos blockPos, BlockPos blockPos2) {
      double xsize = 1.0 / (bounds.getXsize() * 2.0 + 1.0);
      double ysize = 1.0 / (bounds.getYsize() * 2.0 + 1.0);
      double zsize = 1.0 / (bounds.getZsize() * 2.0 + 1.0);
      if (!(xsize <= 0.0) && !(ysize <= 0.0) && !(zsize <= 0.0)) {
         double index = (1.0 - Math.floor(1.0 / xsize) * xsize) / 2.0;
         double index2 = (1.0 - Math.floor(1.0 / zsize) * zsize) / 2.0;
         int value = 0;
         int value2 = 0;

         for (double index3 = 0.0; index3 <= 1.0; index3 += xsize) {
            for (double index4 = 0.0; index4 <= 1.0; index4 += ysize) {
               for (double index5 = 0.0; index5 <= 1.0; index5 += zsize) {
                  Vec3 position2 = new Vec3(
                     Mth.lerp(index3, bounds.minX, bounds.maxX) + index, Mth.lerp(index4, bounds.minY, bounds.maxY), Mth.lerp(index5, bounds.minZ, bounds.maxZ) + index2
                  );
                  if (raycast(position2, position, blockPos, blockPos2) == Type.MISS) {
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

   public static float getDamageIgnoringBlock(Vec3 position, Player player, BlockPos blockPos) {
      AABB bounds;
      if (player == null) {
         bounds = null;
      } else {
         bounds = player.getBoundingBox();
      }

      AABB bounds2 = bounds;
      return getExplosionDamage(position, player, bounds2, blockPos, null);
   }

   private static float applyDamageReductions(Player player, float result) {
      if (mc.level.getDifficulty() == Difficulty.EASY) {
         result = Math.min(result / 2.0F + 1.0F, result);
      } else if (mc.level.getDifficulty() == Difficulty.HARD) {
         result = result * 3.0F / 2.0F;
      }

      float armorValue = player.getArmorValue();
      float attributeValue = (float)player.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
      float computedValue = 2.0F + attributeValue / 4.0F;
      float clampedValue = Mth.clamp(armorValue - result / computedValue, armorValue * 0.2F, 20.0F);
      result *= 1.0F - clampedValue / 25.0F;
      if (player.hasEffect(MobEffects.RESISTANCE)) {
         int amplifier = player.getEffect(MobEffects.RESISTANCE).getAmplifier();
         int computedValue2 = 25 - (amplifier + 1) * 5;
         result = Math.max(result * computedValue2 / 25.0F, 0.0F);
      }

      if (result <= 0.0F) {
         return 0.0F;
      }

      float protectionAmount = getProtectionAmount(player);
      if (protectionAmount > 0.0F) {
         result *= 1.0F - Math.min(20.0F, protectionAmount) / 25.0F;
      }

      return result;
   }

   public static AABB predictBox(Player player, int ticks) {
      if (player == null) {
         return new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
      } else {
         AABB boundingBox = player.getBoundingBox();
         Vec3 deltaMovement = player.getDeltaMovement();
         if (ticks <= 0) {
            return boundingBox;
         } else {
            double x = deltaMovement.x * ticks;
            double z = deltaMovement.z * ticks;
            double y = deltaMovement.y * ticks;
            if (player.isFallFlying()) {
               return boundingBox.move(x, y, z);
            } else if (!player.onGround()) {
               y += -0.08 * ticks;
               return boundingBox.move(x, y, z);
            } else {
               return boundingBox.move(x, 0.0, z);
            }
         }
      }
   }

   public static AABB crystalOccupancy(BlockPos blockPos) {
      AABB bounds = new AABB(blockPos);
      return bounds.expandTowards(0.0, 1.0, 0.0);
   }

   private static Type raycast(Vec3 position, Vec3 position2, BlockPos blockPos, BlockPos blockPos2) {
      if (mc.level != null && mc.player != null) {
         Vec3 position3 = position;

         for (int value = 0; value < 20; value++) {
            if (blockPos2 != null) {
               AABB bounds = new AABB(blockPos2);
               var optional = bounds.clip(position3, position2);
               BlockHitResult blockHitResult = clip(position3, position2);
               if (optional.isPresent()) {
                  double distance = position3.distanceToSqr((Vec3)optional.get());
                  if ((blockHitResult.getType() != Type.BLOCK || distance <= position3.distanceToSqr(blockHitResult.getLocation()) + 1.0E-6) && (blockPos == null || !blockPos2.equals(blockPos))) {
                     return Type.BLOCK;
                  }
               }
            }

            BlockHitResult blockHitResult2 = clip(position3, position2);
            if (blockHitResult2.getType() != Type.BLOCK) {
               return Type.MISS;
            }

            BlockPos blockPos3 = blockHitResult2.getBlockPos();
            if (blockPos == null || !blockPos3.equals(blockPos)) {
               return Type.BLOCK;
            }

            position3 = nudgePast(blockHitResult2.getLocation(), position2);
         }

         return Type.BLOCK;
      } else {
         return Type.MISS;
      }
   }

   public static boolean isFluidAtCrystal(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         if (!isFluid(blockPos)) {
            BlockPos blockPos2 = blockPos.above();
            if (!isFluid(blockPos2)) {
               return false;
            }
         }

         return true;
      } else {
         return true;
      }
   }

   public static boolean isTotem(ItemStack itemStack) {
      if (itemStack != null) {
         if (!itemStack.isEmpty()) {
            if (itemStack.is(Items.TOTEM_OF_UNDYING)) {
               return true;
            }

            if (itemStack.has(DataComponents.DEATH_PROTECTION)) {
               return true;
            }
         }
      }

      return false;
   }

   private static double squaredDistance(AABB bounds, Vec3 position) {
      double clampedValue = Mth.clamp(position.x, bounds.minX, bounds.maxX);
      double x = clampedValue - position.x;
      double y = Mth.clamp(position.y, bounds.minY, bounds.maxY) - position.y;
      double clampedValue2 = Mth.clamp(position.z, bounds.minZ, bounds.maxZ);
      double z = clampedValue2 - position.z;
      return x * x + y * y + z * z;
   }

   public static boolean intersectsEntityBox(BlockPos blockPos, Entity entity) {
      if (entity != null) {
         AABB boundingBox = entity.getBoundingBox();
         AABB bounds = crystalOccupancy(blockPos);
         if (boundingBox.intersects(bounds)) {
            return true;
         }
      }

      return false;
   }

   public static boolean isFluid(BlockPos blockPos) {
      if (mc.level != null && blockPos != null) {
         FluidState fluidState = mc.level.getFluidState(blockPos);
         if (!fluidState.isEmpty()) {
            if (fluidState.is(FluidTags.WATER)) {
               return true;
            }

            if (fluidState.is(FluidTags.LAVA)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static float remainingHp(Player player) {
      if (player == null) {
         return 0.0F;
      }

      return player.getHealth() + player.getAbsorptionAmount();
   }

   public static boolean isCrystalBase(BlockState blockState) {
      if (blockState != null) {
         if (blockState.is(Blocks.OBSIDIAN)) {
            return true;
         }

         if (blockState.is(Blocks.BEDROCK)) {
            return true;
         }
      }

      return false;
   }

   public static float getDamageOfGhostBlock(Vec3 position, Player player, BlockPos blockPos) {
      AABB bounds;
      if (player == null) {
         bounds = null;
      } else {
         bounds = player.getBoundingBox();
      }

      AABB bounds2 = bounds;
      return getExplosionDamage(position, player, bounds2, null, blockPos);
   }

   private CrystalDamage() {
   }

   public static float getExplosionDamage(Vec3 position, Player player) {
      AABB bounds;
      if (player == null) {
         bounds = null;
      } else {
         bounds = player.getBoundingBox();
      }

      return getExplosionDamage(position, player, bounds, null, null);
   }

   private static Vec3 nudgePast(Vec3 position, Vec3 position2) {
      Vec3 position3 = position2.subtract(position);
      double length = position3.length();
      if (length < 1.0E-4) {
         return position2;
      }

      double intermediate = 0.05 / length;
      Vec3 position4 = position3.scale(intermediate);
      return position.add(position4);
   }

   private static float getProtectionAmount(Player player) {
      float slot = 0.0F;
      slot += protectionPoints(player.getItemBySlot(EquipmentSlot.HEAD));
      slot += protectionPoints(player.getItemBySlot(EquipmentSlot.CHEST));
      slot += protectionPoints(player.getItemBySlot(EquipmentSlot.LEGS));
      slot += protectionPoints(player.getItemBySlot(EquipmentSlot.FEET));
      return Math.min(slot, 20.0F);
   }

   private static float protectionPoints(ItemStack itemStack) {
      float value = enchantLevel(itemStack, Enchantments.PROTECTION);
      return value + enchantLevel(itemStack, Enchantments.BLAST_PROTECTION) * 2.0F;
   }

   public static float getExplosionDamage(Vec3 position, Player player, AABB bounds) {
      return getExplosionDamage(position, player, bounds, null, null);
   }

   public static boolean wouldKill(Player player, float value) {
      return player != null && !hasTotem(player) && value >= remainingHp(player) && value > 0.0F;
   }

   public static float getExplosionDamage(Vec3 position, Player player, AABB bounds, BlockPos blockPos, BlockPos blockPos2) {
      if (mc.level == null || player == null || position == null || bounds == null) {
         return 0.0F;
      }

      if (!player.isInvulnerable() && !player.isCreative() && !player.isSpectator()) {
         if (!bounds.inflate(12.0).contains(position) && bounds.distanceToSqr(position) > 144.0) {
            return 0.0F;
         }

         double computedValue = Math.sqrt(squaredDistance(bounds, position)) / 12.0;
         if (computedValue > 1.0) {
            return 0.0F;
         }

         float exposure = getExposure(position, bounds, blockPos, blockPos2);
         if (exposure <= 0.0F) {
            return 0.0F;
         }

         double computedValue2 = (1.0 - computedValue) * exposure;
         float damage = (float)((computedValue2 * computedValue2 + computedValue2) / 2.0 * 7.0 * 12.0 + 1.0);
         return applyDamageReductions(player, damage);
      } else {
         return 0.0F;
      }
   }
}
