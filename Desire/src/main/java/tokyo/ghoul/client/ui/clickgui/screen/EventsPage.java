package tokyo.ghoul.client.ui.clickgui.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.joml.Vector3i;
import tokyo.ghoul.api.system.client.GpsManager;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.client.services.AnarchyReconnectService;
import tokyo.ghoul.client.services.AnarchyReconnectService.Target;
import tokyo.ghoul.client.services.ServerEventsService;
import tokyo.ghoul.client.services.ServerEventsService.Card;
import tokyo.ghoul.client.services.ServerEventsService.Source;
import tokyo.ghoul.client.services.ServerEventsService.Status;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static tokyo.ghoul.client.ui.clickgui.screen.GuiStyle.*;

public final class EventsPage {
    private static final int COLUMNS = 3;
    private static final Source[] SOURCES = {Source.FT, Source.HW};
    private static final Identifier PIN = glyph("pin");
    private static final Identifier CLOCK = glyph("clock");
    private static final Identifier SERVER = glyph("server");
    private static final Identifier SPARKLE = glyph("sparkle");

    private final ServerEventsService service = ServerEventsService.getInstance();

    private float x, y, w, h;
    private float scroll, scrollTarget;
    private float sourceAnim;
    private final float[] segmentHover = new float[SOURCES.length];
    private final Map<String, float[]> cardAnims = new HashMap<>();
    private final List<Card> shown = new ArrayList<>();
    private long copiedAt;
    private String statusText = "";

    public void resetScroll() {
        scroll = scrollTarget = 0f;
    }


    private static float headerH() {
        return r(28f);
    }

    private float gridY() {
        return y + headerH() + r(14f);
    }

    private float gridH() {
        return y + h - gridY();
    }

    private static float gap() {
        return r(10f);
    }

    private float cardW() {
        return (w - gap() * (COLUMNS - 1)) / COLUMNS;
    }

    private static float cardH() {
        return r(100f);
    }

    private static float segmentW() {
        return r(48f);
    }

    private static float switchW() {
        return segmentW() * SOURCES.length + r(6f);
    }


    public void render(float x, float y, float w, float h, String query, float alpha, double mx, double my, float dt) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        service.tick();

        shown.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Card card : service.getCards()) {
            if (q.isEmpty() || card.title().toLowerCase(Locale.ROOT).contains(q)
                    || card.server().toLowerCase(Locale.ROOT).contains(q)) shown.add(card);
        }

        drawHeader(alpha, mx, my, dt);
        if (shown.isEmpty()) {
            drawEmpty(q.isEmpty(), alpha);
            return;
        }
        drawGrid(alpha, mx, my, dt);
    }

    private void drawHeader(float alpha, double mx, double my, float dt) {
        float cy = y + headerH() / 2f;
        int active = service.getSource().ordinal();
        sourceAnim = ease(sourceAnim, active, 16f, dt);

        float sw = switchW(), sh = headerH(), seg = segmentW();
        rect(x, y, sw, sh, r(8f), a(ROW, alpha));
        RenderUtil.RECT.drawOutline(RenderUtil.matrices(), x, y, sw, sh, r(8f), OUTLINE * 0.4f,
                a(new Color(255, 255, 255, 10), alpha));
        rect(x + r(3f) + seg * sourceAnim, y + r(3f), seg, sh - r(6f), r(6f), a(BADGE, alpha));
        for (int i = 0; i < SOURCES.length; i++) {
            float segX = x + r(3f) + seg * i;
            segmentHover[i] = ease(segmentHover[i], inside(mx, my, segX, y, seg, sh) ? 1f : 0f, 18f, dt);
            float on = Math.max(0f, 1f - Math.abs(sourceAnim - i));
            Color idle = mix(SETTING_TEXT, LABEL, segmentHover[i]);
            textCenter(SEMIBOLD, SOURCES[i].name(), segX + seg / 2f, cy, r(12.5f), a(mix(idle, BADGE_TEXT, on), alpha));
        }

        float tx = x + sw + r(14f);
        if (!shown.isEmpty()) {
            Set<String> servers = new HashSet<>();
            for (Card card : shown) servers.add(card.server());
            String count = shown.size() + " " + plural(shown.size(), "ивент", "ивента", "ивентов");
            text(SEMIBOLD, count, tx, cy, r(14f), a(TEXT, alpha));
            float countW = SEMIBOLD.getWidth(count, r(14f));
            int n = servers.size();
            text(MEDIUM, "на " + n + " " + plural(n, "сервере", "серверах", "серверах"), tx + countW + r(6f), cy,
                    r(12f), a(DIM, alpha));
        } else {
            text(SEMIBOLD, service.getSource() == Source.FT ? "FunTime" : "HolyWorld", tx, cy, r(14f), a(TEXT, alpha));
        }

        long now = System.currentTimeMillis();
        String status;
        Color dot;
        float pulse = pulse(1400L);
        if (now - copiedAt < 1600L) {
            status = statusText;
            dot = new Color(96, 165, 250);
        } else if (service.getStatus() == Status.LOADING) {
            status = "Загрузка";
            dot = new Color(250, 204, 21);
        } else if (service.getStatus() == Status.ERROR) {
            status = "Нет ответа от сервера";
            dot = new Color(248, 113, 113);
        } else {
            long ago = (now - service.getUpdatedAt()) / 1000L;
            status = ago < 2 ? "Обновлено только что" : "Обновлено " + ago + " с назад";
            dot = new Color(74, 222, 128);
        }
        float size = r(11.5f);
        float right = x + w;
        textRight(MEDIUM, status, right, cy, size, a(SETTING_TEXT, alpha));
        float dx = right - MEDIUM.getWidth(status, size) - r(10f), d = r(6f);
        rect(dx - d / 2f - r(3f) * pulse, cy - d / 2f - r(3f) * pulse, d + r(6f) * pulse, d + r(6f) * pulse, d,
                a(dot, alpha * 0.45f * (1f - pulse)));
        rect(dx - d / 2f, cy - d / 2f, d, d, d / 2f, a(dot, alpha));
    }

    private void drawGrid(float alpha, double mx, double my, float dt) {
        float gy = gridY(), gh = gridH(), cw = cardW(), ch = cardH();
        int rows = (shown.size() + COLUMNS - 1) / COLUMNS;
        float content = rows * (ch + gap()) - gap();
        float maxScroll = Math.max(0f, content - gh);
        scrollTarget = Math.max(0f, Math.min(maxScroll, scrollTarget));
        scroll = ease(scroll, scrollTarget, 16f, dt);

        var matrices = RenderUtil.matrices();
        ScissorUtil.start(matrices, x - r(2f), gy, w + r(4f), gh);
        boolean live = inside(mx, my, x, gy, w, gh);
        Set<String> alive = new HashSet<>();
        for (int i = 0; i < shown.size(); i++) {
            Card card = shown.get(i);
            alive.add(card.key());
            float cx = x + (i % COLUMNS) * (cw + gap());
            float cy = gy + (i / COLUMNS) * (ch + gap()) - scroll;
            float[] anim = cardAnims.computeIfAbsent(card.key(), k -> new float[2]);
            anim[0] = ease(anim[0], live && inside(mx, my, cx, cy, cw, ch) ? 1f : 0f, 16f, dt);
            anim[1] = ease(anim[1], 0f, 5f, dt);
            if (cy + ch < gy || cy > gy + gh) continue;
            drawCard(card, cx, cy, cw, ch, anim[0], anim[1], alpha);
        }
        cardAnims.keySet().retainAll(alive);
        ScissorUtil.stop(matrices);

        if (maxScroll > 0f) {
            float thumbH = Math.max(r(36f), gh * gh / content);
            float thumbY = gy + (gh - thumbH) * (scroll / maxScroll);
            rect(x + w + r(7f), thumbY, r(2.5f), thumbH, r(1.25f), a(new Color(255, 255, 255, 34), alpha));
        }
    }

    private void drawCard(Card card, float cx, float cy, float cw, float ch, float hover, float flash, float alpha) {
        var matrices = RenderUtil.matrices();
        float radius = r(10f);
        Color black = new Color(0, 0, 0);

        if (card.banner() != null) {
            float aspect = cw / ch, zoom = 1f - 0.05f * hover;
            float uw = aspect < 3f ? aspect / 3f : 1f, vh = aspect < 3f ? 1f : 3f / aspect;
            uw *= zoom;
            vh *= zoom;
            RenderUtil.TEXTURE_RECT.drawSmooth(matrices, cx, cy, cw, ch, radius, a(Color.WHITE, alpha),
                    (1f - uw) / 2f, (1f - vh) / 2f, uw, vh, card.banner());
        } else {
            Color from = mix(card.from(), black, 0.3f), to = mix(card.to(), black, 0.55f), mid = mix(from, to, 0.5f);
            RenderUtil.GRADIENT_RECT.draw(matrices, cx, cy, cw, ch, radius,
                    a(from, alpha), a(mid, alpha), a(mid, alpha), a(to, alpha));
            RenderUtil.GRADIENT_RECT.draw(matrices, cx, cy, cw, ch, radius,
                    a(withAlpha(card.from(), 120), alpha), a(withAlpha(card.from(), 0), alpha),
                    a(withAlpha(card.from(), 0), alpha), a(withAlpha(card.from(), 0), alpha));
            float mark = ch * 0.78f + r(4f) * hover;
            icon(card.glyph(), cx + cw - ch * 0.46f, cy + ch * 0.5f, mark, a(new Color(255, 255, 255, 34), alpha));
        }

        RenderUtil.GRADIENT_RECT.draw(matrices, cx, cy, cw, ch, radius,
                a(new Color(0, 0, 0, 40), alpha), a(new Color(0, 0, 0, 10), alpha),
                a(new Color(0, 0, 0, 215), alpha), a(new Color(0, 0, 0, 165), alpha));
        if (hover > 0.01f) rect(cx, cy, cw, ch, radius, a(new Color(255, 255, 255, 12), alpha * hover));
        if (flash > 0.01f) rect(cx, cy, cw, ch, radius, a(new Color(255, 255, 255, 60), alpha * flash));
        RenderUtil.RECT.drawOutline(matrices, cx, cy, cw, ch, radius, OUTLINE * 1.2f,
                a(mix(new Color(255, 255, 255, 14), withAlpha(card.from(), 190), hover), alpha));

        float pillH = r(18f), py = cy + r(9f), pcy = py + pillH / 2f, small = r(10.5f), pillR = r(3f);
        float px = cx + r(9f);
        float phaseW = MEDIUM.getWidth(card.phase(), small) + r(23f);
        rect(px, py, phaseW, pillH, pillR, a(new Color(10, 10, 12, 175), alpha));
        float d = r(6f), dotX = px + r(10f);
        if (card.live()) {
            float p = pulse(1400L);
            rect(dotX - d / 2f - r(3f) * p, pcy - d / 2f - r(3f) * p, d + r(6f) * p, d + r(6f) * p, d,
                    a(card.phaseColor(), alpha * 0.5f * (1f - p)));
        }
        rect(dotX - d / 2f, pcy - d / 2f, d, d, d / 2f, a(card.phaseColor(), alpha));
        text(MEDIUM, card.phase(), px + r(17f), pcy, small, a(new Color(236, 236, 240), alpha));
        px += phaseW + r(5f);
        if (card.count() > 1) {
            String count = "x" + card.count();
            float countW = SEMIBOLD.getWidth(count, small) + r(14f);
            rect(px, py, countW, pillH, pillR, a(new Color(10, 10, 12, 175), alpha));
            textCenter(SEMIBOLD, count, px + countW / 2f, pcy, small, a(new Color(236, 236, 240), alpha));
        }
        if (card.tag() != null) {
            float tagW = SEMIBOLD.getWidth(card.tag(), small) + r(16f), tagX = cx + cw - r(9f) - tagW;
            Color tagColor = card.tagColor() != null ? card.tagColor() : LABEL;
            rect(tagX, py, tagW, pillH, pillR, a(new Color(10, 10, 12, 175), alpha));
            rect(tagX, py, tagW, pillH, pillR, a(withAlpha(tagColor, 34), alpha));
            textCenter(SEMIBOLD, card.tag(), tagX + tagW / 2f, pcy, small, a(tagColor, alpha));
        }

        float titleY = cy + ch - r(37f), titleSize = r(15f);
        float timerW = 0f;
        if (card.endsAt() > 0L) {
            String timer = timer(card.endsAt());
            timerW = SEMIBOLD.getWidth(timer, titleSize);
            textRight(SEMIBOLD, timer, cx + cw - r(11f), titleY, titleSize, a(mix(card.phaseColor(), Color.WHITE, 0.15f), alpha));
            icon(CLOCK, cx + cw - r(11f) - timerW - r(9f), titleY, r(10f), a(mix(card.phaseColor(), Color.WHITE, 0.15f), alpha * 0.9f));
            timerW += r(20f);
        }
        String title = ellipsize(SEMIBOLD, card.title(), titleSize, cw - r(22f) - timerW);
        text(SEMIBOLD, title, cx + r(11f), titleY, titleSize, a(Color.WHITE, alpha));

        float subY = cy + ch - r(17f), subSize = r(11.5f), rightSize = r(11f);
        float serverX = cx + r(11f), serverTextX = serverX + r(13f), limit = cx + cw - r(11f);
        String server = ellipsize(MEDIUM, card.server(), subSize, limit - serverTextX);
        float serverEnd = serverTextX + MEDIUM.getWidth(server, subSize);
        if (card.serverColor() != null) {
            float sd = r(7f);
            rect(serverX, subY - sd / 2f, sd, sd, sd / 2f, a(card.serverColor(), alpha));
        } else {
            icon(SERVER, serverX + r(4f), subY, r(10f), a(new Color(176, 176, 184), alpha));
        }
        text(MEDIUM, server, serverTextX, subY, subSize, a(new Color(214, 214, 220), alpha));

        boolean isCoords = card.coords() != null;
        String right = isCoords ? card.coords() : card.extra();
        if (right != null) {
            float avail = limit - serverEnd - r(10f) - (isCoords ? r(14f) : 0f);
            if (isCoords && MEDIUM.getWidth(right, rightSize) > avail) {
                String[] xyz = right.split(" ");
                if (xyz.length == 3) right = xyz[0] + " " + xyz[2];
            }
            if (MEDIUM.getWidth(right, rightSize) > avail) right = avail > r(24f) ? ellipsize(MEDIUM, right, rightSize, avail) : null;
            if (right != null) {
                float rightW = MEDIUM.getWidth(right, rightSize);
                textRight(MEDIUM, right, limit, subY, rightSize, a(new Color(200, 200, 206), alpha));
                if (isCoords) icon(PIN, limit - rightW - r(8f), subY, r(10f), a(new Color(200, 200, 206), alpha));
            }
        }
    }

    private void drawEmpty(boolean noQuery, float alpha) {
        float cx = x + w / 2f, cy = gridY() + gridH() / 2f - r(10f);
        float box = r(56f);
        rect(cx - box / 2f, cy - r(52f), box, box, r(12f), a(ROW, alpha));
        String title, hint;
        Status status = service.getStatus();
        String name = service.getSource() == Source.FT ? "FunTime" : "HolyWorld";
        if (!noQuery && status == Status.OK) {
            title = "Ничего не найдено";
            hint = "Поиск идёт по названию ивента и серверу";
        } else if (status == Status.LOADING) {
            title = "Загружаем ивенты";
            hint = "Запрашиваем " + name + "...";
        } else if (status == Status.ERROR) {
            title = "Не удалось получить ивенты";
            hint = name + " не отвечает, пробуем ещё раз";
        } else {
            title = "Сейчас нет активных ивентов";
            hint = "Список обновляется автоматически";
        }
        if (status == Status.LOADING) {
            for (int i = 0; i < 3; i++) {
                float t = pulse(900L, i * 150L), s = r(6f) + r(2f) * (1f - Math.abs(t * 2f - 1f));
                float dx = cx + (i - 1) * r(12f);
                rect(dx - s / 2f, cy - r(24f) - s / 2f, s, s, s / 2f, a(ROW_TEXT_ON, alpha * (0.4f + 0.6f * (1f - Math.abs(t * 2f - 1f)))));
            }
        } else {
            icon(SPARKLE, cx, cy - r(24f), r(24f), a(status == Status.ERROR ? new Color(248, 113, 113) : ROW_TEXT_ON, alpha));
        }
        textCenter(SEMIBOLD, title, cx, cy + r(22f), r(14f), a(TEXT, alpha));
        textCenter(MEDIUM, hint, cx, cy + r(42f), r(11f), a(DIM, alpha));
    }


    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return false;
        for (int i = 0; i < SOURCES.length; i++) {
            if (inside(mx, my, x + r(3f) + segmentW() * i, y, segmentW(), headerH())) {
                if (service.getSource() != SOURCES[i]) {
                    service.setSource(SOURCES[i]);
                    cardAnims.clear();
                    resetScroll();
                }
                return true;
            }
        }
        if (!inside(mx, my, x, gridY(), w, gridH())) return false;
        float cw = cardW(), ch = cardH();
        for (int i = 0; i < shown.size(); i++) {
            float cx = x + (i % COLUMNS) * (cw + gap());
            float cy = gridY() + (i / COLUMNS) * (ch + gap()) - scroll;
            if (!inside(mx, my, cx, cy, cw, ch)) continue;
            openEvent(shown.get(i));
            return true;
        }
        return false;
    }

    private void openEvent(Card card) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        float[] anim = cardAnims.get(card.key());
        if (anim != null) anim[1] = 1f;

        Vector3i gps = parseCoords(card.coords());
        if (gps != null) {
            GpsManager.getInstance().setGpsName(card.title());
            GpsManager.getInstance().setGpsPosition(gps);
        }

        int anarchy = digits(card.server());
        if (anarchy > 0) AnarchyReconnectService.getInstance().start(Target.ANARCHY, anarchy);
        statusText = anarchy > 0 ? "Заходим на " + anarchy : "Метка установлена";
        copiedAt = System.currentTimeMillis();
        mc.gui.setScreen(null);
    }

    private static Vector3i parseCoords(String coords) {
        if (coords == null) return null;
        String[] xyz = coords.trim().split(" ");
        if (xyz.length != 3) return null;
        try {
            return new Vector3i(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int digits(String text) {
        Matcher m = Pattern.compile("(\\d+)").matcher(text == null ? "" : text);
        if (!m.find()) return -1;
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!inside(mx, my, x, gridY(), w + r(14f), gridH())) return false;
        scrollTarget -= (float) amount * r(46f);
        return true;
    }


    private static String timer(long endsAt) {
        long left = Math.max(0L, endsAt - System.currentTimeMillis()) / 1000L;
        long hours = left / 3600L, minutes = left / 60L % 60L, seconds = left % 60L;
        return hours > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private static String plural(int n, String one, String few, String many) {
        int mod100 = n % 100, mod10 = n % 10;
        if (mod100 >= 11 && mod100 <= 14) return many;
        if (mod10 == 1) return one;
        if (mod10 >= 2 && mod10 <= 4) return few;
        return many;
    }

    private static float pulse(long period) {
        return pulse(period, 0L);
    }

    private static float pulse(long period, long offset) {
        return ((System.currentTimeMillis() + period - offset % period) % period) / (float) period;
    }

    private static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private static Identifier glyph(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/events/glyphs/" + name + ".svg");
    }
}
