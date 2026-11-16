package deluxe.mixin;

import deluxe.client.command.ClientCommand;
import deluxe.client.DeluxeClient;
import ru.dreamix.fabricloader.VMBridge;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatInputSuggestor.class)
public abstract class ChatInputSuggestorMixin {
   @Shadow
   @Final
   TextFieldWidget textField;
   @Shadow
   @Final
   private List<OrderedText> messages;
   @Shadow
   boolean completingSuggestions;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;

   @Shadow
   public abstract void show(boolean var1);

   @Shadow
   public abstract boolean isOpen();

   @Shadow
   public abstract void clearWindow();

   @Inject(method = "refresh()V", at = @At("HEAD"), cancellable = true)
   private void suggestClientCommands(CallbackInfo callbackInfo) {
      if (DeluxeClient.inst().commandDispatcher() != null && this.textField.getText().startsWith(DeluxeClient.inst().commandDispatcher().method1899())) {
         if (this.isOpen() && this.completingSuggestions) {
            callbackInfo.cancel();
         } else {
            this.textField.setSuggestion(null);
            this.messages.clear();
            SuggestionsBuilder suggestionsBuilder = new SuggestionsBuilder(this.textField.getText(), this.textField.getText().lastIndexOf(32) + 1);
            if (this.textField.getText().contains(" ")) {
               for (String local8392_4 : DeluxeClient.inst().commandDispatcher().method1895(this.textField.getText())) {
                  suggestionsBuilder.suggest(local8392_4);
               }
            } else {
               for (ClientCommand clientCommand : DeluxeClient.inst().commandDispatcher().method1896(this.textField.getText())) {
                  suggestionsBuilder.suggest(DeluxeClient.inst().commandDispatcher().method1899() + clientCommand.getName(), Text.literal(clientCommand.getDescription()));
               }
            }

            this.pendingSuggestions = suggestionsBuilder.buildFuture();
            this.clearWindow();
            this.show(false);
            callbackInfo.cancel();
         }
      }
   }

   static {
      VMBridge.identifyClass(ChatInputSuggestorMixin.class, "l35ylYf5");
   }
}
