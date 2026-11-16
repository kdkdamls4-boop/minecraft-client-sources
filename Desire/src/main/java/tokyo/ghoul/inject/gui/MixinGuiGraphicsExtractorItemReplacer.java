package tokyo.ghoul.inject.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import tokyo.ghoul.client.features.modules.render.ItemReplacerModule;

@Mixin(GuiGraphicsExtractor.class)
public abstract class MixinGuiGraphicsExtractorItemReplacer {
    @ModifyArg(
            method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemModelResolver;updateForTopItem(Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/ItemOwner;I)V"),
            index = 1
    )
    private ItemStack static$replaceGuiModel(ItemStack stack) {
        ItemReplacerModule module = ItemReplacerModule.getInstance();
        return module.isEnabled() ? module.getRenderStack(stack) : stack;
    }
}
