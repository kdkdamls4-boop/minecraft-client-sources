package net.skycore.client.util;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.Vec3;

public final class CheckHeldItemHelper {
   private final ArrayDeque<CheckHeldItemHelper.Accent> queuedAccents;
   private boolean wet;
   private String lastBiome;
   private final Random rng = new Random();
   private Object world;
   private int ticks;
   private final Map<String, CheckHeldItemHelper.AmbienceLoop> loops = new HashMap<>();
   private boolean outside;
   private boolean cave;
   private boolean night;
   private String lastItem;

   private void checkHeldItem(Minecraft minecraft) {
      ItemStack mainHandItem = minecraft.player.getMainHandItem();
      String text = mainHandItem.isEmpty() ? "" : mainHandItem.getItem().toString();
      if (!text.equals(this.lastItem)) {
         this.lastItem = text;
         if (!text.isEmpty() && this.ticks >= 10) {
            String text2 = text.contains("sword")
               ? "item.sword.equip"
               : (
                  !text.contains("axe") && !text.contains("mace")
                     ? (
                        text.contains("bow")
                           ? "item.bow.equip"
                           : (text.contains("shield") ? "item.shield.equip" : (text.contains("potion") ? "item.potion.equip" : "item.tool.equip"))
                     )
                     : "item.blunt.equip"
               );
            play(minecraft, text2, minecraft.player.position(), 0.22F, SoundSource.PLAYERS);
         }
      }
   }

   public CheckHeldItemHelper() {
      this.queuedAccents = new ArrayDeque<>();
      this.lastBiome = "";
      this.lastItem = "";
   }

   public void onVanillaSound(SoundInstance soundInstance, boolean condition) {
      if (condition && soundInstance.getIdentifier().getNamespace().equals("minecraft")) {
         String path = soundInstance.getIdentifier().getPath();

         String text = switch (path) {
            case "block.grass.step" -> "footsteps.grass";
            case "block.gravel.step" -> "footsteps.gravel";
            case "block.sand.step" -> "footsteps.sand";
            case "block.dirt.step" -> "footsteps.dirt";
            case "block.stone.step" -> "footsteps.stone";
            case "block.snow.step" -> "footsteps.snow";
            case "block.glass.step" -> "footsteps.wood";
            case "block.wood.step" -> "footsteps.wood";
            case "block.metal.step" -> "footsteps.metalbox";
            case "block.wool.step" -> "footsteps.rug";
            default -> null;
         };
         if (text != null && this.queuedAccents.size() < 12 && !(this.rng.nextFloat() >= 0.7F)) {
            Minecraft instance = Minecraft.getInstance();
            if (instance.level != null && instance.player != null) {
               Vec3 position = new Vec3(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
               if (!(position.distanceToSqr(instance.player.position()) > 144.0)) {
                  BlockPos blockPos = BlockPos.containing(position.x, position.y - 0.12, position.z);
                  if (instance.level.hasChunkAt(blockPos)) {
                     String path2 = BuiltInRegistries.BLOCK.getKey(instance.level.getBlockState(blockPos).getBlock()).getPath();
                     if (path.equals("block.grass.step")) {
                        if (path2.contains("leaves") || path2.equals("hay_block")) {
                           text = "footsteps.leaves_through";
                        } else if (path2.equals("dirt_path")) {
                           text = "footsteps.dirt";
                        }
                     } else if (!path.equals("block.glass.step") || !path2.contains("ice") && !path2.equals("frosted_ice")) {
                        if (path.equals("block.wood.step")) {
                           if (path2.contains("_log") || path2.contains("_stem") || path2.contains("_wood")) {
                              text = "footsteps.log";
                           } else if (path2.contains("fence") || path2.contains("gate")) {
                              text = "footsteps.bluntwood";
                           } else if (path2.contains("mushroom") || path2.contains("beehive")) {
                              text = "footsteps.organic";
                           }
                        } else if (path.equals("block.stone.step") && path2.contains("cauldron")) {
                           text = "footsteps.metalbox";
                        } else if (path.equals("block.metal.step") && path2.equals("iron_bars")) {
                           text = "footsteps.metalbar";
                        }
                     } else {
                        text = "footsteps.muffledice";
                     }
                  }

                  this.queuedAccents.addLast(new CheckHeldItemHelper.Accent(text, position, 0.19F + this.rng.nextFloat() * 0.09F, SoundSource.PLAYERS));
               }
            }
         }
      }
   }

   private static Map<String, Float> select(CheckHeldItemHelper.Environment environment, float value) {
      HashMap lookup = new HashMap();
      if (environment.dimension().contains("nether")) {
         lookup.put("dimension.nether", value * 0.43F);
         return lookup;
      }

      if (environment.dimension().contains("end")) {
         lookup.put("dimension.theend", value * 0.4F);
         return lookup;
      }

      String text = environment.biome();
      if (environment.underwater()) {
         String text2 = text.contains("deep_ocean")
            ? "biome.ocean.deep.under"
            : (text.contains("ocean") ? "biome.ocean.under" : (text.contains("river") ? "biome.river.under" : "biome.water.under"));
         lookup.put(text2, value * 0.5F);
         return lookup;
      }

      if (environment.cave()) {
         lookup.put("biome.underground", value * 0.32F);
         return lookup;
      }

      if (!environment.outside()) {
         return lookup;
      }

      String text3;
      if (text.contains("beach")) {
         text3 = "biome.beach";
      } else if (text.contains("deep_ocean")) {
         text3 = "biome.ocean.deep";
      } else if (text.contains("ocean")) {
         text3 = "biome.ocean";
      } else if (text.contains("river")) {
         text3 = "biome.river";
      } else if (text.contains("jungle") || text.contains("bamboo")) {
         text3 = "biome.jungle";
      } else if (text.contains("savanna")) {
         text3 = environment.night() ? "biome.savanna.night" : "biome.savanna.day";
      } else if (text.contains("swamp") || text.contains("mangrove")) {
         text3 = "biome.bog";
      } else if (text.contains("old_growth") || text.contains("taiga")) {
         text3 = "biome.forest.pine";
      } else if (text.contains("dark_forest")) {
         text3 = "biome.forest.silent";
      } else if (text.contains("forest") || text.contains("grove")) {
         text3 = "biome.forest";
      } else if (!text.contains("plains") && !text.contains("meadow")) {
         text3 = null;
      } else {
         text3 = "biome.plains";
      }

      if (text3 != null && !environment.rainy()) {
         lookup.put(text3, value * 0.42F);
      }

      if (text.contains("frozen") || text.contains("snow") || text.contains("ice")) {
         lookup.put("biome.wind.arctic", value * 0.28F);
      } else if (text.contains("desert") || text.contains("badlands")) {
         lookup.put("biome.wind.desert", value * 0.28F);
      } else if (text.contains("peak") || text.contains("mountain") || text.contains("slope")) {
         lookup.put("biome.wind.mountains", value * 0.24F);
      } else if (text.contains("windswept")) {
         lookup.put("biome.wind.hills", value * 0.25F);
      } else if (text3 == null) {
         lookup.put("biome.wind", value * 0.18F);
      } else if (environment.rainy()) {
         lookup.put("biome.wind", value * 0.13F);
      }

      if (environment.night() && !environment.rainy() && !text.contains("snow") && !text.contains("frozen") && !text.contains("desert") && !text.contains("ocean")) {
         lookup.put("biome.crickets", value * 0.24F);
      }

      return lookup;
   }

   public void stopAll(Minecraft minecraft) {
      Iterator iterator = this.loops.values().iterator();

      while (true) {
         if (!iterator.hasNext()) {
            this.loops.clear();
            this.queuedAccents.clear();
            this.world = null;
            this.lastItem = "";
            this.ticks = 0;
            this.cave = false;
            this.outside = this.night = this.wet = false;
            this.lastBiome = "";
            return;
         }

         CheckHeldItemHelper.AmbienceLoop element = (CheckHeldItemHelper.AmbienceLoop)iterator.next();
         SoundManager soundManager = minecraft.getSoundManager();
         soundManager.stop(element);
      }
   }

   private void details(Minecraft minecraft, float value) {
      String text = null;
      if (this.wet) {
         if (this.rng.nextInt(5) == 0) {
            text = "whale";
         }
      } else if (this.cave) {
         if (this.rng.nextInt(3) == 0) {
            text = this.rng.nextBoolean() ? "waterdrops" : "rockfall";
         }
      } else if (this.outside && !minecraft.level.isRaining()) {
         if (this.night) {
            if (this.rng.nextInt(6) == 0) {
               text = this.lastBiome.contains("forest") ? "owl" : "coyote";
            }
         } else if (!this.lastBiome.contains("beach") && !this.lastBiome.contains("ocean")) {
            if (!this.lastBiome.contains("forest") && !this.lastBiome.contains("taiga") && !this.lastBiome.contains("jungle")) {
               if ((this.lastBiome.contains("plains") || this.lastBiome.contains("savanna")) && this.rng.nextInt(7) == 0) {
                  text = "bird";
               }
            } else if (this.rng.nextInt(4) == 0) {
               text = this.rng.nextBoolean() ? "bird" : "woodpecker";
            }
         } else if (this.rng.nextInt(4) == 0) {
            text = "seagulls";
         }
      }

      if (text != null) {
         Vec3 position = minecraft.player.position();
         double computedValue = this.rng.nextDouble() * Math.PI * 2.0;
         double computedValue2 = 4.0 + this.rng.nextDouble() * 9.0;
         Vec3 position2 = position.add(Math.cos(computedValue) * computedValue2, 0.5 + this.rng.nextDouble() * 2.0, Math.sin(computedValue) * computedValue2);
         play(minecraft, text, position2, value * (0.18F + this.rng.nextFloat() * 0.22F), SoundSource.AMBIENT);
      }
   }

   private static void play(Minecraft minecraft, String text, Vec3 position, float value, SoundSource soundSource) {
      if (!(value <= 0.001F)) {
         SoundManager soundManager = minecraft.getSoundManager();
         Identifier texture = Identifier.parse("dsurround:" + text);
         float minimum = Math.min(0.8F, value);
         RandomSource randomSource = RandomSource.create();
         soundManager.play(new SimpleSoundInstance(texture, soundSource, minimum, 1.0F, randomSource, false, 0, Attenuation.LINEAR, position.x, position.y, position.z, false));
      }
   }

   public void tick(Minecraft world2, boolean condition, float value, boolean condition2, boolean condition3, boolean condition4) {
      if (world2.level != null && world2.player != null) {
         if (this.world != world2.level) {
            this.stopAll(world2);
            this.world = world2.level;
         }

         this.ticks++;
         if (!condition3) {
            this.queuedAccents.clear();
         }

         int value2 = 3;

         while (condition3 && value2-- > 0 && !this.queuedAccents.isEmpty()) {
            CheckHeldItemHelper.Accent accent = this.queuedAccents.pollFirst();
            play(world2, accent.event(), accent.origin(), accent.gain(), accent.category());
         }

         if (this.ticks % 10 == 1) {
            CheckHeldItemHelper.Environment environment = inspect(world2);
            this.lastBiome = environment.biome();
            this.outside = environment.outside();
            this.night = environment.night();
            this.wet = environment.underwater();
            this.cave = environment.cave();
            Map<String, Float> lookup = condition && value > 0.001F ? select(environment, value) : Map.of();

            for (Entry entry : this.loops.entrySet()) {
               ((CheckHeldItemHelper.AmbienceLoop)entry.getValue()).target(lookup.getOrDefault(entry.getKey(), 0.0F));
            }

            for (Entry entry2 : lookup.entrySet()) {
               if (!((Float)entry2.getValue() < 0.01F)) {
                  CheckHeldItemHelper.AmbienceLoop key = this.loops.get(entry2.getKey());
                  if (key != null && !key.isStopped()) {
                     key.target((Float)entry2.getValue());
                  } else {
                     CheckHeldItemHelper.AmbienceLoop ambienceLoop = new CheckHeldItemHelper.AmbienceLoop((String)entry2.getKey(), (Float)entry2.getValue());
                     this.loops.put((String)entry2.getKey(), ambienceLoop);
                     world2.getSoundManager().play(ambienceLoop);
                  }
               }
            }

            this.clearFinished(world2);
         }

         if (condition2 && this.ticks % 30 == 0) {
            this.details(world2, value);
         }

         if (condition4 && this.ticks % 2 == 0) {
            this.checkHeldItem(world2);
         }
      } else {
         this.stopAll(world2);
      }
   }

   public boolean fireflyConditions() {
      if (this.outside && this.night) {
         if (!this.wet) {
            if (!this.cave) {
               if (this.lastBiome.contains("forest") || this.lastBiome.contains("swamp")) {
                  return true;
               }

               if (this.lastBiome.contains("plains") || this.lastBiome.contains("jungle")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private void clearFinished(Minecraft minecraft) {
      var entries = this.loops.entrySet();
      Iterator iterator = entries.iterator();

      while (true) {
         if (!iterator.hasNext()) {
            return;
         }

         Entry element = (Entry)iterator.next();
         if (((CheckHeldItemHelper.AmbienceLoop)element.getValue()).isStopped()) {
            SoundManager soundManager = minecraft.getSoundManager();
            soundManager.stop((SoundInstance)element.getValue());
            iterator.remove();
         }
      }
   }

   private static CheckHeldItemHelper.Environment inspect(Minecraft minecraft) {
      BlockPos blockPos = minecraft.player.blockPosition();
      String biome = minecraft.level.getBiome(blockPos).unwrapKey().map(resourceKey -> {
         Identifier texture = resourceKey.identifier();
         return texture.getPath();
      }).orElse("plains");
      String path = minecraft.level.dimension().identifier().getPath();
      BlockPos eyePosition = BlockPos.containing(minecraft.player.getEyePosition());
      int height = !minecraft.level.dimensionType().hasCeiling() && minecraft.level.getHeight(Types.MOTION_BLOCKING, eyePosition.getX(), eyePosition.getZ()) <= eyePosition.getY() + 2 ? 1 : 0;
      long overworldClockTime = minecraft.level.getOverworldClockTime() % 24000L;
      int value = overworldClockTime > 13000L && overworldClockTime < 23000L ? 1 : 0;
      boolean raining = minecraft.level.isRaining();
      boolean is = minecraft.level.getFluidState(eyePosition).is(FluidTags.WATER);
      int y = path.equals("overworld") && !is && height == 0 && eyePosition.getY() < minecraft.level.getHeight(Types.MOTION_BLOCKING, eyePosition.getX(), eyePosition.getZ()) - 8
         ? 1
         : 0;
      return new CheckHeldItemHelper.Environment(biome, path, (height != 0), (value != 0), raining, is, (y != 0));
   }

   public record Accent(String event, Vec3 origin, float gain, SoundSource category) {
      public String event() {
         return this.event;
      }

      public SoundSource category() {
         return this.category;
      }

      public float gain() {
         return this.gain;
      }

      public Vec3 origin() {
         return this.origin;
      }
   }

   static final class AmbienceLoop extends AbstractTickableSoundInstance {
      private float target;
      private boolean retiring;

      @Override
      public void tick() {
         float value = this.target > this.volume ? 0.16F : 0.095F;
         float computedValue = this.volume + (this.target - this.volume) * value;
         this.volume = computedValue;
         if (this.retiring) {
            if (this.volume < 0.007F) {
               this.stop();
            }
         }
      }

      private AmbienceLoop(String text, float target2) {
         super(SoundEvent.createVariableRangeEvent(Identifier.parse("dsurround:" + text)), SoundSource.AMBIENT, RandomSource.create());
         this.looping = true;
         this.relative = true;
         this.attenuation = Attenuation.NONE;
         this.volume = 0.005F;
         this.pitch = 1.0F;
         this.target = target2;
      }

      private void target(float value) {
         int intermediate = -1;
         intermediate = (byte)-1;
         float maximum = Math.max(0.0F, value);
         this.target = maximum;
         intermediate = value <= 0.0F ? 1 : 0;
         this.retiring = (intermediate != 0);
      }
   }

   public record Environment(String biome, String dimension, boolean outside, boolean night, boolean rainy, boolean underwater, boolean cave) {
      public boolean outside() {
         return this.outside;
      }

      public boolean rainy() {
         return this.rainy;
      }

      public String biome() {
         return this.biome;
      }

      public boolean night() {
         return this.night;
      }

      public boolean underwater() {
         return this.underwater;
      }

      public String dimension() {
         return this.dimension;
      }
   }
}
