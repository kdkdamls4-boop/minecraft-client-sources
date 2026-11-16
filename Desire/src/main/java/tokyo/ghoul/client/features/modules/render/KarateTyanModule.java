package tokyo.ghoul.client.features.modules.render;

import lombok.Getter;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.SliderSetting;

@ModuleRegister(name = "Karate Tyan", description = "Анимированный персонаж на экране", category = Category.RENDER)
public class KarateTyanModule extends Module {
    @Getter private static final KarateTyanModule instance = new KarateTyanModule();

    private final SliderSetting speed = new SliderSetting("Скорость").value(1.0f).range(0.4f, 2.5f).step(0.1f);

    public KarateTyanModule() {
        addSettings(speed);
    }

    @Override
    public void onEvent() {
        addEvents(UpdateEvent.getInstance().subscribe(new Listener<>(event -> {})));
    }

    public void applyPose(HumanoidModel<?> model) {
        float t = (System.currentTimeMillis() % 6000L) / 1000.0f * speed.getValue();
        float cycle = t % 6.0f;
        if (cycle < 2.0f) {
            float k = Mth.sin(cycle / 2.0f * Mth.PI);
            model.rightLeg.xRot = -1.95f * k;
            model.leftArm.xRot = -1.15f * k;
            model.rightArm.xRot = -0.55f * k;
        } else if (cycle < 4.0f) {
            float k = Mth.sin((cycle - 2.0f) / 2.0f * Mth.PI);
            model.rightArm.xRot = -1.55f * k;
            model.rightArm.yRot = -0.75f * k;
            model.leftArm.xRot = -0.9f * k;
            model.body.yRot = 0.55f * k;
        } else {
            float k = (cycle - 4.0f) / 2.0f;
            model.body.yRot = k * Mth.PI * 2.0f;
            model.leftLeg.xRot = -1.25f * Mth.sin(k * Mth.PI);
            model.rightLeg.xRot = 1.1f * Mth.sin(k * Mth.PI);
            model.rightArm.zRot = 1.2f * Mth.sin(k * Mth.PI);
            model.leftArm.zRot = -1.2f * Mth.sin(k * Mth.PI);
        }
    }
}
