package tokyo.ghoul.client.features.modules.autobuy.engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class AutoBuyRefreshProfile {
   public static final class RefreshPoint {
      private final long id;
      private final float position;
      private final long delayMs;
      private final boolean userCreated;

      public RefreshPoint(float position, long delayMs) {
         this(nextPointId(), position, delayMs, false);
      }

      public RefreshPoint(float position, long delayMs, boolean userCreated) {
         this(nextPointId(), position, delayMs, userCreated);
      }

      public RefreshPoint(long id, float position, long delayMs, boolean userCreated) {
         this.id = id;
         nextPointId = Math.max(nextPointId, id + 1L);
         this.position = Math.max(0f, Math.min(1f, position));
         this.delayMs = Math.max(0L, Math.min(1000L, delayMs));
         this.userCreated = userCreated;
      }

      public long getId() {
         return id;
      }

      public float getPosition() {
         return position;
      }

      public long getDelayMs() {
         return delayMs;
      }

      public boolean isUserCreated() {
         return userCreated;
      }
   }

   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private static final String FILE_NAME = "autobuy-refresh-profile.json";
   private static final String SAMPLES_KEY = "samples";
   private static final String CUSTOM_POINTS_KEY = "customPoints";
   private static final String UPDATED_AT_KEY = "updatedAt";
   private static final String REFRESH_BUTTON_TEXT = "обновить аукцион";
   private static final String REFRESH_BUTTON_TEXT_EN = "refresh auction";
   private static final int MAX_SAMPLES = 240;
   private static final long MIN_INTERVAL_MS = 250L;
   private static final long MAX_INTERVAL_MS = 15000L;

   private static final Object LOCK = new Object();
   private static final List<Long> samplesMs = new ArrayList<>();
   private static final String POINT_X_KEY = "x";
   private static final String POINT_ID_KEY = "id";
   private static final String POINT_DELAY_KEY = "delayMs";
   private static final String POINT_USER_CREATED_KEY = "userCreated";
   private static final int MAX_CUSTOM_POINTS = 7;
   private static final List<RefreshPoint> customPoints = new ArrayList<>();
   private static final float[] DEFAULT_CUSTOM_POINT_X = new float[]{0f, 0.36f, 0.70f, 1f};
   private static final long[] DEFAULT_CUSTOM_POINTS_MS = new long[]{0L, 250L, 500L, 250L};

   private static boolean loaded;
   private static long lastManualRefreshAtMs;
   private static int suppressRecordingDepth;
   private static long nextPointId = 1L;

   private AutoBuyRefreshProfile() {
      throw new UnsupportedOperationException("Utility class");
   }

   public static void recordRefreshClickIfManual(int syncId, int slotId, int button, ContainerInput actionType, Player player) {
      if (player == null || actionType != ContainerInput.PICKUP || button != 0 || isRecordingSuppressed()) {
         return;
      }

      AbstractContainerMenu handler = player.containerMenu;
      if (handler == null || handler.containerId != syncId || !AutoBuyUtil.isAuction(handler)) {
         return;
      }

      if (!isRefreshAuctionButton(handler, slotId)) {
         return;
      }

      long now = System.currentTimeMillis();
      synchronized (LOCK) {
         ensureLoadedLocked();
         if (lastManualRefreshAtMs > 0L) {
            long intervalMs = now - lastManualRefreshAtMs;
            if (intervalMs >= MIN_INTERVAL_MS && intervalMs <= MAX_INTERVAL_MS) {
               samplesMs.add(intervalMs);
               trimSamplesLocked();
               saveLocked(now);
            }
         }
         lastManualRefreshAtMs = now;
      }
   }

   public static long chooseDelayMs(long fallbackMinMs, long fallbackMaxMs) {
      synchronized (LOCK) {
         ensureLoadedLocked();
         if (!samplesMs.isEmpty()) {
            return samplesMs.get(ThreadLocalRandom.current().nextInt(samplesMs.size()));
         }
      }

      long minMs = Math.min(fallbackMinMs, fallbackMaxMs);
      long maxMs = Math.max(fallbackMinMs, fallbackMaxMs);
      return ThreadLocalRandom.current().nextLong(minMs, maxMs + 1L);
   }

   public static long chooseLearnedDelayMs() {
      synchronized (LOCK) {
         ensureLoadedLocked();
         if (!customPoints.isEmpty()) {
            return customPoints.get(ThreadLocalRandom.current().nextInt(customPoints.size())).getDelayMs();
         }
         if (samplesMs.isEmpty()) {
            return -1L;
         }

         return samplesMs.get(ThreadLocalRandom.current().nextInt(samplesMs.size()));
      }
   }

   public static void runWithoutRecording(Runnable action) {
      if (action == null) {
         return;
      }

      suppressRecordingDepth++;
      try {
         action.run();
      } finally {
         suppressRecordingDepth = Math.max(0, suppressRecordingDepth - 1);
      }
   }

   public static List<RefreshPoint> getCustomPoints() {
      synchronized (LOCK) {
         ensureLoadedLocked();
         return copyPoints(customPoints.isEmpty() ? defaultPoints() : customPoints);
      }
   }

   public static void setCustomPoints(List<RefreshPoint> points) {
      synchronized (LOCK) {
         ensureLoadedLocked();
         customPoints.clear();
         if (points != null) {
            for (RefreshPoint point : points) {
               if (point == null) continue;
               customPoints.add(new RefreshPoint(
                  point.getId(),
                  point.getPosition(),
                  point.getDelayMs(),
                  point.isUserCreated()
               ));
               if (customPoints.size() >= MAX_CUSTOM_POINTS) {
                  break;
               }
            }
         }
         normalizeCustomPointsLocked();
         saveLocked(System.currentTimeMillis());
      }
   }

   private static boolean isRecordingSuppressed() {
      return suppressRecordingDepth > 0;
   }

   private static boolean isRefreshAuctionButton(AbstractContainerMenu handler, int slotId) {
      if (handler == null || slotId < 0) {
         return false;
      }

      for (Slot slot : handler.slots) {
         if (slot == null || slot.index != slotId) {
            continue;
         }

         ItemStack stack = slot.getItem();
         if (stack == null || stack.isEmpty()) {
            return false;
         }

         String normalizedName = AutoBuyParsedItems.normalizeDisplayName(stack.getHoverName().getString())
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .trim();
         return (stack.getItem() == Items.EMERALD && normalizedName.contains(REFRESH_BUTTON_TEXT))
            || (slot.index == 47 && normalizedName.contains(REFRESH_BUTTON_TEXT))
            || normalizedName.contains(REFRESH_BUTTON_TEXT_EN);
      }

      return false;
   }

   private static void ensureLoadedLocked() {
      if (loaded) {
         return;
      }

      loaded = true;
      Path path = resolvePath();
      if (!Files.exists(path)) {
         return;
      }

      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
         JsonElement root = JsonParser.parseReader(reader);
         if (!root.isJsonObject()) {
            return;
         }

         JsonArray array = root.getAsJsonObject().getAsJsonArray(SAMPLES_KEY);
         if (array != null) {
            for (JsonElement element : array) {
               if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                  continue;
               }

               long value = element.getAsLong();
               if (value >= MIN_INTERVAL_MS && value <= MAX_INTERVAL_MS) {
                  samplesMs.add(value);
               }
            }
            trimSamplesLocked();
         }
         JsonArray customArray = root.getAsJsonObject().getAsJsonArray(CUSTOM_POINTS_KEY);
         if (customArray != null) {
            for (JsonElement element : customArray) {
               if (element.isJsonObject()) {
                  JsonObject pointObject = element.getAsJsonObject();
                  long id = pointObject.has(POINT_ID_KEY) ? pointObject.get(POINT_ID_KEY).getAsLong() : nextPointId();
                  float x = pointObject.has(POINT_X_KEY) ? pointObject.get(POINT_X_KEY).getAsFloat() : 0f;
                  long delay = pointObject.has(POINT_DELAY_KEY) ? pointObject.get(POINT_DELAY_KEY).getAsLong() : 0L;
                  boolean userCreated = pointObject.has(POINT_USER_CREATED_KEY)
                     && pointObject.get(POINT_USER_CREATED_KEY).getAsBoolean();
                  customPoints.add(new RefreshPoint(id, x, delay, userCreated));
               } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                  long value = element.getAsLong();
                  int index = customPoints.size();
                  float x = index < DEFAULT_CUSTOM_POINT_X.length
                          ? DEFAULT_CUSTOM_POINT_X[index]
                          : (index / (float) Math.max(1, DEFAULT_CUSTOM_POINTS_MS.length - 1));
                  customPoints.add(new RefreshPoint(nextPointId(), x, value, index >= DEFAULT_CUSTOM_POINTS_MS.length));
               }
               if (customPoints.size() >= MAX_CUSTOM_POINTS) {
                  break;
               }
            }
            normalizeCustomPointsLocked();
         }
      } catch (Exception ignored) {
      }
   }

   private static void saveLocked(long now) {
      Path path = resolvePath();
      try {
         Path parent = path.getParent();
         if (parent != null) {
            Files.createDirectories(parent);
         }

         JsonArray array = new JsonArray();
         for (Long sample : samplesMs) {
            array.add(sample);
         }
         JsonArray customArray = new JsonArray();
         List<RefreshPoint> pointsToSave = customPoints.isEmpty() ? defaultPoints() : customPoints;
         for (RefreshPoint point : pointsToSave) {
            JsonObject pointObject = new JsonObject();
            pointObject.addProperty(POINT_ID_KEY, point.getId());
            pointObject.addProperty(POINT_X_KEY, point.getPosition());
            pointObject.addProperty(POINT_DELAY_KEY, point.getDelayMs());
            pointObject.addProperty(POINT_USER_CREATED_KEY, point.isUserCreated());
            customArray.add(pointObject);
         }

         JsonObject root = new JsonObject();
         root.add(SAMPLES_KEY, array);
         root.add(CUSTOM_POINTS_KEY, customArray);
         root.addProperty(UPDATED_AT_KEY, now);

         try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            GSON.toJson(root, writer);
         }
      } catch (IOException ignored) {
      }
   }

   private static void trimSamplesLocked() {
      int overflow = samplesMs.size() - MAX_SAMPLES;
      if (overflow <= 0) {
         return;
      }

      samplesMs.subList(0, overflow).clear();
   }

   private static List<RefreshPoint> defaultPoints() {
      List<RefreshPoint> points = new ArrayList<>(DEFAULT_CUSTOM_POINTS_MS.length);
      for (int i = 0; i < DEFAULT_CUSTOM_POINTS_MS.length; i++) {
         points.add(new RefreshPoint(DEFAULT_CUSTOM_POINT_X[i], DEFAULT_CUSTOM_POINTS_MS[i], false));
      }
      return points;
   }

   private static List<RefreshPoint> copyPoints(List<RefreshPoint> source) {
      List<RefreshPoint> copy = new ArrayList<>(source.size());
      for (RefreshPoint point : source) {
         copy.add(new RefreshPoint(point.getId(), point.getPosition(), point.getDelayMs(), point.isUserCreated()));
      }
      return copy;
   }

   private static void normalizeCustomPointsLocked() {
      if (customPoints.isEmpty()) {
         customPoints.addAll(defaultPoints());
         return;
      }
      customPoints.sort((a, b) -> Float.compare(a.getPosition(), b.getPosition()));
      if (customPoints.size() > MAX_CUSTOM_POINTS) {
         customPoints.subList(MAX_CUSTOM_POINTS, customPoints.size()).clear();
      }
   }

   private static long nextPointId() {
      return nextPointId++;
   }

   private static Path resolvePath() {
      return Path.of(ClientInfo.CONFIG_PATH_OTHER, FILE_NAME);
   }
}
