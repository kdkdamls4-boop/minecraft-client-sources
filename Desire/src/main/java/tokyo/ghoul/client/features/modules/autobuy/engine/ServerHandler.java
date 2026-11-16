package tokyo.ghoul.client.features.modules.autobuy.engine;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.apache.commons.lang3.StringUtils;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;

import java.util.Locale;

@Getter
public class ServerHandler {
   @Getter private static final ServerHandler instance = new ServerHandler();

   private String server = "Vanilla";
   private float TPS = 20.0F;
   private long timestamp;
   @Setter private boolean serverSprint;
   private int anarchy = -1;
   private boolean pvpEnd;
   private long lastPvpTime;

   public ServerHandler() {
      TickEvent.getInstance().subscribe(new Listener<>(e -> tick()));
      PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
   }

   public void tick() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player == null || mc.level == null) {
         this.server = "Vanilla";
         this.anarchy = -1;
         return;
      }
      this.server = updateServer();
      this.anarchy = getAnarchyMode();
      this.pvpEnd = inPvpEnd();
      if (inPvp()) {
         this.lastPvpTime = System.currentTimeMillis();
      }
   }

   public void onPacket(PacketEvent.PacketEventData e) {
      if (e == null) return;
      Packet<?> packet = e.packet();
      if (packet instanceof ClientboundSetTimePacket) {
         long nanoTime = System.nanoTime();
         float maxTPS = 20.0F;
         if (this.timestamp != 0L) {
            float rawTPS = maxTPS * (1.0E9F / (float) (nanoTime - this.timestamp));
            this.TPS = Mth.clamp(rawTPS, 0.0F, maxTPS);
         }
         this.timestamp = nanoTime;
      }

      if (e.isSend() && packet instanceof ServerboundPlayerCommandPacket command) {
         if (command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING) {
            this.serverSprint = true;
         } else if (command.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING) {
            this.serverSprint = false;
         }
      }
   }

   private String updateServer() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() != null) {
         String serverIp = "";
         ServerData currentServer = mc.getCurrentServer();
         if (currentServer != null && currentServer.ip != null) {
            serverIp = currentServer.ip.toLowerCase(Locale.ROOT);
         } else if (mc.getConnection().getServerData() != null && mc.getConnection().getServerData().ip != null) {
            serverIp = mc.getConnection().getServerData().ip.toLowerCase(Locale.ROOT);
         }

         String brand = mc.getConnection().serverBrand() != null ? mc.getConnection().serverBrand().toLowerCase(Locale.ROOT) : "";
         if (brand.contains("botfilter")) {
            return "FunTime";
         } else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) {
            return "CopyTime";
         } else if (brand.contains("holyworld") || brand.contains("hollyworld") || brand.contains("leaf") || brand.contains("vk.com/idwok") || serverIp.contains("holyworld")) {
            return "HolyWorld";
         } else if (serverIp.contains("lonygrief")) {
            return "LonyGrief";
         } else if (serverIp.contains("reallyworld")) {
            return "ReallyWorld";
         } else {
            return "Vanilla";
         }
      }
      return "Vanilla";
   }

   private int getAnarchyMode() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return -1;
      Scoreboard scoreboard = mc.level.getScoreboard();
      Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (objective == null) return -1;

      try {
         switch (this.server) {
            case "FunTime" -> {
               String[] string = objective.getDisplayName().getString().split("-");
               if (string.length > 1) {
                  return Integer.parseInt(string[1].trim());
               }
            }
            case "HolyWorld" -> {
               for (PlayerScoreEntry scoreboardEntry : scoreboard.listPlayerScores(objective)) {
                  PlayerTeam team = scoreboard.getPlayersTeam(scoreboardEntry.owner());
                  String text = PlayerTeam.formatNameForTeam(team, scoreboardEntry.ownerName()).getString();
                  if (!text.isEmpty()) {
                     String string = StringUtils.substringBetween(text, "#", " -◆-");
                     if (string != null && !string.isEmpty()) {
                        return Integer.parseInt(string.replace(" (1.20)", "").trim());
                     }
                  }
               }
            }
         }
      } catch (NumberFormatException ignored) {}

      return -1;
   }

   public boolean isPvp() {
      return (System.currentTimeMillis() - this.lastPvpTime) < 250L;
   }

   private boolean inPvp() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui == null || mc.gui.hud == null || mc.gui.hud.getBossOverlay() == null) return false;
      return mc.gui.hud.getBossOverlay().events.values().stream().map(c -> c.getName().getString().toLowerCase(Locale.ROOT)).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
   }

   private boolean inPvpEnd() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.gui == null || mc.gui.hud == null || mc.gui.hud.getBossOverlay() == null) return false;
      return mc.gui.hud.getBossOverlay().events.values().stream().map(c -> c.getName().getString().toLowerCase(Locale.ROOT)).anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
   }

   public boolean isCopyTime() {
      return this.server.equals("CopyTime") || this.server.equals("SpookyTime") || this.server.equals("FunTime");
   }

   public boolean isFunTime() {
      return this.server.equals("FunTime");
   }

   public boolean isReallyWorld() {
      return this.server.equals("ReallyWorld");
   }

   public boolean isHolyWorld() {
      return this.server.equals("HolyWorld");
   }

   public boolean isVanilla() {
      return this.server.equals("Vanilla");
   }
}
