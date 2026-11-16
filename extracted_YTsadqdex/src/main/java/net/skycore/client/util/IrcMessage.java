package net.skycore.client.util;

public final class IrcMessage {
   private String timestamp;
   private String username;
   private String message;
   private String prefix;

   public String getPrefix() {
      return this.prefix;
   }

   public String getTimestamp() {
      return this.timestamp;
   }

   public String getUsername() {
      return this.username;
   }

   public void setMessage(String message2) {
      this.message = message2;
   }

   public void setUsername(String username2) {
      this.username = username2;
   }

   public void setPrefix(String prefix2) {
      this.prefix = prefix2;
   }

   public void setTimestamp(String timestamp2) {
      this.timestamp = timestamp2;
   }

   public String getMessage() {
      return this.message;
   }
}
