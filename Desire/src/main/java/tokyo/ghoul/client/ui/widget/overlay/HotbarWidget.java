package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;

public class HotbarWidget extends Widget {
    @Getter private static HotbarWidget instance;
    private static final AnimationUtil slotAnim = new AnimationUtil();
    private static int lastSlot = -1;

    public HotbarWidget() {
        super(-1f, -1f);
        instance = this;
        setEnabled(true);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() {
        return "Hotbar";
    }

    public static boolean isHotbarEnabled() {
        return instance != null && instance.isEnabled() && InterfaceModule.getInstance().isEnabled()
                && InterfaceModule.getInstance().widgets.isEnabled("Hotbar");
    }

    public static void renderHotbarUnder(PoseStack ms) {
        if (!isHotbarEnabled()) return;
        Player player = mc.player;
        if (player == null || player.isSpectator() || (mc.gui != null && mc.gui.hud != null && mc.gui.hud.isHidden())) return;

        var screen = mc.gui.screen();
        if (screen != null && !(screen instanceof ChatScreen)) return;

        float midX = mc.getWindow().getGuiScaledWidth() / 2f;
        float screenHeight = (float) mc.getWindow().getGuiScaledHeight();
        float barW = 182f;
        float barH = 22f;
        float barX = midX - 91f;
        float barY = screenHeight - barH - 1f;
        float corner = 4.0f;
        Color bg = PANEL_BODY;

        int selectedSlot = player.getInventory().getSelectedSlot();
        if (lastSlot == -1) {
            slotAnim.setValue(selectedSlot);
            lastSlot = selectedSlot;
        }
        slotAnim.update();
        if (selectedSlot != lastSlot) {
            slotAnim.run(selectedSlot, 120, Easing.EXPO_OUT);
            lastSlot = selectedSlot;
        }
        float currentSlot = (float) slotAnim.getValue();

        EvaRenderer.getInstance().beginUnder();

        if (instance.liquidGlass()) {
            RenderUtil.BLUR_RECT.draw(ms, barX, barY, barW, barH, corner, new Color(255, 255, 255, 255), 0f);
            RenderUtil.RECT.draw(ms, barX, barY, barW, barH, corner, new Color(14, 14, 16, 150));
        } else {
            RenderUtil.BLUR_RECT.draw(ms, barX, barY, barW, barH, corner, new Color(255, 255, 255, 255), 0f);
            RenderUtil.RECT.draw(ms, barX, barY, barW, barH, corner, bg);
        }

        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            boolean leftHand = player.getMainArm().getOpposite() == HumanoidArm.LEFT;
            float offW = 24f;
            float offH = barH;
            float offX = leftHand ? (midX - 91f - 30f) : (midX + 91f + 6f);
            float offY = barY;

            if (instance.liquidGlass()) {
                RenderUtil.BLUR_RECT.draw(ms, offX, offY, offW, offH, corner, new Color(255, 255, 255, 255), 0f);
                RenderUtil.RECT.draw(ms, offX, offY, offW, offH, corner, new Color(14, 14, 16, 150));
            } else {
                RenderUtil.BLUR_RECT.draw(ms, offX, offY, offW, offH, corner, new Color(255, 255, 255, 255), 0f);
                RenderUtil.RECT.draw(ms, offX, offY, offW, offH, corner, bg);
            }
        }

        float selW = 20f;
        float selH = 20f;
        float selX = midX - 90f + currentSlot * 20f;
        float selY = barY + 1f;
        float selCorner = 3f;
        Color selFill = ColorUtil.setAlpha(UIColors.primary(), 180);
        RenderUtil.RECT.draw(ms, selX, selY, selW, selH, selCorner, selFill);

        for (int s = 0; s < 9; s++) {
            if (s != selectedSlot && player.getInventory().getItem(s).isEmpty()) {
                String num = String.valueOf(s + 1);
                float numSize = 7.5f;
                Fonts.SF_BOLD.drawCenteredText(ms, num, midX - 80f + s * 20f, barY + (barH - numSize) / 2f + 0.5f, numSize, new Color(255, 255, 255, 120));
            }
        }

        EvaRenderer.getInstance().endUnder();
    }

    @Override
    public void render(PoseStack ms) {
        if (mc.player == null) return;
        float midX = mc.getWindow().getGuiScaledWidth() / 2f;
        float barW = 182f;
        float barH = 22f;
        float barX = midX - 91f;
        float barY = mc.getWindow().getGuiScaledHeight() - barH - 1f;

        getDraggable().setX(barX);
        getDraggable().setY(barY);
        getDraggable().setWidth(barW);
        getDraggable().setHeight(barH);

        renderHudSettingsMenu(ms);
    }
}
