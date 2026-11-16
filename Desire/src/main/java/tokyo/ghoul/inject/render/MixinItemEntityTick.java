package tokyo.ghoul.inject.render;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.misc.OptimizationModule;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntityTick {
    @Unique private boolean desire$skipFlip;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void desire$restingItems(CallbackInfo ci) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!OptimizationModule.shouldSkipRestingItemTick(self)) return;
        desire$skipFlip = !desire$skipFlip;
        if (desire$skipFlip) ci.cancel();
    }
}
