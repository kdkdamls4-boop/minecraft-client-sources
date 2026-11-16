package tokyo.ghoul.api.utils.render.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@UtilityClass
public class EvaTextures {
    public GpuTextureView view(Identifier texture) {
        return Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
    }

    public GpuSampler sampler() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    public GpuSampler nearest() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
    }
}
