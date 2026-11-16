package net.skycore.client.util;

public class SpearAnimationFlags {
   public boolean lockChargeTranslation;
   public boolean useChargedPose;
   public boolean enabled;
   public boolean lockChargeRotation;

   public SpearAnimationFlags(boolean enabled2, boolean lockChargeRotation2, boolean lockChargeTranslation2, boolean useChargedPose2) {
      this.enabled = enabled2;
      this.lockChargeRotation = lockChargeRotation2;
      this.lockChargeTranslation = lockChargeTranslation2;
      this.useChargedPose = useChargedPose2;
   }
}
