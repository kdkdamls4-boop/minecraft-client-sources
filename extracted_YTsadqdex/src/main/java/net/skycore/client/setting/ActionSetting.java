package net.skycore.client.setting;

import net.skycore.client.setting.Setting;

public final class ActionSetting extends Setting<Boolean> {
   private Runnable action = () -> {};
   private String actionLabel;

   public ActionSetting(String text) {
      super(text, Boolean.FALSE);
   }

   public boolean hasSeparateLabel() {
      return this.actionLabel != null && !this.actionLabel.isEmpty();
   }

   public ActionSetting action(Runnable runnable) {
      Runnable runnable2;
      if (runnable == null) {
         runnable2 = () -> {
         };
      } else {
         runnable2 = runnable;
      }

      this.action = runnable2;
      return this;
   }

   public ActionSetting(String text, String text2) {
      super(text, text2, Boolean.FALSE);
   }

   public ActionSetting actionLabel(String actionLabel2) {
      this.actionLabel = actionLabel2;
      return this;
   }

   public void press() {
      this.action.run();
   }

   public String getActionLabel() {
      if (this.actionLabel != null) {
         if (!this.actionLabel.isEmpty()) {
            return this.actionLabel;
         }
      }

      return this.getName();
   }
}
