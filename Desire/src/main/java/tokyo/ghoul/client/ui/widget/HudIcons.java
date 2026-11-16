package tokyo.ghoul.client.ui.widget;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.api.utils.render.RenderUtil;
import tokyo.ghoul.api.utils.render.gui.GuiTexture;

import java.awt.Color;

public final class HudIcons {
    private HudIcons() {}

    public static final Identifier LOGO = icon("j.svg");
    public static final Identifier KEYBOARD = icon("keyboard.svg");
    public static final Identifier FLASK = icon("flask.svg");
    public static final Identifier TIMER = icon("timer.svg");
    public static final Identifier KEYBINDS = icon("keybinds.svg");
    public static final Identifier POTIONS = icon("potions.svg");
    public static final Identifier COOLDOWNS = icon("cooldowns.svg");
    public static final Identifier ON = icon("on.svg");
    public static final Identifier OFF = icon("off.svg");
    public static final Identifier TRIANGLE = icon("triangle.svg");
    public static final Identifier FPS = icon("fps.svg");
    public static final Identifier USERS = icon("users.svg");
    public static final Identifier SIGNAL = icon("signal.svg");
    public static final Identifier WIFI = icon("wifi.svg");
    public static final Identifier USER = icon("user.svg");
    public static final Identifier LAYER = icon("layer.svg");
    public static final Identifier TPS = icon("tps.svg");
    public static final Identifier HEART = icon("heart.svg");
    public static final Identifier BOLT = Identifier.fromNamespaceAndPath("desire", "textures/hud/logo_bolt.svg");
    public static final Identifier WATERMARK_USER = Identifier.fromNamespaceAndPath("desire", "textures/hud/watermark_user.png");
    public static final Identifier WATERMARK_LOTUS = Identifier.fromNamespaceAndPath("desire", "textures/hud/watermark_lotus.png");
    public static final Identifier WATERMARK_CLOUD = Identifier.fromNamespaceAndPath("desire", "textures/hud/watermark_cloud.png");
    public static final Identifier WATERMARK_GRID = Identifier.fromNamespaceAndPath("desire", "textures/hud/watermark_grid.png");
    public static final Identifier WM_USER = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_user.svg");
    public static final Identifier WM_GAUGE = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_gauge.svg");
    public static final Identifier WM_CLOCK = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_clock.svg");
    public static final Identifier WM_HOME = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_home.svg");
    public static final Identifier WM_GLOBE = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_globe.svg");
    public static final Identifier WM_LAYERS = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_layers.svg");
    public static final Identifier WM_STAR = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_star.svg");
    public static final Identifier WM_BELL = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_bell.svg");
    public static final Identifier WM_PLUS = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_plus.svg");
    public static final Identifier WM_CROSS = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_cross.svg");
    public static final Identifier WM_SLIDERS = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_sliders.svg");
    public static final Identifier WM_PIN = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_pin.svg");
    public static final Identifier WM_BOARD = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_board.svg");
    public static final Identifier WM_PREV = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_prev.svg");
    public static final Identifier WM_NEXT = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_next.svg");
    public static final Identifier WM_PLAY = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_play.svg");
    public static final Identifier WM_PAUSE = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_pause.svg");
    public static final Identifier WM_USERS = Identifier.fromNamespaceAndPath("desire", "textures/hud/wm_users.svg");

    public static void draw(float x, float y, float size, Identifier texture, Color color) {
        draw(RenderUtil.matrices(), x, y, size, size, texture, color);
    }

    public static void draw(float x, float y, float width, float height, Identifier texture, Color color) {
        draw(RenderUtil.matrices(), x, y, width, height, texture, color);
    }

    public static void draw(PoseStack ms, float x, float y, float size, Identifier texture, Color color) {
        draw(ms, x, y, size, size, texture, color);
    }

    public static void draw(PoseStack ms, float x, float y, float width, float height, Identifier texture, Color color) {
        if (texture.getPath().endsWith(".svg")) {
            float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
            var setup = GuiTexture.load(texture).textureSetup(width * scale, height * scale);
            RenderUtil.TEXTURE_RECT.draw(ms, x, y, width, height, 0.01f, color, 0f, 0f, 1f, 1f, setup.texure0());
            return;
        }
        RenderUtil.TEXTURE_RECT.draw(ms, x, y, width, height, 0f, color, 0f, 0f, 1f, 1f, texture);
    }

    public static void drawGradient(PoseStack ms, float x, float y, float width, float height, Identifier texture, Color start, Color end) {
        Color mid = new Color((start.getRed() + end.getRed()) / 2, (start.getGreen() + end.getGreen()) / 2,
                (start.getBlue() + end.getBlue()) / 2, (start.getAlpha() + end.getAlpha()) / 2);
        if (texture.getPath().endsWith(".svg")) {
            float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
            var setup = GuiTexture.load(texture).textureSetup(width * scale, height * scale);
            RenderUtil.TEXTURE_RECT.drawGradient(ms, x, y, width, height, 0.01f, start, mid, mid, end, 0f, 0f, 1f, 1f, setup.texure0());
            return;
        }
        RenderUtil.TEXTURE_RECT.drawGradient(ms, x, y, width, height, 0f, start, mid, mid, end, 0f, 0f, 1f, 1f, texture);
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/icons/" + name);
    }
}
