package tokyo.ghoul.client.ui.menu;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.theme.ThemeEditor;

import java.awt.Color;
import java.util.Arrays;

public final class MainMenuScreen extends Screen {
    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final float LAYOUT_W = 1920f;
    private static final float LAYOUT_H = 1080f;
    private static final String[] BUTTON_TITLES = {"Singleplayer", "Multiplayer", "AltManager", "Settings", "Quit"};
    private static final String[] BUTTON_ICONS = {"e", "g", "h", "i", "d"};
    private static final float BUTTON_HEIGHT = 48.4f;
    private static final float BUTTON_STEP = 58.9f;
    private static final float BUTTON_RADIUS = 13.125f;
    private static final float BUTTON_EXTRA_W = 95.15f;
    private static final float BUTTON_WIDTH_SCALE = 1.5f;
    private static final float ICON_X = 21f, ICON_Y = 1.05f, ICON_SIZE = 18.9f;
    private static final float LABEL_Y = 16.8f, LABEL_SIZE = 13.125f;
    public static final int BLUR_ITERATIONS = 3;
    private static final float OUTLINE_PX = 0.4f;
    private static final Color FILL_IDLE = new Color(255, 255, 255, 10);
    private static final Color FILL_HOVER = new Color(255, 255, 255, 24);
    private static final Color OUTLINE_IDLE = new Color(255, 255, 255, 5);
    private static final Color OUTLINE_HOVER = new Color(255, 255, 255, 10);
    private static final Color ICON_IDLE = new Color(255, 255, 255, 123);
    private static final Color LABEL = new Color(255, 255, 255, 184);
    private static final int BUTTON_COUNT = BUTTON_TITLES.length;

    private static final float WELCOME_SIZE = 52f;
    private static final float NAME_SIZE = 10f;
    private static final float NAME_OFFSET = 22f;
    private static final float WELCOME_GAP = 80f;
    private static final float WELCOME_OFFSET = NAME_OFFSET + WELCOME_GAP;

    private static final float GALLERY_BTN_W = 116f, GALLERY_BTN_H = 34f, GALLERY_BTN_GAP = 16f;
    private static final float ACTION_RADIUS = 38f / 6f;
    private static final float CARD_VISIBLE = 2.6f;
    private static final float CARD_W = 330f, CARD_H = 185.6f, CARD_SPACING = 372f, CARD_RADIUS = 14f;
    private static final float CARD_SIDE_SCALE = 0.78f;
    private static final float ACTION_W = 150f, ACTION_H = 38f, ACTION_GAP = 14f;
    private static final float DRAG_THRESHOLD = 4f;

    private static final float INTRO_VEIL = 0.75f;
    private static final float INTRO_WALLPAPER = 1.6f;
    private static final float WELCOME_START = 0.12f, WELCOME_STAGGER = 0.045f, WELCOME_DUR = 0.75f;
    private static final float NAME_START = 0.55f, NAME_DUR = 0.7f;
    private static final float BUTTON_START = 0.32f, BUTTON_STAGGER = 0.065f, BUTTON_DUR = 0.85f;
    private static final float GALLERY_BTN_START = BUTTON_START + BUTTON_COUNT * BUTTON_STAGGER;

    private static final float WALLPAPER_BASE_ZOOM = 1.045f;
    private static final float WALLPAPER_INTRO_ZOOM = 0.12f;
    private static final float WALLPAPER_SWITCH_ZOOM = 0.06f;
    private static final float PARALLAX_UV = 0.016f;

    private static final float PRESS_DELAY = 0.14f;
    private static final float SHEEN_DUR = 0.65f;

    private static boolean allowVanillaTitleOnce;

    private static volatile float activeOpenProgress;
    private static volatile boolean active;

    private float openProgress;
    private float time;
    private long lastFrame = System.nanoTime();
    private final Spring[] hover = Spring.array(BUTTON_COUNT);
    private final Spring[] press = Spring.array(BUTTON_COUNT);
    private final boolean[] wasHovered = new boolean[BUTTON_COUNT];
    private final float[] sheen = new float[BUTTON_COUNT];
    private int pendingButton = -1;
    private float pendingTimer;
    private final Spring parallaxX = new Spring();
    private final Spring parallaxY = new Spring();
    private float buttonX;
    private final float[] buttonY = new float[BUTTON_COUNT];
    private final float[] buttonW = new float[BUTTON_COUNT];
    private float columnW;
    private float layoutK = 1f;
    private float buttonH;
    private float mouseFixedX, mouseFixedY;
    private float menuFade = 1f;

    private int wallpaperShown = MenuWallpapers.selectedIndex();
    private int wallpaperFrom = wallpaperShown;
    private float wallpaperFade = 1f;

    private boolean galleryOpen;
    private final Spring galleryAnim = new Spring();
    private final Spring galleryPos = new Spring();
    private float galleryTarget;
    private boolean galleryDown, galleryDragging;
    private float dragStartX, dragStartTarget, dragLastX, dragVelocity;
    private final Spring galleryBtnHover = new Spring();
    private final Spring applyHover = new Spring();
    private final Spring backHover = new Spring();
    private float galleryBtnX, galleryBtnY;
    private float cardCenterX, cardCenterY;
    private float applyX, backX, actionY;

    public MainMenuScreen() {
        super(Component.literal("Solution Main Menu"));
    }

    public static boolean consumeVanillaBypass() {
        boolean allow = allowVanillaTitleOnce;
        allowVanillaTitleOnce = false;
        return allow;
    }

    public static boolean isActive() {
        return active && Minecraft.getInstance().gui.screen() instanceof MainMenuScreen;
    }

    public static float openProgress() {
        return activeOpenProgress;
    }

    public static void renderBackdropLayer() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof MainMenuScreen screen)) return;
        RenderService.setGuiContext(true);
        try {
            screen.drawBackdrop();
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    public static void renderInterfaceLayer() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof MainMenuScreen screen)) return;
        RenderService.setGuiContext(true);
        try {
            screen.drawInterface();
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    @Override
    protected void init() {
        ThemeEditor.getInstance().init();
        openProgress = 0f;
        time = 0f;
        lastFrame = System.nanoTime();
        active = true;
        pendingButton = -1;
        Arrays.fill(sheen, 1f);
        for (int i = 0; i < BUTTON_COUNT; i++) {
            hover[i].reset(0f);
            press[i].reset(0f);
            wasHovered[i] = false;
        }
        galleryOpen = false;
        galleryDown = galleryDragging = false;
        galleryAnim.reset(0f);
        galleryBtnHover.reset(0f);
        applyHover.reset(0f);
        backHover.reset(0f);
        galleryTarget = MenuWallpapers.selectedIndex();
        galleryPos.reset(galleryTarget);
        wallpaperShown = MenuWallpapers.selectedIndex();
        wallpaperFrom = wallpaperShown;
        wallpaperFade = 1f;
        MenuWallpapers.warmTextures();
    }

    @Override
    public void removed() {
        active = false;
        super.removed();
    }

    private Minecraft client() {
        return Minecraft.getInstance();
    }

    private int fixedW() {
        return (int) Math.ceil(client().getWindow().getWidth() / (double) FIXED_GUI_SCALE);
    }

    private int fixedH() {
        return (int) Math.ceil(client().getWindow().getHeight() / (double) FIXED_GUI_SCALE);
    }

    private float toFixed(double c) {
        return (float) (c * client().getWindow().getGuiScale() / FIXED_GUI_SCALE);
    }

    private float toGui(float fixed) {
        return fixed * FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
    }

    private static Color accent() {
        return UIColors.primary();
    }

    private void layout(float sw, float sh) {
        float winW = client().getWindow().getWidth();
        float winH = client().getWindow().getHeight();
        float scale = Math.min(winW / LAYOUT_W, winH / LAYOUT_H);
        layoutK = scale / FIXED_GUI_SCALE;

        buttonH = BUTTON_HEIGHT * layoutK;
        columnW = 0f;
        for (int i = 0; i < BUTTON_COUNT; i++) {
            columnW = Math.max(columnW,
                    BUTTON_EXTRA_W * layoutK + Fonts.MENU_INTER.getWidth(BUTTON_TITLES[i], LABEL_SIZE * layoutK));
        }
        columnW *= BUTTON_WIDTH_SCALE;
        buttonX = (sw - columnW) / 2f;

        float headH = WELCOME_OFFSET;
        float buttonsH = (BUTTON_COUNT - 1) * BUTTON_STEP * layoutK + buttonH
                + (GALLERY_BTN_GAP + GALLERY_BTN_H) * layoutK;
        float firstY = (sh - (headH + buttonsH)) / 2f + headH;
        for (int i = 0; i < BUTTON_COUNT; i++) {
            buttonY[i] = firstY + i * BUTTON_STEP * layoutK;
            buttonW[i] = columnW;
        }

        layoutWallpaperPicker(sw, sh);
    }

    private void layoutWallpaperPicker(float sw, float sh) {
        float k = layoutK;
        galleryBtnX = buttonX + (columnW - GALLERY_BTN_W * k) / 2f;
        galleryBtnY = buttonY[BUTTON_COUNT - 1] + buttonH + GALLERY_BTN_GAP * k;

        cardCenterX = sw / 2f;
        cardCenterY = sh * 0.5f;
        actionY = cardCenterY + CARD_H * k / 2f + 64f * k;
        float pair = (ACTION_W * 2f + ACTION_GAP) * k;
        backX = (sw - pair) / 2f;
        applyX = backX + (ACTION_W + ACTION_GAP) * k;
    }

    private int galleryIndex() {
        return wrapIndex(Math.round(galleryTarget));
    }

    private static int wrapIndex(int i) {
        int count = MenuWallpapers.size();
        return count == 0 ? 0 : Math.floorMod(i, count);
    }

    // shortest signed distance from the carousel position to a card, looping around the ends
    private static float wrapOffset(int index, float pos) {
        int count = MenuWallpapers.size();
        float off = index - pos;
        return off - count * Math.round(off / count);
    }

    private void showWallpaper(int index) {
        if (index == wallpaperShown) return;
        wallpaperFrom = wallpaperShown;
        wallpaperShown = index;
        wallpaperFade = 0f;
    }

    private void openGallery() {
        if (MenuWallpapers.size() == 0) return;
        galleryOpen = true;
        galleryTarget = wallpaperShown;
        galleryPos.reset(galleryTarget);
        galleryDown = galleryDragging = false;
    }

    private void closeGallery(boolean apply) {
        if (!galleryOpen) return;
        galleryOpen = false;
        galleryDown = galleryDragging = false;
        if (apply) MenuWallpapers.select(galleryIndex());
        showWallpaper(MenuWallpapers.selectedIndex());
    }

    private void browseTo(float target) {
        galleryTarget = target;
        showWallpaper(galleryIndex());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);

            mouseFixedX = toFixed(mouseX);
            mouseFixedY = toFixed(mouseY);
            layout(fixedW(), fixedH());

            active = true;
            activeOpenProgress = openProgress;
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void drawBackdrop() {
        var matrices = RenderUtil.matrices();
        matrices.pushPose();
        float s = FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
        matrices.scale(s, s, 1f);
        drawWallpaper(fixedW(), fixedH());
        matrices.popPose();
    }

    private void drawInterface() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;
        time += dt;
        openProgress = easeOutCubic(clamp01(time / 0.6f));
        wallpaperFade = Math.min(1f, wallpaperFade + dt / 0.85f);
        galleryAnim.update(galleryOpen ? 1f : 0f, dt, 190f, 1f);
        if (galleryDragging) galleryPos.update(galleryTarget, dt, 700f, 1f);
        else galleryPos.update(galleryTarget, dt, 170f, 0.78f);
        menuFade = 1f - clamp01(galleryAnim.value);
        activeOpenProgress = openProgress;

        float sw = fixedW();
        float sh = fixedH();
        layout(sw, sh);

        float nx = clamp(mouseFixedX / Math.max(1f, sw) * 2f - 1f, -1f, 1f);
        float ny = clamp(mouseFixedY / Math.max(1f, sh) * 2f - 1f, -1f, 1f);
        parallaxX.update(nx, dt, 18f, 1f);
        parallaxY.update(ny, dt, 18f, 1f);

        if (pendingButton >= 0) {
            pendingTimer -= dt;
            if (pendingTimer <= 0f) {
                int button = pendingButton;
                pendingButton = -1;
                activateButton(button);
                return;
            }
        }

        var matrices = RenderUtil.matrices();
        matrices.pushPose();
        float s = FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
        matrices.scale(s, s, 1f);

        drawWelcome();
        drawButtons(dt);
        drawGalleryButton(dt);
        drawGallery(dt);

        matrices.popPose();
    }

    private void drawWelcome() {
        var matrices = RenderUtil.matrices();

        float centerX = buttonX + columnW / 2f;
        float nameY = buttonY[0] - NAME_OFFSET;
        String username = client().getUser().getName();

        String title = "Welcome!";
        float titleW = Fonts.ETERNAL_HEAVEN.getWidth(title, WELCOME_SIZE);
        float titleX = centerX - titleW / 2f;
        float titleY = nameY - WELCOME_GAP + (float) Math.sin(time * 1.15f) * 1.4f * smoothstep(clamp01(time - 1.2f));
        Color titleColor = new Color(247, 247, 252);
        for (int c = 0; c < title.length(); c++) {
            float p = clamp01((time - WELCOME_START - c * WELCOME_STAGGER) / WELCOME_DUR);
            if (p <= 0f) continue;
            String ch = title.substring(c, c + 1);
            float chW = Fonts.ETERNAL_HEAVEN.getWidth(ch, WELCOME_SIZE);
            float chX = titleX + Fonts.ETERNAL_HEAVEN.getWidth(title.substring(0, c + 1), WELCOME_SIZE) - chW;
            float rise = (1f - easeOutBack(p, 2.2f)) * 18f;
            float scale = 0.6f + 0.4f * easeOutBack(p, 1.6f);
            int a = (int) (255 * easeOutCubic(clamp01(p * 1.8f)) * menuFade);

            float pivotX = chX + chW / 2f;
            float pivotY = titleY + WELCOME_SIZE * 0.6f;
            matrices.pushPose();
            matrices.translate(pivotX, pivotY + rise, 0f);
            matrices.scale(scale, scale, 1f);
            matrices.translate(-pivotX, -pivotY, 0f);
            Fonts.ETERNAL_HEAVEN.drawText(matrices, ch, chX, titleY, WELCOME_SIZE, withA(titleColor, a));
            matrices.popPose();
        }

        float np = clamp01((time - NAME_START) / NAME_DUR);
        if (np <= 0f) return;
        float ne = easeOutExpo(np);
        int na = (int) (255 * easeOutCubic(np) * menuFade);
        float nameWidth = Fonts.DREAMLINE_SF.getWidth(username, NAME_SIZE);
        Fonts.DREAMLINE_SF.drawCenteredGradientText(matrices, username, centerX, nameY + (1f - ne) * 8f, NAME_SIZE,
                UIColors.primary(na), UIColors.secondary(na), Math.max(1.1f, nameWidth / 4f));
    }

    private void drawButtons(float dt) {
        for (int i = 0; i < BUTTON_COUNT; i++) drawButton(i, dt);
    }

    private void drawButton(int i, float dt) {
        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float x = buttonX, y = buttonY[i], width = buttonW[i];

        float p = clamp01((time - BUTTON_START - i * BUTTON_STAGGER) / BUTTON_DUR);
        if (p <= 0f) return;
        float alpha = easeOutCubic(clamp01(p * 1.7f)) * menuFade;
        if (alpha <= 0.004f) return;
        float enter = easeOutBack(p, 1.5f);

        boolean hovered = !galleryOpen && pendingButton < 0 && p > 0.4f && inside(mouseFixedX, mouseFixedY, x, y, width, buttonH);
        if (hovered && !wasHovered[i]) sheen[i] = 0f;
        wasHovered[i] = hovered;
        sheen[i] = Math.min(1f, sheen[i] + dt / SHEEN_DUR);

        hover[i].update(hovered ? 1f : 0f, dt, 260f, 0.62f);
        press[i].update(0f, dt, 420f, 0.5f);
        float h = clamp01(hover[i].value);

        float scale = (0.9f + 0.1f * enter) * (1f + 0.035f * hover[i].value) * (1f - 0.06f * press[i].value);
        float cx = x + width / 2f;
        float cy = y + buttonH / 2f + (1f - enter) * 26f * k;
        float sw = width * scale, sh = buttonH * scale;
        float sx = cx - sw / 2f, sy = cy - sh / 2f;
        float radius = BUTTON_RADIUS * k * scale;

        if (h > 0.01f) {
            RenderUtil.RECT.drawGlassShadow(matrices, sx, sy + 2f * k * h, sw, sh, radius, 18f * k,
                    withA(accent(), (int) (70 * h * alpha)));
        }
        drawGlass(sx, sy, sw, sh, radius, alpha);

        matrices.pushPose();
        matrices.translate(cx, cy, 0f);
        matrices.scale(scale, scale, 1f);
        matrices.translate(-(x + width / 2f), -(y + buttonH / 2f), 0f);

        RenderUtil.RECT.draw(matrices, x, y, width, buttonH, BUTTON_RADIUS * k, fade(mix(FILL_IDLE, FILL_HOVER, h), alpha));
        Color outline = mix(OUTLINE_IDLE, withA(accent(), 60), h);
        RenderUtil.RECT.drawOutline(matrices, x, y, width, buttonH, BUTTON_RADIUS * k, OUTLINE_PX / FIXED_GUI_SCALE + 0.35f * h,
                fade(outline, alpha));
        drawSheen(x, y, width, buttonH, BUTTON_RADIUS * k, sheen[i], alpha);

        float iconShift = 5f * k * hover[i].value;
        Fonts.MENU_ICONS.drawText(matrices, BUTTON_ICONS[i], x + ICON_X * k + iconShift, y + ICON_Y * k, ICON_SIZE * k,
                fade(mix(ICON_IDLE, accent(), h), alpha));
        float labelW = Fonts.MENU_INTER.getWidth(BUTTON_TITLES[i], LABEL_SIZE * k);
        Fonts.MENU_INTER.drawText(matrices, BUTTON_TITLES[i], x + (width - labelW) / 2f, y + LABEL_Y * k, LABEL_SIZE * k,
                fade(mix(LABEL, Color.WHITE, h), alpha));

        matrices.popPose();
    }

    private void drawSheen(float x, float y, float width, float height, float radius, float progress, float alpha) {
        if (progress >= 1f) return;
        var matrices = RenderUtil.matrices();
        float t = easeInOutCubic(progress);
        float left = x + radius, right = x + width - radius;
        float bandW = height * 1.1f;
        float center = left - bandW + (right - left + bandW * 2f) * t;
        float edgeFade = (float) Math.sin(Math.PI * t);
        int peak = (int) (34 * edgeFade * alpha);
        if (peak <= 0) return;

        Color clear = new Color(255, 255, 255, 0);
        Color glow = new Color(255, 255, 255, peak);
        float l0 = Math.max(left, center - bandW / 2f), l1 = Math.min(right, center);
        if (l1 > l0) RenderUtil.GRADIENT_RECT.draw(matrices, l0, y + 1f, l1 - l0, height - 2f, 0f, clear, glow, clear, glow);
        float r0 = Math.max(left, center), r1 = Math.min(right, center + bandW / 2f);
        if (r1 > r0) RenderUtil.GRADIENT_RECT.draw(matrices, r0, y + 1f, r1 - r0, height - 2f, 0f, glow, clear, glow, clear);
    }

    private static Color mix(Color from, Color to, float t) {
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }

    private static Color fade(Color color, float alpha) {
        return withA(color, Math.round(color.getAlpha() * alpha));
    }

    private void drawGlass(float x, float y, float width, float height, float radius, float alpha) {
        var matrices = RenderUtil.matrices();

        float inv = (float) client().getWindow().getGuiScale() / FIXED_GUI_SCALE;
        matrices.pushPose();
        matrices.scale(inv, inv, 1f);
        RenderUtil.BLUR_RECT.draw(matrices, toGui(x), toGui(y), toGui(width), toGui(height), toGui(radius),
                new Color(255, 255, 255, (int) (255 * alpha)), 0f);
        matrices.popPose();

    }

    private void drawWallpaper(float sw, float sh) {
        if (MenuWallpapers.size() == 0) return;

        var matrices = RenderUtil.matrices();
        float intro = easeOutExpo(clamp01(time / INTRO_WALLPAPER));
        float baseZoom = WALLPAPER_BASE_ZOOM + WALLPAPER_INTRO_ZOOM * (1f - intro);
        float fade = smoothstep(clamp01(wallpaperFade));
        if (fade < 1f) {
            drawWallpaperCover(matrices, MenuWallpapers.get(wallpaperFrom), sw, sh, 255, baseZoom);
        }
        float switchZoom = WALLPAPER_SWITCH_ZOOM * (1f - easeOutCubic(clamp01(wallpaperFade)));
        drawWallpaperCover(matrices, MenuWallpapers.get(wallpaperShown), sw, sh, (int) (255 * fade), baseZoom + switchZoom);

        RenderUtil.GRADIENT_RECT.draw(matrices, 0f, 0f, sw, sh, 0f,
                new Color(5, 7, 13, 18), new Color(5, 7, 13, 18),
                new Color(3, 5, 11, 70), new Color(3, 5, 11, 70));

        float veil = 1f - easeOutCubic(clamp01(time / INTRO_VEIL));
        if (veil > 0f) {
            RenderUtil.RECT.draw(matrices, 0f, 0f, sw, sh, 0f, new Color(3, 4, 8, (int) (255 * veil)));
        }
    }

    private void drawWallpaperCover(PoseStack matrices, MenuWallpapers.Wallpaper wallpaper, float sw, float sh, int alpha, float zoom) {
        if (wallpaper == null || alpha <= 0) return;

        float screen = sw / Math.max(1f, sh);
        float u, v, uvWidth, uvHeight;
        if (screen > wallpaper.aspect()) {
            u = 0f;
            uvWidth = 1f;
            uvHeight = wallpaper.aspect() / screen;
            v = (1f - uvHeight) * 0.5f;
        } else {
            v = 0f;
            uvHeight = 1f;
            uvWidth = screen / wallpaper.aspect();
            u = (1f - uvWidth) * 0.5f;
        }

        float zw = uvWidth / zoom, zh = uvHeight / zoom;
        float cu = u + uvWidth * 0.5f - parallaxX.value * PARALLAX_UV;
        float cv = v + uvHeight * 0.5f - parallaxY.value * PARALLAX_UV;
        u = clamp(cu - zw * 0.5f, 0f, 1f - zw);
        v = clamp(cv - zh * 0.5f, 0f, 1f - zh);
        uvWidth = zw;
        uvHeight = zh;

        RenderUtil.TEXTURE_RECT.drawSmooth(matrices, 0f, 0f, sw, sh, 0f,
                new Color(255, 255, 255, alpha), u, v, uvWidth, uvHeight, wallpaper.image());
    }

    private void drawGalleryButton(float dt) {
        float p = clamp01((time - GALLERY_BTN_START) / BUTTON_DUR);
        float alpha = easeOutCubic(clamp01(p * 1.7f)) * menuFade;
        if (alpha <= 0.004f || MenuWallpapers.size() == 0) return;

        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float w = GALLERY_BTN_W * k, h = GALLERY_BTN_H * k;
        float slide = (1f - easeOutBack(p, 1.5f)) * 26f * k;
        boolean hovered = !galleryOpen && pendingButton < 0 && p > 0.4f
                && inside(mouseFixedX, mouseFixedY, galleryBtnX, galleryBtnY, w, h);
        galleryBtnHover.update(hovered ? 1f : 0f, dt, 300f, 0.6f);
        float hv = clamp01(galleryBtnHover.value);

        float scale = 1f + 0.06f * galleryBtnHover.value;
        float sw = w * scale, sh = h * scale;
        float x = galleryBtnX - (sw - w) / 2f;
        float y = galleryBtnY + slide - (sh - h) / 2f;
        float radius = sh / 2f;

        if (hv > 0.01f) {
            RenderUtil.RECT.drawGlassShadow(matrices, x, y + 2f, sw, sh, radius, 12f * k,
                    withA(accent(), (int) (70 * hv * alpha)));
        }
        drawGlass(x, y, sw, sh, radius, alpha);
        RenderUtil.RECT.draw(matrices, x, y, sw, sh, radius, fade(mix(FILL_IDLE, FILL_HOVER, hv), alpha));
        RenderUtil.RECT.drawOutline(matrices, x, y, sw, sh, radius, OUTLINE_PX / FIXED_GUI_SCALE + 0.35f * hv,
                fade(mix(OUTLINE_IDLE, withA(accent(), 60), hv), alpha));

        float thumb = sh - 10f * k;
        MenuWallpapers.Wallpaper current = MenuWallpapers.selected();
        float tx = x + 5f * k, ty = y + 5f * k;
        if (current != null) {
            RenderUtil.TEXTURE_RECT.drawSmooth(matrices, tx, ty, thumb, thumb, thumb / 2f,
                    new Color(255, 255, 255, (int) (255 * alpha)), 0f, 0f, 1f, 1f, current.preview());
        }
        String label = "Gallery";
        float size = 12f * k;
        float lw = Fonts.MENU_INTER.getWidth(label, size);
        float lx = tx + thumb + (x + sw - tx - thumb - lw) / 2f - 1f * k;
        Fonts.MENU_INTER.drawText(matrices, label, lx, textTop(ty + thumb / 2f, size), size,
                fade(mix(LABEL, Color.WHITE, hv), alpha));
    }

    private void drawGallery(float dt) {
        float open = clamp01(galleryAnim.value);
        int count = MenuWallpapers.size();
        if (open <= 0.004f || count == 0) return;

        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float sw = fixedW(), sh = fixedH();
        float ease = easeOutCubic(open);

        // soft vignette at the bottom so cards and buttons stay readable on bright wallpapers
        RenderUtil.GRADIENT_RECT.draw(matrices, 0f, sh * 0.35f, sw, sh * 0.65f, 0f,
                new Color(2, 4, 9, 0), new Color(2, 4, 9, 0),
                new Color(2, 4, 9, (int) (170 * open)), new Color(2, 4, 9, (int) (170 * open)));

        float pos = galleryPos.value;
        int centerIdx = wrapIndex(Math.round(pos));

        // title of the focused wallpaper
        String name = MenuWallpapers.get(centerIdx).name();
        float titleSize = 15f * k;
        float tw = Fonts.MENU_INTER.getWidth(name, titleSize);
        float titleY = cardCenterY - CARD_H * k / 2f - 46f * k + (1f - ease) * 14f;
        Fonts.MENU_INTER.drawText(matrices, name, cardCenterX - tw / 2f, titleY, titleSize,
                new Color(255, 255, 255, (int) (235 * open)));
        if (centerIdx == MenuWallpapers.selectedIndex()) {
            String tag = "Current";
            float tagSize = 9.5f * k;
            float tagW = Fonts.MENU_INTER.getWidth(tag, tagSize);
            Fonts.MENU_INTER.drawText(matrices, tag, cardCenterX - tagW / 2f, titleY + titleSize + 5f * k, tagSize,
                    withA(accent(), (int) (230 * open)));
        }

        // cards, far ones first so the focused one ends up on top
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) order[i] = i;
        final float p = pos;
        Arrays.sort(order, (l, r) -> Float.compare(Math.abs(wrapOffset(r, p)), Math.abs(wrapOffset(l, p))));
        for (int idx : order) drawGalleryCard(idx, pos, open, ease);

        drawGalleryActions(dt, open);
        drawGalleryDots(pos, open, count);
    }

    private void drawGalleryCard(int index, float pos, float open, float ease) {
        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float off = wrapOffset(index, pos);
        float dist = Math.abs(off);
        if (dist > CARD_VISIBLE) return;
        float focus = 1f - clamp01(dist);

        float scale = (CARD_SIDE_SCALE + (1f - CARD_SIDE_SCALE) * focus) * (0.9f + 0.1f * ease);
        float w = CARD_W * k * scale, h = CARD_H * k * scale;
        float spacing = CARD_SPACING * k;
        float offScaled = Math.signum(off) * (Math.min(dist, 1f) * spacing + Math.max(0f, dist - 1f) * spacing * 0.82f);
        float cx = cardCenterX + offScaled * ease;
        float cy = cardCenterY + (1f - ease) * 30f * k;
        float x = cx - w / 2f, y = cy - h / 2f;
        float alpha = open * (0.45f + 0.55f * focus) * clamp01((CARD_VISIBLE - dist) * 1.6f);
        if (alpha <= 0.01f || x > fixedW() || x + w < 0f) return;
        float radius = CARD_RADIUS * k * scale;

        RenderUtil.RECT.drawGlassShadow(matrices, x, y + 8f * k, w, h, radius, 22f * k * (0.5f + 0.5f * focus),
                new Color(0, 0, 0, (int) (150 * alpha)));

        MenuWallpapers.Wallpaper wallpaper = MenuWallpapers.get(index);
        float cardAspect = CARD_W / CARD_H;
        float u = 0f, v = 0f, uw = 1f, uh = 1f;
        if (cardAspect > wallpaper.aspect()) {
            uh = wallpaper.aspect() / cardAspect;
            v = (1f - uh) * 0.5f;
        } else {
            uw = cardAspect / wallpaper.aspect();
            u = (1f - uw) * 0.5f;
        }
        RenderUtil.TEXTURE_RECT.drawSmooth(matrices, x, y, w, h, radius,
                new Color(255, 255, 255, (int) (255 * alpha)), u, v, uw, uh, wallpaper.image());

        float dim = 1f - focus;
        if (dim > 0.01f) {
            RenderUtil.RECT.draw(matrices, x, y, w, h, radius, new Color(6, 9, 16, (int) (120 * dim * alpha)));
        }
        boolean applied = index == MenuWallpapers.selectedIndex();
        Color edge = applied ? withA(accent(), (int) (230 * alpha)) : new Color(255, 255, 255, (int) (50 * focus * alpha));
        RenderUtil.RECT.drawOutline(matrices, x, y, w, h, radius, applied ? 1.2f : 0.6f, edge);
    }

    private void drawGalleryActions(float dt, float open) {
        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float w = ACTION_W * k, h = ACTION_H * k;
        float y = actionY + (1f - easeOutCubic(open)) * 24f * k;
        float radius = ACTION_RADIUS * k;
        float labelSize = 12.5f * k;
        float labelY = textTop(y + h / 2f, labelSize);

        boolean backHovered = galleryOpen && inside(mouseFixedX, mouseFixedY, backX, actionY, w, h);
        boolean applyHovered = galleryOpen && inside(mouseFixedX, mouseFixedY, applyX, actionY, w, h);
        backHover.update(backHovered ? 1f : 0f, dt, 300f, 0.6f);
        applyHover.update(applyHovered ? 1f : 0f, dt, 300f, 0.6f);
        float bh = clamp01(backHover.value), ah = clamp01(applyHover.value);

        // Back — glass
        drawGlass(backX, y, w, h, radius, open);
        RenderUtil.RECT.draw(matrices, backX, y, w, h, radius, fade(mix(FILL_IDLE, FILL_HOVER, bh), open));
        String back = "Back";
        float bw = Fonts.MENU_INTER.getWidth(back, labelSize);
        Fonts.MENU_INTER.drawText(matrices, back, backX + (w - bw) / 2f, labelY, labelSize,
                fade(mix(LABEL, Color.WHITE, bh), open));

        // Apply — accent
        boolean already = galleryIndex() == MenuWallpapers.selectedIndex();
        float enabled = already ? 0.45f : 1f;
        float ax = applyX - 2f * k * ah, aw = w + 4f * k * ah, ay = y - 1f * k * ah, ah2 = h + 2f * k * ah;
        if (!already) {
            RenderUtil.RECT.drawGlassShadow(matrices, ax, ay + 3f, aw, ah2, radius, (14f + 6f * ah) * k,
                    withA(accent(), (int) (110 * open)));
        }
        RenderUtil.RECT.draw(matrices, ax, ay, aw, ah2, radius,
                withA(mix(accent(), Color.WHITE, 0.12f * ah), (int) ((already ? 70 : 215 + 25 * ah) * open)));
        String apply = already ? "Applied" : "Apply";
        float pw = Fonts.MENU_INTER.getWidth(apply, labelSize);
        Fonts.MENU_INTER.drawText(matrices, apply, applyX + (w - pw) / 2f, labelY, labelSize,
                new Color(255, 255, 255, (int) (255 * enabled * open)));
    }

    private void drawGalleryDots(float pos, float open, int count) {
        var matrices = RenderUtil.matrices();
        float k = layoutK;
        float gap = 14f * k, dot = 5f * k;
        float total = (count - 1) * gap + dot;
        float x0 = cardCenterX - total / 2f;
        float y = actionY + ACTION_H * k + 26f * k;
        for (int i = 0; i < count; i++) {
            float near = 1f - clamp01(Math.abs(wrapOffset(i, pos)));
            float size = dot * (1f + 0.4f * near);
            int a = (int) ((90 + 150 * near) * open);
            RenderUtil.RECT.draw(matrices, x0 + i * gap - (size - dot) / 2f, y - (size - dot) / 2f, size, size, size / 2f,
                    new Color(255, 255, 255, a));
        }
    }

    // ---- input helpers -------------------------------------------------

    private boolean overCards(float mx, float my) {
        float k = layoutK;
        float h = CARD_H * k + 40f * k;
        return my >= cardCenterY - h / 2f && my <= cardCenterY + h / 2f;
    }

    private int cardAt(float mx, float my) {
        int count = MenuWallpapers.size();
        float k = layoutK;
        float pos = galleryPos.value;
        for (int i = 0; i < count; i++) {
            float off = wrapOffset(i, pos);
            float dist = Math.abs(off);
            if (dist > CARD_VISIBLE) continue;
            float focus = 1f - clamp01(dist);
            float scale = CARD_SIDE_SCALE + (1f - CARD_SIDE_SCALE) * focus;
            float spacing = CARD_SPACING * k;
            float offScaled = Math.signum(off) * (Math.min(dist, 1f) * spacing + Math.max(0f, dist - 1f) * spacing * 0.82f);
            float w = CARD_W * k * scale, h = CARD_H * k * scale;
            if (inside(mx, my, cardCenterX + offScaled - w / 2f, cardCenterY - h / 2f, w, h)) return Math.round(pos + off);
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubled);
        float mx = toFixed(event.x());
        float my = toFixed(event.y());
        float k = layoutK;

        if (galleryOpen) {
            float w = ACTION_W * k, h = ACTION_H * k;
            if (inside(mx, my, backX, actionY, w, h)) {
                closeGallery(false);
                return true;
            }
            if (inside(mx, my, applyX, actionY, w, h)) {
                closeGallery(true);
                return true;
            }
            if (overCards(mx, my)) {
                galleryDown = true;
                galleryDragging = false;
                dragStartX = dragLastX = mx;
                dragStartTarget = galleryTarget;
                dragVelocity = 0f;
                return true;
            }
            return true;
        }

        if (MenuWallpapers.size() > 0
                && inside(mx, my, galleryBtnX, galleryBtnY, GALLERY_BTN_W * k, GALLERY_BTN_H * k)) {
            openGallery();
            return true;
        }
        for (int i = 0; i < BUTTON_COUNT; i++) {
            if (inside(mx, my, buttonX, buttonY[i], buttonW[i], buttonH)) {
                if (pendingButton >= 0) return true;
                press[i].reset(1f);
                pendingButton = i;
                pendingTimer = PRESS_DELAY;
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (!galleryOpen || !galleryDown) return super.mouseDragged(event, dragX, dragY);
        float mx = toFixed(event.x());
        float spacing = CARD_SPACING * layoutK;
        if (!galleryDragging && Math.abs(mx - dragStartX) > DRAG_THRESHOLD) galleryDragging = true;
        if (galleryDragging) {
            dragVelocity = (dragLastX - mx) / spacing;
            dragLastX = mx;
            galleryTarget = dragStartTarget - (mx - dragStartX) / spacing;
            showWallpaper(galleryIndex());
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (!galleryDown) return super.mouseReleased(event);
        galleryDown = false;
        if (galleryDragging) {
            galleryDragging = false;
            browseTo(Math.round(galleryTarget + clamp(dragVelocity * 6f, -1.5f, 1.5f)));
        } else {
            int hit = cardAt(toFixed(event.x()), toFixed(event.y()));
            if (hit >= 0) browseTo(hit);
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!galleryOpen) return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        double amount = Math.abs(horizontalAmount) > Math.abs(verticalAmount) ? horizontalAmount : -verticalAmount;
        if (amount != 0) browseTo(Math.round(galleryTarget) + (amount > 0 ? 1 : -1));
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (galleryOpen) {
            switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> closeGallery(false);
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> closeGallery(true);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> browseTo(Math.round(galleryTarget) - 1);
                case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> browseTo(Math.round(galleryTarget) + 1);
                default -> {
                }
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) return true;
        return super.keyPressed(input);
    }

    private void activateButton(int i) {
        Minecraft mc = client();
        switch (i) {
            case 0 -> mc.gui.setScreen(new SelectWorldScreen(this));
            case 1 -> mc.gui.setScreen(new JoinMultiplayerScreen(this));
            case 2 -> mc.gui.setScreen(new AltScreen(this));
            case 3 -> mc.gui.setScreen(new OptionsScreen(this, mc.options, false));
            case 4 -> mc.stop();
            default -> {
            }
        }
    }

    private static float textTop(float centerY, float size) {
        return centerY - size * 0.564f;
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static Color withA(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float clamp01(float v) {
        return clamp(v, 0f, 1f);
    }

    private static float easeOutCubic(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    private static float easeOutExpo(float t) {
        return t >= 1f ? 1f : 1f - (float) Math.pow(2, -10f * t);
    }

    private static float easeOutBack(float t, float overshoot) {
        float u = t - 1f;
        return 1f + (overshoot + 1f) * u * u * u + overshoot * u * u;
    }

    private static float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private static final class Spring {
        private static final float STEP = 1f / 240f;

        float value;
        float velocity;

        static Spring[] array(int n) {
            Spring[] springs = new Spring[n];
            for (int i = 0; i < n; i++) springs[i] = new Spring();
            return springs;
        }

        void reset(float v) {
            value = v;
            velocity = 0f;
        }

        void update(float target, float dt, float stiffness, float damping) {
            float c = 2f * (float) Math.sqrt(stiffness) * damping;
            while (dt > 0f) {
                float h = Math.min(STEP, dt);
                velocity += ((target - value) * stiffness - velocity * c) * h;
                value += velocity * h;
                dt -= h;
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
