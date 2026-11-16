package net.skycore.client.combat;

import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

public final class MiningTools {
   public static boolean betterTool(ItemStack itemStack, ItemStack itemStack2, float value, float value2, BlockState blockState) {
      if (value > value2 + 1.0E-4F) {
         return true;
      }

      if (value + 1.0E-4F < value2) {
         return false;
      }

      boolean correctToolForDrops = itemStack.isCorrectToolForDrops(blockState);
      boolean correctToolForDrops2 = !itemStack2.isEmpty() && itemStack2.isCorrectToolForDrops(blockState);
      if (correctToolForDrops != correctToolForDrops2) {
         return (boolean)correctToolForDrops;
      }

      float value3 = miningEfficiency(itemStack);
      float value4 = miningEfficiency(itemStack2);
      if (value3 != value4) {
         return value3 > value4;
      }

      int value5 = itemStack.is(Items.SHEARS) && value3 <= 0.0F ? 1 : 0;
      int value6 = itemStack2.is(Items.SHEARS) && value4 <= 0.0F ? 1 : 0;
      return value5 != value6 && value5 == 0;
   }

   public static float destroySpeed(ItemStack itemStack, BlockState blockState) {
      float destroySpeed = itemStack.isEmpty() ? 1.0F : itemStack.getDestroySpeed(blockState);
      return destroySpeed > 1.0F ? destroySpeed + miningEfficiency(itemStack) : destroySpeed;
   }

   private MiningTools() {
   }

   public static float rate(ItemStack itemStack, BlockState blockState) {
      float value = destroySpeed(itemStack, blockState);
      int value2 = blockState.requiresCorrectToolForDrops() && !itemStack.isCorrectToolForDrops(blockState) ? 0 : 1;
      return value / (value2 != 0 ? 30.0F : 100.0F);
   }

   public static int bestHotbarSlot(BlockState blockState) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return -1;
      }

      int index = -1;
      ItemStack eMPTY = ItemStack.EMPTY;
      float index2 = rate(ItemStack.EMPTY, blockState);

      for (int value = 0; value < 9; value++) {
         ItemStack item = instance.getInventory().getItem(value);
         float value2 = rate(item, blockState);
         if (betterTool(item, eMPTY, value2, index2, blockState)) {
            index = value;
            eMPTY = item;
            index2 = value2;
         }
      }

      return index;
   }

   private static float miningEfficiency(ItemStack itemStack) {
      if (itemStack.isEmpty()) {
         return 0.0F;
      }

      Attribute attribute = Attributes.MINING_EFFICIENCY.value();
      double[] doubleValues = new double[]{attribute.getDefaultValue(), 0.0, 1.0};
      itemStack.forEachModifier(EquipmentSlot.MAINHAND, (holder, attributeModifier) -> {
         var optional = Attributes.MINING_EFFICIENCY.unwrapKey();
         ResourceKey resourceKey = (ResourceKey)optional.orElseThrow();
         if (holder.is(resourceKey)) {
            int[] intValues = new int[]{1, 2, 3};
            Operation operation = attributeModifier.operation();
            switch (intValues[operation.ordinal()]) {
               case 1:
                  double value = doubleValues[0];
                  doubleValues[0] = value + attributeModifier.amount();
                  break;
               case 2:
                  double value2 = doubleValues[1];
                  doubleValues[1] = value2 + attributeModifier.amount();
                  break;
               case 3:
                  doubleValues[2] *= 1.0 + attributeModifier.amount();
            }
         }
      });
      double computedValue = doubleValues[0] * (1.0 + doubleValues[1]) * doubleValues[2];
      return (float)attribute.sanitizeValue(computedValue);
   }
}
