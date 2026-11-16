package tokyo.ghoul.client.features.modules.render;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.system.files.FileUtil;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@ModuleRegister(name = "Damage Tint", description = "Подсветка при получении урона", category = Category.RENDER)
public class DamageTintModule extends Module {
    @Getter private static final DamageTintModule instance = new DamageTintModule();

    private static final Color HEAL = new Color(85, 255, 80);
    private static final Color DAMAGE = new Color(255, 220, 50);
    private static final Identifier GLOW = FileUtil.getImage("interface/glow");
    private static final int MAX_AGE = 36;
    private static final float FONT_SCALE = 2.2F;

    private final List<Tint> tints = new ArrayList<>();
    private final Random random = new Random();
    private float lastTotal = -1.0F;

    @Override
    public void onEnable() {
        lastTotal = -1.0F;
        tints.clear();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        tints.clear();
        lastTotal = -1.0F;
        super.onDisable();
    }

    @Override
    public void onEvent() {
        EventListener tick = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onTick()));
        EventListener render = Render2DEvent.getInstance().subscribe(new Listener<>(this::onRender));
        addEvents(tick, render);
    }

    private void onTick() {
        if (mc.player == null || mc.level == null) {
            lastTotal = -1.0F;
            tints.clear();
            return;
        }

        float total = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        if (lastTotal < 0.0F) {
            lastTotal = total;
            tickTints();
            return;
        }

        float delta = total - lastTotal;
        if (Math.abs(delta) >= 0.05F) {
            spawn(Math.abs(delta), delta > 0.0F);
        }
        lastTotal = total;
        tickTints();
    }

    private void spawn(float amount, boolean heal) {
        double x = mc.player.getX() + (random.nextDouble() - 0.5) * 1.1;
        double y = mc.player.getY() + mc.player.getBbHeight() * (0.25 + random.nextDouble() * 0.7);
        double z = mc.player.getZ() + (random.nextDouble() - 0.5) * 1.1;
        tints.add(new Tint(
                x, y, z,
                (random.nextDouble() - 0.5) * 0.045,
                0.018 + random.nextDouble() * 0.025,
                (random.nextDouble() - 0.5) * 0.045,
                amount, heal
        ));
    }

    private void tickTints() {
        Iterator<Tint> iterator = tints.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().tick()) iterator.remove();
        }
    }

    private void onRender(Render2DEvent.Render2DEventData event) {
        if (tints.isEmpty() || mc.font == null) return;
        float partial = event.partialTicks();
        PoseStack ms = event.matrixStack();
        GuiGraphicsExtractor graphics = event.context();
        Font font = mc.font;

        for (Tint tint : tints) {
            Vector2f point = ProjectionUtil.project(tint.x(partial), tint.y(partial), tint.z(partial));
            if (point.x == Float.MAX_VALUE || point.y == Float.MAX_VALUE) continue;

            float life = 1.0F - tint.age / (float) tint.maxAge;
            float fade = life < 0.25F ? life / 0.25F : 1.0F;
            int alpha = Mth.clamp((int) (255 * fade), 0, 255);
            if (alpha <= 8) continue;

            String text = format(tint.amount);
            Color color = tint.heal ? HEAL : DAMAGE;
            float width = font.width(text) * FONT_SCALE;
            float height = font.lineHeight * FONT_SCALE;
            float x = point.x - width / 2.0F;
            float y = point.y - height / 2.0F;

            drawGlow(ms, x, y, width, height, color, fade);
            int packed = (alpha << 24) | (color.getRGB() & 0x00FFFFFF);
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(FONT_SCALE, FONT_SCALE);
            graphics.text(font, text, 0, 0, packed, false);
            graphics.pose().popMatrix();
        }
    }

    private static void drawGlow(PoseStack ms, float x, float y, float width, float height, Color color, float fade) {
        float cx = x + width / 2.0F;
        float cy = y + height / 2.0F;
        glowLayer(ms, cx, cy, width * 4.2F, height * 4.0F, color, 0.55F * fade);
        glowLayer(ms, cx, cy, width * 2.6F, height * 2.4F, color, 0.85F * fade);
        glowLayer(ms, cx, cy, width * 1.6F, height * 1.5F, Color.WHITE, 0.35F * fade);
    }

    private static void glowLayer(PoseStack ms, float cx, float cy, float w, float h, Color color, float alpha) {
        RenderUtil.TEXTURE_RECT.draw(ms, cx - w / 2.0F, cy - h / 2.0F, w, h, 0.01F,
                ColorUtil.setAlpha(color, Mth.clamp((int) (255 * alpha), 0, 255)), 0f, 0f, 1f, 1f, GLOW);
    }

    private static String format(float amount) {
        float rounded = Math.round(amount * 10.0F) / 10.0F;
        if (Math.abs(rounded - Math.round(rounded)) < 0.05F) {
            return String.valueOf(Math.round(rounded));
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    private static final class Tint {
        double x, y, z, prevX, prevY, prevZ, vx, vy, vz;
        final float amount;
        final boolean heal;
        int age;
        final int maxAge = MAX_AGE;

        Tint(double x, double y, double z, double vx, double vy, double vz, float amount, boolean heal) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.amount = amount;
            this.heal = heal;
        }

        boolean tick() {
            prevX = x;
            prevY = y;
            prevZ = z;
            x += vx;
            y += vy;
            z += vz;
            vx *= 0.96;
            vz *= 0.96;
            vy *= 0.985;
            age++;
            return age >= maxAge;
        }

        double x(float partial) { return Mth.lerp(partial, prevX, x); }
        double y(float partial) { return Mth.lerp(partial, prevY, y); }
        double z(float partial) { return Mth.lerp(partial, prevZ, z); }
    }
}
