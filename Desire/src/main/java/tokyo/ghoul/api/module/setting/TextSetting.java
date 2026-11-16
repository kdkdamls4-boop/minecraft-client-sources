package tokyo.ghoul.api.module.setting;

import lombok.Getter;
import java.util.Objects;
import java.util.function.Supplier;

@Getter
public class TextSetting extends Setting<String> {
    private String placeholder = "";

    public TextSetting(String name) {
        super(name);
        this.value = "";
    }

    public TextSetting placeholder(String placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    @Override
    public TextSetting value(String value) {
        setValue(value);
        return this;
    }

    @Override
    public void setValue(String value) {
        String safe = value == null ? "" : value;
        if (sameValue(safe)) return;
        super.setValue(safe);
        runAction();
    }

    @Override
    protected boolean sameValue(String newValue) {
        return Objects.equals(this.value, newValue);
    }

    @Override
    public TextSetting setVisible(Supplier<Boolean> condition) {
        return (TextSetting) super.setVisible(condition);
    }

    @Override
    public TextSetting onAction(Runnable action) {
        return (TextSetting) super.onAction(action);
    }

    public String getText() {
        return value == null ? "" : value;
    }

    public void setText(String text) {
        setValue(text);
    }
}
