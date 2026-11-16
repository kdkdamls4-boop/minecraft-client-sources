package tokyo.ghoul.inject.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;

@Mixin(LocalPlayer.class)
public class MixinBotLocalPlayer {
    @Inject(method = "isControlledCamera", at = @At("HEAD"), cancellable = true)
    private void botControlledCamera(CallbackInfoReturnable<Boolean> cir) {
        if (BotContext.isBackgroundContextActive() && Minecraft.getInstance().player == (Object) this) {
            cir.setReturnValue(true);
        }
    }
}
