package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.PlayerSkinRenderCache.RenderInfo;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class ExtractArgumentHelper implements SpecialModelRenderer<RenderInfo> {
   private static final float GUI_LIFT = -0.0374F;
   private final SpecialModelRenderer<RenderInfo> delegate;

   public ExtractArgumentHelper(SpecialModelRenderer<RenderInfo> delegate2) {
      this.delegate = delegate2;
   }

   @Override
   public void getExtents(Consumer<Vector3fc> consumer) {
      SpecialModelRenderer delegate2 = this.delegate;
      Consumer<org.joml.Vector3fc> consumer2 = vector3fc -> {
         consumer.accept(new Vector3f(vector3fc).add(0.0F, -0.0374F, 0.0F));
      };
      delegate2.getExtents(consumer2);
   }

   public RenderInfo extractArgument(ItemStack itemStack) {
      return this.delegate.extractArgument(itemStack);
   }

   public void submit(RenderInfo renderInfo, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int value, int value2, boolean condition, int value3) {
      poseStack.pushPose();
      poseStack.translate(0.0F, -0.0374F, 0.0F);
      this.delegate.submit(renderInfo, poseStack, submitNodeCollector, value, value2, condition, value3);
      poseStack.popPose();
   }
}
