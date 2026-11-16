package tokyo.ghoul.inject.accessor;

import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientboundContainerSetSlotPacket.class)
public interface ClientboundContainerSetSlotPacketAccessor {
    @Mutable
    @Accessor("itemStack")
    void setItemStack(ItemStack itemStack);
}
