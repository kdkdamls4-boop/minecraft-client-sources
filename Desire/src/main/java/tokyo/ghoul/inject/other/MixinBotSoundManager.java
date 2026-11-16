package tokyo.ghoul.inject.other;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;

@Mixin(SoundManager.class)
public class MixinBotSoundManager {
    @Inject(method = "play", at = @At("HEAD"), cancellable = true)
    private void botPlay(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
        if (BotContext.isBackgroundContextActive()) {
            cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
        }
    }

    @Inject(method = {"playDelayed", "queueTickingSound"}, at = @At("HEAD"), cancellable = true)
    private void botPlayDelayed(CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }
}
