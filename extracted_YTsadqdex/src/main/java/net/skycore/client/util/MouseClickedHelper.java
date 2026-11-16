package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.render.ViewModelModule;
import net.skycore.client.theme.ThemeManager;

public final class MouseClickedHelper extends Screen {
   private HumanoidArm activeArm;
   private static final double DRAG_SENS = 0.0048;
   private float hoverAnimation;
   private double lastMx;
   private final boolean showChrome;
   private double lastMy;
   private long lastAnimationAt;
   private static final double SCROLL_STEP = 0.07;
   private float dragAnimation;
   private final long openedAt = System.nanoTime();

   private boolean hasItem(HumanoidArm humanoidArm) {
      if (this.minecraft != null && this.minecraft.player != null) {
         HumanoidArm mainArm = this.minecraft.player.getMainArm();
         InteractionHand interactionHand;
         if (humanoidArm == mainArm) {
            interactionHand = InteractionHand.MAIN_HAND;
         } else {
            interactionHand = InteractionHand.OFF_HAND;
         }

         InteractionHand interactionHand2 = interactionHand;
         ItemStack itemInHand = this.minecraft.player.getItemInHand(interactionHand2);
         return itemInHand != null && !itemInHand.isEmpty();
      } else {
         return false;
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent lastMy2, boolean condition) {
      if (lastMy2.button() != 0) {
         return true;
      }

      double x = lastMy2.x();
      double y = lastMy2.y();
      HumanoidArm humanoidArm = this.hitTest(x, y);
      if (humanoidArm == null) {
         return true;
      }

      this.activeArm = humanoidArm;
      ViewModelModule.INSTANCE.setHoveredArm(humanoidArm);
      ViewModelModule.INSTANCE.setDragging(true);
      double x2 = lastMy2.x();
      this.lastMx = x2;
      this.lastMy = lastMy2.y();
      return true;
   }

   @Override
   public boolean mouseScrolled(double value, double value2, double value3, double value4) {
      HumanoidArm humanoidArm;
      if (ViewModelModule.INSTANCE.isDragging()) {
         humanoidArm = this.activeArm;
      } else {
         humanoidArm = this.hitTest(value, value2);
      }

      HumanoidArm humanoidArm2 = humanoidArm;
      if (humanoidArm2 != null && value4 != 0.0) {
         ViewModelModule.INSTANCE.setHoveredArm(humanoidArm2);
         double computedValue = value4 * 0.07;
         ViewModelModule.INSTANCE.nudgeZ(humanoidArm2, computedValue);
         return true;
      } else {
         return true;
      }
   }

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   private static float easeOutCubic(float progress) {
      float computedValue = 1.0F - clamp(progress, 0.0F, 1.0F);
      return 1.0F - computedValue * computedValue * computedValue;
   }

   private HumanoidArm hitTest(double width, double value) {
      if (this.minecraft != null) {
         if (this.minecraft.player != null) {
            HumanoidArm humanoidArm;
            if (width < this.width * 0.5) {
               humanoidArm = HumanoidArm.LEFT;
            } else {
               humanoidArm = HumanoidArm.RIGHT;
            }

            HumanoidArm humanoidArm2 = humanoidArm;
            return this.hasItem(humanoidArm2) ? humanoidArm2 : null;
         }
      }

      return null;
   }

   private void forceFreeCursor() {
      Minecraft minecraft = this.minecraft;
      if (minecraft != null && minecraft.mouseHandler != null) {
         if (minecraft.mouseHandler.isMouseGrabbed()) {
            minecraft.mouseHandler.releaseMouse();
         }
      }
   }

   public MouseClickedHelper() {
      this(true);
   }

   private static float clamp(float value, float value2, float value3) {
      return Math.max(value2, Math.min(value3, value));
   }

   @Override
   public void tick() {
      this.forceFreeCursor();
      ViewModelModule iNSTANCE = ViewModelModule.INSTANCE;
      if (!iNSTANCE.isEnabled() || !iNSTANCE.isWorldEditing()) {
         this.onClose();
      }
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor guiGraphicsExtractor, int coordinate, int coordinate2, float coordinate3) {
      ViewModelModule iNSTANCE = ViewModelModule.INSTANCE;
      if (!iNSTANCE.isEnabled()) {
         this.onClose();
      } else {
         HumanoidArm humanoidArm = this.hitTest(coordinate, coordinate2);
         if (!iNSTANCE.isDragging()) {
            iNSTANCE.setHoveredArm(humanoidArm);
            this.activeArm = humanoidArm;
         } else if (this.activeArm != null) {
            iNSTANCE.setHoveredArm(this.activeArm);
         }

         this.updateAnimations(iNSTANCE);
         if (this.showChrome && this.minecraft != null) {
            CurrentGuiGraphicsExtractorHelper.enter2D(guiGraphicsExtractor, this.minecraft.getDeltaTracker());
            BeginFrameHelper.beginFrame();

            try {
               this.renderHint(coordinate, coordinate2, iNSTANCE);
               BeginFrameHelper.flush();
            } finally {
               CurrentGuiGraphicsExtractorHelper.exit2D();
            }
         }
      }
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent progress) {
      if (progress.button() == 0) {
         ViewModelModule.INSTANCE.setDragging(false);
         this.lastMx = Double.NaN;
         this.lastMy = Double.NaN;
         double x = progress.x();
         HumanoidArm humanoidArm = this.hitTest(x, progress.y());
         ViewModelModule.INSTANCE.setHoveredArm(humanoidArm);
         this.activeArm = humanoidArm;
      }

      return true;
   }

   private static float approach(float value, float value2, float value3, float value4) {
      float computedValue = 1.0F - (float)Math.pow(value4, value3);
      float computedValue2 = value + (value2 - value) * computedValue;
      return Math.abs(value2 - computedValue2) < 0.001F ? value2 : computedValue2;
   }

   private void updateAnimations(ViewModelModule viewModelModule) {
      long nowNanos = System.nanoTime();
      float intermediate = (float)(nowNanos - this.lastAnimationAt) / 1.0E9F;
      float clampedValue = clamp(intermediate, 0.0F, 0.05F);
      this.lastAnimationAt = nowNanos;
      float value = this.activeArm == null ? 0.0F : 1.0F;
      float value2 = viewModelModule.isDragging() && this.activeArm != null ? 1.0F : 0.0F;
      float intermediate2 = approach(this.hoverAnimation, value, clampedValue, 8.0E-5F);
      this.hoverAnimation = intermediate2;
      this.dragAnimation = approach(this.dragAnimation, value2, clampedValue, 2.0E-5F);
   }

   private static float smoothstep(float progress) {
      progress = clamp(progress, 0.0F, 1.0F);
      return progress * progress * (3.0F - 2.0F * progress);
   }

   @Override
   public void onClose() {
      ViewModelModule.INSTANCE.setWorldEditing(false);
      if (this.minecraft != null) {
         this.minecraft.setScreenAndShow(null);
      } else {
         super.onClose();
      }
   }

   @Override
   public void removed() {
      ViewModelModule.INSTANCE.setWorldEditing(false);
      super.removed();
   }

   private void renderHint(int coordinate, int coordinate2, ViewModelModule viewModelModule) {
      ThemeManager themeManager = SkycoreClient.themes();
      float alpha = easeOutCubic(clamp((float)(System.nanoTime() - this.openedAt) / 2.2E8F, 0.0F, 1.0F));
      float computedValue = 12.0F - (1.0F - alpha) * 5.0F;
      String text = "ViewModel";
      String text2 = "Drag item   |   Wheel depth   |   Esc done";
      float width = 10.0F;
      float width2 = 9.0F;
      float value = 8.0F;
      float width3 = InterBoldHelper.forWeight(600).getWidth(text, width);
      float width4 = InterBoldHelper.forWeight(500).getWidth(text2, width2);
      float width5 = width3 + value + width4;
      float computedValue2 = (this.width - width5) * 0.5F;
      BeginFrameHelper.text(computedValue2, computedValue, width, text).font(InterBoldHelper.forWeight(600)).color(alpha(themeManager.accent(), alpha)).draw();
      BeginFrameHelper.text(computedValue2 + width3 + value, computedValue + 0.5F, width2, text2)
         .font(InterBoldHelper.forWeight(500))
         .color(alpha(themeManager.textSecondary(), 0.92F * alpha))
         .draw();
      if (this.activeArm != null) {
         float value2 = smoothstep(this.hoverAnimation);
         float value3 = smoothstep(this.dragAnimation);
         float computedValue3 = 8.0F + 4.0F * value3;
         float computedValue4 = coordinate - computedValue3 * 0.5F;
         float computedValue5 = coordinate2 - computedValue3 * 0.5F;
         BeginFrameHelper.rect(computedValue4, computedValue5, computedValue3, computedValue3)
            .color(alpha(themeManager.accent(), 0.025F * alpha * value2))
            .border(0.8F, alpha(themeManager.accent(), (0.38F + 0.42F * value3) * alpha * value2))
            .radius(computedValue3 * 0.5F)
            .draw();
      }
   }

   @Override
   public boolean keyPressed(KeyEvent keyEvent) {
      if (keyEvent.key() == 256) {
         this.onClose();
         return true;
      } else {
         return true;
      }
   }

   private static int alpha(int value, float value2) {
      int value3 = value >>> 24 & 0xFF;
      float value4 = value3;
      int computedValue = Math.round(value4 * clamp(value2, 0.0F, 1.0F));
      return value & 16777215 | computedValue << 24;
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent lastMx2, double value, double value2) {
      ViewModelModule iNSTANCE = ViewModelModule.INSTANCE;
      if (iNSTANCE.isDragging() && this.activeArm != null && lastMx2.button() == 0) {
         double value3 = value;
         double value4 = value2;
         if (Math.abs(value) < 1.0E-4 && Math.abs(value2) < 1.0E-4 && !Double.isNaN(this.lastMx) && !Double.isNaN(this.lastMy)) {
            value3 = lastMx2.x() - this.lastMx;
            value4 = lastMx2.y() - this.lastMy;
         }

         iNSTANCE.nudge(this.activeArm, value3 * 0.0048, -value4 * 0.0048, 0.0);
         this.lastMx = lastMx2.x();
         this.lastMy = lastMx2.y();
         return true;
      } else {
         return true;
      }
   }

   @Override
   public void extractBackground(GuiGraphicsExtractor guiGraphicsExtractor, int value, int value2, float value3) {
   }

   @Override
   protected void init() {
      super.init();
      this.forceFreeCursor();
   }

   public MouseClickedHelper(boolean showChrome2) {
      super(Component.literal("ViewModel Editor"));
      this.lastMx = Double.NaN;
      this.lastMy = Double.NaN;
      this.lastAnimationAt = System.nanoTime();
      this.showChrome = showChrome2;
   }
}
