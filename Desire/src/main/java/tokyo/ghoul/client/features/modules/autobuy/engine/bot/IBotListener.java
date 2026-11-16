package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

import org.jetbrains.annotations.Nullable;

public interface IBotListener {
    @Nullable
    BotSession ghoul$getBotSession();
}
