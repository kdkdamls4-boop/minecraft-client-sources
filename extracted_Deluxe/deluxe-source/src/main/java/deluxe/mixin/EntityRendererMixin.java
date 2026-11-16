package deluxe.mixin;

import deluxe.client.module.render.TagsModule;
import deluxe.client.module.render.WardenEspModule;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
   @Inject(method = "hasLabel(Lnet/minecraft/entity/Entity;D)Z", at = @At("HEAD"), cancellable = true)
   private void hideVanillaNameTag(Entity entity, double local8103_2, CallbackInfoReturnable<Boolean> local8103_4) {
      if (TagsModule.method4106().isEnabled() && entity instanceof AbstractClientPlayerEntity) {
         local8103_4.setReturnValue(false);
      }
   }

   @Inject(method = "shouldRender(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/Frustum;DDD)Z", at = @At("HEAD"), cancellable = true)
   private void hideWardenHologram(
      Entity entity, Frustum frustum, double local8104_3, double local8104_5, double local8104_7, CallbackInfoReturnable<Boolean> local8104_9
   ) {
      if (WardenEspModule.method1959().method1965(entity)) {
         local8104_9.setReturnValue(false);
      }
   }
}
