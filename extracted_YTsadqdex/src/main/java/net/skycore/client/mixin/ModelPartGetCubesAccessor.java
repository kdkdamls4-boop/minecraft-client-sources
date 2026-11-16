package net.skycore.client.mixin;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ModelPart.class)
public interface ModelPartGetCubesAccessor {
   @Accessor("cubes")
   List<Cube> skycore$getCubes();

   @Accessor("children")
   Map<String, ModelPart> skycore$getChildren();
}
