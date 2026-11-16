package tokyo.ghoul.inject.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.ui.menu.MainMenuScreen;

@Mixin(Gui.class)
public abstract class MixinGui {
    @Shadow
    private Screen screen;

    @Shadow
    public abstract Overlay overlay();

    @Shadow
    public abstract void setOverlay(Overlay overlay);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void botSetScreen(Screen screen, CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            BotContext.current().setScreen(screen);
            ci.cancel();
        }
    }

    @Inject(method = "screen", at = @At("HEAD"), cancellable = true)
    private void botScreen(CallbackInfoReturnable<Screen> cir) {
        if (BotContext.isBackgroundContextActive()) {
            cir.setReturnValue(BotContext.current().getScreen());
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void showCustomMainMenu(CallbackInfo ci) {
        if (this.screen instanceof TitleScreen) {
            if (MainMenuScreen.consumeVanillaBypass()) return;
            MainMenuScreen menu = new MainMenuScreen();
            this.screen = menu;
            Minecraft minecraft = Minecraft.getInstance();
            menu.init(minecraft.getWindow().getGuiScaledWidth(), minecraft.getWindow().getGuiScaledHeight());
            if (this.overlay() instanceof LoadingOverlay) {
                this.setOverlay(null);
            }
        }
    }
}
