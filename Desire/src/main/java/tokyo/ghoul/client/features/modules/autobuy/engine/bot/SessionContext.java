package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

public class SessionContext {
    private static final ThreadLocal<BotSession> currentSession = new ThreadLocal<>();

    public static BotSession getCurrent() {
        BotSession session = currentSession.get();
        if (session == null) {
            return BotSessionManager.getInstance().getMainSession();
        }
        return session;
    }

    public static void setCurrent(BotSession session) {
        currentSession.set(session);
    }

    public static void clear() {
        currentSession.remove();
    }

    public static boolean isMainSession() {
        return getCurrent() == BotSessionManager.getInstance().getMainSession();
    }
}
