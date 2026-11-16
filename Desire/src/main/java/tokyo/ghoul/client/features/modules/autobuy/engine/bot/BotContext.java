package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import tokyo.ghoul.api.event.events.Event;

public final class BotContext {

    private static BotSession active;

    private static ClientLevel savedWorld;
    private static LocalPlayer savedPlayer;
    private static MultiPlayerGameMode savedGameMode;
    private static HitResult savedHitResult;
    private static Entity savedPickEntity;
    private static BotSession savedSession;

    private BotContext() {
    }

    public static boolean enter(BotSession session) {
        if (active != null) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();

        savedWorld = mc.level;
        savedPlayer = mc.player;
        savedGameMode = mc.gameMode;
        savedHitResult = mc.hitResult;
        savedPickEntity = mc.crosshairPickEntity;
        savedSession = SessionContext.getCurrent();

        mc.level = session.getWorld();
        mc.player = session.getPlayer();
        mc.gameMode = session.getInteractionManager();
        mc.hitResult = null;
        mc.crosshairPickEntity = null;
        SessionContext.setCurrent(session);

        active = session;
        if (BotSessionManager.getInstance().shouldMuteEvents(session)) {
            Event.setMutedThread(Thread.currentThread());
        }
        return true;
    }

    public static void exit(BotSession session) {
        if (active != session) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        session.setWorld(mc.level);
        session.setPlayer(mc.player);
        session.setInteractionManager(mc.gameMode);
        if (mc.player != null) {
            session.setNetworkHandler(mc.player.connection);
        }

        mc.level = savedWorld;
        mc.player = savedPlayer;
        mc.gameMode = savedGameMode;
        mc.hitResult = savedHitResult;
        mc.crosshairPickEntity = savedPickEntity;
        SessionContext.setCurrent(savedSession);
        Event.setMutedThread(null);

        active = null;
        savedWorld = null;
        savedPlayer = null;
        savedGameMode = null;
        savedHitResult = null;
        savedPickEntity = null;
        savedSession = null;

        if (session.getState() == SessionState.CONNECTING
                && session.getWorld() != null
                && session.getPlayer() != null) {
            session.setState(SessionState.CONNECTED);
        }
    }

    public static BotSession current() {
        return active;
    }

    public static boolean isBackgroundContextActive() {
        BotSession session = active;
        return session != null && !BotSessionManager.getInstance().isForeground(session);
    }
}
