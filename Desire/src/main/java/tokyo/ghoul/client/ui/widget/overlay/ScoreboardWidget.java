package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import tokyo.ghoul.api.system.draggable.Draggable;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.other.ReplaceUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;
import tokyo.ghoul.client.ui.widget.RelicStyle;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ScoreboardWidget extends Widget {
    private static final long REFRESH_NS = 100_000_000L;

    private final AnimationUtil widthAnim = new AnimationUtil();
    private final AnimationUtil alphaAnim = new AnimationUtil();
    private boolean showPoints = false;

    private record Seg(String text, Color color) {}

    private record Line(List<Seg> segs, String scoreText) {
        boolean spacer() {
            for (Seg seg : segs) if (!seg.text().isBlank()) return false;
            return true;
        }
    }

    public record ScoreRow(Component name, String scoreText) {}

    private List<Seg> cachedTitle = List.of();
    private List<Line> cachedLines = List.of();
    private boolean cachedEditing;
    private long lastRefresh;

    public ScoreboardWidget() {
        super(-1f, -1f);
        setEnabled(true);
    }

    @Override
    public String getName() {
        return "Scoreboard";
    }

    @Override
    public void render(PoseStack ms) {
        if (RemovalsModule.getInstance().isScoreboard()) return;

        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        long now = System.nanoTime();
        if (now - lastRefresh >= REFRESH_NS || editing != cachedEditing) {
            lastRefresh = now;
            cachedEditing = editing;
            refresh(editing);
        }

        alphaAnim.update();
        alphaAnim.run(!cachedLines.isEmpty() ? 1f : 0f, 250, Easing.EXPO_OUT);
        float alpha = (float) alphaAnim.getValue();
        if (alpha <= 0.01f) return;

        renderClassic(ms, alpha);
    }

    private void refresh(boolean editing) {
        Scoreboard scoreboard = mc.level != null ? mc.level.getScoreboard() : null;
        Objective objective = getSidebarObjective(scoreboard);

        Component title = null;
        List<ScoreRow> rows = List.of();
        if (objective != null && scoreboard != null) {
            title = objective.getDisplayName();
            rows = extractScores(scoreboard, objective);
        } else if (editing) {
            title = Component.literal("Scoreboard");
            rows = getPreviewRows();
        }

        cachedTitle = title != null ? flatten(title) : List.of();
        List<Line> lines = new ArrayList<>(rows.size());
        for (ScoreRow row : rows) lines.add(new Line(flatten(row.name()), row.scoreText()));
        cachedLines = lines;
    }

    private void renderClassic(PoseStack ms, float alpha) {
        float titleSize = su(49f);
        float textSize = su(40f);
        float padX = su(39f);
        float headerH = su(134f);
        float rowH = su(51f);
        float topPad = su(20f), bottomPad = su(35f);
        float corner = su(36f), stroke = su(4f);

        Font titleFont = Fonts.HUD_SEMIBOLD;
        Font textFont = Fonts.HUD_MEDIUM;

        float maxWidth = width(titleFont, cachedTitle, titleSize);
        for (Line line : cachedLines) {
            float nameW = width(textFont, line.segs(), textSize);
            if (showPoints && !line.scoreText().isEmpty() && !line.spacer()) {
                maxWidth = Math.max(maxWidth, nameW + su(100f) + RelicStyle.width(textFont, line.scoreText(), textSize));
            } else {
                maxWidth = Math.max(maxWidth, nameW);
            }
        }

        float targetWidth = Math.max(su(420f), maxWidth + padX * 2f);
        widthAnim.update();
        widthAnim.run(targetWidth, 200, Easing.EXPO_OUT);
        float width = (float) widthAnim.getValue();
        float height = headerH + (cachedLines.isEmpty() ? 0f : topPad + bottomPad) + cachedLines.size() * rowH;

        Draggable d = getDraggable();
        if (d.getX() == -1f || (d.getX() == d.initialXVal && !d.isDragging())) {
            float defX = mc.getWindow().getGuiScaledWidth() - width - scaled(6.5f);
            float defY = (mc.getWindow().getGuiScaledHeight() - height) / 2f;
            d.setX(defX);
            d.setY(defY);
        }
        float x = d.getX(), y = d.getY();

        RelicStyle.headerPanel(ms, x, y, width, height, headerH, corner, stroke, alpha, liquidGlass());

        float titleW = width(titleFont, cachedTitle, titleSize);
        float titleTop = y + headerH / 2f - titleSize / 2f + titleSize * 0.09f;
        draw(ms, titleFont, cachedTitle, x + (width - titleW) / 2f, titleTop, titleSize, alpha);

        float currentY = y + headerH + topPad;
        for (Line line : cachedLines) {
            if (!line.spacer()) {
                float textTop = currentY + rowH / 2f - textSize / 2f + textSize * 0.09f;
                draw(ms, textFont, line.segs(), x + padX, textTop, textSize, alpha);

                if (showPoints && !line.scoreText().isEmpty()) {
                    float scoreW = RelicStyle.width(textFont, line.scoreText(), textSize);
                    RelicStyle.textAt(ms, textFont, line.scoreText(), x + width - padX - scoreW, textTop, textSize,
                            RelicStyle.ACCENT, alpha);
                }
            }
            currentY += rowH;
        }

        d.setWidth(width);
        d.setHeight(height);
    }

    private static float su(float px) {
        return RelicStyle.u(px * 1.1f);
    }

    private static float width(Font font, List<Seg> segs, float size) {
        float total = 0f;
        for (Seg seg : segs) total += RelicStyle.width(font, seg.text(), size);
        return total;
    }

    private static void draw(PoseStack ms, Font font, List<Seg> segs, float x, float top, float size, float alpha) {
        float cursor = x;
        for (Seg seg : segs) {
            RelicStyle.textAt(ms, font, seg.text(), cursor, top, size, seg.color(), alpha);
            cursor += RelicStyle.width(font, seg.text(), size);
        }
    }

    private static List<Seg> flatten(Component component) {
        List<Seg> out = new ArrayList<>();
        component.visit((style, text) -> {
            if (!text.isEmpty()) {
                TextColor color = style.getColor();
                addLegacy(out, ReplaceUtil.replaceSymbols(text), color != null ? new Color(color.getValue()) : RelicStyle.TEXT);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private static void addLegacy(List<Seg> out, String text, Color base) {
        StringBuilder current = new StringBuilder();
        Color color = base;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!isCodeChar(c) || i + 1 >= text.length()) {
                current.append(c);
                continue;
            }

            char code = Character.toLowerCase(text.charAt(i + 1));
            Color next = color;
            int consumed = 1;
            if (code == 'x' && isHexCodes(text, i + 2)) {
                int rgb = 0;
                for (int k = 0; k < 6; k++) rgb = (rgb << 4) | Character.digit(text.charAt(i + 3 + k * 2), 16);
                next = new Color(rgb);
                consumed = 13;
            } else if (code == 'r') {
                next = base;
            } else {
                ChatFormatting format = ChatFormatting.getByCode(code);
                if (format != null) {
                    TextColor legacy = TextColor.fromLegacyFormat(format);
                    if (legacy != null) next = new Color(legacy.getValue());
                    else if (format == ChatFormatting.RESET) next = base;
                }
            }

            if (current.length() > 0 && !next.equals(color)) {
                out.add(new Seg(current.toString(), color));
                current.setLength(0);
            }
            color = next;
            i += consumed;
        }
        if (current.length() > 0) out.add(new Seg(current.toString(), color));
    }

    private static boolean isCodeChar(char c) {
        return c == '\u00A7' || c == '\u0E22' || c == '\u0E07';
    }

    private static boolean isHexCodes(String text, int from) {
        if (from + 12 > text.length()) return false;
        for (int k = 0; k < 6; k++) {
            int at = from + k * 2;
            if (text.charAt(at) != '§' || Character.digit(text.charAt(at + 1), 16) < 0) return false;
        }
        return true;
    }

    private Objective getSidebarObjective(Scoreboard scoreboard) {
        if (scoreboard == null || mc.player == null) return null;

        Objective objective = null;
        PlayerTeam team = scoreboard.getPlayersTeam(mc.player.getScoreboardName());
        if (team != null && team.getColor().isPresent()) {
            DisplaySlot slot = team.getColor().get().displaySlot();
            objective = scoreboard.getDisplayObjective(slot);
        }
        if (objective == null) {
            objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        }
        return objective;
    }

    private List<ScoreRow> extractScores(Scoreboard scoreboard, Objective objective) {
        NumberFormat numberFormat = objective.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);
        Collection<PlayerScoreEntry> playerScores = scoreboard.listPlayerScores(objective);

        return playerScores.stream()
                .filter(entry -> !entry.isHidden())
                .filter(entry -> entry.owner() != null && !entry.owner().startsWith("#"))
                .sorted(Comparator.comparing(PlayerScoreEntry::value).reversed()
                        .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER))
                .limit(15)
                .map(entry -> {
                    PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
                    Component name = PlayerTeam.formatNameForTeam(team, entry.ownerName());
                    Component scoreComp = entry.formatValue(numberFormat);
                    String scoreStr = (scoreComp != null && !scoreComp.getString().isEmpty())
                            ? scoreComp.getString() : "";
                    return new ScoreRow(name, scoreStr);
                })
                .toList();
    }

    private List<ScoreRow> getPreviewRows() {
        List<ScoreRow> preview = new ArrayList<>();
        String playerName = mc.player != null ? mc.player.getScoreboardName() : "Player";
        preview.add(new ScoreRow(Component.literal("Игрок: " + playerName), ""));
        preview.add(new ScoreRow(Component.literal("Убийств: 5"), ""));
        preview.add(new ScoreRow(Component.literal(""), ""));
        preview.add(new ScoreRow(Component.literal("Монет: 1250"), ""));
        preview.add(new ScoreRow(Component.literal("StaticBase Client"), ""));
        return preview;
    }
}
