package tokyo.ghoul.client.features.modules.render.nametags;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;

import java.awt.Color;

/**
 * Предмет в пиксельных координатах экрана: сам предмет, число над ним и полоска прочности внизу.
 * Размеры и цвета как в оригинальном рендере предметов над ником.
 */
public final class NameTagItemRenderer {
    private static final float COUNT_SIZE = 11.0F;
    private static final float COUNT_LIFT = 11.0F;
    private static final float BAR_LIFT = 1.5F;
    private static final float BAR_HEIGHT = 3.0F;
    // vanilla-предмет не умеет прозрачность, поэтому при затухании плашки он пропадает раньше остального
    private static final float MIN_ITEM_ALPHA = 0.5F;

    private NameTagItemRenderer() {
    }

    /**
     * @param ms матрица, уже отмасштабированная в пиксели экрана (1 / guiScale)
     */
    public static void draw(GuiGraphicsExtractor extractor, PoseStack ms, ItemStack stack,
                            float x, float y, float size, float alpha) {
        if (stack == null || stack.isEmpty() || size <= 0.0F || alpha <= 0.001F) return;
        float guiScale = (float) Minecraft.getInstance().getWindow().getGuiScale();

        if (alpha >= MIN_ITEM_ALPHA) {
            extractor.pose().pushMatrix();
            extractor.pose().translate(x / guiScale, y / guiScale);
            extractor.pose().scale(size / guiScale / 16.0F);
            extractor.item(stack, 0, 0);
            extractor.pose().popMatrix();
        }

        if (stack.getCount() > 1) {
            Font font = Fonts.DIMASIK_MEDIUM;
            String count = String.valueOf(stack.getCount());
            float width = font.getWidth(count, COUNT_SIZE);
            font.drawText(ms, count, x + size / 2.0F - width / 2.0F, y - COUNT_LIFT, COUNT_SIZE, withAlpha(Color.WHITE, alpha));
        }

        if (stack.isDamageableItem() && stack.isDamaged()) {
            float fraction = (float) (stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
            int filled = Math.round(size * fraction);
            Color bar = new Color(
                    Math.clamp((int) (255.0F * (1.0F - fraction)), 0, 255),
                    Math.clamp((int) (255.0F * fraction), 0, 255),
                    0,
                    Math.clamp((int) (255.0F * alpha), 0, 255));
            RenderUtil.RECT.draw(ms, x, y + size - BAR_LIFT, size, BAR_HEIGHT, 0.0F, withAlpha(Color.BLACK, alpha));
            RenderUtil.RECT.draw(ms, x, y + size - BAR_LIFT, filled, BAR_HEIGHT, 0.0F, bar);
        }
    }

    private static Color withAlpha(Color color, float alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(),
                Math.clamp(Math.round(color.getAlpha() * alpha), 0, 255));
    }
}
