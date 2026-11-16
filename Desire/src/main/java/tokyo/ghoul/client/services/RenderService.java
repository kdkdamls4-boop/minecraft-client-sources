package tokyo.ghoul.client.services;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.math.MathUtil;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;

public class RenderService implements QuickImports {
    @Getter private static final RenderService instance = new RenderService();

    @Getter private float hudScale = 1.0f;
    @Getter private float guiScale = 1.0f;
    private static boolean guiContext = false;

    private final Listener<Render2DEvent.Render2DEventData> renderListener = new Listener<>(event -> updateScale());

    public static void setGuiContext(boolean gui) {
        guiContext = gui;
    }

    public float getScale() {
        return guiContext ? guiScale : hudScale;
    }

    public float scaled(float value) {
        return value * getScale();
    }

    public float scaledHud(float value) {
        return value * hudScale;
    }

    public float scaledGui(float value) {
        return value * guiScale;
    }

    public void load() {
        Render2DEvent.getInstance().subscribe(renderListener);
    }

    public void updateScale() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.getWindow() == null) return;

        float w = client.getWindow().getGuiScaledWidth();
        float h = client.getWindow().getGuiScaledHeight();

        float bW = 1366f / 2f;
        float bH = 768f / 2f;

        float base = Math.max(w / bW, h / bH);

        hudScale = converge(hudScale, base * InterfaceModule.getHudScale());
        guiScale = converge(guiScale, base * InterfaceModule.getGuiScale());
    }

    private float converge(float current, float target) {
        if (Math.abs(current - target) < 0.001f) return target;
        return MathUtil.interpolate(current, target, 0.15f);
    }
}
