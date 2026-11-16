package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.ui.bots.ScreenBots;

@ModuleRegister(name = "Bots", description = "Управление ботами", category = Category.OTHER)
public class BotsModule extends Module {
    @Getter private static final BotsModule instance = new BotsModule();

    public final RunSetting openMenu = new RunSetting("Открыть меню").value(BotsModule::open);
    public final SliderSetting maxBots = new SliderSetting("Макс. ботов").range(1, 15).step(1).value(5f)
            .onAction(this::apply);
    public final BooleanSetting botChat = new BooleanSetting("Чат ботов в чат").value(false)
            .onAction(this::apply);
    public final BooleanSetting botAutoMine = new BooleanSetting("AutoMine у ботов").value(false);

    public BotsModule() {
        addSettings(openMenu, maxBots, botChat, botAutoMine);
        apply();
    }

    private void apply() {
        BotSessionManager manager = BotSessionManager.getInstance();
        manager.setMaxBots(Math.round(maxBots.getValue()));
        manager.setShowBotChat(botChat.getValue());
    }

    public static void open() {
        mc.gui.setScreen(ScreenBots.getInstance());
    }

    @Override
    public void onEnable() {
        apply();
        if (mc.gui.screen() != null) return;
        open();
    }

    @Override
    public void onEvent() {
        toggle();
    }
}
