package tokyo.ghoul.client.ui.clickgui.screen;

import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.configs.ConfigManager;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;

import java.awt.Color;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class ConfigsPage {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withZone(ZoneId.systemDefault());
    private static final int COLUMNS = 2;

    private final StringBuilder name = new StringBuilder();
    private boolean typing;
    private float focus;
    private float createHover;
    private List<String> configs = new ArrayList<>();
    private final Map<String, float[]> anims = new HashMap<>();
    private String flashed;
    private float flash;
    private float scroll, scrollTarget;

    private float x, y, w, h;
    private final List<String> shown = new ArrayList<>();

    public void refresh() {
        configs = new ArrayList<>(ConfigManager.getInstance().getConfigsNames());
        configs.removeIf(config -> config.equalsIgnoreCase("autoConfig"));
        configs.sort(String.CASE_INSENSITIVE_ORDER);
    }

    public int count() {
        return configs.size();
    }

    public boolean isTyping() {
        return typing;
    }

    public void releaseKeyboard() {
        typing = false;
    }

    private float barH() {
        return r(38f);
    }

    private float createW() {
        return r(86f);
    }

    private float cardH() {
        return r(62f);
    }

    private float gap() {
        return r(10f);
    }

    private float cardW() {
        return (w - gap() * (COLUMNS - 1)) / COLUMNS;
    }

    private float listY() {
        return y + barH() + r(38f);
    }

    private float cardX(int index) {
        return x + (index % COLUMNS) * (cardW() + gap());
    }

    private float cardY(int index) {
        return listY() + (index / COLUMNS) * (cardH() + gap()) - scroll;
    }

    private float loadW() {
        return r(48f);
    }

    private float loadH() {
        return r(24f);
    }

    private float trashX(int index) {
        return cardX(index) + cardW() - r(22f);
    }

    private float loadX(int index) {
        return trashX(index) - r(14f) - loadW();
    }

    private float loadY(int index) {
        return cardY(index) + cardH() / 2f - loadH() / 2f;
    }

    public void render(float x, float y, float w, float h, String query, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        focus = ease(focus, typing ? 1f : 0f, 16f, dt);
        flash = ease(flash, 0f, 4f, dt);

        float fw = w - createW() - r(8f), cy = y + barH() / 2f;
        rect(x, y, fw, barH(), r(8f), a(ROW, alpha));
        RenderUtil.RECT.drawOutline(RenderUtil.matrices(), x, y, fw, barH(), r(8f), OUTLINE * 0.4f,
                a(mix(new Color(255, 255, 255, 6), new Color(255, 255, 255, 46), focus), alpha));
        icon(PENCIL, x + r(18f), cy, r(11f), a(mix(ICON_DIM, Color.WHITE, focus), alpha));
        float size = r(12.5f);
        if (name.isEmpty() && !typing) {
            text(MEDIUM, "Config name..", x + r(34f), cy, size, a(ICON_DIM, alpha));
        } else {
            text(MEDIUM, name.toString(), x + r(34f), cy, size, a(ROW_TEXT_ON, alpha));
            if (typing && System.currentTimeMillis() % 1000 < 500) {
                float caretX = x + r(34f) + MEDIUM.getWidth(name.toString(), size) + r(1f);
                rect(caretX, cy - r(6.5f), r(0.9f), r(13f), 0f, a(Color.WHITE, alpha));
            }
        }
        float bx = x + w - createW();
        createHover = ease(createHover, inside(mx, my, bx, y, createW(), barH()) ? 1f : 0f, 16f, dt);
        rect(bx, y, createW(), barH(), r(8f), a(mix(BADGE, Color.WHITE, createHover), alpha));
        textCenter(SEMIBOLD, "Create", bx + createW() / 2f, cy, r(12.5f), a(BADGE_TEXT, alpha));

        shown.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (String config : configs) {
            if (q.isEmpty() || config.toLowerCase(Locale.ROOT).contains(q)) shown.add(config);
        }

        float hy = y + barH() + r(20f);
        text(MEDIUM, "Saved configs", x + r(2f), hy, r(12.5f), a(HEADER_TEXT, alpha));
        textRight(MEDIUM, String.valueOf(shown.size()), x + w - r(2f), hy, r(12.5f), a(DIM, alpha));
        rect(x, y + barH() + r(32f), w, r(1f), 0f, a(new Color(255, 255, 255, 9), alpha));

        float listH = y + h - listY();
        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        float content = rows * (cardH() + gap()) - gap();
        float maxScroll = Math.max(0f, content - listH);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll = ease(scroll, scrollTarget, 16f, dt);

        if (shown.isEmpty()) {
            float ey = listY() + listH / 2f - r(16f);
            textCenter(SEMIBOLD, configs.isEmpty() ? "No configs yet" : "Nothing found", x + w / 2f, ey, r(14f),
                    a(ROW_TEXT, alpha));
            textCenter(MEDIUM, configs.isEmpty() ? "Type a name above and press Create" : "Try another search",
                    x + w / 2f, ey + r(22f), r(11.5f), a(DIM, alpha));
            return;
        }

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x - r(4f), listY(), w + r(8f), listH);
        boolean live = inside(mx, my, x, listY(), w, listH);
        for (int i = 0; i < shown.size(); i++) {
            float cx = cardX(i), cyy = cardY(i);
            if (cyy + cardH() < listY() || cyy > listY() + listH) continue;
            drawCard(i, shown.get(i), cx, cyy, alpha, live, mx, my, dt);
        }
        ScissorUtil.stop(matrices);

        if (maxScroll > 0f) {
            float trackX = x + w + r(5f), thumbH = Math.max(r(36f), listH * listH / content);
            float thumbY = listY() + (listH - thumbH) * (scroll / maxScroll);
            rect(trackX, thumbY, r(2.5f), thumbH, r(1.25f), a(new Color(255, 255, 255, 34), alpha));
        }
    }

    private void drawCard(int index, String config, float cx, float cy, float alpha, boolean live,
                          double mx, double my, float dt) {
        float[] anim = anims.computeIfAbsent(config, k -> new float[3]);
        boolean cardHover = live && inside(mx, my, cx, cy, cardW(), cardH());
        anim[0] = ease(anim[0], cardHover ? 1f : 0f, 16f, dt);
        anim[1] = ease(anim[1], cardHover && inside(mx, my, loadX(index), loadY(index), loadW(), loadH()) ? 1f : 0f, 18f, dt);
        anim[2] = ease(anim[2], cardHover && inside(mx, my, trashX(index) - r(10f), cy, r(20f), cardH()) ? 1f : 0f, 18f, dt);
        float f = config.equals(flashed) ? flash : 0f;

        rect(cx, cy, cardW(), cardH(), r(9f), a(mix(mix(ROW, ROW_ON, anim[0]), ROW_HOVER, f), alpha));

        float rcy = cy + cardH() / 2f;
        float textMaxW = loadX(index) - (cx + r(16f)) - r(10f);
        text(MEDIUM, ellipsize(MEDIUM, config, r(14f), textMaxW), cx + r(16f), rcy - r(8f), r(14f),
                a(mix(LABEL, ROW_TEXT_ON, anim[0]), alpha));
        String modified = TIME.format(Instant.ofEpochMilli(ConfigManager.getInstance().getLastModified(config)));
        text(MEDIUM, modified, cx + r(16f), rcy + r(9f), r(11f), a(DIM, alpha));

        float lx = loadX(index), ly = loadY(index);
        rect(lx, ly, loadW(), loadH(), r(6f), a(mix(mix(new Color(52, 52, 57), BADGE, anim[1]), BADGE, f), alpha));
        textCenter(SEMIBOLD, "Load", lx + loadW() / 2f, ly + loadH() / 2f, r(11.5f),
                a(mix(mix(LABEL, BADGE_TEXT, anim[1]), BADGE_TEXT, f), alpha));
        icon(TRASH, trashX(index), rcy, r(12f), a(mix(ICON_DIM, new Color(255, 92, 102), anim[2]), alpha));
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        float fw = w - createW() - r(8f);
        if (inside(mx, my, x, y, fw, barH())) {
            typing = true;
            return true;
        }
        if (inside(mx, my, x + w - createW(), y, createW(), barH())) {
            create();
            return true;
        }
        if (my < listY() || my > y + h) return false;
        for (int i = 0; i < shown.size(); i++) {
            if (!inside(mx, my, cardX(i), cardY(i), cardW(), cardH())) continue;
            String config = shown.get(i);
            if (inside(mx, my, loadX(i), loadY(i), loadW(), loadH())) {
                ConfigManager.getInstance().load(config);
                flashed = config;
                flash = 1f;
            } else if (inside(mx, my, trashX(i) - r(10f), cardY(i), r(20f), cardH())) {
                ConfigManager.getInstance().remove(config);
                anims.remove(config);
                refresh();
            }
            return true;
        }
        return false;
    }

    private void create() {
        if (name.isEmpty()) {
            typing = true;
            return;
        }
        String config = name.toString().trim();
        ConfigManager.getInstance().save(config);
        name.setLength(0);
        typing = false;
        refresh();
        flashed = config;
        flash = 1f;
    }

    public boolean mouseScrolled(double amount) {
        scrollTarget -= (float) amount * r(46f);
        return true;
    }

    public boolean keyPressed(int key) {
        if (!typing) return false;
        if (key == GLFW.GLFW_KEY_BACKSPACE && !name.isEmpty()) name.deleteCharAt(name.length() - 1);
        else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) create();
        else if (key == GLFW.GLFW_KEY_ESCAPE) typing = false;
        return true;
    }

    public boolean charTyped(char chr) {
        if (!typing) return false;
        if (name.length() < 24 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-' || chr == ' ')) {
            name.append(chr);
        }
        return true;
    }
}
