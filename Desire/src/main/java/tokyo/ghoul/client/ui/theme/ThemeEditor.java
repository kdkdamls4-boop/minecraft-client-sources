package tokyo.ghoul.client.ui.theme;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.WindowResizeEvent;
import tokyo.ghoul.api.system.configs.ThemeManager;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.fonts.Icons;
import tokyo.ghoul.client.ui.UIComponent;
import tokyo.ghoul.client.ui.clickgui.Panel;
import tokyo.ghoul.client.ui.clickgui.module.settings.ColorComponent;
import com.mojang.blaze3d.vertex.PoseStack;

import java.awt.Color;
import net.minecraft.client.Minecraft;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.List;

@Getter
public class ThemeEditor extends UIComponent {
    @Getter private static final ThemeEditor instance = new ThemeEditor();

    private final List<ThemeSelectable> themeSelectables = new ArrayList<>();
    private final List<ThemeBound> themeBounds = new ArrayList<>();

    private final Theme defaultTheme = new Theme("Desire", true);
    private Theme currentTheme = defaultTheme;
    protected Theme editTheme;

    private DeleteButton deleteButton = new DeleteButton(-1f, -1f, -1f);

    private final AnimationUtil openAnimation = new AnimationUtil();
    @Setter private boolean open;
    @Setter private float anim;

    private double scroll;
    private float scrollMax;
    private final AnimationUtil scrollAnimation = new AnimationUtil();

    private int alphaAnim() {
        return (int) (openAnimation.getValue() * anim * 255);
    }

    private record ThemeBound(float x, float y, float width, float height, Theme.ElementColor elementColor) { }
    private record DeleteButton(float x, float y, float size) {}

    public static final List<String> LOCKED_COLORS = List.of("Blur", "Widget blur", "Background blur");

    public void setCurrentTheme(Theme theme) {
        if (theme == null) return;
        this.currentTheme = theme;
        ThemeManager.getInstance().saveLastSelected(theme);
    }

    public ThemeEditor() {
        setWidth(scaled(95f));
        setHeight(scaled(150f));

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            setWidth(scaled(95f));
        }));
    }

    public void init() {
        refresh();
    }

    private void refresh() {
        ThemeManager.getInstance().refresh();
    }

    public void save(boolean last) {
        if (!last) {
            ThemeManager.getInstance().saveAll();
        } else if (currentTheme != null) {
            ThemeManager.getInstance().saveLastSelected(currentTheme);
        }
    }

    public void load() {
        ThemeManager.getInstance().refresh();
        Theme last = ThemeManager.getInstance().loadLastSelected();
        currentTheme = last != null ? last : defaultTheme;
        editTheme = currentTheme;
    }

    @Override
    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        openAnimation.update();
        openAnimation.run(open ? 1.0 : 0.0, 100, Easing.SINE_OUT);
        if (openAnimation.getValue() <= 0.1) return;

        themeBounds.clear();

        float round = getWidth() * 0.05f;
        float headerHeight = scaled(getHeaderHeight());
        float headerFontSize = headerHeight * 0.52f;
        String text = "Theme Editor";
        float textWidth = Fonts.PS_REGULAR.getWidth(text, headerFontSize);
        setWidth(textWidth * 1.5f);

        PoseStack matrixStack = RenderUtil.matrices();

        Vector4f allRound = new Vector4f(round, round, round, round);
        Vector4f topRound = new Vector4f(round, round, 0f, 0f);
        Color tintL = ColorUtil.interpolate(UIColors.secondary(), new Color(18, 20, 19), 0.22f);
        Color tintR = ColorUtil.interpolate(UIColors.primary(), new Color(22, 25, 23), 0.22f);
        RenderUtil.BLUR_RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), allRound,
                ColorUtil.setAlpha(tintL, (int) (232f * alphaAnim() / 255f)),
                ColorUtil.setAlpha(tintR, (int) (232f * alphaAnim() / 255f)),
                ColorUtil.setAlpha(tintL, (int) (232f * alphaAnim() / 255f)),
                ColorUtil.setAlpha(tintR, (int) (232f * alphaAnim() / 255f)), 0.18f);
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), getHeight(), allRound,
                new Color(4, 6, 5, (int) (180f * alphaAnim() / 255f)));
        RenderUtil.RECT.draw(matrixStack, getX(), getY(), getWidth(), headerHeight, topRound,
                new Color(3, 5, 4, (int) (190f * alphaAnim() / 255f)));
        Fonts.PS_REGULAR.drawText(matrixStack, text, getX() + getWidth() / 2f - textWidth / 2f,
                getY() + headerHeight / 2f - headerFontSize / 2f, headerFontSize,
                ColorUtil.setAlpha(Color.WHITE, alphaAnim()));

        placeRender(context, mouseX, mouseY, delta);

        scrollAnimation.update();
        scrollAnimation.run(scroll, 600, Easing.EXPO_OUT);
        float scrollOffset = (float) scrollAnimation.getValue();
        float screenBottom = Minecraft.getInstance().getWindow().getGuiScaledHeight() - scaled(8f);

        float nameRowH = editTheme != null ? scaled(getTypingFieldHeight()) + gap() : 0f;
        float listY = gap() + headerHeight + nameRowH;

        ScissorUtil.start(matrixStack, getX(), getY() + headerHeight, getWidth(), getHeight() - headerHeight);

        float contentHeight = 0f;
        if (editTheme == null) {
            float themeY = listY;
            for (ThemeSelectable theme : themeSelectables) {
                theme.setAlpha((float) (openAnimation.getValue() * anim));
                theme.setX(getX() + offset());
                theme.setY(getY() + themeY + scrollOffset);
                theme.setWidth(getWidth() - offset() * 2f);
                theme.setHeight(scaled(17f));

                theme.render(context, mouseX, mouseY, delta);

                themeY += theme.getHeight() + gap();
                contentHeight += theme.getHeight() + gap();
            }
        } else {
            float elementY = listY + scrollOffset;
            float elementHeight = scaled(getElementHeight());

            float textSize = elementHeight * 0.6f;
            float textY = getY() + elementHeight / 2f - textSize / 2f;

            float elementWidth = getWidth() - offset();

            float colorSize = elementHeight * 0.8f;
            float colorX = getX() + elementWidth - colorSize;
            float colorY = getY() + elementHeight / 2f - colorSize / 2f;

            float roundColor = colorSize * 0.2f;

            for (Theme.ElementColor elementColor : editTheme.getElementColors()) {
                if (LOCKED_COLORS.contains(elementColor.getName())) continue;

                float height = elementHeight;
                Fonts.PS_REGULAR.drawText(matrixStack, elementColor.getName(), getX() + offset(), textY + elementY, textSize, UIColors.textColor(alphaAnim()));
                RenderUtil.RECT.draw(matrixStack, colorX, colorY + elementY, colorSize, colorSize, roundColor, ColorUtil.setAlpha(elementColor.getColor(), alphaAnim()));

                ColorComponent colorComponent = elementColor.getColorComponent();
                colorComponent.updateOpen();
                float cAnim = colorComponent.getValue();

                if (cAnim > 0.0) {
                    colorComponent.setX(getX() + offset());
                    colorComponent.setY(textY + elementY + elementHeight);
                    colorComponent.setWidth(elementWidth);
                    colorComponent.setAlpha(alphaAnim() / 255f);
                    colorComponent.render(context, mouseX, mouseY, delta);

                    height += colorComponent.getHeight() + (gap() * 2f) * cAnim;
                }

                themeBounds.add(new ThemeBound(getX() + offset(), colorY + elementY, elementWidth, height, elementColor));

                elementY += height + gap();
                contentHeight += height + gap();
            }
        }

        float available = screenBottom - (getY() + listY) - gap();
        float visibleBody = Math.max(0f, Math.min(contentHeight, available));
        setHeight(listY + visibleBody + gap());
        scrollMax = Math.max(0f, contentHeight - visibleBody);

        ScissorUtil.stop(matrixStack);
    }

    private void placeRender(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (editTheme == null) {
            deleteButton = new DeleteButton(-1f, -1f, -1f);
            return;
        }

        PoseStack matrixStack = RenderUtil.matrices();

        float placeX = getX() + offset();
        float placeY = getY() + scaled(getHeaderHeight());
        float placeWidth = getWidth() - offset() * 2f;
        float placeHeight = scaled(getTypingFieldHeight());
        float placeFontSize = placeHeight * 0.4f;
        float placeRound = placeHeight * 0.2f;

        RenderUtil.BLUR_RECT.draw(matrixStack, placeX, placeY, placeWidth, placeHeight, placeRound, UIColors.widgetBlur(alphaAnim()));
        Fonts.PS_REGULAR.drawText(matrixStack, editTheme.getName(), placeX + offset(), placeY + placeHeight / 2f - placeFontSize / 2f, placeFontSize, UIColors.textColor(alphaAnim()));

        float margin = gap();
        float deleteSize = placeHeight - margin * 2f;
        float deleteX = placeX + placeWidth - deleteSize - margin;
        float deleteY = placeY + margin;
        float iconSize = deleteSize * 0.5f;
        RenderUtil.BLUR_RECT.draw(matrixStack, deleteX, deleteY, deleteSize, deleteSize, placeRound, UIColors.blur(alphaAnim()));
        Fonts.ICONS.drawCenteredText(matrixStack, Icons.CROSS.getLetter(), deleteX + deleteSize / 2f, deleteY + deleteSize / 2f - iconSize / 2f, iconSize, UIColors.textColor(alphaAnim()), 0.1f);

        deleteButton = new DeleteButton(deleteX, deleteY, deleteSize);
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!open) return;

        if (editTheme != null) {
            if (MouseUtil.isHovered(mouseX, mouseY, deleteButton.x(), deleteButton.y(), deleteButton.size(), deleteButton.size())) {
                if (themeSelectables.removeIf(ts -> ts.getTheme() == editTheme)) {
                    ThemeManager.getInstance().remove(editTheme.getName());
                    editTheme = null;
                }

                return;
            }

            if (MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), scaled(getHeaderHeight()))) {
                editTheme = null;
                return;
            }

            for (ThemeBound themeBound : themeBounds) {
                ColorComponent colorComponent = themeBound.elementColor().getColorComponent();
                if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x(), themeBound.y(), themeBound.width(), scaled(getElementHeight()))) {
                    colorComponent.toggleOpen();
                    return;
                }
                if (MouseUtil.isHovered(mouseX, mouseY, themeBound.x(), themeBound.y(), themeBound.width(), themeBound.height())) {
                    colorComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        } else {
            for (ThemeSelectable theme : themeSelectables) {
                if (MouseUtil.isHovered(mouseX, mouseY, theme.getX(), theme.getY(), theme.getWidth(), theme.getHeight())) {
                    if (button == 1 && !theme.getTheme().isPreset()) {
                        editTheme = theme.getTheme();
                    } else if (button == 0) {
                        currentTheme = theme.getTheme();
                        ThemeManager.getInstance().saveLastSelected(currentTheme);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (!open) return;

        if (editTheme != null) {
            for (ThemeBound themeBound : themeBounds) {
                themeBound.elementColor().getColorComponent().mouseReleased(mouseX, mouseY, button);
            }
        }
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    private float getElementHeight() {
        return 12f;
    }
    private float getHeaderHeight() {
        return 19f;
    }
    private float getTypingFieldHeight() {
        return 19f;
    }

    @Override
    public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (!open) return;
        if (!MouseUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), getHeight())) return;

        scroll += verticalAmount * scaled(45f);
        scroll = Math.min(Math.max(scroll, -scrollMax), 0f);
    }
}
