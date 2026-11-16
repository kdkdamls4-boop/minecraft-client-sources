package tokyo.ghoul.client.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import tokyo.ghoul.api.command.Command;
import tokyo.ghoul.api.command.CommandRegister;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.AutoBuyEngine;
import tokyo.ghoul.client.autobuy.AutoBuyReport;
import tokyo.ghoul.client.features.modules.misc.FlowAutoBuyModule;
import tokyo.ghoul.client.ui.autobuy.AutoBuyItemsScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Locale;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;

@CommandRegister(name = "autobuy", aliases = {"ab"})
public class CommandAutoBuy extends Command {

    @Override
    public void execute(LiteralArgumentBuilder<SharedSuggestionProvider> builder) {
        builder.executes(context -> {
            openGui();
            return SINGLE_SUCCESS;
        });

        builder.then(literal("items")
                .executes(context -> {
                    openGui();
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("gui")
                .executes(context -> {
                    openGui();
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("toggle")
                .executes(context -> {
                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                    cfg.enabled = !cfg.enabled;
                    AutoBuyConfig.save();
                    print("AutoBuy: " + (cfg.enabled ? "§aВКЛ" : "§cВЫКЛ"));
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("compact")
                .executes(context -> {
                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                    cfg.compactChat = !cfg.compactChat;
                    AutoBuyConfig.save();
                    print("§6[AutoBuy] Анти-спам чата: " + (cfg.compactChat ? "§aВКЛ" : "§cВЫКЛ"));
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("antispam")
                .executes(context -> {
                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                    cfg.compactChat = !cfg.compactChat;
                    AutoBuyConfig.save();
                    print("§6[AutoBuy] Анти-спам чата: " + (cfg.compactChat ? "§aВКЛ" : "§cВЫКЛ"));
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("stats")
                .executes(context -> {
                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                    long spent = cfg.totalSpent;
                    long earned = cfg.totalEarned;
                    long profit = earned - spent;
                    print("§e[AutoBuy Отчёт]");
                    print("§7Потрачено: §c" + AutoBuyReport.formatMoney(spent) + " §7(купил: " + cfg.totalBoughtCount + ")");
                    print("§7Заработано: §a" + AutoBuyReport.formatMoney(earned) + " §7(продано: " + cfg.totalSoldCount + ")");
                    print("§7Профит: " + (profit >= 0 ? "§a+" : "§c") + AutoBuyReport.formatMoney(profit));
                    return SINGLE_SUCCESS;
                })
        );

        builder.then(literal("resetstats")
                .executes(context -> {
                    AutoBuyConfig.cfg().resetStats();
                    print("§a[AutoBuy] Статистика потрачено/заработано сброшена на 0!");
                    return SINGLE_SUCCESS;
                })
        );

        // .autobuy nick / .autobuy blacklist / .autobuy ignore / .autobuy ingnor / .autobuy ign / .autobuy ban
        builder.then(createNickSubcommand("nick"));
        builder.then(createNickSubcommand("blacklist"));
        builder.then(createNickSubcommand("ignore"));
        builder.then(createNickSubcommand("ingnor"));
        builder.then(createNickSubcommand("ign"));
        builder.then(createNickSubcommand("ban"));

        // .autobuy pay / .autobuy autopay
        builder.then(createPaySubcommand("pay"));
        builder.then(createPaySubcommand("autopay"));
    }

    private LiteralArgumentBuilder<SharedSuggestionProvider> createNickSubcommand(String name) {
        return literal(name)
                .executes(context -> {
                    print("§6[AutoBuy] Использование команды черного списка:");
                    print("§f.autobuy " + name + " add <ник> §7- запретить покупку у игрока");
                    print("§f.autobuy " + name + " remove <ник> §7- разрешить покупку у игрока");
                    print("§f.autobuy " + name + " list §7- показать список запрещенных игроков");
                    print("§f.autobuy " + name + " clear §7- очистить черный список");
                    return SINGLE_SUCCESS;
                })
                .then(literal("add")
                        .then(argument("player", string())
                                .suggests((context, builder) -> {
                                    if (mc.getConnection() != null) {
                                        String rem = builder.getRemaining().toLowerCase();
                                        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                                            if (entry != null && entry.getProfile() != null && entry.getProfile().name() != null) {
                                                String pName = entry.getProfile().name();
                                                if (pName.toLowerCase().startsWith(rem)) {
                                                    builder.suggest(pName);
                                                }
                                            }
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String raw = getString(context, "player");
                                    String player = raw.replace("\"", "").replace("'", "").trim();
                                    if (player.isEmpty()) {
                                        print("§6[AutoBuy] §cНикнейм игрока не может быть пустым!");
                                        return SINGLE_SUCCESS;
                                    }
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    if (cfg.ignored(player)) {
                                        print("§6[AutoBuy] §cИгрок §f" + player + " §cуже в черном списке!");
                                    } else {
                                        cfg.addBan(player);
                                        print("§6[AutoBuy] §aИгрок §f" + player + " §aдобавлен в черный список (покупка у него запрещена)!");
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("remove")
                        .then(argument("player", string())
                                .suggests((context, builder) -> {
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    String rem = builder.getRemaining().toLowerCase();
                                    for (String s : cfg.banList) {
                                        if (s != null && s.toLowerCase().startsWith(rem)) {
                                            builder.suggest(s);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String raw = getString(context, "player");
                                    String player = raw.replace("\"", "").replace("'", "").trim();
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    if (cfg.ignored(player)) {
                                        cfg.removeBan(player);
                                        print("§6[AutoBuy] §aИгрок §f" + player + " §aудален из черного списка!");
                                    } else {
                                        print("§6[AutoBuy] §cИгрок §f" + player + " §cне найден в черном списке!");
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("del")
                        .then(argument("player", string())
                                .suggests((context, builder) -> {
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    String rem = builder.getRemaining().toLowerCase();
                                    for (String s : cfg.banList) {
                                        if (s != null && s.toLowerCase().startsWith(rem)) {
                                            builder.suggest(s);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String raw = getString(context, "player");
                                    String player = raw.replace("\"", "").replace("'", "").trim();
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    if (cfg.ignored(player)) {
                                        cfg.removeBan(player);
                                        print("§6[AutoBuy] §aИгрок §f" + player + " §aудален из черного списка!");
                                    } else {
                                        print("§6[AutoBuy] §cИгрок §f" + player + " §cне найден в черном списке!");
                                    }
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("list")
                        .executes(context -> {
                            AutoBuyConfig cfg = AutoBuyConfig.cfg();
                            if (cfg.banList.isEmpty()) {
                                print("§6[AutoBuy] §eЧерный список игроков пуст!");
                            } else {
                                print("§6[AutoBuy] Черный список игроков (§f" + cfg.banList.size() + "§6): §f" + String.join(", ", cfg.banList));
                            }
                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("clear")
                        .executes(context -> {
                            AutoBuyConfig cfg = AutoBuyConfig.cfg();
                            if (cfg.banList.isEmpty()) {
                                print("§6[AutoBuy] §eЧерный список игроков уже пуст!");
                            } else {
                                cfg.banList.clear();
                                AutoBuyConfig.save();
                                print("§6[AutoBuy] §aЧерный список игроков очищен!");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
                .then(argument("directPlayer", string())
                        .suggests((context, builder) -> {
                            if (mc.getConnection() != null) {
                                String rem = builder.getRemaining().toLowerCase();
                                for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                                    if (entry != null && entry.getProfile() != null && entry.getProfile().name() != null) {
                                        String pName = entry.getProfile().name();
                                        if (pName.toLowerCase().startsWith(rem)) {
                                            builder.suggest(pName);
                                        }
                                    }
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            String raw = getString(context, "directPlayer");
                            String player = raw.replace("\"", "").replace("'", "").trim();
                            if (player.isEmpty()) {
                                print("§6[AutoBuy] §cНикнейм игрока не может быть пустым!");
                                return SINGLE_SUCCESS;
                            }
                            AutoBuyConfig cfg = AutoBuyConfig.cfg();
                            if (cfg.ignored(player)) {
                                print("§6[AutoBuy] §cИгрок §f" + player + " §cуже в черном списке!");
                            } else {
                                cfg.addBan(player);
                                print("§6[AutoBuy] §aИгрок §f" + player + " §aдобавлен в черный список (покупка у него запрещена)!");
                            }
                            return SINGLE_SUCCESS;
                        })
                );
    }

    private LiteralArgumentBuilder<SharedSuggestionProvider> createPaySubcommand(String name) {
        return literal(name)
                .executes(context -> {
                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                    print("§6[AutoPay] Настройки авто-перевода:");
                    print("§7• Состояние: " + (cfg.autoPay ? "§aВКЛ" : "§cВЫКЛ"));
                    print("§7• Получатель: " + (cfg.autoPayNick.isEmpty() ? "§cне указан" : "§f" + cfg.autoPayNick));
                    print("§7• Сумма перевода: §f" + (cfg.autoPayAmount / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", cfg.autoPayAmount) + ")");
                    print("§7• Оставлять баланс: §f" + (cfg.autoPayKeepBalance / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", cfg.autoPayKeepBalance) + ")");
                    print("§6[AutoPay] Команды:");
                    print("§f.autobuy " + name + " toggle §7- переключить вкл/выкл");
                    print("§f.autobuy " + name + " nick <ник> §7- установить получателя");
                    print("§f.autobuy " + name + " amount <кк> §7- сумма перевода (1-100кк)");
                    print("§f.autobuy " + name + " keep <кк> §7- сколько оставлять на балансе (0-100кк)");
                    print("§f.autobuy " + name + " send §7- проверить и перевести сейчас");
                    return SINGLE_SUCCESS;
                })
                .then(literal("toggle")
                        .executes(context -> {
                            AutoBuyConfig cfg = AutoBuyConfig.cfg();
                            cfg.autoPay = !cfg.autoPay;
                            FlowAutoBuyModule.getInstance().autoPay.setValue(cfg.autoPay);
                            AutoBuyConfig.save();
                            print("§6[AutoPay] Авто-перевод: " + (cfg.autoPay ? "§aВКЛЮЧЕН" : "§cВЫКЛЮЧЕН"));
                            if (cfg.autoPay && (cfg.autoPayNick == null || cfg.autoPayNick.isBlank())) {
                                print("§e[AutoPay] §cВнимание: укажите ник получателя: §f.autobuy " + name + " nick <ник>");
                            }
                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("nick")
                        .then(argument("recipient", string())
                                .suggests((context, builder) -> {
                                    if (mc.getConnection() != null) {
                                        String rem = builder.getRemaining().toLowerCase();
                                        for (PlayerInfo entry : mc.getConnection().getOnlinePlayers()) {
                                            if (entry != null && entry.getProfile() != null && entry.getProfile().name() != null) {
                                                String pName = entry.getProfile().name();
                                                if (pName.toLowerCase().startsWith(rem)) {
                                                    builder.suggest(pName);
                                                }
                                            }
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> {
                                    String raw = getString(context, "recipient");
                                    String recipient = raw.replace("\"", "").replace("'", "").trim();
                                    if (recipient.isEmpty()) {
                                        print("§6[AutoPay] §cНик получателя не может быть пустым!");
                                        return SINGLE_SUCCESS;
                                    }
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    cfg.autoPayNick = recipient;
                                    FlowAutoBuyModule.getInstance().autoPayNick.setText(recipient);
                                    AutoBuyConfig.save();
                                    print("§6[AutoPay] §aНик получателя установлен на: §f" + recipient);
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("amount")
                        .then(argument("value", string())
                                .executes(context -> {
                                    String raw = getString(context, "value");
                                    Long amount = parseMoneyInput(raw);
                                    if (amount == null || amount < 1000000L || amount > 100000000L) {
                                        print("§6[AutoPay] §cСумма перевода должна быть от §f1кк §cдо §f100кк §c(например: 5, 5kk, 10кк, 50)!");
                                        return SINGLE_SUCCESS;
                                    }
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    cfg.autoPayAmount = amount;
                                    FlowAutoBuyModule.getInstance().autoPayAmount.setValue((float) (amount / 1000000.0));
                                    AutoBuyConfig.save();
                                    print("§6[AutoPay] §aСумма перевода установлена на: §f" + (amount / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", amount) + ")");
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("keep")
                        .then(argument("value", string())
                                .executes(context -> {
                                    String raw = getString(context, "value");
                                    Long keep = parseMoneyInput(raw);
                                    if (keep == null || keep < 0L || keep > 100000000L) {
                                        print("§6[AutoPay] §cОставляемый баланс должен быть от §f0кк §cдо §f100кк §c(например: 0, 5, 5kk, 10кк)!");
                                        return SINGLE_SUCCESS;
                                    }
                                    AutoBuyConfig cfg = AutoBuyConfig.cfg();
                                    cfg.autoPayKeepBalance = keep;
                                    FlowAutoBuyModule.getInstance().autoPayKeep.setValue((float) (keep / 1000000.0));
                                    AutoBuyConfig.save();
                                    print("§6[AutoPay] §aОставляемый баланс установлен на: §f" + (keep / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", keep) + ")");
                                    return SINGLE_SUCCESS;
                                })
                        )
                )
                .then(literal("send")
                        .executes(context -> {
                            AutoBuyEngine.INSTANCE.autoPay.forcePay();
                            print("§6[AutoPay] §eЗапрошена проверка баланса и отправка перевода...");
                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("now")
                        .executes(context -> {
                            AutoBuyEngine.INSTANCE.autoPay.forcePay();
                            print("§6[AutoPay] §eЗапрошена проверка баланса и отправка перевода...");
                            return SINGLE_SUCCESS;
                        })
                )
                .then(literal("info")
                        .executes(context -> {
                            AutoBuyConfig cfg = AutoBuyConfig.cfg();
                            print("§6[AutoPay] Информация:");
                            print("§7• Состояние: " + (cfg.autoPay ? "§aВКЛ" : "§cВЫКЛ"));
                            print("§7• Получатель: " + (cfg.autoPayNick.isEmpty() ? "§cне указан" : "§f" + cfg.autoPayNick));
                            print("§7• Сумма перевода: §f" + (cfg.autoPayAmount / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", cfg.autoPayAmount) + ")");
                            print("§7• Оставлять баланс: §f" + (cfg.autoPayKeepBalance / 1000000L) + "кк §7(" + String.format(Locale.ROOT, "%,d", cfg.autoPayKeepBalance) + ")");
                            return SINGLE_SUCCESS;
                        })
                );
    }

    public static Long parseMoneyInput(String str) {
        if (str == null || str.isBlank()) return null;
        String clean = str.replace("\"", "").replace("'", "").trim().toLowerCase(Locale.ROOT);
        long multiplier = 1L;
        if (clean.endsWith("kk") || clean.endsWith("кк") || clean.endsWith("m") || clean.endsWith("м") || clean.endsWith("млн")) {
            multiplier = 1000000L;
            clean = clean.replaceAll("[a-zа-я]", "");
        } else if (clean.endsWith("k") || clean.endsWith("к") || clean.endsWith("тыс")) {
            multiplier = 1000L;
            clean = clean.replaceAll("[a-zа-я]", "");
        }
        clean = clean.replace(',', '.').replaceAll("[\\s\\u00A0]", "");
        try {
            double val = Double.parseDouble(clean);
            if (multiplier == 1L && val <= 100.0) {
                val *= 1000000.0;
            } else {
                val *= multiplier;
            }
            return (long) Math.round(val);
        } catch (Exception e) {
            return null;
        }
    }

    private void openGui() {
        mc.execute(() -> mc.gui.setScreen(new AutoBuyItemsScreen(mc.gui.screen())));
    }
}
