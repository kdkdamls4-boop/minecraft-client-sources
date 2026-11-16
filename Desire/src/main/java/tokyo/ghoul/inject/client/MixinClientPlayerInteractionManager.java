package tokyo.ghoul.inject.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.api.event.events.player.world.ClickSlotEvent;
import tokyo.ghoul.client.features.modules.combat.PacketCriticalsModule;

@Mixin(MultiPlayerGameMode.class)
public class MixinClientPlayerInteractionManager {
    @Inject(method = "attack", at = @At("HEAD"))
    private void packetCriticalsOnAttack(Player player, Entity target, CallbackInfo ci) {
        if (player != Minecraft.getInstance().player) return;
        PacketCriticalsModule.getInstance().onAttack();
    }

    @Inject(method = "handleContainerInput", at = @At("HEAD"), cancellable = true)
    public void onClickSlot(int syncId, int slotId, int button, ContainerInput actionType, Player player, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null || Minecraft.getInstance().level == null) return;
        ClickSlotEvent.ClickSlotEventData event = new ClickSlotEvent.ClickSlotEventData(actionType, slotId, button, syncId);
        if (ClickSlotEvent.getInstance().call(event)) {
            ci.cancel();
        }
    }
}
