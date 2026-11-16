package deluxe.client.util;

import deluxe.client.ui.UiTheme;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PlayerTickEvent;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.SuggestCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class IrcClient implements MinecraftAccess {
   private static IrcClient ircClient;
   private static final Style field09533 = Style.EMPTY.withFont(new Font(Identifier.of("deluxe", "irc"))).withColor(16777215).withoutShadow();
   private static final DateTimeFormatter field09534 = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
   private final HttpClient field09535 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
   private Path field09536;
   private boolean field09537;
   private volatile boolean field09538;
   private long field09539;
   private HashMap<String, String> field09540 = new HashMap<>();
   private final EventListener<PlayerTickEvent> field09541 = playerTickEvent -> {
      if (this.field09537 && !this.field09538 && System.currentTimeMillis() - this.field09539 >= 2000L) {
         this.field09538 = true;
         this.field09539 = System.currentTimeMillis();
         this.method2827(this.method2819("poll")).whenComplete((jsonObject, throwable) -> {
            this.field09538 = false;
            if (jsonObject != null && jsonObject.has("messages")) {
               for (JsonElement jsonElement : jsonObject.getAsJsonArray("messages")) {
                  mc.execute(() -> this.method2831(jsonElement.getAsJsonObject()));
               }
            }
         });
      }
   };

   private JsonObject method2819(String local4430_1) {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("action", local4430_1);
      jsonObject.addProperty("name", ProfileAPI.get().getModel().getName());
      jsonObject.addProperty("role", ProfileAPI.get().getModel().getRole().name());
      jsonObject.addProperty("nick", mc.getSession().getUsername());
      return jsonObject;
   }

   public void method2820(String local4431_1) {
      if (!local4431_1.isBlank()) {
         JsonObject jsonObject = this.method2819("send");
         Matcher matcher = Pattern.compile("^([\\p{L}\\p{N}_.\\-]{1,24}),\\s*(.+)$").matcher(local4431_1);
         boolean local4431_4 = matcher.matches() && this.field09540.containsKey(matcher.group(1).toLowerCase(Locale.ROOT));
         jsonObject.addProperty("text", local4431_4 ? matcher.group(2) : local4431_1);
         if (local4431_4) {
            jsonObject.addProperty("replyTo", matcher.group(1));
            jsonObject.addProperty("replyToNick", this.field09540.get(matcher.group(1).toLowerCase(Locale.ROOT)));
         }

         this.method2827(jsonObject).thenAccept(jsonObject2 -> {
            if (jsonObject2 == null) {
               ClientChat.method2267("&cIRC сейчас недоступен, попробуй позже.");
            } else if (!jsonObject2.get("ok").getAsBoolean()) {
               ClientChat.method2267("&c" + (jsonObject2.has("message") ? jsonObject2.get("message").getAsString() : "Сообщение не отправлено."));
            } else {
               this.field09539 = 0L;
            }
         });
      }
   }

   private int method2821(RoleObject roleObject) {
      return switch (roleObject.name()) {
         case "BETA" -> 11832575;
         case "YOUTUBE" -> 16734826;
         case "ADMIN" -> 16757575;
         default -> 13226457;
      };
   }

   private RoleObject method2823(String local4434_1) {
      try {
         return RoleObject.valueOf(local4434_1);
      } catch (Exception exception) {
         return RoleObject.DEFAULT;
      }
   }

   public void method2824() {
      this.method2827(this.method2819("online"))
         .thenAccept(
            jsonObject -> {
               if (jsonObject != null && jsonObject.has("online")) {
                  MutableText text = Text.literal("В IRC сейчас " + jsonObject.getAsJsonArray("online").size() + ": ");
                  boolean local4439_3 = true;

                  for (JsonElement jsonElement : jsonObject.getAsJsonArray("online")) {
                     if (!local4439_3) {
                        text.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
                     }

                     local4439_3 = false;
                     RoleObject roleObject = this.method2823(jsonElement.getAsJsonObject().get("role").getAsString());
                     text.append(Text.literal(String.valueOf((char)(57360 + roleObject.ordinal()))).setStyle(field09533)).append(" ");
                     text.append(
                        Text.literal(jsonElement.getAsJsonObject().get("name").getAsString()).setStyle(Style.EMPTY.withColor(this.method2821(roleObject)))
                     );
                  }

                  ClientChat.method2261(text);
               } else {
                  ClientChat.method2267("&cIRC сейчас недоступен, попробуй позже.");
               }
            }
         );
   }

   public boolean method2826() {
      return this.field09537;
   }

   private CompletableFuture<JsonObject> method2827(JsonObject jsonObject) {
      // Neutralized: Remote IRC backend polling disabled for privacy and offline security.
      return CompletableFuture.completedFuture(null);
   }

   public void method2829(boolean local4440_1) {
      this.field09537 = local4440_1;
      this.field09539 = 0L;
      if (!local4440_1) {
         this.method2827(this.method2819("leave"));
      }

      try {
         Files.createDirectories(this.field09536.getParent());
         JsonObject jsonObject = new JsonObject();
         jsonObject.addProperty("enabled", local4440_1);
         Files.writeString(this.field09536, jsonObject.toString(), StandardCharsets.UTF_8);
      } catch (Exception exception) {
      }
   }

   private IrcClient() {
      this.field09536 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("irc.json");

      try {
         if (Files.exists(this.field09536)) {
            this.field09537 = JsonParser.parseString(Files.readString(this.field09536, StandardCharsets.UTF_8)).getAsJsonObject().get("enabled").getAsBoolean();
         }
      } catch (Exception exception) {
      }
   }

   private void method2831(JsonObject jsonObject) {
      String asString = jsonObject.get("name").getAsString();
      String asString2 = jsonObject.get("text").getAsString();
      String asString3 = jsonObject.get("nick").getAsString();
      String name = ProfileAPI.get().getModel().getName();
      String username = mc.getSession().getUsername();
      String local4444_7 = jsonObject.has("replyTo") ? jsonObject.get("replyTo").getAsString() : "";
      String local4444_8 = jsonObject.has("replyToNick") ? jsonObject.get("replyToNick").getAsString() : "";
      RoleObject roleObject = this.method2823(jsonObject.get("role").getAsString());
      boolean local4444_10 = asString.equalsIgnoreCase(name) && asString3.equalsIgnoreCase(username);
      boolean local4444_11 = !local4444_10 && local4444_7.equalsIgnoreCase(name) && (local4444_8.isEmpty() || local4444_8.equalsIgnoreCase(username));
      boolean local4444_12 = local4444_11 || !local4444_10 && asString2.toLowerCase(Locale.ROOT).contains("@" + name.toLowerCase(Locale.ROOT));
      if (!local4444_10) {
         this.field09540.put(asString.toLowerCase(Locale.ROOT), asString3);
      }

      Text text = Text.literal(roleObject.toString())
         .setStyle(Style.EMPTY.withColor(this.method2821(roleObject)))
         .append(
            Text.literal("\nНик в игре: " + (jsonObject.get("nick").getAsString().isEmpty() ? "—" : jsonObject.get("nick").getAsString()))
               .formatted(Formatting.GRAY)
         )
         .append(Text.literal("\nОтправлено: " + field09534.format(Instant.ofEpochSecond(jsonObject.get("time").getAsLong()))).formatted(Formatting.GRAY));
      ClickEvent clickEvent = new SuggestCommand("@" + asString + ", ");
      MutableText text2 = Text.empty();
      text2.append(Text.literal("\ue000").setStyle(field09533.withColor(local4444_11 ? 16763709 : UiTheme.field09686 & 16777215)));
      text2.append(Text.literal(" "));
      text2.append(Text.literal(String.valueOf((char)(57600 + Math.floorMod(jsonObject.get("emoji").getAsInt(), 20)))).setStyle(field09533));
      text2.append(Text.literal(" "));
      HoverEvent hoverEvent = ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN) ? new ShowText(text) : null;
      text2.append(Text.literal(String.valueOf((char)(57360 + roleObject.ordinal()))).setStyle(field09533.withHoverEvent(hoverEvent)));
      text2.append(Text.literal(" "));
      text2.append(
         Text.literal(asString).setStyle(Style.EMPTY.withColor(this.method2821(roleObject)).withHoverEvent(hoverEvent).withClickEvent(clickEvent))
      );
      if (local4444_11) {
         text2.append(Text.literal(" "))
            .append(Text.literal("\ue014").setStyle(field09533.withHoverEvent(new ShowText(Text.literal(asString + " ответил тебе")))));
      } else if (!local4444_7.isEmpty()) {
         text2.append(Text.literal(" → ").formatted(Formatting.DARK_GRAY)).append(Text.literal(local4444_7).setStyle(Style.EMPTY.withColor(10134445)));
      }

      text2.append(Text.literal(" » ").formatted(Formatting.DARK_GRAY));
      text2.append(Text.literal(asString2).setStyle(Style.EMPTY.withColor(local4444_11 ? 16765788 : (local4444_12 ? 16769902 : 15593459))));
      text2.append(Text.literal(" "));
      text2.append(
         Text.literal("\ue001")
            .setStyle(field09533.withColor(9279397).withHoverEvent(new ShowText(Text.literal("Ответить " + asString))).withClickEvent(clickEvent))
      );
      ClientChat.method2260(text2);
      if (local4444_12) {
         SoundPlayer.method3158("irc_mention");
         if (local4444_11) {
            mc.inGameHud
               .setOverlayMessage(
                  Text.empty()
                     .append(Text.literal("\ue014").setStyle(field09533))
                     .append(Text.literal(" " + asString).setStyle(Style.EMPTY.withColor(this.method2821(roleObject))))
                     .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
                     .append(
                        Text.literal(asString2.length() > 60 ? asString2.substring(0, 60) + "…" : asString2).setStyle(Style.EMPTY.withColor(16765788))
                     ),
                  false
               );
         }
      }
   }

   public static IrcClient method2834() {
      if (ircClient == null) {
         ircClient = new IrcClient();
      }

      return ircClient;
   }
}
