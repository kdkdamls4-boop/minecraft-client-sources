package tokyo.ghoul.inject.other;

import com.mojang.blaze3d.platform.FramerateLimitTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;
import tokyo.ghoul.client.ui.menu.AltScreen;
import tokyo.ghoul.client.ui.menu.MainMenuScreen;

@Mixin(FramerateLimitTracker.class)
public class MixinFramerateLimitTracker {
    private static final int MENU_FPS_CAP = 120;

    @Shadow
    private int framerateLimit;

    @Inject(method = "getFramerateLimit", at = @At("HEAD"), cancellable = true)
    private void desire$uncapMenu(CallbackInfoReturnable<Integer> cir) {
        if (MainMenuScreen.isActive() || AltScreen.isActive()) {
            cir.setReturnValue(Math.min(framerateLimit, MENU_FPS_CAP));
        }
    }

    @Inject(method = "getFramerateLimit", at = @At("RETURN"), cancellable = true)
    private void desire$backgroundLimit(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(OptimizationModule.limitFramerate(cir.getReturnValue()));
    }
}
