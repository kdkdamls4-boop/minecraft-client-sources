package tokyo.ghoul.inject.gui;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSession;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;

@Mixin(ChatComponent.class)
public class MixinBotChatComponent {
    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void botChat(Component contents, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (source == GuiMessageSource.SYSTEM_CLIENT) {
            return;
        }
        BotSessionManager manager = BotSessionManager.getInstance();
        BotSession background = BotContext.current();
        if (background != null && BotContext.isBackgroundContextActive()) {
            manager.onChatMessage(background, contents);
            ci.cancel();
            return;
        }
        BotSession foreground = manager.getActiveSession();
        if (foreground != manager.getMainSession()) {
            manager.onChatMessage(foreground, contents);
        }
    }
}
