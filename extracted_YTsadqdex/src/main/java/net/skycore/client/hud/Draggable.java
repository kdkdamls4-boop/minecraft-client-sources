package net.skycore.client.hud;

public final class Draggable {
   private float velocityY;
   private boolean dragging;
   private float velocityX;
   private final float defaultNormalizedX;
   private final String id;
   private float y;
   private float x;
   private float height;
   private float dragOffsetY;
   private Draggable.AxisLock axisLock = Draggable.AxisLock.NONE;
   private float targetX;
   private final float defaultNormalizedY;
   private float dragOffsetX;
   private float width;
   private float targetY;

   public float y() {
      return this.y;
   }

   public String id() {
      return this.id;
   }

   public float x() {
      return this.x;
   }

   public void endDrag() {
      this.dragging = false;
   }

   public Draggable.AxisLock axisLock() {
      return this.axisLock;
   }

   public float defaultNormalizedX() {
      return this.defaultNormalizedX;
   }

   public float defaultNormalizedY() {
      return this.defaultNormalizedY;
   }

   public boolean contains(double width2, double height2) {
      if (width2 >= this.x && height2 >= this.y && width2 <= this.x + this.width) {
         if (height2 <= this.y + this.height) {
            return true;
         }
      }

      return false;
   }

   public void setBounds(float bounds, float bounds2) {
      this.width = Math.max(0.0F, bounds);
      float maximum = Math.max(0.0F, bounds2);
      this.height = maximum;
   }

   public float height() {
      return this.height;
   }

   public void setPosition(float x2, float y2) {
      this.x = x2;
      this.y = y2;
      this.targetX = x2;
      this.targetY = y2;
      this.velocityX = 0.0F;
      this.velocityY = 0.0F;
   }

   public float normalizedTargetX(int value) {
      float width2 = value;
      float computedValue = width2 - this.width;
      float maximum = Math.max(1.0F, computedValue);
      float computedValue2 = this.targetX / maximum;
      return clamp01(computedValue2);
   }

   public Draggable lockAxis(Draggable.AxisLock axisLock2) {
      Draggable.AxisLock axisLock3;
      if (axisLock2 == null) {
         axisLock3 = Draggable.AxisLock.NONE;
      } else {
         axisLock3 = axisLock2;
      }

      this.axisLock = axisLock3;
      return this;
   }

   public boolean dragging() {
      return this.dragging;
   }

   public float width() {
      return this.width;
   }

   public float targetY() {
      return this.targetY;
   }

   public Draggable(String id2, float value, float value2) {
      this.id = id2;
      this.defaultNormalizedX = clamp01(value);
      this.defaultNormalizedY = clamp01(value2);
   }

   public float targetX() {
      return this.targetX;
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   public float normalizedTargetY(int height2) {
      float maximum = Math.max(1.0F, height2 - this.height);
      return clamp01(this.targetY / maximum);
   }

   public void dragTo(double value, double value2, int width2, int value3) {
      float computedValue = width2 - this.width;
      float maximum = Math.max(0.0F, computedValue);
      float height2 = value3;
      float computedValue2 = height2 - this.height;
      float maximum2 = Math.max(0.0F, computedValue2);
      float value4 = (float)value;
      float computedValue3 = value4 - this.dragOffsetX;
      float clampedValue = clamp(computedValue3, 0.0F, maximum);
      float value5 = (float)value2;
      float computedValue4 = value5 - this.dragOffsetY;
      float clampedValue2 = clamp(computedValue4, 0.0F, maximum2);
      if (this.axisLock == Draggable.AxisLock.VERTICAL) {
         clampedValue = this.targetX;
      } else {
         if (this.axisLock == Draggable.AxisLock.HORIZONTAL) {
            clampedValue2 = this.targetY;
         }
      }

      this.targetX = clampedValue;
      this.targetY = clampedValue2;
   }

   public void setTarget(float targetX2, float targetY2) {
      this.targetX = targetX2;
      this.targetY = targetY2;
   }

   private static float clamp01(float value) {
      return clamp(value, 0.0F, 1.0F);
   }

   public void beginDrag(double value, double value2) {
      this.dragging = true;
      float value3 = (float)value;
      float x2 = value3 - this.x;
      this.dragOffsetX = x2;
      this.dragOffsetY = (float)value2 - this.y;
      this.targetX = this.x;
      this.targetY = this.y;
   }

   public void snapX(float x2) {
      this.x = x2;
      this.targetX = x2;
      this.velocityX = 0.0F;
   }

   public void updateMotion(float value) {
      float maximum = Math.max(value, 0.0F);
      float value2 = this.dragging ? 108.0F : 105.0F;
      float value3 = this.dragging ? 11.5F : 13.0F;
      float x2 = (this.targetX - this.x) * value2 - this.velocityX * value3;
      float y2 = (this.targetY - this.y) * value2 - this.velocityY * value3;
      this.velocityX += x2 * maximum;
      this.velocityY += y2 * maximum;
      this.x = this.x + this.velocityX * maximum;
      this.y = this.y + this.velocityY * maximum;
      if (Math.abs(this.targetX - this.x) < 0.01F && Math.abs(this.velocityX) < 0.02F) {
         this.x = this.targetX;
         this.velocityX = 0.0F;
      }

      if (Math.abs(this.targetY - this.y) < 0.01F && Math.abs(this.velocityY) < 0.02F) {
         this.y = this.targetY;
         this.velocityY = 0.0F;
      }
   }

   public enum AxisLock {
      NONE,
      VERTICAL,
      HORIZONTAL;
   }
}
