package tokyo.ghoul.inject.world;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

@Mixin(Level.class)
public abstract class MixinLevel {
    @Inject(method = "getRainLevel", at = @At("HEAD"), cancellable = true)
    private void hideRain(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (RemovalsModule.getInstance().isWeather()) {
            cir.setReturnValue(0.0F);
        }
    }

    @Inject(method = "getThunderLevel", at = @At("HEAD"), cancellable = true)
    private void hideThunder(float partialTick, CallbackInfoReturnable<Float> cir) {
        if (RemovalsModule.getInstance().isWeather()) {
            cir.setReturnValue(0.0F);
        }
    }
}
