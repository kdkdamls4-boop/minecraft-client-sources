package tokyo.ghoul.client.ui.clickgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.features.modules.render.ItemReplacerModule;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsGuiPreview;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;
import tokyo.ghoul.client.ui.clickgui.screen.GuiStyle;

import java.awt.Color;
import java.util.List;

public class ClickGuiCosmetics {
    private static final String[] TABS = {"Wings", "Body", "Pets", "Hats", "Masks", "Ears", "Tails", "Heads", "Paws", "Stockings", "Sword", "Models"};
    private static final String[] TAB_LABELS = {"Крылья", "Тело", "Питомцы", "Шляпы", "Маски", "Уши", "Хвосты", "Головы", "Лапки", "Чулки", "Мечи", "Модели"};
    private static final Identifier[] TAB_ICONS = {
            tabIcon("wings"), tabIcon("shirt"), tabIcon("paw"), tabIcon("hat"), tabIcon("mask"),
            tabIcon("ears"), tabIcon("tail"), tabIcon("animal"), tabIcon("hand"), tabIcon("sock"),
            tabIcon("sword"), tabIcon("person")
    };
    private static final int COLUMNS = 3;

    private int tab;
    private float switchAnim = 1f;
    private float selX = Float.NaN, selY, selW;
    private final float[] tabAnim = new float[TABS.length];
    private long lastFrame = System.nanoTime();
    private float scroll;
    private final AnimationUtil scrollAnimation = new AnimationUtil();
    private float lookX = Float.NaN;
    private float lookY = Float.NaN;
    private boolean draggingPreview;

    public void resetScroll() {
        scroll = 0f;
        scrollAnimation.setValue(0);
    }

    private static Identifier tabIcon(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/gui/cosmetics/" + name + ".svg");
    }

    private static float iconSize() {
        return ClickGuiLayout.scaled(10f);
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static float ease(float current, float target, float speed, float dt) {
        return current + (target - current) * (1f - (float) Math.exp(-speed * dt));
    }

    private float tileProgress(int i) {
        return smooth((switchAnim - Math.min(i, 12) * 0.045f) / 0.45f);
    }

    private float tileShift(int i) {
        return (1f - tileProgress(i)) * ClickGuiLayout.scaled(12f);
    }

    private void selectTab(int index) {
        if (tab == index) return;
        tab = index;
        switchAnim = 0f;
        resetScroll();
    }

    public void resetView(float previewX, float previewY, float previewW, float previewH) {
        lookX = previewX + previewW / 2f;
        lookY = previewY + previewH / 2f;
        draggingPreview = false;
        CosmeticsModule.getInstance().unequipAll();
    }

    private float[][] tabRects(Layout layout) {
        float[][] rects = new float[TABS.length][4];
        float tabX = layout.leftX;
        float tabRowY = layout.tabY;
        for (int i = 0; i < TABS.length; i++) {
            float textSize = ClickGuiLayout.scaled(10f);
            float textW = Fonts.SF_MEDIUM.getWidth(TAB_LABELS[i], textSize);
            float tabW = iconSize() + ClickGuiLayout.scaled(4f) + textW + ClickGuiLayout.scaled(16f);
            if (i > 0 && tabX + tabW > layout.leftX + layout.leftW + 0.5f) {
                tabX = layout.leftX;
                tabRowY += layout.tabH + layout.tabGap;
            }
            rects[i][0] = tabX;
            rects[i][1] = tabRowY;
            rects[i][2] = tabW;
            rects[i][3] = layout.tabH;
            tabX += tabW + layout.tabGap;
        }
        return rects;
    }

    private float tabBlockBottom(Layout layout) {
        float[][] rects = tabRects(layout);
        float bottom = layout.tabY + layout.tabH;
        for (float[] rect : rects) bottom = Math.max(bottom, rect[1] + rect[3]);
        return bottom;
    }

    public void renderUnder(float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        var matrices = RenderUtil.matrices();
        Layout layout = layout(windowX, windowY);
        CosmeticsModule cosmetics = CosmeticsModule.getInstance();

        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;
        switchAnim = Math.min(1.6f, switchAnim + dt);

        float[][] tabs = tabRects(layout);
        for (int i = 0; i < TABS.length; i++) {
            float tabX = tabs[i][0], tabY = tabs[i][1], tabW = tabs[i][2], tabH = tabs[i][3];
            boolean hovered = MouseUtil.isHovered(mouseX, mouseY, tabX, tabY, tabW, tabH);
            tabAnim[i] = ease(tabAnim[i], i == tab ? 1f : 0f, 14f, dt);
            Color fill = new Color(1, 3, 2, (int) ((hovered ? 48f : 30f) * alpha));
            RenderUtil.RECT.draw(matrices, tabX, tabY, tabW, tabH, ClickGuiLayout.scaled(6f), fill);
        }
        float[] target = tabs[tab];
        if (Float.isNaN(selX)) {
            selX = target[0];
            selY = target[1];
            selW = target[2];
        } else {
            selX = ease(selX, target[0], 16f, dt);
            selY = ease(selY, target[1], 16f, dt);
            selW = ease(selW, target[2], 16f, dt);
        }
        RenderUtil.RECT.draw(matrices, selX, selY, selW, target[3], ClickGuiLayout.scaled(6f),
                ColorUtil.setAlpha(UIColors.primary(), (int) (140f * alpha)));

        List<CosmeticsModule.Entry> items = CosmeticsModule.entriesForCategory(TABS[tab]);
        scrollAnimation.update();
        scrollAnimation.run(scroll, 220, Easing.EXPO_OUT);
        float offset = (float) scrollAnimation.getValue();

        ScissorUtil.start(matrices, layout.leftX, layout.gridY, layout.leftW, layout.gridH);
        for (int i = 0; i < items.size(); i++) {
            CosmeticsModule.Entry entry = items.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float x = layout.leftX + col * (layout.tileW + layout.tileGap);
            float y = layout.gridY + row * (layout.tileH + layout.tileGap) + offset + tileShift(i);
            if (y + layout.tileH < layout.gridY || y > layout.gridY + layout.gridH) continue;

            float tileAlpha = alpha * tileProgress(i);
            boolean equipped = cosmetics.isEquipped(entry);
            boolean hovered = MouseUtil.isHovered(mouseX, mouseY, x, y, layout.tileW, layout.tileH);
            Color bg = equipped
                    ? ColorUtil.setAlpha(UIColors.primary(), (int) (70f * tileAlpha))
                    : new Color(1, 3, 2, (int) ((hovered ? 48f : 34f) * tileAlpha));
            RenderUtil.RECT.draw(matrices, x, y, layout.tileW, layout.tileH, ClickGuiLayout.scaled(6f), bg);
            if (equipped) {
                RenderUtil.RECT.draw(matrices, x, y, layout.tileW, layout.tileH, ClickGuiLayout.scaled(6f),
                        ColorUtil.setAlpha(UIColors.primary(), (int) (36f * tileAlpha)));
            }
        }
        ScissorUtil.stop(matrices);

        float rows = Math.max(1, (items.size() + COLUMNS - 1) / (float) COLUMNS);
        float content = rows * layout.tileH + Math.max(0, rows - 1) * layout.tileGap;
        scroll = Mth.clamp(scroll, Math.min(layout.gridH - content, 0f), 0f);

        RenderUtil.RECT.draw(matrices, layout.previewX, layout.previewY, layout.previewW, layout.previewH,
                layout.round, new Color(1, 3, 2, (int) (40f * alpha)));

        boolean resetHover = MouseUtil.isHovered(mouseX, mouseY, layout.resetX, layout.resetY, layout.resetW, layout.resetH);
        RenderUtil.RECT.draw(matrices, layout.resetX, layout.resetY, layout.resetW, layout.resetH,
                ClickGuiLayout.scaled(6f), new Color(1, 3, 2, (int) ((resetHover ? 56f : 36f) * alpha)));
    }

    public void renderEntities(GuiGraphicsExtractor context, float windowX, float windowY) {
        if (context == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;

        Layout layout = layout(windowX, windowY);
        List<CosmeticsModule.Entry> items = CosmeticsModule.entriesForCategory(TABS[tab]);
        float offset = (float) scrollAnimation.getValue();

        CosmeticsGuiPreview.clearTiles();
        boolean swordTab = "Sword".equals(TABS[tab]);
        boolean modelsTab = "Models".equals(TABS[tab]);
        context.enableScissor(
                Math.round(layout.leftX),
                Math.round(layout.gridY),
                Math.round(layout.leftX + layout.leftW),
                Math.round(layout.gridY + layout.gridH));

        for (int i = 0; i < items.size(); i++) {
            CosmeticsModule.Entry entry = items.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float x = layout.leftX + col * (layout.tileW + layout.tileGap);
            float y = layout.gridY + row * (layout.tileH + layout.tileGap) + offset + tileShift(i);
            if (y + layout.tileH < layout.gridY || y > layout.gridY + layout.gridH) continue;
            float appear = tileProgress(i);
            if (appear < 0.2f) continue;
            float grow = 0.7f + 0.3f * appear;

            float modelPad = ClickGuiLayout.scaled(6f);
            float modelBottomPad = ClickGuiLayout.scaled(20f);
            float modelLeft = x + modelPad;
            float modelTop = y + modelPad;
            float modelRight = x + layout.tileW - modelPad;
            float modelBottom = y + layout.tileH - modelBottomPad;

            if (swordTab || CosmeticsModule.isSword(entry)) {
                drawSwordItem3D(context, entry, modelLeft, modelTop, modelRight, modelBottom);
                continue;
            }

            int modelScale = Math.max(14, Math.round((modelBottom - modelTop) * 0.52f * grow));
            float lookAtX = (modelLeft + modelRight) / 2f;
            float lookAtY = (modelTop + modelBottom) / 2f;

            if (modelsTab || CosmeticsModule.isModel(entry)) {
                modelScale = Math.max(12, Math.round((modelBottom - modelTop) * 0.36f * grow));
                CosmeticsGuiPreview.pendingModelPreview(entry.label());
                try {
                    InventoryScreen.extractEntityInInventoryFollowsMouse(
                            context,
                            Math.round(modelLeft), Math.round(modelTop),
                            Math.round(modelRight), Math.round(modelBottom),
                            modelScale, 0.0625f, lookAtX, lookAtY, player);
                } finally {
                    CosmeticsGuiPreview.clearPending();
                }
                continue;
            }

            int entryIndex = CosmeticsModule.indexOf(entry);
            if (entryIndex < 0) continue;

            CosmeticsGuiPreview.pendingTile(entryIndex);
            try {
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        context,
                        Math.round(modelLeft), Math.round(modelTop),
                        Math.round(modelRight), Math.round(modelBottom),
                        modelScale, 0.0625f, lookAtX, lookAtY, player);
            } finally {
                CosmeticsGuiPreview.clearPending();
            }
        }
        context.disableScissor();

        if (Float.isNaN(lookX) || Float.isNaN(lookY)) {
            lookX = (layout.modelLeft + layout.modelRight) / 2f;
            lookY = (layout.modelTop + layout.modelBottom) / 2f;
        }

        if (swordTab) {
            CosmeticsModule.Entry preview = equippedSwordOrFirst(items);
            if (preview != null) {
                drawSwordItem3D(context, preview, layout.modelLeft, layout.modelTop, layout.modelRight, layout.modelBottom);
            }
        } else {
            float boxH = Math.max(1f, layout.modelBottom - layout.modelTop);
            int scale = Math.max(18, Math.round(boxH * 0.30f));
            float lookAtX = draggingPreview ? lookX : (layout.modelLeft + layout.modelRight) / 2f;
            float lookAtY = draggingPreview ? lookY : (layout.modelTop + layout.modelBottom) / 2f;
            context.enableScissor(
                    Math.round(layout.modelLeft),
                    Math.round(layout.modelTop),
                    Math.round(layout.modelRight),
                    Math.round(layout.modelBottom));
            CosmeticsModule.Entry modelPreview = modelsTab ? equippedModelOrFirst(items) : null;
            if (modelPreview != null) {
                CosmeticsGuiPreview.pendingModelPreview(modelPreview.label());
            } else {
                CosmeticsGuiPreview.pendingPreview();
            }
            try {
                InventoryScreen.extractEntityInInventoryFollowsMouse(
                        context,
                        Math.round(layout.modelLeft), Math.round(layout.modelTop),
                        Math.round(layout.modelRight), Math.round(layout.modelBottom),
                        scale, 0.0625f, lookAtX, lookAtY, player);
            } finally {
                CosmeticsGuiPreview.clearPending();
                context.disableScissor();
            }
        }
    }

    private static CosmeticsModule.Entry equippedModelOrFirst(List<CosmeticsModule.Entry> items) {
        CosmeticsModule cosmetics = CosmeticsModule.getInstance();
        for (CosmeticsModule.Entry entry : items) {
            if (cosmetics.isEquipped(entry)) return entry;
        }
        return items.isEmpty() ? null : items.getFirst();
    }

    private static CosmeticsModule.Entry equippedSwordOrFirst(List<CosmeticsModule.Entry> items) {
        CosmeticsModule cosmetics = CosmeticsModule.getInstance();
        for (CosmeticsModule.Entry entry : items) {
            if (cosmetics.isEquipped(entry)) return entry;
        }
        return items.isEmpty() ? null : items.getFirst();
    }

    private static void drawSwordItem3D(GuiGraphicsExtractor context, CosmeticsModule.Entry entry,
                                        float left, float top, float right, float bottom) {
        var stack = ItemReplacerModule.getInstance().previewStack(entry.label());
        float areaW = right - left;
        float areaH = bottom - top;
        float size = Math.min(areaW, areaH) * 0.92f;
        float scale = size / 16f;
        float drawX = left + (areaW - 16f * scale) / 2f;
        float drawY = top + (areaH - 16f * scale) / 2f;
        context.pose().pushMatrix();
        context.pose().translate(drawX, drawY);
        context.pose().scale(scale, scale);
        context.item(stack, 0, 0);
        context.pose().popMatrix();
    }

    public void renderOver(float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        var matrices = RenderUtil.matrices();
        int full = (int) (alpha * 255f);
        Layout layout = layout(windowX, windowY);

        float titleSize = ClickGuiLayout.scaled(15f);
        Fonts.SF_BOLD.drawText(matrices, "Косметика", layout.leftX, layout.topY, titleSize, UIColors.textColor(full));
        float hintSize = ClickGuiLayout.scaled(9f);
        Fonts.SF_REGULAR.drawText(matrices, "Нажми, чтобы надеть. Ещё раз — снять.",
                layout.leftX, layout.topY + ClickGuiLayout.scaled(18f), hintSize,
                UIColors.inactiveTextColor((int) (0.75f * full)));

        float[][] tabs = tabRects(layout);
        for (int i = 0; i < TABS.length; i++) {
            float textSize = ClickGuiLayout.scaled(10f);
            float textW = Fonts.SF_MEDIUM.getWidth(TAB_LABELS[i], textSize);
            float tabX = tabs[i][0], tabY = tabs[i][1], tabW = tabs[i][2], tabH = tabs[i][3];
            Color color = GuiStyle.mix(UIColors.textColor(full), ColorUtil.setAlpha(Color.WHITE, full), tabAnim[i]);
            float icon = iconSize();
            float contentW = icon + ClickGuiLayout.scaled(4f) + textW;
            float startX = tabX + (tabW - contentW) / 2f;
            ClickGuiIcons.draw(startX, tabY + (tabH - icon) / 2f, icon, TAB_ICONS[i], color);
            Fonts.SF_MEDIUM.drawText(matrices, TAB_LABELS[i], startX + icon + ClickGuiLayout.scaled(4f),
                    tabY + (tabH - textSize) / 2f, textSize, color);
        }

        List<CosmeticsModule.Entry> items = CosmeticsModule.entriesForCategory(TABS[tab]);
        float offset = (float) scrollAnimation.getValue();
        ScissorUtil.start(matrices, layout.leftX, layout.gridY, layout.leftW, layout.gridH);
        for (int i = 0; i < items.size(); i++) {
            CosmeticsModule.Entry entry = items.get(i);
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float x = layout.leftX + col * (layout.tileW + layout.tileGap);
            float y = layout.gridY + row * (layout.tileH + layout.tileGap) + offset + tileShift(i);
            if (y + layout.tileH < layout.gridY || y > layout.gridY + layout.gridH) continue;

            float nameSize = ClickGuiLayout.scaled(9f);
            String name = truncate(entry.label(), layout.tileW - ClickGuiLayout.scaled(8f), nameSize);
            float nameW = Fonts.SF_MEDIUM.getWidth(name, nameSize);
            Fonts.SF_MEDIUM.drawText(matrices, name, x + (layout.tileW - nameW) / 2f,
                    y + layout.tileH - ClickGuiLayout.scaled(16f), nameSize,
                    UIColors.textColor((int) (full * tileProgress(i))));
        }
        ScissorUtil.stop(matrices);

        float previewTitle = ClickGuiLayout.scaled(12f);
        float titleW = Fonts.SF_MEDIUM.getWidth("Превью", previewTitle);
        Fonts.SF_MEDIUM.drawText(matrices, "Превью",
                layout.previewX + (layout.previewW - titleW) / 2f,
                layout.previewY + ClickGuiLayout.scaled(10f),
                previewTitle, UIColors.textColor(full));

        float resetText = ClickGuiLayout.scaled(10f);
        float resetTextW = Fonts.SF_MEDIUM.getWidth("Сброс вида", resetText);
        Fonts.SF_MEDIUM.drawText(matrices, "Сброс вида",
                layout.resetX + (layout.resetW - resetTextW) / 2f,
                layout.resetY + (layout.resetH - resetText) / 2f,
                resetText, UIColors.textColor(full));
    }

    public void renderLayered(GuiGraphicsExtractor context, float windowX, float windowY, float alpha, int mouseX, int mouseY) {
        renderUnder(windowX, windowY, alpha, mouseX, mouseY);
        EvaRenderer.getInstance().endUnder();
        renderEntities(context, windowX, windowY);
        renderOver(windowX, windowY, alpha, mouseX, mouseY);
    }

    public boolean mouseClicked(double mouseX, double mouseY, float windowX, float windowY) {
        CosmeticsModule cosmetics = CosmeticsModule.getInstance();
        Layout layout = layout(windowX, windowY);

        float[][] tabs = tabRects(layout);
        for (int i = 0; i < TABS.length; i++) {
            if (MouseUtil.isHovered(mouseX, mouseY, tabs[i][0], tabs[i][1], tabs[i][2], tabs[i][3])) {
                selectTab(i);
                return true;
            }
        }

        if (MouseUtil.isHovered(mouseX, mouseY, layout.resetX, layout.resetY, layout.resetW, layout.resetH)) {
            resetView(layout.modelLeft, layout.modelTop,
                    layout.modelRight - layout.modelLeft, layout.modelBottom - layout.modelTop);
            return true;
        }

        if (MouseUtil.isHovered(mouseX, mouseY, layout.modelLeft, layout.modelTop,
                layout.modelRight - layout.modelLeft, layout.modelBottom - layout.modelTop)) {
            draggingPreview = true;
            lookX = (float) mouseX;
            lookY = (float) mouseY;
            return true;
        }

        if (!MouseUtil.isHovered(mouseX, mouseY, layout.leftX, layout.gridY, layout.leftW, layout.gridH)) return false;

        List<CosmeticsModule.Entry> items = CosmeticsModule.entriesForCategory(TABS[tab]);
        float offset = scroll;
        for (int i = 0; i < items.size(); i++) {
            int col = i % COLUMNS;
            int row = i / COLUMNS;
            float x = layout.leftX + col * (layout.tileW + layout.tileGap);
            float y = layout.gridY + row * (layout.tileH + layout.tileGap) + offset;
            if (y + layout.tileH < layout.gridY || y > layout.gridY + layout.gridH) continue;
            if (MouseUtil.isHovered(mouseX, mouseY, x, y, layout.tileW, layout.tileH)) {
                cosmetics.toggleEquip(items.get(i));
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY) {
        if (!draggingPreview) return false;
        lookX = (float) mouseX;
        lookY = (float) mouseY;
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY) {
        if (!draggingPreview) return false;
        draggingPreview = false;
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float windowX, float windowY) {
        Layout layout = layout(windowX, windowY);
        if (!MouseUtil.isHovered(mouseX, mouseY, layout.leftX, layout.gridY, layout.leftW, layout.gridH)) return false;
        scroll += (float) (amount * ClickGuiLayout.scaled(18f));
        return true;
    }

    private Layout layout(float windowX, float windowY) {
        float contentX = ClickGuiLayout.contentX(windowX);
        float contentY = ClickGuiLayout.contentY(windowY);
        float contentW = ClickGuiLayout.contentWidth();
        float contentH = ClickGuiLayout.contentHeight();
        float pad = ClickGuiLayout.scaled(14f);
        float previewW = ClickGuiLayout.scaled(168f);
        float previewGap = ClickGuiLayout.scaled(12f);
        float leftW = contentW - previewW - pad * 2f - previewGap;
        float leftX = contentX + pad;
        float topY = contentY + ClickGuiLayout.scaled(8f);
        float tabY = topY + ClickGuiLayout.scaled(34f);
        float tabH = ClickGuiLayout.scaled(20f);
        float tabGap = ClickGuiLayout.scaled(6f);
        Layout probe = new Layout(leftX, topY, leftW, tabY, tabH, tabGap,
                tabY + tabH + ClickGuiLayout.scaled(10f), 1f, 1f, 1f, 1f,
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        float gridY = tabBlockBottom(probe) + ClickGuiLayout.scaled(10f);
        float gridH = contentY + contentH - gridY - ClickGuiLayout.scaled(12f);
        float tileGap = ClickGuiLayout.scaled(8f);
        float tileW = (leftW - tileGap * (COLUMNS - 1)) / COLUMNS;
        float tileH = tileW + ClickGuiLayout.scaled(18f);
        float previewX = contentX + contentW - pad - previewW;
        float previewY = topY;
        float previewH = contentH - ClickGuiLayout.scaled(16f);
        float resetH = ClickGuiLayout.scaled(24f);
        float resetY = previewY + previewH - resetH - ClickGuiLayout.scaled(10f);
        float resetX = previewX + ClickGuiLayout.scaled(12f);
        float resetW = previewW - ClickGuiLayout.scaled(24f);
        float modelTop = previewY + ClickGuiLayout.scaled(28f);
        float modelBottom = resetY - ClickGuiLayout.scaled(8f);
        float modelLeft = previewX + ClickGuiLayout.scaled(10f);
        float modelRight = previewX + previewW - ClickGuiLayout.scaled(10f);
        return new Layout(leftX, topY, leftW, tabY, tabH, tabGap, gridY, gridH, tileW, tileH, tileGap,
                previewX, previewY, previewW, previewH, ClickGuiLayout.scaled(12f),
                resetX, resetY, resetW, resetH, modelLeft, modelTop, modelRight, modelBottom);
    }

    private static String truncate(String text, float maxWidth, float size) {
        if (Fonts.SF_MEDIUM.getWidth(text, size) <= maxWidth) return text;
        String ellipsis = "...";
        float ellipsisW = Fonts.SF_MEDIUM.getWidth(ellipsis, size);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            builder.append(text.charAt(i));
            if (Fonts.SF_MEDIUM.getWidth(builder.toString(), size) + ellipsisW > maxWidth) {
                if (!builder.isEmpty()) builder.deleteCharAt(builder.length() - 1);
                return builder + ellipsis;
            }
        }
        return text;
    }

    private record Layout(
            float leftX, float topY, float leftW,
            float tabY, float tabH, float tabGap,
            float gridY, float gridH, float tileW, float tileH, float tileGap,
            float previewX, float previewY, float previewW, float previewH, float round,
            float resetX, float resetY, float resetW, float resetH,
            float modelLeft, float modelTop, float modelRight, float modelBottom
    ) {
    }
}
