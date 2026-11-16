package net.skycore.client.module.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.skycore.client.SkycoreClient;
import net.skycore.client.event.WorldChangeEvent;
import net.skycore.client.mixin.ChannelGetSourceAccessor;
import net.skycore.client.module.ClientModule;
import net.skycore.client.module.ModuleCategory;
import net.skycore.client.util.CheckHeldItemHelper;
import net.skycore.client.setting.NumberSetting;
import net.skycore.client.setting.StringSetting;
import net.skycore.client.util.AcousticMaterials;
import net.skycore.client.util.SoundEffectsProcessor;
import net.skycore.client.util.RenderWorldHelper;
import net.skycore.client.setting.BooleanSetting;

public final class DynamicSurroundingsModule extends ClientModule {
   private final NumberSetting reflectionsGainMultiplier;
   private final BooleanSetting equipped;
   private final NumberSetting lateGainMultiplier;
   private final CheckHeldItemHelper soundscape;
   private Set<Identifier> highReverb;
   private final NumberSetting waterGainHF;
   private float prevSky;
   private final NumberSetting obstructionMax;
   private final NumberSetting lateGainBase;
   private Set<Identifier> lowReverb;
   private Map<Identifier, Float> dimensionBase;
   private final NumberSetting lateDelayMultiplier;
   private volatile SoundEffectsProcessor.ReverbValues reverbValues;
   private final BooleanSetting reverb;
   private final NumberSetting reflectionsDelayMultiplier;
   private final BooleanSetting footsteps;
   private final NumberSetting decayHF;
   private final BooleanSetting liquid;
   private float prevDecay;
   private final BooleanSetting fireflies;
   private final NumberSetting diffusion;
   private final BooleanSetting obstruction;
   private final BooleanSetting details;
   private int scanTicks;
   private final StringSetting heavyBlocks;
   private final NumberSetting lavaGain;
   private final BooleanSetting defaultMaterials;
   private String previousHigh;
   private final NumberSetting reverbPercent;
   private String previousLow;
   private final NumberSetting density;
   private Set<Identifier> ignored;
   private final RenderWorldHelper visuals;
   public static final DynamicSurroundingsModule INSTANCE = new DynamicSurroundingsModule();
   private final NumberSetting ambienceVolume;
   private final NumberSetting maxBlocks;
   private final NumberSetting gainHF;
   private final BooleanSetting checkSky;
   private final StringSetting dimensions;
   private String previousDimension;
   private final StringSetting lowBlocks;
   private final StringSetting ignoredEvents;
   private Set<Identifier> highObstruction;
   private String previousHeavy;
   private float prevRoom;
   private final NumberSetting waterGain;
   private final StringSetting highBlocks;
   private final NumberSetting lavaGainHF;
   private final BooleanSetting rainSplashes;
   private String previousIgnore;
   private float liquidGainHF;
   private float liquidGain;
   private final NumberSetting minDecay;
   private final NumberSetting airAbsorptionHF;
   private final BooleanSetting ambience = this.add(new BooleanSetting("Ambient Soundscapes", true));
   private ChannelAccess channels;
   private int soundTicks;
   private final NumberSetting gain;
   private final NumberSetting obstructionStep;
   private final Map<SoundInstance, Float> obstructions;
   private final NumberSetting reflectionsGainBase;

   private DynamicSurroundingsModule() {
      super("DynamicSurroundings", "Standalone Dynamic Surroundings audio, ambience and environment effects for 26.2", ModuleCategory.RENDER);
      this.ambienceVolume = this.number("Ambience Volume", 0.75, 0.0, 1.0, 0.05);
      this.details = this.add(new BooleanSetting("Wildlife and Cave Sounds", true));
      this.footsteps = this.add(new BooleanSetting("Footstep Accents", true));
      this.equipped = this.add(new BooleanSetting("Equip Sounds", true));
      this.fireflies = this.add(new BooleanSetting("Fireflies", true));
      this.rainSplashes = this.add(new BooleanSetting("Rain Splashes", true));
      this.liquid = this.add(new BooleanSetting("Liquid Filter", true));
      this.waterGain = this.number("Water Volume", 0.8, 0.0, 1.0, 0.01);
      this.waterGainHF = this.number("Water High Frequencies", 0.3, 0.0, 1.0, 0.01);
      this.lavaGain = this.number("Lava Volume", 0.5, 0.0, 1.0, 0.01);
      this.lavaGainHF = this.number("Lava High Frequencies", 0.1, 0.0, 1.0, 0.01);
      this.reverb = this.add(new BooleanSetting("Reverb Filter", true));
      this.reverbPercent = this.number("Reverb Amount", 1.0, 0.0, 2.0, 0.01);
      this.maxBlocks = this.number("Reverb Scan Blocks", 1024.0, 64.0, 4096.0, 1.0);
      this.checkSky = this.add(new BooleanSetting("Check Sky Access", true));
      this.dimensions = this.add(new StringSetting("Dimension Reverb", "minecraft:the_nether;1.0", 2048));
      this.defaultMaterials = this.add(new BooleanSetting("Default Material Lists", true));
      this.lowBlocks = this.add(new StringSetting("Low Reverb Blocks", "", 8192));
      this.highBlocks = this.add(new StringSetting("High Reverb Blocks", "", 8192));
      this.density = this.number("Reverb Density", 0.2, 0.0, 1.0, 0.01);
      this.diffusion = this.number("Reverb Diffusion", 0.6, 0.0, 1.0, 0.01);
      this.gain = this.number("Reverb Gain", 0.15, 0.0, 1.0, 0.01);
      this.gainHF = this.number("Reverb High Frequencies", 0.8, 0.0, 1.0, 0.01);
      this.minDecay = this.number("Minimum Decay Time", 0.1, 0.1, 20.0, 0.01);
      this.decayHF = this.number("Decay High Frequency Ratio", 0.7, 0.1, 2.0, 0.01);
      this.airAbsorptionHF = this.number("Air Absorption High Frequencies", 0.99, 0.892, 1.0, 0.001);
      this.reflectionsGainBase = this.number("Reflections Gain Base", 0.05, 0.0, 3.16, 0.01);
      this.reflectionsGainMultiplier = this.number("Reflections Gain Multiplier", 0.05, 0.0, 3.16, 0.01);
      this.reflectionsDelayMultiplier = this.number("Reflections Delay Multiplier", 0.025, 0.0, 0.3, 0.001);
      this.lateGainBase = this.number("Late Reverb Gain Base", 1.26, 0.0, 10.0, 0.01);
      this.lateGainMultiplier = this.number("Late Reverb Gain Multiplier", 0.1, 0.0, 10.0, 0.01);
      this.lateDelayMultiplier = this.number("Late Reverb Delay Multiplier", 0.01, 0.0, 0.1, 0.001);
      this.obstruction = this.add(new BooleanSetting("Obstruction Filter", true));
      this.obstructionStep = this.number("Obstruction Per Block", 0.1, 0.0, 1.0, 0.01);
      this.obstructionMax = this.number("Maximum Obstruction", 0.98, 0.0, 0.98, 0.01);
      this.heavyBlocks = this.add(
         new StringSetting(
            "Heavy Obstruction Blocks",
            "minecraft:black_wool,minecraft:blue_wool,minecraft:brown_wool,minecraft:cyan_wool,minecraft:gray_wool,minecraft:green_wool,minecraft:light_blue_wool,minecraft:light_gray_wool,minecraft:lime_wool,minecraft:magenta_wool,minecraft:orange_wool,minecraft:pink_wool,minecraft:purple_wool,minecraft:red_wool,minecraft:white_wool,minecraft:yellow_wool,minecraft:sponge,minecraft:wet_sponge",
            4096
         )
      );
      this.ignoredEvents = this.add(
         new StringSetting(
            "Ignored Sound Events",
            "minecraft:music.creative,minecraft:music.credits,minecraft:music.dragon,minecraft:music.end,minecraft:music.game,minecraft:music.menu,minecraft:music.nether.basalt_deltas,minecraft:music.nether.nether_wastes,minecraft:music.nether.soul_sand_valley,minecraft:music.nether.crimson_forest,minecraft:music.nether.warped_forest,minecraft:music.under_water,minecraft:music.overworld.deep_dark,minecraft:music.overworld.dripstone_caves,minecraft:music.overworld.frozen_peaks,minecraft:music.overworld.grove,minecraft:music.overworld.jagged_peaks,minecraft:music.overworld.jungle_and_forest,minecraft:music.overworld.lush_caves,minecraft:music.overworld.meadow,minecraft:music.overworld.old_growth_taiga,minecraft:music.overworld.snowy_slopes,minecraft:music.overworld.stony_peaks,minecraft:music.overworld.swamp,minecraft:ui.button.click,minecraft:ui.toast.challenge_complete,minecraft:ui.toast.in,minecraft:ui.toast.out",
            4096
         )
      );
      this.soundscape = new CheckHeldItemHelper();
      this.visuals = new RenderWorldHelper();
      this.obstructions = new HashMap<>();
      this.ignored = Set.of();
      this.highObstruction = Set.of();
      this.lowReverb = Set.of();
      this.highReverb = Set.of();
      this.dimensionBase = Map.of();
      this.previousIgnore = "";
      this.previousHeavy = "";
      this.previousLow = "";
      this.previousHigh = "";
      this.previousDimension = "";
      this.liquidGain = 1.0F;
      this.liquidGainHF = 1.0F;
      this.reverbValues = SoundEffectsProcessor.ReverbValues.off();
      this.ambienceVolume.visibleWhen(this.ambience::get);

      for (NumberSetting numberSetting : List.of(this.waterGain, this.waterGainHF, this.lavaGain, this.lavaGainHF)) {
         numberSetting.visibleWhen(this.liquid::get);
      }

      for (NumberSetting numberSetting2 : List.of(
         this.reverbPercent,
         this.maxBlocks,
         this.minDecay,
         this.density,
         this.diffusion,
         this.gain,
         this.gainHF,
         this.decayHF,
         this.airAbsorptionHF,
         this.reflectionsGainBase,
         this.reflectionsGainMultiplier,
         this.reflectionsDelayMultiplier,
         this.lateGainBase,
         this.lateGainMultiplier,
         this.lateDelayMultiplier
      )) {
         numberSetting2.visibleWhen(this.reverb::get);
      }

      for (NumberSetting numberSetting3 : List.of(this.obstructionStep, this.obstructionMax)) {
         numberSetting3.visibleWhen(this.obstruction::get);
      }

      SkycoreClient.events().subscribe(WorldChangeEvent.class, worldChangeEvent -> {
         this.resetWorld();
         if (this.isEnabled()) {
            this.detachAudio();
         }
      });
   }

   private boolean isLow(BlockState blockState, Identifier texture) {
      if (this.lowReverb.contains(texture)) {
         return true;
      }

      return this.defaultMaterials.get() && (blockState.getBlock() instanceof FlowerPotBlock || AcousticMaterials.LOW_REVERB.contains(blockState.getSoundType()));
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   private void scanReverb(Minecraft minecraft) {
      int value = this.maxBlocks.get().intValue();
      BlockPos eyePosition = BlockPos.containing(minecraft.player.getEyePosition());
      Random random = new Random();
      int value2 = 0;
      if (this.checkSky.get()) {
         if (hasSky(minecraft, eyePosition)) {
            value2++;
         }

         for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            BlockPos blockPos = eyePosition.relative(direction, random.nextInt(5) + 5);
            if (hasSky(minecraft, blockPos)) {
               value2++;
            }

            if (hasSky(minecraft, blockPos.above(5))) {
               value2++;
            }
         }

         if (value2 >= 5) {
            this.prevDecay = this.prevRoom = this.prevSky = 0.0F;
            this.reverbValues = SoundEffectsProcessor.ReverbValues.off();
            return;
         }
      }

      HashSet entries = new HashSet();
      HashSet entries2 = new HashSet();
      ArrayList elements = new ArrayList();
      elements.add(eyePosition);
      entries2.add(eyePosition);
      double value3 = 0.0;
      double value4 = 0.0;
      double value5 = 0.0;

      for (int value6 = 0; value6 < value && !elements.isEmpty(); value6++) {
         BlockPos blockPos2 = (BlockPos)elements.remove(random.nextInt(elements.size()));
         entries2.remove(blockPos2);
         entries.add(blockPos2);

         for (Direction direction2 : Direction.values()) {
            BlockPos blockPos3 = blockPos2.relative(direction2);
            if (minecraft.level.hasChunkAt(blockPos3)) {
               BlockState blockState = minecraft.level.getBlockState(blockPos3);
               Identifier key = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
               if (!blockState.isSolidRender()) {
                  if (!entries.contains(blockPos3) && entries2.add(blockPos3)) {
                     elements.add(blockPos3);
                  }

                  if (blockState.isAir() || !blockState.getFluidState().isEmpty()) {
                     continue;
                  }
               }

               if (this.isLow(blockState, key)) {
                  value3++;
               } else if (this.isHigh(blockState, key)) {
                  value4++;
               } else {
                  value5++;
               }
            }
         }
      }

      float orDefault = this.dimensionBase.getOrDefault(minecraft.level.dimension().identifier(), 0.0F);
      if (value3 + value4 + value5 > 0.0) {
         orDefault += (float)((value4 - value3) / (value4 + value5 + value3));
      }

      orDefault = clamp(orDefault, 0.0F, 1.0F);
      float computedValue = (float)entries.size() / value;
      float computedValue2 = this.checkSky.get() ? 1.0F - value2 / 9.0F : 1.0F;
      computedValue2 *= computedValue2;
      orDefault = this.prevDecay = (orDefault + this.prevDecay) * 0.5F;
      computedValue = this.prevRoom = (computedValue + this.prevRoom) * 0.5F;
      computedValue2 = this.prevSky = (computedValue2 + this.prevSky) * 0.5F;
      float computedValue3 = clamp((float)(this.reverbPercent.get() * 6.0 * orDefault * computedValue * computedValue2), this.minDecay.get().floatValue(), 20.0F);
      float value7 = this.reverbPercent.get().floatValue();
      float computedValue4 = clamp(value7 * (this.reflectionsGainBase.get().floatValue() + this.reflectionsGainMultiplier.get().floatValue() * computedValue), 0.0F, 3.16F);
      float computedValue5 = clamp(this.reflectionsDelayMultiplier.get().floatValue() * computedValue, 0.0F, 0.3F);
      float computedValue6 = clamp(value7 * (this.lateGainBase.get().floatValue() + this.lateGainMultiplier.get().floatValue() * computedValue), 0.0F, 10.0F);
      float computedValue7 = clamp(this.lateDelayMultiplier.get().floatValue() * computedValue, 0.0F, 0.1F);
      this.reverbValues = new SoundEffectsProcessor.ReverbValues(
         true,
         this.density.get().floatValue(),
         this.diffusion.get().floatValue(),
         this.gain.get().floatValue(),
         this.gainHF.get().floatValue(),
         computedValue3,
         this.decayHF.get().floatValue(),
         computedValue4,
         computedValue5,
         computedValue6,
         computedValue7,
         this.airAbsorptionHF.get().floatValue()
      );
   }

   private float scanObstruction(Minecraft minecraft, SoundInstance soundInstance) {
      Vec3 position = new Vec3(soundInstance.getX(), soundInstance.getY(), soundInstance.getZ());
      Vec3 eyePosition = minecraft.player.getEyePosition();
      if (Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z)) {
         Vec3 position2 = eyePosition.subtract(position);
         double length = position2.length();
         if (!(length < 0.001) && !(length > 160.0)) {
            BlockPos blockPos = BlockPos.containing(position);
            BlockPos blockPos2 = BlockPos.containing(eyePosition);
            int x = blockPos.getX();
            int y = blockPos.getY();
            int z = blockPos.getZ();
            int x2 = (int)Math.signum(position2.x);
            int y2 = (int)Math.signum(position2.y);
            int z2 = (int)Math.signum(position2.z);
            double x3 = x2 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / position2.x);
            double y3 = y2 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / position2.y);
            double z3 = z2 == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / position2.z);
            double x4 = x2 == 0 ? Double.POSITIVE_INFINITY : ((x2 > 0 ? x + 1 : x) - position.x) / position2.x;
            double y4 = y2 == 0 ? Double.POSITIVE_INFINITY : ((y2 > 0 ? y + 1 : y) - position.y) / position2.y;
            double z4 = z2 == 0 ? Double.POSITIVE_INFINITY : ((z2 > 0 ? z + 1 : z) - position.z) / position2.z;
            double value = 0.0;

            for (int value2 = 0; value2 < 100 && (x != blockPos2.getX() || y != blockPos2.getY() || z != blockPos2.getZ()); value2++) {
               double value3;
               if (x4 <= y4 && x4 <= z4) {
                  value3 = x4;
                  x += x2;
                  x4 += x3;
               } else if (y4 <= z4) {
                  value3 = y4;
                  y += y2;
                  y4 += y3;
               } else {
                  value3 = z4;
                  z += z2;
                  z4 += z3;
               }

               if (value3 >= 1.0) {
                  break;
               }

               double computedValue = Math.min(1.0, Math.min(x4, Math.min(y4, z4)));
               BlockPos blockPos3 = new BlockPos(x, y, z);
               if (!minecraft.level.hasChunkAt(blockPos3)) {
                  break;
               }

               BlockState blockState = minecraft.level.getBlockState(blockPos3);
               if (blockState.isCollisionShapeFullBlock(minecraft.level, blockPos3)) {
                  Identifier key = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                  double value4 = this.highObstruction.contains(key) ? 2.0 : 1.0;
                  value += this.obstructionStep.get() * value4 * Math.max(0.0, computedValue - value3) * length;
                  if (value >= this.obstructionMax.get()) {
                     return this.obstructionMax.get().floatValue();
                  }
               }
            }

            return (float)value;
         } else {
            return 0.0F;
         }
      } else {
         return 0.0F;
      }
   }

   private NumberSetting number(String text, double value, double value2, double value3, double value4) {
      NumberSetting numberSetting = new NumberSetting(text, "", value, value2, value3, value4, "");
      return this.add(numberSetting);
   }

   private static boolean hasSky(Minecraft minecraft, BlockPos blockPos) {
      int value = 0;
      int value2 = 1;
      DimensionType dimensionType = minecraft.level.dimensionType();
      value2 = (byte)1;
      if (!dimensionType.hasCeiling() && minecraft.level.hasChunkAt(blockPos)) {
         ClientLevel level = minecraft.level;
         Types mOTION_BLOCKING = Types.MOTION_BLOCKING;
         value2 = blockPos.getX();
         int z = blockPos.getZ();
         value = level.getHeight(mOTION_BLOCKING, value2, z);
         if (value <= blockPos.getY()) {
            return true;
         }
      }

      return false;
   }

   @Override
   protected void onEnable() {
      this.resetWorld();
      this.refreshLists();
   }

   private void apply(SoundInstance soundInstance, ChannelHandle channelHandle) {
      Minecraft instance = Minecraft.getInstance();
      int identifier = this.isEnabled() && instance.level != null && instance.player != null && !this.ignored.contains(soundInstance.getIdentifier()) ? 0 : 1;
      float value = identifier == 0 && this.liquid.get() ? this.liquidGain : 1.0F;
      float value2 = identifier == 0 && this.liquid.get() ? this.liquidGainHF : 1.0F;
      float orDefault = identifier == 0 && this.obstruction.get() ? this.obstructions.getOrDefault(soundInstance, 0.0F) : 0.0F;
      value *= 1.0F - orDefault;
      value2 *= 1.0F - (float)Math.sqrt(orDefault);
      int value3 = identifier != 0
            || !this.reverb.get()
            || !(this.reverbPercent.get() > 0.0)
            || !this.reverbValues.enabled()
            || !(this.reverbValues.reflectionsDelay() > 0.0F) && !(this.reverbValues.lateDelay() > 0.0F)
         ? 0
         : 1;
      SoundEffectsProcessor.ReverbValues reverbValues2 = this.reverbValues;
      SoundEffectsProcessor.SourceValues sourceValues = new SoundEffectsProcessor.SourceValues(clamp(value, 0.0F, 1.0F), clamp(value2, 0.0F, 1.0F), (value3 != 0), (identifier != 0));
      channelHandle.execute(channel -> {
         int value4 = 0;
         value4 = (byte)0;
         ChannelGetSourceAccessor channelGetSourceAccessor = (ChannelGetSourceAccessor)channel;
         value4 = channelGetSourceAccessor.skycore$getSource();
         SoundEffectsProcessor.apply(value4, sourceValues, reverbValues2);
      });
   }

   private void refreshLists() {
      if (!this.ignoredEvents.get().equals(this.previousIgnore)) {
         this.previousIgnore = this.ignoredEvents.get();
         this.ignored = parseIds(this.previousIgnore);
      }

      if (!this.heavyBlocks.get().equals(this.previousHeavy)) {
         this.previousHeavy = this.heavyBlocks.get();
         this.highObstruction = parseIds(this.previousHeavy);
      }

      if (!this.lowBlocks.get().equals(this.previousLow)) {
         this.previousLow = this.lowBlocks.get();
         this.lowReverb = parseIds(this.previousLow);
      }

      if (!this.highBlocks.get().equals(this.previousHigh)) {
         this.previousHigh = this.highBlocks.get();
         this.highReverb = parseIds(this.previousHigh);
      }

      if (!this.dimensions.get().equals(this.previousDimension)) {
         this.previousDimension = this.dimensions.get();
         HashMap lookup = new HashMap();

         for (String text : this.previousDimension.split("[,\\r\\n]+")) {
            String[] stringValues = text.trim().split(";", 2);
            if (stringValues.length == 2) {
               try {
                  Identifier texture = Identifier.tryParse(stringValues[0].trim());
                  if (texture != null) {
                     lookup.put(texture, clamp(Float.parseFloat(stringValues[1].trim()), -1.0F, 1.0F));
                  }
               } catch (NumberFormatException failure) {
               }
            }
         }

         this.dimensionBase = Map.copyOf(lookup);
      }
   }

   @Override
   protected void onTick() {
      Minecraft instance = Minecraft.getInstance();
      this.refreshLists();
      if (instance.level != null && instance.player != null && instance.isRunning()) {
         if (!this.liquid.get()) {
            this.liquidGain = this.liquidGainHF = 1.0F;
         } else {
            BlockPos eyePosition = BlockPos.containing(instance.player.getEyePosition());
            FluidState fluidState = instance.level.getFluidState(eyePosition);
            float value = fluidState.is(FluidTags.WATER) ? this.waterGain.get().floatValue() : (fluidState.is(FluidTags.LAVA) ? this.lavaGain.get().floatValue() : 1.0F);
            float value2 = fluidState.is(FluidTags.WATER) ? this.waterGainHF.get().floatValue() : (fluidState.is(FluidTags.LAVA) ? this.lavaGainHF.get().floatValue() : 1.0F);
            this.liquidGain = clamp((this.liquidGain + value) * 0.5F, 0.0F, 1.0F);
            this.liquidGainHF = clamp((this.liquidGainHF + value2) * 0.5F, 0.0F, 1.0F);
         }

         if (!this.reverb.get()) {
            this.reverbValues = SoundEffectsProcessor.ReverbValues.off();
            this.scanTicks = 0;
         } else if (this.scanTicks++ == 20) {
            this.scanTicks = 0;
            this.scanReverb(instance);
         }

         if (!this.obstruction.get()) {
            this.obstructions.clear();
         }

         this.soundscape.tick(instance, this.ambience.get(), this.ambienceVolume.get().floatValue(), this.details.get(), this.footsteps.get(), this.equipped.get());
         this.visuals.tick(instance, this.fireflies.get(), this.rainSplashes.get());
      } else {
         this.resetWorld();
      }
   }

   private boolean isHigh(BlockState blockState, Identifier texture) {
      if (!this.highReverb.contains(texture)) {
         Boolean get = this.defaultMaterials.get();
         if (!get || !AcousticMaterials.HIGH_REVERB.contains(blockState.getSoundType())) {
            return false;
         }
      }

      return true;
   }

   public void onSoundStarted(SoundInstance soundInstance, ChannelHandle channelHandle) {
      if (this.isEnabled()) {
         CheckHeldItemHelper soundscape2 = this.soundscape;
         Boolean get = this.footsteps.get();
         boolean condition = get;
         soundscape2.onVanillaSound(soundInstance, condition);
         if (channelHandle != null) {
            this.apply(soundInstance, channelHandle);
         }
      }
   }

   private void resetWorld() {
      CheckHeldItemHelper soundscape2 = this.soundscape;
      Minecraft instance = Minecraft.getInstance();
      soundscape2.stopAll(instance);
      this.visuals.reset();
      this.scanTicks = 0;
      this.soundTicks = 0;
      this.obstructions.clear();
      this.liquidGain = this.liquidGainHF = 1.0F;
      this.prevSky = 0.0F;
      float value = this.prevRoom = 0.0F;
      this.prevDecay = value;
      this.reverbValues = SoundEffectsProcessor.ReverbValues.off();
   }

   private static Set<Identifier> parseIds(String text) {
      HashSet entries = new HashSet();

      for (String text2 : text.split("[,\\r\\n]+")) {
         Identifier texture = Identifier.tryParse(text2.trim());
         if (texture != null) {
            entries.add(texture);
         }
      }

      return Set.copyOf(entries);
   }

   private void detachAudio() {
      ChannelAccess channels2 = this.channels;
      if (channels2 != null) {
         channels2.executeOnChannels(stream -> {
            Consumer<com.mojang.blaze3d.audio.Channel> consumer = channel -> {
               SoundEffectsProcessor.detach(((ChannelGetSourceAccessor)channel).skycore$getSource());
            };
            stream.forEach(consumer);
            SoundEffectsProcessor.release();
         });
      }
   }

   public void renderWorld() {
      if (this.isEnabled()) {
         Boolean get = this.fireflies.get();
         if (get) {
            this.visuals.renderWorld();
         }
      }
   }

   public void onSoundTick(Map<SoundInstance, ChannelHandle> lookup, ChannelAccess channels2) {
      this.channels = channels2;
      if (this.isEnabled()) {
         this.obstructions.keySet().retainAll(lookup.keySet());
         Minecraft instance = Minecraft.getInstance();
         if (instance.level != null && instance.player != null) {
            for (Entry entry : lookup.entrySet()) {
               if (entry.getValue() != null && !((ChannelHandle)entry.getValue()).isStopped()) {
                  SoundInstance key = (SoundInstance)entry.getKey();
                  if (!this.ignored.contains(key.getIdentifier())
                     && this.obstruction.get()
                     && key.getAttenuation() == Attenuation.LINEAR
                     && !key.isRelative()) {
                     float orDefault = this.obstructions.getOrDefault(key, 0.0F);
                     float value = this.scanObstruction(instance, key);
                     this.obstructions.put(key, (orDefault * 3.0F + value) * 0.25F);
                  }

                  this.apply(key, (ChannelHandle)entry.getValue());
               }
            }

            if (++this.soundTicks % 20 == 0) {
               channels2.executeOnChannels(stream -> {
                  HashSet entries = new HashSet();
                  stream.forEach(channel -> {
                     int value2 = 1;
                     value2 = (byte)1;
                     value2 = ((ChannelGetSourceAccessor)channel).skycore$getSource();
                     entries.add(value2);
                  });
                  SoundEffectsProcessor.prune(entries);
               });
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.resetWorld();
      this.detachAudio();
   }
}
