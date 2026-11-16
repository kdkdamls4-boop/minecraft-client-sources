package tokyo.ghoul.inject.other;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;

@Mixin(Item.class)
public abstract class MixinItem {
    @ModifyExpressionValue(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 hookFixRotation(Vec3 original, Level world, Player player, ClipContext.Fluid fluidHandling) {
        var rotation = RotationManager.getInstance().getCurrentRotation();

        if (player == SharedClass.player() && rotation != null) {
            return rotation.getVector();
        }

        return original;
    }
}
