package net.skycore.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererInvokeSetupRotationsAccessor {
   @Invoker("setupRotations")
   void skycore$invokeSetupRotations(LivingEntityRenderState livingEntityRenderState, PoseStack poseStack, float value, float value2);

   @Invoker("scale")
   void skycore$invokeScale(LivingEntityRenderState livingEntityRenderState, PoseStack poseStack);
}
