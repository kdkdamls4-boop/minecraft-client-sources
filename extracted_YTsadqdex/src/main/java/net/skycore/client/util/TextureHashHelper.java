package net.skycore.client.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.hash.Hashing;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Base64.Encoder;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.PlayerSkinRenderCache.RenderInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset.DownloadedTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin.Patch;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

public final class TextureHashHelper {
   private static final Map<String, String> URL_BY_NAME = new ConcurrentHashMap<>();
   private static final Object READY_LOCK = new Object();
   private static final long PENDING_STALE_MS = 60000L;
   private static final int MAX_FAILED = 64;
   private static final Map<Identifier, PlayerSkin> SKIN_BY_ID = new ConcurrentHashMap<>();
   private static final Map<String, Identifier> BY_NICK = new ConcurrentHashMap<>();
   private static final Map<UUID, String> URL_BY_PROFILE = new ConcurrentHashMap<>();
   private static final int MAX_REDIRECTS = 3;
   private static final int MAX_NICK_BINDINGS = 48;
   private static final Map<String, Long> FAILED = new ConcurrentHashMap<>();
   private static final Map<String, Long> PENDING = new ConcurrentHashMap<>();
   private static final Pattern TEXTURE_HASH = Pattern.compile("/texture/([a-fA-F0-9]{32,128})");
   private static final long FAIL_RETRY_MS = 30000L;
   private static final Pattern HTTP_URL = Pattern.compile("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%\\-]+", 2);
   private static final int MAX_URL_HINTS = 64;
   private static final int MAX_IMAGE_PIXELS = 65536;
   private static final Map<String, String> HASH_TO_KEY = new ConcurrentHashMap<>();
   private static final ExecutorService IO = Executors.newFixedThreadPool(1, runnable -> {
      byte flag = 0;
      flag = 0;
      Thread thread = new Thread(runnable, "pasta-head-skin");
      thread.setDaemon(true);
      return thread;
   });
   private static final int MAX_DOWNLOAD_BYTES = 131072;
   private static final Pattern EY_BASE64 = Pattern.compile("(eyJ[A-Za-z0-9_\\-/=+]{40,}|ewog[A-Za-z0-9_\\-/=+\\s]{40,})");
   private static final LinkedHashMap<String, Identifier> READY = new LinkedHashMap<>(32, 0.75F, true);
   private static volatile String LAST_APPLIED_URL;
   private static final int MAX_READY_SKINS = 24;
   private static final Map<String, String> NICK_URL = new ConcurrentHashMap<>();

   private static String textureHash(String text) {
      if (text == null) {
         return null;
      } else {
         Matcher matcher = TEXTURE_HASH.matcher(text);
         if (matcher.find()) {
            String text2 = matcher.group(1);
            return text2.toLowerCase(Locale.ROOT);
         } else {
            return null;
         }
      }
   }

   private TextureHashHelper() {
   }

   public static PlayerSkin skinIfReady(ItemStack itemStack) {
      String text = nickFromStack(itemStack);
      Identifier texture;
      if (text != null) {
         texture = BY_NICK.get(text);
      } else {
         texture = null;
      }

      Identifier texture2 = texture;
      if (texture2 != null) {
         return skinForId(texture2, NICK_URL.getOrDefault(text, ""));
      }

      String text2 = resolveUrlFromStack(itemStack);
      if (text2 == null) {
         return null;
      }

      rememberProfileUrl(itemStack, text2);
      ensureProfileHasTextures(itemStack, text2);
      return skinIfReadyUrl(text2, text);
   }

   private static NativeImage processLikeVanilla(NativeImage nativeImage) {
      int height = nativeImage.getHeight();
      int width = nativeImage.getWidth();
      if (width == 64 && (height == 32 || height == 64)) {
         if (height == 32) {
            NativeImage nativeImage2 = new NativeImage(64, 64, true);
            nativeImage2.copyFrom(nativeImage);
            nativeImage.close();
            nativeImage = nativeImage2;
            nativeImage.fillRect(0, 32, 64, 32, 0);
         }

         return nativeImage;
      } else {
         return nativeImage;
      }
   }

   public static GameProfile gameProfileForUrl(String text, ItemStack itemStack, RenderInfo renderInfo) {
      if (text == null) {
         return renderInfo != null ? renderInfo.gameProfile() : null;
      }

      GameProfile gameProfile = null;
      if (itemStack != null) {
         ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
         if (resolvableProfile != null) {
            gameProfile = resolvableProfile.partialProfile();
         }
      }

      if (gameProfile == null && renderInfo != null) {
         gameProfile = renderInfo.gameProfile();
      }

      UUID uUID = profileIdForUrl(text);
      String text2 = gameProfile != null && gameProfile.name() != null && !gameProfile.name().isBlank() ? gameProfile.name() : "skycore";
      if (gameProfile != null && uUID.equals(gameProfile.id()) && gameProfile.properties() != null && !gameProfile.properties().isEmpty()) {
         return gameProfile;
      }

      PropertyMap lookup = gameProfile != null && gameProfile.properties() != null
         ? gameProfile.properties()
         : new PropertyMap(ImmutableMultimap.of("textures", new Property("textures", encodedTextures(text))));
      return new GameProfile(uUID, text2, lookup);
   }

   private static void downloadAndRegister(String text, String text2, String text3) {
      try {
         if (text3 != null && BY_NICK.containsKey(text3)) {
            PENDING.remove(text2);
            return;
         }

         byte[] byteValues = download(text, 0);
         if (byteValues == null || byteValues.length < 100) {
            markFailed(text2, text);
            return;
         }

         NativeImage nativeImage = NativeImage.read(byteValues);
         if (nativeImage.getWidth() <= 0 || nativeImage.getHeight() <= 0 || (long)nativeImage.getWidth() * nativeImage.getHeight() > 65536L) {
            nativeImage.close();
            markFailed(text2, text);
            return;
         }

         NativeImage nativeImage2 = processLikeVanilla(ensureSkinSize(nativeImage));
         Minecraft instance = Minecraft.getInstance();
         if (instance == null) {
            nativeImage2.close();
            PENDING.remove(text2);
            return;
         }

         instance.execute(() -> {
            try {
               try {
                  synchronized (READY_LOCK) {
                     if (READY.containsKey(text2) || text3 != null && BY_NICK.containsKey(text3)) {
                        nativeImage2.close();
                        return;
                     }
                  }

                  String text4 = Hashing.sha1().hashUnencodedChars(text2).toString();
                  Identifier texture = Identifier.withDefaultNamespace("skins/pasta_" + text4);
                  DynamicTexture dynamicTexture = new DynamicTexture(texture::toString, nativeImage2);
                  instance.getTextureManager().register(texture, dynamicTexture);
                  putReady(text2, text, texture);
                  if (text3 != null) {
                     bindNick(text3, text, texture);
                  }

                  FAILED.remove(text2);
                  String text5 = textureHash(text);
                  if (text5 != null) {
                     FAILED.remove(text5);
                     return;
                  }
               } catch (Throwable failure) {
                  nativeImage2.close();
                  markFailed(text2, text);
               }
            } finally {
               PENDING.remove(text2);
            }
         });
      } catch (Throwable failure2) {
         markFailed(text2, text);
      }
   }

   private static PlayerSkin skinForId(Identifier texture, String text) {
      return SKIN_BY_ID.computeIfAbsent(texture, texture2 -> {
         String text2;
         textureHashLookup: {
            if (text != null) {
               if (!text.isBlank()) {
                  text2 = text;
                  break textureHashLookup;
               }
            }

            text2 = texture2.toString();
         }

         String text3 = text2;
         DownloadedTexture downloadedTexture = new DownloadedTexture(texture2, text3);
         return PlayerSkin.insecure(downloadedTexture, null, null, PlayerModelType.WIDE);
      });
   }

   private static String nickFromStack(ItemStack itemStack) {
      if (itemStack == null) {
         return null;
      }

      ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
      if (resolvableProfile != null && resolvableProfile.partialProfile() != null) {
         GameProfile gameProfile = resolvableProfile.partialProfile();
         String text = gameProfile.name();
         if (text != null) {
            if (!text.isBlank()) {
               if (!"skycore".equalsIgnoreCase(text)) {
                  String text2 = text.trim();
                  return text2.toLowerCase(Locale.ROOT);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static Identifier getIfReady(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = normalizeKey(text);
         String text3 = textureHash(text);
         synchronized (READY_LOCK) {
            Identifier texture = READY.get(text2);
            if (texture != null) {
               return texture;
            }

            if (text3 != null) {
               String text4 = HASH_TO_KEY.get(text3);
               if (text4 != null) {
                  return READY.get(text4);
               }
            }

            return null;
         }
      } else {
         return null;
      }
   }

   private static String urlFromProperty(Property property) {
      if (property == null) {
         return null;
      }

      String text = property.value();
      if (text == null || text.isBlank()) {
         text = property.signature();
      }

      if (text != null && !text.isBlank()) {
         Property property2 = FindTextureInTagHelper.fixPropertyKeepSignature(new Property("textures", text));
         MinecraftProfileTextures minecraftProfileTextures = UnpackHelper.unpack(property2 != null ? property2 : property);
         MinecraftProfileTexture minecraftProfileTexture = minecraftProfileTextures != null ? minecraftProfileTextures.skin() : null;
         return minecraftProfileTexture != null && minecraftProfileTexture.getUrl() != null && !minecraftProfileTexture.getUrl().isBlank() ? minecraftProfileTexture.getUrl().trim().replace("\\/", "/") : extractUrlFromBlob(text);
      } else {
         return null;
      }
   }

   public static String cleanUrl(String text) {
      if (text == null) {
         return null;
      }

      String text2 = text.trim().replace("\\/", "/");
      StringBuilder stringBuilder = new StringBuilder(text2.length());

      for (int value = 0; value < text2.length(); value++) {
         char character = text2.charAt(value);
         if (character == '"' || character == '\'' || character == '}' || character == ']' || character == ' ' || character == '<' || character == '>' || character == '\\' || character == ',') {
            break;
         }

         stringBuilder.append(character);
      }

      text2 = stringBuilder.toString();
      if (text2.regionMatches(true, 0, "http://", 0, 7)) {
         text2 = "https://" + text2.substring(7);
      }

      return text2;
   }

   public static PlayerSkin resolveSkinForSubmit(ItemStack itemStack, RenderInfo renderInfo) {
      if (itemStack != null) {
         PlayerSkin playerSkin = skinIfReady(itemStack);
         if (playerSkin != null) {
            return playerSkin;
         }
      }

      if (renderInfo == null) {
         return null;
      }

      try {
         Identifier texture = renderInfo.playerSkin().body().texturePath();
         if (texture != null && texture.getPath().startsWith("skins/pasta_")) {
            return renderInfo.playerSkin();
         }
      } catch (Throwable failure) {
      }

      GameProfile gameProfile = renderInfo.gameProfile();
      String text = gameProfile != null ? extractUrl(gameProfile) : null;
      return text == null ? null : skinIfReadyUrl(text);
   }

   private static String normalizeKey(String text) {
      String text2 = cleanUrl(text);
      return text2.toLowerCase(Locale.ROOT);
   }

   public static RenderInfo renderInfoForStack(PlayerSkinRenderCache playerSkinRenderCache, ItemStack itemStack, RenderInfo renderInfo, boolean condition) {
      if (itemStack != null && playerSkinRenderCache != null) {
         String text = nickFromStack(itemStack);
         if (text != null && BY_NICK.containsKey(text)) {
            PlayerSkin orDefault = skinForId(BY_NICK.get(text), NICK_URL.getOrDefault(text, ""));
            GameProfile gameProfile = gameProfileForUrl(NICK_URL.get(text), itemStack, renderInfo);
            Objects.requireNonNull(playerSkinRenderCache);
            return playerSkinRenderCache.new RenderInfo(gameProfile, orDefault, Patch.EMPTY);
         }

         String text2 = resolveUrlFromStackInternal(itemStack);
         if (text2 == null) {
            return renderInfo;
         }

         if (text != null && NICK_URL.containsKey(text)) {
            text2 = NICK_URL.get(text);
         }

         request(text2, text);
         PlayerSkin playerSkin = skinIfReadyUrl(text2, text);
         if (playerSkin == null && condition) {
            return null;
         }

         GameProfile gameProfile2 = gameProfileForUrl(text2, itemStack, renderInfo);
         PlayerSkin playerSkin2 = playerSkin != null ? playerSkin : (renderInfo != null ? renderInfo.playerSkin() : null);
         if (playerSkin2 == null) {
            return renderInfo;
         }

         Objects.requireNonNull(playerSkinRenderCache);
         return playerSkinRenderCache.new RenderInfo(gameProfile2, playerSkin2, Patch.EMPTY);
      } else {
         return renderInfo;
      }
   }

   private static String urlFromBase64Token(String text) {

      try {
         byte[] byteValues = FindTextureInTagHelper.decodeBase64(text);
         if (byteValues == null) {
            return null;
         }

         String text2 = new String(byteValues, StandardCharsets.UTF_8);
         Pattern hTTP_URL = HTTP_URL;
         String text3 = text2.replace("\\/", "/");
         Matcher matcher = hTTP_URL.matcher(text3);
         if (matcher.find()) {
            return matcher.group();
         }
      } catch (Throwable failure) {
      }

      return null;
   }

   private static <K, V> void putCapped(Map<K, V> lookup, K k, V v, int value) {
      lookup.put(k, v);
      if (lookup.size() > value) {
         int computedValue = lookup.size() - value + value / 4;
         Iterator iterator = lookup.keySet().iterator();

         while (computedValue-- > 0 && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
         }
      }
   }

   private static void evictEldestReady() {
      Iterator iterator = READY.entrySet().iterator();
      if (iterator.hasNext()) {
         Entry element = (Entry)iterator.next();
         Identifier value = (Identifier)element.getValue();
         if (BY_NICK.containsValue(value)) {
            iterator.remove();
            READY.put((String)element.getKey(), value);
            if (READY.size() > 48) {
               Iterator iterator2 = READY.entrySet().iterator();

               while (iterator2.hasNext()) {
                  Entry element2 = (Entry)iterator2.next();
                  if (!BY_NICK.containsValue(element2.getValue())) {
                     releaseTexture((Identifier)element2.getValue());
                     HASH_TO_KEY.entrySet().removeIf(entry -> {
                        return ((String)element2.getKey()).equals(entry.getValue());
                     });
                     iterator2.remove();
                     return;
                  }
               }
            }
         } else {
            iterator.remove();
            HASH_TO_KEY.entrySet().removeIf(entry2 -> {
               String key = (String)element.getKey();
               return key.equals(entry2.getValue());
            });
            releaseTexture(value);
         }
      }
   }

   private static String encodedTextures(String text) {
      String text2 = cleanUrl(text);
      if (text2 == null) {
         text2 = text;
      }

      Encoder encoder = Base64.getEncoder();
      String text3 = "{\"textures\":{\"SKIN\":{\"url\":\"" + text2.replace("\"", "") + "\"}}}";
      return encoder.encodeToString(text3.getBytes(StandardCharsets.UTF_8));
   }

   private static void markFailed(String text, String text2) {
      long now = System.currentTimeMillis();
      putCapped(FAILED, text, now, 64);
      String text3 = textureHash(text2);
      if (text3 != null) {
         var fAILED = FAILED;
         Long element = now;
         putCapped(fAILED, text3, element, 64);
      }

      PENDING.remove(text);
   }

   public static String extractUrl(GameProfile gameProfile) {
      if (gameProfile != null && gameProfile.properties() != null) {
         if (!gameProfile.properties().isEmpty()) {
            Iterator iterator = gameProfile.properties().values().iterator();

            String text;
            do {
               if (!iterator.hasNext()) {
                  return null;
               }

               Property element = (Property)iterator.next();
               text = urlFromProperty(element);
            } while (text == null);

            return cleanUrl(text);
         }
      }

      return null;
   }

   private static void releaseTexture(Identifier progress) {
      SKIN_BY_ID.remove(progress);
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         try {
            instance.getTextureManager().release(progress);
         } catch (Throwable failure) {
         }
      }
   }

   private static void rememberProfileUrl(ItemStack itemStack, String text) {
      if (itemStack != null && text != null) {
         ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
         if (resolvableProfile != null) {
            if (resolvableProfile.partialProfile() != null) {
               noteAppliedUrl(resolvableProfile.partialProfile(), text);
               return;
            }
         }

         noteAppliedUrl(null, text);
      }
   }

   public static String extractUrlFromBlob(String text) {
      if (text != null && !text.isBlank()) {
         Matcher matcher = HTTP_URL.matcher(text.replace("\\/", "/"));

         while (matcher.find()) {
            String text2 = cleanUrl(matcher.group());
            String text3 = text2.toLowerCase(Locale.ROOT);
            if (text3.contains("texture") || text3.contains("skin") || text3.contains("minecraft.net")) {
               return text2;
            }
         }

         matcher.reset();
         if (matcher.find()) {
            return cleanUrl(matcher.group());
         }

         Matcher matcher2 = EY_BASE64.matcher(text);

         while (matcher2.find()) {
            String text4 = matcher2.group().replace(" ", "").replace("\n", "").replace("\r", "");
            String text5 = urlFromBase64Token(text4);
            if (text5 != null) {
               return cleanUrl(text5);
            }
         }

         return null;
      } else {
         return null;
      }
   }

   public static void request(String text, String text2) {
      if (text2 == null || !BY_NICK.containsKey(text2)) {
         String text3 = cleanUrl(text);
         if (text3 != null && !text3.isBlank()) {
            if (text2 != null) {
               String text4 = NICK_URL.putIfAbsent(text2, text3);
               if (text4 != null) {
                  text3 = text4;
               }
            }

            String text5 = normalizeKey(text3);
            String text6 = textureHash(text3);
            synchronized (READY_LOCK) {
               Identifier texture = READY.get(text5);
               if (texture == null && text6 != null) {
                  String text7 = HASH_TO_KEY.get(text6);
                  if (text7 != null) {
                     texture = READY.get(text7);
                  }
               }

               if (texture != null) {
                  if (text2 != null) {
                     bindNick(text2, text3, texture);
                  }

                  return;
               }
            }

            long now = System.currentTimeMillis();
            Long element = FAILED.get(text5);
            if (element == null || now - element >= 30000L) {
               if (text6 != null) {
                  Long element2 = FAILED.get(text6);
                  if (element2 != null && now - element2 < 30000L) {
                     return;
                  }
               }

               Long element3 = PENDING.get(text5);
               if (element3 == null || now - element3 >= 60000L) {
                  PENDING.put(text5, now);
                  FAILED.remove(text5);
                  if (text6 != null) {
                     FAILED.remove(text6);
                  }

                  String text8 = text3;
                  String text9 = text2;
                  IO.execute(() -> {
                     downloadAndRegister(text8, text5, text9);
                  });
               }
            }
         }
      }
   }

   public static RenderInfo buildRenderInfo(PlayerSkinRenderCache playerSkinRenderCache, ItemStack itemStack, RenderInfo renderInfo) {
      return renderInfoForStack(playerSkinRenderCache, itemStack, renderInfo, true);
   }

   private static byte[] download(String text, int value) throws Exception {
      if (value <= 3 && text != null && !text.isBlank()) {
         Minecraft instance = Minecraft.getInstance();
         Proxy proxy = instance != null ? instance.getProxy() : Proxy.NO_PROXY;
         HttpURLConnection httpURLConnection = (HttpURLConnection)URI.create(text).toURL().openConnection(proxy);
         httpURLConnection.setInstanceFollowRedirects(false);
         httpURLConnection.setDoInput(true);
         httpURLConnection.setDoOutput(false);
         httpURLConnection.setConnectTimeout(15000);
         httpURLConnection.setReadTimeout(15000);
         httpURLConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Minecraft)");
         httpURLConnection.setRequestProperty("Accept", "*/*");
         httpURLConnection.connect();
         int responseCode = httpURLConnection.getResponseCode();
         if (responseCode != 301 && responseCode != 302 && responseCode != 303) {
            if (responseCode / 100 != 2) {
               httpURLConnection.disconnect();
               return null;
            }

            int contentLength = httpURLConnection.getContentLength();
            if (contentLength > 131072) {
               httpURLConnection.disconnect();
               return null;
            }

            try (
               InputStream inputStream = httpURLConnection.getInputStream();
               ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ) {
               byte[] byteValues = new byte[8192];
               int value2 = 0;

               int value3;
               while ((value3 = inputStream.read(byteValues)) >= 0) {
                  value2 += value3;
                  if (value2 > 131072) {
                     return null;
                  }

                  byteArrayOutputStream.write(byteValues, 0, value3);
               }

               return byteArrayOutputStream.toByteArray();
            } finally {
               httpURLConnection.disconnect();
            }
         } else {
            String headerField = httpURLConnection.getHeaderField("Location");
            httpURLConnection.disconnect();
            return headerField != null && !headerField.isBlank() ? download(cleanUrl(headerField), value + 1) : null;
         }
      } else {
         return null;
      }
   }

   public static void noteAppliedUrl(GameProfile gameProfile, String text) {
      if (text != null) {
         String text2 = cleanUrl(text);
         LAST_APPLIED_URL = text2;
         if (gameProfile != null) {
            if (gameProfile.id() != null) {
               putCapped(URL_BY_PROFILE, gameProfile.id(), text2, 64);
            }

            if (gameProfile.name() != null && !gameProfile.name().isBlank() && !"skycore".equalsIgnoreCase(gameProfile.name())) {
               String text3 = gameProfile.name().toLowerCase(Locale.ROOT);
               NICK_URL.putIfAbsent(text3, text2);
               putCapped(URL_BY_NAME, text3, NICK_URL.get(text3), 64);
            }
         }
      }
   }

   private static String resolveUrlFromStackInternal(ItemStack itemStack) {
      ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
      if (resolvableProfile != null) {
         String text = extractUrl(resolvableProfile.partialProfile());
         if (text != null) {
            return cleanUrl(text);
         }
      }

      String text2 = FindTextureInTagHelper.bestTextureValue(itemStack);
      if (text2 != null) {
         String text3 = extractUrlFromTextureValue(text2);
         if (text3 != null) {
            return cleanUrl(text3);
         }
      }

      return null;
   }

   public static String extractUrlFromTextureValue(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim();
         String text3 = text2.toLowerCase(Locale.ROOT);
         if (text3.startsWith("http://") || text3.startsWith("https://")) {
            return cleanUrl(text2);
         } else {
            return !TEXTURE_HASH.matcher(text2).matches() && (text2.length() < 32 || text2.length() > 128 || !text2.chars().allMatch(value -> {
               return value >= 48 && value <= 57 || value >= 97 && value <= 102 || value >= 65 && value <= 70;
            })) ? extractUrlFromBlob(text2) : cleanUrl("https://textures.minecraft.net/texture/" + text2);
         }
      } else {
         return null;
      }
   }

   private static void ensureProfileHasTextures(ItemStack itemStack, String text) {

      try {
         ResolvableProfile resolvableProfile = itemStack.get(DataComponents.PROFILE);
         if (resolvableProfile != null) {
            if (extractUrl(resolvableProfile.partialProfile()) != null) {
               FindTextureInTagHelper.ensureTextureUniqueProfile(itemStack);
               return;
            }
         }

         Encoder encoder = Base64.getEncoder();
         String text2 = text.replace("\"", "");
         String bytes = encoder.encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + text2 + "\"}}}").getBytes(StandardCharsets.UTF_8));
         UUID uUID = profileIdForUrl(text);
         ImmutableMultimap immutableMultimap = ImmutableMultimap.of("textures", new Property("textures", bytes));
         PropertyMap lookup = new PropertyMap(immutableMultimap);
         itemStack.set(DataComponents.PROFILE, ResolvableProfile.createResolved(new GameProfile(uUID, "skycore", lookup)));
      } catch (Throwable failure) {
      }
   }

   public static RenderInfo renderInfoForUrl(PlayerSkinRenderCache playerSkinRenderCache, String text, ItemStack itemStack, RenderInfo renderInfo) {
      if (text != null && playerSkinRenderCache != null) {
         String text2 = nickFromStack(itemStack);
         if (text2 != null && BY_NICK.containsKey(text2)) {
            PlayerSkin orDefault = skinForId(BY_NICK.get(text2), NICK_URL.getOrDefault(text2, text));
            Objects.requireNonNull(playerSkinRenderCache);
            return playerSkinRenderCache.new RenderInfo(gameProfileForUrl(NICK_URL.get(text2), itemStack, renderInfo), orDefault, Patch.EMPTY);
         }

         if (text2 != null && NICK_URL.containsKey(text2)) {
            text = NICK_URL.get(text2);
         }

         request(text, text2);
         PlayerSkin playerSkin = skinIfReadyUrl(text, text2);
         if (playerSkin == null) {
            return renderInfo;
         }

         GameProfile gameProfile = gameProfileForUrl(text, itemStack, renderInfo);
         Objects.requireNonNull(playerSkinRenderCache);
         return playerSkinRenderCache.new RenderInfo(gameProfile, playerSkin, Patch.EMPTY);
      } else {
         return renderInfo;
      }
   }

   public static void preloadFromStack(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty() && itemStack.is(Items.PLAYER_HEAD)) {
         String text = nickFromStack(itemStack);
         if (text != null) {
            if (BY_NICK.containsKey(text)) {
               return;
            }
         }

         String text2 = resolveUrlFromStackInternal(itemStack);
         if (text2 != null) {
            rememberProfileUrl(itemStack, text2);
            request(text2, text);
         }
      }
   }

   private static NativeImage ensureSkinSize(NativeImage nativeImage) {
      int width = nativeImage.getWidth();
      int height = nativeImage.getHeight();
      if (width != 64 || height != 32 && height != 64) {
         NativeImage nativeImage2 = new NativeImage(64, 64, true);

         try {
            if (width > 0 && height > 0) {
               for (int value = 0; value < 64; value++) {
                  int minimum = Math.min(height - 1, value * height / 64);

                  for (int value2 = 0; value2 < 64; value2++) {
                     int minimum2 = Math.min(width - 1, value2 * width / 64);
                     nativeImage2.setPixel(value2, value, nativeImage.getPixel(minimum2, minimum));
                  }
               }
            }

            nativeImage.close();
            return nativeImage2;
         } catch (Throwable failure) {
            nativeImage2.close();
            return nativeImage;
         }
      } else {
         return nativeImage;
      }
   }

   public static PlayerSkin skinIfReadyUrl(String text, String text2) {
      if (text != null && !text.isBlank()) {
         if (text2 != null) {
            Identifier texture = BY_NICK.get(text2);
            if (texture != null) {
               return skinForId(texture, NICK_URL.getOrDefault(text2, text));
            }
         }

         String text3 = cleanUrl(text);
         request(text3, text2);
         Identifier ifReady = getIfReady(text3);
         if (ifReady == null) {
            return null;
         }

         if (text2 != null) {
            bindNick(text2, text3, ifReady);
         }

         return skinForId(ifReady, text3);
      } else {
         return null;
      }
   }

   private static void bindNick(String text, String text2, Identifier texture) {
      if (text != null && texture != null) {
         if (BY_NICK.size() >= 48 && !BY_NICK.containsKey(text)) {
            Iterator iterator = BY_NICK.keySet().iterator();
            if (iterator.hasNext()) {
               String element = (String)iterator.next();
               iterator.remove();
               NICK_URL.remove(element);
            }
         }

         BY_NICK.putIfAbsent(text, texture);
         if (text2 != null) {
            NICK_URL.putIfAbsent(text, cleanUrl(text2));
         }
      }
   }

   public static String urlFromStack(ItemStack itemStack) {
      return resolveUrlFromStackInternal(itemStack);
   }

   public static void request(String text) {
      request(text, null);
   }

   private static void putReady(String text, String text2, Identifier texture) {
      synchronized (READY_LOCK) {
         while (true) {
            if (READY.size() < 24 || READY.isEmpty()) {
               READY.put(text, texture);
               String text3 = textureHash(text2);
               if (text3 != null) {
                  HASH_TO_KEY.put(text3, text);
               }

               return;
            }

            evictEldestReady();
         }
      }
   }

   public static String urlForProfile(GameProfile gameProfile) {
      if (gameProfile == null) {
         return null;
      }

      if (gameProfile.name() != null && !gameProfile.name().isBlank()) {
         String text = NICK_URL.get(gameProfile.name().toLowerCase(Locale.ROOT));
         if (text != null) {
            return text;
         }
      }

      String text2 = extractUrl(gameProfile);
      if (text2 != null) {
         return text2;
      }

      if (gameProfile.id() != null) {
         text2 = URL_BY_PROFILE.get(gameProfile.id());
         if (text2 != null) {
            return text2;
         }
      }

      return null;
   }

   public static UUID profileIdForUrl(String text) {
      String text2 = cleanUrl(text);
      if (text2 != null && !text2.isBlank()) {
         byte[] bytes = ("PastaSkull:url:" + text2.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8);
         return UUID.nameUUIDFromBytes(bytes);
      } else {
         return UUID.nameUUIDFromBytes("pasta-head".getBytes(StandardCharsets.UTF_8));
      }
   }

   public static String resolveUrlFromStack(ItemStack itemStack) {
      if (itemStack != null && !itemStack.isEmpty()) {
         if (itemStack.is(Items.PLAYER_HEAD)) {
            FindTextureInTagHelper.fixItemStack(itemStack);
            return resolveUrlFromStackInternal(itemStack);
         }
      }

      return null;
   }

   public static PlayerSkin skinIfReadyUrl(String text) {
      return skinIfReadyUrl(text, null);
   }
}
