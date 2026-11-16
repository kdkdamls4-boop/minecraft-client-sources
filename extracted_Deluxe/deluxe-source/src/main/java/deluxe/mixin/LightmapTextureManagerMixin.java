package deluxe.mixin;

import deluxe.client.module.render.BrightnessModule;
import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.module.render.NoRenderModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightmapTextureManager.class)
public abstract class LightmapTextureManagerMixin {
   @Inject(method = "getDarkness(Lnet/minecraft/entity/LivingEntity;FF)F", at = @At("HEAD"), cancellable = true)
   private void removeDarkness(LivingEntity livingEntity, float local8198_2, float local8198_3, CallbackInfoReturnable<Float> local8198_4) {
      if (NoRenderModule.method3426().method3427("Темнота")) {
         local8198_4.setReturnValue(0.0F);
      }
   }

   @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
   private float boostWorldBrightness(float local8199_1) {
      CustomWorldModule customWorldModule = CustomWorldModule.method2204();
      return local8199_1
         * (
            BrightnessModule.method0820().isEnabled() && BrightnessModule.method0820().modeSetting.isSelected("Гамма")
               ? BrightnessModule.method0820().legacyNumberSetting.getValue()
               : 1.0F
         )
         * (customWorldModule.isEnabled() ? customWorldModule.field07409.getValue() : 1.0F);
   }

   static {
      VMBridge.identifyClass(LightmapTextureManagerMixin.class, "4fxSAVJ0");
   }
}
