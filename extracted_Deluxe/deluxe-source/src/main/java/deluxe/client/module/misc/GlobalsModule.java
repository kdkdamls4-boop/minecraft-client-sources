package deluxe.client.module.misc;

import deluxe.client.setting.BooleanSetting;
import deluxe.client.event.ClientTickEvent;
import deluxe.client.cosmetic.CosmeticCatalog;
import deluxe.client.cosmetic.CosmeticCategory;
import deluxe.client.cosmetic.CosmeticEntry;
import deluxe.client.cosmetic.CosmeticStore;
import deluxe.client.ui.DeluxeCosmeticsScreen;
import deluxe.client.event.EventListener;
import deluxe.client.storage.FriendStore;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.setting.ModeSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;

@ModuleInfo(name = "Globals", description = "Синхронизирует косметику с игроками, у которых тоже включён Globals.", category = ModuleCategory.MISC)
public final class GlobalsModule extends Module {
   private static GlobalsModule globalsModule;
   public BooleanSetting booleanSetting;
   public ModeSetting modeSetting;
   public KeybindSetting keybindSetting;
   private final HttpClient field00893 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final ExecutorService field00894 = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "deluxe-globals-sync");
      thread.setDaemon(true);
      return thread;
   });
   private final AtomicBoolean field00895 = new AtomicBoolean();
   private volatile String field00896;
   private volatile GlobalsModule.Nested0358 field00897;
   private String field00898;
   private String field00899;
   private String field00900;
   private long field00901;
   private boolean field00902;
   public final EventListener<ClientTickEvent> field00903 = clientTickEvent -> {
      if (clientTickEvent.isPre()) {
         boolean local809_2 = this.keybindSetting.getBinding().method2749(mc.getWindow().getHandle());
         if (local809_2 && !this.field00902 && mc.currentScreen == null && mc.player != null) {
            mc.setScreen(new DeluxeCosmeticsScreen());
         }

         this.field00902 = local809_2;
         ServerInfo currentServerEntry = mc.getCurrentServerEntry();
         IntegratedServer server = mc.getServer();
         boolean local809_5 = server != null && server.isRemote();
         boolean local809_6 = this.booleanSetting.isChecked()
            && mc.player != null
            && mc.world != null
            && mc.getNetworkHandler() != null
            && (currentServerEntry != null || local809_5);
         InetSocketAddress inetSocketAddress = local809_6
               && mc.getNetworkHandler().getConnection().getAddress() instanceof InetSocketAddress inetSocketAddress2
               && inetSocketAddress2.getAddress() != null
            ? inetSocketAddress2
            : null;
         String local809_8 = local809_6 ? mc.player.getGameProfile().name().toLowerCase(Locale.ROOT) : null;
         String local809_9 = !local809_6
            ? null
            : (
               local809_5
                  ? "lan:" + server.getServerPort()
                  : (
                     inetSocketAddress == null || !inetSocketAddress.getAddress().isLoopbackAddress() && !inetSocketAddress.getAddress().isSiteLocalAddress()
                        ? currentServerEntry.address.trim().toLowerCase(Locale.ROOT).replaceFirst(":25565$", "").replaceFirst("\\.$", "")
                        : "lan:" + inetSocketAddress.getPort()
                  )
            );
         if (!Objects.equals(local809_8, this.field00898) || !Objects.equals(local809_9, this.field00899)) {
            this.method0636();
            if (local809_8 == null) {
               return;
            }

            this.field00898 = local809_8;
            this.field00899 = local809_9;
            this.field00896 = UUID.randomUUID().toString();
         }

         if (local809_8 != null && System.currentTimeMillis() - this.field00901 >= 1000L) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("action", "beat");
            jsonObject.addProperty("nick", local809_8);
            jsonObject.addProperty("session", this.field00896);
            jsonObject.addProperty("host", local809_9);
            jsonObject.addProperty("ip", inetSocketAddress != null ? inetSocketAddress.getAddress().getHostAddress() + ":" + inetSocketAddress.getPort() : "");
            JsonObject jsonObject2 = new JsonObject();

            for (CosmeticCategory cosmeticCategory : CosmeticCategory.values()) {
               CosmeticEntry cosmeticEntry = CosmeticStore.method3519(cosmeticCategory);
               jsonObject2.addProperty(cosmeticCategory.name().toLowerCase(Locale.ROOT), cosmeticEntry == null ? -1 : cosmeticEntry.id());
            }

            jsonObject.add("selections", jsonObject2);
            JsonArray jsonArray = new JsonArray();

            for (String local809_14 : FriendStore.method3432()) {
               jsonArray.add(local809_14.toLowerCase(Locale.ROOT));
            }

            jsonObject.add("friends", jsonArray);
            jsonObject.addProperty("audience", this.modeSetting.isSelected("Все с Globals") ? "all" : "friends");
            LinkedHashSet<String> local809_13 = new LinkedHashSet<>();

            for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
               if (abstractClientPlayerEntity != mc.player) {
                  local809_13.add(abstractClientPlayerEntity.getGameProfile().name().toLowerCase(Locale.ROOT));
               }
            }

            TreeSet<String> local809_14 = new TreeSet<>();

            for (PlayerListEntry playerListEntry : mc.getNetworkHandler().getPlayerList()) {
               local809_14.add(playerListEntry.getProfile().name().toLowerCase(Locale.ROOT));
            }

            local809_14.remove(local809_8);
            local809_13.addAll(local809_14);
            JsonArray jsonArray2 = new JsonArray();

            for (String local809_17 : local809_13) {
               if (jsonArray2.size() >= 300) {
                  break;
               }

               jsonArray2.add(local809_17);
            }

            jsonObject.add("nearby", jsonArray2);
            String local809_16 = jsonObject.toString();
            if (System.currentTimeMillis() - this.field00901 >= (local809_16.equals(this.field00900) ? 3000L : 1000L)) {
               if (this.field00895.compareAndSet(false, true)) {
                  this.field00901 = System.currentTimeMillis();
                  this.field00900 = local809_16;
                  String local809_17 = this.field00896;
                  this.field00894.execute(() -> {
                     try {
                        JsonObject asJsonObject = JsonParser.parseString(this.method0644(local809_16)).getAsJsonObject().getAsJsonObject("players");
                        HashMap<String, EnumMap<CosmeticCategory, Integer>> local812_4 = new HashMap<>();

                        for (Entry<String, JsonElement> local812_6 : asJsonObject.entrySet()) {
                           EnumMap<CosmeticCategory, Integer> local812_7 = new EnumMap<>(CosmeticCategory.class);

                           for (CosmeticCategory cosmeticCategory2 : CosmeticCategory.values()) {
                              JsonElement jsonElement = local812_6.getValue().getAsJsonObject().get(cosmeticCategory2.name().toLowerCase(Locale.ROOT));
                              if (jsonElement != null && jsonElement.isJsonPrimitive()) {
                                 local812_7.put(cosmeticCategory2, jsonElement.getAsInt());
                              }
                           }

                           local812_4.put(local812_6.getKey(), local812_7);
                        }

                        this.field00897 = new GlobalsModule.Nested0358(local809_17, local812_4, System.currentTimeMillis());
                     } catch (Exception var16x) {
                     } finally {
                        this.field00895.set(false);
                     }
                  });
               }
            }
         }
      }
   };

   private Future<?> method0636() {
      String local805_1 = this.field00896;
      String local805_2 = this.field00898;
      this.field00896 = null;
      this.field00898 = null;
      this.field00899 = null;
      this.field00897 = null;
      this.field00900 = null;
      this.field00901 = 0L;
      if (local805_1 == null) {
         return null;
      }

      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("action", "leave");
      jsonObject.addProperty("nick", local805_2);
      jsonObject.addProperty("session", local805_1);
      return this.field00894.submit(() -> {
         try {
            this.method0644(jsonObject.toString());
         } catch (Exception var3x) {
         }
      });
   }

   public boolean method0639() {
      return this.field00897 != null;
   }

   public CosmeticEntry method0641(String local810_1, CosmeticCategory cosmeticCategory) {
      GlobalsModule.Nested0358 local810_3 = this.field00897;
      if (local810_3 == null
         || local810_1 == null
         || !local810_3.sessionId().equals(this.field00896)
         || System.currentTimeMillis() - local810_3.receivedAtMs() > 12000L) {
         return null;
      }

      if (this.modeSetting.isSelected("Только друзья") && !FriendStore.method3431(local810_1)) {
         return null;
      }

      EnumMap<CosmeticCategory, Integer> local810_4 = local810_3.selectionsByNick().get(local810_1.toLowerCase(Locale.ROOT));
      CosmeticEntry cosmeticEntry = local810_4 == null ? null : CosmeticCatalog.method0518(local810_4.getOrDefault(cosmeticCategory, -1));
      return cosmeticEntry != null && cosmeticEntry.category() == cosmeticCategory ? cosmeticEntry : null;
   }

   @Override
   protected void onDisable() {
      this.method0636();
   }

   private GlobalsModule() {
      this.booleanSetting = this.addSetting(new BooleanSetting("Синхронизация моделей", "Игроки с включённым Globals видят твою косметику, а ты их.", true));
      this.modeSetting = this.addSetting(
         new ModeSetting(
            "С кем",
            "Только друзья - обмен косметикой только с друзьями из .friend, Все с Globals - с любым игроком, у кого включён Globals.",
            "Только друзья",
            "Только друзья",
            "Все с Globals"
         )
      );
      this.modeSetting.withVisibility(() -> this.booleanSetting.isChecked());
      this.keybindSetting = this.addSetting(new KeybindSetting("Меню косметики", "Открывает меню косметики прямо в игре."));
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)minecraftClient -> {
         Future<?> local804_2 = this.method0636();
         if (local804_2 != null) {
            try {
               local804_2.get(1500L, TimeUnit.MILLISECONDS);
            } catch (Exception exception) {
            }
         }
      });
   }

   public static GlobalsModule method0643() {
      if (globalsModule == null) {
         globalsModule = new GlobalsModule();
      }

      return globalsModule;
   }

   private String method0644(String local815_1) throws IOException, InterruptedException {
      // Neutralized: Remote telemetry and cosmetic sync disabled for privacy.
      throw new IOException("Remote sync disabled (offline mode)");
   }

   private record Nested0358(String sessionId, HashMap<String, EnumMap<CosmeticCategory, Integer>> selectionsByNick, long receivedAtMs) {
   }
}
