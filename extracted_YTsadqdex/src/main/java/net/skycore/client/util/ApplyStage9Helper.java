package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class ApplyStage9Helper {
   private static void applyStage9(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (renderContext.main) {
         frame.animationState.mainConsumingProgress = frame.animationState.mainConsumingProgress * frame.animationState.consumingAnimationWeight;
         frame.animationState.offConsumingProgress = frame.animationState.offConsumingProgress * frame.animationState.consumingAnimationWeight;
         frame.animationState.offUsePhase = frame.animationState.offUsePhase * frame.animationState.consumingAnimationWeight;
         frame.animationState.mainUsePhase = frame.animationState.mainUsePhase * frame.animationState.consumingAnimationWeight;
         frame.consumingBlend = frame.animationState.mainConsumingProgress * frame.animationState.mainConsumingProgress;
         if ((frame.itemKind.equals("eat") || frame.itemKind.equals("toot_horn")) && renderContext.main) {
            RotateXHelper.translateZ(frame.poseStack, 0.155 * frame.consumingBlend);
            RotateXHelper.translateX(frame.poseStack, 0.135 * frame.handSign * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, -0.27 * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.animationState.mainConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 30.0 * frame.consumingBlend);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.animationState.mainConsumingAnimationWeight);
            if (frame.itemKind.equals("eat")) {
               RotateXHelper.rotateX(frame.poseStack, 3.0 * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.mainUsePhase * 3.0))) * frame.consumingBlend);
               RotateXHelper.rotateY(frame.poseStack, 4.0 * frame.handSign * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.mainUsePhase * 2.0))) * frame.consumingBlend);
               RotateXHelper.rotateZ(frame.poseStack, 6.0 * frame.handSign * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.mainUsePhase * 2.0))) * frame.consumingBlend);
            } else {
               RotateXHelper.rotateX(frame.poseStack, 2.0 * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.mainUsePhase * 2.0)) * frame.consumingBlend);
               RotateXHelper.rotateY(frame.poseStack, 3.0 * frame.handSign * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.mainUsePhase)) * frame.consumingBlend);
               RotateXHelper.rotateZ(frame.poseStack, 5.0 * frame.handSign * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.mainUsePhase)) * frame.consumingBlend);
            }

            RotateXHelper.rotateYAround(frame.poseStack, 60.0 * frame.consumingBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 25.0 * RotateXHelper.sin(frame.consumingBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         }

         if ((frame.itemKind.equals("drink") || ChargedHelper.empty(renderContext.item) || ChargedHelper.matches(renderContext.item, Items.GLASS_BOTTLE)) && renderContext.main) {
            RotateXHelper.translateZ(frame.poseStack, 0.1 * frame.consumingBlend);
            RotateXHelper.translateX(frame.poseStack, 0.11 * frame.handSign * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, -0.5 * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.animationState.mainConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 50.0 * frame.consumingBlend);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.animationState.mainConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 2.0 * RotateXHelper.sin(frame.animationState.mainUsePhase * 6.0) * frame.animationState.mainConsumingAnimationWeight);
            RotateXHelper.rotateYAround(frame.poseStack, frame.handSign * 60.0 * frame.consumingBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 25.0 * RotateXHelper.sin(frame.consumingBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         }
      } else {
         frame.consumingBlend = frame.animationState.offConsumingProgress * frame.animationState.offConsumingProgress;
         if ((frame.itemKind.equals("eat") || frame.itemKind.equals("toot_horn")) && !renderContext.main) {
            RotateXHelper.translateZ(frame.poseStack, 0.155 * frame.consumingBlend);
            RotateXHelper.translateX(frame.poseStack, 0.135 * frame.handSign * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, -0.27 * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.animationState.offConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 30.0 * frame.consumingBlend);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.animationState.offConsumingAnimationWeight);
            if (frame.itemKind.equals("eat")) {
               RotateXHelper.rotateX(frame.poseStack, 3.0 * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.offUsePhase * 3.0))) * frame.consumingBlend);
               RotateXHelper.rotateY(frame.poseStack, 4.0 * frame.handSign * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.offUsePhase * 2.0))) * frame.consumingBlend);
               RotateXHelper.rotateZ(frame.poseStack, 6.0 * frame.handSign * ArmPoseHelper.easeInOutBack(RotateXHelper.abs(RotateXHelper.sin(frame.animationState.offUsePhase * 2.0))) * frame.consumingBlend);
            } else {
               RotateXHelper.rotateX(frame.poseStack, 2.0 * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.offUsePhase * 2.0)) * frame.consumingBlend);
               RotateXHelper.rotateY(frame.poseStack, 3.0 * frame.handSign * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.offUsePhase)) * frame.consumingBlend);
               RotateXHelper.rotateZ(frame.poseStack, 5.0 * frame.handSign * ArmPoseHelper.easeInOutSine(RotateXHelper.sin(frame.animationState.offUsePhase)) * frame.consumingBlend);
            }

            RotateXHelper.rotateYAround(frame.poseStack, 60.0 * frame.handSign * frame.consumingBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 25.0 * RotateXHelper.sin(frame.consumingBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         }

         if (frame.itemKind.equals("drink") && !renderContext.main) {
            RotateXHelper.translateZ(frame.poseStack, 0.1 * frame.consumingBlend);
            RotateXHelper.translateX(frame.poseStack, 0.11 * frame.handSign * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, -0.5 * frame.consumingBlend);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.animationState.offConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 50.0 * frame.consumingBlend);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.animationState.offConsumingAnimationWeight);
            RotateXHelper.rotateX(frame.poseStack, 2.0 * RotateXHelper.sin(frame.animationState.offUsePhase * 6.0) * frame.animationState.offConsumingAnimationWeight);
            RotateXHelper.rotateYAround(frame.poseStack, 60.0 * frame.handSign * frame.consumingBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 25.0 * RotateXHelper.sin(frame.consumingBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         }
      }

      frame.consumingBlend = ArmPoseHelper.easeInOutBack(frame.animationState.mainBrushProgress);
      frame.offBrushBlend = ArmPoseHelper.easeInOutBack(frame.animationState.offBrushProgress);
      if (frame.itemKind.equals("brush") && renderContext.main) {
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.consumingBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * frame.consumingBlend);
         RotateXHelper.translateY(frame.poseStack, -0.3 * frame.consumingBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * RotateXHelper.sin(frame.animationState.mainUsePhase * 4.14) * frame.consumingBlend);
         RotateXHelper.translateY(frame.poseStack, -0.3 * RotateXHelper.sin(frame.animationState.mainUsePhase * 4.14) * frame.consumingBlend);
         RotateXHelper.rotateX(frame.poseStack, 10.0 * RotateXHelper.sin(frame.consumingBlend * 3.14));
         RotateXHelper.rotateY(frame.poseStack, 20.0 * frame.handSign * frame.consumingBlend);
         RotateXHelper.rotateY(frame.poseStack, 10.0 * frame.handSign * RotateXHelper.sin(frame.animationState.mainUsePhase * 4.14) * frame.consumingBlend);
         RotateXHelper.rotateZ(frame.poseStack, 30.0 * frame.handSign * frame.consumingBlend);
         RotateXHelper.rotateZ(frame.poseStack, 30.0 * frame.handSign * RotateXHelper.sin(frame.animationState.mainUsePhase * 4.14) * frame.consumingBlend);
      }
   }

   private static void applyStage2(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (renderContext.offItemChanged) {
         frame.animationState.offEquipProgress = 0.0;
      }

      frame.animationState.offEquipProgress = frame.animationState.offEquipProgress + 0.015 * renderContext.delta * 30.0;
      frame.animationState.offEquipProgress = RotateXHelper.clamp(frame.animationState.offEquipProgress, 0.0, 1.0);
      if (!renderContext.main && frame.animationState.offConsumingProgress == 0.0) {
         frame.equipRotationBlend = RotateXHelper.sin(RotateXHelper.clamp(frame.animationState.offEquipProgress, 0.0, 0.5) * 3.14) * frame.animationState.offEquipAnimationWeight;
         frame.equipTranslationBlend = RotateXHelper.sin(RotateXHelper.clamp(frame.animationState.offEquipProgress, 0.0, 0.125) * 12.56) * frame.animationState.offEquipAnimationWeight;
         frame.equipRotationBlend = ArmPoseHelper.easeInOutBack(frame.equipRotationBlend);
         if (frame.itemKind.equals("crossbow")) {
            RotateXHelper.translateY(frame.poseStack, -0.25 * frame.equipTranslationBlend);
         }

         RotateXHelper.translate(frame.poseStack, 0.0 * frame.equipTranslationBlend, 0.0, -0.2 * frame.equipTranslationBlend);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.equipTranslationBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -55.0 * frame.equipTranslationBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 40.0 * frame.equipTranslationBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, -40.0 * frame.equipRotationBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 55.0 * frame.equipRotationBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, -25.0 * frame.equipRotationBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         if (frame.itemKind.equals("crossbow")) {
            RotateXHelper.translateY(frame.poseStack, 0.25 * frame.equipTranslationBlend);
         }

         RotateXHelper.translate(frame.poseStack, 0.0 * frame.equipRotationBlend, 0.0, 0.2 * frame.equipRotationBlend);
      }

      if (!VerticalSpeedHelper.onGround(frame.abstractClientPlayer) && renderContext.mainSwing == 0.0F && renderContext.main) {
         frame.animationState.groundedSwingReady = false;
      } else if (renderContext.main && renderContext.mainSwing == 0.0F) {
         frame.animationState.groundedSwingReady = true;
      }

      if (frame.animationState.previousMainSwingStarted != renderContext.mainSwingStarted && renderContext.main) {
         frame.alternateSwingSnapshot = frame.animationState.alternateSwing = ((!frame.animationState.alternateSwing ? 1 : 0) != 0);
      }

      if (VerticalSpeedHelper.crawling(frame.abstractClientPlayer) && frame.horizontalSpeed > 0.08) {
         frame.animationState.crawlingPhase = frame.animationState.crawlingPhase + frame.horizontalSpeed * renderContext.delta * 30.0;
      }

      frame.animationState.lookingDownProgress = VerticalSpeedHelper.pitch(frame.abstractClientPlayer) > 40.0
         ? (frame.animationState.lookingDownProgress = frame.animationState.lookingDownProgress + 0.05 * renderContext.delta * 30.0)
         : (frame.animationState.lookingDownProgress = frame.animationState.lookingDownProgress - 0.095 * renderContext.delta * 30.0);
      frame.animationState.lookingDownProgress = RotateXHelper.clamp(frame.animationState.lookingDownProgress, 0.0, 1.0);
      frame.equipRotationBlend = frame.animationState.mainSpearRotationProgress;
      frame.equipTranslationBlend = frame.animationState.mainSpearTranslationProgress;
      frame.previousOffSpearRotation = frame.animationState.offSpearRotationProgress;
      frame.previousOffSpearTranslation = frame.animationState.offSpearTranslationProgress;
      if (frame.usingItem && frame.itemKind.equals("spear") && frame.interactionHand == renderContext.hand && renderContext.main && ChargedHelper.spear(renderContext.item).enabled) {
         frame.spearFlags = ChargedHelper.spear(renderContext.item);
         frame.animationState.mainSpearUseProgress = frame.animationState.mainSpearUseProgress + 0.08 * renderContext.delta * 30.0;
         if (!frame.spearFlags.lockChargeTranslation) {
            frame.animationState.mainSpearTranslationProgress = frame.animationState.mainSpearTranslationProgress + 0.035 * renderContext.delta * 30.0;
         }

         if (!frame.spearFlags.lockChargeRotation) {
            frame.animationState.mainSpearRotationProgress = frame.animationState.mainSpearRotationProgress + 0.055 * renderContext.delta * 30.0;
         }

         if (frame.spearFlags.useChargedPose) {
            frame.animationState.mainSpearChargedPoseProgress = frame.animationState.mainSpearChargedPoseProgress + 0.08 * renderContext.delta * 30.0;
         }

         if (frame.animationState.mainSpearChargedPoseProgress > 0.0) {
            frame.animationState.mainSpearChargedPoseProgress = frame.animationState.mainSpearChargedPoseProgress + 0.08 * renderContext.delta * 30.0;
         }
      } else if (renderContext.main) {
         frame.animationState.mainSpearUseProgress = frame.animationState.mainSpearUseProgress - 0.08 * renderContext.delta * 30.0;
         frame.animationState.mainSpearTranslationProgress = 0.0;
         frame.animationState.mainSpearRotationProgress = 0.0;
         frame.animationState.spearReleaseProgress = 0.0;
         frame.animationState.mainSpearChargedPoseProgress = 0.0;
      }

      if (frame.animationState.mainSpearChargedPoseProgress >= 1.0) {
         frame.animationState.mainSpearChargedPoseProgress = 0.0;
      }

      frame.animationState.mainSpearUseProgress = RotateXHelper.clamp(frame.animationState.mainSpearUseProgress, 0.0, 1.0) * RotateXHelper.clamp(1.0 - RotateXHelper.clamp(frame.swingArc * 8.0, 0.0, 1.0), 0.0, 1.0);
      frame.animationState.mainSpearTranslationProgress = RotateXHelper.clamp(frame.animationState.mainSpearTranslationProgress, 0.0, 1.0);
      frame.animationState.mainSpearRotationProgress = RotateXHelper.clamp(frame.animationState.mainSpearRotationProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("spear") && frame.interactionHand == renderContext.hand && !renderContext.main && ChargedHelper.spear(renderContext.item).enabled) {
         frame.spearFlags = ChargedHelper.spear(renderContext.item);
         frame.animationState.offSpearUseProgress = frame.animationState.offSpearUseProgress + 0.08 * renderContext.delta * 30.0;
         if (!frame.spearFlags.lockChargeTranslation) {
            frame.animationState.offSpearTranslationProgress = frame.animationState.offSpearTranslationProgress + 0.035 * renderContext.delta * 30.0;
         }

         if (!frame.spearFlags.lockChargeRotation) {
            frame.animationState.offSpearRotationProgress = frame.animationState.offSpearRotationProgress + 0.055 * renderContext.delta * 30.0;
         }
      } else if (!renderContext.main) {
         frame.animationState.offSpearUseProgress = frame.animationState.offSpearUseProgress - 0.08 * renderContext.delta * 30.0;
         frame.animationState.offSpearTranslationProgress = 0.0;
         frame.animationState.offSpearRotationProgress = 0.0;
         frame.animationState.spearReleaseProgress = 0.0;
      }
   }

   private static void applyStage10(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (frame.itemKind.equals("brush") && !renderContext.main) {
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.offBrushBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * frame.offBrushBlend);
         RotateXHelper.translateY(frame.poseStack, -0.3 * frame.offBrushBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * RotateXHelper.sin(frame.animationState.offUsePhase * 4.14) * frame.offBrushBlend);
         RotateXHelper.translateY(frame.poseStack, -0.3 * RotateXHelper.sin(frame.animationState.offUsePhase * 4.14) * frame.offBrushBlend);
         RotateXHelper.rotateX(frame.poseStack, 10.0 * RotateXHelper.sin(frame.offBrushBlend * 3.14));
         RotateXHelper.rotateY(frame.poseStack, 20.0 * frame.handSign * frame.offBrushBlend);
         RotateXHelper.rotateY(frame.poseStack, 10.0 * frame.handSign * RotateXHelper.sin(frame.animationState.offUsePhase * 4.14) * frame.offBrushBlend);
         RotateXHelper.rotateZ(frame.poseStack, 30.0 * frame.handSign * frame.offBrushBlend);
         RotateXHelper.rotateZ(frame.poseStack, 30.0 * frame.handSign * RotateXHelper.sin(frame.animationState.offUsePhase * 4.14) * frame.offBrushBlend);
      }

      if (ChargedHelper.matches(renderContext.item, HeldItemTags.DOORS)) {
         RotateXHelper.translateX(frame.poseStack, 0.2 * frame.handSign);
         RotateXHelper.rotateXAround(frame.poseStack, 6.0, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 20.0 * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (VerticalSpeedHelper.cooldown(renderContext.item, frame.abstractClientPlayer) && frame.itemKind.equals("block")) {
         frame.animationState.shieldCooldownProgress = frame.animationState.shieldCooldownProgress + 0.04 * renderContext.delta * 30.0;
      } else if (frame.itemKind.equals("block")) {
         frame.animationState.shieldCooldownProgress = frame.animationState.shieldCooldownProgress - 0.06 * renderContext.delta * 30.0;
      }

      frame.animationState.shieldCooldownProgress = RotateXHelper.clamp(frame.animationState.shieldCooldownProgress, 0.0, 1.0);
      frame.shieldCooldownBlend = frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress;
      if (frame.itemKind.equals("block")) {
         RotateXHelper.translateZ(frame.poseStack, -0.4 * frame.shieldCooldownBlend);
         RotateXHelper.translateY(frame.poseStack, 0.15 * frame.shieldCooldownBlend);
         RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.shieldCooldownBlend);
         RotateXHelper.rotateX(frame.poseStack, -30.0 * frame.shieldCooldownBlend);
         RotateXHelper.rotateX(frame.poseStack, -10.0 * RotateXHelper.sin(frame.shieldCooldownBlend * 3.14));
         RotateXHelper.rotateY(frame.poseStack, -20.0 * frame.handSign * frame.shieldCooldownBlend);
         RotateXHelper.rotateZ(frame.poseStack, -6.0 * frame.handSign * frame.shieldCooldownBlend);
      }

      frame.animationState.previousMainSwingStarted = renderContext.mainSwingStarted;
      frame.consumingRollImpulse = RotateXHelper.sin(RotateXHelper.clamp(frame.animationState.mainConsumingProgress, 0.80041, 1.0) * 3.14 * 5.0) * 0.45;
      frame.animationState.consumingRollSpring = frame.animationState.consumingRollSpring + frame.consumingRollImpulse * renderContext.delta * 30.0;
      frame.animationState.consumingRollSpring = frame.animationState.consumingRollSpring * RotateXHelper.pow(0.8, renderContext.delta * 30.0F);
      if (ChargedHelper.empty(renderContext.item) && frame.animationState.mainConsumingAnimationWeight > 0.0) {
         RotateXHelper.rotateZ(frame.poseStack, -6.0 * frame.handSign);
         RotateXHelper.translateY(frame.poseStack, -0.35);
      }

      frame.twoHandedMapBlend = ArmPoseHelper.easeInOutBack(frame.animationState.twoHandedMapProgress);
      frame.lookingDownBlend = ArmPoseHelper.easeOutBack(frame.animationState.lookingDownProgress);
      if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP)) {
         RotateXHelper.translateX(frame.poseStack, (0.3 - 0.1 * frame.lookingDownBlend) * frame.handSign * frame.twoHandedMapBlend);
         RotateXHelper.translateY(frame.poseStack, 0.18 * frame.twoHandedMapBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.12 * frame.lookingDownBlend * frame.twoHandedMapBlend);
         RotateXHelper.rotateX(frame.poseStack, RotateXHelper.clamp(VerticalSpeedHelper.pitch(frame.abstractClientPlayer), 0.0, 50.0) * frame.twoHandedMapBlend);
         RotateXHelper.rotateX(frame.poseStack, -40.0 * frame.twoHandedMapBlend);
         RotateXHelper.rotateYAround(frame.poseStack, (40.0 + 30.0 * frame.lookingDownBlend) * frame.handSign * frame.twoHandedMapBlend, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP)) {
         frame.oneHandedMapBlend = 1.0 - frame.twoHandedMapBlend;
         RotateXHelper.translateX(frame.poseStack, 0.1 * frame.handSign * frame.oneHandedMapBlend);
         RotateXHelper.translateY(frame.poseStack, -0.35 * frame.oneHandedMapBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.22 * frame.oneHandedMapBlend);
         RotateXHelper.rotateX(frame.poseStack, 24.0 * frame.oneHandedMapBlend);
         RotateXHelper.rotateY(frame.poseStack, 10.0 * frame.handSign * frame.oneHandedMapBlend);
      }

      if (frame.itemKind.equals("crossbow")) {
         RotateXHelper.translateX(frame.poseStack, 0.1 * frame.handSign);
         RotateXHelper.translateZ(frame.poseStack, 0.2);
         RotateXHelper.rotateXAround(frame.poseStack, -5.0, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 20.0 * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
      }

      frame.inspectionKeySnapshot = frame.inspectionKeyCode = frame.animationState.inspectionKeyCode != 0 ? frame.animationState.inspectionKeyCode : 67;
      frame.animationState.inspectionKeyProgress = GameplayKeyState.isPressed(frame.inspectionKeyCode)
         ? (frame.animationState.inspectionKeyProgress = frame.animationState.inspectionKeyProgress + 0.04 * renderContext.delta * 30.0)
         : (frame.animationState.inspectionKeyProgress = frame.animationState.inspectionKeyProgress - 0.04 * renderContext.delta * 30.0);
      frame.animationState.inspectionKeyProgress = RotateXHelper.clamp(frame.animationState.inspectionKeyProgress, 0.0, 1.0);
      if ((
            ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
               || ChargedHelper.matches(renderContext.item, HeldItemTags.PICKAXES)
               || ChargedHelper.matches(renderContext.item, HeldItemTags.AXES)
               || frame.itemKind.equals("trident")
         )
         && renderContext.main) {
         RotateXHelper.translateX(frame.poseStack, 0.35 * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress));
         RotateXHelper.translateZ(frame.poseStack, -0.15 * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress));
         RotateXHelper.rotateYAround(frame.poseStack, 40.0 * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 13.0 * RotateXHelper.clamp(RotateXHelper.sin(frame.animationState.inspectionKeyProgress * 4.14), 0.0, 1.0), 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateX(frame.poseStack, 10.0 * RotateXHelper.sin(ArmPoseHelper.easeInOutBack(frame.animationState.inspectionProgress) * 6.28));
      }
   }

   private static void applyStage7(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (frame.usingItem && frame.interactionHand == renderContext.hand && frame.itemKind.equals("block")) {
         if (renderContext.main) {
            RotateXHelper.translateX(
               frame.poseStack,
               0.0
                  - 0.25
                     * (RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + 4.0 * RotateXHelper.sin(frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * 3.14))
                     * frame.handSign
                     * frame.animationState.mainBlockingProgress
                     * frame.animationState.blockingAnimationWeight
            );
            RotateXHelper.rotateZAround(
               frame.poseStack,
               10.0
                  * frame.handSign
                  * (RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + 4.0 * RotateXHelper.sin(frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * 3.14))
                  * frame.animationState.mainBlockingProgress
                  * frame.animationState.blockingAnimationWeight,
               0.3 * frame.handSign,
               -0.4,
               0.0
            );
         } else {
            RotateXHelper.translateX(
               frame.poseStack,
               0.0
                  - 0.25
                     * (RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + 4.0 * RotateXHelper.sin(frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * 3.14))
                     * frame.handSign
                     * frame.animationState.offBlockingProgress
                     * frame.animationState.blockingAnimationWeight
            );
            RotateXHelper.rotateZAround(
               frame.poseStack,
               10.0
                  * frame.handSign
                  * (RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + 4.0 * RotateXHelper.sin(frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * frame.animationState.shieldCooldownProgress * 3.14))
                  * frame.animationState.offBlockingProgress
                  * frame.animationState.blockingAnimationWeight,
               0.3 * frame.handSign,
               -0.4,
               0.0
            );
         }
      }

      if (frame.itemKind.equals("crossbow") && frame.animationState.mainCrossbowChargeProgress + frame.animationState.offCrossbowChargeProgress == 0.0) {
         RotateXHelper.translateZ(frame.poseStack, 0.0 + 0.25 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) * frame.animationState.crossbowAnimationWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 20.0 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) * frame.animationState.crossbowAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      } else if (frame.animationState.mainConsumingProgress == 0.0 && !frame.itemKind.equals("bow") && !frame.itemKind.equals("block") && frame.animationState.mainTridentUseProgress == 0.0 && frame.animationState.offTridentUseProgress == 0.0) {
         RotateXHelper.translateZ(frame.poseStack, 0.0 - 0.25 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip));
         RotateXHelper.rotateXAround(frame.poseStack, -20.0 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip), 0.3 * frame.handSign, -0.4, 0.0);
         if (ChargedHelper.matches(renderContext.item, Items.MACE)) {
            RotateXHelper.translateZ(frame.poseStack, 0.0 - 0.25 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip));
            RotateXHelper.rotateZ(frame.poseStack, 6.0 * frame.handSign * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip));
            RotateXHelper.rotateXAround(frame.poseStack, -10.0 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip), 0.3 * frame.handSign, -0.4, 0.0);
         }

         if (!ChargedHelper.isBlockItem(renderContext.item)) {
            RotateXHelper.rotateX(frame.poseStack, frame.animationState.mainPitchSpring * 0.35 * frame.swingArc);
         }
      }

      frame.pitchCompensation = 0.0;
      frame.pitchCompensation = VerticalSpeedHelper.pitch(frame.abstractClientPlayer) != 0.0 ? 90.0 / VerticalSpeedHelper.pitch(frame.abstractClientPlayer) / 2.5 : 1.0;
      if (frame.pitchCompensation > 1.0) {
         frame.pitchCompensation = 1.0;
      }

      if (frame.pitchCompensation < 0.0) {
         frame.pitchCompensation = 1.0;
      }

      frame.lanternMotionWeight = ChargedHelper.isLantern(renderContext.item) ? 0.2 : 1.0;
      RotateXHelper.translateZ(frame.poseStack, 0.2 * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      RotateXHelper.translateY(frame.poseStack, -0.2 * RotateXHelper.cos(frame.animationState.climbingPhase) * frame.handSign * frame.pitchCompensation * frame.animationState.climbingProgress * frame.lanternMotionWeight * frame.animationState.climbingAnimationWeight);
      RotateXHelper.rotateX(frame.poseStack, -30.0 * frame.handSign * RotateXHelper.sin(frame.animationState.climbingPhase) * frame.pitchCompensation * frame.animationState.climbingProgress * frame.lanternMotionWeight * frame.animationState.climbingAnimationWeight);
      RotateXHelper.rotateX(frame.poseStack, VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      RotateXHelper.translateZ(frame.poseStack, 0.01 * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      RotateXHelper.translateY(frame.poseStack, 0.003 * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      RotateXHelper.translateX(frame.poseStack, -0.0025 * frame.handSign * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      if (!ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateX(frame.poseStack, -0.05 * frame.handSign * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * frame.animationState.climbingProgress * frame.animationState.climbingAnimationWeight);
      }

      RotateXHelper.translateZ(frame.poseStack, 0.2 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.handSign * RotateXHelper.sin(frame.animationState.crawlingPhase) * frame.animationState.crawlingProgress * frame.pitchCompensation * frame.lanternMotionWeight * frame.animationState.crawlMovementProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.rotateY(frame.poseStack, 10.0 * RotateXHelper.sin(frame.animationState.crawlingPhase) * frame.animationState.crawlingProgress * frame.lanternMotionWeight * frame.animationState.crawlMovementProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.rotateX(
         frame.poseStack, RotateXHelper.clamp(20.0 * frame.handSign * RotateXHelper.cos(frame.animationState.crawlingPhase) * frame.animationState.crawlingProgress * frame.lanternMotionWeight * frame.animationState.crawlMovementProgress, 0.0, 20.0) * frame.animationState.crawlingAnimationWeight
      );
      if (ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateY(frame.poseStack, 0.3 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.55 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
         RotateXHelper.rotateX(frame.poseStack, -45.0 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
         RotateXHelper.rotateZAround(
            frame.poseStack,
            RotateXHelper.clamp(16.0 * RotateXHelper.sin(frame.animationState.crawlingPhase) * frame.animationState.crawlingProgress * frame.lanternMotionWeight * frame.animationState.crawlMovementProgress, 0.0, 20.0) * frame.animationState.crawlingAnimationWeight,
            0.3,
            -0.4,
            0.0
         );
      }

      RotateXHelper.rotateX(frame.poseStack, VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.rotateX(frame.poseStack, -7.0 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.translateZ(frame.poseStack, 0.01 * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      if (ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateZ(frame.poseStack, 0.005 * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      }

      RotateXHelper.translateY(frame.poseStack, 0.003 * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      RotateXHelper.translateX(frame.poseStack, -0.0025 * frame.handSign * VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      if (!ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * frame.animationState.crawlingProgress * frame.animationState.crawlingAnimationWeight);
      }

      frame.mainBowDrawBlend = ArmPoseHelper.easeInOutBack(frame.animationState.mainBowDrawProgress) * frame.animationState.bowAnimationWeight;
      frame.offBowDrawBlend = ArmPoseHelper.easeInOutBack(frame.animationState.offBowDrawProgress) * frame.animationState.bowAnimationWeight;
      if (frame.animationState.mainBowDrawProgress > 0.0) {
         frame.crossbowChargeSnapshot = frame.animationState.chargedCrossbowProgress = frame.mainBowDrawBlend == 1.0 ? -0.1 : 1.0 - frame.mainBowDrawBlend;
      }

      if (frame.itemKind.equals("bow") && renderContext.main) {
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign);
         RotateXHelper.translateZ(frame.poseStack, -0.085);
         RotateXHelper.translateX(frame.poseStack, 0.1 * frame.handSign * frame.mainBowDrawBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.085 * frame.mainBowDrawBlend);
         RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.mainBowDrawBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * frame.mainBowDrawBlend);
         RotateXHelper.translateY(frame.poseStack, 0.15 * frame.mainBowDrawBlend);
         RotateXHelper.rotateXAround(frame.poseStack, 5.0 * RotateXHelper.sin(frame.mainBowDrawBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 0.0 * frame.handSign * frame.mainBowDrawBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.translateZ(frame.poseStack, 0.0015 * RotateXHelper.sin(frame.animationState.mainBowShakePhase * 15.14) * frame.animationState.mainBowHoldProgress);
      }
   }

   private static void applyStage1(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      frame.animationState = HeldItemAnimationState.INSTANCE;
      frame.abstractClientPlayer = renderContext.player;
      frame.itemKind = ChargedHelper.kind(renderContext.item);
      frame.usingItem = VerticalSpeedHelper.using(frame.abstractClientPlayer);
      frame.interactionHand = VerticalSpeedHelper.usedHand(frame.abstractClientPlayer);
      frame.poseStack = renderContext.pose;
      frame.horizontalSpeed = VerticalSpeedHelper.speed(frame.abstractClientPlayer);
      if (frame.itemKind.equals("spear")) {
         renderContext.equip = 0.0F;
      }

      frame.handSign = renderContext.right ? 1.0 : -1.0;
      renderContext.swing = ChargedHelper.matches(renderContext.item, HeldItemTags.PICKAXES) ? (float)SwingEasing.easePickaxeSwing(renderContext.swing) : (float)SwingEasing.easeSwing(renderContext.swing);
      RotateXHelper.translateX(frame.poseStack, 0.2 * frame.handSign);
      frame.swingAcceleration = renderContext.swing < 0.70016
         ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.0, 0.308) * 5.1)
         : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.70016, 1.0) * 5.1 - 2.0);
      frame.swingTranslation = renderContext.swing < 0.65245
         ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.0, 0.16675) * 3.14 * 3.0)
         : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.65245, 1.0) * 4.4 - 1.2584);
      frame.swingAcceleration = frame.swingAcceleration * frame.swingAcceleration * frame.swingAcceleration;
      frame.swingArc = RotateXHelper.clamp(RotateXHelper.sin(renderContext.swing * 4.78), 0.0, 1.0);
      frame.swingYaw = RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.16561, 0.49422) * 4.78 * 2.0 + 4.7);
      frame.swingRoll = renderContext.swing < 0.65594
         ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.16561, 0.32991) * 4.78 * 2.0 + 4.7)
         : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.65594, 0.82025) * 4.78 * 2.0 - 4.7);
      if (frame.usingItem && frame.itemKind.equals("bow") && !renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.offBowDrawProgress = frame.animationState.offBowDrawProgress + 0.075 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offBowDrawProgress = frame.animationState.offBowDrawProgress - 0.07 * renderContext.delta * 30.0;
      }

      frame.animationState.offBowDrawProgress = RotateXHelper.clamp(frame.animationState.offBowDrawProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("bow") && !renderContext.main && frame.interactionHand == renderContext.hand && frame.animationState.offBowDrawProgress == 1.0) {
         frame.animationState.offBowHoldProgress = frame.animationState.offBowHoldProgress + 0.025 * renderContext.delta * 30.0;
         frame.animationState.offBowShakePhase = frame.animationState.offBowShakePhase + 0.07 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offBowHoldProgress = frame.animationState.offBowHoldProgress - 0.11 * renderContext.delta * 30.0;
      }

      frame.animationState.offBowHoldProgress = RotateXHelper.clamp(frame.animationState.offBowHoldProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("bow") && renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.mainBowDrawProgress = frame.animationState.mainBowDrawProgress + 0.075 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainBowDrawProgress = frame.animationState.mainBowDrawProgress - 0.07 * renderContext.delta * 30.0;
      }

      frame.animationState.mainBowDrawProgress = RotateXHelper.clamp(frame.animationState.mainBowDrawProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("bow") && renderContext.main && frame.interactionHand == renderContext.hand && frame.animationState.mainBowDrawProgress == 1.0) {
         frame.animationState.mainBowHoldProgress = frame.animationState.mainBowHoldProgress + 0.025 * renderContext.delta * 30.0;
         frame.animationState.mainBowShakePhase = frame.animationState.mainBowShakePhase + 0.07 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainBowHoldProgress = 0.0;
      }

      frame.animationState.mainBowHoldProgress = RotateXHelper.clamp(frame.animationState.mainBowHoldProgress, 0.0, 1.0);
      frame.pitchSpring = renderContext.main ? frame.animationState.mainPitchSpring : frame.animationState.offPitchSpring;
      frame.yawSpring = frame.yawSpringSnapshot = renderContext.main ? frame.animationState.mainYawSpring : frame.animationState.offYawSpring;
      if (frame.horizontalSpeed > 0.05) {
         frame.animationState.walkingPhase = frame.animationState.walkingPhase + frame.horizontalSpeed * 2.0 * renderContext.delta * 30.0;
      }

      frame.animationState.waterProgress = VerticalSpeedHelper.water(frame.abstractClientPlayer)
         ? (frame.animationState.waterProgress = frame.animationState.waterProgress + 0.07 * renderContext.delta * 30.0)
         : (frame.animationState.waterProgress = frame.animationState.waterProgress - 0.07 * renderContext.delta * 30.0);
      frame.animationState.waterProgress = RotateXHelper.clamp(frame.animationState.waterProgress, 0.0, 1.0);
      frame.idleOffsetX = frame.animationState.idleOffsetX;
      RotateXHelper.translate(frame.poseStack, frame.animationState.idleOffsetX * frame.handSign, frame.animationState.idleOffsetY, frame.animationState.idleOffsetZ);
      frame.animationState.swingPhase = frame.animationState.swingPhase + renderContext.swing * renderContext.delta * 3.0F;
      if (!ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateZ(frame.poseStack, -0.16);
      } else {
         RotateXHelper.translateZ(frame.poseStack, -0.08);
      }

      if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP) && renderContext.main && ChargedHelper.empty(VerticalSpeedHelper.offhand(frame.abstractClientPlayer))) {
         frame.animationState.twoHandedMapProgress = frame.animationState.twoHandedMapProgress + 0.07 * renderContext.delta * 30.0;
      } else if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP)) {
         frame.animationState.twoHandedMapProgress = frame.animationState.twoHandedMapProgress - 0.07 * renderContext.delta * 30.0;
      }

      frame.animationState.twoHandedMapProgress = RotateXHelper.clamp(frame.animationState.twoHandedMapProgress, 0.0, 1.0);
      if (renderContext.mainItemChanged && renderContext.main && frame.animationState.mainConsumingAnimationWeight == 0.0) {
         frame.animationState.mainEquipProgress = 0.0;
      }

      frame.animationState.mainEquipProgress = frame.animationState.mainEquipProgress + 0.015 * renderContext.delta * 30.0;
      frame.animationState.mainEquipProgress = RotateXHelper.clamp(frame.animationState.mainEquipProgress, 0.0, 1.0);
      if (renderContext.main) {
         frame.equipRotationBlend = RotateXHelper.sin(RotateXHelper.clamp(frame.animationState.mainEquipProgress, 0.0, 0.5) * 3.14) * frame.animationState.mainEquipAnimationWeight;
         frame.equipTranslationBlend = RotateXHelper.sin(RotateXHelper.clamp(frame.animationState.mainEquipProgress, 0.0, 0.125) * 12.56) * frame.animationState.mainEquipAnimationWeight;
         frame.equipRotationBlend = ArmPoseHelper.easeInOutBack(frame.equipRotationBlend);
         if (frame.itemKind.equals("trident") && frame.animationState.mainTridentUseProgress > 0.9) {
            RotateXHelper.translate(frame.poseStack, 0.0, -0.15 * frame.equipTranslationBlend, -0.3 * frame.equipTranslationBlend);
            RotateXHelper.rotateXAround(frame.poseStack, 75.0 * frame.equipTranslationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -75.0 * frame.equipRotationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.translate(frame.poseStack, 0.0, 0.15 * frame.equipTranslationBlend, 0.3 * frame.equipTranslationBlend);
         } else {
            if (frame.itemKind.equals("crossbow")) {
               RotateXHelper.translateY(frame.poseStack, -0.25 * frame.equipTranslationBlend);
            }

            RotateXHelper.translate(frame.poseStack, 0.0 * frame.equipTranslationBlend, 0.0, -0.2 * frame.equipTranslationBlend);
            RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.handSign * frame.equipTranslationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -55.0 * frame.equipTranslationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZAround(frame.poseStack, 40.0 * frame.handSign * frame.equipTranslationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZAround(frame.poseStack, -40.0 * frame.handSign * frame.equipRotationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 55.0 * frame.equipRotationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateYAround(frame.poseStack, -25.0 * frame.handSign * frame.equipRotationBlend, 0.3 * frame.handSign, -0.4, 0.0);
            if (frame.itemKind.equals("crossbow")) {
               RotateXHelper.translateY(frame.poseStack, 0.25 * frame.equipRotationBlend);
            }

            RotateXHelper.translate(frame.poseStack, 0.0 * frame.equipRotationBlend, 0.0, 0.2 * frame.equipRotationBlend);
         }
      }
   }

   private static void applyStage3(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      frame.animationState.offSpearUseProgress = RotateXHelper.clamp(frame.animationState.offSpearUseProgress, 0.0, 1.0) * RotateXHelper.clamp(1.0 - RotateXHelper.clamp(frame.swingArc * 8.0, 0.0, 1.0), 0.0, 1.0);
      frame.animationState.offSpearTranslationProgress = RotateXHelper.clamp(frame.animationState.offSpearTranslationProgress, 0.0, 1.0);
      frame.animationState.offSpearRotationProgress = RotateXHelper.clamp(frame.animationState.offSpearRotationProgress, 0.0, 1.0);
      if (frame.animationState.mainSpearTranslationProgress == 0.0) {
         frame.animationState.mainSpearTranslationProgress = RotateXHelper.lerp(0.5 * renderContext.delta * 30.0, frame.equipTranslationBlend, frame.animationState.mainSpearTranslationProgress);
      }

      if (frame.animationState.mainSpearRotationProgress == 0.0) {
         frame.animationState.mainSpearRotationProgress = RotateXHelper.lerp(0.5 * renderContext.delta * 30.0, frame.equipRotationBlend, frame.animationState.mainSpearRotationProgress);
      }

      if (frame.animationState.offSpearTranslationProgress == 0.0) {
         frame.animationState.offSpearTranslationProgress = RotateXHelper.lerp(0.5 * renderContext.delta * 30.0, frame.previousOffSpearTranslation, frame.animationState.offSpearTranslationProgress);
      }

      if (frame.animationState.offSpearRotationProgress == 0.0) {
         frame.animationState.offSpearRotationProgress = RotateXHelper.lerp(0.5 * renderContext.delta * 30.0, frame.previousOffSpearRotation, frame.animationState.offSpearRotationProgress);
      }

      if (frame.usingItem
         && (frame.itemKind.equals("eat") || frame.itemKind.equals("drink") || frame.itemKind.equals("toot_horn"))
         && renderContext.main
         && frame.interactionHand == renderContext.hand) {
         frame.animationState.mainConsumingProgress = frame.animationState.mainConsumingProgress + 0.1 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainConsumingProgress = frame.animationState.mainConsumingProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainConsumingProgress = RotateXHelper.clamp(frame.animationState.mainConsumingProgress, 0.0, 1.0);
      if (frame.usingItem
         && (frame.itemKind.equals("eat") || frame.itemKind.equals("drink") || frame.itemKind.equals("toot_horn") || frame.itemKind.equals("brush"))
         && renderContext.main
         && frame.interactionHand == renderContext.hand
         && (frame.animationState.mainConsumingProgress == 1.0 || frame.animationState.mainBrushProgress > 0.4)) {
         frame.animationState.mainUsePhase = frame.animationState.mainUsePhase + 0.1 * renderContext.delta * 30.0;
      }

      if (frame.usingItem
         && (frame.itemKind.equals("eat") || frame.itemKind.equals("drink") || frame.itemKind.equals("toot_horn"))
         && !renderContext.main
         && frame.interactionHand == renderContext.hand) {
         frame.animationState.offConsumingProgress = frame.animationState.offConsumingProgress + 0.1 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offConsumingProgress = frame.animationState.offConsumingProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offConsumingProgress = RotateXHelper.clamp(frame.animationState.offConsumingProgress, 0.0, 1.0);
      if (frame.usingItem
         && (frame.itemKind.equals("eat") || frame.itemKind.equals("drink") || frame.itemKind.equals("toot_horn") || frame.itemKind.equals("brush"))
         && !renderContext.main
         && frame.interactionHand == renderContext.hand
         && (frame.animationState.offConsumingProgress == 1.0 || frame.animationState.offBrushProgress > 0.4)) {
         frame.animationState.offUsePhase = frame.animationState.offUsePhase + 0.1 * renderContext.delta * 30.0;
      }

      if (frame.usingItem && frame.itemKind.equals("drink") && renderContext.main && frame.interactionHand == renderContext.hand && frame.animationState.mainConsumingProgress == 1.0) {
         frame.animationState.mainConsumingAnimationWeight = frame.animationState.mainConsumingAnimationWeight + 0.04 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainConsumingAnimationWeight = frame.animationState.mainConsumingAnimationWeight - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainConsumingAnimationWeight = RotateXHelper.clamp(frame.animationState.mainConsumingAnimationWeight, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("drink") && !renderContext.main && frame.interactionHand == renderContext.hand && frame.animationState.offConsumingProgress == 1.0) {
         frame.animationState.offConsumingAnimationWeight = frame.animationState.offConsumingAnimationWeight + 0.04 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offConsumingAnimationWeight = frame.animationState.offConsumingAnimationWeight - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offConsumingAnimationWeight = RotateXHelper.clamp(frame.animationState.offConsumingAnimationWeight, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("crossbow") && !renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.offCrossbowChargeProgress = frame.animationState.offCrossbowChargeProgress + 0.1 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offCrossbowChargeProgress = frame.animationState.offCrossbowChargeProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offCrossbowChargeProgress = RotateXHelper.clamp(frame.animationState.offCrossbowChargeProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("crossbow") && !renderContext.main && frame.interactionHand == renderContext.hand && frame.animationState.offCrossbowChargeProgress == 1.0) {
         frame.animationState.offCrossbowHoldProgress = frame.animationState.offCrossbowHoldProgress + 0.02 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offCrossbowHoldProgress = frame.animationState.offCrossbowHoldProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offCrossbowHoldProgress = RotateXHelper.clamp(frame.animationState.offCrossbowHoldProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("crossbow") && renderContext.main && frame.interactionHand == renderContext.hand && !ChargedHelper.charged(renderContext.item)) {
         frame.animationState.mainCrossbowChargeProgress = frame.animationState.mainCrossbowChargeProgress + 0.1 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainCrossbowChargeProgress = frame.animationState.mainCrossbowChargeProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainCrossbowChargeProgress = RotateXHelper.clamp(frame.animationState.mainCrossbowChargeProgress, 0.0, 1.0);
      if (frame.usingItem
         && frame.itemKind.equals("crossbow")
         && renderContext.main
         && frame.interactionHand == renderContext.hand
         && frame.animationState.mainCrossbowChargeProgress == 1.0
         && !ChargedHelper.charged(renderContext.item)) {
         frame.animationState.mainCrossbowHoldProgress = frame.animationState.mainCrossbowHoldProgress + 0.02 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainCrossbowHoldProgress = frame.animationState.mainCrossbowHoldProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainCrossbowHoldProgress = RotateXHelper.clamp(frame.animationState.mainCrossbowHoldProgress, 0.0, 1.0);
      if (!renderContext.main
         && ChargedHelper.empty(renderContext.item)
         && (
            !frame.usingItem
               || ChargedHelper.charged(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer))
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("block")
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("eat")
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("toot_horn")
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("drink")
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("brush")
               || ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("spear")
         )
         && !VerticalSpeedHelper.climbing(frame.abstractClientPlayer)
         && !VerticalSpeedHelper.swimming(frame.abstractClientPlayer)
         && !VerticalSpeedHelper.crawling(frame.abstractClientPlayer)) {
         frame.animationState.chargedCrossbowProgress = frame.animationState.chargedCrossbowProgress + 0.08 * renderContext.delta * 30.0;
      } else if (!renderContext.main
         || frame.usingItem
            && !ChargedHelper.charged(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer))
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("block")
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("eat")
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("drink")
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("toot_horn")
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("brush")
            && !ChargedHelper.kind(VerticalSpeedHelper.mainhand(frame.abstractClientPlayer)).equals("spear")
         || VerticalSpeedHelper.climbing(frame.abstractClientPlayer)
         || VerticalSpeedHelper.crawling(frame.abstractClientPlayer)) {
         frame.animationState.chargedCrossbowProgress = frame.animationState.chargedCrossbowProgress - 0.08 * renderContext.delta * 30.0;
      }

      frame.animationState.chargedCrossbowProgress = RotateXHelper.clamp(frame.animationState.chargedCrossbowProgress, 0.0, 1.0);
   }

   private static void applyStage4(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (frame.usingItem && frame.itemKind.equals("brush") && renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.mainBrushProgress = frame.animationState.mainBrushProgress + 0.1 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         frame.animationState.mainBrushProgress = frame.animationState.mainBrushProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainBrushProgress = RotateXHelper.clamp(frame.animationState.mainBrushProgress, 0.0, 1.0) * frame.animationState.brushAnimationWeight;
      if (frame.usingItem && frame.itemKind.equals("brush") && !renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.offBrushProgress = frame.animationState.offBrushProgress + 0.1 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offBrushProgress = frame.animationState.offBrushProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offBrushProgress = RotateXHelper.clamp(frame.animationState.offBrushProgress, 0.0, 1.0) * frame.animationState.brushAnimationWeight;
      if (frame.usingItem && frame.itemKind.equals("trident") && !renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.offTridentUseProgress = frame.animationState.offTridentUseProgress + 0.07 * renderContext.delta * 30.0;
         frame.animationState.offTridentChargeProgress = frame.animationState.offTridentChargeProgress + 0.045 * renderContext.delta * 30.0;
         frame.animationState.offTridentUsePhase = frame.animationState.offTridentUsePhase + 0.1 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         if (VerticalSpeedHelper.spinAttack(frame.abstractClientPlayer)) {
            frame.animationState.offTridentUseProgress = frame.animationState.offTridentUseProgress - 0.57 * renderContext.delta * 30.0;
            frame.animationState.offTridentChargeProgress = frame.animationState.offTridentChargeProgress - 0.53 * renderContext.delta * 30.0;
         } else {
            frame.animationState.offTridentUseProgress = frame.animationState.offTridentUseProgress - 0.1 * renderContext.delta * 30.0;
            frame.animationState.offTridentChargeProgress = frame.animationState.offTridentChargeProgress - 0.07 * renderContext.delta * 30.0;
         }

         frame.animationState.offTridentUsePhase = frame.animationState.offTridentUsePhase - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.offTridentUseProgress = RotateXHelper.clamp(frame.animationState.offTridentUseProgress, 0.0, 1.0);
      frame.animationState.offTridentChargeProgress = RotateXHelper.clamp(frame.animationState.offTridentChargeProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("trident") && renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.mainTridentUseProgress = frame.animationState.mainTridentUseProgress + 0.07 * renderContext.delta * 30.0;
         frame.animationState.mainTridentChargeProgress = frame.animationState.mainTridentChargeProgress + 0.05 * renderContext.delta * 30.0;
         frame.animationState.mainTridentUsePhase = frame.animationState.mainTridentUsePhase + 0.1 * renderContext.delta * 30.0;
      } else if (renderContext.main) {
         if (VerticalSpeedHelper.spinAttack(frame.abstractClientPlayer)) {
            frame.animationState.mainTridentUseProgress = frame.animationState.mainTridentUseProgress - 0.57 * renderContext.delta * 30.0;
            frame.animationState.mainTridentChargeProgress = frame.animationState.mainTridentChargeProgress - 0.53 * renderContext.delta * 30.0;
         } else {
            frame.animationState.mainTridentUseProgress = frame.animationState.mainTridentUseProgress - 0.1 * renderContext.delta * 30.0;
            frame.animationState.mainTridentChargeProgress = frame.animationState.mainTridentChargeProgress - 0.07 * renderContext.delta * 30.0;
         }

         frame.animationState.mainTridentUsePhase = frame.animationState.mainTridentUsePhase - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.mainTridentChargeProgress = frame.animationState.mainTridentChargeProgress * RotateXHelper.pow(0.95, renderContext.delta * 30.0F);
      frame.animationState.mainTridentUseProgress = RotateXHelper.clamp(frame.animationState.mainTridentUseProgress, 0.0, 1.0);
      frame.animationState.mainTridentChargeProgress = RotateXHelper.clamp(frame.animationState.mainTridentChargeProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("block") && renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.mainBlockingProgress = ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
            ? (frame.animationState.mainBlockingProgress = frame.animationState.mainBlockingProgress + 0.12 * renderContext.delta * 30.0)
            : (frame.animationState.mainBlockingProgress = frame.animationState.mainBlockingProgress + 0.07 * renderContext.delta * 30.0);
      } else if (renderContext.main) {
         frame.animationState.mainBlockingProgress = ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
            ? (frame.animationState.mainBlockingProgress = frame.animationState.mainBlockingProgress - 0.12 * renderContext.delta * 30.0)
            : (frame.animationState.mainBlockingProgress = frame.animationState.mainBlockingProgress - 0.07 * renderContext.delta * 30.0);
      }

      frame.animationState.mainBlockingProgress = frame.animationState.mainBlockingProgress - frame.animationState.shieldCooldownProgress * 0.04 * renderContext.delta * 30.0;
      frame.animationState.mainBlockingProgress = RotateXHelper.clamp(frame.animationState.mainBlockingProgress, 0.0, 1.0);
      if (frame.usingItem && frame.itemKind.equals("block") && !renderContext.main && frame.interactionHand == renderContext.hand) {
         frame.animationState.offBlockingProgress = frame.animationState.offBlockingProgress + 0.07 * renderContext.delta * 30.0;
      } else if (!renderContext.main) {
         frame.animationState.offBlockingProgress = frame.animationState.offBlockingProgress - 0.07 * renderContext.delta * 30.0;
      }

      frame.animationState.offBlockingProgress = frame.animationState.offBlockingProgress - frame.animationState.shieldCooldownProgress * 0.04 * renderContext.delta * 30.0;
      frame.animationState.offBlockingProgress = RotateXHelper.clamp(frame.animationState.offBlockingProgress, 0.0, 1.0);
      if (frame.horizontalSpeed > 0.08
         && RotateXHelper.abs(VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer)) < 0.08
         && VerticalSpeedHelper.onGround(frame.abstractClientPlayer)) {
         frame.animationState.walkingBobPhase = frame.animationState.walkingBobPhase + frame.horizontalSpeed * renderContext.delta * 30.0;
         frame.animationState.walkingAnimationProgress = frame.animationState.walkingAnimationProgress + 0.1 * renderContext.delta * 30.0;
      } else {
         frame.animationState.walkingAnimationProgress = frame.animationState.walkingAnimationProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.walkingAnimationProgress = RotateXHelper.clamp(frame.animationState.walkingAnimationProgress, 0.0, 1.0);
      frame.animationState.verticalBobbingVelocity = frame.animationState.verticalBobbingVelocity
         + (
               -1.0 * VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer)
                  + RotateXHelper.sin(frame.animationState.sneakingProgress * 3.14) * 0.14
                  + RotateXHelper.sin(frame.animationState.mainBowDrawProgress * 3.14) * 0.12
                  + RotateXHelper.sin(frame.animationState.offBowDrawProgress * 3.14) * 0.12
            )
            * 0.27
            * renderContext.delta
            * 30.0;
      frame.animationState.verticalBobbingVelocity = frame.animationState.verticalBobbingVelocity - 0.1 * frame.animationState.verticalBobbingSpring * renderContext.delta * 30.0;
      frame.animationState.verticalBobbingVelocity = frame.animationState.verticalBobbingVelocity * RotateXHelper.pow(0.82, renderContext.delta * 30.0F);
      frame.animationState.verticalBobbingSpring = frame.animationState.verticalBobbingSpring + frame.animationState.verticalBobbingVelocity * renderContext.delta * 30.0;
      frame.animationState.sneakingProgress = VerticalSpeedHelper.sneaking(frame.abstractClientPlayer)
         ? (frame.animationState.sneakingProgress = frame.animationState.sneakingProgress + 0.1 * renderContext.delta * 30.0)
         : (frame.animationState.sneakingProgress = frame.animationState.sneakingProgress - 0.1 * renderContext.delta * 30.0);
      frame.animationState.sneakingProgress = RotateXHelper.clamp(frame.animationState.sneakingProgress, 0.0, 1.0);
      RotateXHelper.translateY(frame.poseStack, -0.08 * frame.animationState.sneakingProgress);
      RotateXHelper.rotateXAround(frame.poseStack, 4.0 * RotateXHelper.sin(frame.animationState.sneakingProgress * 3.14), 0.0, -0.4, 0.0);
      frame.animationState.idlePhase = frame.animationState.idlePhase + 0.04 * renderContext.delta * 30.0;
   }

   private static void applyStage11(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (ChargedHelper.matches(renderContext.item, Items.MACE) && renderContext.main) {
         RotateXHelper.translateX(frame.poseStack, 0.35 * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress));
         RotateXHelper.translateZ(frame.poseStack, -0.15 * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress));
         RotateXHelper.rotateYAround(frame.poseStack, 40.0 * ArmPoseHelper.easeInOutBack(frame.animationState.inspectionKeyProgress) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 17.0 * RotateXHelper.clamp(RotateXHelper.sin(frame.animationState.inspectionKeyProgress * 3.14), 0.0, 1.0), 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (renderContext.main) {
         frame.animationState.mainCrossbowCharged = ChargedHelper.charged(renderContext.item);
      } else {
         frame.animationState.offCrossbowCharged = ChargedHelper.charged(renderContext.item);
      }

      if (VerticalSpeedHelper.spinAttack(frame.abstractClientPlayer) && frame.itemKind.equals("trident") && frame.interactionHand == renderContext.hand) {
         if (renderContext.main) {
            frame.animationState.mainRiptideProgress = frame.animationState.mainRiptideProgress + 0.08 * renderContext.delta * 30.0;
         } else {
            frame.animationState.offRiptideProgress = frame.animationState.offRiptideProgress + 0.08 * renderContext.delta * 30.0;
         }
      } else if (renderContext.main) {
         frame.animationState.mainRiptideProgress = frame.animationState.mainRiptideProgress - 0.025 * renderContext.delta * 30.0;
      } else {
         frame.animationState.offRiptideProgress = frame.animationState.offRiptideProgress - 0.025 * renderContext.delta * 30.0;
      }

      frame.animationState.mainRiptideProgress = RotateXHelper.clamp(frame.animationState.mainRiptideProgress, 0.0, 1.0);
      frame.animationState.offRiptideProgress = RotateXHelper.clamp(frame.animationState.offRiptideProgress, 0.0, 1.0);
      frame.animationState.mainRiptideProgress = frame.animationState.mainRiptideProgress
         * RotateXHelper.pow(0.95, renderContext.delta * 30.0F)
         * (1.0 - RotateXHelper.clamp(renderContext.swing * 4.0F, 0.0, 1.0))
         * (1.0 - RotateXHelper.clamp(frame.animationState.mainTridentUseProgress * 6.0, 0.0, 1.0));
      frame.animationState.offRiptideProgress = frame.animationState.offRiptideProgress
         * RotateXHelper.pow(0.95, renderContext.delta * 30.0F)
         * (1.0 - RotateXHelper.clamp(renderContext.swing * 4.0F, 0.0, 1.0))
         * RotateXHelper.clamp(1.0 - frame.animationState.offTridentUseProgress * 6.0, 0.0, 1.0);
      if (frame.itemKind.equals("trident")) {
         frame.riptidePoseBlend = renderContext.main ? ArmPoseHelper.easeOutBack(frame.animationState.mainRiptideProgress) : ArmPoseHelper.easeOutBack(frame.animationState.offRiptideProgress);
         frame.riptideAttackBlend = renderContext.main ? RotateXHelper.clamp(frame.animationState.mainRiptideProgress * 3.0, 0.0, 1.0) : RotateXHelper.clamp(frame.animationState.offRiptideProgress * 3.0, 0.0, 1.0);
         RotateXHelper.translateZ(frame.poseStack, 0.2 * frame.riptidePoseBlend * frame.riptideAttackBlend);
         RotateXHelper.translateX(frame.poseStack, -1.25 * frame.handSign * frame.riptidePoseBlend * frame.riptideAttackBlend);
         RotateXHelper.translateY(frame.poseStack, 0.0 * frame.riptidePoseBlend * frame.riptideAttackBlend);
         RotateXHelper.rotateY(frame.poseStack, 5.0 * frame.handSign * frame.riptidePoseBlend * frame.riptideAttackBlend);
         RotateXHelper.rotateX(frame.poseStack, -30.0 * ArmPoseHelper.easeOutBack(RotateXHelper.sin(renderContext.main ? frame.animationState.mainRiptideProgress : frame.animationState.offRiptideProgress * 3.14)) * frame.riptideAttackBlend);
         RotateXHelper.rotateZAround(
            frame.poseStack,
            70.0 * ArmPoseHelper.easeOutBack(RotateXHelper.sin(renderContext.main ? frame.animationState.mainRiptideProgress : frame.animationState.offRiptideProgress * 3.14)) * frame.riptideAttackBlend * frame.handSign,
            0.7 * frame.handSign,
            0.7,
            0.0
         );
      }

      frame.riptidePoseBlend = renderContext.main ? frame.animationState.mainSpearUseProgress : frame.animationState.offSpearUseProgress;
      frame.riptideAttackBlend = renderContext.main ? frame.animationState.mainSpearTranslationProgress : frame.animationState.offSpearTranslationProgress;
      frame.spearRotationBlend = renderContext.main ? frame.animationState.mainSpearRotationProgress : frame.animationState.offSpearRotationProgress;
      frame.equipProgress = renderContext.main ? frame.animationState.mainEquipProgress : frame.animationState.offEquipProgress;
      frame.chargedPoseSnapshot = frame.spearChargedPoseBlend = renderContext.main ? ArmPoseHelper.easeInOutSine(frame.animationState.mainSpearChargedPoseProgress) : frame.animationState.offSpearChargedPoseProgress;
      if (frame.itemKind.equals("spear")) {
         RotateXHelper.translateZ(frame.poseStack, -0.25);
         RotateXHelper.translateY(frame.poseStack, 0.1);
         RotateXHelper.rotateX(frame.poseStack, -20.0);
         RotateXHelper.translateZ(frame.poseStack, 0.75 * RotateXHelper.sin(ArmPoseHelper.easeInOutSine(frame.spearChargedPoseBlend) * 3.14));
         RotateXHelper.translateZ(frame.poseStack, -0.25 * ArmPoseHelper.easeInOutBack(frame.riptidePoseBlend));
         RotateXHelper.translateZ(frame.poseStack, 0.25 * ArmPoseHelper.easeOutBack(frame.spearRotationBlend) * frame.spearRotationBlend);
         RotateXHelper.rotateY(frame.poseStack, 8.0 * ArmPoseHelper.easeInOutBack(frame.riptidePoseBlend) * frame.handSign);
         RotateXHelper.rotateXAround(frame.poseStack, 40.0 * RotateXHelper.sin(frame.riptidePoseBlend * 3.14), 0.0, -0.2, -0.35);
         RotateXHelper.rotateXAround(frame.poseStack, -8.0 * ArmPoseHelper.easeInOutBack(frame.riptideAttackBlend), 0.5 * frame.handSign, -0.5, -0.35);
         RotateXHelper.rotateYAround(frame.poseStack, -1.5 * RotateXHelper.sin(frame.animationState.idlePhase * 1.5) * ArmPoseHelper.easeInOutBack(frame.riptideAttackBlend) * frame.handSign, 0.5 * frame.handSign, -0.5, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -1.5 * RotateXHelper.sin(frame.animationState.idlePhase * 3.0) * ArmPoseHelper.easeInOutBack(frame.riptideAttackBlend), 0.5 * frame.handSign, -0.5, 0.0);
         RotateXHelper.rotateX(frame.poseStack, 5.0 * RotateXHelper.sin(frame.spearChargedPoseBlend * 3.14));
         RotateXHelper.rotateZAround(frame.poseStack, -30.0 * ArmPoseHelper.easeOutBack(frame.spearRotationBlend) * frame.spearRotationBlend * frame.handSign, 0.5 * frame.handSign, -0.5, -0.35);
         RotateXHelper.rotateZAround(
            frame.poseStack,
            -8.0
               * RotateXHelper.sin(RotateXHelper.clamp(frame.equipProgress * 2.0, 0.0, 1.0) * 6.28)
               * RotateXHelper.sin(RotateXHelper.clamp(frame.equipProgress * 2.0, 0.0, 1.0) * 6.28)
               * frame.handSign,
            0.5 * frame.handSign,
            -0.5,
            0.0
         );
         RotateXHelper.rotateXAround(
            frame.poseStack,
            20.0 * RotateXHelper.sin(RotateXHelper.clamp(frame.equipProgress * 2.0, 0.0, 1.0) * 3.14) * RotateXHelper.sin(RotateXHelper.clamp(frame.equipProgress * 2.0, 0.0, 1.0) * 3.14),
            0.5 * frame.handSign,
            -0.5,
            0.0
         );
      }
   }

   private static void applyStage8(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (frame.itemKind.equals("bow") && !renderContext.main) {
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign);
         RotateXHelper.translateZ(frame.poseStack, -0.085);
         RotateXHelper.translateX(frame.poseStack, 0.1 * frame.handSign * frame.offBowDrawBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.085 * frame.offBowDrawBlend);
         RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.offBowDrawBlend);
         RotateXHelper.translateX(frame.poseStack, -0.2 * frame.handSign * frame.offBowDrawBlend);
         RotateXHelper.translateY(frame.poseStack, 0.15 * frame.offBowDrawBlend);
         RotateXHelper.rotateXAround(frame.poseStack, 5.0 * RotateXHelper.sin(frame.offBowDrawBlend * 3.14), 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 0.0 * frame.handSign * frame.offBowDrawBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.translateZ(frame.poseStack, 0.0015 * RotateXHelper.sin(frame.animationState.offBowShakePhase * 15.14) * frame.animationState.offBowHoldProgress);
      }

      if (!renderContext.main && frame.usingItem || !renderContext.main && ChargedHelper.empty(renderContext.item)) {
         frame.bowHoldBlend = ArmPoseHelper.easeOutBack(frame.animationState.mainBowHoldProgress) * frame.animationState.bowAnimationWeight;
         RotateXHelper.translateX(frame.poseStack, (-frame.idleOffsetX - frame.idleOffsetX / 1.5) * frame.handSign * frame.mainBowDrawBlend);
         RotateXHelper.translateX(frame.poseStack, 0.27 * frame.handSign * frame.mainBowDrawBlend);
         if (!ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, RotateXHelper.sin(frame.mainBowDrawBlend * 1.56 + 3.14));
         }

         RotateXHelper.translateZ(frame.poseStack, -0.65 * frame.mainBowDrawBlend);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * RotateXHelper.sin(frame.mainBowDrawBlend * 3.14) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 70.0 * frame.mainBowDrawBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.bowHoldBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateY(frame.poseStack, 0.25 * frame.handSign * RotateXHelper.sin(frame.animationState.mainBowShakePhase * 15.14) * frame.bowHoldBlend);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.mainBowDrawBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.translateY(frame.poseStack, -0.5 * RotateXHelper.sin(frame.mainBowDrawBlend * 3.14));
      }

      if (renderContext.main && frame.usingItem || renderContext.main && ChargedHelper.empty(renderContext.item)) {
         frame.bowHoldBlend = ArmPoseHelper.easeOutBack(frame.animationState.offBowHoldProgress);
         RotateXHelper.translateX(frame.poseStack, (-frame.idleOffsetX - frame.idleOffsetX / 1.5) * frame.handSign * frame.offBowDrawBlend);
         RotateXHelper.translateX(frame.poseStack, 0.27 * frame.handSign * frame.offBowDrawBlend);
         if (!ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, RotateXHelper.sin(frame.offBowDrawBlend * 1.56 + 3.14));
         }

         RotateXHelper.translateZ(frame.poseStack, -0.65 * frame.offBowDrawBlend);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * RotateXHelper.sin(frame.offBowDrawBlend * 3.14) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 70.0 * frame.offBowDrawBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.bowHoldBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateY(frame.poseStack, 0.25 * frame.handSign * RotateXHelper.sin(frame.animationState.offBowShakePhase * 15.14) * frame.bowHoldBlend);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.offBowDrawBlend * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.translateY(frame.poseStack, -0.5 * RotateXHelper.sin(frame.offBowDrawBlend * 3.14));
      }

      frame.bowHoldBlend = ArmPoseHelper.easeOutBack(frame.animationState.mainCrossbowChargeProgress) * frame.animationState.crossbowAnimationWeight;
      frame.mainCrossbowHoldBlend = ArmPoseHelper.easeOutBack(frame.animationState.mainCrossbowHoldProgress) * frame.animationState.crossbowAnimationWeight;
      frame.offCrossbowChargeBlend = ArmPoseHelper.easeOutBack(frame.animationState.offCrossbowChargeProgress) * frame.animationState.crossbowAnimationWeight;
      frame.offCrossbowHoldBlend = ArmPoseHelper.easeOutBack(frame.animationState.offCrossbowHoldProgress) * frame.animationState.crossbowAnimationWeight;
      if (frame.itemKind.equals("crossbow") && renderContext.main) {
         RotateXHelper.translateY(frame.poseStack, -0.15 * frame.bowHoldBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.3 * frame.bowHoldBlend);
         RotateXHelper.rotateZAround(frame.poseStack, 20.0 * frame.handSign * frame.bowHoldBlend, -0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign * frame.bowHoldBlend, -0.3 * frame.handSign, -0.4, 0.0);
      }

      if (!renderContext.main && frame.usingItem || !renderContext.main && ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateX(frame.poseStack, (-frame.idleOffsetX - frame.idleOffsetX / 1.5) * frame.handSign * frame.bowHoldBlend);
         RotateXHelper.translateX(frame.poseStack, 0.25 * frame.handSign * frame.bowHoldBlend);
         RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.bowHoldBlend);
         RotateXHelper.translateY(frame.poseStack, 0.55 * frame.bowHoldBlend);
         if (!ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, RotateXHelper.sin(frame.bowHoldBlend * 1.56 + 3.14));
         }

         RotateXHelper.rotateZAround(frame.poseStack, 15.0 * frame.handSign * frame.bowHoldBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 80.0 * frame.handSign * frame.bowHoldBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign * frame.mainCrossbowHoldBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -7.0 * frame.mainCrossbowHoldBlend, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (frame.itemKind.equals("crossbow") && !renderContext.main) {
         RotateXHelper.translateY(frame.poseStack, -0.15 * frame.offCrossbowChargeBlend);
         RotateXHelper.translateZ(frame.poseStack, 0.3 * frame.offCrossbowChargeBlend);
         RotateXHelper.rotateZAround(frame.poseStack, 20.0 * frame.handSign * frame.offCrossbowChargeBlend, -0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign * frame.offCrossbowChargeBlend, -0.3 * frame.handSign, -0.4, 0.0);
      }

      if (renderContext.main && frame.usingItem || renderContext.main && ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateX(frame.poseStack, (-frame.idleOffsetX - frame.idleOffsetX / 1.5) * frame.handSign * frame.offCrossbowChargeBlend);
         RotateXHelper.translateX(frame.poseStack, 0.25 * frame.handSign * frame.offCrossbowChargeBlend);
         RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.offCrossbowChargeBlend);
         RotateXHelper.translateY(frame.poseStack, 0.55 * frame.offCrossbowChargeBlend);
         if (!ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, RotateXHelper.sin(frame.offCrossbowChargeBlend * 1.56 + 3.14));
         }

         RotateXHelper.rotateZAround(frame.poseStack, 15.0 * frame.handSign * frame.offCrossbowChargeBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 80.0 * frame.handSign * frame.offCrossbowChargeBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign * frame.offCrossbowHoldBlend, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -7.0 * frame.offCrossbowChargeBlend, 0.3 * frame.handSign, -0.4, 0.0);
      }
   }

   public static void apply(HeldItemRenderContext renderContext) {
      if (renderContext.player != null) {
         ApplyStage9Helper.PoseFrame frame = new ApplyStage9Helper.PoseFrame();
         applyStage1(renderContext, frame);
         applyStage2(renderContext, frame);
         applyStage3(renderContext, frame);
         applyStage4(renderContext, frame);
         applyStage5(renderContext, frame);
         applyStage6(renderContext, frame);
         applyStage7(renderContext, frame);
         applyStage8(renderContext, frame);
         applyStage9(renderContext, frame);
         applyStage10(renderContext, frame);
         applyStage11(renderContext, frame);
      }
   }

   private static void applyStage5(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      frame.animationState.climbingProgress = VerticalSpeedHelper.climbing(frame.abstractClientPlayer)
         ? (frame.animationState.climbingProgress = frame.animationState.climbingProgress + 0.1 * renderContext.delta * 30.0)
         : (frame.animationState.climbingProgress = frame.animationState.climbingProgress - 0.1 * renderContext.delta * 30.0);
      if (frame.animationState.climbingProgress > 1.0) {
         frame.animationState.climbingProgress = 1.0;
      }

      if (frame.animationState.climbingProgress < 0.0) {
         frame.animationState.climbingProgress = 0.0;
      }

      frame.animationState.crawlingProgress = VerticalSpeedHelper.crawling(frame.abstractClientPlayer)
         ? (frame.animationState.crawlingProgress = frame.animationState.crawlingProgress + 0.1 * renderContext.delta * 30.0)
         : (frame.animationState.crawlingProgress = frame.animationState.crawlingProgress - 0.1 * renderContext.delta * 30.0);
      frame.animationState.crawlingProgress = RotateXHelper.clamp(frame.animationState.crawlingProgress, 0.0, 1.0);
      frame.animationState.crawlMovementProgress = VerticalSpeedHelper.crawling(frame.abstractClientPlayer) && frame.horizontalSpeed > 0.08
         ? (frame.animationState.crawlMovementProgress = frame.animationState.crawlMovementProgress + 0.1 * renderContext.delta * 30.0)
         : (frame.animationState.crawlMovementProgress = frame.animationState.crawlMovementProgress - 0.06 * renderContext.delta * 30.0);
      frame.animationState.crawlMovementProgress = RotateXHelper.clamp(frame.animationState.crawlMovementProgress, 0.0, 1.0);
      if (VerticalSpeedHelper.climbing(frame.abstractClientPlayer) && RotateXHelper.abs(VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer)) > 0.08) {
         frame.animationState.climbingPhase = VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer) > 0.0
            ? (frame.animationState.climbingPhase = frame.animationState.climbingPhase + VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer) * renderContext.delta * 30.0)
            : (frame.animationState.climbingPhase = frame.animationState.climbingPhase + VerticalSpeedHelper.verticalSpeed(frame.abstractClientPlayer) / 2.0 * renderContext.delta * 30.0);
      }

      if (VerticalSpeedHelper.swimming(frame.abstractClientPlayer) && !frame.usingItem) {
         frame.animationState.swimmingPhase = frame.animationState.swimmingPhase + frame.horizontalSpeed * renderContext.delta * 30.0;
         frame.animationState.swimmingProgress = frame.animationState.swimmingProgress + 0.1 * renderContext.delta * 30.0;
      } else {
         frame.animationState.swimmingProgress = frame.animationState.swimmingProgress - 0.1 * renderContext.delta * 30.0;
      }

      frame.animationState.swimmingProgress = RotateXHelper.clamp(frame.animationState.swimmingProgress, 0.0, 1.0);
      RotateXHelper.translateZ(frame.poseStack, 0.3 * RotateXHelper.sin(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight);
      if (!ChargedHelper.matches(renderContext.item, HeldItemTags.AXES) && !ChargedHelper.matches(renderContext.item, Items.MACE)) {
         RotateXHelper.rotateXAround(frame.poseStack, VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * -0.03 + frame.pitchSpring * 0.1, 0.0, -0.4, 0.0);
      } else {
         RotateXHelper.rotateXAround(frame.poseStack, VerticalSpeedHelper.pitch(frame.abstractClientPlayer) * -0.03 + frame.pitchSpring * 0.1, 0.0, -0.4, 0.0);
      }

      if (ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translate(frame.poseStack, 0.0, -0.15 * -RotateXHelper.cos(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress + 0.25 * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, -15.0 * frame.handSign * RotateXHelper.cos(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -10.0 * RotateXHelper.cos(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -30.0 * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, -20.0 * frame.handSign * RotateXHelper.sin(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      } else {
         RotateXHelper.rotateYAround(frame.poseStack, -15.0 * frame.handSign * RotateXHelper.cos(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -10.0 * RotateXHelper.cos(frame.animationState.swimmingPhase * 0.55) * frame.animationState.swimmingProgress * frame.animationState.swimmingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (!ChargedHelper.matches(renderContext.item, Items.BELL)
         && !ChargedHelper.isLantern(renderContext.item)
         && !ChargedHelper.matches(renderContext.item, HeldItemTags.HANGING_SIGNS)
         && !ChargedHelper.matches(renderContext.item, Items.PINK_PETALS)
         && !ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
         && !ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS)
         && !ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)
         && !ChargedHelper.matches(renderContext.item, Items.PAINTING)
         && !ChargedHelper.matches(renderContext.item, Items.ITEM_FRAME)) {
         if (!ChargedHelper.empty(renderContext.item) && !frame.itemKind.equals("crossbow")) {
            RotateXHelper.translateY(frame.poseStack, -0.12);
            RotateXHelper.rotateZ(frame.poseStack, -6.0 * frame.handSign);
            RotateXHelper.rotateX(frame.poseStack, 6.0);
         }
      } else if (ChargedHelper.matches(renderContext.item, Items.PINK_PETALS)
         || ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
         || ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS)) {
         RotateXHelper.translate(frame.poseStack, 0.0, 0.25, -0.05);
      } else if (ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)) {
         RotateXHelper.translateZ(frame.poseStack, -0.12);
         RotateXHelper.rotateX(frame.poseStack, -10.0);
      } else {
         RotateXHelper.translate(frame.poseStack, 0.0, -0.1, 0.05);
         RotateXHelper.rotateX(frame.poseStack, 25.0);
      }

      RotateXHelper.translateY(frame.poseStack, 0.01 * RotateXHelper.sin(frame.animationState.idlePhase));
      RotateXHelper.rotateXAround(frame.poseStack, 1.1 * frame.handSign * RotateXHelper.cos(frame.animationState.idlePhase), 0.3 * frame.handSign, -0.4, 0.0);
      RotateXHelper.rotateYAround(frame.poseStack, 0.5 * frame.handSign * RotateXHelper.sin(frame.animationState.idlePhase) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
      RotateXHelper.rotateZAround(frame.poseStack, 2.0 * frame.handSign * RotateXHelper.sin(frame.animationState.idlePhase * 0.3) * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
      if (ChargedHelper.empty(renderContext.item) && !renderContext.main) {
         RotateXHelper.translate(frame.poseStack, 0.0, -1.0 * ArmPoseHelper.easeInOutExpo(frame.animationState.chargedCrossbowProgress), 0.5 * ArmPoseHelper.easeInOutExpo(frame.animationState.chargedCrossbowProgress));
      }

      frame.itemBobWeight = !ChargedHelper.empty(renderContext.item) && !ChargedHelper.isBlockItem(renderContext.item) ? 1.0 : 0.7;
      if (ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translateZ(frame.poseStack, 0.06 * (frame.animationState.verticalBobbingSpring * frame.itemBobWeight));
      }

      RotateXHelper.rotateXAround(frame.poseStack, 2.0 * (frame.animationState.verticalBobbingSpring * frame.itemBobWeight), 0.0, -0.4, 0.0);
      RotateXHelper.translateY(frame.poseStack, 0.06 * frame.animationState.verticalBobbingSpring * frame.itemBobWeight);
      frame.handWalkingPhase = renderContext.right ? frame.animationState.walkingBobPhase : frame.animationState.walkingBobPhase - 0.75;
      RotateXHelper.rotateXAround(frame.poseStack, 1.5 * RotateXHelper.sin(frame.handWalkingPhase) * frame.animationState.walkingAnimationProgress, 0.0, -0.4, 0.0);
      RotateXHelper.rotateYAround(frame.poseStack, -0.5 * RotateXHelper.cos(frame.animationState.walkingBobPhase * 1.5) * frame.animationState.walkingAnimationProgress * frame.handSign, 0.0, -0.4, 0.0);
      RotateXHelper.rotateZAround(frame.poseStack, 1.0 * RotateXHelper.cos(frame.animationState.walkingBobPhase * 1.5) * frame.animationState.walkingAnimationProgress * frame.handSign, 0.0, -0.4, 0.0);
      if (frame.itemKind.equals("block") && !ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
         RotateXHelper.translate(frame.poseStack, 0.2 * frame.handSign, 0.0, 0.1);
         RotateXHelper.rotateYAround(frame.poseStack, 20.0 * frame.handSign, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (frame.itemKind.equals("block") && renderContext.main) {
         RotateXHelper.translate(
            frame.poseStack, -frame.idleOffsetX * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.mainBlockingProgress) + 0.1 * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.mainBlockingProgress) * frame.animationState.blockingAnimationWeight, 0.0, 0.0
         );
         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
            RotateXHelper.rotateYAround(frame.poseStack, 50.0 * ArmPoseHelper.easeInOutBack(frame.animationState.mainBlockingProgress) * frame.handSign * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         } else {
            RotateXHelper.rotateYAround(frame.poseStack, 70.0 * ArmPoseHelper.easeInOutBack(frame.animationState.mainBlockingProgress) * frame.handSign * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         }

         RotateXHelper.rotateXAround(frame.poseStack, 13.0 * RotateXHelper.clamp(RotateXHelper.sin(frame.animationState.mainBlockingProgress * 4.14), 0.0, 1.0) * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -15.0 * ArmPoseHelper.easeInOutBack(frame.animationState.mainBlockingProgress) * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }
   }

   private static void applyStage6(HeldItemRenderContext renderContext, ApplyStage9Helper.PoseFrame frame) {
      if (frame.itemKind.equals("block") && !renderContext.main) {
         RotateXHelper.translate(
            frame.poseStack, -frame.idleOffsetX * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.offBlockingProgress) + 0.1 * frame.handSign * ArmPoseHelper.easeInOutBack(frame.animationState.offBlockingProgress) * frame.animationState.blockingAnimationWeight, 0.0, 0.0
         );
         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
            RotateXHelper.rotateYAround(frame.poseStack, 50.0 * ArmPoseHelper.easeInOutBack(frame.animationState.offBlockingProgress) * frame.handSign * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         } else {
            RotateXHelper.rotateYAround(frame.poseStack, 70.0 * ArmPoseHelper.easeInOutBack(frame.animationState.offBlockingProgress) * frame.handSign * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         }

         RotateXHelper.rotateXAround(frame.poseStack, 13.0 * RotateXHelper.clamp(RotateXHelper.sin(frame.animationState.offBlockingProgress * 4.14), 0.0, 1.0) * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -15.0 * ArmPoseHelper.easeInOutBack(frame.animationState.offBlockingProgress) * frame.animationState.blockingAnimationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      frame.mainTridentBlend = ArmPoseHelper.easeOutBack(frame.animationState.mainTridentUseProgress);
      if (frame.itemKind.equals("trident") && renderContext.main) {
         RotateXHelper.translate(frame.poseStack, 0.0, -0.15 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight, -0.3 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 65.0 * frame.mainTridentBlend, 0.3 * frame.handSign * frame.animationState.pickaxeSwingWeight, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 55.0 * frame.animationState.mainTridentChargeProgress * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 10.0 * frame.handSign * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 0.3 * RotateXHelper.sin(frame.animationState.mainTridentUsePhase * frame.mainTridentBlend * 9.14) * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (!renderContext.main) {
         if (ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, 0.6 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight);
         } else {
            RotateXHelper.translateY(frame.poseStack, 0.2 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight);
         }

         RotateXHelper.translate(frame.poseStack, 0.25 * frame.handSign * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight, 0.0, -0.15 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.handSign * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -30.0 * frame.mainTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      frame.offTridentBlend = ArmPoseHelper.easeOutBack(frame.animationState.offTridentUseProgress);
      if (frame.itemKind.equals("trident") && !renderContext.main) {
         RotateXHelper.translate(frame.poseStack, 0.0, -0.15 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, -0.3 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 65.0 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 10.0 * frame.handSign * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 0.3 * RotateXHelper.sin(frame.animationState.offTridentUsePhase * frame.offTridentBlend * 9.14) * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      if (renderContext.main) {
         if (ChargedHelper.empty(renderContext.item)) {
            RotateXHelper.translateY(frame.poseStack, 0.6 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight);
         } else {
            RotateXHelper.translateY(frame.poseStack, 0.2 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight);
         }

         RotateXHelper.translate(frame.poseStack, 0.25 * frame.handSign * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, 0.0, -0.15 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight);
         RotateXHelper.rotateYAround(frame.poseStack, 25.0 * frame.handSign * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -30.0 * frame.offTridentBlend * frame.animationState.pickaxeSwingWeight, 0.3 * frame.handSign, -0.4, 0.0);
      }

      frame.swingAcceleration = frame.swingAcceleration * renderContext.attackStrength;
      frame.swingTranslation = frame.swingTranslation * renderContext.attackStrength;
      frame.swingArc = frame.swingArc * renderContext.attackStrength;
      frame.swingYaw = frame.swingYaw * renderContext.attackStrength;
      frame.swingRoll = frame.swingRoll * renderContext.attackStrength;
      frame.swingHalfWave = RotateXHelper.sin(renderContext.swing * 3.14) * renderContext.attackStrength;
      frame.positiveSwingWave = RotateXHelper.clamp(RotateXHelper.sin(renderContext.swing * 6.28), 0.0, 1.0) * renderContext.attackStrength;
      frame.swingFullWave = RotateXHelper.sin(renderContext.swing * 6.28) * renderContext.attackStrength;
      if (ChargedHelper.empty(renderContext.item)) {
         RotateXHelper.translate(
            frame.poseStack,
            -0.15 * frame.handSign * frame.swingArc * frame.animationState.swingArcWeight,
            0.1 * frame.swingFullWave + 0.33 * frame.swingArc + 0.05 * frame.swingAcceleration + 0.14 * frame.positiveSwingWave * frame.animationState.swingArcWeight,
            -0.1 * frame.swingFullWave - 0.4 * frame.swingYaw - 0.2 * frame.swingArc * frame.animationState.swingArcWeight
         );
         RotateXHelper.rotateX(frame.poseStack, -10.0 * frame.positiveSwingWave * frame.animationState.swingArcWeight);
         RotateXHelper.translateZ(frame.poseStack, 0.15 * frame.swingAcceleration * frame.animationState.swingArcWeight);
         RotateXHelper.rotateXAround(frame.poseStack, -20.0 * frame.swingArc * frame.animationState.swingArcWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -7.0 * frame.swingYaw, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.swingAcceleration, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 20.0 * frame.handSign * frame.swingArc, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 5.0 * frame.handSign * frame.swingArc, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -5.0 * frame.swingFullWave, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 10.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 5.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 15.0 * frame.handSign * frame.swingYaw, 0.3 * frame.handSign * frame.animationState.swingArcWeight, -0.4, 0.0);
      } else if (ChargedHelper.matches(renderContext.item, HeldItemTags.PICKAXES)) {
         RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.swingTranslation * frame.animationState.swingRotationWeight);
         RotateXHelper.translateZ(frame.poseStack, 0.0 * frame.swingYaw * frame.animationState.swingRotationWeight);
         RotateXHelper.translateZ(frame.poseStack, 0.0 * frame.swingRoll * frame.animationState.swingRotationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.0 * frame.swingYaw * frame.animationState.swingRotationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * frame.swingFullWave * frame.animationState.swingRotationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingYaw * frame.animationState.swingRotationWeight);
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign * frame.swingRoll * frame.animationState.swingRotationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.3 * frame.swingHalfWave * frame.animationState.swingRotationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.2 * frame.swingHalfWave * frame.animationState.swingRotationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingHalfWave * frame.animationState.swingRotationWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 20.0 * frame.swingTranslation * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 40.0 * frame.swingTranslation * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.positiveSwingWave * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.swingAcceleration * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -45.0 * frame.swingYaw * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -10.0 * frame.swingRoll * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -95.0 * frame.swingHalfWave * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateYAround(frame.poseStack, 20.0 * frame.swingHalfWave * frame.handSign * frame.animationState.swingRotationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(
            frame.poseStack, RotateXHelper.clamp(20.0 * frame.handSign * RotateXHelper.sin(frame.animationState.swingPhase) * frame.swingArc, -60.0, 60.0) * frame.animationState.swingRotationWeight, 0.5 * frame.handSign, -0.5, 0.0
         );
      } else if (!ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
         && !ChargedHelper.matches(renderContext.item, Items.MACE)
         && !frame.itemKind.equals("trident")
         && !frame.itemKind.equals("spear")
         && !ChargedHelper.matches(renderContext.item, HeldItemTags.AXES)) {
         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SHOVELS)) {
            RotateXHelper.translateY(frame.poseStack, 0.6 * frame.swingTranslation);
            RotateXHelper.translateZ(frame.poseStack, -0.5 * frame.swingTranslation);
            RotateXHelper.translateZ(frame.poseStack, -0.3 * frame.swingHalfWave);
            RotateXHelper.translateY(frame.poseStack, -0.3 * frame.swingHalfWave);
            RotateXHelper.translateY(frame.poseStack, -0.4 * frame.swingAcceleration);
            RotateXHelper.translateZ(frame.poseStack, 0.1 * frame.swingAcceleration);
            RotateXHelper.translateY(frame.poseStack, -0.1 * frame.swingRoll);
            RotateXHelper.rotateX(frame.poseStack, -130.0 * frame.swingTranslation);
            RotateXHelper.rotateX(frame.poseStack, 70.0 * frame.swingHalfWave);
            RotateXHelper.rotateX(frame.poseStack, 40.0 * frame.swingAcceleration);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.swingRoll);
            RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.positiveSwingWave);
            RotateXHelper.rotateX(frame.poseStack, -10.0 * frame.swingFullWave);
         } else {
            RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.swingArc);
            RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.swingArc);
            RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.positiveSwingWave);
            RotateXHelper.translateZ(frame.poseStack, -0.05 * frame.swingYaw);
            RotateXHelper.translateY(frame.poseStack, 0.25 * frame.swingYaw);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.swingArc);
            RotateXHelper.rotateXAround(frame.poseStack, 5.0 * frame.swingAcceleration, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -25.0 * frame.swingYaw, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 5.0 * frame.positiveSwingWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZ(frame.poseStack, -5.0 * frame.handSign * frame.swingHalfWave);
            RotateXHelper.rotateY(frame.poseStack, 15.0 * frame.handSign * frame.swingHalfWave);
            RotateXHelper.rotateXAround(frame.poseStack, -2.0 * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZAround(frame.poseStack, 5.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateYAround(frame.poseStack, 5.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
         }
      } else if (frame.animationState.groundedSwingReady && frame.animationState.alternateSwing && !renderContext.breakingBlock && ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
         RotateXHelper.translateZ(frame.poseStack, 0.2 * frame.swingTranslation * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.5 * frame.handSign * frame.swingTranslation * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.5 * frame.swingTranslation * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, 0.0 * frame.swingRoll * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.0 * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * frame.swingFullWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign * frame.swingRoll * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.3 * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.2 * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 20.0 * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, 70.0 * frame.handSign * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 30.0 * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.positiveSwingWave * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.swingAcceleration * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -25.0 * frame.swingYaw * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -10.0 * frame.swingRoll * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -75.0 * frame.swingHalfWave * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      } else if ((!frame.animationState.groundedSwingReady || renderContext.breakingBlock || !ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS))
         && !frame.itemKind.equals("trident")
         && !frame.itemKind.equals("spear")) {
         if ((!frame.animationState.groundedSwingReady || renderContext.breakingBlock) && ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
            || ChargedHelper.matches(renderContext.item, Items.MACE)
            || ChargedHelper.matches(renderContext.item, HeldItemTags.AXES)
            || renderContext.breakingBlock && frame.itemKind.equals("trident")) {
            RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.swingTranslation);
            RotateXHelper.translateZ(frame.poseStack, 0.0 * frame.swingYaw);
            RotateXHelper.translateZ(frame.poseStack, 0.0 * frame.swingRoll);
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.swingYaw);
            RotateXHelper.translateY(frame.poseStack, -0.1 * frame.swingFullWave);
            RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingYaw);
            RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign * frame.swingRoll);
            RotateXHelper.translateZ(frame.poseStack, -0.3 * frame.swingHalfWave);
            RotateXHelper.translateY(frame.poseStack, 0.2 * frame.swingHalfWave);
            RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingHalfWave);
            RotateXHelper.rotateXAround(frame.poseStack, 20.0 * frame.swingTranslation, 0.3 * frame.handSign, -0.4, 0.0);
            if (ChargedHelper.matches(renderContext.item, HeldItemTags.AXES) && renderContext.breakingBlock) {
               RotateXHelper.rotateZAround(frame.poseStack, -60.0 * frame.handSign * frame.swingTranslation, 0.3 * frame.handSign, -0.4, 0.0);
            } else {
               RotateXHelper.rotateZAround(frame.poseStack, -40.0 * frame.handSign * frame.swingTranslation, 0.3 * frame.handSign, -0.4, 0.0);
            }

            RotateXHelper.rotateXAround(frame.poseStack, 30.0 * frame.swingTranslation, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.positiveSwingWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.swingAcceleration, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -45.0 * frame.swingYaw, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -10.0 * frame.swingRoll, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -95.0 * frame.swingHalfWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZ(frame.poseStack, RotateXHelper.clamp(30.0 * frame.handSign * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc, 0.0, 30.0));
            RotateXHelper.translateY(frame.poseStack, -0.2 * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc);
         } else {
            RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.swingArc);
            RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingArc);
            RotateXHelper.translateZ(frame.poseStack, -0.1 * frame.positiveSwingWave);
            RotateXHelper.translateZ(frame.poseStack, -0.15 * frame.swingYaw);
            RotateXHelper.translateY(frame.poseStack, 0.1 * frame.swingYaw);
            RotateXHelper.translateY(frame.poseStack, -0.3 * frame.swingArc);
            RotateXHelper.rotateXAround(frame.poseStack, 20.0 * frame.swingAcceleration, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -40.0 * frame.swingYaw, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, -20.0 * frame.swingYaw, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateXAround(frame.poseStack, 5.0 * frame.positiveSwingWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZ(frame.poseStack, -5.0 * frame.handSign * frame.swingHalfWave);
            RotateXHelper.rotateY(frame.poseStack, 15.0 * frame.handSign * frame.swingHalfWave);
            if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
               RotateXHelper.translateZ(frame.poseStack, -0.15 * frame.swingRoll);
               RotateXHelper.translateZ(frame.poseStack, -0.15 * frame.swingHalfWave);
               RotateXHelper.rotateY(frame.poseStack, -10.0 * frame.handSign * frame.swingHalfWave);
               RotateXHelper.rotateX(frame.poseStack, -25.0 * frame.swingRoll);
               RotateXHelper.rotateX(frame.poseStack, -20.0 * frame.swingHalfWave);
               RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.swingTranslation);
            }

            if (!ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
               RotateXHelper.rotateXAround(frame.poseStack, -5.0 * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
            }

            RotateXHelper.rotateZAround(frame.poseStack, 10.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateYAround(frame.poseStack, 5.0 * frame.handSign * frame.swingFullWave, 0.3 * frame.handSign, -0.4, 0.0);
            RotateXHelper.rotateZ(frame.poseStack, RotateXHelper.clamp(30.0 * frame.handSign * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc, 0.0, 30.0));
            RotateXHelper.translateY(frame.poseStack, -0.2 * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc);
         }
      } else if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
         RotateXHelper.translateZ(frame.poseStack, -0.2 * frame.swingTranslation * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.15 * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.25 * frame.swingRoll * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.2 * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * frame.swingFullWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingYaw * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, 0.15 * frame.handSign * frame.swingRoll * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateZ(frame.poseStack, -0.45 * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateY(frame.poseStack, 0.2 * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingHalfWave * frame.animationState.swingTranslationWeight);
         RotateXHelper.rotateXAround(frame.poseStack, 20.0 * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateZAround(frame.poseStack, -70.0 * frame.handSign * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 30.0 * frame.swingTranslation * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.positiveSwingWave * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, 10.0 * frame.swingAcceleration * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -55.0 * frame.swingYaw * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -15.0 * frame.swingRoll * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
         RotateXHelper.rotateXAround(frame.poseStack, -95.0 * frame.swingHalfWave * frame.animationState.swingTranslationWeight, 0.3 * frame.handSign, -0.4, 0.0);
      } else {
         if (frame.itemKind.equals("trident")) {
            RotateXHelper.translateY(frame.poseStack, -0.5 * frame.swingTranslation * frame.animationState.swingLiftWeight);
            RotateXHelper.translateZ(frame.poseStack, 0.1 * frame.swingTranslation * frame.animationState.swingLiftWeight);
         } else {
            RotateXHelper.translateY(frame.poseStack, 0.0 * frame.swingTranslation * frame.animationState.swingLiftWeight);
            RotateXHelper.translateZ(frame.poseStack, 0.37 * frame.swingTranslation * frame.animationState.swingLiftWeight);
         }

         RotateXHelper.translateX(frame.poseStack, -0.15 * frame.handSign * frame.swingTranslation * frame.animationState.swingLiftWeight);
         if (frame.itemKind.equals("trident")) {
            RotateXHelper.translateZ(frame.poseStack, -0.25 * frame.swingRoll * frame.animationState.swingLiftWeight);
         } else {
            RotateXHelper.translateZ(frame.poseStack, -0.35 * frame.swingRoll * frame.animationState.swingLiftWeight);
            RotateXHelper.translateZ(frame.poseStack, -0.25 * ArmPoseHelper.easeOutQuart(frame.swingArc) * frame.animationState.swingLiftWeight);
         }

         RotateXHelper.translateZ(frame.poseStack, 0.14 * frame.swingHalfWave * frame.animationState.swingLiftWeight);
         RotateXHelper.translateY(frame.poseStack, -0.2 * frame.swingHalfWave * frame.animationState.swingLiftWeight);
         RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.swingYaw * frame.animationState.swingLiftWeight);
         RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.swingHalfWave * frame.animationState.swingLiftWeight);
         RotateXHelper.translateX(frame.poseStack, -0.1 * frame.handSign * frame.swingRoll * frame.animationState.swingLiftWeight);
         if (frame.itemKind.equals("trident")) {
            RotateXHelper.rotateZ(frame.poseStack, 80.0 * frame.swingTranslation * frame.handSign * frame.animationState.swingLiftWeight);
         } else {
            RotateXHelper.rotateZ(frame.poseStack, 20.0 * frame.swingTranslation * frame.handSign * frame.animationState.swingLiftWeight);
            RotateXHelper.rotateY(frame.poseStack, 6.0 * frame.swingTranslation * frame.handSign * frame.animationState.swingLiftWeight);
         }

         RotateXHelper.rotateZ(frame.poseStack, 5.0 * frame.swingHalfWave * frame.handSign * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, -40.0 * frame.swingTranslation * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, 15.0 * frame.swingRoll * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, 15.0 * frame.swingYaw * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, 20.0 * frame.swingHalfWave * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, 10.0 * frame.swingHalfWave * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateX(frame.poseStack, -10.0 * frame.positiveSwingWave * frame.animationState.swingLiftWeight);
         RotateXHelper.rotateZ(frame.poseStack, RotateXHelper.clamp(10.0 * frame.handSign * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc, 0.0, 30.0) * frame.animationState.swingLiftWeight);
         RotateXHelper.translateY(frame.poseStack, -0.1 * RotateXHelper.sin(frame.animationState.swingPhase * 2.0) * frame.swingArc * frame.animationState.swingLiftWeight);
      }
   }

   static final class PoseFrame {
      private double riptideAttackBlend;
      private SpearAnimationFlags spearFlags;
      private double swingYaw;
      private double pitchCompensation;
      private double crossbowChargeSnapshot;
      private int inspectionKeySnapshot;
      private HeldItemAnimationState animationState;
      private double handWalkingPhase;
      private double riptidePoseBlend;
      private double offCrossbowHoldBlend;
      private double offBrushBlend;
      private String itemKind;
      private double pitchSpring;
      private double previousOffSpearTranslation;
      private double yawSpringSnapshot;
      private double positiveSwingWave;
      private double mainTridentBlend;
      private double equipProgress;
      private double consumingBlend;
      private double itemBobWeight;
      private double twoHandedMapBlend;
      private double chargedPoseSnapshot;
      private double previousOffSpearRotation;
      private double swingFullWave;
      private double oneHandedMapBlend;
      private double horizontalSpeed;
      private double offTridentBlend;
      private double mainBowDrawBlend;
      private double idleOffsetX;
      private double equipRotationBlend;
      private double spearRotationBlend;
      private PoseStack poseStack;
      private double swingArc;
      private double mainCrossbowHoldBlend;
      private double bowHoldBlend;
      private boolean usingItem;
      private double yawSpring;
      private double equipTranslationBlend;
      private double spearChargedPoseBlend;
      private InteractionHand interactionHand;
      private double handSign;
      private double lanternMotionWeight;
      private boolean alternateSwingSnapshot;
      private double lookingDownBlend;
      private AbstractClientPlayer abstractClientPlayer;
      private double offCrossbowChargeBlend;
      private double swingTranslation;
      private double swingRoll;
      private double consumingRollImpulse;
      private double shieldCooldownBlend;
      private double swingAcceleration;
      private int inspectionKeyCode;
      private double swingHalfWave;
      private double offBowDrawBlend;

      private PoseFrame() {
      }
   }
}
