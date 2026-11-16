package net.skycore.client.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.skycore.client.SkycoreClient;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;

public final class SoundEffectsProcessor {
   private static boolean failureReported;
   private static int slot;
   private static SoundEffectsProcessor.ReverbValues appliedReverb;
   private static int effect;
   private static long context;
   private static final Map<Integer, Integer> LOWPASS = new HashMap<>();
   private static boolean unsupported;
   private static final Set<Integer> ATTACHED = new HashSet<>();

   public static void apply(int value, SoundEffectsProcessor.SourceValues sourceValues, SoundEffectsProcessor.ReverbValues reverbValues) {
      if (value > 0 && ready()) {
         if (!sourceValues.bypass() && (!(sourceValues.gain() >= 0.9999F) || !(sourceValues.gainHF() >= 0.9999F) || sourceValues.reverb())) {
            if (!applyOnce(value, sourceValues, reverbValues)) {
               release();
               if (!applyOnce(value, sourceValues, reverbValues)) {
                  if (!failureReported) {
                     failureReported = true;
                     SkycoreClient.LOGGER.error("Dynamic Surroundings: OpenAL EFX failed twice; disabling filtering until sound device restart");
                  }

                  unsupported = true;
               }
            }
         } else {
            detach(value);
         }
      }
   }

   public static void release() {
      if (ALC10.alcGetCurrentContext() == context && context != 0L) {
         for (int index : ATTACHED) {
            AL10.alSourcei(index, 131077, 0);
            AL11.alSource3i(index, 131078, 0, 0, 0);
         }

         ATTACHED.clear();
         AL10.alGetError();

         for (int index2 : LOWPASS.values()) {
            if (index2 > 0) {
               EXTEfx.alDeleteFilters(index2);
            }
         }

         LOWPASS.clear();
         if (slot > 0) {
            EXTEfx.alAuxiliaryEffectSloti(slot, 1, 0);
            EXTEfx.alDeleteAuxiliaryEffectSlots(slot);
         }

         if (effect > 0) {
            EXTEfx.alDeleteEffects(effect);
         }

         slot = 0;
         effect = 0;
         appliedReverb = null;
         AL10.alGetError();
      } else {
         LOWPASS.clear();
         ATTACHED.clear();
         slot = 0;
         effect = 0;
         appliedReverb = null;
      }
   }

   public static void prune(Set<Integer> integers) {
      if (ready()) {
         ATTACHED.retainAll(integers);

         for (int index : new HashSet<>(LOWPASS.keySet())) {
            if (!integers.contains(index)) {
               ATTACHED.remove(index);
               Integer value = LOWPASS.remove(index);
               if (value != null && value > 0) {
                  EXTEfx.alDeleteFilters(value);
               }
            }
         }
      }
   }

   private static boolean applyOnce(int slot2, SoundEffectsProcessor.SourceValues sourceValues, SoundEffectsProcessor.ReverbValues reverbValues) {
      int value = 0;
      int value2 = !(sourceValues.gain() < 0.9999F) && !(sourceValues.gainHF() < 0.9999F) ? 0 : 1;
      if (value2 != 0) {
         value = LOWPASS.computeIfAbsent(slot2, value3 -> {
            int result = EXTEfx.alGenFilters();
            if (result > 0) {
               EXTEfx.alFilteri(result, 32769, 1);
            }

            return result;
         });
         if (value <= 0) {
            return false;
         }

         EXTEfx.alFilterf(value, 1, clamp(sourceValues.gain(), 0.0F, 1.0F));
         EXTEfx.alFilterf(value, 2, clamp(sourceValues.gainHF(), 0.0F, 1.0F));
      }

      if (sourceValues.reverb() && !configureReverb(reverbValues)) {
         return false;
      }

      AL10.alSourcei(slot2, 131077, value);
      AL11.alSource3i(slot2, 131078, sourceValues.reverb() ? slot : 0, 0, value);
      int value4 = AL10.alGetError() == 0 ? 1 : 0;
      if (value4 != 0) {
         ATTACHED.add(slot2);
      }

      return (value4 != 0);
   }

   private static boolean configureReverb(SoundEffectsProcessor.ReverbValues reverbValues) {
      if (!reverbValues.enabled()) {
         return false;
      }

      if (effect == 0 || slot == 0) {
         effect = EXTEfx.alGenEffects();
         slot = EXTEfx.alGenAuxiliaryEffectSlots();
         if (effect <= 0 || slot <= 0) {
            return false;
         }

         appliedReverb = null;
      }

      if (reverbValues.equals(appliedReverb)) {
         return true;
      }

      EXTEfx.alAuxiliaryEffectSlotf(slot, 2, 0.0F);
      EXTEfx.alEffecti(effect, 32769, 1);
      EXTEfx.alEffectf(effect, 1, clamp(reverbValues.density(), 0.0F, 1.0F));
      EXTEfx.alEffectf(effect, 2, clamp(reverbValues.diffusion(), 0.0F, 1.0F));
      EXTEfx.alEffectf(effect, 3, clamp(reverbValues.gain(), 0.0F, 1.0F));
      EXTEfx.alEffectf(effect, 4, clamp(reverbValues.gainHF(), 0.0F, 1.0F));
      EXTEfx.alEffectf(effect, 5, clamp(reverbValues.decayTime(), 0.1F, 20.0F));
      EXTEfx.alEffectf(effect, 6, clamp(reverbValues.decayHFRatio(), 0.1F, 2.0F));
      EXTEfx.alEffectf(effect, 7, clamp(reverbValues.reflectionsGain(), 0.0F, 3.16F));
      EXTEfx.alEffectf(effect, 8, clamp(reverbValues.reflectionsDelay(), 0.0F, 0.3F));
      EXTEfx.alEffectf(effect, 9, clamp(reverbValues.lateGain(), 0.0F, 10.0F));
      EXTEfx.alEffectf(effect, 10, clamp(reverbValues.lateDelay(), 0.0F, 0.1F));
      EXTEfx.alEffectf(effect, 11, clamp(reverbValues.airAbsorptionHF(), 0.892F, 1.0F));
      EXTEfx.alEffectf(effect, 12, 0.0F);
      EXTEfx.alEffecti(effect, 13, 1);
      EXTEfx.alAuxiliaryEffectSloti(slot, 1, effect);
      EXTEfx.alAuxiliaryEffectSlotf(slot, 2, 1.0F);
      if (AL10.alGetError() != 0) {
         return false;
      }

      appliedReverb = reverbValues;
      return true;
   }

   public static void detach(int value) {
      if (value > 0 && ready()) {
         AL10.alSourcei(value, 131077, 0);
         AL11.alSource3i(value, 131078, 0, 0, 0);
         AL10.alGetError();
         ATTACHED.remove(value);
      }
   }

   private static float clamp(float value, float value2, float value3) {
      float minimum = Math.min(value3, value);
      return Math.max(value2, minimum);
   }

   private SoundEffectsProcessor() {
   }

   private static boolean ready() {
      long timestamp = ALC10.alcGetCurrentContext();
      if (timestamp == 0L) {
         return false;
      }

      if (timestamp != context) {
         LOWPASS.clear();
         ATTACHED.clear();
         slot = 0;
         effect = 0;
         appliedReverb = null;
         context = timestamp;
         unsupported = false;
         failureReported = false;
         long timestamp2 = ALC10.alcGetContextsDevice(timestamp);
         if (timestamp2 == 0L || !ALC10.alcIsExtensionPresent(timestamp2, "ALC_EXT_EFX")) {
            unsupported = true;
            SkycoreClient.LOGGER.warn("Dynamic Surroundings: audio device does not support OpenAL EFX; keeping vanilla sound");
         }
      }

      return !unsupported;
   }

   public record ReverbValues(
      boolean enabled,
      float density,
      float diffusion,
      float gain,
      float gainHF,
      float decayTime,
      float decayHFRatio,
      float reflectionsGain,
      float reflectionsDelay,
      float lateGain,
      float lateDelay,
      float airAbsorptionHF
   ) {
      public boolean enabled() {
         return this.enabled;
      }

      public float lateGain() {
         return this.lateGain;
      }

      public float gainHF() {
         return this.gainHF;
      }

      public float lateDelay() {
         return this.lateDelay;
      }

      public float density() {
         return this.density;
      }

      public float reflectionsDelay() {
         return this.reflectionsDelay;
      }

      public float airAbsorptionHF() {
         return this.airAbsorptionHF;
      }

      public float reflectionsGain() {
         return this.reflectionsGain;
      }

      public float diffusion() {
         return this.diffusion;
      }

      public float decayTime() {
         return this.decayTime;
      }

      public static SoundEffectsProcessor.ReverbValues off() {
         return new SoundEffectsProcessor.ReverbValues(false, 0.2F, 0.6F, 0.15F, 0.8F, 0.1F, 0.7F, 0.0F, 0.0F, 0.0F, 0.0F, 0.99F);
      }

      public float decayHFRatio() {
         return this.decayHFRatio;
      }

      public float gain() {
         return this.gain;
      }
   }

   public record SourceValues(float gain, float gainHF, boolean reverb, boolean bypass) {
      public boolean reverb() {
         return this.reverb;
      }

      public boolean bypass() {
         return this.bypass;
      }

      public float gainHF() {
         return this.gainHF;
      }

      public float gain() {
         return this.gain;
      }
   }
}
