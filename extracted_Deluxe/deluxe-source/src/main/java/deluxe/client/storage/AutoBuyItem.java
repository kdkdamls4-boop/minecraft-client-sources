package deluxe.client.storage;

import deluxe.client.util.AttributeBonus;
import deluxe.client.util.EnchantmentRequirement;
import deluxe.client.util.StatusEffectRequirement;

import deluxe.client.MinecraftAccess;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.BiPredicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Formatting;

public record AutoBuyItem(
   String name,
   ItemStack displayStack,
   Item item,
   String category,
   ArrayList<String> tooltip,
   ArrayList<AttributeBonus> attributes,
   ArrayList<StatusEffectRequirement> effects,
   ArrayList<EnchantmentRequirement> enchantments,
   ArrayList<Pair<String, String>> tags
) implements MinecraftAccess {
   public static final BiPredicate<ItemStack, AutoBuyItem> CHECKER = (itemStack, autoBuyItem) -> {
      if (itemStack.getItem() != autoBuyItem.item) {
         return false;
      }

      for (EnchantmentRequirement enchantmentRequirement : autoBuyItem.enchantments) {
         int local7067_4 = 0;

         for (Entry<RegistryEntry<Enchantment>> local7067_6 : itemStack.getEnchantments().getEnchantmentEntries()) {
            if (((RegistryEntry)local7067_6.getKey()).matchesKey(enchantmentRequirement.enchantment())) {
               local7067_4 = local7067_6.getIntValue();
            }
         }

         if (local7067_4 != enchantmentRequirement.level()) {
            return false;
         }
      }

      if (!autoBuyItem.attributes.isEmpty()) {
         AttributeModifiersComponent attributeModifiersComponent = itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
         if (attributeModifiersComponent == null) {
            return false;
         }

         for (AttributeBonus attributeBonus : autoBuyItem.attributes) {
            if (attributeModifiersComponent.modifiers()
               .stream()
               .noneMatch(
                  entry -> attributeBonus.attribute().value() == entry.attribute().value()
                     && attributeBonus.amount() == entry.modifier().value()
                     && attributeBonus.operation() == entry.modifier().operation()
               )) {
               return false;
            }
         }
      }

      if (!autoBuyItem.tooltip.isEmpty()) {
         if (mc.player == null) {
            return false;
         }

         for (String local7067_3 : autoBuyItem.tooltip) {
            if (itemStack.getTooltip(TooltipContext.create(mc.world), mc.player, TooltipType.BASIC)
               .stream()
               .noneMatch(text -> local7067_3.equalsIgnoreCase(Formatting.strip(text.getString())))) {
               return false;
            }
         }
      }

      if (!autoBuyItem.effects.isEmpty()) {
         PotionContentsComponent potionContentsComponent = itemStack.get(DataComponentTypes.POTION_CONTENTS);
         if (potionContentsComponent == null) {
            return false;
         }

         for (StatusEffectRequirement statusEffectRequirement : autoBuyItem.effects) {
            boolean local7067_5 = false;

            for (StatusEffectInstance statusEffectInstance : potionContentsComponent.getEffects()) {
               if (statusEffectInstance.getEffectType().value() == statusEffectRequirement.effect().value()
                  && statusEffectInstance.getDuration() >= statusEffectRequirement.durationTicks()
                  && statusEffectInstance.getAmplifier() >= statusEffectRequirement.amplifier()) {
                  local7067_5 = true;
               }
            }

            if (!local7067_5) {
               return false;
            }
         }
      }

      if (!autoBuyItem.tags.isEmpty()) {
         NbtComponent nbtComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
         if (nbtComponent == null) {
            return false;
         }

         for (Pair<String, String> local7067_4 : autoBuyItem.tags) {
            if (!nbtComponent.copyNbt()
               .getCompoundOrEmpty("PublicBukkitValues")
               .getString((String)local7067_4.getFirst(), "")
               .equalsIgnoreCase((String)local7067_4.getSecond())) {
               return false;
            }
         }
      }

      return true;
   };

   public ItemStack displayStack() {
      return this.displayStack.isEmpty() ? this.item.getDefaultStack() : this.displayStack;
   }

   public static final class Nested0523 {
      private String field14428;
      private String __ = "Прочее";
      private ItemStack field14429 = ItemStack.EMPTY;
      private Item field14430;
      private ArrayList<String> field14431 = new ArrayList<>();
      private ArrayList<AttributeBonus> field14432 = new ArrayList<>();
      private ArrayList<StatusEffectRequirement> field14433 = new ArrayList<>();
      private ArrayList<EnchantmentRequirement> field14434 = new ArrayList<>();
      private ArrayList<Pair<String, String>> field14435 = new ArrayList<>();

      public AutoBuyItem.Nested0523 method4218(String... stringArray) {
         this.field14431.addAll(Arrays.asList(stringArray));
         return this;
      }

      public AutoBuyItem.Nested0523 method4219(AttributeBonus... recoveredClass0021Array) {
         this.field14432.addAll(Arrays.asList(recoveredClass0021Array));
         return this;
      }

      @SafeVarargs
      public final AutoBuyItem.Nested0523 method4220(Pair<String, String>... local7074_1) {
         this.field14435.addAll(Arrays.asList(local7074_1));
         return this;
      }

      public AutoBuyItem.Nested0523 method4221(Item newItem) {
         this.field14430 = newItem;
         return this;
      }

      public AutoBuyItem method4222() {
         if (this.field14430 == null) {
            throw new IllegalArgumentException("No item in Builder");
         } else if (this.field14428 != null && !this.field14428.isEmpty()) {
            return new AutoBuyItem(
               this.field14428, this.field14429, this.field14430, this.__, this.field14431, this.field14432, this.field14433, this.field14434, this.field14435
            );
         } else {
            throw new IllegalArgumentException("No name in Builder");
         }
      }

      public AutoBuyItem.Nested0523 method4223(ItemStack itemStack) {
         this.field14429 = itemStack;
         return this;
      }

      public AutoBuyItem.Nested0523 method4224(StatusEffectRequirement... recoveredClass0114Array) {
         this.field14433.addAll(Arrays.asList(recoveredClass0114Array));
         return this;
      }

      public AutoBuyItem.Nested0523 method4225(EnchantmentRequirement... recoveredClass0089Array) {
         this.field14434.addAll(Arrays.asList(recoveredClass0089Array));
         return this;
      }

      public AutoBuyItem.Nested0523 with__(String new__) {
         this.__ = new__;
         return this;
      }

      public AutoBuyItem.Nested0523 method4227(String local7082_1) {
         this.field14428 = local7082_1;
         return this;
      }
   }
}
