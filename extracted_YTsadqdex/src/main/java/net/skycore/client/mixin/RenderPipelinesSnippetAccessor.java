package net.skycore.client.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesSnippetAccessor {
    @Accessor("GUI_SNIPPET")
    static RenderPipeline.Snippet skycore$getGuiSnippet() {
        throw new AssertionError("Mixin accessor was not applied");
    }

    @Accessor("GUI_TEXTURED_SNIPPET")
    static RenderPipeline.Snippet skycore$getGuiTexturedSnippet() {
        throw new AssertionError("Mixin accessor was not applied");
    }
}
