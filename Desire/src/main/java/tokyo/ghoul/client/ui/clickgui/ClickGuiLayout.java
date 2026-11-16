package tokyo.ghoul.client.ui.clickgui;

import tokyo.ghoul.client.services.RenderService;

public final class ClickGuiLayout {
    private ClickGuiLayout() {}

    public static float scaled(float value) {
        return RenderService.getInstance().scaledGui(value) * 0.5f;
    }

    public static float windowWidth() {
        return scaled(670f);
    }

    public static float windowHeight() {
        return scaled(425f);
    }

    public static float headerHeight() {
        return scaled(50f);
    }

    public static float sidebarWidth() {
        return scaled(130f);
    }

    public static float round() {
        return scaled(16f);
    }

    public static float cardWidth() {
        return scaled(240f);
    }

    public static float cardGap() {
        return scaled(15f);
    }

    public static float rowGap() {
        return scaled(3f);
    }

    private static float[] contentOverride;

    public static void setContentRect(float x, float y, float width, float height) {
        contentOverride = new float[]{x, y, width, height};
    }

    public static void clearContentRect() {
        contentOverride = null;
    }

    public static float contentX(float windowX) {
        return contentOverride != null ? contentOverride[0] : windowX + sidebarWidth();
    }

    public static float contentY(float windowY) {
        return contentOverride != null ? contentOverride[1] : windowY + headerHeight();
    }

    public static float contentWidth() {
        return contentOverride != null ? contentOverride[2] : windowWidth() - sidebarWidth();
    }

    public static float contentHeight() {
        return contentOverride != null ? contentOverride[3] : windowHeight() - headerHeight();
    }
}
