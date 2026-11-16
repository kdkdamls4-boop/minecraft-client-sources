package net.skycore.client.mixin;

import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemCooldowns.class)
public interface ItemCooldownsGetCooldownsAccessor {
   @Accessor("cooldowns")
   Map<Identifier, ?> skycore$getCooldowns();

   @Accessor("tickCount")
   int skycore$getTickCount();

   @Invoker("getCooldownGroup")
   Identifier skycore$invokeGetCooldownGroup(ItemStack itemStack);
}
