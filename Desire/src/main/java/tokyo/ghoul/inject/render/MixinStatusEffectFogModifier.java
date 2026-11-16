package tokyo.ghoul.inject.render;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

@Mixin(MobEffectFogEnvironment.class)
public class MixinStatusEffectFogModifier {
    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    private boolean removeBadEffectFog(boolean original) {
        return original && !RemovalsModule.getInstance().isBadEffects();
    }
}
