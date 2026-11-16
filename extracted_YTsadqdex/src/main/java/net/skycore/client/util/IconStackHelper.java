package net.skycore.client.util;

import it.unimi.dsi.fastutil.Hash.Strategy;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.mixin.GuiGraphicsExtractorAccessor;
import org.joml.Matrix3x2f;

public final class IconStackHelper {
   private Object player;
   private static final Strategy<ItemStack> STACKS = new Strategy<ItemStack>() {
      public boolean equals(ItemStack itemStack, ItemStack itemStack2) {
         return itemStack == itemStack2 || itemStack != null && itemStack2 != null && ItemStack.matches(itemStack, itemStack2);
      }

      public int hashCode(ItemStack itemStack) {
         int value = 0;
         value = (byte)0;
         if (itemStack == null) {
            value = 0;
         } else {
            value = 31 * ItemStack.hashItemAndComponents(itemStack) + itemStack.getCount();
         }

         return value;
      }
   };
   private long tick;
   private Object level;
   private boolean showGlint;
   private final Object2ObjectOpenCustomHashMap<ItemStack, IconStackHelper.Entry> entries = new Object2ObjectOpenCustomHashMap<>(STACKS);

   public IconStackHelper() {
      this.tick = Long.MIN_VALUE;
   }

   public static ItemStack iconStack(ItemStack itemStack, boolean condition) {
      if (!condition) {
         if (itemStack.hasFoil()) {
            ItemStack itemStack2 = itemStack.copy();
            DataComponentType eNCHANTMENT_GLINT_OVERRIDE = DataComponents.ENCHANTMENT_GLINT_OVERRIDE;
            Boolean condition2 = false;
            itemStack2.set(eNCHANTMENT_GLINT_OVERRIDE, condition2);
            return itemStack2;
         }
      }

      return itemStack;
   }

   public void clear() {
      this.entries.clear();
      this.level = null;
      this.player = null;
      this.tick = Long.MIN_VALUE;
   }

   public void draw(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, boolean showGlint2) {
      if (!itemStack.isEmpty()) {
         Minecraft instance = Minecraft.getInstance();
         long gameTime = instance.level == null ? 0L : instance.level.getGameTime();
         if (this.level != instance.level || this.player != instance.player || this.tick != gameTime || this.showGlint != showGlint2) {
            this.entries.clear();
            this.level = instance.level;
            this.player = instance.player;
            this.tick = gameTime;
            this.showGlint = showGlint2;
         }

         Identifier texture = itemStack.get(DataComponents.ITEM_MODEL);
         ItemModel itemModel = texture == null ? null : instance.getModelManager().getItemModel(texture);
         IconStackHelper.Entry entry = this.entries.get(itemStack);
         if (entry == null || entry.model != itemModel || entry.state.isAnimated()) {
            ItemStack itemStack2 = itemStack.copy();
            ItemStack itemStack3 = iconStack(itemStack2, showGlint2);
            TrackingItemStackRenderState trackingItemStackRenderState = new TrackingItemStackRenderState();
            instance.getItemModelResolver().updateForTopItem(trackingItemStackRenderState, itemStack3, ItemDisplayContext.GUI, instance.level, instance.player, 0);
            entry = new IconStackHelper.Entry(itemModel, trackingItemStackRenderState);
            if (this.entries.size() >= 512) {
               this.entries.clear();
            }

            this.entries.put(itemStack2, entry);
         }

         ((GuiGraphicsExtractorAccessor)guiGraphicsExtractor).getGuiRenderState().addItem(new GuiItemRenderState(new Matrix3x2f(guiGraphicsExtractor.pose()), entry.state, 0, 0, null));
      }
   }

   public record Entry(Object model, TrackingItemStackRenderState state) {
      public Object model() {
         return this.model;
      }
   }
}
