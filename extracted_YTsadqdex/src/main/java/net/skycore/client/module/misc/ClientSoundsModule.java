package net.skycore.client.module.misc;

import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.ModeSetting;

public final class ClientSoundsModule extends ClientModule {
   private static final long SOUND_BATCH_NS = 90000000L;
   private final ModeSetting disableEffect;
   private final ModeSetting sound;
   private static final String[] SOUND_MODES = new String[]{"Mode 1", "Mode 2", "Mode 3", "Mode 4", "Mode 5", "Mode 6"};
   private long lastSoundAt;
   private final NumberSetting volume;

   public ClientSoundsModule() {
      super("Client Sounds", "Plays sounds when modules are toggled", ModuleCategory.MISC);
      this.sound = this.add(new ModeSetting("Sound", SOUND_MODES[0], SOUND_MODES));
      this.volume = this.add(new NumberSetting("Volume", "", 70.0, 0.0, 100.0, 1.0, "%"));
      this.disableEffect = this.add(new ModeSetting("Disable Effect", "Pitch Down", "Pitch Down", "Deeper", "Soft Fade"));
      this.setEnabled(true);
   }

   public void onModuleToggle(ClientModule clientModule, boolean condition) {
   }

   private int soundModeIndex() {
      String text = this.sound.get();
      int value = 0;

      while (true) {
         if (value >= SOUND_MODES.length) {
            return 0;
         }

         String text2 = SOUND_MODES[value];
         if (text2.equals(text)) {
            return value;
         }

         value++;
      }
   }

   private float disablePitch() {

      return switch ((String)this.disableEffect.get()) {
         case "Deeper" -> 0.5F;
         case "Soft Fade" -> 0.68F;
         default -> 0.58F;
      };
   }
}
