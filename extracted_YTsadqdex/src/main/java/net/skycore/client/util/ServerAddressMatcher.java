package net.skycore.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

public final class ServerAddressMatcher {
   private ServerAddressMatcher() {
   }

   public static boolean isReallyWorld() {
      return isConnectedToServer("reallyworld") || isConnectedToServer("playrw");
   }

   public static boolean isConnectedToServer(String text) {
      Minecraft instance = Minecraft.getInstance();
      if (instance.getConnection() == null) {
         return false;
      }

      ServerData currentServer = instance.getCurrentServer();
      if (currentServer != null && currentServer.ip != null) {
         String text2 = currentServer.ip.toLowerCase();
         String text3 = text.toLowerCase();
         if (text2.contains(text3)) {
            return true;
         }
      }

      return false;
   }
}
