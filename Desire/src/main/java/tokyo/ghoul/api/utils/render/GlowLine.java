package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.experimental.UtilityClass;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@UtilityClass
public class GlowLine {
    public void sprite(VertexConsumer buffer, Matrix4f pose, double x, double y, double z,
                       Quaternionf orientation, float size, int argb) {
        float half = size / 2.0F;
        Vector3f right = orientation.transform(new Vector3f(half, 0.0F, 0.0F));
        Vector3f up = orientation.transform(new Vector3f(0.0F, half, 0.0F));
        float px = (float) x;
        float py = (float) y;
        float pz = (float) z;
        buffer.addVertex(pose, px - right.x - up.x, py - right.y - up.y, pz - right.z - up.z).setUv(0.0F, 1.0F).setColor(argb);
        buffer.addVertex(pose, px + right.x - up.x, py + right.y - up.y, pz + right.z - up.z).setUv(1.0F, 1.0F).setColor(argb);
        buffer.addVertex(pose, px + right.x + up.x, py + right.y + up.y, pz + right.z + up.z).setUv(1.0F, 0.0F).setColor(argb);
        buffer.addVertex(pose, px - right.x + up.x, py - right.y + up.y, pz - right.z + up.z).setUv(0.0F, 0.0F).setColor(argb);
    }

    public int segment(VertexConsumer buffer, Matrix4f pose, Vec3 from, Vec3 to,
                       Quaternionf orientation, float size, int argb, double spacing) {
        double length = from.distanceTo(to);
        int steps = Math.max(1, (int) Math.ceil(length / spacing));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            sprite(buffer, pose, from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t, from.z + (to.z - from.z) * t,
                    orientation, size, argb);
        }
        return steps + 1;
    }
}
