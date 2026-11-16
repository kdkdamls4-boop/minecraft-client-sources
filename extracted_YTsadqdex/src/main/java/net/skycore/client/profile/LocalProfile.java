package net.skycore.client.profile;

/** Clean local profile without hardware identifiers, remote calls or advertising links. */
public final class LocalProfile {
    public static final String DISPLAY_NAME = "Skycore";
    private static final LocalProfile INSTANCE = new LocalProfile();
    private LocalProfile() {}
    public static LocalProfile getInstance() { return INSTANCE; }
    public String profile(String field) {
        return switch (field) {
            case "username", "role" -> DISPLAY_NAME;
            case "uid" -> "0";
            default -> "";
        };
    }
}
