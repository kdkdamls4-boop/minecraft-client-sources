package net.skycore.client.integration;

public final class RemoteApiConfig {
    public static final String CHAT_PREFIX = "$";
    private RemoteApiConfig() {}
    public static boolean isOwnerUsername(String username) { return false; }
    public static String getApiKey() { return ""; }
    public static String getApiUrl() { return ""; }
    public static boolean isConfigured() { return false; }
}
