package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;

@Mixin(GameRenderer.class)
public class MixinBotGameRenderer {
    @Inject(method = "displayItemActivation", at = @At("HEAD"), cancellable = true)
    private void botItemActivation(ItemStack itemStack, CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }
}
