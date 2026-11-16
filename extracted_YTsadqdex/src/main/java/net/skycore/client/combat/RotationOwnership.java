package net.skycore.client.combat;

public final class RotationOwnership {
   private int ownerPriority = Integer.MIN_VALUE;
   private float yaw;
   private Object correctionOwner;
   private boolean freeCorrection;
   private float pitch;
   private static final RotationOwnership INSTANCE = new RotationOwnership();
   public static final int CRYSTAL_AURA_PRIORITY = 3000;
   private Object owner;
   private int correctionPriority = Integer.MIN_VALUE;

   public static RotationOwnership get() {
      return INSTANCE;
   }

   public void clearCorrection(Object value) {
      if (value != null && this.correctionOwner == value) {
         this.correctionOwner = null;
         this.correctionPriority = Integer.MIN_VALUE;
         this.freeCorrection = false;
      }
   }

   public boolean isTakenByHigherPriority(int value) {
      return this.owner != null && this.ownerPriority > value;
   }

   public boolean shouldCorrectMovement(Object value) {
      if (value != null) {
         if (this.correctionOwner == value) {
            if (this.freeCorrection) {
               return true;
            }
         }
      }

      return false;
   }

   public float getPitch() {
      return this.pitch;
   }

   public boolean shouldSyncMotion() {
      return this.owner != null;
   }

   public void set(Object owner2, int ownerPriority2, float yaw2, float pitch2) {
      if (owner2 != null) {
         if (this.owner != owner2 && this.owner != null) {
            if (ownerPriority2 < this.ownerPriority) {
               return;
            }
         }

         this.owner = owner2;
         this.ownerPriority = ownerPriority2;
         this.yaw = yaw2;
         this.pitch = pitch2;
      }
   }

   public void requestCorrection(Object correctionOwner2, int correctionPriority2, boolean freeCorrection2) {
      if (correctionOwner2 != null) {
         if (this.correctionOwner == correctionOwner2 || this.correctionOwner == null || correctionPriority2 >= this.correctionPriority) {
            this.correctionOwner = correctionOwner2;
            this.correctionPriority = correctionPriority2;
            this.freeCorrection = freeCorrection2;
         }
      }
   }

   public boolean isOwnedBy(Object value) {
      if (value != null) {
         if (this.owner == value) {
            return true;
         }
      }

      return false;
   }

   public void release(Object progress) {
      if (progress != null) {
         if (this.owner == progress) {
            this.owner = null;
            this.ownerPriority = Integer.MIN_VALUE;
         }

         this.clearCorrection(progress);
      }
   }

   private RotationOwnership() {
   }

   public float getYaw() {
      return this.yaw;
   }
}
