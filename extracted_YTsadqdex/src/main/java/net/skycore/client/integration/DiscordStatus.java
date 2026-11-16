package net.skycore.client.integration;

/** Retains the client UI contract without Discord IPC or presence sharing. */
public final class DiscordStatus {
    public DiscordStatus start() { return this; }
    public void updatePresence() {}
    public void stopRpc() {}
    public String getDiscordUserId() { return null; }
    public String getDiscordUsername() { return null; }
    public String getDiscordAvatar() { return null; }
    public String getAvatarUrl() { return null; }
    public boolean hasDiscordUser() { return false; }
}
