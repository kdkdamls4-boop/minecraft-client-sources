package deluxe.client.util;

import deluxe.client.MinecraftAccess;

public final class RotationQuantizer implements MinecraftAccess {
   private float remainder;

   public void reset() {
      this.remainder = 0.0F;
   }

   public float quantize(float local8002_1) {
      float local8002_2 = MouseSensitivity.method4408();
      float local8002_3 = local8002_1 + this.remainder;
      float local8002_4 = Math.round(local8002_3 / local8002_2) * local8002_2;
      this.remainder = local8002_3 - local8002_4;
      return local8002_4;
   }
}
