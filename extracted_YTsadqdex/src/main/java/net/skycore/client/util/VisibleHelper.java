package net.skycore.client.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Iterator;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;

public final class VisibleHelper {
   private static final Random RANDOM = new Random();

   public static boolean visible(Vec3 position) {
      Minecraft instance = Minecraft.getInstance();
      ClientLevel level = instance.level;
      Vec3 eyePosition = instance.player.getEyePosition();
      return level.clip(new ClipContext(eyePosition, position, Block.COLLIDER, Fluid.NONE, instance.player)).getType() == Type.MISS;
   }

   private VisibleHelper() {
   }

   public static double relativeDegrees(Vec3 position, float value) {
      float angleRadians = (float)Math.atan2(-position.x, position.z);
      double value2 = angleRadians;
      return Math.toDegrees(Mth.wrapDegrees(value2 - Math.toRadians(Mth.wrapDegrees(value))));
   }

   public static Input directionalInput(Input input, double value, float value2) {
      int computedValue = value >= -90.0F + value2 && value <= 90.0F - value2 ? 1 : 0;
      int computedValue2 = computedValue != 0 || !(value < -90.0F - value2) && !(value > 90.0F + value2) ? 0 : 1;
      int computedValue3 = value >= value2 && value <= 180.0F - value2 ? 1 : 0;
      int computedValue4 = computedValue3 == 0 && value >= -180.0F + value2 && value <= -value2 ? 1 : 0;
      return new Input(
         computedValue != 0 || input.forward(),
         computedValue2 != 0 || input.backward(),
         computedValue4 != 0 || input.left(),
         computedValue3 != 0 || input.right(),
         input.jump(),
         input.shift(),
         input.sprint()
      );
   }

   public static float random(float value, float value2) {
      return value + RANDOM.nextFloat() * (value2 - value);
   }

   public static int enchantment(ItemStack itemStack, ResourceKey<Enchantment> resourceKey) {
      ItemEnchantments enchantments = itemStack.getEnchantments();
      Iterator iterator = enchantments.entrySet().iterator();

      Entry entry;
      do {
         if (!iterator.hasNext()) {
            return 0;
         }

         entry = (Entry)iterator.next();
      } while (!((Holder)entry.getKey()).is(resourceKey));

      return entry.getIntValue();
   }

   public static boolean betterTool(ItemStack itemStack, ItemStack itemStack2, float value, float value2, BlockState blockState) {
      if (blockState != null && blockState.is(Blocks.COBWEB)) {
         boolean is = itemStack.is(Items.SHEARS);
         boolean is2 = itemStack2.is(Items.SHEARS);
         if (is != is2) {
            return is;
         }
      }

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

      float value3 = EfficiencyHelper.efficiency(itemStack);
      float value4 = EfficiencyHelper.efficiency(itemStack2);
      if (value3 != value4) {
         return value3 > value4;
      }

      int value5 = itemStack.is(Items.SHEARS) && value3 <= 0.0F ? 1 : 0;
      int value6 = itemStack2.is(Items.SHEARS) && value4 <= 0.0F ? 1 : 0;
      return value5 != value6 && value5 == 0;
   }

   public static int bestHotbar(BlockState blockState) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance == null) {
         return -1;
      }

      int index = -1;
      ItemStack eMPTY = ItemStack.EMPTY;
      float index2 = EfficiencyHelper.rate(eMPTY, blockState);

      for (int value = 0; value < 9; value++) {
         ItemStack item = instance.getInventory().getItem(value);
         float value2 = EfficiencyHelper.rate(item, blockState);
         if (betterTool(item, eMPTY, value2, index2, blockState)) {
            index = value;
            eMPTY = item;
            index2 = value2;
         }
      }

      return index;
   }

   public static ItemStack bestTool(BlockState blockState) {
      return VisualSlotHelper.getBestAvailableTool(blockState);
   }
}
