package tokyo.ghoul.client.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.joml.Vector4f;
import com.mojang.math.Axis;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.system.draggable.DraggableManager;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.system.interfaces.IRenderer;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.widget.overlay.WatermarkWidget;


import java.awt.Color;

@Getter
@Setter
public abstract class Widget implements QuickImports, IRenderer {
    protected Widget(float x, float y) {
        this.draggable = create(x, y, getName());
    }

    private final Easing easing = Easing.SINE_OUT;
    private final long duration = 100;

    public abstract String getName();
    private final Draggable draggable;
    private boolean enabled;
    private boolean showBackground = true;
    private boolean liquidGlass;
    private Boolean userLiquidGlassChoice;
    private boolean hudSettingsMenu;
    private boolean menuOpen;
    private Screen menuScreen;
    private float menuX, menuY, menuW, menuRowH;
    private final AnimationUtil menuAnim = new AnimationUtil();
    private static final String[] HUD_MENU_OPTIONS = {"Фон", "Жидкое стекло"};

    private Draggable create(float x, float y, String name) {
        return DraggableManager.getInstance().create(InterfaceModule.getInstance(), name, x, y);
    }

    public void render(Render2DEvent.Render2DEventData event) {
        render(event.matrixStack());
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaledHud(value);
    }

    public float getScale() { return RenderService.getInstance().getHudScale(); }
    public float getGap() { return scaled(3f); }
    public Font getMediumFont() { return Fonts.PS_MEDIUM; }
    public Font getSemiBoldFont() { return Fonts.PS_BOLD; }


    protected static final Color PANEL_OUTLINE = new Color(23, 23, 23);
    protected static final Color PANEL_HEADER = new Color(14, 14, 16);
    protected static final Color PANEL_BODY = new Color(14, 14, 16);
    protected static final Color PANEL_SEPARATOR = new Color(83, 83, 83);
    protected static final Identifier GLOW = FileUtil.getImage("interface/glow");

    protected float k(float value) {
        return scaled(value * 2f / 3f);
    }

    public boolean isLiquidGlass() {
        Boolean saved = draggable.getLiquidGlass();
        return saved != null ? saved : liquidGlass;
    }

    public void setLiquidGlass(boolean value) {
        liquidGlass = value;
        draggable.setLiquidGlass(value);
        DraggableManager.getInstance().save();
    }

    public boolean isEffectiveLiquidGlass() {
        if (this instanceof WatermarkWidget) {
            return isLiquidGlass();
        }
        Boolean saved = draggable.getLiquidGlass();
        if (saved != null) {
            return saved;
        }
        return isWatermarkLiquidGlass();
    }

    protected boolean showBackground() {
        return !isEffectiveLiquidGlass();
    }

    public static boolean isWatermarkLiquidGlass() {
        try {
            for (Widget widget : WidgetManager.getInstance().getWidgets()) {
                if (widget instanceof WatermarkWidget) {
                    return widget.isLiquidGlass();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    protected boolean liquidGlass() {
        return isEffectiveLiquidGlass();
    }

    protected void enableHudSettingsMenu() {
        if (hudSettingsMenu) return;
        hudSettingsMenu = true;
        KeyEvent.getInstance().subscribe(new Listener<>(event -> {
            if (!event.mouse() || !isEnabled() || mc.player == null) return;
            var screen = mc.gui.screen();
            if (screen != null && !(screen instanceof ChatScreen)) return;
            if (event.action() == 1 && (event.key() == 0 || event.key() == 1)) handleHudSettingsPress(event.key());
        }));
    }

    protected void drawPanel(PoseStack ms, float x, float y, float width, float height, float headerHeight, float alpha) {
        float corner = k(5.5f), stroke = k(1f);
        int a = (int) (255 * alpha);

        if (liquidGlass()) {
            drawLiquidGlass(ms, x, y, width, height, headerHeight, alpha);
            return;
        }

        RenderUtil.RECT.draw(ms, x, y, width, height, corner, ColorUtil.setAlpha(PANEL_OUTLINE, a));
        RenderUtil.RECT.draw(ms, x + stroke, y + stroke, width - stroke * 2f, headerHeight - stroke,
                new Vector4f(corner - stroke, corner - stroke, 0f, 0f), ColorUtil.setAlpha(PANEL_HEADER, a));
        RenderUtil.RECT.draw(ms, x + stroke, y + headerHeight, width - stroke * 2f, height - headerHeight - stroke,
                new Vector4f(0f, 0f, corner - stroke, corner - stroke), ColorUtil.setAlpha(PANEL_BODY, a));
    }

    protected void drawLiquidGlass(PoseStack ms, float x, float y, float width, float height, float headerHeight, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (255f * alpha)));
        RenderUtil.BLUR_RECT.draw(ms, x, y, width, height, k(5.5f), new Color(255, 255, 255, a), 0f);
        RenderUtil.RECT.draw(ms, x, y, width, height, k(5.5f), new Color(14, 14, 16, (int) (150f * alpha)));
    }

    protected void drawPanelHeader(PoseStack ms, float x, float y, float width, float headerHeight, String iconChar, String title, float alpha) {
        int hc = (int) (255 * alpha);
        float textSize = k(7.5f);
        float textY = y + (headerHeight - textSize) / 2f + k(0.6f);

        float iconSize = k(11f);
        float iconW = Fonts.WYVERN.getWidth(iconChar, iconSize);
        Fonts.WYVERN.drawText(ms, iconChar, x + k(6.5f), y + headerHeight / 2f - iconSize / 2f, iconSize,
                ColorUtil.setAlpha(UIColors.primary(), hc));

        float slashX = x + k(6.5f) + iconW + k(1.5f);
        float slashW = Fonts.SF_BOLD.getWidth("/", textSize);
        Fonts.SF_BOLD.drawText(ms, "/", slashX, textY, textSize, ColorUtil.setAlpha(Color.WHITE, (int) (110 * alpha)));

        float titleX = slashX + slashW + k(3f);
        float titleW = Fonts.SF_MEDIUM.getWidth(title, textSize);
        RenderUtil.TEXTURE_RECT.draw(ms, titleX + titleW / 2f - k(30f), textY + textSize / 2f - k(17.5f),
                k(60f), k(35f), 0.01f, ColorUtil.setAlpha(Color.WHITE, (int) (18 * alpha)), 0f, 0f, 1f, 1f, GLOW);
        Fonts.SF_MEDIUM.drawText(ms, title, titleX, textY, textSize, ColorUtil.setAlpha(Color.WHITE, (int) (230 * alpha)));

        float dotsSize = k(11f);
        float dotsW = Fonts.SF_BOLD.getWidth("...", dotsSize);
        Fonts.SF_BOLD.drawText(ms, "...", x + width - dotsW - k(6f), y + k(2.5f), dotsSize,
                ColorUtil.setAlpha(UIColors.primary(), hc));
    }

    protected void drawIslandHeader(PoseStack ms, float x, float y, float width, float headerHeight, String iconChar, String title, float alpha) {
        int hc = (int) (255 * alpha);

        float titleSize = k(7.5f);
        float titleY = y + (headerHeight - titleSize) / 2f + k(0.6f);
        float titleW = Fonts.SF_MEDIUM.getWidth(title, titleSize);
        RenderUtil.TEXTURE_RECT.draw(ms, x + k(6.5f) + titleW / 2f - k(30f), titleY + titleSize / 2f - k(17.5f),
                k(60f), k(35f), 0.01f, ColorUtil.setAlpha(Color.WHITE, (int) (18 * alpha)), 0f, 0f, 1f, 1f, GLOW);
        Fonts.SF_MEDIUM.drawText(ms, title, x + k(6.5f), titleY, titleSize,
                ColorUtil.setAlpha(Color.WHITE, (int) (230 * alpha)));

        float iconSize = k(11f);
        float iconW = Fonts.WYVERN.getWidth(iconChar, iconSize);
        float iconX = x + width - iconW - k(6f);
        Fonts.WYVERN.drawText(ms, iconChar, iconX, y + headerHeight / 2f - iconSize / 2f,
                iconSize, ColorUtil.setAlpha(UIColors.primary(), hc));

        float islandH = k(2.4f);
        float islandY = y;
        Vector4f radius = new Vector4f(0f, 0f, islandH / 2f, islandH / 2f);
        float glowW = iconW + k(16f), glowH = k(14f);
        RenderUtil.TEXTURE_RECT.draw(ms, iconX + iconW / 2f - glowW / 2f, islandY + islandH / 2f - glowH / 2f,
                glowW, glowH, 0.01f, ColorUtil.setAlpha(UIColors.primary(), (int) (120 * alpha)), 0f, 0f, 1f, 1f, GLOW);
        RenderUtil.RECT.draw(ms, iconX, islandY, iconW, islandH, radius, ColorUtil.setAlpha(UIColors.primary(), hc));
    }

    protected void drawHudRing(PoseStack ms, float centerX, float centerY, float radius, float thickness, float progress, Color color) {
        if (progress <= 0f || color.getAlpha() <= 0) return;
        int segments = Math.max(24, (int) Math.ceil(84f * progress));
        float sweep = 360f * progress;
        float start = -90f;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) Math.toRadians(start + sweep * i / segments);
            float a1 = (float) Math.toRadians(start + sweep * (i + 1) / segments);
            float x0 = centerX + (float) Math.cos(a0) * radius;
            float y0 = centerY + (float) Math.sin(a0) * radius;
            float x1 = centerX + (float) Math.cos(a1) * radius;
            float y1 = centerY + (float) Math.sin(a1) * radius;
            float dx = x1 - x0, dy = y1 - y0;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            ms.pushPose();
            ms.translate(x0, y0, 0f);
            ms.mulPose(Axis.ZP.rotation((float) Math.atan2(dy, dx)));
            RenderUtil.RECT.draw(ms, -0.35f, -thickness * 0.5f, length + 0.7f, thickness, thickness * 0.5f, color);
            ms.popPose();
        }
    }

    protected String fitText(String text, float maxWidth, float size, boolean medium) {
        if (maxWidth <= 0f) return "";
        Font font = medium ? Fonts.SF_MEDIUM : Fonts.SF_BOLD;
        if (font.getWidth(text, size) <= maxWidth) return text;

        String dots = "..";
        float dotsWidth = font.getWidth(dots, size);
        if (dotsWidth > maxWidth) return "";

        StringBuilder result = new StringBuilder();
        float width = 0f;
        for (int i = 0; i < text.length(); i++) {
            float charWidth = font.getWidth(String.valueOf(text.charAt(i)), size);
            if (width + charWidth + dotsWidth > maxWidth) break;
            result.append(text.charAt(i));
            width += charWidth;
        }
        return result + dots;
    }

    protected void handleHudSettingsPress(int button) {
        Draggable d = getDraggable();
        float mx = d.normaliseX(), my = d.normaliseY();

        if (d.isHovering()) {
            if (button == 1) {
                if (menuOpen) closeHudSettingsMenu();
                else {
                    menuScreen = mc.gui.screen();
                    menuOpen = true;
                    menuAnim.run(1f, 150, Easing.EXPO_OUT);
                }
            } else if (menuOpen) {
                closeHudSettingsMenu();
            }
            return;
        }
        if (!menuOpen) return;

        int option = hudMenuOptionAt(mx, my);
        if (option < 0) {
            closeHudSettingsMenu();
            return;
        }
        toggleHudSetting(option);
    }

    private void toggleHudSetting(int option) {
        if (option == 0) {
            userLiquidGlassChoice = false;
            setLiquidGlass(false);
            setShowBackground(true);
        } else if (option == 1) {
            userLiquidGlassChoice = true;
            setLiquidGlass(true);
            setShowBackground(false);
        }
    }

    private boolean hudSettingEnabled(int option) {
        boolean lg = isEffectiveLiquidGlass();
        return option == 0 ? !lg : lg;
    }

    private void closeHudSettingsMenu() {
        menuOpen = false;
        menuAnim.run(0f, 150, Easing.EXPO_OUT);
    }

    private int hudMenuOptionAt(float mx, float my) {
        if (mx < menuX || mx > menuX + menuW || my < menuY || my > menuY + menuRowH * HUD_MENU_OPTIONS.length) return -1;
        return (int) ((my - menuY) / menuRowH);
    }

    protected void renderHudSettingsMenu(PoseStack ms) {
        if (!hudSettingsMenu) return;
        if (menuOpen && mc.gui.screen() != menuScreen) closeHudSettingsMenu();
        if (menuOpen && getDraggable().isDragging()) closeHudSettingsMenu();

        menuAnim.update();
        float anim = (float) menuAnim.getValue();

        float textS = k(7.5f), pad = k(5.5f), corner = k(2f);
        float optW = 0f;
        for (String option : HUD_MENU_OPTIONS) {
            optW = Math.max(optW, Fonts.SF_MEDIUM.getWidth(option, textS) + Fonts.SF_MEDIUM.getWidth("Выкл", textS) + k(18f));
        }
        menuW = optW + pad * 2f;
        menuRowH = k(13f);
        menuX = getDraggable().getX();
        menuY = getDraggable().getY() + getDraggable().getHeight() + k(3f);

        if (anim <= 0.01f) return;

        int a = (int) (255 * anim);
        float slide = (1f - anim) * k(4f);
        float top = menuY - slide;
        float menuH = menuRowH * HUD_MENU_OPTIONS.length;

        RelicStyle.menuPanel(ms, menuX, top, menuW, menuH, corner, anim);

        float mx = getDraggable().normaliseX(), my = getDraggable().normaliseY();
        for (int i = 0; i < HUD_MENU_OPTIONS.length; i++) {
            float rowY = top + i * menuRowH;
            boolean hovered = mx >= menuX && mx <= menuX + menuW && my >= rowY && my < rowY + menuRowH;
            boolean on = hudSettingEnabled(i);
            String state = on ? "Вкл" : "Выкл";

            if (hovered) {
                RelicStyle.menuHover(ms, menuX + k(1.5f), rowY + k(1.5f), menuW - k(3f), menuRowH - k(3f), k(1.5f), anim);
            }

            Fonts.SF_MEDIUM.drawText(ms, HUD_MENU_OPTIONS[i], menuX + pad, rowY + (menuRowH - textS) / 2f + k(1f),
                    textS, ColorUtil.setAlpha(RelicStyle.ROW_TEXT, hovered ? a : (int) (a * 0.8f)));
            float stateW = Fonts.SF_MEDIUM.getWidth(state, textS);
            Fonts.SF_MEDIUM.drawText(ms, state, menuX + menuW - pad - stateW, rowY + (menuRowH - textS) / 2f + k(1f),
                    textS, ColorUtil.setAlpha(on ? UIColors.primary() : Color.WHITE, on ? a : (int) (a * 0.55f)));
        }
    }
}
