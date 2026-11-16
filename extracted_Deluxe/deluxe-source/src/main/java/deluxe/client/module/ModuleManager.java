package deluxe.client.module;

import deluxe.client.module.misc.AdminRecordModule;
import deluxe.client.module.misc.AdminShardModule;
import deluxe.client.module.combat.AimAssistModule;
import deluxe.client.module.movement.AirStuckModule;
import deluxe.client.module.render.ArmorBarModule;
import deluxe.client.module.render.ArrowsModule;
import deluxe.client.module.render.AspectRatioModule;
import deluxe.client.module.combat.AttackAuraModule;
import deluxe.client.module.misc.AuctionHelperModule;
import deluxe.client.module.misc.AutoAcceptModule;
import deluxe.client.module.misc.AutoAppleFarmModule;
import deluxe.client.module.misc.AutoBuyModule;
import deluxe.client.module.misc.AutoDeliverModule;
import deluxe.client.module.misc.AutoEatModule;
import deluxe.client.module.player.AutoExpModule;
import deluxe.client.module.misc.AutoLeaveModule;
import deluxe.client.module.misc.AutoMessageModule;
import deluxe.client.module.misc.AutoMineModule;
import deluxe.client.module.misc.AutoRegisterModule;
import deluxe.client.module.misc.AutoSellModule;
import deluxe.client.module.movement.AutoSprintModule;
import deluxe.client.module.combat.AutoSwapModule;
import deluxe.client.module.misc.AutoToolModule;
import deluxe.client.module.combat.AutoTotemModule;
import deluxe.client.module.misc.AutoVillagerModule;
import deluxe.client.module.misc.AutoWardenModule;
import deluxe.client.module.combat.BackTrackModule;
import deluxe.client.module.misc.BassBoostModule;
import deluxe.client.module.render.BlockEspModule;
import deluxe.client.module.render.BlockOverlayModule;
import deluxe.client.module.combat.BlockSlotModule;
import deluxe.client.module.render.BrightnessModule;
import deluxe.client.module.render.CameraFeatureModule;
import deluxe.client.module.render.ChamsModule;
import deluxe.client.module.misc.ChestStealerModule;
import deluxe.client.module.misc.ClanUpgraderModule;
import deluxe.client.module.player.ClickActionModule;
import deluxe.client.module.render.CrosshairModule;
import deluxe.client.module.render.CustomWorldModule;
import deluxe.client.module.misc.DiscordRPCModule;
import deluxe.client.module.misc.DuelHelperModule;
import deluxe.client.module.player.ElytraHelperModule;
import deluxe.client.module.movement.FlightModule;
import deluxe.client.module.render.FreeCameraModule;
import deluxe.client.module.misc.GlobalsModule;
import deluxe.client.module.combat.HitBoxesModule;
import deluxe.client.module.misc.InfoReplaceModule;
import deluxe.client.module.render.InterfaceModule;
import deluxe.client.module.combat.ItemHelperModule;
import deluxe.client.module.player.ItemScrollerModule;
import deluxe.client.module.render.JumpCircleModule;
import deluxe.client.module.combat.MaceHelperModule;
import deluxe.client.module.render.MotionBlurModule;
import deluxe.client.module.misc.NameProtectModule;
import deluxe.client.module.combat.NoEntityTraceModule;
import deluxe.client.module.player.NoPlayerDelayModule;
import deluxe.client.module.player.NoPushModule;
import deluxe.client.module.render.NoRenderModule;
import deluxe.client.module.combat.NoServerPackModule;
import deluxe.client.module.movement.NoSlowModule;
import deluxe.client.module.render.ParticlesModule;
import deluxe.client.module.misc.PartyModule;
import deluxe.client.module.misc.PotionCombinerModule;
import deluxe.client.module.player.PotionThrowerModule;
import ru.dreamix.protection.api.impl.profile.ProfileAPI;
import deluxe.client.module.render.ProjectilePredictionModule;
import ru.dreamix.protection.api.impl.profile.objects.RoleObject;
import deluxe.client.module.movement.ScreenWalkModule;
import deluxe.client.module.render.SeeInvisibleModule;
import deluxe.client.module.player.ServerHelperModule;
import deluxe.client.module.render.ShulkerPreviewModule;
import deluxe.client.module.render.SkeletonEspModule;
import deluxe.client.module.render.SmoothFeatureModule;
import deluxe.client.module.movement.SpiderModule;
import deluxe.client.module.misc.StreamerModModule;
import deluxe.client.module.render.StructureInfoModule;
import deluxe.client.module.render.SwordAnimationsModule;
import deluxe.client.module.render.TagsModule;
import deluxe.client.module.render.TargetEspModule;
import deluxe.client.module.render.TorusModule;
import deluxe.client.module.render.TotemGhostModule;
import deluxe.client.module.combat.TriggerBotModule;
import deluxe.client.module.player.UseTrackerModule;
import deluxe.client.module.combat.VelocityModule;
import deluxe.client.module.render.ViewModelModule;
import deluxe.client.module.render.WardenEspModule;
import deluxe.client.module.misc.YTMODEModule;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
   private static final List<Module> modules = new ArrayList<>();
   private static boolean initialized;

   public static Module findByName(String local866_0) {
      for (Module module : modules) {
         if (module.name.equalsIgnoreCase(local866_0)) {
            return module;
         }
      }

      return null;
   }

   public static void registerModule(Module module) {
      if (module != null && !modules.contains(module)) {
         if (!(module instanceof AutoMineModule)
               && !(module instanceof AutoBuyModule)
               && !(module instanceof AutoVillagerModule)
               && !(module instanceof AutoAppleFarmModule)
               && !(module instanceof AutoWardenModule)
            || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.BETA)) {
            if (!(module instanceof YTMODEModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.YOUTUBE)) {
               if (!(module instanceof AdminShardModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
                  if (!(module instanceof AdminRecordModule) || ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.ADMIN)) {
                     modules.add(module);
                  }
               }
            }
         }
      }
   }

   public static List<Module> getModules() {
      return modules;
   }

   private ModuleManager() {
   }

   public static void unregisterModule(Module module) {
      modules.remove(module);
   }

   public static List<Module> getModulesByCategory(ModuleCategory moduleCategory) {
      List<Module> local871_1 = new ArrayList<>();

      for (Module module : modules) {
         if (module.category == moduleCategory) {
            local871_1.add(module);
         }
      }

      return local871_1;
   }

   public static void initializeModules() {
      if (!initialized) {
         initialized = true;
         registerModule(AttackAuraModule.method1029());
         registerModule(MaceHelperModule.method1776());
         registerModule(AutoSwapModule.method2577());
         registerModule(AutoTotemModule.method3667());
         registerModule(TriggerBotModule.method4593());
         registerModule(ItemHelperModule.method3711());
         registerModule(AimAssistModule.method0883());
         registerModule(NoEntityTraceModule.method3535());
         registerModule(NoServerPackModule.method1257());
         registerModule(HitBoxesModule.method1050());
         registerModule(BackTrackModule.method2909());
         registerModule(AutoRegisterModule.method2677());
         registerModule(AuctionHelperModule.method2310());
         registerModule(AutoLeaveModule.method4407());
         registerModule(AutoToolModule.method3003());
         registerModule(AutoEatModule.method3868());
         registerModule(AutoAcceptModule.method3705());
         registerModule(AutoDeliverModule.method3798());
         registerModule(ClanUpgraderModule.method1377());
         if (ProfileAPI.get().getModel().getRole().priorityHigherOrEquals(RoleObject.BETA)) {
            registerModule(AutoMineModule.method3460());
            registerModule(AutoBuyModule.method1734());
            registerModule(AutoVillagerModule.method2125());
            registerModule(AutoAppleFarmModule.method1189());
            registerModule(AutoWardenModule.method1584());
         }

         registerModule(AutoSellModule.method1417());
         registerModule(PotionCombinerModule.method0722());
         registerModule(ChestStealerModule.method0936());
         registerModule(InfoReplaceModule.method3429());
         registerModule(StreamerModModule.method0449());
         registerModule(YTMODEModule.method1426());
         registerModule(NameProtectModule.method2868());
         registerModule(GlobalsModule.method0643());
         registerModule(PartyModule.method2968());
         registerModule(DiscordRPCModule.method3302());
         registerModule(DuelHelperModule.method1172());
         registerModule(AutoMessageModule.method2253());
         registerModule(AirStuckModule.method2785());
         registerModule(AutoSprintModule.method3360());
         registerModule(FlightModule.method2902());
         registerModule(ScreenWalkModule.method0991());
         registerModule(VelocityModule.method1538());
         registerModule(NoSlowModule.method4030());
         registerModule(SpiderModule.method2836());
         registerModule(ItemScrollerModule.method3994());
         registerModule(NoPushModule.method3305());
         registerModule(BlockSlotModule.method1380());
         registerModule(NoRenderModule.method3426());
         ProjectilePredictionModule.method1623();
         registerModule(TagsModule.method4106());
         registerModule(TargetEspModule.method1936());
         registerModule(ArmorBarModule.method4127());
         registerModule(ShulkerPreviewModule.method2995());
         registerModule(SkeletonEspModule.method2471());
         registerModule(ChamsModule.method2783());
         registerModule(BlockEspModule.method3044());
         registerModule(SeeInvisibleModule.method4439());
         registerModule(TorusModule.method4172());
         TotemGhostModule.method2610();
         registerModule(CrosshairModule.method3070());
         registerModule(ArrowsModule.method3119());
         registerModule(BassBoostModule.method4264());
         registerModule(ClickActionModule.method0841());
         registerModule(AutoExpModule.method4034());
         registerModule(ElytraHelperModule.method0984());
         registerModule(NoPlayerDelayModule.method0475());
         registerModule(FreeCameraModule.method0910());
         registerModule(CameraFeatureModule.method0782());
         registerModule(SwordAnimationsModule.method1770());
         registerModule(ViewModelModule.method3027());
         registerModule(InterfaceModule.method2247());
         SmoothFeatureModule.method3278();
         registerModule(AspectRatioModule.method1041());
         registerModule(StructureInfoModule.method3231());
         registerModule(WardenEspModule.method1959());
         registerModule(JumpCircleModule.method0833());
         registerModule(BlockOverlayModule.method3643());
         registerModule(ParticlesModule.method0596());
         registerModule(MotionBlurModule.method3837());
         registerModule(CustomWorldModule.method2204());
         registerModule(BrightnessModule.method0820());
         registerModule(ServerHelperModule.method0707());
         registerModule(PotionThrowerModule.method2641());
         registerModule(UseTrackerModule.method1800());
      }
   }
}
