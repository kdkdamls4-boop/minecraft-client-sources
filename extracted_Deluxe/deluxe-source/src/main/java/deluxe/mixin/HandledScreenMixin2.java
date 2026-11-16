package deluxe.mixin;

import deluxe.client.module.misc.AuctionHelperModule;
import deluxe.client.module.misc.AutoBuyModule;
import deluxe.client.event.ContainerRenderEvent;
import deluxe.client.event.EventBus;
import deluxe.client.module.movement.ScreenWalkModule;
import deluxe.client.module.render.SmoothFeatureModule;
import ru.dreamix.fabricloader.VMBridge;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin2 extends Screen {
   @Shadow
   protected Slot focusedSlot;
   @Shadow
   protected int x;
   @Shadow
   protected int y;
   @Shadow
   protected int backgroundWidth;

   protected HandledScreenMixin2(Text text) {
      super(text);
   }

   @Inject(method = "drawSlots(Lnet/minecraft/client/gui/DrawContext;II)V", at = @At("HEAD"))
   private void fireRenderSlotsEvent(DrawContext drawContext, int local8349_2, int local8349_3, CallbackInfo callbackInfo) {
      if (EventBus.getInstance().hasListeners(ContainerRenderEvent.class)) {
         EventBus.getInstance().post(new ContainerRenderEvent((HandledScreen<?>)(Object)this, drawContext));
      }
   }

   @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V", at = @At("HEAD"))
   private void growHoveredSlotItem(DrawContext drawContext, Slot slot, int local8350_3, int local8350_4, CallbackInfo callbackInfo) {
      SmoothFeatureModule.method3278().method3274(drawContext, slot);
   }

   @Inject(method = "drawSlot(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/screen/slot/Slot;II)V", at = @At("RETURN"))
   private void ungrowHoveredSlotItem(DrawContext drawContext, Slot slot, int local8351_3, int local8351_4, CallbackInfo callbackInfo) {
      SmoothFeatureModule.method3278().method3279(drawContext);
   }

   @Inject(method = "init()V", at = @At("RETURN"))
   private void addDropAllButton(CallbackInfo callbackInfo) {
      if ((Object)this instanceof InventoryScreen && ScreenWalkModule.method0991().isEnabled()) {
         int local8352_2 = this.textRenderer.getWidth("Выкинуть все") + 12;
         this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Выкинуть все"), buttonWidget -> ScreenWalkModule.method0991().method0990())
               .dimensions(this.x + this.backgroundWidth / 2 - local8352_2 / 2, this.y - 24, local8352_2, 20)
               .build()
         );
      }
   }

   @Inject(method = "init()V", at = @At("RETURN"))
   private void addAuctionWidgets(CallbackInfo callbackInfo) {
      if (AutoBuyModule.method1734().isEnabled() && AuctionHelperModule.method2310().method2331(this.getTitle().getString())) {
         this.addDrawableChild(
            ButtonWidget.builder(Text.literal(AutoBuyModule.method1734().method1732() ? "Автобай ВКЛ" : "Автобай ВЫКЛ"), buttonWidget -> {
                  AutoBuyModule.method1734().method1744();
                  buttonWidget.setMessage(Text.literal(AutoBuyModule.method1734().method1732() ? "Автобай ВКЛ" : "Автобай ВЫКЛ"));
               })
               .dimensions(
                  this.x + this.backgroundWidth / 2 - (this.textRenderer.getWidth("Автобай ВЫКЛ") + 12) / 2,
                  this.y - 24,
                  this.textRenderer.getWidth("Автобай ВЫКЛ") + 12,
                  20
               )
               .build()
         );
         this.addDrawableChild(
            ButtonWidget.builder(Text.literal(AutoBuyModule.method1734().method1750() ? "Автопарсер ВКЛ" : "Автопарсер ВЫКЛ"), buttonWidget2 -> {
               AutoBuyModule.method1734().method1742();
               buttonWidget2.setMessage(Text.literal(AutoBuyModule.method1734().method1750() ? "Автопарсер ВКЛ" : "Автопарсер ВЫКЛ"));
            }).dimensions(this.x + this.backgroundWidth + 6, this.y, this.textRenderer.getWidth("Автопарсер ВЫКЛ") + 12, 20).build()
         );
         this.addDrawableChild(
            new AutoBuyModule.Nested0407(
               this.x + this.backgroundWidth + 6,
               this.y + 24,
               this.textRenderer.getWidth("Автопарсер ВЫКЛ") + 12,
               "Покупка",
               AutoBuyModule.method1734().field05387
            )
         );
         this.addDrawableChild(
            new AutoBuyModule.Nested0407(
               this.x + this.backgroundWidth + 6,
               this.y + 48,
               this.textRenderer.getWidth("Автопарсер ВЫКЛ") + 12,
               "Продажа",
               AutoBuyModule.method1734().field05388
            )
         );
      }
   }

   static {
      VMBridge.identifyClass(HandledScreenMixin2.class, "nSCB0u1a");
   }
}
