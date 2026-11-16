package brova.toshiba.mixin;

import brova.toshiba.client.core.MinecraftAccess;
import brova.toshiba.client.event.impl.ChunkReloadEvent;
import brova.toshiba.client.utils.input.KeyBindUtil;
import net.minecraft.client.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Keyboard.class)
public class KeyboardMixin implements MinecraftAccess {

    @Inject(method = "onKey", at = @At("HEAD"))
    public void onKey(long window, int key, int scancode, int action, int modifiers, CallbackInfo ci) {
        if (mc.currentScreen == null) {
            KeyBindUtil.call(key, action);
        }
    }

    @Inject(method = "processF3", at = @At("RETURN"))
    private void processF3(int key, CallbackInfoReturnable<Boolean> cir) {
        if (key == 65 && cir.getReturnValue()) {
            new ChunkReloadEvent().call();
        }
    }
}
