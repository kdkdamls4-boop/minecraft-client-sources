package net.skycore.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.skycore.client.module.player.ItemScrollerModule;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenScrollItemsMixin {
   @Shadow
   protected Slot hoveredSlot;
   @Shadow
   protected int leftPos;
   @Shadow
   protected int topPos;
   private long skycore$lastScroll;

   @Shadow
   protected abstract void slotClicked(Slot slot, int value, int value2, ContainerInput containerInput);

   @Inject(method = "extractRenderState", at = @At("RETURN"))
   private void skycore$scrollItems(GuiGraphicsExtractor guiGraphicsExtractor, int value, int value2, float value3, CallbackInfo callbackInfo) {
      ItemScrollerModule iNSTANCE = ItemScrollerModule.INSTANCE;
      Minecraft instance = Minecraft.getInstance();
      long windowHandle = instance.getWindow().handle();
      if (iNSTANCE.isEnabled()
         && this.hoveredSlot != null
         && this.hoveredSlot.hasItem()
         && GLFW.glfwGetMouseButton(windowHandle, 0) == 1
         && (GLFW.glfwGetKey(windowHandle, 340) == 1 || GLFW.glfwGetKey(windowHandle, 344) == 1)) {
         long timestamp = Math.round(iNSTANCE.delay.get() * 10.0);
         if (System.currentTimeMillis() - this.skycore$lastScroll >= timestamp) {
            this.slotClicked(this.hoveredSlot, this.hoveredSlot.index, 0, ContainerInput.QUICK_MOVE);
            this.skycore$lastScroll = System.currentTimeMillis();
         }
      }
   }

   @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
   private void skycore$dragItems(MouseButtonEvent mouseButtonEvent, double value, double value2, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
      if (ItemScrollerModule.INSTANCE.isEnabled() && mouseButtonEvent.button() == 0 && mouseButtonEvent.hasShiftDown()) {
         AbstractContainerScreen abstractContainerScreen = (AbstractContainerScreen)(Object)this;
         Slot slot = null;
         double x = mouseButtonEvent.x() - this.leftPos;
         double y = mouseButtonEvent.y() - this.topPos;

         for (Slot slot2 : abstractContainerScreen.getMenu().slots) {
            if (slot2.isActive() && x >= slot2.x - 1 && x < slot2.x + 17 && y >= slot2.y - 1 && y < slot2.y + 17) {
               slot = slot2;
               break;
            }
         }

         if (slot != null && slot.hasItem()) {
            long timestamp = Math.round(ItemScrollerModule.INSTANCE.delay.get() * 10.0);
            long now = System.currentTimeMillis();
            if (now - this.skycore$lastScroll < timestamp) {
               callbackInfoReturnable.setReturnValue(true);
            } else {
               this.slotClicked(slot, slot.index, 0, ContainerInput.QUICK_MOVE);
               this.skycore$lastScroll = now;
               callbackInfoReturnable.setReturnValue(true);
            }
         }
      }
   }
}
