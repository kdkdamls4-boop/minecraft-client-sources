package net.skycore.client.integration;

/** Plain endpoint names; external services disabled and neutralized. */
public final class RemoteEndpoints {
    private RemoteEndpoints() {}
    public static String pathPresence() { return "presence"; }
    public static String pathPartyDecline() { return "party_decline"; }
    public static String pathSendMessage() { return "send_message"; }
    public static String decodeApiKey() { return ""; }
    public static String pathSetPrefix() { return "set_prefix"; }
    public static String pathLeaveParty() { return "leave_party"; }
    public static String pathGetUpdates() { return "get_updates"; }
    public static String pathDisbandParty() { return "disband_party"; }
    public static String pathSendPartyMessage() { return "send_party_message"; }
    public static String jsonApiKey() { return "api_key"; }
    public static String pathUpdateCoordinates() { return "update_coordinates"; }
    public static String statusOnline() { return "online"; }
    public static String pathGetPartyCoordinates() { return "get_party_coordinates"; }
    public static String statusOffline() { return "offline"; }
    public static String decodeApiUrl() { return ""; }
    public static String pathPartyAccept() { return "party_accept"; }
    public static String pathPartyInvite() { return "party_invite"; }
    public static String pathOnlineCount() { return "online_count"; }
}
