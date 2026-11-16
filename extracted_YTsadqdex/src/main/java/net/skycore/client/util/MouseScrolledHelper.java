package net.skycore.client.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.skycore.client.SkycoreClient;
import net.skycore.client.account.AccountManager;
import net.skycore.client.account.SkinService;

public final class MouseScrolledHelper extends Screen {
   private float altScroll;
   private static final Identifier ENGLISH_FLAG = Identifier.parse("skycore:textures/gui/language/english.png");
   private float nickHover;
   private static final Identifier[][] GREETING_FRAMES = new Identifier[][]{greetingFrames("en"), greetingFrames("ru"), greetingFrames("pl")};
   private long altAnimAt;
   private boolean altView;
   private static final Identifier[] BACKGROUNDS = new Identifier[]{Identifier.parse("skycore:textures/gui/main_menu_new.png")};
   private static final Identifier RUSSIAN_FLAG = Identifier.parse("skycore:textures/gui/language/russian.png");
   private static final int TEXT_IDLE = -1275068417;
   private final float[] languageHover;
   private static final int GREETING_FRAME_COUNT = 48;
   private static final long GREETING_ERASE_MS = 1650L;
   private static final long TYPE_LINE_GAP_MS = 280L;
   private final List<MouseScrolledHelper.MenuRegion> regions = new ArrayList<>();
   private static final long ALT_TRANSITION_MS = 620L;
   private boolean nickFocused;
   private static final long ENTER_MS = 620L;
   private static final long STAGGER_MS = 70L;
   private static final int EXIT_HOVER = -50373;
   private boolean quoteAlt;
   private static final MouseScrolledHelper.QuoteLine QUOTE_BOTTOM = new MouseScrolledHelper.QuoteLine(
      new MouseScrolledHelper.QuotePart("Core", true), new MouseScrolledHelper.QuotePart(" stays quiet.", false)
   );
   private long lastFrame;
   private final Map<String, Float> altHover;
   private float altMaxScroll;
   private boolean altAnimToAlt;
   private static final Identifier[] LANGUAGE_PROMPTS = new Identifier[]{
      Identifier.parse("skycore:textures/gui/language/choose_your_language.png"), Identifier.parse("skycore:textures/gui/language/choose_your_language_ru.png")
   };
   private long openedAt;
   private static final int BUTTON_IDLE = 184549375;
   private static final int BORDER_IDLE = 218103807;
   private static final float BACKGROUND_ASPECT = 1.7768332F;
   private static final MouseScrolledHelper.QuoteLine QUOTE_ALT_BOTTOM = new MouseScrolledHelper.QuoteLine(
      new MouseScrolledHelper.QuotePart("Session", true), new MouseScrolledHelper.QuotePart(" is yours.", false)
   );
   private static final long TYPE_START_MS = 320L;
   private static final int TEXT_HOVER = -1;
   private static final int EXIT_IDLE = -1711276033;
   private float frameDelta;
   private static final MouseScrolledHelper.QuoteLine QUOTE_TOP = new MouseScrolledHelper.QuoteLine(
      new MouseScrolledHelper.QuotePart("Sky", true), new MouseScrolledHelper.QuotePart(" above the noise.", false)
   );
   private static final int QUOTE_BASE = -419430401;
   private static final long TYPE_CHAR_MS = 26L;
   private String nickBuffer;
   private final float[] hover = new float[5];
   private static final long GREETING_HOLD_MS = 700L;
   private long quoteAt;
   private static final MouseScrolledHelper.QuoteLine QUOTE_ALT_TOP = new MouseScrolledHelper.QuoteLine(
      new MouseScrolledHelper.QuotePart("Alts", true), new MouseScrolledHelper.QuotePart(" lined up.", false)
   );
   private static final long GREETING_DRAW_MS = 1650L;

   @Override
   public boolean mouseScrolled(double value, double value2, double value3, double value4) {
      if (!SelectAndSaveHelper.hasSavedSelection()) {
         return true;
      }

      if (this.altBlend() > 0.45F) {
         if (this.altMaxScroll > 0.0F) {
            float computedValue = this.altScroll - (float)value4 * 28.0F;
            this.altScroll = clamp(computedValue, 0.0F, this.altMaxScroll);
            return true;
         }
      }

      return false;
   }

   private void drawBackgroundLayer(Identifier texture, float alpha, float coordinate) {
      if (!(alpha <= 0.01F)) {
         float maximum = this.width / Math.max(1.0F, this.height);
         float value = 0.0F;
         float value2 = 0.0F;
         float value3 = 1.0F;
         float value4 = 1.0F;
         if (maximum > 1.7768332F) {
            float computedValue = 1.7768332F / maximum;
            value2 = (1.0F - computedValue) * 0.5F;
            value4 = 1.0F - value2;
         } else if (maximum < 1.7768332F) {
            float computedValue2 = maximum / 1.7768332F;
            value = (1.0F - computedValue2) * 0.5F;
            value3 = 1.0F - value;
         }

         float maximum2 = Math.max(1.0F, coordinate);
         float computedValue3 = (value + value3) * 0.5F;
         float computedValue4 = (value2 + value4) * 0.5F;
         float computedValue5 = (value3 - value) * 0.5F / maximum2;
         float computedValue6 = (value4 - value2) * 0.5F / maximum2;
         BeginFrameHelper.texture(0.0F, 0.0F, this.width, this.height, texture)
            .uv(computedValue3 - computedValue5, computedValue4 - computedValue6, computedValue3 + computedValue5, computedValue4 + computedValue6)
            .color(withAlpha(-1, alpha))
            .draw();
      }
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor guiGraphicsExtractor, int value, int value2, float value3) {
   }

   private static float spring(float value) {
      float clampedValue = clamp(value, 0.0F, 1.0F);
      if (clampedValue <= 0.0F) {
         return 0.0F;
      }

      if (clampedValue >= 1.0F) {
         return 1.0F;
      }

      float value2 = 1.70158F;
      float computedValue = value2 + 1.0F;
      float computedValue2 = clampedValue - 1.0F;
      return 1.0F + computedValue * computedValue2 * computedValue2 * computedValue2 + value2 * computedValue2 * computedValue2;
   }

   private void renderBackgrounds() {
      this.drawBackgroundLayer(BACKGROUNDS[0], 1.0F, 1.0F);
      float width = this.width;
      float height = this.height;
      BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(0.0F, 0.0F, width, height).color(-671088640);
      rectBuilder.draw();
   }

   private void removeAlt(String text) {
      AccountManager.INSTANCE.remove(text);
      String text2;
      if (text == null) {
         text2 = null;
      } else {
         text2 = text.toLowerCase(Locale.ROOT);
      }

      this.altHover.remove(text2);
   }

   private static float measureVisible(MouseScrolledHelper.QuoteLine quoteLine, float width, int index) {
      float width2 = 0.0F;
      int index2 = index;

      for (MouseScrolledHelper.QuotePart quotePart : quoteLine.parts) {
         if (index2 <= 0) {
            break;
         }

         String text = quotePart.text.substring(0, Math.min(index2, quotePart.text.length()));
         if (!text.isEmpty()) {
            width2 += InterBoldHelper.sfRoundedMedium().getWidth(text, width);
         }

         index2 -= quotePart.text.length();
      }

      return width2;
   }

   private void renderLegalNotice() {
      if (SelectAndSaveHelper.hasSavedSelection()) {
         String text = SelectAndSaveHelper.isRussian() ? "SkyCore не имеет отношения к Mojang" : "SkyCore is not affiliated with Mojang";
         String text2 = SelectAndSaveHelper.isRussian() ? "Все права защищены" : "All rights reserved";
         float value = this.uiScale();
         float width = 8.5F * value;
         float computedValue = 4.0F * value;
         float width2 = this.height - 17.0F * value;
         float width3 = width2 - width - computedValue;
         TextHeightHelper textHeightHelper = InterBoldHelper.sfRoundedMedium();
         BeginFrameHelper.text(this.width * 0.5F, width3, width, text).font(textHeightHelper).align(TextAlignment.CENTER).color(-1929379841).draw();
         BeginFrameHelper.text(this.width * 0.5F, width2, width, text2).font(textHeightHelper).align(TextAlignment.CENTER).color(-1929379841).draw();
      }
   }

   private void renderAltManager(int coordinate, int coordinate2, float coordinate3) {
      float value = this.panelSpring();
      float clampedValue = clamp(coordinate3 * 1.15F, 0.0F, 1.0F);
      if (!(clampedValue <= 0.01F)) {
         float value2 = this.uiScale();
         int color = SkycoreClient.themes().accent() | 0xFF000000;
         var strings = AccountManager.INSTANCE.alts();
         String text = AccountManager.INSTANCE.selected();
         float computedValue = 31.0F * value2;
         float color2 = 6.5F * value2;
         float computedValue2 = 6.0F * value2;
         float width = 14.0F * value2;
         float width2 = 11.0F * value2;
         float alpha = 14.0F * value2;
         float computedValue3 = 9.0F * value2;
         float computedValue4 = 10.0F * value2;
         String text2 = this.nickBuffer.isEmpty() ? "Enter nickname..." : this.nickBuffer;
         float width3 = width + InterBoldHelper.sfRoundedMedium().getWidth(text2, width2) + width;
         width3 = Math.max(width3, 140.0F * value2);
         float computedValue5 = Math.min(strings.size(), 7) * computedValue + Math.max(0, Math.min(strings.size(), 7) - 1) * computedValue2;
         float height = computedValue + (strings.isEmpty() ? 0.0F : 12.0F * value2 + computedValue5);
         float computedValue6 = 88.0F * value2;
         float computedValue7 = (this.height - height) * 0.5F + 10.0F * value2;
         float computedValue8 = (1.0F - Math.min(value, 1.0F)) * 26.0F * value2;
         if (value > 1.0F) {
            computedValue8 -= (value - 1.0F) * 14.0F * value2;
         }

         int value3 = coordinate3 > 0.55F ? 1 : 0;
         float computedValue9 = 1.0F - (float)Math.pow(4.0E-4F, this.frameDelta);
         float value4 = computedValue7;
         float computedValue10 = value4 + computedValue8;
         int color3 = withAlpha(184549375, clampedValue);
         int value5 = withAlpha(218103807, clampedValue);
         BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(computedValue6, computedValue10, width3, computedValue).color(color3).radius(color2).blur(12.0F * value2, clampedValue);
         if ((value5 >>> 24 & 0xFF) > 4) {
            rectBuilder.border(Math.max(0.55F, 0.8F * value2), value5);
         }

         rectBuilder.draw();
         float computedValue11 = computedValue10 + (computedValue - width2) * 0.5F - 0.5F * value2;
         int color4 = this.nickBuffer.isEmpty() && !this.nickFocused ? withAlpha(-1275068417, clampedValue) : withAlpha(-1, clampedValue);
         BeginFrameHelper.text(computedValue6 + width, computedValue11, width2, this.nickBuffer.isEmpty() && this.nickFocused ? "" : text2)
            .font(InterBoldHelper.sfRoundedMedium())
            .color(color4)
            .draw();
         if (value3 != 0) {
            this.regions.add(new MouseScrolledHelper.MenuRegion(MouseScrolledHelper.Action.ALT_INPUT, null, computedValue6, value4, width3, computedValue));
         }

         float computedValue12 = value4 + computedValue + 12.0F * value2;
         float computedValue13 = 7.0F * computedValue + 6.0F * computedValue2;
         this.altMaxScroll = Math.max(0.0F, strings.size() * (computedValue + computedValue2) - computedValue2 - computedValue13);
         this.altScroll = clamp(this.altScroll, 0.0F, this.altMaxScroll);
         float value6 = width3;

         for (String text3 : strings) {
            float width4 = InterBoldHelper.sfRoundedMedium().getWidth(text3, width2);
            value6 = Math.max(value6, width + width4 + computedValue4 + computedValue3 + computedValue4 + alpha + width);
         }

         BeginFrameHelper.pushScissor(computedValue6 - 4.0F * value2, computedValue12 + computedValue8 - 2.0F * value2, value6 + 8.0F * value2, computedValue13 + 4.0F * value2);

         try {
            for (int value7 = 0; value7 < strings.size(); value7++) {
               String text4 = (String)strings.get(value7);
               float width5 = InterBoldHelper.sfRoundedMedium().getWidth(text4, width2);
               float computedValue14 = width + width5 + computedValue4 + computedValue3 + computedValue4 + alpha + width;
               float computedValue15 = computedValue12 + value7 * (computedValue + computedValue2) - this.altScroll;
               float computedValue16 = computedValue15 + computedValue8;
               if (!(computedValue16 + computedValue < computedValue12 + computedValue8 - computedValue) && !(computedValue16 > computedValue12 + computedValue8 + computedValue13 + computedValue)) {
                  int value8 = text != null && text.equalsIgnoreCase(text4) ? 1 : 0;
                  int value9 = value3 != 0 && contains(coordinate, coordinate2, computedValue6, computedValue15, computedValue14, computedValue) ? 1 : 0;
                  String text5 = text4.toLowerCase(Locale.ROOT);
                  float computedValue17 = this.altHover.computeIfAbsent(text5, text6 -> {
                     float result = value8 != 0 ? 1.0F : 0.0F;
                     return result;
                  });
                  float value10 = value8 == 0 && value9 == 0 ? 0.0F : 1.0F;
                  computedValue17 += (value10 - computedValue17) * computedValue9;
                  this.altHover.put(text5, computedValue17);
                  float clampedValue2 = clamp(computedValue17, 0.0F, 1.0F);
                  int color5 = withAlpha(mixColor(184549375, color, clampedValue2), clampedValue);
                  int value11 = withAlpha(mixColor(218103807, 16777215, clampedValue2), clampedValue);
                  int color6 = withAlpha(mixColor(-1275068417, -1, clampedValue2), clampedValue);
                  BeginFrameHelper.RectBuilder rectBuilder2 = BeginFrameHelper.rect(computedValue6, computedValue16, computedValue14, computedValue).color(color5).radius(color2).blur(12.0F * value2, clampedValue);
                  if ((value11 >>> 24 & 0xFF) > 4) {
                     rectBuilder2.border(Math.max(0.55F, 0.8F * value2), value11);
                  }

                  rectBuilder2.draw();
                  float computedValue18 = computedValue6 + width;
                  float computedValue19 = computedValue16 + (computedValue - width2) * 0.5F - 0.5F * value2;
                  BeginFrameHelper.text(computedValue18, computedValue19, width2, text4).font(InterBoldHelper.sfRoundedMedium()).color(color6).draw();
                  float alpha2 = computedValue6 + computedValue14 - width - alpha + 1.5F * value2 + 2.0F;
                  float alpha3 = computedValue16 + (computedValue - alpha) * 0.5F;
                  float computedValue20 = alpha2 - computedValue4 - computedValue3;
                  float computedValue21 = computedValue16 + (computedValue - computedValue3) * 0.5F - 2.8F * value2 - 2.5F * value2;
                  BeginFrameHelper.text(computedValue20, computedValue21, computedValue3, "m").font(InterBoldHelper.clickGuiIcons()).color(color6).draw();
                  RotatedTextureRenderer.draw(alpha2, alpha3, alpha, SkinService.skin(text4), alpha * 0.22F, withAlpha(-1, clampedValue));
                  if (value3 != 0) {
                     float computedValue22 = computedValue3 + 8.0F * value2;
                     this.regions.add(new MouseScrolledHelper.MenuRegion(MouseScrolledHelper.Action.ALT_ACCOUNT, text4, computedValue6, computedValue15, computedValue14, computedValue));
                     this.regions
                        .add(new MouseScrolledHelper.MenuRegion(MouseScrolledHelper.Action.ALT_DELETE, text4, computedValue20 - 4.0F * value2, computedValue15, computedValue22, computedValue));
                  }
               }
            }
         } finally {
            BeginFrameHelper.popScissor();
         }
      }
   }

   private static int withAlpha(int value, float value2) {
      float intermediate = value >>> 24 & 0xFF;
      intermediate *= clamp(value2, 0.0F, 1.0F);
      int roundedValue = Math.round(intermediate);
      return value & 16777215 | roundedValue << 24;
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int coordinate, int coordinate2, float coordinate3) {
      this.updateClock();
      AccountManager.INSTANCE.ensureSession();
      CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, this.minecraft.getDeltaTracker());
      BeginFrameHelper.beginFrame();

      try {
         this.regions.clear();
         if (this.openedAt == 0L) {
            long nowNanos = System.nanoTime();
            this.openedAt = nowNanos;
            this.quoteAt = nowNanos;
         }

         float value = this.altBlend();
         this.renderBackgrounds();
         this.renderMenu(coordinate, coordinate2, value);
         this.renderAltManager(coordinate, coordinate2, value);
         this.renderHeroQuote();
         this.renderLegalNotice();
         if (!SelectAndSaveHelper.hasSavedSelection()) {
            this.renderLanguageSelection(coordinate, coordinate2);
         }

         BeginFrameHelper.flush();
      } finally {
         CurrentGuiGraphicsExtractorHelper.exit2D();
      }
   }

   private static int typedChars(long timestamp, long timestamp2) {
      return timestamp < timestamp2 ? 0 : (int)((timestamp - timestamp2) / 26L) + 1;
   }

   private void openAltManager() {
      this.altView = true;
      this.altAnimToAlt = true;
      this.altAnimAt = System.nanoTime();
      this.quoteAlt = true;
      long nowNanos = System.nanoTime();
      this.quoteAt = nowNanos;
      this.nickFocused = false;
      this.nickBuffer = "";
      this.altScroll = 0.0F;
      AccountManager.INSTANCE.ensureSession();
   }

   private float enterProgress(long timestamp, int value) {
      long timestamp2 = 70L + value * 70L;
      float computedValue = (float)(timestamp - timestamp2) / 620.0F;
      return clamp(computedValue, 0.0F, 1.0F);
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   private void closeAltManager() {
      this.altView = false;
      this.altAnimToAlt = false;
      long nowNanos = System.nanoTime();
      this.altAnimAt = nowNanos;
      this.quoteAlt = false;
      long nowNanos2 = System.nanoTime();
      this.quoteAt = nowNanos2;
      this.nickFocused = false;
      this.nickBuffer = "";
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   public MouseScrolledHelper() {
      super(Component.literal("Main Menu"));
      this.languageHover = new float[2];
      this.altHover = new HashMap<>();
      this.lastFrame = System.nanoTime();
      this.frameDelta = 0.016666668F;
      this.nickBuffer = "";
   }

   private void renderHeroQuote() {
      float value = this.altBlend();
      float clampedValue = clamp(1.0F - value * 1.15F, 0.0F, 1.0F);
      if (!(clampedValue <= 0.01F)) {
         float value2 = this.uiScale();
         int alpha = SkycoreClient.themes().accent() | 0xFF000000;
         float computedValue = this.width * 0.5F;
         float computedValue2 = 31.0F * value2;
         float computedValue3 = 6.0F * value2;
         float computedValue4 = 8.0F * value2;
         float computedValue5 = 17.5F * value2;
         float height = 4.0F * computedValue2 + 3.0F * computedValue3 + computedValue4 + computedValue5;
         float computedValue6 = (this.height - height) * 0.5F + 28.0F * value2;
         long timestamp = elapsedMs(this.openedAt);
         float computedValue7 = smooth(clamp((float)timestamp / 700.0F, 0.0F, 1.0F));
         float alpha2 = clampedValue * computedValue7;
         String text = "a";
         float width = 31.0F * value2;
         float width2 = InterBoldHelper.skycore().getWidth(text, width);
         float computedValue8 = computedValue6 - 105.0F * value2;
         BeginFrameHelper.text(computedValue - width2 * 0.5F, computedValue8, width, text).font(InterBoldHelper.skycore()).color(withAlpha(alpha, alpha2)).draw();
         long timestamp2 = 4000L * GREETING_FRAMES.length;
         long timestamp3 = timestamp2 <= 0L ? 0L : timestamp % timestamp2;
         int minimum = (int)Math.min(GREETING_FRAMES.length - 1, timestamp3 / 4000L);
         long timestamp4 = timestamp3 % 4000L;
         float value3;
         if (timestamp4 < 1650L) {
            value3 = smooth(clamp((float)timestamp4 / 1650.0F, 0.0F, 1.0F));
         } else if (timestamp4 < 2350L) {
            value3 = 1.0F;
         } else {
            value3 = 1.0F - smooth(clamp((float)(timestamp4 - 1650L - 700L) / 1650.0F, 0.0F, 1.0F));
         }

         float computedValue9 = value3 * 48.0F;
         float computedValue10 = 116.0F * value2;
         float computedValue11 = 42.5F * value2;
         float computedValue12 = computedValue - computedValue10 * 0.5F;
         float computedValue13 = computedValue6 - 61.0F * value2;
         Identifier[] identifierValues = GREETING_FRAMES[minimum];
         if (value3 > 0.001F) {
            int computedValue14 = Math.max(0, Math.min(47, (int)Math.floor(computedValue9) - 1));
            float value4 = computedValue9 < 1.0F ? clamp(computedValue9, 0.0F, 1.0F) : 1.0F;
            BeginFrameHelper.texture(computedValue12, computedValue13, computedValue10, computedValue11, identifierValues[computedValue14]).color(withAlpha(-1, alpha2 * value4)).draw();
         }
      }
   }

   private float altBlend() {
      return clamp(this.panelAmount(), 0.0F, 1.0F);
   }

   @Override
   public boolean charTyped(CharacterEvent characterEvent) {
      if (!SelectAndSaveHelper.hasSavedSelection()) {
         return true;
      }

      if ((this.altView || this.altBlend() > 0.55F) && this.nickFocused) {
         char character = (char)characterEvent.codepoint();
         if (this.nickBuffer.length() >= 16) {
            return true;
         }

         if (!Character.isLetterOrDigit(character) && character != '_') {
            return true;
         }

         this.nickBuffer = this.nickBuffer + character;
         return true;
      } else {
         return super.charTyped(characterEvent);
      }
   }

   private void renderLanguageSelection(int coordinate, int coordinate2) {
      this.regions.clear();
      BeginFrameHelper.rect(0.0F, 0.0F, this.width, this.height).color(-1476395008).blur(34.0F, 1.0F).hudGlow(false).draw();
      float computedValue = Math.max(0.55F, this.uiScale());
      float computedValue2 = 190.0F * computedValue;
      float height = 120.0F * computedValue;
      float computedValue3 = 42.0F * computedValue;
      float width = computedValue2 * 2.0F + computedValue3;
      float computedValue4 = (this.width - width) * 0.5F;
      float computedValue5 = (this.height - height) * 0.5F;
      this.renderLanguageFlag(0, MouseScrolledHelper.Action.LANGUAGE_RUSSIAN, RUSSIAN_FLAG, computedValue4, computedValue5, computedValue2, height, 150.0F * computedValue, 100.0F * computedValue, coordinate, coordinate2);
      this.renderLanguageFlag(
         1, MouseScrolledHelper.Action.LANGUAGE_ENGLISH, ENGLISH_FLAG, computedValue4 + computedValue2 + computedValue3, computedValue5, computedValue2, height, 180.0F * computedValue, 90.0F * computedValue, coordinate, coordinate2
      );
      this.renderLanguagePrompt(computedValue5 + height + 18.0F * computedValue, computedValue);
   }

   private static float smooth(float value) {
      float clampedValue = clamp(value, 0.0F, 1.0F);
      return clampedValue * clampedValue * (3.0F - 2.0F * clampedValue);
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean condition) {
      double x = mouseButtonEvent.x();
      double y = mouseButtonEvent.y();
      int index = mouseButtonEvent.button();
      if (SelectAndSaveHelper.hasSavedSelection()) {
         for (int index2 = this.regions.size() - 1; index2 >= 0; index2--) {
            MouseScrolledHelper.MenuRegion menuRegion = this.regions.get(index2);
            if (contains(x, y, menuRegion.x, menuRegion.y, menuRegion.w, menuRegion.h)) {
               if (index == 1) {
                  if (menuRegion.action != MouseScrolledHelper.Action.ALT_ACCOUNT && menuRegion.action != MouseScrolledHelper.Action.ALT_DELETE) {
                     return false;
                  }

                  this.removeAlt(menuRegion.payload);
                  return true;
               }

               if (index != 0) {
                  return false;
               }

               switch (menuRegion.action) {
                  case SINGLEPLAYER:
                     this.minecraft.setScreenAndShow(new SelectWorldScreen(this));
                     break;
                  case MULTIPLAYER:
                     this.minecraft.setScreenAndShow(new JoinMultiplayerScreen(this));
                     break;
                  case ALT_MANAGER:
                     this.openAltManager();
                     break;
                  case SETTINGS:
                     this.minecraft.setScreenAndShow(new OptionsScreen(this, this.minecraft.options, false));
                     break;
                  case EXIT:
                     this.minecraft.stop();
                     break;
                  case ALT_INPUT:
                     this.nickFocused = true;
                     return true;
                  case ALT_ACCOUNT:
                     this.nickFocused = false;
                     AccountManager.INSTANCE.select(menuRegion.payload);
                     return true;
                  case ALT_DELETE:
                     this.removeAlt(menuRegion.payload);
                     return true;
               }

               return true;
            }
         }

         if (index == 0 && this.altBlend() > 0.55F) {
            this.nickFocused = false;
         }

         return false;
      } else {
         if (index == 0) {
            for (int index3 = this.regions.size() - 1; index3 >= 0; index3--) {
               MouseScrolledHelper.MenuRegion menuRegion2 = this.regions.get(index3);
               if (contains(x, y, menuRegion2.x, menuRegion2.y, menuRegion2.w, menuRegion2.h)) {
                  if (menuRegion2.action == MouseScrolledHelper.Action.LANGUAGE_RUSSIAN) {
                     this.selectLanguage(true);
                  } else if (menuRegion2.action == MouseScrolledHelper.Action.LANGUAGE_ENGLISH) {
                     this.selectLanguage(false);
                  }

                  return true;
               }
            }
         }

         return true;
      }
   }

   private void renderMenu(int coordinate, int coordinate2, float coordinate3) {
      float value = this.uiScale();
      int color = SkycoreClient.themes().accent() | 0xFF000000;
      float value2 = this.panelSpring();
      float clampedValue = clamp(1.0F - coordinate3 * 1.15F, 0.0F, 1.0F);
      if (!(clampedValue <= 0.01F)) {
         MouseScrolledHelper.MenuEntry[] menuEntryValues = new MouseScrolledHelper.MenuEntry[]{
            new MouseScrolledHelper.MenuEntry("Singleplayer", "e", MouseScrolledHelper.Action.SINGLEPLAYER),
            new MouseScrolledHelper.MenuEntry("Multiplayer", "g", MouseScrolledHelper.Action.MULTIPLAYER),
            new MouseScrolledHelper.MenuEntry("AltManager", "h", MouseScrolledHelper.Action.ALT_MANAGER),
            new MouseScrolledHelper.MenuEntry("Settings", "i", MouseScrolledHelper.Action.SETTINGS)
         };
         TextHeightHelper textHeightHelper = InterBoldHelper.sfRoundedMedium();
         float computedValue = 31.0F * value;
         float color2 = 6.5F * value;
         float computedValue2 = 6.0F * value;
         float computedValue3 = 14.0F * value;
         float computedValue4 = 10.5F * value;
         float width = 11.0F * value;
         float width2 = 9.5F * value;
         float computedValue5 = 9.0F * value;
         float computedValue6 = 6.0F * value;
         float width3 = textHeightHelper.getWidth("Exit", width2);
         float width4 = computedValue5 + 6.0F * value + width3;
         float computedValue7 = Math.max(width2, computedValue5) + 8.0F * value;
         float height = menuEntryValues.length * computedValue + (menuEntryValues.length - 1) * computedValue2 + computedValue6 + computedValue7;
         float computedValue8 = (this.height - height) * 0.5F + 18.0F * value;
         long timestamp = elapsedMs(this.openedAt);
         float computedValue9 = Math.min(value2, 1.0F) * 28.0F * value;
         if (value2 > 1.0F) {
            computedValue9 += (value2 - 1.0F) * 16.0F * value;
         }

         for (int value3 = 0; value3 < menuEntryValues.length; value3++) {
            MouseScrolledHelper.MenuEntry menuEntry = menuEntryValues[value3];
            float width5 = textHeightHelper.getWidth(menuEntry.label, width);
            float width6 = computedValue3 + width5 + 22.0F * value + computedValue4 + computedValue3;
            float computedValue10 = (this.width - width6) * 0.5F;
            float value4 = spring(this.enterProgress(timestamp, value3));
            float computedValue11 = (1.0F - Math.min(value4, 1.0F)) * 22.0F * value;
            if (value4 > 1.0F) {
               computedValue11 -= (value4 - 1.0F) * 14.0F * value;
            }

            float color3 = clamp(value4 * 1.15F, 0.0F, 1.0F) * clampedValue;
            float computedValue12 = computedValue8 + computedValue11 + computedValue9;
            int value5 = clampedValue > 0.7F && coordinate3 < 0.2F ? 1 : 0;
            int value6 = value5 != 0 && color3 > 0.55F && contains(coordinate, coordinate2, computedValue10, computedValue12, width6, computedValue) ? 1 : 0;
            int value7 = menuEntry.action == MouseScrolledHelper.Action.MULTIPLAYER ? 1 : 0;
            float value8 = value7 != 0 ? (value6 != 0 ? 1.0F : 0.55F) : (value6 != 0 ? 1.0F : 0.0F);
            float computedValue13 = 1.0F - (float)Math.pow(4.0E-4F, this.frameDelta);
            this.hover[value3] = this.hover[value3] + (value8 - this.hover[value3]) * computedValue13;
            float clampedValue2 = clamp(this.hover[value3], 0.0F, 1.0F);
            if (color3 > 0.01F) {
               int color4 = withAlpha(mixColor(184549375, color, clampedValue2), color3);
               int value9 = withAlpha(mixColor(218103807, 16777215, clampedValue2), color3);
               int color5 = withAlpha(mixColor(-1275068417, -1, clampedValue2), color3 * 0.95F);
               BeginFrameHelper.RectBuilder rectBuilder = BeginFrameHelper.rect(computedValue10, computedValue12, width6, computedValue).color(color4).radius(color2).blur(12.0F * value, color3);
               if ((value9 >>> 24 & 0xFF) > 4) {
                  rectBuilder.border(Math.max(0.55F, 0.8F * value), value9);
               }

               rectBuilder.draw();
               float computedValue14 = computedValue12 + (computedValue - width) * 0.5F - 0.5F * value;
               BeginFrameHelper.text(computedValue10 + computedValue3, computedValue14, width, menuEntry.label).font(textHeightHelper).color(color5).draw();
               float computedValue15 = computedValue10 + width6 - computedValue3 - computedValue4 + 2.0F;
               float computedValue16 = computedValue12 + (computedValue - computedValue4) * 0.5F - 3.0F * value - 2.5F;
               BeginFrameHelper.text(computedValue15, computedValue16, computedValue4, menuEntry.icon).font(InterBoldHelper.clickGuiIcons()).color(color5).draw();
            }

            if (value5 != 0) {
               this.regions.add(new MouseScrolledHelper.MenuRegion(menuEntry.action, null, computedValue10, computedValue12, width6, computedValue));
            }

            computedValue8 += computedValue + computedValue2;
         }

         float value10 = spring(this.enterProgress(timestamp, menuEntryValues.length));
         float computedValue17 = clamp(value10 * 1.15F, 0.0F, 1.0F) * clampedValue;
         float computedValue18 = (1.0F - Math.min(value10, 1.0F)) * 16.0F * value;
         if (value10 > 1.0F) {
            computedValue18 -= (value10 - 1.0F) * 10.0F * value;
         }

         float computedValue19 = (this.width - width4) * 0.5F;
         float computedValue20 = computedValue8 + computedValue6 + 2.0F;
         float computedValue21 = computedValue20 + computedValue18 + computedValue9;
         int value11 = clampedValue > 0.7F && computedValue17 > 0.55F && contains(coordinate, coordinate2, computedValue19, computedValue21, width4, computedValue7) ? 1 : 0;
         float value12 = value11 != 0 ? 1.0F : 0.0F;
         float computedValue22 = 1.0F - (float)Math.pow(4.0E-4F, this.frameDelta);
         this.hover[4] = this.hover[4] + (value12 - this.hover[4]) * computedValue22;
         float clampedValue3 = clamp(this.hover[4], 0.0F, 1.0F);
         if (computedValue17 > 0.01F) {
            int color6 = withAlpha(mixColor(-1711276033, -50373, clampedValue3), computedValue17 * 0.95F);
            float computedValue23 = computedValue21 + (computedValue7 - computedValue5) * 0.5F - 2.6F * value - 2.0F;
            float computedValue24 = computedValue21 + (computedValue7 - width2) * 0.5F - 0.4F * value;
            BeginFrameHelper.text(computedValue19, computedValue23, computedValue5, "m").font(InterBoldHelper.clickGuiIcons()).color(color6).draw();
            BeginFrameHelper.text(computedValue19 + computedValue5 + 6.0F * value - 1.0F, computedValue24, width2, "Exit").font(textHeightHelper).color(color6).draw();
         }

         if (clampedValue > 0.7F) {
            this.regions.add(new MouseScrolledHelper.MenuRegion(MouseScrolledHelper.Action.EXIT, null, computedValue19, computedValue21, width4, computedValue7));
         }
      }
   }

   private static int clampIndex(int value) {
      int value2 = 1;
      value2 = Math.min(BACKGROUNDS.length - 1, value);
      return Math.max(0, value2);
   }

   private float panelAmount() {
      if (this.altAnimAt == 0L) {
         return this.altView ? 1.0F : 0.0F;
      }

      float computedValue = Math.min(this.transitionSpring(), 1.0F);
      return this.altAnimToAlt ? computedValue : 1.0F - computedValue;
   }

   private static int mixColor(int value, int value2, float value3) {
      value3 = clamp(value3, 0.0F, 1.0F);
      float intermediate = (value >>> 24 & 0xFF) + ((value2 >>> 24 & 0xFF) - (value >>> 24 & 0xFF)) * value3;
      int roundedValue = Math.round(intermediate);
      int computedValue = Math.round((value >>> 16 & 0xFF) + ((value2 >>> 16 & 0xFF) - (value >>> 16 & 0xFF)) * value3);
      intermediate = (value >>> 8 & 0xFF) + ((value2 >>> 8 & 0xFF) - (value >>> 8 & 0xFF)) * value3;
      int roundedValue2 = Math.round(intermediate);
      intermediate = (value & 0xFF) + ((value2 & 0xFF) - (value & 0xFF)) * value3;
      int roundedValue3 = Math.round(intermediate);
      return roundedValue << 24 | computedValue << 16 | roundedValue2 << 8 | roundedValue3;
   }

   private float uiScale() {
      float computedValue = this.width / 1920.0F;
      float computedValue2 = this.height / 1080.0F;
      return clamp(Math.min(computedValue, computedValue2), 0.45F, 1.0F);
   }

   private void updateClock() {
      long nowNanos = System.nanoTime();
      float delta = (float)(nowNanos - this.lastFrame) / 1.0E9F;
      delta = clamp(delta, 0.004166667F, 0.05F);
      this.frameDelta = delta;
      this.lastFrame = nowNanos;
   }

   private void commitNick() {
      String text = this.nickBuffer.trim();
      if (AccountManager.isValidName(text)) {
         if (AccountManager.INSTANCE.add(text)) {
            AccountManager.INSTANCE.select(text);
            this.nickBuffer = "";
         }
      }
   }

   private float panelSpring() {
      if (this.altAnimAt == 0L) {
         return this.altView ? 1.0F : 0.0F;
      }

      float value = this.transitionSpring();
      return this.altAnimToAlt ? value : 1.0F - Math.min(value, 1.0F);
   }

   private static long elapsedMs(long timestamp) {
      return (System.nanoTime() - timestamp) / 1000000L;
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (!SelectAndSaveHelper.hasSavedSelection()) {
         return true;
      }

      int value = keyEvent.key();
      if (this.altView || this.altBlend() > 0.05F) {
         if (value == 256) {
            if (this.nickFocused && !this.nickBuffer.isEmpty()) {
               this.nickFocused = false;
               return true;
            }

            this.closeAltManager();
            return true;
         }

         if (this.nickFocused || this.altBlend() > 0.55F) {
            if (value == 257 || value == 335) {
               this.commitNick();
               return true;
            }

            if (value == 259 && !this.nickBuffer.isEmpty()) {
               this.nickBuffer = this.nickBuffer.substring(0, this.nickBuffer.length() - 1);
               return true;
            }
         }
      }

      return super.keyPressed(keyEvent);
   }

   @Override
   protected void init() {
      long nowNanos = System.nanoTime();
      this.lastFrame = nowNanos;
      this.openedAt = nowNanos;
      this.quoteAt = nowNanos;
      if (this.altView) {
         this.altAnimToAlt = true;
         this.altAnimAt = nowNanos;
         this.quoteAlt = true;
      } else {
         this.altAnimAt = 0L;
         this.altAnimToAlt = false;
         this.quoteAlt = false;
      }

      for (int value = 0; value < this.hover.length; value++) {
         this.hover[value] = value == 1 ? 0.55F : 0.0F;
      }

      String text = AccountManager.INSTANCE.selected();
      if (text != null) {
         this.altHover.put(text.toLowerCase(Locale.ROOT), 1.0F);
      }

      AccountManager.INSTANCE.ensureSession();
   }

   private void renderLanguagePrompt(float coordinate, float coordinate2) {
      long timestamp = 4000L * LANGUAGE_PROMPTS.length;
      long timestamp2 = timestamp <= 0L ? 0L : elapsedMs(this.openedAt) % timestamp;
      int minimum = (int)Math.min(LANGUAGE_PROMPTS.length - 1, timestamp2 / 4000L);
      long timestamp3 = timestamp2 % 4000L;
      float value;
      if (timestamp3 < 1650L) {
         value = smooth(clamp((float)timestamp3 / 1650.0F, 0.0F, 1.0F));
      } else if (timestamp3 < 2350L) {
         value = 1.0F;
      } else {
         value = 1.0F - smooth(clamp((float)(timestamp3 - 1650L - 700L) / 1650.0F, 0.0F, 1.0F));
      }

      if (!(value <= 0.001F)) {
         float width = 318.0F * coordinate2;
         float computedValue = 106.0F * coordinate2;
         float computedValue2 = (this.width - width) * 0.5F;
         BeginFrameHelper.pushScissor(computedValue2, coordinate, width * value, computedValue);

         try {
            BeginFrameHelper.texture(computedValue2, coordinate, width, computedValue, LANGUAGE_PROMPTS[minimum]).color(withAlpha(-1, Math.min(1.0F, value * 4.0F))).draw();
         } finally {
            BeginFrameHelper.popScissor();
         }
      }
   }

   private static Identifier[] greetingFrames(String text) {
      Identifier[] identifierValues = new Identifier[48];

      for (int value = 0; value < identifierValues.length; value++) {
         identifierValues[value] = Identifier.parse(String.format(Locale.ROOT, "skycore:textures/gui/greeting_frames/%s/frame_%02d.png", text, value));
      }

      return identifierValues;
   }

   private void selectLanguage(boolean condition) {
      if (SelectAndSaveHelper.selectAndSave(condition)) {
         this.regions.clear();
         long nowNanos = System.nanoTime();
         this.openedAt = nowNanos;
         this.quoteAt = nowNanos;
      }
   }

   private void renderLanguageFlag(
      int coordinate, MouseScrolledHelper.Action action, Identifier texture, float coordinate2, float coordinate3, float coordinate4, float coordinate5, float coordinate6, float coordinate7, int coordinate8, int coordinate9
   ) {
      double value = coordinate8;
      double value2 = coordinate9;
      boolean contains = contains(value, value2, coordinate2, coordinate3, coordinate4, coordinate5);
      float computedValue = 1.0F - (float)Math.pow(4.0E-4F, this.frameDelta);
      float value3 = this.languageHover[coordinate];
      float intermediate = contains ? 1.0F : 0.0F;
      this.languageHover[coordinate] = value3 + (intermediate - this.languageHover[coordinate]) * computedValue;
      float computedValue2 = 1.0F + 0.065F * this.languageHover[coordinate];
      float computedValue3 = coordinate6 * computedValue2;
      float computedValue4 = coordinate7 * computedValue2;
      float computedValue5 = coordinate2 + (coordinate4 - computedValue3) * 0.5F;
      float computedValue6 = coordinate3 + (coordinate5 - computedValue4) * 0.5F;
      BeginFrameHelper.TextureBuilder textureBuilder = BeginFrameHelper.texture(computedValue5, computedValue6, computedValue3, computedValue4, texture).radius(5.0F * this.uiScale());
      textureBuilder.draw();
      var regions2 = this.regions;
      MouseScrolledHelper.MenuRegion menuRegion = new MouseScrolledHelper.MenuRegion(action, null, coordinate2, coordinate3, coordinate4, coordinate5);
      regions2.add(menuRegion);
   }

   private static boolean contains(double value, double value2, float value3, float value4, float value5, float value6) {
      return value >= value3 && value2 >= value4 && value <= value3 + value5 && value2 <= value4 + value6;
   }

   private float transitionSpring() {
      if (this.altAnimAt == 0L) {
         return this.altView ? 1.0F : 0.0F;
      }

      float computedValue = clamp((float)elapsedMs(this.altAnimAt) / 620.0F, 0.0F, 1.0F);
      return spring(computedValue);
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void drawQuoteLineRight(float coordinate, float coordinate2, float width, MouseScrolledHelper.QuoteLine quoteLine, int coordinate3, int coordinate4) {
      if (coordinate4 > 0) {
         float index = measureVisible(quoteLine, width, coordinate4);
         float width2 = coordinate - index;
         int index2 = coordinate4;

         for (MouseScrolledHelper.QuotePart quotePart : quoteLine.parts) {
            if (index2 <= 0) {
               break;
            }

            String text = quotePart.text.substring(0, Math.min(index2, quotePart.text.length()));
            if (!text.isEmpty()) {
               int color = quotePart.accent ? coordinate3 : -419430401;
               BeginFrameHelper.text(width2, coordinate2, width, text).font(InterBoldHelper.sfRoundedMedium()).color(color).draw();
               width2 += InterBoldHelper.sfRoundedMedium().getWidth(text, width);
            }

            index2 -= quotePart.text.length();
         }
      }
   }

   enum Action {
      SINGLEPLAYER,
      MULTIPLAYER,
      ALT_MANAGER,
      SETTINGS,
      EXIT,
      ALT_INPUT,
      ALT_ACCOUNT,
      ALT_DELETE,
      LANGUAGE_RUSSIAN,
      LANGUAGE_ENGLISH;
   }

   public record MenuEntry(String label, String icon, MouseScrolledHelper.Action action) {
      public String label() {
         return this.label;
      }

      public String icon() {
         return this.icon;
      }

      public MouseScrolledHelper.Action action() {
         return this.action;
      }
   }

   public record MenuRegion(MouseScrolledHelper.Action action, String payload, float x, float y, float w, float h) {
      public float w() {
         return this.w;
      }

      public String payload() {
         return this.payload;
      }

      public float h() {
         return this.h;
      }

      public float x() {
         return this.x;
      }

      public MouseScrolledHelper.Action action() {
         return this.action;
      }

      public float y() {
         return this.y;
      }
   }

   public record QuoteLine(MouseScrolledHelper.QuotePart... parts) {
      public MouseScrolledHelper.QuotePart[] parts() {
         return this.parts;
      }

      private int length() {
         int value = 0;

         for (MouseScrolledHelper.QuotePart quotePart : this.parts) {
            value += quotePart.text.length();
         }

         return value;
      }
   }

   public record QuotePart(String text, boolean accent) {
      public String text() {
         return this.text;
      }

      public boolean accent() {
         return this.accent;
      }
   }
}
