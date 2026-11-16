package deluxe.client.module.misc;

import deluxe.client.util.ClientChat;
import deluxe.client.event.EventListener;
import deluxe.client.setting.LegacyNumberSetting;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.setting.NumericTextSetting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.inventory.Inventory;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "AutoDeliver", description = "Автоматическая выдача валюты по заказам FunPay.", category = ModuleCategory.MISC)
public final class AutoDeliverModule extends Module {
   private static AutoDeliverModule autoDeliverModule;
   public NumericTextSetting field13027;
   public NumericTextSetting field13028;
   public LegacyNumberSetting legacyNumberSetting;
   public NumericTextSetting field13030;
   private static final Pattern field13031 = Pattern.compile("data-app-data=\"([^\"]*)\"");
   private static final Pattern field13032 = Pattern.compile("<form[^>]*action=\"([^\"]*)\"");
   private static final Pattern field13033 = Pattern.compile("<input[^>]*>");
   private static final Pattern field13034 = Pattern.compile("(\\d+)\\s*шт\\.");
   private static final Pattern field13035 = Pattern.compile("Анархия-(\\d+)");
   private final HttpClient field13036 = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).followRedirects(Redirect.NORMAL).build();
   private ScheduledExecutorService field13037;
   private String field13038;
   private String field13039;
   private final HashMap<String, String> field13040 = new HashMap<>();
   private String field13041 = Integer.toHexString((int)(System.nanoTime() & 2147483647L));
   private ArrayList<AutoDeliverModule.Nested0506> field13042;
   private ArrayList<AutoDeliverModule.Nested0505> field13043;
   private final ArrayList<String> field13044 = new ArrayList<>();
   private final ConcurrentLinkedQueue<AutoDeliverModule.Nested0504> field13045 = new ConcurrentLinkedQueue<>();
   private long field13046;
   private long field13047;
   private boolean field13048;
   private final EventListener<PlayerTickEvent> field13049 = playerTickEvent -> {
      if (mc.player != null && mc.world != null && System.currentTimeMillis() - this.field13046 >= 300L) {
         AutoDeliverModule.Nested0504 local6233_2 = this.field13045.peek();
         if (local6233_2 != null) {
            this.field13048 = false;
            if (local6233_2.field13203) {
               if (mc.currentScreen != null
                  && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler
                  && mc.currentScreen.getTitle().getString().contains("Кикнуть")) {
                  Inventory inventory = genericContainerScreenHandler.getInventory();

                  for (int index = 0; index < inventory.size(); index++) {
                     if (inventory.getStack(index).getName().getString().contains("Подтвердить")) {
                        InventoryUtils.method0547(index, 0, SlotActionType.PICKUP, true);
                        mc.player.closeHandledScreen();
                        this.field13045.poll();
                        this.field13046 = System.currentTimeMillis();
                        if (this.field13037 != null) {
                           this.field13037
                              .execute(
                                 () -> this.method3764(
                                    local6233_2.field13193,
                                    "✅ Готово, валюта у вас — приятной игры!\n\n\ud83d\ude4f Подтвердите заказ и оставьте отзыв, мне это очень поможет:\n\ud83d\udd17 https://funpay.com/orders/"
                                       + (local6233_2.field13194.startsWith("#") ? local6233_2.field13194.substring(1) : local6233_2.field13194)
                                       + "/\n\n\ud83d\udc9b Спасибо за покупку!"
                                 )
                              );
                        }

                        return;
                     }
                  }
               }

               if (System.currentTimeMillis() - local6233_2.field13198 >= 1500L) {
                  local6233_2.field13198 = System.currentTimeMillis();
                  this.field13046 = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan kick " + local6233_2.field13192);
               }
            } else if (this.method3783() != this.method3778()) {
               if (System.currentTimeMillis() - this.field13047 >= 5000L) {
                  this.field13047 = System.currentTimeMillis();
                  this.field13046 = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand(this.field13030.getText().replace("/", ""));
               }
            } else if (!local6233_2.field13199) {
               if (mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(local6233_2.field13192) != null) {
                  if (System.currentTimeMillis() - local6233_2.field13196 >= 2500L) {
                     local6233_2.field13196 = System.currentTimeMillis();
                     this.field13046 = System.currentTimeMillis();
                     mc.player.networkHandler.sendChatCommand("clan invite " + local6233_2.field13192);
                  }
               }
            } else if (!local6233_2.field13200) {
               local6233_2.field13200 = true;
               this.field13046 = System.currentTimeMillis();
               mc.player.networkHandler.sendChatCommand("clan invest " + local6233_2.field13195 * 1000000L);
            } else if (local6233_2.field13201) {
               if (!local6233_2.field13202) {
                  local6233_2.field13202 = true;
                  if (this.field13037 != null) {
                     this.field13037
                        .execute(
                           () -> this.method3764(
                              local6233_2.field13193,
                              "\ud83d\udcb0 Права выданы, ваши "
                                 + local6233_2.field13195
                                 + " кк уже лежат в казне клана!\n\n\ud83d\udce5 Напишите в игровом чате: /clan withdraw all\n\ud83d\udcb5 Деньги упадут вам сразу же\n\n⚡ После вывода я выкину вас из клана — так и задумано"
                           )
                        );
                  }
               }
            } else {
               if (mc.currentScreen != null && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler genericContainerScreenHandler2) {
                  Inventory inventory2 = genericContainerScreenHandler2.getInventory();
                  int local6233_5 = -1;
                  int local6233_6 = -1;

                  for (int index2 = 0; index2 < inventory2.size(); index2++) {
                     String string = inventory2.getStack(index2).getName().getString();
                     if (local6233_5 == -1 && string.toLowerCase().contains(local6233_2.field13192.toLowerCase())) {
                        local6233_5 = index2;
                     }

                     if (local6233_6 == -1 && string.contains("Список участников")) {
                        local6233_6 = index2;
                     }
                  }

                  if (local6233_5 != -1) {
                     InventoryUtils.method0547(local6233_5, 0, SlotActionType.QUICK_MOVE, true);
                     mc.player.closeHandledScreen();
                     local6233_2.field13201 = true;
                     this.field13046 = System.currentTimeMillis();
                     return;
                  }

                  if (local6233_6 != -1) {
                     InventoryUtils.method0547(local6233_6, 0, SlotActionType.PICKUP, true);
                     this.field13046 = System.currentTimeMillis();
                     return;
                  }
               }

               if (System.currentTimeMillis() - local6233_2.field13197 >= 1500L) {
                  local6233_2.field13197 = System.currentTimeMillis();
                  this.field13046 = System.currentTimeMillis();
                  mc.player.networkHandler.sendChatCommand("clan menu");
               }
            }
         } else if (!this.field13048) {
            this.field13048 = true;
            this.field13046 = System.currentTimeMillis();
            mc.player.networkHandler.sendChatCommand("hub");
         }
      }
   };
   private final EventListener<PacketEvent> field13050 = packetEvent -> {
      if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket) {
         String var6 = gameMessageS2CPacket.content().getString().replaceAll("(?i)§[0-9a-fk-or]", "").toLowerCase();

         for (AutoDeliverModule.Nested0504 local6245_5 : this.field13045) {
            if (local6245_5.field13192 != null && var6.contains(local6245_5.field13192.toLowerCase())) {
               if (var6.contains("присоединился к клану")) {
                  local6245_5.field13199 = true;
               } else if (var6.contains("из казны клана")) {
                  local6245_5.field13203 = true;
               }
            }
         }
      }
   };

   private void method3764(String local6218_1, String local6218_2) {
      if (local6218_1 != null && !local6218_1.isEmpty()) {
         try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("node", local6218_1);
            jsonObject.addProperty("last_message", -1);
            jsonObject.addProperty("content", local6218_2);
            JsonObject jsonObject2 = new JsonObject();
            jsonObject2.addProperty("action", "chat_message");
            jsonObject2.add("data", jsonObject);
            JsonObject jsonObject3 = new JsonObject();
            jsonObject3.addProperty("type", "chat_node");
            jsonObject3.addProperty("id", local6218_1);
            jsonObject3.addProperty("tag", "00000000");
            JsonObject jsonObject4 = new JsonObject();
            jsonObject4.addProperty("node", local6218_1);
            jsonObject4.addProperty("last_message", -1);
            jsonObject4.addProperty("content", "");
            jsonObject3.add("data", jsonObject4);
            JsonArray jsonArray = new JsonArray();
            jsonArray.add(jsonObject3);
            if (this.method3765(this.method3794(jsonArray.toString(), jsonObject2.toString()))) {
               return;
            }

            this.field13038 = null;
            if (this.method3770()) {
               this.method3794(jsonArray.toString(), jsonObject2.toString());
            }
         } catch (Exception exception) {
         }
      }
   }

   private boolean method3765(HttpResponse<String> local6219_1) {
      if (local6219_1.statusCode() != 200) {
         return false;
      }

      try {
         JsonObject asJsonObject = JsonParser.parseString(local6219_1.body()).getAsJsonObject();
         if (asJsonObject.has("error") && !asJsonObject.get("error").isJsonNull()) {
            return false;
         } else {
            return asJsonObject.has("response") && asJsonObject.get("response").isJsonObject()
               ? !asJsonObject.getAsJsonObject("response").has("error") || asJsonObject.getAsJsonObject("response").get("error").isJsonNull()
               : true;
         }
      } catch (Exception exception) {
         return false;
      }
   }

   private boolean method3766(double local6220_1) {
      if (this.field13028.getText().isBlank()) {
         return true;
      }

      try {
         double local6220_3 = Double.parseDouble(this.field13028.getText().replace(',', '.').replaceAll("[^0-9.]", ""));
         return local6220_3 <= 0.0 || Math.abs(local6220_1 - local6220_3) <= local6220_3 * 0.25;
      } catch (Exception exception) {
         return true;
      }
   }

   private String method3768(String local6222_1, String local6222_2) {
      Matcher matcher = Pattern.compile(local6222_2 + "=\"([^\"]*)\"").matcher(local6222_1);
      return matcher.find() ? matcher.group(1) : "";
   }

   private String method3769() {
      StringBuilder stringBuilder = new StringBuilder("golden_key=").append(this.field13027.getText());

      for (String local6223_3 : this.field13040.keySet()) {
         stringBuilder.append("; ").append(local6223_3).append('=').append(this.field13040.get(local6223_3));
      }

      return stringBuilder.toString();
   }

   @Override
   protected void onEnable() {
      if (this.field13027.getText().isBlank()) {
         ClientChat.method2267("Укажите Golden-Key в настройках AutoDeliver");
         this.toggle();
      } else {
         this.field13038 = null;
         this.field13042 = null;
         this.field13043 = null;
         this.field13044.clear();
         this.field13045.clear();
         this.field13048 = false;
         this.field13037 = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "deluxe-funpay-deliver");
            thread.setDaemon(true);
            return thread;
         });
         this.field13037.scheduleWithFixedDelay(this::method3771, 0L, 4L, TimeUnit.SECONDS);
      }
   }

   private boolean method3770() {
      if (this.field13038 != null) {
         return true;
      }

      try {
         this.field13040.clear();
         HttpResponse<String> local6225_1 = this.field13036
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", "golden_key=" + this.field13027.getText())
                  .GET()
                  .build(),
               BodyHandlers.ofString()
            );
         if (this.method3792(local6225_1.body(), "user-link-name").isEmpty()) {
            return false;
         }

         Matcher matcher = field13031.matcher(local6225_1.body());
         if (!matcher.find()) {
            return false;
         }

         JsonObject asJsonObject = JsonParser.parseString(this.method3781(matcher.group(1))).getAsJsonObject();
         this.field13038 = asJsonObject.get("userId").getAsString();
         this.field13039 = asJsonObject.get("csrf-token").getAsString();
         this.method3776(local6225_1);
         this.field13042 = null;
         return true;
      } catch (Exception exception) {
         return false;
      }
   }

   private void method3771() {
      try {
         if (!this.method3770()) {
            return;
         }

         this.method3785();
         this.method3796();
      } catch (Exception exception) {
         this.field13038 = null;
      }
   }

   @Override
   protected void onDisable() {
      if (this.field13037 != null) {
         this.field13037.shutdownNow();
      }

      this.field13037 = null;
      this.field13045.clear();
   }

   private ArrayList<AutoDeliverModule.Nested0505> method3775() {
      ArrayList<AutoDeliverModule.Nested0505> local6231_1 = new ArrayList<>();

      try {
         HttpResponse<String> local6231_2 = this.field13036
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/orders/trade"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.method3769())
                  .header("accept", "*/*")
                  .header("x-requested-with", "XMLHttpRequest")
                  .POST(BodyPublishers.noBody())
                  .build(),
               BodyHandlers.ofString()
            );
         this.method3776(local6231_2);
         String local6231_3 = local6231_2.body();

         for (String local6231_5 : this.method3780(local6231_3, "tc-item")) {
            try {
               String local6231_6 = local6231_5.contains("order-desc") ? local6231_5.substring(local6231_5.indexOf("order-desc")) : "";
               if (this.method3792(local6231_5, "tc-status").equalsIgnoreCase("Оплачен") && this.method3792(local6231_6, "text-muted").contains("Валюта")) {
                  String local6231_7 = this.method3792(local6231_5, "order-desc");
                  int local6231_8 = 1;
                  Matcher matcher = field13034.matcher(local6231_7);

                  while (matcher.find()) {
                     local6231_8 = Integer.parseInt(matcher.group(1));
                  }

                  double local6231_10 = Double.parseDouble(this.method3792(local6231_5, "tc-price").replace(',', '.').replaceAll("[^0-9.]", ""));
                  if (this.method3766(local6231_10 / local6231_8)) {
                     String local6231_12 = this.method3792(local6231_5, "media-user-name");
                     if (!local6231_12.isEmpty()) {
                        local6231_1.add(new AutoDeliverModule.Nested0505(this.method3792(local6231_5, "tc-order"), local6231_12, local6231_10, local6231_8));
                     }
                  }
               }
            } catch (Exception exception) {
            }
         }

         return local6231_1;
      } catch (Exception exception2) {
         return null;
      }
   }

   private void method3776(HttpResponse<String> local6232_1) {
      for (String local6232_3 : local6232_1.headers().allValues("set-cookie")) {
         String local6232_4 = local6232_3.split(";", 2)[0].trim();
         if (local6232_4.indexOf(61) > 0 && !local6232_4.startsWith("golden_key=")) {
            this.field13040.put(local6232_4.substring(0, local6232_4.indexOf(61)), local6232_4.substring(local6232_4.indexOf(61) + 1));
         }
      }
   }

   private int method3778() {
      String local6234_1 = this.field13030.getText().replaceAll("\\D", "");
      return local6234_1.isEmpty() ? -1 : Integer.parseInt(local6234_1);
   }

   private ArrayList<AutoDeliverModule.Nested0506> method3779() throws Exception {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("type", "chat_bookmarks");
      jsonObject.addProperty("id", this.field13038);
      jsonObject.addProperty("tag", this.field13041);
      jsonObject.addProperty("data", false);
      JsonArray jsonArray = new JsonArray();
      jsonArray.add(jsonObject);
      ArrayList<AutoDeliverModule.Nested0506> local6235_3 = new ArrayList<>();

      for (JsonElement jsonElement : JsonParser.parseString(this.method3794(jsonArray.toString(), String.valueOf(false)).body())
         .getAsJsonObject()
         .getAsJsonArray("objects")) {
         JsonObject asJsonObject = jsonElement.getAsJsonObject();
         if ("chat_bookmarks".equalsIgnoreCase(asJsonObject.get("type").getAsString())) {
            this.field13041 = asJsonObject.get("tag").getAsString();

            for (String local6235_8 : this.method3780(asJsonObject.getAsJsonObject("data").get("html").getAsString(), "contact-item")) {
               AutoDeliverModule.Nested0506 local6235_9 = new AutoDeliverModule.Nested0506();
               local6235_9.field13204 = this.method3768(local6235_8, "data-id");
               local6235_9.field13207 = this.method3768(local6235_8, "data-node-msg").isEmpty()
                  ? 0L
                  : Long.parseLong(this.method3768(local6235_8, "data-node-msg"));
               local6235_9.field13205 = this.method3792(local6235_8, "media-user-name");
               if (!local6235_9.field13204.isEmpty() && !local6235_9.field13205.isEmpty()) {
                  local6235_3.add(local6235_9);
               }
            }
         }
      }

      return local6235_3;
   }

   private ArrayList<String> method3780(String local6236_1, String local6236_2) {
      ArrayList<String> local6236_3 = new ArrayList<>();
      Matcher matcher = this.method3790(local6236_2).matcher(local6236_1);

      int local6236_5;
      for (local6236_5 = -1; matcher.find(); local6236_5 = Math.max(local6236_1.lastIndexOf(60, matcher.start()), 0)) {
         if (local6236_5 != -1) {
            local6236_3.add(local6236_1.substring(local6236_5, matcher.start()));
         }
      }

      if (local6236_5 != -1) {
         local6236_3.add(local6236_1.substring(local6236_5));
      }

      return local6236_3;
   }

   private String method3781(String local6237_1) {
      return local6237_1.replace("&quot;", "\"")
         .replace("&#039;", "'")
         .replace("&apos;", "'")
         .replace("&lt;", "<")
         .replace("&gt;", ">")
         .replace("&nbsp;", " ")
         .replace(" ", " ")
         .replace("&amp;", "&");
   }

   private void method3782(String local6238_1) {
      try {
         HttpResponse<String> local6238_2 = this.field13036
            .send(
               HttpRequest.newBuilder(URI.create("https://funpay.com/orders/" + (local6238_1.startsWith("#") ? local6238_1.substring(1) : local6238_1) + "/"))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.method3769())
                  .GET()
                  .build(),
               BodyHandlers.ofString()
            );
         this.method3776(local6238_2);
         String local6238_3 = local6238_2.body();
         if (!local6238_3.contains("modal-refund")) {
            return;
         }

         String local6238_4 = local6238_3.substring(local6238_3.indexOf("modal-refund"));
         Matcher matcher = field13032.matcher(local6238_4);
         if (!matcher.find()) {
            return;
         }

         String local6238_6 = local6238_4.contains("</form>") ? local6238_4.substring(0, local6238_4.indexOf("</form>")) : local6238_4;
         StringBuilder stringBuilder = new StringBuilder();
         Matcher matcher2 = field13033.matcher(local6238_6);

         while (matcher2.find()) {
            if (matcher2.group().contains("type=\"hidden\"")) {
               stringBuilder.append(URLEncoder.encode(this.method3768(matcher2.group(), "name"), StandardCharsets.UTF_8))
                  .append('=')
                  .append(URLEncoder.encode(this.method3768(matcher2.group(), "value"), StandardCharsets.UTF_8))
                  .append('&');
            }
         }

         stringBuilder.append("csrf_token=").append(URLEncoder.encode(this.field13039 == null ? "" : this.field13039, StandardCharsets.UTF_8));
         this.field13036
            .send(
               HttpRequest.newBuilder(URI.create(matcher.group(1).startsWith("http") ? matcher.group(1) : "https://funpay.com" + matcher.group(1)))
                  .timeout(Duration.ofSeconds(5L))
                  .header("cookie", this.method3769())
                  .header("accept", "*/*")
                  .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                  .header("x-requested-with", "XMLHttpRequest")
                  .POST(BodyPublishers.ofString(stringBuilder.toString(), StandardCharsets.UTF_8))
                  .build(),
               BodyHandlers.ofString()
            );
      } catch (Exception exception) {
      }
   }

   private int method3783() {
      if (mc.world == null) {
         return -1;
      }

      ScoreboardObjective objectiveForSlot = mc.world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      if (objectiveForSlot == null) {
         return -1;
      }

      Matcher matcher = field13035.matcher(objectiveForSlot.getDisplayName().getString().replaceAll("(?i)§[0-9a-fk-or]", ""));
      return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
   }

   private void method3785() throws Exception {
      ArrayList<AutoDeliverModule.Nested0506> local6241_1 = this.method3779();
      if (!local6241_1.isEmpty()) {
         ArrayList<AutoDeliverModule.Nested0506> local6241_2 = this.field13042;
         this.field13042 = local6241_1;
         if (local6241_2 != null) {
            for (AutoDeliverModule.Nested0506 local6241_4 : local6241_1) {
               AutoDeliverModule.Nested0506 local6241_5 = local6241_2.stream()
                  .filter(local6228_1 -> local6228_1.field13204.equals(local6241_4.field13204))
                  .findFirst()
                  .orElse(null);
               long local6241_6 = local6241_5 != null ? local6241_5.field13207 : 0L;
               if (local6241_5 != null) {
                  local6241_4.field13208 = local6241_5.field13208;
                  local6241_4.field13206 = local6241_5.field13206;
                  local6241_4.field13209 = local6241_5.field13209;
               }

               if (local6241_4.field13207 > local6241_6) {
                  JsonObject jsonObject = new JsonObject();
                  jsonObject.addProperty("type", "chat_node");
                  jsonObject.addProperty("id", local6241_4.field13204);
                  jsonObject.addProperty("tag", "00000000");
                  JsonObject jsonObject2 = new JsonObject();
                  jsonObject2.addProperty("node", local6241_4.field13204);
                  jsonObject2.addProperty("last_message", -1);
                  jsonObject2.addProperty("content", "");
                  jsonObject.add("data", jsonObject2);
                  JsonArray jsonArray = new JsonArray();
                  jsonArray.add(jsonObject);

                  for (JsonElement jsonElement : JsonParser.parseString(this.method3794(jsonArray.toString(), String.valueOf(false)).body())
                     .getAsJsonObject()
                     .getAsJsonArray("objects")) {
                     JsonObject asJsonObject = jsonElement.getAsJsonObject();
                     if ("chat_node".equalsIgnoreCase(asJsonObject.get("type").getAsString())) {
                        for (JsonElement jsonElement2 : asJsonObject.getAsJsonObject("data").getAsJsonArray("messages")) {
                           JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
                           if (asJsonObject2.get("id").getAsLong() > local6241_6
                              && !asJsonObject2.get("author").getAsString().equals(this.field13038)
                              && !asJsonObject2.get("author").getAsString().equals("0")) {
                              String local6241_17 = this.method3792(asJsonObject2.get("html").getAsString(), "chat-msg-text");
                              String local6241_18 = local6241_17.replaceAll("[^a-zA-Zа-яА-Я]", "").toLowerCase();
                              AutoDeliverModule.Nested0505 local6241_19 = this.method3799(local6241_4.field13205);
                              if (local6241_19 == null
                                 || local6241_4.field13208 != AutoDeliverModule.Nested0507.NICKNAME
                                 || !local6241_18.equals("отмена") && !local6241_18.equals("otmena")) {
                                 if (local6241_4.field13208 == AutoDeliverModule.Nested0507.NICKNAME) {
                                    local6241_4.field13206 = local6241_17.replaceAll("[^a-zA-Z0-9_]", "");
                                    local6241_4.field13208 = AutoDeliverModule.Nested0507.CONFIRM_NICKNAME;
                                    this.method3764(
                                       local6241_4.field13204,
                                       "\ud83d\udd0e Проверьте данные перед выдачей\n\n\ud83d\udc64 Ник: "
                                          + local6241_4.field13206
                                          + "\n\ud83d\udce6 Сумма: "
                                          + (local6241_19 != null ? local6241_19.units() : 0)
                                          + " кк\n\n✅ Всё верно — напишите «Да» или «+»\n✏️ Ошибся в нике — напишите «Нет» или «-»"
                                    );
                                 } else if (local6241_4.field13208 == AutoDeliverModule.Nested0507.CONFIRM_NICKNAME) {
                                    if (!local6241_18.equals("да") && !local6241_18.equals("da") && !local6241_17.contains("+") && !local6241_17.contains("✓")) {
                                       if (local6241_18.equals("нет") || local6241_18.equals("net") || local6241_17.contains("-") || local6241_17.contains("✗")
                                          )
                                        {
                                          local6241_4.field13208 = AutoDeliverModule.Nested0507.NICKNAME;
                                          this.method3764(local6241_4.field13204, "✏️ Хорошо, напишите ваш игровой ник ещё раз одним сообщением");
                                       }
                                    } else if (local6241_19 != null && local6241_19.units() > 0) {
                                       AutoDeliverModule.Nested0504 local6241_20 = new AutoDeliverModule.Nested0504();
                                       local6241_20.field13192 = local6241_4.field13206;
                                       local6241_20.field13193 = local6241_4.field13204;
                                       local6241_20.field13194 = local6241_19.id();
                                       local6241_20.field13195 = local6241_19.units();
                                       this.field13045.add(local6241_20);
                                       this.method3764(
                                          local6241_4.field13204,
                                          "\ud83d\ude80 Данные приняты, начинаю выдачу!\n\n1️⃣ Зайдите на Анархию-"
                                             + this.method3778()
                                             + "\n2️⃣ Примите приглашение в клан — оно уже летит вам\n3️⃣ Дождитесь моего сообщения и напишите /clan withdraw all\n\n\ud83d\udce6 К выдаче: "
                                             + local6241_19.units()
                                             + " кк"
                                       );
                                       local6241_4.field13208 = AutoDeliverModule.Nested0507.DELIVERY;
                                       local6241_4.field13209 = System.currentTimeMillis();
                                    } else {
                                       this.method3764(local6241_4.field13204, "⏳ Не вижу ваш оплаченный заказ, напишите мне через минуту — выдам сразу же!");
                                    }
                                 }
                              } else {
                                 local6241_4.field13208 = null;
                                 this.method3782(local6241_19.id());
                                 this.method3764(
                                    local6241_4.field13204,
                                    "↩️ Заказ отменён, средства вернулись на ваш баланс FunPay\n\n\ud83d\udc9b Будем рады видеть вас снова!"
                                 );
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private AutoDeliverModule() {
      this.field13027 = this.addSetting(new NumericTextSetting("Golden-Key", "Ключ сессии FunPay из cookie golden_key.", "", 64));
      this.field13028 = this.addSetting(
         new NumericTextSetting("Цена за 1кк", "Своя цена лота без комиссии FunPay, пусто - обслуживать любые заказы на валюту.", "", 16)
      );
      this.legacyNumberSetting = this.addSetting(
         new LegacyNumberSetting("Продавать при сумме от ляма", "Заказы меньше этой суммы бот возвращает покупателю.", 10.0, 1.0, 50.0, 1.0)
      );
      this.field13030 = this.addSetting(new NumericTextSetting("Анархия выдачи", "Куда бот летит выдавать и куда зовёт покупателя.", "an305", 16));
   }

   private Pattern method3790(String local6247_1) {
      return Pattern.compile("class=\"[^\"]*(?<![-\\w])" + local6247_1 + "(?![-\\w])[^\"]*\"");
   }

   private String method3792(String local6249_1, String local6249_2) {
      Matcher matcher = this.method3790(local6249_2).matcher(local6249_1);
      if (!matcher.find()) {
         return "";
      }

      int local6249_4 = local6249_1.indexOf(62, matcher.end());

      while (local6249_4 != -1) {
         int local6249_5 = local6249_1.indexOf(60, local6249_4 + 1);
         String local6249_6 = this.method3781(local6249_5 == -1 ? local6249_1.substring(local6249_4 + 1) : local6249_1.substring(local6249_4 + 1, local6249_5))
            .trim();
         if (!local6249_6.isEmpty()) {
            return local6249_6;
         }

         if (local6249_5 == -1) {
            return "";
         }

         local6249_4 = local6249_1.indexOf(62, local6249_5);
      }

      return "";
   }

   private HttpResponse<String> method3794(String local6251_1, String local6251_2) throws Exception {
      String local6251_3 = "objects="
         + URLEncoder.encode(local6251_1 == null ? "" : local6251_1, StandardCharsets.UTF_8)
         + "&request="
         + URLEncoder.encode(local6251_2 == null ? "" : local6251_2, StandardCharsets.UTF_8)
         + "&csrf_token="
         + URLEncoder.encode(this.field13039 == null ? "" : this.field13039, StandardCharsets.UTF_8);
      HttpResponse<String> local6251_4 = this.field13036
         .send(
            HttpRequest.newBuilder(URI.create("https://funpay.com/runner/"))
               .timeout(Duration.ofSeconds(5L))
               .header("cookie", this.method3769())
               .header("accept", "*/*")
               .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
               .header("x-requested-with", "XMLHttpRequest")
               .POST(BodyPublishers.ofString(local6251_3, StandardCharsets.UTF_8))
               .build(),
            BodyHandlers.ofString()
         );
      this.method3776(local6251_4);
      return local6251_4;
   }

   private void method3796() {
      ArrayList<AutoDeliverModule.Nested0505> local6253_1 = this.method3775();
      if (local6253_1 != null) {
         ArrayList<AutoDeliverModule.Nested0505> local6253_2 = this.field13043;
         this.field13043 = local6253_1;
         if (local6253_2 != null) {
            if (this.field13042 != null) {
               for (AutoDeliverModule.Nested0505 local6253_4 : local6253_2) {
                  if (!local6253_1.stream().anyMatch(local6242_1 -> local6242_1.id().equals(local6253_4.id()))) {
                     this.field13042
                        .stream()
                        .filter(local6230_1 -> local6230_1.field13205.equalsIgnoreCase(local6253_4.buyerLogin()))
                        .filter(local6254_0 -> local6254_0.field13208 != null)
                        .findFirst()
                        .ifPresent(local6240_0 -> local6240_0.field13208 = null);
                  }
               }

               for (AutoDeliverModule.Nested0505 local6253_4 : local6253_1) {
                  if (!this.field13044.contains(local6253_4.id())) {
                     this.field13044.add(local6253_4.id());
                     this.field13042
                        .stream()
                        .filter(local6221_1 -> local6221_1.field13205.equalsIgnoreCase(local6253_4.buyerLogin()))
                        .filter(local6250_0 -> local6250_0.field13208 == null)
                        .findFirst()
                        .ifPresent(
                           local6227_2 -> {
                              if (local6253_4.units() < this.legacyNumberSetting.getValue()) {
                                 this.method3782(local6253_4.id());
                                 this.method3764(
                                    local6227_2.field13204,
                                    "❌ Минимальный заказ — "
                                       + this.legacyNumberSetting.intValue()
                                       + " кк, ваш меньше\n\n\ud83d\udcb8 Средства уже вернулись на ваш баланс FunPay, извините за неудобство!"
                                 );
                              } else {
                                 this.method3764(
                                    local6227_2.field13204,
                                    "\ud83d\udc4b Привет! На связи автовыдача валюты — deluxe client\n\n\ud83d\udce6 Ваш заказ: "
                                       + local6253_4.units()
                                       + " кк\n✏️ Напишите ваш игровой ник одним сообщением\n\nℹ️ Передумали? Напишите «Отмена» — сразу вернём средства"
                                 );
                                 local6227_2.field13208 = AutoDeliverModule.Nested0507.NICKNAME;
                              }
                           }
                        );
                  }
               }
            }
         } else {
            for (AutoDeliverModule.Nested0505 local6253_4 : local6253_1) {
               this.field13044.add(local6253_4.id());
            }
         }
      }
   }

   public static AutoDeliverModule method3798() {
      if (autoDeliverModule == null) {
         autoDeliverModule = new AutoDeliverModule();
      }

      return autoDeliverModule;
   }

   private AutoDeliverModule.Nested0505 method3799(String local6257_1) {
      return this.field13043 == null
         ? null
         : this.field13043.stream().filter(local6248_1 -> local6248_1.buyerLogin().equalsIgnoreCase(local6257_1)).findFirst().orElse(null);
   }

   private static final class Nested0504 {
      private String field13192;
      private String field13193;
      private String field13194;
      private int field13195;
      private long field13196;
      private long field13197;
      private long field13198;
      private boolean field13199;
      private boolean field13200;
      private boolean field13201;
      private boolean field13202;
      private boolean field13203;
   }

   private record Nested0505(String id, String buyerLogin, double price, int units) {
   }

   private static final class Nested0506 {
      private String field13204;
      private String field13205;
      private String field13206;
      private long field13207;
      private AutoDeliverModule.Nested0507 field13208;
      private long field13209;
   }

   private enum Nested0507 {
      NICKNAME,
      CONFIRM_NICKNAME,
      DELIVERY;
   }
}
