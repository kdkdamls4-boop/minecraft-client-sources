package tokyo.ghoul.client.ui.radial;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.events.client.MouseLookEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Радиальное меню без экрана: курсор двигается мышью (MouseLookEvent), камера в это время стоит. */
public final class RadialMenu implements QuickImports {
    private static final float RADIUS = 92.0F;
    private static final float RING_RADIUS = 34.0F;
    private static final float RING_WIDTH = 5.0F;
    private static final float DEAD_ZONE = 30.0F;
    private static final float GAP = 2.5F;
    private static final double SENSITIVITY = 0.67;
    private static final List<RadialMenu> INSTANCES = new ArrayList<>();

    private static final Color BACKGROUND = new Color(18, 19, 26);
    private static final Color SURFACE = new Color(12, 13, 18);
    private static final Color TEXT = new Color(236, 236, 240);
    private static final Color SECONDARY_TEXT = new Color(150, 150, 160);

    private final AnimationUtil openAnimation = new AnimationUtil();
    private final List<AnimationUtil> slotAnimations = new ArrayList<>();
    private final List<Boolean> slotTargets = new ArrayList<>();
    private final AnimationUtil centerAnimation = new AnimationUtil();
    private final AnimationUtil labelAnimation = new AnimationUtil();
    private String currentLabel = "";
    private boolean opened;
    private boolean centerTarget;
    private double cursorX;
    private double cursorY;
    private boolean plusIcon;
    private Supplier<String> defaultLabel = () -> "";
    private Supplier<String> hint;

    public RadialMenu() {
        INSTANCES.add(this);
    }

    public static boolean isAnyOpen() {
        for (RadialMenu menu : INSTANCES) {
            if (menu.opened) return true;
        }
        return false;
    }

    public RadialMenu withPlusIcon() {
        this.plusIcon = true;
        return this;
    }

    public RadialMenu withDefaultLabel(Supplier<String> supplier) {
        this.defaultLabel = supplier;
        return this;
    }

    public RadialMenu withHint(Supplier<String> supplier) {
        this.hint = supplier;
        return this;
    }

    public void open() {
        if (opened) return;
        opened = true;
        cursorX = 0.0;
        cursorY = 0.0;
        for (AnimationUtil animation : slotAnimations) {
            force(animation, 0.0);
        }
        slotTargets.replaceAll(ignored -> false);
        force(centerAnimation, 0.0);
        centerTarget = false;
        force(labelAnimation, 0.0);
        currentLabel = "";
        openAnimation.run(1.0, 300, Easing.EXPO_OUT);
    }

    public void close() {
        if (opened) {
            opened = false;
            openAnimation.run(0.0, 160, Easing.QUAD_IN);
        }
    }

    public void closeImmediately() {
        opened = false;
        force(openAnimation, 0.0);
    }

    public boolean isOpen() {
        return opened;
    }

    public boolean isVisible() {
        openAnimation.update();
        return opened || openAnimation.getValue() > 0.01;
    }

    public boolean onMouseMove(MouseLookEvent.MouseLookEventData event) {
        if (!opened) return false;

        cursorX += event.getDeltaX() * SENSITIVITY;
        cursorY += event.getDeltaY() * SENSITIVITY;
        double distance = getCursorDistance();
        double max = RADIUS - 6.0;
        if (distance > max) {
            double factor = max / distance;
            cursorX *= factor;
            cursorY *= factor;
        }

        MouseLookEvent.getInstance().setCancel(true);
        return true;
    }

    public int getSelectedIndex(int count) {
        if (count <= 0 || isInDeadZone()) return -1;

        double angle = Math.toDegrees(Math.atan2(cursorY, cursorX)) + 90.0;
        double wrapped = angle % 360.0;
        if (wrapped < 0.0) wrapped += 360.0;

        double step = 360.0 / count;
        return (int) Math.round(wrapped / step) % count;
    }

    public boolean isInDeadZone() {
        return getCursorDistance() <= DEAD_ZONE;
    }

    private double getCursorDistance() {
        return Math.hypot(cursorX, cursorY);
    }

    public void render(GuiGraphicsExtractor context, PoseStack ms, List<Entry> entries) {
        openAnimation.update();
        float open = (float) openAnimation.getValue();
        if (open <= 0.01F) return;

        float cx = mc.getWindow().getGuiScaledWidth() / 2.0F;
        float cy = mc.getWindow().getGuiScaledHeight() / 2.0F;
        int count = entries.size();
        int selected = getSelectedIndex(count);
        updateAnimations(count, selected);

        renderRing(ms, cx, cy, count, open);
        renderItems(context, cx, cy, entries, open);
        renderCenter(ms, cx, cy, open);
        renderLabel(ms, cx, cy, entries, selected, open);
    }

    private void updateAnimations(int count, int selected) {
        while (slotAnimations.size() < count) {
            slotAnimations.add(new AnimationUtil());
            slotTargets.add(false);
        }

        for (int i = 0; i < slotAnimations.size(); i++) {
            AnimationUtil animation = slotAnimations.get(i);
            boolean target = i == selected;
            if (slotTargets.get(i) != target) {
                slotTargets.set(i, target);
                animation.run(target ? 1.0 : 0.0, target ? 220 : 160, target ? Easing.BACK_OUT : Easing.QUAD_IN);
            }
            animation.update();
        }

        boolean dead = isInDeadZone();
        if (centerTarget != dead) {
            centerTarget = dead;
            centerAnimation.run(dead ? 1.0 : 0.0, 180, Easing.QUAD_OUT);
        }
        centerAnimation.update();
    }

    private float getSlotProgress(int index) {
        return index >= 0 && index < slotAnimations.size() ? (float) slotAnimations.get(index).getValue() : 0.0F;
    }

    private void renderRing(PoseStack ms, float cx, float cy, int count, float open) {
        Color background = alpha(BACKGROUND, 0.55F * open);
        Color primary = alpha(UIColors.primary(), 0.85F * open);
        Matrix4f matrix = ms.last().pose();
        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder buffer = renderer.begin(EvaPipelines.FLAT_COLOR);

        if (count <= 1) {
            float progress = count == 1 ? Math.min(getSlotProgress(0), 1.0F) : 0.0F;
            float radius = RING_RADIUS + 4.0F * progress;
            drawArc(buffer, matrix, cx, cy, radius, 0.0F, 360.0F, lerp(background, primary, progress));
        } else {
            float step = 360.0F / count;
            for (int i = 0; i < count; i++) {
                float progress = Math.min(getSlotProgress(i), 1.0F);
                float start = i * step - step / 2.0F + GAP / 2.0F;
                float sweep = step - GAP;
                drawArc(buffer, matrix, cx, cy, RING_RADIUS + 4.0F * progress, start, sweep, lerp(background, primary, progress));
            }
        }

        renderer.submit(buffer, EvaPipelines.FLAT_COLOR, EvaUniform.EMPTY);
    }

    // угол 0 — вверх, по часовой
    private void drawArc(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float radius, float startDeg, float sweepDeg, Color color) {
        float inner = radius - RING_WIDTH / 2.0F;
        float outer = radius + RING_WIDTH / 2.0F;
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();
        int steps = Math.max(8, (int) (72 * sweepDeg / 360.0F));
        double start = Math.toRadians(startDeg - 90.0);
        double step = Math.toRadians(sweepDeg) / steps;

        for (int i = 0; i < steps; i++) {
            double a0 = start + step * i;
            double a1 = start + step * (i + 1);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

            buffer.addVertex(matrix, cx + c0 * inner, cy + s0 * inner, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c0 * outer, cy + s0 * outer, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c1 * outer, cy + s1 * outer, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c1 * inner, cy + s1 * inner, 0f).setColor(r, g, b, a);
        }
    }

    private void renderItems(GuiGraphicsExtractor context, float cx, float cy, List<Entry> entries, float open) {
        int count = entries.size();
        if (count == 0 || open < 0.35F) return;

        double step = 360.0 / count;
        for (int i = 0; i < count; i++) {
            Entry entry = entries.get(i);
            float progress = getSlotProgress(i);
            double angle = Math.toRadians(-90.0 + i * step);
            float distance = 75.0F + 1.5F * Math.min(progress, 1.0F);
            float x = cx + (float) Math.cos(angle) * distance;
            float y = cy + (float) Math.sin(angle) * distance;
            float scale = 1.0F + 0.18F * progress;

            context.pose().pushMatrix();
            context.pose().translate(x, y);
            context.pose().scale(scale, scale);
            context.item(entry.stack(), -8, -8);
            context.pose().popMatrix();
        }
    }

    private void renderCenter(PoseStack ms, float cx, float cy, float open) {
        if (!plusIcon) return;

        float hover = (float) centerAnimation.getValue();
        float radius = 15.0F + 1.5F * hover;
        if (hover > 0.01F) {
            RenderUtil.RECT.drawOutline(ms, cx - radius - 1.5F, cy - radius - 1.5F, radius * 2.0F + 3.0F, radius * 2.0F + 3.0F,
                    radius + 1.5F, 1.5F, alpha(UIColors.primary(), 0.5F * hover * open));
        }
        RenderUtil.RECT.draw(ms, cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, alpha(SURFACE, 0.8F * open));

        Color icon = alpha(lerp(SECONDARY_TEXT, UIColors.primary(), hover), open);
        RenderUtil.RECT.draw(ms, cx - 6.0F, cy - 1.0F, 12.0F, 2.0F, 1.0F, icon);
        RenderUtil.RECT.draw(ms, cx - 1.0F, cy - 6.0F, 2.0F, 12.0F, 1.0F, icon);
    }

    private void renderLabel(PoseStack ms, float cx, float cy, List<Entry> entries, int selected, float open) {
        String label = selected >= 0 ? entries.get(selected).label() : defaultLabel.get();
        if (!label.equals(currentLabel)) {
            currentLabel = label;
            force(labelAnimation, 0.0);
            labelAnimation.run(1.0, 200, Easing.QUAD_OUT);
        }
        labelAnimation.update();

        float progress = (float) labelAnimation.getValue();
        float size = 9.0F;
        float width = Fonts.HUD_BOLD.getWidth(label, size);
        Fonts.HUD_BOLD.drawText(ms, label, cx - width / 2.0F, cy - RADIUS - 16.0F + (1.0F - progress) * 4.0F, size,
                alpha(TEXT, open * (0.25F + 0.75F * progress)));

        if (hint != null) {
            String text = hint.get();
            float hintSize = 6.5F;
            float hintWidth = Fonts.HUD_REGULAR.getWidth(text, hintSize);
            Fonts.HUD_REGULAR.drawText(ms, text, cx - hintWidth / 2.0F, cy + RADIUS + 6.0F, hintSize,
                    alpha(SECONDARY_TEXT, 0.85F * open));
        }
    }

    private static void force(AnimationUtil animation, double value) {
        animation.setValue(value);
        animation.run(value, 1, Easing.LINEAR);
    }

    private static Color alpha(Color color, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(color.getAlpha() * factor)));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a);
    }

    private static Color lerp(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }

    public record Entry(ItemStack stack, String label) {
    }
}
