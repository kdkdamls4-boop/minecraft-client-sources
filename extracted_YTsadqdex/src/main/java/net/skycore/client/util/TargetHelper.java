package net.skycore.client.util;

public record TargetHelper(GuiActionType kind, Object target, String value, float x, float y, float w, float h, Object panel) {
   public float x() {
      return this.x;
   }

   public String value() {
      return this.value;
   }

   public float y() {
      return this.y;
   }

   public float h() {
      return this.h;
   }

   public GuiActionType kind() {
      return this.kind;
   }

   public Object panel() {
      return this.panel;
   }

   public float w() {
      return this.w;
   }

   public Object target() {
      return this.target;
   }
}
