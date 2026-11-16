package net.skycore.client.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class EfficiencyHelper {
   private static int cacheTick;
   private static ResourceKey<Attribute> miningEfficiencyKey;
   private static Map<BlockState, Float> miningRateCache;

   public static float ticks(BlockPos blockPos) {
      Minecraft instance = Minecraft.getInstance();
      float result;
      if (instance.level == null) {
         result = Float.MAX_VALUE;
      } else {
         BlockState blockState = Minecraft.getInstance().level.getBlockState(blockPos);
         result = ticks(blockState, blockPos);
      }

      return result;
   }

   public static float efficiency(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0F;
      }

      if (miningEfficiencyKey == null) {
         miningEfficiencyKey = Attributes.MINING_EFFICIENCY.unwrapKey().orElseThrow();
      }

      Attribute attribute = Attributes.MINING_EFFICIENCY.value();
      double[] doubleValues = new double[]{attribute.getDefaultValue(), 0.0, 1.0};
      itemStack.forEachModifier(EquipmentSlot.MAINHAND, (holder, attributeModifier) -> {
         if (holder.is(miningEfficiencyKey)) {
            int[] intValues = new int[]{1, 2, 3};
            Operation operation = attributeModifier.operation();
            switch (intValues[operation.ordinal()]) {
               case 1:
                  doubleValues[0] += attributeModifier.amount();
                  break;
               case 2:
                  doubleValues[1] += attributeModifier.amount();
                  break;
               case 3:
                  doubleValues[2] *= 1.0 + attributeModifier.amount();
            }
         }
      });
      double computedValue = doubleValues[0] * (1.0 + doubleValues[1]) * doubleValues[2];
      return (float)attribute.sanitizeValue(computedValue);
   }

   public static float ticks(BlockState blockState, BlockPos blockPos) {
      int value = 0;
      value = (byte)0;
      value = (byte)0;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         if (Minecraft.getInstance().level != null) {
            value = cacheTick;
            Minecraft instance2 = Minecraft.getInstance();
            if (value != instance2.player.tickCount) {
               miningRateCache.clear();
               cacheTick = Minecraft.getInstance().player.tickCount;
            }

            Float value2 = miningRateCache.get(blockState);
            if (value2 != null) {
               return value2;
            }

            float result = computeTicks(blockState, blockPos);
            miningRateCache.put(blockState, result);
            return result;
         }
      }

      return Float.MAX_VALUE;
   }

   static {
      initialize();
   }

   public static float destroySpeed(ItemStack itemStack, BlockState blockState) {
      float destroySpeed = itemStack.isEmpty() ? 1.0F : itemStack.getDestroySpeed(blockState);
      if (destroySpeed > 1.0F) {
         destroySpeed += efficiency(itemStack);
      }

      return destroySpeed;
   }

   public static int divisor(ItemStack itemStack, BlockState blockState) {
      byte flag;
      efficiencyCalculation: {
         flag = 0;
         flag = 0;
         if (blockState.requiresCorrectToolForDrops()) {
            if (!itemStack.isCorrectToolForDrops(blockState)) {
               flag = 0;
               break efficiencyCalculation;
            }
         }

         flag = 1;
      }

      byte flag2 = flag;
      return flag2 != 0 ? 30 : 100;
   }

   public static float rate(ItemStack itemStack, BlockState blockState) {
      return destroySpeed(itemStack, blockState) / divisor(itemStack, blockState);
   }

   private EfficiencyHelper() {
   }

   private static float computeTicks(BlockState blockState, BlockPos blockPos) {
      float destroyProgress = blockState.getDestroyProgress(Minecraft.getInstance().player, Minecraft.getInstance().level, blockPos);
      if (destroyProgress <= 0.0F) {
         return Float.MAX_VALUE;
      }

      Minecraft instance = Minecraft.getInstance();
      ItemStack selectedItem = instance.player.getInventory().getSelectedItem();
      float value = rate(selectedItem, blockState);
      ItemStack itemStack = VisibleHelper.bestTool(blockState);
      float value2 = rate(itemStack, blockState);
      if (value > 0.0F && value2 > 0.0F) {
         destroyProgress *= value2 / value;
      }

      return 1.0F / destroyProgress;
   }

   public static void initialize() {
      HashMap lookup = new HashMap();
      miningRateCache = lookup;
      cacheTick = Integer.MIN_VALUE;
   }
}
