package net.skycore.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.module.render.SwingAnimationsModule;
import net.skycore.client.module.render.ViewModelModule;

public final class SwingCountHelper {
   private static float mainSwing;
   private static float offSwing;
   private static int swingCount;
   private static Item previousOff = Items.AIR;
   private static boolean mainStarted;
   private static float delta;
   private static long previousFrame;
   private static boolean wasOffSwinging;
   private static final HeldItemRenderContext OFF = new HeldItemRenderContext();
   private static final HeldItemRenderContext MAIN = new HeldItemRenderContext();
   private static boolean offStarted;
   private static boolean wasMainSwinging;
   private static Item previousMain = Items.AIR;

   public static int swingCount() {
      return swingCount;
   }

   public static void render(
      ItemInHandRenderer itemInHandRenderer,
      SwingCountHelper.ArmRenderer armRenderer,
      SwingCountHelper.MapRenderer lookup,
      AbstractClientPlayer player,
      float coordinate,
      float pitch,
      InteractionHand interactionHand,
      float coordinate2,
      ItemStack itemStack,
      float coordinate3,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      int coordinate4
   ) {
      if (!player.isScoping()) {
         int value = interactionHand == InteractionHand.MAIN_HAND ? 1 : 0;
         HumanoidArm opposite = value != 0 ? player.getMainArm() : player.getMainArm().getOpposite();
         int value2 = opposite == HumanoidArm.RIGHT ? 1 : 0;
         int computedValue = value2 != 0 ? 1 : -1;
         SwingAnimationsModule iNSTANCE = SwingAnimationsModule.INSTANCE;
         coordinate2 = iNSTANCE.holdMyItemsSwing(coordinate2);
         HeldItemRenderContext renderContext = value != 0 ? MAIN : OFF;
         renderContext.player = player;
         renderContext.item = itemStack;
         renderContext.pose = poseStack;
         renderContext.collector = submitNodeCollector;
         renderContext.hand = interactionHand;
         renderContext.main = (value != 0);
         renderContext.right = (value2 != 0);
         renderContext.side = computedValue;
         renderContext.light = coordinate4;
         renderContext.swing = coordinate2;
         renderContext.equip = coordinate3;
         renderContext.delta = delta;
         renderContext.partialTick = coordinate;
         renderContext.pitch = pitch;
         renderContext.attackStrength = iNSTANCE.hmiStrength.get().floatValue();
         if (value != 0) {
            mainSwing = coordinate2;
         } else {
            offSwing = coordinate2;
         }

         renderContext.mainSwing = mainSwing;
         renderContext.offSwing = offSwing;
         renderContext.mainSwingStarted = renderContext.mainEvent = mainStarted;
         renderContext.offSwingStarted = renderContext.offEvent = offStarted;
         renderContext.mainItemChanged = ((player.getMainHandItem().getItem() != previousMain ? 1 : 0) != 0);
         renderContext.offItemChanged = ((player.getOffhandItem().getItem() != previousOff ? 1 : 0) != 0);
         renderContext.usingItem = ((player.isUsingItem() && player.getUsedItemHand() == interactionHand ? 1 : 0) != 0);
         renderContext.breakingBlock = ((Minecraft.getInstance().gameMode != null && Minecraft.getInstance().gameMode.isDestroying() ? 1 : 0) != 0);
         poseStack.pushPose();

         try {
            ViewModelModule.INSTANCE.applyHandPosition(poseStack, opposite);
            ApplyStage9Helper.apply(renderContext);
            if (!itemStack.isEmpty()) {
               poseStack.translate(0.0, -0.35, 0.2);
            }

            poseStack.pushPose();

            try {
               HeldItemIdleTransform.apply(renderContext);
               if (!itemStack.isEmpty()) {
                  poseStack.translate(1.5 * computedValue, -0.3, -0.6);
                  poseStack.rotateAround(Axis.XP.rotationDegrees(15.0F), 0.5F * computedValue, 0.5F, 0.5F);
                  poseStack.rotateAround(Axis.YP.rotationDegrees(35 * computedValue), 0.5F * computedValue, 0.5F, 0.5F);
                  poseStack.rotateAround(Axis.ZP.rotationDegrees(-65 * computedValue), 0.5F * computedValue, 0.5F, 0.5F);
                  poseStack.scale(0.9F, 0.9F, 0.9F);
               }

               if (!player.isInvisible()) {
                  armRenderer.render(poseStack, submitNodeCollector, coordinate4, opposite);
               }
            } finally {
               poseStack.popPose();
            }

            if (!itemStack.isEmpty()) {
               poseStack.pushPose();

               try {
                  renderContext.swing = coordinate2;
                  poseStack.translate(0.5 * computedValue, -0.15, -0.85);
                  poseStack.rotateAround(Axis.XP.rotationDegrees(15.0F), 0.5F, 0.5F, 0.5F);
                  poseStack.scale(0.9F, 0.9F, 0.9F);
                  HeldItemSwingTransform.apply(renderContext);
                  float value3 = iNSTANCE.hmiItemSize.get().floatValue();
                  poseStack.scale(value3, value3, value3);
                  if (itemStack.has(DataComponents.MAP_ID)) {
                     poseStack.translate(-0.05F * computedValue, 0.2F, 0.1F);
                     poseStack.mulPose(Axis.YP.rotationDegrees(-12 * computedValue));
                     lookup.render(poseStack, submitNodeCollector, coordinate4, itemStack);
                  } else {
                     itemInHandRenderer.renderItem(
                        player, itemStack, value2 != 0 ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND, poseStack, submitNodeCollector, coordinate4
                     );
                  }
               } finally {
                  poseStack.popPose();
               }
            }

            if (value != 0) {
               previousMain = itemStack.getItem();
            } else {
               previousOff = itemStack.getItem();
            }
         } finally {
            poseStack.popPose();
         }
      }
   }

   private SwingCountHelper() {
   }

   public static void beginFrame() {
      long nowNanos = System.nanoTime();
      delta = previousFrame == 0L ? 0.0F : Math.min(0.05F, (float)(nowNanos - previousFrame) / 1.0E9F);
      previousFrame = nowNanos;
      Minecraft instance = Minecraft.getInstance();
      if (instance.isPaused()) {
         delta = 0.0F;
      }

      int value = instance.player != null && instance.player.swinging ? 1 : 0;
      int value2 = value != 0 && instance.player.swingingArm == InteractionHand.MAIN_HAND ? 1 : 0;
      int value3 = value != 0 && instance.player.swingingArm == InteractionHand.OFF_HAND ? 1 : 0;
      mainStarted = ((value2 != 0 && !wasMainSwinging ? 1 : 0) != 0);
      offStarted = ((value3 != 0 && !wasOffSwinging ? 1 : 0) != 0);
      if (mainStarted || offStarted) {
         swingCount++;
      }

      wasMainSwinging = (value2 != 0);
      wasOffSwinging = (value3 != 0);
   }

   public interface ArmRenderer {
      public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int coordinate, HumanoidArm humanoidArm);
   }

   public interface MapRenderer {
      public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int coordinate, ItemStack itemStack);
   }
}
