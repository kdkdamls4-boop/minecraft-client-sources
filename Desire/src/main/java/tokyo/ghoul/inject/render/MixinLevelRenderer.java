package tokyo.ghoul.inject.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.render.AtmosphereRenderer;
import tokyo.ghoul.api.utils.render.StormState;
import tokyo.ghoul.client.features.modules.render.AmbienceModule;
import tokyo.ghoul.client.features.modules.render.AtmosphereModule;
import tokyo.ghoul.client.features.modules.render.GlowEspModule;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import java.util.Set;
import tokyo.ghoul.client.features.modules.render.jumpeffect.JumpEffectModule;

@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Shadow
    @Final
    private RenderTarget entityOutlineTarget;

    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ShaderManager;getPostChain(Lnet/minecraft/resources/Identifier;Ljava/util/Set;)Lnet/minecraft/client/renderer/PostChain;"))
    private PostChain skipVanillaOutlineChain(ShaderManager manager, Identifier id, Set<Identifier> targets, Operation<PostChain> original) {
        if (GlowEspModule.getInstance().replacesOutline() && id.getPath().equals("entity_outline")) return null;
        return original.call(manager, id, targets);
    }

    @Inject(method = "doEntityOutline", at = @At("HEAD"), cancellable = true)
    private void glowEspComposite(CallbackInfo ci) {
        GlowEspModule glow = GlowEspModule.getInstance();
        if (!glow.replacesOutline()) return;
        ci.cancel();
        if (this.levelRenderState.shouldShowEntityOutlines) {
            glow.composite(entityOutlineTarget, Minecraft.getInstance().gameRenderer.mainRenderTarget());
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderJumpEffect(GraphicsResourceAllocator allocator,
                                  DeltaTracker deltaTracker,
                                  boolean renderBlockOutline,
                                  CameraRenderState cameraRenderState,
                                  Matrix4fc projectionMatrix,
                                  GpuBufferSlice fogParameters,
                                  Vector4f skyColor,
                                  boolean hasCapturedFrustum,
                                  CallbackInfo ci) {
        ProjectionUtil.captureLevelProjection(projectionMatrix);
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        AmbienceModule.getInstance().renderWetWorld(cameraRenderState);
        AmbienceModule.getInstance().renderFogBlur(cameraRenderState);
        AtmosphereModule atmosphere = atmosphereRenderTarget(AtmosphereModule.getInstance());
        if (atmosphere != null) {
            AtmosphereRenderer.INSTANCE.render(atmosphere, cameraRenderState, this.levelRenderState.skyRenderState);
        } else if (AtmosphereRenderer.INSTANCE.isArmed()) {
            AtmosphereRenderer.INSTANCE.release();
        }
        JumpEffectModule.getInstance().render(cameraRenderState, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    private static AtmosphereModule atmosphereRenderTarget(AtmosphereModule feature) {
        if (feature == null) {
            return null;
        }
        boolean ambient = feature.isEnabled() && feature.usesAtmosphere();
        boolean liveStrike = StormState.INSTANCE.hasVisibleStrike();
        return ambient || liveStrike ? feature : null;
    }
}
