package net.skycore.client.module.player;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.SliderSetting;

public final class ItemScrollerModule extends ClientModule {
   public static final ItemScrollerModule INSTANCE = new ItemScrollerModule();
   public final SliderSetting delay = this.add(new SliderSetting("Delay", 8.0, 0.0, 10.0, 1.0));

   private ItemScrollerModule() {
      super("Item Scroller", "Quickly moves items while holding the mouse button", ModuleCategory.PLAYER);
   }
}
