package net.skycore.client.module.render;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.skycore.client.SkycoreClient;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.module.ModuleManager;
import net.skycore.client.setting.MultiSelectSetting;
import net.skycore.client.setting.BooleanSetting;

public final class NoRenderModule extends ClientModule {
   private final MultiSelectSetting world;
   private final BooleanSetting cameraClip = this.add(new BooleanSetting("Camera Clip", false));
   private final MultiSelectSetting overlay;
   private final MultiSelectSetting sounds;
   private boolean lastGrass;
   private final BooleanSetting removeVignette = this.add(new BooleanSetting("Убрать затемнение", false));

   public static boolean shouldRemoveVignette() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         Boolean get = noRenderModule2.removeVignette.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   private void reloadTerrain() {
      Minecraft instance = Minecraft.getInstance();
      if (instance.level != null) {
         instance.levelExtractor.allChanged();
      }
   }

   @Override
   protected void onDisable() {
      if (this.lastGrass) {
         this.reloadTerrain();
      }

      this.lastGrass = false;
   }

   public static boolean shouldRemoveSound(SoundEvent soundEvent) {
      NoRenderModule noRenderModule2 = enabled();
      if (soundEvent == null) {
         return false;
      }

      String path = soundEvent.location().getPath();
      if (!AtmosphereModule.INSTANCE.shouldHideVanillaWeather() || !path.contains("weather.rain") && !path.contains("ambient.weather")) {
         if (noRenderModule2 == null) {
            return false;
         }

         if (noRenderModule2.sounds.isSelected("Totem") && path.contains("totem")) {
            return true;
         }

         if (noRenderModule2.sounds.isSelected("Firework") && path.contains("firework")) {
            return true;
         }

         if (noRenderModule2.sounds.isSelected("Step") && path.contains("step")) {
            return true;
         }

         if (!noRenderModule2.sounds.isSelected("Eat") || !path.contains("eat") && !path.contains("drink") && !path.contains("burp")) {
            if (!noRenderModule2.sounds.isSelected("Hit") || !path.contains("attack") && !path.contains("hit")) {
               return !noRenderModule2.sounds.isSelected("Warden")
                     || !path.contains("warden") && !path.contains("sculk_sensor") && !path.contains("sculk_shrieker") && !path.contains("shriek")
                  ? noRenderModule2.sounds.isSelected("Hurt") && path.contains("hurt")
                  : true;
            } else {
               return true;
            }
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static boolean isGrassSelected() {
      ModuleManager moduleManager = SkycoreClient.modules();
      NoRenderModule noRenderModule2 = moduleManager.get(NoRenderModule.class).orElse(null);
      if (noRenderModule2 != null) {
         if (noRenderModule2.isEnabled() && noRenderModule2.world.isSelected("Grass")) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void onEnable() {
      if (isGrassSelected()) {
         this.reloadTerrain();
      }

      boolean grassSelected = isGrassSelected();
      this.lastGrass = grassSelected;
   }

   public static boolean shouldRemoveWeather() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.world.isSelected("Weather")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldHideGrass(BlockState blockState) {
      if (shouldRemoveGrass() && blockState != null) {
         Block block = blockState.getBlock();
         if (block != Blocks.SHORT_GRASS) {
            if (block != Blocks.TALL_GRASS) {
               if (block != Blocks.FERN && block != Blocks.LARGE_FERN) {
                  if (block != Blocks.DEAD_BUSH) {
                     if (block != Blocks.SEAGRASS) {
                        if (block != Blocks.TALL_SEAGRASS) {
                           if (block != Blocks.KELP) {
                              if (block != Blocks.KELP_PLANT && block != Blocks.VINE && block != Blocks.GLOW_LICHEN) {
                                 return false;
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static boolean shouldRemoveEffectIcons() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Effect Icons")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveOtherPlayers() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.world.isSelected("Players")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldDisableCameraClipping() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         Boolean get = noRenderModule2.cameraClip.get();
         if (get) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveFireOverlay() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Fire")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveGrass() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.world.isSelected("Grass")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveScoreboard() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Scoreboard")) {
            return true;
         }
      }

      return false;
   }

   private static NoRenderModule enabled() {
      var optional = SkycoreClient.modules().get(NoRenderModule.class);
      NoRenderModule noRenderModule2 = (NoRenderModule)optional.orElse(null);
      if (noRenderModule2 != null) {
         if (noRenderModule2.isEnabled()) {
            return noRenderModule2;
         }
      }

      return null;
   }

   public static boolean shouldRemoveParticle(ParticleOptions particleOptions) {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null && particleOptions != null) {
         if (noRenderModule2.world.isSelected("Totem Particles")) {
            ParticleType type = particleOptions.getType();
            if (type == ParticleTypes.TOTEM_OF_UNDYING) {
               return true;
            }
         }

         return noRenderModule2.world.isSelected("Effects");
      } else {
         return false;
      }
   }

   public static boolean shouldRemoveTotemOverlay() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Totem Overlay")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveBadEffectsVisuals() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Bad Effects")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveBossBar() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.overlay.isSelected("Boss Bar")) {
            return true;
         }
      }

      return false;
   }

   public static boolean shouldRemoveThirdPersonSelf() {
      NoRenderModule noRenderModule2 = enabled();
      if (noRenderModule2 != null) {
         if (noRenderModule2.world.isSelected("Third Person Self")) {
            return true;
         }
      }

      return false;
   }

   public NoRenderModule() {
      super("No Render", "Hides overlays, world effects, sounds, and third-person camera clipping", ModuleCategory.RENDER);
      this.overlay = this.add(
         new MultiSelectSetting(
            "Overlay", List.of("Fire", "Bad Effects", "Effect Icons", "Shaking", "Totem Overlay", "Scoreboard", "Boss Bar"), Set.of("Fire", "Bad Effects")
         )
      );
      this.world = this.add(
         new MultiSelectSetting("World", List.of("Weather", "Totem Particles", "Effects", "Grass", "Third Person Self", "Players"), Set.of("Weather"))
      );
      this.sounds = this.add(new MultiSelectSetting("Sounds", List.of("Hit", "Hurt", "Totem", "Step", "Eat", "Firework", "Warden"), Set.of()));
   }

   public static boolean shouldRemoveShaking() {
      NoRenderModule noRenderModule2 = enabled();
      return noRenderModule2 != null && noRenderModule2.overlay.isSelected("Shaking");
   }

   @Override
   protected void onTick() {
      boolean grassSelected = isGrassSelected();
      if (grassSelected != this.lastGrass) {
         this.lastGrass = grassSelected;
         this.reloadTerrain();
      }
   }
}
