package net.skycore.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.math.Transformation;
import java.util.Optional;
import net.minecraft.client.renderer.item.SpecialModelWrapper.Unbaked;
import net.skycore.client.util.WithLegacyDefaultHelper;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Unbaked.class)
public abstract class SpecialModelWrapperUnbakedLegacyHeadTransformMixin {
   @Shadow
   @Final
   private net.minecraft.client.renderer.special.SpecialModelRenderer.Unbaked<?> specialModel;

   @WrapOperation(
      method = "bake",
      at = @At(value = "INVOKE", target = "Lcom/mojang/math/Transformation;compose(Lorg/joml/Matrix4fc;Ljava/util/Optional;)Lorg/joml/Matrix4fc;")
   )
   private Matrix4fc skycore$legacyHeadTransform(Matrix4fc matrix4fc, Optional<Transformation> optional, Operation<Matrix4fc> operation) {
      if (this.specialModel instanceof net.minecraft.client.renderer.special.PlayerHeadSpecialRenderer.Unbaked
         || this.specialModel instanceof net.minecraft.client.renderer.special.SkullSpecialRenderer.Unbaked) {
         optional = WithLegacyDefaultHelper.withLegacyDefault(optional);
      }

      return (Matrix4fc)operation.call(new Object[]{matrix4fc, optional});
   }
}
