package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.services.MediaService;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class MusicMiniWidget extends Widget {
    private static final Color ARTIST = new Color(84, 86, 90);

    private final AnimationUtil alphaAnim = new AnimationUtil();

    public MusicMiniWidget() {
        super(200f, 3f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return "Music mini"; }

    @Override
    public void render(PoseStack ms) {
        MediaService media = MediaService.getInstance();
        media.ensureStarted();

        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        boolean hasTrack = media.hasTrack();

        alphaAnim.update();
        alphaAnim.run(hasTrack || editing ? 1f : 0f, 250, Easing.EXPO_OUT);
        float alpha = (float) alphaAnim.getValue();
        if (alpha <= 0.01f) return;

        float x = getDraggable().getX(), y = getDraggable().getY();

        float h = u(124f), corner = u(34f), stroke = u(4f);
        float coverSize = u(88f), coverR = u(16f);
        float textSize = u(44f);
        float sepW = u(10f), sepH = u(23f);

        var font = Fonts.HUD_MEDIUM;
        String title = fit(font, hasTrack ? media.info().title() : "Example", textSize, u(330f));
        String artist = fit(font, hasTrack ? media.info().artist() : "Artist", textSize, u(230f));
        String time = MediaService.formatTime(hasTrack ? media.positionMs() : 83_000L);

        float titleW = width(font, title, textSize);
        float artistW = artist.isEmpty() ? 0f : width(font, artist, textSize);
        float timeW = width(font, time, textSize);

        float w = u(18f) + coverSize + u(22f) + titleW
                + (artistW > 0f ? u(31f) + artistW : 0f)
                + u(39f) + sepW + u(34f) + timeW + u(44f);

        panel(ms, x, y, w, h, corner, stroke, BORDER, FILL, alpha, liquidGlass());

        float cy = y + h / 2f;
        float cx = x + u(18f);

        Identifier cover = media.coverTexture();
        if (cover != null) {
            cover(ms, cover, media.coverAspect(), cx, cy - coverSize / 2f, coverSize, coverR, alpha);
        } else {
            RenderUtil.RECT.draw(ms, cx, cy - coverSize / 2f, coverSize, coverSize, coverR,
                    a(ACCENT, alpha * 0.13f));
        }
        cx += coverSize + u(22f);

        text(ms, font, title, cx, cy, textSize, ACCENT, alpha);
        cx += titleW;
        if (artistW > 0f) {
            cx += u(31f);
            text(ms, font, artist, cx, cy, textSize, ARTIST, alpha);
            cx += artistW;
        }
        cx += u(39f);
        separator(ms, cx, cy, sepW, sepH, alpha);
        cx += sepW + u(34f);
        text(ms, font, time, cx, cy, textSize, ACCENT, alpha);

        getDraggable().setWidth(w);
        getDraggable().setHeight(h);
        renderHudSettingsMenu(ms);
    }
}
