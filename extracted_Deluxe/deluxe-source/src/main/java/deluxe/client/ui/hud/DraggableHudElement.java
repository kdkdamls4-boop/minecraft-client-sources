package deluxe.client.ui.hud;

import deluxe.client.util.Animation;

public abstract class DraggableHudElement {
   public final Animation visibilityAnimation = new Animation();
   public boolean visible;
   public float yOffset;
   public int sortIndex;
   public boolean initialized;
}
