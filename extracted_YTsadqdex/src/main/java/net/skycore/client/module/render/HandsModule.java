package net.skycore.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ColorSetting;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.UpdateTrailHelper;

public final class HandsModule extends ClientModule {
   private final SliderSetting trailBlur;
   private final SliderSetting speed;
   private final SliderSetting handBlur;
   private final ModeSetting colorMode;
   private final UpdateTrailHelper renderer;
   private final ColorSetting customColor;
   private boolean mainSwingArmed;
   private final SliderSetting smoke;
   private float slashImpulse;
   private long lastPulseNs;
   private final SliderSetting trailFade;
   private float swingImpulse;
   private final SliderSetting length;
   private final SliderSetting handSoftness;
   private final SliderSetting rainbowScale;
   private final SliderSetting rainbowSpeed;
   private final SliderSetting intensity = this.add(new SliderSetting("Intensity", 0.85, 0.1, 1.5, 0.05));
   private final SliderSetting handFade;
   private boolean offSwingArmed;
   public static final HandsModule INSTANCE = new HandsModule();
   private final SliderSetting trailSoftness;
   private float slashDirection;
   private static final long PULSE_DEBOUNCE_NS = 180000000L;

   private void pulse(InteractionHand interactionHand) {
      long nowNanos = System.nanoTime();
      if (nowNanos - this.lastPulseNs >= 180000000L) {
         this.lastPulseNs = nowNanos;
         this.swingImpulse = 1.0F;
         this.slashImpulse = 1.0F;
         Minecraft instance = Minecraft.getInstance();
         HumanoidArm mainArm = instance.player != null ? instance.player.getMainArm() : HumanoidArm.RIGHT;
         HumanoidArm opposite = interactionHand == InteractionHand.MAIN_HAND ? mainArm : mainArm.getOpposite();
         this.slashDirection = opposite == HumanoidArm.RIGHT ? 1.0F : -1.0F;
      }
   }

   @Override
   protected void onEnable() {
      this.slashImpulse = 0.0F;
      this.swingImpulse = 0.0F;
      this.slashDirection = 1.0F;
      this.lastPulseNs = 0L;
      this.mainSwingArmed = this.offSwingArmed = true;
      this.renderer.resetHistory();
   }

   public float trailSoftness() {
      return this.trailSoftness.get().floatValue();
   }

   public float slashDirection() {
      return this.slashDirection;
   }

   public float length() {
      Double value = this.length.get();
      return value.floatValue();
   }

   public boolean rainbow() {
      return this.colorMode.is("Rainbow");
   }

   public float trailFade() {
      return this.trailFade.get().floatValue();
   }

   public float rainbowScale() {
      return this.rainbowScale.get().floatValue();
   }

   public float rainbowSpeed() {
      return this.rainbowSpeed.get().floatValue();
   }

   public void advancePulse(float value) {
      float computedValue = Math.max(0.25F, Math.min(3.0F, value * 60.0F));
      float swingImpulse2 = this.swingImpulse;
      Double value2 = this.handFade.get();
      this.swingImpulse = swingImpulse2 * (float)Math.pow(value2, computedValue);
      float slashImpulse2 = this.slashImpulse;
      double value3 = this.trailFade.get();
      double value4 = computedValue;
      float computedValue2 = slashImpulse2 * (float)Math.pow(value3, value4);
      this.slashImpulse = computedValue2;
      if (this.swingImpulse < 0.01F) {
         this.swingImpulse = 0.0F;
      }

      if (this.slashImpulse < 0.01F) {
         this.slashImpulse = 0.0F;
      }
   }

   public boolean custom() {
      return this.colorMode.is("Custom");
   }

   public float trailBlur() {
      Double value = this.trailBlur.get();
      return value.floatValue();
   }

   private HandsModule() {
      super("Hands", "Customizable hand smoke, swing trails and color effects", ModuleCategory.RENDER);
      this.speed = this.add(new SliderSetting("Speed", 1.15, 0.2, 3.0, 0.05));
      this.length = this.add(new SliderSetting("Length", 0.55, 0.1, 1.0, 0.05));
      this.trailFade = this.add(new SliderSetting("Trail Fade", 0.84, 0.55, 0.96, 0.01));
      this.trailSoftness = this.add(new SliderSetting("Trail Softness", 1.35, 0.45, 2.5, 0.05));
      this.trailBlur = this.add(new SliderSetting("Trail Blur", 1.55, 0.2, 3.0, 0.05));
      this.handFade = this.add(new SliderSetting("Hand Fade", 0.68, 0.45, 0.9, 0.01));
      this.handSoftness = this.add(new SliderSetting("Hand Softness", 1.3, 0.45, 2.5, 0.05));
      this.handBlur = this.add(new SliderSetting("Hand Blur", 1.45, 0.2, 3.0, 0.05));
      this.smoke = this.add(new SliderSetting("Smoke", 0.55, 0.0, 0.8, 0.05));
      this.colorMode = this.add(new ModeSetting("Color Mode", "Item", "Item", "Custom", "Rainbow"));
      this.customColor = this.add(new ColorSetting("Custom Color", -10079233, false));
      this.rainbowSpeed = this.add(new SliderSetting("Rainbow Speed", 1.0, 0.1, 3.0, 0.05));
      this.rainbowScale = this.add(new SliderSetting("Rainbow Scale", 1.0, 0.25, 3.0, 0.05));
      this.renderer = new UpdateTrailHelper(this);
      this.slashDirection = 1.0F;
      this.mainSwingArmed = true;
      this.offSwingArmed = true;
      this.customColor.visibleWhen(() -> {
         return this.colorMode.is("Custom");
      });
      this.rainbowSpeed.visibleWhen(() -> {
         return this.colorMode.is("Rainbow");
      });
      this.rainbowScale.visibleWhen(() -> {
         return this.colorMode.is("Rainbow");
      });
   }

   public float smoke() {
      Double value = this.smoke.get();
      return value.floatValue();
   }

   public float speed() {
      Double value = this.speed.get();
      return value.floatValue();
   }

   public float slash() {
      return this.slashImpulse;
   }

   public float handBlur() {
      return this.handBlur.get().floatValue();
   }

   public float intensity() {
      return this.intensity.get().floatValue();
   }

   public void observeSwing(InteractionHand interactionHand, float value) {
      if (this.isEnabled()) {
         int value2 = interactionHand == InteractionHand.MAIN_HAND ? 1 : 0;
         boolean condition = value2 != 0 ? this.mainSwingArmed : this.offSwingArmed;
         if (value < 0.08F || value > 0.95F) {
            condition = true;
         } else if (value >= 0.19F && condition) {
            condition = false;
            this.pulse(interactionHand);
         }

         if (value2 != 0) {
            this.mainSwingArmed = (boolean)condition;
         } else {
            this.offSwingArmed = (boolean)condition;
         }
      }
   }

   public UpdateTrailHelper renderer() {
      return this.renderer;
   }

   public void onHit() {
      if (this.isEnabled()) {
         this.pulse(InteractionHand.MAIN_HAND);
      }
   }

   public float handSoftness() {
      return this.handSoftness.get().floatValue();
   }

   @Override
   protected void onDisable() {
      this.slashImpulse = 0.0F;
      this.swingImpulse = 0.0F;
      this.lastPulseNs = 0L;
      this.renderer.release();
   }

   public int color() {
      if (this.colorMode.is("Custom")) {
         return this.customColor.get();
      }

      if (this.rainbow()) {
         return -1;
      }

      Minecraft instance = Minecraft.getInstance();
      if (instance.player == null) {
         return -1;
      }

      ItemStack mainHandItem = instance.player.getMainHandItem();
      if (mainHandItem.isEmpty()) {
         mainHandItem = instance.player.getOffhandItem();
      }

      if (mainHandItem.isEmpty()) {
         return -1;
      } else {
         String path = BuiltInRegistries.ITEM.getKey(mainHandItem.getItem()).getPath();
         if (path.contains("diamond")) {
            return -11149856;
         } else if (path.contains("netherite")) {
            return -10794393;
         } else if (path.contains("gold")) {
            return -11174;
         } else if (path.contains("iron")) {
            return -2564376;
         } else if (path.contains("emerald")) {
            return -13250449;
         } else if (path.contains("redstone")) {
            return -1950917;
         } else {
            return path.contains("lapis") ? -13543724 : -1644826;
         }
      }
   }

   public float activity() {
      return this.swingImpulse;
   }
}
