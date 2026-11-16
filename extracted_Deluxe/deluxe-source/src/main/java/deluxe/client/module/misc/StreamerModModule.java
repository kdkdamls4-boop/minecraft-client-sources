package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;

import java.util.Arrays;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

@ModuleInfo(name = "StreamerMod", description = "Что.", category = ModuleCategory.MISC)
public final class StreamerModModule extends Module {
   private static StreamerModModule streamerModModule;
   public final EventListener<PacketEvent> field00002 = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof MapUpdateS2CPacket mapUpdateS2CPacket && !mapUpdateS2CPacket.updateData().isEmpty()) {
         int local494_2 = mapUpdateS2CPacket.mapId().id();
         if (local494_2 >= 8000 && local494_2 < 8044) {
            Arrays.fill(mapUpdateS2CPacket.updateData().get().colors(), (byte)25);
         }
      }
   };

   public Text method0447(Text text) {
      return Text.literal(text.getString().isEmpty() ? "" : text.getString().charAt(0) + " ")
         .formatted(Formatting.GOLD)
         .append(Text.literal("Дуэли #4").formatted(Formatting.RED, Formatting.BOLD));
   }

   public static StreamerModModule method0449() {
      if (streamerModModule == null) {
         streamerModModule = new StreamerModModule();
      }

      return streamerModModule;
   }

   public boolean method0450(ScoreboardEntry scoreboardEntry) {
      return mc.world == null
         ? false
         : Team.decorateName(mc.world.getScoreboard().getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name())
            .getString()
            .toLowerCase()
            .contains("фортун");
   }

   public Text method0451() {
      return ClientChat.method2262("Обычная дуэль", 47084, 3047398);
   }

   public boolean method0452(String local499_1) {
      return !local499_1.contains("\n")
            || !local499_1.toLowerCase().contains("spookytime") && !local499_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().startsWith("**")
         ? !local499_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().isEmpty()
            && "╔╠╚".indexOf(local499_1.replaceAll("(?i)§[0-9a-fk-or]", "").strip().charAt(0)) >= 0
         : true;
   }
}
