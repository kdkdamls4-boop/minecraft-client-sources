package tokyo.ghoul.client.features.modules.combat.autoswap;

import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;
import tokyo.ghoul.client.features.modules.combat.AutoSwapModule;
import tokyo.ghoul.client.services.RenderService;

import java.awt.Color;

public class AutoSwapWheelScreen extends Screen implements QuickImports {
    private static final float OUTER_RADIUS = 96f;
    private static final float INNER_RADIUS = 50f;
    private static final float HOVER_GROW = 7f;
    private static final float SEGMENT_GAP = 3.5f;

    private static final Color GLASS_TINT = new Color(12, 13, 20, 165);
    private static final Color GLASS_STROKE = new Color(255, 255, 255, 22);
    private static final Color HUB_FILL = new Color(14, 15, 23, 215);
    private static final Color TEXT_MUTED = new Color(255, 255, 255, 110);

    private final AutoSwapModule autoSwap;
    private int hoveredSector = -1;
    private final float[] hoverAnim = new float[8];
    private long lastFrameNs = System.nanoTime();
    private float openAnim = 0f;

    private int removeFlashIndex = -1;
    private long removeFlashUntilMs = 0L;

    public AutoSwapWheelScreen(AutoSwapModule autoSwap) {
        super(Component.empty());
        this.autoSwap = autoSwap;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        autoSwap.onWheelClosed(this);
        mc.gui.setScreen(null);
    }

    public int getSelectedIndex() {
        return hoveredSector;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        int count = autoSwap.getWheelSlotCount();
        int hover = getHoverIndex((float) click.x(), (float) click.y(), width / 2f, height / 2f, count);
        if (hover >= 0 && hover < count) {
            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                autoSwap.clearWheelSlot(hover);
                removeFlashIndex = hover;
                removeFlashUntilMs = System.currentTimeMillis() + 250L;
                return true;
            }

            if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                ItemStack stack = autoSwap.getWheelSlotStack(hover);
                if (stack.isEmpty() || stack.is(Items.AIR)) {
                    autoSwap.startPicking(hover);
                    return true;
                }

                autoSwap.swapWheelNow(hover);
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);
            renderWheel(context, mouseX, mouseY);
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void renderWheel(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrameNs) / 1.0E9f, 0.1f);
        lastFrameNs = now;
        openAnim = approach(openAnim, 1f, 9f, dt);

        int count = autoSwap.getWheelSlotCount();
        float cx = width / 2f;
        float cy = height / 2f;
        hoveredSector = getHoverIndex(mouseX, mouseY, cx, cy, count);
        for (int i = 0; i < hoverAnim.length; i++) {
            hoverAnim[i] = approach(hoverAnim[i], i == hoveredSector ? 1f : 0f, 16f, dt);
        }

        float appear = easeOut(openAnim);
        Color accent = UIColors.primary();
        var ms = RenderUtil.matrices();

        EvaRenderer renderer = EvaRenderer.getInstance();
        renderer.beginUnder();
        try {
            float discR = OUTER_RADIUS + HOVER_GROW + 6f;
            RenderUtil.BLUR_RECT.draw(ms, cx - discR, cy - discR, discR * 2f, discR * 2f, discR, new Color(255, 255, 255, 255), 0f);
            RenderUtil.RECT.draw(ms, cx - discR, cy - discR, discR * 2f, discR * 2f, discR, alpha(GLASS_TINT, appear));
            RenderUtil.RECT.drawOutline(ms, cx - discR, cy - discR, discR * 2f, discR * 2f, discR, 1f, alpha(GLASS_STROKE, appear));

            Matrix4f matrix = ms.last().pose();
            BufferBuilder buffer = renderer.begin(EvaPipelines.FLAT_COLOR);
            for (int i = 0; i < count; i++) {
                float h = hoverAnim[i];
                boolean empty = autoSwap.getWheelSlotStack(i).isEmpty();
                boolean flash = i == removeFlashIndex && System.currentTimeMillis() <= removeFlashUntilMs;

                Color base = empty ? new Color(255, 255, 255, 10) : new Color(255, 255, 255, 22);
                Color fill = flash ? new Color(255, 70, 70, 150) : lerp(base, ColorUtil.setAlpha(accent, 135), h);

                float outer = OUTER_RADIUS * (0.85f + 0.15f * appear) + HOVER_GROW * h;
                float inner = INNER_RADIUS * (0.85f + 0.15f * appear);
                float[] span = sectorSpan(i, count, outer);
                drawRingSegment(buffer, matrix, cx, cy, inner, outer, span[0], span[1], alpha(fill, appear));

                if (h > 0.01f) {
                    drawRingSegment(buffer, matrix, cx, cy, outer - 2f, outer, span[0], span[1],
                            ColorUtil.setAlpha(accent, (int) (230 * h * appear)));
                }
            }
            renderer.submit(buffer, EvaPipelines.FLAT_COLOR, EvaUniform.EMPTY);

            float hubR = INNER_RADIUS - 6f;
            RenderUtil.RECT.draw(ms, cx - hubR, cy - hubR, hubR * 2f, hubR * 2f, hubR, alpha(HUB_FILL, appear));
            RenderUtil.RECT.drawOutline(ms, cx - hubR, cy - hubR, hubR * 2f, hubR * 2f, hubR, 1f,
                    ColorUtil.setAlpha(accent, (int) (70 * appear)));
            renderHubText(ms, cx, cy, count, accent, appear);

            for (int i = 0; i < count; i++) {
                float[] pos = iconPos(i, count, cx, cy);
                ItemStack stack = autoSwap.getWheelSlotStack(i);
                if (stack.isEmpty() || stack.is(Items.AIR)) {
                    Color plus = lerp(new Color(255, 255, 255, 90), accent, hoverAnim[i]);
                    Fonts.SF_SEMIBOLD.drawCenteredText(ms, "+", pos[0], pos[1] - 6.5f, 14f, alpha(plus, appear));
                } else {
                    int amount = countItem(i);
                    Color countColor = amount == 0 ? new Color(255, 90, 90, 200) : new Color(255, 255, 255, 170);
                    Fonts.SF_MEDIUM.drawCenteredText(ms, amount == 0 ? "нет" : "x" + amount,
                            pos[0], pos[1] + 9.5f, 7.5f, alpha(countColor, appear));
                }
            }
        } finally {
            renderer.endUnder();
        }

        for (int i = 0; i < count; i++) {
            ItemStack stack = autoSwap.getWheelSlotStack(i);
            if (stack.isEmpty() || stack.is(Items.AIR)) continue;
            float[] pos = iconPos(i, count, cx, cy);
            context.item(stack, Math.round(pos[0] - 8f), Math.round(pos[1] - 8f));
        }
    }

    private void renderHubText(com.mojang.blaze3d.vertex.PoseStack ms, float cx, float cy, int count, Color accent, float appear) {
        if (hoveredSector >= 0 && hoveredSector < count) {
            ItemStack stack = autoSwap.getWheelSlotStack(hoveredSector);
            if (!stack.isEmpty() && !stack.is(Items.AIR)) {
                Fonts.SF_SEMIBOLD.drawCenteredText(ms, trim(stack.getHoverName().getString(), 72f, 9f),
                        cx, cy - 8f, 9f, alpha(Color.WHITE, appear));
                Fonts.SF_MEDIUM.drawCenteredText(ms, "x" + countItem(hoveredSector),
                        cx, cy + 3f, 8f, ColorUtil.setAlpha(accent, (int) (230 * appear)));
            } else {
                Fonts.SF_MEDIUM.drawCenteredText(ms, "Пусто", cx, cy - 4f, 9f, alpha(TEXT_MUTED, appear));
            }
            return;
        }

        ItemStack offhand = mc.player != null ? mc.player.getOffhandItem() : ItemStack.EMPTY;
        Fonts.SF_SEMIBOLD.drawCenteredText(ms, "AutoSwap", cx, cy - 8f, 10f, alpha(Color.WHITE, appear));
        String hand = offhand.isEmpty() ? "рука пуста" : trim(offhand.getHoverName().getString(), 72f, 7.5f);
        Fonts.SF_REGULAR.drawCenteredText(ms, hand, cx, cy + 4f, 7.5f, alpha(TEXT_MUTED, appear));
    }

    private int countItem(int index) {
        ItemStack target = autoSwap.getWheelSlotStack(index);
        if (mc.player == null || target.isEmpty()) return 0;
        int total = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, target)) total += stack.getCount();
        }
        return total;
    }

    private float[] sectorSpan(int i, int count, float radius) {
        double step = Math.PI * 2d / count;
        double start = -Math.PI / 2d - step / 2d + step * i;
        double gap = (SEGMENT_GAP / 2f) / radius;
        return new float[]{(float) (start + gap), (float) (start + step - gap)};
    }

    private float[] iconPos(int i, int count, float cx, float cy) {
        double step = Math.PI * 2d / count;
        double mid = -Math.PI / 2d + step * i;
        float r = (INNER_RADIUS + OUTER_RADIUS) / 2f + HOVER_GROW * 0.5f * hoverAnim[i];
        return new float[]{cx + (float) Math.cos(mid) * r, cy + (float) Math.sin(mid) * r - 2f};
    }

    private int getHoverIndex(float mouseX, float mouseY, float cx, float cy, int count) {
        float dx = mouseX - cx;
        float dy = mouseY - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < INNER_RADIUS - 6f || dist > OUTER_RADIUS + HOVER_GROW + 30f) return -1;

        double step = Math.PI * 2d / count;
        double ang = Math.atan2(dy, dx) + Math.PI / 2d + step / 2d;
        ang = ((ang % (Math.PI * 2d)) + Math.PI * 2d) % (Math.PI * 2d);
        int idx = (int) Math.floor(ang / step);
        return idx >= 0 && idx < count ? idx : -1;
    }

    private void drawRingSegment(BufferBuilder buffer, Matrix4f matrix, float cx, float cy, float innerR, float outerR,
                                 float start, float end, Color color) {
        int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();
        int steps = Math.max(12, (int) (64 * (Math.abs(end - start) / (Math.PI * 2f))));
        float step = (end - start) / steps;

        for (int i = 0; i < steps; i++) {
            float a0 = start + step * i;
            float a1 = start + step * (i + 1);
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0);
            float c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);

            buffer.addVertex(matrix, cx + c0 * innerR, cy + s0 * innerR, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c0 * outerR, cy + s0 * outerR, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c1 * outerR, cy + s1 * outerR, 0f).setColor(r, g, b, a);
            buffer.addVertex(matrix, cx + c1 * innerR, cy + s1 * innerR, 0f).setColor(r, g, b, a);
        }
    }

    private static String trim(String text, float maxWidth, float size) {
        if (Fonts.SF_SEMIBOLD.getWidth(text, size) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && Fonts.SF_SEMIBOLD.getWidth(cut + "…", size) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private static float approach(float value, float target, float speed, float dt) {
        return value + (target - value) * (1f - (float) Math.exp(-speed * dt));
    }

    private static float easeOut(float t) {
        float inv = 1f - Math.min(1f, Math.max(0f, t));
        return 1f - inv * inv * inv;
    }

    private static Color alpha(Color color, float factor) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * factor));
    }

    private static Color lerp(Color from, Color to, float t) {
        t = Math.min(1f, Math.max(0f, t));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }
}
