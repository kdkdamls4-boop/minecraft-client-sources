package deluxe.client.storage;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public final class AccountEntry {
   private static final DateTimeFormatter field12109 = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
   private static final DateTimeFormatter field12110 = DateTimeFormatter.ofPattern("HH:mm:ss");
   private String field12111;
   private UUID field12112;
   private AccountEntry.Nested0488 field12113;
   private String field12114;
   private String field12115;
   private long field12116;
   private long field12117;
   private boolean field12118;
   private ArrayList<String[]> field12119 = new ArrayList<>();

   public static String method3439(long local5615_0) {
      long local5615_2 = local5615_0 / 60000L;
      long local5615_4 = local5615_2 / 1440L;
      long local5615_6 = local5615_2 / 60L % 24L;
      long local5615_8 = local5615_2 % 60L;
      StringBuilder stringBuilder = new StringBuilder();
      if (local5615_4 > 0L) {
         stringBuilder.append(local5615_4).append("d ");
      }

      if (local5615_4 > 0L || local5615_6 > 0L) {
         stringBuilder.append(local5615_6).append("h ");
      }

      stringBuilder.append(local5615_8).append('m');
      return stringBuilder.toString();
   }

   public void method3440(long local5616_1) {
      this.field12117 = this.field12117 + Math.max(0L, local5616_1);
   }

   public static AccountEntry method3441(String local5617_0) {
      return new AccountEntry(
         local5617_0, method3449(local5617_0), AccountEntry.Nested0488.CRACKED, "—", "Player", System.currentTimeMillis(), 0L, false
      );
   }

   public ArrayList<String[]> method3442() {
      return this.field12119;
   }

   public String method3443() {
      return this.field12114;
   }

   public AccountEntry.Nested0488 method3444() {
      return this.field12113;
   }

   public String method3445() {
      return field12109.format(Instant.ofEpochMilli(this.field12116).atZone(ZoneId.systemDefault()));
   }

   public long method3446() {
      return this.field12116;
   }

   public void method3447(boolean local5623_1) {
      this.field12118 = local5623_1;
   }

   public void method3448(String local5624_1) {
      this.field12115 = local5624_1;
   }

   public static UUID method3449(String local5625_0) {
      return UUID.nameUUIDFromBytes(("OfflinePlayer:" + local5625_0).getBytes(StandardCharsets.UTF_8));
   }

   public AccountEntry(
      String local5626_1,
      UUID uUID,
      AccountEntry.Nested0488 local5626_3,
      String local5626_4,
      String local5626_5,
      long local5626_6,
      long local5626_8,
      boolean local5626_10
   ) {
      this.field12111 = local5626_1;
      this.field12112 = uUID;
      this.field12113 = local5626_3;
      this.field12114 = local5626_4;
      this.field12115 = local5626_5;
      this.field12116 = local5626_6;
      this.field12117 = local5626_8;
      this.field12118 = local5626_10;
   }

   public UUID method3450() {
      return this.field12112;
   }

   public void method3451(UUID uUID) {
      this.field12112 = uUID;
   }

   public void method3452(String local5630_1) {
      if (local5630_1 != null && !local5630_1.isBlank()) {
         this.field12119.add(0, new String[]{local5630_1.trim(), field12110.format(Instant.now().atZone(ZoneId.systemDefault()))});

         while (this.field12119.size() > 50) {
            this.field12119.remove(this.field12119.size() - 1);
         }
      }
   }

   public long method3453() {
      return this.field12117;
   }

   public void method3454(String local5632_1) {
      this.field12114 = local5632_1;
   }

   public String method3455() {
      return this.field12111;
   }

   public boolean method3456() {
      return this.field12118;
   }

   public void method3457(String local5635_1) {
      this.field12111 = local5635_1;
   }

   public String method3458() {
      return this.field12115;
   }

   public enum Nested0488 {
      CRACKED,
      MICROSOFT;
   }
}
