package tokyo.ghoul.api.system;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.firstdark.rpc.enums.ErrorCode;
import dev.firstdark.rpc.handlers.RPCEventHandler;
import dev.firstdark.rpc.models.DiscordRichPresence;
import dev.firstdark.rpc.models.User;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.Connection;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DiscordRpc {
    private static final String APPLICATION_ID = "1550638960282443828";
    private static final String LARGE_IMAGE_URL = "https://i.ibb.co/V0jjGn6t/a5d1e3a2-0062-4099-b986-58486c7bef3c.png";
    private static final String LARGE_IMAGE_TEXT = "krasava marat";
    private static final String CLIENT_UID = "1337";

    private static dev.firstdark.rpc.DiscordRpc rpc;
    public static volatile User currentUser;
    public static volatile String currentUserId;
    public static volatile String currentAvatar;
    private static boolean started;
    private static Thread presenceThread;
    private static long rpcStartTimestampSeconds;

    private DiscordRpc() {}

    public static String getDiscordUsername() {
        User user = currentUser;
        if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername();
        }
        Minecraft mc = Minecraft.getInstance();
        return mc.getUser() != null ? mc.getUser().getName() : "Player";
    }

    public static String getAvatarUrl() {
        if (currentAvatar == null || currentAvatar.isBlank() || currentUserId == null) {
            return "https://cdn.discordapp.com/embed/avatars/0.png";
        }
        return "https://cdn.discordapp.com/avatars/" + currentUserId + "/" + currentAvatar + ".png?size=128";
    }

    public static synchronized void start() {
        if (started) return;

        rpc = new dev.firstdark.rpc.DiscordRpc();
        rpc.setDebugMode(false);

        RPCEventHandler handler = new RPCEventHandler() {
            @Override
            public void ready(User user) {
                currentUser = user;
                if (currentAvatar == null) currentAvatar = user.getAvatar();
                if (currentUserId == null) {
                    try {
                        Field f = user.getClass().getDeclaredField("userId");
                        f.setAccessible(true);
                        currentUserId = (String) f.get(user);
                    } catch (Exception ignored) {}
                }

                if (currentUserId == null) {
                    extractUserIdFromLibrary();
                }

                if (currentUserId == null) {
                    try {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        String userJson = gson.toJson(user);
                        JsonObject obj = JsonParser.parseString(userJson).getAsJsonObject();
                        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                            if ("id".equals(entry.getKey()) || "userId".equals(entry.getKey())) {
                                currentUserId = entry.getValue().getAsString();
                            }
                        }
                    } catch (Exception ignored) {}
                }

                updatePresenceSafely();
            }

            @Override
            public void disconnected(ErrorCode errorCode, String message) {
                currentUser = null;
            }

            @Override
            public void errored(ErrorCode errorCode, String message) {
                System.err.println("[Static] Discord RPC error: " + errorCode + " - " + message);
            }
        };

        try {
            rpc.init(APPLICATION_ID, handler, false);
            started = true;
            rpcStartTimestampSeconds = System.currentTimeMillis() / 1000L;
            startPresenceThread();
        } catch (Throwable throwable) {
            System.err.println("[Static] Failed to init Discord RPC: " + throwable.getMessage());
            rpc = null;
            started = false;
        }

        fetchDiscordUserIdSync();
    }

    public static synchronized void startRPC() {
        start();
    }

    public static synchronized void stopRPC() {
        stop();
    }

    private static void startPresenceThread() {
        if (presenceThread != null) return;

        presenceThread = new Thread(() -> {
            while (started && rpc != null) {
                updatePresenceSafely();
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException ignored) {
                    return;
                }
            }
        }, "Static-DiscordRPC");
        presenceThread.setDaemon(true);
        presenceThread.start();
    }

    private static void updatePresenceSafely() {
        try {
            updatePresence();
        } catch (Throwable throwable) {
            System.err.println("[Static] Discord RPC update failed: " + throwable.getMessage());
        }
    }

    private static void updatePresence() {
        if (rpc == null) return;

        String serverIp = getServerIp();
        String details = "Version: " + ClientInfo.VERSION;
        String state = "UID: " + CLIENT_UID;

        var builder = DiscordRichPresence.builder()
                .details(details)
                .state(state)
                .largeImageKey(LARGE_IMAGE_URL)
                .largeImageText(LARGE_IMAGE_TEXT)
                .startTimestamp(rpcStartTimestampSeconds)
                .button(DiscordRichPresence.RPCButton.of("Discord", "https://discord.gg/EGTpSskWPG"))
                .button(DiscordRichPresence.RPCButton.of("Telegram", "https://t.me/DesireAlpha"));

        if (serverIp != null) {
            String name = getServerName();
            builder.smallImageKey("https://api.mcstatus.io/v2/icon/" + serverIp)
                    .smallImageText((name == null || name.isBlank()) ? serverIp : name);
        }

        rpc.updatePresence(builder.build());
    }

    private static Object lastIpServer;
    private static Object lastIpConn;
    private static String cachedServerIp;

    private static String getServerIp() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null || mc.isLocalServer()) return null;

            Object server = mc.getCurrentServer();
            Object conn = mc.getConnection() != null ? mc.getConnection().getConnection() : null;
            if (server == lastIpServer && conn == lastIpConn) {
                return cachedServerIp;
            }
            lastIpServer = server;
            lastIpConn = conn;

            String serverAddress = null;
            if (server != null) {
                serverAddress = ((ServerData) server).ip;
            }

            if ((serverAddress == null || serverAddress.isEmpty()) && conn != null) {
                SocketAddress address = ((Connection) conn).getRemoteAddress();
                if (address != null) {
                    serverAddress = address.toString();
                    if (serverAddress.startsWith("/")) {
                        serverAddress = serverAddress.substring(1);
                    }
                }
            }

            return cachedServerIp = (serverAddress == null || serverAddress.isEmpty()) ? null : serverAddress;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String getServerName() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return null;
            ServerData server = mc.getCurrentServer();
            if (server == null) return null;
            String name = server.name;
            return (name == null || name.isBlank()) ? null : name;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void fetchDiscordUserIdSync() {
        Thread t = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                String pipePath = getPipePath(i);
                if (pipePath == null) continue;
                try {
                    java.io.RandomAccessFile raf = new java.io.RandomAccessFile(pipePath, "rw");
                    try {
                        if (readReadyUser(raf)) return;
                    } finally {
                        raf.close();
                    }
                } catch (Exception ignored) {}
            }
        }, "Static-DiscordIPC");
        t.setDaemon(true);
        t.start();
    }

    private static volatile boolean userIdRetryInProgress = false;
    private static volatile long lastUserIdRetryTime = 0;
    private static final long USER_ID_RETRY_COOLDOWN_MS = 10000;

    public static void retryFetchUserId() {
        if (currentUserId != null) return;
        if (userIdRetryInProgress) return;
        long now = System.currentTimeMillis();
        if (now - lastUserIdRetryTime < USER_ID_RETRY_COOLDOWN_MS) return;
        lastUserIdRetryTime = now;

        userIdRetryInProgress = true;
        Thread t = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String pipePath = getPipePath(i);
                    if (pipePath == null) continue;
                    try {
                        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(pipePath, "rw");
                        try {
                            if (readReadyUser(raf)) return;
                        } finally {
                            raf.close();
                        }
                    } catch (Exception ignored) {}
                }
            } finally {
                userIdRetryInProgress = false;
            }
        }, "Static-DiscordIPC-Retry");
        t.setDaemon(true);
        t.start();
    }

    private static boolean readReadyUser(java.io.RandomAccessFile raf) throws IOException {
        JsonObject handshakeJson = new JsonObject();
        handshakeJson.addProperty("v", 1);
        handshakeJson.addProperty("client_id", APPLICATION_ID);
        byte[] jsonBytes = handshakeJson.toString().getBytes(StandardCharsets.UTF_8);

        ByteBuffer frame = ByteBuffer.allocate(jsonBytes.length + 8);
        frame.order(ByteOrder.LITTLE_ENDIAN);
        frame.putInt(0);
        frame.putInt(jsonBytes.length);
        frame.put(jsonBytes);
        frame.flip();
        raf.write(frame.array());

        ByteBuffer intBuf = ByteBuffer.allocate(4);
        intBuf.order(ByteOrder.LITTLE_ENDIAN);
        ipcReadFully(raf, intBuf, 8000);

        intBuf.clear();
        intBuf.order(ByteOrder.LITTLE_ENDIAN);
        ipcReadFully(raf, intBuf, 5000);
        int respLen = intBuf.getInt(0);
        if (respLen <= 0 || respLen > 65536) return false;

        ByteBuffer bodyBuf = ByteBuffer.allocate(respLen);
        bodyBuf.order(ByteOrder.LITTLE_ENDIAN);
        ipcReadFully(raf, bodyBuf, 5000);
        bodyBuf.flip();

        byte[] respBody = new byte[respLen];
        bodyBuf.get(respBody);
        JsonObject obj = JsonParser.parseString(new String(respBody, StandardCharsets.UTF_8)).getAsJsonObject();
        String cmd = obj.has("cmd") ? obj.get("cmd").getAsString() : null;
        String evt = obj.has("evt") ? obj.get("evt").getAsString() : null;
        if ("DISPATCH".equals(cmd) && "READY".equals(evt) && obj.has("data")) {
            JsonObject data = obj.getAsJsonObject("data");
            if (data.has("user")) {
                JsonObject userObj = data.getAsJsonObject("user");
                if (userObj.has("id")) currentUserId = userObj.get("id").getAsString();
                if (userObj.has("avatar")) currentAvatar = userObj.get("avatar").getAsString();
                return true;
            }
        }
        return false;
    }

    private static void ipcReadFully(java.io.RandomAccessFile raf, ByteBuffer buf, long timeoutMs) throws IOException {
        buf.rewind();
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (raf.length() < buf.remaining()) {
            if (System.currentTimeMillis() > deadline) throw new IOException("IPC read timeout, waiting for " + buf.remaining() + " bytes");
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
        while (buf.hasRemaining()) {
            raf.getChannel().read(buf);
        }
    }

    private static String getPipePath(int index) {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return "\\\\.\\pipe\\discord-ipc-" + index;
        }
        return System.getProperty("java.io.tmpdir") + "/discord-ipc-" + index;
    }

    private static void extractUserIdFromLibrary() {
        if (currentUserId != null || rpc == null) return;
        try {
            for (Field f : rpc.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(rpc);
                if (val == null) continue;
                deepSearchUserId(val, new HashSet<>(), 0);
                if (currentUserId != null) return;
            }
            if (rpc.getClass().getSuperclass() != null) {
                for (Field f : rpc.getClass().getSuperclass().getDeclaredFields()) {
                    f.setAccessible(true);
                    Object val = f.get(rpc);
                    if (val == null) continue;
                    deepSearchUserId(val, new HashSet<>(), 0);
                    if (currentUserId != null) return;
                }
            }
        } catch (Exception ignored) {}
    }

    private static void deepSearchUserId(Object obj, Set<Integer> visited, int depth) {
        if (obj == null || depth > 8 || obj instanceof Class || obj instanceof Thread || obj instanceof ThreadGroup) return;
        int id = System.identityHashCode(obj);
        if (visited.contains(id)) return;
        visited.add(id);

        if (obj instanceof String s) {
            if (s.length() >= 17 && s.length() <= 20 && s.chars().allMatch(Character::isDigit)) {
                currentUserId = s;
            }
            return;
        }

        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                Object val = f.get(obj);
                if (val == null) continue;

                if (val instanceof String str) {
                    if (str.length() >= 17 && str.length() <= 20 && str.chars().allMatch(Character::isDigit)) {
                        currentUserId = str;
                        return;
                    }
                } else if (val instanceof JsonObject json) {
                    if (json.has("id") && json.has("username")) {
                        currentUserId = json.get("id").getAsString();
                        if (json.has("avatar")) currentAvatar = json.get("avatar").getAsString();
                        return;
                    }
                } else if (!(val instanceof Number) && !(val instanceof Boolean) && !(val instanceof Character) && val.getClass().getName().startsWith("ua.") || val.getClass().getName().startsWith("dev.")) {
                    deepSearchUserId(val, visited, depth + 1);
                    if (currentUserId != null) return;
                }
            }
        } catch (Exception ignored) {}
    }

    public static synchronized void stop() {
        if (rpc != null) {
            rpc.shutdown();
            rpc = null;
        }
        currentUser = null;
        started = false;
        if (presenceThread != null) {
            try {
                presenceThread.interrupt();
            } catch (Throwable ignored) {
            }
            presenceThread = null;
        }
    }
}
