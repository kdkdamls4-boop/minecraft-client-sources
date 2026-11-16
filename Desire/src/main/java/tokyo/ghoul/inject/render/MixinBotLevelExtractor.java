package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;

@Mixin(LevelExtractor.class)
public class MixinBotLevelExtractor {
    @Inject(method = {"blockChanged", "setBlocksDirty", "setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V",
            "setSectionDirtyWithNeighbors", "setSectionRangeDirty", "setSectionDirty(III)V"},
            at = @At("HEAD"), cancellable = true)
    private void botDirty(CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }
}
