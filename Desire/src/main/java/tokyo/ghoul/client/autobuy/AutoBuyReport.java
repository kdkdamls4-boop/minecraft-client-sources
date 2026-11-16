package tokyo.ghoul.client.autobuy;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Учёт отчёта AutoBuy / AutoSell:
 * - сколько монет потратил AutoBuy на покупки
 * - сколько заработал AutoSell с продаж
 * - автоматически парсит информацию из чата сервера HolyWorld
 * Отрисовка плашки — {@link tokyo.ghoul.client.ui.widget.overlay.AutoBuyReportWidget}.
 */
public final class AutoBuyReport {
    private static long lastPurchaseTime = 0L;
    private static long lastPurchaseAmount = 0L;
    private static long lastSaleTime = 0L;
    private static long lastSaleAmount = 0L;

    private AutoBuyReport() {
    }

    /**
     * Записывает совершённую покупку монет AutoBuy.
     */
    public static synchronized void recordPurchase(long amount) {
        if (amount <= 0L) return;
        long now = System.currentTimeMillis();
        if (now - lastPurchaseTime < 2500L && lastPurchaseAmount == amount) {
            return;
        }
        lastPurchaseTime = now;
        lastPurchaseAmount = amount;
        AutoBuyConfig.cfg().addSpent(amount);
    }

    /**
     * Записывает совершённую продажу (когда предмет купили на аукционе).
     */
    public static synchronized void recordSale(long amount) {
        if (amount <= 0L) return;
        long now = System.currentTimeMillis();
        if (now - lastSaleTime < 2000L && lastSaleAmount == amount) {
            return;
        }
        lastSaleTime = now;
        lastSaleAmount = amount;
        AutoBuyConfig.cfg().addEarned(amount);
    }

    /**
     * Парсит цену продажи из серверного сообщения HolyWorld.
     */
    public static Long parseSalePrice(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return null;
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        boolean isSale = lower.contains("купил у вас")
                || lower.contains("купила у вас")
                || lower.contains("у вас купили")
                || lower.contains("у вас купил")
                || lower.contains("приобрел у вас")
                || lower.contains("приобрела у вас")
                || lower.contains("успешно продали")
                || lower.contains("успешно продан")
                || (lower.contains("предмет") && lower.contains("куплен"));

        if (!isSale) return null;

        // Ищем "за <число>"
        Pattern pZa = Pattern.compile("(?i)за\\s*([\\d\\s\\u00A0.,]+)\\s*(?:\\$|монет|руб|кк|k|m)?");
        Matcher mZa = pZa.matcher(clean);
        if (mZa.find()) {
            Long val = parseMoneyString(mZa.group(1), clean.substring(mZa.start()));
            if (val != null && val > 0L) return val;
        }

        // Ищем "$<число>" или "<число>$"
        Pattern pSign = Pattern.compile("(?i)(?:\\$|€|₽)\\s*([\\d\\s\\u00A0.,]{2,})|([\\d\\s\\u00A0.,]{2,})\\s*(?:\\$|монет|руб)");
        Matcher mSign = pSign.matcher(clean);
        if (mSign.find()) {
            String num = mSign.group(1) != null ? mSign.group(1) : mSign.group(2);
            Long val = parseMoneyString(num, clean.substring(mSign.start()));
            if (val != null && val > 0L) return val;
        }

        return null;
    }

    /**
     * Парсит цену покупки из серверного сообщения HolyWorld.
     */
    public static Long parseBuyPrice(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return null;
        String clean = rawMessage.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        String lower = clean.toLowerCase(Locale.ROOT);

        boolean isBuy = lower.contains("вы успешно приобрели")
                || lower.contains("вы успешно купили")
                || lower.contains("вы купили")
                || lower.contains("успешная покупка")
                || (lower.contains("куплено") && lower.contains("autobuy"));

        if (!isBuy) return null;

        Pattern pZa = Pattern.compile("(?i)за\\s*([\\d\\s\\u00A0.,]+)\\s*(?:\\$|монет|руб|кк|k|m)?");
        Matcher mZa = pZa.matcher(clean);
        if (mZa.find()) {
            Long val = parseMoneyString(mZa.group(1), clean.substring(mZa.start()));
            if (val != null && val > 0L) return val;
        }

        Pattern pSign = Pattern.compile("(?i)(?:\\$|€|₽)\\s*([\\d\\s\\u00A0.,]{2,})|([\\d\\s\\u00A0.,]{2,})\\s*(?:\\$|монет|руб)");
        Matcher mSign = pSign.matcher(clean);
        if (mSign.find()) {
            String num = mSign.group(1) != null ? mSign.group(1) : mSign.group(2);
            Long val = parseMoneyString(num, clean.substring(mSign.start()));
            if (val != null && val > 0L) return val;
        }

        return null;
    }

    /**
     * Преобразует строковое представление числа с учетом разделителей и суффиксов в long.
     */
    public static Long parseMoneyString(String numStr, String context) {
        if (numStr == null || numStr.isBlank()) return null;
        String s = numStr.replaceAll("[\\s\\u00A0]", "").trim();
        if (s.isEmpty()) return null;

        long multiplier = 1L;
        if (context != null) {
            String ctxLower = context.toLowerCase(Locale.ROOT);
            if (ctxLower.contains("кк") || ctxLower.contains("kk") || ctxLower.contains("млн") || ctxLower.matches(".*\\d\\s*m(?![a-z]).*")) {
                multiplier = 1_000_000L;
            } else if (ctxLower.contains("тыс") || ctxLower.matches(".*\\d\\s*k(?![a-z]).*")) {
                multiplier = 1_000L;
            }
        }

        try {
            if (multiplier > 1L) {
                String sanitized = s.replace(',', '.');
                double val = Double.parseDouble(sanitized);
                return (long) Math.round(val * multiplier);
            }

            if (s.matches(".*[.,]\\d{2}$")) {
                s = s.substring(0, s.length() - 3);
            }
            s = s.replaceAll("[.,]", "");
            s = s.replaceAll("\\D", "");
            if (s.isEmpty()) return null;
            return Long.parseLong(s);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Форматирует число монет в аккуратный читаемый вид.
     */
    public static String formatMoney(long amount) {
        long abs = Math.abs(amount);
        String sign = amount < 0 ? "-" : "";
        if (abs >= 1_000_000_000L) {
            return sign + String.format(Locale.ROOT, "%.2fB$", abs / 1_000_000_000.0);
        } else if (abs >= 10_000_000L) {
            return sign + String.format(Locale.ROOT, "%.1fM$", abs / 1_000_000.0);
        } else if (abs >= 1_000_000L) {
            return sign + String.format(Locale.ROOT, "%.2fM$", abs / 1_000_000.0);
        } else if (abs >= 1000L) {
            return sign + String.format(Locale.ROOT, "%,d$", abs).replace(',', ' ');
        } else {
            return sign + abs + "$";
        }
    }
}
