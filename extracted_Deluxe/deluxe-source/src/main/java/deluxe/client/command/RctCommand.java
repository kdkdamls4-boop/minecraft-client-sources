package deluxe.client.command;

import deluxe.mixin.BossBarHudMixin;
import deluxe.client.util.ClientChat;
import deluxe.client.event.EventBus;
import deluxe.client.event.EventListener;
import deluxe.client.MinecraftAccess;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.util.InventoryUtils;
import deluxe.client.util.Stopwatch;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

public class RctCommand implements ClientCommand, MinecraftAccess {
   private static final Pattern field01344 = Pattern.compile("(?:#|№)\\s*(\\d+)(?!\\d)");
   private final Stopwatch stopwatch = new Stopwatch();
   private boolean field01346;
   private boolean field01347;
   private boolean field01348;
   private int field01349;
   private long field01350;
   private final EventListener<PacketEvent> field01351 = packetEvent -> {
      if (this.field01349 != 0 && packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessageS2CPacket && packetEvent.isIncoming()) {
         String local1024_3 = gameMessageS2CPacket.content().getString().toLowerCase();
         if (!local1024_3.contains("хаб") && local1024_3.contains("не удалось")) {
            ClientChat.method2267("&7На эту анархию нельзя зайти");
            this.method0769();
            EventBus.getInstance().unsubscribe(this);
         }
      }
   };
   private final EventListener<PlayerTickEvent> field01352 = playerTickEvent -> {
      if (this.field01350 == 0L) {
         if (this.field01349 != 0) {
            if (!this.method0759().equals("HolyWorld")) {
               this.method0769();
               EventBus.getInstance().unsubscribe(this);
            } else if (!this.field01346 && this.method0764("HolyWorld") == this.field01349) {
               this.method0769();
               EventBus.getInstance().unsubscribe(this);
            } else if (this.field01346) {
               if (this.method0764("HolyWorld") == -1) {
                  this.field01346 = false;
               } else {
                  mc.player.networkHandler.sendChatCommand("hub");
               }
            } else if (mc.currentScreen instanceof GenericContainerScreen genericContainerScreen
               && genericContainerScreen.getTitle().getString().equals("Выбор Лайт анархии:")) {
               if (genericContainerScreen.getScreenHandler().getInventory().size() < 10) {
                  if (!this.field01347) {
                     InventoryUtils.method0547(method0765(this.field01349), 0, SlotActionType.PICKUP, false);
                     this.field01347 = true;
                  }
               } else {
                  if (!this.field01348) {
                     int local1022_3 = method0770(genericContainerScreen, this.field01349);
                     if (local1022_3 < 0) {
                        local1022_3 = method0768(this.field01349);
                     }

                     InventoryUtils.method0547(local1022_3, 0, SlotActionType.PICKUP, false);
                     this.field01348 = true;
                  }
               }
            } else {
               if (this.stopwatch.hasExceededMillis(500L)) {
                  this.stopwatch.reset();
                  mc.player.networkHandler.sendChatCommand("lite");
               }
            }
         }
      } else if (mc.player != null && mc.world != null && System.currentTimeMillis() >= this.field01350) {
         int local1022_2 = this.method0764(this.method0759());
         if (local1022_2 < 1 || local1022_2 > 63) {
            local1022_2 = Math.abs(this.field01349);
         }

         mc.player.networkHandler.sendChatCommand("an" + local1022_2);
         this.method0769();
         EventBus.getInstance().unsubscribe(this);
      }
   };

   private String method0759() {
      if (mc.player != null
         && mc.world != null
         && mc.getNetworkHandler() != null
         && mc.getNetworkHandler().getServerInfo() != null
         && mc.getNetworkHandler().getBrand() != null) {
         String local1019_1 = mc.getNetworkHandler().getServerInfo().address.toLowerCase(Locale.ROOT);
         String local1019_2 = mc.getNetworkHandler().getBrand().toLowerCase(Locale.ROOT);
         if (local1019_2.contains("botfilter")) {
            return "FunTime";
         } else if (local1019_1.contains("funtime")
            || local1019_1.contains("skytime")
            || local1019_1.contains("space-times")
            || local1019_1.contains("funsky")
            || local1019_1.contains("spookytime")) {
            return "CopyTime";
         } else if (local1019_2.contains("holyworld")
            || local1019_2.contains("leaf")
            || local1019_2.contains("vk.com/idwok")
            || local1019_2.contains("(proxy ")) {
            return "HolyWorld";
         } else {
            return local1019_1.contains("reallyworld") ? "ReallyWorld" : "Vanilla";
         }
      } else {
         return "Vanilla";
      }
   }

   @Override
   public String getName() {
      return "rct";
   }

   private static int method0761(ItemStack itemStack) {
      int local1023_1 = method0767(itemStack.getName().getString());
      if (local1023_1 > 0) {
         return local1023_1;
      }

      LoreComponent loreComponent = itemStack.get(DataComponentTypes.LORE);
      if (loreComponent == null) {
         return -1;
      }

      for (Text text : loreComponent.lines()) {
         local1023_1 = method0767(text.getString());
         if (local1023_1 > 0) {
            return local1023_1;
         }
      }

      return -1;
   }

   private static boolean method0763(String local1025_0) {
      return local1025_0.equals("CopyTime") || local1025_0.equals("FunTime");
   }

   private int method0764(String local1026_1) {
      Scoreboard scoreboard = mc.world.getScoreboard();
      ScoreboardObjective objectiveForSlot = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
      switch (local1026_1) {
         case "FunTime":
         case "CopyTime":
            if (objectiveForSlot != null) {
               String[] stringArray = objectiveForSlot.getDisplayName().getString().split("-");
               if (stringArray.length > 1) {
                  return Integer.parseInt(stringArray[1]);
               }
            }
            break;
         case "HolyWorld":
            if (objectiveForSlot == null) {
               return -1;
            }

            for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objectiveForSlot)) {
               String string = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
               int local1026_9 = string.indexOf(35);
               int local1026_10 = string.indexOf(" -◆-", local1026_9 + 1);
               if (local1026_9 != -1 && local1026_10 > local1026_9 + 1) {
                  return Integer.parseInt(string.substring(local1026_9 + 1, local1026_10));
               }
            }
      }

      return -1;
   }

   @Override
   public String getDescription() {
      return "Переподключение к текущей анархии (HolyWorld / CopyTime)";
   }

   private static int method0765(int local1028_0) {
      return local1028_0 < 15 ? 0 : (local1028_0 < 33 ? 1 : (local1028_0 < 48 ? 2 : 3));
   }

   @Override
   public void execute(String[] stringArray) {
      String local1029_2 = this.method0759();
      if (!local1029_2.equals("HolyWorld") && !method0763(local1029_2)) {
         ClientChat.method2267("&7Не работает на этом сервере");
      } else if (local1029_2.equals("HolyWorld")
         && ((BossBarHudMixin)mc.inGameHud.getBossBarHud())
            .getEvents()
            .values()
            .stream()
            .map(clientBossBar -> clientBossBar.getName().getString().toLowerCase())
            .anyMatch(local1030_0 -> local1030_0.contains("pvp") || local1030_0.contains("пвп"))) {
         ClientChat.method2267("&7Вы в режиме PvP");
      } else {
         int local1029_3 = this.method0764(local1029_2);
         if (local1029_3 > 0 && (local1029_3 < 64 || method0763(local1029_2))) {
            EventBus.getInstance().subscribe(this);
            this.field01347 = false;
            this.field01348 = false;
            this.field01349 = local1029_3;
            if (method0763(local1029_2)) {
               this.field01346 = false;
               this.field01350 = System.currentTimeMillis() + 900L;
               mc.player.networkHandler.sendChatCommand("hub");
            } else {
               this.field01346 = true;
               this.field01350 = 0L;
            }
         } else {
            ClientChat.method2267("&7Не верный лайт");
         }
      }
   }

   private static int method0767(String local1031_0) {
      if (local1031_0 != null && !local1031_0.isEmpty()) {
         Matcher matcher = field01344.matcher(local1031_0);
         return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
      } else {
         return -1;
      }
   }

   private static int method0768(int local1033_0) {
      int local1033_1 = local1033_0 < 15 ? 1 : (local1033_0 < 33 ? 15 : (local1033_0 < 48 ? 33 : 48));
      return 18 + local1033_0 - local1033_1;
   }

   private void method0769() {
      this.field01349 = 0;
      this.field01346 = false;
      this.field01350 = 0L;
      this.field01347 = false;
      this.field01348 = false;
   }

   private static int method0770(GenericContainerScreen genericContainerScreen, int local1035_1) {
      for (Slot slot : genericContainerScreen.getScreenHandler().slots) {
         if (slot.id >= 18 && !slot.getStack().isEmpty() && method0761(slot.getStack()) == local1035_1) {
            return slot.id;
         }
      }

      return -1;
   }
}
