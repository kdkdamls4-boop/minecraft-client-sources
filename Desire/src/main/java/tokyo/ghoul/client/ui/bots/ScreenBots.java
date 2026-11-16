package tokyo.ghoul.client.ui.bots;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.animation.AnimationUtil;
import tokyo.ghoul.api.utils.animation.Easing;
import tokyo.ghoul.api.utils.math.MouseUtil;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Font;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.api.utils.render.pipeline.EvaRenderer;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.SessionState;
import tokyo.ghoul.client.services.RenderService;

import java.awt.Color;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntPredicate;

public class ScreenBots extends Screen implements QuickImports {
    @Getter private static final ScreenBots instance = new ScreenBots();

    private static final String[] NICK_PREFIXES = {
            "Shadow", "Night", "Cyber", "Swift", "Ghost", "Frost", "Storm", "Pixel", "Toxic", "Lucky"
    };

    private static final float PAD = 10f;
    private static final float BTN_H = 16f;
    private static final float ROW_H = 34f;
    private static final float SLOT_GAP = 3f;

    private static final Color WINDOW = new Color(12, 12, 14, 170);
    private static final Color PANEL = new Color(255, 255, 255, 6);
    private static final Color TILE = new Color(255, 255, 255, 7);
    private static final Color SLOT = new Color(255, 255, 255, 9);
    private static final Color MUTED = new Color(140, 140, 150);
    private static final Color DIM = new Color(105, 105, 115);
    private static final Color GREEN = new Color(52, 196, 92);
    private static final Color ORANGE = new Color(230, 138, 16);
    private static final Color RED = new Color(235, 80, 80);

    private final Field nickField = new Field("Ник", 16, c -> Character.isLetterOrDigit(c) && c < 128 || c == '_', false);
    private final Field ipField = new Field("IP сервера", 64, c -> c > ' ' && c != 127, false);
    private final Field passField = new Field("Пароль (авто /login)", 32, c -> c > ' ' && c != 127, true);
    private final Field chatField = new Field("Напишите сообщение", 256, c -> c >= ' ' && c != 127, false);
    private final Field inlineField = new Field("", 64, c -> c > ' ' && c != 127, false);
    private BotSession inlineTarget;
    private InlineMode inlineMode;

    private BotSession selected;
    private boolean adding;
    private ViewMode view = ViewMode.INVENTORY;
    private boolean chatToAll;

    private Field focused;
    private String status;
    private boolean statusError;
    private long statusAtMs;

    private float scroll;
    private float maxScroll;
    private final AnimationUtil scrollAnim = new AnimationUtil();
    private float listX, listY, listW, listH;

    private final List<Hit> hits = new ArrayList<>();
    private final List<ItemDraw> items = new ArrayList<>();
    private final Map<BotSession, BotMiniMap> miniMaps = new IdentityHashMap<>();
    private float clipTop = Float.NEGATIVE_INFINITY;
    private float clipBottom = Float.POSITIVE_INFINITY;

    protected ScreenBots() {
        super(Component.literal("Bots"));
    }

    @Override
    protected void init() {
        super.init();
        if (ipField.text.isEmpty()) {
            ServerData server = mc.getCurrentServer();
            if (server != null && server.ip != null) {
                ipField.set(server.ip);
            }
        }
        closeInline();
        focused = null;
        adding = false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public void onClose() {
        focused = null;
        closeInline();
        super.onClose();
    }


    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);
            hits.clear();
            items.clear();
            EvaRenderer renderer = EvaRenderer.getInstance();
            renderer.beginUnder();
            try {
                renderWindow(context, mouseX, mouseY);
            } finally {
                renderer.endUnder();
            }
            for (ItemDraw item : items) {
                context.item(item.stack(), item.x(), item.y());
                context.itemDecorations(font, item.stack(), item.x(), item.y());
            }
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void renderWindow(GuiGraphicsExtractor context, int mouseX, int mouseY) {
        PoseStack m = RenderUtil.matrices();
        BotSessionManager manager = BotSessionManager.getInstance();

        float winW = Math.min(width - 30f, 620f);
        float winH = Math.min(height - 30f, 400f);
        float winX = (width - winW) / 2f;
        float winY = (height - winH) / 2f;

        RenderUtil.RECT.draw(m, 0f, 0f, width, height, 0f, new Color(0, 0, 0, 60));
        RenderUtil.BLUR_RECT.draw(m, winX, winY, winW, winH, 8f, new Color(255, 255, 255, 255), 0f);
        RenderUtil.RECT.draw(m, winX, winY, winW, winH, 8f, WINDOW);
        RenderUtil.RECT.drawOutline(m, winX, winY, winW, winH, 8f, 1f, new Color(255, 255, 255, 8));

        Fonts.PS_BOLD.drawText(m, "Боты", winX + PAD + 2f, winY + 11f, 10f, Color.WHITE);
        String counter = manager.getActiveBotsCount() + " / " + manager.getMaxBots();
        Fonts.PS_MEDIUM.drawText(m, counter, winX + winW - PAD - 2f - Fonts.PS_MEDIUM.getWidth(counter, 8f), winY + 12f, 8f, MUTED);
        if (status != null && System.currentTimeMillis() - statusAtMs < 4000L) {
            float sx = winX + PAD + 14f + Fonts.PS_BOLD.getWidth("Боты", 10f);
            float maxW = winX + winW - PAD - 20f - Fonts.PS_MEDIUM.getWidth(counter, 8f) - sx;
            Fonts.PS_REGULAR.drawText(m, fit(Fonts.PS_REGULAR, status, 7.5f, maxW), sx, winY + 12.5f, 7.5f,
                    statusError ? RED : GREEN);
        }

        List<BotSession> sessions = new ArrayList<>();
        sessions.add(manager.getMainSession());
        sessions.addAll(manager.getAllBots());
        miniMaps.keySet().removeIf(session -> {
            if (sessions.contains(session)) return false;
            miniMaps.get(session).release();
            return true;
        });
        if (selected == null || !sessions.contains(selected)) {
            selected = manager.getActiveSession();
        }
        if (inlineTarget != null && inlineTarget != selected) {
            closeInline();
        }

        float top = winY + 30f;
        float chatH = 20f;
        float bodyH = winH - (top - winY) - PAD - chatH - 8f;
        float leftW = Math.min(170f, winW * 0.3f);

        renderList(m, sessions, winX + PAD, top, leftW, bodyH, mouseX, mouseY);

        float rx = winX + PAD + leftW + 8f;
        float rw = winX + winW - PAD - rx;
        RenderUtil.RECT.draw(m, rx, top, rw, bodyH, 6f, PANEL);
        if (adding) {
            renderAddForm(m, rx, top, rw, bodyH, mouseX, mouseY);
        } else {
            renderDetails(context, m, selected, rx, top, rw, bodyH, mouseX, mouseY);
        }

        renderChatBar(m, winX + PAD, top + bodyH + 8f, winW - PAD * 2f, chatH, mouseX, mouseY);
    }


    private void renderList(PoseStack m, List<BotSession> sessions, float x, float y, float w, float h,
                            int mouseX, int mouseY) {
        BotSessionManager manager = BotSessionManager.getInstance();
        RenderUtil.RECT.draw(m, x, y, w, h, 6f, PANEL);

        listX = x;
        listY = y + 5f;
        listW = w;
        listH = h - 5f - BTN_H - 12f;

        scrollAnim.update();
        scrollAnim.run(scroll, 220, Easing.EXPO_OUT);
        float offset = (float) scrollAnim.getValue();

        clipTop = listY;
        clipBottom = listY + listH;
        ScissorUtil.start(m, listX, listY, listW, listH);
        try {
            for (int i = 0; i < sessions.size(); i++) {
                BotSession session = sessions.get(i);
                float ry = listY + offset + i * (ROW_H + 3f);
                if (ry + ROW_H < listY || ry > listY + listH) continue;

                boolean sel = session == selected && !adding;
                boolean hov = MouseUtil.isHovered(mouseX, mouseY, x + 5f, ry, w - 10f, ROW_H)
                        && mouseY >= clipTop && mouseY <= clipBottom;
                if (sel || hov) {
                    RenderUtil.RECT.draw(m, x + 5f, ry, w - 10f, ROW_H, 5f, new Color(255, 255, 255, sel ? 13 : 6));
                }
                RenderUtil.RECT.draw(m, x + 12f, ry + 10.5f, 4f, 4f, 2f, stateColor(session));
                Fonts.PS_MEDIUM.drawText(m, fit(Fonts.PS_MEDIUM, displayName(session), 8.5f, w - 32f),
                        x + 21f, ry + 8f, 8.5f, Color.WHITE);
                Fonts.PS_REGULAR.drawText(m, fit(Fonts.PS_REGULAR, listSubtitle(session), 7f, w - 32f),
                        x + 21f, ry + 19.5f, 7f, manager.isForeground(session) ? MUTED : DIM);
                hit(x + 5f, ry, w - 10f, ROW_H, () -> {
                    selected = session;
                    adding = false;
                    closeInline();
                });
            }
        } finally {
            ScissorUtil.stop(m);
            clipTop = Float.NEGATIVE_INFINITY;
            clipBottom = Float.POSITIVE_INFINITY;
        }

        float contentH = sessions.size() * (ROW_H + 3f) - 3f;
        maxScroll = Math.min(listH - contentH, 0f);
        scroll = Mth.clamp(scroll, maxScroll, 0f);

        float by = y + h - BTN_H - 6f;
        float bw = (w - 14f) / 2f;
        boolean full = manager.getAllBots().size() >= manager.getMaxBots();
        button(m, x + 5f, by, bw, BTN_H, full ? "Лимит " + manager.getMaxBots() : "+ добавить", adding ? GREEN : null,
                !full, mouseX, mouseY, this::toggleAddForm);
        BotSession target = selected;
        button(m, x + 9f + bw, by, bw, BTN_H, "Удалить", null,
                !adding && target != null && target != manager.getMainSession(), mouseX, mouseY, () -> removeBot(target));
    }


    private void renderDetails(GuiGraphicsExtractor context, PoseStack m, BotSession session, float x, float y,
                               float w, float h, int mouseX, int mouseY) {
        BotSessionManager manager = BotSessionManager.getInstance();
        boolean onScreen = manager.isForeground(session);
        LocalPlayer player = onScreen ? mc.player : session.getPlayer();
        ClientLevel level = onScreen ? mc.level : session.getWorld();
        if (level == null) player = null;

        float ix = x + 10f;
        float iw = w - 20f;
        float cy = y + 9f;

        Fonts.PS_BOLD.drawText(m, fit(Fonts.PS_BOLD, displayName(session), 10f, iw - 90f), ix, cy, 10f, Color.WHITE);
        String state = onScreen ? "На экране" : stateText(session);
        Fonts.PS_REGULAR.drawText(m, state, ix + iw - Fonts.PS_REGULAR.getWidth(state, 7.5f), cy + 1.5f, 7.5f, DIM);
        cy += 17f;

        float hp = player != null ? player.getHealth() : 0f;
        float maxHp = player != null ? Math.max(1f, player.getMaxHealth()) : 20f;
        String hpText = player != null ? Math.round(hp) + " / " + Math.round(maxHp) : "— / —";
        Fonts.PS_MEDIUM.drawText(m, hpText, ix, cy, 7.5f, MUTED);
        float barX = ix + Fonts.PS_MEDIUM.getWidth(hpText, 7.5f) + 8f;
        float barW = ix + iw - barX;
        RenderUtil.RECT.draw(m, barX, cy + 2.5f, barW, 3f, 1.5f, new Color(255, 255, 255, 14));
        if (player != null) {
            float ratio = Mth.clamp(hp / maxHp, 0f, 1f);
            RenderUtil.RECT.draw(m, barX, cy + 2.5f, barW * ratio, 3f, 1.5f, hp <= 6f ? RED : GREEN);
        }
        cy += 13f;

        float tileH = 25f;
        float tileGap = 5f;
        float tileW = (iw - tileGap * 2f) / 3f;
        String position = "—";
        String ping = "—";
        if (player != null) {
            String dimension = level.dimension() == Level.NETHER ? "Ад" : level.dimension() == Level.END ? "Энд" : "Мир";
            position = dimension + " " + player.getBlockX() + " " + player.getBlockY() + " " + player.getBlockZ();
            ping = ping(player) + " мс";
        }
        tile(m, ix, cy, tileW, tileH, "Сервер", serverOf(session));
        tile(m, ix + tileW + tileGap, cy, tileW, tileH, "Позиция", position);
        tile(m, ix + (tileW + tileGap) * 2f, cy, tileW, tileH, "Пинг", ping);
        cy += tileH + 7f;

        float rowGap = 5f;
        float controlsY = y + h - 9f - (BTN_H * 3f + rowGap * 2f);
        String label = switch (view) {
            case INVENTORY -> "Инвентарь";
            case SCREEN -> "Экран бота";
            case CHAT -> "Чат бота";
        };
        Fonts.PS_MEDIUM.drawText(m, label, ix, cy, 7f, DIM);
        float viewY = cy + 11f;
        float viewH = controlsY - 8f - viewY;
        switch (view) {
            case INVENTORY -> renderInventory(m, player, ix, viewY, iw, viewH, cy, mouseX, mouseY);
            case SCREEN -> renderMiniScreen(context, m, session, player, level, ix, viewY, iw, viewH, mouseX, mouseY);
            case CHAT -> renderChatLog(m, session, ix, viewY, iw, viewH);
        }

        renderControls(m, session, ix, controlsY, iw, rowGap, mouseX, mouseY);
    }

    private void tile(PoseStack m, float x, float y, float w, float h, String title, String value) {
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, TILE);
        Fonts.PS_REGULAR.drawCenteredText(m, title, x + w / 2f, y + 4.5f, 6.5f, DIM);
        Fonts.PS_MEDIUM.drawCenteredText(m, fit(Fonts.PS_MEDIUM, value, 7.5f, w - 8f), x + w / 2f, y + 13.5f, 7.5f, Color.WHITE);
    }

    private void renderInventory(PoseStack m, LocalPlayer player, float x, float y, float w, float h, float labelY,
                                 int mouseX, int mouseY) {
        float sectionGap = 5f;
        float slot = Math.min((w - SLOT_GAP * 8f) / 9f, (h - SLOT_GAP * 2f - sectionGap * 2f) / 5f);
        slot = Math.max(12f, Math.min(slot, 34f));
        float gridW = slot * 9f + SLOT_GAP * 8f;
        float gx = x + (w - gridW) / 2f;

        ItemStack hovered = ItemStack.EMPTY;
        EquipmentSlot[] armor = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int col = 0; col < armor.length; col++) {
            ItemStack stack = player != null ? player.getItemBySlot(armor[col]) : ItemStack.EMPTY;
            if (slot(m, stack, gx + col * (slot + SLOT_GAP), y, slot, false, mouseX, mouseY)) hovered = stack;
        }
        ItemStack offhand = player != null ? player.getItemBySlot(EquipmentSlot.OFFHAND) : ItemStack.EMPTY;
        if (slot(m, offhand, gx + 8f * (slot + SLOT_GAP), y, slot, false, mouseX, mouseY)) hovered = offhand;

        float rowsY = y + slot + sectionGap + SLOT_GAP;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                ItemStack stack = player != null ? player.getInventory().getItem(9 + row * 9 + col) : ItemStack.EMPTY;
                if (slot(m, stack, gx + col * (slot + SLOT_GAP), rowsY + row * (slot + SLOT_GAP), slot, false, mouseX, mouseY)) {
                    hovered = stack;
                }
            }
        }

        float hotbarY = rowsY + 3f * (slot + SLOT_GAP) + sectionGap;
        int selectedSlot = player != null ? player.getInventory().getSelectedSlot() : -1;
        for (int col = 0; col < 9; col++) {
            ItemStack stack = player != null ? player.getInventory().getItem(col) : ItemStack.EMPTY;
            if (slot(m, stack, gx + col * (slot + SLOT_GAP), hotbarY, slot, col == selectedSlot, mouseX, mouseY)) {
                hovered = stack;
            }
        }

        String right = !hovered.isEmpty() ? hovered.getHoverName().getString() : player == null ? "нет данных" : null;
        if (right != null) {
            right = fit(Fonts.PS_MEDIUM, right, 7f, w - 70f);
            Fonts.PS_MEDIUM.drawText(m, right, x + w - Fonts.PS_MEDIUM.getWidth(right, 7f), labelY, 7f,
                    hovered.isEmpty() ? DIM : Color.WHITE);
        }
    }

    private boolean slot(PoseStack m, ItemStack stack, float x, float y, float size, boolean active, int mouseX, int mouseY) {
        boolean hov = MouseUtil.isHovered(mouseX, mouseY, x, y, size, size) && !stack.isEmpty();
        Color fill = active ? new Color(255, 255, 255, 34) : hov ? new Color(255, 255, 255, 18) : SLOT;
        RenderUtil.RECT.draw(m, x, y, size, size, 3.5f, fill);
        if (active) {
            RenderUtil.RECT.drawOutline(m, x, y, size, size, 3.5f, 1f, new Color(255, 255, 255, 70));
        }
        if (!stack.isEmpty()) {
            items.add(new ItemDraw(stack, Math.round(x + (size - 16f) / 2f), Math.round(y + (size - 16f) / 2f)));
        }
        return hov;
    }

    private void renderChatLog(PoseStack m, BotSession session, float x, float y, float w, float h) {
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, SLOT);
        int count = Math.max(1, (int) ((h - 8f) / 10f));
        List<String> chat = session.getRecentChat(count);
        if (chat.isEmpty()) {
            Fonts.PS_REGULAR.drawCenteredText(m, "Чат пуст", x + w / 2f, y + h / 2f - 4f, 8f, DIM);
            return;
        }
        float lineY = y + h - 5f - chat.size() * 10f;
        for (String line : chat) {
            Fonts.PS_REGULAR.drawText(m, fit(Fonts.PS_REGULAR, line, 7.5f, w - 12f), x + 6f, lineY + 1f, 7.5f,
                    new Color(225, 225, 230));
            lineY += 10f;
        }
    }

    private void renderMiniScreen(GuiGraphicsExtractor context, PoseStack m, BotSession session, LocalPlayer player,
                                  ClientLevel level, float x, float y, float w, float h, int mouseX, int mouseY) {
        BotSessionManager manager = BotSessionManager.getInstance();
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, new Color(8, 8, 10, 255));

        if (level == null || player == null) {
            String title;
            Color color = MUTED;
            switch (session.getState()) {
                case CONNECTING -> title = "Подключение…";
                case CONNECTED -> title = session == manager.getMainSession() ? "Не в игре" : "Загрузка мира…";
                case ERROR -> {
                    title = "Ошибка";
                    color = RED;
                }
                default -> title = "Оффлайн";
            }
            if (session == manager.getMainSession()) {
                title = "Не в игре";
            }
            Fonts.PS_BOLD.drawCenteredText(m, title, x + w / 2f, y + h / 2f - 10f, 10f, color);
            String reason = session.getDisconnectReason();
            if (reason != null && session.getState() != SessionState.CONNECTING) {
                Fonts.PS_REGULAR.drawCenteredText(m, fit(Fonts.PS_REGULAR, reason, 7.5f, w - 12f), x + w / 2f, y + h / 2f + 4f,
                        7.5f, DIM);
            }
            long reconnectIn = manager.getReconnectInMs(session);
            if (reconnectIn >= 0) {
                Fonts.PS_REGULAR.drawCenteredText(m, "Реконнект через " + (reconnectIn / 1000L + 1) + "с",
                        x + w / 2f, y + h / 2f + 15f, 7.5f, ORANGE);
            }
            return;
        }

        BotMiniMap map = miniMaps.computeIfAbsent(session, s -> new BotMiniMap());
        Identifier texture = map.update(level, player);
        float visible = Math.min(1f, h / w);
        float v0 = (1f - visible) / 2f;
        RenderUtil.TEXTURE_RECT.draw(m, x, y, w, h, 4f, Color.WHITE, 0f, v0, 1f, visible, texture);
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, new Color(0, 0, 0, 40));

        float scale = w / BotMiniMap.SIZE;
        float centerX = x + w / 2f;
        float centerY = y + h / 2f;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player) continue;
            float dx = (float) (entity.getX() - player.getX()) * scale;
            float dz = (float) (entity.getZ() - player.getZ()) * scale;
            if (Math.abs(dx) > w / 2f - 2f || Math.abs(dz) > h / 2f - 2f) continue;
            Color dot;
            float size;
            if (entity instanceof Player) {
                dot = new Color(255, 70, 70);
                size = 3f;
            } else if (entity instanceof Enemy) {
                dot = new Color(255, 170, 40);
                size = 2.5f;
            } else {
                continue;
            }
            RenderUtil.RECT.draw(m, centerX + dx - size / 2f, centerY + dz - size / 2f, size, size, size / 2f, dot);
        }

        float yaw = (float) Math.toRadians(player.getYRot());
        for (int i = 1; i <= 3; i++) {
            float step = i * 2.6f;
            RenderUtil.RECT.draw(m, centerX - Mth.sin(yaw) * step - 0.8f, centerY + Mth.cos(yaw) * step - 0.8f,
                    1.6f, 1.6f, 0.8f, new Color(255, 255, 255, 230 - i * 50));
        }
        RenderUtil.RECT.draw(m, centerX - 2.5f, centerY - 2.5f, 5f, 5f, 2.5f, Color.WHITE);

        int modelX1 = (int) (x + w - 4f);
        int modelX0 = (int) (modelX1 - h * 0.45f);
        int modelY1 = (int) (y + h - 2f);
        int modelY0 = (int) (y + 6f);
        if (player.isAlive()) {
            InventoryScreen.extractEntityInInventoryFollowsMouse(context, modelX0, modelY0, modelX1, modelY1,
                    Math.max(10, (int) (h * 0.2f)), 0.0625f, mouseX, mouseY, player);
        } else {
            Fonts.PS_BOLD.drawCenteredText(m, "МЁРТВ", x + w / 2f, y + h / 2f - 18f, 10f, RED);
        }

        float textMaxW = w - (modelX1 - modelX0) - 14f;
        List<String> chat = session.getRecentChat(3);
        float lineY = y + h - 4f - chat.size() * 9f;
        if (session.getActionBar() != null && System.currentTimeMillis() - session.getActionBarAtMs() < 3000L) {
            String bar = fit(Fonts.PS_MEDIUM, session.getActionBar(), 7.5f, textMaxW);
            Fonts.PS_MEDIUM.drawText(m, bar, x + 6f, lineY - 11f, 7.5f, new Color(255, 230, 120));
        }
        for (String line : chat) {
            String text = fit(Fonts.PS_REGULAR, line, 7f, textMaxW);
            RenderUtil.RECT.draw(m, x + 4f, lineY - 1f, Fonts.PS_REGULAR.getWidth(text, 7f) + 6f, 9f, 2f, new Color(0, 0, 0, 120));
            Fonts.PS_REGULAR.drawText(m, text, x + 7f, lineY + 0.5f, 7f, new Color(235, 235, 235));
            lineY += 9f;
        }
    }

    private void renderControls(PoseStack m, BotSession session, float x, float y, float w, float gap,
                                int mouseX, int mouseY) {
        BotSessionManager manager = BotSessionManager.getInstance();
        boolean isMain = session == manager.getMainSession();
        boolean onScreen = manager.isForeground(session);
        boolean online = isMain ? mainOnline() : session.isActive();

        float third = (w - gap * 2f) / 3f;
        boolean canPlay = !onScreen && (isMain || session.isPlayable());
        button(m, x, y, third, BTN_H, onScreen ? "На экране" : "Играть", null, canPlay, mouseX, mouseY, () -> {
            if (isMain) manager.switchToMain();
            else manager.switchTo(session);
        });
        button(m, x + third + gap, y, third, BTN_H, "Выйти в /hub", ORANGE, online, mouseX, mouseY, () -> {
            manager.sendChat(session, "/hub");
            setStatus(displayName(session) + ": /hub", false);
        });
        boolean running = session.getState() == SessionState.CONNECTED || session.getState() == SessionState.CONNECTING;
        button(m, x + (third + gap) * 2f, y, third, BTN_H, running || isMain ? "Отключить" : "Подключить", null, !isMain,
                mouseX, mouseY, () -> {
                    if (running) {
                        manager.disconnectBot(session);
                        setStatus("Бот " + session.getName() + " отключён", false);
                    } else {
                        manager.connectBot(session);
                        setStatus("Бот " + session.getName() + " заходит на " + session.getServerAddress(), false);
                    }
                });

        float y2 = y + BTN_H + gap;
        if (inlineTarget == session) {
            renderInline(m, x, y2, w, mouseX, mouseY);
        } else {
            boolean hasPass = !isMain && session.getPassword() != null && !session.getPassword().isBlank();
            button(m, x, y2, third, BTN_H, "Реконнект", !isMain && session.isAutoReconnect() ? GREEN : null, !isMain,
                    mouseX, mouseY, () -> manager.updateBot(session, null, null, !session.isAutoReconnect()));
            button(m, x + third + gap, y2, third, BTN_H, "Авто-логин", hasPass ? GREEN : null, !isMain,
                    mouseX, mouseY, () -> openInline(session, InlineMode.PASSWORD));
            button(m, x + (third + gap) * 2f, y2, third, BTN_H, "Сменить IP", null, !isMain,
                    mouseX, mouseY, () -> openInline(session, InlineMode.IP));
        }

        float y3 = y2 + BTN_H + gap;
        float half = (w - gap) / 2f;
        button(m, x, y3, half, BTN_H, "Экран бота", view == ViewMode.SCREEN ? GREEN : null, true, mouseX, mouseY,
                () -> view = view == ViewMode.SCREEN ? ViewMode.INVENTORY : ViewMode.SCREEN);
        button(m, x + half + gap, y3, half, BTN_H, "Чат бота", view == ViewMode.CHAT ? GREEN : null, true, mouseX, mouseY,
                () -> view = view == ViewMode.CHAT ? ViewMode.INVENTORY : ViewMode.CHAT);
    }

    private void renderInline(PoseStack m, float x, float y, float w, int mouseX, int mouseY) {
        float okW = 40f;
        float closeW = BTN_H;
        field(m, inlineField, x, y, w - okW - closeW - 8f, BTN_H, mouseX, mouseY);
        button(m, x + w - okW - closeW - 4f, y, okW, BTN_H, "OK", GREEN, true, mouseX, mouseY, this::submitInline);
        button(m, x + w - closeW, y, closeW, BTN_H, "X", null, true, mouseX, mouseY, this::closeInline);
    }


    private void renderAddForm(PoseStack m, float x, float y, float w, float h, int mouseX, int mouseY) {
        BotSessionManager manager = BotSessionManager.getInstance();
        float ix = x + 10f;
        float iw = Math.min(w - 20f, 300f);
        float cy = y + 9f;
        float fh = 18f;

        Fonts.PS_BOLD.drawText(m, "Новый бот", ix, cy, 10f, Color.WHITE);
        button(m, x + w - 10f - BTN_H, cy - 3f, BTN_H, BTN_H, "X", null, true, mouseX, mouseY, () -> adding = false);
        cy += 24f;

        Fonts.PS_MEDIUM.drawText(m, "Ник", ix, cy, 7f, DIM);
        cy += 10f;
        field(m, nickField, ix, cy, iw - fh - 4f, fh, mouseX, mouseY);
        button(m, ix + iw - fh, cy, fh, fh, "?", null, true, mouseX, mouseY, this::generateNick);
        cy += fh + 9f;

        Fonts.PS_MEDIUM.drawText(m, "IP сервера", ix, cy, 7f, DIM);
        cy += 10f;
        field(m, ipField, ix, cy, iw, fh, mouseX, mouseY);
        cy += fh + 9f;

        Fonts.PS_MEDIUM.drawText(m, "Пароль для авто /login — необязательно", ix, cy, 7f, DIM);
        cy += 10f;
        field(m, passField, ix, cy, iw, fh, mouseX, mouseY);
        cy += fh + 12f;

        boolean full = manager.getAllBots().size() >= manager.getMaxBots();
        button(m, ix, cy, 110f, fh, full ? "Лимит " + manager.getMaxBots() : "+ Добавить", GREEN, !full,
                mouseX, mouseY, this::addBot);
        Fonts.PS_REGULAR.drawText(m, "Enter — добавить   ·   Tab — следующее поле", ix + 120f, cy + 5.5f, 7f, DIM);
    }


    private void renderChatBar(PoseStack m, float x, float y, float w, float h, int mouseX, int mouseY) {
        float allW = 92f;
        float sendW = 24f;
        field(m, chatField, x, y, w - allW - sendW - 8f, h, mouseX, mouseY);
        button(m, x + w - allW - sendW - 4f, y, sendW, h, ">", null, !chatField.text.isEmpty(), mouseX, mouseY, this::sendChatBar);
        button(m, x + w - allW, y, allW, h, "от всех ботов", chatToAll ? GREEN : null, true, mouseX, mouseY,
                () -> chatToAll = !chatToAll);
    }


    private void button(PoseStack m, float x, float y, float w, float h, String label, Color fill, boolean enabled,
                        int mouseX, int mouseY, Runnable action) {
        boolean hov = enabled && MouseUtil.isHovered(mouseX, mouseY, x, y, w, h) && mouseY >= clipTop && mouseY <= clipBottom;
        Color bg;
        if (!enabled) {
            bg = new Color(255, 255, 255, 6);
        } else if (fill != null) {
            bg = hov ? lighten(fill, 22) : fill;
        } else {
            bg = new Color(255, 255, 255, hov ? 22 : 11);
        }
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, bg);
        float size = h >= 18f ? 8f : 7.5f;
        Fonts.PS_MEDIUM.drawCenteredText(m, fit(Fonts.PS_MEDIUM, label, size, w - 6f), x + w / 2f, y + (h - size) / 2f + 0.5f,
                size, enabled ? Color.WHITE : DIM);
        if (enabled) {
            hit(x, y, w, h, action);
        }
    }

    private void field(PoseStack m, Field field, float x, float y, float w, float h, int mouseX, int mouseY) {
        boolean focus = focused == field;
        boolean hov = MouseUtil.isHovered(mouseX, mouseY, x, y, w, h);
        RenderUtil.RECT.draw(m, x, y, w, h, 4f, new Color(255, 255, 255, focus ? 14 : hov ? 11 : 8));
        if (focus) RenderUtil.RECT.drawOutline(m, x, y, w, h, 4f, 1f, new Color(255, 255, 255, 40));

        float size = h >= 18f ? 8f : 7.5f;
        boolean empty = field.text.isEmpty();
        String shown = empty ? (focus ? "" : field.placeholder) : field.display();
        while (!shown.isEmpty() && Fonts.PS_REGULAR.getWidth(shown, size) > w - 12f) {
            shown = shown.substring(1);
        }
        Fonts.PS_REGULAR.drawText(m, shown, x + 6f, y + (h - size) / 2f, size, empty ? DIM : Color.WHITE);
        if (focus) {
            float textW = empty ? 0f : Fonts.PS_REGULAR.getWidth(shown, size);
            float blink = (float) ((Math.sin(System.currentTimeMillis() / 250.0) + 1.0) / 2.0);
            RenderUtil.RECT.draw(m, x + 6f + textW + 1f, y + 3f, 1f, h - 6f, 0.5f, new Color(255, 255, 255, (int) (blink * 255)));
        }
        hit(x, y, w, h, () -> focused = field);
    }

    private void hit(float x, float y, float w, float h, Runnable action) {
        float top = Math.max(y, clipTop);
        float bottom = Math.min(y + h, clipBottom);
        if (bottom <= top) return;
        hits.add(new Hit(x, top, w, bottom - top, action));
    }


    private void toggleAddForm() {
        adding = !adding;
        closeInline();
        if (adding) {
            if (ipField.text.isEmpty()) {
                ServerData server = mc.getCurrentServer();
                if (server != null && server.ip != null) ipField.set(server.ip);
            }
            focused = nickField;
        }
    }

    private void addBot() {
        BotSessionManager manager = BotSessionManager.getInstance();
        String nick = nickField.text.toString().trim();
        String ip = ipField.text.toString().trim();
        if (!BotSessionManager.isValidNick(nick)) {
            setStatus("Ник: 3–16 символов, латиница, цифры и _", true);
            focused = nickField;
            return;
        }
        if (ip.isEmpty()) {
            setStatus("Укажите IP сервера", true);
            focused = ipField;
            return;
        }
        if (manager.getBot(nick) != null) {
            setStatus("Бот " + nick + " уже есть", true);
            return;
        }
        if (manager.getAllBots().size() >= manager.getMaxBots()) {
            setStatus("Достигнут лимит ботов (" + manager.getMaxBots() + ") — поднимите в настройках модуля", true);
            return;
        }
        if (manager.createBot(nick, ip, passField.text.toString(), false)) {
            setStatus("Бот " + nick + " заходит на " + ip, false);
            nickField.set("");
            focused = null;
            adding = false;
            selected = manager.getBot(nick);
        } else {
            setStatus("Не удалось создать бота", true);
        }
    }

    private void removeBot(BotSession session) {
        if (session == null || session == BotSessionManager.getInstance().getMainSession()) return;
        BotSessionManager.getInstance().removeBot(session.getName());
        if (inlineTarget == session) closeInline();
        selected = null;
        setStatus("Бот " + session.getName() + " удалён", false);
    }

    private void generateNick() {
        String prefix = NICK_PREFIXES[ThreadLocalRandom.current().nextInt(NICK_PREFIXES.length)];
        nickField.set(prefix + "_" + ThreadLocalRandom.current().nextInt(100, 9999));
        focused = nickField;
    }

    private void sendChatBar() {
        BotSessionManager manager = BotSessionManager.getInstance();
        String text = chatField.text.toString().trim();
        focused = chatField;
        if (text.isEmpty()) return;

        if (chatToAll) {
            int sent = 0;
            for (BotSession bot : manager.getAllBots()) {
                if (!bot.isActive()) continue;
                manager.sendChat(bot, text);
                sent++;
            }
            if (sent == 0) {
                setStatus("Нет ботов онлайн", true);
                return;
            }
            setStatus("Отправлено от " + sent + " ботов", false);
        } else {
            BotSession target = selected;
            boolean online = target == manager.getMainSession() ? mainOnline() : target != null && target.isActive();
            if (!online) {
                setStatus(target == null ? "Бот не выбран" : displayName(target) + " не в сети", true);
                return;
            }
            manager.sendChat(target, text);
        }
        chatField.set("");
    }

    private boolean mainOnline() {
        return BotSessionManager.getInstance().getMainSession().isActive();
    }

    private void openInline(BotSession session, InlineMode mode) {
        inlineTarget = session;
        inlineMode = mode;
        switch (mode) {
            case IP -> {
                inlineField.placeholder = "IP сервера";
                inlineField.masked = false;
                inlineField.set(session.getServerAddress() == null ? "" : session.getServerAddress());
            }
            case PASSWORD -> {
                inlineField.placeholder = "Пароль (пусто — без авто-логина)";
                inlineField.masked = true;
                inlineField.set(session.getPassword() == null ? "" : session.getPassword());
            }
        }
        focused = inlineField;
    }

    private void closeInline() {
        if (focused == inlineField) focused = null;
        inlineTarget = null;
        inlineMode = null;
        inlineField.set("");
    }

    private void submitInline() {
        BotSession target = inlineTarget;
        if (target == null) return;
        BotSessionManager manager = BotSessionManager.getInstance();
        String value = inlineField.text.toString().trim();
        switch (inlineMode) {
            case IP -> {
                if (value.isEmpty() || value.contains(" ")) {
                    setStatus("Некорректный IP", true);
                    focused = inlineField;
                    return;
                }
                manager.updateBot(target, value, null, target.isAutoReconnect());
                setStatus("IP бота " + target.getName() + ": " + value + " (применится при следующем входе)", false);
            }
            case PASSWORD -> {
                manager.updateBot(target, null, value, target.isAutoReconnect());
                setStatus(value.isEmpty() ? "Авто-логин выключен" : "Пароль сохранён", false);
            }
        }
        closeInline();
    }

    private void setStatus(String text, boolean error) {
        status = text;
        statusError = error;
        statusAtMs = System.currentTimeMillis();
    }


    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(click, doubled);
        }
        double mx = click.x();
        double my = click.y();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit hit = hits.get(i);
            if (MouseUtil.isHovered(mx, my, hit.x, hit.y, hit.w, hit.h)) {
                focused = null;
                hit.action.run();
                return true;
            }
        }
        focused = null;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (MouseUtil.isHovered(mouseX, mouseY, listX, listY, listW, listH)) {
            scroll += (float) (verticalAmount * 30.0);
            scroll = Mth.clamp(scroll, maxScroll, 0f);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        boolean ctrl = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

        if (focused != null) {
            switch (key) {
                case GLFW.GLFW_KEY_ESCAPE -> {
                    if (focused == inlineField) closeInline();
                    focused = null;
                }
                case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                    if (focused == inlineField) submitInline();
                    else if (focused == chatField) sendChatBar();
                    else addBot();
                }
                case GLFW.GLFW_KEY_TAB -> {
                    if (focused == nickField) focused = ipField;
                    else if (focused == ipField) focused = passField;
                    else if (focused == passField) focused = nickField;
                }
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    if (ctrl) focused.set("");
                    else focused.backspace();
                }
                case GLFW.GLFW_KEY_V -> {
                    if (ctrl) {
                        String clip = GLFW.glfwGetClipboardString(mc.getWindow().handle());
                        if (clip != null) focused.type(clip.trim());
                    }
                }
                default -> {
                }
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (adding) {
                adding = false;
            } else {
                onClose();
            }
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (focused != null) {
            focused.type(Character.toString(input.codepoint()));
            return true;
        }
        return super.charTyped(input);
    }


    private static String displayName(BotSession session) {
        return session == BotSessionManager.getInstance().getMainSession() ? mc.getUser().getName() : session.getName();
    }

    private static String listSubtitle(BotSession session) {
        BotSessionManager manager = BotSessionManager.getInstance();
        if (manager.isForeground(session)) return "Ты играешь";
        if (session == manager.getMainSession()) return "основной аккаунт";
        return stateText(session);
    }

    private static String serverOf(BotSession session) {
        BotSessionManager manager = BotSessionManager.getInstance();
        if (session != manager.getMainSession()) {
            String address = session.getServerAddress();
            return address == null || address.isBlank() ? "—" : address;
        }
        if (mc.getCurrentServer() != null) return mc.getCurrentServer().ip;
        return mc.level != null && manager.isForeground(session) ? "одиночный мир" : "—";
    }

    private static Color stateColor(BotSession session) {
        BotSessionManager manager = BotSessionManager.getInstance();
        if (session == manager.getMainSession()) {
            return mc.level != null || session.isActive() ? GREEN : DIM;
        }
        return switch (session.getState()) {
            case CONNECTED -> GREEN;
            case CONNECTING -> new Color(255, 200, 70);
            case ERROR -> RED;
            default -> DIM;
        };
    }

    private static String stateText(BotSession session) {
        return switch (session.getState()) {
            case CONNECTED -> session.isPlayable() ? "онлайн " + formatUptime(session.getUptime()) : "загрузка";
            case CONNECTING -> "подключение";
            case ERROR -> "ошибка";
            default -> "оффлайн";
        };
    }

    private static String formatUptime(long ms) {
        long seconds = ms / 1000L;
        long hours = seconds / 3600L;
        long minutes = (seconds / 60L) % 60L;
        return hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds % 60L)
                : String.format("%d:%02d", minutes, seconds % 60L);
    }

    private static int ping(LocalPlayer player) {
        PlayerInfo info = player.connection.getPlayerInfo(player.getUUID());
        return info != null ? info.getLatency() : 0;
    }

    private static Color lighten(Color color, int amount) {
        return new Color(Math.min(255, color.getRed() + amount), Math.min(255, color.getGreen() + amount),
                Math.min(255, color.getBlue() + amount), color.getAlpha());
    }

    private static String fit(Font font, String text, float size, float maxWidth) {
        if (text == null) return "";
        if (font.getWidth(text, size) <= maxWidth) return text;
        String cut = text;
        while (cut.length() > 1 && font.getWidth(cut + "…", size) > maxWidth) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "…";
    }

    private record Hit(float x, float y, float w, float h, Runnable action) {
    }

    private record ItemDraw(ItemStack stack, int x, int y) {
    }

    private enum InlineMode {
        IP, PASSWORD
    }

    private enum ViewMode {
        INVENTORY, SCREEN, CHAT
    }

    private static final class Field {
        private final StringBuilder text = new StringBuilder();
        private final int maxLength;
        private final IntPredicate filter;
        private String placeholder;
        private boolean masked;

        private Field(String placeholder, int maxLength, IntPredicate filter, boolean masked) {
            this.placeholder = placeholder;
            this.maxLength = maxLength;
            this.filter = filter;
            this.masked = masked;
        }

        private void type(String input) {
            input.codePoints().forEach(c -> {
                if (text.length() < maxLength && filter.test(c)) {
                    text.appendCodePoint(c);
                }
            });
        }

        private void backspace() {
            if (!text.isEmpty()) {
                text.deleteCharAt(text.length() - 1);
            }
        }

        private void set(String value) {
            text.setLength(0);
            type(value);
        }

        private String display() {
            return masked ? "*".repeat(text.length()) : text.toString();
        }
    }
}
