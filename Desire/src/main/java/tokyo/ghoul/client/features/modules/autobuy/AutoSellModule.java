package tokyo.ghoul.client.features.modules.autobuy;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;

@ModuleRegister(name = "AutoSell", description = "Автоматическая продажа предметов", category = Category.AUTOBUY)
public class AutoSellModule extends Module {
    @Getter private static final AutoSellModule instance = new AutoSellModule();

    public final BooleanSetting smartUndercut = new BooleanSetting("Smart Undercut").value(true);
    public final BooleanSetting batchResale = new BooleanSetting("Пачками").value(true);
    public final SliderSetting batchMin = new SliderSetting("Мин. в пачке")
            .range(1, 10).step(1).value(1f)
            .setVisible(batchResale::getValue);
    public final SliderSetting batchMax = new SliderSetting("Макс. в пачке")
            .range(1, 10).step(1).value(4f)
            .setVisible(batchResale::getValue);

    public AutoSellModule() {
        addSettings(smartUndercut, batchResale, batchMin, batchMax);
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public void onEvent() {
    }
}
