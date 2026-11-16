package deluxe.mixin;

import deluxe.client.module.render.CustomWorldModule;
import ru.dreamix.fabricloader.VMBridge;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/client/world/ClientWorld$Properties")
public abstract class ClientWorld_PropertiesMixin {
   @ModifyReturnValue(method = "getTimeOfDay()J", at = @At("RETURN"))
   private long forceWorldTime(long local8075_1) {
      CustomWorldModule customWorldModule = CustomWorldModule.method2204();
      return customWorldModule.isEnabled() && customWorldModule.field07411.isChecked()
         ? local8075_1 - local8075_1 % 24000L + (long)customWorldModule.field07412.getValue()
         : local8075_1;
   }

   static {
      VMBridge.identifyClass(ClientWorld_PropertiesMixin.class, "tZhzwdn1");
   }
}
