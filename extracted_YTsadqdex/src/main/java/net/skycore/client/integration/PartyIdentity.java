package net.skycore.client.integration;

import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.skycore.client.profile.LocalProfile;

public final class PartyIdentity {
   private static volatile boolean started;
   private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "skycore-presence");
      thread.setDaemon(true);
      return thread;
   });
   private static final String CLIENT_ID = UUID.randomUUID().toString().replace("-", "");
   private static volatile String lastUsername = "";
   private static volatile boolean stopping;

   public static void goOffline() {
      stopping = true;
      SCHEDULER.shutdownNow();
      String lastUsername2 = lastUsername;
      if (lastUsername2 == null || lastUsername2.isBlank()) {
         lastUsername2 = resolveUsername();
      }

      if (lastUsername2 != null && !lastUsername2.isBlank()) {
         String text = lastUsername2;
         Thread thread = new Thread(() -> {
            sendPresence(text, RemoteEndpoints.statusOffline(), 2000, 2000, false);
         }, "skycore-presence-offline");
         thread.start();
      }
   }

   public static String readBody(HttpURLConnection httpURLConnection) {

      try {
         BufferedReader bufferedReader;
         InputStreamReader inputStreamReader;
         InputStream inputStream;
         if (httpURLConnection.getResponseCode() >= 400) {
            inputStream = httpURLConnection.getErrorStream();
         } else {
            inputStream = httpURLConnection.getInputStream();
         }

         inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
         bufferedReader = new BufferedReader(inputStreamReader);
         BufferedReader bufferedReader2 = bufferedReader;

         try (bufferedReader2) {
            var stream = bufferedReader2.lines();
            Collector<CharSequence, ?, String> collector = Collectors.joining();
            return stream.collect(collector);
         }
      } catch (Exception failure) {
         return "";
      }
   }

   private static void syncLiveState() {
      if (!stopping) {
         String text = resolveUsername();
         if (!text.isBlank()) {
            lastUsername = text;
            sendPresence(text, RemoteEndpoints.statusOnline());
         }
      }
   }

   public static String appendGameNickQuery(String text) {
      String text2 = resolveGameNick();
      if (text2 != null) {
         if (!text2.isBlank()) {
            String text3 = URLEncoder.encode(text2, StandardCharsets.UTF_8);
            return text + "&game_nick=" + text3;
         }
      }

      return text;
   }

   public static String getLastUsername() {
      return lastUsername;
   }

   public static void rememberUsername(String text) {
      if (text != null && !text.isBlank()) {
         lastUsername = text;
      }
   }

   private static void sendPresence(String text, String text2) {
      sendPresence(text, text2, 8000, 15000, true);
   }

   public static String getClientId() {
      return CLIENT_ID;
   }

   public static void goOnline() {
      String text = resolveUsername();
      if (!text.isBlank()) {
         lastUsername = text;
         SCHEDULER.execute(() -> {
            if (!stopping) {
               sendPresence(text, RemoteEndpoints.statusOnline());
            }
         });
      }
   }

   public static String resolveGameNick() {
      Minecraft instance = Minecraft.getInstance();
      if (instance == null) {
         return "";
      }

      if (instance.player != null) {
         ClientPacketListener connection = instance.getConnection();
         if (connection != null) {
            PlayerInfo playerInfo = connection.getPlayerInfo(instance.player.getUUID());
            if (playerInfo != null && playerInfo.getProfile() != null && playerInfo.getProfile().name() != null) {
               String profile = playerInfo.getProfile().name().trim();
               if (!profile.isEmpty()) {
                  return profile;
               }
            }
         }

         String gameProfile = instance.player.getGameProfile().name();
         if (gameProfile != null && !gameProfile.isBlank()) {
            return gameProfile.trim();
         }
      }

      if (instance.getUser() != null) {
         String name = instance.getUser().getName();
         if (name != null && !name.isBlank()) {
            return name.trim();
         }
      }

      return "";
   }

   public static String resolveTitleUsername() {
      return "";
   }

   public static String resolveUsername() {

      try {
         LocalProfile instance = LocalProfile.getInstance();
         String text = instance.profile("username");
         if (text != null) {
            if (!text.isBlank()) {
               return text.trim();
            }
         }
      } catch (Throwable failure) {
      }

      Minecraft instance2 = Minecraft.getInstance();
      if (instance2.player != null) {
         return instance2.player.getGameProfile().name();
      }

      String text2;
      if (instance2.getUser() != null) {
         User user = instance2.getUser();
         text2 = user.getName();
      } else {
         text2 = "";
      }

      return text2;
   }

   public static String appendTitleUserQuery(String text) {
      String text2 = resolveTitleUsername();
      if (text2 != null) {
         if (!text2.isBlank()) {
            return text + "&title_user=" + URLEncoder.encode(text2, StandardCharsets.UTF_8);
         }
      }

      return text;
   }

   private PartyIdentity() {
   }

   private static void sendPresence(String text, String text2, int elapsed, int elapsed2, boolean condition) {
      // Telemetry / remote presence reporting disabled and neutralized
   }

   public static void init() {
      // Background presence sync disabled
   }

   public static String appendPresenceQuery(String text) {
      text = appendGameNickQuery(text);
      text = appendTitleUserQuery(text);
      String text2 = URLEncoder.encode(CLIENT_ID, StandardCharsets.UTF_8);
      return text + "&client_id=" + text2;
   }
}


