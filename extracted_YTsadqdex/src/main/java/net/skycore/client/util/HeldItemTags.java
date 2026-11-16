package net.skycore.client.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class HeldItemTags {
   static final TagKey<Item> AXES = tag("minecraft", "axes");
   static final TagKey<Item> COPPER_GOLEM_STATUES = tag("minecraft", "copper_golem_statues");
   static final TagKey<Item> SIGNS = tag("minecraft", "signs");
   static final TagKey<Item> MUSIC_DISCS = tag("c", "music_discs");
   static final TagKey<Item> HANGING_SIGNS = tag("minecraft", "hanging_signs");
   static final TagKey<Item> SKULLS = tag("minecraft", "skulls");
   static final TagKey<Item> NUGGETS = tag("c", "nuggets");
   static final TagKey<Item> BUNDLES = tag("minecraft", "bundles");
   static final TagKey<Item> PICKAXES = tag("minecraft", "pickaxes");
   static final TagKey<Item> BEDS = tag("minecraft", "beds");
   static final TagKey<Item> DOORS = tag("minecraft", "doors");
   static final TagKey<Item> SWORDS = tag("minecraft", "swords");
   static final TagKey<Item> SHOVELS = tag("minecraft", "shovels");

   private static TagKey<Item> tag(String text, String text2) {
      ResourceKey iTEM = Registries.ITEM;
      Identifier texture = Identifier.fromNamespaceAndPath(text, text2);
      return TagKey.create(iTEM, texture);
   }
}
