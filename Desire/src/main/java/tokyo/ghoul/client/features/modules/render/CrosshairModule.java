package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.ColorSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.utils.render.pipeline.EvaPipelines;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.api.utils.render.pipeline.EvaUniform;

import java.awt.Color;

@ModuleRegister(name = "Crosshair", description = "Кастомный прицел", category = Category.RENDER)
public class CrosshairModule extends Module {
    @Getter private static final CrosshairModule instance = new CrosshairModule();

    private final SliderSetting length = new SliderSetting("Длина").value(4f).range(1f, 5f).step(0.1f);
    private final SliderSetting gap = new SliderSetting("Пробел").value(1f).range(0f, 5f).step(0.1f);
    private final SliderSetting thickness = new SliderSetting("Толщина").value(1f).range(1f, 5f).step(0.1f);
    private final BooleanSetting dynamic = new BooleanSetting("Динамический").value(true);
    private final SliderSetting spread = new SliderSetting("Сила расширения").value(10f).range(5f, 15f).step(1f)
            .setVisible(dynamic::getValue);
    private final BooleanSetting hoverColor = new BooleanSetting("Менять цвет при наведении").value(false);
    private final ColorSetting color = new ColorSetting("Цвет").value(Color.WHITE);
    private final ColorSetting hoveredColor = new ColorSetting("Цвет при наведении").value(new Color(255, 0, 0))
            .setVisible(hoverColor::getValue);

    private float hoverAnim;
    private long lastFrame = System.nanoTime();

    public CrosshairModule() {
        addSettings(length, gap, thickness, dynamic, spread, hoverColor, color, hoveredColor);
    }

    @Override
    public void onEvent() {
        EventListener render = Render2DEvent.getInstance().subscribe(new Listener<>(this::onRender));
        addEvents(render);
    }

    private void onRender(Render2DEvent.Render2DEventData event) {
        if (mc.player == null || mc.level == null || mc.getWindow() == null) return;
        if (mc.gui.hud.isHidden() || !mc.options.getCameraType().isFirstPerson()) return;
        if (mc.gui.screen() != null && !(mc.gui.screen() instanceof ChatScreen)) return;

        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;
        boolean hovering = hoverColor.getValue() && mc.crosshairPickEntity instanceof LivingEntity;
        hoverAnim += ((hovering ? 1f : 0f) - hoverAnim) * (1f - (float) Math.exp(-10f * dt));

        double guiScale = mc.getWindow().getGuiScale();
        int scale = Math.max(2, (int) Math.round(guiScale));
        int gapPx = Math.round(gap.getValue() * scale);
        int lengthPx = Math.max(1, Math.round(length.getValue() * scale));
        int thickPx = Math.max(1, Math.round(thickness.getValue() * scale));
        if (dynamic.getValue()) {
            float strength = mc.player.getAttackStrengthScale(event.partialTicks());
            gapPx += Math.round((1f - strength) * spread.getValue());
        }

        int cx = mc.getWindow().getScreenWidth() / 2;
        int cy = mc.getWindow().getScreenHeight() / 2;
        int half = thickPx / 2;
        Color fill = mix(color.getValue(), hoveredColor.getValue(), hoverAnim);

        int[][] bars = {
                {cx - gapPx - lengthPx, cy - half, lengthPx, thickPx},
                {cx + gapPx + (thickPx % 2), cy - half, lengthPx, thickPx},
                {cx - half, cy - gapPx - lengthPx, thickPx, lengthPx},
                {cx - half, cy + gapPx + (thickPx % 2), thickPx, lengthPx},
        };

        PoseStack stack = event.matrixStack();
        Matrix4f matrix = stack.last().pose();
        float toGui = (float) (1.0 / guiScale);
        EvaRenderer renderer = EvaRenderer.getInstance();
        BufferBuilder buffer = renderer.begin(EvaPipelines.FLAT_COLOR);
        int outline = new Color(0, 0, 0, fill.getAlpha()).getRGB();
        for (int[] bar : bars) quad(buffer, matrix, bar[0] - 1, bar[1] - 1, bar[2] + 2, bar[3] + 2, toGui, outline);
        for (int[] bar : bars) quad(buffer, matrix, bar[0], bar[1], bar[2], bar[3], toGui, fill.getRGB());
        renderer.submit(buffer, EvaPipelines.FLAT_COLOR, EvaUniform.EMPTY);
    }

    private static void quad(BufferBuilder buffer, Matrix4f matrix, int x, int y, int w, int h, float toGui, int argb) {
        float x0 = x * toGui, y0 = y * toGui, x1 = (x + w) * toGui, y1 = (y + h) * toGui;
        buffer.addVertex(matrix, x0, y0, 0f).setColor(argb);
        buffer.addVertex(matrix, x0, y1, 0f).setColor(argb);
        buffer.addVertex(matrix, x1, y1, 0f).setColor(argb);
        buffer.addVertex(matrix, x1, y0, 0f).setColor(argb);
    }

    private static Color mix(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t),
                Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * t));
    }
}
