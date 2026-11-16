package net.skycore.client.module.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.setting.ActionSetting;
import net.skycore.client.util.SliderSetting;
import net.skycore.client.util.MouseClickedHelper;
import net.skycore.client.setting.BooleanSetting;

public final class ViewModelModule extends ClientModule {
   private volatile HumanoidArm hoveredArm;
   public final SliderSetting leftX = this.add(new SliderSetting("Left Hand X", 0.0, -2.0, 2.0, 0.01));
   public final ActionSetting resetRight;
   public final SliderSetting rightX;
   public static final ViewModelModule INSTANCE = new ViewModelModule();
   public final ActionSetting configureInWorld;
   public final SliderSetting leftZ;
   private volatile boolean dragging;
   public final SliderSetting leftY = this.add(new SliderSetting("Left Hand Y", 0.0, -2.0, 2.0, 0.01));
   public final SliderSetting rightY;
   public final BooleanSetting onlyWithAura;
   private final ViewModelModule.EditorSpring rightEditorSpring;
   public final SliderSetting rightZ;
   private final ViewModelModule.EditorSpring leftEditorSpring;
   public final ActionSetting resetLeft;
   private volatile boolean worldEditing;

   public boolean shouldApply() {
      if (!this.isEnabled()) {
         return false;
      }

      if (this.worldEditing) {
         return true;
      }

      if (!this.onlyWithAura.get()) {
         return true;
      }

      var optional = SkycoreClient.modules().get(AttackAuraModule.class);
      var optional2 = optional.map(attackAuraModule -> {
         return attackAuraModule.isEnabled() && attackAuraModule.currentTarget() != null;
      });
      Boolean condition = false;
      Boolean orElse = optional2.orElse(condition);
      return orElse;
   }

   private void resetRightHand() {
      this.rightX.reset();
      this.rightY.reset();
      this.rightZ.reset();
   }

   private void resetLeftHand() {
      this.leftX.reset();
      this.leftY.reset();
      this.leftZ.reset();
   }

   public boolean isWorldEditing() {
      return this.worldEditing;
   }

   @Override
   protected void onDisable() {
      if (this.worldEditing) {
         this.setWorldEditing(false);
         Minecraft instance = Minecraft.getInstance();
         if (instance != null) {
            if (instance.gui.screen() instanceof MouseClickedHelper) {
               instance.setScreenAndShow(null);
            }
         }
      }

      super.onDisable();
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public void applyHandPosition(PoseStack poseStack, HumanoidArm humanoidArm) {
      if (this.shouldApply()) {
         float value;
         float value2;
         float value3;
         if (humanoidArm == HumanoidArm.RIGHT) {
            value = this.rightX.get().floatValue();
            value2 = this.rightY.get().floatValue();
            value3 = this.rightZ.get().floatValue();
            if (this.worldEditing) {
               this.rightEditorSpring.update(value, value2, value3, this.dragging && this.hoveredArm == humanoidArm);
               value = this.rightEditorSpring.x;
               value2 = this.rightEditorSpring.y;
               value3 = this.rightEditorSpring.z;
            }
         } else {
            value = this.leftX.get().floatValue();
            value2 = this.leftY.get().floatValue();
            value3 = this.leftZ.get().floatValue();
            if (this.worldEditing) {
               this.leftEditorSpring.update(value, value2, value3, this.dragging && this.hoveredArm == humanoidArm);
               value = this.leftEditorSpring.x;
               value2 = this.leftEditorSpring.y;
               value3 = this.leftEditorSpring.z;
            }
         }

         poseStack.translate(value, value2, value3);
      }
   }

   public HumanoidArm hoveredArm() {
      return this.hoveredArm;
   }

   public void setWorldEditing(boolean worldEditing2) {
      this.worldEditing = worldEditing2;
      if (worldEditing2) {
         ViewModelModule.EditorSpring leftEditorSpring2 = this.leftEditorSpring;
         Double value = this.leftX.get();
         float value2 = value.floatValue();
         float value3 = this.leftY.get().floatValue();
         leftEditorSpring2.reset(value2, value3, this.leftZ.get().floatValue());
         ViewModelModule.EditorSpring rightEditorSpring2 = this.rightEditorSpring;
         Double value4 = this.rightX.get();
         float value5 = value4.floatValue();
         float value6 = this.rightY.get().floatValue();
         Double value7 = this.rightZ.get();
         rightEditorSpring2.reset(value5, value6, value7.floatValue());
      } else {
         this.hoveredArm = null;
         this.dragging = false;
      }
   }

   public static void openWorldEditor(boolean condition) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         Runnable runnable = () -> {
            ViewModelModule iNSTANCE = INSTANCE;
            if (!iNSTANCE.isEnabled()) {
               iNSTANCE.toggle();
            }

            iNSTANCE.setWorldEditing(true);
            iNSTANCE.setHoveredArm(null);
            iNSTANCE.setDragging(false);
            instance.setScreenAndShow(new MouseClickedHelper(condition));
         };
         instance.execute(runnable);
      }
   }

   public void nudge(HumanoidArm humanoidArm, double value, double value2, double value3) {
      if (humanoidArm == HumanoidArm.LEFT) {
         SliderSetting leftX2 = this.leftX;
         double intermediate = this.leftX.get() + value;
         leftX2.set(intermediate);
         leftX2 = this.leftY;
         Double value4 = this.leftY.get();
         leftX2.set(value4 + value2);
         leftX2 = this.leftZ;
         value4 = this.leftZ.get();
         double computedValue = value4 + value3;
         leftX2.set(computedValue);
      } else {
         SliderSetting rightX2 = this.rightX;
         Double value5 = this.rightX.get() + value;
         rightX2.set(value5);
         rightX2 = this.rightY;
         double computedValue2 = this.rightY.get() + value2;
         rightX2.set(computedValue2);
         this.rightZ.set(this.rightZ.get() + value3);
      }
   }

   private ViewModelModule() {
      super("View Model", "Configures first-person hand positions", ModuleCategory.RENDER);
      this.leftZ = this.add(new SliderSetting("Left Hand Z", 0.0, -2.0, 2.0, 0.01));
      this.rightX = this.add(new SliderSetting("Right Hand X", 0.0, -2.0, 2.0, 0.01));
      this.rightY = this.add(new SliderSetting("Right Hand Y", 0.0, -2.0, 2.0, 0.01));
      this.rightZ = this.add(new SliderSetting("Right Hand Z", 0.0, -2.0, 2.0, 0.01));
      this.onlyWithAura = this.add(new BooleanSetting("Only with AttackAura", false));
      this.resetLeft = this.add(new ActionSetting("Reset Left Hand").action(this::resetLeftHand));
      this.resetRight = this.add(new ActionSetting("Reset Right Hand").action(this::resetRightHand));
      this.configureInWorld = this.add(new ActionSetting("Configure in World").action(() -> {
         openWorldEditor(true);
      }));
      this.leftEditorSpring = new ViewModelModule.EditorSpring();
      this.rightEditorSpring = new ViewModelModule.EditorSpring();
   }

   public void setDragging(boolean dragging2) {
      this.dragging = dragging2;
   }

   public void nudgeZ(HumanoidArm humanoidArm, double value) {
      this.nudge(humanoidArm, 0.0, 0.0, value);
   }

   public void setHoveredArm(HumanoidArm hoveredArm2) {
      this.hoveredArm = hoveredArm2;
   }

   public boolean isDragging() {
      return this.dragging;
   }

   static final class EditorSpring {
      private float y;
      private float x;
      private float velocityY;
      private long lastAt = System.nanoTime();
      private float accumulator;
      private float z;
      private float velocityX;
      private float velocityZ;
      private static final float FIXED_STEP = 0.008333334F;

      private void reset(float x2, float y2, float z2) {
         this.x = x2;
         this.y = y2;
         this.z = z2;
         this.velocityX = 0.0F;
         this.velocityY = 0.0F;
         this.velocityZ = 0.0F;
         this.accumulator = 0.0F;
         this.lastAt = System.nanoTime();
      }

      private void update(float value, float value2, float value3, boolean condition) {
         long nowNanos = System.nanoTime();
         float index = ViewModelModule.clamp((float)(nowNanos - this.lastAt) / 1.0E9F, 0.0F, 0.05F);
         this.lastAt = nowNanos;
         float index2 = this.accumulator + index;

         for (this.accumulator = index2; this.accumulator >= 0.008333334F; this.accumulator -= 0.008333334F) {
            this.step(value, value2, value3, condition, 0.008333334F);
         }
      }

      private void step(float x2, float y2, float z2, boolean condition, float value) {
         float value2 = condition ? 108.0F : 105.0F;
         float value3 = condition ? 11.5F : 13.0F;
         float x3 = (x2 - this.x) * value2 - this.velocityX * value3;
         float y3 = (y2 - this.y) * value2 - this.velocityY * value3;
         float z3 = (z2 - this.z) * value2 - this.velocityZ * value3;
         this.velocityX += x3 * value;
         this.velocityY += y3 * value;
         this.velocityZ += z3 * value;
         this.x = this.x + this.velocityX * value;
         this.y = this.y + this.velocityY * value;
         this.z = this.z + this.velocityZ * value;
         if (Math.abs(x2 - this.x) < 1.0E-4F && Math.abs(this.velocityX) < 2.0E-4F) {
            this.x = x2;
            this.velocityX = 0.0F;
         }

         if (Math.abs(y2 - this.y) < 1.0E-4F && Math.abs(this.velocityY) < 2.0E-4F) {
            this.y = y2;
            this.velocityY = 0.0F;
         }

         if (Math.abs(z2 - this.z) < 1.0E-4F && Math.abs(this.velocityZ) < 2.0E-4F) {
            this.z = z2;
            this.velocityZ = 0.0F;
         }
      }

      private EditorSpring() {
      }
   }
}
