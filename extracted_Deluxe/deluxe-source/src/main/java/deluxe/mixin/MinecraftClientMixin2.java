package deluxe.mixin;

import deluxe.client.module.render.TagsModule;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin2 {
   @Inject(method = "hasOutline(Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
   private void forcePlayersGlowMask(Entity entity, CallbackInfoReturnable<Boolean> local8058_2) {
      if (TagsModule.method4106().method4095()) {
         local8058_2.setReturnValue(entity instanceof AbstractClientPlayerEntity abstractClientPlayerEntity && TagsModule.method4106().method4105(abstractClientPlayerEntity));
      }
   }
}
