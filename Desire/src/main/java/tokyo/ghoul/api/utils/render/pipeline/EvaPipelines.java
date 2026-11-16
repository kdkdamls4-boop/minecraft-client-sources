package tokyo.ghoul.api.utils.render.pipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.system.backend.ClientInfo;
import tokyo.ghoul.api.utils.render.post.PostPipelines;

@UtilityClass
public class EvaPipelines {
    public final String UBO = "EvaParams";
    private final BindGroupLayout EVA_PARAMS = BindGroupLayout.builder().withUniform(UBO, UniformType.UNIFORM_BUFFER).build();
    private final BindGroupLayout INPUT_SAMPLER = BindGroupLayout.builder().withSampler("InSampler").build();
    private final BindGroupLayout MOTION_INPUT_SAMPLERS = BindGroupLayout.builder()
            .withSampler("MainSampler")
            .withSampler("MainDepthSampler")
            .build();

    public final RenderPipeline RECT = RenderPipelines.register(gui("rect", "rect/rect", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline PILL = RenderPipelines.register(gui("pill_rect", "rect/pill_rect", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline GRADIENT_RECT = RenderPipelines.register(gui("gradient_rect", "rect/gradient_rect", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline TEXTURE_RECT = RenderPipelines.register(gui("texture_rect", "rect/texture_rect", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline BLURRED_RECT = RenderPipelines.register(gui("blurred_rect", "rect/blurred_rect", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline HUD_BLURRED_RECT = RenderPipelines.register(gui("hud_blurred_rect", "rect/hud_blurred_rect", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline GLASS_SHADOW = RenderPipelines.register(gui("glass_shadow", "rect/glass_shadow", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline LENS_GLASS = RenderPipelines.register(gui("lens_glass", "rect/lens_glass", DefaultVertexFormat.POSITION_TEX_COLOR).build());
    public final RenderPipeline FLAT_COLOR = RenderPipelines.register(gui("flat_color", "flat_color", DefaultVertexFormat.POSITION_COLOR).build());
    public final RenderPipeline TEXT_10 = text("text_10", 10.0f);
    public final RenderPipeline TEXT_12 = text("text_12", 12.0f);
    public final RenderPipeline TEXT_20 = text("text_20", 20.0f);
    public final RenderPipeline TEXT_32 = text("text_32", 32.0f);
    public final RenderPipeline TEXT = TEXT_32;

    public final RenderPipeline BLUR_DOWNSCALE = fullscreen("blur_downscale", "post/blur/downscale");
    public final RenderPipeline BLUR_UPSCALE = fullscreen("blur_upscale", "post/blur/upscale");
    public final RenderPipeline GAUSSIAN_H = fullscreen("gaussian_h", "post/blur/gaussian_h");
    public final RenderPipeline GAUSSIAN_V = fullscreen("gaussian_v", "post/blur/gaussian_v");
    public final RenderPipeline MAGNIFIER = fullscreen("magnifier", "post/magnifier");
    public final RenderPipeline AMBIENCE_SATURATION = fullscreen("ambience_saturation", "post/ambience_saturation");
    public final RenderPipeline MOTION_BLUR = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(id("pipeline/motion_blur"))
            .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
            .withFragmentShader(shader("post/motionblur/motion_blur"))
            .withBindGroupLayout(MOTION_INPUT_SAMPLERS)
            .withBindGroupLayout(EVA_PARAMS)
            .build());

    public void register() {
        RenderPipelines.register(PostPipelines.JUMP_EFFECT);
        RenderPipelines.register(PostPipelines.WET_SOLUTION);
        RenderPipelines.register(PostPipelines.WET_WORLD);
        RenderPipelines.register(PostPipelines.ATMOSPHERE_FOG);
        RenderPipelines.register(PostPipelines.FOG_BLUR);
        RenderPipelines.register(PostPipelines.HAND_HALO);
        RenderPipelines.register(PostPipelines.HAND_ELECTRIC);
        RenderPipelines.register(PostPipelines.HAND_FLAME);
        RenderPipelines.register(PostPipelines.HAND_TRAIL_ACCUM);
        RenderPipelines.register(PostPipelines.HAND_BLUR_DOWN);
        RenderPipelines.register(PostPipelines.HAND_BLUR_UP);
    }

    private RenderPipeline.Builder gui(String name, String shader, VertexFormat format) {
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.GLOBALS_SNIPPET)
                .withBindGroupLayout(BindGroupLayouts.MATRICES_PROJECTION);
        if (format == DefaultVertexFormat.POSITION_TEX_COLOR) {
            builder.withBindGroupLayout(BindGroupLayouts.SAMPLER0);
        }
        return builder.withBindGroupLayout(EVA_PARAMS)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(shader(shader))
                .withFragmentShader(shader(shader))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
                .withCull(false)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .withVertexBinding(0, format)
                .withPrimitiveTopology(PrimitiveTopology.QUADS);
    }

    private RenderPipeline fullscreen(String name, String shader) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withVertexShader(Identifier.withDefaultNamespace("core/screenquad"))
                .withFragmentShader(shader(shader))
                .withBindGroupLayout(INPUT_SAMPLER)
                .withBindGroupLayout(EVA_PARAMS)
                .build());
    }

    private RenderPipeline text(String name, float range) {
        return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                .withLocation(id("pipeline/" + name))
                .withFragmentShader(shader("msdf_text"))
                .withShaderDefine("MSDF_RANGE", range)
                .withBindGroupLayout(EVA_PARAMS)
                .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
                .build());
    }

    private Identifier shader(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), "core/" + path);
    }

    private Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ClientInfo.NAME.toLowerCase(), path);
    }
}
