package deluxe.client.event;

import net.minecraft.client.gui.DrawContext;

public class HudRenderEvent extends TickEvent {
   private final DrawContext field15211;
   private final float field15212;
   private final int field15213;
   private final int field15214;

   public DrawContext method4459() {
      return this.field15211;
   }

   public HudRenderEvent(DrawContext drawContext, float local7536_2, int local7536_3, int local7536_4) {
      this.field15211 = drawContext;
      this.field15212 = local7536_2;
      this.field15213 = local7536_3;
      this.field15214 = local7536_4;
   }

   public int method4460() {
      return this.field15214;
   }

   public float method4461() {
      return this.field15212;
   }

   public int method4462() {
      return this.field15213;
   }
}
