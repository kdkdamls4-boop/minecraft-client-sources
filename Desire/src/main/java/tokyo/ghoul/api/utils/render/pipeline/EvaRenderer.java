package tokyo.ghoul.api.utils.render.pipeline;


import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tokyo.ghoul.api.system.interfaces.QuickImports;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Optional;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.DynamicUniformStorage;

public final class EvaRenderer implements QuickImports {
    private static final EvaRenderer INSTANCE = new EvaRenderer();
    private static final int MIN_BUFFER_SIZE = 4096;

    private final ByteBufferBuilder allocator = new ByteBufferBuilder(786432);
    private final List<Command> commands = new ArrayList<>();
    private final Map<VertexFormat, VertexStorage> storages = new HashMap<>();
    private final PoseStack matrices = new PoseStack();

    private @Nullable DynamicUniformStorage<EvaUniform> uniforms;
    private @Nullable DynamicUniformStorage<EvaMotionBlurUniform> motionBlurUniforms;
    private @Nullable ProjectionMatrixBuffer projection;
    private @Nullable ScissorBox scissor;
    private final Deque<ScissorBox> scissors = new ArrayDeque<>();

    public static EvaRenderer getInstance() {
        return INSTANCE;
    }

    public PoseStack matrices() {
        return matrices;
    }

    public BufferBuilder begin(RenderPipeline pipeline) {
        ByteBufferBuilder alloc = recordingUnder ? underAllocator : allocator;
        return new BufferBuilder(alloc, pipeline.getPrimitiveTopology(), pipeline.getVertexFormatBinding(0));
    }

    public void submit(BufferBuilder builder, RenderPipeline pipeline, TextureSetup texture, EvaUniform uniform) {
        MeshData built = builder.build();
        if (built == null) return;
        List<Command> target = recordingUnder ? underCommands : commands;
        target.add(new Command(built, pipeline, texture, uniform, scissor));
    }

    public void submit(BufferBuilder builder, RenderPipeline pipeline, EvaUniform uniform) {
        submit(builder, pipeline, TextureSetup.noTexture(), uniform);
    }

    public GpuBufferSlice writeUniform(EvaUniform uniform) {
        if (uniforms == null) uniforms = new DynamicUniformStorage<>("Desire Params", EvaUniform.SIZE, 512);
        return uniforms.writeUniform(uniform);
    }

    public GpuBufferSlice writeMotionBlurUniform(EvaMotionBlurUniform uniform) {
        if (motionBlurUniforms == null) motionBlurUniforms = new DynamicUniformStorage<>("Desire Motion Blur Params", EvaMotionBlurUniform.SIZE, 16);
        return motionBlurUniforms.writeUniform(uniform);
    }

    public void endMotionBlurFrame() {
        if (motionBlurUniforms != null) {
            motionBlurUniforms.endFrame();
        }
    }

    public void fullscreen(RenderPipeline pipeline, RenderTarget target, GpuTextureView source, GpuSampler sampler, EvaUniform uniform) {
        GpuBufferSlice slice = writeUniform(uniform);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Desire post", target.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
            pass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(EvaPipelines.UBO, slice);
            pass.bindTexture("InSampler", source, sampler);
            pass.draw(3, 1, 0, 0);
        } catch (Throwable ignored) {
        }
    }

    public void motionBlur(RenderTarget target, GpuTextureView source, GpuTextureView depthSource, GpuSampler sampler, EvaMotionBlurUniform uniform) {
        GpuBufferSlice slice = writeMotionBlurUniform(uniform);

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Desire motion blur", target.getColorTextureView(), Optional.empty(), null, OptionalDouble.empty())) {
            pass.setPipeline(EvaPipelines.MOTION_BLUR);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform(EvaPipelines.UBO, slice);
            pass.bindTexture("MainSampler", source, sampler);
            pass.bindTexture("MainDepthSampler", depthSource, sampler);
            pass.draw(3, 1, 0, 0);
        } catch (Throwable ignored) {
        }
    }

    public void pushScissor(float x, float y, float width, float height) {
        float scale = (float) mc.getWindow().getGuiScale();
        int sx = (int) (x * scale);
        int sy = (int) ((mc.getWindow().getGuiScaledHeight() - y - height) * scale);
        int sw = (int) (width * scale);
        int sh = (int) (height * scale);

        ScissorBox box = new ScissorBox(sx, sy, sw, sh);
        ScissorBox parent = scissors.peek();
        if (parent != null) {
            int left = Math.max(parent.x, sx);
            int bottom = Math.max(parent.y, sy);
            int right = Math.min(parent.x + parent.width, sx + sw);
            int top = Math.min(parent.y + parent.height, sy + sh);
            box = new ScissorBox(left, bottom, Math.max(0, right - left), Math.max(0, top - bottom));
        }

        scissors.push(box);
        scissor = box;
    }

    public void popScissor() {
        if (!scissors.isEmpty()) scissors.pop();
        scissor = scissors.peek();
    }

    private final ByteBufferBuilder underAllocator = new ByteBufferBuilder(65536);
    private final List<Command> underCommands = new ArrayList<>();
    private boolean recordingUnder = false;

    public void beginUnder() {
        recordingUnder = true;
    }

    public void endUnder() {
        recordingUnder = false;
    }

    public void flushUnder() {
        flush(underCommands, underAllocator, false);
    }

    public void flush() {
        flush(commands, allocator, true);
    }

    public void flushLayer() {
        flush(commands, allocator, false);
    }

    private void flush(List<Command> targetCommands, ByteBufferBuilder targetAlloc, boolean endFrame) {
        if (targetCommands.isEmpty()) {
            if (endFrame) {
                if (uniforms != null) uniforms.endFrame();
                if (motionBlurUniforms != null) motionBlurUniforms.endFrame();
            }
            targetAlloc.clear();
            return;
        }

        Window window = mc.getWindow();
        RenderTarget framebuffer = mc.gameRenderer.mainRenderTarget();
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();

        if (projection == null) projection = new ProjectionMatrixBuffer("desire");
        if (uniforms == null) uniforms = new DynamicUniformStorage<>("Desire Params", EvaUniform.SIZE, 512);

        int[] baseVertices = uploadVertices(encoder, targetCommands);

        int maxIndices = 0;
        for (Command command : targetCommands) maxIndices = Math.max(maxIndices, command.built.drawState().indexCount());

        RenderSystem.AutoStorageIndexBuffer shape = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = shape.getBuffer(maxIndices);
        IndexType indexType = shape.type();

        RenderSystem.backupProjectionMatrix();
        float guiWidth = (float) window.getWidth() / (float) window.getGuiScale();
        float guiHeight = (float) window.getHeight() / (float) window.getGuiScale();
        Matrix4f projectionMatrix = new Matrix4f().setOrtho(0.0f, guiWidth, guiHeight, 0.0f, 1000.0f, 21000.0f);
        RenderSystem.setProjectionMatrix(projection.getBuffer(projectionMatrix), ProjectionType.ORTHOGRAPHIC);
        GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0f, 0f, -11000f), new Vector4f(1f, 1f, 1f, 1f), new Vector3f(), new Matrix4f());

        GpuBufferSlice[] slices = new GpuBufferSlice[targetCommands.size()];
        for (int i = 0; i < targetCommands.size(); i++) slices[i] = uniforms.writeUniform(targetCommands.get(i).uniform);

        try (RenderPass pass = encoder.createRenderPass(() -> "Desire GUI", framebuffer.getColorTextureView(), Optional.empty(), framebuffer.useDepth ? framebuffer.getDepthTextureView() : null, OptionalDouble.empty())) {
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transforms);

            for (int i = 0; i < targetCommands.size(); i++) {
                Command command = targetCommands.get(i);

                pass.setPipeline(command.pipeline);
                pass.setVertexBuffer(0, storages.get(command.pipeline.getVertexFormatBinding(0)).buffer.slice());
                pass.setUniform(EvaPipelines.UBO, slices[i]);

                if (command.scissor != null) {
                    int left = Math.max(0, command.scissor.x);
                    int bottom = Math.max(0, command.scissor.y);
                    int right = Math.min(framebuffer.width, command.scissor.x + command.scissor.width);
                    int top = Math.min(framebuffer.height, command.scissor.y + command.scissor.height);
                    if (right <= left || top <= bottom) continue;
                    pass.enableScissor(left, bottom, right - left, top - bottom);
                } else {
                    pass.disableScissor();
                }

                if (command.texture.texure0() != null) pass.bindTexture("Sampler0", command.texture.texure0(), command.texture.sampler0());

                pass.setIndexBuffer(indexBuffer, indexType);
                pass.drawIndexed(command.built.drawState().indexCount(), 1, 0, baseVertices[i], 0);
            }
        }

        RenderSystem.restoreProjectionMatrix();
        if (endFrame) {
            if (uniforms != null) uniforms.endFrame();
            if (motionBlurUniforms != null) motionBlurUniforms.endFrame();
        }
        for (Command command : targetCommands) command.built.close();
        targetCommands.clear();
        targetAlloc.clear();
    }

    private int[] uploadVertices(CommandEncoder encoder, List<Command> targetCommands) {
        Map<VertexFormat, Integer> sizes = new HashMap<>();
        int[] offsets = new int[targetCommands.size()];
        int[] baseVertices = new int[targetCommands.size()];

        for (int i = 0; i < targetCommands.size(); i++) {
            Command command = targetCommands.get(i);
            VertexFormat format = command.pipeline.getVertexFormatBinding(0);
            int offset = sizes.getOrDefault(format, 0);

            offsets[i] = offset;
            baseVertices[i] = offset / format.getVertexSize();
            sizes.put(format, offset + command.built.drawState().vertexCount() * format.getVertexSize());
        }

        sizes.forEach((format, size) -> storages.computeIfAbsent(format, VertexStorage::new).ensureCapacity(size));

        for (int i = 0; i < targetCommands.size(); i++) {
            Command command = targetCommands.get(i);
            VertexFormat format = command.pipeline.getVertexFormatBinding(0);
            int size = command.built.drawState().vertexCount() * format.getVertexSize();
            encoder.writeToBuffer(storages.get(format).buffer.slice(offsets[i], size), command.built.vertexBuffer());
        }

        return baseVertices;
    }

    private static final class VertexStorage {
        private final VertexFormat format;
        private GpuBuffer buffer;
        private int capacity;

        private VertexStorage(VertexFormat format) {
            this.format = format;
        }

        private void ensureCapacity(int size) {
            if (buffer != null && capacity >= size) return;
            if (buffer != null) buffer.close();
            capacity = Math.max(size, MIN_BUFFER_SIZE);
            buffer = RenderSystem.getDevice().createBuffer(() -> "Desire vertices " + format, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, capacity);
        }
    }

    private record Command(MeshData built, RenderPipeline pipeline, TextureSetup texture, EvaUniform uniform, @Nullable ScissorBox scissor) { }

    private record ScissorBox(int x, int y, int width, int height) { }
}
