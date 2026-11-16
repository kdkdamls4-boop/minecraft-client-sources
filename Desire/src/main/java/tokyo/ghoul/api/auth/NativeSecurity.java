package tokyo.ghoul.api.auth;

public final class NativeSecurity {

    private NativeSecurity() {}

    public static void loadNativeLibrary() {
    }

    public static boolean initSecurity(String sessionToken, long launcherPid) {
        return true;
    }

    public static String getHwid() {
        return "unbound";
    }

    public static boolean checkSubscription() {
        return true;
    }

    public static void heartbeat() {
    }

    public static void onNativeEntrypoint() {
    }

    public static boolean bootstrap() {
        return true;
    }

    public static boolean isVerified() {
        return true;
    }
}
