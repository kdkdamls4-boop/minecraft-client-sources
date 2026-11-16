package deluxe.client.storage;

import deluxe.client.module.misc.AutoRegisterModule;
import deluxe.client.event.EventListener;
import deluxe.mixin.MinecraftClientMixin5;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Formatting;

public final class AccountStore {
   private static final AccountStore accountStore = new AccountStore();
   private ArrayList<AccountEntry> field14247 = new ArrayList<>();
   private AccountEntry accountEntry;
   private long field14249;
   private Path field14250;
   private final EventListener<PacketEvent> field14251 = packetEvent -> {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null) {
         if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandExecutionC2SPacket) {
            String[] stringArray = commandExecutionC2SPacket.command().trim().split("\\s+");
            if (stringArray.length >= 2
               && (
                  stringArray[0].equalsIgnoreCase("reg")
                     || stringArray[0].equalsIgnoreCase("register")
                     || stringArray[0].equalsIgnoreCase("l")
                     || stringArray[0].equalsIgnoreCase("login")
               )) {
               minecraftClient.execute(() -> AutoRegisterModule.method2677().field09189.setValue(stringArray[1]));
            }
         }

         if (this.accountEntry != null) {
            AccountEntry currentAccountEntry = this.accountEntry;
            if (packetEvent.isOutgoing() && packetEvent.getPacket() instanceof ChatMessageC2SPacket chatMessageC2SPacket) {
               minecraftClient.execute(() -> currentAccountEntry.method3452(chatMessageC2SPacket.chatMessage()));
            } else if (packetEvent.isIncoming() && packetEvent.getPacket() instanceof GameJoinS2CPacket) {
               minecraftClient.execute(() -> {
                  if (minecraftClient.getCurrentServerEntry() != null) {
                     currentAccountEntry.method3454(method4154(minecraftClient.getCurrentServerEntry().address));
                     this.method4158();
                  }
               });
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field14252 = playerTickEvent -> {
      MinecraftClient minecraftClient2 = MinecraftClient.getInstance();
      if (minecraftClient2 != null && this.accountEntry != null && minecraftClient2.player != null && minecraftClient2.world != null) {
         Team team = minecraftClient2.world.getScoreboard().getScoreHolderTeam(minecraftClient2.player.getNameForScoreboard());
         String local6917_4 = team != null && team.getPrefix() != null
            ? Formatting.strip(team.getPrefix().getString()).replaceAll("[^\\p{L}\\p{N}]", "")
            : "";
         if (!local6917_4.isEmpty() && !local6917_4.equals(this.accountEntry.method3458())) {
            this.accountEntry.method3448(local6917_4);
            this.method4158();
         }
      }
   };

   private static AccountEntry method4132(String local6907_0) {
      if (local6907_0 != null && !local6907_0.isBlank()) {
         String[] stringArray = local6907_0.split("\t", -1);
         if (stringArray.length < 9) {
            return null;
         }

         try {
            return new AccountEntry(
               stringArray[0],
               UUID.fromString(stringArray[1]),
               "MICROSOFT".equals(stringArray[2]) ? AccountEntry.Nested0488.MICROSOFT : AccountEntry.Nested0488.CRACKED,
               stringArray[3],
               stringArray[4],
               Long.parseLong(stringArray[5]),
               Long.parseLong(stringArray[6]),
               Boolean.parseBoolean(stringArray[7])
            );
         } catch (RuntimeException exception) {
            return null;
         }
      } else {
         return null;
      }
   }

   public void method4133() {
      if (this.accountEntry != null && this.field14249 > 0L) {
         this.accountEntry.method3440(System.currentTimeMillis() - this.field14249);
      }

      this.field14249 = System.currentTimeMillis();
   }

   private static long method4134(int local6909_0, int local6909_1, int local6909_2) {
      return LocalDate.of(local6909_0, local6909_1, local6909_2).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
   }

   public long method4135(AccountEntry newAccountEntry) {
      return newAccountEntry.method3453()
         + (newAccountEntry == this.accountEntry && this.field14249 > 0L ? Math.max(0L, System.currentTimeMillis() - this.field14249) : 0L);
   }

   public void method4137(AccountEntry newAccountEntry, String local6913_2) {
      String local6913_3 = local6913_2 == null ? "" : local6913_2.trim();
      if (newAccountEntry != null && !local6913_3.isEmpty() && !local6913_3.equals(newAccountEntry.method3455())) {
         for (AccountEntry currentAccountEntry : this.field14247) {
            if (currentAccountEntry != newAccountEntry && currentAccountEntry.method3455().equalsIgnoreCase(local6913_3)) {
               return;
            }
         }

         newAccountEntry.method3457(local6913_3);
         if (newAccountEntry.method3444() == AccountEntry.Nested0488.CRACKED) {
            newAccountEntry.method3451(AccountEntry.method3449(local6913_3));
         }

         if (newAccountEntry == this.accountEntry) {
            this.method4156(newAccountEntry);
         }

         this.method4158();
      }
   }

   private void method4138() {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      Session session = minecraftClient != null ? minecraftClient.getSession() : null;
      if (session != null) {
         for (AccountEntry currentAccountEntry : this.field14247) {
            if (currentAccountEntry.method3455().equalsIgnoreCase(session.getUsername())) {
               this.accountEntry = currentAccountEntry;
               this.field14249 = System.currentTimeMillis();
               return;
            }
         }
      }
   }

   public void method4140(AccountEntry newAccountEntry) {
      newAccountEntry.method3447(!newAccountEntry.method3456());
      this.method4158();
   }

   public void method4142(AccountEntry newAccountEntry) {
      if (newAccountEntry == this.accountEntry) {
         this.method4133();
         this.accountEntry = null;
      }

      this.field14247.remove(newAccountEntry);
      this.method4158();
   }

   private static long method4143(long local6919_0, long local6919_2, long local6919_4) {
      return ((local6919_0 * 24L + local6919_2) * 60L + local6919_4) * 60000L;
   }

   public AccountEntry method4145(String local6921_1) {
      String local6921_2 = local6921_1.trim();
      if (local6921_2.isEmpty()) {
         return null;
      }

      for (AccountEntry currentAccountEntry : this.field14247) {
         if (currentAccountEntry.method3455().equalsIgnoreCase(local6921_2)) {
            return currentAccountEntry;
         }
      }

      AccountEntry currentAccountEntry2 = AccountEntry.method3441(local6921_2);
      this.field14247.add(currentAccountEntry2);
      this.method4158();
      return currentAccountEntry2;
   }

   public ArrayList<AccountEntry> method4146() {
      return this.field14247;
   }

   private void method4147() {
   }

   public AccountEntry getAccountEntry() {
      return this.accountEntry;
   }

   private void method4149() {
      this.field14247.clear();

      try {
         if (Files.exists(this.field14250)) {
            for (String local6925_2 : Files.readAllLines(this.field14250, StandardCharsets.UTF_8)) {
               AccountEntry currentAccountEntry = method4132(local6925_2);
               if (currentAccountEntry != null) {
                  this.field14247.add(currentAccountEntry);
               }
            }
         }
      } catch (IOException exception) {
      }

      if (this.field14247.isEmpty()) {
         this.method4147();
         this.method4158();
      }
   }

   public boolean method4150(AccountEntry newAccountEntry) {
      return newAccountEntry != null && newAccountEntry == this.accountEntry;
   }

   private static String method4151(String local6927_0) {
      return local6927_0 == null ? "" : local6927_0.replace('\t', ' ').replace('\n', ' ');
   }

   private void method4152(String local6928_1, String local6928_2, String local6928_3, long local6928_4, long local6928_6, boolean local6928_8) {
      this.field14247
         .add(
            new AccountEntry(
               local6928_1,
               AccountEntry.method3449(local6928_1),
               AccountEntry.Nested0488.CRACKED,
               local6928_3,
               local6928_2,
               local6928_4,
               local6928_6,
               local6928_8
            )
         );
   }

   public static AccountStore method4153() {
      return accountStore;
   }

   private AccountStore() {
      this.field14250 = FabricLoader.getInstance().getConfigDir().resolve("deluxe").resolve("altmanager.txt");
      this.method4149();
      this.method4138();
   }

   public static String method4154(String local6931_0) {
      if (local6931_0 != null && !local6931_0.isBlank()) {
         String local6931_1 = local6931_0.trim().toLowerCase(Locale.ROOT).split(":", 2)[0];
         String[] stringArray = local6931_1.split("\\.");
         String local6931_3 = stringArray.length >= 2 ? stringArray[stringArray.length - 2] : stringArray[0];
         return local6931_3.isEmpty() ? "—" : Character.toUpperCase(local6931_3.charAt(0)) + local6931_3.substring(1);
      } else {
         return "—";
      }
   }

   public ArrayList<AccountEntry> method4155(String local6932_1) {
      String local6932_2 = local6932_1 == null ? "" : local6932_1.trim().toLowerCase(Locale.ROOT);
      ArrayList<AccountEntry> local6932_3 = new ArrayList<>();

      for (AccountEntry currentAccountEntry : this.field14247) {
         if (local6932_2.isEmpty() || currentAccountEntry.method3455().toLowerCase(Locale.ROOT).contains(local6932_2)) {
            local6932_3.add(currentAccountEntry);
         }
      }

      local6932_3.sort(
         (newAccountEntry, newAccountEntry2) -> newAccountEntry.method3456() != newAccountEntry2.method3456()
            ? (newAccountEntry.method3456() ? -1 : 1)
            : Long.compare(newAccountEntry2.method3446(), newAccountEntry.method3446())
      );
      return local6932_3;
   }

   public void method4156(AccountEntry newAccountEntry) {
      MinecraftClient minecraftClient = MinecraftClient.getInstance();
      if (minecraftClient != null && newAccountEntry != null) {
         ((MinecraftClientMixin5)minecraftClient).setUser(new Session(newAccountEntry.method3455(), newAccountEntry.method3450(), "", Optional.empty(), Optional.empty()));
         this.method4133();
         this.accountEntry = newAccountEntry;
         this.field14249 = System.currentTimeMillis();
         this.method4158();
      }
   }

   public void method4158() {
      try {
         Files.createDirectories(this.field14250.getParent());
         StringBuilder stringBuilder = new StringBuilder();

         for (AccountEntry currentAccountEntry : this.field14247) {
            stringBuilder.append(method4151(currentAccountEntry.method3455()))
               .append('\t')
               .append(currentAccountEntry.method3450())
               .append('\t')
               .append(currentAccountEntry.method3444())
               .append('\t')
               .append(method4151(currentAccountEntry.method3443()))
               .append('\t')
               .append(method4151(currentAccountEntry.method3458()))
               .append('\t')
               .append(currentAccountEntry.method3446())
               .append('\t')
               .append(currentAccountEntry == this.accountEntry ? this.method4135(currentAccountEntry) : currentAccountEntry.method3453())
               .append('\t')
               .append(currentAccountEntry.method3456())
               .append('\t')
               .append('1')
               .append('\n');
         }

         Files.writeString(this.field14250, stringBuilder.toString(), StandardCharsets.UTF_8);
      } catch (IOException exception) {
      }
   }

   public int method4159(AccountEntry.Nested0488 local6936_1) {
      int local6936_2 = 0;

      for (AccountEntry currentAccountEntry : this.field14247) {
         if (currentAccountEntry.method3444() == local6936_1) {
            local6936_2++;
         }
      }

      return local6936_2;
   }
}
