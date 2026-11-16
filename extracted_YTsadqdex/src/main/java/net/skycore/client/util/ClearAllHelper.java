package net.skycore.client.util;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.DownloadedPackSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ClearAllHelper {
   private static final Map<UUID, ClearAllHelper.StickyPack> STICKY = new ConcurrentHashMap<>();
   private static final Logger LOGGER = LoggerFactory.getLogger("skycore/server-pack");

   public static void clearAll() {
      STICKY.clear();
   }

   private static Path findCachedDownload(UUID uUID, URL uRL) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && instance.gameDirectory != null) {
         Path path = instance.gameDirectory.toPath().resolve("downloads").resolve(uUID.toString());
         if (!Files.isDirectory(path)) {
            return null;
         }

         try (var stream = Files.list(path)) {
            return stream.filter(path2 -> {
               return Files.isRegularFile(path2);
            }).findFirst().orElse(null);
         } catch (Exception failure) {
            return null;
         }
      } else {
         return null;
      }
   }

   public static void restoreLater() {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         instance.execute(() -> {
            DownloadedPackSource downloadedPackSource = instance.getDownloadedPackSource();
            if (downloadedPackSource != null) {
               restore(downloadedPackSource);
            }
         });
      }
   }

   public static void remember(UUID uUID, URL uRL, Path path) {
      if (uUID != null && path != null) {
         LinkOption[] linkOptionValues = new LinkOption[0];
         if (Files.isRegularFile(path, linkOptionValues)) {
            var sTICKY = STICKY;
            Path path2 = path.toAbsolutePath();
            sTICKY.put(uUID, new ClearAllHelper.StickyPack(uUID, uRL, path2));
            Logger lOGGER = LOGGER;
            Path fileName = path.getFileName();
            lOGGER.info("Remembered server pack {} -> {}", uUID, fileName);
            return;
         }
      }
   }

   public static boolean hasSticky() {
      return !STICKY.isEmpty();
   }

   private ClearAllHelper() {
   }

   public static void restore(DownloadedPackSource downloadedPackSource) {
      if (downloadedPackSource != null && !STICKY.isEmpty()) {
         downloadedPackSource.allowServerPacks();

         for (ClearAllHelper.StickyPack stickyPack : STICKY.values()) {
            Path path = stickyPack.path;
            if (path == null || !Files.isRegularFile(path)) {
               path = findCachedDownload(stickyPack.id, stickyPack.url);
            }

            if (path != null && Files.isRegularFile(path)) {
               stickyPack.path = path;

               try {
                  downloadedPackSource.pushLocalPack(stickyPack.id, path);
                  LOGGER.info("Restored server pack {}", stickyPack.id);
               } catch (Throwable failure) {
                  LOGGER.warn("Failed to restore server pack {}", stickyPack.id, failure);
               }
            } else {
               LOGGER.warn("Cannot restore server pack {}, cache missing", stickyPack.id);
            }
         }
      }
   }

   public static void rememberDownloaded(UUID uUID, URL uRL, String text) {
      if (uUID != null) {
         ClearAllHelper.StickyPack stickyPack = STICKY.get(uUID);
         if (stickyPack == null || stickyPack.path == null) {
            STICKY.putIfAbsent(uUID, new ClearAllHelper.StickyPack(uUID, uRL, null));
         }
      }
   }

   static final class StickyPack {
      final UUID id;
      Path path;
      final URL url;

      public StickyPack(UUID id2, URL url2, Path path2) {
         this.id = id2;
         this.url = url2;
         this.path = path2;
      }
   }
}
