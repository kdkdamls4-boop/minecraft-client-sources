package net.skycore.client.util;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.ServerData.Type;
import net.skycore.client.mixin.ServerListGetServerListAccessor;

public final class ApplyIdentityHelper {
   public static final String PINNED_IP = "skycore.bravohvh.su";
   private static final String LEGACY_PINNED_HOST = "mc.bravohvh.fun";
   public static final int HIGHLIGHT_COLOR = 956256336;
   public static final String PINNED_NAME = "Best PvP Server";

   public static void applyIdentity(ServerData serverData) {
      serverData.name = "Best PvP Server";
      serverData.ip = "skycore.bravohvh.su";
   }

   private static ServerData removeMatching(List<ServerData> serverDatas, ServerData serverData) {
      Iterator iterator = serverDatas.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return serverData;
         }

         ServerData element = (ServerData)iterator.next();
         if (matches(element)) {
            if (serverData == null) {
               serverData = element;
            }

            iterator.remove();
         }
      }
   }

   public static ServerData create() {
      ServerData serverData = new ServerData("Best PvP Server", "skycore.bravohvh.su", Type.OTHER);
      applyIdentity(serverData);
      return serverData;
   }

   public static boolean matches(ServerData serverData) {
      if (serverData != null && serverData.ip != null) {
         String text = normalizeHost(serverData.ip);
         if (!"skycore.bravohvh.su".equalsIgnoreCase(text)) {
            if (!"mc.bravohvh.fun".equalsIgnoreCase(text)) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   private static String normalizeHost(String text) {
      String text2 = text.trim();
      int value = text2.indexOf(58);
      if (value >= 0) {
         text2 = text2.substring(0, value);
      }

      return text2.toLowerCase(Locale.ROOT);
   }

   public static void ensurePresent(ServerList elements) {
      ServerListGetServerListAccessor elements2 = (ServerListGetServerListAccessor)elements;
      var serverDatas = elements2.skycore$getServerList();
      var serverDatas2 = elements2.skycore$getHiddenServerList();
      ServerData serverData = null;
      serverData = removeMatching(serverDatas, serverData);
      serverData = removeMatching(serverDatas2, serverData);
      if (serverData == null) {
         serverData = create();
      } else {
         applyIdentity(serverData);
      }

      serverDatas.remove(serverData);
      serverDatas.add(0, serverData);
   }

   public static boolean isFirstPinned(ServerList elements) {
      if (elements.size() > 0) {
         if (matches(elements.get(0))) {
            return true;
         }
      }

      return false;
   }

   private ApplyIdentityHelper() {
   }
}
