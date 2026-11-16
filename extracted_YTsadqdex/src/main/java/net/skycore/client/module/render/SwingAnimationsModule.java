package net.skycore.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.SmoothHelper;
import net.skycore.client.setting.BooleanSetting;
import org.joml.Quaternionf;

public final class SwingAnimationsModule extends ClientModule {
   public final SliderSetting hmiSmoothing;
   private static final ThreadLocal<Float> CAPTURED_EQUIP = ThreadLocal.withInitial(() -> {
      byte flag = 1;
      return 0.0F;
   });
   public final SliderSetting dropDownCorner;
   public final SliderSetting sourceDuration;
   public final SliderSetting transform;
   public final SliderSetting hmiDuration;
   public static final SwingAnimationsModule INSTANCE = new SwingAnimationsModule();
   public final SliderSetting sourceItemSize;
   public final SliderSetting sourceTilt;
   public final BooleanSetting onlyWithAura;
   public final SliderSetting dropDownSlant;
   public final SliderSetting speed;
   public final ModeSetting animationMode = this.add(
      new ModeSetting(
         "Mode",
         "Mode 1",
         "Mode 1",
         "Mode 2",
         "Mode 3",
         "Mode 4",
         "Mode 5",
         "Mode 6",
         "Mode 7",
         "Mode 8",
         "Mode 9",
         "Mode 10",
         "Mode 11",
         "Mode 12",
         "Mode 13",
         "Mode 14",
         "Mode 15",
         "Mode 16",
         "Mode 17",
         "Mode 18",
         "HoldMyItems"
      )
   );
   public final BooleanSetting eatAnim;
   public final SliderSetting swingStrength = this.add(new SliderSetting("Animation Strength", 1.0, 0.1, 3.0, 0.01));
   public final SliderSetting hmiItemSize;
   public final BooleanSetting swapHands;
   private static final ThreadLocal<Float> CAPTURED_SWING = ThreadLocal.withInitial(() -> {
      return 0.0F;
   });
   public final BooleanSetting hmiChangeSpeed;
   public final SliderSetting swingPower;
   public final SliderSetting sourceSmoothing;
   public final BooleanSetting onlyDuringHit;
   private static final String[] SELF_OFFSET_MODES = new String[]{
      "Smooth", "Static", "Down", "DropDown", "Poke", "Feast", "ToBack", "Block", "Pander", "Slant", "Mode 2"
   };
   public final SliderSetting hmiStrength;

   private boolean isLegacyMode() {
      return !this.isSourceMode() && !this.isHoldMyItemsSelected();
   }

   public static void captureSwing(float value, float value2) {
      CAPTURED_SWING.set(value);
      CAPTURED_EQUIP.set(value2);
   }

   public float modifyEquipProgress(HumanoidArm humanoidArm, float result) {
      if (this.shouldApplyAnimation() && !this.isSelfOffsetMode()) {
         if (!this.onlyDuringHit.get()) {
            LocalPlayer instance = Minecraft.getInstance().player;
            if (instance != null && humanoidArm == instance.getMainArm()) {
               return this.transform.get().floatValue();
            }

            return result;
         }
      }

      return result;
   }

   public void applySelfOffsetSwing(PoseStack poseStack, float value, float value2, HumanoidArm humanoidArm) {
      int computedValue = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
      float value3 = this.swingStrength.get().floatValue();
      float computedValue2 = Mth.sin(Mth.sqrt(value) * (float) Math.PI);
      switch (this.resolvedMode()) {
         case "Down":
            poseStack.translate(computedValue * 0.56F, -0.32F, -0.72F);
            poseStack.mulPose(Axis.YP.rotationDegrees(76 * computedValue));
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * -5.0F * value3));
            poseStack.mulPose(Axis.XN.rotationDegrees(computedValue2 * -100.0F * value3));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue2 * -155.0F * value3));
            poseStack.mulPose(Axis.XP.rotationDegrees(-100.0F));
            break;
         case "Poke":
            float computedValue3 = (float)Math.sin(value * (Math.PI / 2) * 2.0);
            float computedValue4 = value3 / 3.0F;
            poseStack.translate(computedValue * 0.56F, -0.52F, -0.72F);
            poseStack.translate(0.0F, 0.0F, computedValue4 * -computedValue3);
            poseStack.mulPose(Axis.YP.rotationDegrees(75.0F * computedValue));
            poseStack.mulPose(Axis.ZP.rotationDegrees((-75.0F * (value3 / 4.0F) * computedValue3 - 60.0F) * computedValue));
            poseStack.mulPose(Axis.XP.rotationDegrees(-75.0F));
            break;
         case "Static":
            poseStack.translate(computedValue * 0.56F, -0.42F, -0.72F);
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue2 * -60.0F * value3));
            poseStack.translate(0.0F, -0.1F, 0.0F);
            break;
         case "Feast":
            poseStack.translate(computedValue * 0.56F, -0.32F, -0.72F);
            poseStack.mulPose(Axis.YP.rotationDegrees(30 * computedValue));
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * 75.0F * computedValue * value3));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue2 * -65.0F * value3));
            poseStack.mulPose(Axis.YP.rotationDegrees(30 * computedValue));
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(35 * computedValue));
            break;
         case "Smooth":
            this.applySmoothSwing(poseStack, computedValue, value, value3);
            break;
         case "Block":
            if (value > 0.0F) {
               float computedValue5 = Mth.sin(Mth.sqrt(value) * (float) Math.PI);
               poseStack.translate(0.56F * computedValue, value2 * -0.2F - 0.5F, -0.7F);
               poseStack.mulPose(Axis.YP.rotationDegrees(45 * computedValue));
               poseStack.mulPose(Axis.XP.rotationDegrees(computedValue5 * -85.0F * value3));
               poseStack.translate(-0.1F * computedValue, 0.28F, 0.2F);
               poseStack.mulPose(Axis.XP.rotationDegrees(-85.0F));
            } else {
               float computedValue6 = -0.4F * Mth.sin(Mth.sqrt(value) * (float) Math.PI);
               float computedValue7 = 0.2F * Mth.sin(Mth.sqrt(value) * (float) (Math.PI * 2));
               float computedValue8 = -0.2F * Mth.sin(value * (float) Math.PI);
               poseStack.translate(computedValue6 * computedValue * value3, computedValue7 * value3, computedValue8 * value3);
               this.applyEquipOffset(poseStack, computedValue, value2);
               this.applySmoothSwing(poseStack, computedValue, value, value3);
            }
            break;
         case "ToBack":
            float computedValue9 = Mth.sin(Mth.sqrt(value) * (float) Math.PI);
            poseStack.translate(0.65F * computedValue, -0.45F, -0.9F);
            poseStack.mulPose(Axis.XP.rotationDegrees(50.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees((-30.0F * (1.0F - computedValue9 * value3) - 30.0F) * computedValue));
            poseStack.mulPose(Axis.ZP.rotationDegrees(110.0F * computedValue));
            break;
         case "DropDown":
            float computedValue10 = (float)Math.sin(value * (Math.PI / 2) * 2.0);
            this.applyEquipOffset(poseStack, computedValue, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(80.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(this.dropDownCorner.get().floatValue()));
            poseStack.mulPose(Axis.XP.rotationDegrees(-this.dropDownSlant.get().floatValue() * computedValue10 * value3));
            break;
         case "Pander":
            float computedValue11 = Mth.sin(value * (float) Math.PI);
            float computedValue12 = 1.0F - value2;
            poseStack.translate(computedValue * 0.56F, -0.52F, -0.72F);
            poseStack.translate((0.3F - computedValue11 * 0.15F) * computedValue, 0.2F - computedValue12 * 0.12F, -0.15F - computedValue11 * 0.13F);
            poseStack.mulPose(Axis.YP.rotationDegrees((76.0F - 10.0F * computedValue11) * computedValue));
            poseStack.mulPose(Axis.ZP.rotationDegrees((-16.0F - 8.0F * computedValue11) * computedValue));
            poseStack.mulPose(Axis.XP.rotationDegrees(-83.0F - 26.0F * computedValue11));
            break;
         case "Slant":
            float computedValue13 = (float)Math.sin(value * (Math.PI / 2) * 2.0);
            float computedValue14 = 35.0F * value3;
            poseStack.translate(computedValue * 0.56F, -0.52F, -0.72F);
            poseStack.translate(0.0F, 0.0F, -0.3F * computedValue13 * value3);
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue13 * -computedValue14));
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue13 * computedValue14));
            break;
         case "Mode 2":
            float computedValue15 = Mth.sin(Mth.sqrt(value) * (float) Math.PI);
            this.applyEquipOffset(poseStack, computedValue, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(50.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue * -60.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue * (110.0F + 20.0F * value3 * computedValue15)));
            break;
         default:
            this.applySmoothSwing(poseStack, computedValue, value, value3);
      }
   }

   private String resolvedMode() {
      String text = this.animationMode.get();
      if (text != null && !text.isEmpty()) {
         return switch (text.toLowerCase(Locale.ROOT)) {
            case "mode 1" -> "Smooth";
            case "mode 2" -> "Static";
            case "mode 3" -> "Down";
            case "mode 4" -> "DropDown";
            case "mode 5" -> "Poke";
            case "mode 6" -> "Feast";
            case "mode 7" -> "ToBack";
            case "mode 8" -> "Block";
            case "mode 9" -> "Pander";
            case "mode 10" -> "Slant";
            case "mode 11" -> "Mode 2";
            case "mode 12" -> "Swipe";
            case "mode 13" -> "Soft";
            case "mode 14" -> "Thrust";
            case "mode 15" -> "Spin";
            case "mode 16" -> "Fade";
            case "mode 17" -> "Mod 11";
            case "selfback", "akrien", "break", "classic", "slash", "punch", "chop" -> "Smooth";
            default -> text;
         };
      } else {
         return "Smooth";
      }
   }

   public boolean shouldApplyCustomSwing(float value) {
      if (!this.shouldApplyAnimation()) {
         return false;
      }

      if (!this.isSourceMode()) {
         Boolean get = this.onlyDuringHit.get();
         if (get && !(value > 0.0F)) {
            return false;
         }
      }

      return true;
   }

   private boolean isMode(int... intValues) {

      for (int index : intValues) {
         String text = "Mode " + index;
         if (this.animationMode.is(text)) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldApplyAnimation() {
      if (!this.isEnabled()) {
         return false;
      }

      return !this.onlyWithAura.get() ? true : SkycoreClient.modules().get(AttackAuraModule.class).map(attackAuraModule -> {
         return attackAuraModule.isEnabled() && attackAuraModule.currentTarget() != null;
      }).orElse(false);
   }

   public boolean applyCustomSwing(PoseStack poseStack, float value, float value2, HumanoidArm humanoidArm) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         if (this.shouldApplyCustomSwing(value)) {
            HumanoidArm mainArm = player.getMainArm();
            Boolean get = this.swapHands.get();
            if (get) {
               mainArm = mainArm.getOpposite();
            }

            if (humanoidArm != mainArm) {
               return false;
            }

            this.renderSwordAnimation(poseStack, value, value2, humanoidArm);
            return true;
         }
      }

      return false;
   }

   public static float capturedEquip() {
      return CAPTURED_EQUIP.get();
   }

   public void renderSwordAnimation(PoseStack poseStack, float coordinate, float coordinate2, HumanoidArm humanoidArm) {
      if (this.isSourceMode()) {
         this.applySourceSwing(poseStack, coordinate, humanoidArm);
      } else {
         if (this.isSelfOffsetMode()) {
            this.applySelfOffsetSwing(poseStack, coordinate, coordinate2, humanoidArm);
         } else {
            this.applyOverlaySwing(poseStack, coordinate, humanoidArm);
         }
      }
   }

   public boolean isSelfOffsetMode() {
      if (this.isSourceMode()) {
         return true;
      }

      String text = this.resolvedMode();

      for (String text2 : SELF_OFFSET_MODES) {
         if (text2.equalsIgnoreCase(text)) {
            return true;
         }
      }

      return false;
   }

   public void applyCustomEatTransform(PoseStack poseStack, float value, HumanoidArm humanoidArm, ItemStack itemStack) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         float useItemRemainingTicks = instance.getUseItemRemainingTicks() - value + 1.0F;
         float useDuration = useItemRemainingTicks / itemStack.getUseDuration(instance);
         if (useDuration < 0.8F) {
            float computedValue = Math.abs(Mth.cos(useItemRemainingTicks / 4.0F * (float) Math.PI) * 0.005F);
            poseStack.translate(0.0F, computedValue, 0.0F);
         }

         float computedValue2 = 1.0F - (float)Math.pow(useDuration, 27.0);
         int computedValue3 = humanoidArm == HumanoidArm.RIGHT ? 1 : -1;
         float value2 = 0.0F;
         float value3 = 0.0F;
         float value4 = 0.0F;
         ViewModelModule iNSTANCE = ViewModelModule.INSTANCE;
         if (iNSTANCE.shouldApply()) {
            if (humanoidArm == HumanoidArm.RIGHT) {
               value2 = iNSTANCE.rightX.get().floatValue();
               value3 = iNSTANCE.rightY.get().floatValue();
               value4 = iNSTANCE.rightZ.get().floatValue();
            } else {
               value2 = iNSTANCE.leftX.get().floatValue();
               value3 = iNSTANCE.leftY.get().floatValue();
               value4 = iNSTANCE.leftZ.get().floatValue();
            }
         }

         poseStack.translate(computedValue2 * 0.6F * computedValue3 + value2, computedValue2 * -0.5F + value3, value4);
         poseStack.mulPose(Axis.YP.rotationDegrees(computedValue3 * computedValue2 * 90.0F));
         poseStack.mulPose(Axis.XP.rotationDegrees(computedValue2 * 10.0F));
         poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue3 * computedValue2 * 30.0F));
      }
   }

   public boolean isHoldMyItems() {
      if (this.isHoldMyItemsSelected()) {
         if (this.shouldApplyAnimation()) {
            return true;
         }
      }

      return false;
   }

   public boolean shouldSkipEquipOffset(HumanoidArm humanoidArm) {
      Minecraft instance = Minecraft.getInstance();
      LocalPlayer player = instance.player;
      if (player != null) {
         float value = capturedSwing();
         if (this.shouldApplyCustomSwing(value)) {
            HumanoidArm mainArm = player.getMainArm();
            if (this.swapHands.get()) {
               mainArm = mainArm.getOpposite();
            }

            return this.isSelfOffsetMode() && humanoidArm == mainArm;
         }
      }

      return false;
   }

   private void applySourceSwing(PoseStack poseStack, float value, HumanoidArm humanoidArm) {
      float computedValue = Mth.clamp(2.0F + this.sourceSmoothing.get().floatValue(), 2.1F, 6.0F);
      float clampedValue = Mth.clamp(value, 0.0F, 1.0F);
      if (computedValue != 3.0F) {
         float computedValue2 = 0.5F - Mth.cos(clampedValue * (float) Math.PI) * 0.5F;
         clampedValue = Mth.clamp(Mth.lerp((computedValue - 3.0F) / 3.0F, clampedValue, computedValue2), 0.0F, 1.0F);
      }

      float sine = (float)Math.sin(clampedValue * Math.PI);
      int value2 = humanoidArm == HumanoidArm.LEFT ? 1 : 0;
      poseStack.translate(value2 != 0 ? -0.56F : 0.56F, -0.52F, -0.72F);
      if (value2 != 0) {
         poseStack.scale(-1.0F, 1.0F, 1.0F);
      }

      float value3 = this.sourceItemSize.get().floatValue();
      poseStack.scale(value3, value3, value3);
      poseStack.translate(0.0F, 0.2F, 0.0F);
      poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
      poseStack.mulPose(Axis.ZP.rotationDegrees(85.0F));
      poseStack.mulPose(Axis.YP.rotationDegrees(-35.0F));
      poseStack.mulPose(Axis.ZN.rotationDegrees(10.0F - this.sourceTilt.get().floatValue() * 10.0F * sine));
      poseStack.mulPose(Axis.YP.rotationDegrees(85.0F));
      if (value2 != 0) {
         poseStack.scale(-1.0F, 1.0F, 1.0F);
      }
   }

   private void applyEquipOffset(PoseStack poseStack, int value, float value2) {
      poseStack.translate(value * 0.56F, -0.52F + value2 * -0.6F, -0.72F);
   }

   private SwingAnimationsModule() {
      super("Swing Animations", "Customizes attack animations", ModuleCategory.RENDER);
      this.dropDownCorner = this.add(new SliderSetting("DropDown Angle", 12.0, 1.0, 360.0, 1.0));
      this.dropDownSlant = this.add(new SliderSetting("DropDown Slant", 12.0, 1.0, 360.0, 1.0));
      this.swingPower = this.add(new SliderSetting("Swing Power", 8.0, 1.0, 10.0, 1.0));
      this.transform = this.add(new SliderSetting("Downward Swing", 0.0, 0.0, 1.0, 0.1));
      this.speed = this.add(new SliderSetting("Swing Smoothness", 11.0, 1.0, 50.0, 1.0));
      this.onlyWithAura = this.add(new BooleanSetting("Only with AttackAura", false));
      this.onlyDuringHit = this.add(new BooleanSetting("Only During Hit", true));
      this.swapHands = this.add(new BooleanSetting("Swap Hands", false));
      this.eatAnim = this.add(new BooleanSetting("Eating Animation", false));
      this.sourceItemSize = this.add(new SliderSetting("Mode 18 Item Size", 1.0, 0.5, 1.5, 0.01));
      this.sourceTilt = this.add(new SliderSetting("Mode 18 Tilt", 6.0, 1.0, 10.0, 1.0));
      this.sourceDuration = this.add(new SliderSetting("Mode 18 Swing Duration", 12.0, 1.0, 50.0, 1.0));
      this.sourceSmoothing = this.add(new SliderSetting("Mode 18 Smoothing", 1.0, 0.1, 10.0, 0.1));
      this.hmiSmoothing = this.add(new SliderSetting("HMI Swing Smoothness", 0.0, 0.0, 1.0, 0.01));
      this.hmiStrength = this.add(new SliderSetting("HMI Swing Strength", 1.0, 0.1, 2.0, 0.01));
      this.hmiItemSize = this.add(new SliderSetting("HMI Item Size", 1.0, 0.5, 1.5, 0.01));
      this.hmiChangeSpeed = this.add(new BooleanSetting("HMI Change Swing Speed", false));
      this.hmiDuration = this.add(new SliderSetting("HMI Swing Duration", 10.0, 1.0, 30.0, 1.0));
      this.swingStrength.visibleWhen(() -> {
         int[] intValues = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 10, 11};
         return this.isMode(intValues);
      });
      this.dropDownCorner.visibleWhen(() -> {
         return this.animationMode.is("Mode 4");
      });
      this.dropDownSlant.visibleWhen(() -> {
         return this.animationMode.is("Mode 4");
      });
      this.swingPower.visibleWhen(() -> {
         int[] intValues2 = new int[]{13, 16, 17};
         return this.isMode(intValues2);
      });
      this.speed.visibleWhen(this::isLegacyMode);
      this.onlyDuringHit.visibleWhen(this::isLegacyMode);
      this.transform.visibleWhen(() -> {
         int[] intValues3 = new int[]{12, 13, 14, 15, 16, 17};
         if (this.isMode(intValues3)) {
            Boolean get = this.onlyDuringHit.get();
            if (!get) {
               Boolean get2 = this.swapHands.get();
               if (!get2) {
                  return true;
               }
            }
         }

         return false;
      });
      this.swapHands.visibleWhen(() -> {
         return !this.isHoldMyItemsSelected();
      });
      this.eatAnim.visibleWhen(() -> {
         return !this.isHoldMyItemsSelected();
      });
      this.sourceItemSize.visibleWhen(this::isSourceMode);
      this.sourceTilt.visibleWhen(this::isSourceMode);
      this.sourceSmoothing.visibleWhen(this::isSourceMode);
      this.sourceDuration.visibleWhen(this::isSourceMode);
      this.hmiSmoothing.visibleWhen(this::isHoldMyItemsSelected);
      this.hmiStrength.visibleWhen(this::isHoldMyItemsSelected);
      this.hmiItemSize.visibleWhen(this::isHoldMyItemsSelected);
      this.hmiChangeSpeed.visibleWhen(this::isHoldMyItemsSelected);
      this.hmiDuration.visibleWhen(() -> {
         if (this.isHoldMyItemsSelected()) {
            if (this.hmiChangeSpeed.get()) {
               return true;
            }
         }

         return false;
      });
   }

   public boolean isHoldMyItemsSelected() {
      return this.animationMode.is("HoldMyItems");
   }

   private void applySmoothSwing(PoseStack poseStack, int value, float value2, float value3) {
      float computedValue = Mth.sin(value2 * value2 * (float) Math.PI);
      poseStack.translate(0.56F * value, -0.52F, -0.72F);
      float angle = value * (45.0F + computedValue * -20.0F * value3);
      Quaternionf rotation = Axis.YP.rotationDegrees(angle);
      poseStack.mulPose(rotation);
      double computedValue2 = Mth.sqrt(value2) * (float) Math.PI;
      float sine = Mth.sin(computedValue2);
      angle = value * sine * -20.0F * value3;
      Quaternionf rotation2 = Axis.ZP.rotationDegrees(angle);
      poseStack.mulPose(rotation2);
      angle = sine * -80.0F * value3;
      poseStack.mulPose(Axis.XP.rotationDegrees(angle));
      Quaternionf rotation3 = Axis.YP.rotationDegrees(value * -45.0F);
      poseStack.mulPose(rotation3);
   }

   public static float capturedSwing() {
      return CAPTURED_SWING.get();
   }

   private void applyOverlaySwing(PoseStack poseStack, float value, HumanoidArm humanoidArm) {
      float value2 = value;
      float computedValue = Mth.sin(Mth.sqrt(value2) * (float) Math.PI);
      int value3 = humanoidArm == HumanoidArm.LEFT ? 1 : 0;
      float computedValue2 = value3 != 0 ? -1.0F : 1.0F;
      poseStack.scale(0.82F, 0.82F, 0.82F);
      switch (this.resolvedMode()) {
         case "Swipe":
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * 45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue * -20.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue2 * computedValue * -20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue * -80.0F));
            poseStack.translate(computedValue2 * 0.4F, 0.2F, 0.2F);
            poseStack.translate(computedValue2 * -0.5F, 0.08F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * 20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-80.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * 20.0F));
            break;
         case "Soft":
            float computedValue3 = Mth.sin(value2 * value2 * (float) Math.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * (45.0F + computedValue3 * -20.0F)));
            float computedValue4 = Mth.sin(Mth.sqrt(value2) * (float) Math.PI);
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue2 * computedValue4 * -20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue4 * -this.swingPower.get().floatValue() * 10.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(-computedValue2 * 45.0F));
            break;
         case "Thrust":
            poseStack.translate(computedValue2 * (0.4F - computedValue * 0.3F), 0.0F, -computedValue * 0.2F);
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * 90.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue2 * -30.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F - 30.0F * computedValue));
            break;
         case "Spin":
            poseStack.mulPose(Axis.XP.rotationDegrees(-360.0F * value2));
            break;
         case "Fade":
            float computedValue5 = Mth.sin(value2 * value2 * (float) Math.PI);
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue2 * (45.0F + computedValue5 * -5.0F)));
            float computedValue6 = Mth.sin(Mth.sqrt(value2 * value2) * (float) Math.PI);
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue2 * computedValue6 * -20.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue6 * -(this.swingPower.get().floatValue() * 8.0F)));
            poseStack.mulPose(Axis.YP.rotationDegrees(-computedValue2 * 45.0F));
            break;
         case "Mod 11":
            float computedValue7 = this.swingPower.get().floatValue() / 6.0F;
            poseStack.translate(0.0F, 0.1F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(computedValue * -80.0F * computedValue7));
            poseStack.mulPose(Axis.ZP.rotationDegrees(computedValue * 45.0F * computedValue7));
            poseStack.mulPose(Axis.YP.rotationDegrees(computedValue * 15.0F * computedValue7));
            poseStack.translate(0.0F, -0.1F, 0.0F);
      }
   }

   public Integer customSwingDuration(InteractionHand interactionHand) {
      if (this.isHoldMyItemsSelected()) {
         return this.shouldApplyAnimation() && this.hmiChangeSpeed.get() ? this.hmiDuration.get().intValue() : null;
      } else if (!this.isSourceMode()) {
         return this.shouldApplyAnimation() && interactionHand == InteractionHand.MAIN_HAND ? Math.max(1, (int)this.speed.get().floatValue()) : null;
      } else {
         return this.shouldApplyAnimation() && interactionHand == InteractionHand.MAIN_HAND ? this.sourceDuration.get().intValue() : null;
      }
   }

   public float holdMyItemsSwing(float value) {
      Double value2 = this.hmiSmoothing.get();
      float value3 = value2.floatValue();
      return SmoothHelper.smooth(value, value3);
   }

   public boolean isSourceMode() {
      return "Mode 18".equals(this.animationMode.get());
   }
}
