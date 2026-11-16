package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleManager;
import tokyo.ghoul.api.system.backend.KeyStorage;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.clickgui.ClickGuiTab;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class KeybindsWidget extends Widget {
    private static final String TITLE = "Hotkeys";

    private final AnimationUtil widthAnim = new AnimationUtil();
    private final AnimationUtil alphaAnim = new AnimationUtil();
    private final Map<Module, AnimationUtil> rowAnims = new HashMap<>();

    public KeybindsWidget() {
        super(3f, 120f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return TITLE; }

    private record Row(String name, String key, ClickGuiTab tab, float anim) {}

    @Override
    public void render(PoseStack ms) {
        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        List<Row> rows = updateRows();
        if (rows.isEmpty() && editing) rows = List.of(new Row("Example", "R", ClickGuiTab.RENDER, 1f));

        alphaAnim.update();
        alphaAnim.run(hasActiveBind() || editing ? 1f : 0f, 200, Easing.CUBIC_OUT);
        float alpha = (float) alphaAnim.getValue();
        if (alpha <= 0.01f) return;

        float posX = getDraggable().getX();
        float posY = getDraggable().getY();

        float corner = ul(36f), stroke = ul(4f);
        float headerH = ul(134f), rowH = ul(62f), topPad = ul(20f), bottomPad = ul(35f);
        float padX = ul(39f), nameOffset = ul(100f);
        float titleSize = ul(49f), textSize = ul(48f);
        float iconW = ul(46f), iconH = ul(48f);

        var nameFont = Fonts.HUD_MEDIUM;
        var keyFont = Fonts.HUD_BOLD;

        float headerW = ul(42f) + iconW + ul(39f) + ul(10f) + ul(36f) + width(nameFont, TITLE, titleSize) + ul(42f);
        float maxNameW = 0f, maxKeyW = 0f;
        for (Row row : rows) {
            maxNameW = Math.max(maxNameW, width(nameFont, row.name(), textSize));
            maxKeyW = Math.max(maxKeyW, width(keyFont, row.key(), textSize));
        }
        float bodyW = nameOffset + maxNameW + ul(100f) + maxKeyW + padX;

        widthAnim.update();
        widthAnim.run(Math.max(ul(554f), Math.max(headerW, bodyW)), 180, Easing.EXPO_OUT);
        float panelW = (float) widthAnim.getValue();

        float contentH = headerH + (rows.isEmpty() ? 0f : topPad + bottomPad) + rows.size() * rowH;

        headerPanel(ms, posX, posY, panelW, contentH, headerH, corner, stroke, alpha, liquidGlass());

        float headerCy = posY + headerH / 2f;
        float iconX = posX + ul(42f);
        icon(ms, HudIcons.WM_LAYERS, iconX, headerCy, iconW, iconH, ACCENT, alpha);
        float sepX = iconX + iconW + ul(39f);
        separator(ms, sepX, headerCy, ul(10f), ul(26f), alpha);
        text(ms, Fonts.HUD_SEMIBOLD, TITLE, sepX + ul(10f) + ul(36f), headerCy, titleSize, TEXT, alpha);

        float currentY = posY + headerH + topPad;
        for (Row row : rows) {
            float rowA = row.anim() * alpha;
            if (rowA <= 0.01f) continue;

            float rowCy = currentY + rowH / 2f + (row.anim() - 1f) * ul(12f);
            float textTop = rowCy - textSize / 2f + textSize * 0.09f;

            tabIcon(ms, row.tab(), posX + padX, textTop, textSize * 1.05f, rowA);
            text(ms, nameFont, row.name(), posX + nameOffset, rowCy, textSize, ROW_TEXT, rowA);

            float keyW = width(keyFont, row.key(), textSize);
            text(ms, keyFont, row.key(), posX + panelW - padX - keyW, rowCy, textSize, ACCENT, rowA);

            currentY += rowH * row.anim();
        }

        getDraggable().setWidth(panelW);
        getDraggable().setHeight(contentH);
        renderHudSettingsMenu(ms);
    }

    private List<Row> updateRows() {
        List<Row> rows = new ArrayList<>();
        for (Module module : ModuleManager.getInstance().getModules()) {
            boolean bound = module.isEnabled() && module.hasBind() && module.getBind() != -1;
            if (!bound && !rowAnims.containsKey(module)) continue;

            AnimationUtil anim = rowAnims.computeIfAbsent(module, key -> new AnimationUtil());
            anim.update();
            anim.run(bound ? 1f : 0f, 240, Easing.EXPO_OUT);

            float value = (float) anim.getValue();
            if (!bound && value < 0.01f) {
                rowAnims.remove(module);
                continue;
            }
            if (value > 0.01f) {
                rows.add(new Row(module.getName(), KeyStorage.getBind(module.getBind()),
                        ClickGuiTab.of(module.getCategory()), value));
            }
        }
        return rows;
    }

    private boolean hasActiveBind() {
        for (Module module : ModuleManager.getInstance().getModules()) {
            if (module.isEnabled() && module.hasBind() && module.getBind() != -1) return true;
        }
        return false;
    }
}
