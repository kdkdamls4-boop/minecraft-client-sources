package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.Widget;

import java.util.Locale;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class CoordsWidget extends Widget {
    public CoordsWidget() {
        super(3f, 200f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return "Coordinates"; }

    @Override
    public void render(PoseStack ms) {
        if (mc.player == null) return;

        float x = getDraggable().getX(), y = getDraggable().getY();

        float h = u(126f), corner = u(28f), stroke = u(4f);
        float textSize = u(42f), subSize = u(31f);
        float subDy = (textSize - subSize) * 0.78f - u(2f);
        float pinW = u(36f), pinH = u(38f), boardW = u(40f), boardH = u(42f);
        float sepW = u(10f), sepH = u(24f);
        float subGap = u(3f), partGap = u(14f);

        var font = Fonts.HUD_MEDIUM;

        String[] values = {
                String.valueOf((int) Math.floor(mc.player.getX())),
                String.valueOf((int) Math.floor(mc.player.getY())),
                String.valueOf((int) Math.floor(mc.player.getZ()))
        };
        String[] axes = {"X", "Y", "Z"};

        double bps = Math.hypot(mc.player.getX() - mc.player.xo, mc.player.getZ() - mc.player.zo) * 20.0;
        String speed = bps >= 10.0 ? String.valueOf((int) Math.round(bps)) : String.format(Locale.ROOT, "%.1f", bps);

        float coordsW = 0f;
        for (int i = 0; i < 3; i++) {
            coordsW += width(font, values[i], textSize) + subGap + width(font, axes[i], subSize);
        }
        coordsW += partGap * 2f;
        float speedW = width(font, speed, textSize) + subGap + width(font, "BPS", subSize);

        float w = u(45f) + pinW + u(25f) + coordsW
                + u(37f) + sepW + u(37f)
                + boardW + u(22f) + speedW + u(41f);

        panel(ms, x, y, w, h, corner, stroke, BORDER, FILL, 1f, liquidGlass());

        float cy = y + h / 2f;
        float mainTop = cy - textSize / 2f + textSize * 0.09f;
        float cx = x + u(45f);
        icon(ms, HudIcons.WM_PIN, cx, cy, pinW, pinH, ACCENT, 1f);
        cx += pinW + u(25f);

        for (int i = 0; i < 3; i++) {
            text(ms, font, values[i], cx, cy, textSize, TEXT, 1f);
            cx += width(font, values[i], textSize) + subGap;
            textAt(ms, font, axes[i], cx, mainTop + subDy, subSize, ACCENT_DIM, 1f);
            cx += width(font, axes[i], subSize) + (i < 2 ? partGap : 0f);
        }

        cx += u(37f);
        separator(ms, cx, cy, sepW, sepH, 1f);
        cx += sepW + u(37f);

        icon(ms, HudIcons.WM_BOARD, cx, cy, boardW, boardH, ACCENT, 1f);
        cx += boardW + u(22f);
        text(ms, font, speed, cx, cy, textSize, TEXT, 1f);
        cx += width(font, speed, textSize) + subGap;
        textAt(ms, font, "BPS", cx, mainTop + subDy, subSize, ACCENT_DIM, 1f);

        getDraggable().setWidth(w);
        getDraggable().setHeight(h);
        renderHudSettingsMenu(ms);
    }
}
