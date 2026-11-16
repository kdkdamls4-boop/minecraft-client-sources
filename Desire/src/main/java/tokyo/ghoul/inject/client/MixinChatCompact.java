package tokyo.ghoul.inject.client;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.client.autobuy.AutoBuyConfig;
import tokyo.ghoul.client.autobuy.PriceParser;

@Mixin(ChatComponent.class)
public abstract class MixinChatCompact {
    @Shadow @Final private List<GuiMessage> allMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow protected abstract void refreshTrimmedMessages();

    @Unique private Component lastOriginalText = null;
    @Unique private String lastCleanString = null;
    @Unique private int lastMessageCount = 1;

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void desire$compactAutoBuyChat(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (message == null) return;

        AutoBuyConfig cfg = AutoBuyConfig.cfg();
        if (cfg != null && !cfg.compactChat) {
            this.lastCleanString = null;
            this.lastOriginalText = null;
            this.lastMessageCount = 1;
            return;
        }

        String raw = message.getString();
        if (raw == null || raw.isBlank()) return;

        String clean = PriceParser.stripFormatting(raw).trim();
        if (clean.isEmpty()) return;

        // Check if message is identical to the last message and messages list is non-empty
        if (clean.equals(this.lastCleanString) && !this.allMessages.isEmpty() && this.lastOriginalText != null) {
            this.lastMessageCount++;

            // Create stacked text: original message + " (xN)"
            Component stacked = this.lastOriginalText.copy().append(Component.literal(" §7(x" + this.lastMessageCount + ")"));

            GuiMessage previousLine = this.allMessages.get(0);
            this.allMessages.set(0, new GuiMessage(previousLine.addedTime(), stacked, previousLine.signature(), previousLine.source(), previousLine.tag()));

            int scroll = this.chatScrollbarPos;
            this.refreshTrimmedMessages();
            if (scroll > 0) {
                this.chatScrollbarPos = scroll;
            }
            ci.cancel();
            return;
        }

        // New distinct message
        this.lastCleanString = clean;
        this.lastOriginalText = message.copy();
        this.lastMessageCount = 1;
    }

    @Inject(method = "clearMessages", at = @At("HEAD"))
    private void desire$resetCompactAutoBuyChat(boolean clearHistory, CallbackInfo ci) {
        this.lastCleanString = null;
        this.lastOriginalText = null;
        this.lastMessageCount = 1;
    }
}
