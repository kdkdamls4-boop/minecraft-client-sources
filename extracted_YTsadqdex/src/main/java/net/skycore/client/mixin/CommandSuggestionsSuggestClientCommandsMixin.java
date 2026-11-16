package net.skycore.client.mixin;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.skycore.client.SkycoreClient;
import net.skycore.client.command.CommandManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsSuggestClientCommandsMixin {
   @Shadow
   @Final
   EditBox input;
   @Shadow
   private ParseResults<ClientSuggestionProvider> currentParse;
   @Shadow
   private CompletableFuture<Suggestions> pendingSuggestions;
   @Shadow
   private boolean currentParseIsCommand;
   @Shadow
   private boolean keepSuggestions;

   @Shadow
   protected abstract void showSuggestions(boolean condition);

   @Shadow
   public abstract void hide();

   @Inject(method = "updateCommandInfo", at = @At("HEAD"), cancellable = true)
   private void skycore$suggestClientCommands(CallbackInfo callbackInfo) {
      CommandManager commandManager = SkycoreClient.commands();
      if (commandManager != null) {
         String prefix = commandManager.getPrefix();
         String value = this.input.getValue();
         if (prefix != null && !prefix.isEmpty() && value != null && value.startsWith(prefix)) {
            if (this.keepSuggestions) {
               callbackInfo.cancel();
            } else {
               this.input.setSuggestion(null);
               this.hide();
               this.pendingSuggestions = null;
               ClientSuggestionProvider source = commandManager.getSource();
               if (source == null) {
                  callbackInfo.cancel();
               } else {
                  StringReader stringReader = new StringReader(value);
                  stringReader.setCursor(prefix.length());
                  this.currentParse = commandManager.getDispatcher().parse(stringReader, source);
                  this.currentParseIsCommand = true;
                  int cursorPosition = this.input.getCursorPosition();
                  if (cursorPosition >= prefix.length()) {
                     CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> completionSuggestions = commandManager.getDispatcher().getCompletionSuggestions(this.currentParse, cursorPosition);
                     this.pendingSuggestions = completionSuggestions;
                     completionSuggestions.thenAccept(
                        suggestions -> Minecraft.getInstance()
                           .execute(
                              () -> {
                                 if (this.pendingSuggestions == completionSuggestions
                                    && value.equals(this.input.getValue())
                                    && cursorPosition == this.input.getCursorPosition()
                                    && !suggestions.isEmpty()) {
                                    this.showSuggestions(false);
                                 }
                              }
                           )
                     );
                  }

                  callbackInfo.cancel();
               }
            }
         }
      }
   }
}
