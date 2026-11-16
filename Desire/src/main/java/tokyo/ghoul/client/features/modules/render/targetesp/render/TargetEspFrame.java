package tokyo.ghoul.client.features.modules.render.targetesp.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public record TargetEspFrame(DeltaTracker deltaTracker, PoseStack poseStack, Vec3 cameraPos, float partialTicks,
                             Matrix4f projection, Matrix4f viewMatrix) {
}
