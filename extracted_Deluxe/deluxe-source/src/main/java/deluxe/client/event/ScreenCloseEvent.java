package deluxe.client.event;

import net.minecraft.client.gui.screen.Screen;

public class ScreenCloseEvent extends CancellableEvent {
   private final Screen field16189;

   public Screen method4839() {
      return this.field16189;
   }

   public ScreenCloseEvent(Screen screen) {
      this.field16189 = screen;
   }
}
