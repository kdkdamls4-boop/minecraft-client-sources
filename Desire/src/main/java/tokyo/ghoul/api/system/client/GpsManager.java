package tokyo.ghoul.api.system.client;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3i;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.client.KeyEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.ProjectionUtil;
import tokyo.ghoul.api.utils.render.KawaseBlurProgram;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.rotation.RotationUtil;
import tokyo.ghoul.api.utils.rotation.manager.Rotation;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;

import java.awt.Color;
import java.util.Locale;

@Getter
@Setter
public class GpsManager implements QuickImports {
    @Getter private static final GpsManager instance = new GpsManager();

    private static final String[] MENU_OPTIONS = {"Удалить", "К метке"};
    private static final Identifier[] MENU_ICONS = {HudIcons.WM_CROSS, HudIcons.WM_PIN};

    private Vector3i gpsPosition = null;
    private Vector3i lastGpsPosition = null;
    private String gpsName = "GPS";
    private String lastGpsName = "GPS";

    private final AnimationUtil distanceAnimation = new AnimationUtil();
    private final AnimationUtil switchAnimation = new AnimationUtil();
    private final AnimationUtil menuAnimation = new AnimationUtil();

    private boolean inputReady;
    private boolean markerVisible;
    private float boxX, boxY, boxW, boxH;
    private boolean menuOpen;
    private Screen menuScreen;
    private float menuX, menuY, menuW, menuRowH;
    private boolean lookAtMarker;

    public void update(GuiGraphicsExtractor context) {
        if (!inputReady) {
            inputReady = true;
            KeyEvent.getInstance().subscribe(new Listener<>(event -> {
                if (event.mouse() && event.action() == 1) onMousePress(event.key());
            }));
        }

        if (gpsPosition != null) {
            lastGpsPosition = gpsPosition;
            lastGpsName = gpsName;
        }

        markerVisible = false;
        if (context == null || mc.player == null) return;

        boolean noGps = gpsPosition == null;
        boolean noLastGps = lastGpsPosition == null;
        float distance = !noLastGps ? getDistance(lastGpsPosition) : 0f;
        float maxDistance = 10f;
        float minDistance = 3f;

        distanceAnimation.update();
        switchAnimation.update();
        menuAnimation.update();

        distanceAnimation.run(distance <= minDistance ? 0.0 : distance >= maxDistance ? 1.0 : (distance - minDistance) / (maxDistance - minDistance), 500, Easing.EXPO_OUT);
        switchAnimation.run(noGps ? 0.0 : 1.0, 500, Easing.EXPO_OUT);

        if (noGps) {
            lookAtMarker = false;
            closeMenu();
        }
        if (lookAtMarker && !noLastGps) turnToMarker();

        if (noLastGps || distanceAnimation.getValue() < 0.1 || switchAnimation.getValue() < 0.1) return;

        float alpha = (float) (distanceAnimation.getValue() * switchAnimation.getValue());
        int a255 = (int) (255 * alpha);

        float screenW = mc.getWindow().getGuiScaledWidth();
        float screenH = mc.getWindow().getGuiScaledHeight();

        Vec3 worldPos = markerWorldPos();
        Vector2f screen = ProjectionUtil.projectToScreen(worldPos);
        if (screen == null) {
            Vector2f fallback = ProjectionUtil.project(worldPos);
            if (fallback != null && fallback.x != Float.MAX_VALUE) screen = fallback;
        }
        if (screen == null || screen.x < 0 || screen.x > screenW || screen.y < 0 || screen.y > screenH) {
            closeMenu();
            return;
        }

        String distText = String.format(Locale.US, "%.0fm", distance);
        String coordText = lastGpsPosition.x + " " + lastGpsPosition.y + " " + lastGpsPosition.z;

        Font font = Fonts.SF_SEMIBOLD;
        float textSize = 7f;
        float pinW = 5.5f;
        float pinH = 7.5f;
        float padX = 4.5f;
        float gap = 3f;
        float radius = 3.2f;

        float coordWidth = font.getWidth(coordText, textSize);
        float distWidth = font.getWidth(distText, textSize);
        boxW = padX + pinW + gap + coordWidth + gap + distWidth + padX;
        boxH = 12.5f;
        boxX = screen.x - boxW / 2f;
        boxY = screen.y - boxH / 2f;
        markerVisible = true;

        Color accent = UIColors.gradient(0);
        PoseStack ms = RenderUtil.matrices();

        if (!KawaseBlurProgram.fbos.isEmpty()) {
            RenderUtil.BLUR_RECT.draw(ms, boxX, boxY, boxW, boxH, radius, new Color(255, 255, 255, a255), 0f);
        }
        RenderUtil.RECT.draw(ms, boxX, boxY, boxW, boxH, radius, new Color(11, 12, 22, (int) (204 * alpha)));

        float cx = boxX + padX;
        HudIcons.draw(ms, cx, boxY + (boxH - pinH) / 2f, pinW, pinH, HudIcons.WM_PIN, new Color(235, 238, 250, a255));
        cx += pinW + gap;

        float textY = boxY + (boxH - font.getHeight(textSize)) / 2f;
        font.drawText(ms, coordText, cx, textY, textSize, new Color(255, 255, 255, a255));
        cx += coordWidth + gap;
        font.drawText(ms, distText, cx, textY, textSize, ColorUtil.setAlpha(accent, a255));

        renderMenu(ms);
    }

    private Vec3 markerWorldPos() {
        return new Vec3(lastGpsPosition.x + 0.5, lastGpsPosition.y + 1.0, lastGpsPosition.z + 0.5);
    }

    private boolean overMarker(float mx, float my) {
        return markerVisible && mx >= boxX && mx <= boxX + boxW && my >= boxY && my <= boxY + boxH;
    }

    private void onMousePress(int button) {
        if (mc.player == null || !(mc.gui.screen() instanceof ChatScreen)) return;
        float mx = (float) (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
        float my = (float) (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());

        if (overMarker(mx, my)) {
            if (button == 1 && !menuOpen) {
                menuScreen = mc.gui.screen();
                menuOpen = true;
                menuAnimation.run(1.0, 150, Easing.EXPO_OUT);
            } else {
                closeMenu();
            }
            return;
        }
        if (!menuOpen) return;

        int option = menuOptionAt(mx, my);
        closeMenu();
        if (option == 0) {
            gpsPosition = null;
            print("Метка успешно удалена.");
        } else if (option == 1) {
            lookAtMarker = true;
        }
    }

    private void closeMenu() {
        if (!menuOpen) return;
        menuOpen = false;
        menuAnimation.run(0.0, 150, Easing.EXPO_OUT);
    }

    private int menuOptionAt(float mx, float my) {
        if (mx < menuX || mx > menuX + menuW || my < menuY || my > menuY + menuRowH * MENU_OPTIONS.length) return -1;
        return (int) ((my - menuY) / menuRowH);
    }

    private void renderMenu(PoseStack ms) {
        if (menuOpen && mc.gui.screen() != menuScreen) closeMenu();

        float textS = 7f;
        float pad = 5f;
        float iconS = 6.5f;
        float iconGap = 4f;
        float corner = 2f;

        float optW = 0f;
        for (String option : MENU_OPTIONS) {
            optW = Math.max(optW, Fonts.SF_MEDIUM.getWidth(option, textS));
        }
        menuW = pad + iconS + iconGap + optW + pad + 4f;
        menuRowH = 12f;
        menuX = boxX;
        menuY = boxY + boxH + 2.5f;

        float anim = (float) menuAnimation.getValue();
        if (anim <= 0.01f) return;

        float top = menuY - (1f - anim) * 4f;
        float menuH = menuRowH * MENU_OPTIONS.length;

        RelicStyle.menuPanel(ms, menuX, top, menuW, menuH, corner, anim);

        float mx = (float) (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
        float my = (float) (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            float rowY = top + i * menuRowH;
            boolean hovered = mx >= menuX && mx <= menuX + menuW && my >= rowY && my < rowY + menuRowH;
            if (hovered) {
                RelicStyle.menuHover(ms, menuX + 1.5f, rowY + 1.5f, menuW - 3f, menuRowH - 3f, 1.5f, anim);
            }
            float cy = rowY + menuRowH / 2f;
            RelicStyle.icon(ms, MENU_ICONS[i], menuX + pad, cy, iconS, iconS,
                    hovered ? RelicStyle.ACCENT : RelicStyle.ACCENT_DIM, anim);
            RelicStyle.text(ms, Fonts.SF_MEDIUM, MENU_OPTIONS[i], menuX + pad + iconS + iconGap, cy, textS,
                    RelicStyle.ROW_TEXT, hovered ? anim : anim * 0.8f);
        }
    }

    private void turnToMarker() {
        Rotation target = RotationUtil.rotationAt(markerWorldPos());
        float dYaw = Mth.wrapDegrees(target.getYRot() - mc.player.getYRot());
        float dPitch = target.getXRot() - mc.player.getXRot();
        if (Math.abs(dYaw) < 0.5f && Math.abs(dPitch) < 0.5f) {
            lookAtMarker = false;
            return;
        }
        mc.player.setYRot(mc.player.getYRot() + dYaw * 0.2f);
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + dPitch * 0.2f, -90f, 90f));
    }

    private float getDistance(Vector3i targetVec) {
        double dx = mc.player.position().x - targetVec.x;
        double dy = mc.player.position().y - targetVec.y;
        double dz = mc.player.position().z - targetVec.z;
        return Mth.sqrt((float) (dx * dx + dy * dy + dz * dz));
    }
}
