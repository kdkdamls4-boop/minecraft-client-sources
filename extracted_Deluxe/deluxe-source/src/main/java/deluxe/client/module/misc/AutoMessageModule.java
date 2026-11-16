package deluxe.client.module.misc;

import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.setting.NumericTextSetting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "AutoMessage", description = "Автоматически отправляет сообщение в личку убитому игроку.", category = ModuleCategory.MISC)
public final class AutoMessageModule extends Module {
   private static AutoMessageModule autoMessageModule;
   private static final Pattern field07975 = Pattern.compile("\\[\\$] Вы подобрали \\$\\d+ игрока (.+)");
   public ModeSetting modeSetting;
   public NumericTextSetting numericTextSetting;
   private final EventListener<PacketEvent> field07978 = packetEvent -> {
      if (mc.player != null
         && mc.world != null
         && packetEvent.isIncoming()
         && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         Matcher matcher = field07975.matcher(Formatting.strip(gameMessageS2CPacket.content().getString()));
         if (matcher.find()) {
            if (this.modeSetting.isSelected("Дуэль")) {
               mc.player.networkHandler.sendChatMessage(this.numericTextSetting.getValue().trim());
            } else {
               mc.player.networkHandler.sendChatCommand("m " + matcher.group(1).trim() + " " + this.numericTextSetting.getValue().trim());
            }
         }
      }
   };

   private AutoMessageModule() {
      this.modeSetting = this.addSetting(new ModeSetting("Режим", "РТП пишет убитому в личку, Дуэль — в общий чат.", "РТП", "РТП", "Дуэль"));
      this.numericTextSetting = this.addSetting(new NumericTextSetting("Текст сообщения", "Что написать убитому.", "ez", 256));
   }

   public static AutoMessageModule method2253() {
      if (autoMessageModule == null) {
         autoMessageModule = new AutoMessageModule();
      }

      return autoMessageModule;
   }
}
