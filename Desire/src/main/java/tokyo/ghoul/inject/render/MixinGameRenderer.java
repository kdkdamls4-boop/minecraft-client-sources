package tokyo.ghoul.inject.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.render.Render3DEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.render.AmbienceSaturationProgram;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.KawaseBlurProgram;
import tokyo.ghoul.api.utils.render.MagnifierProgram;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.rotation.RaytracingUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.features.modules.render.MotionBlurModule;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tokyo.ghoul.client.features.modules.render.ChinaHatModule;
import tokyo.ghoul.client.features.modules.render.HandsModule;
import tokyo.ghoul.client.features.modules.render.targetesp.TargetEspModule;
import tokyo.ghoul.client.features.modules.render.targetesp.render.SurfaceLightingPass;
import tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspFrame;
import tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspLightning;
import tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspRenderer;
import tokyo.ghoul.client.features.modules.render.targetesp.render.WorldProjection;
import tokyo.ghoul.client.ui.menu.AltScreen;
import tokyo.ghoul.client.ui.menu.MainMenuScreen;
import tokyo.ghoul.client.ui.widget.overlay.HotbarWidget;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {
    @Inject(method = "shouldRenderBlockOutline", at = @At("HEAD"), cancellable = true)
    private void desire$hideVanillaBlockOutline(CallbackInfoReturnable<Boolean> cir) {
        if (tokyo.ghoul.client.features.modules.render.BlockOverlayModule.hidesVanillaOutline()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onTiltViewWhenHurt(CameraRenderState cameraState, PoseStack matrices, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isHurtCamera()) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V"))
    private void prepareInterfaceBlur(CallbackInfo ci) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;
        AmbienceSaturationProgram.render();
        MotionBlurModule.getInstance().render();
        KawaseBlurProgram.render(RenderUtil.matrices());
        RenderUtil.matrices().setIdentity();
        HotbarWidget.renderHotbarUnder(RenderUtil.matrices());
        EvaRenderer.getInstance().flushUnder();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V", shift = At.Shift.AFTER))
    private void renderInterface(CallbackInfo ci) {
        if (MainMenuScreen.isActive()) {
            MainMenuScreen.renderBackdropLayer();
            EvaRenderer.getInstance().flushLayer();
            KawaseBlurProgram.captureNow(MainMenuScreen.BLUR_ITERATIONS);
            MainMenuScreen.renderInterfaceLayer();
            EvaRenderer.getInstance().flush();
        } else if (AltScreen.isActive()) {
            AltScreen.renderBackdropLayer();
            EvaRenderer.getInstance().flushLayer();
            KawaseBlurProgram.captureNow();
            AltScreen.renderInterfaceLayer();
            EvaRenderer.getInstance().flush();
        } else {
            EvaRenderer.getInstance().flush();
        }
        MagnifierProgram.render();
    }

    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private float onRenderWorld(float delta, float first, float second) {
        if (Minecraft.getInstance().player == null) return 0;

        if (RemovalsModule.getInstance().isBadEffects()) {
            return 0;
        }
        return Mth.lerp(delta, first, second);
    }


    private static final String LEVEL_RENDER = "Lnet/minecraft/client/renderer/LevelRenderer;render(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/renderer/state/level/CameraRenderState;Lorg/joml/Matrix4fc;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V";
    private static final String RENDER_HAND = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V";

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private FeatureRenderDispatcher featureRenderDispatcher;
    @Shadow @Final private SubmitNodeStorage handAndScreenSubmitNodeStorage;

    @Unique private Matrix4f desire$worldProjection;

    @Unique
    private Matrix4f desire$worldProjection() {
        if (desire$worldProjection == null) desire$worldProjection = new Matrix4f();
        return desire$worldProjection;
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = LEVEL_RENDER))
    private void captureWorldMatrices(DeltaTracker tracker, CallbackInfo ci, @Local(ordinal = 0) Matrix4f projection) {
        desire$worldProjection().set(projection);
        WorldProjection.m_2695(new Matrix4f(viewRotation()), projection);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = LEVEL_RENDER, shift = At.Shift.AFTER))
    private void renderWorldEffects(DeltaTracker tracker, CallbackInfo ci) {
        if (minecraft.player == null || minecraft.level == null) return;
        TargetEspModule targetEsp = TargetEspModule.getInstance();
        ChinaHatModule chinaHat = ChinaHatModule.getInstance();
        if (targetEsp.isEnabled()) {
            TargetEspRenderer.m_7901();
            targetEsp.renderWorld(worldFrame(tracker));
        }
        if (chinaHat.isEnabled()) chinaHat.m_5668(worldFrame(tracker));
    }

    @Unique
    private TargetEspFrame worldFrame(DeltaTracker tracker) {
        Matrix4f view = new Matrix4f(viewRotation());
        PoseStack pose = new PoseStack();
        pose.mulPose(new Matrix4f(view));
        float partialTicks = tracker.getGameTimeDeltaPartialTick(false);
        return new TargetEspFrame(tracker, pose, minecraft.gameRenderer.mainCamera().position(), partialTicks,
                new Matrix4f(desire$worldProjection()), view);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = RENDER_HAND))
    private void beforeHands(DeltaTracker tracker, CallbackInfo ci) {
        boolean cube = TargetEspRenderer.m_7940();
        if (cube || HandsModule.getInstance().m_8181()) {
            featureRenderDispatcher.renderAllFeatures(handAndScreenSubmitNodeStorage);
        }
        if (cube) TargetEspRenderer.m_7876();
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = RENDER_HAND, shift = At.Shift.AFTER))
    private void compositeHands(DeltaTracker tracker, CallbackInfo ci) {
        if (HandsModule.f_28498) {
            featureRenderDispatcher.renderAllFeatures(handAndScreenSubmitNodeStorage);
            HandsModule.getInstance().m_8130();
        }
    }

    @Inject(method = "mainRenderTarget", at = @At("RETURN"), cancellable = true)
    private void routeHandsTarget(CallbackInfoReturnable<RenderTarget> cir) {
        if (HandsModule.f_28498 && HandsModule.f_28499 != null) {
            cir.setReturnValue(HandsModule.f_28499);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;doEntityOutline()V", shift = At.Shift.AFTER))
    private void renderDeferredTargetEsp(DeltaTracker tracker, boolean renderLevel, CallbackInfo ci) {
        TargetEspRenderer.m_7857();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void endTargetEspFrame(DeltaTracker tracker, boolean renderLevel, CallbackInfo ci) {
        SurfaceLightingPass.m_9270();
        TargetEspLightning.m_1855();
    }

    @Unique
    private Matrix4f viewRotation() {
        return new Matrix4f(minecraft.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.viewRotationMatrix);
    }
}
