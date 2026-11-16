package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import tokyo.ghoul.api.utils.render.post.PostFx;
import tokyo.ghoul.api.utils.render.post.PostPipelines;
import tokyo.ghoul.api.utils.render.post.PostTarget;
import tokyo.ghoul.client.features.modules.render.AtmosphereModule;

import java.nio.ByteBuffer;

public final class AtmosphereRenderer {
    private static final float DENSITY_SCALE = 0.005F;

    private static final float HAZE_DENSITY_SCALE = 0.02F;

    private static final double ORIGIN_WRAP = 65536.0D;

    private static final int UNIFORM_SIZE = 304;

    public static final AtmosphereRenderer INSTANCE = new AtmosphereRenderer();

    private GpuBuffer uniforms;
    private final PostTarget sceneCopy = new PostTarget("desire-atmosphere-scene", PostPipelines.EFFECT_FORMAT, false);
    private final Vector4f boltTop = new Vector4f();
    private final Vector4f boltGround = new Vector4f();
    private boolean armed;

    private AtmosphereRenderer() {
    }

    public boolean isArmed() {
        return this.armed;
    }

    private GpuBuffer uniforms() {
        if (this.uniforms == null || this.uniforms.isClosed()) {
            this.uniforms = PostFx.createUniforms("Desire Atmosphere UBO", UNIFORM_SIZE);
        }
        return this.uniforms;
    }

    public void render(AtmosphereModule feature, CameraRenderState cameraState, SkyRenderState skyState) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.gameRenderer.mainRenderTarget();
        if (!valid(mainTarget) || cameraState == null || !cameraState.initialized) {
            return;
        }

        StormState storm = StormState.INSTANCE;
        storm.update(feature, cameraState.pos);

        boolean ambientActive = feature.isEnabled() && feature.usesAtmosphere();
        if (!ambientActive && !storm.hasVisibleStrike()) {
            return;
        }
        this.armed = true;

        if (cameraState.projectionMatrix == null || cameraState.viewRotationMatrix == null) {
            return;
        }
        Matrix4f viewProjection = new Matrix4f(cameraState.projectionMatrix).mul(cameraState.viewRotationMatrix);
        Matrix4f inverseViewProjection = new Matrix4f(viewProjection).invert();

        boolean depthZeroToOne = RenderSystem.getDevice().getDeviceInfo().isZZeroToOne();

        TextureTarget scene = this.sceneCopy.ensure(mainTarget.width, mainTarget.height);
        if (scene == null || scene.getColorTexture() == null) {
            return;
        }

        writeUniforms(
                feature,
                skyState,
                storm,
                cameraState,
                viewProjection,
                inverseViewProjection,
                depthZeroToOne,
                mainTarget.width,
                mainTarget.height
        );

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(
                mainTarget.getColorTexture(),
                scene.getColorTexture(),
                0, 0, 0, 0, 0,
                mainTarget.width,
                mainTarget.height
        );

        PostFx.pass("Desire Atmosphere Fog", PostPipelines.ATMOSPHERE_FOG, mainTarget, pass -> {
            pass.setUniform("AtmosphereUniforms", this.uniforms());
            pass.bindTexture("DepthSampler", mainTarget.getDepthTextureView(), PostFx.nearestSampler());
            pass.bindTexture("SceneSampler", scene.getColorTextureView(), PostFx.linearSampler());
        });
    }

    private void writeUniforms(AtmosphereModule feature,
                               SkyRenderState skyState,
                               StormState storm,
                               CameraRenderState cameraState,
                               Matrix4f viewProjection,
                               Matrix4f inverseViewProjection,
                               boolean depthZeroToOne,
                               int width,
                               int height) {
        float starBrightness = skyState != null ? skyState.starBrightness : 0.0F;

        int fogColor = ColorMode.resolve(feature.fogColorMode, feature.fogColor);
        if (feature.syncWithSky.getValue() && skyState != null) {
            fogColor = lerp(fogColor, skyState.skyColor, feature.skySync.getValue());

            int sunsetColor = skyState.sunriseAndSunsetColor;
            float sunsetWeight = Math.min(
                    (alpha(sunsetColor) / 255.0F) * feature.sunsetBoost.getValue(),
                    1.0F
            );
            if (sunsetWeight > 0.001F) {
                fogColor = lerp(fogColor, sunsetColor, sunsetWeight);
            }
        }

        boolean masterOn = feature.isEnabled();

        boolean usesFog = masterOn && feature.usesFog();
        float density = usesFog
                ? feature.fogDensity.getValue() * DENSITY_SCALE
                : 0.0F;
        float ambientStrength = usesFog
                ? feature.ambientStrength.getValue() * (1.0F - 0.55F * starBrightness)
                : 0.0F;
        if (usesFog && feature.stormFog.getValue()) {
            float intensity = storm.intensity();
            density *= 1.0F + intensity * 1.35F;
            ambientStrength *= 1.0F - 0.4F * intensity;
        }
        float inverseHeight = 1.0F / Math.max(feature.fogHeight.getValue(), 1.0F);
        float aspect = height > 0 ? (float) width / height : 1.0F;

        float sunAngle = skyState != null ? skyState.sunAngle : 0.0F;
        float sunUp = (float) Math.cos(sunAngle);
        float sunEast = -(float) Math.sin(sunAngle);
        boolean daylight = sunUp >= 0.0F;
        float lightAmount = usesFog
                ? (Math.clamp(sunUp * 6.0F, 0.0F, 1.0F) + Math.clamp(-sunUp * 6.0F, 0.0F, 1.0F) * 0.3F)
                * (1.0F - 0.7F * storm.intensity())
                : 0.0F;

        float flash = storm.flashStrength() * feature.flashStrength.getValue();

        int boltColor = feature.boltColor.getValue().getRGB();
        float boltRed = red(boltColor) / 255.0F;
        float boltGreen = green(boltColor) / 255.0F;
        float boltBlue = blue(boltColor) / 255.0F;

        float brightness = 0.0F;
        float seed = 0.0F;
        float strikeDistance = 0.0F;
        StormState.Strike strike = storm.activeStrike();
        if (strike != null
                && project(viewProjection, cameraState.pos, strike.topX, strike.topY, strike.topZ, this.boltTop)
                && project(viewProjection, cameraState.pos, strike.bottomX, strike.bottomY, strike.bottomZ, this.boltGround)) {
            brightness = StormState.boltEnvelope(strike);
            seed = strike.seed;
            double dx = strike.bottomX - cameraState.pos.x();
            double dy = strike.bottomY - cameraState.pos.y();
            double dz = strike.bottomZ - cameraState.pos.z();
            strikeDistance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        }

        float radius = Math.max(feature.boltWidth.getValue(), 0.1F) / Math.max(height, 1);
        float glow = radius * 6.0F + 0.005F;

        boolean snow = feature.usesSnow();
        int rainColor = feature.rainColor.getValue().getRGB();
        float rainAmount = masterOn && feature.usesRain()
                ? Math.min(
                        storm.intensity()
                                * feature.rainDensity.getValue()
                                * storm.skyVisibility()
                                * 0.7F,
                        0.72F)
                : 0.0F;
        float rippleStrength = feature.ripples.getValue() && !snow && rainAmount > 0.0F
                ? feature.rippleStrength.getValue() * Math.min(1.0F, rainAmount * 2.5F)
                : 0.0F;

        float nightFactor = Math.clamp((-sunUp + 0.12F) * 3.0F, 0.0F, 1.0F);
        boolean usesHaze = masterOn && feature.usesNightHaze();
        float hazeNight = feature.hazeNightOnly.getValue() ? nightFactor : 1.0F;
        float hazeDensity = usesHaze
                ? feature.hazeAmount.getValue() * HAZE_DENSITY_SCALE * hazeNight
                : 0.0F;
        float hazeGlow = feature.hazeGlow.getValue();
        int hazeColor = ColorMode.resolve(feature.hazeColorMode, feature.hazeColor);
        float hazeDeck = feature.hazeFollowGround.getValue()
                ? storm.groundHeight() + feature.hazeOffset.getValue()
                : feature.hazeLevel.getValue();
        float hazeCameraOffset = (float) (cameraState.pos.y() - hazeDeck);
        float hazeInverseThickness = 1.0F / Math.max(feature.hazeThickness.getValue(), 0.5F);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer data = Std140Builder.onStack(stack, UNIFORM_SIZE)
                    .putMat4f(inverseViewProjection)
                    .putVec4(
                            red(fogColor) / 255.0F,
                            green(fogColor) / 255.0F,
                            blue(fogColor) / 255.0F,
                            density
                    )
                    .putVec4(
                            inverseHeight,
                            ambientStrength,
                            depthZeroToOne ? 1.0F : 0.0F,
                            aspect
                    )
                    .putVec4(
                            usesFog ? feature.turbulence.getValue() : 0.0F,
                            feature.sunScatter.getValue(),
                            storm.fogDrift(),
                            storm.intensity()
                    )
                    .putVec4(
                            daylight ? sunEast : -sunEast,
                            daylight ? sunUp : -sunUp,
                            0.0F,
                            lightAmount
                    )
                    .putVec4(
                            (float) wrap(cameraState.pos.x()),
                            (float) cameraState.pos.y(),
                            (float) wrap(cameraState.pos.z()),
                            strikeDistance
                    )
                    .putVec4(storm.windX(), storm.windZ(), storm.windStrength(), 0.0F)
                    .putVec4(
                            boltRed + (1.0F - boltRed) * 0.3F,
                            boltGreen + (1.0F - boltGreen) * 0.3F,
                            boltBlue + (1.0F - boltBlue) * 0.3F,
                            flash
                    )
                    .putVec4(this.boltTop.x, this.boltTop.y, this.boltGround.x, this.boltGround.y)
                    .putVec4(boltRed, boltGreen, boltBlue, glow)
                    .putVec4(
                            brightness,
                            radius,
                            seed,
                            feature.boltBranches.getValue()
                    )
                    .putVec4(
                            red(rainColor) / 255.0F,
                            green(rainColor) / 255.0F,
                            blue(rainColor) / 255.0F,
                            feature.rainOpacity.getValue()
                    )
                    .putVec4(
                            rainAmount,
                            storm.rainPhase(),
                            snow ? 1.0F : 0.0F,
                            snow ? 1.5F : 0.25F
                    )
                    .putVec4(
                            rippleStrength,
                            storm.ripplePhase(),
                            Math.min(rainAmount * 1.4F, 0.85F),
                            0.0F
                    )
                    .putVec4(
                            red(hazeColor) / 255.0F * hazeGlow,
                            green(hazeColor) / 255.0F * hazeGlow,
                            blue(hazeColor) / 255.0F * hazeGlow,
                            hazeDensity
                    )
                    .putVec4(
                            hazeInverseThickness,
                            hazeCameraOffset,
                            feature.hazeBanks.getValue(),
                            feature.hazeMoonScatter.getValue()
                    )
                    .get();
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.uniforms().slice(), data);
        }
    }

    private static double wrap(double value) {
        return value - Math.floor(value / ORIGIN_WRAP) * ORIGIN_WRAP;
    }

    private static final float NEAR_PLANE_GUARD = 0.5F;

    private static boolean project(Matrix4f viewProjection,
                                   Vec3 cameraPos,
                                   double x,
                                   double y,
                                   double z,
                                   Vector4f out) {
        out.set(
                (float) (x - cameraPos.x()),
                (float) (y - cameraPos.y()),
                (float) (z - cameraPos.z()),
                1.0F
        );
        viewProjection.transform(out);
        if (out.w <= NEAR_PLANE_GUARD) {
            return false;
        }
        out.x = (out.x / out.w) * 0.5F + 0.5F;
        out.y = (out.y / out.w) * 0.5F + 0.5F;
        return true;
    }

    private static boolean valid(RenderTarget target) {
        return target != null
                && target.width > 0
                && target.height > 0
                && target.getColorTexture() != null
                && target.getColorTextureView() != null
                && target.getDepthTexture() != null
                && target.getDepthTextureView() != null;
    }

    public void release() {
        this.sceneCopy.release();
        if (this.uniforms != null) {
            if (!this.uniforms.isClosed()) {
                this.uniforms.close();
            }
            this.uniforms = null;
        }
        StormState.INSTANCE.reset();
        this.armed = false;
    }

    private static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    private static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    private static int blue(int color) {
        return color & 0xFF;
    }

    private static int alpha(int color) {
        return (color >>> 24) & 0xFF;
    }

    private static int clamp255(int value) {
        return Math.clamp(value, 0, 255);
    }

    private static int pack(int r, int g, int b, int a) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int lerp(int from, int to, float delta) {
        float t = Math.clamp(delta, 0.0F, 1.0F);
        return pack(
                Math.round(red(from) + (red(to) - red(from)) * t),
                Math.round(green(from) + (green(to) - green(from)) * t),
                Math.round(blue(from) + (blue(to) - blue(from)) * t),
                Math.round(alpha(from) + (alpha(to) - alpha(from)) * t)
        );
    }
}
