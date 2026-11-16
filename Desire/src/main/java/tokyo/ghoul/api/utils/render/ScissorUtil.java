package tokyo.ghoul.api.utils.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.UtilityClass;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;

@UtilityClass
public class ScissorUtil {
    public void start(PoseStack matrixStack, float x, float y, float width, float height) {
        matrixStack.pushPose();
        EvaRenderer.getInstance().pushScissor(x, y, width, height);
    }

    public void stop(PoseStack matrixStack) {
        EvaRenderer.getInstance().popScissor();
        matrixStack.popPose();
    }
}
