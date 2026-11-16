package deluxe.client.module.movement;

import deluxe.client.ui.ClickGuiScreen;
import deluxe.mixin.ClientCommonNetworkHandlerMixin;
import deluxe.client.event.EventListener;
import deluxe.client.module.Module;
import deluxe.client.module.ModuleCategory;
import deluxe.client.module.ModuleInfo;
import deluxe.client.event.PacketEvent;
import deluxe.client.event.PlayerTickEvent;
import deluxe.client.module.movement.MovementPauseController;
import deluxe.client.module.player.ServerHelperModule;

import java.util.ArrayList;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.SlotActionType;

@ModuleInfo(name = "ScreenWalk", description = "Позволяет двигаться с открытым контейнером, задерживая пакеты инвентаря.", category = ModuleCategory.MOVEMENT)
public final class ScreenWalkModule extends Module {
   private static ScreenWalkModule screenWalkModule;
   private final ArrayList<ScreenWalkModule.Nested0364> field02086 = new ArrayList<>();
   private final EventListener<PacketEvent> field02087 = packetEvent -> {
      if (packetEvent.isOutgoing()) {
         if (mc.currentScreen instanceof InventoryScreen && packetEvent.getPacket() instanceof ClickSlotC2SPacket clickSlotC2SPacket && this.method0992()) {
            boolean local1352_3 = clickSlotC2SPacket.button() == 1
               && mc.player.currentScreenHandler.getCursorStack().getItem() instanceof BlockItem blockItem
               && blockItem.getBlock() instanceof ShulkerBoxBlock;
            if (local1352_3) {
               MovementPauseController.getInstance().pauseForTicks(2);
            }

            this.field02086.add(new ScreenWalkModule.Nested0364(packetEvent.getPacket(), 2, local1352_3));
            packetEvent.method4423();
         }

         if (packetEvent.getPacket() instanceof CloseHandledScreenC2SPacket && this.method0992() && mc.currentScreen instanceof InventoryScreen) {
            packetEvent.method4423();

            for (ScreenWalkModule.Nested0364 local1352_3 : this.field02086) {
               MovementPauseController.getInstance().pauseForTicks(local1352_3.field02090);
            }
         }
      }
   };
   private final EventListener<PlayerTickEvent> field02088 = playerTickEvent -> {
      if (mc.player != null) {
         if (mc.currentScreen != null
            && !(mc.currentScreen instanceof ChatScreen)
            && !(mc.currentScreen instanceof SignEditScreen)
            && !(mc.currentScreen instanceof AnvilScreen)
            && !(mc.currentScreen instanceof CreativeInventoryScreen)) {
            for (KeyBinding keyBinding : new KeyBinding[]{
               mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
            }) {
               keyBinding.setPressed(
                  !(
                        mc.currentScreen instanceof ClickGuiScreen clickGuiScreen
                           && (clickGuiScreen.getCtx().textSetting != null || clickGuiScreen.getCtx().field12425 != null)
                     )
                     && InputUtil.isKeyPressed(mc.getWindow(), KeyBindingHelper.getBoundKeyOf(keyBinding).getCode())
               );
            }
         }

         if (!this.method0992() && !this.field02086.isEmpty()) {
            for (ScreenWalkModule.Nested0364 local1349_3 : this.field02086) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler()).connection().send(local1349_3.field02089, null);
            }

            this.field02086.clear();
            if (mc.currentScreen == null) {
               ((ClientCommonNetworkHandlerMixin)mc.getNetworkHandler())
                  .connection()
                  .send(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId), null);
            }
         }
      }
   };

   @Override
   protected void onDisable() {
      this.field02086.clear();
   }

   public boolean method0988() {
      return ServerHelperModule.method0707().modeSetting.matchesOption("SpookyTime");
   }

   private ScreenWalkModule() {
   }

   public void method0990() {
      if (mc.player != null && mc.interactionManager != null) {
         int local1354_1 = mc.player.playerScreenHandler.syncId;
         if (!mc.player.playerScreenHandler.getCursorStack().isEmpty()) {
            mc.interactionManager.clickSlot(local1354_1, -999, 0, SlotActionType.PICKUP, mc.player);
         }

         for (int index = 1; index < mc.player.playerScreenHandler.slots.size(); index++) {
            if (mc.player.playerScreenHandler.getSlot(index).hasStack()) {
               mc.interactionManager.clickSlot(local1354_1, index, 1, SlotActionType.THROW, mc.player);
            }
         }
      }
   }

   public static ScreenWalkModule method0991() {
      if (screenWalkModule == null) {
         screenWalkModule = new ScreenWalkModule();
      }

      return screenWalkModule;
   }

   public boolean method0992() {
      return mc.player != null && (mc.player.input.getMovementInput().lengthSquared() > 0.0F || mc.player.input.playerInput.jump());
   }

   static final class Nested0364 {
      private final Packet<?> field02089;
      private final int field02090;
      private final boolean field02091;

      private Nested0364(Packet<?> local1357_1, int local1357_2, boolean local1357_3) {
         this.field02089 = local1357_1;
         this.field02090 = local1357_2;
         this.field02091 = local1357_3;
      }
   }
}
