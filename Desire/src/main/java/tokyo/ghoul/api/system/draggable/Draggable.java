package tokyo.ghoul.api.system.draggable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.other.WindowResizeEvent;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.api.utils.math.MathUtil;

@Getter
@Setter
public class Draggable implements QuickImports {
    @Expose
    @SerializedName("x")
    private float x;
    @Expose
    @SerializedName("y")
    private float y;

    @Expose
    @SerializedName("liquidGlass")
    private Boolean liquidGlass;

    public float initialXVal;
    public float initialYVal;

    private float startX, startY;
    private boolean dragging;
    private boolean consumeClick;
    private float width = 0f;
    private float height = 0f;
    @Expose
    @SerializedName("name")
    private final String name;
    private final Module module;

    public Draggable(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.x = roundToHalf(initialXVal);
        this.y = roundToHalf(initialYVal);
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;

        WindowResizeEvent.getInstance().subscribe(new Listener<>(-1, event -> {
            if (dragging) clampToScreen();
        }));
    }

    public static final float GRID = 10f;
    private static final float SNAP = 2.5f;

    private float offsetY;

    public final void onDraw() {
        if (dragging) {
            float screenWidth = mc.getWindow().getGuiScaledWidth();
            float screenHeight = mc.getWindow().getGuiScaledHeight();
            float targetX = snap(normaliseX() - startX, width, screenWidth);
            float targetY = snap(normaliseY() - startY, height, screenHeight);

            x = roundToHalf(MathUtil.interpolate(x, targetX, .15f));
            y = roundToHalf(MathUtil.interpolate(y, targetY, .15f));

            clampToScreen();
        }
    }

    private static float snap(float pos, float size, float screen) {
        float center = pos + size / 2f;
        if (Math.abs(center - screen / 2f) < SNAP * 2f) return screen / 2f - size / 2f;

        float start = Math.round(pos / GRID) * GRID;
        if (Math.abs(pos - start) < SNAP) return start;

        float end = Math.round((pos + size) / GRID) * GRID;
        if (Math.abs(pos + size - end) < SNAP) return end - size;
        return pos;
    }

    public void consumeClick() {
        this.consumeClick = true;
    }

    public final void onClick(int button) {
        if (consumeClick) {
            consumeClick = false;
            return;
        }
        if (button == 0 && isHovering()) {
            boolean anotherDragging = DraggableManager.getInstance().getDraggables().values().stream().anyMatch(Draggable::isDragging);
            if (!anotherDragging) {
                dragging = true;
                startX = (int) (normaliseX() - x);
                startY = (int) (normaliseY() - (y + offsetY));
            }
        }
    }

    public final void onRelease(int button) {
        if (button == 0) dragging = false;
    }

    public boolean isHovering() {
        float top = y + offsetY;
        return normaliseX() > Math.min(x, x + width) && normaliseX() < Math.max(x, x + width) && normaliseY() > Math.min(top, top + height) && normaliseY() < Math.max(top, top + height);
    }

    public int normaliseX() {
        return (int) (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
    }

    public int normaliseY() {
        return (int) (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());
    }

    private float roundToHalf(float value) {
        return Math.round(value * 2) / 2.0f;
    }

    private void clampToScreen() {
        float margin = 3f;
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();

        if (x < margin) x = margin;
        if (y < margin) y = margin;
        if (x + width > screenWidth - margin) x = screenWidth - width - margin;
        if (y + height > screenHeight - margin) y = screenHeight - height - margin;
    }
}
