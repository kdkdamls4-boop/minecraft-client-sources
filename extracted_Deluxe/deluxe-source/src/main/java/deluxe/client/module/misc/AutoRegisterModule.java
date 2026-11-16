package deluxe.client.module.misc;

import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.setting.NumericTextSetting;

import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;

@ModuleInfo(name = "AutoRegister", description = "Автоматически вводит пароль при запросе регистрации или входа.", category = ModuleCategory.MISC)
public final class AutoRegisterModule extends Module {
   private static AutoRegisterModule autoRegisterModule;
   public NumericTextSetting field09189;
   public NumericTextSetting field09190;
   public NumericTextSetting field09191;
   private long field09192;
   private final EventListener<PacketEvent> field09193 = packetEvent -> {
      if (mc.player != null
         && packetEvent.isIncoming()
         && !this.field09189.getText().isEmpty()
         && System.currentTimeMillis() - this.field09192 >= 2000L
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         String local4176_3 = gameMessageS2CPacket.content().getString().toLowerCase();
         if (local4176_3.contains("войд") || local4176_3.contains("логин") || local4176_3.contains("login") || local4176_3.contains("авториз")) {
            this.field09192 = System.currentTimeMillis();
            this.method2678(this.field09191.getText());
         } else if (local4176_3.contains("регист") || local4176_3.contains("register")) {
            this.field09192 = System.currentTimeMillis();
            this.method2678(this.field09190.getText());
         }
      }
   };

   private AutoRegisterModule() {
      this.field09189 = this.addSetting(new NumericTextSetting("Пароль", "Твой пароль сервера для регистрации и входа.", "", 64));
      this.field09190 = new NumericTextSetting("Команда регистрации", "%pass% заменяется на пароль.", "reg %pass% %pass%", 128);
      this.field09191 = new NumericTextSetting("Команда входа", "%pass% заменяется на пароль.", "l %pass%", 128);
   }

   public static AutoRegisterModule method2677() {
      if (autoRegisterModule == null) {
         autoRegisterModule = new AutoRegisterModule();
      }

      return autoRegisterModule;
   }

   private void method2678(String local4178_1) {
      mc.player
         .networkHandler
         .sendChatCommand((local4178_1.startsWith("/") ? local4178_1.substring(1) : local4178_1).replace("%pass%", this.field09189.getText()));
   }
}
