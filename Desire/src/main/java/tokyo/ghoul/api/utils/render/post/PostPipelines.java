package tokyo.ghoul.api.utils.render.post;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.BlendFactor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class PostPipelines {
    private static final String NS = "desire";

    private static final BindGroupLayout HAND_MASK_LAYOUT = BindGroupLayout.builder()
            .withSampler("BeforeSampler")
            .withSampler("AfterSampler")
            .withSampler("DepthBeforeSampler")
            .withSampler("DepthAfterSampler")
            .withUniform("MaskData", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout RICH_HANDS_COMPOSITE_LAYOUT = BindGroupLayout.builder()
            .withSampler("Sampler0")
            .withSampler("Sampler1")
            .withUniform("HandsData", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout RICH_HANDS_BLUR_LAYOUT = BindGroupLayout.builder()
            .withSampler("CurrentInput")
            .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout RICH_HANDS_TRAIL_LAYOUT = BindGroupLayout.builder()
            .withSampler("Sampler0")
            .withUniform("TrailData", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout JUMP_EFFECT_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler")
            .withSampler("DepthSampler")
            .withUniform("JumpData", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout WET_SURFACE_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler")
            .withSampler("DepthSampler")
            .withUniform("WetSurfaceUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout WET_WORLD_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler")
            .withSampler("DepthSampler")
            .withUniform("WetWorldUniforms", UniformType.UNIFORM_BUFFER)
            .build();
    private static final BindGroupLayout ATMOSPHERE_LAYOUT = BindGroupLayout.builder()
            .withSampler("DepthSampler").withSampler("SceneSampler")
            .withUniform("AtmosphereUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout FOG_BLUR_LAYOUT = BindGroupLayout.builder()
            .withSampler("SceneSampler").withSampler("BlurSampler").withSampler("DepthSampler")
            .withUniform("FogBlurUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout HAND_HALO_LAYOUT = BindGroupLayout.builder()
            .withSampler("BlurredSampler")
            .withSampler("MaskSampler")
            .withUniform("HandHaloUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout HAND_ELECTRIC_LAYOUT = BindGroupLayout.builder()
            .withSampler("BlurredSampler")
            .withSampler("MaskSampler")
            .withUniform("HandElectricUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout HAND_FLAME_LAYOUT = BindGroupLayout.builder()
            .withSampler("BlurredSampler")
            .withSampler("MaskSampler")
            .withUniform("HandCompositeUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout HAND_TRAIL_ACCUM_LAYOUT = BindGroupLayout.builder()
            .withSampler("PrevSampler")
            .withSampler("InjectSampler")
            .withUniform("HandTrailUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    private static final BindGroupLayout HAND_BLUR_LAYOUT = BindGroupLayout.builder()
            .withSampler("CurrentInput")
            .withUniform("HandBlurUniforms", UniformType.UNIFORM_BUFFER)
            .build();

    public static final GpuFormat MASK_RAW_FORMAT = GpuFormat.RGBA16_FLOAT;
    public static final GpuFormat EFFECT_FORMAT = GpuFormat.RGBA8_UNORM;

    public static final RenderPipeline HAND_MASK = RenderPipeline.builder()
            .withLocation(id("pipeline/post/hand_mask"))
            .withVertexShader(id("core/hands/pass"))
            .withFragmentShader(id("core/hands/hands_mask"))
            .withBindGroupLayout(HAND_MASK_LAYOUT)
            .withColorTargetState(new ColorTargetState(Optional.empty(), MASK_RAW_FORMAT, ColorTargetState.WRITE_ALL))
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline RICH_HANDS_OVERLAY = richHandsPipeline("overlay", "hands_overlay", new ColorTargetState(BlendFunction.TRANSLUCENT));
    public static final RenderPipeline RICH_HANDS_GLOW = richHandsPipeline("glow", "hands_glow", new ColorTargetState(BlendFunction.LIGHTNING));
    public static final RenderPipeline RICH_HANDS_MIRROR = richHandsPipeline("mirror", "hands_mirror", new ColorTargetState(BlendFunction.TRANSLUCENT));
    public static final RenderPipeline RICH_HANDS_OUTLINE = richHandsPipeline("outline", "hands_outline", new ColorTargetState(BlendFunction.TRANSLUCENT));
    public static final RenderPipeline RICH_HANDS_KAWASE_DOWN = richHandsBlurPipeline("kawase_down", "hands_kawase_down");
    public static final RenderPipeline RICH_HANDS_KAWASE_UP = richHandsBlurPipeline("kawase_up", "hands_kawase_up");
    private static final ColorTargetState RICH_HANDS_PREMULTIPLIED = new ColorTargetState(new BlendFunction(BlendFactor.ONE, BlendFactor.ONE_MINUS_SRC_ALPHA));
    public static final RenderPipeline RICH_HANDS_TRAIL_FADE = richHandsTrailPipeline("trail_fade", "hands_trail_fade", ColorTargetState.DEFAULT);
    public static final RenderPipeline RICH_HANDS_TRAIL_COLOR = richHandsTrailPipeline("trail_color", "hands_trail_color", RICH_HANDS_PREMULTIPLIED);
    public static final RenderPipeline RICH_HANDS_TRAIL_MODEL = richHandsTrailPipeline("trail_model", "hands_blit", new ColorTargetState(BlendFunction.TRANSLUCENT));
    public static final RenderPipeline RICH_HANDS_TRAIL_BLIT = richHandsTrailPipeline("trail_blit", "hands_blit", RICH_HANDS_PREMULTIPLIED);

    public static final RenderPipeline HAND_HALO = handGlowPipeline("hand_halo", "hand_halo", HAND_HALO_LAYOUT);
    public static final RenderPipeline HAND_ELECTRIC = handGlowPipeline("hand_electric", "hand_electric", HAND_ELECTRIC_LAYOUT);
    public static final RenderPipeline HAND_FLAME = handGlowPipeline("hand_flame", "hand_flame", HAND_FLAME_LAYOUT);
    public static final RenderPipeline HAND_TRAIL_ACCUM = RenderPipeline.builder()
            .withLocation(id("pipeline/post/hand_trail_accum"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/hand_trail_accum"))
            .withBindGroupLayout(HAND_TRAIL_ACCUM_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();
    public static final RenderPipeline HAND_BLUR_DOWN = RenderPipeline.builder()
            .withLocation(id("pipeline/post/hand_blur_down"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/hand_blur_down"))
            .withBindGroupLayout(HAND_BLUR_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();
    public static final RenderPipeline HAND_BLUR_UP = RenderPipeline.builder()
            .withLocation(id("pipeline/post/hand_blur_up"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/hand_blur_up"))
            .withBindGroupLayout(HAND_BLUR_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline JUMP_EFFECT = RenderPipeline.builder()
            .withLocation(id("pipeline/world/jump_effect"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/jump_effect"))
            .withBindGroupLayout(JUMP_EFFECT_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline WET_SOLUTION = RenderPipeline.builder()
            .withLocation(id("pipeline/world/wet_solution"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/wet_solution"))
            .withBindGroupLayout(WET_SURFACE_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline WET_WORLD = RenderPipeline.builder()
            .withLocation(id("pipeline/world/wet_world"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/wet_world"))
            .withBindGroupLayout(WET_WORLD_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline ATMOSPHERE_FOG = RenderPipeline.builder()
            .withLocation(id("pipeline/world/atmosphere_fog"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/atmosphere_fog"))
            .withBindGroupLayout(ATMOSPHERE_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    public static final RenderPipeline FOG_BLUR = RenderPipeline.builder()
            .withLocation(id("pipeline/world/fog_blur"))
            .withVertexShader(id("post/blurs/kawase_common"))
            .withFragmentShader(id("post/fog_blur"))
            .withBindGroupLayout(FOG_BLUR_LAYOUT)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(Optional.empty())
            .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
            .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
            .withCull(false)
            .build();

    private static RenderPipeline richHandsPipeline(String name, String fragment, ColorTargetState colorTarget) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/hands/" + name))
                .withVertexShader(id("core/hands/pass"))
                .withFragmentShader(id("core/hands/" + fragment))
                .withBindGroupLayout(RICH_HANDS_COMPOSITE_LAYOUT)
                .withColorTargetState(colorTarget)
                .withDepthStencilState(Optional.empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static RenderPipeline richHandsBlurPipeline(String name, String fragment) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/hands/" + name))
                .withVertexShader(id("core/hands/pass"))
                .withFragmentShader(id("core/hands/" + fragment))
                .withBindGroupLayout(RICH_HANDS_BLUR_LAYOUT)
                .withColorTargetState(ColorTargetState.DEFAULT)
                .withDepthStencilState(Optional.empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static RenderPipeline richHandsTrailPipeline(String name, String fragment, ColorTargetState colorTarget) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/hands/" + name))
                .withVertexShader(id("core/hands/pass"))
                .withFragmentShader(id("core/hands/" + fragment))
                .withBindGroupLayout(RICH_HANDS_TRAIL_LAYOUT)
                .withColorTargetState(colorTarget)
                .withDepthStencilState(Optional.empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static RenderPipeline handGlowPipeline(String name, String fragment, BindGroupLayout layout) {
        return RenderPipeline.builder()
                .withLocation(id("pipeline/post/" + name))
                .withVertexShader(id("post/blurs/kawase_common"))
                .withFragmentShader(id("post/" + fragment))
                .withBindGroupLayout(layout)
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withDepthStencilState(Optional.empty())
                .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX)
                .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
                .withCull(false)
                .build();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NS, path);
    }

    private PostPipelines() {
    }
}
