package tokyo.ghoul.api.auth;

public final class SiteProfile {
    private static volatile Integer uid = 1337;

    private SiteProfile() {}

    public static Integer uid() {
        return uid;
    }

    public static void lookupAsync(String nickname) {
        // Telemetry/Appwrite cloud connection removed for safety
        uid = 1337;
    }
}

