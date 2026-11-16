package deluxe.client.setting;

import deluxe.client.setting.TextSetting;

public final class NumericTextSetting extends TextSetting {
   private boolean field04773;

   public NumericTextSetting method1539() {
      this.field04773 = true;
      return this;
   }

   public NumericTextSetting(String name, String local2252_2, String local2252_3, int local2252_4) {
      super(name, local2252_2, local2252_3, local2252_4);
   }

   @Override
   public void insertText(String local2253_1) {
      super.insertText(local2253_1 != null && this.field04773 ? local2253_1.replaceAll("\\D", "") : local2253_1);
   }
}
