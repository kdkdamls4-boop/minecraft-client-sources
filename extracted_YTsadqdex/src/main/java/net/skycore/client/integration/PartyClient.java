package net.skycore.client.integration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.ClickEvent.RunCommand;
import net.minecraft.network.chat.HoverEvent.ShowText;
import net.skycore.client.util.SendDonationHelper;

public final class PartyClient {
   private static final AtomicBoolean COORD_PULL_IN_FLIGHT = new AtomicBoolean(false);
   private static volatile int partyMemberCount;
   private static volatile String lastTimestamp = "";
   private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
   private static final AtomicBoolean COORD_PUSH_IN_FLIGHT = new AtomicBoolean(false);
   private static volatile String pendingInvitePartyName = "";
   private static final long CHAT_COOLDOWN_MS = 4000L;
   private static volatile String partyCode = "";
   private static volatile String partyName = "";
   private static final long COORD_SYNC_INTERVAL_MS = 1000L;
   private static volatile long lastCoordSyncAtMs;
   private static volatile String pendingInviteFrom = "";
   private static volatile String partyLeader = "";
   private static long lastChatAtMs;
   private static final List<String> partyMemberNames = Collections.synchronizedList(new ArrayList<>());
   private static final ConcurrentHashMap<String, PartyMember> MEMBERS = new ConcurrentHashMap<>();

   public static List<PartyMember> getMembersForRender() {
      if (!isMetkaEnabled()) {
         return Collections.emptyList();
      }

      String text = PartyIdentity.resolveUsername();
      String text2 = text.toLowerCase(Locale.ROOT);
      ArrayList elements = new ArrayList();
      Iterator iterator = MEMBERS.values().iterator();

      while (iterator.hasNext()) {
         PartyMember element = (PartyMember)iterator.next();
         if (element.getUsername() != null) {
            String username = element.getUsername();
            String text3 = username.toLowerCase(Locale.ROOT);
            if (!text3.equals(text2)) {
               elements.add(element);
            }
         }
      }

      return elements;
   }

   private static void tick() {
      String text = PartyIdentity.resolveUsername();
      if (!text.isBlank()) {
         PartyIdentity.rememberUsername(text);
         pollUpdates(text);
         if (!isMetkaEnabled()) {
            MEMBERS.clear();
         }
      }
   }

   public static void disband() {
      if (!hasParty()) {
         partySystem("Ты не в пати");
      } else {
         if (!isLeader()) {
            partySystem("Только лидер может распустить пати");
         } else {
            Executors.newSingleThreadExecutor().submit(() -> {

               try {
                  JsonObject json = baseJson();
                  if (partyCode != null) {
                     if (!partyCode.isBlank()) {
                        json.addProperty("party_code", partyCode);
                     }
                  }

                  String text = RemoteEndpoints.pathDisbandParty();
                  JsonObject json2 = post(text, json);
                  if (isSuccess(json2)) {
                     clearLocalParty();
                     runOnMain(() -> {
                        partySystem("Пати распущено");
                     });
                  } else {
                     runOnMain(() -> {
                        String text2 = errorMessage(json2);
                        partySystem(text2);
                     });
                  }
               } catch (Exception failure) {
                  runOnMain(() -> {
                     partySystem(failure.getMessage());
                  });
               }
            });
         }
      }
   }

   public static void accept() {
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      Runnable asString = () -> {
         try {
            JsonObject json = post(RemoteEndpoints.pathPartyAccept(), baseJson());
            if (isSuccess(json)) {
               partyCode = json.has("party_code") ? json.get("party_code").getAsString() : "";
               partyName = json.has("party_name") ? json.get("party_name").getAsString() : pendingInvitePartyName;
               if (json.has("leader") && !json.get("leader").isJsonNull()) {
                  partyLeader = json.get("leader").getAsString();
               } else if (json.has("party_leader") && !json.get("party_leader").isJsonNull()) {
                  partyLeader = json.get("party_leader").getAsString();
               }

               applyPartyMembers(json);
               pendingInviteFrom = "";
               pendingInvitePartyName = "";
               resetCursor();
               String partyName2 = getPartyName();
               runOnMain(() -> {
                  String text = "Ты вступил в пати " + partyName2;
                  partySystem(text);
               });
               scheduleCoordinateSync(PartyIdentity.resolveUsername());
            } else {
               runOnMain(() -> {
                  String text2 = errorMessage(json);
                  partySystem(text2);
               });
            }
         } catch (Exception failure) {
            runOnMain(() -> {
               partySystem(failure.getMessage());
            });
         }
      };
      executorService.submit(asString);
   }

   private static boolean isInviteAccepted(String text) {
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         text2 = text.toLowerCase(Locale.ROOT);
      }

      String text3 = text2;
      if (!text3.equals("accepted")) {
         if (!text3.equals("accepted_out")) {
            if (!text3.equals("joined")) {
               if (!text3.equals("joined_out")) {
                  if (!text3.equals("invite_accepted")) {
                     if (!text3.equals("invite_joined")) {
                        return false;
                     }
                  }
               }
            }
         }
      }

      return true;
   }

   private static void pullCoordinates() {
      if (partyCode != null && !partyCode.isBlank()) {
         try {
            String apiUrl = RemoteApiConfig.getApiUrl()
               + RemoteEndpoints.pathGetPartyCoordinates()
               + "?"
               + RemoteEndpoints.jsonApiKey()
               + "="
               + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8)
               + "&party_code="
               + URLEncoder.encode(partyCode, StandardCharsets.UTF_8);
            HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setConnectTimeout(3000);
            httpURLConnection.setReadTimeout(3000);
            if (httpURLConnection.getResponseCode() != 200) {
               return;
            }

            JsonObject json = parseJson(readBody(httpURLConnection));
            if (!isSuccess(json) || !json.has("members")) {
               return;
            }

            HashSet entries = new HashSet();

            for (JsonElement jsonElement : json.getAsJsonArray("members")) {
               JsonObject asJsonObject = jsonElement.getAsJsonObject();
               String asString = asJsonObject.get("username").getAsString();
               if (asString != null && !asString.isBlank()) {
                  String text = asString.toLowerCase(Locale.ROOT);
                  entries.add(text);
                  double asDouble = asJsonObject.get("x").getAsDouble();
                  double asDouble2 = asJsonObject.has("y") ? asJsonObject.get("y").getAsDouble() : 64.0;
                  double asDouble3 = asJsonObject.get("z").getAsDouble();
                  float asFloat = asJsonObject.has("hp") ? asJsonObject.get("hp").getAsFloat() : 20.0F;
                  int asBoolean = asJsonObject.has("online") && !asJsonObject.get("online").getAsBoolean() ? 0 : 1;
                  PartyMember partyMember = MEMBERS.get(text);
                  if (partyMember == null) {
                     partyMember = new PartyMember(asString, asDouble, asDouble2, asDouble3, asFloat, (asBoolean != 0));
                     MEMBERS.put(text, partyMember);
                  } else {
                     partyMember.applyNetwork(asDouble, asDouble2, asDouble3, asFloat, (asBoolean != 0));
                  }
               }
            }

            MEMBERS.keySet().removeIf(text2 -> {
               return !entries.contains(text2);
            });
         } catch (Exception failure) {
         }
      }
   }

   public static void create(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            if (hasParty()) {
               partySystem("Ты уже в пати «" + getPartyName() + "»");
               return;
            }

            String text2 = text.trim();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> {
               try {
                  JsonObject json = baseJson();
                  json.addProperty("name", text2);
                  JsonObject json2 = post("create_party", json);
                  if (isSuccess(json2)) {
                     partyCode = json2.has("party_code") ? json2.get("party_code").getAsString() : "";
                     partyName = json2.has("party_name") ? json2.get("party_name").getAsString() : text2;
                     partyLeader = PartyIdentity.resolveUsername();
                     partyMemberCount = 1;
                     partyMemberNames.clear();
                     partyMemberNames.add(partyLeader);
                     MEMBERS.clear();
                     resetCursor();
                     runOnMain(() -> {
                        String text3 = "Пати создано: " + partyName;
                        partySystem(text3);
                     });
                  } else {
                     String text4 = errorMessage(json2);
                     runOnMain(() -> {
                        partySystem(text4);
                     });
                  }
               } catch (Exception failure) {
                  runOnMain(() -> {
                     String message = failure.getMessage();
                     partySystem(message);
                  });
               }
            });
            return;
         }
      }

      partySystem("Укажи название пати");
   }

   public static String getPartyCode() {
      return partyCode == null ? "" : partyCode;
   }

   public static String getPendingInviteFrom() {
      return pendingInviteFrom == null ? "" : pendingInviteFrom;
   }

   private static void partySystem(String text) {
      if (text != null && !text.isBlank()) {
         Runnable runnable = () -> {
            MutableComponent mutableComponent = Component.literal(text);
            MutableComponent mutableComponent2 = mutableComponent.withStyle(ChatFormatting.GRAY);
            SendDonationHelper.sendParty(mutableComponent2);
         };
         runOnMain(runnable);
      }
   }

   public static void sendChat(String text) {
      if (text != null && !text.isBlank()) {
         if (!hasParty()) {
            partySystem("Ты не в пати");
         } else {
            long timestamp = System.currentTimeMillis() - lastChatAtMs;
            if (lastChatAtMs > 0L && timestamp < 4000L) {
               long timestamp2 = (4000L - timestamp + 999L) / 1000L;
               partySystem("Подожди " + timestamp2 + " сек.");
            } else {
               lastChatAtMs = System.currentTimeMillis();
               String text2 = text.trim();
               String text3 = PartyIdentity.resolveUsername();
               Executors.newSingleThreadExecutor().submit(() -> {
                  try {
                     JsonObject json = baseJson();
                     json.addProperty("party_code", partyCode);
                     json.addProperty("message", text2);
                     JsonObject json2 = post(RemoteEndpoints.pathSendPartyMessage(), json);
                     if (isSuccess(json2)) {
                        if (json2.has("timestamp")) {
                           String asString = json2.get("timestamp").getAsString();
                           if (asString.compareTo(lastTimestamp) > 0) {
                              lastTimestamp = asString;
                           }
                        }

                        displayPartyMessage(text3, text2);
                     } else {
                        runOnMain(() -> {
                           String text4 = errorMessage(json2);
                           partySystem(text4);
                        });
                     }
                  } catch (Exception failure) {
                     runOnMain(() -> {
                        String message = failure.getMessage();
                        partySystem(message);
                     });
                  }
               });
            }
         }
      }
   }

   private static JsonObject baseJson() {
      JsonObject json = new JsonObject();
      String text = RemoteEndpoints.jsonApiKey();
      json.addProperty(text, RemoteApiConfig.getApiKey());
      String text2 = PartyIdentity.resolveUsername();
      json.addProperty("username", text2);
      return json;
   }

   public static void leaveOnExit() {
      if (!isLeader()) {
         leaveInternal(false);
      }
   }

   private PartyClient() {
   }

   private static boolean isSuccess(JsonObject json) {
      if (json != null) {
         if (json.has("status")) {
            if ("success".equals(json.get("status").getAsString())) {
               return true;
            }
         }
      }

      return false;
   }

   public static void invite(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            if (!hasParty()) {
               partySystem("Сначала создай пати");
               return;
            }

            Executors.newSingleThreadExecutor().submit(() -> {

               try {
                  JsonObject json = baseJson();
                  json.addProperty("target", text.trim());
                  if (partyCode != null && !partyCode.isBlank()) {
                     json.addProperty("party_code", partyCode);
                  }

                  JsonObject json2 = post(RemoteEndpoints.pathPartyInvite(), json);
                  if (isSuccess(json2)) {
                     runOnMain(() -> {
                        String text2 = "Приглашение отправлено: " + text.trim();
                        partySystem(text2);
                     });
                  } else {
                     Runnable runnable = () -> {
                        partySystem(errorMessage(json2));
                     };
                     runOnMain(runnable);
                  }
               } catch (Exception failure) {
                  Runnable message = () -> {
                     String message2 = failure.getMessage();
                     partySystem(message2);
                  };
                  runOnMain(message);
               }
            });
            return;
         }
      }

      partySystem("Укажи ник для приглашения");
   }

   private static MutableComponent actionButton(String text, ChatFormatting chatFormatting, String text2, String text3) {
      Style style = Style.EMPTY.withColor(chatFormatting);
      Style style2 = style.withBold(true);
      Style style3 = style2.withClickEvent(new RunCommand(text2));
      ShowText showText = new ShowText(Component.literal(text3));
      Style style4 = style3.withHoverEvent(showText);
      String text4 = "[" + text + "]";
      return Component.literal(text4).setStyle(style4);
   }

   private static String eventString(JsonObject json, String... stringValues) {

      for (String text : stringValues) {
         if (json.has(text)) {
            if (!json.get(text).isJsonNull()) {
               String asString = json.get(text).getAsString();
               if (asString != null) {
                  if (!asString.isBlank()) {
                     return asString;
                  }
               }
            }
         }
      }

      return "";
   }

   public static String getLeader() {
      String text;
      if (partyLeader == null) {
         text = "";
      } else {
         text = partyLeader;
      }

      return text;
   }

   public static void kick(String text) {
      if (text != null && !text.isBlank()) {
         if (!hasParty()) {
            partySystem("Ты не в пати");
         } else if (!isLeader()) {
            partySystem("Только лидер может исключать игроков");
         } else {
            String text2 = text.trim();
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Runnable message = () -> {

               try {
                  JsonObject json = baseJson();
                  json.addProperty("party_code", partyCode);
                  json.addProperty("kicked_user", text2);
                  JsonObject json2 = post("/kick_party_member", json);
                  if (isSuccess(json2)) {
                     Runnable runnable = () -> {
                        partySystem("Исключён: " + text2);
                     };
                     runOnMain(runnable);
                  } else {
                     Runnable runnable2 = () -> {
                        String text3 = errorMessage(json2);
                        partySystem(text3);
                     };
                     runOnMain(runnable2);
                  }
               } catch (Exception failure) {
                  runOnMain(() -> {
                     String message2 = failure.getMessage();
                     partySystem(message2);
                  });
               }
            };
            executorService.submit(message);
         }
      }
   }

   public static List<String> getMemberNames() {
      synchronized (partyMemberNames) {
         return new ArrayList<>(partyMemberNames);
      }
   }

   private static void applyPartyMembers(JsonObject json) {
      if (json.has("party_members") && json.get("party_members").isJsonArray()) {
         JsonArray asJsonArray = json.getAsJsonArray("party_members");
         partyMemberCount = asJsonArray.size();
         partyMemberNames.clear();

         for (JsonElement jsonElement : asJsonArray) {
            if (jsonElement.isJsonPrimitive()) {
               partyMemberNames.add(jsonElement.getAsString());
            } else if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("username")) {
               partyMemberNames.add(jsonElement.getAsJsonObject().get("username").getAsString());
            }
         }
      } else if (json.has("members") && json.get("members").isJsonArray()) {
         JsonArray asJsonArray2 = json.getAsJsonArray("members");
         partyMemberCount = asJsonArray2.size();
         partyMemberNames.clear();

         for (JsonElement jsonElement2 : asJsonArray2) {
            if (jsonElement2.isJsonPrimitive()) {
               partyMemberNames.add(jsonElement2.getAsString());
            } else if (jsonElement2.isJsonObject() && jsonElement2.getAsJsonObject().has("username")) {
               partyMemberNames.add(jsonElement2.getAsJsonObject().get("username").getAsString());
            }
         }
      } else {
         partyMemberCount = Math.max(partyMemberCount, 2);
      }
   }

   private static boolean isInviteDeclined(String text) {
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         text2 = text.toLowerCase(Locale.ROOT);
      }

      String text3 = text2;
      if (!text3.equals("declined")) {
         if (!text3.equals("declined_out")) {
            if (!text3.equals("rejected")) {
               if (!text3.equals("rejected_out")) {
                  if (!text3.equals("invite_declined")) {
                     if (!text3.equals("invite_rejected")) {
                        return false;
                     }
                  }
               }
            }
         }
      }

      return true;
   }

   private static void clearLocalParty() {
      partyCode = "";
      partyName = "";
      partyLeader = "";
      partyMemberCount = 0;
      partyMemberNames.clear();
      MEMBERS.clear();
   }

   public static boolean hasPendingInvite() {
      return pendingInviteFrom != null && !pendingInviteFrom.isBlank();
   }

   private static void scheduleCoordinateSync(String text) {
      scheduleCoordinateSync(text, null);
   }

   private static String errorMessage(JsonObject json) {
      if (json != null && json.has("message")) {
         JsonElement jsonElement = json.get("message");
         return jsonElement.getAsString();
      } else {
         return "ошибка";
      }
   }

   private static void syncCoordinatesTick() {
      if (isMetkaEnabled()) {
         String text = PartyIdentity.resolveUsername();
         if (!text.isBlank()) {
            Minecraft instance = Minecraft.getInstance();
            if (instance != null) {
               long now = System.currentTimeMillis();
               if (now - lastCoordSyncAtMs >= 1000L) {
                  lastCoordSyncAtMs = now;
                  instance.execute(() -> {

                     try {
                        snapshotAndPushCoordinates();
                     } catch (Exception failure) {
                     }
                  });
                  if (COORD_PULL_IN_FLIGHT.compareAndSet(false, true)) {
                     SCHEDULER.execute(() -> {
                        boolean condition = false /* VF: Semaphore variable */;

                        membershipDecision: {
                           try {
                              condition = true;
                              pullCoordinates();
                              condition = false;
                              break membershipDecision;
                           } catch (Exception failure2) {
                              condition = false;
                           } finally {
                              if (condition) {
                                 COORD_PULL_IN_FLIGHT.set(false);
                              }
                           }

                           COORD_PULL_IN_FLIGHT.set(false);
                           return;
                        }

                        COORD_PULL_IN_FLIGHT.set(false);
                     });
                  }
               }
            }
         }
      }
   }

   public static boolean hasParty() {
      if (partyCode != null) {
         if (!partyCode.isBlank()) {
            return true;
         }
      }

      return false;
   }

   private static void pollUpdates(String text) {
      try {
         String apiUrl = RemoteApiConfig.getApiUrl()
            + RemoteEndpoints.pathGetUpdates()
            + "?"
            + RemoteEndpoints.jsonApiKey()
            + "="
            + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8)
            + "&username="
            + URLEncoder.encode(text, StandardCharsets.UTF_8);
         apiUrl = PartyIdentity.appendPresenceQuery(apiUrl);
         if (!lastTimestamp.isEmpty()) {
            apiUrl = apiUrl + "&last_timestamp=" + URLEncoder.encode(lastTimestamp, StandardCharsets.UTF_8);
         }

         HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
         httpURLConnection.setRequestMethod("GET");
         httpURLConnection.setConnectTimeout(8000);
         httpURLConnection.setReadTimeout(15000);
         if (httpURLConnection.getResponseCode() != 200) {
            return;
         }

         JsonObject json = parseJson(readBody(httpURLConnection));
         if (!isSuccess(json)) {
            return;
         }

         int partyMemberCount2 = partyMemberCount;
         if (json.has("party_members") && json.get("party_members").isJsonArray()) {
            JsonArray asJsonArray = json.getAsJsonArray("party_members");
            partyMemberCount = asJsonArray.size();
            partyMemberNames.clear();

            for (JsonElement jsonElement : asJsonArray) {
               if (jsonElement.isJsonPrimitive()) {
                  partyMemberNames.add(jsonElement.getAsString());
               } else if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("username")) {
                  partyMemberNames.add(jsonElement.getAsJsonObject().get("username").getAsString());
               }
            }

            if (partyMemberCount == 0) {
               clearLocalParty();
            } else if (json.has("party_code") && !json.get("party_code").isJsonNull()) {
               String asString = json.get("party_code").getAsString();
               if (asString != null && !asString.isBlank()) {
                  partyCode = asString;
               }
            }

            if (json.has("party_name") && !json.get("party_name").isJsonNull()) {
               String asString2 = json.get("party_name").getAsString();
               if (asString2 != null && !asString2.isBlank()) {
                  partyName = asString2;
               }
            }

            if (json.has("party_leader") && !json.get("party_leader").isJsonNull()) {
               String asString3 = json.get("party_leader").getAsString();
               if (asString3 != null && !asString3.isBlank()) {
                  partyLeader = asString3;
               }
            }

            if (partyMemberCount2 <= 1 && partyMemberCount > 1) {
               scheduleCoordinateSync(text);
            }
         } else if (json.has("party_code") && !json.get("party_code").isJsonNull()) {
            String asString4 = json.get("party_code").getAsString();
            if (asString4 != null && !asString4.isBlank()) {
               partyCode = asString4;
            }
         }

         if (json.has("invite_events") && json.get("invite_events").isJsonArray()) {
            for (JsonElement jsonElement2 : json.getAsJsonArray("invite_events")) {
               if (jsonElement2.isJsonObject()) {
                  JsonObject asJsonObject = jsonElement2.getAsJsonObject();
                  String asString5 = asJsonObject.has("type") ? asJsonObject.get("type").getAsString() : "";
                  if ("expired_out".equals(asString5)) {
                     String asString6 = asJsonObject.has("target") ? asJsonObject.get("target").getAsString() : "";
                     String text2 = asString6 != null && !asString6.isBlank() ? asString6 : "игрока";
                     runOnMain(() -> {
                        String text3 = "Приглашение " + text2 + " истекло";
                        partySystem(text3);
                     });
                  } else if ("expired_in".equals(asString5)) {
                     String asString7 = asJsonObject.has("party_name") ? asJsonObject.get("party_name").getAsString() : "";
                     String text4 = asString7 != null && !asString7.isBlank() ? asString7 : "пати";
                     runOnMain(() -> {
                        String text5 = "Приглашение в " + text4 + " истекло";
                        partySystem(text5);
                     });
                     pendingInviteFrom = "";
                     pendingInvitePartyName = "";
                  } else if (isInviteAccepted(asString5) || isInviteDeclined(asString5)) {
                     String text6 = eventString(asJsonObject, "target", "username", "player", "from");
                     String text7 = text6 != null && !text6.isBlank() ? text6 : "Игрок";
                     String text8 = isInviteAccepted(asString5) ? "Игрок " + text7 + " зашел в пати" : "Игрок " + text7 + " отклонил заявку в пати";
                     runOnMain(() -> {
                        partySystem(text8);
                     });
                  }
               }
            }
         }

         if (json.has("invites") && json.get("invites").isJsonArray()) {
            JsonArray asJsonArray2 = json.getAsJsonArray("invites");
            if (asJsonArray2.isEmpty()) {
               pendingInviteFrom = "";
               pendingInvitePartyName = "";
            }

            for (JsonElement jsonElement3 : asJsonArray2) {
               JsonObject asJsonObject2 = jsonElement3.getAsJsonObject();
               String asString8 = asJsonObject2.has("from") ? asJsonObject2.get("from").getAsString() : "";
               String asString9 = asJsonObject2.has("party_name") ? asJsonObject2.get("party_name").getAsString() : "";
               if (!asString8.isBlank() && !asString8.equalsIgnoreCase(pendingInviteFrom)) {
                  pendingInviteFrom = asString8;
                  pendingInvitePartyName = asString9 == null ? "" : asString9;
                  String text9 = pendingInvitePartyName.isBlank() ? "пати" : pendingInvitePartyName;
                  runOnMain(() -> {
                     showInvite(asString8, text9);
                  });
               }
            }
         }

         if (json.has("party_messages") && json.get("party_messages").isJsonArray()) {
            JsonArray asJsonArray3 = json.getAsJsonArray("party_messages");

            for (int index = asJsonArray3.size() - 1; index >= 0; index--) {
               JsonObject asJsonObject3 = asJsonArray3.get(index).getAsJsonObject();
               String asString10 = asJsonObject3.get("username").getAsString();
               String asString11 = asJsonObject3.get("message").getAsString();
               String asString12 = asJsonObject3.get("timestamp").getAsString();
               if (asString10.equalsIgnoreCase(text)) {
                  if (asString12.compareTo(lastTimestamp) > 0) {
                     lastTimestamp = asString12;
                  }
               } else {
                  displayPartyMessage(asString10, asString11);
                  if (asString12.compareTo(lastTimestamp) > 0) {
                     lastTimestamp = asString12;
                  }
               }
            }
         }
      } catch (Exception failure) {
      }
   }

   public static boolean isMetkaEnabled() {
      if (!hasParty()) {
         return false;
      }

      synchronized (partyMemberNames) {
         if (partyMemberNames.size() > 1) {
            return true;
         }
      }

      if (partyMemberCount <= 1) {
         if (MEMBERS.isEmpty()) {
            return false;
         }
      }

      return true;
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

   private static void showInvite(String text, String text2) {
      MutableComponent mutableComponent = Component.empty();
      MutableComponent mutableComponent2 = Component.literal("Вас пригласили в пати " + text2 + " ").withStyle(ChatFormatting.WHITE);
      mutableComponent = mutableComponent.append(mutableComponent2);
      mutableComponent = mutableComponent.append(actionButton("Принять", ChatFormatting.GREEN, ".party accept", "Принять приглашение от " + text));
      mutableComponent2 = Component.literal(" ");
      mutableComponent = mutableComponent.append(mutableComponent2.withStyle(ChatFormatting.GRAY));
      mutableComponent2 = actionButton("Отклонить", ChatFormatting.RED, ".party decline", "Отклонить приглашение от " + text);
      MutableComponent mutableComponent3 = mutableComponent.append(mutableComponent2);
      runOnMain(() -> {
         SendDonationHelper.sendParty(mutableComponent3);
      });
   }

   public static String getPendingInvitePartyName() {
      String text;
      if (pendingInvitePartyName == null) {
         text = "";
      } else {
         text = pendingInvitePartyName;
      }

      return text;
   }

   public static void init() {
      ScheduledExecutorService sCHEDULER = SCHEDULER;
      Runnable runnable = () -> {

         try {
            tick();
         } catch (Exception failure) {
         }
      };
      sCHEDULER.scheduleAtFixedRate(runnable, 1L, 1L, TimeUnit.SECONDS);
      ScheduledExecutorService sCHEDULER2 = SCHEDULER;
      Runnable runnable2 = () -> {

         try {
            syncCoordinatesTick();
         } catch (Exception failure2) {
         }
      };
      sCHEDULER2.scheduleAtFixedRate(runnable2, 500L, 500L, TimeUnit.MILLISECONDS);
   }

   public static void leave() {
      if (isLeader()) {
         partySystem("Лидер не может выйти — распусти пати");
      } else {
         ExecutorService executorService = Executors.newSingleThreadExecutor();
         executorService.submit(() -> {
            leaveInternal(true);
         });
      }
   }

   private static JsonObject post(String text, JsonObject json) throws Exception {
      if (!RemoteApiConfig.isConfigured()) {
         JsonObject obj = new JsonObject();
         obj.addProperty("status", "disabled");
         return obj;
      }
      HttpURLConnection apiUrl = (HttpURLConnection)new URI(RemoteApiConfig.getApiUrl() + text).toURL().openConnection();
      apiUrl.setRequestMethod("POST");
      apiUrl.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
      apiUrl.setDoOutput(true);
      apiUrl.setConnectTimeout(4000);
      apiUrl.setReadTimeout(4000);

      try (OutputStream outputStream = apiUrl.getOutputStream()) {
         outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
      }

      return parseJson(readBody(apiUrl));
   }

   private static void leaveInternal(boolean condition) {
      try {
         JsonObject json = baseJson();
         String lastUsername = PartyIdentity.getLastUsername();
         if ((json.get("username") == null || json.get("username").getAsString().isBlank()) && lastUsername != null && !lastUsername.isBlank()) {
            json.addProperty("username", lastUsername);
         }

         if (partyCode != null && !partyCode.isBlank()) {
            json.addProperty("party_code", partyCode);
         }

         JsonObject json2 = post(RemoteEndpoints.pathLeaveParty(), json);
         if (!isSuccess(json2)) {
            if (condition) {
               runOnMain(() -> {
                  partySystem(errorMessage(json2));
               });
            }

            return;
         }
      } catch (Exception failure) {
      }

      clearLocalParty();
      if (condition) {
         runOnMain(() -> {
            partySystem("Ты вышел из пати");
         });
      }
   }

   public static String getPartyName() {
      return partyName != null && !partyName.isBlank() ? partyName : getPartyCode();
   }

   private static void scheduleCoordinateSync(String text, Runnable runnable) {
      if (isMetkaEnabled() && text != null && !text.isBlank()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance != null) {
            Runnable runnable2 = () -> {

               try {
                  snapshotAndPushCoordinates();
               } catch (Exception failure) {
               }
            };
            instance.execute(runnable2);
         }

         ScheduledExecutorService sCHEDULER = SCHEDULER;
         Runnable runnable3 = () -> {
            boolean condition = false /* VF: Semaphore variable */;

            responseHandling: {
               try {
                  condition = true;
                  pullCoordinates();
                  condition = false;
                  break responseHandling;
               } catch (Exception failure2) {
                  condition = false;
               } finally {
                  if (condition) {
                     if (runnable != null) {
                        runnable.run();
                     }
                  }
               }

               if (runnable != null) {
                  runnable.run();
               }

               return;
            }

            if (runnable != null) {
               runnable.run();
            }
         };
         sCHEDULER.execute(runnable3);
      } else {
         if (runnable != null) {
            runnable.run();
         }
      }
   }

   private static void snapshotAndPushCoordinates() {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null && partyCode != null && !partyCode.isBlank()) {
         LocalPlayer player = instance.player;
         if (player != null) {
            if (COORD_PUSH_IN_FLIGHT.compareAndSet(false, true)) {
               String partyCode2 = partyCode;
               double x = player.getX();
               double y = player.getY();
               double z = player.getZ();
               float health = player.getHealth();
               SCHEDULER.execute(() -> {
                  try {
                     JsonObject json = baseJson();
                     json.addProperty("party_code", partyCode2);
                     json.addProperty("x", x);
                     json.addProperty("y", y);
                     json.addProperty("z", z);
                     json.addProperty("hp", health);
                     post(RemoteEndpoints.pathUpdateCoordinates(), json);
                  } catch (Exception failure) {
                  } finally {
                     COORD_PUSH_IN_FLIGHT.set(false);
                  }
               });
            }
         }
      }
   }

   private static JsonObject parseJson(String text) {
      if (text != null) {
         if (!text.isBlank()) {
            JsonElement jsonElement = JsonParser.parseString(text);
            return jsonElement.getAsJsonObject();
         }
      }

      return new JsonObject();
   }

   public static void decline() {
      ExecutorService executorService = Executors.newSingleThreadExecutor();
      Runnable message = () -> {

         try {
            String text = RemoteEndpoints.pathPartyDecline();
            JsonObject json = baseJson();
            JsonObject json2 = post(text, json);
            if (isSuccess(json2)) {
               pendingInviteFrom = "";
               pendingInvitePartyName = "";
               runOnMain(() -> {
                  partySystem("Приглашение отклонено");
               });
            } else {
               runOnMain(() -> {
                  partySystem(errorMessage(json2));
               });
            }
         } catch (Exception failure) {
            runOnMain(() -> {
               String message2 = failure.getMessage();
               partySystem(message2);
            });
         }
      };
      executorService.submit(message);
   }

   private static void resetCursor() {
      lastTimestamp = "";

      try {
         String text = PartyIdentity.resolveUsername();
         String apiUrl = RemoteApiConfig.getApiUrl()
            + RemoteEndpoints.pathGetUpdates()
            + "?"
            + RemoteEndpoints.jsonApiKey()
            + "="
            + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8)
            + "&username="
            + URLEncoder.encode(text, StandardCharsets.UTF_8);
         apiUrl = PartyIdentity.appendPresenceQuery(apiUrl);
         HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
         httpURLConnection.setRequestMethod("GET");
         httpURLConnection.setConnectTimeout(3000);
         httpURLConnection.setReadTimeout(3000);
         if (httpURLConnection.getResponseCode() != 200) {
            return;
         }

         JsonObject json = parseJson(readBody(httpURLConnection));
         if (json.has("latest_timestamp")) {
            lastTimestamp = json.get("latest_timestamp").getAsString();
         }
      } catch (Exception failure) {
      }
   }

   public static boolean isLeader() {
      if (!hasParty()) {
         return false;
      }

      String text = PartyIdentity.resolveUsername();
      return !text.isBlank() && text.equalsIgnoreCase(partyLeader);
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         byte flag = 1;
         byte flag2 = 0;
         SCHEDULER.shutdown();

         try {
            flag = -1;
            byte flag3 = 1;
            if (!SCHEDULER.awaitTermination(1L, TimeUnit.SECONDS)) {
               flag = 1;
               SCHEDULER.shutdownNow();
            }
         } catch (InterruptedException failure) {
            byte flag4 = 1;
            SCHEDULER.shutdownNow();
            flag = 1;
            Thread.currentThread().interrupt();
         }
      }));
   }

   private static void displayPartyMessage(String text, String text2) {
      MutableComponent mutableComponent = Component.empty();
      MutableComponent mutableComponent2 = Component.literal(text == null ? "?" : text).withStyle(ChatFormatting.WHITE);
      MutableComponent mutableComponent3 = mutableComponent.append(mutableComponent2);
      MutableComponent mutableComponent4 = Component.literal(": ");
      MutableComponent mutableComponent5 = mutableComponent4.withStyle(ChatFormatting.DARK_GRAY);
      MutableComponent mutableComponent6 = mutableComponent3.append(mutableComponent5).append(Component.literal(text2 == null ? "" : text2).withStyle(ChatFormatting.RESET));
      runOnMain(() -> {
         SendDonationHelper.sendParty(mutableComponent6);
      });
   }

   private static String readBody(HttpURLConnection httpURLConnection) {

      try {
         BufferedReader bufferedReader;
         InputStreamReader inputStreamReader;
         InputStream inputStream;
         if (httpURLConnection.getResponseCode() >= 400) {
            inputStream = httpURLConnection.getErrorStream() != null ? httpURLConnection.getErrorStream() : httpURLConnection.getInputStream();
         } else {
            inputStream = httpURLConnection.getInputStream();
         }

         inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
         bufferedReader = new BufferedReader(inputStreamReader);
         BufferedReader bufferedReader2 = bufferedReader;
         BufferedReader bufferedReader3 = bufferedReader2;

         String text;
         try {
            var stream = bufferedReader2.lines();
            Collector<CharSequence, ?, String> collector = Collectors.joining();
            text = stream.collect(collector);
         } catch (Throwable failure) {
            if (bufferedReader3 != null) {
               try {
                  bufferedReader3.close();
               } catch (Throwable failure2) {
                  failure.addSuppressed(failure2);
               }
            }

            throw failure;
         }

         if (bufferedReader3 != null) {
            bufferedReader3.close();
         }

         return text;
      } catch (Exception failure3) {
         return "";
      }
   }
}


