package net.skycore.client.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.skycore.client.integration.PartyIdentity;

public final class ShouldShowSkycoreTagHelper {
   private static volatile int onlineCount;
   private static volatile List<ShouldShowSkycoreTagHelper.Session> onlineSessions = List.of();
   private static volatile boolean ready;
   private static volatile List<String> onlineUsers = List.of();
   private static final Set<String> TAG_EXCLUDED_SITE_LOGINS = Set.of("youngleqzhe", "fametyca", "kodek");

   public static boolean isExcludedSiteLogin(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            var tAG_EXCLUDED_SITE_LOGINS = TAG_EXCLUDED_SITE_LOGINS;
            String text2 = text.trim();
            String text3 = text2.toLowerCase(Locale.ROOT);
            return tAG_EXCLUDED_SITE_LOGINS.contains(text3);
         }
      }

      return false;
   }

   public static void update(int value, List<String> strings) {
      update(value, strings, null);
   }

   private ShouldShowSkycoreTagHelper() {
   }

   public static boolean hasPresenceData() {
      return ready;
   }

   public static List<ShouldShowSkycoreTagHelper.Session> getOnlineSessions() {
      return onlineSessions;
   }

   public static int getOnlineCount() {
      return onlineCount;
   }

   public static boolean isOnline(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = PartyIdentity.resolveUsername();
         if (!text2.isBlank() && text2.equalsIgnoreCase(text)) {
            return true;
         }

         String text3 = text.trim().toLowerCase(Locale.ROOT);

         for (String text4 : onlineUsers) {
            if (text4 != null && text4.trim().toLowerCase(Locale.ROOT).equals(text3)) {
               return true;
            }
         }

         for (ShouldShowSkycoreTagHelper.Session session : onlineSessions) {
            if (session.matches(text3)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static boolean shouldShowSkycoreTag(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = resolveSiteLogin(text);
         if (text2 != null) {
            if (!text2.isBlank()) {
               return !isExcludedSiteLogin(text2);
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public static List<String> getOnlineUsers() {
      return onlineUsers;
   }

   public static void update(int value, List<String> strings, List<ShouldShowSkycoreTagHelper.Session> sessions) {
      onlineCount = Math.max(0, value);
      ready = true;
      if (strings != null && !strings.isEmpty()) {
         onlineUsers = Collections.unmodifiableList(new ArrayList<>(strings));
      } else {
         List<String> strings2 = List.of();
         onlineUsers = strings2;
      }

      if (sessions != null && !sessions.isEmpty()) {
         var elements = Collections.unmodifiableList(new ArrayList(sessions));
         onlineSessions = elements;
      } else {
         onlineSessions = List.of();
      }
   }

   public static String resolveSiteLogin(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = PartyIdentity.resolveGameNick();
         String text3 = PartyIdentity.resolveUsername();
         if (!text2.isBlank() && text2.equalsIgnoreCase(text) && !text3.isBlank()) {
            return text3.trim();
         }

         if (!text3.isBlank() && text3.equalsIgnoreCase(text)) {
            return text3.trim();
         }

         String text4 = text.trim().toLowerCase(Locale.ROOT);

         for (ShouldShowSkycoreTagHelper.Session session : onlineSessions) {
            if (session.matches(text4)) {
               return session.login;
            }
         }

         for (String text5 : onlineUsers) {
            if (text5 != null && text5.trim().toLowerCase(Locale.ROOT).equals(text4)) {
               return text5.trim();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static final class Session {
      public final String gameNick;
      public final String login;

      private boolean matches(String text) {
         if (!this.login.isEmpty()) {
            String text2 = this.login.toLowerCase(Locale.ROOT);
            if (text2.equals(text)) {
               return true;
            }
         }

         if (!this.gameNick.isEmpty()) {
            String text3 = this.gameNick.toLowerCase(Locale.ROOT);
            if (text3.equals(text)) {
               return true;
            }
         }

         return false;
      }

      public Session(String login2, String gameNick2) {
         this.login = login2 == null ? "" : login2.trim();
         this.gameNick = gameNick2 == null ? "" : gameNick2.trim();
      }
   }
}
