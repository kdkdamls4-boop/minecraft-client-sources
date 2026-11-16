package tokyo.ghoul.client.features.modules.misc;

import lombok.Getter;
import tokyo.ghoul.api.event.EventListener;
import tokyo.ghoul.api.event.Listener;
import tokyo.ghoul.api.event.events.player.other.UpdateEvent;
import tokyo.ghoul.api.module.Category;
import tokyo.ghoul.api.module.Module;
import tokyo.ghoul.api.module.ModuleRegister;
import tokyo.ghoul.api.module.setting.BooleanSetting;
import tokyo.ghoul.api.module.setting.RunSetting;
import tokyo.ghoul.api.module.setting.SliderSetting;
import tokyo.ghoul.api.module.setting.TextSetting;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.AutoBuyEngine;
import tokyo.ghoul.client.ui.autobuy.AutoBuyItemsScreen;

@ModuleRegister(name = "AutoBuy", description = "Автоматическая покупка и перепродажа предметов на аукционе", category = Category.OTHER)
public class FlowAutoBuyModule extends Module {
    @Getter private static final FlowAutoBuyModule instance = new FlowAutoBuyModule();

    public final BooleanSetting autoRefresh = new BooleanSetting("Авто-обновление").value(true);
    public final BooleanSetting autoSell = new BooleanSetting("Авто-продажа").value(false);
    public final BooleanSetting autoRelist = new BooleanSetting("Авто-релист").value(false);
    public final BooleanSetting autoParse = new BooleanSetting("Авто-парсинг цен").value(true);
    public final BooleanSetting safeMode = new BooleanSetting("SafeMode").value(true);
    public final SliderSetting armorDurability = new SliderSetting("Мин. прочность брони %").range(0f, 100f).step(1f).value(50f);
    public final TextSetting autoPayNick = new TextSetting("Ник").value("");
    public final BooleanSetting autoPay = new BooleanSetting("AutoPay").value(false);
    public final SliderSetting autoPayAmount = new SliderSetting("Сумма перевода").range(1f, 100f).step(1f).value(5f);
    public final SliderSetting autoPayKeep = new SliderSetting("Остаток баланс").range(0f, 100f).step(1f).value(5f);
    public final SliderSetting sellDiscount = new SliderSetting("Скидка продажи %").range(1f, 50f).step(1f).value(5f);
    public final SliderSetting minPrice = new SliderSetting("Мин. цена").range(1000f, 500000f).step(1000f).value(15000f);
    public final BooleanSetting compactChat = new BooleanSetting("Анти-спам чата").value(true);
    public final RunSetting itemsButton = new RunSetting("Предметы").value(() ->
            mc.execute(() -> mc.gui.setScreen(new AutoBuyItemsScreen(mc.gui.screen()))));

    public FlowAutoBuyModule() {
        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        autoRefresh.setValue(cfg.autoRefresh);
        autoSell.setValue(cfg.autoSell);
        autoRelist.setValue(cfg.autoRelist);
        autoParse.setValue(cfg.autoParse);
        safeMode.setValue(cfg.safeMode);
        armorDurability.setValue((float) cfg.armorDurabilityPercent);
        autoPay.setValue(cfg.autoPay);
        autoPayNick.setText(cfg.autoPayNick != null ? cfg.autoPayNick : "");
        autoPayAmount.setValue((float) (Math.max(1_000_000L, Math.min(100_000_000L, cfg.autoPayAmount)) / 1_000_000.0));
        autoPayKeep.setValue((float) (Math.max(0L, Math.min(100_000_000L, cfg.autoPayKeepBalance)) / 1_000_000.0));
        sellDiscount.setValue((float) cfg.sellDiscountPercent);
        minPrice.setValue((float) cfg.minPrice);
        compactChat.setValue(cfg.compactChat);

        autoPayAmount.setVisible(autoPay::getValue);
        autoPayKeep.setVisible(autoPay::getValue);

        addSettings(autoRefresh, autoSell, autoRelist, autoParse, safeMode, armorDurability, autoPayNick, autoPay, autoPayAmount, autoPayKeep, sellDiscount, minPrice, compactChat, itemsButton);
    }

    @Override
    public void onEnable() {
        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        cfg.enabled = true;
        syncSettings();
        AutoBuyConfig.save();
    }

    @Override
    public void onDisable() {
        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        cfg.enabled = false;
        AutoBuyConfig.save();
    }

    @Override
    public void onEvent() {
        EventListener update = UpdateEvent.getInstance().subscribe(new Listener<>(event -> onUpdate()));
        addEvents(update);
    }

    private void onUpdate() {
        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        // Двусторонняя синхронизация: если оверлей переключил состояние AutoBuy или AutoSell или SafeMode
        if (cfg.enabled != isEnabled()) {
            this.setEnabled(cfg.enabled);
        }
        if (cfg.autoSell != autoSell.getValue()) {
            autoSell.setValue(cfg.autoSell);
            if (!cfg.autoSell) {
                AutoBuyEngine.INSTANCE.seller.stop();
            }
        }
        if (cfg.safeMode != safeMode.getValue()) {
            safeMode.setValue(cfg.safeMode);
        }
        if (cfg.armorDurabilityPercent != armorDurability.getValue().intValue()) {
            armorDurability.setValue((float) cfg.armorDurabilityPercent);
        }
        if (cfg.sellDiscountPercent != sellDiscount.getValue().intValue()) {
            sellDiscount.setValue((float) cfg.sellDiscountPercent);
        }

        syncSettings();
    }

    public void syncSettings() {
        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        boolean changed = false;

        if (cfg.autoRefresh != autoRefresh.getValue()) {
            cfg.autoRefresh = autoRefresh.getValue();
            changed = true;
        }
        if (cfg.autoSell != autoSell.getValue()) {
            cfg.autoSell = autoSell.getValue();
            if (!cfg.autoSell) {
                AutoBuyEngine.INSTANCE.seller.stop();
            }
            changed = true;
        }
        if (cfg.autoRelist != autoRelist.getValue()) {
            cfg.autoRelist = autoRelist.getValue();
            changed = true;
        }
        if (cfg.autoParse != autoParse.getValue()) {
            cfg.autoParse = autoParse.getValue();
            changed = true;
        }
        if (cfg.safeMode != safeMode.getValue()) {
            cfg.safeMode = safeMode.getValue();
            changed = true;
        }
        int armorDur = armorDurability.getValue().intValue();
        if (cfg.armorDurabilityPercent != armorDur) {
            cfg.armorDurabilityPercent = armorDur;
            changed = true;
        }
        if (cfg.autoPay != autoPay.getValue()) {
            cfg.autoPay = autoPay.getValue();
            changed = true;
        }
        String cleanNick = autoPayNick.getText().trim();
        if (!cleanNick.equals(cfg.autoPayNick)) {
            cfg.autoPayNick = cleanNick;
            changed = true;
        }
        long payAmount = Math.max(1000000L, Math.min(100000000L, (long) (autoPayAmount.getValue() * 1000000L)));
        if (cfg.autoPayAmount != payAmount) {
            cfg.autoPayAmount = payAmount;
            changed = true;
        }
        long payKeep = Math.max(0L, Math.min(100000000L, (long) (autoPayKeep.getValue() * 1000000L)));
        if (cfg.autoPayKeepBalance != payKeep) {
            cfg.autoPayKeepBalance = payKeep;
            changed = true;
        }
        int discount = sellDiscount.getValue().intValue();
        if (cfg.sellDiscountPercent != discount) {
            cfg.sellDiscountPercent = discount;
            changed = true;
        }
        long minPr = minPrice.getValue().longValue();
        if (cfg.minPrice != minPr) {
            cfg.minPrice = minPr;
            changed = true;
        }
        if (cfg.compactChat != compactChat.getValue()) {
            cfg.compactChat = compactChat.getValue();
            changed = true;
        }

        if (changed) {
            AutoBuyConfig.save();
        }
    }
}
