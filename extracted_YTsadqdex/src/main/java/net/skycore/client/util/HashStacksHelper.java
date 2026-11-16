package net.skycore.client.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntObjectBiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.HashedStack;
import net.minecraft.network.HashedPatchMap.HashGenerator;
import net.minecraft.world.item.ItemStack;

public final class HashStacksHelper {
   public static Int2ObjectMap<HashedStack> hashStacks(Int2ObjectMap<ItemStack> lookup) {
      int size = lookup.size();
      Int2ObjectOpenHashMap lookup2 = new Int2ObjectOpenHashMap(size);
      HashGenerator hashGenerator = stackHasher();
      IntObjectBiConsumer<ItemStack> intObjectBiConsumer = (value, itemStack) -> {
         lookup2.put(value, HashedStack.create(itemStack, hashGenerator));
      };
      lookup.forEach(intObjectBiConsumer);
      return lookup2;
   }

   private HashStacksHelper() {
   }

   public static HashedStack hash(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         return HashedStack.create(itemStack, stackHasher());
      } else {
         return HashedStack.EMPTY;
      }
   }

   public static HashGenerator stackHasher() {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && instance.getConnection() != null) {
         return instance.getConnection().decoratedHashOpsGenenerator();
      } else {
         throw new IllegalStateException("Network handler is not available");
      }
   }
}
