package net.skycore.client.hud;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.skycore.client.SkycoreClient;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.InterBoldHelper;
import net.skycore.client.util.TextHeightHelper;

public final class InformationHud {
   private static final float ICON_SIZE = 11.0F;
   private static final float RADIUS = 8.0F;
   private static final float PAD_LEFT = 6.5F;
   private static final float SECTION_GAP = 6.5F;
   private static final float PAD_RIGHT = 7.5F;
   private static final float HEIGHT = 26.0F;
   private static final float SEPARATOR_HEIGHT = 10.0F;
   private static final float ICON_TEXT_GAP = 3.5F;
   private static final float TEXT_SIZE = 9.5F;
   private final Draggable draggable = new Draggable("information", 0.03F, 0.12F);
   private static final float SEPARATOR_WIDTH = 1.0F;

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round(value3 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      float computedValue = (value >>> 16 & 0xFF) * value2;
      int roundedValue = Math.round(computedValue);
      float computedValue2 = (value >>> 8 & 0xFF) * value2;
      int roundedValue2 = Math.round(computedValue2);
      float computedValue3 = (value & 0xFF) * value2;
      int roundedValue3 = Math.round(computedValue3);
      return value3 << 24 | roundedValue << 16 | roundedValue2 << 8 | roundedValue3;
   }

   private static String formatBps(double value) {
      if (Double.isFinite(value) && !(value < 0.0)) {
         Locale rOOT = Locale.ROOT;
         Object[] objectValues = new Object[]{value};
         return String.format(rOOT, "%.0f", objectValues);
      } else {
         return "0";
      }
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private static float drawSegment(float width, float coordinate, float width2, String text, int color) {
      BeginFrameHelper.TextBuilder textBuilder = BeginFrameHelper.text(width, coordinate, width2, text);
      BeginFrameHelper.TextBuilder textBuilder2 = textBuilder.weight(500);
      BeginFrameHelper.TextBuilder textBuilder3 = textBuilder2.color(color);
      textBuilder3.draw();
      TextHeightHelper textHeightHelper = InterBoldHelper.forWeight(500);
      return width + textHeightHelper.getWidth(text, width2);
   }

   private static void drawBlock(float coordinate, float coordinate2, float coordinate3, float coordinate4, float radius, float radius2, float coordinate5) {
      int value = 1;
      value = (byte)1;
      value = (byte)1;
      if (HudStyle.transparent()) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = HudStyle.background(1.0F);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(shade(value, coordinate5));
         BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.radius(radius).hudGlow(false);
         float value2 = HudStyle.blurRadius(radius2);
         BeginFrameHelper.RectBuilder rectBuilder4 = rectBuilder3.blur(value2, 1.0F);
         rectBuilder4.draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder5 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = shade(-15856114, coordinate5);
         BeginFrameHelper.RectBuilder rectBuilder6 = rectBuilder5.color(value);
         BeginFrameHelper.RectBuilder rectBuilder7 = rectBuilder6.radius(radius);
         rectBuilder7.hudGlow(false).draw();
      }
   }

   public Draggable draggable() {
      return this.draggable;
   }

   public void render(float coordinate, float alpha, boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player == null) {
         this.draggable.setBounds(0.0F, 0.0F);
      } else {
         ThemeManager themeManager = SkycoreClient.themes();
         alpha = clamp(alpha, 0.0F, 1.0F);
         int x = player.blockPosition().getX();
         int y = player.blockPosition().getY();
         int z = player.blockPosition().getZ();
         String text = Integer.toString(x);
         String text2 = Integer.toString(y);
         String text3 = Integer.toString(z);
         double x2 = player.getX() - player.xOld;
         double y2 = player.getY() - player.yOld;
         double z2 = player.getZ() - player.zOld;
         double computedValue = Math.sqrt(x2 * x2 + y2 * y2 + z2 * z2) * 20.0;
         String text4 = formatBps(computedValue);
         float computedValue2 = 26.0F * coordinate;
         float width = 11.0F * coordinate;
         float width2 = 9.5F * coordinate;
         float computedValue3 = 6.5F * coordinate;
         float computedValue4 = 7.5F * coordinate;
         float computedValue5 = 3.5F * coordinate;
         float computedValue6 = 6.5F * coordinate;
         float computedValue7 = 10.0F * coordinate;
         float width3 = InterBoldHelper.forWeight(500).getWidth(text, width2);
         float width4 = InterBoldHelper.forWeight(500).getWidth(text2, width2);
         float width5 = InterBoldHelper.forWeight(500).getWidth(text3, width2);
         float width6 = InterBoldHelper.forWeight(500).getWidth("x", width2);
         float width7 = InterBoldHelper.forWeight(500).getWidth("  ", width2);
         float computedValue8 = width3 + width6 + width7 + width4 + width6 + width7 + width5 + width6;
         float width8 = InterBoldHelper.forWeight(500).getWidth(text4, width2);
         float width9 = InterBoldHelper.forWeight(500).getWidth("bps", width2);
         float computedValue9 = width8 + width9;
         float width10 = InterBoldHelper.csGuiIcons().getWidth("\ue00c", width);
         float width11 = InterBoldHelper.csGuiIcons().getWidth("\ue00a", width);
         float computedValue10 = computedValue3 + width10 + computedValue5 + computedValue8 + computedValue6 + 1.0F + computedValue6 + width11 + computedValue5 + computedValue9 + computedValue4;
         this.draggable.setBounds(computedValue10, computedValue2);
         float x3 = this.draggable.x();
         float y3 = this.draggable.y();
         drawBlock(x3, y3, computedValue10, computedValue2, 8.0F * coordinate + 1.0F, coordinate, alpha);
         int color = shade(themeManager.accent(), alpha);
         int value = shade(alpha(themeManager.textPrimary(), 0.75F), alpha);
         int computedValue11 = shade(alpha(-1, 0.25F), alpha);
         int color2 = shade(alpha(themeManager.textPrimary(), 0.03F), alpha);
         float computedValue12 = y3 + computedValue2 * 0.5F;
         float computedValue13 = computedValue12 - width2 * 0.43F - 0.9F * coordinate - 0.3F;
         float computedValue14 = computedValue12 - computedValue7 * 0.5F;
         float computedValue15 = x3 + computedValue3;
         BeginFrameHelper.text(computedValue15, computedValue13, width, "\ue00c").font(InterBoldHelper.csGuiIcons()).color(color).draw();
         computedValue15 += width10 + computedValue5;
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, text, value);
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, "x", computedValue11);
         computedValue15 += width7;
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, text2, value);
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, "y", computedValue11);
         computedValue15 += width7;
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, text3, value);
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, "z", computedValue11);
         computedValue15 += computedValue6;
         BeginFrameHelper.rect(computedValue15, computedValue14, 1.0F, computedValue7).color(color2).radius(0.5F).hudGlow(false).draw();
         computedValue15 += 1.0F + computedValue6;
         BeginFrameHelper.text(computedValue15, computedValue13, width, "\ue00a").font(InterBoldHelper.csGuiIcons()).color(color).draw();
         computedValue15 += width11 + computedValue5;
         computedValue15 = drawSegment(computedValue15, computedValue13, width2, text4, value);
         drawSegment(computedValue15, computedValue13, width2, "bps", computedValue11);
      }
   }
}
