package tokyo.ghoul.api.utils.math;

import com.mojang.math.Axis;
import lombok.experimental.UtilityClass;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import tokyo.ghoul.api.system.interfaces.QuickImports;

@UtilityClass
public class ProjectionUtil implements QuickImports {
    private static final Matrix4f LEVEL_PROJECTION = new Matrix4f();
    private static boolean levelProjectionCaptured;

    public static void captureLevelProjection(Matrix4fc projection) {
        if (projection == null) return;
        LEVEL_PROJECTION.set(projection);
        levelProjectionCaptured = true;
    }

    public static Matrix4f levelProjectionCopy() {
        return levelProjectionCaptured ? new Matrix4f(LEVEL_PROJECTION) : null;
    }

    public static Vector2f projectToScreen(Vec3 pos) {
        if (pos == null || mc.getWindow() == null || mc.gameRenderer == null || !levelProjectionCaptured) {
            return null;
        }
        CameraRenderState cameraState = mc.gameRenderer.gameRenderState().levelRenderState.cameraRenderState;
        if (cameraState == null || !cameraState.initialized || cameraState.viewRotationMatrix == null) {
            return null;
        }

        int guiWidth = mc.getWindow().getGuiScaledWidth();
        int guiHeight = mc.getWindow().getGuiScaledHeight();
        if (guiWidth <= 0 || guiHeight <= 0) return null;

        Vector4f clip = new Vector4f(
                (float) (pos.x - cameraState.pos.x()),
                (float) (pos.y - cameraState.pos.y()),
                (float) (pos.z - cameraState.pos.z()),
                1.0F
        );
        cameraState.viewRotationMatrix.transform(clip);
        LEVEL_PROJECTION.transform(clip);
        if (clip.w <= 1.0E-4F) return null;

        float ndcX = clip.x / clip.w;
        float ndcY = clip.y / clip.w;
        if (Math.abs(ndcX) > 2.0F || Math.abs(ndcY) > 2.0F) return null;

        return new Vector2f((ndcX + 1.0F) * 0.5F * guiWidth, (1.0F - ndcY) * 0.5F * guiHeight);
    }

    public Vector2f project(@NotNull Vec3 vec3d) {
        return project(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public Vector2f project(double x, double y, double z) {
        var camera = mc.getEntityRenderDispatcher().camera;
        var cameraPos = camera.position();

        var yawQuat = Axis.YP.rotationDegrees(-camera.yRot());
        var pitchQuat = Axis.XP.rotationDegrees(camera.xRot());

        var cameraRotation = yawQuat.mul(pitchQuat, new Quaternionf());
        cameraRotation = cameraRotation.conjugate(new Quaternionf());

        var result3f = new Vector3f(
                (float)(cameraPos.x() - x),
                (float)(cameraPos.y() - y),
                (float)(cameraPos.z() - z)
        );

        result3f.rotate(cameraRotation);

        if (mc.options.bobView().get()) {
            if (mc.getCameraEntity() instanceof AbstractClientPlayer p) {
                calculateViewBobbing((AbstractClientPlayer) mc.getCameraEntity(), result3f);
            }
        }

        float fov = mc.options.fov().get();

        return calculateScreenPosition(result3f, fov);
    }

    private void calculateViewBobbing(AbstractClientPlayer player, Vector3f result3f) {
        float tickDelta = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        var state = player.avatarState();
        float g = state.getBackwardsInterpolatedWalkDistance(tickDelta);
        float strideDistance = state.getInterpolatedBob(tickDelta);

        float translateX = Mth.sin(g * (float)Math.PI) * strideDistance * 0.5F;
        float translateY = -Math.abs(Mth.cos(g * (float)Math.PI) * strideDistance);
        float rotateZ = Mth.sin(g * (float)Math.PI) * strideDistance * 3.0F;
        float rotateX = Math.abs(Mth.cos(g * (float)Math.PI - 0.2F) * strideDistance) * 5.0F;

        float rotateZRad = rotateZ * ((float)Math.PI / 180f);
        float rotateXRad = rotateX * ((float)Math.PI / 180f);

        if (rotateZRad != 0) {
            Quaternionf zRot = new Quaternionf().setAngleAxis(rotateZRad, 0.0f, 0.0f, 1.0f).conjugate();
            result3f.rotate(zRot);
        }

        if (rotateXRad != 0) {
            Quaternionf xRot = new Quaternionf().setAngleAxis(rotateXRad, 1.0f, 0.0f, 0.0f).conjugate();
            result3f.rotate(xRot);
        }

        result3f.add(translateX, -translateY, 0.0f);
    }

    private Vector2f calculateScreenPosition(Vector3f result3f, double fov) {
        float width = mc.getWindow().getGuiScaledWidth() / 2.0f;
        float height = mc.getWindow().getGuiScaledHeight() / 2.0f;
        float x = result3f.x;
        float y = result3f.y;
        float z = result3f.z;

        double scaleFactor = height / (z * Math.tan(Math.toRadians(fov / 2.0)));
        return (z < 0.0f)
                ? new Vector2f((float) (-x * scaleFactor + width), (float) (height - y * scaleFactor))
                : new Vector2f(Float.MAX_VALUE, Float.MAX_VALUE);
    }
}
