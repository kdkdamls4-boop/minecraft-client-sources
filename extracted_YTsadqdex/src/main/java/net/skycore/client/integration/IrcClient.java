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
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.skycore.client.profile.LocalProfile;
import net.skycore.client.util.SendDonationHelper;
import net.skycore.client.util.ShouldShowSkycoreTagHelper;

public final class IrcClient {
   private static final int CONNECT_FAILURES_BEFORE_BACKOFF = 3;
   private static long lastConnectAttemptMs;
   private static volatile int connectFailures;
   private static final long SEND_COOLDOWN_MS = 4000L;
   private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);
   private static long lastSendAtMs;
   private static final long CONNECT_RETRY_MS = 5000L;
   private static String currentPrefix = "user";
   private static volatile long nextConnectAttemptMs;
   private static String lastTimestamp = "";
   private static final long CONNECT_BACKOFF_MS = TimeUnit.MINUTES.toMillis(10L);
   private static final Logger LOGGER = Logger.getLogger(IrcClient.class.getName());
   private static volatile boolean userEnabled = true;
   private static volatile boolean connecting;
   private static volatile boolean connected;
   private static volatile boolean recentMessagesLoaded;
   private static volatile boolean quietReconnect;

   private static String formatUtcNow() {
      ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneOffset.UTC);
      Locale rOOT = Locale.ROOT;
      Object[] objectValues = new Object[]{zonedDateTime.getYear(), zonedDateTime.getMonthValue(), null, null, null, null, null};
      int dayOfMonth = zonedDateTime.getDayOfMonth();
      objectValues[2] = dayOfMonth;
      dayOfMonth = zonedDateTime.getHour();
      objectValues[3] = dayOfMonth;
      dayOfMonth = zonedDateTime.getMinute();
      objectValues[4] = dayOfMonth;
      objectValues[5] = zonedDateTime.getSecond();
      objectValues[6] = zonedDateTime.getNano() / 1000;
      return String.format(rOOT, "%04d-%02d-%02dT%02d:%02d:%02d.%06dZ", objectValues);
   }

   private static String resolveSendPrefix() {
      String text = resolveUsername();
      if (RemoteApiConfig.isOwnerUsername(text)) {
         return "owner";
      }

      try {
         LocalProfile instance = LocalProfile.getInstance();
         String text2 = instance.profile("role");
         if (text2 != null) {
            if (!text2.isBlank()) {
               return mapProfileRole(text2);
            }
         }
      } catch (Throwable failure) {
      }

      return currentPrefix;
   }

   private static String resolveUsername() {
      return PartyIdentity.resolveUsername();
   }

   private static void checkForUpdates() {
      try {
         String text = resolveUsername();
         String apiUrl = RemoteApiConfig.getApiUrl()
            + RemoteEndpoints.pathGetUpdates()
            + "?"
            + RemoteEndpoints.jsonApiKey()
            + "="
            + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8);
         if (!text.isBlank()) {
            apiUrl = apiUrl + "&username=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
            apiUrl = PartyIdentity.appendPresenceQuery(apiUrl);
         }

         if (!lastTimestamp.isEmpty()) {
            apiUrl = apiUrl + "&last_timestamp=" + URLEncoder.encode(lastTimestamp, StandardCharsets.UTF_8);
         }

         HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
         httpURLConnection.setRequestMethod("GET");
         httpURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
         httpURLConnection.setConnectTimeout(3000);
         httpURLConnection.setReadTimeout(3000);
         if (httpURLConnection.getResponseCode() == 200) {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
               JsonObject json = parseJson(bufferedReader.lines().collect(Collectors.joining()));
               if ("success".equals(json.get("status").getAsString())) {
                  JsonArray asJsonArray = json.getAsJsonArray("messages");
                  if (json.has("online_count")) {
                     int asInt = json.get("online_count").getAsInt();
                     ArrayList elements = new ArrayList();
                     if (json.has("online_users") && json.get("online_users").isJsonArray()) {
                        for (JsonElement jsonElement : json.getAsJsonArray("online_users")) {
                           if (jsonElement != null && !jsonElement.isJsonNull()) {
                              if (jsonElement.isJsonPrimitive()) {
                                 elements.add(jsonElement.getAsString());
                              } else if (jsonElement.isJsonObject()) {
                                 JsonObject asJsonObject = jsonElement.getAsJsonObject();
                                 if (asJsonObject.has("login") && !asJsonObject.get("login").isJsonNull()) {
                                    elements.add(asJsonObject.get("login").getAsString());
                                 }
                              }
                           }
                        }
                     }

                     ArrayList elements2 = new ArrayList();
                     if (json.has("online_sessions") && json.get("online_sessions").isJsonArray()) {
                        for (JsonElement jsonElement2 : json.getAsJsonArray("online_sessions")) {
                           if (jsonElement2 != null && jsonElement2.isJsonObject()) {
                              JsonObject asJsonObject2 = jsonElement2.getAsJsonObject();
                              String asString = asJsonObject2.has("login") && !asJsonObject2.get("login").isJsonNull() ? asJsonObject2.get("login").getAsString() : "";
                              String asString2 = asJsonObject2.has("game_nick") && !asJsonObject2.get("game_nick").isJsonNull() ? asJsonObject2.get("game_nick").getAsString() : "";
                              if (!asString.isBlank() || !asString2.isBlank()) {
                                 elements2.add(new ShouldShowSkycoreTagHelper.Session(asString, asString2));
                              }
                           }
                        }
                     }

                     ShouldShowSkycoreTagHelper.update(asInt, elements, elements2);
                  }

                  if (json.has("dm_messages") && json.get("dm_messages").isJsonArray()) {
                     for (JsonElement jsonElement3 : json.getAsJsonArray("dm_messages")) {
                        JsonObject asJsonObject3 = jsonElement3.getAsJsonObject();
                        String asString3 = asJsonObject3.get("from").getAsString();
                        String asString4 = asJsonObject3.get("to").getAsString();
                        String asString5 = asJsonObject3.get("message").getAsString();
                        String asString6 = asJsonObject3.get("timestamp").getAsString();
                        if (!asString3.equalsIgnoreCase(text)) {
                           displayDmMessage(asString3, asString4, asString5, false);
                           if (asString6.compareTo(lastTimestamp) > 0) {
                              lastTimestamp = asString6;
                           }
                        }
                     }
                  }

                  for (int index = asJsonArray.size() - 1; index >= 0; index--) {
                     JsonObject asJsonObject4 = asJsonArray.get(index).getAsJsonObject();
                     String asString7 = asJsonObject4.get("username").getAsString();
                     String asString8 = asJsonObject4.get("message").getAsString();
                     String asString9 = asJsonObject4.has("prefix") && !asJsonObject4.get("prefix").isJsonNull() ? asJsonObject4.get("prefix").getAsString() : null;
                     String asString10 = asJsonObject4.get("timestamp").getAsString();
                     if (userEnabled && !asString7.equals(text)) {
                        displayIrcMessage(asString7, asString8, asString9);
                        if (asString10.compareTo(lastTimestamp) > 0) {
                           lastTimestamp = asString10;
                           LOGGER.info("Updated IRC timestamp: " + asString10);
                        }
                     }
                  }

                  return;
               }

               return;
            }
         }
      } catch (Exception failure) {
      }
   }

   public static void connect() {
      if (!RemoteApiConfig.isConfigured()) return;
      if (userEnabled && !connecting) {
         if (!connected) {
            connecting = true;
            lastConnectAttemptMs = System.currentTimeMillis();
            final int value = !quietReconnect ? 1 : 0;
            quietReconnect = false;
            if (value != 0) {
               Runnable runnable = () -> {
                  ircSystem("Подключение...");
               };
               runOnMainThread(runnable);
            }

            ExecutorService executorService = Executors.newSingleThreadExecutor();
            executorService.submit(new Runnable() {
               @Override
               public void run() {
                  try {
                     if (!IrcClient.userEnabled) {
                        return;
                     }

                     if (!IrcClient.testConnection()) {
                        IrcClient.connected = false;
                        IrcClient.recordConnectFailure("Не удалось подключиться к IRC");
                        return;
                     }

                     IrcClient.syncPrefixOnConnect();
                     IrcClient.resetMessageCursorOnConnect();
                     IrcClient.connected = true;
                     IrcClient.resetReconnectCycle();
                     if (value != 0) {
                        IrcClient.runOnMainThread(() -> {
                           IrcClient.ircSystem("Подключено");
                        });
                     }
                  } catch (Throwable failure) {
                     IrcClient.connected = false;
                     String text = IrcClient.describeError(failure);
                     IrcClient.recordConnectFailure("Ошибка IRC: " + text);
                     return;
                  } finally {
                     IrcClient.connecting = false;
                  }
               }
            });
         }
      }
   }

   private static void displayIrcMessage(String text, String text2, String text3, String text4) {
      if (userEnabled && text != null && text2 != null) {
         MutableComponent mutableComponent = Component.empty().append(formatPrefixComponent(text3, text));
         MutableComponent mutableComponent2 = Component.literal(" ");
         MutableComponent mutableComponent3 = mutableComponent.append(mutableComponent2.withStyle(ChatFormatting.GRAY));
         MutableComponent mutableComponent4 = Component.literal(text);
         MutableComponent mutableComponent5 = mutableComponent3.append(mutableComponent4.withStyle(ChatFormatting.WHITE));
         MutableComponent mutableComponent6 = Component.literal(": ");
         MutableComponent mutableComponent7 = mutableComponent6.withStyle(ChatFormatting.DARK_GRAY);
         MutableComponent mutableComponent8 = mutableComponent5.append(mutableComponent7);
         MutableComponent mutableComponent9 = Component.literal(text2);
         MutableComponent mutableComponent10 = mutableComponent8.append(mutableComponent9.withStyle(ChatFormatting.RESET));
         Runnable runnable = () -> {
            SendDonationHelper.sendIrc(mutableComponent10);
         };
         runOnMainThread(runnable);
      }
   }

   public static boolean isConnected() {
      return connected;
   }

   private static MutableComponent gradientRole(String text, int index, int index2) {
      MutableComponent mutableComponent = Component.empty();
      int index3 = Math.max(1, text.length() - 1);

      for (int value = 0; value < text.length(); value++) {
         float computedValue = (float)value / index3;
         int computedValue2 = Math.round((index >> 16 & 0xFF) + ((index2 >> 16 & 0xFF) - (index >> 16 & 0xFF)) * computedValue);
         int computedValue3 = Math.round((index >> 8 & 0xFF) + ((index2 >> 8 & 0xFF) - (index >> 8 & 0xFF)) * computedValue);
         int computedValue4 = Math.round((index & 0xFF) + ((index2 & 0xFF) - (index & 0xFF)) * computedValue);
         mutableComponent.append(Component.literal(String.valueOf(text.charAt(value))).withStyle(style -> {
            int color = -1;
            color = computedValue2 << 16 | computedValue3 << 8 | computedValue4;
            TextColor textColor = TextColor.fromRgb(color);
            Style style2 = style.withColor(textColor);
            return style2.withBold(true);
         }));
      }

      return mutableComponent;
   }

   public static void disconnect() {
      connected = false;
      connecting = false;
      lastTimestamp = "";
   }

   private static JsonObject parseJson(String text) {
      JsonElement jsonElement = JsonParser.parseString(text);
      return jsonElement.getAsJsonObject();
   }

   private static void displayDmMessage(String text, String text2, String text3, boolean condition) {
      if (userEnabled && text3 != null) {
         String text4 = condition ? text2 : text;
         MutableComponent mutableComponent = Component.empty();
         String text5 = condition ? "Вы → " : "DM ";
         MutableComponent mutableComponent2 = Component.literal(text5);
         MutableComponent mutableComponent3 = mutableComponent.append(mutableComponent2.withStyle(ChatFormatting.LIGHT_PURPLE));
         String text6 = text4 == null ? "?" : text4;
         MutableComponent mutableComponent4 = Component.literal(text6);
         MutableComponent mutableComponent5 = mutableComponent3.append(mutableComponent4.withStyle(ChatFormatting.WHITE));
         MutableComponent mutableComponent6 = Component.literal(": ");
         MutableComponent mutableComponent7 = mutableComponent5.append(mutableComponent6.withStyle(ChatFormatting.DARK_GRAY));
         MutableComponent mutableComponent8 = Component.literal(text3);
         MutableComponent mutableComponent9 = mutableComponent8.withStyle(ChatFormatting.RESET);
         MutableComponent mutableComponent10 = mutableComponent7.append(mutableComponent9);
         Runnable runnable = () -> {
            SendDonationHelper.sendIrc(mutableComponent10);
         };
         runOnMainThread(runnable);
      }
   }

   private static void displayIrcMessage(String text, String text2, String text3) {
      displayIrcMessage(text, text2, text3, null);
   }

   private static synchronized void recordConnectFailure(String text) {
      if (userEnabled) {
         int computedValue = ++connectFailures;
         long now = System.currentTimeMillis();
         if (computedValue >= 3) {
            connectFailures = 0;
            nextConnectAttemptMs = now + CONNECT_BACKOFF_MS;
            runOnMainThread(() -> {
               ircSystem(text + " (следующая попытка через 10 минут)");
            });
         } else {
            nextConnectAttemptMs = now + 5000L;
            runOnMainThread(() -> {
               ircSystem(text);
            });
         }
      }
   }

   public static void turnOff() {
      userEnabled = false;
      quietReconnect = false;
      resetReconnectCycle();
      disconnect();
      Runnable runnable = () -> {
         ircSystem("IRC выключен (.irc on чтобы снова включить)");
      };
      runOnMainThread(runnable);
   }

   public static boolean isUserEnabled() {
      return userEnabled;
   }

   private static String describeError(Throwable failure) {
      if (failure == null) {
         return "unknown";
      }

      String message = failure.getMessage();
      if (message != null && !message.isBlank()) {
         Class element = failure.getClass();
         return element.getSimpleName() + ": " + message;
      }

      Throwable cause = failure.getCause();
      if (cause != null) {
         if (cause.getMessage() != null) {
            String message2 = cause.getMessage();
            if (!message2.isBlank()) {
               String simpleName = failure.getClass().getSimpleName();
               String simpleName2 = cause.getClass().getSimpleName();
               String message3 = cause.getMessage();
               return simpleName + " <- " + simpleName2 + ": " + message3;
            }
         }
      }

      return failure.getClass().getSimpleName();
   }

   private static void ircSystem(String text) {
      if (text != null && !text.isBlank()) {
         runOnMainThread(() -> {
            MutableComponent mutableComponent = Component.literal(text);
            SendDonationHelper.sendIrc(mutableComponent.withStyle(ChatFormatting.GRAY));
         });
      }
   }

   private IrcClient() {
   }

   public static void sendMessage(String text, String text2, String text3) {
      if (text != null && !text.isBlank() && userEnabled) {
         if (!connected) {
            ircSystem("Ещё не подключено...");
         } else if (checkSendCooldown()) {
            lastSendAtMs = System.currentTimeMillis();
            int value = text2 != null && !text2.isBlank() ? 1 : 0;
            String text4 = value != 0 ? text2.trim() : null;
            Executors.newSingleThreadExecutor().submit(() -> {
               try {
                  JsonObject json = new JsonObject();
                  json.addProperty(RemoteEndpoints.jsonApiKey(), RemoteApiConfig.getApiKey());
                  json.addProperty("username", resolveUsername());
                  json.addProperty("message", text);
                  if (value != 0) {
                     json.addProperty("target", text4);
                  } else {
                     json.addProperty("prefix", resolveSendPrefix());
                  }

                  String text5 = resolveUsername();
                  HttpURLConnection apiUrl = (HttpURLConnection)new URI(RemoteApiConfig.getApiUrl() + RemoteEndpoints.pathSendMessage()).toURL().openConnection();
                  apiUrl.setRequestMethod("POST");
                  apiUrl.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                  apiUrl.setDoOutput(true);
                  apiUrl.setConnectTimeout(3000);
                  apiUrl.setReadTimeout(3000);

                  try (OutputStream outputStream = apiUrl.getOutputStream()) {
                     outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
                  }

                  int responseCode = apiUrl.getResponseCode();
                  if (responseCode == 200) {
                     try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(apiUrl.getInputStream(), StandardCharsets.UTF_8))) {
                        JsonObject json2 = parseJson(bufferedReader.lines().collect(Collectors.joining()));
                        if ("success".equals(json2.get("status").getAsString())) {
                           String asString = json2.get("timestamp").getAsString();
                           if (asString.compareTo(lastTimestamp) > 0) {
                              lastTimestamp = asString;
                           }

                           if (value != 0) {
                              displayDmMessage(text5, text4, text, true);
                           } else {
                              displayIrcMessage(text5, text, resolveSendPrefix(), text3);
                           }
                        } else if (json2.has("message")) {
                           if (value != 0) {
                              ircSystem(json2.get("message").getAsString());
                           } else {
                              ircSystem(json2.get("message").getAsString());
                           }
                        }
                     }
                  } else {
                     String text6 = readError(apiUrl);
                     String text7 = "HTTP " + responseCode + formatHttpError(text6);
                     if (value != 0) {
                        ircSystem(text7);
                     } else {
                        ircSystem(text7);
                     }
                  }
               } catch (Exception failure) {
                  if (value != 0) {
                     ircSystem(failure.getMessage());
                  } else {
                     ircSystem(failure.getMessage());
                  }
               }
            });
         }
      }
   }

   public static void sendMessage(String text, String text2) {
      sendMessage(text, text2, null);
   }

   private static void resetMessageCursorOnConnect() throws Exception {
      String apiUrl = RemoteApiConfig.getApiUrl()
         + RemoteEndpoints.pathGetUpdates()
         + "?"
         + RemoteEndpoints.jsonApiKey()
         + "="
         + URLEncoder.encode(RemoteApiConfig.getApiKey(), StandardCharsets.UTF_8);
      HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
      httpURLConnection.setRequestMethod("GET");
      httpURLConnection.setRequestProperty("Accept-Charset", "UTF-8");
      httpURLConnection.setConnectTimeout(5000);
      httpURLConnection.setReadTimeout(5000);
      if (httpURLConnection.getResponseCode() != 200) {
         lastTimestamp = formatUtcNow();
      } else {
         try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject json = parseJson(bufferedReader.lines().collect(Collectors.joining()));
            if (json.has("latest_timestamp")) {
               lastTimestamp = json.get("latest_timestamp").getAsString();
               return;
            }

            String text = "";
            if (json.has("messages")) {
               for (JsonElement jsonElement : json.getAsJsonArray("messages")) {
                  String asString = jsonElement.getAsJsonObject().get("timestamp").getAsString();
                  if (asString.compareTo(text) > 0) {
                     text = asString;
                  }
               }
            }

            lastTimestamp = text.isEmpty() ? formatUtcNow() : text;
         }
      }
   }

   public static void sendDm(String text, String text2) {
      sendMessage(text2, text, null);
   }

   private static String formatHttpError(String text) {
      if (text != null && !text.isBlank()) {
         String text2 = text.trim();
         if (!text2.regionMatches(true, 0, "<!DOCTYPE", 0, 9) && !text2.regionMatches(true, 0, "<html", 0, 5)) {
            String text3;
            if (text2.length() > 180) {
               text3 = ": " + text2.substring(0, 180) + "...";
            } else {
               text3 = ": " + text2;
            }

            return text3;
         } else {
            return ": Internal Server Error";
         }
      } else {
         return "";
      }
   }

   private static MutableComponent labeledRole(String text) {
      return switch (text) {
         case "owner" -> gradientRole("OWNER", -5636096, -43691);
         case "admin" -> gradientRole("ADMIN", -22016, -171);
         case "media" -> gradientRole("YT", -43691, -1);
         case "beta" -> gradientRole("BETA", -5614081, -43521);
         default -> gradientRole("USER", -11141291, -16733696);
      };
   }

   static {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         byte flag = -1;
         byte flag2 = 0;
         flag = -1;
         flag2 = 0;
         flag = 1;
         byte flag3 = 1;
         SCHEDULER.shutdown();

         try {
            if (!SCHEDULER.awaitTermination(1L, TimeUnit.SECONDS)) {
               SCHEDULER.shutdownNow();
            }
         } catch (InterruptedException failure) {
            flag = 1;
            flag3 = -1;
            SCHEDULER.shutdownNow();
            Thread thread = Thread.currentThread();
            flag3 = 1;
            thread.interrupt();
         }
      }));
   }

   public static void init() {
      userEnabled = true;
      resetReconnectCycle();
      connect();
      SCHEDULER.scheduleAtFixedRate(new Runnable() {
         @Override
         public void run() {
            try {
               if (!IrcClient.userEnabled) {
                  return;
               }

               if (IrcClient.connected) {
                  IrcClient.checkForUpdates();
               } else if (!IrcClient.connecting) {
                  long now = System.currentTimeMillis();
                  if (now >= IrcClient.nextConnectAttemptMs) {
                     if (now - IrcClient.lastConnectAttemptMs >= 5000L) {
                        IrcClient.lastConnectAttemptMs = now;
                        IrcClient.quietReconnect = true;
                        IrcClient.connect();
                     }
                  }
               }
            } catch (Exception failure) {
            }
         }
      }, 1L, 1L, TimeUnit.SECONDS);
   }

   private static String mapProfileRole(String text) {
      String text2 = normalizeRole(text);

      return switch (text2) {
         case "admin" -> "admin";
         case "owner" -> "owner";
         case "beta", "betatester", "beta_tester", "beta tester" -> "beta";
         case "media", "yt", "youtube" -> "media";
         default -> !"user".equals(text2) && !text2.isBlank() ? text2 : currentPrefix;
      };
   }

   public static void sendDm(String text, String text2, String text3) {
      sendMessage(text2, text, text3);
   }

   public static void ensureRecentMessagesLoaded() {
      recentMessagesLoaded = true;
   }

   public static void sendMessage(String text) {
      sendMessage(text, null, null);
   }

   private static void syncPrefixOnConnect() {
      try {
         String text = resolveSendPrefix();
         JsonObject json = new JsonObject();
         json.addProperty(RemoteEndpoints.jsonApiKey(), RemoteApiConfig.getApiKey());
         json.addProperty("username", resolveUsername());
         json.addProperty("prefix", text);
         HttpURLConnection apiUrl = (HttpURLConnection)new URI(RemoteApiConfig.getApiUrl() + RemoteEndpoints.pathSetPrefix()).toURL().openConnection();
         apiUrl.setRequestMethod("POST");
         apiUrl.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
         apiUrl.setDoOutput(true);
         apiUrl.setConnectTimeout(5000);
         apiUrl.setReadTimeout(5000);

         try (OutputStream outputStream = apiUrl.getOutputStream()) {
            outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
         }

         if (apiUrl.getResponseCode() == 200) {
            currentPrefix = text;
         }
      } catch (Exception failure) {
      }
   }

   private static synchronized void resetReconnectCycle() {
      connectFailures = 0;
      nextConnectAttemptMs = 0L;
      lastConnectAttemptMs = 0L;
   }

   private static boolean testConnection() throws Exception {
      String apiUrl = RemoteApiConfig.getApiUrl();
      if (apiUrl != null && !apiUrl.isBlank()) {
         HttpURLConnection httpURLConnection = (HttpURLConnection)new URI(apiUrl).toURL().openConnection();
         httpURLConnection.setRequestMethod("GET");
         httpURLConnection.setConnectTimeout(5000);
         httpURLConnection.setReadTimeout(5000);
         if (httpURLConnection.getResponseCode() != 200) {
            return false;
         }

         try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject json = parseJson(bufferedReader.lines().collect(Collectors.joining()));
            if (json != null && json.has("status") && json.get("status") != null && !json.get("status").isJsonNull()) {
               return "success".equals(json.get("status").getAsString());
            }

         }

         return false;
      } else {
         throw new IllegalStateException("IRC API URL is empty (secret decode failed)");
      }
   }

   private static boolean checkSendCooldown() {
      long now = System.currentTimeMillis();
      long timestamp = now - lastSendAtMs;
      if (lastSendAtMs > 0L && timestamp < 4000L) {
         long timestamp2 = (4000L - timestamp + 999L) / 1000L;
         String text = "Подожди " + timestamp2 + " сек.";
         ircSystem(text);
         return false;
      } else {
         return true;
      }
   }

   public static void setPrefix(String prefix) {
      if (prefix != null && !prefix.isBlank()) {
         Executors.newSingleThreadExecutor().submit(() -> {
            try {
               JsonObject json = new JsonObject();
               json.addProperty(RemoteEndpoints.jsonApiKey(), RemoteApiConfig.getApiKey());
               json.addProperty("username", resolveUsername());
               json.addProperty("prefix", prefix);
               HttpURLConnection apiUrl = (HttpURLConnection)new URI(RemoteApiConfig.getApiUrl() + RemoteEndpoints.pathSetPrefix()).toURL().openConnection();
               apiUrl.setRequestMethod("POST");
               apiUrl.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
               apiUrl.setDoOutput(true);
               apiUrl.setConnectTimeout(3000);
               apiUrl.setReadTimeout(3000);

               try (OutputStream outputStream = apiUrl.getOutputStream()) {
                  outputStream.write(json.toString().getBytes(StandardCharsets.UTF_8));
               }

               int responseCode = apiUrl.getResponseCode();
               if (responseCode == 200) {
                  try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(apiUrl.getInputStream(), StandardCharsets.UTF_8))) {
                     JsonObject json2 = parseJson(bufferedReader.lines().collect(Collectors.joining()));
                     if ("success".equals(json2.get("status").getAsString())) {
                        currentPrefix = prefix;
                     } else if (json2.has("message")) {
                        ircSystem(json2.get("message").getAsString());
                     }
                  }
               } else {
                  currentPrefix = prefix;
               }
            } catch (Exception failure) {
               currentPrefix = prefix;
            }
         });
      }
   }

   private static void runOnMainThread(Runnable runnable) {
      Minecraft instance = Minecraft.getInstance();
      if (instance != null) {
         if (instance.isSameThread()) {
            runnable.run();
         } else {
            instance.execute(runnable);
         }
      }
   }

   public static void turnOn() {
      if (userEnabled) {
         if (connected || connecting) {
            runOnMainThread(() -> {
               ircSystem("IRC уже включён");
            });
            return;
         }
      }

      userEnabled = true;
      quietReconnect = false;
      resetReconnectCycle();
      connect();
   }

   private static String readError(HttpURLConnection httpURLConnection) {

      try {
         InputStream errorStream = httpURLConnection.getErrorStream();
         InputStreamReader inputStreamReader = new InputStreamReader(errorStream, StandardCharsets.UTF_8);

         try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            var stream = bufferedReader.lines();
            Collector<CharSequence, ?, String> collector = Collectors.joining();
            return stream.collect(collector);
         }
      } catch (Exception failure) {
         return "";
      }
   }

   public static String getCurrentPrefix() {
      return currentPrefix;
   }

   private static MutableComponent formatPrefixComponent(String text, String text2) {
      if (RemoteApiConfig.isOwnerUsername(text2)) {
         return labeledRole("owner");
      }

      String text3 = normalizeRole(text);

      return switch (text3) {
         case "owner" -> labeledRole("owner");
         case "admin" -> labeledRole("admin");
         case "beta", "betatester", "beta_tester", "beta tester" -> labeledRole("beta");
         case "media", "yt", "youtube" -> labeledRole("media");
         default -> labeledRole("user");
      };
   }

   private static String normalizeRole(String text) {
      return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
   }
}

