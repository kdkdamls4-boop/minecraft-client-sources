package deluxe.client.util;

import deluxe.client.MinecraftAccess;

public final class MouseSensitivity implements MinecraftAccess {
   public static float method4408() {
      return (float)(method4418() * 0.15);
   }

   public static float method4409(float local7424_0) {
      return local7424_0 / method4416() * method4416();
   }

   public static float method4410(float local7425_0) {
      return method4414(local7425_0) * method4408();
   }

   public float method4411() {
      return (float)(Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 8.0 * 0.15);
   }

   public static float method4412(float local7427_0) {
      return method4414(local7427_0) == 0.0F && Math.abs(local7427_0) >= method4408() * 0.2F
         ? Math.signum(local7427_0) * method4408()
         : method4410(local7427_0);
   }

   public static float method4413(float local7428_0) {
      return Math.round(local7428_0 / method4416()) * method4416();
   }

   public static float method4414(float local7429_0) {
      return Math.round(local7429_0 / method4408());
   }

   public static float method4415() {
      return (float)(Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 0.78);
   }

   public static float method4416() {
      return method4415() * 0.15F;
   }

   public float method4417(float local7433_1) {
      return Math.round(local7433_1 / this.method4411()) * this.method4411();
   }

   public static float method4418() {
      double value = mc.options.getMouseSensitivity().getValue();
      return (float)(Math.pow(value * 0.6F + 0.2F, 3.0) * 8.0);
   }
}
