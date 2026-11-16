package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.combat.RotationController;
import net.skycore.client.module.render.HandsModule;
import net.skycore.client.module.render.SwingAnimationsModule;
import net.skycore.client.module.render.ViewModelModule;
import net.skycore.client.util.CameraYawNearBobHelper;
import net.skycore.client.util.SpoofElytraAsChestplateHelper;
import net.skycore.client.util.SwingCountHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererHoldMyItemsFrameMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private float mainHandHeight;
   @Shadow
   private float oMainHandHeight;

   @Shadow
   protected abstract void renderPlayerArm(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int coordinate, float coordinate2, float coordinate3, HumanoidArm humanoidArm);

   @Shadow
   protected abstract void renderMap(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int coordinate, ItemStack itemStack);

   @Inject(method = "submitHandsWithItems", at = @At("HEAD"))
   private void skycore$holdMyItemsFrame(CallbackInfo callbackInfo) {
      if (SwingAnimationsModule.INSTANCE.isHoldMyItems()) {
         SwingCountHelper.beginFrame();
      }
   }

   @ModifyExpressionValue(
      method = "submitHandsWithItems",
      at = {
            @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderMainHand:Z"),
            @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer$HandRenderSelection;renderOffHand:Z")
      }
   )
   private boolean skycore$holdMyItemsBothHands(boolean condition) {
      return SwingAnimationsModule.INSTANCE.isHoldMyItems() || condition;
   }

   @Inject(method = "tick", at = @At("HEAD"))
   private void skycore$stabilizeVisualSwap(CallbackInfo callbackInfo) {
      if (SpoofElytraAsChestplateHelper.shouldSuppressEquipAnimation()) {
         LocalPlayer player = this.minecraft.player;
         if (player != null) {
            this.mainHandItem = SpoofElytraAsChestplateHelper.getVisualStack(player.getMainHandItem());
            this.mainHandHeight = 1.0F;
            this.oMainHandHeight = 1.0F;
         }
      }
   }

   @Redirect(
      method = "tick",
      at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
   )
   private ItemStack skycore$spoofMainHandInTick(LocalPlayer player) {
      return SpoofElytraAsChestplateHelper.getVisualStack(player.getMainHandItem());
   }

   @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
   private void skycore$instantReplaceDuringVisualHold(ItemStack itemStack, ItemStack itemStack2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (SpoofElytraAsChestplateHelper.shouldSuppressEquipAnimation()) {
         callbackInfoReturnable.setReturnValue(true);
      }
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot(F)F"), require = 0)
   private float skycore$useFreeLookPitchLocal(LocalPlayer player, float pitch) {
      return this.skycore$freeLookPitch(player, pitch);
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getXRot(F)F"), require = 0)
   private float skycore$useFreeLookPitchEntity(Entity entity, float pitch) {
      return entity instanceof LocalPlayer player ? this.skycore$freeLookPitch(player, pitch) : entity.getXRot(pitch);
   }

   private float skycore$freeLookPitch(LocalPlayer player, float value) {
      return RotationController.isActive() && !RotationController.isClientLook() ? RotationController.getFreePitch() : player.getXRot(value);
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewXRot(F)F"), require = 0)
   private float skycore$useCameraPitch(LocalPlayer player, float value) {
      return this.minecraft.gameRenderer.mainCamera().xRot();
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"), require = 0)
   private float skycore$useCameraPitchEntity(Entity entity, float value) {
      return this.minecraft.gameRenderer.mainCamera().xRot();
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getViewYRot(F)F"), require = 0)
   private float skycore$useCameraYaw(LocalPlayer player, float value) {
      float yaw = Mth.lerp(value, player.yBobO, player.yBob);
      return CameraYawNearBobHelper.cameraYawNearBob(this.minecraft.gameRenderer.mainCamera().yRot(), yaw);
   }

   @Redirect(method = "submitHandsWithItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"), require = 0)
   private float skycore$useCameraYawEntity(Entity entity, float value) {
      LocalPlayer player = entity instanceof LocalPlayer player2 ? player2 : this.minecraft.player;
      float yaw = this.minecraft.gameRenderer.mainCamera().yRot();
      if (player == null) {
         return yaw;
      }

      float yaw2 = Mth.lerp(value, player.yBobO, player.yBob);
      return CameraYawNearBobHelper.cameraYawNearBob(yaw, yaw2);
   }

   @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
   private void skycore$captureSwingProgress(
      AbstractClientPlayer player,
      float count,
      float count2,
      InteractionHand interactionHand,
      float count3,
      ItemStack itemStack,
      float count4,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int count5,
      CallbackInfo callbackInfo
   ) {
      SwingAnimationsModule.captureSwing(count3, count4);
      HandsModule.INSTANCE.observeSwing(interactionHand, count3);
      if (SwingAnimationsModule.INSTANCE.isHoldMyItems()) {
         SwingCountHelper.render(
            (ItemInHandRenderer)(Object)this,
            (poseStack2, submitNodeCollector2, value, humanoidArm) -> this.renderPlayerArm(poseStack2, submitNodeCollector2, value, 0.0F, 0.0F, humanoidArm),
            this::renderMap,
            player,
            count,
            count2,
            interactionHand,
            count3,
            itemStack,
            count4,
            poseStack,
            submitNodeCollector,
            count5
         );
         callbackInfo.cancel();
      }
   }

   @Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
   private void skycore$modifyEquipOffset(PoseStack poseStack, HumanoidArm humanoidArm, float progress, CallbackInfo callbackInfo) {
      SwingAnimationsModule iNSTANCE = SwingAnimationsModule.INSTANCE;
      if (iNSTANCE.shouldSkipEquipOffset(humanoidArm)) {
         ViewModelModule.INSTANCE.applyHandPosition(poseStack, humanoidArm);
         callbackInfo.cancel();
      } else {
         float value = iNSTANCE.modifyEquipProgress(humanoidArm, progress);
         if (value != progress) {
            int computedValue = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(computedValue * 0.56F, -0.52F + value * -0.6F, -0.72F);
            ViewModelModule.INSTANCE.applyHandPosition(poseStack, humanoidArm);
            callbackInfo.cancel();
         }
      }
   }

   @Inject(method = "applyItemArmTransform", at = @At("RETURN"))
   private void skycore$applyViewModel(PoseStack poseStack, HumanoidArm humanoidArm, float value, CallbackInfo callbackInfo) {
      ViewModelModule.INSTANCE.applyHandPosition(poseStack, humanoidArm);
   }

   @Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
   private void skycore$customSwingArm(float value, PoseStack poseStack, int value2, HumanoidArm humanoidArm, CallbackInfo callbackInfo) {
      SwingAnimationsModule iNSTANCE = SwingAnimationsModule.INSTANCE;
      float value3 = SwingAnimationsModule.capturedEquip();
      if (iNSTANCE.applyCustomSwing(poseStack, value, value3, humanoidArm)) {
         callbackInfo.cancel();
      }
   }

   @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
   private void skycore$customEatTransform(PoseStack poseStack, float value, HumanoidArm humanoidArm, ItemStack itemStack, Player player, CallbackInfo callbackInfo) {
      SwingAnimationsModule iNSTANCE = SwingAnimationsModule.INSTANCE;
      if (iNSTANCE.shouldApplyAnimation() && iNSTANCE.eatAnim.get() && player.isUsingItem()) {
         iNSTANCE.applyCustomEatTransform(poseStack, value, humanoidArm, itemStack);
         callbackInfo.cancel();
      }
   }
}
