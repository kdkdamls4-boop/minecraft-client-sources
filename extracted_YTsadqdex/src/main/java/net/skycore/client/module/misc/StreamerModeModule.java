package net.skycore.client.module.misc;

import net.skycore.client.profile.LocalProfile;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.skycore.client.SkycoreClient;
import net.skycore.client.config.FriendManager;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.setting.ModeSetting;
import net.skycore.client.util.ReplaceLiteralHelper;
import net.skycore.client.setting.BooleanSetting;

public final class StreamerModeModule extends ClientModule {
   private final BooleanSetting hideGrief;
   private final BooleanSetting hidePrefixes;
   private static final Pattern RANK_SYMBOLS = Pattern.compile("[ꔀꔄꔈꔒꔖꔠꔤꔨꕠꕡꔲꔶꕄꕖꕈꕀꕒꔉꔓꔗꔡꔥꔩꔳꔷꔁꔅꕁꕉꕅꕓ]");
   private final BooleanSetting maskDonation;
   private final ModeSetting donation;
   private final BooleanSetting hideDaisies;
   private static final Pattern BRACKET_PREFIX = Pattern.compile("(?:§[0-9a-fk-or])*\\s*\\[.*?\\]\\s*");
   private final BooleanSetting hideBalance;
   private final BooleanSetting hideRelics;
   public static final StreamerModeModule INSTANCE = new StreamerModeModule();
   private final BooleanSetting hideIp;
   private final BooleanSetting hideRank = new BooleanSetting("Hide Rank", true);
   private static final Pattern GRIEF_NUMBER = Pattern.compile("(ГРИФ\\s*#)\\d+");

   private static String replaceIgnoreCase(String text, String text2, String text3) {
      int length = text.length();
      StringBuilder stringBuilder = new StringBuilder(length);
      String text4 = text.toLowerCase(Locale.ROOT);
      String text5 = text2.toLowerCase(Locale.ROOT);
      int value = 0;

      while (true) {
         int value2;
         if ((value2 = text4.indexOf(text5, value)) < 0) {
            return stringBuilder.append(text, value, text.length()).toString();
         }

         stringBuilder.append(text, value, value2).append(text3);
         value = value2 + text2.length();
      }
   }

   private String getDonationSymbol() {
      return switch (this.donation.get().toLowerCase(Locale.ROOT)) {
         case "hero" -> "ꔄ";
         case "titan" -> "ꔈ";
         case "avenger" -> "ꔒ";
         case "overlord" -> "ꔖ";
         case "magister" -> "ꔠ";
         case "imperator" -> "ꔤ";
         case "dragon" -> "ꔨ";
         case "d.helper" -> "ꕠ";
         case "bull" -> "ꔲ";
         case "tiger" -> "ꔶ";
         case "dracula" -> "ꕄ";
         case "bunny" -> "ꕖ";
         case "cobra" -> "ꕈ";
         case "hydra" -> "ꕀ";
         case "rabbit" -> "ꕒ";
         case "ml.moder" -> "ꔓ";
         case "moder" -> "ꔗ";
         case "moder+" -> "ꔡ";
         case "st.moder" -> "ꔥ";
         case "glmoder" -> "ꔩ";
         case "mladmin" -> "ꔳ";
         case "admin" -> "ꔷ";
         case "media" -> "ꔁ";
         case "yt" -> "ꔅ";
         case "god" -> "ꕁ";
         default -> "ꔀ";
      };
   }

   private StreamerModeModule() {
      super("Streamer Mode", "Protects player names and sensitive scoreboard info", ModuleCategory.MISC);
      this.hideGrief = new BooleanSetting("Hide Grief", true);
      this.hideIp = new BooleanSetting("Hide IP", true);
      this.hideBalance = new BooleanSetting("Hide Balance", true);
      this.hideRelics = new BooleanSetting("Hide Relics", true);
      this.hideDaisies = new BooleanSetting("Hide Daisies", true);
      this.hidePrefixes = new BooleanSetting("Hide Nametag Prefixes", true);
      this.maskDonation = new BooleanSetting("Mask Donation", true);
      this.donation = new ModeSetting(
         "Donation",
         "Player",
         "Player",
         "Hero",
         "Titan",
         "Avenger",
         "Overlord",
         "Magister",
         "Imperator",
         "Dragon",
         "D.Helper",
         "Bull",
         "Tiger",
         "Dracula",
         "Bunny",
         "Cobra",
         "Hydra",
         "Rabbit",
         "Ml.Moder",
         "Moder",
         "Moder+",
         "St.Moder",
         "GlModer",
         "MlAdmin",
         "Admin",
         "Media",
         "YT",
         "God"
      );
      this.donation.visibleWhen(this.maskDonation::get);
      this.addSettings(
         this.hideRank, this.hideGrief, this.hideIp, this.hideBalance, this.hideRelics, this.hideDaisies, this.hidePrefixes, this.maskDonation, this.donation
      );
   }

   public static String protectScoreboardString(String text) {
      return text != null && INSTANCE.isEnabled() ? protectScoreboardLine(text) : text;
   }

   private static boolean containsIgnoreCase(String text, String text2) {
      String text3 = text.toLowerCase(Locale.ROOT);
      String text4 = text2.toLowerCase(Locale.ROOT);
      return text3.contains(text4);
   }

   @Override
   protected void onEnable() {
      BooleanSetting hideRank2 = this.hideRank;
      Boolean condition = true;
      hideRank2.set(condition);
      this.hideGrief.set(true);
      this.hideIp.set(true);
      BooleanSetting hideBalance2 = this.hideBalance;
      Boolean condition2 = true;
      hideBalance2.set(condition2);
      this.hideRelics.set(true);
      BooleanSetting hideDaisies2 = this.hideDaisies;
      Boolean condition3 = true;
      hideDaisies2.set(condition3);
      BooleanSetting maskDonation2 = this.maskDonation;
      Boolean condition4 = true;
      maskDonation2.set(condition4);
   }

   public static String maskOwnNametag(String text, String text2) {
      if (text != null && text2 != null && INSTANCE.isEnabled()) {
         Boolean get = INSTANCE.maskDonation.get();
         if (get) {
            Minecraft instance = Minecraft.getInstance();
            String text3;
            if (instance.player != null) {
               text3 = instance.player.getScoreboardName();
            } else {
               text3 = "";
            }

            String text4 = text3;
            if (!text2.equalsIgnoreCase(text4)) {
               String maskedNickname = INSTANCE.getMaskedNickname();
               if (!text2.equalsIgnoreCase(maskedNickname)) {
                  return text;
               }
            }

            Matcher matcher = RANK_SYMBOLS.matcher(text);
            String text5 = matcher.replaceAll("");
            return INSTANCE.getDonationSymbol() + text5;
         }
      }

      return text;
   }

   public boolean shouldHidePrefixes() {
      if (this.isEnabled()) {
         Boolean get = this.hidePrefixes.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public static String getCustomName(String text) {
      if (text != null && !text.isEmpty() && INSTANCE.isEnabled()) {
         Minecraft instance = Minecraft.getInstance();
         if (instance.player == null) {
            return text;
         }

         String text2 = text;
         String user = instance.getUser() != null ? instance.getUser().getName() : null;
         if (user != null && !user.isEmpty() && text2.contains(user)) {
            text2 = text2.replace(user, INSTANCE.getMaskedNickname());
         }

         String scoreboardName = instance.player.getScoreboardName();
         if (scoreboardName != null && !scoreboardName.isEmpty() && !scoreboardName.equals(user) && text2.contains(scoreboardName)) {
            text2 = text2.replace(scoreboardName, INSTANCE.getMaskedNickname());
         }

         if (INSTANCE.hideIp.get()) {
            text2 = replaceIgnoreCase(text2, "ReallyWorld", LocalProfile.DISPLAY_NAME);
         }

         for (String text3 : FriendManager.INSTANCE.getFriends()) {
            String display = SkycoreClient.customNames().getDisplay(text3);
            String text4 = display != null && !display.isEmpty() ? display : "Protected";
            text2 = Pattern.compile("(?i)(?<![A-Za-z0-9_])" + Pattern.quote(text3) + "(?![A-Za-z0-9_])").matcher(text2).replaceAll(Matcher.quoteReplacement(text4));
         }

         return getCleanName(protectScoreboardLine(text2), false);
      } else {
         return text;
      }
   }

   public static String getCleanName(String text, boolean condition) {
      if (text == null || text.isEmpty() || !INSTANCE.isEnabled()) {
         return text;
      }

      if (!INSTANCE.hidePrefixes.get() && !condition) {
         return text;
      }

      String text2 = text;

      String text3;
      do {
         text3 = text2;
         text2 = BRACKET_PREFIX.matcher(text2).replaceAll("");
      } while (!text2.equals(text3));

      return text2.trim().isEmpty() ? text : text2;
   }

   private static String replaceValue(String text, String text2, String text3) {
      int value = text.toLowerCase(Locale.ROOT).indexOf(text2.toLowerCase(Locale.ROOT));
      if (value < 0) {
         return text;
      }

      int length = value + text2.length();

      while (length < text.length() && Character.isWhitespace(text.charAt(length))) {
         length++;
      }

      int value2 = length;
      int value3 = !containsIgnoreCase(text2, "Баланс") && !containsIgnoreCase(text2, "Риллик") && !containsIgnoreCase(text2, "Ромашки") ? 0 : 1;

      while (value2 < text.length() && (value3 != 0 ? Character.isDigit(text.charAt(value2)) : !Character.isWhitespace(text.charAt(value2)))) {
         value2++;
      }

      String text4 = text.substring(value2);
      if (value3 != 0) {
         text4 = text4.replaceFirst("\\d+$", "");
      }

      return text.substring(0, value + text2.length()) + " " + text3 + text4;
   }

   public static String getCustomName() {
      if (INSTANCE.isEnabled()) {
         return INSTANCE.getMaskedNickname();
      }

      Minecraft instance = Minecraft.getInstance();
      String text;
      if (instance.player != null) {
         text = instance.player.getScoreboardName();
      } else {
         text = "";
      }

      return text;
   }

   private String getMaskedNickname() {
      return LocalProfile.DISPLAY_NAME;
   }

   private static String protectScoreboardLine(String text) {
      if (INSTANCE.hideRank.get() && containsIgnoreCase(text, "Ранг:")) {
         int value = text.toLowerCase(Locale.ROOT).indexOf("ранг:");
         return text.substring(0, value + "Ранг:".length()) + " " + INSTANCE.getDonationSymbol();
      }

      if (INSTANCE.hideGrief.get() && containsIgnoreCase(text, "ГРИФ")) {
         return GRIEF_NUMBER.matcher(text).replaceAll("$167");
      }

      if (!INSTANCE.hideIp.get()
         || !text.contains(".ru")
            && !text.contains(".net")
            && !text.contains(".com")
            && !text.contains(".su")
            && !text.contains(".org")
            && !text.contains(".me")) {
         if (INSTANCE.hideBalance.get() && containsIgnoreCase(text, "Баланс:")) {
            return replaceValue(text, "Баланс:", "15");
         } else if (INSTANCE.hideRelics.get() && containsIgnoreCase(text, "Рилликов:")) {
            return replaceValue(text, "Рилликов:", "36");
         } else if (INSTANCE.hideRelics.get() && containsIgnoreCase(text, "Риллики:")) {
            return replaceValue(text, "Риллики:", "360");
         } else if (INSTANCE.hideDaisies.get() && containsIgnoreCase(text, "Ромашки:")) {
            return replaceValue(text, "Ромашки:", "25");
         } else if (text.contains("Ник:")) {
            return replaceValue(text, "Ник:", INSTANCE.getMaskedNickname());
         } else {
            return text.contains("ник:") ? replaceValue(text, "ник:", INSTANCE.getMaskedNickname()) : text;
         }
      } else {
         return LocalProfile.DISPLAY_NAME;
      }
   }

   public static Component protectScoreboardText(Component text) {
      if (text != null) {
         if (INSTANCE.isEnabled()) {
            String string = text.getString();
            String text2 = protectScoreboardString(string);
            Component text3;
            if (string.equals(text2)) {
               text3 = text;
            } else {
               text3 = ReplaceLiteralHelper.replaceLiteral(text, string, text2);
            }

            return text3;
         }
      }

      return text;
   }
}
