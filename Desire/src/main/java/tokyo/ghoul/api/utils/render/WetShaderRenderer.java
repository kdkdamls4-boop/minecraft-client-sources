package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import tokyo.ghoul.client.features.modules.render.AmbienceModule;
import tokyo.ghoul.api.utils.render.post.PostFx;
import tokyo.ghoul.api.utils.render.post.PostPipelines;
import tokyo.ghoul.api.utils.render.post.PostTarget;

import java.nio.ByteBuffer;

public final class WetShaderRenderer {
    private static final WetShaderRenderer INSTANCE = new WetShaderRenderer();
    private static final int SOLUTION_SIZE = new Std140SizeCalculator()
            .putMat4f().putMat4f().putMat4f().putMat4f()
            .putVec4().putVec4().putVec4().putVec4()
            .get();
    private static final int WORLD_SIZE = new Std140SizeCalculator()
            .putMat4f().putMat4f().putMat4f()
            .putVec4().putVec4().putVec4().putVec4().putVec4().putVec4()
            .get();

    private GpuBuffer solutionUniforms;
    private GpuBuffer worldUniforms;
    private PostTarget sceneCopy;

    private WetShaderRenderer() {
    }

    public static WetShaderRenderer getInstance() {
        return INSTANCE;
    }

    public void render(AmbienceModule sky, CameraRenderState camera) {
        if (sky == null || !sky.isWetWorldEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (camera == null || !camera.initialized
                || target == null || target.width <= 1 || target.height <= 1
                || target.getColorTexture() == null || target.getColorTextureView() == null
                || target.getDepthTextureView() == null
                || camera.projectionMatrix == null || camera.viewRotationMatrix == null) {
            return;
        }

        if (sceneCopy == null) sceneCopy = new PostTarget("static-wet-scene", PostPipelines.EFFECT_FORMAT, false);
        TextureTarget scene = sceneCopy.ensure(target.width, target.height);
        if (scene == null || scene.getColorTexture() == null) return;

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                target.getColorTexture(), scene.getColorTexture(),
                0, 0, 0, 0, 0, target.width, target.height
        );

        if (sky.wetMode.is("Solution New")) {
            renderWorld(sky, camera, target, scene);
        } else {
            renderSolution(sky, camera, target, scene);
        }
    }

    private void renderSolution(AmbienceModule sky, CameraRenderState camera, RenderTarget target, TextureTarget scene) {
        Matrix4f projection = new Matrix4f(camera.projectionMatrix);
        Matrix4f view = new Matrix4f(camera.viewRotationMatrix);
        Matrix4f inverseProjection = new Matrix4f(projection).invert();
        Matrix4f inverseView = new Matrix4f(view).invert();
        inverseView.m30((float) camera.pos.x());
        inverseView.m31((float) camera.pos.y());
        inverseView.m32((float) camera.pos.z());
        float reverseZ = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;

        if (solutionUniforms == null) {
            solutionUniforms = PostFx.createUniforms("Desire Wet Solution UBO", SOLUTION_SIZE);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, SOLUTION_SIZE)
                    .putMat4f(view)
                    .putMat4f(projection)
                    .putMat4f(inverseProjection)
                    .putMat4f(inverseView)
                    .putVec4(target.width, target.height, PostFx.shaderTime(), sky.wetness.getValue())
                    .putVec4(sky.puddles.getValue(), sky.reflection.getValue(), 56.0F, 0.8F)
                    .putVec4(sky.rain.getValue(), 10.0F, 1.0F, AmbienceModule.getSaturation())
                    .putVec4(reverseZ, 0.0F, 0.0F, 0.0F)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(solutionUniforms.slice(), data);
        }
        pass(PostPipelines.WET_SOLUTION, "Desire Wet Solution", target, scene, "WetSurfaceUniforms", solutionUniforms);
    }

    private void renderWorld(AmbienceModule sky, CameraRenderState camera, RenderTarget target, TextureTarget scene) {
        Matrix4f projection = new Matrix4f(camera.projectionMatrix);
        Matrix4f viewRot = new Matrix4f(camera.viewRotationMatrix);
        Matrix4f viewToWorld = new Matrix4f(viewRot).invert();
        Matrix4f invProjection = new Matrix4f(projection).invert();
        Vec3 camPos = new Vec3(camera.pos.x(), camera.pos.y(), camera.pos.z());
        float reverseZ = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F;
        float fov = camera.hudFov;
        int viewDist = Minecraft.getInstance().options.renderDistance().get();
        float fogEnd = viewDist * 16.0f;
        float fogStart = fogEnd * 0.75f;
        float quality = sky.wetQualityIndex();
        float rain = sky.newRain.getValue();
        float reflection = sky.newReflection.getValue();
        float darkening = Math.min(0.8F, sky.newWetness.getValue() * 0.5F);
        float time = (float) ((System.currentTimeMillis() % 120_000L) / 1000.0);

        if (worldUniforms == null) {
            worldUniforms = PostFx.createUniforms("Desire Wet World UBO", WORLD_SIZE);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, WORLD_SIZE)
                    .putMat4f(invProjection)
                    .putMat4f(projection)
                    .putMat4f(viewToWorld)
                    .putVec4((float) camPos.x, (float) camPos.y, (float) camPos.z, fov)
                    .putVec4(target.width, target.height, reverseZ, 0.0F)
                    .putVec4(reflection, darkening, time, 0.0F)
                    .putVec4(quality, sky.ripples.getValue() ? 1.0F : 0.0F, sky.rippleSpeed.getValue(), rain)
                    .putVec4(fogStart, fogEnd, 0.0F, 0.0F)
                    .putVec4(0.0F, 0.0F, 0.0F, 0.0F)
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(worldUniforms.slice(), data);
        }
        pass(PostPipelines.WET_WORLD, "Desire Wet World", target, scene, "WetWorldUniforms", worldUniforms);
    }

    private void pass(RenderPipeline pipeline, String label, RenderTarget target, TextureTarget scene,
                      String uniformName, GpuBuffer uniforms) {
        PostFx.pass(label, pipeline, target, p -> {
            p.setUniform(uniformName, uniforms);
            p.bindTexture("SceneSampler", scene.getColorTextureView(), PostFx.linearSampler());
            p.bindTexture("DepthSampler", target.getDepthTextureView(), PostFx.nearestSampler());
        });
    }
}
