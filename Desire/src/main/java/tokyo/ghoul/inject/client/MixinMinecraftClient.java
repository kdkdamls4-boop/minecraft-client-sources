package tokyo.ghoul.inject.client;

import net.minecraft.client.GameLoadCookie;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tokyo.ghoul.Desire;
import tokyo.ghoul.api.event.events.client.GameLoopEvent;
import tokyo.ghoul.api.event.events.client.PostTickEvent;
import tokyo.ghoul.api.event.events.client.ResourceReloadEvent;
import tokyo.ghoul.api.event.events.client.TickEvent;
import tokyo.ghoul.api.event.events.player.world.HandleBreakingEvent;
import tokyo.ghoul.api.system.backend.SharedClass;
import tokyo.ghoul.api.utils.framelimiter.FrameLimiter;
import tokyo.ghoul.api.utils.rotation.manager.RotationManager;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotContext;
import tokyo.ghoul.client.features.modules.autobuy.engine.bot.BotSessionManager;
import tokyo.ghoul.client.features.modules.misc.AutoMineModule;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    @Unique
    private FrameLimiter frameLimiter;

    @Inject(method = "runTick", at = @At("HEAD"))
    public void gameLoopHook(boolean tick, CallbackInfo ci) {
        if (frameLimiter == null) frameLimiter = new FrameLimiter(false);
        frameLimiter.execute(60, () -> GameLoopEvent.getInstance().call());
        RotationManager.getInstance().applyClientLookFrame();
        AutoMineModule.frame();
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void preTickHook(CallbackInfo ci) {
        BotSessionManager.getInstance().tick();

        if (SharedClass.player() == null) return;

        TickEvent.getInstance().call();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    public void postTickHook(CallbackInfo ci) {
        if (SharedClass.player() == null) return;

        PostTickEvent.getInstance().call();
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketProcessor;processQueuedPackets()V", shift = At.Shift.AFTER))
    private void drainForegroundBotPackets(boolean advanceGameTime, CallbackInfo ci) {
        BotSessionManager.getInstance().drainForegroundPackets();
    }

    @Inject(method = "setLevel", at = @At("HEAD"), cancellable = true)
    private void botSetLevel(ClientLevel level, CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ((Minecraft) (Object) this).level = level;
            ci.cancel();
        }
    }

    @Inject(method = "setCameraEntity", at = @At("HEAD"), cancellable = true)
    private void botSetCamera(Entity cameraEntity, CallbackInfo ci) {
        if (BotContext.isBackgroundContextActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldEntityAppearGlowing", at = @At("RETURN"), cancellable = true)
    private void glowEspTargets(net.minecraft.world.entity.Entity entity, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && tokyo.ghoul.client.features.modules.render.GlowEspModule.getInstance().shouldGlow(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = {"disconnectWithProgressScreen()V", "disconnectWithSavingScreen()V", "disconnectWithProgressScreen(Z)V"}, at = @At("RETURN"))
    private void resetTargetEspResources(CallbackInfo ci) {
        tokyo.ghoul.client.features.modules.render.targetesp.render.SurfaceLightingPass.m_9271();
        tokyo.ghoul.client.features.modules.render.targetesp.render.TargetEspLightning.m_1844();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void handleBlockBreakingHook(boolean breaking, CallbackInfo ci) {
        if (HandleBreakingEvent.getInstance().call()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void stealerOpenWalls(CallbackInfo ci) {
        if (tokyo.ghoul.client.features.modules.misc.StealerModule.tryOpenThroughWalls()) {
            ci.cancel();
        }
    }

    @Inject(method = "close", at = @At("HEAD"))
    public void closeHook(CallbackInfo ci) {
        Desire.getInstance().onClose();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(GameConfig args, CallbackInfo ci) {
        Desire.getInstance().postLoad();
    }

    @Inject(method = "onResourceLoadFinished", at = @At("TAIL"))
    private void onResourceLoadFinishedHook(GameLoadCookie cookie, CallbackInfo ci) {
        ResourceReloadEvent.getInstance().call();
    }
}
