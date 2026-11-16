package net.skycore.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.skycore.client.module.combat.AimbotModule;
import net.skycore.client.module.combat.AntiBotModule;
import net.skycore.client.module.combat.AntiThornsModule;
import net.skycore.client.module.combat.ArrowSwapModule;
import net.skycore.client.module.combat.AttackAuraModule;
import net.skycore.client.module.combat.AutoExplosionModule;
import net.skycore.client.module.combat.AutoMaceModule;
import net.skycore.client.module.combat.AutoPotionModule;
import net.skycore.client.module.combat.AutoSwapModule;
import net.skycore.client.module.combat.AutoTotemModule;
import net.skycore.client.module.combat.AutoWebTrapModule;
import net.skycore.client.module.combat.CriticalsModule;
import net.skycore.client.module.combat.ElytraTargetModule;
import net.skycore.client.module.combat.TargetPearlModule;
import net.skycore.client.module.combat.TriggerBotModule;
import net.skycore.client.module.misc.AutoDuelModule;
import net.skycore.client.module.misc.AutoJoinerModule;
import net.skycore.client.module.misc.AutoMessageModule;
import net.skycore.client.module.misc.AutoRespawnModule;
import net.skycore.client.module.misc.ClickFriendModule;
import net.skycore.client.module.misc.ClientSoundsModule;
import net.skycore.client.module.misc.DeathCoordsModule;
import net.skycore.client.module.misc.ElytraHelperModule;
import net.skycore.client.module.misc.FTHelperModule;
import net.skycore.client.module.misc.IRCModule;
import net.skycore.client.module.misc.ReallyWorldHelperModule;
import net.skycore.client.module.misc.ScoreboardHPModule;
import net.skycore.client.module.misc.ServerRPSpoofModule;
import net.skycore.client.module.misc.StreamerModeModule;
import net.skycore.client.module.misc.Top4ikModule;
import net.skycore.client.module.movement.AirStuckModule;
import net.skycore.client.module.movement.AutoJumpModule;
import net.skycore.client.module.movement.DragonFlyModule;
import net.skycore.client.module.movement.ElytraMotionModule;
import net.skycore.client.module.movement.FlightModule;
import net.skycore.client.module.movement.FreeCameraModule;
import net.skycore.client.module.movement.GrimGlideModule;
import net.skycore.client.module.movement.GuiMoveModule;
import net.skycore.client.module.movement.JesusModule;
import net.skycore.client.module.movement.NoClipModule;
import net.skycore.client.module.movement.NoFallModule;
import net.skycore.client.module.movement.NoPushModule;
import net.skycore.client.module.movement.NoWebModule;
import net.skycore.client.module.movement.SpeedHVHModule;
import net.skycore.client.module.movement.SprintModule;
import net.skycore.client.module.movement.SuperFireworkModule;
import net.skycore.client.module.movement.TimerModule;
import net.skycore.client.module.movement.WebIgnoreModule;
import net.skycore.client.module.player.AimingItemsModule;
import net.skycore.client.module.player.AntiAFKModule;
import net.skycore.client.module.player.AutoPenisBuilderModule;
import net.skycore.client.module.player.AutoToolModule;
import net.skycore.client.module.player.ChestStealerModule;
import net.skycore.client.module.player.ClickPearlModule;
import net.skycore.client.module.player.CrystalAuraModule;
import net.skycore.client.module.player.FastBreakModule;
import net.skycore.client.module.player.FastEXPModule;
import net.skycore.client.module.player.ItemScrollerModule;
import net.skycore.client.module.player.LockSlotModule;
import net.skycore.client.module.player.NoDelayModule;
import net.skycore.client.module.render.ArrowsModule;
import net.skycore.client.module.render.AtmosphereModule;
import net.skycore.client.module.render.BlockOverlayModule;
import net.skycore.client.module.render.Box3DModule;
import net.skycore.client.module.render.CapeModule;
import net.skycore.client.module.render.ClickGUIModule;
import net.skycore.client.module.render.CometsModule;
import net.skycore.client.module.render.CosmeticsModule;
import net.skycore.client.module.render.CubesModule;
import net.skycore.client.module.render.DuplisheModule;
import net.skycore.client.module.render.DynamicSurroundingsModule;
import net.skycore.client.module.render.FocusModule;
import net.skycore.client.module.render.FogModule;
import net.skycore.client.module.render.FullBrightModule;
import net.skycore.client.module.render.HandsModule;
import net.skycore.client.module.render.InterfaceModule;
import net.skycore.client.module.render.InterpolateF5Module;
import net.skycore.client.module.render.ItemPhysicsModule;
import net.skycore.client.module.render.JumpCirclesModule;
import net.skycore.client.module.render.MotionBlurModule;
import net.skycore.client.module.render.NameTagsModule;
import net.skycore.client.module.render.NoRenderModule;
import net.skycore.client.module.render.ParticlesModule;
import net.skycore.client.module.render.PetsModule;
import net.skycore.client.module.render.PredictionModule;
import net.skycore.client.module.render.SwingAnimationsModule;
import net.skycore.client.module.render.TargetESPModule;
import net.skycore.client.module.render.ThunderShatterModule;
import net.skycore.client.module.render.TrailsModule;
import net.skycore.client.module.render.ViewModelModule;
import net.skycore.client.module.render.WaypointModule;
import net.skycore.client.module.render.WorldTweaksModule;

public final class ModuleManager {
   private final Map<Class<?>, ClientModule> byClass;
   private final Map<String, ClientModule> byName = new LinkedHashMap<>();
   private boolean defaultsRegistered;

   public ModuleManager() {
      this.byClass = new LinkedHashMap<>();
   }

   public synchronized boolean hasEnabledGameplayModules() {
      var stream = this.byName.values().stream();
      Predicate<net.skycore.client.module.ClientModule> predicate = clientModule -> {
         if (clientModule.isEnabled()) {
            if (clientModule.affectsGameplay()) {
               return true;
            }
         }

         return false;
      };
      return stream.anyMatch(predicate);
   }

   private static String normalize(String text) {
      String text2;
      if (text == null) {
         text2 = "";
      } else {
         String text3 = text.trim();
         text2 = text3.toLowerCase(Locale.ROOT);
      }

      return text2;
   }

   public synchronized <T extends ClientModule> Optional<T> get(Class<T> element) {
      ClientModule clientModule = this.byClass.get(element);
      if (clientModule != null) {
         return Optional.of((T)clientModule);
      }

      var values = this.byName.values();

      for (ClientModule clientModule2 : values) {
         if (element.isInstance(clientModule2)) {
            return Optional.of((T)clientModule2);
         }
      }

      return Optional.empty();
   }

   public synchronized List<ClientModule> getByCategory(ModuleCategory moduleCategory) {
      ArrayList elements = new ArrayList();

      for (ClientModule clientModule : this.byName.values()) {
         if (clientModule.getCategory() == moduleCategory) {
            elements.add(clientModule);
         }
      }

      Function<net.skycore.client.module.ClientModule, java.lang.String> function = ClientModule::getName;
      Comparator<net.skycore.client.module.ClientModule> comparator = Comparator.comparing(function, String.CASE_INSENSITIVE_ORDER);
      elements.sort(comparator);
      return List.copyOf(elements);
   }

   public synchronized void registerDefaults() {
      if (this.defaultsRegistered) {
         return;
      }
      this.defaultsRegistered = true;
      this.trialRegister(AutoPotionModule.INSTANCE);
      this.trialRegister(AutoSwapModule.INSTANCE);
      this.trialRegister(AutoExplosionModule.INSTANCE);
      this.trialRegister(new AutoMaceModule());
      this.trialRegister(new AttackAuraModule());
      this.trialRegister(AntiBotModule.INSTANCE);
      this.trialRegister(AntiThornsModule.INSTANCE);
      this.trialRegister(AimbotModule.INSTANCE);
      this.trialRegister(AutoWebTrapModule.INSTANCE);
      this.trialRegister(TargetPearlModule.INSTANCE);
      this.trialRegister(new TriggerBotModule());
      this.trialRegister(CriticalsModule.INSTANCE);
      this.trialRegister(CrystalAuraModule.INSTANCE);
      this.trialRegister(ElytraTargetModule.INSTANCE);
      this.trialRegister(new AutoTotemModule());
      this.trialRegister(net.skycore.client.module.combat.CrystalAuraModule.INSTANCE);
      this.trialRegister(ArrowsModule.INSTANCE);
      this.trialRegister(AtmosphereModule.INSTANCE);
      this.trialRegister(LockSlotModule.INSTANCE);
      this.trialRegister(NoDelayModule.INSTANCE);
      this.trialRegister(FastEXPModule.INSTANCE);
      this.trialRegister(ItemScrollerModule.INSTANCE);
      this.trialRegister(ClickPearlModule.INSTANCE);
      this.trialRegister(FastBreakModule.INSTANCE);
      this.trialRegister(AutoToolModule.INSTANCE);
      this.trialRegister(ChestStealerModule.INSTANCE);
      this.trialRegister(AutoJoinerModule.INSTANCE);
      this.trialRegister(AutoPenisBuilderModule.INSTANCE);
      this.trialRegister(ArrowSwapModule.INSTANCE);
      this.trialRegister(new InterpolateF5Module());
      this.trialRegister(new InterfaceModule());
      this.trialRegister(new FullBrightModule());
      this.trialRegister(HandsModule.INSTANCE);
      this.trialRegister(new FocusModule());
      this.trialRegister(new FogModule());
      this.trialRegister(new DuplisheModule());
      this.trialRegister(DynamicSurroundingsModule.INSTANCE);
      this.trialRegister(new CometsModule());
      this.trialRegister(CubesModule.INSTANCE);
      this.trialRegister(new BlockOverlayModule());
      this.trialRegister(CapeModule.INSTANCE);
      this.trialRegister(new TargetESPModule());
      this.trialRegister(Box3DModule.INSTANCE);
      this.trialRegister(SwingAnimationsModule.INSTANCE);
      this.trialRegister(new NoRenderModule());
      this.trialRegister(new CosmeticsModule());
      this.trialRegister(new PetsModule());
      this.trialRegister(PredictionModule.INSTANCE);
      this.trialRegister(ParticlesModule.INSTANCE);
      this.trialRegister(new MotionBlurModule());
      this.trialRegister(new NameTagsModule());
      this.trialRegister(ItemPhysicsModule.INSTANCE);
      this.trialRegister(JumpCirclesModule.INSTANCE);
      this.trialRegister(new WorldTweaksModule());
      this.trialRegister(ViewModelModule.INSTANCE);
      this.trialRegister(new WaypointModule());
      this.trialRegister(ThunderShatterModule.INSTANCE);
      this.trialRegister(TrailsModule.INSTANCE);
      this.trialRegister(ReallyWorldHelperModule.INSTANCE);
      this.trialRegister(FTHelperModule.INSTANCE);
      this.trialRegister(IRCModule.INSTANCE);
      this.trialRegister(DeathCoordsModule.INSTANCE);
      this.trialRegister(ElytraHelperModule.INSTANCE);
      this.trialRegister(ClickFriendModule.INSTANCE);
      this.trialRegister(new ClientSoundsModule());
      this.trialRegister(AutoMessageModule.INSTANCE);
      this.trialRegister(AutoRespawnModule.INSTANCE);
      this.trialRegister(new ClickGUIModule());
      this.trialRegister(AutoDuelModule.INSTANCE);
      this.trialRegister(GrimGlideModule.INSTANCE);
      this.trialRegister(GuiMoveModule.INSTANCE);
      this.trialRegister(FlightModule.INSTANCE);
      this.trialRegister(FreeCameraModule.INSTANCE);
      this.trialRegister(DragonFlyModule.INSTANCE);
      this.trialRegister(ElytraMotionModule.INSTANCE);
      this.trialRegister(AirStuckModule.INSTANCE);
      this.trialRegister(AutoJumpModule.INSTANCE);
      this.trialRegister(StreamerModeModule.INSTANCE);
      this.trialRegister(Top4ikModule.INSTANCE);
      this.trialRegister(new ScoreboardHPModule());
      this.trialRegister(ServerRPSpoofModule.INSTANCE);
      this.trialRegister(AimingItemsModule.INSTANCE);
      this.trialRegister(AntiAFKModule.INSTANCE);
      this.trialRegister(TimerModule.INSTANCE);
      this.trialRegister(WebIgnoreModule.INSTANCE);
      this.trialRegister(SprintModule.INSTANCE);
      this.trialRegister(SuperFireworkModule.INSTANCE);
      this.trialRegister(SpeedHVHModule.INSTANCE);
      this.trialRegister(NoPushModule.INSTANCE);
      this.trialRegister(NoWebModule.INSTANCE);
      this.trialRegister(NoFallModule.INSTANCE);
      this.trialRegister(JesusModule.INSTANCE);
      this.trialRegister(NoClipModule.INSTANCE);
   }

   public synchronized Collection<ClientModule> enabled() {
      var stream = this.byName.values().stream();
      stream = stream.filter(ClientModule::isEnabled);
      return stream.toList();
   }

   public void tickEnabled() {
      var modules = this.getModules();
      Iterator iterator = modules.iterator();

      while (iterator.hasNext()) {
         ClientModule element = (ClientModule)iterator.next();
         element.tick();
      }
   }

   public synchronized List<ClientModule> getModules() {
      var values = this.byName.values();
      return List.copyOf(values);
   }

   public synchronized Optional<ClientModule> get(String text) {
      return Optional.ofNullable(this.byName.get(normalize(text)));
   }

   public synchronized <T extends ClientModule> T register(T t) {
      Objects.requireNonNull(t);
      String name = normalize(t.getName());
      if (this.byName.containsKey(name)) {
         String name2 = t.getName();
         throw new IllegalArgumentException("Duplicate module name: " + name2);
      } else {
         if (this.byClass.containsKey(t.getClass())) {
            String name3 = t.getClass().getName();
            throw new IllegalArgumentException("Duplicate module class: " + name3);
         } else {
            this.byName.put(name, t);
            var byClass2 = this.byClass;
            Class element = t.getClass();
            byClass2.put(element, t);
            return (T)t;
         }
      }
   }

   private ClientModule trialRegister(ClientModule clientModule) {
      this.byName.putIfAbsent(normalize(clientModule.getName()), clientModule);
      this.byClass.putIfAbsent(clientModule.getClass(), clientModule);
      return clientModule;
   }
}
