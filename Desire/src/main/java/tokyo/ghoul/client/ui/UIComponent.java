package tokyo.ghoul.client.ui;

import lombok.Getter;
import lombok.Setter;
import tokyo.ghoul.api.system.interfaces.UIApi;
import tokyo.ghoul.client.services.RenderService;

@Getter
@Setter
public abstract class UIComponent implements UIApi {
    private float x, y, width, height, alpha;
    private float scaleMultiplier = 1f;

    public float gap() {
        return scaled(2f);
    }

    public float offset() {
        return gap() * 1.5f;
    }

    public float scaled(float value) {
        return RenderService.getInstance().scaledGui(value * scaleMultiplier);
    }

    public float getScale() {
        return RenderService.getInstance().getGuiScale();
    }
}
