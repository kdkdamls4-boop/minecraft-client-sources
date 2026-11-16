package net.skycore.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.CurrentGuiGraphicsExtractorHelper;
import net.skycore.client.util.InterBoldHelper;
import org.joml.Matrix3x2fStack;

public final class ArmorHudHud {
   private static final float CHIP_LEFT_PAD = 4.0F;
   private static final float RING_SIZE = 7.0F;
   private final Draggable draggable = new Draggable("armor_hud", 0.055F, 0.31F);
   private static final float CHIP_HEIGHT = 10.0F;
   private static final float RING_THICKNESS = 1.1F;
   private static final float CHIP_RIGHT_PAD = 3.0F;
   private static final float PAD_LEFT = 6.0F;
   private static final float ROW_RADIUS = 9.5F;
   private static final float ITEM_SIZE = 12.0F;
   private static final float TEXT_SIZE = 7.0F;
   private static final float CHIP_TEXT_RING_GAP = 3.0F;
   private static final float ROW_GAP = 6.0F;
   private static final float ROW_HEIGHT = 24.0F;
   private static final float ITEM_SEPARATOR_GAP = 5.0F;
   private static final String EMPTY_ARMOR_GLYPH = "k";
   private static final float CHIP_RADIUS = 5.0F;
   private static final float PAD_RIGHT = 6.0F;
   private static final float SEPARATOR_CHIP_GAP = 6.0F;
   private static final float SEPARATOR_WIDTH = 1.0F;
   private static final float SEPARATOR_HEIGHT = 10.0F;

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round((value >>> 16 & 0xFF) * value2);
      float computedValue2 = (value >>> 8 & 0xFF) * value2;
      int roundedValue = Math.round(computedValue2);
      float computedValue3 = (value & 0xFF) * value2;
      int roundedValue2 = Math.round(computedValue3);
      return value3 << 24 | computedValue << 16 | roundedValue << 8 | roundedValue2;
   }

   public Draggable draggable() {
      return this.draggable;
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float computedValue = value3 * clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(computedValue);
      return value & 16777215 | roundedValue << 24;
   }

   public void render(float width, float coordinate, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         ItemStack[] itemStackValues = new ItemStack[]{
            player.getItemBySlot(EquipmentSlot.HEAD),
            player.getItemBySlot(EquipmentSlot.CHEST),
            player.getItemBySlot(EquipmentSlot.LEGS),
            player.getItemBySlot(EquipmentSlot.FEET)
         };
         int value = 0;

         for (ItemStack itemStack : itemStackValues) {
            if (itemStack != null && !itemStack.isEmpty()) {
               value++;
            }
         }

         if (value == 0) {
            this.draggable.setBounds(0.0F, 0.0F);
         } else {
            ItemStack[] itemStackValues2 = new ItemStack[value];
            int value2 = 0;

            for (ItemStack itemStack2 : itemStackValues) {
               if (itemStack2 != null && !itemStack2.isEmpty()) {
                  itemStackValues2[value2++] = itemStack2;
               }
            }

            float index = 24.0F * width;
            float index2 = 6.0F * width;
            float index3 = 0.0F;
            float[] floatValues = new float[itemStackValues2.length];

            for (int value3 = 0; value3 < itemStackValues2.length; value3++) {
               floatValues[value3] = rowWidth(itemStackValues2[value3], width);
               index3 = Math.max(index3, floatValues[value3]);
            }

            float index4 = index * itemStackValues2.length + index2 * (itemStackValues2.length - 1);
            this.draggable.setBounds(index3, index4);
            float x = this.draggable.x();
            float y = this.draggable.y();
            float window = instance.getWindow().getGuiScaledWidth() * 0.5F;
            int index5 = x + index3 * 0.5F > window ? 1 : 0;

            for (int value4 = 0; value4 < itemStackValues2.length; value4++) {
               float computedValue = index5 != 0 ? x + index3 - floatValues[value4] : x;
               drawRow(itemStackValues2[value4], computedValue, y + value4 * (index + index2), floatValues[value4], index, width, coordinate);
            }
         }
      }
   }

   private static void drawRow(ItemStack itemStack, float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, float alpha) {
      drawBlock(coordinate, coordinate2, coordinate3, coordinate4, 9.5F * coordinate5, coordinate5, alpha);
      float computedValue = 12.0F * coordinate5;
      float computedValue2 = coordinate + 6.0F * coordinate5;
      float computedValue3 = coordinate2 + (coordinate4 - computedValue) * 0.5F;
      GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
      if (guiGraphicsExtractor != null && itemStack != null && !itemStack.isEmpty()) {
         BeginFrameHelper.flush();
         drawItem(guiGraphicsExtractor, itemStack, computedValue2, computedValue3, computedValue);
      } else {
         float width = InterBoldHelper.skycoreIcons().getWidth("k", computedValue - 1.0F * coordinate5);
         BeginFrameHelper.text(computedValue2 + (computedValue - width) * 0.5F, computedValue3 + 0.2F * coordinate5, computedValue - 1.0F * coordinate5, "k")
            .font(InterBoldHelper.skycoreIcons())
            .color(shade(alpha(-1, 0.1F), alpha))
            .draw();
      }

      float computedValue4 = computedValue2 + computedValue + 5.0F * coordinate5;
      float computedValue5 = 10.0F * coordinate5;
      float computedValue6 = coordinate2 + (coordinate4 - computedValue5) * 0.5F;
      BeginFrameHelper.rect(computedValue4, computedValue6, 1.0F, computedValue5).color(shade(alpha(-1, 0.03F), alpha)).radius(10.0F).hudGlow(false).draw();
      String text = durabilityText(itemStack);
      float width2 = 7.0F * coordinate5;
      float width3 = InterBoldHelper.forWeight(600).getWidth(text, width2);
      float computedValue7 = 7.0F * coordinate5;
      float computedValue8 = 4.0F * coordinate5 + width3 + 3.0F * coordinate5 + computedValue7 + 3.0F * coordinate5;
      float computedValue9 = 10.0F * coordinate5;
      float computedValue10 = computedValue4 + 1.0F + 6.0F * coordinate5;
      float computedValue11 = coordinate2 + (coordinate4 - computedValue9) * 0.5F;
      BeginFrameHelper.rect(computedValue10, computedValue11, computedValue8, computedValue9).color(shade(alpha(-1, 0.02F), alpha)).radius(5.0F * coordinate5).hudGlow(false).draw();
      float computedValue12 = computedValue10 + 4.0F * coordinate5;
      float computedValue13 = computedValue11 + (computedValue9 - width2) * 0.5F - 0.6F * coordinate5;
      BeginFrameHelper.text(computedValue12, computedValue13, width2, text).weight(600).color(shade(alpha(-1, 0.5F), alpha)).draw();
      float computedValue14 = computedValue12 + width3 + 3.0F * coordinate5;
      float computedValue15 = computedValue11 + (computedValue9 - computedValue7) * 0.5F;
      float color = durabilityProgress(itemStack);
      BeginFrameHelper.ring(computedValue14, computedValue15, computedValue7).thickness(Math.max(1.0F, 1.1F * coordinate5)).color(shade(alpha(-1, 0.03F), alpha)).progress(1.0F).draw();
      if (color > 0.0F) {
         BeginFrameHelper.ring(computedValue14, computedValue15, computedValue7)
            .thickness(Math.max(1.0F, 1.1F * coordinate5))
            .color(shade(SkycoreClient.themes().accent(), alpha))
            .progress(color)
            .draw();
      }
   }

   private static String durabilityText(ItemStack itemStack) {
      int value = 1;
      value = (byte)1;
      if (itemStack != null) {
         if (!itemStack.isEmpty() && itemStack.getMaxDamage() > 0) {
            value = itemStack.getMaxDamage();
            return Integer.toString(Math.max(0, value - itemStack.getDamageValue()));
         }
      }

      return "0";
   }

   private static float rowWidth(ItemStack itemStack, float value) {
      String text = durabilityText(itemStack);
      float width = InterBoldHelper.forWeight(600).getWidth(text, 7.0F * value);
      float computedValue = 4.0F * value + width + 3.0F * value + 7.0F * value + 3.0F * value;
      return 6.0F * value + 12.0F * value + 5.0F * value + 1.0F + 6.0F * value + computedValue + 6.0F * value;
   }

   private static float durabilityProgress(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         int maxDamage = itemStack.getMaxDamage();
         if (maxDamage <= 0) {
            return 0.0F;
         }

         float damageValue = (float)(maxDamage - itemStack.getDamageValue()) / maxDamage;
         return clamp(damageValue, 0.0F, 1.0F);
      } else {
         return 0.0F;
      }
   }

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float color, float radius, float coordinate5) {
      int value = 1;
      value = (byte)-1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = HudStyle.background(1.0F);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(shade(value, coordinate5)).radius(color).hudGlow(false).blur(HudStyle.blurRadius(radius), 1.0F);
         rectBuilder2.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder3 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = shade(-15856114, coordinate5);
         BeginFrameHelper.RectBuilder rectBuilder4 = rectBuilder3.color(value).radius(color);
         rectBuilder4.hudGlow(false).draw();
      }
   }

   private static void drawItem(GuiGraphicsExtractor guiGraphicsExtractor, ItemStack itemStack, float coordinate, float coordinate2, float coordinate3) {
      if (!itemStack.isEmpty()) {
         BeginFrameHelper.hudItemGlow(coordinate, coordinate2, coordinate3);
      }

      Minecraft instance = Minecraft.getInstance();
      Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
      float guiScale = instance.getWindow().getGuiScale();
      float computedValue = coordinate3 / 16.0F;
      float computedValue2 = Math.round(coordinate * guiScale) / guiScale;
      float computedValue3 = Math.round(coordinate2 * guiScale) / guiScale;
      matrix3x2fStack.pushMatrix();
      matrix3x2fStack.translate(computedValue2, computedValue3);
      matrix3x2fStack.scale(computedValue);
      guiGraphicsExtractor.item(itemStack, 0, 0);
      matrix3x2fStack.popMatrix();
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }
}
