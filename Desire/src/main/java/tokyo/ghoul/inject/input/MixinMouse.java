package tokyo.ghoul.inject.input;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.client.MouseLookEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.client.features.modules.combat.AuraModule;
import tokyo.ghoul.client.ui.clickgui.ClickGui;

@Mixin(MouseHandler.class)
public class MixinMouse {
    @Inject(method = "grabMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"), cancellable = true)
    private void lockCursorHook(CallbackInfo ci) {
        if (Minecraft.getInstance().gui.screen() instanceof ClickGui) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAccumulatedMovement", at = @At("TAIL"))
    private void legitAimFrame(CallbackInfo ci) {
        AuraModule aura = AuraModule.getInstance();
        if (aura.isEnabled()) {
            aura.onLegitFrame();
        }
    }

    @WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void mouseLookHook(LocalPlayer player, double deltaX, double deltaY, Operation<Void> original) {
        MouseLookEvent.MouseLookEventData event = new MouseLookEvent.MouseLookEventData(deltaX, deltaY);
        if (MouseLookEvent.getInstance().call(event)) {
            original.call(player, 0.0, 0.0);
        } else {
            original.call(player, event.getDeltaX(), event.getDeltaY());
        }
    }

    @Inject(method = "onButton", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/FramerateLimitTracker;onInputReceived()V"))
    public void mousePressHook(long window, MouseButtonInfo input, int action, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        int button = input.button();
        KeyEvent.getInstance().call(new KeyEvent.KeyEventData(button, action, true));

        DraggableManager.getInstance().getDraggables().forEach((s, draggable) -> {
            if (draggable.getModule().isEnabled()) {
                if (action == 0) {
                    draggable.onRelease(button);
                } else if (action == 1) {
                    draggable.onClick(button);
                }
            }
        });
    }

}
