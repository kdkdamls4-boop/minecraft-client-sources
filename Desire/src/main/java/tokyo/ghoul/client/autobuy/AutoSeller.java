package tokyo.ghoul.client.autobuy;

import tokyo.ghoul.api.system.interfaces.QuickImports;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ContainerInput;

public class AutoSeller implements QuickImports {
   private static final int FULL_RETRY_DELAY = 600;
   private AutoSeller.State state = AutoSeller.State.IDLE;
   private int tickDelay = 0;
   private int waitTimeout = 0;
   private int reopenAttempts = 0;
   private int fullAuctionCooldown = 0;
   private String currentSellingItemId = null;
   private ItemEntry currentSellingEntry = null;
   private int currentSellingCount = 1;
   private boolean isRelist = false;
   private final Set<String> soldBlacklist = new HashSet<>();
   private volatile boolean triggered = false;
   private volatile boolean relistTriggered = false;
   private volatile boolean auctionFullWaiting = false;
   private volatile boolean listedSuccess = false;
   private volatile boolean auctionFullReceived = false;
   private volatile boolean cooldownReceived = false;
   public volatile long lastListedPrice = 0L;

   public void stop() {
      this.state = AutoSeller.State.IDLE;
      this.tickDelay = 0;
      this.waitTimeout = 0;
      this.reopenAttempts = 0;
      this.fullAuctionCooldown = 0;
      this.currentSellingItemId = null;
      this.currentSellingEntry = null;
      this.currentSellingCount = 1;
      this.isRelist = false;
      this.soldBlacklist.clear();
      this.triggered = false;
      this.relistTriggered = false;
      this.auctionFullWaiting = false;
      this.listedSuccess = false;
      this.auctionFullReceived = false;
      this.cooldownReceived = false;
   }

   public void trigger() {
      this.triggered = true;
      this.relistTriggered = false;
   }

   public void triggerRelist() {
      this.triggered = true;
      this.relistTriggered = true;
   }

   public boolean isActive() {
      return this.state != AutoSeller.State.IDLE;
   }

   public void onListed() {
      if (this.state == AutoSeller.State.WAIT) {
         this.listedSuccess = true;
      }
   }

   public void onAuctionFull() {
      if (this.state == AutoSeller.State.SELL_AUTO || this.state == AutoSeller.State.WAIT) {
         this.auctionFullReceived = true;
      }
   }

   public void onSold() {
      if (this.auctionFullWaiting) {
         this.auctionFullWaiting = false;
         this.triggered = true;
      }
   }

   public void onCommandCooldown() {
      if (this.state == AutoSeller.State.REOPEN || this.state == AutoSeller.State.WAIT_REOPEN) {
         this.cooldownReceived = true;
      }
   }

   public synchronized void tick() {
      if (mc.player != null) {
         if (this.tickDelay > 0) {
            this.tickDelay--;
         } else {
            switch (this.state) {
               case IDLE:
                  if (this.auctionFullWaiting) {
                     if (--this.fullAuctionCooldown > 0) {
                        return;
                     }

                     this.auctionFullWaiting = false;
                     this.triggered = true;
                     this.relistTriggered = this.isRelist;
                  }

                  if (!this.triggered) {
                     return;
                  }

                  this.triggered = false;
                  this.isRelist = this.relistTriggered;
                  this.relistTriggered = false;
                  this.soldBlacklist.clear();
                  this.state = AutoSeller.State.CLOSE;
                  break;
               case CLOSE:
                  if (mc.gui.screen() != null) {
                     mc.player.closeContainer();
                     this.tickDelay = 6;
                  } else {
                     this.state = AutoSeller.State.SWEEP;
                  }
                  break;
               case SWEEP:
                  int slot = this.findMatchingInventorySlot();
                  if (slot < 0) {
                     this.reopenAttempts = 0;
                     this.state = AutoSeller.State.REOPEN;
                  } else {
                     ItemStack stack = mc.player.getInventory().getItem(slot);
                     this.currentSellingItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                     this.currentSellingEntry = this.findMatchingEntry(stack);
                     this.currentSellingCount = Math.max(1, stack.getCount());
                     this.selectHotbarSlot(slot);
                     this.state = AutoSeller.State.SELL_AUTO;
                     this.tickDelay = 8;
                  }
                  break;
               case SELL_AUTO:
                  this.listedSuccess = false;
                  this.auctionFullReceived = false;
                  mc.player.connection.sendCommand(this.buildSellCommand());
                  this.state = AutoSeller.State.WAIT;
                  this.waitTimeout = 100;
                  break;
               case WAIT:
                  if (this.auctionFullReceived) {
                     this.auctionFullReceived = false;
                     this.auctionFullWaiting = true;
                     this.fullAuctionCooldown = 600;
                     this.reopenAttempts = 0;
                     this.state = AutoSeller.State.REOPEN;
                  } else if (this.listedSuccess) {
                     this.listedSuccess = false;
                     this.state = AutoSeller.State.SWEEP;
                     this.tickDelay = 6;
                  } else {
                     this.waitTimeout--;
                     if (this.waitTimeout <= 0) {
                        if (this.currentSellingItemId != null) {
                           this.soldBlacklist.add(this.currentSellingItemId);
                        }

                        this.state = AutoSeller.State.SWEEP;
                        this.tickDelay = 4;
                     }
                  }
                  break;
               case REOPEN:
                  this.cooldownReceived = false;
                  mc.player.connection.sendCommand("ah");
                  this.reopenAttempts++;
                  this.state = AutoSeller.State.WAIT_REOPEN;
                  this.waitTimeout = 40;
                  break;
               case WAIT_REOPEN:
                  if (this.isAuctionScreen()) {
                     this.reopenAttempts = 0;
                     this.state = AutoSeller.State.IDLE;
                     this.tickDelay = 10;
                  } else if (this.cooldownReceived) {
                     this.cooldownReceived = false;
                     this.state = AutoSeller.State.REOPEN;
                     this.tickDelay = 25;
                  } else {
                     this.waitTimeout--;
                     if (this.waitTimeout <= 0) {
                        if (this.reopenAttempts < 6) {
                           this.state = AutoSeller.State.REOPEN;
                           this.tickDelay = 10;
                        } else {
                           this.reopenAttempts = 0;
                           this.state = AutoSeller.State.IDLE;
                        }
                     }
                  }
                  break;
               default:
                  this.state = AutoSeller.State.IDLE;
            }
         }
      }
   }

   private int findMatchingInventorySlot() {
      if (mc.player == null) {
         return -1;
      }

      Inventory inv = mc.player.getInventory();

      for (int i = 0; i < 36; i++) {
         ItemStack stack = inv.getItem(i);
         if (!stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!this.soldBlacklist.contains(itemId) && this.matchesAnyEntry(stack)) {
               return i;
            }
         }
      }

      return -1;
   }

   private boolean matchesAnyEntry(ItemStack stack) {
      return this.findMatchingEntry(stack) != null;
   }

   private ItemEntry findMatchingEntry(ItemStack stack) {
      Lot lot = PriceParser.parseForMatch(stack);
      if (lot == null) {
         return null;
      }

      for (ItemEntry e : AutoBuyConfig.cfg().entries) {
         if (ItemMatcher.matches(e, lot)) {
            return e;
         }
      }

      return null;
   }

   private String buildSellCommand() {
      long market = this.currentSellingEntry != null ? this.currentSellingEntry.marketPrice : 0L;
      if (market <= 0L) {
         return "ah sell auto";
      }

      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      int discount = this.isRelist ? cfg.relistDiscountPercent : cfg.sellDiscountPercent;
      discount = Math.max(0, Math.min(99, discount));
      long totalPrice = market * Math.max(1, this.currentSellingCount) * (100L - discount) / 100L;
      if (totalPrice < 1L) {
         totalPrice = 1L;
      }

      this.lastListedPrice = totalPrice;
      return "ah sell " + totalPrice;
   }

   private boolean isAuctionScreen() {
      return mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs);
   }

   private void selectHotbarSlot(int slot) {
      if (slot < 9) {
         this.setHeldSlot(slot);
      } else {
         int selected = mc.player.getInventory().getSelectedSlot();
         if (mc.gameMode != null) {
            mc.gameMode.handleContainerInput(0, slot, selected, ContainerInput.SWAP, mc.player);
         }

         this.setHeldSlot(selected);
      }
   }

   private void setHeldSlot(int slot) {
      mc.player.getInventory().setSelectedSlot(slot);
      mc.player.connection.send(new ServerboundSetCarriedItemPacket(slot));
   }

   private enum State {
      IDLE,
      CLOSE,
      SWEEP,
      SELL_AUTO,
      WAIT,
      REOPEN,
      WAIT_REOPEN;
   }
}
