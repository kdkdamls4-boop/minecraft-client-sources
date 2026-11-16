package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.model.object.skull.SkullModelBase.State;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

public final class CustomSkullRenderer implements NoDataSpecialModelRenderer {
   private final float animation;
   private final String skinUrl;
   private final RenderType fallback;
   private final SkullModelBase model;

   public CustomSkullRenderer(SkullModelBase model2, float animation2, RenderType fallback2, String skinUrl2) {
      this.model = model2;
      this.animation = animation2;
      this.fallback = fallback2;
      this.skinUrl = skinUrl2;
   }

   @Override
   public void getExtents(Consumer<Vector3fc> consumer) {
      PoseStack poseStack = new PoseStack();
      poseStack.translate(0.5F, 0.0F, 0.5F);
      poseStack.scale(-1.0F, -1.0F, 1.0F);
      State state = new State();
      state.animationPos = this.animation;
      state.yRot = 180.0F;
      this.model.setupAnim(state);
      this.model.root().getExtentsForGui(poseStack, consumer);
   }

   @Override
   public void submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int value, int value2, boolean condition, int value3) {
      RenderType fallback2 = this.fallback;
      if (this.skinUrl != null) {
         TextureHashHelper.request(this.skinUrl);
         Identifier ifReady = TextureHashHelper.getIfReady(this.skinUrl);
         if (ifReady != null) {
            fallback2 = SkullBlockRenderer.getPlayerSkinRenderType(ifReady);
         }
      }

      SkullBlockRenderer.submitSkull(this.animation, poseStack, submitNodeCollector, value, this.model, fallback2, value3, null);
   }
}
