package tokyo.ghoul.client.features.modules.autobuy.engine.bot;

import org.jetbrains.annotations.Nullable;

public interface IBotConnection {
    @Nullable
    BotSession ghoul$getBotSession();

    void ghoul$setBotSession(@Nullable BotSession session);
}
