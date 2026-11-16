package tokyo.ghoul.client.features.modules.autobuy.engine;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;

public final class ServerReconnectUtil {
   private static final String HOLYWORLD_DIRECT_CONNECT_ADDRESS = "play.holyworld.ru";
   private static PendingConnect pendingConnect;

   private ServerReconnectUtil() {
   }

   public static ServerTarget captureCurrentServerTarget(int anarchy) {
      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return null;
      }

      ServerData currentEntry = client.getCurrentServer();
      if (currentEntry == null) {
         ClientPacketListener networkHandler = client.getConnection();
         if (networkHandler != null) {
            currentEntry = networkHandler.getServerData();
         }
      }

      if (currentEntry == null || currentEntry.ip == null || currentEntry.ip.isBlank()) {
         return null;
      }

      String name = currentEntry.name == null || currentEntry.name.isBlank() ? "HolyWorld" : currentEntry.name;
      return normalizeTarget(new ServerTarget(name, currentEntry.ip, anarchy));
   }

   public static boolean isConnectedToServer() {
      Minecraft client = Minecraft.getInstance();
      return client != null && client.level != null && client.getConnection() != null;
   }

   public static void disconnect(String reason) {
      Minecraft client = Minecraft.getInstance();
      if (client == null || client.getConnection() == null) {
         return;
      }

      String message = reason == null || reason.isBlank() ? "Disconnected" : reason;
      client.getConnection().getConnection().disconnect(Component.literal(message));
   }

   public static boolean connect(ServerTarget target) {
      ServerTarget normalizedTarget = normalizeTarget(target);
      if (normalizedTarget == null || normalizedTarget.address() == null || normalizedTarget.address().isBlank()) {
         return false;
      }

      Minecraft client = Minecraft.getInstance();
      if (client == null) {
         return false;
      }
      pendingConnect = new PendingConnect(normalizedTarget, System.currentTimeMillis() + 600L);
      return true;
   }

   public static void cancelPendingConnect() {
      pendingConnect = null;
   }

   public static void tickPendingConnect(long now) {
      Minecraft client = Minecraft.getInstance();
      if (client == null || pendingConnect == null) {
         return;
      }
      if (client.level != null || client.getConnection() != null) {
         pendingConnect = null;
         return;
      }
      if (client.gui != null && client.gui.screen() instanceof ConnectScreen) {
         return;
      }
      if (now < pendingConnect.nextActionAtMs()) {
         return;
      }

      ServerTarget target = pendingConnect.target();
      ServerAddress address = ServerAddress.parseString(target.address());
      ServerData serverData = new ServerData(target.name(), target.address(), ServerData.Type.OTHER);
      try {
         ConnectScreen.startConnecting(new TitleScreen(), client, address, serverData, false, null);
      } catch (Throwable t) {
         t.printStackTrace();
      }
      pendingConnect = null;
   }

   public record ServerTarget(String name, String address, int anarchy) {
   }

   private static ServerTarget normalizeTarget(ServerTarget target) {
      if (target == null) {
         return null;
      }

      String name = target.name() == null || target.name().isBlank() ? "HolyWorld" : target.name();
      String address = target.address();
      if (address == null || address.isBlank()) {
         return new ServerTarget(name, HOLYWORLD_DIRECT_CONNECT_ADDRESS, target.anarchy());
      }

      String normalizedAddress = address.trim().toLowerCase();
      if (normalizedAddress.contains("holyworld")) {
         return new ServerTarget(name, HOLYWORLD_DIRECT_CONNECT_ADDRESS, target.anarchy());
      }

      return new ServerTarget(name, address.trim(), target.anarchy());
   }

   private record PendingConnect(ServerTarget target, long nextActionAtMs) {
   }
}
