package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.ui.UiTheme;

import java.util.ArrayDeque;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "DuelHelper", description = "Сам ищет дуэли: встаёт в очередь, перезапускает поиск и выходит после боя.", category = ModuleCategory.MISC)
public final class DuelHelperModule extends Module {
   private static DuelHelperModule duelHelperModule;
   public ModeSetting modeSetting;
   private final ArrayDeque<DuelHelperModule.Nested0376> field03261 = new ArrayDeque<>();
   private DuelHelperModule.Nested0375 field03262 = DuelHelperModule.Nested0375.IDLE;
   private long field03263;
   private long field03264;
   private long field03265;
   private String field03266;
   private final EventListener<PlayerTickEvent> field03267 = playerTickEvent -> {
      if (mc.player != null && mc.getNetworkHandler() != null) {
         long local1616_2 = System.currentTimeMillis();
         if (!this.modeSetting.getSelectedOption().equals(this.field03266)) {
            this.field03266 = this.modeSetting.getSelectedOption();
            this.method1176("Режим: " + this.modeSetting.getSelectedOption().toLowerCase());
            this.method1179(0L);
         }

         while (!this.field03261.isEmpty() && this.field03261.peek().runAt() <= local1616_2) {
            String local1616_4 = this.field03261.poll().command();
            if (local1616_4 == null) {
               this.method1174();
            } else {
               mc.getNetworkHandler().sendChatCommand(local1616_4);
            }
         }

         if (this.field03262 == DuelHelperModule.Nested0375.WAITING_SEARCH_CONFIRM && local1616_2 - this.field03263 >= 2000L) {
            this.method1174();
         } else if (this.field03262 == DuelHelperModule.Nested0375.IN_DUEL && this.field03261.isEmpty() && local1616_2 - this.field03265 >= 2500L) {
            this.method1176("Дуэль началась, модуль выключен");
            this.setEnabled(false);
         } else if (this.field03262 == DuelHelperModule.Nested0375.IN_QUEUE && this.field03261.isEmpty() && local1616_2 - this.field03264 >= 65000L) {
            this.method1176("Прошло 1:05 без соперника, ищем заново");
            this.method1174();
         }
      }
   };
   private final EventListener<PacketEvent> field03268 = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && !gameMessageS2CPacket.overlay()) {
         String string = gameMessageS2CPacket.content().getString();
         mc.execute(() -> this.method1175(string));
      }
   };

   public static DuelHelperModule method1172() {
      if (duelHelperModule == null) {
         duelHelperModule = new DuelHelperModule();
      }

      return duelHelperModule;
   }

   private void method1174() {
      this.field03262 = DuelHelperModule.Nested0375.WAITING_SEARCH_CONFIRM;
      this.field03263 = System.currentTimeMillis();
      if (mc.player != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendChatCommand(this.modeSetting.isSelected("Командный") ? "duel team search" : "duel search");
      }
   }

   private void method1175(String local1614_1) {
      if (this.isEnabled()) {
         if (local1614_1.contains("Вы начали поиск") || local1614_1.contains("уже был начат")) {
            this.field03262 = DuelHelperModule.Nested0375.IN_QUEUE;
            this.field03264 = System.currentTimeMillis();
            this.method1176("В очереди, ищем соперника");
         }

         if (local1614_1.contains("был отменен") || local1614_1.contains("был отменён") || local1614_1.contains("не было найдено противников")) {
            this.method1176("Поиск отменён, перезапускаем");
            this.field03262 = DuelHelperModule.Nested0375.IDLE;
            this.field03261.clear();
            this.field03261.add(new DuelHelperModule.Nested0376(null, System.currentTimeMillis() + 300L));
         }

         if (local1614_1.contains("Дуэль начнётся") || local1614_1.contains("Дуэль начнется")) {
            this.field03265 = System.currentTimeMillis();
            if (this.field03262 != DuelHelperModule.Nested0375.IN_DUEL) {
               this.method1176("Соперник найден, дуэль начинается");
            }

            this.field03262 = DuelHelperModule.Nested0375.IN_DUEL;
         }

         if (local1614_1.contains("покинул дуэль")
            && this.field03262 == DuelHelperModule.Nested0375.IN_DUEL
            && (mc.player == null || !local1614_1.contains(mc.player.getName().getString()))) {
            this.method1176("Соперник ливнул, ищем заново");
            this.method1179(300L);
         }

         if (local1614_1.contains("одержала Победу") || local1614_1.contains("одержал Победу")) {
            this.method1176("Победа, ищем следующую дуэль");
            this.method1179(300L);
         }

         if (local1614_1.contains("одержала Поражение") || local1614_1.contains("одержал Поражение") || local1614_1.contains("потерпела Поражение")) {
            this.method1176("Поражение, ищем снова");
            this.method1179(300L);
         }

         if (local1614_1.contains("Дуэль завершена") && this.field03262 == DuelHelperModule.Nested0375.IN_DUEL) {
            this.method1179(300L);
         }
      }
   }

   private void method1176(String local1615_1) {
      ClientChat.method2260(
         ClientChat.method2262("DuelHelper", UiTheme.field09686, UiTheme.field09688, true)
            .append(Text.literal(Formatting.DARK_GRAY + " » " + Formatting.RESET))
            .append(Text.literal(local1615_1))
      );
   }

   private DuelHelperModule() {
      this.modeSetting = this.addSetting(
         new ModeSetting("Режим", "Одиночный /duel search, Командный /duel team search.", "Одиночный", "Одиночный", "Командный")
      );
   }

   @Override
   protected void onEnable() {
      this.field03261.clear();
      this.field03266 = this.modeSetting.getSelectedOption();
      this.method1176("Включён, " + this.modeSetting.getSelectedOption().toLowerCase() + " режим");
      this.method1174();
   }

   @Override
   protected void onDisable() {
      this.field03261.clear();
      this.field03262 = DuelHelperModule.Nested0375.IDLE;
   }

   private void method1179(long local1621_1) {
      this.field03262 = DuelHelperModule.Nested0375.IDLE;
      this.field03261.clear();
      long local1621_3 = System.currentTimeMillis();
      this.field03261.add(new DuelHelperModule.Nested0376("duel leave", local1621_3 + local1621_1));
      this.field03261.add(new DuelHelperModule.Nested0376(null, local1621_3 + local1621_1 + 300L));
   }

   private enum Nested0375 {
      IDLE,
      WAITING_SEARCH_CONFIRM,
      IN_QUEUE,
      IN_DUEL;
   }

   private record Nested0376(String command, long runAt) {
   }
}
