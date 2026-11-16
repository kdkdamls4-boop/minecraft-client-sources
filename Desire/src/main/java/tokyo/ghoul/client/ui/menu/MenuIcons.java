package tokyo.ghoul.client.ui.menu;

import net.minecraft.resources.Identifier;
import tokyo.ghoul.client.ui.clickgui.ClickGuiIcons;

import java.awt.Color;

public final class MenuIcons {
    private MenuIcons() {}

    public static final Identifier MENU_SINGLEPLAYER = icon("menu_singleplayer.svg");
    public static final Identifier MENU_MULTIPLAYER = icon("menu_multiplayer.svg");
    public static final Identifier MENU_ACCOUNTS = icon("menu_accounts.svg");
    public static final Identifier MENU_SETTINGS = icon("menu_settings.svg");
    public static final Identifier DICES = icon("dices.svg");
    public static final Identifier DELETE_ACCOUNT = icon("delete_account.svg");
    public static final Identifier COPY_NICK = icon("copy_nick.svg");
    public static final Identifier STAR = icon("star.svg");
    public static final Identifier STAR_WHITE = icon("star_white.svg");

    public static final Identifier BACKGROUND = title("desire_menu_bg.png");
    public static final Identifier WORDMARK = title("desire_wordmark.png");
    public static final Identifier ACCOUNTS_BACKGROUND = title("accounts_background.png");
    public static final Identifier ACCOUNTS_HEADING = title("accounts_heading.png");
    public static final Identifier AVATAR = title("avatar.png");
    public static final Identifier LOGO = title("logo.svg");
    public static final Identifier TITLE_SINGLEPLAYER = title("singleplayer.svg");
    public static final Identifier TITLE_MULTIPLAYER = title("multiplayer.svg");
    public static final Identifier TITLE_ACCOUNTS = title("accounts.svg");
    public static final Identifier TITLE_OPTIONS = title("options.svg");
    public static final Identifier TITLE_QUIT = title("quit.svg");
    public static final Identifier TITLE_PROFILE = title("profile.svg");
    public static final Identifier TITLE_CHEVRON = title("chevron.svg");
    public static final Identifier TITLE_DISCORD = title("discord.svg");
    public static final Identifier TITLE_TELEGRAM = title("telegram.svg");
    public static final Identifier TITLE_BROADCAST = title("broadcast.svg");
    public static final Identifier TITLE_BROADCAST_16 = title("broadcast-16.svg");
    public static final Identifier TITLE_USERS = title("users.svg");
    public static final Identifier TITLE_WORLD = title("world-wide-web.svg");

    public static final Identifier ACCOUNT_MICROSOFT = account("microsoft.svg");
    public static final Identifier ACCOUNT_EDIT = account("edit.svg");
    public static final Identifier ACCOUNT_PUSH_PIN = account("pushpin-filled.svg");
    public static final Identifier ACCOUNT_PINNED = account("pinned-filled.svg");
    public static final Identifier ACCOUNT_STAR = account("star-solid.svg");
    public static final Identifier ACCOUNT_NAME = account("account-pin-box-fill.svg");
    public static final Identifier ACCOUNT_PASSWORD = account("ellipsis-24.svg");
    public static final Identifier ACCOUNT_GENERATE = account("layers-down-right.svg");
    public static final Identifier ACCOUNT_TRASH = account("trash.svg");
    public static final Identifier ACCOUNT_PLUS = account("plus-circle.svg");
    public static final Identifier ACCOUNT_SORT = account("sort.svg");
    public static final Identifier ACCOUNT_BACK = account("arrow-left.svg");
    public static final Identifier ACCOUNT_SEARCH = account("search.svg");

    public static void draw(float x, float y, float width, float height, Identifier texture, Color color) {
        ClickGuiIcons.draw(x, y, width, height, texture, color);
    }

    private static Identifier icon(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/icons/" + name);
    }

    private static Identifier title(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/title/" + name);
    }

    private static Identifier account(String name) {
        return Identifier.fromNamespaceAndPath("desire", "textures/menu/account/" + name);
    }
}
