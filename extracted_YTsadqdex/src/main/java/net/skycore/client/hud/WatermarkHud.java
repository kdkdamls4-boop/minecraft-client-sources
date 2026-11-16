package net.skycore.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.skycore.client.SkycoreClient;
import net.skycore.client.profile.LocalProfile;
import net.skycore.client.theme.ThemeManager;
import net.skycore.client.util.BeginFrameHelper;
import net.skycore.client.util.DrawDigitHelper;
import net.skycore.client.util.InterBoldHelper;

public final class WatermarkHud {
   private static final float LOGO_SIZE = 12.0F;
   private int previousFps;
   private long lastFpsSampleAt;
   private static final String LOGO_GLYPH = "a";
   private static final float SECTION_GAP = 6.5F;
   private static final float ICON_SIZE = 11.0F;
   private long lastPingSampleAt;
   private int displayedPing;
   private static final long VALUE_TRANSITION_MS = 280L;
   private static final float ICON_TEXT_GAP = 3.5F;
   private int previousPing;
   private int displayedFps;
   private static final float SEPARATOR_HEIGHT = 10.0F;
   private static final float TEXT_SIZE = 9.5F;
   private static final float OUTER_LEFT = 6.5F;
   private long fpsTransitionAt;
   private static final long PING_SAMPLE_MS = 5000L;
   private final Draggable draggable = new Draggable("watermark", 0.0065F, 0.0115F);
   private static final float OUTER_RIGHT = 7.5F;
   private static final float HEIGHT = 26.0F;
   private static final float RADIUS = 8.0F;
   private static final long FPS_SAMPLE_MS = 1500L;
   private long pingTransitionAt;
   private static final float SEPARATOR_WIDTH = 1.0F;

   public Draggable draggable() {
      return this.draggable;
   }

   private void drawBackground(float coordinate, float coordinate2, float coordinate3, float coordinate4, float radius, float radius2, float coordinate5, boolean condition) {
      int value = 1;
      if (condition) {
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4);
         value = HudStyle.background(1.0F);
         value = shade(value, coordinate5);
         BeginFrameHelper.RectBuilder rectBuilder2 = rectBuilder.color(value);
         BeginFrameHelper.RectBuilder rectBuilder3 = rectBuilder2.radius(radius).hudGlow(false);
         float value2 = HudStyle.blurRadius(radius2);
         rectBuilder3.blur(value2, 1.0F).draw();
      } else {
         BeginFrameHelper.RectBuilder rectBuilder4 = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(shade(-15856114, coordinate5));
         BeginFrameHelper.RectBuilder rectBuilder5 = rectBuilder4.radius(radius).hudGlow(false);
         rectBuilder5.draw();
      }
   }

   public void render(float coordinate, float alpha) {
      Minecraft instance = Minecraft.getInstance();
      ThemeManager themeManager = SkycoreClient.themes();
      alpha = clamp(alpha, 0.0F, 1.0F);
      this.updateFps(instance);
      this.updatePing(instance);
      String text = displayUsername();
      String text2 = Integer.toString(this.displayedFps);
      String text3 = Integer.toString(this.previousFps);
      String text4 = Integer.toString(this.displayedPing);
      String text5 = Integer.toString(this.previousPing);
      float x = this.draggable.x();
      float y = this.draggable.y();
      float computedValue = 26.0F * coordinate;
      float computedValue2 = 8.0F * coordinate + 1.0F;
      float width = 12.0F * coordinate;
      float width2 = 11.0F * coordinate;
      float width3 = 9.5F * coordinate;
      float computedValue3 = 6.5F * coordinate;
      float computedValue4 = 3.5F * coordinate;
      float computedValue5 = 1.0F * coordinate + 0.5F;
      float computedValue6 = 10.0F * coordinate;
      float computedValue7 = 6.5F * coordinate;
      float computedValue8 = 7.5F * coordinate;
      float width4 = InterBoldHelper.skycore().getWidth("a", width);
      float width5 = InterBoldHelper.clickGuiIcons().getWidth("e", width2);
      float width6 = InterBoldHelper.clickGuiIcons().getWidth("f", width2);
      float width7 = InterBoldHelper.clickGuiIcons().getWidth("k", width2);
      float width8 = InterBoldHelper.forWeight(500).getWidth(text, width3);
      long nowNanos = System.nanoTime();
      float value = DrawDigitHelper.slotWidth(text3, text2, width3, 500, this.fpsTransitionAt, nowNanos, 280L, true);
      float value2 = DrawDigitHelper.slotWidth(text5, text4, width3, 500, this.pingTransitionAt, nowNanos, 280L, true);
      float width9 = InterBoldHelper.forWeight(500).getWidth("fps", width3);
      float width10 = InterBoldHelper.forWeight(500).getWidth("ms", width3);
      float computedValue9 = computedValue7
         + width4
         + computedValue3
         + computedValue5
         + computedValue3
         + width5
         + computedValue4
         + width8
         + computedValue3
         + computedValue5
         + computedValue3
         + width6
         + computedValue4
         + value
         + width9
         + computedValue3
         + computedValue5
         + computedValue3
         + width7
         + computedValue4
         + value2
         + width10
         + computedValue8;
      this.draggable.setBounds(computedValue9, computedValue);
      boolean transparent = HudStyle.transparent();
      this.drawBackground(x, y, computedValue9, computedValue, computedValue2, coordinate, alpha, transparent);
      int color = shade(themeManager.accent(), alpha);
      int color2 = shade(alpha(themeManager.textPrimary(), 0.75F), alpha);
      int color3 = shade(alpha(-1, 0.25F), alpha);
      int value3 = shade(alpha(themeManager.textPrimary(), 0.03F), alpha);
      float computedValue10 = y + computedValue * 0.5F;
      float computedValue11 = computedValue10 - width3 * 0.43F - 0.9F * coordinate - 0.3F;
      float computedValue12 = computedValue10 - width2 * 0.5F - 1.0F * coordinate - 4.5F;
      float computedValue13 = computedValue10 - width * 0.5F - 1.0F * coordinate + 0.5F;
      float computedValue14 = computedValue10 - computedValue6 * 0.5F;
      float computedValue15 = x + computedValue7;
      BeginFrameHelper.text(computedValue15, computedValue13, width, "a").font(InterBoldHelper.skycore()).color(color).draw();
      computedValue15 += width4;
      computedValue15 += computedValue3;
      this.drawSeparator(computedValue15, computedValue14, computedValue5, computedValue6, coordinate, value3);
      computedValue15 += computedValue5 + computedValue3;
      BeginFrameHelper.text(computedValue15, computedValue12, width2, "e").font(InterBoldHelper.clickGuiIcons()).color(color).draw();
      computedValue15 += width5 + computedValue4;
      BeginFrameHelper.text(computedValue15, computedValue11, width3, text).weight(500).color(color2).draw();
      computedValue15 += width8;
      computedValue15 += computedValue3;
      this.drawSeparator(computedValue15, computedValue14, computedValue5, computedValue6, coordinate, value3);
      computedValue15 += computedValue5 + computedValue3;
      BeginFrameHelper.text(computedValue15, computedValue12, width2, "f").font(InterBoldHelper.clickGuiIcons()).color(color).draw();
      computedValue15 += width6 + computedValue4;
      DrawDigitHelper.renderLeft(computedValue15, computedValue11, width3, text3, text2, this.fpsTransitionAt, nowNanos, 280L, color2, 1.0F, 500, width3 * 0.85F, true);
      computedValue15 += value;
      BeginFrameHelper.text(computedValue15, computedValue11, width3, "fps").weight(500).color(color3).draw();
      computedValue15 += width9;
      computedValue15 += computedValue3;
      this.drawSeparator(computedValue15, computedValue14, computedValue5, computedValue6, coordinate, value3);
      computedValue15 += computedValue5 + computedValue3;
      BeginFrameHelper.text(computedValue15, computedValue12, width2, "k").font(InterBoldHelper.clickGuiIcons()).color(color).draw();
      computedValue15 += width7 + computedValue4;
      DrawDigitHelper.renderLeft(computedValue15, computedValue11, width3, text5, text4, this.pingTransitionAt, nowNanos, 280L, color2, 1.0F, 500, width3 * 0.85F, true);
      computedValue15 += value2;
      BeginFrameHelper.text(computedValue15, computedValue11, width3, "ms").weight(500).color(color3).draw();
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private void drawSeparator(float coordinate, float coordinate2, float coordinate3, float coordinate4, float coordinate5, int color) {
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(coordinate, coordinate2, coordinate3, coordinate4).color(color);
      float intermediate = coordinate3 * 0.5F;
      float maximum = Math.max(0.5F, intermediate);
      rectBuilder.radius(maximum).hudGlow(false).draw();
   }

   private void updatePing(Minecraft minecraft) {
      long nowNanos = System.nanoTime();
      long timestamp = nowNanos / 1000000L;
      if (this.lastPingSampleAt == 0L || timestamp - this.lastPingSampleAt >= 5000L) {
         int value = 0;
         if (minecraft.player != null && minecraft.player.connection != null) {
            PlayerInfo playerInfo = minecraft.player.connection.getPlayerInfo(minecraft.player.getUUID());
            if (playerInfo != null) {
               value = Math.max(0, playerInfo.getLatency());
            }
         }

         if (this.lastPingSampleAt == 0L) {
            this.displayedPing = value;
            this.previousPing = value;
         } else if (value != this.displayedPing) {
            this.previousPing = this.displayedPing;
            this.displayedPing = value;
            this.pingTransitionAt = nowNanos;
         }

         this.lastPingSampleAt = timestamp;
      }
   }

   public void render(float coordinate) {
      this.render(coordinate, 1.0F);
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round(value3 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   private static int shade(int value, float value2) {
      value2 = clamp(value2, 0.0F, 1.0F);
      int value3 = value >>> 24 & 0xFF;
      int computedValue = Math.round((value >>> 16 & 0xFF) * value2);
      int computedValue2 = Math.round((value >>> 8 & 0xFF) * value2);
      float computedValue3 = (value & 0xFF) * value2;
      int roundedValue = Math.round(computedValue3);
      return value3 << 24 | computedValue << 16 | computedValue2 << 8 | roundedValue;
   }

   private void updateFps(Minecraft minecraft) {
      long nowNanos = System.nanoTime();
      long timestamp = nowNanos / 1000000L;
      if (this.lastFpsSampleAt == 0L || timestamp - this.lastFpsSampleAt >= 1500L) {
         int fps = Math.max(0, minecraft.getFps());
         if (this.lastFpsSampleAt == 0L) {
            this.displayedFps = fps;
            this.previousFps = fps;
         } else if (fps != this.displayedFps) {
            this.previousFps = this.displayedFps;
            this.displayedFps = fps;
            this.fpsTransitionAt = nowNanos;
         }

         this.lastFpsSampleAt = timestamp;
      }
   }

   public static String displayUsername() {
      String instance = LocalProfile.getInstance().profile("username");
      if (instance != null) {
         if (!instance.isBlank()) {
            return instance.trim();
         }
      }

      return LocalProfile.DISPLAY_NAME;
   }
}
