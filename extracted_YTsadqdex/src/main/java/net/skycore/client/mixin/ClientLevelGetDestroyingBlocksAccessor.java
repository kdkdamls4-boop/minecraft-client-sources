package net.skycore.client.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.server.level.BlockDestructionProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface ClientLevelGetDestroyingBlocksAccessor {
   @Accessor("destroyingBlocks")
   Int2ObjectMap<BlockDestructionProgress> skycore$getDestroyingBlocks();

   @Invoker("getBlockStatePredictionHandler")
   BlockStatePredictionHandler skycore$getBlockStatePredictionHandler();
}
