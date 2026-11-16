package net.skycore.client.util;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2fc;

public abstract class ScissorAreaHelper implements GuiElementRenderState {
   private final float m11;
   private final float m00;
   private final float m01;
   private final float m21;
   private final float m10;
   private final ScreenRectangle bounds;
   private final ScreenRectangle scissor;
   private final float m20;

   @Override
   public final ScreenRectangle scissorArea() {
      return this.scissor;
   }

   @Override
   public final ScreenRectangle bounds() {
      return this.bounds;
   }

   protected final float transformY(float value, float value2) {
      return this.m01 * value + this.m11 * value2 + this.m21;
   }

   protected ScissorAreaHelper(Matrix3x2fc m002, ScreenRectangle scissor2, float value, float value2, float value3, float value4) {
      this.m00 = m002.m00();
      this.m01 = m002.m01();
      this.m10 = m002.m10();
      this.m11 = m002.m11();
      this.m20 = m002.m20();
      this.m21 = m002.m21();
      this.scissor = scissor2;
      this.bounds = new ScreenRectangle(Math.round(value), Math.round(value2), Math.max(1, Math.round(value3)), Math.max(1, Math.round(value4)))
         .transformMaxBounds(m002);
   }

   protected final float transformX(float value, float value2) {
      float computedValue = this.m00 * value;
      float computedValue2 = computedValue + this.m10 * value2;
      return computedValue2 + this.m20;
   }
}
