package tokyo.ghoul.client.ui.widget.overlay;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import tokyo.ghoul.api.system.configs.StaffManager;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.color.ColorUtil;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.framelimiter.FrameLimiter;
import tokyo.ghoul.api.utils.other.ReplaceUtil;
import tokyo.ghoul.api.utils.player.PlayerUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.ui.widget.HudIcons;
import tokyo.ghoul.client.ui.widget.RelicStyle;
import tokyo.ghoul.client.ui.widget.Widget;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StaffsWidget extends Widget {
    private static final String TITLE = "Staff";

    private final FrameLimiter frameLimiter = new FrameLimiter(false);
    private final AnimationUtil globalAlpha = new AnimationUtil();
    private final AnimationUtil widthAnim = new AnimationUtil();
    private List<Staff> cacheStaffs = new ArrayList<>();
    private final Map<String, AnimationUtil> animMap = new HashMap<>();

    public record Staff(String name, String rawName, Status status) {}

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ONLINE("Online"), NEAR("Near"), GM3("Gm3"), VANISH("Vanish");
        private final String label;
    }

    public StaffsWidget() {
        super(100f, 100f);
        enableHudSettingsMenu();
    }

    @Override
    public String getName() { return "Staffs"; }

    private record Row(Staff staff, float anim) {}

    @Override
    public void render(PoseStack ms) {
        boolean editing = getDraggable().isDragging() || mc.gui.screen() instanceof ChatScreen;
        List<Staff> staffs = new ArrayList<>(getStaffList());
        if (staffs.isEmpty() && editing) {
            staffs.add(new Staff("Admin", "Admin", Status.ONLINE));
        }

        for (Staff staff : staffs) {
            AnimationUtil anim = animMap.computeIfAbsent(staff.rawName(), k -> new AnimationUtil());
            anim.update();
            anim.run(1f, 240, Easing.EXPO_OUT);
        }
        animMap.entrySet().removeIf(e -> {
            if (staffs.stream().anyMatch(s -> s.rawName().equals(e.getKey()))) return false;
            AnimationUtil anim = e.getValue();
            anim.update();
            anim.run(0f, 200, Easing.EXPO_OUT);
            return anim.getValue() < 0.01;
        });

        boolean hasVisible = animMap.values().stream().anyMatch(a -> a.getValue() > 0.01);
        globalAlpha.update();
        globalAlpha.run(hasVisible || editing ? 1f : 0f, 300, Easing.EXPO_OUT);
        float alpha = (float) globalAlpha.getValue();
        if (alpha <= 0.01f && staffs.isEmpty() && !editing) return;

        float posX = getDraggable().getX();
        float posY = getDraggable().getY();

        float corner = RelicStyle.ul(36f), stroke = RelicStyle.ul(4f);
        float headerH = RelicStyle.ul(134f), rowH = RelicStyle.ul(62f);
        float topPad = RelicStyle.ul(20f), bottomPad = RelicStyle.ul(35f);
        float titleSize = RelicStyle.ul(49f), textSize = RelicStyle.ul(48f);
        float headerIconW = RelicStyle.ul(52f), headerIconH = RelicStyle.ul(44f);
        float headSize = RelicStyle.ul(48f);
        float headX = RelicStyle.ul(40f), textX = RelicStyle.ul(110f), padRight = RelicStyle.ul(41f);

        var nameFont = Fonts.HUD_MEDIUM;
        var statusFont = Fonts.HUD_SEMIBOLD;

        List<Row> rows = new ArrayList<>();
        float maxNameW = 0f, maxStatusW = 0f;
        for (Staff staff : staffs) {
            AnimationUtil anim = animMap.get(staff.rawName());
            if (anim == null) continue;
            float a = (float) anim.getValue();
            if (a <= 0.01f) continue;
            rows.add(new Row(staff, a));

            maxNameW = Math.max(maxNameW, RelicStyle.width(nameFont, staff.name(), textSize));
            maxStatusW = Math.max(maxStatusW, RelicStyle.width(statusFont, staff.status().getLabel(), textSize));
        }

        float headerW = RelicStyle.ul(42f) + headerIconW + RelicStyle.ul(39f) + RelicStyle.ul(10f) + RelicStyle.ul(36f)
                + RelicStyle.width(nameFont, TITLE, titleSize) + RelicStyle.ul(42f);
        float bodyW = textX + maxNameW + RelicStyle.ul(100f) + maxStatusW + padRight;

        widthAnim.update();
        widthAnim.run(Math.max(RelicStyle.ul(554f), Math.max(headerW, bodyW)), 180, Easing.EXPO_OUT);
        float panelW = (float) widthAnim.getValue();

        float contentH = headerH + (rows.isEmpty() ? 0f : topPad + bottomPad) + rows.size() * rowH;

        RelicStyle.headerPanel(ms, posX, posY, panelW, contentH, headerH, corner, stroke, alpha, liquidGlass());

        float headerCy = posY + headerH / 2f;
        float hIconX = posX + RelicStyle.ul(42f);
        RelicStyle.icon(ms, HudIcons.WM_USERS, hIconX, headerCy, headerIconW, headerIconH, RelicStyle.ACCENT, alpha);
        float sepX = hIconX + headerIconW + RelicStyle.ul(39f);
        RelicStyle.separator(ms, sepX, headerCy, RelicStyle.ul(10f), RelicStyle.ul(26f), alpha);
        RelicStyle.text(ms, nameFont, TITLE, sepX + RelicStyle.ul(10f) + RelicStyle.ul(36f), headerCy, titleSize,
                RelicStyle.TEXT, alpha);

        float currentY = posY + headerH + topPad;
        for (Row row : rows) {
            float rowA = row.anim() * alpha;
            if (rowA <= 0.01f) continue;

            float rowCy = currentY + rowH / 2f + (row.anim() - 1f) * RelicStyle.ul(12f);

            drawHead(ms, row.staff().rawName(), posX + headX, rowCy - headSize / 2f, headSize, (int) (255f * rowA));
            RelicStyle.text(ms, nameFont, row.staff().name(), posX + textX, rowCy, textSize, RelicStyle.ROW_TEXT, rowA);

            String statusLabel = row.staff().status().getLabel();
            float statusW = RelicStyle.width(statusFont, statusLabel, textSize);
            RelicStyle.text(ms, statusFont, statusLabel, posX + panelW - padRight - statusW, rowCy, textSize,
                    statusColor(row.staff().status()), rowA);

            currentY += rowH * row.anim();
        }

        getDraggable().setWidth(panelW);
        getDraggable().setHeight(contentH);
        renderHudSettingsMenu(ms);
    }

    private static Color statusColor(Status status) {
        return switch (status) {
            case ONLINE -> UIColors.positiveColor();
            case NEAR -> UIColors.middleColor();
            case GM3, VANISH -> UIColors.negativeColor();
        };
    }

    private void drawHead(PoseStack ms, String rawName, float x, float y, float size, int alpha) {
        Identifier skin = null;
        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                if (info.getProfile().name().equals(rawName)) {
                    skin = info.getSkin().body().texturePath();
                    break;
                }
            }
        }

        float r = size * 0.22f;
        if (skin != null) {
            RenderUtil.TEXTURE_RECT.draw(ms, x, y, size, size, r, ColorUtil.setAlpha(Color.WHITE, alpha),
                    8f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, skin);
            RenderUtil.TEXTURE_RECT.draw(ms, x, y, size, size, r, ColorUtil.setAlpha(Color.WHITE, alpha),
                    40f / 64f, 8f / 64f, 8f / 64f, 8f / 64f, skin);
        } else {
            RenderUtil.RECT.draw(ms, x, y, size, size, r, ColorUtil.setAlpha(new Color(25, 25, 30), alpha));
            Fonts.HUD_SEMIBOLD.drawCenteredText(ms, "?", x + size / 2f, y + size * 0.2f, size * 0.6f,
                    ColorUtil.setAlpha(Color.WHITE, (int) (180 * (alpha / 255f))));
        }
    }

    private List<Staff> getStaffList() {
        frameLimiter.execute(15, () -> {
            List<Staff> list = new ArrayList<>();
            if (!mc.isLocalServer()) {
                list.addAll(getOnlineStaff());
                list.addAll(getVanishedPlayers());
            }
            cacheStaffs = list;
        });

        return cacheStaffs;
    }

    private List<Staff> getOnlineStaff() {
        List<Staff> staff = new ArrayList<>();
        if (mc.player == null || mc.player.connection == null || mc.level == null) return staff;

        for (PlayerInfo player : mc.player.connection.getOnlinePlayers()) {
            PlayerTeam team = player.getTeam();
            if (team == null) continue;

            String name = player.getProfile().name();
            if (!PlayerUtil.isValidName(name)) continue;

            String prefix = ReplaceUtil.replaceSymbols(team.getPlayerPrefix().getString());

            if (StaffManager.getInstance().contains(name) || isStaffPrefix(prefix.toLowerCase())) {
                Status status = Status.ONLINE;

                if (player.getGameMode() == GameType.SPECTATOR) {
                    status = Status.GM3;
                } else if (mc.level.players().stream().anyMatch(p -> p.getGameProfile().name().equals(name))) {
                    status = Status.NEAR;
                }

                String displayName = prefix.isBlank() ? name : (prefix + " " + name).trim();
                staff.add(new Staff(displayName, name, status));
            }
        }
        return staff;
    }

    private List<Staff> getVanishedPlayers() {
        List<Staff> vanished = new ArrayList<>();
        if (mc.level == null || mc.level.getScoreboard() == null || mc.getConnection() == null)
            return vanished;

        Set<String> onlineNames = new HashSet<>();
        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
            onlineNames.add(entry.getProfile().name());
        }

        for (PlayerTeam team : mc.level.getScoreboard().getPlayerTeams()) {
            for (String name : team.getPlayers()) {
                if (!PlayerUtil.isValidName(name)) continue;
                if (!onlineNames.contains(name) && StaffManager.getInstance().contains(name)) {
                    vanished.add(new Staff(name, name, Status.VANISH));
                }
            }
        }

        return vanished;
    }

    private boolean isStaffPrefix(String prefix) {
        return (
                prefix.contains("helper") ||
                prefix.contains("moder") ||
                prefix.contains("admin") ||
                prefix.contains("owner") ||
                prefix.contains("developer") ||
                prefix.contains("staff") ||
                prefix.contains("curator") ||

                prefix.contains("куратор") ||
                prefix.contains("разраб") ||
                prefix.contains("модер") ||
                prefix.contains("админ") ||
                prefix.contains("стажер") ||
                prefix.contains("стажёр") ||
                prefix.contains("хелпер")
        );
    }
}
