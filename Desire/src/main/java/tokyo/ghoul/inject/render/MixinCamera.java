package tokyo.ghoul.inject.render;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.player.FreeCamModule;

@Mixin(Camera.class)
public abstract class MixinCamera {
    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "alignWithEntity", at = @At("TAIL"))
    private void desire$freeCam(float partialTick, CallbackInfo ci) {
        Vec3 pos = FreeCamModule.interpolatedPos(partialTick);
        if (pos == null) return;
        setPosition(pos.x, pos.y, pos.z);
    }

}
