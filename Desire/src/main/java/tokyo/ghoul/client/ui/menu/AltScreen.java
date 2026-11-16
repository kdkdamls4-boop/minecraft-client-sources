package tokyo.ghoul.client.ui.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import tokyo.ghoul.api.utils.color.UIColors;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.ScissorUtil;
import tokyo.ghoul.api.utils.render.fonts.Fonts;
import tokyo.ghoul.client.account.AccountSwitcher;
import tokyo.ghoul.client.services.RenderService;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;
import tokyo.ghoul.client.ui.theme.ThemeEditor;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AltScreen extends Screen {
    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final float COL_W = 200f;
    private static final float PANEL_PAD = 7f;
    private static final float INNER_W = COL_W - PANEL_PAD * 2f;
    private static final float HEADER_H = 84f;
    private static final float PANEL_RADIUS = 8f;
    private static final float PLATE_RADIUS = 5f;

    private static final float FIELD_H = 18f;
    private static final float SORT_W = 56f;
    private static final float NICK_W = 78f;
    private static final float ADD_W = 46f;
    private static final float SECTION_GAP = 6f;
    private static final float SEARCH_TEXT_INSET = 17f;
    private static final float PANEL_FIXED_H = PANEL_PAD + FIELD_H + SECTION_GAP + SECTION_GAP
            + FIELD_H + 5f + FIELD_H + PANEL_PAD;

    private static final float ROW_H = 26f;
    private static final float ROW_GAP = 4f;
    private static final float ROW_STEP = ROW_H + ROW_GAP;
    private static final float LIST_MAX_H = ROW_STEP * 5f - ROW_GAP;

    private static final float CHIP_W = 56f;
    private static final float CORNER_MARGIN = 14f;

    private static final float DRAG_THRESHOLD = 3f;

    private static final Color TEXT = new Color(245, 245, 249);
    private static final Color TEXT_DIM = new Color(150, 156, 172);
    private static final Color ICON = new Color(216, 216, 230);

    private static final int B_ADD = 0, B_GENERATE = 1, B_MICROSOFT = 2, B_SORT = 3, B_CLEAR = 4, B_BACK = 5;
    private static final String[] SORT_NAMES = {"Custom", "Name", "Date"};

    private static final String[] RANDOM_PREFIXES = {
            "Desire", "Ghost", "Shadow", "Night", "Cyber", "Void", "Static", "Aura",
            "Frost", "Eclipse", "Tokyo", "Ghoul", "Vortex", "Apex", "Nexus", "Storm"
    };

    private static volatile float activeOpenProgress;
    private static volatile boolean active;

    private final Screen parent;
    private final List<AltEntry> entries = new ArrayList<>();
    private final List<AltEntry> view = new ArrayList<>();
    private AltEntry selected;

    private float openProgress;
    private float uiProgress;
    private boolean closing;
    private long lastFrame = System.nanoTime();
    private final float[] btnHover = new float[6];
    private final float[] btnFlash = new float[6];

    private float colX, colY;
    private float panelX, panelY, panelH;
    private float listH = LIST_MAX_H;
    private float screenW, screenH;

    private float scroll;
    private float scrollTarget;
    private String inputText = "";
    private boolean inputFocused;
    private String searchText = "";
    private boolean searchFocused;
    private int sortMode;

    private float fieldShake;
    private float shakePhase;
    private float toastAnim;

    private int pressIndex = -1;
    private float pressY;
    private boolean dragging;
    private int dragIndex = -1;
    private int dragTarget = -1;
    private float dragMouseY;
    private float dragGrabOffset;

    private String statusMessage = "";
    private float statusTime;

    private float switchProgress = 1f;
    private String switchFrom = "";
    private String switchTo = "";

    public static final class AltEntry {
        public String name;
        public boolean favorite;
        public UUID uuid;
        public String date;
        public float hover;
        public float activeGlow;
        public float switchPulse;
        public float y;
        public boolean yInit;

        public AltEntry(String name, boolean favorite, String date) {
            this.name = name;
            this.favorite = favorite;
            this.date = date;
            this.uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }
    }

    public AltScreen(Screen parent) {
        super(Component.literal("Alt Manager"));
        this.parent = parent;
        load();
        rebuildView();
    }

    public static boolean isActive() {
        return active && Minecraft.getInstance().gui.screen() instanceof AltScreen;
    }

    public static float openProgress() {
        return activeOpenProgress;
    }

    public static void renderBackdropLayer() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof AltScreen screen)) return;
        RenderService.setGuiContext(true);
        try {
            screen.drawBackdrop();
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    public static void renderInterfaceLayer() {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.gui.screen() instanceof AltScreen screen)) return;
        RenderService.setGuiContext(true);
        try {
            screen.drawInterface();
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    @Override
    protected void init() {
        ThemeEditor.getInstance().init();
        openProgress = 0f;
        uiProgress = 0f;
        closing = false;
        lastFrame = System.nanoTime();
        active = true;
        switchProgress = 1f;
        switchFrom = "";
        switchTo = "";
        MenuWallpapers.warmTextures();
    }

    @Override
    public void removed() {
        active = false;
        super.removed();
    }

    @Override
    public void tick() {
        super.tick();
        if (closing && uiProgress < 0.04f) {
            client().gui.setScreen(parent != null ? parent : new MainMenuScreen());
        }
    }

    private void beginClose() {
        if (closing) return;
        closing = true;
        inputFocused = false;
        searchFocused = false;
        dragging = false;
        dragIndex = -1;
        pressIndex = -1;
        if (statusTime > 0f) statusTime = Math.min(statusTime, 0.25f);
    }

    private Minecraft client() {
        return Minecraft.getInstance();
    }

    private int fixedW() {
        return (int) Math.ceil(client().getWindow().getWidth() / (double) FIXED_GUI_SCALE);
    }

    private int fixedH() {
        return (int) Math.ceil(client().getWindow().getHeight() / (double) FIXED_GUI_SCALE);
    }

    private float toFixed(double c) {
        return (float) (c * client().getWindow().getGuiScale() / FIXED_GUI_SCALE);
    }

    private float toGui(float fixed) {
        return fixed * FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
    }

    private static Color accent() {
        return UIColors.primary();
    }

    private static Color withA(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float ease(float current, float target, float speed, float dt) {
        return current + (target - current) * Math.min(1f, speed * dt);
    }

    private static float smoothstep(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static Color mixColor(Color from, Color to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(Mth.lerp(t, from.getRed(), to.getRed())),
                Math.round(Mth.lerp(t, from.getGreen(), to.getGreen())),
                Math.round(Mth.lerp(t, from.getBlue(), to.getBlue())),
                Math.round(Mth.lerp(t, from.getAlpha(), to.getAlpha())));
    }

    private String displayedAccountName() {
        if (switchProgress < 0.5f) return switchFrom;
        if (switchProgress < 1f) return switchTo;
        return client().getUser().getName();
    }

    private float reveal() {
        float t = Math.max(0f, Math.min(1f, uiProgress));
        return t * t * (3f - 2f * t);
    }

    private float ui() {
        return Math.max(0f, Math.min(1f, uiProgress));
    }

    private void load() {
        entries.clear();
        JsonArray array = MenuConfigStore.getArray("alts");
        String selectedName = MenuConfigStore.getString("selectedAccount", "");
        if (array != null) {
            for (JsonElement element : array) {
                if (!element.isJsonObject()) continue;
                JsonObject object = element.getAsJsonObject();
                String name = object.has("name") ? object.get("name").getAsString() : "";
                if (!name.matches("^[a-zA-Z0-9_]{1,16}$")) continue;
                AltEntry entry = new AltEntry(name,
                        object.has("favorite") && object.get("favorite").getAsBoolean(),
                        object.has("date") ? object.get("date").getAsString() : "");
                entries.add(entry);
                if (name.equalsIgnoreCase(selectedName)) selected = entry;
            }
        }
        if (selected == null && !entries.isEmpty()) selected = entries.get(0);
    }

    private void save() {
        JsonArray array = new JsonArray();
        for (AltEntry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("name", entry.name);
            object.addProperty("favorite", entry.favorite);
            object.addProperty("date", entry.date);
            array.add(object);
        }
        String selectedName = selected == null ? "" : selected.name;
        MenuConfigStore.save(data -> {
            data.add("alts", array);
            data.addProperty("selectedAccount", selectedName);
        });
    }


    private boolean canReorder() {
        return sortMode == 0 && searchText.isEmpty();
    }

    private void rebuildView() {
        view.clear();
        String query = searchText.trim().toLowerCase();
        for (AltEntry entry : entries) {
            if (query.isEmpty() || entry.name.toLowerCase().contains(query)) view.add(entry);
        }
        if (sortMode == 1) {
            view.sort(Comparator.comparing(e -> e.name.toLowerCase()));
        } else if (sortMode == 2) {
            view.sort(Comparator.comparing(AltScreen::dateKey).reversed());
        }
        view.sort(Comparator.comparing(e -> !e.favorite));
    }

    private int groupStart(boolean favorite) {
        if (favorite) return 0;
        int pinned = 0;
        for (AltEntry entry : view) if (entry.favorite) pinned++;
        return pinned;
    }

    private int groupEnd(boolean favorite) {
        if (!favorite) return view.size() - 1;
        return groupStart(false) - 1;
    }

    private static String dateKey(AltEntry entry) {
        String d = entry.date;
        if (d == null || d.length() != 10) return "";
        return d.substring(6, 10) + d.substring(3, 5) + d.substring(0, 2);
    }


    private void layout(float sw, float sh) {
        screenW = sw;
        screenH = sh;
        float available = sh - HEADER_H - PANEL_FIXED_H - 40f;
        listH = Mth.clamp(available, ROW_STEP * 2f - ROW_GAP, LIST_MAX_H);
        panelH = PANEL_FIXED_H + listH;

        float totalH = HEADER_H + panelH;
        float drop = (1f - reveal()) * 18f;
        colX = (sw - COL_W) / 2f;
        colY = (sh - totalH) / 2f + drop;
        panelX = colX;
        panelY = colY + HEADER_H;
    }

    private float searchX() {
        return panelX + PANEL_PAD;
    }

    private float searchY() {
        return panelY + PANEL_PAD;
    }

    private float searchW() {
        return INNER_W - SORT_W - 5f;
    }

    private float sortX() {
        return searchX() + searchW() + 5f;
    }

    private float listX() {
        return panelX + PANEL_PAD;
    }

    private float listY() {
        return searchY() + FIELD_H + SECTION_GAP;
    }

    private float listW() {
        return INNER_W;
    }

    private float nickY() {
        return listY() + listH + SECTION_GAP;
    }

    private float nickX() {
        return panelX + PANEL_PAD;
    }

    private float addX() {
        return nickX() + NICK_W + 5f;
    }

    private float genX() {
        return addX() + ADD_W + 5f;
    }

    private float genW() {
        return INNER_W - NICK_W - ADD_W - 10f;
    }

    private float msY() {
        return nickY() + FIELD_H + 5f;
    }

    private float backX() {
        return screenW - CORNER_MARGIN - CHIP_W;
    }

    private float clearX() {
        return backX() - 5f - CHIP_W;
    }

    private float chipY() {
        return screenH - CORNER_MARGIN - FIELD_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        RenderService.setGuiContext(true);
        try {
            super.extractRenderState(context, mouseX, mouseY, delta);

            layout(fixedW(), fixedH());

            active = true;
            activeOpenProgress = openProgress;
        } finally {
            RenderService.setGuiContext(false);
        }
    }

    private void drawBackdrop() {
        var matrices = RenderUtil.matrices();
        matrices.pushPose();
        float s = FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
        matrices.scale(s, s, 1f);
        drawWallpaper(fixedW(), fixedH());
        matrices.popPose();
    }

    private void drawInterface() {
        long now = System.nanoTime();
        float dt = Math.min((now - lastFrame) / 1.0E9f, 0.1f);
        lastFrame = now;

        openProgress = ease(openProgress, closing ? 0f : 1f, closing ? 14f : 12f, dt);
        uiProgress = ease(uiProgress, closing ? 0f : 1f, closing ? 16f : 10f, dt);
        switchProgress = ease(switchProgress, 1f, 6.5f, dt);
        if (switchProgress > 0.9995f) switchProgress = 1f;

        activeOpenProgress = openProgress;

        if (statusTime > 0f) {
            statusTime = Math.max(0f, statusTime - dt);
        }
        toastAnim = ease(toastAnim, statusTime > 0f ? 1f : 0f, 12f, dt);
        fieldShake = ease(fieldShake, 0f, 5.5f, dt);
        shakePhase += dt * 60f;

        float sw = fixedW();
        float sh = fixedH();
        float mx = toFixed(client().mouseHandler.xpos());
        float my = toFixed(client().mouseHandler.ypos());
        layout(sw, sh);
        rebuildView();

        var matrices = RenderUtil.matrices();
        matrices.pushPose();
        float s = FIXED_GUI_SCALE / (float) client().getWindow().getGuiScale();
        matrices.scale(s, s, 1f);

        drawHeader(sw);
        drawPanel(mx, my, dt);
        drawCorners(mx, my, dt);
        drawToast(sw);

        matrices.popPose();
    }

    private void drawWallpaper(float sw, float sh) {
        MenuBackdrop.draw(RenderUtil.matrices(), sw, sh, 255f);
    }

    private void drawHeader(float sw) {
        var matrices = RenderUtil.matrices();
        int a = (int) (255 * ui());
        float cx = sw / 2f;

        float logoSize = 36f;
        ClickGuiIcons.drawGradientLogo(cx - logoSize / 2f, colY, logoSize, a);

        String title = "Desire Client";
        float titleSize = 8.5f;
        float titleW = Fonts.DREAMLINE_SF.getWidth(title, titleSize);
        Fonts.DREAMLINE_SF.drawCenteredGradientText(matrices, title, cx, colY + 42f, titleSize,
                UIColors.primary(a), UIColors.secondary(a), Math.max(1.1f, titleW / 4f));

        String sub = "Alt Manager";
        float subW = Fonts.DREAMLINE_SF.getWidth(sub, 5.5f);
        Fonts.DREAMLINE_SF.drawText(matrices, sub, cx - subW / 2f, colY + 54f, 5.5f, withA(TEXT_DIM, a));

        String heading = "Accounts" + (entries.isEmpty() ? "" : " (" + entries.size() + ")");
        float headingW = Fonts.DREAMLINE_SF.getWidth(heading, 7f);
        float headingX = cx - headingW / 2f;
        Fonts.DREAMLINE_SF.drawText(matrices, heading, headingX, colY + 68f, 7f, withA(TEXT, a));
        drawIcon("n", headingX - 4f, colY + 68.5f, 7f, withA(accent(), a));
    }

    private float hoverOf(int index, boolean hovered, float dt) {
        btnHover[index] += ((hovered ? 1f : 0f) - btnHover[index]) * Math.min(1f, dt * 14f);
        btnFlash[index] = ease(btnFlash[index], 0f, 6f, dt);
        return btnHover[index];
    }

    private void drawPanel(float mx, float my, float dt) {
        var matrices = RenderUtil.matrices();
        int a = (int) (255 * ui());

        drawGlass(panelX, panelY, COL_W, panelH, PANEL_RADIUS, 0f, 0f);

        drawField(searchX(), searchY(), searchW(), searchFocused, 0f);
        String searchShown = searchText.isEmpty() && !searchFocused ? "Search..." : searchText;
        Color searchCol = searchText.isEmpty() ? new Color(125, 130, 145) : TEXT;
        float searchIcon = 7f;
        MenuIcons.draw(searchX() + 6f, searchY() + (FIELD_H - searchIcon) / 2f, searchIcon, searchIcon,
                MenuIcons.ACCOUNT_SEARCH, withA(searchFocused ? accent() : TEXT_DIM, a));
        float searchTextX = searchX() + SEARCH_TEXT_INSET;
        Fonts.DREAMLINE_SF.drawText(matrices, searchShown, searchTextX, searchY() + textOffset(6f), 6f,
                withA(searchCol, a));
        drawCaret(searchFocused, searchText, searchTextX - 7f, searchY(), a);

        float sh = hoverOf(B_SORT, inside(mx, my, sortX(), searchY(), SORT_W, FIELD_H), dt);
        drawPlate(sortX(), searchY() - sh, SORT_W, FIELD_H, PLATE_RADIUS, sh, btnFlash[B_SORT] * 30f);
        drawIconLabel(MenuIcons.ACCOUNT_SORT, SORT_NAMES[sortMode], sortX() + SORT_W / 2f, searchY() - sh, sh, a);

        drawList(mx, my, dt, a);

        float shakeX = (float) Math.sin(shakePhase) * 3.5f * fieldShake;
        float nickX = nickX() + shakeX;
        drawField(nickX, nickY(), NICK_W, inputFocused, 0f);
        String nickShown = inputText.isEmpty() && !inputFocused ? "Nick..." : inputText;
        Color nickCol = inputText.isEmpty() ? new Color(125, 130, 145) : TEXT;
        Fonts.DREAMLINE_SF.drawText(matrices, nickShown, nickX + 7f, nickY() + textOffset(6f), 6f, withA(nickCol, a));
        drawCaret(inputFocused, inputText, nickX, nickY(), a);

        drawTextButton(B_ADD, addX(), nickY(), ADD_W, MenuIcons.ACCOUNT_PLUS, "Add", mx, my, dt, a, 22f);
        drawTextButton(B_GENERATE, genX(), nickY(), genW(), MenuIcons.DICES, "Generate", mx, my, dt, a, 0f);

        float msH = hoverOf(B_MICROSOFT, inside(mx, my, listX(), msY(), INNER_W, FIELD_H), dt);
        float msYy = msY() - msH;
        drawPlate(listX(), msYy, INNER_W, FIELD_H, PLATE_RADIUS, msH, btnFlash[B_MICROSOFT] * 30f);
        String ms = "Microsoft login";
        float msW = Fonts.DREAMLINE_SF.getWidth(ms, 6f);
        float msTextX = listX() + (INNER_W - msW) / 2f + 5f;
        MenuIcons.draw(msTextX - 12f, msYy + (FIELD_H - 8f) / 2f, 8f, 8f, MenuIcons.ACCOUNT_MICROSOFT,
                withA(Color.WHITE, a));
        Fonts.DREAMLINE_SF.drawText(matrices, ms, msTextX, msYy + textOffset(6f), 6f, withA(TEXT, a));
    }

    private static float textOffset(float size) {
        return (FIELD_H - size) / 2f - 0.6f;
    }

    private void drawField(float x, float y, float w, boolean focused, float hover) {
        drawPlate(x, y, w, FIELD_H, PLATE_RADIUS, focused ? 0.6f : hover, focused ? 22f : 0f);
    }

    private void drawCaret(boolean focused, String text, float x, float y, int a) {
        if (!focused || System.currentTimeMillis() % 1000 >= 500) return;
        float tw = Fonts.DREAMLINE_SF.getWidth(text, 6f);
        RenderUtil.RECT.draw(RenderUtil.matrices(), x + 7.5f + tw, y + 4f, 1f, 10f, 0f, withA(accent(), a));
    }

    private void drawTextButton(int index, float x, float y, float w, Identifier icon, String text,
                                float mx, float my, float dt, int a, float accentAlpha) {
        float h = hoverOf(index, inside(mx, my, x, y, w, FIELD_H), dt);
        float by = y - h + btnFlash[index];
        drawPlate(x, by, w, FIELD_H, PLATE_RADIUS, h, accentAlpha + h * 14f + btnFlash[index] * 40f);
        drawIconLabel(icon, text, x + w / 2f, by, h, a);
    }

    private void drawIconLabel(Identifier icon, String text, float cx, float y, float hover, int a) {
        float size = 6f, iconSize = 7f, gap = 3.5f;
        float textW = Fonts.DREAMLINE_SF.getWidth(text, size);
        float startX = cx - (iconSize + gap + textW) / 2f;
        MenuIcons.draw(startX, y + (FIELD_H - iconSize) / 2f, iconSize, iconSize, icon,
                withA(mixColor(ICON, accent(), hover), a));
        Fonts.DREAMLINE_SF.drawText(RenderUtil.matrices(), text, startX + iconSize + gap, y + textOffset(size), size,
                withA(TEXT, a));
    }

    private void drawCorners(float mx, float my, float dt) {
        var matrices = RenderUtil.matrices();
        int a = (int) (255 * ui());
        float lift = (1f - reveal()) * 8f;

        String name = displayedAccountName();
        float fade = switchProgress < 1f ? Math.abs(1f - 2f * switchProgress) : 1f;
        float avatar = 16f;
        float nameW = Fonts.DREAMLINE_SF.getWidth(name, 6.5f);
        float chipW = 8f + avatar + 6f + Math.max(nameW, 40f) + 10f;
        float cx = CORNER_MARGIN;
        float cy = screenH - CORNER_MARGIN - 26f + lift;
        drawPlate(cx, cy, chipW, 26f, PLATE_RADIUS, 0f, 0f);
        RenderUtil.TEXTURE_RECT.draw(matrices, cx + 5f, cy + 5f, avatar, avatar, 4f,
                withA(Color.WHITE, a), 0f, 0f, 1f, 1f, MenuIcons.AVATAR);
        float tx = cx + 5f + avatar + 6f;
        Fonts.DREAMLINE_SF.drawText(matrices, name, tx, cy + 5f, 6.5f, withA(TEXT, (int) (a * fade)));
        Fonts.DREAMLINE_SF.drawText(matrices, "Active", tx, cy + 15f, 5f, withA(accent(), (int) (a * fade)));

        drawTextButton(B_CLEAR, clearX(), chipY() + lift, CHIP_W, MenuIcons.ACCOUNT_TRASH, "Clear All", mx, my, dt, a, 0f);
        drawTextButton(B_BACK, backX(), chipY() + lift, CHIP_W, MenuIcons.ACCOUNT_BACK, "Back", mx, my, dt, a, 0f);
    }

    private void drawIcon(String glyph, float rightX, float y, float size, Color color) {
        float width = Fonts.DREAMLINE_ICONS.getWidth(glyph, size);
        Fonts.DREAMLINE_ICONS.drawText(RenderUtil.matrices(), glyph, rightX - width, y, size, color);
    }

    private void drawPlate(float x, float y, float w, float h, float radius, float hover, float accentAlpha) {
        float u = ui();
        var matrices = RenderUtil.matrices();

        float inv = (float) client().getWindow().getGuiScale() / FIXED_GUI_SCALE;
        matrices.pushPose();
        matrices.scale(inv, inv, 1f);
        RenderUtil.BLUR_RECT.draw(matrices, toGui(x), toGui(y), toGui(w), toGui(h), toGui(radius),
                new Color(255, 255, 255, (int) (255 * u)), 0f);
        matrices.popPose();

        float hv = Math.max(0f, Math.min(1f, hover));
        RenderUtil.RECT.draw(matrices, x, y, w, h, radius,
                new Color(255, 255, 255, (int) ((10f + 14f * hv) * u)));
        if (accentAlpha > 0.01f) {
            RenderUtil.RECT.draw(matrices, x, y, w, h, radius, withA(accent(), (int) (accentAlpha * u)));
        }
    }

    private void drawGlass(float x, float y, float w, float h, float radius, float hover, float accentAlpha) {
        float u = ui();
        var matrices = RenderUtil.matrices();

        RenderUtil.RECT.drawGlassShadow(matrices, x, y, w, h, radius, 6f,
                new Color(0, 0, 0, (int) (96 * u)));

        float inv = (float) client().getWindow().getGuiScale() / FIXED_GUI_SCALE;
        matrices.pushPose();
        matrices.scale(inv, inv, 1f);
        RenderUtil.BLUR_RECT.draw(matrices, toGui(x), toGui(y), toGui(w), toGui(h), toGui(radius),
                new Color(255, 255, 255, (int) (255 * u)), 0f);
        matrices.popPose();

        RenderUtil.RECT.draw(matrices, x, y, w, h, radius,
                new Color(14, 14, 16, (int) ((120f - hover * 18f) * u)));
        if (accentAlpha > 0.01f) {
            RenderUtil.RECT.draw(matrices, x, y, w, h, radius, withA(accent(), (int) (accentAlpha * u)));
        }
    }

    private void drawList(float mx, float my, float dt, int a) {
        var matrices = RenderUtil.matrices();
        float listX = listX();
        float listY = listY();
        float listW = listW();

        if (view.isEmpty()) {
            String t1 = entries.isEmpty() ? "No accounts added yet" : "Nothing found";
            String t2 = entries.isEmpty() ? "Type a nickname below to add one" : "Try another search";
            float w1 = Fonts.DREAMLINE_SF.getWidth(t1, 6.5f);
            float w2 = Fonts.DREAMLINE_SF.getWidth(t2, 5.5f);
            Fonts.DREAMLINE_SF.drawText(matrices, t1, listX + (listW - w1) / 2f,
                    listY + listH / 2f - 9f, 6.5f, withA(TEXT_DIM, a));
            Fonts.DREAMLINE_SF.drawText(matrices, t2, listX + (listW - w2) / 2f,
                    listY + listH / 2f + 4f, 5.5f, withA(new Color(110, 116, 130), a));
            return;
        }

        float contentH = view.size() * ROW_STEP - ROW_GAP;
        float maxScroll = Math.min(0f, listH - contentH);
        scrollTarget = Mth.clamp(scrollTarget, maxScroll, 0f);
        scroll = ease(scroll, scrollTarget, 14f, dt);

        String activeName = displayedAccountName();
        for (int i = 0; i < view.size(); i++) {
            AltEntry entry = view.get(i);
            entry.activeGlow = ease(entry.activeGlow, activeName.equalsIgnoreCase(entry.name) ? 1f : 0f, 9f, dt);
            entry.switchPulse = ease(entry.switchPulse, 0f, 6.5f, dt);
            float slot = listY + i * ROW_STEP + scroll;
            if (!entry.yInit) {
                entry.y = slot + 26f + i * 5f;
                entry.yInit = true;
            }
            if (dragging && i == dragIndex) {
                entry.y = dragMouseY - dragGrabOffset;
                continue;
            }
            float shift = 0f;
            if (dragging) {
                if (dragIndex < i && i <= dragTarget) shift = -ROW_STEP;
                else if (dragTarget <= i && i < dragIndex) shift = ROW_STEP;
            }
            entry.y = ease(entry.y, slot + shift, 17f, dt);
        }

        ScissorUtil.start(matrices, toGui(listX), toGui(listY), toGui(listW), toGui(listH));
        for (int i = 0; i < view.size(); i++) {
            if (dragging && i == dragIndex) continue;
            AltEntry entry = view.get(i);
            float ry = entry.y;
            boolean visible = ry + ROW_H >= listY && ry <= listY + listH;
            if (!visible) continue;
            drawAccountRow(entry, listX, ry, listW, false, true, mx, my, dt, a);
        }
        ScissorUtil.stop(matrices);

        if (dragging && dragIndex >= 0 && dragIndex < view.size()) {
            AltEntry entry = view.get(dragIndex);
            drawAccountRow(entry, listX, entry.y, listW, true, false, mx, my, dt, a);
        }

        if (contentH > listH) {
            float barX = panelX + COL_W - 3.5f;
            float barY = listY;
            float thumbH = Math.max(16f, listH * (listH / contentH));
            float scrollRatio = -scroll / (contentH - listH);
            float thumbY = barY + scrollRatio * (listH - thumbH);
            RenderUtil.RECT.draw(matrices, barX, barY, 1.5f, listH, 0.75f, new Color(255, 255, 255, 10));
            RenderUtil.RECT.draw(matrices, barX, thumbY, 1.5f, thumbH, 0.75f,
                    new Color(255, 255, 255, (int) (60 * ui())));
        }
    }

    private void drawAccountRow(AltEntry entry, float rx, float ry, float rw,
                                boolean dragged, boolean visible, float mx, float my, float dt, int a) {
        var matrices = RenderUtil.matrices();
        boolean hov = !dragged && visible && inListBox(mx, my) && inside(mx, my, rx, ry, rw, ROW_H);
        entry.hover += (((hov || dragged) ? 1f : 0f) - entry.hover) * Math.min(1f, dt * 14f);
        float h = entry.hover;
        float y = dragged ? ry : ry - h * 1f;

        float glow = Math.max(0f, Math.min(1f, entry.activeGlow));
        float pulse = Math.max(0f, Math.min(1f, entry.switchPulse));
        boolean isSelected = selected == entry;

        if (dragged) {
            drawPlate(rx - 2f, y - 1.5f, rw + 4f, ROW_H + 3f, PLATE_RADIUS + 1f, 1f, 46f);
        } else {
            drawPlate(rx, y, rw, ROW_H, PLATE_RADIUS, h,
                    10f * (isSelected ? 1f : 0f) + (22f + h * 14f) * glow + pulse * 22f);
        }

        float avatarSize = 18f;
        float avatarX = rx + 5f;
        float avatarY = y + (ROW_H - avatarSize) / 2f;
        if (pulse > 0.01f) {
            RenderUtil.RECT.draw(matrices, avatarX - 2.5f, avatarY - 2.5f, avatarSize + 5f, avatarSize + 5f, 6.5f,
                    withA(accent(), (int) (pulse * 70f * ui())));
        }
        RenderUtil.TEXTURE_RECT.draw(matrices, avatarX, avatarY, avatarSize, avatarSize, 4f,
                Color.WHITE, 0f, 0f, 1f, 1f, MenuIcons.AVATAR);

        float nameX = avatarX + avatarSize + 6f;
        float nameLight = Math.max(isSelected ? 0.6f : 0f, glow);
        Color nameCol = mixColor(new Color(226, 230, 240), Color.WHITE, nameLight);
        if (pulse > 0.01f) nameCol = mixColor(nameCol, accent(), pulse * 0.35f);
        Fonts.DREAMLINE_SF.drawText(matrices, entry.name, nameX, y + 5f, 6.5f, withA(nameCol, a));

        String dateStr = entry.date != null && !entry.date.isEmpty() ? entry.date : "Offline";
        float dateAlpha = 1f - Math.min(1f, glow * 2f);
        float activeAlpha = Math.max(0f, glow * 2f - 1f);
        if (dateAlpha > 0.01f) {
            Fonts.DREAMLINE_SF.drawText(matrices, dateStr, nameX, y + 15f, 5f,
                    withA(new Color(132, 138, 152), (int) (a * dateAlpha)));
        }
        if (activeAlpha > 0.01f) {
            Fonts.DREAMLINE_SF.drawText(matrices, "• Active Account", nameX, y + 15f, 5f,
                    withA(accent(), (int) (a * activeAlpha)));
        }

        float iconFade = 0.45f + 0.55f * h;
        float iconY = y + (ROW_H - 10f) / 2f;
        float starX = rx + rw - 42f;
        float copyX = rx + rw - 27f;
        float delX = rx + rw - 14f;

        boolean starHov = !dragged && inside(mx, my, starX - 2f, iconY - 2f, 14f, 14f);
        boolean copyHov = !dragged && inside(mx, my, copyX - 2f, iconY - 2f, 14f, 14f);
        boolean delHov = !dragged && inside(mx, my, delX - 2f, iconY - 2f, 14f, 14f);

        Color starCol = entry.favorite
                ? new Color(255, 215, 60, a)
                : withA(TEXT_DIM, (int) (a * iconFade * (starHov ? 1.1f : 1f)));
        MenuIcons.draw(starX, iconY, 10f, 10f,
                entry.favorite ? MenuIcons.ACCOUNT_PINNED : MenuIcons.ACCOUNT_PUSH_PIN, starCol);

        Color copyCol = withA(new Color(170, 190, 224), (int) (a * iconFade * (copyHov ? 1.3f : 1f)));
        MenuIcons.draw(copyX, iconY, 10f, 10f, MenuIcons.COPY_NICK, copyCol);

        Color delCol = withA(delHov ? new Color(255, 92, 102) : new Color(168, 173, 184),
                (int) (a * iconFade * (delHov ? 1.1f : 1f)));
        MenuIcons.draw(delX, iconY, 10f, 10f, MenuIcons.ACCOUNT_TRASH, delCol);
    }

    private void drawToast(float sw) {
        if (toastAnim < 0.02f) return;
        var matrices = RenderUtil.matrices();
        float alpha = toastAnim * ui();
        int a = (int) (Math.max(0f, Math.min(1f, alpha)) * 255);
        float textW = Fonts.DREAMLINE_SF.getWidth(statusMessage, 6f);
        float sx = (sw - textW) / 2f;
        float sy = panelY + panelH + 11f + (1f - toastAnim) * 8f;

        drawPlate(sx - 10f, sy - 4.5f, textW + 20f, 18f, PLATE_RADIUS, 0f, 30f * alpha);
        Fonts.DREAMLINE_SF.drawText(matrices, statusMessage, sx, sy, 6f, withA(TEXT, a));
    }

    private int rowAt(float mx, float my) {
        if (!inListBox(mx, my)) return -1;
        float listX = listX();
        float listY = listY();
        for (int i = 0; i < view.size(); i++) {
            AltEntry entry = view.get(i);
            float ry = entry.yInit ? entry.y : listY + i * ROW_STEP + scroll;
            if (ry + ROW_H < listY || ry > listY + listH) continue;
            if (inside(mx, my, listX, ry, listW(), ROW_H)) return i;
        }
        return -1;
    }

    private boolean inListBox(float mx, float my) {
        return mx >= listX() && mx <= listX() + listW()
                && my >= listY() && my <= listY() + listH;
    }

    private void resetDrag() {
        dragging = false;
        dragIndex = -1;
        dragTarget = -1;
        pressIndex = -1;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseClicked(event, doubled);
        if (closing) return true;
        float mx = toFixed(event.x());
        float my = toFixed(event.y());

        boolean inSearch = inside(mx, my, searchX(), searchY(), searchW(), FIELD_H);
        boolean inNick = inside(mx, my, nickX(), nickY(), NICK_W, FIELD_H);
        searchFocused = inSearch;
        inputFocused = inNick;
        if (inSearch || inNick) return true;

        if (inside(mx, my, sortX(), searchY(), SORT_W, FIELD_H)) {
            btnFlash[B_SORT] = 1f;
            sortMode = (sortMode + 1) % SORT_NAMES.length;
            resetDrag();
            scrollTarget = 0f;
            rebuildView();
            return true;
        }
        if (inside(mx, my, addX(), nickY(), ADD_W, FIELD_H)) {
            btnFlash[B_ADD] = 1f;
            addAccount(inputText);
            return true;
        }
        if (inside(mx, my, genX(), nickY(), genW(), FIELD_H)) {
            btnFlash[B_GENERATE] = 1f;
            addAccount(generateRandomNick());
            return true;
        }
        if (inside(mx, my, listX(), msY(), INNER_W, FIELD_H)) {
            btnFlash[B_MICROSOFT] = 1f;
            setStatus("Microsoft login is not available yet");
            return true;
        }
        if (inside(mx, my, clearX(), chipY(), CHIP_W, FIELD_H)) {
            btnFlash[B_CLEAR] = 1f;
            if (!entries.isEmpty()) {
                entries.clear();
                selected = null;
                resetDrag();
                save();
                rebuildView();
                setStatus("Cleared account list");
            }
            return true;
        }
        if (inside(mx, my, backX(), chipY(), CHIP_W, FIELD_H)) {
            btnFlash[B_BACK] = 1f;
            beginClose();
            return true;
        }

        int rowIndex = rowAt(mx, my);
        if (rowIndex >= 0) {
            AltEntry entry = view.get(rowIndex);
            float ry = entry.yInit ? entry.y : listY() + rowIndex * ROW_STEP + scroll;
            float iconY = ry + (ROW_H - 10f) / 2f;
            float starX = listX() + listW() - 42f;
            float copyX = listX() + listW() - 27f;
            float delX = listX() + listW() - 14f;

            if (inside(mx, my, starX - 2f, iconY - 2f, 14f, 14f)) {
                entry.favorite = !entry.favorite;
                save();
                setStatus(entry.favorite ? "Pinned " + entry.name : "Unpinned " + entry.name);
                return true;
            }
            if (inside(mx, my, copyX - 2f, iconY - 2f, 14f, 14f)) {
                GLFW.glfwSetClipboardString(client().getWindow().handle(), entry.name);
                setStatus("Copied " + entry.name);
                return true;
            }
            if (inside(mx, my, delX - 2f, iconY - 2f, 14f, 14f)) {
                entries.remove(entry);
                if (selected == entry) selected = entries.isEmpty() ? null : entries.get(0);
                resetDrag();
                save();
                rebuildView();
                setStatus("Removed " + entry.name);
                return true;
            }

            pressIndex = rowIndex;
            pressY = my;
            dragGrabOffset = my - ry;
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (closing) return true;
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseDragged(event, dragX, dragY);
        float my = toFixed(event.y());

        if (pressIndex >= 0 && !dragging && canReorder() && Math.abs(my - pressY) > DRAG_THRESHOLD) {
            dragging = true;
            dragIndex = pressIndex;
            dragTarget = pressIndex;
            dragMouseY = my;
        }
        if (dragging && dragIndex >= 0 && dragIndex < view.size()) {
            dragMouseY = my;
            float center = dragMouseY - dragGrabOffset + ROW_H * 0.5f;
            boolean favorite = view.get(dragIndex).favorite;
            dragTarget = Mth.clamp((int) Math.floor((center - listY() - scroll) / ROW_STEP),
                    groupStart(favorite), groupEnd(favorite));
        }
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (closing) return true;
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return super.mouseReleased(event);
        float mx = toFixed(event.x());
        float my = toFixed(event.y());

        if (dragging) {
            if (dragIndex >= 0 && dragIndex < view.size()
                    && dragTarget >= 0 && dragTarget < view.size() && dragTarget != dragIndex) {
                AltEntry entry = view.get(dragIndex);
                AltEntry anchor = view.get(dragTarget);
                entries.remove(entry);
                int anchorIndex = entries.indexOf(anchor);
                entries.add(dragTarget > dragIndex ? anchorIndex + 1 : anchorIndex, entry);
                save();
                rebuildView();
                setStatus("Moved " + entry.name);
            }
            resetDrag();
            return true;
        }

        if (pressIndex >= 0 && pressIndex < view.size()) {
            AltEntry entry = view.get(pressIndex);
            float ry = entry.yInit ? entry.y : listY() + pressIndex * ROW_STEP + scroll;
            if (inListBox(mx, my) && inside(mx, my, listX(), ry, listW(), ROW_H)) {
                switchAccount(entry);
            }
            pressIndex = -1;
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (closing || dragging) return true;
        scrollTarget += (float) verticalAmount * 24f;
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (closing) return true;
        if (!inputFocused && !searchFocused) return super.charTyped(event);
        char character = (char) event.codepoint();
        if (character >= 32 && character <= 126) {
            String filtered = Character.toString(character).replaceAll("[^a-zA-Z0-9_]", "");
            if (!filtered.isEmpty()) {
                if (searchFocused && searchText.length() < 16) {
                    searchText += filtered;
                    scrollTarget = 0f;
                } else if (inputFocused && inputText.length() < 16) {
                    inputText += filtered;
                }
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (inputFocused || searchFocused) {
            if (input.key() == GLFW.GLFW_KEY_ENTER || input.key() == GLFW.GLFW_KEY_KP_ENTER) {
                if (inputFocused) addAccount(inputText);
                else searchFocused = false;
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_BACKSPACE) {
                if (searchFocused && !searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                } else if (inputFocused && !inputText.isEmpty()) {
                    inputText = inputText.substring(0, inputText.length() - 1);
                }
                return true;
            }
            if ((input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0 && input.key() == GLFW.GLFW_KEY_V) {
                pasteClipboard();
                return true;
            }
            if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
                inputFocused = false;
                searchFocused = false;
                return true;
            }
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            beginClose();
            return true;
        }
        if (closing) return true;
        return super.keyPressed(input);
    }

    private void switchAccount(AltEntry entry) {
        String current = displayedAccountName();
        selected = entry;
        entry.switchPulse = 1f;

        if (current.equalsIgnoreCase(entry.name)) {
            save();
            setStatus("Already active: " + entry.name);
            return;
        }

        switchFrom = current;
        switchTo = entry.name;
        switchProgress = 0f;
        AccountSwitcher.switchTo(entry.name);
        save();
        setStatus("Logged in as " + entry.name);
    }

    private void addAccount(String raw) {
        String name = raw.trim().replaceAll("[^a-zA-Z0-9_]", "");
        if (name.isEmpty() || name.length() > 16) {
            fieldShake = 1f;
            setStatus("Invalid nickname");
            return;
        }
        for (AltEntry entry : entries) {
            if (entry.name.equalsIgnoreCase(name)) {
                switchAccount(entry);
                inputText = "";
                return;
            }
        }
        LocalDate now = LocalDate.now();
        String date = String.format("%02d.%02d.%04d", now.getDayOfMonth(), now.getMonthValue(), now.getYear());
        AltEntry entry = new AltEntry(name, false, date);
        entries.add(0, entry);
        rebuildView();
        switchAccount(entry);
        inputText = "";
        setStatus("Added & logged in as " + name);
    }

    private void pasteClipboard() {
        String clip = GLFW.glfwGetClipboardString(client().getWindow().handle());
        if (clip != null) {
            String filtered = clip.trim().replaceAll("[^a-zA-Z0-9_]", "");
            if (!filtered.isEmpty()) {
                String value = filtered.substring(0, Math.min(16, filtered.length()));
                if (searchFocused) {
                    searchText = value;
                    scrollTarget = 0f;
                } else {
                    inputText = value;
                    inputFocused = true;
                }
                setStatus("Pasted from clipboard");
                return;
            }
        }
        fieldShake = 1f;
        setStatus("Clipboard is empty or invalid");
    }

    private String generateRandomNick() {
        String prefix = RANDOM_PREFIXES[ThreadLocalRandom.current().nextInt(RANDOM_PREFIXES.length)];
        int num = ThreadLocalRandom.current().nextInt(100, 9999);
        return prefix + num;
    }

    private void setStatus(String message) {
        this.statusMessage = message;
        this.statusTime = 2.5f;
    }

    @Override
    public void onClose() {
        beginClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
