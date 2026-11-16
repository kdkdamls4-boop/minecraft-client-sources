package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.services.MediaService;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class MusicWidget extends Widget {
    private static final Color ARTIST = new Color(84, 86, 90);
    private static final Color DURATION = new Color(80, 81, 84);
    private static final Color CURRENT = new Color(197, 198, 199);

    private final AnimationUtil alphaAnim = new AnimationUtil();

    private final float[][] hit = new float[3][4];
    private float lastAlpha;

    public MusicWidget() {
        super(3f, 160f);
        enableHudSettingsMenu();
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.mouse() || !isEnabled() || mc.player == null) return;
            if (!(mc.gui.screen() instanceof ChatScreen)) return;
            if (event.action() == 1 && event.key() == 0) handleClick();
        }));
    }

    @Override
    public String getName() { return "Music"; }

    @Override
    public void render(PoseStack ms) {
        MediaService media = MediaService.getInstance();
        media.ensureStarted();

        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        boolean hasTrack = media.hasTrack();

        alphaAnim.update();
        alphaAnim.run(hasTrack || editing ? 1f : 0f, 250, Easing.EXPO_OUT);
        float alpha = (float) alphaAnim.getValue();
        lastAlpha = alpha;
        if (alpha <= 0.01f) return;

        float x = getDraggable().getX(), y = getDraggable().getY();

        float w = u(606f), h = u(324f), corner = u(32f), stroke = u(4f);
        float coverSize = u(176f), coverR = u(28f);
        float textX = x + u(225f), textMaxW = w - u(225f) - u(38f);
        float titleSize = u(50f), artistSize = u(42f), timeSize = u(40f);

        String title = hasTrack ? media.info().title() : "Example";
        String artist = hasTrack ? media.info().artist() : "Artist";

        panel(ms, x, y, w, h, corner, stroke, BORDER, FILL, alpha, liquidGlass());

        float coverX = x + u(20f), coverY = y + u(18f);
        Identifier cover = media.coverTexture();
        if (cover != null) {
            cover(ms, cover, media.coverAspect(), coverX, coverY, coverSize, coverR, alpha);
        } else {
            RenderUtil.RECT.draw(ms, coverX, coverY, coverSize, coverSize, coverR, a(ACCENT, alpha * 0.13f));
        }

        var titleFont = Fonts.HUD_SEMIBOLD;
        var textFont = Fonts.HUD_MEDIUM;
        text(ms, titleFont, fit(titleFont, title, titleSize, textMaxW), textX, y + u(80f), titleSize, ACCENT, alpha);
        text(ms, textFont, fit(textFont, artist, artistSize, textMaxW), textX, y + u(138f), artistSize, ARTIST, alpha);

        float rowCy = y + u(239f);
        long position = hasTrack ? media.positionMs() : 83_000L;
        long duration = hasTrack ? media.info().durationMs() : 189_000L;
        float progress = hasTrack ? media.progress() : 0.44f;
        text(ms, textFont, MediaService.formatTime(position), x + u(40f), rowCy, timeSize, CURRENT, alpha);

        String total = MediaService.formatTime(duration);
        float totalW = width(textFont, total, timeSize);
        text(ms, textFont, total, x + w - u(38f) - totalW, rowCy, timeSize, DURATION, alpha);

        float centerX = x + w / 2f;
        boolean playing = hasTrack && media.info().playing();
        float mx = getDraggable().normaliseX(), my = getDraggable().normaliseY();
        control(ms, 0, HudIcons.WM_PREV, centerX - u(64f), rowCy, u(32f), u(28f), mx, my, alpha);
        control(ms, 1, playing ? HudIcons.WM_PAUSE : HudIcons.WM_PLAY, centerX + u(3f), rowCy, u(24f), u(28f), mx, my, alpha);
        control(ms, 2, HudIcons.WM_NEXT, centerX + u(65f), rowCy, u(32f), u(28f), mx, my, alpha);

        progressBar(ms, x + u(40f), y + u(271f), w - u(40f) - u(38f), u(15f), progress, alpha);

        getDraggable().setWidth(w);
        getDraggable().setHeight(h);
        renderHudSettingsMenu(ms);
    }

    private void control(PoseStack ms, int index, Identifier texture, float cx, float cy, float w, float h,
                         float mx, float my, float alpha) {
        float pad = u(16f);
        hit[index][0] = cx - w / 2f - pad;
        hit[index][1] = cy - h / 2f - pad;
        hit[index][2] = w + pad * 2f;
        hit[index][3] = h + pad * 2f;

        boolean hovered = mc.gui.screen() instanceof ChatScreen
                && mx >= hit[index][0] && mx <= hit[index][0] + hit[index][2]
                && my >= hit[index][1] && my <= hit[index][1] + hit[index][3];
        if (index == 1) {
            icon(ms, texture, cx - w / 2f, cy, w, h, ACCENT, alpha);
        } else {
            float tintAlpha = alpha * (hovered ? 1f : 0.55f);
            HudIcons.drawGradient(ms, cx - w / 2f, cy - h / 2f, w, h, texture, a(ACCENT, tintAlpha), a(accent2(), tintAlpha));
        }
    }

    private void handleClick() {
        if (lastAlpha < 0.5f) return;
        Draggable d = getDraggable();
        float mx = d.normaliseX(), my = d.normaliseY();
        MediaService media = MediaService.getInstance();
        for (int i = 0; i < hit.length; i++) {
            if (mx >= hit[i][0] && mx <= hit[i][0] + hit[i][2] && my >= hit[i][1] && my <= hit[i][1] + hit[i][3]) {
                switch (i) {
                    case 0 -> media.previous();
                    case 1 -> media.toggle();
                    default -> media.next();
                }
                return;
            }
        }
    }
}
