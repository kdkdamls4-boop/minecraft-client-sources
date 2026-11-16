package tokyo.ghoul.api.module;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.system.backend.Configurable;
import tokyo.ghoul.api.system.interfaces.QuickImports;
import tokyo.ghoul.client.features.modules.other.ToggleSoundsModule;
import tokyo.ghoul.client.features.modules.render.ClickGUIModule;
import tokyo.ghoul.client.ui.widget.overlay.NotifyWidget;

@Getter
public abstract class Module extends Configurable implements QuickImports {
    private final String name;
    private final String description;
    private final Category category;
    private final boolean hidden;
    @Setter private int bind;

    private boolean enabled;
    private boolean insideEnable;

    public Module() {
        ModuleRegister data = getClass().getAnnotation(ModuleRegister.class);

        if (data == null) try {
            throw new Exception("No data for " + getClass().getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.name = data.name();
        this.description = data.description();
        this.category = data.category();
        this.bind = data.bind();
        this.hidden = data.hidden();
    }

    public boolean hasBind() { return bind != -999; }

    public void toggle() {
        setEnabled(!enabled, false);
    }

    public void setEnabled(boolean newState) {
        setEnabled(newState, false);
    }

    public void setEnabled(boolean newState, boolean config) {
        if (enabled == newState) return;

        enabled = newState;
        if (config) return;

        applyEnabledState();

        if (insideEnable) return;

        if (enabled != newState) return;

        if (this instanceof ClickGUIModule || hidden) return;
        ToggleSoundsModule.playToggle(newState);
        NotifyWidget.toggle(this, newState);
    }

    public void applyEnabledState() {
        if (enabled) {
            insideEnable = true;
            try {
                onEnable();
            } finally {
                insideEnable = false;
            }
            if (enabled) {
                onEvent();
            }
        } else {
            onDisable();
            removeAllEvents();
        }
    }

    public abstract void onEvent();

    public void onEnable() {}
    public void onDisable() {}
}
