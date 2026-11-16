package tokyo.ghoul.api.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.system.backend.ClientInfo;

import java.util.Optional;
import java.util.OptionalDouble;

@UtilityClass
public class SkyPipelines {
    private final BindGroupLayout SKY_SAMPLERS = BindGroupLayout.builder()
            .withSampler("DepthSampler")
            .withUniform(EvaPipelines.UBO, UniformType.UNIFORM_BUFFER)
            .build();

    public final RenderPipeline SKY_AURORA = sky("sky_aurora", "sky_aurora");
    public final RenderPipeline SKY_SAKURA = sky("sky_sakura", "sky_sakura");
    public final RenderPipeline SKY_PLASMA = sky("sky_plasma", "sky_plasma");
    public final RenderPipeline PLASMA_2 = sky("plasma_2", "plasma2");
    public final RenderPipeline CAUSTIC = sky("caustic", "caustic");
    public final RenderPipeline AURORA = sky("aurora", "aurora");
    public final RenderPipeline AURORA_2 = sky("aurora2", "aurora2");
    public final RenderPipeline NIGHT = sky("night", "night");
    public final RenderPipeline SUMMER = sky("summer", "summer");
    public final RenderPipeline THUNDER = sky("thunder", "thunder");

    private TextureTarget depthCopy;

    public void register() {
    }

    public void copyDepth() {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        int w = main.width;
        int h = main.height;
        if (depthCopy == null) {
            depthCopy = new TextureTarget("Static Sky Depth", w, h, true,
                    main.getColorTexture().getFormat());
        } else if (depthCopy.width != w || depthCopy.height != h) {
            depthCopy.resize(w, h);
        }
        depthCopy.copyDepthFrom(main);
    }

    public void render(RenderPipeline pipeline, EvaUniform uniform) {
        RenderTarget main = Minecraft.getInstance().gameRenderer.mainRenderTarget();
        GpuTextureView depthView = depthCopy != null
                ? depthCopy.getDepthTextureView()
                : main.getDepthTextureView();

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Static sky", main.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(EvaPipelines.UBO, EvaRenderer.getInstance().writeUniform(uniform));
            pass.bindTexture("DepthSampler", depthView, EvaTextures.nearest());
            pass.draw(3, 1, 0, 0);
        }
    }

    private RenderPipeline sky(String name, String shader) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(id("core/skyshader/" + shader))
                .withBindGroupLayout(SKY_SAMPLERS)
                .build());
    }

    private Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), path);
    }
}
