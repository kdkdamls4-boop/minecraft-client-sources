package tokyo.ghoul.inject.other;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.movement.NoWebModule;

@Mixin(WebBlock.class)
public class MixinWebBlock {
    @Inject(method = "entityInside", at = @At("HEAD"))
    private void markInWeb(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects, CallbackInfo ci) {
        if (entity instanceof LocalPlayer) {
            NoWebModule.setInWeb(true);
        }
    }

    @Inject(method = "entityInside", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;makeStuckInBlock(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/phys/Vec3;)V",
            shift = At.Shift.BEFORE), cancellable = true)
    private void breakWeb(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects, CallbackInfo ci) {
        if (entity instanceof LocalPlayer && NoWebModule.onWebInside(pos)) {
            ci.cancel();
        }
    }
}
