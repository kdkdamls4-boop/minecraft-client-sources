package tokyo.ghoul.client.features.modules.autobuy;

import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.HashedStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.other.TextUtil;
import tokyo.ghoul.client.features.modules.autobuy.engine.*;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "AuctionAutoBuy", description = "Автоматическая скупка лотов на аукционе", category = Category.AUTOBUY)
public final class AuctionAutoBuy extends Module {
   @Getter private static final AuctionAutoBuy instance = new AuctionAutoBuy();

   private static final long START_DELAY_MS = 0L;
   private static final long AUCTION_REOPEN_COOLDOWN_MS = 3000L;
   private static final long AUCTION_OPEN_TIMEOUT_MS = 4500L;
   private static final long PAGE_SCAN_DELAY_MS = 55L;
   private static final long PAGE_LOAD_TIMEOUT_MS = 450L;
   private static final long CONFIRM_TIMEOUT_MS = 1800L;
   private static final long REFRESH_DELAY_MIN_MS = 100L;
   private static final long REFRESH_DELAY_MAX_MS = 200L;
   private static final long REFRESH_DELAY_SHORT_MAX_MS = 200L;
   private static final long REFRESH_DELAY_LONG_MIN_MS = 100L;
   private static final long REFRESH_BURST_SPACING_FAST_MIN_MS = 100L;
   private static final long REFRESH_BURST_SPACING_FAST_MAX_MS = 200L;
   private static final long REFRESH_BURST_SPACING_SLOW_MIN_MS = 100L;
   private static final long REFRESH_BURST_SPACING_SLOW_MAX_MS = 200L;
   private static final int RCT_FAST_REFRESH_TRIGGER_MIN = 250;
   private static final int RCT_FAST_REFRESH_TRIGGER_MAX = 300;
   private static final int RCT_MEDIUM_REFRESH_TRIGGER_MIN = 140;
   private static final int RCT_MEDIUM_REFRESH_TRIGGER_MAX = 200;
   private static final int RCT_SLOW_REFRESH_TRIGGER_MIN = 60;
   private static final int RCT_SLOW_REFRESH_TRIGGER_MAX = 100;
   private static final int RCT_REFRESH_TRIGGER_CAP = 200;
   private static final long RCT_RECONNECT_TIMEOUT_MS = 25000L;
   private static final long INSUFFICIENT_FUNDS_REFRESH_DELAY_MIN_MS = 1000L;
   private static final long INSUFFICIENT_FUNDS_REFRESH_DELAY_MAX_MS = 1800L;
   private static final long REOPEN_AFTER_CONFIRM_MIN_MS = 100L;
   private static final long REOPEN_AFTER_CONFIRM_MAX_MS = 240L;
   private static final long PURCHASE_ARRIVAL_TIMEOUT_MS = 4000L;
   private static final long RESALE_AFTER_PURCHASE_DELAY_MS = 1400L;
   private static final long RESALE_COMMAND_CONFIRM_DELAY_MS = 900L;
   private static final long RESALE_RESULT_TIMEOUT_MS = 2600L;
   private static final long RESALE_RETRY_DELAY_MS = 1400L;
   private static final long RESALE_CLEAR_CLICK_DELAY_MS = 650L;
   private static final long RESALE_CLEAR_OPEN_TIMEOUT_MS = 5200L;
   private static final long RESALE_CHAT_SUBMIT_DELAY_MS = 260L;
   private static final long RESALE_RESUME_AFTER_LIST_MIN_MS = 1800L;
   private static final long RESALE_RESUME_AFTER_LIST_MAX_MS = 3200L;
   private static final long SMART_RESALE_AUCTION_TIMEOUT_MS = 8000L;
   private static final long SMART_RESALE_SORT_WAIT_MS = 750L;
   private static final long SMART_RESALE_PRICE_DROP_LIMIT = 5_000L;
   private static final int SMART_RESALE_UNIT_UNDERCUT = 1;
   private static final int RESALE_MAX_ATTEMPTS = 5;
   private static final long OFFLINE_SALE_SUMMARY_IGNORE_MS = 10_000L;
   private static final int RESALE_BATCH_TRIGGER_MIN = 1;
   private static final int RESALE_BATCH_TRIGGER_MAX = 4;
   private static final int MAX_ACTIVE_AUCTION_LISTINGS = 5;
   private static final int MAX_STALE_REFRESHES = 5;
   private static final long AUCTION_STALL_RECOVERY_TIMEOUT_MS = 10_000L;
   private static final int AUCTION_LISTING_SLOT_COUNT = 45;
   private static final int AUCTION_CONTROL_ROW_START = 45;
   private static final int AUCTION_CONTROL_ROW_END = 53;
   private static final Pattern UNIT_PRICE_PATTERN = Pattern.compile("([0-9][\\d\\s,]*)\\s*[¤$]");
   private static final Pattern TOTAL_PRICE_PATTERN = Pattern.compile("([0-9][\\d\\s,]*)\\s*[¤$]");
   private static final Pattern OFFLINE_SALE_SUMMARY_LINE_PATTERN = Pattern.compile("^\\[\\d{2}\\.\\d{2}\\s+\\d{2}:\\d{2}\\].*");
   private static final Pattern RESALE_CONFIRM_UNIT_PRICE_PATTERN = Pattern.compile(
      "(?:за\\s*1\\s*(?:шт|ед)[\\w.]*|per\\s*1|for\\s*1)\\D*([0-9][0-9\\s,\\.]*[0-9]|[0-9])",
      Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
   );

   public final BooleanSetting fastSniping = new BooleanSetting("Fast Sniper").value(true);
   public final BooleanSetting autoConfirm = new BooleanSetting("Авто-подтверждение").value(true);
   public final BooleanSetting autoRct = new BooleanSetting("AutoRCT").value(false);
   public final SliderSetting rctRefreshCap = new SliderSetting("Порог RCT")
           .range(50, 500).step(10).value(200f)
           .setVisible(autoRct::getValue);
   public final SliderSetting refreshMinDelay = new SliderSetting("Мин. задержка (мс)").range(50, 500).step(10).value(100f);
   public final SliderSetting refreshMaxDelay = new SliderSetting("Макс. задержка (мс)").range(50, 1000).step(10).value(200f);

   private volatile AutoBuyState state = AutoBuyState.IDLE;
   private List<AutoBuyParsedItemData> cachedParsedItems = List.of();
   private volatile List<AutoBuyParsedItemData> cachedEnabledItems = List.of();
   private long parsedItemsCacheExpiresAtMs;
   private volatile long nextActionAtMs;
   private long auctionOpenDeadlineAtMs;
   private volatile long pageActionAtMs;
   private volatile long pageLoadDeadlineAtMs;
   private long auctionSortActionAtMs;
   private AutoBuyUtil.AuctionSort auctionExpectedSort;
   private volatile boolean auctionSortReady;
   private volatile long confirmDeadlineAtMs;
   private volatile long nextRefreshAtMs;
   private long lastHandledScreenCloseAtMs;
   private volatile int pendingRefreshClicks;
   private volatile boolean pendingAuctionScan;
   private volatile PurchaseCandidate currentCandidate;
   private volatile Path lastPurchasedPath;
   private LastPurchaseInfo lastPurchaseInfo;
   private volatile LastPurchaseInfo pendingPurchaseInfo;
   private volatile AutoBuyParsedItemData pendingPurchaseItem;
   private volatile ItemStack pendingPurchaseReferenceStack;
   private volatile int pendingPurchasePreInventoryCount;
   private volatile long pendingPurchaseDeadlineAtMs;
   private final List<LastPurchaseInfo> purchaseHistory = new CopyOnWriteArrayList<>();
   private final List<ResaleTarget> resaleQueue = new ArrayList<>();
   private final List<ResaleTarget> deferredResaleQueue = new ArrayList<>();
   private volatile ResaleTarget pendingResaleTarget;
   private ResaleTarget activeResaleTarget;
   private ResaleTarget pendingUnsalableHotbarMoveTarget;
   private boolean forceSmartResaleForCurrentTarget;
   private long resaleDeadlineAtMs;
   private int resaleAttempts;
   private int resaleListedThisCycle;
   private boolean retryingDeferredResalePass;
   private boolean resaleLimitDetected;
   private boolean resaleSuccessDetected;
   private boolean resaleFailureDetected;
   private boolean resaleNoFilterDetected;
   private boolean resaleConfirmPriceSeen;
   private boolean resalePriceTooLowDetected;
   private int clearedListingsThisCycle;
   private int resaleBatchThreshold = chooseResaleBatchThreshold();
   private String resaleChatCommand;
   private AutoBuyState resaleChatNextState;
   private long smartResaleAuctionDeadlineAtMs;
   private long smartResalePageActionAtMs;
   private long smartResalePageLoadDeadlineAtMs;
   private long smartResaleSortActionAtMs;
   private AutoBuyUtil.AuctionSort smartResaleExpectedSort;
   private long sessionAutoBuyElapsedMs;
   private long sessionAutoBuyStartedAtMs;
   private long sessionPurchasedItemCount;
   private long sessionSpentCoins;
   private long sessionEarnedCoins;
   private long activeResaleExpectedTotalPrice;
   private int resaleConfirmedUnitPrice = Integer.MAX_VALUE;
   private String lastRefreshSignature;
   private int staleRefreshCount;
   private volatile long lastAuctionActivityAtMs;
   private int rctRefreshCount;
   private int rctRefreshThreshold = chooseRctRefreshThreshold();
   private volatile long rctTransitionAtMs;
   private long nextAutoRctAtMs;
   private long lastAuctionRefreshDelayMs;
   private long offlineSaleSummaryIgnoreUntilMs;
   private int pendingRctAnarchy = -1;
   private long pendingRctDeadlineAtMs;
   private volatile boolean waitingForRandomRct;
   private long lastPacketDrivenAuctionScanAtMs;
   private long lastPacketDrivenConfirmAttemptAtMs;
   private final AtomicBoolean fastPathBusy = new AtomicBoolean();

   public AuctionAutoBuy() {
      addSettings(fastSniping, autoConfirm, autoRct, rctRefreshCap, refreshMinDelay, refreshMaxDelay);
   }

   @Override
   public void onEvent() {
      EventListener tickListener = TickEvent.getInstance().subscribe(new Listener<>(e -> onTick()));
      EventListener packetListener = PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
      addEvents(tickListener, packetListener);
   }

   @Override
   public void onEnable() {
      logDebug("ENABLE", "AuctionAutoBuy enabled");
      AutoBuySellerBlacklistManager.getInstance().reloadBeforeAutoBuy();
      resumeSessionAutoBuyTimer(System.currentTimeMillis());
      invalidateParsedItemsCache();
      resetRuntimeState();

      if (mc == null || mc.player == null || mc.getConnection() == null) {
         return;
      }

      List<AutoBuyParsedItemData> enabledItems = getEnabledParsedItems(true);
      if (enabledItems.isEmpty()) {
         notifyAutoBuy("Нет включённых предметов в AutoBuy.");
         setEnabled(false);
         return;
      }

      long now = System.currentTimeMillis();
      if (mc.player.containerMenu != null && AutoBuyUtil.isAuction(mc.player.containerMenu)) {
         notifyAutoBuy("Запускаю AutoBuy на текущей странице аукциона.");
         state = AutoBuyState.WAIT_AUCTION;
         pendingAuctionScan = true;
         pageActionAtMs = now + PAGE_SCAN_DELAY_MS;
         pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
         auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
         nextActionAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
         lastAuctionActivityAtMs = now;
         return;
      }

      state = AutoBuyState.WAIT_AUCTION;
      nextActionAtMs = now + START_DELAY_MS;
      openAuction(now);
   }

   @Override
   public void onDisable() {
      logDebug("DISABLE", "AuctionAutoBuy disabled");
      pauseSessionAutoBuyTimer(System.currentTimeMillis());
      resetRuntimeState();
   }

   public void onTick() {
      if (!isEnabled() || mc == null || mc.player == null || mc.getConnection() == null) {
         return;
      }

      BotSessionManager botManager = BotSessionManager.getInstance();
      if (botManager.isBackgroundAutoBuyActive()
            && BotContext.current() != botManager.getMainSession()) {
         return;
      }

      long now = System.currentTimeMillis();
      movePendingUnsalableHotbarItem();
      handlePendingRandomRctReconnect(now);
      if (waitingForRandomRct) {
         return;
      }

      promotePendingPurchaseIfReady(now);

      if (isResaleState()) {
         if (!AutoSellModule.getInstance().isEnabled()) {
            resumeAuctionBuyingAfterResale(now);
            return;
         }
         handleResaleState(now);
         return;
      }

      List<AutoBuyParsedItemData> enabledItems = getEnabledParsedItems(false);
      if (enabledItems.isEmpty()) {
         notifyAutoBuy("Все предметы выключены, останавливаю AutoBuy.");
         setEnabled(false);
         return;
      }

      if (mc.player.containerMenu != null && AutoBuyUtil.isWaitBuy(mc.player.containerMenu)) {
         handleConfirmScreen(now);
         return;
      }

      if (mc.player.containerMenu != null && AutoBuyUtil.isAuction(mc.player.containerMenu)) {
         handleAuctionScreen(enabledItems, now);
         return;
      }

      if (state == AutoBuyState.WAIT_CONFIRM && now > confirmDeadlineAtMs) {
         notifyAutoBuy("Не дождался подтверждения покупки, заново открываю аукцион.");
         state = AutoBuyState.WAIT_AUCTION;
         nextActionAtMs = earliestAuctionOpenAt(now);
      }

      if (mc.gui.screen() != null) {
         return;
      }

      if (now < nextActionAtMs) {
         return;
      }

      openAuction(now);
   }

   public void onPacket(PacketEvent.PacketEventData event) {
      if (!isEnabled()) {
         return;
      }
      long now = System.currentTimeMillis();
      Packet<?> rawPacket = event.packet();
      if (shouldLogPacket(rawPacket)) {
         AutoBuyPacketDebugLogger.logPacket(event.isSend() ? "PACKET-SEND" : "PACKET-RECV", rawPacket, buildPacketContext());
      }
      if (!event.isReceive()) {
         return;
      }

      if (fastSniping.getValue()
         && AutoBuyScreenMirror.INSTANCE.accept(rawPacket)
         && tryFastPath(AutoBuyScreenMirror.INSTANCE.snapshot(), now)) {
         return;
      }
      handlePacketDrivenState(rawPacket, now);

      String rawText = null;
      if (rawPacket instanceof ClientboundSystemChatPacket packet) {
         rawText = packet.content().getString();
      } else if (rawPacket instanceof ClientboundDisguisedChatPacket disguised) {
         rawText = disguised.message().getString();
      } else if (rawPacket instanceof ClientboundPlayerChatPacket playerChat) {
         rawText = playerChat.body().content();
      }

      if (rawText == null) {
         return;
      }

      logDebug("CHAT", "message=" + rawText);
      handleCompletedAuctionSaleMessage(normalizeIncomingChat(rawText));
      String normalizedMessage = normalizeIncomingChat(rawText);
      if (isResaleState()) {
         if (isResaleLimitMessage(normalizedMessage)) {
            handleResaleLimitNow(now);
            return;
         }
         handleResaleChatMessage(normalizedMessage);
      }

      if (pendingPurchaseInfo != null && isSuccessfulPurchaseMessage(normalizedMessage)) {
         commitPendingPurchase(now, "purchase-chat");
         return;
      }

      if (!isWaitingForPurchaseResult()) {
         return;
      }

      if (!normalizedMessage.contains("недостаточно средств")) {
         return;
      }

      handleInsufficientFunds(now);
   }

   private void handleAuctionScreen(List<AutoBuyParsedItemData> enabledItems, long now) {
      if (recoverStalledAuction(now)) {
         return;
      }

      if (rctTransitionAtMs > 0L) {
         if (now < rctTransitionAtMs) {
            return;
         }
         rctTransitionAtMs = 0L;
         triggerRandomRctReconnect(now);
         return;
      }

      if (state == AutoBuyState.WAIT_REOPEN) {
         if (now < nextActionAtMs) {
            return;
         }

         state = AutoBuyState.WAIT_AUCTION;
         pendingAuctionScan = true;
         pageActionAtMs = now + PAGE_SCAN_DELAY_MS;
         pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
         auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
      }

      if (state == AutoBuyState.WAIT_AUCTION && !ensureAuctionSort(now)) {
         return;
      }

      if (state == AutoBuyState.WAIT_CONFIRM) {
         if (now <= confirmDeadlineAtMs) {
            return;
         }

         notifyAutoBuy("Окно покупки не открылось, продолжаю скан аукциона.");
         state = AutoBuyState.WAIT_AUCTION;
         pendingAuctionScan = true;
         pageActionAtMs = now + PAGE_SCAN_DELAY_MS;
         pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
      }

      List<ItemStack> slotStacks = handlerSlotStacks();
      if (pendingAuctionScan) {
         if (now < pageActionAtMs) {
            return;
         }
         if (!hasAuctionContentsLoaded(slotStacks) && now < pageLoadDeadlineAtMs) {
            return;
         }

         pendingAuctionScan = false;
         lastAuctionActivityAtMs = now;
         notifyAutoBuy("Сканирую страницу аукциона.");
         logDebug("SCAN", "Scanning auction page; " + describeAuctionScreen());
         PurchaseCandidate candidate = findBestCandidate(enabledItems, slotStacks);
         if (candidate != null && isCandidateStillValid(candidate, slotStacks)) {
            logDebug("CANDIDATE", describeCandidate(candidate));
            clickAuctionSlot(candidate.slotId());
            currentCandidate = candidate;
            state = AutoBuyState.WAIT_CONFIRM;
            confirmDeadlineAtMs = now + CONFIRM_TIMEOUT_MS;
            pendingRefreshClicks = 0;
            nextRefreshAtMs = 0L;
            notifyAutoBuy("Нашёл лот: " + formatItemName(candidate.item()) + " за " + candidate.unitPrice() + ", открываю покупку.");
            return;
         }

         if (pendingRefreshClicks <= 0) {
            scheduleRefreshCycle(now);
         }
      }

      if (nextRefreshAtMs <= 0L || now < nextRefreshAtMs) {
         if (now > auctionOpenDeadlineAtMs && auctionOpenDeadlineAtMs > 0L) {
            auctionOpenDeadlineAtMs = 0L;
         }
         return;
      }

      int refreshSlotId = findRefreshButtonIndex(slotStacks);
      if (refreshSlotId < 0) {
         notifyAutoBuy("Не вижу кнопку обновления, заново открываю /ah.");
         pendingAuctionScan = true;
         pendingRefreshClicks = Math.max(1, pendingRefreshClicks);
         nextRefreshAtMs = now + 180L;
         pageActionAtMs = now + 120L;
         pageLoadDeadlineAtMs = now + Math.max(PAGE_LOAD_TIMEOUT_MS, 520L);
         return;
      }

      String currentSignature = computeAuctionPageSignature(slotStacks);
      if (lastRefreshSignature != null) {
         if (currentSignature.equals(lastRefreshSignature)) {
            staleRefreshCount++;
            if (staleRefreshCount >= MAX_STALE_REFRESHES) {
               notifyAutoBuy("Аукцион не обновляется, переоткрываю /ah.");
               staleRefreshCount = 0;
               lastRefreshSignature = null;
               pendingAuctionScan = false;
               pendingRefreshClicks = 0;
               nextRefreshAtMs = 0L;
               state = AutoBuyState.WAIT_AUCTION;
               nextActionAtMs = earliestAuctionOpenAt(now);
               if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
                  closeHandledScreen(now);
               }
               return;
            }
         } else {
            staleRefreshCount = 0;
         }
      }
      lastRefreshSignature = currentSignature;
      clickSlot(refreshSlotId);
      lastAuctionActivityAtMs = now;
      rctRefreshCount++;
      if (shouldTriggerAutoRct(now)) {
         rctTransitionAtMs = now;
         return;
      }
      int clicksLeft = Math.max(0, pendingRefreshClicks - 1);
      pendingRefreshClicks = clicksLeft;
      if (clicksLeft > 0) {
         lastAuctionRefreshDelayMs = nextBurstSpacingMs();
         nextRefreshAtMs = now + lastAuctionRefreshDelayMs;
      } else {
         nextRefreshAtMs = 0L;
      }
      pendingAuctionScan = true;
      pageActionAtMs = now + PAGE_SCAN_DELAY_MS;
      pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
      auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
   }

   private boolean ensureAuctionSort(long now) {
      Slot sortSlot = AutoBuyUtil.findAuctionSortSlot(mc.player.containerMenu);
      if (auctionSortReady) {
         return true;
      }
      if (sortSlot == null) {
         return false;
      }
      AutoBuyUtil.AuctionSort selectedSort = AutoBuyUtil.getSelectedAuctionSort(sortSlot);
      if (selectedSort == AutoBuyUtil.AuctionSort.NEWEST) {
         auctionSortReady = true;
         auctionExpectedSort = null;
         return true;
      }
      if (selectedSort == null || (auctionExpectedSort != null && selectedSort != auctionExpectedSort)) {
         return false;
      }
      auctionExpectedSort = null;
      if (now < auctionSortActionAtMs) {
         return false;
      }

      clickSlot(sortSlot.index);
      auctionExpectedSort = selectedSort.next();
      auctionSortActionAtMs = now + 750L;
      pageActionAtMs = auctionSortActionAtMs;
      pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
      pendingAuctionScan = true;
      return false;
   }

   private void handleConfirmScreen(long now) {
      if (state == AutoBuyState.WAIT_REOPEN) {
         if (now < nextActionAtMs) {
            return;
         }

         if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
            closeHandledScreen(now);
         }
         state = AutoBuyState.WAIT_AUCTION;
         nextActionAtMs = earliestAuctionOpenAt(now);
         return;
      }

      List<ItemStack> slotStacks = handlerSlotStacks();
      int confirmSlotId = findConfirmButtonIndex(slotStacks);
      if (currentCandidate != null && !hasMatchingConfirmItem(currentCandidate, slotStacks)) {
         int cancelSlotId = findCancelButtonIndex(slotStacks);
         if (cancelSlotId >= 0) {
            clickSlot(cancelSlotId);
            currentCandidate = null;
            state = AutoBuyState.WAIT_REOPEN;
            pendingAuctionScan = false;
            pendingRefreshClicks = 0;
            nextRefreshAtMs = 0L;
            nextActionAtMs = now + ThreadLocalRandom.current().nextLong(120L, 241L);
            confirmDeadlineAtMs = now + CONFIRM_TIMEOUT_MS;
            return;
         }
         if (now <= confirmDeadlineAtMs) {
            return;
         }

         notifyAutoBuy("Предмет в окне подтверждения не совпал, возвращаюсь к аукциону.");
         currentCandidate = null;
         state = AutoBuyState.WAIT_AUCTION;
         nextActionAtMs = earliestAuctionOpenAt(now);
         if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
            closeHandledScreen(now);
         }
         return;
      }

      if (confirmSlotId < 0) {
         if (state != AutoBuyState.WAIT_CONFIRM && now < confirmDeadlineAtMs) {
            return;
         }
         if (now <= confirmDeadlineAtMs) {
            return;
         }

         notifyAutoBuy("Не вижу кнопку подтверждения, возвращаюсь к аукциону.");
         state = AutoBuyState.WAIT_AUCTION;
         nextActionAtMs = earliestAuctionOpenAt(now);
         if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
            closeHandledScreen(now);
         }
         return;
      }

      clickSlot(confirmSlotId);
      recordConfirmedPurchase(now);
   }

   private void recordConfirmedPurchase(long now) {
      boolean resaleStartedWhileResolvingPreviousPurchase = false;
      if (currentCandidate != null) {
         resolvePendingPurchase(now, "superseded");
         resaleStartedWhileResolvingPreviousPurchase = isResaleState();

         notifyAutoBuy("Покупаю: " + formatItemName(currentCandidate.item()) + " за " + currentCandidate.unitPrice() + ".");
         logDebug("CONFIRM", "Confirming purchase: " + describeCandidate(currentCandidate));
         lastPurchasedPath = currentCandidate.item().path();
         int preInventoryCount = countMatchingInventoryItems(currentCandidate.item(), currentCandidate.iconStack());
         pendingPurchaseInfo = new LastPurchaseInfo(
            currentCandidate.displayName(),
            currentCandidate.count(),
            currentCandidate.sellerName(),
            currentCandidate.totalPrice(),
            currentCandidate.iconStack().copy()
         );
         pendingPurchaseItem = currentCandidate.item();
         pendingPurchaseReferenceStack = currentCandidate.iconStack().copy();
         pendingPurchasePreInventoryCount = preInventoryCount;
         pendingPurchaseDeadlineAtMs = now + PURCHASE_ARRIVAL_TIMEOUT_MS;
         if (AutoSellModule.getInstance().isEnabled()) {
            pendingResaleTarget = new ResaleTarget(
               currentCandidate.item(),
               currentCandidate.iconStack().copy(),
               currentCandidate.count(),
               preInventoryCount,
               currentCandidate.totalPrice()
            );
            queuePendingResaleTarget(now);
            resaleStartedWhileResolvingPreviousPurchase = resaleStartedWhileResolvingPreviousPurchase || isResaleState();
         }
      }

      currentCandidate = null;
      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      confirmDeadlineAtMs = now + CONFIRM_TIMEOUT_MS;
      if (!resaleStartedWhileResolvingPreviousPurchase) {
         state = AutoBuyState.WAIT_REOPEN;
         nextActionAtMs = now + ThreadLocalRandom.current().nextLong(REOPEN_AFTER_CONFIRM_MIN_MS, REOPEN_AFTER_CONFIRM_MAX_MS + 1L);
      }
   }

   private void openAuction(long now) {
      if (mc.player == null || mc.getConnection() == null) {
         return;
      }

      logDebug("OPEN", "Sending /ah command");
      mc.getConnection().sendCommand("ah");
      state = AutoBuyState.WAIT_AUCTION;
      pendingAuctionScan = true;
      pageActionAtMs = now + PAGE_SCAN_DELAY_MS;
      pageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
      auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
      nextActionAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      lastRefreshSignature = null;
      staleRefreshCount = 0;
      lastAuctionActivityAtMs = now;
   }

   private boolean recoverStalledAuction(long now) {
      if (state != AutoBuyState.WAIT_AUCTION
         || lastAuctionActivityAtMs <= 0L
         || now - lastAuctionActivityAtMs < AUCTION_STALL_RECOVERY_TIMEOUT_MS) {
         return false;
      }

      notifyAutoBuy("Аукцион завис, переоткрываю /ah.");
      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      lastRefreshSignature = null;
      staleRefreshCount = 0;
      auctionSortReady = false;
      auctionExpectedSort = null;
      lastAuctionActivityAtMs = 0L;
      nextActionAtMs = earliestAuctionOpenAt(now);
      if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
      return true;
   }

   private boolean isWaitingForPurchaseResult() {
      return currentCandidate != null
         || state == AutoBuyState.WAIT_CONFIRM
         || state == AutoBuyState.WAIT_REOPEN;
   }

   private void handleInsufficientFunds(long now) {
      logDebug("FUNDS", "Insufficient funds message received");
      currentCandidate = null;
      pendingPurchaseInfo = null;
      clearPendingPurchaseTracking();
      pendingResaleTarget = null;
      confirmDeadlineAtMs = 0L;

      if (mc != null && mc.player != null && mc.player.containerMenu != null && AutoBuyUtil.isAuction(mc.player.containerMenu)) {
         state = AutoBuyState.WAIT_AUCTION;
         pendingAuctionScan = false;
         pendingRefreshClicks = 1;
         nextRefreshAtMs = now + ThreadLocalRandom.current().nextLong(
            INSUFFICIENT_FUNDS_REFRESH_DELAY_MIN_MS,
            INSUFFICIENT_FUNDS_REFRESH_DELAY_MAX_MS + 1L
         );
         auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
         return;
      }

      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      state = AutoBuyState.WAIT_AUCTION;
      nextActionAtMs = earliestAuctionOpenAt(now);
      if (mc != null && mc.player != null && mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
   }

   private void handlePacketDrivenState(Packet<?> packet, long now) {
      if (packet instanceof ClientboundOpenScreenPacket) {
         if (currentCandidate != null) {
            nextActionAtMs = now;
         }
      } else if (packet instanceof ClientboundContainerClosePacket) {
         if (state == AutoBuyState.WAIT_REOPEN && pendingPurchaseInfo != null) {
            nextActionAtMs = now;
         }
      } else if (packet instanceof ClientboundContainerSetSlotPacket) {
         if (state == AutoBuyState.WAIT_CONFIRM && currentCandidate != null) {
            lastPacketDrivenConfirmAttemptAtMs = now;
            nextActionAtMs = now;
         } else if (state == AutoBuyState.WAIT_AUCTION && pendingAuctionScan) {
            lastPacketDrivenAuctionScanAtMs = now;
            pageActionAtMs = 0L;
            pageLoadDeadlineAtMs = 0L;
            nextActionAtMs = now;
         }
      }
   }

   private void promotePendingPurchaseIfReady(long now) {
      if (pendingPurchaseInfo == null) {
         return;
      }
      if (hasPendingPurchaseArrived()) {
         commitPendingPurchase(now, "inventory");
         return;
      }
      if (now >= pendingPurchaseDeadlineAtMs) {
         discardPendingPurchase("timeout");
      }
   }

   private boolean hasPendingPurchaseArrived() {
      if (mc == null || mc.player == null) {
         return false;
      }
      if (pendingPurchaseItem == null
         && (pendingPurchaseReferenceStack == null || pendingPurchaseReferenceStack.isEmpty())) {
         return false;
      }

      return countMatchingInventoryItems(pendingPurchaseItem, pendingPurchaseReferenceStack)
         > pendingPurchasePreInventoryCount;
   }

   private void resolvePendingPurchase(long now, String discardReason) {
      if (pendingPurchaseInfo == null) {
         return;
      }
      if (hasPendingPurchaseArrived()) {
         commitPendingPurchase(now, "inventory");
         return;
      }

      discardPendingPurchase(discardReason);
   }

   private void discardPendingPurchase(String reason) {
      if (pendingPurchaseInfo == null) {
         return;
      }

      logDebug("PURCHASE", "Purchase discarded: item=" + pendingPurchaseInfo.itemName()
         + ", total=" + pendingPurchaseInfo.totalPrice()
         + ", reason=" + reason);
      pendingPurchaseInfo = null;
      clearPendingPurchaseTracking();
      pendingResaleTarget = null;
   }

   private void clearPendingPurchaseTracking() {
      pendingPurchaseItem = null;
      pendingPurchaseReferenceStack = null;
      pendingPurchasePreInventoryCount = 0;
      pendingPurchaseDeadlineAtMs = 0L;
   }

   private void commitPendingPurchase(long now, String source) {
      LastPurchaseInfo purchaseInfo = pendingPurchaseInfo;
      if (purchaseInfo == null) {
         return;
      }

      lastPurchaseInfo = purchaseInfo;
      logDebug("PURCHASE", "Purchase committed: item=" + purchaseInfo.itemName()
         + ", count=" + purchaseInfo.count()
         + ", seller=" + purchaseInfo.sellerName()
         + ", total=" + purchaseInfo.totalPrice()
         + ", source=" + source);
      purchaseHistory.add(0, purchaseInfo);
      sessionPurchasedItemCount += Math.max(1, purchaseInfo.count());
      sessionSpentCoins += Math.max(0, purchaseInfo.totalPrice());
      if (purchaseHistory.size() > 120) {
         purchaseHistory.remove(purchaseHistory.size() - 1);
      }
      pendingPurchaseInfo = null;
      clearPendingPurchaseTracking();
      beginResaleAfterPurchase(now);
   }

   private void beginResaleAfterPurchase(long now) {
      if (pendingResaleTarget == null) {
         return;
      }
      if (!AutoSellModule.getInstance().isEnabled()) {
         pendingResaleTarget = null;
         return;
      }

      queuePendingResaleTarget(now);
   }

   private void queuePendingResaleTarget(long now) {
      if (pendingResaleTarget == null || !AutoSellModule.getInstance().isEnabled()) {
         pendingResaleTarget = null;
         return;
      }

      resaleQueue.add(pendingResaleTarget);
      pendingResaleTarget = null;
      if (resaleQueue.size() < resaleBatchThreshold && !hasAtLeastTotemsOrTalismans(4)) {
         return;
      }
      if (isResaleState()) {
         return;
      }

      activeResaleTarget = resaleQueue.remove(0);
      forceSmartResaleForCurrentTarget = false;
      resaleAttempts = 0;
      resaleListedThisCycle = 0;
      clearedListingsThisCycle = 0;
      resetResaleResultFlags();
      resetResaleChatState();
      smartResaleAuctionDeadlineAtMs = 0L;
      smartResalePageActionAtMs = 0L;
      smartResalePageLoadDeadlineAtMs = 0L;
      smartResaleSortActionAtMs = 0L;
      smartResaleExpectedSort = null;
      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
      state = AutoBuyState.WAIT_RESALE_ITEM;
      nextActionAtMs = now + RESALE_AFTER_PURCHASE_DELAY_MS;
      resaleDeadlineAtMs = 0L;
   }

   private boolean isResaleState() {
      return state == AutoBuyState.WAIT_RESALE_ITEM
         || state == AutoBuyState.WAIT_SMART_RESALE_AUCTION
         || state == AutoBuyState.TYPE_RESALE_SELL_COMMAND
         || state == AutoBuyState.WAIT_RESALE_CONFIRM
         || state == AutoBuyState.TYPE_RESALE_CONFIRM_COMMAND
         || state == AutoBuyState.WAIT_RESALE_RESULT
         || state == AutoBuyState.WAIT_RESALE_CLEAR_AUCTION
         || state == AutoBuyState.WAIT_RESALE_OWN_LISTINGS
         || state == AutoBuyState.CLAIM_RESALE_OWN_LISTINGS;
   }

   private void handleResaleState(long now) {
      switch (state) {
         case WAIT_RESALE_ITEM -> handleResaleItem(now);
         case WAIT_SMART_RESALE_AUCTION -> handleSmartResaleAuction(now);
         case TYPE_RESALE_SELL_COMMAND, TYPE_RESALE_CONFIRM_COMMAND -> handleResaleChatCommand(now);
         case WAIT_RESALE_CONFIRM -> handleResaleConfirm(now);
         case WAIT_RESALE_RESULT -> handleResaleResult(now);
         case WAIT_RESALE_CLEAR_AUCTION -> handleResaleClearAuction(now);
         case WAIT_RESALE_OWN_LISTINGS -> handleResaleOwnListings(now);
         case CLAIM_RESALE_OWN_LISTINGS -> handleResaleClaimListings(now);
         default -> {}
      }
   }

   private void handleResaleItem(long now) {
      if (activeResaleTarget == null) {
         startNextResaleTargetOrResume(now);
         return;
      }
      if (now < nextActionAtMs) {
         return;
      }
      if (resaleAttempts >= RESALE_MAX_ATTEMPTS) {
         skipCurrentResale(now);
         return;
      }
      if (isResaleTargetListed(activeResaleTarget)) {
         finishCurrentResale(now);
         return;
      }
      if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
         nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
         return;
      }
      if (!prepareResaleTargetInHand(activeResaleTarget)) {
         resaleAttempts++;
         nextActionAtMs = now + RESALE_RETRY_DELAY_MS;
         return;
      }

      resaleAttempts++;
      resetResaleResultFlags();
      if (AutoSellModule.getInstance().smartUndercut.getValue()
         && (forceSmartResaleForCurrentTarget || isSmartResaleTarget(activeResaleTarget))) {
         startSmartResaleAuctionSearch(now);
         return;
      }
      if (usesManualArmorResale(activeResaleTarget)) {
         startResaleChatCommand(
            "/ah sell " + resolveManualArmorResalePrice(activeResaleTarget),
            AutoBuyState.TYPE_RESALE_SELL_COMMAND,
            AutoBuyState.WAIT_RESALE_RESULT,
            now
         );
         return;
      }

      startResaleChatCommand("/ah sell auto", AutoBuyState.TYPE_RESALE_SELL_COMMAND, AutoBuyState.WAIT_RESALE_CONFIRM, now);
   }

   private void startSmartResaleAuctionSearch(long now) {
      if (mc == null || mc.player == null || mc.getConnection() == null) {
         skipCurrentResale(now);
         return;
      }

      smartResaleExpectedSort = null;
      smartResaleSortActionAtMs = 0L;
      mc.getConnection().sendCommand("ah search");
      state = AutoBuyState.WAIT_SMART_RESALE_AUCTION;
      smartResaleAuctionDeadlineAtMs = now + SMART_RESALE_AUCTION_TIMEOUT_MS;
      smartResalePageActionAtMs = now + PAGE_SCAN_DELAY_MS;
      smartResalePageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
   }

   private void handleSmartResaleAuction(long now) {
      if (activeResaleTarget == null) {
         startNextResaleTargetOrResume(now);
         return;
      }

      if (mc.player != null && mc.player.containerMenu != null && AutoBuyUtil.isAuction(mc.player.containerMenu)) {
         if (!ensureSmartResaleSort(now)) {
            return;
         }
         if (now < smartResalePageActionAtMs || (!hasAuctionContentsLoaded(handlerSlotStacks()) && now < smartResalePageLoadDeadlineAtMs)) {
            return;
         }

         int marketUnitPrice = findSmartResaleMarketUnitPrice(activeResaleTarget);
         if (marketUnitPrice == Integer.MAX_VALUE) {
            skipCurrentResale(now);
            return;
         }

         int saleUnitPrice = Math.max(1, marketUnitPrice - SMART_RESALE_UNIT_UNDERCUT);
         long marketTotalPrice = (long)saleUnitPrice * Math.max(1, activeResaleTarget.count());
         long purchaseTotalPrice = Math.max(0L, activeResaleTarget.purchaseTotalPrice());
         if (purchaseTotalPrice - marketTotalPrice >= SMART_RESALE_PRICE_DROP_LIMIT) {
            skipCurrentResale(now);
            return;
         }

         activeResaleExpectedTotalPrice = marketTotalPrice;
         startResaleChatCommand(
            "/ah sell " + Math.max(1L, marketTotalPrice),
            AutoBuyState.TYPE_RESALE_SELL_COMMAND,
            AutoBuyState.WAIT_RESALE_RESULT,
            now
         );
         return;
      }

      if (now >= smartResaleAuctionDeadlineAtMs) {
         skipCurrentResale(now);
      }
   }

   private boolean ensureSmartResaleSort(long now) {
      Slot sortSlot = AutoBuyUtil.findAuctionSortSlot(mc.player.containerMenu);
      if (sortSlot == null) {
         return false;
      }

      AutoBuyUtil.AuctionSort selectedSort = AutoBuyUtil.getSelectedAuctionSort(sortSlot);
      if (selectedSort == AutoBuyUtil.AuctionSort.CHEAPEST_UNIT) {
         smartResaleExpectedSort = null;
         return true;
      }
      if (selectedSort == null || (smartResaleExpectedSort != null && selectedSort != smartResaleExpectedSort)) {
         return false;
      }
      smartResaleExpectedSort = null;
      if (now < smartResaleSortActionAtMs) {
         return false;
      }

      clickSlot(sortSlot.index);
      smartResaleExpectedSort = selectedSort.next();
      smartResaleSortActionAtMs = now + SMART_RESALE_SORT_WAIT_MS;
      smartResalePageActionAtMs = smartResaleSortActionAtMs;
      smartResalePageLoadDeadlineAtMs = now + PAGE_LOAD_TIMEOUT_MS;
      return false;
   }

   private int findSmartResaleMarketUnitPrice(ResaleTarget target) {
      if (mc.player == null || mc.player.containerMenu == null) {
         return Integer.MAX_VALUE;
      }

      int bestPrice = Integer.MAX_VALUE;
      int listingEnd = Math.min(AUCTION_LISTING_SLOT_COUNT, mc.player.containerMenu.slots.size());
      for (int slotIndex = 0; slotIndex < listingEnd; slotIndex++) {
         Slot slot = mc.player.containerMenu.slots.get(slotIndex);
         ItemStack stack = stackOrNull(slot);
         if (stack == null || stack.isEmpty() || !matchesSmartResaleMarketItem(target, stack)) {
            continue;
         }
         int price = resolveAuctionComparisonPrice(stack, extractAuctionPriceInfo(stack));
         if (price < bestPrice) {
            bestPrice = price;
         }
      }
      return bestPrice;
   }

   private void handleResaleConfirm(long now) {
      if (now < nextActionAtMs) {
         return;
      }
      if (resaleNoFilterDetected) {
         retryCurrentResale(now);
         return;
      }
      if (resaleLimitDetected) {
         startClearingOwnListings(now);
         return;
      }
      if (resalePriceTooLowDetected) {
         retryCurrentResale(now);
         return;
      }
      if (!resaleConfirmPriceSeen && now < resaleDeadlineAtMs) {
         return;
      }
      if (!resaleConfirmPriceSeen) {
         retryCurrentResale(now);
         return;
      }

      if (activeResaleTarget != null && resaleConfirmedUnitPrice != Integer.MAX_VALUE) {
         activeResaleExpectedTotalPrice = (long)resaleConfirmedUnitPrice * Math.max(1, activeResaleTarget.count());
      }

      startResaleChatCommand("/ah sell auto confirm", AutoBuyState.TYPE_RESALE_CONFIRM_COMMAND, AutoBuyState.WAIT_RESALE_RESULT, now);
   }

   private void startResaleChatCommand(String command, AutoBuyState typingState, AutoBuyState nextState, long now) {
      if (command == null || command.isBlank() || mc == null || mc.player == null || mc.getConnection() == null) {
         state = AutoBuyState.WAIT_RESALE_ITEM;
         nextActionAtMs = now + RESALE_RETRY_DELAY_MS;
         return;
      }

      String commandWithoutSlash = command.startsWith("/") ? command.substring(1) : command;
      mc.getConnection().sendCommand(commandWithoutSlash);
      resetResaleChatState();
      state = nextState;
      if (nextState == AutoBuyState.WAIT_RESALE_CONFIRM) {
         nextActionAtMs = now + RESALE_COMMAND_CONFIRM_DELAY_MS;
         resaleDeadlineAtMs = now + RESALE_RESULT_TIMEOUT_MS;
      } else if (nextState == AutoBuyState.WAIT_RESALE_RESULT) {
         nextActionAtMs = now;
         resaleDeadlineAtMs = now + RESALE_RESULT_TIMEOUT_MS;
      } else {
         nextActionAtMs = now;
      }
   }

   private void handleResaleChatCommand(long now) {
      if (now >= resaleDeadlineAtMs) {
         resetResaleChatState();
         state = AutoBuyState.WAIT_RESALE_ITEM;
         nextActionAtMs = now + RESALE_RETRY_DELAY_MS;
      }
   }

   private void handleResaleResult(long now) {
      if (resaleLimitDetected) {
         startClearingOwnListings(now);
         return;
      }
      if (resaleSuccessDetected) {
         finishCurrentResale(now);
         return;
      }
      if (resaleFailureDetected) {
         retryCurrentResale(now);
         return;
      }
      if (activeResaleTarget != null && isResaleTargetListed(activeResaleTarget)) {
         finishCurrentResale(now);
         return;
      }
      if (now >= resaleDeadlineAtMs) {
         retryCurrentResale(now);
      }
   }

   private void handleResaleLimitNow(long now) {
      resaleLimitDetected = true;
      resaleFailureDetected = true;
      startClearingOwnListings(now);
   }

   private void retryCurrentResale(long now) {
      if (activeResaleTarget == null) {
         startNextResaleTargetOrResume(now);
         return;
      }
      if (resaleAttempts >= RESALE_MAX_ATTEMPTS) {
         skipCurrentResale(now);
         return;
      }

      resetResaleResultFlags();
      resetResaleChatState();
      state = AutoBuyState.WAIT_RESALE_ITEM;
      nextActionAtMs = now + RESALE_RETRY_DELAY_MS;
   }

   private void skipCurrentResale(long now) {
      if (activeResaleTarget != null && !retryingDeferredResalePass) {
         deferredResaleQueue.add(activeResaleTarget);
      }
      scheduleUnsalableHotbarMove(activeResaleTarget);
      activeResaleTarget = null;
      forceSmartResaleForCurrentTarget = false;
      resaleAttempts = 0;
      resetResaleResultFlags();
      resetResaleChatState();
      startNextResaleTargetOrResume(now);
   }

   private void scheduleUnsalableHotbarMove(ResaleTarget target) {
      if (target == null || mc == null || mc.player == null) {
         return;
      }

      pendingUnsalableHotbarMoveTarget = target;
      if (mc.player.containerMenu != mc.player.inventoryMenu) {
         closeHandledScreen(System.currentTimeMillis());
      }
   }

   private void movePendingUnsalableHotbarItem() {
      ResaleTarget target = pendingUnsalableHotbarMoveTarget;
      if (target == null || mc == null || mc.player == null || mc.gameMode == null) {
         return;
      }
      if (mc.player.containerMenu != mc.player.inventoryMenu) {
         return;
      }

      Inventory inventory = mc.player.getInventory();
      int hotbarSlot = inventory.getSelectedSlot();
      if (!matchesResaleStack(target, inventory.getItem(hotbarSlot))) {
         pendingUnsalableHotbarMoveTarget = null;
         return;
      }

      int emptyInventorySlot = -1;
      for (int slot = 9; slot < 36; slot++) {
         if (inventory.getItem(slot).isEmpty()) {
            emptyInventorySlot = slot;
            break;
         }
      }
      if (emptyInventorySlot < 0) {
         pendingUnsalableHotbarMoveTarget = null;
         return;
      }

      int sourceSlotId = 36 + hotbarSlot;
      mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, sourceSlotId, 0, ContainerInput.PICKUP, mc.player);
      mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, emptyInventorySlot, 0, ContainerInput.PICKUP, mc.player);
      mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, sourceSlotId, 0, ContainerInput.PICKUP, mc.player);
      pendingUnsalableHotbarMoveTarget = null;
   }

   private void finishCurrentResale(long now) {
      resaleListedThisCycle++;
      activeResaleTarget = null;
      activeResaleExpectedTotalPrice = 0L;
      forceSmartResaleForCurrentTarget = false;
      resaleAttempts = 0;
      resetResaleResultFlags();
      startNextResaleTargetOrResume(now);
   }

   private void startNextResaleTargetOrResume(long now) {
      if (!resaleQueue.isEmpty() && resaleListedThisCycle < MAX_ACTIVE_AUCTION_LISTINGS) {
         activeResaleTarget = resaleQueue.remove(0);
         forceSmartResaleForCurrentTarget = false;
         resaleAttempts = 0;
         resetResaleResultFlags();
         state = AutoBuyState.WAIT_RESALE_ITEM;
         nextActionAtMs = now + RESALE_AFTER_PURCHASE_DELAY_MS;
         return;
      }

      if (!retryingDeferredResalePass && !deferredResaleQueue.isEmpty()
         && resaleListedThisCycle < MAX_ACTIVE_AUCTION_LISTINGS) {
         resaleQueue.addAll(deferredResaleQueue);
         deferredResaleQueue.clear();
         retryingDeferredResalePass = true;
         activeResaleTarget = resaleQueue.remove(0);
         forceSmartResaleForCurrentTarget = false;
         resaleAttempts = 0;
         resetResaleResultFlags();
         state = AutoBuyState.WAIT_RESALE_ITEM;
         nextActionAtMs = now + RESALE_RETRY_DELAY_MS;
         return;
      }

      resumeAuctionBuyingAfterResale(now);
   }

   private void resumeAuctionBuyingAfterResale(long now) {
      long resumeDelayMs = resaleListedThisCycle > 0
         ? ThreadLocalRandom.current().nextLong(RESALE_RESUME_AFTER_LIST_MIN_MS, RESALE_RESUME_AFTER_LIST_MAX_MS + 1L)
         : 0L;
      activeResaleTarget = null;
      pendingResaleTarget = null;
      resaleQueue.clear();
      resaleBatchThreshold = chooseResaleBatchThreshold();
      resaleAttempts = 0;
      resaleListedThisCycle = 0;
      retryingDeferredResalePass = false;
      clearedListingsThisCycle = 0;
      resetResaleResultFlags();
      resetResaleChatState();
      resaleDeadlineAtMs = 0L;
      auctionSortReady = false;
      auctionExpectedSort = null;
      auctionSortActionAtMs = 0L;
      if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
      state = AutoBuyState.WAIT_AUCTION;
      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      nextActionAtMs = Math.max(earliestAuctionOpenAt(now), now + resumeDelayMs);
   }

   private void startClearingOwnListings(long now) {
      if (activeResaleTarget != null) {
         resaleQueue.add(0, activeResaleTarget);
         activeResaleTarget = null;
      }
      resaleAttempts = 0;
      clearedListingsThisCycle = 0;
      resetResaleResultFlags();
      resetResaleChatState();
      if (mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
      state = AutoBuyState.WAIT_RESALE_CLEAR_AUCTION;
      nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
      resaleDeadlineAtMs = now + RESALE_CLEAR_OPEN_TIMEOUT_MS;
   }

   private void handleResaleClearAuction(long now) {
      if (now < nextActionAtMs) {
         return;
      }

      if (mc.player.containerMenu != null && AutoBuyUtil.isAuction(mc.player.containerMenu)) {
         Slot ownListingsSlot = findOwnListingsButtonSlot();
         if (ownListingsSlot != null) {
            clickSlot(ownListingsSlot.index);
            state = AutoBuyState.WAIT_RESALE_OWN_LISTINGS;
            nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
            resaleDeadlineAtMs = now + RESALE_CLEAR_OPEN_TIMEOUT_MS;
            return;
         }
      }

      if (mc.gui.screen() != null && mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
         nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
         return;
      }

      mc.getConnection().sendCommand("ah");
      nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
      resaleDeadlineAtMs = now + RESALE_CLEAR_OPEN_TIMEOUT_MS;
   }

   private void handleResaleOwnListings(long now) {
      if (now < nextActionAtMs) {
         return;
      }
      if (isOwnListingsScreen()) {
         state = AutoBuyState.CLAIM_RESALE_OWN_LISTINGS;
         nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
         resaleDeadlineAtMs = now + 900L;
         return;
      }
      if (now >= resaleDeadlineAtMs) {
         startNextResaleTargetOrResume(now);
      }
   }

   private void handleResaleClaimListings(long now) {
      if (now < nextActionAtMs) {
         return;
      }
      if (!isOwnListingsScreen()) {
         if (now >= resaleDeadlineAtMs) {
            startNextResaleTargetOrResume(now);
         }
         return;
      }

      Slot ownListingSlot = findOwnListingItemSlot();
      if (ownListingSlot != null) {
         enqueueClaimedListing(ownListingSlot.getItem());
         clickSlot(ownListingSlot.index);
         clearedListingsThisCycle++;
         nextActionAtMs = now + RESALE_CLEAR_CLICK_DELAY_MS;
         resaleDeadlineAtMs = now + 900L;
         return;
      }

      startNextResaleTargetOrResume(now);
   }

   private void handleResaleChatMessage(String normalizedMessage) {
      if (isResaleNoFilterMessage(normalizedMessage)) {
         if (state != AutoBuyState.WAIT_SMART_RESALE_AUCTION && activeResaleTarget != null) {
            forceSmartResaleForCurrentTarget = true;
            resetResaleResultFlags();
            startSmartResaleAuctionSearch(System.currentTimeMillis());
            return;
         }
         resaleNoFilterDetected = true;
         resaleFailureDetected = true;
         return;
      }
      if (isResaleSellConfirmMessage(normalizedMessage)) {
         resaleConfirmPriceSeen = true;
         int unitPrice = parseResaleConfirmUnitPrice(normalizedMessage);
         resaleConfirmedUnitPrice = unitPrice;
         if (unitPrice != Integer.MAX_VALUE && isResaleConfirmPriceTooLow(unitPrice)) {
            resalePriceTooLowDetected = true;
            resaleFailureDetected = true;
         }
         return;
      }
      if (isResaleSuccessMessage(normalizedMessage)) {
         resaleSuccessDetected = true;
      }
      if (isResaleLimitMessage(normalizedMessage)) {
         resaleLimitDetected = true;
      }
      if (isResaleFailureMessage(normalizedMessage)) {
         resaleFailureDetected = true;
      }
   }

   private void handleCompletedAuctionSaleMessage(String message) {
      if (message == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (isOfflineSaleSummaryHeader(message)) {
         offlineSaleSummaryIgnoreUntilMs = now + OFFLINE_SALE_SUMMARY_IGNORE_MS;
         return;
      }
      if (now <= offlineSaleSummaryIgnoreUntilMs && isOfflineSaleSummaryLine(message)) {
         return;
      }
      if (isCompletedAuctionSaleMessage(message)) {
         long salePrice = parseCompletedSalePrice(message);
         if (salePrice > 0L) {
            sessionEarnedCoins += salePrice;
         }
      }
   }

   private boolean isOfflineSaleSummaryHeader(String message) {
      return message.contains("пока вас не было")
         && (message.contains("купили предмет") || message.contains("купили"))
         || message.contains("while you were away") && (message.contains("bought") || message.contains("purchased"));
   }

   private boolean isOfflineSaleSummaryLine(String message) {
      return OFFLINE_SALE_SUMMARY_LINE_PATTERN.matcher(message).matches() && isCompletedAuctionSaleMessage(message);
   }

   private boolean isCompletedAuctionSaleMessage(String message) {
      return message.contains("купил") && message.contains("у вас")
         || message.contains("ваш") && message.contains("продан") && !message.contains("выстав")
         || message.contains("bought from you")
         || message.contains("purchased from you")
         || message.contains("your") && message.contains("sold");
   }

   private long parseCompletedSalePrice(String message) {
      int priceMarker = Math.max(message.lastIndexOf(" за "), message.lastIndexOf(" for "));
      if (priceMarker >= 0) {
         long price = parseFirstNumberAfter(message, priceMarker + 4);
         if (price > 0L) {
            return price;
         }
      }
      return parseLastNumber(message);
   }

   private long parseFirstNumberAfter(String message, int startIndex) {
      StringBuilder number = new StringBuilder();
      for (int index = Math.max(0, startIndex); index < message.length(); index++) {
         char character = message.charAt(index);
         if (Character.isDigit(character)) {
            number.append(character);
         } else if ((character == ' ' || character == ',' || character == '.') && hasDigitAfter(message, index + 1)) {
            number.append(character);
         } else if (!number.isEmpty()) {
            break;
         }
      }
      return parseMoneyNumberOrZero(number);
   }

   private long parseLastNumber(String message) {
      long lastNumber = 0L;
      StringBuilder number = new StringBuilder();
      for (int index = 0; index < message.length(); index++) {
         char character = message.charAt(index);
         if (Character.isDigit(character)) {
            number.append(character);
         } else if ((character == ' ' || character == ',' || character == '.') && !number.isEmpty() && hasDigitAfter(message, index + 1)) {
            number.append(character);
         } else if (!number.isEmpty()) {
            lastNumber = parseMoneyNumberOrZero(number);
            number.setLength(0);
         }
      }
      if (!number.isEmpty()) {
         lastNumber = parseMoneyNumberOrZero(number);
      }
      return lastNumber;
   }

   private boolean hasDigitAfter(String message, int startIndex) {
      for (int index = Math.max(0, startIndex); index < message.length(); index++) {
         char character = message.charAt(index);
         if (Character.isDigit(character)) {
            return true;
         }
         if (character != ' ' && character != ',' && character != '.') {
            return false;
         }
      }
      return false;
   }

   private long parseMoneyNumberOrZero(StringBuilder number) {
      try {
         if (number.isEmpty()) {
            return 0L;
         }
         return Long.parseLong(number.toString().replace(" ", "").replace(",", "").replace(".", ""));
      } catch (NumberFormatException ignored) {
         return 0L;
      }
   }

   private boolean prepareResaleTargetInHand(ResaleTarget target) {
      if (target == null || mc == null || mc.player == null || mc.gameMode == null) {
         return false;
      }
      if (matchesResaleStack(target, mc.player.getMainHandItem())) {
         return true;
      }

      int selectedSlot = Math.max(0, Math.min(8, mc.player.getInventory().getSelectedSlot()));
      int hotbarSlot = findResaleInventorySlot(target, 0, 8, false);
      if (hotbarSlot >= 0) {
         selectHotbarSlot(hotbarSlot);
         return matchesResaleStack(target, mc.player.getMainHandItem());
      }

      int inventorySlot = findResaleInventorySlot(target, 9, 35, true);
      if (inventorySlot >= 0) {
         mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, inventorySlot, selectedSlot, ContainerInput.SWAP, mc.player);
         return matchesResaleStack(target, mc.player.getMainHandItem());
      }

      hotbarSlot = findResaleInventorySlot(target, 0, 8, true);
      if (hotbarSlot >= 0) {
         selectHotbarSlot(hotbarSlot);
         return matchesResaleStack(target, mc.player.getMainHandItem());
      }
      return false;
   }

   private void selectHotbarSlot(int hotbarSlot) {
      if (mc == null || mc.player == null || hotbarSlot < 0 || hotbarSlot > 8) {
         return;
      }

      Inventory inventory = mc.player.getInventory();
      if (inventory.getSelectedSlot() == hotbarSlot) {
         return;
      }

      inventory.setSelectedSlot(hotbarSlot);
      if (mc.getConnection() != null) {
         mc.getConnection().send(new ServerboundSetCarriedItemPacket(hotbarSlot));
      }
   }

   private boolean usesManualArmorResale(ResaleTarget target) {
      if (target == null
         || target.purchaseTotalPrice() <= 0
         || target.referenceStack() == null
         || target.referenceStack().isEmpty()) {
         return false;
      }
      ItemStack stack = target.referenceStack();
      return stack.has(DataComponents.EQUIPPABLE) || stack.getItem() == Items.ELYTRA;
   }

   private long resolveManualArmorResalePrice(ResaleTarget target) {
      if (target == null || target.purchaseTotalPrice() <= 0) {
         return 1L;
      }

      return Math.max(1L, Math.round(target.purchaseTotalPrice() * 1.75d));
   }

   private int findResaleInventorySlot(ResaleTarget target, int preferredStart, int preferredEnd, boolean allowFallback) {
      if (target == null || mc == null || mc.player == null) {
         return -1;
      }

      Inventory inventory = mc.player.getInventory();
      int existingToSkip = Math.max(0, target.preInventoryCount());
      int fallback = -1;
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (!matchesResaleStack(target, stack)) {
            continue;
         }

         boolean preferred = slot >= preferredStart && slot <= preferredEnd;
         if (preferred && fallback < 0) {
            fallback = slot;
         }
         if (existingToSkip >= stack.getCount()) {
            existingToSkip -= stack.getCount();
            continue;
         }
         if (preferred) {
            return slot;
         }
      }
      return allowFallback ? fallback : -1;
   }

   private boolean isResaleTargetListed(ResaleTarget target) {
      return target != null && countMatchingInventoryItems(target) <= target.preInventoryCount();
   }

   private int countMatchingInventoryItems(ResaleTarget target) {
      return target == null ? 0 : countMatchingInventoryItems(target.item(), target.referenceStack());
   }

   private int countMatchingInventoryItems(AutoBuyParsedItemData item, ItemStack referenceStack) {
      if (mc == null || mc.player == null) {
         return 0;
      }

      int count = 0;
      Inventory inventory = mc.player.getInventory();
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = inventory.getItem(slot);
         if (matchesResaleStack(item, referenceStack, stack)) {
            count += Math.max(1, stack.getCount());
         }
      }
      return count;
   }

   private boolean matchesResaleStack(ResaleTarget target, ItemStack stack) {
      return target != null && matchesResaleStack(target.item(), target.referenceStack(), stack);
   }

   private boolean matchesResaleStack(AutoBuyParsedItemData item, ItemStack referenceStack, ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      }
      if (item != null && AutoBuyParsedItems.matchesAuctionStack(item, stack, resolveDurabilityRequirement(item))) {
         return true;
      }
      if (referenceStack == null || referenceStack.isEmpty() || stack.getItem() != referenceStack.getItem()) {
         return false;
      }

      String stackName = normalizeStackName(stack);
      String referenceName = normalizeStackName(referenceStack);
      return referenceName.isBlank() || stackName.equals(referenceName);
   }

   private Slot findOwnListingsButtonSlot() {
      if (mc.player == null || mc.player.containerMenu == null) {
         return null;
      }

      if (mc.player.containerMenu.slots.size() > 45) {
         Slot slot = mc.player.containerMenu.slots.get(45);
         if (slot != null && slot.getItem() != null && !slot.getItem().isEmpty()
            && slot.getItem().getItem() == Items.ENDER_CHEST
            && stackContainsText(slot.getItem(), "товар", "продаж")) {
            return slot;
         }
      }

      for (Slot slot : mc.player.containerMenu.slots) {
         if (slot == null) {
            continue;
         }

         ItemStack stack = slot.getItem();
         if (stack == null || stack.isEmpty() || stack.getItem() != Items.ENDER_CHEST) {
            continue;
         }
         if (stackContainsText(stack, "товар", "продаж")
            || stackContainsText(stack, "selling")) {
            return slot;
         }
      }
      return null;
   }

   private boolean isOwnListingsScreen() {
      if (mc.gui.screen() == null) {
         return false;
      }

      String title = AutoBuyParsedItems.normalizeDisplayName(mc.gui.screen().getTitle().getString())
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е');
      return title.contains("товар") && title.contains("продаж")
         || title.contains("selling");
   }

   private Slot findOwnListingItemSlot() {
      if (mc.player == null || mc.player.containerMenu == null) {
         return null;
      }

      int listingEnd = Math.min(AUCTION_LISTING_SLOT_COUNT, mc.player.containerMenu.slots.size());
      for (int slotIndex = 0; slotIndex < listingEnd; slotIndex++) {
         Slot slot = mc.player.containerMenu.slots.get(slotIndex);
         if (slot == null) {
            continue;
         }

         ItemStack stack = slot.getItem();
         if (stack == null || stack.isEmpty() || isOwnListingsControlStack(stack)) {
            continue;
         }
         return slot;
      }
      return null;
   }

   private boolean isOwnListingsControlStack(ItemStack stack) {
      Item item = stack.getItem();
      if (Items.STAINED_GLASS_PANE.asList().contains(item)
         || item == Items.GLASS_PANE
         || item == Items.ARROW
         || item == Items.BARRIER
         || item == Items.ENDER_CHEST
         || item == Items.NETHER_STAR) {
         return true;
      }

      String normalizedName = normalizeStackName(stack);
      return normalizedName.contains("назад")
         || normalizedName.contains("далее")
         || normalizedName.contains("след")
         || normalizedName.contains("закрыт")
         || normalizedName.contains("back")
         || normalizedName.contains("next")
         || normalizedName.contains("close");
   }

   private void enqueueClaimedListing(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return;
      }
      AutoBuyParsedItemData item = findMatchingParsedItem(stack);
      resaleQueue.add(new ResaleTarget(
         item,
         stack.copy(),
         Math.max(1, stack.getCount()),
         countMatchingInventoryItems(item, stack),
         0
      ));
   }

   private AutoBuyParsedItemData findMatchingParsedItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      for (AutoBuyParsedItemData item : getEnabledParsedItems(false)) {
         if (AutoBuyParsedItems.matchesAuctionStack(item, stack, resolveDurabilityRequirement(item))) {
            return item;
         }
      }
      return null;
   }

   private boolean stackContainsText(ItemStack stack, String... needles) {
      if (stack == null || stack.isEmpty() || needles == null || needles.length == 0) {
         return false;
      }

      StringBuilder text = new StringBuilder(normalizeStackName(stack));
      for (Component line : AutoBuyUtil.displayLines(stack)) {
         text.append(' ').append(AutoBuyParsedItems.normalizeDisplayName(line.getString()).toLowerCase(Locale.ROOT).replace('ё', 'е'));
      }

      String normalized = text.toString();
      for (String needle : needles) {
         if (needle != null && !needle.isBlank() && !normalized.contains(needle.toLowerCase(Locale.ROOT))) {
            return false;
         }
      }
      return true;
   }

   private boolean isResaleSuccessMessage(String message) {
      return message.contains("выстав") && message.contains("продаж")
         || message.contains("успеш") && message.contains("продаж")
         || message.contains("listed") && message.contains("auction")
         || message.contains("success") && message.contains("sell");
   }

   private boolean isResaleLimitMessage(String message) {
      return message.contains("максим") && (message.contains("товар") || message.contains("лот") || message.contains("аукцион"))
         || message.contains("вы не можете больше выставлять товары на аукцион")
         || message.contains("не можете больше выставлять") && message.contains("товар") && message.contains("аукцион")
         || message.contains("не") && message.contains("можете") && message.contains("выстав") && message.contains("аукцион")
         || message.contains("лимит") && message.contains("продаж")
         || message.contains("maximum") && message.contains("auction")
         || message.contains("limit") && message.contains("sell");
   }

   private boolean isResaleNoFilterMessage(String message) {
      return message.contains("по вашему запросу")
         && message.contains("не было найдено")
         && message.contains("фильтр")
         || message.contains("no filter") && message.contains("found");
   }

   private boolean isResaleSellConfirmMessage(String message) {
      return message.contains("/ah sell auto confirm")
         || message.contains("подтверд") && message.contains("продаж")
         || message.contains("полная цена") && message.contains("за 1");
   }

   private int parseResaleConfirmUnitPrice(String message) {
      if (message == null || message.isBlank()) {
         return Integer.MAX_VALUE;
      }

      Matcher matcher = RESALE_CONFIRM_UNIT_PRICE_PATTERN.matcher(message);
      if (!matcher.find()) {
         return Integer.MAX_VALUE;
      }

      return parseResalePriceAmount(matcher.group(1));
   }

   private int parseResalePriceAmount(String amount) {
      if (amount == null || amount.isBlank()) {
         return Integer.MAX_VALUE;
      }

      StringBuilder digits = new StringBuilder();
      for (int i = 0; i < amount.length(); i++) {
         char character = amount.charAt(i);
         if (Character.isDigit(character)) {
            digits.append(character);
         }
      }
      if (digits.isEmpty()) {
         return Integer.MAX_VALUE;
      }

      try {
         long value = Long.parseLong(digits.toString());
         return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private boolean isResaleConfirmPriceTooLow(int unitPrice) {
      if (unitPrice <= 0 || activeResaleTarget == null || activeResaleTarget.item() == null) {
         return false;
      }

      int configuredPrice = resolveConfiguredPrice(activeResaleTarget.item());
      return configuredPrice != Integer.MAX_VALUE && configuredPrice > 0 && unitPrice < configuredPrice;
   }

   private boolean isResaleFailureMessage(String message) {
      return message.contains("не удал")
         || message.contains("нельзя")
         || message.contains("невозмож")
         || message.contains("ошиб")
         || message.contains("failed")
         || message.contains("cannot")
         || message.contains("error");
   }

   private void resetResaleResultFlags() {
      resaleLimitDetected = false;
      resaleSuccessDetected = false;
      resaleFailureDetected = false;
      resaleNoFilterDetected = false;
      resaleConfirmPriceSeen = false;
      resalePriceTooLowDetected = false;
      resaleConfirmedUnitPrice = Integer.MAX_VALUE;
   }

   private long earliestAuctionOpenAt(long now) {
      return Math.max(now + START_DELAY_MS, lastHandledScreenCloseAtMs + AUCTION_REOPEN_COOLDOWN_MS);
   }

   private void closeHandledScreen(long now) {
      if (mc == null || mc.player == null || mc.player.containerMenu == null) {
         return;
      }
      if (mc.player.containerMenu.containerId == 0) {
         return;
      }

      logDebug("SCREEN", "Closing containerMenu containerId=" + mc.player.containerMenu.containerId);
      mc.player.closeContainer();
      lastHandledScreenCloseAtMs = now;
   }

   private PurchaseCandidate findBestCandidate(List<AutoBuyParsedItemData> enabledItems, List<ItemStack> slotStacks) {
      if (slotStacks.isEmpty() || enabledItems.isEmpty()) {
         return null;
      }

      PurchaseCandidate bestCandidate = null;
      Map<String, List<AutoBuyParsedItemData>> indexedItems = indexEnabledItemsByItemId(enabledItems);
      int listingEnd = Math.min(AUCTION_LISTING_SLOT_COUNT, slotStacks.size());
      for (int slotIndex = 0; slotIndex < listingEnd; slotIndex++) {
         ItemStack stack = slotStacks.get(slotIndex);
         if (stack == null || stack.isEmpty()) {
            continue;
         }

         String stackItemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
         List<AutoBuyParsedItemData> candidateItems = indexedItems.get(stackItemId);
         if ((candidateItems == null || candidateItems.isEmpty())
            && !(stack.getItem() instanceof SpawnEggItem || stackItemId.endsWith("_spawn_egg"))) {
            continue;
         }

         AuctionPriceInfo priceInfo = extractAuctionPriceInfo(stack);
         if (priceInfo.bidOnly()) {
            continue;
         }
         int unitPrice = resolveAuctionComparisonPrice(stack, priceInfo);
         int totalPrice = resolveAuctionTotalPrice(stack, priceInfo, unitPrice);
         String sellerName = resolveAuctionSeller(stack);

         for (AutoBuyParsedItemData item : candidateItems == null || candidateItems.isEmpty() ? enabledItems : candidateItems) {
            int durabilityRequirement = resolveDurabilityRequirement(item);
            if (!matchesAuctionStack(item, stack, durabilityRequirement)) {
               continue;
            }

            int configuredPrice = resolveConfiguredPrice(item);
            String displayName = resolveCandidateDisplayName(item, stack);

            if (isOwnAuctionSeller(sellerName)) {
               continue;
            }
            if (isBlacklistedAuctionSeller(sellerName)) {
               continue;
            }

            if (configuredPrice <= 0 || unitPrice == Integer.MAX_VALUE || unitPrice > configuredPrice) {
               continue;
            }

            boolean preferBecauseDifferentFromLast = bestCandidate != null
               && unitPrice == bestCandidate.unitPrice()
               && item.path() != null
               && !item.path().equals(lastPurchasedPath)
               && bestCandidate.item().path() != null
               && bestCandidate.item().path().equals(lastPurchasedPath);
            if (bestCandidate == null || unitPrice < bestCandidate.unitPrice() || preferBecauseDifferentFromLast) {
               bestCandidate = new PurchaseCandidate(
                  item,
                  slotIndex,
                  unitPrice,
                  configuredPrice,
                  totalPrice,
                  Math.max(1, stack.getCount()),
                  displayName,
                  sellerName,
                  stack.copy()
               );
            }
            break;
         }
      }

      return bestCandidate;
   }

   private String resolveCandidateDisplayName(AutoBuyParsedItemData item, ItemStack stack) {
      String stackName = stack == null ? "" : AutoBuyParsedItems.normalizeDisplayName(stack.getHoverName().getString());
      if (!stackName.isBlank()) {
         return stackName;
      }
      return formatItemName(item);
   }

   private String resolveAuctionSeller(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return "";
      }

      List<Component> tooltip = AutoBuyUtil.displayLines(stack);
      for (int i = tooltip.size() - 1; i >= 0; i--) {
         Component line = tooltip.get(i);
         String raw = AutoBuyParsedItems.normalizeDisplayName(line.getString()).trim();
         if (raw.isBlank()) {
            continue;
         }

         String normalized = raw.toLowerCase(Locale.ROOT).replace('ё', 'е');
         if (!normalized.contains("продавец") && !normalized.contains("seller")) {
            continue;
         }

         int colonIndex = raw.indexOf(':');
         if (colonIndex >= 0 && colonIndex + 1 < raw.length()) {
            return raw.substring(colonIndex + 1).trim();
         }
      }

      return "";
   }

   private boolean isOwnAuctionSeller(String sellerName) {
      String normalizedSellerName = normalizeAuctionSellerName(sellerName);
      if (normalizedSellerName.isBlank() || mc == null) {
         return false;
      }

      String sessionName = mc.getUser() != null ? mc.getUser().getName() : "";
      if (normalizedSellerName.equals(normalizeAuctionSellerName(sessionName))) {
         return true;
      }

      String playerName = mc.player != null ? mc.player.getName().getString() : "";
      return normalizedSellerName.equals(normalizeAuctionSellerName(playerName));
   }

   private boolean isBlacklistedAuctionSeller(String sellerName) {
      String normalizedSellerName = normalizeAuctionSellerName(sellerName);
      if (normalizedSellerName.isBlank()) {
         return false;
      }
      return AutoBuySellerBlacklistManager.getInstance().isBlacklisted(normalizedSellerName);
   }

   private String normalizeAuctionSellerName(String name) {
      if (name == null || name.isBlank()) {
         return "";
      }

      String normalized = AutoBuyParsedItems.normalizeDisplayName(name).trim();
      int spaceIndex = normalized.indexOf(' ');
      if (spaceIndex >= 0) {
         normalized = normalized.substring(0, spaceIndex).trim();
      }

      return normalized.toLowerCase(Locale.ROOT).replace('ё', 'е');
   }

   private int resolveDurabilityRequirement(AutoBuyParsedItemData parsedItem) {
      if (parsedItem == null) {
         return -1;
      }
      if (usesGlobalDurabilityRequirement(parsedItem)) {
         return Math.round(AutoSborModule.getInstance().armorDurability.getValue());
      }
      return -1;
   }

   private boolean usesGlobalDurabilityRequirement(AutoBuyParsedItemData parsedItem) {
      if (parsedItem == null) {
         return false;
      }

      String itemId = parsedItem.itemId();
      String normalizedName = formatItemName(parsedItem).toLowerCase(Locale.ROOT);
      if ("minecraft:golden_helmet".equals(itemId) && normalizedName.contains("солнца")) {
         return false;
      }
      if ("minecraft:elytra".equals(itemId)) {
         return true;
      }
      return parsedItem.iconStack() != null && parsedItem.iconStack().has(DataComponents.EQUIPPABLE);
   }

   private int resolveConfiguredPrice(AutoBuyParsedItemData item) {
      String normalized = AutoBuyParsedItems.normalizeStoredPrice(item == null ? "" : item.price());
      if (normalized.isBlank()) {
         return Integer.MAX_VALUE;
      }
      try {
         return Integer.parseInt(normalized);
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private int resolveAuctionComparisonPrice(ItemStack stack, AuctionPriceInfo priceInfo) {
      if (priceInfo != null && priceInfo.bidOnly()) {
         return Integer.MAX_VALUE;
      }
      if (priceInfo != null && priceInfo.unitPrice() != Integer.MAX_VALUE) {
         return priceInfo.unitPrice();
      }
      int totalPrice = priceInfo == null ? Integer.MAX_VALUE : priceInfo.totalPrice();
      if (totalPrice == Integer.MAX_VALUE) {
         return Integer.MAX_VALUE;
      }

      return Math.max(1, Math.round(totalPrice / (float) Math.max(1, stack.getCount())));
   }

   private AuctionPriceInfo extractAuctionPriceInfo(ItemStack stack) {
      int unitPrice = Integer.MAX_VALUE;
      int totalPrice = Integer.MAX_VALUE;
      boolean bidOnly = false;
      if (stack == null || stack.isEmpty()) {
         return new AuctionPriceInfo(totalPrice, unitPrice, false);
      }

      List<Component> tooltip = AutoBuyUtil.displayLines(stack);
      for (int i = tooltip.size() - 1; i >= 0; i--) {
         Component line = tooltip.get(i);
         String normalized = AutoBuyParsedItems.normalizeDisplayName(line.getString())
            .toLowerCase(Locale.ROOT)
            .replace('ё', 'е')
            .replace("в¤", "¤");
         if (normalized.contains("сделать ставку") || normalized.contains("place a bid")) {
            bidOnly = true;
         }
         if (isUnitAuctionPriceLine(normalized) || hasExtraUnitPriceMarkers(normalized)) {
            int parsedPrice = parseTooltipPrice(normalized, UNIT_PRICE_PATTERN);
            if (parsedPrice != Integer.MAX_VALUE) {
               unitPrice = parsedPrice;
            }
            continue;
         }
         if (isTotalAuctionPriceLine(normalized)) {
            int parsedPrice = parseTooltipPrice(normalized, TOTAL_PRICE_PATTERN);
            if (parsedPrice != Integer.MAX_VALUE) {
               totalPrice = parsedPrice;
            }
         }
      }

      return new AuctionPriceInfo(totalPrice, unitPrice, bidOnly);
   }

   private boolean isUnitAuctionPriceLine(String normalized) {
      return normalized.contains("цена за 1 ед")
         || normalized.contains("цена за 1 шт")
         || normalized.contains("price per 1")
         || normalized.contains("price for 1");
   }

   private boolean isTotalAuctionPriceLine(String normalized) {
      return (normalized.contains("цена:") || normalized.contains("price:"))
         && !isUnitAuctionPriceLine(normalized);
   }

   private int parseTooltipPrice(String normalized, Pattern pattern) {
      if (normalized == null || normalized.isBlank()) {
         return Integer.MAX_VALUE;
      }

      Matcher matcher = pattern.matcher(normalized);
      if (!matcher.find()) {
         return Integer.MAX_VALUE;
      }

      String amount = matcher.group(1).replace(" ", "").replace(",", "");
      try {
         return Integer.parseInt(amount);
      } catch (NumberFormatException ignored) {
         return Integer.MAX_VALUE;
      }
   }

   private boolean hasExtraUnitPriceMarkers(String normalized) {
      if (normalized == null || normalized.isBlank()) {
         return false;
      }

      return normalized.contains("цена за штуку")
         || normalized.contains("цена за ед")
         || normalized.contains("price per item")
         || normalized.contains("price for item");
   }

   private boolean hasAuctionContentsLoaded(List<ItemStack> slotStacks) {
      int listingEnd = Math.min(AUCTION_LISTING_SLOT_COUNT, slotStacks.size());
      for (int slotIndex = 0; slotIndex < listingEnd; slotIndex++) {
         ItemStack stack = slotStacks.get(slotIndex);
         if (stack != null && !stack.isEmpty()) {
            return true;
         }
      }

      return findRefreshButtonIndex(slotStacks) >= 0;
   }

   private int findRefreshButtonIndex(List<ItemStack> slotStacks) {
      int controlEnd = Math.min(AUCTION_CONTROL_ROW_END, slotStacks.size() - 1);
      for (int slotIndex = AUCTION_CONTROL_ROW_START; slotIndex <= controlEnd; slotIndex++) {
         ItemStack stack = slotStacks.get(slotIndex);
         if (stack == null || stack.isEmpty()) {
            continue;
         }
         if (isRefreshAuctionButton(slotIndex, stack)) {
            return slotIndex;
         }
      }

      return -1;
   }

   private boolean isRefreshAuctionButton(int slotIndex, ItemStack stack) {
      String normalizedName = normalizeStackName(stack);
      return (stack.getItem() == Items.EMERALD && normalizedName.contains("обновить аукцион"))
         || (slotIndex == 47 && normalizedName.contains("обновить аукцион"));
   }

   private int findConfirmButtonIndex(List<ItemStack> slotStacks) {
      for (int slotIndex = 0; slotIndex < slotStacks.size(); slotIndex++) {
         ItemStack stack = slotStacks.get(slotIndex);
         if (stack == null || stack.isEmpty()) {
            continue;
         }
         if (isConfirmButton(slotIndex, stack)) {
            return slotIndex;
         }
      }

      return -1;
   }

   private boolean isConfirmButton(int slotId, ItemStack stack) {
      String normalizedName = normalizeStackName(stack);
      return (stack.getItem() == Items.STAINED_GLASS_PANE.lime() && normalizedName.contains("купить"))
         || (slotId == 11 && normalizedName.contains("купить"));
   }

   private int findCancelButtonIndex(List<ItemStack> slotStacks) {
      for (int slotIndex = 0; slotIndex < slotStacks.size(); slotIndex++) {
         ItemStack stack = slotStacks.get(slotIndex);
         if (stack == null || stack.isEmpty()) {
            continue;
         }
         if (isCancelButton(slotIndex, stack)) {
            return slotIndex;
         }
      }

      return -1;
   }

   private boolean isCancelButton(int slotId, ItemStack stack) {
      String normalizedName = normalizeStackName(stack);
      return stack.getItem() == Items.STAINED_GLASS_PANE.red()
         || normalizedName.contains("отмен")
         || normalizedName.contains("назад")
         || normalizedName.contains("cancel")
         || (slotId == 15 && stack.getItem() == Items.STAINED_GLASS_PANE.red());
   }

   private String normalizeStackName(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return "";
      }
      return AutoBuyParsedItems.normalizeDisplayName(stack.getHoverName().getString())
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е')
         .trim();
   }

   private void scheduleRefreshCycle(long now) {
      pendingRefreshClicks = chooseRefreshBurstCount();
      lastAuctionRefreshDelayMs = chooseRefreshLeadDelayMs(pendingRefreshClicks);
      if (rctRefreshCount == 0) {
         rctRefreshThreshold = chooseRctRefreshThreshold();
      }
      nextRefreshAtMs = now + lastAuctionRefreshDelayMs;
      logDebug("REFRESH", "Scheduled refresh cycle: clicks=" + pendingRefreshClicks
         + ", leadDelayMs=" + lastAuctionRefreshDelayMs
         + ", nextAtMs=" + nextRefreshAtMs);
   }

   private void handlePendingRandomRctReconnect(long now) {
      if (!waitingForRandomRct) {
         return;
      }

      if (pendingRctAnarchy <= 0) {
         clearRandomRctState();
         return;
      }

      if (mc == null || mc.player == null) {
         if (now >= pendingRctDeadlineAtMs) {
            clearRandomRctState();
         }
         return;
      }

      int currentAnarchy = ServerHandler.getInstance().getAnarchy();
      if (currentAnarchy == pendingRctAnarchy && ServerHandler.getInstance().isHolyWorld()) {
         completeRandomRctTransition(now);
         notifyAutoBuy("Перехожу на анархию " + currentAnarchy + ".");
         return;
      }

      if (now >= pendingRctDeadlineAtMs) {
         clearRandomRctState();
      }
   }

   private void completeRandomRctTransition(long now) {
      waitingForRandomRct = false;
      pendingRctAnarchy = -1;
      pendingRctDeadlineAtMs = 0L;
      state = AutoBuyState.WAIT_AUCTION;
      pendingAuctionScan = false;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      pageActionAtMs = 0L;
      pageLoadDeadlineAtMs = 0L;
      auctionOpenDeadlineAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
      nextActionAtMs = earliestAuctionOpenAt(now);
      lastRefreshSignature = null;
      staleRefreshCount = 0;
   }

   public boolean isRandomRctTransitionActive() {
      return waitingForRandomRct && pendingRctAnarchy > 0;
   }

   public void finishRandomRctReconnect() {
      if (!isRandomRctTransitionActive()) {
         return;
      }

      if (isEnabled()) {
         completeRandomRctTransition(System.currentTimeMillis());
         return;
      }

      clearRandomRctState();
      setEnabled(true);
   }

   public void cancelRandomRctReconnect() {
      if (!isRandomRctTransitionActive()) {
         return;
      }

      clearRandomRctState();
   }

   private void triggerRandomRctReconnect(long now) {
      int targetAnarchy = chooseRandomRctAnarchy();
      if (targetAnarchy <= 0) {
         clearRandomRctState();
         return;
      }

      notifyAutoBuy("Перехожу на анархию " + targetAnarchy + "...");
      pendingRctAnarchy = targetAnarchy;
      scheduleNextAutoRct(now);
      pendingRctDeadlineAtMs = now + RCT_RECONNECT_TIMEOUT_MS;
      waitingForRandomRct = true;
      rctRefreshCount = 0;
      rctRefreshThreshold = chooseRctRefreshThreshold();
      rctTransitionAtMs = 0L;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      pendingAuctionScan = false;
      state = AutoBuyState.WAIT_AUCTION;
      nextActionAtMs = now + AUCTION_OPEN_TIMEOUT_MS;
      pageActionAtMs = 0L;
      pageLoadDeadlineAtMs = 0L;
      auctionOpenDeadlineAtMs = pendingRctDeadlineAtMs;
      lastRefreshSignature = null;
      staleRefreshCount = 0;
      if (mc != null && mc.player != null && mc.player.containerMenu != null && mc.player.containerMenu.containerId != 0) {
         closeHandledScreen(now);
      }
      RCTRepository.getInstance().reconnect(targetAnarchy);
   }

   private void clearRandomRctState() {
      waitingForRandomRct = false;
      pendingRctAnarchy = -1;
      pendingRctDeadlineAtMs = 0L;
   }

   private int chooseRandomRctAnarchy() {
      int currentAnarchy = ServerHandler.getInstance().getAnarchy();
      List<Integer> pool = new ArrayList<>();
      for (int i = 18; i <= RCTRepository.MAX_ANARCHY; i++) {
         if (i != currentAnarchy) {
            pool.add(i);
         }
      }
      if (pool.isEmpty()) {
         return -1;
      }
      return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
   }

   private boolean shouldTriggerAutoRct(long now) {
      if (!autoRct.getValue()) {
         return false;
      }
      if (rctRefreshCount >= Math.round(rctRefreshCap.getValue())) {
         return true;
      }
      return nextAutoRctAtMs > 0L && now >= nextAutoRctAtMs;
   }

   private void scheduleNextAutoRct(long now) {
      float minM = AutoBuyModule.getInstance().rctMinMinutes.getValue();
      float maxM = AutoBuyModule.getInstance().rctMaxMinutes.getValue();
      long intervalMs = Math.round(ThreadLocalRandom.current().nextDouble(minM, maxM + 0.1) * 60_000L);
      nextAutoRctAtMs = now + intervalMs;
   }

   private boolean tryFastPath(AutoBuyScreenMirror.Snapshot snapshot, long now) {
      if (snapshot == null || !snapshot.isOpen()) {
         return false;
      }

      if (!fastPathBusy.compareAndSet(false, true)) {
         return false;
      }

      try {
         if (snapshot.isConfirm()) {
            return fastConfirmPurchase(snapshot, now);
         }
         if (snapshot.isAuction()) {
            return fastScanAuction(snapshot, now);
         }
         return false;
      } finally {
         fastPathBusy.set(false);
      }
   }

   private boolean fastScanAuction(AutoBuyScreenMirror.Snapshot snapshot, long now) {
      if (state != AutoBuyState.WAIT_AUCTION || !pendingAuctionScan || !auctionSortReady) {
         return false;
      }

      List<AutoBuyParsedItemData> enabledItems = cachedEnabledItems;
      if (enabledItems.isEmpty()) {
         return false;
      }

      PurchaseCandidate candidate = findBestCandidate(enabledItems, snapshot.slots());
      if (candidate == null) {
         return false;
      }

      pendingAuctionScan = false;
      lastAuctionActivityAtMs = now;
      currentCandidate = candidate;
      state = AutoBuyState.WAIT_CONFIRM;
      confirmDeadlineAtMs = now + CONFIRM_TIMEOUT_MS;
      pendingRefreshClicks = 0;
      nextRefreshAtMs = 0L;
      logDebug("FAST", "Packet-driven lot click: " + describeCandidate(candidate));
      sendClickPacket(snapshot, candidate.slotId());
      notifyAutoBuy("Нашёл лот: " + formatItemName(candidate.item()) + " за " + candidate.unitPrice() + ", открываю покупку.");
      return true;
   }

   private boolean fastConfirmPurchase(AutoBuyScreenMirror.Snapshot snapshot, long now) {
      if (state != AutoBuyState.WAIT_CONFIRM || currentCandidate == null) {
         return false;
      }

      int confirmSlotId = findConfirmButtonIndex(snapshot.slots());
      if (confirmSlotId < 0 || !hasMatchingConfirmItem(currentCandidate, snapshot.slots())) {
         return false;
      }

      logDebug("FAST", "Packet-driven confirm click slotId=" + confirmSlotId);
      sendClickPacket(snapshot, confirmSlotId);
      recordConfirmedPurchase(now);
      return true;
   }

   private void sendClickPacket(AutoBuyScreenMirror.Snapshot snapshot, int slotId) {
      if (mc == null || mc.getConnection() == null || slotId < 0) {
         return;
      }

      ServerboundContainerClickPacket packet = new ServerboundContainerClickPacket(
         snapshot.syncId(),
         snapshot.revision(),
         (short) slotId,
         (byte) 0,
         ContainerInput.PICKUP,
         Int2ObjectMaps.emptyMap(),
         HashedStack.EMPTY
      );
      AutoBuyRefreshProfile.runWithoutRecording(() -> mc.getConnection().send(packet));
   }

   private void clickAuctionSlot(int slotId) {
      clickSlot(slotId);
   }

   private void clickSlot(int slotId) {
      if (mc == null || mc.player == null || mc.gameMode == null || mc.player.containerMenu == null) {
         return;
      }

      AbstractContainerMenu handler = mc.player.containerMenu;
      int containerId = handler.containerId;
      logDebug("CLICK", "clickSlot slotId=" + slotId + ", containerId=" + containerId);
      ItemStack cursorBefore = handler.getCarried().copy();
      Slot clickedSlot = slotId >= 0 && slotId < handler.slots.size() ? handler.slots.get(slotId) : null;
      ItemStack slotBefore = clickedSlot != null ? clickedSlot.getItem().copy() : ItemStack.EMPTY;

      AutoBuyRefreshProfile.runWithoutRecording(() -> mc.gameMode.handleContainerInput(
         containerId,
         slotId,
         0,
         ContainerInput.PICKUP,
         mc.player
      ));

      handler.setCarried(cursorBefore);
      if (clickedSlot != null) {
         clickedSlot.set(slotBefore);
      }
   }

   private boolean matchesAuctionStack(AutoBuyParsedItemData item, ItemStack stack, int durabilityRequirement) {
      return AutoBuyParsedItems.matchesAuctionStack(item, stack, durabilityRequirement)
         || AutoBuyParsedItems.matchesAuctionStack(item, stack, durabilityRequirement, true);
   }

   private boolean isCandidateStillValid(PurchaseCandidate candidate, List<ItemStack> slotStacks) {
      if (candidate == null || slotStacks == null || candidate.slotId() >= slotStacks.size()) {
         return false;
      }
      ItemStack current = slotStacks.get(candidate.slotId());
      if (current == null || current.isEmpty()) {
         return false;
      }
      return matchesAuctionStack(candidate.item(), current, resolveDurabilityRequirement(candidate.item()));
   }

   private boolean hasMatchingConfirmItem(PurchaseCandidate candidate, List<ItemStack> slotStacks) {
      if (candidate == null || slotStacks == null) {
         return false;
      }
      for (ItemStack stack : slotStacks) {
         if (stack != null && !stack.isEmpty() && matchesAuctionStack(candidate.item(), stack, resolveDurabilityRequirement(candidate.item()))) {
            return true;
         }
      }
      return false;
   }

   private boolean matchesSmartResaleMarketItem(ResaleTarget target, ItemStack stack) {
      if (target == null || stack == null || stack.isEmpty()) {
         return false;
      }
      if (target.item() != null) {
         return AutoBuyParsedItems.matchesAuctionStack(target.item(), stack, resolveDurabilityRequirement(target.item()));
      }
      ItemStack ref = target.referenceStack();
      return ref != null && !ref.isEmpty() && stack.getItem() == ref.getItem();
   }

   private boolean hasAtLeastTotemsOrTalismans(int count) {
      if (mc == null || mc.player == null) return false;
      int c = 0;
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (stack != null && !stack.isEmpty() && (stack.getItem() == Items.TOTEM_OF_UNDYING || normalizeStackName(stack).contains("талисман"))) {
            c += stack.getCount();
            if (c >= count) return true;
         }
      }
      return false;
   }

   private boolean isSmartResaleTarget(ResaleTarget target) {
      if (target == null || target.referenceStack() == null) return false;
      Item item = target.referenceStack().getItem();
      return item == Items.TOTEM_OF_UNDYING || normalizeStackName(target.referenceStack()).contains("талисман");
   }

   private int resolveAuctionTotalPrice(ItemStack stack, AuctionPriceInfo priceInfo, int unitPrice) {
      if (priceInfo != null && priceInfo.totalPrice() != Integer.MAX_VALUE) {
         return priceInfo.totalPrice();
      }
      return unitPrice * Math.max(1, stack.getCount());
   }

   private Map<String, List<AutoBuyParsedItemData>> indexEnabledItemsByItemId(List<AutoBuyParsedItemData> items) {
      Map<String, List<AutoBuyParsedItemData>> index = new HashMap<>();
      for (AutoBuyParsedItemData item : items) {
         if (item.itemId() != null) {
            index.computeIfAbsent(item.itemId(), k -> new ArrayList<>()).add(item);
         }
      }
      return index;
   }

   private List<ItemStack> handlerSlotStacks() {
      if (mc.player == null || mc.player.containerMenu == null) {
         return Collections.emptyList();
      }
      List<ItemStack> list = new ArrayList<>(mc.player.containerMenu.slots.size());
      for (Slot slot : mc.player.containerMenu.slots) {
         list.add(slot == null ? ItemStack.EMPTY : slot.getItem());
      }
      return list;
   }

   private ItemStack stackOrNull(Slot slot) {
      return slot == null ? ItemStack.EMPTY : slot.getItem();
   }

   private String computeAuctionPageSignature(List<ItemStack> slotStacks) {
      if (slotStacks == null || slotStacks.isEmpty()) return "";
      StringBuilder sb = new StringBuilder();
      int end = Math.min(AUCTION_LISTING_SLOT_COUNT, slotStacks.size());
      for (int i = 0; i < end; i++) {
         ItemStack st = slotStacks.get(i);
         if (st != null && !st.isEmpty()) {
            sb.append(BuiltInRegistries.ITEM.getKey(st.getItem())).append(':').append(st.getCount()).append(';');
         }
      }
      return sb.toString();
   }

   private int chooseRefreshBurstCount() {
      return ThreadLocalRandom.current().nextInt(1, 4);
   }

   private long chooseRefreshLeadDelayMs(int clicks) {
      float minD = refreshMinDelay.getValue();
      float maxD = refreshMaxDelay.getValue();
      return Math.round(ThreadLocalRandom.current().nextDouble(minD, maxD + 0.1));
   }

   private long nextBurstSpacingMs() {
      return ThreadLocalRandom.current().nextLong(REFRESH_BURST_SPACING_FAST_MIN_MS, REFRESH_BURST_SPACING_FAST_MAX_MS + 1L);
   }

   private int chooseResaleBatchThreshold() {
      return ThreadLocalRandom.current().nextInt(RESALE_BATCH_TRIGGER_MIN, RESALE_BATCH_TRIGGER_MAX + 1);
   }

   private int chooseRctRefreshThreshold() {
      return ThreadLocalRandom.current().nextInt(RCT_FAST_REFRESH_TRIGGER_MIN, RCT_FAST_REFRESH_TRIGGER_MAX + 1);
   }

   private void resetResaleChatState() {
      resaleChatCommand = null;
      resaleChatNextState = null;
   }

   private List<AutoBuyParsedItemData> getEnabledParsedItems(boolean forceRefresh) {
      List<AutoBuyParsedItemData> enabled = getParsedItems(forceRefresh).stream()
         .filter(AutoBuyParsedItemData::enabled)
         .toList();
      cachedEnabledItems = enabled;
      return enabled;
   }

   private List<AutoBuyParsedItemData> getParsedItems(boolean forceRefresh) {
      long now = System.currentTimeMillis();
      if (forceRefresh || now >= parsedItemsCacheExpiresAtMs) {
         cachedParsedItems = AutoBuyParsedItems.loadItems();
         parsedItemsCacheExpiresAtMs = now + 1000L;
      }
      return cachedParsedItems;
   }

   public void invalidateParsedItemsCache() {
      parsedItemsCacheExpiresAtMs = 0L;
   }

   public LastPurchaseInfo getLastPurchaseInfo() {
      return lastPurchaseInfo;
   }

   public List<LastPurchaseInfo> getPurchaseHistory() {
      return purchaseHistory.stream().filter(Objects::nonNull).toList();
   }

   public long getSessionAutoBuyElapsedMs() {
      long elapsedMs = sessionAutoBuyElapsedMs;
      if (sessionAutoBuyStartedAtMs > 0L) {
         elapsedMs += Math.max(0L, System.currentTimeMillis() - sessionAutoBuyStartedAtMs);
      }
      return elapsedMs;
   }

   public boolean isSessionAutoBuyTimerRunning() {
      return sessionAutoBuyStartedAtMs > 0L;
   }

   public long getSessionPurchasedItemCount() {
      return sessionPurchasedItemCount;
   }

   public long getSessionSpentCoins() {
      return sessionSpentCoins;
   }

   public long getSessionEarnedCoins() {
      return sessionEarnedCoins;
   }

   public long getSessionNetProfit() {
      return sessionEarnedCoins - sessionSpentCoins;
   }

   private void logDebug(String stage, String message) {
      AutoBuyPacketDebugLogger.log(stage, "state=" + state + " | " + message + " | " + buildPacketContext());
   }

   private boolean shouldLogPacket(Packet<?> packet) {
      if (packet == null) {
         return false;
      }

      return packet instanceof ClientboundSystemChatPacket
         || packet instanceof ClientboundOpenScreenPacket
         || packet instanceof ClientboundContainerClosePacket
         || packet instanceof ServerboundContainerClickPacket
         || packet instanceof ServerboundContainerClosePacket
         || packet instanceof ServerboundSetCarriedItemPacket;
   }

   private String buildPacketContext() {
      return "screen=" + describeCurrentScreen()
         + ", pendingAuctionScan=" + pendingAuctionScan
         + ", pendingRefreshClicks=" + pendingRefreshClicks
         + ", currentCandidate=" + (currentCandidate == null ? "null" : currentCandidate.displayName() + "@" + currentCandidate.slotId())
         + ", pendingPurchase=" + (pendingPurchaseInfo == null ? "null" : pendingPurchaseInfo.itemName());
   }

   private String describeCurrentScreen() {
      if (mc == null || mc.player == null || mc.player.containerMenu == null) {
         return "none";
      }

      return mc.player.containerMenu.getClass().getSimpleName() + "#" + mc.player.containerMenu.containerId;
   }

   private String describeAuctionScreen() {
      if (mc == null || mc.player == null || mc.player.containerMenu == null) {
         return "screen=none";
      }

      return "containerId=" + mc.player.containerMenu.containerId
         + ", slots=" + mc.player.containerMenu.slots.size()
         + ", signature=" + computeAuctionPageSignature(handlerSlotStacks());
   }

   private String describeCandidate(PurchaseCandidate candidate) {
      if (candidate == null) {
         return "candidate=null";
      }

      return "item=" + candidate.displayName()
         + ", seller=" + candidate.sellerName()
         + ", slot=" + candidate.slotId()
         + ", count=" + candidate.count()
         + ", unitPrice=" + candidate.unitPrice()
         + ", totalPrice=" + candidate.totalPrice()
         + ", configuredPrice=" + candidate.configuredPrice();
   }

   private void notifyAutoBuy(String message) {
      TextUtil.sendMessage("§d[AutoBuy]§r " + message);
   }

   private String formatItemName(AutoBuyParsedItemData item) {
      if (item == null) {
         return "unknown";
      }

      String name = AutoBuyParsedItems.normalizeDisplayName(item.displayName());
      if (name.isBlank()) {
         name = AutoBuyParsedItems.normalizeDisplayName(item.baseName());
      }
      return name.isBlank() ? "unknown" : name;
   }

   private String formatResaleTargetName(ResaleTarget target) {
      if (target == null) {
         return "unknown";
      }
      if (target.item() != null) {
         return formatItemName(target.item());
      }

      ItemStack referenceStack = target.referenceStack();
      if (referenceStack != null && !referenceStack.isEmpty()) {
         String name = AutoBuyParsedItems.normalizeDisplayName(referenceStack.getHoverName().getString());
         if (!name.isBlank()) {
            return name;
         }
      }
      return "unknown";
   }

   private String normalizeIncomingChat(String text) {
      return AutoBuyParsedItems.normalizeDisplayName(text)
         .toLowerCase(Locale.ROOT)
         .replace('ё', 'е')
         .replaceAll("\\s{2,}", " ")
         .trim();
   }

   private boolean isSuccessfulPurchaseMessage(String message) {
      return message.contains("вы купили")
         || message.contains("вы приобрели")
         || message.contains("you bought")
         || message.contains("you purchased");
   }

   private void resetRuntimeState() {
      AutoBuyScreenMirror.INSTANCE.reset();
      state = AutoBuyState.IDLE;
      nextActionAtMs = 0L;
      auctionOpenDeadlineAtMs = 0L;
      pageActionAtMs = 0L;
      pageLoadDeadlineAtMs = 0L;
      auctionSortActionAtMs = 0L;
      auctionExpectedSort = null;
      auctionSortReady = false;
      confirmDeadlineAtMs = 0L;
      nextRefreshAtMs = 0L;
      lastHandledScreenCloseAtMs = 0L;
      pendingRefreshClicks = 0;
      pendingAuctionScan = false;
      lastRefreshSignature = null;
      staleRefreshCount = 0;
      lastAuctionActivityAtMs = 0L;
      currentCandidate = null;
      lastPurchasedPath = null;
      pendingPurchaseInfo = null;
      clearPendingPurchaseTracking();
      pendingResaleTarget = null;
      activeResaleTarget = null;
      pendingUnsalableHotbarMoveTarget = null;
      forceSmartResaleForCurrentTarget = false;
      resaleQueue.clear();
      deferredResaleQueue.clear();
      resaleBatchThreshold = chooseResaleBatchThreshold();
      resaleDeadlineAtMs = 0L;
      resaleAttempts = 0;
      resaleListedThisCycle = 0;
      retryingDeferredResalePass = false;
      clearedListingsThisCycle = 0;
      offlineSaleSummaryIgnoreUntilMs = 0L;
      resetResaleResultFlags();
      resetResaleChatState();
      lastPacketDrivenAuctionScanAtMs = 0L;
      lastPacketDrivenConfirmAttemptAtMs = 0L;
   }

   private void resumeSessionAutoBuyTimer(long now) {
      if (sessionAutoBuyStartedAtMs <= 0L) {
         sessionAutoBuyStartedAtMs = now;
      }
   }

   private void pauseSessionAutoBuyTimer(long now) {
      if (sessionAutoBuyStartedAtMs <= 0L) {
         return;
      }

      sessionAutoBuyElapsedMs += Math.max(0L, now - sessionAutoBuyStartedAtMs);
      sessionAutoBuyStartedAtMs = 0L;
   }

   public enum AutoBuyState {
      IDLE,
      WAIT_AUCTION,
      WAIT_CONFIRM,
      WAIT_REOPEN,
      WAIT_RESALE_ITEM,
      WAIT_SMART_RESALE_AUCTION,
      TYPE_RESALE_SELL_COMMAND,
      WAIT_RESALE_CONFIRM,
      TYPE_RESALE_CONFIRM_COMMAND,
      WAIT_RESALE_RESULT,
      WAIT_RESALE_CLEAR_AUCTION,
      WAIT_RESALE_OWN_LISTINGS,
      CLAIM_RESALE_OWN_LISTINGS
   }

   public record PurchaseCandidate(
      AutoBuyParsedItemData item,
      int slotId,
      int unitPrice,
      int configuredPrice,
      int totalPrice,
      int count,
      String displayName,
      String sellerName,
      ItemStack iconStack
   ) {}

   public record ResaleTarget(
      AutoBuyParsedItemData item,
      ItemStack referenceStack,
      int count,
      int preInventoryCount,
      int purchaseTotalPrice
   ) {}

   public record LastPurchaseInfo(
      String itemName,
      int count,
      String sellerName,
      int totalPrice,
      ItemStack iconStack
   ) {}

   private record AuctionPriceInfo(
      int totalPrice,
      int unitPrice,
      boolean bidOnly
   ) {}
}
