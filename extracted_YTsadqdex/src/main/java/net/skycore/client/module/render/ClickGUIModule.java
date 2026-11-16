package net.skycore.client.module.render;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;

public final class ClickGUIModule extends ClientModule {
   private Runnable openAction;
   private final NumberSetting renderFactor = this.add(new NumberSetting("Render factor", "ClickGUI render scale", 0.45, 0.35, 0.75, 0.01, "x"));
   private final ModeSetting style = this.add(new ModeSetting("Style", "Transparent", "Solid", "Transparent"));

   public ModeSetting style() {
      return this.style;
   }

   public NumberSetting renderFactor() {
      return this.renderFactor;
   }

   @Override
   protected void onEnable() {
      if (this.openAction != null) {
         this.openAction.run();
      }

      this.setEnabled(false);
   }

   public void setOpenAction(Runnable openAction2) {
      this.openAction = openAction2;
   }

   public ClickGUIModule() {
      super("ClickGUI", "Opens the Skycore interface", ModuleCategory.RENDER);
      this.setKeyBind(344);
   }
}
