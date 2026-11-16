package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

import com.mojang.authlib.GameProfile;
import io.netty.channel.ChannelFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.network.EventLoopGroupHolder;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

public class BotConnectionUtil {

    public static void connectBot(BotSession session, String serverAddress) {
        final Minecraft client = Minecraft.getInstance();

        Thread thread = new Thread(() -> {
            try {
                ServerAddress address = ServerAddress.parseString(serverAddress);
                Optional<InetSocketAddress> resolved = ServerNameResolver.DEFAULT.resolveAddress(address)
                        .map(ResolvedServerAddress::asInetSocketAddress);
                if (resolved.isEmpty()) {
                    fail(session, "Сервер не найден: " + serverAddress);
                    return;
                }
                InetSocketAddress inet = resolved.get();

                Connection connection = new Connection(PacketFlow.CLIENTBOUND);
                ((IBotConnection) (Object) connection).ghoul$setBotSession(session);

                ChannelFuture future = Connection.connect(inet, EventLoopGroupHolder.remote(client.options.useNativeTransport()), connection);
                future.syncUninterruptibly();

                if (session.getState() != SessionState.CONNECTING) {
                    connection.disconnect(Component.literal("Cancelled"));
                    return;
                }

                GameProfile profile = createOfflineProfile(session.getName());
                ServerData serverData = new ServerData(session.getName(), serverAddress, ServerData.Type.OTHER);
                ClientHandshakePacketListenerImpl handshakeHandler = new ClientHandshakePacketListenerImpl(
                        connection,
                        client,
                        serverData,
                        null,
                        false,
                        null,
                        text -> {
                        },
                        new LevelLoadTracker(),
                        null
                );

                BotSessionManager.getInstance().registerBotListener(handshakeHandler, session);

                connection.initiateServerboundPlayConnection(address.getHost(), address.getPort(),
                        LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND, handshakeHandler, false);
                connection.send(new ServerboundHelloPacket(profile.name(), profile.id()));

                session.setConnection(connection);
            } catch (Exception e) {
                fail(session, "Ошибка подключения: " + e.getMessage());
            }
        }, "Bot-Connector-" + session.getName());

        thread.setDaemon(true);
        thread.start();
    }

    private static void fail(BotSession session, String reason) {
        session.setState(SessionState.ERROR);
        session.setDisconnectReason(reason);
        Minecraft.getInstance().execute(() -> BotSessionManager.getInstance().onConnectFailed(session));
    }

    private static GameProfile createOfflineProfile(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        return new GameProfile(uuid, name);
    }
}
