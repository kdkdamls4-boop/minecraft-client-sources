# Skycore DynamicSurroundings — standalone port status

Original input: `dynamicsurroundings-fabric-1.21.1-0.4.5.jar` (OreCruncher; original mod metadata states MIT). The original binary has **not** been nested into Skycore and none of its 1.21.1 class files or Architectury runtime have been copied. This module is a standalone client adapter built on Skycore's 26.2 APIs.

## Ported and connected

- All 425 original OGG samples, the 100-event `assets/dsurround/sounds.json` registry including individual audio credit metadata, and the original `dsconfigs` / visual resource files are retained as first-party-packed Skycore resources under their original `dsurround` namespace.
- Reworked client-side audio: independently toggled biome/clock/weather/dimension/cave/underwater background sound layers with gentle cross-fades; spatially distributed bird, owl, woodpecker, whale, coyote and cave sounds.
- Step variants covering the original default mapping and its common material-specific branches; equipment-change accents. Both are additive to vanilla events and run via an asynchronous bounded queue rather than recursively calling SoundEngine.
- Water/lava low-pass, spatial obstruction, and material/dimension-based cave reverb via the existing 26.2-compatible OpenAL EFX bridge. The old Dynamic Sound class and package were removed and the new sound-engine/channel mixins are explicitly registered in `skycore.mixins.json`.
- Lightweight visual systems: limited fireflies at night in suitable overworld biomes and sparse rainfall splash particles; no extra GPU pipeline or external mod dependency.
- Clean stop on disable/world change, active-loop fade-out, capped spawned accents/particles, and retained resource attribution.

## Still not a one-to-one port of 0.4.5

The original JAR includes 469 classes, including its entire biome-condition scripting engine, more complex scanner-driven acoustics, per-entity/item behaviors, season integration, waterfalls, advanced weather/fog, compass/render overlays, and additional particles. These are **not** all implemented in the 26.2 adapter yet. Original JSON config assets are preserved, but the full original selector/expression interpreter is not enabled; the 26.2 runtime uses a safe native profile selector with original sound-event names instead.

## Credits and distribution

See `DynamicSurroundings-0.4.5-CREDITS.md` and the `ds_credits` fields in `assets/dsurround/sounds.json`. Individual samples have different licenses (including Attribution and some NonCommercial); the upstream mod's MIT declaration does **not** override those audio licenses. See the separate `DYNAMIC_SOUND_LICENSE.txt` for the retained Apache-2.0-derived DSP component.

**Validation:** static inspection only. Do not compile or launch Minecraft without the owner's explicit permission.
