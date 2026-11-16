package tokyo.ghoul.client.autobuy;

import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.misc.FlowAutoBuyModule;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;

public class PriceAutoParser implements QuickImports {
   private static final int MAX_PAGE_SCANS = 25;
   private static final int SORT_TICK_DELAY = 6;
   private static final int DEFAULT_ADVANCE_DELAY = 4;
   private static final int DEFAULT_CLOSE_DELAY = 2;
   private static final int SCAN_TIMEOUT = 14;
   private static final int SCREEN_TIMEOUT = 60;
   private PriceAutoParser.State state = PriceAutoParser.State.IDLE;
   private List<ItemEntry> parseEntries = new ArrayList<>();
   private int currentIndex = 0;
   private int tickDelay = 0;
   private long minFoundPrice = -1L;
   private int pageScanCount = 0;
   private long lastScreenHash = 0L;
   private int timeoutTicks = 0;
   private int sortClicksRemaining = 0;
   private String currentItemLabel = "";
   private long prevHash = 0L;
   private int stableHashCount = 0;
   private AuctionSortMode targetSortMode = null;
   private boolean wasAutoBuyEnabled = false;

   public synchronized void start() {
      if (this.state != PriceAutoParser.State.IDLE) {
         this.stop();
      } else if (mc.player != null) {
         this.parseEntries = new ArrayList<>();

         for (ItemEntry e : AutoBuyConfig.cfg().entries) {
            if (e != null && e.enabled) {
               this.parseEntries.add(e);
            }
         }

         if (this.parseEntries.isEmpty()) {
            this.sendMessage("§e[AutoBuy] Нет включённых предметов для парсинга");
         } else {
            this.currentIndex = 0;
            this.tickDelay = 0;
            this.timeoutTicks = 0;
            this.sortClicksRemaining = 0;
            this.wasAutoBuyEnabled = AutoBuyConfig.cfg().enabled;
            AutoBuyConfig.cfg().enabled = false;
            FlowAutoBuyModule.getInstance().setEnabled(false);

            this.currentItemLabel = "сортировка";
            this.state = PriceAutoParser.State.PRE_OPEN;
            this.sendMessage("§b[AutoBuy] Запуск парсинга цен: " + this.parseEntries.size() + " предмет(ов)");
         }
      }
   }

   public synchronized void stop() {
      this.state = PriceAutoParser.State.IDLE;
      this.parseEntries.clear();
      this.currentIndex = 0;
      this.currentItemLabel = "";
      this.targetSortMode = null;
      this.minFoundPrice = -1L;
   }

   public boolean isActive() {
      return this.state != PriceAutoParser.State.IDLE;
   }

   public String statusLine() {
      if (this.state == PriceAutoParser.State.IDLE) {
         return "Парсер цен: ожидание";
      }

      int current = Math.min(this.currentIndex + 1, Math.max(1, this.parseEntries.size()));
      return "Парсер: " + current + "/" + this.parseEntries.size() + " — " + this.currentItemLabel;
   }

   public synchronized void tick() {
      if (this.state != PriceAutoParser.State.IDLE) {
         if (mc.player == null) {
            this.stop();
         } else if (this.tickDelay > 0) {
            this.tickDelay--;
         } else {
            switch (this.state) {
               case PRE_OPEN:
                  this.handlePreOpen();
                  break;
               case PRE_WAIT:
                  this.handlePreWait();
                  break;
               case PRE_SORT:
                  this.handlePreSort();
                  break;
               case NEXT:
                  this.handleNext();
                  break;
               case WAIT_SCREEN:
                  this.handleWaitScreen();
                  break;
               case SCAN:
                  this.handleScan();
                  break;
               case WAIT_PAGE:
                  this.handleWaitPage();
                  break;
               case ADVANCE:
                  this.handleAdvance();
                  break;
               case POST_OPEN:
                  this.handlePostOpen();
                  break;
               case POST_WAIT:
                  this.handlePostWait();
                  break;
               case POST_SORT:
                  this.handlePostSort();
                  break;
            }
         }
      }
   }

   private void handlePreOpen() {
      if (this.isAuctionScreen()) {
         this.prepareSort(AuctionSortMode.CHEAPEST_PER_UNIT, 4, PriceAutoParser.State.PRE_SORT);
      } else if (mc.gui.screen() != null) {
         mc.player.closeContainer();
         this.tickDelay = 6;
      } else {
         mc.player.connection.sendCommand("ah");
         this.state = PriceAutoParser.State.PRE_WAIT;
         this.timeoutTicks = 60;
      }
   }

   private void handlePreWait() {
      if (this.isAuctionScreen()) {
         this.prepareSort(AuctionSortMode.CHEAPEST_PER_UNIT, 4, PriceAutoParser.State.PRE_SORT);
      } else if (--this.timeoutTicks <= 0) {
         this.state = PriceAutoParser.State.NEXT;
         this.tickDelay = 8;
      }
   }

   private void handlePreSort() {
      if (!this.isAuctionScreen()) {
         this.state = PriceAutoParser.State.NEXT;
         this.tickDelay = 8;
      } else {
         ChestMenu handler = this.getScreenHandler();
         int slot = AuctionSortMode.findButton(handler);
         if (slot < 0) {
            this.sendMessage("§e[AutoBuy] Воронка «Сортировка» не найдена");
            this.state = PriceAutoParser.State.NEXT;
            this.tickDelay = 8;
         } else {
            AuctionSortMode current = AuctionSortMode.current(handler.getSlot(slot).getItem());
            if (this.targetSortMode != null && current == this.targetSortMode) {
               this.state = PriceAutoParser.State.NEXT;
               this.tickDelay = 8;
            } else if (this.sortClicksRemaining <= 0) {
               this.state = PriceAutoParser.State.NEXT;
               this.tickDelay = 8;
            } else {
               this.clickSlot(handler.containerId, slot);
               this.sortClicksRemaining--;
               this.tickDelay = 6;
            }
         }
      }
   }

   private void handlePostOpen() {
      this.currentItemLabel = "сортировка";
      if (this.isAuctionScreen()) {
         this.prepareSort(AuctionSortMode.NEWEST, 2, PriceAutoParser.State.POST_SORT);
      } else if (mc.gui.screen() != null) {
         mc.player.closeContainer();
         this.tickDelay = 6;
      } else {
         mc.player.connection.sendCommand("ah");
         this.state = PriceAutoParser.State.POST_WAIT;
         this.timeoutTicks = 60;
      }
   }

   private void handlePostWait() {
      if (this.isAuctionScreen()) {
         this.prepareSort(AuctionSortMode.NEWEST, 2, PriceAutoParser.State.POST_SORT);
      } else if (--this.timeoutTicks <= 0) {
         this.finish();
      }
   }

   private void handlePostSort() {
      if (!this.isAuctionScreen()) {
         this.finish();
      } else {
         ChestMenu handler = this.getScreenHandler();
         int slot = AuctionSortMode.findButton(handler);
         if (slot < 0) {
            this.finish();
         } else {
            AuctionSortMode current = AuctionSortMode.current(handler.getSlot(slot).getItem());
            if (this.targetSortMode != null && current == this.targetSortMode) {
               this.finish();
            } else if (this.sortClicksRemaining <= 0) {
               this.finish();
            } else {
               this.clickSlot(handler.containerId, slot);
               this.sortClicksRemaining--;
               this.tickDelay = 6;
            }
         }
      }
   }

   private void finish() {
      this.sendMessage("§a[AutoBuy] Парсинг цен HolyWorld окончен! Обновлено: " + this.parseEntries.size() + " предм.");
      if (mc.gui.screen() != null && mc.player != null) {
         mc.player.closeContainer();
      }
      if (this.wasAutoBuyEnabled) {
         AutoBuyConfig.cfg().enabled = true;
         FlowAutoBuyModule.getInstance().setEnabled(true);
      }
      AutoBuyConfig.save();
      this.stop();
   }

   private void handleNext() {
      if (this.currentIndex >= this.parseEntries.size()) {
         this.state = PriceAutoParser.State.POST_OPEN;
         this.timeoutTicks = 60;
         this.tickDelay = 4;
      } else {
         ItemEntry entry = this.parseEntries.get(this.currentIndex);
         String label = entry.displayName != null && !entry.displayName.isBlank() ? entry.displayName : entry.itemId;
         if (entry.loreKeyword != null && !entry.loreKeyword.isBlank()) {
            label = label + " [" + entry.loreKeyword + "]";
         }

         this.currentItemLabel = label;
         this.minFoundPrice = -1L;
         this.pageScanCount = 0;
         this.lastScreenHash = 0L;
         if (mc.gui.screen() != null) {
            mc.player.closeContainer();
            this.tickDelay = 4;
         } else {
            String query = entry.query().replaceAll("[«»\"']", "").trim();
            mc.player.connection.sendCommand("ah search " + query);
            this.state = PriceAutoParser.State.WAIT_SCREEN;
            this.tickDelay = 0;
            this.timeoutTicks = 60;
         }
      }
   }

   private void handleWaitScreen() {
      this.timeoutTicks--;
      if (this.timeoutTicks <= 0) {
         this.commitPriceAndAdvance();
      } else {
         if (mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs)) {
            this.startScan();
         }
      }
   }

   private void startScan() {
      this.state = PriceAutoParser.State.SCAN;
      this.timeoutTicks = 14;
      this.prevHash = 0L;
      this.stableHashCount = 0;
   }

   private void handleScan() {
      if (mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs)) {
         ChestMenu handler = gcs.getMenu();
         int rows = handler.getRowCount();
         int lotCount = (rows - 1) * 9;
         ItemEntry entry = this.parseEntries.get(this.currentIndex);
         int nonEmptyCount = 0;

         for (int i = 0; i < lotCount; i++) {
            ItemStack stack = handler.getSlot(i).getItem();
            if (!stack.isEmpty()) {
               nonEmptyCount++;
            }

            Lot lot = PriceParser.parse(i, stack);
            if (lot != null && !lot.barrier && ItemMatcher.matches(entry, lot) && (this.minFoundPrice < 0L || lot.unitPrice < this.minFoundPrice)) {
               this.minFoundPrice = lot.unitPrice;
            }
         }

         long hash = this.computeSlotsHash(handler, lotCount);
         if (hash == this.prevHash) {
            this.stableHashCount++;
         } else {
            this.stableHashCount = 0;
            this.prevHash = hash;
         }

         this.timeoutTicks--;
         boolean isStable = nonEmptyCount > 0 && this.stableHashCount >= 2;
         if (isStable || this.timeoutTicks <= 0) {
            boolean canAdvancePage = this.isArmorPiece(entry) && this.minFoundPrice < 0L && this.pageScanCount + 1 < MAX_PAGE_SCANS;
            if (!canAdvancePage) {
               this.commitPriceAndAdvance();
            } else {
               int nextPageSlot = this.findNextPageButton(handler, rows);
               if (nextPageSlot < 0) {
                  this.commitPriceAndAdvance();
               } else {
                  this.lastScreenHash = this.computeSlotsHash(handler, lotCount);
                  this.clickSlot(handler.containerId, nextPageSlot);
                  this.state = PriceAutoParser.State.WAIT_PAGE;
                  this.tickDelay = 3;
                  this.timeoutTicks = 30;
                  this.pageScanCount++;
               }
            }
         }
      } else {
         this.commitPriceAndAdvance();
      }
   }

   private void handleWaitPage() {
      if (mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs)) {
         ChestMenu handler = gcs.getMenu();
         int rows = handler.getRowCount();
         int lotCount = (rows - 1) * 9;
         if (this.computeSlotsHash(handler, lotCount) != this.lastScreenHash) {
            this.startScan();
         } else {
            this.timeoutTicks--;
            if (this.timeoutTicks <= 0) {
               this.commitPriceAndAdvance();
            }
         }
      } else {
         this.commitPriceAndAdvance();
      }
   }

   private void handleAdvance() {
      if (mc.gui.screen() != null) {
         mc.player.closeContainer();
         this.tickDelay = 4;
      } else {
         this.currentIndex++;
         this.state = PriceAutoParser.State.NEXT;
         this.tickDelay = 4;
      }
   }

   private void commitPriceAndAdvance() {
      ItemEntry entry = this.parseEntries.get(this.currentIndex);
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      int pct = Math.max(1, Math.min(100, cfg.parsePercent));
      long targetPrice = this.minFoundPrice > 0L ? this.minFoundPrice * pct / 100L : 0L;
      entry.maxUnitPrice = targetPrice;
      if (this.minFoundPrice > 0L) {
         entry.marketPrice = this.minFoundPrice;
      }

      AutoBuyConfig.save();
      this.state = PriceAutoParser.State.ADVANCE;
      this.tickDelay = 2;
   }

   private void prepareSort(AuctionSortMode target, int fallbackClicks, PriceAutoParser.State nextState) {
      this.targetSortMode = target;
      this.sortClicksRemaining = fallbackClicks;
      AuctionSortMode current = AuctionSortMode.current(this.getScreenHandler());
      if (current != null) {
         this.sortClicksRemaining = current.clicksTo(target);
      }

      this.state = nextState;
      this.tickDelay = 6;
   }

   private boolean isAuctionScreen() {
      return mc.gui.screen() instanceof ContainerScreen gcs && AutoBuyEngine.isAuctionScreen(gcs);
   }

   private ChestMenu getScreenHandler() {
      return ((ContainerScreen)mc.gui.screen()).getMenu();
   }

   private boolean isArmorPiece(ItemEntry e) {
      if (e != null && e.itemId != null) {
         String id = e.itemId;
         return id.endsWith("_helmet") || id.endsWith("_chestplate") || id.endsWith("_leggings") || id.endsWith("_boots");
      } else {
         return false;
      }
   }

   private int findNextPageButton(ChestMenu handler, int rows) {
      int from = (rows - 1) * 9;
      int to = rows * 9;
      int slot = -1;

      for (int i = from; i < to; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals("minecraft:lime_dye") || id.equals("minecraft:green_dye")) {
               slot = i;
            }
         }
      }

      return slot;
   }

   private long computeSlotsHash(ChestMenu handler, int count) {
      long h = 1L;

      for (int i = 0; i < count; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         h = h * 31L + (stack.isEmpty() ? 0L : BuiltInRegistries.ITEM.getId(stack.getItem()) * 113L + stack.getCount());
      }

      return h;
   }

   private void clickSlot(int syncId, int slot) {
      if (mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, mc.player);
      }
   }
   private static String formatPrice(long v) {
      if (v <= 0L) return "0";
      StringBuilder sb = new StringBuilder(Long.toString(v));
      for (int i = sb.length() - 3; i > 0; i -= 3) {
         sb.insert(i, ' ');
      }
      return sb.toString();
   }

   private void sendMessage(String text) {
      if (mc.player != null) {
         mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text)));
      }
   }

   private enum State {
      IDLE,
      PRE_OPEN,
      PRE_WAIT,
      PRE_SORT,
      NEXT,
      WAIT_SCREEN,
      SCAN,
      WAIT_PAGE,
      ADVANCE,
      POST_OPEN,
      POST_WAIT,
      POST_SORT;
   }
}
