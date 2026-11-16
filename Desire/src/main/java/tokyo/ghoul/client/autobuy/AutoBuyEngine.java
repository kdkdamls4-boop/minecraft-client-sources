package tokyo.ghoul.client.autobuy;

import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.misc.FlowAutoBuyModule;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.PacketEvent;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public class AutoBuyEngine implements QuickImports {
   public static final AutoBuyEngine INSTANCE = new AutoBuyEngine();

   public static AutoBuyEngine getInstance() {
      return INSTANCE;
   }
   private static final Random RNG = ThreadLocalRandom.current();
   public final PriceAutoParser parser = new PriceAutoParser();
   public final AutoSeller seller = new AutoSeller();
   public final RelistManager relist = new RelistManager();
   public final AutoPay autoPay = new AutoPay();
   private int relistCooldown = -1;
   private int relistCheckTimer = -1;
   private boolean relistPending = false;
   private static final Pattern BALANCE_PATTERN = Pattern.compile(
      "(?iu)(?:баланс|на\\s+счету|на\\s+счёте|на\\s+счете|ваш\\s+счёт|ваш\\s+счет)[^\\d\\-]*([\\d\\s\\u00A0.,]+)"
   );
   private int buyCooldownTicks = 0;
   private long lastSlotsHash = 0L;
   private int unchangedHashTicks = 0;
   private int refreshCooldownTicks = 0;
   private int commandCooldownTicks = 0;
   private int noScreenTicks = 0;
   private volatile String targetedItemId = null;
   private volatile String targetedDisplayName = null;
   private volatile String targetedLoreKeyword = null;
   private volatile long targetedMaxUnitPrice = 0L;
   private volatile boolean targetedRequireCustom = false;
   private volatile List<Marker> targetedMarkers = null;
   private volatile String targetedSeller = null;
   private volatile boolean targetedSuspicious = false;
   private volatile boolean pendingPurchaseSuspicious = false;
   private long lastSuspiciousEscapeTime = 0L;
   private int confirmActionCooldown = 0;
   private boolean awaitingInventoryArrival = false;
   private String awaitingItemId = null;
   private int preBuyInventoryCount = 0;
   private int arrivalTimeoutTicks = 0;
   private PurchaseRecord pendingPurchaseRecord = null;
   private static final int DEFAULT_MAX_ARRIVAL_TIMEOUT = 20;
   private static final int DEFAULT_CLOSE_SCREEN_DELAY = 6;
   private float targetAfkYaw = Float.NaN;
   private float targetAfkPitch = Float.NaN;
   private int afkRotateTicks = 0;
   private int afkPauseTicks = 0;
   private boolean isAfkRotating = false;
   private boolean afkInitDone = false;
   private static final float BASE_AFK_SPEED = 8.0F;
   private float currentAfkSpeed = 8.0F;
   private int pauseBeforeActionTicks = 0;
   private int randomHoverSlot = -1;
   private int randomHoverTicks = 0;
   private int hoverResetDelay = 0;
   private int randomMicroPauseTicks = 0;
   private float speedJitterMultiplier = 1.0F;
   private int speedJitterTimer = 0;
   private int refreshThrottleCounter = 0;
   private static final int MAX_HISTORY_SIZE = 100;

   private boolean registered = false;

   private AutoBuyEngine() {
   }

   public void register() {
      if (this.registered) {
         return;
      }
      this.registered = true;
      UpdateEvent.getInstance().subscribe(new Listener<>(event -> this.onUpdate()));
      PacketEvent.getInstance().subscribe(new Listener<>(this::onPacket));
   }

   public void onUpdate() {
      if (mc.player != null && mc.gameMode != null) {
         try {
            FlowAutoBuyModule.getInstance().syncSettings();
         } catch (Throwable ignored) {
         }
         AutoBuyConfig cfg = AutoBuyConfig.cfg();
         if (cfg.autoPay) {
            try {
               this.autoPay.tick();
            } catch (Throwable ignored) {
            }
         }

         this.speedJitterTimer++;
         if (this.speedJitterTimer >= 3) {
            this.speedJitterTimer = 0;
            this.speedJitterMultiplier = 0.9F + RNG.nextFloat() * 0.3F;
         }

         if (this.randomMicroPauseTicks > 0) {
            this.randomMicroPauseTicks--;
         } else {
            try {
               this.parser.tick();
            } catch (Throwable ignored) {
            }

            if (!this.parser.isActive()) {
               if (cfg.enabled) {
                  if (!this.afkInitDone) {
                     this.afkPauseTicks = randomBetween(2, 20);
                     this.currentAfkSpeed = 8.0F * (0.9F + RNG.nextFloat() * 0.2F) * this.speedJitterMultiplier;
                     this.afkInitDone = true;
                  }

                  this.tickAfkCamera();
               } else if (this.afkInitDone) {
                  this.isAfkRotating = false;
                  this.targetAfkYaw = Float.NaN;
                  this.targetAfkPitch = Float.NaN;
                  this.afkInitDone = false;
               }

               if (cfg.enabled) {
                  if (this.pauseBeforeActionTicks > 0) {
                     this.pauseBeforeActionTicks--;
                  } else {
                     try {
                        if (RNG.nextInt(2) == 0) {
                           this.relist.tick();
                        }
                     } catch (Throwable ignored) {
                     }

                     if (!this.relist.isActive()) {
                        if (cfg.autoSell) {
                           try {
                              if (RNG.nextInt(2) == 0) {
                                 this.seller.tick();
                              }
                           } catch (Throwable ignored) {
                           }
                        } else if (this.seller.isActive()) {
                           this.seller.stop();
                        }

                        if (!this.seller.isActive()) {
                           try {
                              this.checkAutoRelistTimer();
                           } catch (Throwable ignored) {
                           }

                           try {
                              this.processAuctionScreen();
                           } catch (Throwable ignored) {
                           }

                           try {
                              this.processConfirmationScreen();
                           } catch (Throwable ignored) {
                           }

                           try {
                              this.processAutoRefresh();
                           } catch (Throwable ignored) {
                           }

                           this.tickHoverSimulation();
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void processAutoRefresh() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (cfg.autoRefresh) {
         if (mc.gui.screen() instanceof ContainerScreen gcs) {
            if (isAuctionScreen(gcs)) {
               ChestMenu handler = gcs.getMenu();
               int lotSlotsCount = (handler.getRowCount() - 1) * 9;
               long hash = this.computeSlotsHash(handler, lotSlotsCount);
               this.refreshThrottleCounter++;
               if (this.refreshThrottleCounter >= -1) {
                  this.refreshThrottleCounter = 0;
                  if (hash != this.lastSlotsHash) {
                     this.lastSlotsHash = hash;
                     this.unchangedHashTicks = 0;
                     if (this.refreshCooldownTicks <= 0) {
                        this.triggerRefreshClick(handler, handler.containerId, handler.getRowCount());
                        this.refreshCooldownTicks = this.calcRefreshDelayTicks(AutoBuyConfig.cfg());
                     }
                  } else {
                     this.unchangedHashTicks++;
                     if (this.unchangedHashTicks >= 1 + RNG.nextInt(2) && this.refreshCooldownTicks <= 0) {
                        this.triggerRefreshClick(handler, handler.containerId, handler.getRowCount());
                        this.refreshCooldownTicks = this.calcRefreshDelayTicks(AutoBuyConfig.cfg());
                        this.unchangedHashTicks = 0;
                     }
                  }
               }
            }
         }
      }
   }

   private void tickHoverSimulation() {
      if (this.hoverResetDelay > 0) {
         this.hoverResetDelay--;
         if (this.hoverResetDelay == 0) {
            this.randomHoverSlot = -1;
            this.randomHoverTicks = 0;
         }
      } else if (RNG.nextInt(40) == 0) {
         this.hoverResetDelay = randomBetween(1, 2);
      }
   }

   private void tickAfkCamera() {
      if (mc.player != null) {
         if (RNG.nextInt(4) == 0) {
            this.currentAfkSpeed = 8.0F * (0.8F + RNG.nextFloat() * 0.4F) * this.speedJitterMultiplier;
         }

         if (this.isAfkRotating) {
            if (this.afkRotateTicks > 0) {
               this.afkRotateTicks--;
               float curYaw = mc.player.getYRot();
               float curPitch = mc.player.getXRot();
               float deltaYaw = Mth.wrapDegrees(this.targetAfkYaw - curYaw);
               float deltaPitch = Mth.wrapDegrees(this.targetAfkPitch - curPitch);
               float speedMod = 0.9F + RNG.nextFloat() * 0.2F;
               float stepYaw = Math.min(Math.abs(deltaYaw), this.currentAfkSpeed * speedMod) * Math.signum(deltaYaw);
               float stepPitch = Math.min(Math.abs(deltaPitch), this.currentAfkSpeed * 0.9F * speedMod) * Math.signum(deltaPitch);
               if (Math.abs(deltaYaw) < 0.5F && Math.abs(deltaPitch) < 0.5F) {
                  this.isAfkRotating = false;
                  this.afkPauseTicks = randomBetween(2, 30);
               } else {
                  mc.player.setYRot(curYaw + stepYaw);
                  mc.player.setXRot(curPitch + stepPitch);
               }
            } else {
               this.isAfkRotating = false;
               this.afkPauseTicks = randomBetween(2, 30);
            }
         } else if (this.afkPauseTicks > 0) {
            this.afkPauseTicks--;
         } else {
            this.pickNewAfkTarget();
         }
      }
   }

   private void pickNewAfkTarget() {
      if (mc.player != null) {
         float yaw = mc.player.getYRot();
         float pitch = mc.player.getXRot();
         this.targetAfkYaw = yaw + (RNG.nextFloat() * 160.0F - 80.0F);
         this.targetAfkPitch = Mth.clamp(pitch + (RNG.nextFloat() * 60.0F - 30.0F), -80.0F, 80.0F);
         this.afkRotateTicks = randomBetween(1, 10);
         this.isAfkRotating = true;
         this.currentAfkSpeed = 8.0F * (0.8F + RNG.nextFloat() * 0.4F) * this.speedJitterMultiplier;
      }
   }

   private void checkAutoRelistTimer() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (!cfg.enabled) {
         this.relistPending = false;
         this.relistCheckTimer = -1;
         this.relistCooldown = -1;
      } else {
         if (this.relistPending) {
            this.relistPending = false;
            cfg.enabled = true;
         }

         if (cfg.autoRelist) {
            if (this.relistCooldown < 0) {
               this.relistCooldown = Math.max(5, cfg.relistIntervalSeconds * 10) + randomBetween(-20, 50);
            }

            if (--this.relistCooldown <= 0) {
               int base = Math.max(5, cfg.relistIntervalSeconds * 10);
               this.relistCooldown = base + randomBetween(-base / 4, base / 3);
               if (RNG.nextInt(3) == 0) {
                  this.relistCooldown = this.relistCooldown + randomBetween(5, 20);
               } else {
                  this.relist.start();
               }

               return;
            }
         } else {
            this.relistCooldown = -1;
         }

         this.pauseBeforeActionTicks = Math.max(0, this.calcPreActionDelayTicks(cfg) - 1);
      }
   }

   public void onPlayerTeleport() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (cfg.enabled || (FlowAutoBuyModule.getInstance() != null && FlowAutoBuyModule.getInstance().isEnabled())) {
         cfg.enabled = false;
         if (FlowAutoBuyModule.getInstance() != null) {
            mc.execute(() -> FlowAutoBuyModule.getInstance().setEnabled(false));
         }
         AutoBuyConfig.save();
         this.resetPendingPurchase();
         if (mc != null && mc.player != null) {
            print("§c[AutoBuy] Персонаж телепортировался — AutoBuy отключен!");
         }
      }
   }

   public void onPacket(PacketEvent.PacketEventData event) {
      if (event.isReceive()) {
         if (event.packet() instanceof ClientboundPlayerPositionPacket
               || event.packet() instanceof ClientboundRespawnPacket) {
            this.onPlayerTeleport();
         }

         String rawMsg = this.extractMessage(event.packet());
         if (rawMsg != null) {
            AutoBuyConfig cfg = AutoBuyConfig.cfg();
            String msg = rawMsg.toLowerCase(Locale.ROOT);
            if (msg.contains("недостаточно средств")) {
               this.onInsufficientFunds();
            }

            if (msg.contains("не можете больше выставлять")) {
               this.seller.onAuctionFull();
            }

            Long saleEarned = AutoBuyReport.parseSalePrice(rawMsg);
            if (saleEarned != null && saleEarned > 0L) {
               AutoBuyReport.recordSale(saleEarned);
               this.seller.onSold();
            } else if (msg.contains("купил у вас") || msg.contains("купила у вас") || msg.contains("приобрел у вас")) {
               long fallbackPrice = this.seller.lastListedPrice;
               if (fallbackPrice > 0L) {
                  AutoBuyReport.recordSale(fallbackPrice);
               }
               this.seller.onSold();
            }

            Long buySpent = AutoBuyReport.parseBuyPrice(rawMsg);
            if (buySpent != null && buySpent > 0L) {
               AutoBuyReport.recordPurchase(buySpent);
               if (cfg.safeMode && (buySpent >= 1L && buySpent <= 15000L)) {
                  this.triggerSuspiciousEscape();
               }
            }

            if (msg.contains("успешно выставили")) {
               this.seller.onListed();
            }

            if (msg.contains("подождите") && msg.contains("для использования этой команды")) {
               this.seller.onCommandCooldown();
               this.commandCooldownTicks = Math.max(this.commandCooldownTicks, this.calcCommandCooldownTicks(cfg));
               if (cfg.enabled) {
                  PacketEvent.getInstance().setCancel(true);
                  return;
               }
            }

            if (cfg.enabled && ((msg.contains("вы уже") && msg.contains("аукцион")) || msg.contains("вы уже на аукционе"))) {
               PacketEvent.getInstance().setCancel(true);
               return;
            }

            if (this.autoPay.isWaitingForMoneyResponse()) {
               Long bal = parseBalance(rawMsg);
               if (bal != null) {
                  this.autoPay.setWaitingForMoneyResponse(false);
                  this.autoPay.onBalance(bal);
                  PacketEvent.getInstance().setCancel(true);
                  return;
               }
            }

            if (!msg.contains("пополнен") && !msg.contains("купил у вас")) {
               this.autoPay.onChatMessage(rawMsg);
            }
         }
      }
   }

   private String extractMessage(Packet<?> packet) {
      if (packet instanceof ClientboundSystemChatPacket g) {
         return g.content().getString();
      } else if (packet instanceof ClientboundDisguisedChatPacket p) {
         return p.message().getString();
      } else if (packet instanceof ClientboundSetActionBarTextPacket o) {
         return o.text().getString();
      } else if (packet instanceof ClientboundPlayerChatPacket c) {
         return c.unsignedContent() != null ? c.unsignedContent().getString() : (c.body() != null ? c.body().content() : null);
      }
      return null;
   }

   private void processAuctionScreen() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (this.buyCooldownTicks > 0) {
         this.buyCooldownTicks--;
      }

      if (this.refreshCooldownTicks > 0) {
         this.refreshCooldownTicks--;
      }

      if (this.commandCooldownTicks > 0) {
         this.commandCooldownTicks--;
      }

      if (RNG.nextInt(20) == 0) {
         this.randomMicroPauseTicks = randomBetween(1, 3);
      } else {
         Screen screen = mc.gui.screen();
         if (!(screen instanceof ContainerScreen gcs)) {
            this.unchangedHashTicks = 0;
            if (screen == null) {
               this.noScreenTicks++;
               if (this.noScreenTicks >= Math.max(1, this.calcCommandCooldownTicks(cfg) / 2)) {
                  this.reopenAuction();
               }
            } else {
               this.noScreenTicks = 0;
            }
         } else {
            this.noScreenTicks = 0;
            if (!isAuctionScreen(gcs)) {
               this.unchangedHashTicks = 0;
            } else if (this.buyCooldownTicks <= 0) {
               ChestMenu handler = gcs.getMenu();
               int syncId = handler.containerId;
               int rows = handler.getRowCount();
               int lotSlotsCount = (rows - 1) * 9;
               if (!this.checkAndCorrectSort(handler, syncId)) {
                  long hash = this.computeSlotsHash(handler, lotSlotsCount);
                  boolean hashChanged = hash != this.lastSlotsHash;
                  if (hashChanged) {
                     this.unchangedHashTicks = 0;
                     this.lastSlotsHash = hash;
                  } else {
                     this.unchangedHashTicks++;
                  }

                  if (RNG.nextInt(8) == 0) {
                     this.randomHoverSlot = RNG.nextInt(lotSlotsCount);
                     this.randomHoverTicks = randomBetween(1, 2);
                  }

                  if (this.randomHoverTicks > 0) {
                     this.randomHoverTicks--;
                  }

                  ArrayList<Lot> lots = new ArrayList<>();

                  for (int slot = 0; slot < lotSlotsCount; slot++) {
                     ItemStack stack = handler.getSlot(slot).getItem();
                     Lot lot = PriceParser.parse(slot, stack);
                     if (lot != null && !lot.barrier) {
                        lots.add(lot);
                     }
                  }

                  Lot chosenLot = null;
                  ItemEntry chosenEntry = null;

                  for (Lot lot : lots) {
                     ItemEntry entry = this.findBestMatchingEntry(cfg, lot);
                     if (entry != null) {
                        chosenLot = lot;
                        chosenEntry = entry;
                        break;
                     }
                  }

                  if (chosenLot != null && chosenEntry != null) {
                     this.targetedItemId = chosenLot.itemId;
                     this.targetedDisplayName = chosenEntry.displayName;
                     this.targetedLoreKeyword = chosenEntry.loreKeyword;
                     this.targetedMaxUnitPrice = chosenEntry.maxUnitPrice;
                     this.targetedRequireCustom = chosenEntry.requireCustom;
                     this.targetedMarkers = chosenEntry.resolveMarkers();
                     this.targetedSeller = (chosenLot.seller != null && !chosenLot.seller.isBlank()) ? chosenLot.seller : null;
                     this.targetedSuspicious = isSuspiciousPrice(chosenLot);
                     if (this.targetedSuspicious) {
                        AutoBuyOverlay.showSuspiciousNotification();
                        NotifyWidget.text("Подозрительная цена!");
                        if (mc.gui.hud != null) {
                           mc.gui.hud.setOverlayMessage(Component.literal("§c§lПодозрительная цена!"), false);
                        }
                     }
                     this.buyCooldownTicks = this.calcBuyClickDelayTicks(cfg);
                     if (RNG.nextInt(8) == 0) {
                        this.buyCooldownTicks = this.buyCooldownTicks + Math.max(1, this.calcPreActionDelayTicks(cfg));
                        this.targetedItemId = null;
                        this.targetedSeller = null;
                        this.targetedSuspicious = false;
                        return;
                     }

                     this.clickSlot(syncId, chosenLot.slot);
                  }
               }
            }
         }
      }
   }

   private boolean checkAndCorrectSort(ChestMenu handler, int syncId) {
      int hopperSlot = AuctionSortMode.findButton(handler);
      if (hopperSlot < 0) {
         return false;
      } else {
         AuctionSortMode currentMode = AuctionSortMode.current(handler.getSlot(hopperSlot).getItem());
         if (currentMode != null && currentMode != AuctionSortMode.NEWEST) {
            this.clickSlot(syncId, hopperSlot);
            this.buyCooldownTicks = Math.max(this.buyCooldownTicks, 6);
            this.lastSlotsHash = 0L;
            this.unchangedHashTicks = 0;
            return true;
         } else {
            return false;
         }
      }
   }

   private void reopenAuction() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (this.commandCooldownTicks <= 0 && mc.player != null && mc.player.connection != null) {
         mc.player.connection.sendCommand("ah");
         this.commandCooldownTicks = this.calcCommandCooldownTicks(cfg);
      }
   }

   private ItemEntry findBestMatchingEntry(AutoBuyConfig cfg, Lot lot) {
      String myName = mc.player.getGameProfile().name();
      if (myName != null && lot.seller != null && myName.equalsIgnoreCase(lot.seller)) {
         return null;
      }

      if (this.isSellerIgnored(cfg, lot)) {
         return null;
      }

      if (cfg.minPriceEnabled && cfg.minPrice > 0L && lot.unitPrice < cfg.minPrice && !isSuspiciousPrice(lot)) {
         return null;
      }

      ItemEntry bestEntry = null;
      int bestScore = -1;
      ArrayList<ItemEntry> candidates = new ArrayList<>(cfg.entries);
      if (RNG.nextInt(2) == 0) {
         Collections.shuffle(candidates, RNG);
      }

      for (ItemEntry e : candidates) {
         if (e.enabled && e.maxUnitPrice > 0L && ItemMatcher.matches(e, lot) && lot.unitPrice <= e.maxUnitPrice) {
            int score = (e.displayName != null ? e.displayName.length() * 2 : 0)
               + (e.loreKeyword != null && !e.loreKeyword.isBlank() ? randomBetween(15, 50) : 0)
               + (e.matchAnySword ? -randomBetween(10, 30) : 0)
               + (e.requireCustom ? randomBetween(10, 30) : 0)
               + (RNG.nextInt(20) - 10);
            if (score > bestScore) {
               bestScore = score;
               bestEntry = e;
            }
         }
      }

      return bestEntry;
   }

   public static boolean isAuctionScreen(ContainerScreen gcs) {
      if (gcs == null || gcs.getMenu() == null) {
         return false;
      }
      ChestMenu handler = gcs.getMenu();
      if (handler.getRowCount() < 3) {
         return false;
      }

      String title = PriceParser.stripFormatting(gcs.getTitle().getString()).toLowerCase(Locale.ROOT);
      return title.contains("аукцион") || title.contains("auction") || title.contains("рынок")
         || title.contains("поиск") || title.contains("хранилище");
   }

   private void triggerRefreshClick(ChestMenu handler, int syncId, int rows) {
      if (this.refreshCooldownTicks <= 0) {
         int total = rows * 9;
         int slot = -1;
         int cfgSlot = AutoBuyConfig.cfg().refreshSlot;
         if (cfgSlot >= 0 && cfgSlot < total) {
            ItemStack stack = handler.getSlot(cfgSlot).getItem();
            if (!stack.isEmpty()) {
               String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
               if (id.equals("minecraft:emerald")) {
                  slot = cfgSlot;
               }
            }
         }

         if (slot < 0) {
            slot = this.findSlotWithItem(handler, (rows - 1) * 9, total, "minecraft:emerald");
         }

         if (slot >= 0) {
            this.clickSlot(syncId, slot);
            this.refreshCooldownTicks = this.calcRefreshDelayTicks(AutoBuyConfig.cfg());
            this.lastSlotsHash = 0L;
            this.unchangedHashTicks = 0;
         }
      }
   }

   private int findSlotWithItem(ChestMenu handler, int from, int to, String itemId) {
      ArrayList<Integer> slots = new ArrayList<>();

      for (int i = from; i < to; i++) {
         slots.add(i);
      }

      if (RNG.nextInt(2) == 0) {
         Collections.shuffle(slots, RNG);
      }

      for (int slot : slots) {
         ItemStack stack = handler.getSlot(slot).getItem();
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (id.equals(itemId)) {
               return slot;
            }
         }
      }

      return -1;
   }

   private long computeSlotsHash(ChestMenu handler, int count) {
      long h = 1L;

      for (int i = 0; i < count; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         h = h * 31L + (stack.isEmpty() ? 0L : BuiltInRegistries.ITEM.getId(stack.getItem()) * 113L + stack.getCount());
      }

      return h;
   }

   private void processConfirmationScreen() {
      AutoBuyConfig cfg = AutoBuyConfig.cfg();
      if (this.awaitingInventoryArrival) {
         this.arrivalTimeoutTicks--;
         if (this.awaitingItemId != null && this.countItemInInventory(this.awaitingItemId) > this.preBuyInventoryCount) {
            String boughtId = this.awaitingItemId;
            boolean wasSuspicious = this.pendingPurchaseSuspicious;
            if (this.pendingPurchaseRecord != null) {
               this.pendingPurchaseRecord.success = true;
               AutoBuyConfig.cfg().addHistory(this.pendingPurchaseRecord);
            }

            this.notifyPurchaseSuccess();
            boolean autoSellEnabled = AutoBuyConfig.cfg().autoSell;
            this.resetPendingPurchase();
            if (wasSuspicious && cfg.safeMode) {
               this.triggerSuspiciousEscape();
            } else if (autoSellEnabled && boughtId != null) {
               this.seller.trigger();
            }
         } else if (this.arrivalTimeoutTicks <= 0) {
            this.resetPendingPurchase();
         }
      }

      if (this.confirmActionCooldown > 0) {
         this.confirmActionCooldown--;
      } else if (mc.gui.screen() instanceof ContainerScreen gcs && isConfirmScreen(gcs)) {
         if (cfg.enabled) {
            ChestMenu handler = gcs.getMenu();
            int syncId = handler.containerId;
            Lot confirmLot = this.findConfirmLot(handler);
            String targetId = this.targetedItemId;
            long maxPrice = this.targetedMaxUnitPrice;
            if (targetId == null || maxPrice <= 0L) {
               this.cancelConfirmation(handler, syncId);
               this.targetedItemId = null;
               this.targetedSeller = null;
               this.targetedSuspicious = false;
            } else if (confirmLot != null && targetId.equals(confirmLot.itemId)) {
               String reqKeyword = this.targetedLoreKeyword;
               String reqDisplay = this.targetedDisplayName;
               if (reqKeyword != null && !reqKeyword.isBlank()) {
                  boolean inLore = TextNormalizer.keywordIn(confirmLot.lore, reqKeyword);
                  boolean inName = TextNormalizer.keywordIn(confirmLot.name, reqKeyword);
                  if (!inLore && !inName) {
                     this.cancelConfirmation(handler, syncId);
                     this.targetedItemId = null;
                     this.targetedSeller = null;
                     return;
                  }

                  if (this.targetedRequireCustom && confirmLot.name != null && confirmLot.name.toLowerCase(Locale.ROOT).contains(reqKeyword.toLowerCase(Locale.ROOT))) {
                     this.cancelConfirmation(handler, syncId);
                     this.targetedItemId = null;
                     this.targetedSeller = null;
                     return;
                  }
               } else if (reqDisplay != null && !reqDisplay.isBlank() && !TextNormalizer.nameMatches(reqDisplay, confirmLot.name)) {
                  this.cancelConfirmation(handler, syncId);
                  this.targetedItemId = null;
                  this.targetedSeller = null;
                  return;
               }

               if (this.targetedRequireCustom && !confirmLot.hasMarkers) {
                  this.cancelConfirmation(handler, syncId);
                  this.targetedItemId = null;
                  this.targetedSeller = null;
               } else {
                  List<Marker> markers = this.targetedMarkers;
                  if (markers != null && !markers.isEmpty()) {
                     boolean matched = markers.stream().anyMatch(m -> ItemMatcher.markerMatches(m, confirmLot));
                     if (!matched) {
                        this.cancelConfirmation(handler, syncId);
                        this.targetedItemId = null;
                        this.targetedMarkers = null;
                        this.targetedSeller = null;
                        return;
                     }
                  } else if (this.targetedRequireCustom) {
                     this.cancelConfirmation(handler, syncId);
                     this.targetedItemId = null;
                     this.targetedMarkers = null;
                     this.targetedSeller = null;
                     return;
                  }

                  boolean ignoredSeller = this.isSellerIgnored(cfg, confirmLot)
                     || (this.targetedSeller != null && cfg.ignored(this.targetedSeller))
                     || (confirmLot != null && confirmLot.lore != null && this.loreContainsBannedSeller(cfg, confirmLot.lore));
                  if (ignoredSeller) {
                     this.cancelConfirmation(handler, syncId);
                     this.targetedItemId = null;
                     this.targetedSeller = null;
                     return;
                  }

                  if (confirmLot.unitPrice > maxPrice) {
                     this.cancelConfirmation(handler, syncId);
                     this.targetedItemId = null;
                     this.targetedSeller = null;
                  } else {
                     int confirmSlot = this.findConfirmGreenButton(handler);
                     if (confirmSlot >= 0) {
                        this.confirmActionCooldown = this.calcPreActionDelayTicks(cfg);
                        if (RNG.nextInt(10) == 0) {
                           this.cancelConfirmation(handler, syncId);
                           this.targetedItemId = null;
                           this.targetedSeller = null;
                           this.targetedSuspicious = false;
                           return;
                         }

                        boolean isSuspicious = this.targetedSuspicious || isSuspiciousPrice(confirmLot);
                        if (isSuspicious) {
                           AutoBuyOverlay.showSuspiciousNotification();
                           NotifyWidget.text("Подозрительная цена!");
                           if (mc.gui.hud != null) {
                              mc.gui.hud.setOverlayMessage(Component.literal("§c§lПодозрительная цена!"), false);
                           }
                        }
                        this.pendingPurchaseSuspicious = isSuspicious;

                        this.clickSlot(syncId, confirmSlot);
                        this.awaitingInventoryArrival = true;
                        this.awaitingItemId = confirmLot.itemId;
                        this.preBuyInventoryCount = this.countItemInInventory(confirmLot.itemId);
                        this.arrivalTimeoutTicks = 20 + RNG.nextInt(10);
                        long totalCost = confirmLot.totalPrice > 0L ? confirmLot.totalPrice : confirmLot.unitPrice * Math.max(1, confirmLot.count);
                        String label = confirmLot.name != null && !confirmLot.name.isBlank() ? confirmLot.name : confirmLot.itemId;
                        String finalSeller = (confirmLot.seller != null && !confirmLot.seller.isBlank())
                           ? confirmLot.seller
                           : (this.targetedSeller != null && !this.targetedSeller.isBlank() ? this.targetedSeller : "");
                        if (finalSeller.isBlank() && confirmLot.lore != null && !confirmLot.lore.isBlank()) {
                           finalSeller = PriceParser.extractSeller(confirmLot.lore);
                        }

                        if (!finalSeller.isBlank() && cfg.ignored(finalSeller)) {
                           this.cancelConfirmation(handler, syncId);
                           this.targetedItemId = null;
                           this.targetedSeller = null;
                           return;
                        }
                        this.pendingPurchaseRecord = new PurchaseRecord(
                           System.currentTimeMillis(), confirmLot.itemId, label, finalSeller, confirmLot.unitPrice, totalCost, confirmLot.count
                        );
                        this.targetedItemId = null;
                        this.targetedMaxUnitPrice = 0L;
                        this.targetedMarkers = null;
                        this.targetedSeller = null;
                        this.targetedSuspicious = false;
                     }
                  }
               }
            } else {
               this.cancelConfirmation(handler, syncId);
               this.targetedItemId = null;
               this.targetedSeller = null;
               this.targetedSuspicious = false;
            }
         }
      }
   }

   private void cancelConfirmation(ChestMenu handler, int syncId) {
      this.confirmActionCooldown = this.calcPreActionDelayTicks(AutoBuyConfig.cfg());
      this.targetedSeller = null;
      this.targetedSuspicious = false;
      this.clickCancelOrClose(handler, syncId);
   }

   public static boolean isConfirmScreen(ContainerScreen gcs) {
      if (gcs == null || gcs.getTitle() == null) {
         return false;
      }
      String title = PriceParser.stripFormatting(gcs.getTitle().getString()).toLowerCase(Locale.ROOT);
      return title.contains("покупка") || title.contains("подтвержд") || title.contains("купить");
   }

   private Lot findConfirmLot(ChestMenu handler) {
      int totalSlots = handler.getRowCount() * 9;
      int mid = RNG.nextInt(Math.max(1, totalSlots / 2));

      for (int i = mid; i < totalSlots; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!id.contains("stained_glass") && !id.contains("concrete") && !id.contains("wool")) {
               Lot lot = PriceParser.parse(i, stack);
               if (lot != null && !lot.barrier) {
                  return lot;
               }
            }
         }
      }

      for (int i = 0; i < mid; i++) {
         ItemStack stack = handler.getSlot(i).getItem();
         if (!stack.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (!id.contains("stained_glass") && !id.contains("concrete") && !id.contains("wool")) {
               Lot lot = PriceParser.parse(i, stack);
               if (lot != null && !lot.barrier) {
                  return lot;
               }
            }
         }
      }

      return null;
   }

   private int findConfirmGreenButton(ChestMenu handler) {
      int totalSlots = handler.getRowCount() * 9;
      int mid = RNG.nextInt(Math.max(1, totalSlots));

      for (int i = mid; i < totalSlots; i++) {
         String id = BuiltInRegistries.ITEM.getKey(handler.getSlot(i).getItem().getItem()).toString();
         if (id.contains("lime_stained_glass")
            || id.contains("green_stained_glass")
            || id.contains("lime_concrete")
            || id.contains("green_wool")
            || id.contains("lime_wool")) {
            return i;
         }
      }

      for (int i = 0; i < mid; i++) {
         String id = BuiltInRegistries.ITEM.getKey(handler.getSlot(i).getItem().getItem()).toString();
         if (id.contains("lime_stained_glass")
            || id.contains("green_stained_glass")
            || id.contains("lime_concrete")
            || id.contains("green_wool")
            || id.contains("lime_wool")) {
            return i;
         }
      }

      return -1;
   }

   private int findCancelRedButton(ChestMenu handler) {
      int totalSlots = handler.getRowCount() * 9;

      for (int i = 0; i < totalSlots; i++) {
         String id = BuiltInRegistries.ITEM.getKey(handler.getSlot(i).getItem().getItem()).toString();
         if (id.contains("red_stained_glass") || id.contains("red_concrete") || id.contains("red_wool")) {
            return i;
         }
      }

      return -1;
   }

   private void clickCancelOrClose(ChestMenu handler, int syncId) {
      int cancelSlot = this.findCancelRedButton(handler);
      if (cancelSlot < 0) {
         mc.player.closeContainer();
      } else {
         this.clickSlot(syncId, cancelSlot);
      }
   }

   public synchronized void onInsufficientFunds() {
      if (this.awaitingInventoryArrival) {
         if (this.pendingPurchaseRecord != null) {
            this.pendingPurchaseRecord.success = false;
            this.pendingPurchaseRecord.failReason = "недостаточно средств";
            AutoBuyConfig.cfg().addHistory(this.pendingPurchaseRecord);
         }

         this.sendMessage("§c[AutoBuy] Недостаточно средств — покупка отменена");
         this.resetPendingPurchase();
      }
   }

   private int countItemInInventory(String itemId) {
      if (mc.player != null && itemId != null) {
         Inventory inv = mc.player.getInventory();
         int total = 0;
         int size = inv.getContainerSize();
         int mid = RNG.nextInt(Math.max(1, size / 2));

         for (int i = mid; i < size; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
               total += stack.getCount();
            }
         }

         for (int i = 0; i < mid; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(itemId)) {
               total += stack.getCount();
            }
         }

         return total;
      } else {
         return 0;
      }
   }

   private void clickSlot(int syncId, int slot) {
      if (mc.gameMode != null && mc.player != null) {
         mc.gameMode.handleContainerInput(syncId, slot, 0, ContainerInput.PICKUP, mc.player);
      }
   }

   private void resetPendingPurchase() {
      this.awaitingInventoryArrival = false;
      this.awaitingItemId = null;
      this.preBuyInventoryCount = 0;
      this.arrivalTimeoutTicks = 0;
      this.pendingPurchaseRecord = null;
      this.pendingPurchaseSuspicious = false;
   }

   private void notifyPurchaseSuccess() {
      String name = this.pendingPurchaseRecord != null && this.pendingPurchaseRecord.label != null && !this.pendingPurchaseRecord.label.isBlank()
         ? this.pendingPurchaseRecord.label
         : (this.awaitingItemId == null ? "?" : this.awaitingItemId);
      int count = this.pendingPurchaseRecord != null ? this.pendingPurchaseRecord.count : 1;
      String seller = this.pendingPurchaseRecord != null ? this.pendingPurchaseRecord.seller : null;
      long price = this.pendingPurchaseRecord != null
         ? (this.pendingPurchaseRecord.totalPrice > 0L ? this.pendingPurchaseRecord.totalPrice : this.pendingPurchaseRecord.pricePerUnit * Math.max(1, count))
         : 0L;

      StringBuilder sb = new StringBuilder();
      sb.append("§a[AutoBuy] Куплено: §f").append(name);
      if (count > 1) {
         sb.append(" §7(×").append(count).append(")");
      }
      if (seller != null && !seller.isBlank()) {
         sb.append(" §7у игрока §f").append(seller);
      }
      if (price > 0L) {
         sb.append(" §7за §6").append(formatPrice(price)).append("$");
         AutoBuyReport.recordPurchase(price);
      }
      this.sendMessage(sb.toString());
   }

   private static String formatPrice(long price) {
      return String.format(Locale.ROOT, "%,d", price).replace(',', ' ');
   }

   private void sendMessage(String text) {
      if (mc.player != null) {
         mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(Component.literal(text)));
      }
   }

   public static boolean isSuspiciousPrice(Lot lot) {
      if (lot == null) {
         return false;
      }
      long total = lot.totalPrice > 0L ? lot.totalPrice : lot.unitPrice * Math.max(1, lot.count);
      long unit = lot.unitPrice;
      return (total >= 1L && total <= 15000L) || (unit >= 1L && unit <= 15000L);
   }

   public synchronized void triggerSuspiciousEscape() {
      long now = System.currentTimeMillis();
      if (now - this.lastSuspiciousEscapeTime < 3000L) {
         return;
      }
      this.lastSuspiciousEscapeTime = now;

      this.sendMessage("§c[AutoBuy] Подозрительная цена! Выход в /hub...");
      AutoBuyOverlay.showSuspiciousNotification();
      mc.execute(() -> {
         NotifyWidget.text("Подозрительная цена!");
         if (mc.gui.hud != null) {
            mc.gui.hud.setOverlayMessage(Component.literal("§c§lПодозрительная цена! Выход в /hub"), false);
         }

         if (mc.player != null) {
            mc.player.closeContainer();
            if (mc.player.connection != null) {
               mc.player.connection.sendCommand("hub");
            }
         }
      });
   }

   private static Long parseBalance(String raw) {
      return AutoPay.parseBalance(raw);
   }

   public void forceRefresh() {
      this.refreshCooldownTicks = 0;
      this.lastSlotsHash = 0L;
      this.unchangedHashTicks = 999;
      this.refreshThrottleCounter = 999;
      if (mc.gui.screen() instanceof ContainerScreen gcs && isAuctionScreen(gcs)) {
         ChestMenu handler = gcs.getMenu();
         this.triggerRefreshClick(handler, handler.containerId, handler.getRowCount());
      }
   }

   private int calcBuyClickDelayTicks(AutoBuyConfig cfg) {
      int delayMs = cfg.delayMsForStage(0.18F, 28, 210);
      int clickInterval = Math.max(0, cfg.clickIntervalTicks) * 16;
      return Math.max(1, this.millisToTicks((delayMs + clickInterval + RNG.nextInt(28)) / 100));
   }

   private int calcRefreshDelayTicks(AutoBuyConfig cfg) {
      if (cfg.refreshIntervalTicks > 0) {
         return Math.max(1, cfg.refreshIntervalTicks / 100);
      }

      int delayMs = cfg.delayMsForStage(0.45F, 18, 160);
      return Math.max(0, this.millisToTicks((delayMs + RNG.nextInt(20)) / 100));
   }

   private int calcPreActionDelayTicks(AutoBuyConfig cfg) {
      int delayMs = cfg.delayMsForStage(0.7F, 10, 130);
      return Math.max(0, this.millisToTicks((delayMs + RNG.nextInt(18)) / 100));
   }

   private int calcCommandCooldownTicks(AutoBuyConfig cfg) {
      int delayMs = cfg.delayMsForStage(1.0F, 200, 1150);
      return Math.max(1, this.millisToTicks(delayMs + RNG.nextInt(60)));
   }

   private int millisToTicks(int millis) {
      return (int)Math.ceil(Math.max(0, millis) / 50.0);
   }

   private boolean isSellerIgnored(AutoBuyConfig cfg, Lot lot) {
      if (lot == null || cfg == null || cfg.banList == null || cfg.banList.isEmpty()) {
         return false;
      }
      if (lot.seller != null && !lot.seller.isBlank() && cfg.ignored(lot.seller)) {
         return true;
      }
      if (this.targetedSeller != null && !this.targetedSeller.isBlank() && cfg.ignored(this.targetedSeller)) {
         return true;
      }
      if (lot.lore != null && !lot.lore.isBlank()) {
         if (lot.seller == null || lot.seller.isBlank()) {
            String extracted = PriceParser.extractSeller(lot.lore);
            if (!extracted.isBlank() && cfg.ignored(extracted)) {
               return true;
            }
         }
         if (this.loreContainsBannedSeller(cfg, lot.lore)) {
            return true;
         }
      }
      return false;
   }

   private boolean loreContainsBannedSeller(AutoBuyConfig cfg, String rawLore) {
      if (rawLore == null || rawLore.isBlank() || cfg == null || cfg.banList == null || cfg.banList.isEmpty()) {
         return false;
      }
      String extracted = PriceParser.extractSeller(rawLore);
      if (extracted != null && !extracted.isBlank() && cfg.ignored(extracted)) {
         return true;
      }

      String cleanLore = PriceParser.stripFormatting(rawLore).replace('\u00A0', ' ');
      String[] lines = cleanLore.split("\n");
      for (String rawLine : lines) {
         String line = rawLine.trim();
         if (line.isEmpty()) continue;
         String lineLower = line.toLowerCase(Locale.ROOT);

         boolean isSellerLine = lineLower.contains("продавец")
            || lineLower.contains("выставил")
            || lineLower.contains("продает")
            || lineLower.contains("продаёт")
            || lineLower.contains("владелец")
            || lineLower.contains("торговец")
            || lineLower.contains("seller")
            || lineLower.contains("owner")
            || lineLower.contains("игрок")
            || lineLower.contains("ник")
            || lineLower.contains("купить")
            || lineLower.contains("покупка")
            || lineLower.contains("покупаете")
            || lineLower.contains("приобрести")
            || lineLower.contains("продажа");

         if (!isSellerLine) continue;

         for (String banned : cfg.banList) {
            if (banned == null || banned.isBlank()) continue;
            String cleanBanned = AutoBuyConfig.cleanNickForCheck(banned);
            if (cleanBanned.isEmpty()) continue;

            if (AutoBuyConfig.containsWord(line.toLowerCase(Locale.ROOT), cleanBanned.toLowerCase(Locale.ROOT))) {
               return true;
            }

            String alphaOnly = line.replaceAll("[^A-Za-z0-9_]", " ").toLowerCase(Locale.ROOT);
            if (AutoBuyConfig.containsWord(alphaOnly, cleanBanned.toLowerCase(Locale.ROOT))) {
               return true;
            }
         }
      }
      return false;
   }

   private static int randomBetween(int min, int max) {
      return min >= max ? min : min + RNG.nextInt(max - min + 1);
   }
}
