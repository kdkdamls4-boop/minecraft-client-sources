package tokyo.ghoul.inject.other;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.api.event.events.other.ScreenEvent;
import tokyo.ghoul.api.system.interfaces.HoveredSlotProvider;
import tokyo.ghoul.client.autobuy.AutoBuyOverlay;
import tokyo.ghoul.client.features.modules.combat.AutoSwapModule;
import tokyo.ghoul.client.features.modules.misc.AhHelperModule;

@Mixin(AbstractContainerScreen.class)
public abstract class MixinHandledScreen<T extends AbstractContainerMenu> extends Screen implements MenuAccess<T>, HoveredSlotProvider {
    protected MixinHandledScreen(Component title) {
        super(title);
    }

    @Shadow protected Slot hoveredSlot;
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow @Final protected int imageWidth;
    @Shadow @Final protected int imageHeight;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderAuctionHighlights(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AhHelperModule.renderHighlights(graphics, this.getMenu(), leftPos, topPos);
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void renderAutoBuyOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (AutoBuyOverlay.shouldShow(self)) {
            AutoBuyOverlay.render(graphics, leftPos, topPos, imageWidth, imageHeight, mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onAutoBuyOverlayClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (AutoBuyOverlay.shouldShow(self)
                && AutoBuyOverlay.mouseClicked(click.x(), click.y(), click.button(), leftPos, topPos, imageWidth, imageHeight)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onAutoBuyOverlayScroll(double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (AutoBuyOverlay.shouldShow(self)
                && AutoBuyOverlay.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount, leftPos, topPos, imageWidth, imageHeight)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onAutoSwapPick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (AutoSwapModule.getInstance().onPickerClick(hoveredSlot)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void onAuctionClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        AhHelperModule.onContainerClick(hoveredSlot);
    }

    @Unique
    @Override
    public Slot static$getHoveredSlot() {
        return hoveredSlot;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ScreenEvent.ScreenEventData event = new ScreenEvent.ScreenEventData(this);
        ScreenEvent.getInstance().call(event);

        for (Button button : event.buttons()) {
            this.addRenderableWidget(button);
        }
    }

}
