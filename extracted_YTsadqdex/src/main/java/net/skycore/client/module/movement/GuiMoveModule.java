package net.skycore.client.module.movement;

import com.mojang.blaze3d.platform.InputConstants.Key;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ContainerClickEvent;
import net.skycore.client.event.PacketReceiveEvent;
import net.skycore.client.event.PacketSendEvent;
import net.skycore.client.event.PlayerInputEvent;
import net.skycore.client.event.PlayerTickEndEvent;
import net.skycore.client.mixin.KeyMappingGetKeyAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.KeyState;
import net.skycore.client.util.ConsumeSilentHelper;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.MovementInput;

public final class GuiMoveModule extends ClientModule {
   private boolean prevStealing;
   public boolean slow;
   private static final long HOLYWORLD_INPUT_DELAY_MS = 222L;
   private int pendingClickCounter;
   private long stealTimer;
   private boolean wasMoving;
   private static final long HOLYWORLD_MOVE_DELAY_MS = 125L;
   private static final long LONYGRIEF_INPUT_DELAY_MS = 50L;
   private boolean inventoryOpen;
   private int extraClickCount;
   private static final long FUNTIME_MOVE_DELAY_MS = 100L;
   public boolean bypassInventorySwap;
   private final List<ServerboundContainerClickPacket> pendingPackets;
   public static final GuiMoveModule INSTANCE = new GuiMoveModule();
   public final ModeSetting mode = this.add(new ModeSetting("Mode", "Vanilla", "Vanilla", "Reallyworld", "Funtime"));
   private int delayCounter;
   private static final long LONYGRIEF_MOVE_DELAY_MS = 50L;
   private int tickCounter;
   private static final long FUNTIME_INPUT_DELAY_MS = 190L;
   private static final long LONYGRIEF_FLUSH_DELAY_MS = 50L;
   private final Queue<ServerboundContainerClickPacket> packetQueue = new LinkedList<>();
   private boolean useDelay;
   private boolean isStealing;
   private final List<GuiMoveModule.PendingClick> pendingClicks = new ArrayList<>();

   private boolean isAnyModeActive() {
      if (!this.mode.is("Reallyworld")) {
         if (!this.mode.is("Funtime")) {
            if (!this.mode.is("Holyworld")) {
               if (!this.mode.is("SpookyTime")) {
                  if (!this.mode.is("LonyGrief")) {
                     return false;
                  }
               }
            }
         }
      }

      return true;
   }

   @Override
   protected void onDisable() {
      this.flushPacketQueue();
      this.tickCounter = 1;
      this.pendingClicks.clear();
      this.pendingPackets.clear();
      this.pendingClickCounter = 0;
      this.delayCounter = 0;
      this.extraClickCount = 0;
      this.prevStealing = false;
      this.isStealing = false;
      this.useDelay = false;
      this.inventoryOpen = false;
      this.packetQueue.clear();
      this.slow = false;
      this.bypassInventorySwap = false;
   }

   private void setExtraClickCount(ContainerInput extraClickCount2) {
      if (this.isEnabled()) {
         if (this.mode.is("SpookyTime")) {
            ContainerInput containerInput;
            if (extraClickCount2 == null) {
               containerInput = ContainerInput.PICKUP;
            } else {
               containerInput = extraClickCount2;
            }

            int clickPriority = this.getClickPriority(containerInput) + 1;
            this.extraClickCount = clickPriority;
         }
      }
   }

   private void resetStealTimer() {
      this.stealTimer = System.currentTimeMillis();
   }

   private void restoreClickState(GuiMoveModule.PendingClick pendingClick, AbstractContainerMenu abstractContainerMenu) {
      int value = 0;
      value = abstractContainerMenu.slots.size();
      var itemStacks = pendingClick.slots();
      int index = Math.min(value, itemStacks.size());

      for (int value2 = 0; value2 < index; value2++) {
         abstractContainerMenu.slots.get(value2).set(pendingClick.slots().get(value2).copy());
      }

      ItemStack itemStack = pendingClick.carried();
      abstractContainerMenu.setCarried(itemStack.copy());
   }

   private boolean isHoldingItem() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.gui.screen() instanceof AbstractContainerScreen && this.isMoving()) {
         if (instance.player != null) {
            if (!instance.player.containerMenu.getCarried().isEmpty()) {
               return true;
            }
         }
      }

      return false;
   }

   private GuiMoveModule.PendingClick createPendingClick(ContainerClickEvent containerClickEvent, AbstractContainerMenu abstractContainerMenu, Player player) {
      int value = 1;
      int value2 = 0;
      value = (byte)1;
      value2 = (byte)0;
      value2 = containerClickEvent.getContainerId();
      int slot = -1;
      if (value2 != abstractContainerMenu.containerId) {
         return null;
      }

      int value3 = 1;
      value3 = (byte)1;
      int size = abstractContainerMenu.slots.size();
      value3 = (byte)-1;
      ArrayList elements = new ArrayList(size);
      int value4 = 1;
      Iterator iterator = abstractContainerMenu.slots.iterator();

      while (true) {
         value4 = (byte)1;
         if (!iterator.hasNext()) {
            ItemStack carried = abstractContainerMenu.getCarried();
            value4 = (byte)1;
            ItemStack itemStack = carried.copy();
            slot = (byte)1;
            value = containerClickEvent.getSlotId();
            value3 = (byte)1;
            abstractContainerMenu.clicked(value, containerClickEvent.getButton(), containerClickEvent.getContainerInput(), player);
            value3 = (byte)1;
            value4 = containerClickEvent.getContainerId();
            slot = containerClickEvent.getSlotId();
            value3 = containerClickEvent.getButton();
            ContainerInput containerInput = containerClickEvent.getContainerInput();
            return new GuiMoveModule.PendingClick(value4, slot, value3, containerInput, elements, itemStack);
         }

         Slot element = (Slot)iterator.next();
         ItemStack item = element.getItem();
         elements.add(item.copy());
      }
   }

   @Override
   protected void onEnable() {
      this.flushPacketQueue();
      this.packetQueue.clear();
      this.pendingClicks.clear();
      this.tickCounter = 1;
   }

   private void onPacketReceive(PacketReceiveEvent packetReceiveEvent) {
   }

   public boolean isReallyworldMode() {
      if (this.isEnabled()) {
         if (this.mode.is("Reallyworld")) {
            return true;
         }
      }

      return false;
   }

   public boolean interceptInventoryClose() {
      return false;
   }

   private void onPacketSend(PacketSendEvent packetSendEvent) {
   }

   private void onUpdate() {
      if (this.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player != null && instance.getConnection() != null) {
            if (this.pendingClickCounter > 0) {
               this.pendingClickCounter--;
            }

            if (this.delayCounter > 0) {
               this.delayCounter--;
            }

            if (this.mode.is("SpookyTime")) {
               if (this.wasMoving && this.prevStealing && this.extraClickCount > 0) {
                  this.flushPacketQueue();
               }

               this.wasMoving = this.prevStealing;
               if (this.isHoldingItem()) {
                  this.setExtraClickCount(null);
               } else if (this.extraClickCount > 0) {
                  this.extraClickCount--;
               }

               this.prevStealing = ((this.extraClickCount > 0 ? 1 : 0) != 0);
               this.tickCounter++;
               if (this.prevStealing) {
                  this.tickCounter = 0;
               }
            }

            if ((!this.mode.is("Reallyworld") || this.pendingClickCounter <= 0)
               && (!this.mode.is("Funtime") || this.hasStealTimerElapsed(100L))
               && (!this.mode.is("Holyworld") || this.delayCounter <= 0 || this.hasStealTimerElapsed(125L))
               && (!this.mode.is("SpookyTime") || !this.prevStealing)
               && (!this.mode.is("LonyGrief") || !this.isStealing || this.hasStealTimerElapsed(50L))) {
               if (!(instance.gui.screen() instanceof ChatScreen) && !(instance.gui.screen() instanceof AbstractSignEditScreen)) {
                  this.updateKeyBindingState(this.movementKeys(instance));
                  if (this.isStealing && this.mode.is("Reallyworld")) {
                     this.sendPendingClicks(instance);
                     this.sendClosePacket(instance);
                     this.isStealing = false;
                  }

                  if (this.isStealing && this.mode.is("Funtime")) {
                     instance.gui.setScreen(new InventoryScreen(instance.player));
                     this.sendPendingClicks(instance);
                     this.isStealing = false;
                     this.sendClosePacket(instance);
                     instance.gui.setScreen(null);
                  }

                  if (this.useDelay && this.mode.is("Holyworld") && this.delayCounter == 0) {
                     this.sendPendingClicks(instance);
                     this.sendClosePacket(instance);
                     this.useDelay = false;
                     this.isStealing = false;
                  }

                  if (this.isStealing && this.mode.is("LonyGrief") && this.hasStealTimerElapsed(50L)) {
                     this.sendPendingPackets();
                     this.sendClosePacket(instance);
                     this.pendingPackets.clear();
                     this.isStealing = false;
                     if (instance.gui.screen() != null) {
                        instance.gui.setScreen(null);
                     }
                  }
               }
            }
         }
      }
   }

   private GuiMoveModule() {
      super("Gui Move", "Allows movement while a GUI is open", ModuleCategory.MOVEMENT);
      this.pendingPackets = new ArrayList<>();
      this.stealTimer = System.currentTimeMillis();
      SkycoreClient.events().subscribe(ContainerClickEvent.class, this::onWindowClick);
      SkycoreClient.events().subscribe(PacketSendEvent.class, this::onPacketSend);
      SkycoreClient.events().subscribe(PacketReceiveEvent.class, this::onPacketReceive);
      SkycoreClient.events().subscribe(PlayerInputEvent.class, this::onInput);
      SkycoreClient.events().subscribe(PlayerTickEndEvent.class, playerTickEndEvent -> {
         this.onUpdate();
      });
   }

   private void sendClosePacket(Minecraft minecraft) {
      ClientPacketListener elements = minecraft.player.connection;
      ServerboundContainerClosePacket serverboundContainerClosePacket = new ServerboundContainerClosePacket(minecraft.player.containerMenu.containerId);
      elements.send(serverboundContainerClosePacket);
   }

   public boolean isBypassMode() {
      return this.mode.is("Reallyworld");
   }

   private boolean isMoving() {
      return MovementInput.hasPlayerMovement() || this.prevStealing;
   }

   private void sendPendingClicks(Minecraft minecraft) {
      if (minecraft.player != null && minecraft.gameMode != null && !this.pendingClicks.isEmpty()) {
         this.inventoryOpen = true;

         try {
            for (GuiMoveModule.PendingClick pendingClick : this.pendingClicks) {
               this.restoreClickState(pendingClick, minecraft.player.containerMenu);
               minecraft.gameMode.handleContainerInput(pendingClick.containerId(), pendingClick.slotId(), pendingClick.button(), pendingClick.containerInput(), minecraft.player);
            }
         } finally {
            this.pendingClicks.clear();
            this.inventoryOpen = false;
         }
      }
   }

   private void onInput(PlayerInputEvent playerInputEvent) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && instance.player != null) {
         int value = (!this.mode.is("Reallyworld") || this.pendingClickCounter <= 0)
               && (!this.mode.is("Funtime") || this.hasStealTimerElapsed(190L))
               && (!this.mode.is("Holyworld") || this.delayCounter <= 0 || this.hasStealTimerElapsed(222L))
               && (!this.mode.is("SpookyTime") || !this.prevStealing)
               && (!this.mode.is("LonyGrief") || !this.isStealing || this.hasStealTimerElapsed(50L))
            ? 0
            : 1;
         if (value != 0) {
            this.slow = true;
            Input input = playerInputEvent.keyPresses();
            playerInputEvent.setMoveVector(Vec2.ZERO);
            playerInputEvent.setKeyPresses(new Input(false, false, false, false, false, false, input.sprint()));
         } else {
            this.slow = false;
         }
      }
   }

   private int getClickPriority(ContainerInput containerInput) {
      int value = 0;
      value = (byte)0;
      if (containerInput == ContainerInput.PICKUP) {
         value = 1;
      } else {
         value = this.extraClickCount > 1 ? 2 : 3;
      }

      return value;
   }

   private void updateKeyBindingState(KeyMapping[] keyMappingValues) {

      for (KeyMapping lookup : keyMappingValues) {
         boolean pressed = this.isPressed(lookup);
         lookup.setDown(pressed);
      }
   }

   private boolean hasStealTimerElapsed(long timestamp) {
      return System.currentTimeMillis() - this.stealTimer > timestamp;
   }

   private KeyMapping[] movementKeys(Minecraft minecraft) {
      KeyMapping[] keyMappingValues = new KeyMapping[]{minecraft.options.keyUp, null, null, null, null};
      keyMappingValues[1] = minecraft.options.keyDown;
      keyMappingValues[2] = minecraft.options.keyLeft;
      keyMappingValues[3] = minecraft.options.keyRight;
      keyMappingValues[4] = minecraft.options.keyJump;
      return keyMappingValues;
   }

   private boolean enqueuePacket(ServerboundContainerClickPacket serverboundContainerClickPacket) {
      return !this.packetQueue.contains(serverboundContainerClickPacket) && this.packetQueue.add(serverboundContainerClickPacket);
   }

   private boolean isPressed(KeyMapping lookup) {
      KeyMappingGetKeyAccessor lookup2 = (KeyMappingGetKeyAccessor)lookup;
      Key key = lookup2.skycore$getKey();
      int value = key.getValue();
      if (value >= 0) {
         if (KeyState.isDown(value)) {
            return true;
         }
      }

      return false;
   }

   private void sendPendingPackets() {
      Iterator iterator = this.pendingPackets.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return;
         }

         ServerboundContainerClickPacket element = (ServerboundContainerClickPacket)iterator.next();
         ConsumeSilentHelper.sendSilentPacket(element);
      }
   }

   private void flushPacketQueue() {
      Minecraft instance = Minecraft.getInstance();
      if (!this.packetQueue.isEmpty() && instance.player != null) {
         if (instance.getConnection() != null) {
            while (true) {
               if (this.packetQueue.isEmpty()) {
                  this.tickCounter = 0;
                  break;
               }

               ServerboundContainerClickPacket serverboundContainerClickPacket = this.packetQueue.poll();
               if (serverboundContainerClickPacket != null) {
                  ConsumeSilentHelper.sendSilentPacket(serverboundContainerClickPacket);
               }
            }
         }
      }
   }

   private void onWindowClick(ContainerClickEvent containerClickEvent) {
      Minecraft instance = Minecraft.getInstance();
      if (this.isEnabled() && this.isAnyModeActive() && !this.mode.is("SpookyTime")) {
         if (!this.mode.is("LonyGrief")) {
            if (!this.inventoryOpen && instance.player != null) {
               if (instance.gui.screen() instanceof InventoryScreen) {
                  if (MovementInput.hasPlayerMovement()) {
                     GuiMoveModule.PendingClick pendingClick = this.createPendingClick(containerClickEvent, instance.player.containerMenu, instance.player);
                     if (pendingClick != null) {
                        this.pendingClicks.add(pendingClick);
                        containerClickEvent.cancel();
                     }
                  }
               }

               return;
            }
         }
      }
   }

   public record PendingClick(int containerId, int slotId, int button, ContainerInput containerInput, List<ItemStack> slots, ItemStack carried) {
      public int button() {
         return this.button;
      }

      public int containerId() {
         return this.containerId;
      }

      public List<ItemStack> slots() {
         return this.slots;
      }

      public int slotId() {
         return this.slotId;
      }

      public ContainerInput containerInput() {
         return this.containerInput;
      }

      public ItemStack carried() {
         return this.carried;
      }
   }
}
