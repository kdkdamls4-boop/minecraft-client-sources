package net.skycore.client.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.skycore.client.integration.PartyIdentity;
import net.skycore.client.integration.RemoteApiConfig;
import net.skycore.client.integration.RemoteEndpoints;

public final class ReadBodyHelper {
   private static final long REFRESH_INTERVAL_MS = 15000L;
   private static final Set<String> ALLOWED_USERS = Set.of("fametyca");
   private static volatile int onlineCount = -1;
   private static volatile long lastFetchMs;

   public static boolean isAllowedUser(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            String text2 = text.trim();

            for (String text3 : ALLOWED_USERS) {
               if (text3.equalsIgnoreCase(text2)) {
                  return true;
               }
            }

            return false;
         }
      }

      return false;
   }

   private ReadBodyHelper() {
   }

   private static String readBody(HttpURLConnection httpURLConnection) {
      try {
         BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(
               httpURLConnection.getResponseCode() >= 400 ? (httpURLConnection.getErrorStream() != null ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream()) : httpURLConnection.getInputStream(),
               StandardCharsets.UTF_8
            )
         );
         BufferedReader bufferedReader2 = bufferedReader;

         String text;
         try {
            text = bufferedReader.lines().collect(Collectors.joining());
         } catch (Throwable failure) {
            if (bufferedReader != null) {
               try {
                  bufferedReader2.close();
               } catch (Throwable failure2) {
                  failure.addSuppressed(failure2);
               }
            }

            throw failure;
         }

         if (bufferedReader != null) {
            bufferedReader.close();
         }

         return text;
      } catch (Exception failure3) {
         return "";
      }
   }

   private static int requestOnlineCount() throws Exception {
      String text = PartyIdentity.resolveUsername();
      if (text == null || text.isBlank()) {
         text = "User";
      }

      String apiUrl = RemoteApiConfig.getApiUrl()
         + RemoteEndpoints.pathOnlineCount()
         + "?"
         + RemoteEndpoints.jsonApiKey()
         + "="
         + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8)
         + "&username="
         + URLEncoder.encode(text.trim(), StandardCharsets.UTF_8);
      HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
      httpURLConnection.setRequestMethod("GET");
      httpURLConnection.setConnectTimeout(4000);
      httpURLConnection.setReadTimeout(4000);
      String text2 = readBody(httpURLConnection);
      JsonObject asJsonObject = JsonParser.parseString(text2 != null && !text2.isBlank() ? text2 : "{}").getAsJsonObject();
      if (asJsonObject.has("status") && "success".equals(asJsonObject.get("status").getAsString()) && asJsonObject.has("online")) {
         return asJsonObject.get("online").getAsInt();
      }

      String asString = asJsonObject.has("message") ? asJsonObject.get("message").getAsString() : "online fetch failed";
      throw new IllegalStateException(asString);
   }

   public static int getCount() {
      int count = -1;
      int value = 0;
      count = (byte)-1;
      value = (byte)0;
      if (onlineCount >= 0) {
         value = onlineCount;
      } else {
         count = ShouldShowSkycoreTagHelper.getOnlineCount();
         value = Math.max(0, count);
      }

      return value;
   }

   public static void fetchAsync(Consumer<Integer> consumer) {
      lastFetchMs = System.currentTimeMillis();
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      Runnable runnable = () -> {

         try {
            int count = requestOnlineCount();
            onlineCount = count;
            Runnable runnable2 = () -> {
               if (consumer != null) {
                  Integer value = count;
                  consumer.accept(value);
               }
            };
            runOnMain(runnable2);
         } catch (Exception failure) {
            runOnMain(() -> {
               if (consumer != null) {
                  Integer count2 = getCount();
                  consumer.accept(count2);
               }
            });
         }
      };
      executorService.submit(runnable);
   }

   public static void refreshIfNeeded() {
      long now = System.currentTimeMillis();
      if (now - lastFetchMs >= 15000L) {
         fetchAsync(null);
      }
   }

   private static void runOnMain(Runnable runnable) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         if (instance.isSameThread()) {
            runnable.run();
         } else {
            instance.execute(runnable);
         }
      }
   }
}
