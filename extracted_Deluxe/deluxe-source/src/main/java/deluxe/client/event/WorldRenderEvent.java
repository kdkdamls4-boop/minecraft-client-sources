package deluxe.client.event;

import net.minecraft.client.util.math.MatrixStack;

public final class WorldRenderEvent extends TickEvent {
   private final MatrixStack field00931;
   private final float field00932;

   public float method0646() {
      return this.field00932;
   }

   public WorldRenderEvent(MatrixStack matrixStack, float local825_2) {
      this.field00931 = matrixStack;
      this.field00932 = local825_2;
   }

   public MatrixStack method0647() {
      return this.field00931;
   }
}
