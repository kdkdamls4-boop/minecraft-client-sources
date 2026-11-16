package deluxe.client.ui.hud;

import deluxe.client.render.Render2D;

public enum KeybindAnimationSprite {
   MAIN("main", 62, 8, 8, 30.0F),
   TOGGLE("toggle", 22, 5, 5, 25.0F);

   public final String field05370;
   public final int field05371;
   public final int field05372;
   public final int field05373;
   public final float field05374;

   private KeybindAnimationSprite(String local2597_3, int local2597_4, int local2597_5, int local2597_6, float local2597_7) {
      this.field05370 = "assets/deluxe/textures/gui/hud/" + local2597_3 + ".png";
      this.field05371 = local2597_4;
      this.field05372 = local2597_5;
      this.field05373 = local2597_6;
      this.field05374 = local2597_7;
   }

   public int method1719(float local2598_1) {
      int local2598_2 = this.field05371 - 1;
      int local2598_3 = Math.round(local2598_1 * local2598_2);
      return Math.max(0, Math.min(local2598_2, local2598_3));
   }

   public void method1720(float local2599_1, float local2599_2, float local2599_3, float local2599_4, int local2599_5, float local2599_6) {
      int local2599_7 = this.method1719(local2599_6);
      int local2599_8 = local2599_7 % this.field05372;
      int local2599_9 = local2599_7 / this.field05372;
      float local2599_10 = 1.0F / this.field05372;
      float local2599_11 = 1.0F / this.field05373;
      Render2D.method4695(
         this.field05370,
         local2599_1,
         local2599_2,
         local2599_3,
         local2599_4,
         0.0F,
         local2599_8 * local2599_10,
         local2599_9 * local2599_11,
         local2599_8 * local2599_10 + local2599_10,
         local2599_9 * local2599_11 + local2599_11,
         local2599_5
      );
   }
}
