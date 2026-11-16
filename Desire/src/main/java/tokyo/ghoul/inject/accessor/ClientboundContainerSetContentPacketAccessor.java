package tokyo.ghoul.inject.accessor;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ClientboundContainerSetContentPacket.class)
public interface ClientboundContainerSetContentPacketAccessor {
    @Mutable
    @Accessor("items")
    void setItems(List<ItemStack> items);
}
