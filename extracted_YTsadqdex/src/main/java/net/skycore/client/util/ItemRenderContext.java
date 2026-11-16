package net.skycore.client.util;

import net.minecraft.world.item.ItemStack;

public final class ItemRenderContext {
   private static final ThreadLocal<ItemStack> RENDER_STACK = new ThreadLocal<>();

   public static void push(ItemStack itemStack) {
      RENDER_STACK.set(itemStack);
   }

   public static void pop() {
      RENDER_STACK.remove();
   }

   public static ItemStack peek() {
      return RENDER_STACK.get();
   }

   private ItemRenderContext() {
   }
}
