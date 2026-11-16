package deluxe.client.render;

public final class GuiEffectsState {
   private static float field13715 = 24.0F;
   private static float field13716 = 1.0F;
   private static boolean field13717;
   private static boolean field13718;

   public static void method3973(boolean local6638_0) {
      field13717 = local6638_0;
   }

   public static boolean method3974() {
      return field13718;
   }

   public static void method3975(float local6640_0) {
      field13715 = Math.max(0.0F, local6640_0);
   }

   public static void method3976(boolean local6641_0) {
      field13718 = local6641_0;
   }

   public static float method3977() {
      return field13716;
   }

   private GuiEffectsState() {
   }

   public static boolean method3978() {
      return field13717;
   }

   public static float method3979() {
      return field13715;
   }

   public static void method3980(float local6647_0) {
      field13716 = Math.max(0.0F, Math.min(1.0F, local6647_0));
   }
}
