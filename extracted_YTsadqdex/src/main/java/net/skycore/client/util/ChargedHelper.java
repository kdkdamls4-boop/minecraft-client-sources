package net.skycore.client.util;

import java.util.HashMap;
import java.util.Iterator;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;

public final class ChargedHelper {
   public static SpearAnimationFlags spear(ItemStack itemStack) {
      return new SpearAnimationFlags(true, false, false, false);
   }

   public static boolean charged(ItemStack itemStack) {
      return CrossbowItem.isCharged(itemStack);
   }

   static boolean isPlaceableBlock(ItemStack itemStack) {
      return isBlockItem(itemStack);
   }

   static boolean matches(ItemStack itemStack, Item item) {
      return itemStack.is(item);
   }

   static boolean matches(ItemStack itemStack, TagKey<Item> tagKey) {
      return itemStack.is(tagKey);
   }

   static boolean isDecorativeItem(ItemStack itemStack) {
      byte flag;
      toolClassification: {
         if (!(itemStack.getItem() instanceof FishingRodItem) && !itemStack.is(ConventionalItemTags.RODS)) {
            if (!itemStack.is(ConventionalItemTags.TOOLS)) {
               if (!itemStack.is(ItemTags.SWORDS)) {
                  if (!itemStack.is(ConventionalItemTags.MACE_TOOLS) && itemStack.getUseAnimation() != ItemUseAnimation.BLOCK) {
                     flag = 0;
                     break toolClassification;
                  }
               }
            }
         }

         flag = 1;
      }

      byte flag2 = flag;
      if (flag2 != 0 || !(damage(itemStack) <= 0.0F)) {
         ItemUseAnimation useAnimation = itemStack.getUseAnimation();
         if (useAnimation != ItemUseAnimation.EAT) {
            if (itemStack.getUseAnimation() != ItemUseAnimation.DRINK && itemStack.getUseAnimation() != ItemUseAnimation.SPYGLASS) {
               return false;
            }
         }
      }

      return true;
   }

   static boolean hasCustomSwing(ItemStack itemStack) {
      var lookup = HeldItemAnimationState.INSTANCE.customSwingItems;
      String text = id(itemStack);
      Boolean orDefault = lookup.getOrDefault(text, false);
      return orDefault;
   }

   public static String kind(ItemStack itemStack) {
      return itemStack.getUseAnimation().getSerializedName();
   }

   public static boolean projectile(ItemStack itemStack) {
      return itemStack.getItem() instanceof SplashPotionItem || itemStack.getItem() instanceof ProjectileItem;
   }

   public static boolean empty(ItemStack itemStack) {
      return itemStack.isEmpty();
   }

   static boolean isLantern(ItemStack itemStack) {
      Item item = itemStack.getItem();
      return Block.byItem(item) instanceof LanternBlock;
   }

   static boolean isBlockItem(ItemStack itemStack) {
      Item item = itemStack.getItem();
      Block block = Block.byItem(item);
      return block != Blocks.AIR;
   }

   public static String id(ItemStack itemStack) {
      Identifier key = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
      return key.toString();
   }

   public static float damage(ItemStack itemStack) {
      ItemAttributeModifiers itemAttributeModifiers = itemStack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (itemAttributeModifiers == null) {
         return 0.0F;
      }

      float result = 0.0F;
      Iterator iterator = itemAttributeModifiers.modifiers().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return result;
         }

         Entry element = (Entry)iterator.next();
         if (element.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
            result += (float)element.modifier().amount();
         }
      }
   }
}
