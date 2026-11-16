package tokyo.ghoul.client.autobuy;

import tokyo.ghoul.api.system.interfaces.QuickImports;
import java.util.Locale;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;

public class RelistManager implements QuickImports {
   private static final String MY_LISTINGS_NEEDLE = "товары на продаже";
   private static final String LISTINGS_SCREEN_TITLE = "продаж";
   private static final int RETRY_INTERVAL = 10;
   private static final int MAX_COLLECT_LIMIT = 300;
   private RelistManager.State state = RelistManager.State.IDLE;
   private int tickDelay = 0;
   private int waitTimeout = 0;
   private int collectedCount = 0;
   private int emptyChecksCount = 0;
   private volatile boolean triggered = false;

   public void start() {
      this.triggered = true;
   }

   public boolean isActive() {
      return this.state != RelistManager.State.IDLE;
   }

   public String statusLine() {
      return this.state == RelistManager.State.IDLE ? "Релист: ожидание" : "Релист: снято " + this.collectedCount;
   }

   public synchronized void tick() {
      if (mc.player == null || mc.gameMode == null) {
         this.state = RelistManager.State.IDLE;
      } else if (this.tickDelay > 0) {
         this.tickDelay--;
      } else {
         switch (this.state) {
            case IDLE:
               if (!this.triggered) {
                  return;
               }

               this.triggered = false;
               this.collectedCount = 0;
               this.emptyChecksCount = 0;
               this.state = this.isAuctionScreen() ? RelistManager.State.OPEN_LISTINGS : RelistManager.State.OPEN_AUCTION;
               break;
            case OPEN_AUCTION:
               if (mc.gui.screen() != null) {
                  mc.player.closeContainer();
                  this.tickDelay = 6;
               } else {
                  mc.player.connection.sendCommand("ah");
                  this.state = RelistManager.State.WAIT_AUCTION;
                  this.waitTimeout = 60;
               }
               break;
            case WAIT_AUCTION:
               if (this.isAuctionScreen()) {
                  this.state = RelistManager.State.OPEN_LISTINGS;
                  this.tickDelay = 4;
               } else if (--this.waitTimeout <= 0) {
                  this.finishProcess();
               }
               break;
            case OPEN_LISTINGS:
               if (!this.isAuctionScreen()) {
                  this.finishProcess();
                  return;
               }

               ChestMenu screenHandler = ((ContainerScreen)mc.gui.screen()).getMenu();
               int listingsSlot = this.findMyListingsSlot(screenHandler);
               if (listingsSlot < 0) {
                  this.sendMessage("§e[Релист] Эндер-сундук «Товары на продаже» не найден");
                  this.finishProcess();
                  return;
               }

               this.clickSlot(screenHandler.containerId, listingsSlot, 0);
               this.state = RelistManager.State.WAIT_LISTINGS;
               this.waitTimeout = 60;
               break;
            case WAIT_LISTINGS:
               if (this.isListingsScreen()) {
                  this.state = RelistManager.State.COLLECT;
                  this.tickDelay = 6;
                  this.emptyChecksCount = 0;
               } else if (--this.waitTimeout <= 0) {
                  this.finishProcess();
               }
               break;
            case COLLECT:
               if (!this.isListingsScreen()) {
                  this.finishProcess();
                  return;
               }

               ChestMenu listingsHandler = ((ContainerScreen)mc.gui.screen()).getMenu();
               int lotSlot = this.findMyMatchingLotSlot(listingsHandler);
               if (lotSlot < 0) {
                  if (++this.emptyChecksCount >= 3) {
                     this.finishProcess();
                  } else {
                     this.tickDelay = 4;
                  }
               } else {
                  this.emptyChecksCount = 0;
                  this.clickSlot(listingsHandler.containerId, lotSlot, 1);
                  this.collectedCount++;
                  this.tickDelay = 10;
                  if (this.collectedCount >= MAX_COLLECT_LIMIT) {
                     this.finishProcess();
                  }
               }
               break;
            case FINISH:
               if (mc.gui.screen() != null) {
                  mc.player.closeContainer();
                  this.tickDelay = 6;
               } else {
                  if (this.collectedCount > 0) {
                     this.sendMessage("§a[Релист] Снято лотов: §f" + this.collectedCount + "§a — перевыставляю");
                     AutoBuyEngine.INSTANCE.seller.triggerRelist();
                  } else {
                     this.sendMessage("§7[Релист] Своих лотов из списка нет");
                  }

                  this.state = RelistManager.State.IDLE;
               }
               break;
            default:
               this.state = RelistManager.State.IDLE;
         }
      }
   }

   private void finishProcess() {
      this.state = RelistManager.State.FINISH;
      this.tickDelay = 4;
   }

   private boolean isAuctionScreen() {
      return mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs);
   }

   private boolean isListingsScreen() {
      if (mc.gui.screen() instanceof ContainerScreen gcs) {
         String title = gcs.getTitle().getString().toLowerCase(Locale.ROOT);
         return title.contains(LISTINGS_SCREEN_TITLE);
      } else {
         return false;
      }
   }

   private int findMyListingsSlot(ChestMenu handler) {
      int totalSlots = handler.getRowCount() * 9;

      for (int i = 0; i < totalSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (itemId.equals("minecraft:ender_chest") && this.stackContainsNeedle(stack, MY_LISTINGS_NEEDLE)) {
               return i;
            }
         }
      }

      for (int i = 0; i < totalSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty() && this.stackContainsNeedle(stack, MY_LISTINGS_NEEDLE)) {
            return i;
         }
      }

      return -1;
   }

   private int findMyMatchingLotSlot(ChestMenu handler) {
      int rows = handler.getRowCount();
      int lotSlots = (rows - 1) * 9;

      for (int i = 0; i < lotSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            Lot lot = PriceParser.parseForMatch(stack);
            if (lot != null) {
               for (ItemEntry e : AutoBuyConfig.cfg().entries) {
                  if (ItemMatcher.matches(e, lot)) {
                     return i;
                  }
               }
            }
         }
      }

      return -1;
   }

   private boolean stackContainsNeedle(ItemStack stack, String needle) {
      String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
      if (name.contains(needle)) {
         return true;
      }

      ItemLore lore = stack.get(DataComponents.LORE);
      if (lore != null) {
         for (Component line : lore.lines()) {
            if (line.getString().toLowerCase(Locale.ROOT).contains(needle)) {
               return true;
            }
         }
      }

      return false;
   }

   private void clickSlot(int syncId, int slot, int button) {
      if (mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleContainerInput(syncId, slot, button, ContainerInput.PICKUP, mc.player);
      }
   }

   private void sendMessage(String text) {
      if (mc.player != null) {
         mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text)));
      }
   }

   private enum State {
      IDLE,
      OPEN_AUCTION,
      WAIT_AUCTION,
      OPEN_LISTINGS,
      WAIT_LISTINGS,
      COLLECT,
      FINISH;
   }
}
