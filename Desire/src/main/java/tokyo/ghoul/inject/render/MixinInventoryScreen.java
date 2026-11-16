package tokyo.ghoul.inject.render;

import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;

@Mixin(InventoryScreen.class)
public abstract class MixinInventoryScreen {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
            at = @At("RETURN")
    )
    private static void desire$bindCosmeticPip(LivingEntity entity, CallbackInfoReturnable<EntityRenderState> cir) {
        CosmeticsGuiPreview.bindIfPending(cir.getReturnValue());
    }
}
