package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.render.FontFace;
import deluxe.client.event.HudRenderEvent;
import deluxe.client.setting.KeybindSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.GuiScale;
import deluxe.client.render.WorldProjection;
import deluxe.client.render.FontText;
import deluxe.client.ui.hud.NotificationHud;
import deluxe.client.render.ShapeDrawing;
import deluxe.client.render.ThemeColors;
import deluxe.client.ui.hud.HudRenderContext;
import deluxe.client.ui.hud.HudStyle;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;

@ModuleInfo(
   name = "Party",
   description = "Пати с друзьями: видно участников на любом расстоянии и общие метки. Управление через .party",
   category = ModuleCategory.MISC
)
public final class PartyModule extends Module {
   private static PartyModule partyModule;
   public KeybindSetting field10049;
   public KeybindSetting field10050;
   private final HttpClient field10051 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private final ExecutorService field10052 = Executors.newSingleThreadExecutor(runnable -> {
      Thread thread = new Thread(runnable, "deluxe-party");
      thread.setDaemon(true);
      return thread;
   });
   private final String field10053 = UUID.randomUUID().toString();
   private final AtomicBoolean field10054 = new AtomicBoolean();
   private final HashMap<String, Vec3d> field10055 = new HashMap<>();
   private final HashSet<String> field10056 = new HashSet<>();
   private volatile PartyModule.Nested0462 field10057;
   private String field10058;
   private long field10059;
   private long field10060;
   private boolean field10061;
   private boolean field10062;
   private final EventListener<PlayerTickEvent> field10063 = playerTickEvent -> {
      if (mc.player != null && mc.world != null) {
         boolean local4677_2 = this.field10049.getBinding().method2749(mc.getWindow().getHandle());
         if (local4677_2 && !this.field10061 && mc.currentScreen == null) {
            this.method2954();
         }

         this.field10061 = local4677_2;
         boolean local4677_3 = this.field10050.getBinding().method2749(mc.getWindow().getHandle());
         if (local4677_3 && !this.field10062 && mc.currentScreen == null) {
            this.method2975();
         }

         this.field10062 = local4677_3;
         if (this.field10058 != null && System.currentTimeMillis() - this.field10059 >= 500L && this.field10054.compareAndSet(false, true)) {
            this.field10059 = System.currentTimeMillis();
            JsonObject jsonObject = this.method2950("beat");
            jsonObject.addProperty("x", mc.player.getX());
            jsonObject.addProperty("y", mc.player.getY());
            jsonObject.addProperty("z", mc.player.getZ());
            jsonObject.addProperty("hp", mc.player.getHealth() + mc.player.getAbsorptionAmount());
            this.field10052.execute(() -> {
               try {
                  JsonObject jsonObject2 = this.method2970(jsonObject);
                  mc.execute(() -> this.method2947(jsonObject2));
               } catch (Exception exception) {
               } finally {
                  this.field10054.set(false);
               }
            });
         }
      }
   };
   private final EventListener<HudRenderEvent> field10064 = hudRenderEvent -> {
      PartyModule.Nested0462 local4681_2 = this.field10057;
      if (local4681_2 != null && mc.player != null && mc.world != null) {
         long local4681_3 = System.currentTimeMillis();
         float local4681_5 = Math.min(1.0F, (float)(local4681_3 - this.field10060) / 1000.0F * 8.0F);
         this.field10060 = local4681_3;
         HudRenderContext.method4333(hudRenderEvent.method4459());
         String local4681_6 = this.method2938();
         String local4681_7 = this.method2973();
         String local4681_8 = mc.player.getGameProfile().name();

         for (PartyModule.Nested0461 local4681_10 : local4681_2.members()) {
            if (!local4681_10.nick().equals(local4681_8) && local4681_10.host().equals(local4681_6) && local4681_10.dim().equals(local4681_7)) {
               AbstractClientPlayerEntity abstractClientPlayerEntity = this.method2967(local4681_10.nick());
               Vec3d vec3d = abstractClientPlayerEntity != null ? abstractClientPlayerEntity.getLerpedPos(hudRenderEvent.method4461()) : local4681_10.position();
               Vec3d vec3d2 = this.field10055.get(local4681_10.nick());
               vec3d2 = vec3d2 != null && abstractClientPlayerEntity == null ? vec3d2.lerp(vec3d, local4681_5) : vec3d;
               this.field10055.put(local4681_10.nick(), vec3d2);
               String local4681_14 = (int)Math.ceil(
                     abstractClientPlayerEntity != null ? abstractClientPlayerEntity.getHealth() + abstractClientPlayerEntity.getAbsorptionAmount() : local4681_10.health()
                  )
                  + "hp";
               this.method2957(hudRenderEvent, vec3d2.add(0.0, 2.3, 0.0), local4681_10.nick(), local4681_14, HudStyle.method4820(1.0F), -1.0F);
            }
         }

         this.field10055.keySet().removeIf(local4676_1 -> local4681_2.members().stream().noneMatch(local4669_1 -> local4669_1.nick().equals(local4676_1)));

         for (PartyModule.Nested0463 local4681_10 : local4681_2.marks()) {
            if (local4681_10.host().equals(local4681_6) && local4681_10.dim().equals(local4681_7)) {
               if (local4681_10.type().equals("player")) {
                  AbstractClientPlayerEntity abstractClientPlayerEntity2 = this.method2967(local4681_10.target());
                  Vec3d vec3d3 = abstractClientPlayerEntity2 != null ? abstractClientPlayerEntity2.getLerpedPos(hudRenderEvent.method4461()) : local4681_10.position();
                  float local4681_13 = (float)mc.gameRenderer.getCamera().getCameraPos().distanceTo(vec3d3);
                  this.method2957(
                     hudRenderEvent,
                     vec3d3.add(0.0, 2.9, 0.0),
                     "Цель " + local4681_10.target(),
                     local4681_10.by(),
                     -43691,
                     Math.max(0.3F, 1.0F - local4681_13 * 0.008F)
                  );
               } else {
                  this.method2957(hudRenderEvent, local4681_10.position(), "Метка", local4681_10.by(), ThemeColors.field13669, -1.0F);
               }
            }
         }
      }
   };

   private void method2937(JsonObject jsonObject, Consumer<JsonObject> local4658_2) {
      this.field10052.execute(() -> {
         try {
            JsonObject jsonObject2 = this.method2970(jsonObject);
            mc.execute(() -> {
               if (jsonObject2.get("ok").getAsBoolean()) {
                  local4658_2.accept(jsonObject2);
               } else {
                  ClientChat.method2267("&cПати: " + this.method2965(jsonObject2.get("error").getAsString()));
               }
            });
         } catch (Exception exception) {
            mc.execute(() -> ClientChat.method2267("&cПати: сервер недоступен"));
         }
      });
   }

   private String method2938() {
      IntegratedServer server = mc.getServer();
      if (server != null) {
         return "lan:" + (server.isRemote() ? server.getServerPort() : 0);
      } else if (mc.getNetworkHandler() == null
         || !(
            mc.getNetworkHandler().getConnection().getAddress() instanceof InetSocketAddress inetSocketAddress
               && inetSocketAddress.getAddress() != null
               && (inetSocketAddress.getAddress().isLoopbackAddress() || inetSocketAddress.getAddress().isSiteLocalAddress())
         )) {
         ServerInfo currentServerEntry = mc.getCurrentServerEntry();
         return currentServerEntry == null ? "" : currentServerEntry.address.trim().toLowerCase(Locale.ROOT).replaceFirst(":25565$", "").replaceFirst("\\.$", "");
      } else {
         return "lan:" + inetSocketAddress.getPort();
      }
   }

   public void method2940(String local4661_1) {
      if (this.method2972()) {
         JsonObject jsonObject = this.method2950("kick");
         jsonObject.addProperty("target", local4661_1);
         this.method2937(jsonObject, jsonObject2 -> {
            this.method2963(jsonObject2);
            ClientChat.method2267("&7" + local4661_1 + "&r исключён из пати.");
         });
      }
   }

   private void method2947(JsonObject jsonObject) {
      if (this.field10058 != null) {
         if (jsonObject.get("ok").getAsBoolean()) {
            this.method2963(jsonObject);
         } else {
            String asString = jsonObject.get("error").getAsString();
            if (asString.equals("not_member") || asString.equals("not_found")) {
               String local4668_3 = this.field10058;
               this.method2949();
               ClientChat.method2267(
                  asString.equals("not_found") ? "Пати &7" + local4668_3 + "&r распущено." : "Тебя больше нет в пати &7" + local4668_3 + "&r."
               );
            }
         }
      }
   }

   private void method2949() {
      this.field10058 = null;
      this.field10057 = null;
      this.field10055.clear();
      this.field10056.clear();
   }

   private JsonObject method2950(String local4671_1) {
      JsonObject jsonObject = this.method2971(local4671_1);
      jsonObject.addProperty("code", this.field10058);
      jsonObject.addProperty("host", this.method2938());
      jsonObject.addProperty("dim", this.method2973());
      return jsonObject;
   }

   public void method2951() {
      if (this.field10058 == null) {
         ClientChat.method2267("Ты не в пати.");
      } else {
         JsonObject jsonObject = this.method2950("leave");
         this.method2949();
         this.method2937(jsonObject, jsonObject2 -> ClientChat.method2267("Ты вышел из пати."));
      }
   }

   public void method2952(String local4673_1) {
      if (mc.player != null) {
         if (!this.isEnabled()) {
            this.setEnabled(true);
         }

         if (this.field10058 != null) {
            this.method2951();
         }

         JsonObject jsonObject = this.method2971("join");
         jsonObject.addProperty("code", local4673_1.toUpperCase(Locale.ROOT));
         this.method2937(jsonObject, jsonObject2 -> {
            this.method2963(jsonObject2);
            ClientChat.method2267("Ты в пати &a" + this.field10058 + "&r, владелец: &7" + jsonObject2.get("owner").getAsString());
         });
      }
   }

   private void method2954() {
      if (this.method2972()) {
         HitResult hitResult = mc.player.raycast(256.0, 1.0F, false);
         if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == Type.BLOCK) {
            Vec3d vec3d = blockHitResult.getBlockPos().toCenterPos().add(0.0, 0.5, 0.0);
            this.method2962("point", "", vec3d);
         } else {
            NotificationHud.method2688(this.category.getIcon(), "Метка", "Слишком далеко", true);
         }
      }
   }

   private void method2957(HudRenderEvent hudRenderEvent, Vec3d vec3d, String local4678_3, String local4678_4, int local4678_5, float local4678_6) {
      Vec3d vec3d2 = WorldProjection.method1125(vec3d, hudRenderEvent.method4462(), hudRenderEvent.method4460());
      if (vec3d2 != null) {
         float local4678_8 = GuiScale.method0797() / GuiScale.method0794();
         float local4678_9 = (float)vec3d2.x * local4678_8;
         float local4678_10 = (float)vec3d2.y * local4678_8;
         float local4678_11 = (float)mc.player.getEntityPos().distanceTo(vec3d);
         String local4678_12 = String.format(Locale.US, "%.0fм", local4678_11);
         float local4678_13 = FontText.method1396(FontFace.SEMI_BOLD, local4678_3, 6.0F);
         float local4678_14 = FontText.method1396(FontFace.SEMI_BOLD, local4678_4, 6.0F);
         float local4678_15 = FontText.method1396(FontFace.SEMI_BOLD, local4678_12, 6.0F);
         float local4678_16 = 4.0F + local4678_13 + 3.0F + local4678_14 + 3.0F + local4678_15 + 4.0F;
         float local4678_17 = Math.round((local4678_9 - local4678_16 / 2.0F) * 2.0F) / 2.0F;
         float local4678_18 = Math.round((local4678_10 - 6.5F) * 2.0F) / 2.0F;
         HudRenderContext.method4336(local4678_9, local4678_10, local4678_6 > 0.0F ? local4678_6 : (float)Math.max(0.6, 1.1 - local4678_11 / 200.0));
         HudStyle.method4812(local4678_17, local4678_18, local4678_16, 13.0F, 1.0F, 4.5F, ShapeDrawing.Nested0502.LOW);
         float local4678_19 = local4678_18 + 6.5F - 3.5F;
         FontText.method1397(FontFace.SEMI_BOLD, local4678_3, local4678_17 + 4.0F, local4678_19, 6.0F, local4678_5);
         FontText.method1397(
            FontFace.SEMI_BOLD, local4678_4, local4678_17 + 4.0F + local4678_13 + 3.0F, local4678_19, 6.0F, HudStyle.method4815(1.0F)
         );
         FontText.method1397(
            FontFace.SEMI_BOLD,
            local4678_12,
            local4678_17 + 4.0F + local4678_13 + 3.0F + local4678_14 + 3.0F,
            local4678_19,
            6.0F,
            HudStyle.method4815(1.0F)
         );
         HudRenderContext.method4337();
      }
   }

   public void method2961(boolean local4682_1) {
      if (this.method2972()) {
         JsonObject jsonObject = this.method2950("lock");
         jsonObject.addProperty("locked", local4682_1);
         this.method2937(jsonObject, jsonObject2 -> {
            this.method2963(jsonObject2);
            ClientChat.method2267(local4682_1 ? "Пати &cзакрыто&r, новые игроки не зайдут." : "Пати &aоткрыто&r для входа.");
         });
      }
   }

   private void method2962(String local4683_1, String local4683_2, Vec3d vec3d) {
      JsonObject jsonObject = this.method2950("mark");
      jsonObject.addProperty("type", local4683_1);
      jsonObject.addProperty("target", local4683_2);
      jsonObject.addProperty("x", vec3d.x);
      jsonObject.addProperty("y", vec3d.y);
      jsonObject.addProperty("z", vec3d.z);
      this.method2937(
         jsonObject,
         jsonObject2 -> {
            this.method2963(jsonObject2);
            NotificationHud.method2688(
               this.category.getIcon(), "Метка", local4683_1.equals("player") ? "Отмечен " + local4683_2 : "Метка поставлена", true
            );
         }
      );
   }

   private void method2963(JsonObject jsonObject) {
      PartyModule.Nested0462 local4684_2 = this.field10057;
      ArrayList<PartyModule.Nested0461> local4684_3 = new ArrayList<>();

      for (JsonElement jsonElement : jsonObject.getAsJsonArray("members")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         local4684_3.add(
            new PartyModule.Nested0461(
               asJsonObject.get("nick").getAsString(),
               asJsonObject.get("host").getAsString(),
               asJsonObject.get("dim").getAsString(),
               new Vec3d(asJsonObject.get("x").getAsDouble(), asJsonObject.get("y").getAsDouble(), asJsonObject.get("z").getAsDouble()),
               asJsonObject.get("hp").getAsFloat()
            )
         );
      }

      ArrayList<PartyModule.Nested0463> local4684_4 = new ArrayList<>();

      for (JsonElement jsonElement2 : jsonObject.getAsJsonArray("marks")) {
         JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
         local4684_4.add(
            new PartyModule.Nested0463(
               asJsonObject2.get("id").getAsString(),
               asJsonObject2.get("by").getAsString(),
               asJsonObject2.get("type").getAsString(),
               asJsonObject2.get("target").getAsString(),
               asJsonObject2.get("host").getAsString(),
               asJsonObject2.get("dim").getAsString(),
               new Vec3d(asJsonObject2.get("x").getAsDouble(), asJsonObject2.get("y").getAsDouble(), asJsonObject2.get("z").getAsDouble())
            )
         );
      }

      PartyModule.Nested0462 local4684_5 = new PartyModule.Nested0462(
         jsonObject.get("code").getAsString(), jsonObject.get("owner").getAsString(), jsonObject.get("locked").getAsBoolean(), local4684_3, local4684_4
      );
      this.field10058 = local4684_5.code();
      this.field10057 = local4684_5;
      if (local4684_2 != null) {
         String local4684_6 = mc.player == null ? "" : mc.player.getGameProfile().name();

         for (PartyModule.Nested0461 local4684_8 : local4684_3) {
            if (local4684_2.members().stream().noneMatch(local4687_1 -> local4687_1.nick().equals(local4684_8.nick()))) {
               ClientChat.method2267("&a+ &7" + local4684_8.nick() + "&r зашёл в пати.");
            }
         }

         for (PartyModule.Nested0461 local4684_8 : local4684_2.members()) {
            if (local4684_3.stream().noneMatch(local4695_1 -> local4695_1.nick().equals(local4684_8.nick()))) {
               ClientChat.method2267("&c- &7" + local4684_8.nick() + "&r вышел из пати.");
            }
         }

         for (PartyModule.Nested0463 local4684_8 : local4684_4) {
            if (this.field10056.add(local4684_8.id()) && !local4684_8.by().equals(local4684_6)) {
               NotificationHud.method2688(
                  this.category.getIcon(),
                  local4684_8.by(),
                  local4684_8.type().equals("player") ? "отметил " + local4684_8.target() : "поставил метку",
                  true
               );
            }
         }

         this.field10056.removeIf(local4679_1 -> local4684_4.stream().noneMatch(local4662_1 -> local4662_1.id().equals(local4679_1)));
      } else {
         for (PartyModule.Nested0463 local4684_7 : local4684_4) {
            this.field10056.add(local4684_7.id());
         }
      }
   }

   public void method2964() {
      PartyModule.Nested0462 local4685_1 = this.field10057;
      if (local4685_1 == null) {
         ClientChat.method2267("Ты не в пати. &7.party create&r или &7.party join <код>");
      } else {
         StringBuilder stringBuilder = new StringBuilder();

         for (PartyModule.Nested0461 local4685_4 : local4685_1.members()) {
            stringBuilder.append(stringBuilder.isEmpty() ? "" : "&r, ")
               .append(local4685_4.nick().equals(local4685_1.owner()) ? "&e" : "&7")
               .append(local4685_4.nick());
         }

         ClientChat.method2267("Пати &a" + local4685_1.code() + "&r " + (local4685_1.locked() ? "&c(закрыто)" : "&a(открыто)") + "&r: " + stringBuilder);
      }
   }

   private String method2965(String local4686_1) {
      return switch (local4686_1) {
         case "not_found" -> "пати с таким кодом нет";
         case "locked" -> "пати закрыто";
         case "full" -> "пати заполнено";
         case "not_owner" -> "это может только создатель пати";
         case "not_member" -> "ты не в этом пати";
         default -> local4686_1;
      };
   }

   private AbstractClientPlayerEntity method2967(String local4688_1) {
      for (AbstractClientPlayerEntity abstractClientPlayerEntity : mc.world.getPlayers()) {
         if (abstractClientPlayerEntity.getGameProfile().name().equalsIgnoreCase(local4688_1)) {
            return abstractClientPlayerEntity;
         }
      }

      return null;
   }

   public static PartyModule method2968() {
      if (partyModule == null) {
         partyModule = new PartyModule();
      }

      return partyModule;
   }

   private JsonObject method2970(JsonObject jsonObject) throws Exception {
      // Neutralized: Party remote coordinates/health heartbeat telemetry disabled.
      throw new java.io.IOException("Party server disabled (offline mode)");
   }

   private JsonObject method2971(String local4692_1) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("action", local4692_1);
      jsonObject.addProperty("nick", mc.player == null ? "" : mc.player.getGameProfile().name());
      jsonObject.addProperty("session", this.field10053);
      return jsonObject;
   }

   private boolean method2972() {
      if (this.field10058 != null) {
         return true;
      }

      ClientChat.method2267("Ты не в пати. &7.party create&r или &7.party join <код>");
      return false;
   }

   private String method2973() {
      return mc.world == null ? "" : mc.world.getRegistryKey().getValue().getPath();
   }

   private void method2975() {
      if (this.method2972()) {
         AbstractClientPlayerEntity abstractClientPlayerEntity = null;
         double local4696_2 = 4.0;
         Vec3d eyePos = mc.player.getEyePos();
         Vec3d rotationVec = mc.player.getRotationVec(1.0F);

         for (AbstractClientPlayerEntity abstractClientPlayerEntity2 : mc.world.getPlayers()) {
            if (abstractClientPlayerEntity2 != mc.player) {
               Vec3d vec3d = abstractClientPlayerEntity2.getBoundingBox().getCenter().subtract(eyePos);
               double local4696_9 = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, vec3d.normalize().dotProduct(rotationVec)))));
               if (local4696_9 < local4696_2) {
                  local4696_2 = local4696_9;
                  abstractClientPlayerEntity = abstractClientPlayerEntity2;
               }
            }
         }

         if (abstractClientPlayerEntity == null) {
            NotificationHud.method2688(this.category.getIcon(), "Метка", "Наведись на игрока", true);
         } else {
            this.method2962("player", abstractClientPlayerEntity.getGameProfile().name(), abstractClientPlayerEntity.getEntityPos());
         }
      }
   }

   public void method2976() {
      if (mc.player != null) {
         if (!this.isEnabled()) {
            this.setEnabled(true);
         }

         if (this.field10058 != null) {
            this.method2951();
         }

         JsonObject jsonObject = this.method2971("create");
         this.method2937(jsonObject, jsonObject2 -> {
            this.method2963(jsonObject2);
            ClientChat.method2267("Пати создано, код: &a" + this.field10058 + "&r. Друзья заходят через &7.party join " + this.field10058);
         });
      }
   }

   public boolean method2977(String local4698_1) {
      PartyModule.Nested0462 local4698_2 = this.field10057;
      if (local4698_2 != null && local4698_1 != null) {
         for (PartyModule.Nested0461 local4698_4 : local4698_2.members()) {
            if (local4698_4.nick().equalsIgnoreCase(local4698_1)) {
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private PartyModule() {
      this.field10049 = this.addSetting(new KeybindSetting("Метка", "Ставит метку туда, куда смотришь. Видят все в пати."));
      this.field10050 = this.addSetting(new KeybindSetting("Метка на игрока", "Отмечает игрока, на которого смотришь. Видят все в пати."));
   }

   @Override
   protected void onDisable() {
      if (this.field10058 != null) {
         this.method2951();
      }
   }

   private record Nested0461(String nick, String host, String dim, Vec3d position, float health) {
   }

   private record Nested0462(String code, String owner, boolean locked, ArrayList<PartyModule.Nested0461> members, ArrayList<PartyModule.Nested0463> marks) {
   }

   private record Nested0463(String id, String by, String type, String target, String host, String dim, Vec3d position) {
   }
}
