package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.Items;

public final class HeldItemIdleTransform {
   public static void apply(HeldItemRenderContext renderContext) {
      HeldItemAnimationState i = HeldItemAnimationState.INSTANCE;
      PoseStack pose = renderContext.pose;
      double computedValue = renderContext.right ? 1.0 : -1.0;
      String text = ChargedHelper.kind(renderContext.item);
      if (ChargedHelper.empty(renderContext.item) && i.mainConsumingAnimationWeight > 0.0) {
         RotateXHelper.translateX(pose, 1.5 * computedValue);
         RotateXHelper.translateY(pose, -0.3);
         RotateXHelper.translateZ(pose, -0.47);
         RotateXHelper.rotateXAround(pose, 15.0, 0.5 * computedValue, 0.5, 0.5);
         RotateXHelper.rotateYAround(pose, 35.0 * computedValue, 0.5 * computedValue, 0.5, 0.5);
         RotateXHelper.rotateZAround(pose, -65.0 * computedValue, 0.5 * computedValue, 0.5, 0.5);
         RotateXHelper.scale(pose, 0.9, 0.9, 0.9);
      }

      double value = renderContext.main ? i.mainEquipProgress : i.offEquipProgress;
      double computedValue2 = ArmPoseHelper.easeInBack(RotateXHelper.sin(RotateXHelper.clamp(value, 0.09723, 0.60632) * 3.24 * 1.65 - 0.1));
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
         RotateXHelper.rotateX(pose, 10.0 * computedValue2);
         RotateXHelper.rotateZ(pose, 6.0 * computedValue2);
      }
   }
}
