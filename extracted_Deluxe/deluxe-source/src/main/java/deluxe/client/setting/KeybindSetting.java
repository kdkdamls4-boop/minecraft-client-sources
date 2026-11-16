package deluxe.client.setting;

import deluxe.client.util.InputBinding;

public final class KeybindSetting extends Setting<KeybindSetting> {
   private InputBinding binding;
   private KeybindSetting.ActivationMode activationMode = KeybindSetting.ActivationMode.HOLD;

   public KeybindSetting(String name, String local3106_2, InputBinding inputBinding) {
      super(name, local3106_2);
      this.binding = inputBinding == null ? InputBinding.UNBOUND : inputBinding;
   }

   public KeybindSetting withActivationMode(KeybindSetting.ActivationMode newActivationMode) {
      this.activationMode = newActivationMode;
      return this;
   }

   public KeybindSetting withKeyCode(int newCode) {
      return this.setBinding(newCode == -1 ? InputBinding.UNBOUND : InputBinding.keyboardKey(newCode));
   }

   public KeybindSetting(String name, String local3109_2) {
      this(name, local3109_2, InputBinding.UNBOUND);
   }

   public String getKeyName() {
      return this.binding.getDisplayName();
   }

   public int getKeyCode() {
      return this.binding.getCode();
   }

   public InputBinding getBinding() {
      return this.binding;
   }

   public boolean isBound() {
      return this.binding.isBound();
   }

   public KeybindSetting setBinding(InputBinding inputBinding) {
      this.binding = inputBinding == null ? InputBinding.UNBOUND : inputBinding;
      this.notifyChanged();
      return this;
   }

   public KeybindSetting.ActivationMode getActivationMode() {
      return this.activationMode;
   }

   public enum ActivationMode {
      HOLD,
      TOGGLE;
   }
}
