package deluxe.client.cosmetic;

import deluxe.client.DeluxeClient;
import deluxe.client.cosmetic.CosmeticWarmup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class CosmeticPreviewAtlas {
   private static final Path field12607 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("cosmetics-cache");
   private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).followRedirects(Redirect.NORMAL).build();
   private static final Set<Identifier> field12608 = ConcurrentHashMap.newKeySet();
   private static final Set<Identifier> field12609 = ConcurrentHashMap.newKeySet();
   private static final Map<Integer, CosmeticPreviewAtlas.Nested0499> field12610 = new ConcurrentHashMap<>();
   private static Map<String, String> field12611;
   private static long field12612;

   public static boolean method3626(Identifier identifier, String local5942_1) {
      boolean local5942_2 = method3632(identifier, local5942_1, null);

      try {
         Thread.sleep(120L);
      } catch (InterruptedException exception) {
         Thread.currentThread().interrupt();
      }

      return local5942_2;
   }

   public static boolean method3627() {
      try {
         JsonObject asJsonObject = JsonParser.parseString(new String(method3631("previews/atlas/index.json"), StandardCharsets.UTF_8)).getAsJsonObject();
         float asFloat = asJsonObject.get("cell").getAsFloat();

         for (Entry<String, JsonElement> local5943_3 : asJsonObject.getAsJsonObject("atlases").entrySet()) {
            JsonObject asJsonObject2 = local5943_3.getValue().getAsJsonObject();
            String asString = asJsonObject2.get("file").getAsString();
            float asFloat2 = asJsonObject2.get("width").getAsFloat();
            float asFloat3 = asJsonObject2.get("height").getAsFloat();
            Identifier identifier = Identifier.of("deluxe", "cosmetics/" + asString);

            for (Entry<String, JsonElement> local5943_10 : asJsonObject2.getAsJsonObject("cells").entrySet()) {
               float asFloat4 = local5943_10.getValue().getAsJsonArray().get(0).getAsFloat();
               float asFloat5 = local5943_10.getValue().getAsJsonArray().get(1).getAsFloat();
               field12610.put(
                  Integer.parseInt(local5943_10.getKey()),
                  new CosmeticPreviewAtlas.Nested0499(
                     identifier,
                     identifier.toString(),
                     (asFloat4 + 0.5F) / asFloat2,
                     (asFloat5 + 0.5F) / asFloat3,
                     (asFloat4 + asFloat - 0.5F) / asFloat2,
                     (asFloat5 + asFloat - 0.5F) / asFloat3
                  )
               );
            }

            CosmeticWarmup.method2914(identifier, asString);
         }

         return true;
      } catch (Exception exception) {
         DeluxeClient.logger().warn("[Cosmetics] preview atlases not loaded: {}", exception.toString());
         return false;
      }
   }

   private static Map<String, String> method3628() {
      if (field12611 != null) {
         return field12611;
      }
      // Neutralized: Remote cosmetic manifest download disabled (offline safe mode).
      return null;
   }

   public static CosmeticPreviewAtlas.Nested0499 method3629(CosmeticEntry cosmeticEntry) {
      if (cosmeticEntry == null) {
         return null;
      }

      CosmeticPreviewAtlas.Nested0499 local5945_1 = field12610.get(cosmeticEntry.id());
      if (local5945_1 != null && field12608.contains(local5945_1.atlasId())) {
         return local5945_1;
      }

      CosmeticWarmup.method2913();
      return null;
   }

   public static synchronized byte[] method3631(String local5947_0) throws IOException {
      Path path = field12607.resolve(local5947_0);
      if (Files.isRegularFile(path)) {
         return Files.readAllBytes(path);
      } else {
         throw new IOException("Remote hosting disabled and no local cache for " + local5947_0);
      }
   }

   public static boolean method3632(Identifier identifier, String local5948_1, Runnable runnable) {
      if (field12609.contains(identifier)) {
         if (runnable != null) {
            MinecraftClient.getInstance().execute(runnable);
         }

         return true;
      } else {
         try {
            NativeImage nativeImage = NativeImage.read(method3631(local5948_1));
            field12609.add(identifier);
            MinecraftClient.getInstance().execute(() -> {
               NativeImageBackedTexture nativeImageBackedTexture = new NativeImageBackedTexture(identifier::toString, nativeImage);
               nativeImageBackedTexture.setImage(null);
               MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, nativeImageBackedTexture);
               field12608.add(identifier);
               if (runnable != null) {
                  runnable.run();
               }
            });
            return true;
         } catch (Exception exception) {
            DeluxeClient.logger().warn("[Cosmetics] texture {} not loaded: {}", local5948_1, exception.toString());
            return false;
         }
      }
   }

   private static String method3633(byte[] byteArray) {
      try {
         return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(byteArray));
      } catch (Exception exception) {
         throw new IllegalStateException(exception);
      }
   }

   private CosmeticPreviewAtlas() {
   }

   public record Nested0499(Identifier atlasId, String atlasTexture, float u0, float v0, float u1, float v1) {
   }
}
