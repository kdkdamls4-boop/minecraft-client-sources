package net.skycore.client.module.render;

import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.skycore.client.SkycoreClient;
import net.skycore.client.combat.RotationController;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;

public final class InterpolateF5Module extends ClientModule {
   private static final float DISTANCE_SPEED = 0.13F;
   private float prevDistance;
   private float prevHeightOffset;
   private static final float SNEAK_OFFSET = 0.5F;
   private static final float ANIM_SPEED = 0.13F;
   private static final float JUMP_MULTIPLIER = 2.0F;
   private static final float ROTATION_SMOOTH = 0.28F;
   private static final float CAMERA_DISTANCE = 4.1F;
   private float currentDistance;
   private float currentYaw;
   private float currentPitch;
   private float prevPitch;
   private float heightOffset;
   private boolean wasThirdPerson;
   private boolean switchAnimating;
   private boolean needsInit = true;
   private float prevYaw;
   private static final float SWITCH_ANIM_SPEED = 0.26F;

   private static float clamp01(float value) {
      return Mth.clamp(value, 0.0F, 1.0F);
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null && instance.level != null) {
         int cameraType = !instance.options.getCameraType().isFirstPerson() ? 1 : 0;
         if (cameraType != 0 && !this.wasThirdPerson) {
            this.initCamera(true);
         }

         if (cameraType == 0 && this.wasThirdPerson) {
            this.needsInit = true;
            this.switchAnimating = false;
         }

         this.wasThirdPerson = (cameraType != 0);
         if (cameraType != 0) {
            this.updateCamera();
         }
      } else {
         this.needsInit = true;
         this.wasThirdPerson = false;
      }
   }

   private float referenceYaw(LocalPlayer player) {
      return RotationController.isActive() ? RotationController.getFreeYaw() : player.getYRot();
   }

   public InterpolateF5Module() {
      super("InterpolateF5", "Smooth F5 camera transitions", ModuleCategory.RENDER);
   }

   public static InterpolateF5Module enabled() {
      ModuleManager moduleManager = SkycoreClient.modules();
      var optional = moduleManager.get(InterpolateF5Module.class).filter(ClientModule::isEnabled);
      return (InterpolateF5Module)optional.orElse(null);
   }

   private void updateCamera() {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         if (this.needsInit) {
            this.initCamera(true);
         } else {
            this.prevYaw = this.currentYaw;
            this.prevPitch = this.currentPitch;
            this.prevDistance = this.currentDistance;
            this.prevHeightOffset = this.heightOffset;
            this.currentYaw = this.currentYaw + Mth.wrapDegrees(this.referenceYaw(instance) - this.currentYaw) * 0.28F;
            this.currentPitch = Mth.clamp(this.currentPitch + (this.referencePitch(instance) - this.currentPitch) * 0.28F, -90.0F, 90.0F);
            float distance = this.switchAnimating ? 0.26F : 0.13F;
            this.currentDistance = this.currentDistance + (4.1F - this.currentDistance) * distance;
            if (this.switchAnimating && Math.abs(4.1F - this.currentDistance) <= 0.02F) {
               this.currentDistance = 4.1F;
               this.switchAnimating = false;
            }

            float height = 0.0F;
            if (instance.isShiftKeyDown()) {
               height = -0.5F;
            }

            if (!instance.onGround()) {
               height += (float)(-instance.getDeltaMovement().y * 2.0);
            }

            this.heightOffset = this.heightOffset + (height - this.heightOffset) * 0.13F;
         }
      }
   }

   @Override
   protected void onEnable() {
      this.needsInit = true;
      this.wasThirdPerson = false;
      Minecraft instance = Minecraft.getInstance();
      if (instance.player != null) {
         CameraType cameraType = instance.options.getCameraType();
         if (!cameraType.isFirstPerson()) {
            this.initCamera(true);
            this.wasThirdPerson = true;
         }
      }
   }

   private float referencePitch(LocalPlayer player) {
      float pitch;
      if (RotationController.isActive()) {
         pitch = RotationController.getFreePitch();
      } else {
         pitch = player.getXRot();
      }

      return pitch;
   }

   @Override
   protected void onDisable() {
      this.needsInit = true;
      this.wasThirdPerson = false;
      this.switchAnimating = false;
      this.currentDistance = 0.0F;
      this.prevDistance = 0.0F;
      this.heightOffset = 0.0F;
      this.prevHeightOffset = 0.0F;
   }

   public float getInterpolatedHeightOffset(float value) {
      this.ensureInitialized();
      return this.prevHeightOffset + (this.heightOffset - this.prevHeightOffset) * clamp01(value);
   }

   private void ensureInitialized() {
      Minecraft instance = Minecraft.getInstance();
      if (this.needsInit && instance.player != null) {
         if (!instance.options.getCameraType().isFirstPerson()) {
            this.initCamera(true);
            this.wasThirdPerson = true;
         }
      }
   }

   public float getInterpolatedPitch(float value) {
      this.ensureInitialized();
      float computedValue = this.currentPitch - this.prevPitch;
      float computedValue2 = this.prevPitch + computedValue * clamp01(value);
      return Mth.clamp(computedValue2, -90.0F, 90.0F);
   }

   public float getInterpolatedDistance(float value) {
      this.ensureInitialized();
      return this.prevDistance + (this.currentDistance - this.prevDistance) * clamp01(value);
   }

   private void initCamera(boolean switchAnimating2) {
      LocalPlayer instance = Minecraft.getInstance().player;
      if (instance != null) {
         float yaw = this.referenceYaw(instance);
         this.prevYaw = yaw;
         this.currentYaw = yaw;
         yaw = this.referencePitch(instance);
         this.prevPitch = yaw;
         this.currentPitch = yaw;
         yaw = switchAnimating2 ? 0.0F : 4.1F;
         this.prevDistance = yaw;
         this.currentDistance = yaw;
         float height = this.prevHeightOffset = 0.0F;
         this.heightOffset = height;
         this.switchAnimating = switchAnimating2;
         this.needsInit = false;
      }
   }

   public float getInterpolatedYaw(float value) {
      this.ensureInitialized();
      return this.prevYaw + (this.currentYaw - this.prevYaw) * clamp01(value);
   }
}
