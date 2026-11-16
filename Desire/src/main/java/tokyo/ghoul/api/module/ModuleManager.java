package tokyo.ghoul.api.module;

import lombok.Getter;
import tokyo.ghoul.client.features.modules.combat.*;
import tokyo.ghoul.client.features.modules.movement.*;
import tokyo.ghoul.client.features.modules.movement.speed.SpeedModule;
import tokyo.ghoul.client.features.modules.other.*;
import tokyo.ghoul.client.features.modules.misc.*;
import tokyo.ghoul.client.features.modules.player.*;
import tokyo.ghoul.client.features.modules.render.*;
import tokyo.ghoul.client.features.modules.render.nametags.NameTagsModule;
import tokyo.ghoul.client.features.modules.render.targetesp.TargetEspModule;
import tokyo.ghoul.client.features.modules.render.cosmetics.CosmeticsModule;
import tokyo.ghoul.client.features.modules.render.ghosthit.GhostHitModule;
import tokyo.ghoul.client.features.modules.render.jumpeffect.JumpEffectModule;
import tokyo.ghoul.client.features.modules.render.particles.ParticlesModule;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ModuleManager {
    @Getter private final static ModuleManager instance = new ModuleManager();

    private final List<Module> modules = new ArrayList<>();

    public void load() {
        register(
                SprintModule.getInstance(),
                ClickGUIModule.getInstance(),
                AmbienceModule.getInstance(),
                AtmosphereModule.getInstance(),
                NoDelaysModule.getInstance(),
                AuraModule.getInstance(),
                PacketCriticalsModule.getInstance(),
                WebTrapModule.getInstance(),
                InterfaceModule.getInstance(),
                AutoLeaveModule.getInstance(),
                NameTagsModule.getInstance(),
                AutoToolModule.getInstance(),
                AntiBotModule.getInstance(),
                NoPushModule.getInstance(),
                NoWebModule.getInstance(),
                NoSlowModule.getInstance(),
                ItemScrollerModule.getInstance(),
                ScoreboardHealthModule.getInstance(),
                AutoTotemModule.getInstance(),
                InventoryMoveModule.getInstance(),
                ElytraSwapModule.getInstance(),
                AutoSwapModule.getInstance(),
                CrystalAuraModule.getInstance(),
                RemovalsModule.getInstance(),
                ClickPearlModule.getInstance(),
                SwingAnimationModule.getInstance(),
                ViewModelModule.getInstance(),
                SpeedModule.getInstance(),
                AirStuckModule.getInstance(),
                ElytraBoosterModule.getInstance(),
                ElytraMotionModule.getInstance(),
                ElytraFlyModule.getInstance(),
                ArrowsModule.getInstance(),
                ItemReplacerModule.getInstance(),
                PredictionsModule.getInstance(),
                SeeInvisiblesModule.getInstance(),
                JumpEffectModule.getInstance(),
                ToggleSoundsModule.getInstance(),
                NightVisionModule.getInstance(),
                TargetEspModule.getInstance(),
                CrosshairModule.getInstance(),
                DamageTintModule.getInstance(),
                KillEffectsModule.getInstance(),
                ParticlesModule.getInstance(),
                CosmeticsModule.getInstance(),
                GhostHitModule.getInstance(),
                MotionBlurModule.getInstance(),
                ObjectInfoModule.getInstance(),
                NoFriendDamageModule.getInstance(),
                AntiCrystalModule.getInstance(),
                FreeCamModule.getInstance(),
                AutoTpacceptModule.getInstance(),
                HandsModule.getInstance(),
                ChinaHatModule.getInstance(),
                GlowEspModule.getInstance(),
                AhHelperModule.getInstance(),
                FlowAutoBuyModule.getInstance(),
                AssistModule.getInstance(),
                BlockerModule.getInstance(),
                OptimizationModule.getInstance(),
                StealerModule.getInstance(),
                BotsModule.getInstance(),
                AutoMineModule.getInstance(),
                BlockOverlayModule.getInstance()
        );

        modules.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
    }

    public void register(Module... modules) {
        this.modules.addAll(List.of(modules));
    }
}
