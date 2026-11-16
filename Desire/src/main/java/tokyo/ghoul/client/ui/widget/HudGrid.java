package tokyo.ghoul.client.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;

import java.awt.Color;

public final class HudGrid implements QuickImports {
    private static final AnimationUtil ALPHA = new AnimationUtil();

    private HudGrid() {}

    public static void render(PoseStack ms) {
        boolean dragging = false;
        for (Draggable draggable : DraggableManager.getInstance().getDraggables().values()) {
            if (draggable.isDragging()) {
                dragging = true;
                break;
            }
        }

        ALPHA.update();
        ALPHA.run(dragging ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = (float) ALPHA.getValue();
        if (alpha <= 0.01f) return;

        float width = mc.getWindow().getGuiScaledWidth();
        float height = mc.getWindow().getGuiScaledHeight();
        float line = 0.5f;
        float step = Draggable.GRID;

        Color minor = new Color(255, 255, 255, Math.round(12 * alpha));
        Color major = new Color(255, 255, 255, Math.round(26 * alpha));

        int index = 0;
        for (float x = 0f; x <= width; x += step, index++) {
            RenderUtil.RECT.draw(ms, x - line / 2f, 0f, line, height, 0f, index % 5 == 0 ? major : minor);
        }
        index = 0;
        for (float y = 0f; y <= height; y += step, index++) {
            RenderUtil.RECT.draw(ms, 0f, y - line / 2f, width, line, 0f, index % 5 == 0 ? major : minor);
        }

        Color axis = UIColors.primary(Math.round(110 * alpha));
        RenderUtil.RECT.draw(ms, width / 2f - line, 0f, line * 2f, height, 0f, axis);
        RenderUtil.RECT.draw(ms, 0f, height / 2f - line, width, line * 2f, 0f, axis);
    }
}
