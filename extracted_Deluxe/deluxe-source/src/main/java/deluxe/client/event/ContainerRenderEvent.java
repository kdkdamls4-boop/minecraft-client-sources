package deluxe.client.event;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;

public class ContainerRenderEvent extends TickEvent {
   private final HandledScreen<?> field10928;
   private final DrawContext field10929;

   public HandledScreen<?> method3212() {
      return this.field10928;
   }

   public ContainerRenderEvent(HandledScreen<?> local5188_1, DrawContext drawContext) {
      this.field10928 = local5188_1;
      this.field10929 = drawContext;
   }

   public DrawContext method3213() {
      return this.field10929;
   }
}
