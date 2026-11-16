package net.skycore.client.mixin;

import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Hud.class)
public interface HudSetOverlayMessageTimeAccessor {
   @Accessor("overlayMessageTime")
   void skycore$setOverlayMessageTime(int value);

   @Accessor("toolHighlightTimer")
   void skycore$setToolHighlightTimer(int value);
}
