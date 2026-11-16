package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.RelicStyle;
import tokyo.ghoul.client.ui.widget.Widget;
import tokyo.ghoul.client.ui.widget.HudIcons;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class WatermarkWidget extends Widget {

    private static final Color ACCENT = RelicStyle.ACCENT;
    private static final Color SUB = RelicStyle.ACCENT_DIM;
    private static final Color TEXT = new Color(208, 208, 208);
    private static final Color BORDER = new Color(35, 36, 40);
    private static final Color FILL = new Color(25, 27, 31);
    private static final Color LOGO_BORDER = new Color(37, 39, 43);
    private static final Color LOGO_FILL = new Color(26, 28, 32);
    private static final Color SEPARATOR = new Color(34, 35, 38);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] MENU_OPTIONS = {"По центру", "Слева", "Фон", "Жидкое стекло"};

    private final AnimationUtil menuAnim = new AnimationUtil();
    private final EventListener menuClickHook;

    private boolean menuOpen;
    private Screen menuScreen;
    private float menuX, menuY, menuW, menuRowH;
    private float bossShift;

    public WatermarkWidget() {
        super(3f, 3f);
        menuClickHook = KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.mouse() || !isEnabled() || mc.player == null) return;
            var screen = mc.gui.screen();
            if (screen != null && !(screen instanceof ChatScreen)) return;
            if (event.action() == 1 && (event.key() == 0 || event.key() == 1)) handleMenuPress(event.key());
        }));
    }

    @Override
    public String getName() { return "Watermark"; }

    @Override
    public void render(PoseStack ms) {
        if (mc.player == null) {
            if (menuOpen) closeMenu();
            return;
        }

        float x = getDraggable().getX(), y = getDraggable().getY();

        float h = u(126f);
        float corner = u(28f);
        float stroke = u(4f);
        float rowGap = u(38f);
        float logoGap = u(22f);

        float textSize = u(42f);
        float subSize = u(31f);
        float sepW = u(10f), sepH = u(24f), sepGap = u(37f);
        float subGap = u(7f);

        var font = Fonts.HUD_MEDIUM;

        String username = mc.player.getScoreboardName();
        if (username == null || username.isEmpty()) username = "User";
        int fps = mc.getFps();
        String fpsVal = String.valueOf(fps > 0 ? fps : 114);
        String[] time = LocalTime.now().format(TIME_FORMAT).split(":");

        float userW = u(28f), userH = u(35f);
        float gaugeW = u(42f), gaugeH = u(35f);
        float clockS = u(41f);
        float nameW = RelicStyle.width(font, username, textSize);
        float fpsValW = RelicStyle.width(font, fpsVal, textSize);
        float fpsSubW = RelicStyle.width(font, "FPS", subSize);
        float colonW = RelicStyle.width(font, ":", textSize);
        float timeW = RelicStyle.width(font, time[0], textSize) + colonW + RelicStyle.width(font, time[1], textSize);

        float w1 = u(50f) + userW + u(27f) + nameW
                + sepGap + sepW + sepGap
                + gaugeW + u(21f) + fpsValW + subGap + fpsSubW
                + sepGap + sepW + sepGap
                + clockS + u(21f) + timeW + u(43f);

        ServerData server = mc.getCurrentServer();
        String host = server != null && server.ip != null && !server.ip.isBlank() ? server.ip : "singleplayer";
        String[] dom = domainParts(host);
        int ping = ping();
        String pingVal = String.valueOf(ping > 0 ? ping : 49);

        float homeW = u(38f), homeH = u(42f);
        float globeS = u(40f);
        float domW = RelicStyle.width(font, dom[0], textSize) + RelicStyle.width(font, dom[1], textSize) + RelicStyle.width(font, dom[2], textSize);
        float pingValW = RelicStyle.width(font, pingVal, textSize);
        float pingSubW = RelicStyle.width(font, "MS", subSize);

        float w2 = u(44f) + homeW + u(22f) + domW
                + u(42f) + sepW + u(38f)
                + globeS + u(21f) + pingValW + subGap + pingSubW + u(42f);

        float rowW = h + logoGap + w1;

        if (getDraggable().isDragging()) {
            bossShift = 0f;
        } else {
            bossShift = MathUtil.interpolate(bossShift, bossBarShift(x, rowW, y), 0.15f);
            if (bossShift < 0.05f) bossShift = 0f;
        }
        getDraggable().setOffsetY(bossShift);
        y += bossShift;
        float x2 = x + (rowW - w2) / 2f;
        float y2 = y + h + rowGap;

        if (liquidGlass()) {
            glass(ms, x, y, h, h, corner);
            glass(ms, x + h + logoGap, y, w1, h, corner);
            glass(ms, x2, y2, w2, h, corner);
        } else {
            panel(ms, x, y, h, h, corner, stroke, LOGO_BORDER, LOGO_FILL);
            panel(ms, x + h + logoGap, y, w1, h, corner, stroke, BORDER, FILL);
            panel(ms, x2, y2, w2, h, corner, stroke, BORDER, FILL);
        }

        float textY = textSize * 0.09f - textSize / 2f;
        float subDy = (textSize - subSize) * 0.78f - u(2f);

        float cy = y + h / 2f;
        float logoS = u(72f);
        RelicStyle.glow(ms, x + h / 2f, cy, logoS + u(60f), logoS + u(46f), 1f);
        Fonts.LOGO.drawGlyphGradient(ms, 'S', x + (h - logoS) / 2f, cy - logoS / 2f, logoS,
                RelicStyle.a(ACCENT, 1f), RelicStyle.a(RelicStyle.accent2(), 1f));

        float cx = x + h + logoGap + u(50f);
        icon(ms, HudIcons.WM_USER, cx, cy, userW, userH);
        cx += userW + u(27f);
        font.drawText(ms, username, cx, cy + textY, textSize, TEXT);
        cx += nameW + sepGap;
        separator(ms, cx, cy, sepW, sepH);
        cx += sepW + sepGap;

        icon(ms, HudIcons.WM_GAUGE, cx, cy, gaugeW, gaugeH);
        cx += gaugeW + u(21f);
        font.drawText(ms, fpsVal, cx, cy + textY, textSize, TEXT);
        cx += fpsValW + subGap;
        RelicStyle.textAt(ms, font, "FPS", cx, cy + textY + subDy, subSize, SUB, 1f);
        cx += fpsSubW + sepGap;
        separator(ms, cx, cy, sepW, sepH);
        cx += sepW + sepGap;

        icon(ms, HudIcons.WM_CLOCK, cx, cy, clockS, clockS);
        cx += clockS + u(21f);
        font.drawText(ms, time[0], cx, cy + textY, textSize, TEXT);
        cx += RelicStyle.width(font, time[0], textSize);
        RelicStyle.textAt(ms, font, ":", cx, cy + textY, textSize, ACCENT, 1f);
        cx += colonW;
        font.drawText(ms, time[1], cx, cy + textY, textSize, TEXT);

        float cy2 = y2 + h / 2f;
        cx = x2 + u(44f);
        icon(ms, HudIcons.WM_HOME, cx, cy2, homeW, homeH);
        cx += homeW + u(22f);
        RelicStyle.textAt(ms, font, dom[0], cx, cy2 + textY, textSize, ACCENT, 1f);
        cx += RelicStyle.width(font, dom[0], textSize);
        font.drawText(ms, dom[1], cx, cy2 + textY, textSize, TEXT);
        cx += RelicStyle.width(font, dom[1], textSize);
        RelicStyle.textAt(ms, font, dom[2], cx, cy2 + textY, textSize, ACCENT, 1f);
        cx += RelicStyle.width(font, dom[2], textSize) + u(42f);
        separator(ms, cx, cy2, sepW, sepH);
        cx += sepW + u(38f);

        icon(ms, HudIcons.WM_GLOBE, cx, cy2, globeS, globeS);
        cx += globeS + u(21f);
        font.drawText(ms, pingVal, cx, cy2 + textY, textSize, TEXT);
        cx += pingValW + subGap;
        RelicStyle.textAt(ms, font, "MS", cx, cy2 + textY + subDy, subSize, SUB, 1f);

        getDraggable().setWidth(rowW);
        getDraggable().setHeight(h + rowGap + h);

        renderMenu(ms, h + rowGap + h, k(1f));
    }

    private float u(float px) {
        return RelicStyle.u(px);
    }

    private void panel(PoseStack ms, float x, float y, float w, float h, float corner, float stroke, Color border, Color fill) {
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, border);
        RenderUtil.RECT.draw(ms, x + stroke, y + stroke, w - stroke * 2f, h - stroke * 2f, corner - stroke, fill);
    }

    private void glass(PoseStack ms, float x, float y, float w, float h, float corner) {
        RenderUtil.BLUR_RECT.draw(ms, x, y, w, h, corner, Color.WHITE, 0f);
        RenderUtil.RECT.draw(ms, x, y, w, h, corner, new Color(14, 14, 16, 150));
    }

    private void icon(PoseStack ms, Identifier texture, float x, float cy, float w, float h) {
        RelicStyle.icon(ms, texture, x, cy, w, h, ACCENT, 1f);
    }

    private void separator(PoseStack ms, float x, float cy, float w, float h) {
        RenderUtil.RECT.draw(ms, x, cy - h / 2f, w, h, w / 2f, SEPARATOR);
    }

    private float bossBarShift(float x, float width, float y) {
        if (RemovalsModule.getInstance().isBossBar() || mc.gui == null || mc.gui.hud == null) return 0f;
        var overlay = mc.gui.hud.getBossOverlay();
        if (overlay == null || overlay.events.isEmpty()) return 0f;

        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();
        if (x + width < screenWidth / 2f - 91f || x > screenWidth / 2f + 91f) return 0f;

        int barY = 12;
        float bottom = 0f;
        for (int i = 0; i < overlay.events.size(); i++) {
            bottom = barY + 5f;
            barY += 19;
            if (barY >= screenHeight / 3f) break;
        }
        return Math.max(0f, bottom + 4f - y);
    }

    private static String[] domainParts(String host) {
        int colon = host.lastIndexOf(':');
        if (colon > 0 && host.indexOf(':') == colon) host = host.substring(0, colon);
        String[] parts = host.split("\\.");
        int n = parts.length;
        if (n >= 3) {
            return new String[]{parts[0] + ".", String.join(".", java.util.Arrays.copyOfRange(parts, 1, n - 1)), "." + parts[n - 1]};
        }
        if (n == 2) return new String[]{"", parts[0], "." + parts[1]};
        return new String[]{"", host, ""};
    }


    private void handleMenuPress(int button) {
        Draggable d = getDraggable();
        float mx = d.normaliseX(), my = d.normaliseY();

        if (d.isHovering()) {
            if (button == 1) {
                if (menuOpen) closeMenu();
                else {
                    menuScreen = mc.gui.screen();
                    menuOpen = true;
                    menuAnim.run(1f, 150, Easing.EXPO_OUT);
                }
            } else if (menuOpen) {
                closeMenu();
            }
            return;
        }
        if (!menuOpen) return;

        int option = menuOptionAt(mx, my);
        if (option < 0) {
            closeMenu();
            return;
        }

        float margin = 3f;
        if (option == 0) {
            position((mc.getWindow().getGuiScaledWidth() - d.getWidth()) / 2f, margin);
            closeMenu();
        } else if (option == 1) {
            position(margin, margin);
            closeMenu();
        } else if (option == 2) {
            setLiquidGlass(false);
            setShowBackground(true);
        } else if (option == 3) {
            setLiquidGlass(true);
            setShowBackground(false);
        }
    }

    private void closeMenu() {
        menuOpen = false;
        menuAnim.run(0f, 150, Easing.EXPO_OUT);
    }

    private void position(float x, float y) {
        getDraggable().setX(x);
        getDraggable().setY(y);
        DraggableManager.getInstance().save();
    }

    private int menuOptionAt(float mx, float my) {
        if (mx < menuX || mx > menuX + menuW || my < menuY || my > menuY + menuRowH * MENU_OPTIONS.length) return -1;
        return (int) ((my - menuY) / menuRowH);
    }

    private void renderMenu(PoseStack ms, float height, float stroke) {
        if (menuOpen && mc.gui.screen() != menuScreen) closeMenu();

        if (menuOpen && getDraggable().isDragging()) closeMenu();

        menuAnim.update();
        float anim = (float) menuAnim.getValue();

        float x = getDraggable().getX();
        float textS = k(7.5f), pad = k(5.5f), corner = k(2f);
        float optW = 0f;
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            float w = Fonts.SF_MEDIUM.getWidth(MENU_OPTIONS[i], textS);
            if (i >= 2) w += Fonts.SF_MEDIUM.getWidth("Выкл", textS) + k(18f);
            optW = Math.max(optW, w);
        }
        menuW = optW + pad * 2f;
        menuRowH = k(13f);
        menuX = x;
        menuY = getDraggable().getY() + getDraggable().getOffsetY() + height + k(3f);

        if (anim <= 0.01f) return;

        int a = (int) (255 * anim);
        float slide = (1f - anim) * k(4f);
        float top = menuY - slide;
        float menuH = menuRowH * MENU_OPTIONS.length;

        RelicStyle.menuPanel(ms, menuX, top, menuW, menuH, corner, anim);

        float mx = getDraggable().normaliseX(), my = getDraggable().normaliseY();
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            float rowY = top + i * menuRowH;
            boolean hovered = mx >= menuX && mx <= menuX + menuW && my >= rowY && my < rowY + menuRowH;

            if (hovered) {
                RelicStyle.menuHover(ms, menuX + k(1.5f), rowY + k(1.5f), menuW - k(3f), menuRowH - k(3f), k(1.5f), anim);
            }

            Fonts.SF_MEDIUM.drawText(ms, MENU_OPTIONS[i], menuX + pad, rowY + (menuRowH - textS) / 2f + k(1f),
                    textS, ColorUtil.setAlpha(RelicStyle.ROW_TEXT, hovered ? a : (int) (a * 0.8f)));

            if (i >= 2) {
                boolean on = i == 3 ? isLiquidGlass() : !isLiquidGlass();
                String state = on ? "Вкл" : "Выкл";
                float stateW = Fonts.SF_MEDIUM.getWidth(state, textS);
                Fonts.SF_MEDIUM.drawText(ms, state, menuX + menuW - pad - stateW, rowY + (menuRowH - textS) / 2f + k(1f),
                        textS, ColorUtil.setAlpha(on ? UIColors.primary() : Color.WHITE, on ? a : (int) (a * 0.55f)));
            }
        }
    }

    private int ping() {
        if (mc.getConnection() == null || mc.player == null) return 0;
        var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
        return info != null ? info.getLatency() : 0;
    }
}
