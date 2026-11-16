package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tokyo.ghoul.client.features.modules.render.ItemReplacerModule;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRendererItemReplacer {
    @ModifyArg(
            method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"),
            index = 1
    )
    private ItemStack static$replaceHeldModel(ItemStack stack) {
        ItemReplacerModule module = ItemReplacerModule.getInstance();
        return module.isEnabled() ? module.getRenderStack(stack) : stack;
    }
}
