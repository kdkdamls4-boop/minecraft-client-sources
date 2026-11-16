#version 330 core
in vec2 uv;
in vec4 tint;
out vec4 finalColor;
layout(std140) uniform ThunderUniforms { vec4 Thunder; };

float hash1(float n) { return fract(sin(n * 127.1 + 311.7) * 43758.5453); }
float noise1(float x) {
    float i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash1(i), hash1(i + 1.0), f);
}
void main() {
    // Each projected ribbon carries its own seven-bend bolt. The fragment
    // shader adds two finer layers of noisy electricity inside that envelope.
    float tick = floor(Thunder.x * 16.0);
    float fork = noise1(uv.x * 21.0 + tick * 0.37);
    float detail = noise1(uv.x * 75.0 - tick * 0.83);
    float center = 0.5 + (fork - 0.5) * 0.21 + (detail - 0.5) * 0.085;
    float distanceFromFilament = abs(uv.y - center);

    float core = 1.0 - smoothstep(0.008, 0.050, distanceFromFilament);
    float hot = 1.0 - smoothstep(0.015, 0.095, distanceFromFilament);
    float halo = exp(-distanceFromFilament * 8.8);
    float cloud = exp(-distanceFromFilament * 3.8) * 0.25;
    float flicker = 0.80 + 0.20 * hash1(tick + floor(uv.x * 8.0) * 11.0);
    float cap = smoothstep(0.0, 0.045, uv.x) * (1.0 - smoothstep(0.955, 1.0, uv.x));
    float strength = tint.a * cap * flicker;
    if (strength * (core + halo) < 0.004) discard;

    vec3 gold = tint.rgb;
    vec3 heat = mix(gold, vec3(1.0, 0.985, 0.88), 0.72);
    vec3 emission = gold * (halo * 1.8 + cloud)
        + heat * hot * 0.88
        + vec3(1.0, 1.0, 1.0) * core * 2.35;
    // BlendFunction.LIGHTNING performs additive blending of the emitted energy.
    finalColor = vec4(emission * strength, strength);
}
