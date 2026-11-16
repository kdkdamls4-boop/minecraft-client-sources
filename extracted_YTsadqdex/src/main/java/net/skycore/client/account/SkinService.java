package net.skycore.client.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class SkinService {
   private static final ExecutorService WORKER = Executors.newFixedThreadPool(2, runnable -> {
      byte flag = 0;
      flag = 0;
      Thread thread = new Thread(runnable, "skycore-alt-skin");
      thread.setDaemon(true);
      return thread;
   });
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8L)).followRedirects(Redirect.NORMAL).build();
   private static final Map<String, Long> RETRY_AT = new ConcurrentHashMap<>();
   private static final Set<String> PENDING = ConcurrentHashMap.newKeySet();
   private static final Map<String, Identifier> SKINS = new ConcurrentHashMap<>();
   private static final Identifier FALLBACK = Identifier.parse("minecraft:textures/entity/player/wide/steve.png");

   private static void register(String text, byte[] byteValues) {
      try {
         NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(byteValues));
         Identifier texture = Identifier.fromNamespaceAndPath("skycore", "alt_skin/" + text);
         DynamicTexture dynamicTexture = new DynamicTexture(() -> {
            return texture.toString();
         }, nativeImage);
         Minecraft.getInstance().getTextureManager().register(texture, dynamicTexture);
         SKINS.put(text, texture);
         RETRY_AT.remove(text);
      } catch (Exception failure) {
         scheduleRetry(text);
      } finally {
         PENDING.remove(text);
      }
   }

   private static UUID resolveUuid(String text) throws Exception {
      String text2 = URLEncoder.encode(text, StandardCharsets.UTF_8);
      byte[] byteValues = download(URI.create("https://api.mojang.com/users/profiles/minecraft/" + text2));
      if (byteValues != null && byteValues.length != 0) {
         JsonObject asJsonObject = JsonParser.parseString(new String(byteValues, StandardCharsets.UTF_8)).getAsJsonObject();
         if (!asJsonObject.has("id")) {
            return null;
         }

         String asString = asJsonObject.get("id").getAsString();
         if (asString.length() != 32) {
            return null;
         }

         String text3 = asString.substring(0, 8)
            + "-"
            + asString.substring(8, 12)
            + "-"
            + asString.substring(12, 16)
            + "-"
            + asString.substring(16, 20)
            + "-"
            + asString.substring(20);
         return UUID.fromString(text3);
      } else {
         return null;
      }
   }

   private static void loadAsync(String text, String text2) {
      try {
         UUID uUID = resolveUuid(text);
         byte[] byteValues = null;
         if (uUID != null) {
            String text3 = uUID.toString().replace("-", "");
            byteValues = download(URI.create("https://crafatar.com/skins/" + text3));
            if (byteValues == null || byteValues.length < 100) {
               byteValues = download(URI.create("https://crafatar.com/skins/" + text3 + "?overlay=true"));
            }
         }

         if (byteValues == null || byteValues.length < 100) {
            scheduleRetry(text2);
            return;
         }

         byte[] byteValues2 = byteValues;
         Minecraft instance = Minecraft.getInstance();
         if (instance == null) {
            scheduleRetry(text2);
            return;
         }

         instance.execute(() -> {
            register(text2, byteValues2);
         });
      } catch (Exception failure) {
         scheduleRetry(text2);
      }
   }

   private static byte[] download(URI uRI) throws Exception {
      Builder builder = HttpRequest.newBuilder(uRI).timeout(Duration.ofSeconds(12L));
      Builder builder2 = builder.header("User-Agent", "Mozilla/5.0 Skycore");
      Builder builder3 = builder2.header("Accept", "image/png,application/json,*/*");
      Builder builder4 = builder3.GET();
      HttpRequest httpRequest = builder4.build();
      HttpResponse httpResponse = HTTP.send(httpRequest, BodyHandlers.ofByteArray());
      if (httpResponse.statusCode() >= 200) {
         if (httpResponse.statusCode() < 300) {
            return (byte[])httpResponse.body();
         }
      }

      return null;
   }

   public static Identifier skin(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            String text2 = text.trim();
            String text3 = text2.toLowerCase(Locale.ROOT);
            Identifier texture = SKINS.get(text3);
            if (texture != null) {
               return texture;
            }

            prefetch(text);
            return FALLBACK;
         }
      }

      return FALLBACK;
   }

   public static void prefetch(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim().toLowerCase(Locale.ROOT);
         if (!SKINS.containsKey(text2) && PENDING.add(text2)) {
            Long element = RETRY_AT.get(text2);
            if (element != null && System.currentTimeMillis() < element) {
               PENDING.remove(text2);
            } else {
               String text3 = text.trim();
               WORKER.execute(() -> {
                  loadAsync(text3, text2);
               });
            }
         }
      }
   }

   private SkinService() {
   }

   private static void scheduleRetry(String text) {
      var rETRY_AT = RETRY_AT;
      long timestamp = System.currentTimeMillis() + 12000L;
      rETRY_AT.put(text, timestamp);
      PENDING.remove(text);
   }
}
