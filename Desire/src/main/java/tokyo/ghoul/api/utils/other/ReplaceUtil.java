package tokyo.ghoul.api.utils.other;

import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;

@UtilityClass
public class ReplaceUtil {
    public Component replace(Component input, String target, String replacement) {
        if (input == null || target == null || replacement == null) return input;
        MutableComponent result = Component.empty().setStyle(input.getStyle());
        appendReplaced(result, input, target, replacement);
        return result;
    }

    private void appendReplaced(MutableComponent result, Component current, String target, String replacement) {
        ComponentContents content = current.getContents();
        Style style = current.getStyle();

        if (content instanceof PlainTextContents.LiteralContents literal) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(target), java.util.regex.Pattern.CASE_INSENSITIVE);
            String replaced = pattern.matcher(literal.text()).replaceAll(replacement);
            result.append(Component.literal(replaced).setStyle(style));
        }

        for (Component sibling : current.getSiblings()) {
            appendReplaced(result, sibling, target, replacement);
        }
    }

    public String replaceSymbols(String string) {
        return string
                .replaceAll("ꔗ", ChatFormatting.BLUE + "MODER")
                .replaceAll("ꔥ", ChatFormatting.BLUE + "ST.MODER")
                .replaceAll("ꔡ", ChatFormatting.LIGHT_PURPLE + "MODER+")
                .replaceAll("ꔀ", ChatFormatting.GRAY + "PLAYER")
                .replaceAll("ꔉ", ChatFormatting.YELLOW + "HELPER")
                .replaceAll("◆", "@")
                .replaceAll("┃", "|")
                .replaceAll("ꔳ", ChatFormatting.AQUA + "ML.ADMIN")
                .replaceAll("ꔅ", ChatFormatting.RED + "Y" + ChatFormatting.WHITE + "T")
                .replaceAll("ꔂ", ChatFormatting.BLUE + "D.MODER")
                .replaceAll("ꕠ", ChatFormatting.YELLOW + "D.HELPER")
                .replaceAll("ꕄ", ChatFormatting.RED + "DRACULA")
                .replaceAll("ꔖ", ChatFormatting.AQUA + "OVERLORD")
                .replaceAll("ꕈ", ChatFormatting.GREEN + "COBRA")
                .replaceAll("ꔨ", ChatFormatting.LIGHT_PURPLE + "DRAGON")
                .replaceAll("ꔤ", ChatFormatting.RED + "IMPERATOR")
                .replaceAll("ꔠ", ChatFormatting.GOLD + "MAGISTER")
                .replaceAll("ꔄ", ChatFormatting.BLUE + "HERO")
                .replaceAll("ꔒ", ChatFormatting.GREEN + "AVENGER")
                .replaceAll("ꕒ", ChatFormatting.WHITE + "RABBIT")
                .replaceAll("ꔈ", ChatFormatting.YELLOW + "TITAN")
                .replaceAll("ꕀ", ChatFormatting.DARK_GREEN + "HYDRA")
                .replaceAll("ꔶ", ChatFormatting.GOLD + "TIGER")
                .replaceAll("ꔲ", ChatFormatting.DARK_PURPLE + "BULL")
                .replaceAll("ꕖ", ChatFormatting.BLACK + "BUNNY")
                .replaceAll("ꕗꕘ", ChatFormatting.YELLOW + "SPONSOR")
                .replaceAll("\uD83D\uDD25", "@")
                .replaceAll("ᴀ", "A")
                .replaceAll("ʙ", "B")
                .replaceAll("ᴄ", "C")
                .replaceAll("ᴅ", "D")
                .replaceAll("ᴇ", "E")
                .replaceAll("ғ", "F")
                .replaceAll("ɢ", "G")
                .replaceAll("ʜ", "H")
                .replaceAll("ɪ", "I")
                .replaceAll("ᴊ", "J")
                .replaceAll("ᴋ", "K")
                .replaceAll("ʟ", "L")
                .replaceAll("ᴍ", "M")
                .replaceAll("ɴ", "N")
                .replaceAll("ꜱ", "S")
                .replaceAll("ᴏ", "O")
                .replaceAll("ᴘ", "P")
                .replaceAll("ǫ", "Q")
                .replaceAll("ʀ", "R")
                .replaceAll("ᴛ", "T")
                .replaceAll("ᴜ", "U")
                .replaceAll("ᴠ", "V")
                .replaceAll("ᴡ", "W")
                .replaceAll("ꜰ", "F")
                .replaceAll("ʏ", "Y")
                .replaceAll("ᴢ", "Z");
    }

    public Component replaceSymbols(Component text) {
        if (text.getString().contains("ꔗ")) text = replace(text, "ꔗ", ChatFormatting.BLUE + "MODER");
        if (text.getString().contains("ꔥ")) text = replace(text, "ꔥ", ChatFormatting.BLUE + "ST.MODER");
        if (text.getString().contains("ꔡ")) text = replace(text, "ꔡ", ChatFormatting.LIGHT_PURPLE + "MODER+");
        if (text.getString().contains("ꔀ")) text = replace(text, "ꔀ", ChatFormatting.GRAY + "PLAYER");
        if (text.getString().contains("ꔉ")) text = replace(text, "ꔉ", ChatFormatting.YELLOW + "HELPER");
        if (text.getString().contains("◆")) text = replace(text, "◆", "@");
        if (text.getString().contains("┃")) text = replace(text, "┃", "|");
        if (text.getString().contains("ꔳ")) text = replace(text, "ꔳ", ChatFormatting.AQUA + "ML.ADMIN");
        if (text.getString().contains("ꔅ")) text = replace(text, "ꔅ", ChatFormatting.RED + "Y" + ChatFormatting.WHITE + "T");
        if (text.getString().contains("ꔂ")) text = replace(text, "ꔂ", ChatFormatting.BLUE + "D.MODER");
        if (text.getString().contains("ꕠ")) text = replace(text, "ꕠ", ChatFormatting.YELLOW + "D.HELPER");
        if (text.getString().contains("ꕄ")) text = replace(text, "ꕄ", ChatFormatting.RED + "DRACULA");
        if (text.getString().contains("ꔖ")) text = replace(text, "ꔖ", ChatFormatting.AQUA + "OVERLORD");
        if (text.getString().contains("ꕈ")) text = replace(text, "ꕈ", ChatFormatting.GREEN + "COBRA");
        if (text.getString().contains("ꔨ")) text = replace(text, "ꔨ", ChatFormatting.LIGHT_PURPLE + "DRAGON");
        if (text.getString().contains("ꔤ")) text = replace(text, "ꔤ", ChatFormatting.RED + "IMPERATOR");
        if (text.getString().contains("ꔠ")) text = replace(text, "ꔠ", ChatFormatting.GOLD + "MAGISTER");
        if (text.getString().contains("ꔄ")) text = replace(text, "ꔄ", ChatFormatting.BLUE + "HERO");
        if (text.getString().contains("ꔒ")) text = replace(text, "ꔒ", ChatFormatting.GREEN + "AVENGER");
        if (text.getString().contains("ꕒ")) text = replace(text, "ꕒ", ChatFormatting.WHITE + "RABBIT");
        if (text.getString().contains("ꔈ")) text = replace(text, "ꔈ", ChatFormatting.YELLOW + "TITAN");
        if (text.getString().contains("ꕀ")) text = replace(text, "ꕀ", ChatFormatting.DARK_GREEN + "HYDRA");
        if (text.getString().contains("ꔶ")) text = replace(text, "ꔶ", ChatFormatting.GOLD + "TIGER");
        if (text.getString().contains("ꔲ")) text = replace(text, "ꔲ", ChatFormatting.DARK_PURPLE + "BULL");
        if (text.getString().contains("ꕖ")) text = replace(text, "ꕖ", ChatFormatting.BLACK + "BUNNY");
        if (text.getString().contains("ꕗꕘ")) text = replace(text, "ꕗꕘ", ChatFormatting.YELLOW + "SPONSOR");
        if (text.getString().contains("\uD83D\uDD25")) text = replace(text, "\uD83D\uDD25", "@");

        if (text.getString().contains("ᴀ")) text = replace(text, "ᴀ", "A");
        if (text.getString().contains("ʙ")) text = replace(text, "ʙ", "B");
        if (text.getString().contains("ᴄ")) text = replace(text, "ᴄ", "C");
        if (text.getString().contains("ᴅ")) text = replace(text, "ᴅ", "D");
        if (text.getString().contains("ᴇ")) text = replace(text, "ᴇ", "E");
        if (text.getString().contains("ғ")) text = replace(text, "ғ", "F");
        if (text.getString().contains("ɢ")) text = replace(text, "ɢ", "G");
        if (text.getString().contains("ʜ")) text = replace(text, "ʜ", "H");
        if (text.getString().contains("ɪ")) text = replace(text, "ɪ", "I");
        if (text.getString().contains("ᴊ")) text = replace(text, "ᴊ", "J");
        if (text.getString().contains("ᴋ")) text = replace(text, "ᴋ", "K");
        if (text.getString().contains("ʟ")) text = replace(text, "ʟ", "L");
        if (text.getString().contains("ᴍ")) text = replace(text, "ᴍ", "M");
        if (text.getString().contains("ɴ")) text = replace(text, "ɴ", "N");
        if (text.getString().contains("ꜱ")) text = replace(text, "ꜱ", "S");
        if (text.getString().contains("ᴏ")) text = replace(text, "ᴏ", "O");
        if (text.getString().contains("ᴘ")) text = replace(text, "ᴘ", "P");
        if (text.getString().contains("ǫ")) text = replace(text, "ǫ", "Q");
        if (text.getString().contains("ʀ")) text = replace(text, "ʀ", "R");
        if (text.getString().contains("ᴛ")) text = replace(text, "ᴛ", "T");
        if (text.getString().contains("ᴜ")) text = replace(text, "ᴜ", "U");
        if (text.getString().contains("ᴠ")) text = replace(text, "ᴠ", "V");
        if (text.getString().contains("ᴡ")) text = replace(text, "ᴡ", "W");
        if (text.getString().contains("ꜰ")) text = replace(text, "ꜰ", "F");
        if (text.getString().contains("ʏ")) text = replace(text, "ʏ", "Y");
        if (text.getString().contains("ᴢ")) text = replace(text, "ᴢ", "Z");

        return text;
    }
}
