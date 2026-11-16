package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.render.AmbienceModule;

@Mixin(SkyRenderer.class)
public class MixinSkyRenderer {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void fogOnlySky(ClientLevel level, float partialTicks, Camera camera, SkyRenderState state, CallbackInfo ci) {
        AmbienceModule ambience = AmbienceModule.getInstance();
        if (!ambience.isFogOnlySky()) return;
        state.skyColor = ambience.fogSkyColor();
        state.sunriseAndSunsetColor = 0;
        state.shouldRenderDarkDisc = false;
        state.starBrightness = 0f;
    }

    @Inject(method = "renderSunMoonAndStars", at = @At("HEAD"), cancellable = true)
    private void hideSunMoonAndStars(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle,
                                     MoonPhase moonPhase, float rainBrightness, float starBrightness, CallbackInfo ci) {
        if (AmbienceModule.getInstance().isFogOnlySky()) ci.cancel();
    }

    @Inject(method = "renderSunriseAndSunset", at = @At("HEAD"), cancellable = true)
    private void hideSunriseAndSunset(PoseStack poseStack, float sunAngle, int color, CallbackInfo ci) {
        if (AmbienceModule.getInstance().isFogOnlySky()) ci.cancel();
    }
}
