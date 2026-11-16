package tokyo.ghoul.inject.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.player.world.AttackEvent;
import tokyo.ghoul.api.event.events.player.move.TravelEvent;
import tokyo.ghoul.api.system.backend.SharedClass;

@Mixin(Player.class)
public abstract class MixinPlayerEntity extends MixinLivingEntity {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    public void attackEventHook(Entity target, CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        if ((Object) this == SharedClass.player()) {
            if (AttackEvent.getInstance().call(new AttackEvent.AttackEventData(target))) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    public void travelHook(Vec3 movementInput, CallbackInfo ci) {
        if ((Object) this != SharedClass.player()) {
            return;
        }

        if (TravelEvent.getInstance().call()) {
            ci.cancel();
        }
    }
}
