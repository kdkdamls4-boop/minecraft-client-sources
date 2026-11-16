package tokyo.ghoul.inject.render;

import tokyo.ghoul.api.utils.render.RenderUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.api.event.events.render.Render2DEvent;
import tokyo.ghoul.client.features.modules.render.CrosshairModule;
import tokyo.ghoul.client.features.modules.render.InterfaceModule;
import tokyo.ghoul.client.features.modules.render.RemovalsModule;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.resources.Identifier;
import tokyo.ghoul.client.ui.clickgui.ClickGui;
import tokyo.ghoul.client.ui.widget.overlay.HotbarWidget;

@Mixin(Hud.class)
public class MixinInGameHud {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    public void renderHook(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        RenderUtil.matrices().setIdentity();

        Render2DEvent.getInstance().call(new Render2DEvent.Render2DEventData(context, RenderUtil.matrices(), tickCounter.getGameTimeDeltaPartialTick(false)));
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void renderStatusEffectOverlay(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (InterfaceModule.getInstance().widgets.isEnabled("Potions")) {
            ci.cancel();
        }
    }

    @Inject(method = "displayScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/world/scores/Objective;)V", at = @At(value = "HEAD"), cancellable = true)
    private void renderScoreboardSidebar(GuiGraphicsExtractor context, Objective objective, CallbackInfo ci) {
        if (Minecraft.getInstance().player == null) return;

        if (RemovalsModule.getInstance().isScoreboard() || (InterfaceModule.getInstance().isEnabled() && InterfaceModule.getInstance().widgets.isEnabled("Scoreboard"))) {
            ci.cancel();
        }
    }

    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void hideVanillaCrosshair(GuiGraphicsExtractor context, DeltaTracker tickCounter, CallbackInfo ci) {
        if (CrosshairModule.getInstance().isEnabled() || Minecraft.getInstance().gui.screen() instanceof ClickGui) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "extractItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V"
            )
    )
    private void wrapHotbarBlitSprite(GuiGraphicsExtractor instance, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height, Operation<Void> original) {
        if (HotbarWidget.isHotbarEnabled()) {
            String path = sprite.getPath();
            if (path.equals("hud/hotbar") || path.equals("hud/hotbar_selection")
                    || path.equals("hud/hotbar_offhand_left") || path.equals("hud/hotbar_offhand_right")) {
                return;
            }
        }
        original.call(instance, pipeline, sprite, x, y, width, height);
    }
}
