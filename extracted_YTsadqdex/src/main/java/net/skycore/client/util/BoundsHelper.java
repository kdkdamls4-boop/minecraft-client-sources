package net.skycore.client.util;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;

public final class BoundsHelper implements GuiElementRenderState {
   private final List<GuiElementRenderState> parts;
   private final ScreenRectangle bounds;

   @Override
   public ScreenRectangle bounds() {
      return this.bounds;
   }

   @Override
   public void buildVertices(VertexConsumer vertexConsumer) {
      Iterator iterator = this.parts.iterator();

      while (iterator.hasNext()) {
         GuiElementRenderState element = (GuiElementRenderState)iterator.next();
         element.buildVertices(vertexConsumer);
      }
   }

   @Override
   public TextureSetup textureSetup() {
      GuiElementRenderState first = this.parts.getFirst();
      return first.textureSetup();
   }

   public BoundsHelper(List<GuiElementRenderState> guiElementRenderStates) {
      if (!guiElementRenderStates.isEmpty() && guiElementRenderStates.getFirst() instanceof FontRenderState fontRenderState) {
         int mAX_VALUE = Integer.MAX_VALUE;
         int mAX_VALUE2 = Integer.MAX_VALUE;
         int mIN_VALUE = Integer.MIN_VALUE;
         int mIN_VALUE2 = Integer.MIN_VALUE;

         for (GuiElementRenderState guiElementRenderState : guiElementRenderStates) {
            if (!compatible(fontRenderState, guiElementRenderState)) {
               throw new IllegalArgumentException("Incompatible text batch");
            }

            ScreenRectangle screenRectangle = guiElementRenderState.bounds();
            mAX_VALUE = Math.min(mAX_VALUE, screenRectangle.left());
            mAX_VALUE2 = Math.min(mAX_VALUE2, screenRectangle.top());
            mIN_VALUE = Math.max(mIN_VALUE, screenRectangle.right());
            mIN_VALUE2 = Math.max(mIN_VALUE2, screenRectangle.bottom());
         }

         this.parts = List.copyOf(guiElementRenderStates);
         this.bounds = new ScreenRectangle(mAX_VALUE, mAX_VALUE2, mIN_VALUE - mAX_VALUE, mIN_VALUE2 - mAX_VALUE2);
      } else {
         throw new IllegalArgumentException("Expected text elements");
      }
   }

   @Override
   public RenderPipeline pipeline() {
      return this.parts.getFirst().pipeline();
   }

   @Override
   public ScreenRectangle scissorArea() {
      GuiElementRenderState first = this.parts.getFirst();
      return first.scissorArea();
   }

   public static boolean compatible(FontRenderState fontRenderState, GuiElementRenderState guiElementRenderState) {
      if (guiElementRenderState instanceof FontRenderState fontRenderState2 && fontRenderState.font() == fontRenderState2.font()) {
         ScreenRectangle screenRectangle = fontRenderState.scissorArea();
         if (Objects.equals(screenRectangle, fontRenderState2.scissorArea())) {
            return true;
         }
      }

      return false;
   }
}
