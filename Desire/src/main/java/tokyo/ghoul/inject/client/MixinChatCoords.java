package tokyo.ghoul.inject.client;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tokyo.ghoul.api.system.client.GpsChatLinks;

@Mixin(ChatComponent.class)
public abstract class MixinChatCoords {
    @ModifyVariable(method = "addMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Component desire$clickableCoords(Component message) {
        return GpsChatLinks.rewrite(message);
    }
}
