package deluxe.mixin;

import deluxe.client.module.render.AspectRatioModule;
import deluxe.client.render.BatchedBlurRenderer;
import deluxe.client.event.CameraFovEvent;
import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.util.DepthSnapshot;
import deluxe.client.event.EventBus;
import deluxe.client.render.HandOutlineRenderer;
import deluxe.client.render.JumpCircleRenderer;
import deluxe.client.module.render.MotionBlurModule;
import deluxe.client.module.render.NoRenderModule;
import deluxe.client.render.RainScreenRenderer;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.GuiEffectsState;
import deluxe.client.render.SaturationRenderer;
import ru.dreamix.fabricloader.VMBridge;
import deluxe.client.render.WetWorldRenderer;
import deluxe.client.render.WorldRainRenderer;
import deluxe.client.event.WorldRenderEvent;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
   @Inject(method = "getBasicProjectionMatrix(F)Lorg/joml/Matrix4f;", at = @At("TAIL"), cancellable = true)
   public void getProjectionMatrixHook(float local8087_1, CallbackInfoReturnable<Matrix4f> local8087_2) {
      if (EventBus.getInstance().hasListeners(CameraFovEvent.class)) {
         CameraFovEvent cameraFovEvent = new CameraFovEvent();
         EventBus.getInstance().post(cameraFovEvent);
         if (cameraFovEvent.method4421()) {
            Matrix4f matrix4f = new Matrix4f();
            matrix4f.perspective(local8087_1 * (float) (Math.PI / 180.0), cameraFovEvent.method3578(), 0.05F, ((GameRenderer)(Object)this).getFarPlaneDistance());
            local8087_2.setReturnValue(matrix4f);
         }
      }
   }

   @ModifyArg(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/ProjectionMatrix3;set(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"),
      index = 0
   )
   private int stretchHandProjection(int local8088_1) {
      return AspectRatioModule.method1041().method1040(local8088_1);
   }

   @Inject(method = "renderBlur()V", at = @At("HEAD"), cancellable = true)
   private void customStratumBlur(CallbackInfo callbackInfo) {
      if (GuiEffectsState.method3978()) {
         BatchedBlurRenderer.method4504().method4528(GuiEffectsState.method3979(), GuiEffectsState.method3977());
         BatchedBlurRenderer.method4504().method4485();
         BatchedBlurRenderer.method4504().method4520();
         callbackInfo.cancel();
      } else {
         if (GuiEffectsState.method3974()) {
            GuiEffectsState.method3976(false);
            BatchedBlurRenderer.method4504().method4485();
            BatchedBlurRenderer.method4504().method4520();
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "tiltViewWhenHurt(Lnet/minecraft/client/util/math/MatrixStack;F)V", at = @At("HEAD"), cancellable = true)
   private void removeHurtShake(MatrixStack matrixStack, float local8090_2, CallbackInfo callbackInfo) {
      if (NoRenderModule.method3426().method3427("Тряска экрана") || NoRenderModule.method3426().method3427("Камера при уроне")) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void smearFrameAfterHand(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      MotionBlurModule.method3838();
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void drawHandOutlineAfterHand(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      HandOutlineRenderer.method0466().method0462();
   }

   @Inject(method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("TAIL"))
   private void applyWorldSaturation(RenderTickCounter renderTickCounter, CallbackInfo callbackInfo) {
      SaturationRenderer.method2158().method2157();
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.BEFORE
      )
   )
   private void captureMatricesBeforeWorld(
      RenderTickCounter renderTickCounter, CallbackInfo callbackInfo, @Local(ordinal = 1) Matrix4f matrix4f, @Local(ordinal = 0) Matrix4f matrix4f2
   ) {
      WorldProjection.method1123(matrix4f, matrix4f2);
   }

   @Inject(
      method = "renderWorld(Lnet/minecraft/client/render/RenderTickCounter;)V",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/WorldRenderer;render(Lnet/minecraft/client/util/memory/ObjectAllocator;Lnet/minecraft/client/render/RenderTickCounter;ZLnet/minecraft/client/render/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V",
         shift = Shift.AFTER
      )
   )
   private void hookWorldRender(
      RenderTickCounter renderTickCounter, CallbackInfo callbackInfo, @Local(ordinal = 1) Matrix4f matrix4f, @Local(ordinal = 0) Matrix4f matrix4f2
   ) {
      WorldProjection.method1123(matrix4f, matrix4f2);
      DepthSnapshot.method4233();
      CustomWorldModule.method2204().method2183(matrix4f, matrix4f2);
      CustomWorldModule.method2204().method2213(matrix4f, matrix4f2);
      WorldRainRenderer.method1953().method1954(matrix4f, matrix4f2);
      WetWorldRenderer.method2297().method2301(matrix4f, matrix4f2);
      RainScreenRenderer.method4319().method4322(matrix4f, matrix4f2);
      JumpCircleRenderer.method4238().method4237(matrix4f, matrix4f2);
      if (EventBus.getInstance().hasListeners(WorldRenderEvent.class)) {
         MatrixStack matrixStack = new MatrixStack();
         matrixStack.multiplyPositionMatrix(matrix4f);
         EventBus.getInstance().post(new WorldRenderEvent(matrixStack, renderTickCounter.getTickProgress(false)));
      }
   }

   @Inject(method = "showFloatingItem(Lnet/minecraft/item/ItemStack;)V", at = @At("HEAD"), cancellable = true)
   private void hideTotemActivation(ItemStack itemStack, CallbackInfo callbackInfo) {
      if (NoRenderModule.method3426().method3427("Анимация тотема")) {
         callbackInfo.cancel();
      }
   }

   static {
      VMBridge.identifyClass(GameRendererMixin.class, "1JXtEvNw");
   }
}
