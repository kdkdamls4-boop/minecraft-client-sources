package tokyo.ghoul.client.ui.clickgui.screen;

import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;
import tokyo.ghoul.client.ui.clickgui.ClickGuiTab;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class DropdownPanel {
    private final ClickGuiTab tab;
    private final List<ModuleRow> rows;
    private final List<ModuleRow> visible = new ArrayList<>();
    private String filter = "";

    private float x, y;
    private float scroll, scrollTarget;

    public DropdownPanel(ClickGuiTab tab, List<ModuleRow> rows) {
        this.tab = tab;
        this.rows = rows;
        this.visible.addAll(rows);
    }

    public static float width() {
        return r(258f);
    }

    public static float height() {
        return r(604f);
    }

    public static float headerH() {
        return r(42f);
    }

    private static float pad() {
        return r(16f);
    }

    private static float gap() {
        return r(8f);
    }

    private float listTop() {
        return y + headerH();
    }

    private float listBottom() {
        return y + height() - r(10f);
    }

    public List<ModuleRow> rows() {
        return rows;
    }

    public void filter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.equals(filter)) return;
        filter = q;
        visible.clear();
        for (ModuleRow row : rows) {
            if (q.isEmpty() || row.name().toLowerCase(Locale.ROOT).contains(q)) visible.add(row);
        }
        scroll = scrollTarget = 0f;
    }

    public void resetScroll() {
        scroll = scrollTarget = 0f;
    }

    public void render(float x, float y, float alpha, double mx, double my, float dt, boolean interactive) {
        this.x = x;
        this.y = y;
        float w = width(), h = height();
        glass(x, y, w, h, r(10f), alpha);

        float cy = y + headerH() / 2f + r(1f);
        float iconSize = r(15f);
        ClickGuiIcons.drawTabIcon(x + r(18f), cy - iconSize / 2f, iconSize, tab, a(HEADER_TEXT, alpha));
        rect(x + r(43f), cy - r(7f), r(1.2f), r(14f), r(0.6f), a(DIVIDER, alpha));
        text(MEDIUM, tab.getLabel(), x + r(53f), cy, r(16f), a(HEADER_TEXT, alpha));
        icon(CHEVRON_DOWN, x + w - r(22f), cy, r(13f), a(ICON_DIM, alpha));

        float top = listTop(), bottom = listBottom(), areaH = bottom - top;
        float rowW = w - pad() * 2f;
        float content = 0f;
        for (ModuleRow row : rows) row.animate(dt);
        for (ModuleRow row : visible) content += row.height(rowW) + gap();
        content = Math.max(0f, content - gap());
        float maxScroll = Math.max(0f, content - areaH + r(4f));
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll = ease(scroll, scrollTarget, 16f, dt);

        boolean live = interactive && inside(mx, my, x, top, w, areaH);
        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x, top, w, areaH);
        float ry = top + r(2f) - scroll;
        for (ModuleRow row : visible) {
            row.render(x + pad(), ry, rowW, alpha, mx, my, dt, live);
            ry += row.height(rowW) + gap();
        }
        ScissorUtil.stop(matrices);

        if (visible.isEmpty()) {
            textCenter(MEDIUM, "Nothing found", x + w / 2f, top + r(40f), r(13.5f), a(DIM, alpha));
        }

        if (maxScroll > 0f) {
            float trackX = x + w - r(7f), trackY = top + r(4f), trackH = areaH - r(8f);
            float thumbH = Math.max(r(36f), trackH * areaH / (content + r(4f)));
            float thumbY = trackY + (trackH - thumbH) * (scroll / maxScroll);
            rect(trackX, thumbY, r(2.5f), thumbH, r(1.25f), a(new Color(255, 255, 255, 34), alpha));
        }
    }

    public boolean hovered(double mx, double my) {
        return inside(mx, my, x, y, width(), height());
    }

    private boolean inList(double mx, double my) {
        return inside(mx, my, x, listTop(), width(), listBottom() - listTop());
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!hovered(mx, my)) return false;
        if (inList(mx, my)) {
            for (ModuleRow row : visible) {
                if (row.mouseClicked(mx, my, button)) return true;
            }
        }
        return true;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!hovered(mx, my)) return false;
        scrollTarget -= (float) amount * r(46f);
        return true;
    }
}
