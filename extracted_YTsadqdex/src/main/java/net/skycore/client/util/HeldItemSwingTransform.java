package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.Items;

public final class HeldItemSwingTransform {
   public static void apply(HeldItemRenderContext renderContext) {
      HeldItemAnimationState i = HeldItemAnimationState.INSTANCE;
      AbstractClientPlayer player = renderContext.player;
      if (player != null) {
         PoseStack pose = renderContext.pose;
         String text = ChargedHelper.kind(renderContext.item);
         double computedValue = renderContext.right ? 1.0 : -1.0;
         i.mainBrushVelocity = i.mainBrushVelocity + RotateXHelper.sin(i.mainUsePhase * 4.14) * i.mainBrushProgress * renderContext.delta * 30.0;
         i.mainBrushVelocity = i.mainBrushVelocity - 0.04 * i.mainBrushSpring * renderContext.delta * 30.0;
         i.mainBrushVelocity = i.mainBrushVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.mainBrushSpring = i.mainBrushSpring + i.mainBrushVelocity * renderContext.delta * 30.0;
         i.offBrushVelocity = i.offBrushVelocity + RotateXHelper.sin(i.offUsePhase * 4.14) * i.offBrushProgress * renderContext.delta * 30.0;
         i.offBrushVelocity = i.offBrushVelocity - 0.04 * i.offBrushSpring * renderContext.delta * 30.0;
         i.offBrushVelocity = i.offBrushVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.offBrushSpring = i.offBrushSpring + i.offBrushVelocity * renderContext.delta * 30.0;
         double value = ChargedHelper.kind(VerticalSpeedHelper.mainhand(player)).equals("spear") ? 1.0 : 0.0;
         double value2 = ChargedHelper.kind(VerticalSpeedHelper.offhand(player)).equals("spear") ? 1.0 : 0.0;
         double value3 = renderContext.main ? i.mainSpearUseProgress : i.offSpearUseProgress;
         double value4 = renderContext.main ? i.mainSpearTranslationProgress : i.offSpearTranslationProgress;
         double value5 = renderContext.main ? i.mainSpearRotationProgress : i.offSpearRotationProgress;
         double value6 = renderContext.main ? i.mainEquipProgress : i.offEquipProgress;
         double value7 = renderContext.main ? ArmPoseHelper.easeInOutSine(i.mainSpearChargedPoseProgress) : i.offSpearChargedPoseProgress;
         i.mainPitchVelocity = i.mainPitchVelocity
            + (
                  VerticalSpeedHelper.speed(player) * 22.0 * i.walkingAnimationProgress * -1.0
                     - RotateXHelper.sin(renderContext.mainSwing * 3.14) * 8.0
                     + i.verticalBobbingSpring * 3.0
                     + RotateXHelper.sin(i.sneakingProgress * 3.14) * 0.3
                     + (VerticalSpeedHelper.pitch(player) - i.previousPitch)
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         if (text.equals("block") && renderContext.main && !ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
            i.mainPitchVelocity = i.mainPitchVelocity + 10.0 * RotateXHelper.sin(i.shieldCooldownProgress * 3.14) * 0.15 * renderContext.delta * 30.0;
            i.mainPitchVelocity = i.mainPitchVelocity + 12.0 * RotateXHelper.sin(i.mainBlockingProgress * 3.14) * 0.15 * renderContext.delta * 30.0;
         }

         i.mainPitchVelocity = i.mainPitchVelocity
            + (
                  -20.0 * RotateXHelper.sin(i.mainSpearTranslationProgress * 3.14) * i.mainSpearUseProgress
                     + 20.0 * RotateXHelper.sin(i.mainSpearRotationProgress * 3.14) * i.mainSpearUseProgress
                     + 12.0 * RotateXHelper.sin(i.inspectionKeyProgress * 3.14)
                     + 15.0 * RotateXHelper.sin(i.mainSpearUseProgress * 3.14)
                     + -10.0 * RotateXHelper.clamp(RotateXHelper.sin(ArmPoseHelper.easeInBack(i.mainSpearChargedPoseProgress) * 6.28), 0.0, 1.0)
                     + 40.0 * RotateXHelper.clamp(RotateXHelper.sin(RotateXHelper.clamp(i.mainEquipProgress * 1.5 * value, 0.0, 1.0) * 6.28), 0.0, 1.0)
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         i.mainPitchVelocity = i.mainPitchVelocity - 0.04 * i.mainPitchSpring * renderContext.delta * 30.0;
         i.mainPitchVelocity = i.mainPitchVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.mainPitchSpring = i.mainPitchSpring + i.mainPitchVelocity * renderContext.delta * 30.0;
         i.mainYawVelocity = i.mainYawVelocity
            + (
                  RotateXHelper.sin(i.walkingBobPhase) * 3.0 * i.walkingAnimationProgress
                     + RotateXHelper.sin(renderContext.mainSwing * 3.14) * 8.0
                     + RotateXHelper.sin(i.swimmingPhase * i.swimmingProgress) * 3.0
                     + RotateXHelper.sin(i.mainEquipProgress * 6.28) * 3.0
                     + VerticalSpeedHelper.yaw(player)
                     - i.previousYaw
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         i.mainYawVelocity = i.mainYawVelocity - 0.04 * i.mainYawSpring * renderContext.delta * 30.0;
         i.mainYawVelocity = i.mainYawVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.mainYawSpring = i.mainYawSpring + i.mainYawVelocity * renderContext.delta * 30.0;
         i.offPitchVelocity = i.offPitchVelocity
            + (
                  VerticalSpeedHelper.speed(player) * 22.0 * i.walkingAnimationProgress * -1.0
                     - RotateXHelper.sin(renderContext.offSwing * 3.14) * 8.0
                     + i.verticalBobbingSpring * 3.0
                     + RotateXHelper.sin(i.sneakingProgress * 3.14) * 0.3
                     + (VerticalSpeedHelper.pitch(player) - i.previousPitch)
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         if (text.equals("block") && !renderContext.main && !ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
            i.offPitchVelocity = i.offPitchVelocity + 10.0 * RotateXHelper.sin(i.shieldCooldownProgress * 3.14) * 0.15 * renderContext.delta * 30.0;
            i.offPitchVelocity = i.offPitchVelocity + 12.0 * RotateXHelper.sin(i.offBlockingProgress * 3.14) * 0.15 * renderContext.delta * 30.0;
         }

         i.offPitchVelocity = i.offPitchVelocity
            + (
                  -20.0 * RotateXHelper.sin(i.offSpearTranslationProgress * 3.14) * i.offSpearUseProgress
                     + 20.0 * RotateXHelper.sin(i.offSpearRotationProgress * 3.14) * i.offSpearUseProgress
                     + 15.0 * RotateXHelper.sin(i.offSpearUseProgress * 3.14)
                     + 40.0 * RotateXHelper.clamp(RotateXHelper.sin(RotateXHelper.clamp(i.offEquipProgress * 1.5 * value2, 0.0, 1.0) * 6.28), 0.0, 1.0)
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         i.offPitchVelocity = i.offPitchVelocity - 0.04 * i.offPitchSpring * renderContext.delta * 30.0;
         i.offPitchVelocity = i.offPitchVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.offPitchSpring = i.offPitchSpring + i.offPitchVelocity * renderContext.delta * 30.0;
         i.offYawVelocity = i.offYawVelocity
            + (
                  RotateXHelper.sin(i.walkingBobPhase) * 3.0 * i.walkingAnimationProgress
                     + RotateXHelper.sin(renderContext.offSwing * 3.14) * 8.0
                     + RotateXHelper.sin(i.swimmingPhase * i.swimmingProgress) * 3.0
                     + RotateXHelper.sin(i.offEquipProgress * 6.28) * 3.0
                     + VerticalSpeedHelper.yaw(player)
                     - i.previousYaw
               )
               * 0.15
               * renderContext.delta
               * 30.0;
         i.offYawVelocity = i.offYawVelocity - 0.04 * i.offYawSpring * renderContext.delta * 30.0;
         i.offYawVelocity = i.offYawVelocity * RotateXHelper.pow(0.85, renderContext.delta * 30.0F);
         i.offYawSpring = i.offYawSpring + i.offYawVelocity * renderContext.delta * 30.0;
         double value8 = renderContext.main ? i.mainYawSpring : i.offYawSpring;
         double pitch = renderContext.main ? i.mainPitchSpring : i.offPitchSpring;
         renderContext.swing = ChargedHelper.matches(renderContext.item, HeldItemTags.PICKAXES)
            ? (float)SwingEasing.easePickaxeSwing(renderContext.swing)
            : (float)SwingEasing.easeSwing(renderContext.swing);
         double computedValue2 = renderContext.swing < 0.70016
            ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.0, 0.308) * 5.1)
            : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.70016, 1.0) * 5.1 - 2.0);
         double computedValue3 = renderContext.swing < 0.65245
            ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.0, 0.16675) * 3.14 * 3.0)
            : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.65245, 1.0) * 4.4 - 1.3);
         computedValue2 = computedValue2 * computedValue2 * computedValue2;
         double computedValue4 = RotateXHelper.clamp(RotateXHelper.sin(renderContext.swing * 4.78), 0.0, 1.0);
         double computedValue5 = RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.16561, 0.49422) * 4.78 * 2.0 + 4.7);
         double computedValue6 = RotateXHelper.sin(renderContext.swing * 3.14);
         double computedValue7 = RotateXHelper.clamp(RotateXHelper.sin(renderContext.swing * 6.28), 0.0, 1.0);
         double computedValue8 = RotateXHelper.sin(renderContext.swing * 6.28);
         double computedValue9 = renderContext.swing < 0.65594
            ? RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.16561, 0.32991) * 4.78 * 2.0 + 4.7)
            : RotateXHelper.sin(RotateXHelper.clamp(renderContext.swing, 0.65594, 0.82025) * 4.78 * 2.0 - 4.7);
         computedValue2 *= renderContext.attackStrength;
         computedValue3 *= renderContext.attackStrength;
         computedValue4 *= renderContext.attackStrength;
         computedValue5 *= renderContext.attackStrength;
         computedValue6 *= renderContext.attackStrength;
         computedValue7 *= renderContext.attackStrength;
         computedValue8 *= renderContext.attackStrength;
         computedValue9 *= renderContext.attackStrength;
         if (text.equals("spear")) {
            RotateXHelper.rotateZ(pose, 180.0 * computedValue);
            RotateXHelper.rotateZ(pose, -180.0 * ArmPoseHelper.easeInOutBack(RotateXHelper.clamp(value6 * 2.0, 0.0, 1.0)) * computedValue);
            RotateXHelper.translateZ(pose, -0.2 * ArmPoseHelper.easeInOutSine(ArmPoseHelper.easeInOutBack(value3 * 0.8)));
            RotateXHelper.translateY(pose, -0.05 * ArmPoseHelper.easeInOutBack(value4));
            RotateXHelper.rotateX(pose, -70.0 * ArmPoseHelper.easeInOutBack(value3 * 0.8));
            RotateXHelper.rotateX(pose, -8.0 * ArmPoseHelper.easeInOutBack(value4));
            RotateXHelper.rotateY(pose, 60.0 * ArmPoseHelper.easeInOutBack(value3 * 0.8) * computedValue);
            RotateXHelper.rotateY(pose, -30.0 * ArmPoseHelper.easeInOutBack(value4) * computedValue);
            RotateXHelper.rotateY(pose, -60.0 * ArmPoseHelper.easeOutBack(value5) * value5 * computedValue);
            RotateXHelper.translateY(pose, -0.25 * RotateXHelper.clamp(RotateXHelper.sin(ArmPoseHelper.easeInOutSine(value7) * 6.28), 0.0, 1.0));
         }

         if (!text.equals("block") && !text.equals("crossbow") || ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
            RotateXHelper.translateZ(pose, -0.05 * computedValue2);
            RotateXHelper.translateY(pose, -0.05 * computedValue2);
            RotateXHelper.rotateX(pose, 10.0 * computedValue2);
            RotateXHelper.rotateX(pose, -30.0 * computedValue2);
            RotateXHelper.rotateX(pose, -10.0 * computedValue5);
            if (!ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
               if (!text.equals("trident") && !text.equals("spear")) {
                  RotateXHelper.translateZ(pose, -0.05 * computedValue2);
                  RotateXHelper.translateY(pose, -0.05 * computedValue2);
                  RotateXHelper.rotateX(pose, -10.0 * computedValue2);
                  RotateXHelper.rotateX(pose, -25.0 * computedValue5);
               } else {
                  RotateXHelper.translateZ(pose, -0.1 * computedValue2);
                  RotateXHelper.translateY(pose, -0.05 * computedValue2);
                  if (text.equals("spear")) {
                     RotateXHelper.translateY(pose, -0.15 * computedValue5);
                     RotateXHelper.rotateX(pose, -5.0 * computedValue5);
                  }

                  RotateXHelper.rotateX(pose, -10.0 * computedValue2);
                  RotateXHelper.rotateX(pose, -15.0 * computedValue5);
                  if (text.equals("trident")) {
                     RotateXHelper.rotateX(pose, -45.0 * computedValue6);
                  } else {
                     RotateXHelper.rotateX(pose, -45.0 * computedValue3);
                  }

                  RotateXHelper.translateY(pose, 0.05 * computedValue5);
                  RotateXHelper.translateY(pose, 0.3 * computedValue6);
               }
            }

            if (ChargedHelper.matches(renderContext.item, HeldItemTags.SHOVELS)) {
               RotateXHelper.translateY(pose, 0.12 * computedValue3);
               RotateXHelper.translateZ(pose, 0.05 * computedValue3);
               RotateXHelper.rotateX(pose, 10.0 * computedValue3);
               RotateXHelper.rotateX(pose, -30.0 * computedValue6);
               RotateXHelper.rotateX(pose, 20.0 * computedValue2);
               RotateXHelper.rotateX(pose, 10.0 * computedValue9);
            }

            if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
               computedValue4 = RotateXHelper.sin(renderContext.swing * 3.14) * renderContext.attackStrength;
               RotateXHelper.translateY(pose, -0.1 * ArmPoseHelper.easeInOutBack(computedValue4));
               if (ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)) {
                  RotateXHelper.rotateX(pose, -60.0 * ArmPoseHelper.easeInOutBack(computedValue4));
               } else {
                  RotateXHelper.rotateX(pose, -30.0 * ArmPoseHelper.easeInOutBack(computedValue4));
               }
            }

            if (text.equals("bow")) {
               RotateXHelper.translateX(pose, -0.065 * computedValue);
            }
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.BEDS)) {
            RotateXHelper.translateZ(pose, 0.2);
            RotateXHelper.rotateYAround(pose, 180.0 * computedValue, -0.15 * computedValue, -0.4, 0.0);
         }

         if (!ChargedHelper.matches(renderContext.item, Items.BELL)
            && !ChargedHelper.isLantern(renderContext.item)
            && !ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)
            && !ChargedHelper.matches(renderContext.item, HeldItemTags.HANGING_SIGNS)
            && !ChargedHelper.matches(renderContext.item, Items.PINK_PETALS)
            && !ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
            && !ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS)) {
            if (ChargedHelper.matches(renderContext.item, Items.PAINTING) || ChargedHelper.matches(renderContext.item, Items.ITEM_FRAME)) {
               renderContext.swing = 0.0F;
               RotateXHelper.rotateX(pose, -25.0);
               RotateXHelper.translateY(pose, -0.65);
               RotateXHelper.rotateXAround(pose, RotateXHelper.clamp(VerticalSpeedHelper.pitch(player) / 2.5, -25.0, 90.0) + pitch, 0.0, 0.45, 0.0);
               RotateXHelper.rotateZAround(pose, value8 * -1.0, 0.0, 0.55, 0.0);
            } else if (ChargedHelper.isBlockItem(renderContext.item)) {
               RotateXHelper.translateY(pose, -0.025);
               RotateXHelper.translateZ(pose, -0.025);
               RotateXHelper.rotateX(pose, -5.0);
            } else {
               if (!ChargedHelper.isBlockItem(renderContext.item) && !ChargedHelper.empty(renderContext.item) && text.equals("none") && !text.equals("crossbow")) {
                  if (!ChargedHelper.matches(renderContext.item, HeldItemTags.AXES) && !ChargedHelper.matches(renderContext.item, Items.MACE)) {
                     RotateXHelper.rotateXAround(pose, -20.0 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + pitch * 0.05, 0.3 * computedValue, -0.4, 0.0);
                  } else {
                     double value9 = ChargedHelper.matches(renderContext.item, Items.MACE) ? 0.2 : 0.15;
                     RotateXHelper.rotateXAround(pose, -20.0 * RotateXHelper.sin(renderContext.equip * renderContext.equip * renderContext.equip) + pitch * value9, 0.3 * computedValue, -0.3, 0.0);
                  }
               }

               if ((ChargedHelper.matches(renderContext.item, HeldItemTags.AXES) || ChargedHelper.matches(renderContext.item, Items.MACE)) && !text.equals("crossbow")) {
                  RotateXHelper.rotateXAround(pose, VerticalSpeedHelper.pitch(player) * -0.05 + pitch * 0.2, 0.0, -0.2, 0.0);
               } else if (!text.equals("crossbow")) {
                  RotateXHelper.rotateXAround(pose, VerticalSpeedHelper.pitch(player) * -0.025 + pitch * 0.1, 0.0, -0.2, 0.0);
               }
            }
         } else {
            if (!ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)) {
               RotateXHelper.translateY(pose, -0.62);
            }

            if (ChargedHelper.matches(renderContext.item, HeldItemTags.HANGING_SIGNS)) {
               RotateXHelper.translateY(pose, -0.07);
            }

            if (!ChargedHelper.matches(renderContext.item, Items.PINK_PETALS)
               && !ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
               && !ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS)) {
               RotateXHelper.translateZ(pose, 0.2);
               RotateXHelper.rotateX(pose, -25.0);
            } else {
               RotateXHelper.translateY(pose, 0.4);
               RotateXHelper.rotateX(pose, -70.0);
            }

            if (ChargedHelper.matches(renderContext.item, Items.PINK_PETALS) || ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS) || ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
               )
             {
               RotateXHelper.rotateXAround(pose, RotateXHelper.clamp(VerticalSpeedHelper.pitch(player) / 2.5, -20.0, 90.0) + pitch + value8 * 0.5, 0.0, -0.13, 0.0);
            }

            if (ChargedHelper.matches(renderContext.item, Items.BELL) || ChargedHelper.isLantern(renderContext.item) || ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)) {
               if (ChargedHelper.matches(renderContext.item, Items.END_CRYSTAL)) {
                  RotateXHelper.scale(
                     pose,
                     1.0 + 0.01 * RotateXHelper.sin(i.idlePhase * 15.0),
                     1.0 + 0.01 * RotateXHelper.sin(i.idlePhase * 15.0),
                     1.0 + 0.01 * RotateXHelper.sin(i.idlePhase * 8.0)
                  );
                  RotateXHelper.translateY(pose, 0.03 * RotateXHelper.sin(i.idlePhase * 2.0));
                  RotateXHelper.translateY(pose, 0.25);
                  RotateXHelper.translateY(pose, pitch / 150.0);
                  RotateXHelper.translateX(pose, value8 / 150.0 * computedValue * -1.0);
                  RotateXHelper.rotateZ(pose, 5.0 * RotateXHelper.sin(i.idlePhase));
                  RotateXHelper.scale(pose, 0.7, 0.7, 0.7);
               } else if (ChargedHelper.matches(renderContext.item, Items.BELL)) {
                  RotateXHelper.translateX(pose, 0.15 * computedValue);
                  RotateXHelper.translateY(pose, -0.05);
                  RotateXHelper.translateZ(pose, -0.1);
                  RotateXHelper.scale(pose, 1.2, 1.2, 1.2);
                  RotateXHelper.rotateXAround(pose, RotateXHelper.clamp(VerticalSpeedHelper.pitch(player) / 2.5, -20.0, 90.0) + pitch, -0.1 * computedValue, 0.4, 0.1);
                  RotateXHelper.rotateZAround(pose, value8 * -1.0, -0.1 * computedValue, 0.4, 0.1);
               } else {
                  RotateXHelper.rotateXAround(pose, RotateXHelper.clamp(VerticalSpeedHelper.pitch(player) / 2.5, -20.0, 90.0) + pitch, 0.0, 0.4, 0.0);
                  RotateXHelper.rotateZAround(pose, value8 * -1.0, 0.0, 0.4, 0.0);
               }
            }

            if (ChargedHelper.matches(renderContext.item, HeldItemTags.HANGING_SIGNS)) {
               RotateXHelper.rotateXAround(pose, RotateXHelper.clamp(VerticalSpeedHelper.pitch(player) / 2.5, -35.0, 90.0) + pitch, 0.0, 0.55, 0.0);
               RotateXHelper.rotateZAround(pose, value8 * -1.0, 0.0, 0.55, 0.0);
            }
         }

         if ((text.equals("drink") || text.equals("eat") || text.equals("toot_horn")) && renderContext.main) {
            RotateXHelper.translateX(pose, 0.02 * computedValue * i.mainConsumingProgress);
            RotateXHelper.translateZ(pose, -0.05 * i.mainConsumingProgress);
            if (text.equals("eat") || text.equals("toot_horn")) {
               RotateXHelper.rotateX(pose, -23.0 * i.mainConsumingProgress * i.mainConsumingProgress);
               RotateXHelper.rotateZ(pose, -12.0 * computedValue * i.mainConsumingProgress * i.mainConsumingProgress);
            }

            RotateXHelper.rotateY(pose, -50.0 * computedValue * i.mainConsumingProgress * i.mainConsumingProgress);
            if (text.equals("drink")) {
               RotateXHelper.rotateX(pose, 15.0 * i.mainConsumingProgress * i.mainConsumingProgress);
            }
         }

         if ((text.equals("drink") || text.equals("eat") || text.equals("toot_horn")) && !renderContext.main) {
            RotateXHelper.translateX(pose, 0.02 * computedValue * i.offConsumingProgress);
            RotateXHelper.translateZ(pose, -0.05 * i.offConsumingProgress);
            if (text.equals("eat") || text.equals("toot_horn")) {
               RotateXHelper.rotateX(pose, -23.0 * i.offConsumingProgress * i.offConsumingProgress);
               RotateXHelper.rotateZ(pose, -12.0 * computedValue * i.offConsumingProgress * i.offConsumingProgress);
            }

            RotateXHelper.rotateY(pose, -50.0 * computedValue * i.offConsumingProgress * i.offConsumingProgress);
            if (text.equals("drink")) {
               RotateXHelper.rotateX(pose, 15.0 * i.offConsumingProgress * i.offConsumingProgress);
            }
         }

         if (text.equals("brush") && renderContext.main) {
            RotateXHelper.translateZ(pose, -0.03 * ArmPoseHelper.easeInOutBack(i.mainBrushProgress));
            RotateXHelper.rotateX(pose, -30.0 * ArmPoseHelper.easeInOutBack(i.mainBrushProgress));
            RotateXHelper.rotateZ(pose, 15.0 * computedValue * RotateXHelper.sin((i.mainUsePhase - 0.5) * 4.14) * ArmPoseHelper.easeInOutBack(i.mainBrushProgress));
            RotateXHelper.rotateZ(pose, computedValue * i.mainBrushSpring);
         }

         if (text.equals("brush") && !renderContext.main) {
            RotateXHelper.translateZ(pose, -0.03 * ArmPoseHelper.easeInOutBack(i.offBrushProgress));
            RotateXHelper.rotateX(pose, -30.0 * ArmPoseHelper.easeInOutBack(i.offBrushProgress));
            RotateXHelper.rotateZ(pose, 15.0 * computedValue * RotateXHelper.sin((i.offUsePhase - 0.5) * 4.14) * ArmPoseHelper.easeInOutBack(i.offBrushProgress));
            RotateXHelper.rotateZ(pose, computedValue * i.offBrushSpring);
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.DOORS)) {
            RotateXHelper.translateX(pose, 0.1 * computedValue);
            RotateXHelper.translateZ(pose, 0.25);
            RotateXHelper.translateY(pose, -0.35);
            RotateXHelper.rotateZ(pose, -10.0 * computedValue);
            RotateXHelper.rotateY(pose, -90.0 * computedValue);
         } else if (ChargedHelper.matches(renderContext.item, HeldItemTags.BEDS)) {
            RotateXHelper.translateZ(pose, 0.17);
            RotateXHelper.rotateYAround(pose, -35.0 * computedValue, 0.3 * computedValue, -0.4, 0.0);
            RotateXHelper.scale(pose, 0.9, 0.9, 0.9);
         }

         if (ChargedHelper.matches(renderContext.item, Items.SLIME_BALL) || ChargedHelper.matches(renderContext.item, Items.SLIME_BLOCK) || ChargedHelper.matches(renderContext.item, Items.HONEY_BLOCK)) {
            if (ChargedHelper.matches(renderContext.item, Items.SLIME_BALL)) {
               RotateXHelper.translateY(pose, -0.1);
               double computedValue10 = i.verticalBobbingSpring < 0.0 ? i.verticalBobbingSpring * 0.06 : i.verticalBobbingSpring * 0.12;
               RotateXHelper.scale(pose, 1.0, 1.0 + computedValue10, 1.0);
               RotateXHelper.translateY(pose, 0.1);
            } else {
               double computedValue11 = i.verticalBobbingSpring < 0.0 ? i.verticalBobbingSpring * 0.05 : i.verticalBobbingSpring * 0.1;
               double computedValue12 = i.verticalBobbingSpring < 0.0 ? i.verticalBobbingSpring * 0.1 : i.verticalBobbingSpring * 0.3;
               RotateXHelper.translateY(pose, 0.0);
               RotateXHelper.scale(pose, 1.0 - computedValue11, 1.0 + computedValue12, 1.0 - computedValue11);
               RotateXHelper.translateY(pose, 0.0);
               if (renderContext.right) {
                  RotateXHelper.shear(pose, 0.0, 0.0 - value8 * 0.006, 0.0);
               } else {
                  RotateXHelper.shear(pose, 0.0, 0.0 + value8 * 0.006, 0.0);
               }
            }
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SHOVELS)) {
            RotateXHelper.translateX(pose, -0.09 * computedValue);
            RotateXHelper.rotateY(pose, 80.0 * computedValue);
         }

         i.previousPitch = VerticalSpeedHelper.pitch(player);
         i.previousYaw = VerticalSpeedHelper.yaw(player);
         if (ChargedHelper.matches(renderContext.item, Items.MAGMA_CREAM)) {
            RotateXHelper.scale(pose, 1.0 - i.verticalBobbingSpring / 5.0, 1.0 + i.verticalBobbingSpring / 5.0, 1.0);
         }

         double computedValue13 = ArmPoseHelper.easeInBack(RotateXHelper.sin(RotateXHelper.clamp(renderContext.main ? i.mainEquipProgress : i.offEquipProgress, 0.09723, 0.60632) * 3.24 * 1.65 - 0.1));
         if ((
               ChargedHelper.matches(renderContext.item, HeldItemTags.BUNDLES)
                  || ChargedHelper.matches(renderContext.item, Items.ENDER_PEARL)
                  || ChargedHelper.matches(renderContext.item, Items.ENDER_EYE)
                  || ChargedHelper.projectile(renderContext.item)
                  || ChargedHelper.matches(renderContext.item, HeldItemTags.MUSIC_DISCS)
                  || ChargedHelper.matches(renderContext.item, HeldItemTags.NUGGETS)
                  || ChargedHelper.matches(renderContext.item, HeldItemTags.SKULLS)
            )
            && !text.equals("trident")) {
            RotateXHelper.rotateX(pose, -10.0 * computedValue13);
            RotateXHelper.translateY(pose, 0.62 * computedValue13);
            RotateXHelper.translateY(pose, RotateXHelper.clamp(0.1 * i.verticalBobbingSpring, 0.0, 255.0));
            int value10 = (!renderContext.main || !renderContext.mainItemChanged) && !renderContext.offItemChanged ? 0 : 1;
            if (ChargedHelper.matches(renderContext.item, HeldItemTags.NUGGETS)) {
               if (value10 != 0) {
               }

               RotateXHelper.translateY(pose, -0.07);
               RotateXHelper.rotateXAround(
                  pose,
                  360.0 * ArmPoseHelper.easeInOutBack(renderContext.main ? RotateXHelper.clamp(i.mainEquipProgress * 1.65, 0.0, 1.0) : RotateXHelper.clamp(i.offEquipProgress * 1.65, 0.0, 1.0)),
                  0.0,
                  0.1,
                  0.0
               );
            } else if (ChargedHelper.matches(renderContext.item, HeldItemTags.MUSIC_DISCS)) {
               if (value10 != 0) {
               }

               RotateXHelper.rotateZAround(
                  pose,
                  360.0 * ArmPoseHelper.easeInOutBack(renderContext.main ? RotateXHelper.clamp(i.mainEquipProgress * 1.65, 0.0, 1.0) : RotateXHelper.clamp(i.offEquipProgress * 1.65, 0.0, 1.0)),
                  -0.1 * computedValue,
                  0.25,
                  0.0
               );
            } else {
               if (value10 != 0) {
               }

               double computedValue14 = renderContext.main ? RotateXHelper.clamp(i.mainEquipProgress * 1.2, 0.0, 1.0) : RotateXHelper.clamp(i.offEquipProgress * 1.2, 0.0, 1.0);
               RotateXHelper.rotateZ(pose, -7.0 * computedValue * RotateXHelper.sin(RotateXHelper.clamp(computedValue14, 0.0943, 0.66791) * 7.07 * 1.5 - 0.8));
            }
         }

         double value11 = ArmPoseHelper.easeInOutBack(i.twoHandedMapProgress);
         double value12 = ArmPoseHelper.easeInOutBack(i.lookingDownProgress);
         if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP)) {
            RotateXHelper.rotateZ(pose, 5.0 * computedValue * value11);
            RotateXHelper.rotateY(pose, (-40.0 - 20.0 * value12) * computedValue * value11);
            RotateXHelper.rotateZ(pose, 15.0 * computedValue * value11);
            RotateXHelper.rotateX(pose, -10.0 * value12 * value11);
         }

         if (ChargedHelper.matches(renderContext.item, Items.FILLED_MAP)) {
            double computedValue15 = 1.0 - value11;
            RotateXHelper.translateZ(pose, -0.05 * computedValue15);
            RotateXHelper.translateY(pose, -0.05 * computedValue15);
            RotateXHelper.rotateX(pose, -40.0 * computedValue15);
            RotateXHelper.rotateY(pose, -10.0 * computedValue * computedValue15);
            RotateXHelper.rotateZ(pose, 5.0 * computedValue * computedValue15);
         } else if (ChargedHelper.isDecorativeItem(renderContext.item)
            && !ChargedHelper.isBlockItem(renderContext.item)
            && !ChargedHelper.matches(renderContext.item, Items.BONE)
            && !text.equals("bow")
            && !text.equals("spear")) {
            RotateXHelper.translateX(pose, -0.05 * computedValue);
            RotateXHelper.rotateX(pose, -8.0);
            RotateXHelper.rotateY(pose, -10.0 * computedValue);
            RotateXHelper.rotateZ(pose, 6.0 * computedValue);
         }

         if (ChargedHelper.hasCustomSwing(renderContext.item)) {
            RotateXHelper.translateX(pose, -0.05 * computedValue);
            RotateXHelper.rotateX(pose, -8.0);
            RotateXHelper.rotateY(pose, -10.0 * computedValue);
            RotateXHelper.rotateZ(pose, 6.0 * computedValue);
         }

         if (ChargedHelper.matches(renderContext.item, Items.SHEARS)) {
            if (!renderContext.right) {
               RotateXHelper.translateZ(pose, 0.1);
               RotateXHelper.rotateY(pose, 180.0);
            }

            RotateXHelper.rotateZ(pose, 45.0);
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SKULLS) && !ChargedHelper.matches(renderContext.item, Items.DRAGON_HEAD)) {
            RotateXHelper.translateX(pose, -0.1 * computedValue);
            RotateXHelper.translateY(pose, 0.11);
            RotateXHelper.rotateZ(pose, 15.0 * computedValue);
            RotateXHelper.rotateY(pose, -85.0 * computedValue);
            RotateXHelper.rotateX(pose, -55.0);
         } else if (ChargedHelper.matches(renderContext.item, Items.DRAGON_HEAD)) {
            RotateXHelper.translateY(pose, 0.25);
            RotateXHelper.rotateZ(pose, 6.0 * computedValue);
            RotateXHelper.rotateY(pose, 160.0 * computedValue);
         }

         if ((!renderContext.main || !renderContext.mainItemChanged) && renderContext.offItemChanged) {
         }

         if ((
               ChargedHelper.matches(renderContext.item, Items.BREWING_STAND)
                  || ChargedHelper.matches(renderContext.item, Items.REDSTONE_TORCH)
                  || ChargedHelper.matches(renderContext.item, Items.TORCH)
                  || ChargedHelper.matches(renderContext.item, Items.LANTERN)
                  || ChargedHelper.matches(renderContext.item, Items.SOUL_TORCH)
                  || ChargedHelper.matches(renderContext.item, Items.SOUL_LANTERN)
            )
            && !ChargedHelper.matches(renderContext.item, Items.BREWING_STAND)
            && !ChargedHelper.matches(renderContext.item, Items.TORCH)
            && !ChargedHelper.matches(renderContext.item, Items.LANTERN)
            && (!ChargedHelper.isLantern(renderContext.item) || !ChargedHelper.id(renderContext.item).contains("copper"))
            && !ChargedHelper.matches(renderContext.item, Items.SOUL_TORCH)
            && !ChargedHelper.matches(renderContext.item, Items.SOUL_LANTERN)
            && ChargedHelper.matches(renderContext.item, Items.REDSTONE_TORCH)) {
         }

         int value13 = i.inspectionKeyCode != 0 ? i.inspectionKeyCode : 67;
         i.inspectionProgress = GameplayKeyState.isPressed(value13) ? (i.inspectionProgress = i.inspectionProgress + 0.025 * renderContext.delta * 30.0) : 0.0;
         i.inspectionProgress = RotateXHelper.clamp(i.inspectionProgress, 0.0, 1.0);
         if ((
               ChargedHelper.matches(renderContext.item, HeldItemTags.SWORDS)
                  || ChargedHelper.matches(renderContext.item, HeldItemTags.PICKAXES)
                  || ChargedHelper.matches(renderContext.item, HeldItemTags.AXES)
                  || text.equals("trident")
            )
            && renderContext.main) {
            RotateXHelper.translateX(pose, -0.2 * computedValue * i.inspectionKeyProgress);
            RotateXHelper.rotateXAround(pose, -360.0 * ArmPoseHelper.easeInOutBack(i.inspectionProgress), 0.0, 0.0, 0.15);
         }

         i.previousHealth = VerticalSpeedHelper.health(player);
         if (i.lastSwingCount != VerticalSpeedHelper.swingCount(player) && renderContext.main && ChargedHelper.matches(renderContext.item, Items.BELL)) {
         }

         i.lastSwingCount = VerticalSpeedHelper.swingCount(player);
         if (renderContext.main) {
            i.mainSwingStarted = renderContext.mainSwingStarted;
         } else {
            i.offSwingStarted = renderContext.offSwingStarted;
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.COPPER_GOLEM_STATUES)) {
            i.itemTransformOverrides.put(ChargedHelper.id(renderContext.item), false);
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.SHOVELS)) {
            i.itemTransformModes.put(ChargedHelper.id(renderContext.item), 14);
         }

         double value14 = ArmPoseHelper.easeInQuart(renderContext.main ? i.mainConsumingProgress : i.offConsumingProgress);
         if (ChargedHelper.matches(renderContext.item, Items.BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.AXOLOTL_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.POWDER_SNOW_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.PUFFERFISH_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.TADPOLE_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.SALMON_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.COD_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.TROPICAL_FISH_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.WATER_BUCKET)
            || ChargedHelper.matches(renderContext.item, Items.LAVA_BUCKET)) {
            RotateXHelper.translateY(pose, 0.025);
            RotateXHelper.translateX(pose, 0.0 * computedValue);
            RotateXHelper.translateZ(pose, -0.1);
            RotateXHelper.rotateY(pose, 180.0);
            RotateXHelper.rotateX(pose, -82.5);
            RotateXHelper.rotateZ(pose, -20.0 * computedValue);
         }

         if (ChargedHelper.matches(renderContext.item, Items.MILK_BUCKET)) {
            RotateXHelper.translateY(pose, 0.025);
            RotateXHelper.translateX(pose, 0.0 * computedValue);
            RotateXHelper.translateZ(pose, -0.1);
            RotateXHelper.rotateY(pose, 180.0);
            RotateXHelper.rotateX(pose, -82.5);
            RotateXHelper.rotateZ(pose, -20.0 * computedValue);
            RotateXHelper.rotateX(pose, 0.0 * value14);
            RotateXHelper.rotateZ(pose, 30.0 * computedValue * value14);
            RotateXHelper.rotateY(pose, 0.0 * computedValue * value14);
            RotateXHelper.translateX(pose, 0.0 * computedValue * value14);
            RotateXHelper.translateY(pose, 0.1 * value14);
            RotateXHelper.translateZ(pose, 0.02 * value14);
         }

         if (ChargedHelper.matches(renderContext.item, Items.LAVA_BUCKET)) {
         }

         if (ChargedHelper.id(renderContext.item).equals("bucket_of_frog:frog_bucket_cold")
            || ChargedHelper.id(renderContext.item).equals("bucket_of_frog:frog_bucket_warm")
            || ChargedHelper.id(renderContext.item).equals("bucket_of_frog:frog_bucket_temperate")) {
            RotateXHelper.translateY(pose, 0.025);
            RotateXHelper.translateX(pose, 0.0 * computedValue);
            RotateXHelper.translateZ(pose, -0.1);
            RotateXHelper.rotateY(pose, 180.0);
            RotateXHelper.rotateX(pose, -82.5);
            RotateXHelper.rotateZ(pose, -20.0 * computedValue);
         }

         if (text.equals("trident")) {
            double computedValue16 = ArmPoseHelper.easeOutBack(RotateXHelper.clamp(renderContext.main ? i.mainTridentUseProgress : i.offTridentUseProgress * 1.5, 0.0, 1.0));
            RotateXHelper.rotateX(pose, 180.0 * computedValue16);
            RotateXHelper.rotateZ(pose, 170.0 * computedValue * computedValue16);
            RotateXHelper.translateZ(pose, -0.1);
            RotateXHelper.rotateY(pose, 40.0 * computedValue);
         }

         if (text.equals("spear")) {
            RotateXHelper.translateZ(pose, -0.1);
            RotateXHelper.rotateY(pose, 10.0 * computedValue);
         }

         if (text.equals("trident")) {
            RotateXHelper.rotateX(pose, -90.0 * ArmPoseHelper.easeOutBack(RotateXHelper.sin(renderContext.main ? i.mainRiptideProgress : i.offRiptideProgress * 3.14)));
            RotateXHelper.rotateZ(pose, -45.0 * computedValue * ArmPoseHelper.easeOutBack(RotateXHelper.sin(renderContext.main ? i.mainRiptideProgress : i.offRiptideProgress * 3.14)));
         }

         if (ChargedHelper.matches(renderContext.item, HeldItemTags.HANGING_SIGNS)
            || ChargedHelper.matches(renderContext.item, HeldItemTags.DOORS)
            || ChargedHelper.matches(renderContext.item, HeldItemTags.SKULLS)
            || ChargedHelper.matches(renderContext.item, HeldItemTags.SIGNS)) {
            i.blockTransforms.put(ChargedHelper.id(renderContext.item), false);
         }

         if (ChargedHelper.isBlockItem(renderContext.item)
            && i.blockTransforms.getOrDefault(ChargedHelper.id(renderContext.item), true)
            && i.itemTransformOverrides.getOrDefault(ChargedHelper.id(renderContext.item), true)
            && !ChargedHelper.matches(renderContext.item, Items.PINK_PETALS)
            && !ChargedHelper.matches(renderContext.item, Items.LEAF_LITTER)
            && !ChargedHelper.matches(renderContext.item, Items.WILDFLOWERS)
            && !ChargedHelper.matches(renderContext.item, Items.REDSTONE)
            && !ChargedHelper.matches(renderContext.item, Items.BELL)) {
            RotateXHelper.translateZ(pose, -0.05);
            if (!ChargedHelper.isLantern(renderContext.item)) {
               RotateXHelper.translateY(pose, -0.15);
               RotateXHelper.rotateZ(pose, 6.0 * computedValue);
               RotateXHelper.rotateX(pose, -8.0);
            }

            RotateXHelper.rotateY(pose, 25.0 * computedValue);
            RotateXHelper.scale(pose, 1.1, 1.1, 1.1);
         }

         double value15 = ArmPoseHelper.easeOutBack(i.mainBowHoldProgress);
         double value16 = ArmPoseHelper.easeOutBack(i.offBowHoldProgress);
         double value17 = renderContext.main ? value15 : value16;
         if (value17 < 0.1) {
            i.itemAnimationOverrides.put("minecraft:bow", false);
         } else {
            i.itemAnimationOverrides.put("minecraft:bow", true);
         }

         i.itemTransformScales.put("minecraft:bow", ArmPoseHelper.smoothStep(value17) * 20.0);
      }
   }
}
