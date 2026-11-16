package net.skycore.client.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.ClientRenderEvent;
import net.skycore.client.module.render.ArrowsModule;
import net.skycore.client.module.render.NameTagsModule;
import net.skycore.client.module.render.PredictionModule;
import net.skycore.client.module.render.WaypointModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiPostRenderEventMixin {
   @Shadow
   @Final
   private GuiRenderState guiRenderState;

   @Inject(
      method = "extractRenderState",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Hud;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
         shift = Shift.AFTER
      )
   )
   private void skycore$postRenderEvent(DeltaTracker deltaTracker, boolean condition, boolean condition2, CallbackInfo callbackInfo) {
      SkycoreClient.modules().get(NameTagsModule.class).ifPresent(nameTagsModule -> nameTagsModule.render(this.guiRenderState, deltaTracker));
      ArrowsModule.INSTANCE.render(this.guiRenderState, deltaTracker);
      SkycoreClient.modules().get(WaypointModule.class).ifPresent(waypointModule -> waypointModule.render(this.guiRenderState, deltaTracker));
      PredictionModule.INSTANCE.render(this.guiRenderState, deltaTracker);
      if (SkycoreClient.interfaceRenderer() != null) {
         SkycoreClient.interfaceRenderer().render(this.guiRenderState, deltaTracker);
      }

      SkycoreClient.events().post(new ClientRenderEvent(Minecraft.getInstance(), deltaTracker));
   }
}
