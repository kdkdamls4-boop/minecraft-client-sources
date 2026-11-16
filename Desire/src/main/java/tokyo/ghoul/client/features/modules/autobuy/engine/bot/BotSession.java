package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BotSession {
    private static final int CHAT_HISTORY = 60;

    private final String name;
    private volatile String serverAddress;
    private String password = "";
    private boolean autoReconnect;
    private volatile SessionState state;

    private volatile Connection connection;
    private ClientPacketListener networkHandler;
    private LocalPlayer player;
    private ClientLevel world;
    private MultiPlayerGameMode interactionManager;
    private Screen screen;
    private PacketProcessor packetProcessor;

    private final Map<String, Boolean> moduleStates = new HashMap<>();
    private final Deque<String> chatLog = new ArrayDeque<>();

    private long connectedTime;
    private long nextRespawnAtMs;
    private volatile String disconnectReason;
    private String actionBar;
    private long actionBarAtMs;

    private String pendingAuthCommand;
    private long pendingAuthAtMs;
    private boolean registerSent;
    private boolean loginSent;

    public BotSession(String name) {
        this.name = name;
        this.state = SessionState.DISCONNECTED;
    }

    public void connect(String serverIp) {
        if (this.state == SessionState.CONNECTED || this.state == SessionState.CONNECTING) {
            return;
        }

        this.serverAddress = serverIp;
        this.state = SessionState.CONNECTING;
        this.disconnectReason = null;
        this.actionBar = null;
        this.pendingAuthCommand = null;
        this.registerSent = false;
        this.loginSent = false;
        this.packetProcessor = new PacketProcessor(Minecraft.getInstance().getRunningThread());

        BotConnectionUtil.connectBot(this, serverIp);
    }

    public void disconnect() {
        if (this.state == SessionState.DISCONNECTED) {
            return;
        }

        Connection current = this.connection;
        if (current != null && current.isConnected()) {
            current.disconnect(Component.literal("Disconnected"));
        }

        cleanup();
        this.state = SessionState.DISCONNECTED;
    }

    void bindMain(Connection connection, ClientPacketListener handler) {
        this.connection = connection;
        this.networkHandler = handler;
        this.packetProcessor = new PacketProcessor(Minecraft.getInstance().getRunningThread());
        this.state = SessionState.CONNECTED;
    }

    public void tick() {
        Connection current = this.connection;
        if (current == null) {
            return;
        }

        if (!current.isConnected()) {
            if (this.disconnectReason == null) {
                DisconnectionDetails details = current.getDisconnectionDetails();
                if (details != null && details.reason() != null) {
                    this.disconnectReason = details.reason().getString();
                }
            }
            BotSessionManager.getInstance().handleBotDisconnect(this);
            return;
        }

        if (!BotContext.enter(this)) {
            return;
        }
        try {
            if (this.packetProcessor != null) {
                this.packetProcessor.processQueuedPackets();
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.player != null && mc.level != null) {
                tickWorld(mc);
            } else if (current.isConnected()) {
                current.tick();
            }
        } catch (Throwable t) {
            this.disconnectReason = "Ошибка: " + t;
            current.disconnect(Component.literal("Bot crashed"));
        } finally {
            BotContext.exit(this);
        }
    }

    private void tickWorld(Minecraft mc) {
        mc.gameMode.tick();
        if (mc.level == null || mc.player == null || !this.connection.isConnected()) {
            return;
        }

        Runnable sectionReady = mc.player.connection.getPlayerCompiledSectionCallback();
        if (sectionReady != null && mc.level.hasChunk(
                SectionPos.blockToSectionCoord(mc.player.getBlockX()),
                SectionPos.blockToSectionCoord(mc.player.getBlockZ()))) {
            sectionReady.run();
        }

        if (mc.player.isDeadOrDying() && this != BotSessionManager.getInstance().getMainSession()) {
            long now = System.currentTimeMillis();
            if (now >= this.nextRespawnAtMs) {
                this.nextRespawnAtMs = now + 1500L;
                mc.player.respawn();
            }
        }

        mc.level.tickEntities();
        this.connection.send(ServerboundClientTickEndPacket.INSTANCE);
    }

    public void addChatLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        synchronized (this.chatLog) {
            this.chatLog.addLast(line);
            while (this.chatLog.size() > CHAT_HISTORY) {
                this.chatLog.removeFirst();
            }
        }
    }

    public List<String> getRecentChat(int count) {
        synchronized (this.chatLog) {
            List<String> all = new ArrayList<>(this.chatLog);
            return all.subList(Math.max(0, all.size() - count), all.size());
        }
    }

    public void setActionBar(String text) {
        this.actionBar = text;
        this.actionBarAtMs = System.currentTimeMillis();
    }

    public boolean isActive() {
        Connection current = this.connection;
        return this.state == SessionState.CONNECTED && current != null && current.isConnected();
    }

    public boolean isPlayable() {
        return isActive() && this.world != null && this.player != null && this.interactionManager != null;
    }

    public void cleanup() {
        if (this.packetProcessor != null) {
            this.packetProcessor.close();
        }
        this.player = null;
        this.world = null;
        this.interactionManager = null;
        this.networkHandler = null;
        this.connection = null;
        this.screen = null;
    }

    public void setState(SessionState state) {
        this.state = state;
        if (state == SessionState.CONNECTED) {
            this.connectedTime = System.currentTimeMillis();
        }
    }

    public long getUptime() {
        if (state != SessionState.CONNECTED) {
            return 0;
        }
        return System.currentTimeMillis() - connectedTime;
    }
}
