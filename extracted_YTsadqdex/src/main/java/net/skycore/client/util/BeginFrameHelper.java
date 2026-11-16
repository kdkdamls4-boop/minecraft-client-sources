package net.skycore.client.util;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.skycore.client.mixin.GuiGraphicsExtractorAccessor;
import org.joml.Matrix3x2fStack;

public final class BeginFrameHelper {
   private static int hudIconGlowColor;
   private static final float ICON_GLOW_TEXTURE_PAD = 0.4F;
   private static final Deque<ScreenRectangle> SCISSORS = new ArrayDeque<>();
   private static int hudGlowColor;
   private static float backdropBlurScale = 1.0F;
   private static final float ICON_GLOW_GLYPH_PAD = 0.25F;
   private static final float ICON_GLOW_STRENGTH = 0.48F;
   private static final float ICON_GLOW_BLUR = 8.0F;
   private static final BeginFrameHelper.FrameState FRAME_STATE = new BeginFrameHelper.FrameState();

   public static void beginFrame() {
      BeginFrameHelper.FrameState fRAME_STATE = FRAME_STATE;
      fRAME_STATE.queued.clear();
      fRAME_STATE.pendingCount = 0;
      SCISSORS.clear();
   }

   public static BeginFrameHelper.ColorGridBuilder colorGrid(float color, float color2, float color3, int color4, int[] intValues) {
      return new BeginFrameHelper.ColorGridBuilder(color, color2, color3, color4, intValues);
   }

   public static void coloredText(float value, float value2, NeedsFallbackHelper needsFallbackHelper) {
      if (!needsFallbackHelper.glyphs().isEmpty() && !(needsFallbackHelper.size() <= 0.0F) && !hasEmptyScissor()) {
         if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
            throw new IllegalStateException("Render2D.coloredText requires a 2D render context");
         }

         if (!needsFallbackHelper.needsFallback()) {
            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            FRAME_STATE.queued.add(new VertexHelper(guiGraphicsExtractor.pose(), value, value2, needsFallbackHelper, currentScissor()));
            FRAME_STATE.pendingCount = FRAME_STATE.queued.size();
         } else {
            for (NeedsFallbackHelper.Glyph glyph : needsFallbackHelper.glyphs()) {
               text(value + glyph.x(), value2, needsFallbackHelper.size(), glyph.text()).font(needsFallbackHelper.font()).color(glyph.color()).draw();
            }
         }
      }
   }

   public static void setHudGlowColor(int hudGlowColor2) {
      hudGlowColor = hudGlowColor2;
   }

   public static BeginFrameHelper.TextBuilder text(float value, float value2, float value3, String text) {
      return new BeginFrameHelper.TextBuilder(value, value2, value3, text);
   }

   public static int flush() {
      BeginFrameHelper.FrameState fRAME_STATE = FRAME_STATE;
      GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
      if (guiGraphicsExtractor == null) {
         fRAME_STATE.queued.clear();
         fRAME_STATE.lastFlushedCount = 0;
         fRAME_STATE.pendingCount = 0;
         return 0;
      }

      GuiGraphicsExtractorAccessor guiGraphicsExtractorAccessor = (GuiGraphicsExtractorAccessor)guiGraphicsExtractor;
      int value = 0;
      int value2 = 0;

      while (value2 < fRAME_STATE.queued.size()) {
         GuiElementRenderState guiElementRenderState = fRAME_STATE.queued.get(value2);
         int computedValue = value2 + 1;
         if (guiElementRenderState instanceof FontRenderState fontRenderState) {
            while (computedValue < fRAME_STATE.queued.size() && computedValue - value2 < 64 && BoundsHelper.compatible(fontRenderState, fRAME_STATE.queued.get(computedValue))) {
               computedValue++;
            }

            if (computedValue - value2 > 1) {
               guiElementRenderState = new BoundsHelper(fRAME_STATE.queued.subList(value2, computedValue));
            }
         }

         guiGraphicsExtractorAccessor.getGuiRenderState().addGuiElement(guiElementRenderState);
         value++;
         value2 = computedValue;
      }

      fRAME_STATE.queued.clear();
      fRAME_STATE.pendingCount = 0;
      fRAME_STATE.lastFlushedCount = value;
      return value;
   }

   private BeginFrameHelper() {
   }

   public static int lastFlushedDrawCount() {
      return FRAME_STATE.lastFlushedCount;
   }

   public static int pendingDrawCount() {
      return FRAME_STATE.pendingCount;
   }

   public static void hudIconGlow(float value, float value2, float value3, float value4, float value5, float value6) {
      if (hudIconGlowColor != 0 && !(value3 <= 0.0F) && !(value4 <= 0.0F) && !(value5 <= 0.0F) && !(value6 <= 0.0F)) {
         GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
         if (guiGraphicsExtractor != null) {
            float intermediate = 0.25F * value6;
            float clampedValue = clamp(value4, 0.0F, 1.0F);
            queueHudIconHalo(guiGraphicsExtractor, value, value2, value3, intermediate, clampedValue * clamp(value5, 0.0F, 1.0F), value6);
         }
      }
   }

   public static BeginFrameHelper.RingBuilder ring(float value, float value2, float value3) {
      return new BeginFrameHelper.RingBuilder(value, value2, value3);
   }

   public static boolean isPointScissored(float value, float value2) {
      if (SCISSORS.isEmpty()) {
         return false;
      }

      ScreenRectangle screenRectangle = SCISSORS.peek();
      if (!(value < screenRectangle.left())) {
         if (!(value >= screenRectangle.right()) && !(value2 < screenRectangle.top()) && !(value2 >= screenRectangle.bottom())) {
            return false;
         }
      }

      return true;
   }

   public static void popScissor() {
      if (SCISSORS.isEmpty()) {
         throw new IllegalStateException("No active render scissor");
      }

      SCISSORS.pop();
   }

   public static void hudItemGlow(float value, float value2, float value3) {
      if (hudIconGlowColor != 0 && !(value3 <= 0.0F)) {
         GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
         if (guiGraphicsExtractor != null) {
            float computedValue = value + value3 * 0.5F;
            float intermediate = value2 + value3 * 0.5F;
            queueHudIconHalo(guiGraphicsExtractor, computedValue, intermediate, value3, 0.4F, 1.0F, 1.0F);
         }
      }
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   public static BeginFrameHelper.TextureBuilder texture(float value, float value2, float value3, float value4, Identifier texture) {
      return new BeginFrameHelper.TextureBuilder(value, value2, value3, value4, texture);
   }

   public static BeginFrameHelper.RectBuilder rect(float value, float value2, float value3, float value4) {
      return new BeginFrameHelper.RectBuilder(value, value2, value3, value4);
   }

   public static void pushScissor(float value, float value2, float value3, float value4) {
      int roundedValue = Math.round(value);
      int roundedValue2 = Math.round(value2);
      int angle = Math.max(0, Math.round(value + value3) - roundedValue);
      int angle2 = Math.max(0, Math.round(value2 + value4) - roundedValue2);
      ScreenRectangle screenRectangle = new ScreenRectangle(roundedValue, roundedValue2, angle, angle2);
      if (!SCISSORS.isEmpty()) {
         screenRectangle = SCISSORS.peek().intersection(screenRectangle);
         if (screenRectangle == null) {
            screenRectangle = ScreenRectangle.empty();
         }
      }

      if (screenRectangle.width() > 0 && screenRectangle.height() > 0) {
         int angle3 = Math.max(0, screenRectangle.left());
         int angle4 = Math.max(0, screenRectangle.top());
         int angle5 = screenRectangle.right() - angle3;
         int angle6 = screenRectangle.bottom() - angle4;
         screenRectangle = angle5 > 0 && angle6 > 0 ? new ScreenRectangle(angle3, angle4, angle5, angle6) : ScreenRectangle.empty();
      }

      if (screenRectangle.width() > 0 && screenRectangle.height() > 0) {
         Window window = MinecraftContext.mc.getWindow();
         ScreenRectangle screenRectangle2 = new ScreenRectangle(0, 0, window.getGuiScaledWidth(), window.getGuiScaledHeight());
         if (screenRectangle2.intersection(screenRectangle) == null) {
            screenRectangle = ScreenRectangle.empty();
         }
      }

      SCISSORS.push(screenRectangle);
   }

   public static void setHudIconGlowColor(int hudIconGlowColor2) {
      hudIconGlowColor = hudIconGlowColor2;
   }

   private static ScreenRectangle currentScissor() {
      return SCISSORS.peek();
   }

   private static boolean hasEmptyScissor() {
      ScreenRectangle screenRectangle = currentScissor();
      if (screenRectangle != null) {
         if (screenRectangle.width() <= 0) {
            return true;
         }

         if (screenRectangle.height() <= 0) {
            return true;
         }
      }

      return false;
   }

   public static void menuBackground(float value, float value2, float value3, float value4, float value5, float value6, int value7, int value8, int value9) {
      if (!(value3 <= 0.0F) && !(value4 <= 0.0F) && !hasEmptyScissor() && CurrentGuiGraphicsExtractorHelper.isIn2D()) {
         GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
         if (guiGraphicsExtractor != null) {
            BeginFrameHelper.FrameState fRAME_STATE = FRAME_STATE;
            var queued = fRAME_STATE.queued;
            MenuBackgroundRenderState textureSetupHelper2 = new MenuBackgroundRenderState(
               guiGraphicsExtractor.pose(), value, value2, value + value3, value2 + value4, value6, value7, value5, value8, value9, currentScissor()
            );
            queued.add(textureSetupHelper2);
            fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
         }
      }
   }

   public static ScreenRectangle activeScissor() {
      return currentScissor();
   }

   public static void setBackdropBlurScale(float backdropBlurScale2) {
      float minimum = Math.min(1.0F, backdropBlurScale2);
      float maximum = Math.max(0.0F, minimum);
      backdropBlurScale = maximum;
   }

   private static void queueHudIconHalo(GuiGraphicsExtractor guiGraphicsExtractor, float value, float value2, float value3, float value4, float value5, float value6) {
      float computedValue = (hudIconGlowColor >>> 24 & 0xFF) / 255.0F;
      int roundedValue = Math.round(26.4F * computedValue * value5);
      float computedValue2 = value3 * value4;
      if (roundedValue > 0 && !(computedValue2 <= 0.01F)) {
         int value7 = hudIconGlowColor & 16777215 | roundedValue << 24;
         var queued = FRAME_STATE.queued;
         Matrix3x2fStack matrix3x2fStack = guiGraphicsExtractor.pose();
         float intermediate = value - computedValue2;
         float computedValue3 = value2 - computedValue2;
         float intermediate2 = value + computedValue2;
         float intermediate3 = value2 + computedValue2;
         float intermediate4 = 8.0F * value6;
         ShadowHelper shadowHelper = ShadowHelper.shadow(matrix3x2fStack, intermediate, computedValue3, intermediate2, intermediate3, computedValue2, computedValue2, computedValue2, computedValue2, intermediate4, value7, currentScissor());
         queued.add(shadowHelper);
         FRAME_STATE.pendingCount = FRAME_STATE.queued.size();
      }
   }

   public static final class ColorGridBuilder {
      private final int columns;
      private final float x;
      private int selectedIndex;
      private float hoveredThickness;
      private float selectedThickness;
      private final int[] colors;
      private int hoveredIndex;
      private float radius;
      private int color = -1;
      private final float y;
      private final float cellSize;

      private ColorGridBuilder(float x2, float y2, float cellSize2, int columns2, int[] colors2) {
         this.selectedIndex = -1;
         this.hoveredIndex = -1;
         this.x = x2;
         this.y = y2;
         this.cellSize = cellSize2;
         this.columns = columns2;
         this.colors = Objects.requireNonNull(colors2, "colors");
      }

      public BeginFrameHelper.ColorGridBuilder selected(int selectedIndex2, float value) {
         this.selectedIndex = selectedIndex2;
         float maximum = Math.max(0.0F, value);
         this.selectedThickness = maximum;
         return this;
      }

      public BeginFrameHelper.ColorGridBuilder color(int color2) {
         this.color = color2;
         return this;
      }

      public BeginFrameHelper.ColorGridBuilder hovered(int hoveredIndex2, float value) {
         this.hoveredIndex = hoveredIndex2;
         this.hoveredThickness = Math.max(0.0F, value);
         return this;
      }

      public void draw() {
         if (!(this.cellSize <= 0.0F) && this.columns > 0 && this.colors.length != 0 && !BeginFrameHelper.hasEmptyScissor()) {
            if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
               throw new IllegalStateException("Render2D.colorGrid().draw() can only be used inside a 2D render context");
            }

            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor == null) {
               throw new IllegalStateException("Render2D.colorGrid().draw() requires an active GuiGraphicsExtractor");
            }

            BeginFrameHelper.FrameState fRAME_STATE = BeginFrameHelper.FRAME_STATE;
            fRAME_STATE.queued
               .add(
                  new ColorGridRenderState(
                     guiGraphicsExtractor.pose(),
                     this.x,
                     this.y,
                     this.cellSize,
                     this.columns,
                     this.radius,
                     this.color,
                     this.selectedIndex,
                     this.selectedThickness,
                     this.hoveredIndex,
                     this.hoveredThickness,
                     this.colors,
                     BeginFrameHelper.currentScissor()
                  )
               );
            fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
         }
      }

      public BeginFrameHelper.ColorGridBuilder radius(float value) {
         this.radius = Math.max(0.0F, value);
         return this;
      }
   }

   static final class FrameState {
      private int pendingCount;
      private int lastFlushedCount;
      private final List<GuiElementRenderState> queued = new ArrayList<>(128);

      private FrameState() {
      }
   }

   public static final class RectBuilder {
      private float backdropBlurRadius;
      private final float y;
      private final float width;
      private float bottomLeftRadius;
      private final float x;
      private float backdropBlurOpacity;
      private int topRightColor;
      private int bottomRightColor;
      private int bottomLeftColor;
      private int borderColor;
      private int topLeftColor = -1;
      private float shadowBlur;
      private final float height;
      private float borderThickness;
      private int shadowColor;
      private float topRightRadius;
      private boolean matteBackdropBlur;
      private float topLeftRadius;
      private static final float BLUR_TINT_SCALE = 0.8F;
      private boolean hudGlow;
      private float bottomRightRadius;

      public BeginFrameHelper.RectBuilder radius(float radius) {
         return this.radius(radius, radius, radius, radius);
      }

      public BeginFrameHelper.RectBuilder blur(float value, float value2) {
         this.matteBackdropBlur = true;
         return this.blurInternal(value, value2);
      }

      private BeginFrameHelper.RectBuilder blurInternal(float value, float value2) {
         float maximum = Math.max(0.0F, value);
         this.backdropBlurRadius = maximum;
         maximum = Math.max(0.0F, Math.min(1.0F, value2));
         this.backdropBlurOpacity = maximum;
         return this;
      }

      public BeginFrameHelper.RectBuilder radius(float value, float value2, float value3, float value4) {
         float maximum = Math.max(0.0F, value);
         this.topLeftRadius = maximum;
         this.topRightRadius = Math.max(0.0F, value2);
         float maximum2 = Math.max(0.0F, value3);
         this.bottomRightRadius = maximum2;
         float maximum3 = Math.max(0.0F, value4);
         this.bottomLeftRadius = maximum3;
         return this;
      }

      private void queueShadow(BeginFrameHelper.FrameState frameState, GuiGraphicsExtractor guiGraphicsExtractor) {
         int shadowColor2 = this.shadowColor;
         float shadowBlur2 = this.shadowBlur;
         if (this.hudGlow
            && shadowColor2 >>> 24 == 0
            && BeginFrameHelper.hudGlowColor != 0
            && this.matteBackdropBlur
            && this.backdropBlurRadius * BeginFrameHelper.backdropBlurScale > 0.05F
            && this.backdropBlurOpacity > 0.01F) {
            float computedValue = (this.topLeftColor >>> 24 & 0xFF) / 255.0F;
            int roundedValue = Math.round(68.0F * this.backdropBlurOpacity * computedValue);
            shadowColor2 = BeginFrameHelper.hudGlowColor & 16777215 | roundedValue << 24;
            shadowBlur2 = Math.max(6.0F, Math.min(11.0F, this.topLeftRadius * 0.9F));
         }

         if (shadowColor2 >>> 24 != 0 && !(shadowBlur2 <= 0.0F)) {
            int computedValue2 = this.backdropBlurRadius * BeginFrameHelper.backdropBlurScale > 0.05F && this.backdropBlurOpacity > 0.01F ? 1 : 0;
            ShadowHelper shadowHelper = computedValue2 != 0
               ? ShadowHelper.glassShadow(
                  guiGraphicsExtractor.pose(),
                  this.x,
                  this.y,
                  this.x + this.width,
                  this.y + this.height,
                  this.topLeftRadius,
                  this.topRightRadius,
                  this.bottomRightRadius,
                  this.bottomLeftRadius,
                  shadowBlur2,
                  shadowColor2,
                  BeginFrameHelper.currentScissor()
               )
               : ShadowHelper.shadow(
                  guiGraphicsExtractor.pose(),
                  this.x,
                  this.y,
                  this.x + this.width,
                  this.y + this.height,
                  this.topLeftRadius,
                  this.topRightRadius,
                  this.bottomRightRadius,
                  this.bottomLeftRadius,
                  shadowBlur2,
                  shadowColor2,
                  BeginFrameHelper.currentScissor()
               );
            frameState.queued.add(shadowHelper);
         }
      }

      public void draw() {
         if (!(this.width <= 0.0F) && !(this.height <= 0.0F) && !BeginFrameHelper.hasEmptyScissor()) {
            if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
               throw new IllegalStateException("Render2D.draw() can only be used inside a 2D render context");
            }

            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor == null) {
               throw new IllegalStateException("Render2D.draw() requires an active GuiGraphicsExtractor");
            }

            BeginFrameHelper.FrameState fRAME_STATE = BeginFrameHelper.FRAME_STATE;
            this.queueShadow(fRAME_STATE, guiGraphicsExtractor);
            if (!this.queueBackdropBlur(fRAME_STATE, guiGraphicsExtractor)) {
               ShadowHelper shadowHelper = new ShadowHelper(
                  guiGraphicsExtractor.pose(),
                  this.x,
                  this.y,
                  this.x + this.width,
                  this.y + this.height,
                  this.topLeftColor,
                  this.bottomLeftColor,
                  this.bottomRightColor,
                  this.topRightColor,
                  this.topLeftRadius,
                  this.topRightRadius,
                  this.bottomRightRadius,
                  this.bottomLeftRadius,
                  this.borderThickness,
                  this.borderColor,
                  BeginFrameHelper.currentScissor()
               );
               fRAME_STATE.queued.add(shadowHelper);
               fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
            }
         }
      }

      public BeginFrameHelper.RectBuilder hudGlow(boolean hudGlow2) {
         this.hudGlow = hudGlow2;
         return this;
      }

      public BeginFrameHelper.RectBuilder blur(float value) {
         return this.blur(value, 1.0F);
      }

      public BeginFrameHelper.RectBuilder color(int color) {
         return this.color(color, color, color, color);
      }

      public BeginFrameHelper.RectBuilder glass(float alpha, float value, float value2) {
         int value3 = 1;
         this.matteBackdropBlur = false;
         int value4 = 1;
         value3 = WriteHexByteHelper.multiplyAlpha(DefaultPalette.BACKGROUND_PRIMARY_50, alpha);
         BeginFrameHelper.RectBuilder rectBuilder = this.color(value3);
         float maximum = Math.max(0.5F, value);
         value4 = WriteHexByteHelper.multiplyAlpha(DefaultPalette.OUTLINES_MEDIUM, alpha);
         return rectBuilder.border(maximum, value4).blurInternal(value2, alpha);
      }

      private RectBuilder(float x2, float y2, float width2, float height2) {
         this.topRightColor = -1;
         this.bottomRightColor = -1;
         this.bottomLeftColor = -1;
         this.borderColor = 0;
         this.shadowColor = 0;
         this.backdropBlurOpacity = 1.0F;
         this.hudGlow = true;
         this.x = x2;
         this.y = y2;
         this.width = width2;
         this.height = height2;
      }

      private boolean queueBackdropBlur(BeginFrameHelper.FrameState frameState, GuiGraphicsExtractor guiGraphicsExtractor) {
         float computedValue = this.backdropBlurRadius * BeginFrameHelper.backdropBlurScale;
         if (!(computedValue <= 0.05F) && !(this.backdropBlurOpacity <= 0.01F)) {
            GpuTextureView gpuTextureView = this.matteBackdropBlur ? AcquireViewHelper.acquireView() : RunPassHelper.acquireView();
            if (gpuTextureView == null) {
               return false;
            }

            int window = Math.max(1, MinecraftContext.mc.getWindow().getGuiScale());
            int computedValue2 = Math.round((this.topLeftColor >>> 24 & 0xFF) * 0.8F);
            ScissorAreaHelper scissorAreaHelper = this.matteBackdropBlur
               ? new BlurTextureRenderState(
                  guiGraphicsExtractor.pose(),
                  this.x,
                  this.y,
                  this.x + this.width,
                  this.y + this.height,
                  this.topLeftColor & 16777215 | computedValue2 << 24,
                  this.topLeftRadius,
                  computedValue * window,
                  this.backdropBlurOpacity,
                  gpuTextureView,
                  BeginFrameHelper.currentScissor()
               )
               : new BlurRectRenderState(
                  guiGraphicsExtractor.pose(),
                  this.x,
                  this.y,
                  this.x + this.width,
                  this.y + this.height,
                  this.topLeftColor & 16777215 | computedValue2 << 24,
                  this.topLeftRadius,
                  computedValue * window,
                  this.backdropBlurOpacity,
                  gpuTextureView,
                  BeginFrameHelper.currentScissor()
               );
            frameState.queued.add(scissorAreaHelper);
            if (this.borderThickness > 0.0F && this.borderColor >>> 24 != 0) {
               frameState.queued
                  .add(
                     new ShadowHelper(
                        guiGraphicsExtractor.pose(),
                        this.x,
                        this.y,
                        this.x + this.width,
                        this.y + this.height,
                        0,
                        this.topLeftRadius,
                        this.borderThickness,
                        this.borderColor,
                        BeginFrameHelper.currentScissor()
                     )
                  );
            }

            frameState.pendingCount = frameState.queued.size();
            return true;
         } else {
            return false;
         }
      }

      public BeginFrameHelper.RectBuilder color(int topLeftColor2, int topRightColor2, int bottomRightColor2, int bottomLeftColor2) {
         this.topLeftColor = topLeftColor2;
         this.topRightColor = topRightColor2;
         this.bottomRightColor = bottomRightColor2;
         this.bottomLeftColor = bottomLeftColor2;
         return this;
      }

      public BeginFrameHelper.RectBuilder shadow(int shadowColor2, float value) {
         this.shadowColor = shadowColor2;
         this.shadowBlur = Math.max(0.0F, value);
         return this;
      }

      public BeginFrameHelper.RectBuilder border(float value, int borderColor2) {
         this.borderThickness = Math.max(0.0F, value);
         this.borderColor = borderColor2;
         return this;
      }
   }

   public static final class RingBuilder {
      private final float size;
      private final float y;
      private int color;
      private float thickness = 1.0F;
      private final float x;
      private float progress;
      private int waveStyle;
      private int bottomColor;

      public BeginFrameHelper.RingBuilder wavy(boolean condition) {
         int intermediate = -1;
         intermediate = (byte)-1;
         intermediate = (byte)-1;
         intermediate = condition ? 2 : 0;
         this.waveStyle = intermediate;
         return this;
      }

      public BeginFrameHelper.RingBuilder thickness(float value) {
         float maximum = Math.max(0.0F, value);
         this.thickness = maximum;
         return this;
      }

      public void draw() {
         if (!(this.size <= 0.0F) && !(this.thickness <= 0.0F) && this.color >>> 24 != 0 && !BeginFrameHelper.hasEmptyScissor()) {
            if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
               throw new IllegalStateException("Render2D.ring().draw() can only be used inside a 2D render context");
            }

            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor == null) {
               throw new IllegalStateException("Render2D.ring().draw() requires an active GuiGraphicsExtractor");
            }

            BeginFrameHelper.FrameState fRAME_STATE = BeginFrameHelper.FRAME_STATE;
            fRAME_STATE.queued
               .add(
                  new CircularProgressRenderState(
                     guiGraphicsExtractor.pose(),
                     this.x,
                     this.y,
                     this.size,
                     this.thickness,
                     this.color,
                     this.bottomColor,
                     this.progress,
                     this.waveStyle,
                     BeginFrameHelper.currentScissor()
                  )
               );
            fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
         }
      }

      private RingBuilder(float x2, float y2, float size2) {
         this.color = -1;
         this.bottomColor = -1;
         this.progress = 1.0F;
         this.x = x2;
         this.y = y2;
         this.size = size2;
      }

      public BeginFrameHelper.RingBuilder color(int color2, int bottomColor2) {
         this.color = color2;
         this.bottomColor = bottomColor2;
         return this;
      }

      public BeginFrameHelper.RingBuilder color(int color2) {
         return this.color(color2, color2);
      }

      public BeginFrameHelper.RingBuilder progress(float value) {
         float minimum = Math.min(1.0F, value);
         this.progress = Math.max(0.0F, minimum);
         return this;
      }

      public BeginFrameHelper.RingBuilder wavyTrack(boolean waveStyle2) {
         this.waveStyle = waveStyle2 ? 1 : 0;
         return this;
      }
   }

   public static final class TextBuilder {
      private float letterSpacing;
      private float offsetY;
      private int weight = 500;
      private int outlineColor;
      private final float y;
      private float scale;
      private TextHeightHelper font;
      private TextAlignment align;
      private final String text;
      private float outlineThickness;
      private float offsetX;
      private final float size;
      private int color = -1;
      private final float x;
      private float oblique;

      public BeginFrameHelper.TextBuilder scale(float value) {
         float maximum = Math.max(1.0E-4F, value);
         this.scale = maximum;
         return this;
      }

      private void drawVanillaFallback(GuiGraphicsExtractor guiGraphicsExtractor, TextHeightHelper textHeightHelper, float coordinate) {
         BeginFrameHelper.flush();
         Font font2 = MinecraftContext.mc.font;
         float computedValue = coordinate / 9.0F;
         int value = font2.width(this.text);

         int color2 = switch (this.align) {
            case LEFT -> 0;
            case CENTER -> -value / 2;
            case RIGHT -> -value;
         };
         float computedValue2 = textHeightHelper.ascender(coordinate) - 7.0F * computedValue;
         guiGraphicsExtractor.pose().pushMatrix();

         try {
            guiGraphicsExtractor.pose().translate(this.x + this.offsetX, this.y + this.offsetY + computedValue2);
            guiGraphicsExtractor.pose().scale(computedValue, computedValue);
            guiGraphicsExtractor.text(font2, this.text, color2, 0, this.color, false);
         } finally {
            guiGraphicsExtractor.pose().popMatrix();
         }
      }

      public BeginFrameHelper.TextBuilder oblique(float oblique2) {
         this.oblique = oblique2;
         return this;
      }

      public void draw() {
         if (!this.text.isEmpty() && !(this.size <= 0.0F) && !(this.scale <= 0.001F) && !BeginFrameHelper.hasEmptyScissor()) {
            if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
               throw new IllegalStateException("Render2D.text().draw() can only be used inside a 2D render context");
            }

            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor == null) {
               throw new IllegalStateException("Render2D.text().draw() requires an active GuiGraphicsExtractor");
            }

            TextHeightHelper textHeightHelper = this.resolveFont();
            float color2 = this.size * this.scale;
            String text2 = normalizeCommonPunctuation(this.text);
            TextHeightHelper.Paragraph paragraph = textHeightHelper.shape(text2);
            if (paragraph.missingGlyphs()) {
               this.drawVanillaFallback(guiGraphicsExtractor, textHeightHelper, color2);
            } else {
               float color3 = this.offsetY - (color2 - this.size) * 0.5F;
               BeginFrameHelper.FrameState fRAME_STATE = BeginFrameHelper.FRAME_STATE;
               if (BeginFrameHelper.hudIconGlowColor != 0
                  && (textHeightHelper == InterBoldHelper.csGuiIcons() || textHeightHelper == InterBoldHelper.clickGuiIcons() || textHeightHelper == InterBoldHelper.skycore())) {
                  float computedValue = (this.color >>> 24 & 0xFF) / 255.0F;
                  float computedValue2 = (BeginFrameHelper.hudIconGlowColor >>> 24 & 0xFF) / 255.0F;
                  float width = this.letterSpacing * this.scale;
                  float pOSITIVE_INFINITY = Float.POSITIVE_INFINITY;
                  float pOSITIVE_INFINITY2 = Float.POSITIVE_INFINITY;
                  float nEGATIVE_INFINITY = Float.NEGATIVE_INFINITY;
                  float nEGATIVE_INFINITY2 = Float.NEGATIVE_INFINITY;
                  float y2 = this.y + color3 + textHeightHelper.ascender(color2);
                  float value = textHeightHelper.lineHeight(color2);

                  for (int value2 = 0; value2 < paragraph.lines().size(); value2++) {
                     TextHeightHelper.Line line = paragraph.lines().get(value2);
                     float value3 = line.width(color2, width);

                     float x2 = switch (this.align) {
                        case LEFT -> this.x;
                        case CENTER -> this.x - value3 * 0.5F;
                        case RIGHT -> this.x - value3;
                     };
                     x2 += this.offsetX;
                     float computedValue3 = y2 + value * value2;

                     for (int value4 = 0; value4 < line.glyphCount(); value4++) {
                        HeightHelper heightHelper = line.glyphAt(value4);
                        if (heightHelper.hasGeometry()) {
                           float computedValue4 = x2 + line.penX(value4, color2, width);
                           pOSITIVE_INFINITY = Math.min(pOSITIVE_INFINITY, computedValue4 + heightHelper.left() * color2);
                           nEGATIVE_INFINITY = Math.max(nEGATIVE_INFINITY, computedValue4 + heightHelper.right() * color2);
                           pOSITIVE_INFINITY2 = Math.min(pOSITIVE_INFINITY2, computedValue3 - heightHelper.top() * color2);
                           nEGATIVE_INFINITY2 = Math.max(nEGATIVE_INFINITY2, computedValue3 - heightHelper.bottom() * color2);
                        }
                     }
                  }

                  if (pOSITIVE_INFINITY <= nEGATIVE_INFINITY && pOSITIVE_INFINITY2 <= nEGATIVE_INFINITY2) {
                     BeginFrameHelper.queueHudIconHalo(guiGraphicsExtractor, (pOSITIVE_INFINITY + nEGATIVE_INFINITY) * 0.5F, (pOSITIVE_INFINITY2 + nEGATIVE_INFINITY2) * 0.5F, color2, 0.25F, computedValue, 1.0F);
                  }

                  int roundedValue = Math.round(33.6F * computedValue2 * computedValue);
                  if (roundedValue > 0) {
                     int value5 = BeginFrameHelper.hudIconGlowColor & 16777215 | roundedValue << 24;
                     fRAME_STATE.queued
                        .add(
                           new BaselineYHelper(
                              guiGraphicsExtractor.pose(),
                              textHeightHelper,
                              this.x,
                              this.y,
                              color2,
                              paragraph,
                              value5,
                              0,
                              0.0F,
                              this.letterSpacing * this.scale,
                              this.align,
                              this.offsetX,
                              color3,
                              this.oblique,
                              BeginFrameHelper.currentScissor()
                           )
                        );
                  }
               }

               BaselineYHelper baselineYHelper = new BaselineYHelper(
                  guiGraphicsExtractor.pose(),
                  textHeightHelper,
                  this.x,
                  this.y,
                  color2,
                  paragraph,
                  this.color,
                  this.outlineColor,
                  this.outlineThickness * this.scale,
                  this.letterSpacing * this.scale,
                  this.align,
                  this.offsetX,
                  color3,
                  this.oblique,
                  BeginFrameHelper.currentScissor()
               );
               fRAME_STATE.queued.add(baselineYHelper);
               fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
            }
         }
      }

      public BeginFrameHelper.TextBuilder outline(int outlineColor2, float value) {
         this.outlineColor = outlineColor2;
         float maximum = Math.max(0.0F, value);
         this.outlineThickness = maximum;
         return this;
      }

      public BeginFrameHelper.TextBuilder offset(float offsetX2, float offsetY2) {
         this.offsetX = offsetX2;
         this.offsetY = offsetY2;
         return this;
      }

      private TextBuilder(float x2, float y2, float size2, String text2) {
         this.outlineColor = 0;
         this.align = TextAlignment.LEFT;
         this.scale = 1.0F;
         this.x = x2;
         this.y = y2;
         this.size = size2;
         this.text = Objects.requireNonNull(text2, "text");
      }

      private TextHeightHelper resolveFont() {
         TextHeightHelper textHeightHelper;
         if (this.font != null) {
            textHeightHelper = this.font;
         } else {
            textHeightHelper = InterBoldHelper.forWeight(this.weight);
         }

         return textHeightHelper;
      }

      public BeginFrameHelper.TextBuilder weight(int weight2) {
         if (weight2 >= 1 && weight2 <= 1000) {
            this.weight = weight2;
            return this;
         } else {
            throw new IllegalArgumentException("Font weight out of range: " + weight2);
         }
      }

      public BeginFrameHelper.TextBuilder letterSpacing(float letterSpacing2) {
         this.letterSpacing = letterSpacing2;
         return this;
      }

      public BeginFrameHelper.TextBuilder color(int color2) {
         this.color = color2;
         return this;
      }

      public BeginFrameHelper.TextBuilder align(TextAlignment textAlignment) {
         TextAlignment textAlignment2 = Objects.requireNonNull(textAlignment, "align");
         this.align = textAlignment2;
         return this;
      }

      public BeginFrameHelper.TextBuilder font(Identifier texture) {
         this.font = TextHeightHelper.load(Objects.requireNonNull(texture, "id"));
         return this;
      }

      public BeginFrameHelper.TextBuilder font(TextHeightHelper textHeightHelper) {
         TextHeightHelper textHeightHelper2 = Objects.requireNonNull(textHeightHelper, "font");
         this.font = textHeightHelper2;
         return this;
      }

      private static String normalizeCommonPunctuation(String text2) {
         String text3 = text2.replace('—', '-').replace('–', '-');
         String text4 = text3.replace('−', '-').replace(' ', ' ');
         String text5 = text4.replace('‘', '\'');
         String text6 = text5.replace('’', '\'').replace('“', '"');
         String text7 = text6.replace('”', '"');
         return text7.replace("…", "...");
      }
   }

   public static final class TextureBuilder {
      private float v1;
      private final Identifier textureId;
      private final float y;
      private final float width;
      private final float height;
      private int color = -1;
      private float u1 = 1.0F;
      private float v0;
      private boolean managed;
      private float radius;
      private boolean iconGlow;
      private float u0;
      private final float x;
      private float iconGlowSize;
      private float iconGlowStrength;

      public BeginFrameHelper.TextureBuilder iconGlow(float value, float value2) {
         this.iconGlow = true;
         float clampedValue = BeginFrameHelper.clamp(value, 0.0F, 1.0F);
         this.iconGlowStrength = clampedValue;
         float maximum = Math.max(0.0F, value2);
         this.iconGlowSize = maximum;
         return this;
      }

      public BeginFrameHelper.TextureBuilder iconGlow() {
         this.iconGlow = true;
         return this;
      }

      public void draw() {
         if (!(this.width <= 0.0F) && !(this.height <= 0.0F) && !BeginFrameHelper.hasEmptyScissor()) {
            if (!CurrentGuiGraphicsExtractorHelper.isIn2D()) {
               throw new IllegalStateException("Render2D.texture().draw() can only be used inside a 2D render context");
            }

            GuiGraphicsExtractor guiGraphicsExtractor = CurrentGuiGraphicsExtractorHelper.currentGuiGraphicsExtractor();
            if (guiGraphicsExtractor == null) {
               throw new IllegalStateException("Render2D.texture().draw() requires an active GuiGraphicsExtractor");
            }

            BeginFrameHelper.FrameState fRAME_STATE = BeginFrameHelper.FRAME_STATE;
            if (this.iconGlow && BeginFrameHelper.hudIconGlowColor != 0) {
               float computedValue = (this.color >>> 24 & 0xFF) / 255.0F;
               float computedValue2 = (BeginFrameHelper.hudIconGlowColor >>> 24 & 0xFF) / 255.0F;
               float maximum = Math.max(this.width, this.height);
               BeginFrameHelper.queueHudIconHalo(
                  guiGraphicsExtractor,
                  this.x + this.width * 0.5F,
                  this.y + this.height * 0.5F,
                  maximum,
                  0.4F * this.iconGlowSize,
                  computedValue * this.iconGlowStrength,
                  this.iconGlowSize
               );
               int roundedValue = Math.round(33.6F * computedValue * computedValue2 * this.iconGlowStrength);
               if (roundedValue > 0) {
                  int width2 = BeginFrameHelper.hudIconGlowColor & 16777215 | roundedValue << 24;
                  fRAME_STATE.queued
                     .add(
                        new RoundedTextureRenderState(
                           guiGraphicsExtractor.pose(),
                           this.textureId,
                           this.managed,
                           this.x,
                           this.y,
                           this.width,
                           this.height,
                           this.u0,
                           this.v0,
                           this.u1,
                           this.v1,
                           width2,
                           this.radius,
                           BeginFrameHelper.currentScissor()
                        )
                     );
               }
            }

            RoundedTextureRenderState pipelineHelper3 = new RoundedTextureRenderState(
               guiGraphicsExtractor.pose(),
               this.textureId,
               this.managed,
               this.x,
               this.y,
               this.width,
               this.height,
               this.u0,
               this.v0,
               this.u1,
               this.v1,
               this.color,
               this.radius,
               BeginFrameHelper.currentScissor()
            );
            fRAME_STATE.queued.add(pipelineHelper3);
            fRAME_STATE.pendingCount = fRAME_STATE.queued.size();
         }
      }

      public BeginFrameHelper.TextureBuilder managed() {
         this.managed = true;
         return this;
      }

      public BeginFrameHelper.TextureBuilder color(int color2) {
         this.color = color2;
         return this;
      }

      private TextureBuilder(float x2, float y2, float width2, float height2, Identifier textureId2) {
         this.v1 = 1.0F;
         this.iconGlowStrength = 1.0F;
         this.iconGlowSize = 1.0F;
         this.x = x2;
         this.y = y2;
         this.width = width2;
         this.height = height2;
         this.textureId = Objects.requireNonNull(textureId2, "textureId");
      }

      public BeginFrameHelper.TextureBuilder uv(float u02, float v02, float u12, float v12) {
         this.u0 = u02;
         this.v0 = v02;
         this.u1 = u12;
         this.v1 = v12;
         return this;
      }

      public BeginFrameHelper.TextureBuilder radius(float radius2) {
         this.radius = radius2;
         return this;
      }
   }
}
