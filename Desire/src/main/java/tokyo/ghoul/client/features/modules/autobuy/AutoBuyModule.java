package tokyo.ghoul.client.features.modules.autobuy;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.ui.autobuy.ScreenAutoBuy;

@ModuleRegister(name = "AutoBuy", description = "Автоматическая покупка выгодных предметов", category = Category.AUTOBUY)
public class AutoBuyModule extends Module {
    @Getter private static final AutoBuyModule instance = new AutoBuyModule();

    public final RunSetting openCatalog = new RunSetting("Каталог предметов")
            .value(() -> mc.gui.setScreen(ScreenAutoBuy.getInstance()));

    public final BooleanSetting autoRct = new BooleanSetting("AutoRCT").value(false);
    public final SliderSetting rctMinMinutes = new SliderSetting("RCT Min (мин)")
            .range(1, 360).step(1).value(5f)
            .setVisible(autoRct::getValue);
    public final SliderSetting rctMaxMinutes = new SliderSetting("RCT Max (мин)")
            .range(1, 360).step(1).value(120f)
            .setVisible(autoRct::getValue);

    public AutoBuyModule() {
        addSettings(openCatalog, autoRct, rctMinMinutes, rctMaxMinutes);
    }

    @Override
    public void onEnable() {
        AuctionAutoBuy sniper = AuctionAutoBuy.getInstance();
        if (!sniper.isEnabled()) {
            sniper.setEnabled(true);
        }
        BotSessionManager.getInstance().setBackgroundAutoBuyEnabled(true);
    }

    @Override
    public void onDisable() {
        AuctionAutoBuy sniper = AuctionAutoBuy.getInstance();
        if (sniper.isEnabled()) {
            sniper.setEnabled(false);
        }
        BotSessionManager.getInstance().setBackgroundAutoBuyEnabled(false);
    }

    @Override
    public void onEvent() {
    }
}
