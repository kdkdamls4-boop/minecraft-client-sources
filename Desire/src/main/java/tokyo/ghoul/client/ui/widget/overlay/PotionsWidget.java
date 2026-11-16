package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.Widget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static tokyo.ghoul.client.ui.widget.RelicStyle.*;

public class PotionsWidget extends Widget {
    private static final String TITLE = "Effects";

    private final AnimationUtil globalAlpha = new AnimationUtil();
    private final AnimationUtil widthAnim = new AnimationUtil();
    private final Map<String, AnimationUtil> animMap = new HashMap<>();
    private final Map<String, Integer> maxDurations = new HashMap<>();

    public PotionsWidget() {
        super(3f, 120f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return TITLE; }

    private record Row(Identifier icon, String name, String level, String duration, float anim) {}

    @Override
    public void render(PoseStack ms) {
        if (mc.player == null) return;

        List<MobEffectInstance> effects = new ArrayList<>(mc.player.getActiveEffectsMap().values());

        for (MobEffectInstance effect : effects) {
            int curDur = effect.getDuration();
            maxDurations.compute(key(effect), (k, old) -> (old == null || curDur > old) ? curDur : old);
            AnimationUtil anim = animMap.computeIfAbsent(key(effect), k -> new AnimationUtil());
            anim.update();
            anim.run(1f, 240, Easing.EXPO_OUT);
        }
        animMap.entrySet().removeIf(e -> {
            if (effects.stream().anyMatch(effect -> key(effect).equals(e.getKey()))) return false;
            AnimationUtil anim = e.getValue();
            anim.update();
            anim.run(0f, 200, Easing.EXPO_OUT);
            return anim.getValue() < 0.01;
        });
        maxDurations.entrySet().removeIf(e -> effects.stream().noneMatch(ef -> key(ef).equals(e.getKey())));

        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        globalAlpha.update();
        globalAlpha.run(!effects.isEmpty() || editing ? 1f : 0f, 300, Easing.EXPO_OUT);
        float alpha = (float) globalAlpha.getValue();
        if (alpha <= 0.01f && effects.isEmpty()) return;

        float posX = getDraggable().getX();
        float posY = getDraggable().getY();

        float corner = ul(36f), stroke = ul(4f);
        float headerH = ul(134f), rowH = ul(62f), topPad = ul(20f), bottomPad = ul(35f);
        float titleSize = ul(49f), textSize = ul(48f);
        float headerIconW = ul(48f), headerIconH = ul(48f);
        float itemSize = ul(56f);
        float iconX = ul(38f), textX = ul(110f), padRight = ul(41f);

        var nameFont = Fonts.HUD_MEDIUM;
        var valueFont = Fonts.HUD_SEMIBOLD;

        List<Row> rows = new ArrayList<>();
        float maxNameW = 0f, maxValueW = 0f;
        for (MobEffectInstance effect : effects) {
            float anim = (float) animMap.get(key(effect)).getValue();
            if (anim <= 0.01f) continue;

            String name = Language.getInstance().getOrDefault(effect.getDescriptionId());
            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String duration = formatDuration(effect.getDuration());
            rows.add(new Row(effectIcon(effect), name, level, duration, anim));

            maxNameW = Math.max(maxNameW, width(nameFont, name + level, textSize));
            maxValueW = Math.max(maxValueW, width(valueFont, duration, textSize));
        }

        if (rows.isEmpty() && editing) {
            rows.add(new Row(previewIcon("speed"), "Speed", " 2", "1:23", 1f));
            rows.add(new Row(previewIcon("haste"), "Haste", "", "2:56", 1f));
            for (Row row : rows) {
                maxNameW = Math.max(maxNameW, width(nameFont, row.name() + row.level(), textSize));
                maxValueW = Math.max(maxValueW, width(valueFont, row.duration(), textSize));
            }
        }

        float headerW = ul(42f) + headerIconW + ul(39f) + ul(10f) + ul(36f) + width(nameFont, TITLE, titleSize) + ul(42f);
        float bodyW = textX + maxNameW + ul(100f) + maxValueW + padRight;

        widthAnim.update();
        widthAnim.run(Math.max(ul(554f), Math.max(headerW, bodyW)), 180, Easing.EXPO_OUT);
        float panelW = (float) widthAnim.getValue();

        float contentH = headerH + (rows.isEmpty() ? 0f : topPad + bottomPad) + rows.size() * rowH;

        headerPanel(ms, posX, posY, panelW, contentH, headerH, corner, stroke, alpha, liquidGlass());

        float headerCy = posY + headerH / 2f;
        float hIconX = posX + ul(42f);
        icon(ms, HudIcons.WM_STAR, hIconX, headerCy, headerIconW, headerIconH, ACCENT, alpha);
        float sepX = hIconX + headerIconW + ul(39f);
        separator(ms, sepX, headerCy, ul(10f), ul(26f), alpha);
        text(ms, Fonts.HUD_SEMIBOLD, TITLE, sepX + ul(10f) + ul(36f), headerCy, titleSize, TEXT, alpha);

        float currentY = posY + headerH + topPad;
        for (Row row : rows) {
            float rowA = row.anim() * alpha;
            if (rowA <= 0.01f) continue;

            float rowCy = currentY + rowH / 2f + (row.anim() - 1f) * ul(12f);

            RenderUtil.TEXTURE_RECT.draw(ms, posX + iconX, rowCy - itemSize / 2f, itemSize, itemSize, 0f,
                    a(java.awt.Color.WHITE, rowA), 0f, 0f, 1f, 1f, row.icon());

            float nameX = posX + textX;
            text(ms, nameFont, row.name(), nameX, rowCy, textSize, ROW_TEXT, rowA);
            if (!row.level().isEmpty()) {
                text(ms, nameFont, row.level(), nameX + width(nameFont, row.name(), textSize), rowCy, textSize, ACCENT, rowA);
            }

            float valueW = width(valueFont, row.duration(), textSize);
            text(ms, valueFont, row.duration(), posX + panelW - padRight - valueW, rowCy, textSize, ACCENT, rowA);

            currentY += rowH * row.anim();
        }

        getDraggable().setWidth(panelW);
        getDraggable().setHeight(contentH);
        renderHudSettingsMenu(ms);
    }

    private String key(MobEffectInstance effect) {
        return effect.getDescriptionId() + ":" + effect.getAmplifier();
    }

    private String formatDuration(int ticks) {
        if (ticks == -1) return "**:**";
        int seconds = ticks / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private static Identifier previewIcon(String name) {
        return Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + name + ".png");
    }

    private Identifier effectIcon(MobEffectInstance effect) {
        String path = effect.getEffect().unwrapKey().get().identifier().getPath();
        return Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + path + ".png");
    }
}
