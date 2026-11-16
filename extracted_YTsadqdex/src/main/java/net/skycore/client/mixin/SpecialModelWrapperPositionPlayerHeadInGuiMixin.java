package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.skycore.client.util.ExtractArgumentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpecialModelWrapper.class)
public abstract class SpecialModelWrapperPositionPlayerHeadInGuiMixin {
   @WrapOperation(
      method = "update",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState$LayerRenderState;setupSpecialModel(Lnet/minecraft/client/renderer/special/SpecialModelRenderer;Ljava/lang/Object;)V"
      )
   )
   private void skycore$positionPlayerHeadInGui(
      LayerRenderState layerRenderState,
      SpecialModelRenderer<?> specialModelRenderer,
      Object value,
      Operation<Void> operation,
      ItemStackRenderState itemStackRenderState,
      ItemStack itemStack,
      ItemModelResolver itemModelResolver,
      ItemDisplayContext itemDisplayContext,
      ClientLevel level,
      ItemOwner itemOwner,
      int value2
   ) {
      if (itemDisplayContext == ItemDisplayContext.GUI && itemStack != null && itemStack.is(Items.PLAYER_HEAD) && specialModelRenderer != null && specialModelRenderer.getClass() == PlayerHeadSpecialRenderer.class) {
         SpecialModelRenderer specialModelRenderer2 = specialModelRenderer;
         operation.call(new Object[]{layerRenderState, new ExtractArgumentHelper(specialModelRenderer2), value});
      } else {
         operation.call(new Object[]{layerRenderState, specialModelRenderer, value});
      }
   }
}
