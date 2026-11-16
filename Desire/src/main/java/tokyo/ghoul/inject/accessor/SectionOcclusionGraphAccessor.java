package tokyo.ghoul.inject.accessor;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SectionOcclusionGraph.class)
public interface SectionOcclusionGraphAccessor {
    @Accessor("loadedChunks")
    LongOpenHashSet ghoul$getLoadedChunks();

    @Accessor("emptySections")
    LongOpenHashSet ghoul$getEmptySections();
}
