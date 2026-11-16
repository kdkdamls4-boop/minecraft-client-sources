package tokyo.ghoul.inject.render;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tokyo.ghoul.client.features.modules.render.ItemReplacerModule;

@Mixin(ItemModelResolver.class)
public abstract class MixinItemModelResolverItemReplacer {
    @ModifyVariable(method = "updateForLiving", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private ItemStack static$replaceThirdPersonModel(ItemStack stack) {
        ItemReplacerModule module = ItemReplacerModule.getInstance();
        return module.isEnabled() ? module.getRenderStack(stack) : stack;
    }
}
