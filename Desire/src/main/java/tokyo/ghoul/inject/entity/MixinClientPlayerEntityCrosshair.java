package tokyo.ghoul.inject.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntityCrosshair {
    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 rotationVectorHook(Vec3 original, Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        if (cameraEntity != SharedClass.player()) {
            return original;
        }

        Rotation rotation = RotationManager.getInstance().getCurrentRotation();
        return rotation != null ? rotation.getVector() : original;
    }

    @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;pick(DFZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult hookRaycast(HitResult original, Entity cameraEntity, double blockInteractionRange, double entityInteractionRange, float tickProgress) {
        if (cameraEntity != SharedClass.player()) {
            return original;
        }

        Rotation current = RotationManager.getInstance().getCurrentRotation();
        Rotation rotation = current != null ? current : new Rotation(cameraEntity.getViewYRot(tickProgress), cameraEntity.getViewXRot(tickProgress));

        return RaytracingUtil.raycast(rotation, Math.max(blockInteractionRange, entityInteractionRange), false, tickProgress);
    }
}
