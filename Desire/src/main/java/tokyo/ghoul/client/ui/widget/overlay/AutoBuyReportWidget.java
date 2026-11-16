package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.AutoBuyReport;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

/**
 * Плашка отчёта AutoBuy / AutoSell: потрачено, заработано и чистый профит.
 */
public class AutoBuyReportWidget extends Widget {
    private static final String TITLE = "AutoBuy";
    private static final Color LABEL = new Color(200, 205, 215);
    private static final Color PROFIT_UP = new Color(100, 255, 120);
    private static final Color PROFIT_DOWN = new Color(255, 100, 100);

    public AutoBuyReportWidget() {
        super(10f, 31f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return "AutoBuyReport"; }

    @Override
    public void render(PoseStack ms) {
        float x = getDraggable().getX(), y = getDraggable().getY();

        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        long spent = cfg != null ? cfg.totalSpent : 0L;
        long earned = cfg != null ? cfg.totalEarned : 0L;
        long profit = earned - spent;

        String spentVal = AutoBuyReport.formatMoney(spent);
        String earnedVal = AutoBuyReport.formatMoney(earned);
        String profitVal = (profit > 0 ? "+" : "") + AutoBuyReport.formatMoney(profit);
        String[] labels = {"Потрачено:", "Заработано:", "Профит:"};
        String[] values = {spentVal, earnedVal, profitVal};

        var font = Fonts.HUD_MEDIUM;
        float titleSize = ul(42f), rowSize = ul(38f);
        float pad = ul(42f), headerH = ul(110f), rowH = ul(80f), corner = ul(28f), stroke = ul(4f);

        float maxRow = width(Fonts.HUD_BOLD, TITLE, titleSize);
        for (int i = 0; i < labels.length; i++) {
            maxRow = Math.max(maxRow, width(font, labels[i], rowSize) + ul(48f) + width(font, values[i], rowSize));
        }
        float w = Math.max(ul(560f), maxRow + pad * 2f);
        float h = headerH + rowH * labels.length + ul(18f);

        headerPanel(ms, x, y, w, h, headerH, corner, stroke, 1f, liquidGlass());
        text(ms, Fonts.HUD_BOLD, TITLE, x + pad, y + headerH / 2f, titleSize, ACCENT, 1f);

        Color profitColor = profit > 0 ? PROFIT_UP : profit < 0 ? PROFIT_DOWN : TEXT;
        for (int i = 0; i < labels.length; i++) {
            float cy = y + headerH + ul(9f) + rowH * i + rowH / 2f;
            text(ms, font, labels[i], x + pad, cy, rowSize, LABEL, 1f);
            float vw = width(font, values[i], rowSize);
            text(ms, font, values[i], x + w - pad - vw, cy, rowSize, i == 2 ? profitColor : ROW_TEXT, 1f);
        }

        getDraggable().setWidth(w);
        getDraggable().setHeight(h);
        renderHudSettingsMenu(ms);
    }
}
