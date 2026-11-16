#version 150

uniform float time;
uniform float speed;
uniform float intensity;
uniform float waveScale;
uniform float starStrength;
uniform float saturation;
uniform float brightness;
uniform vec4 clientColor;
uniform vec4 accentColor;
uniform vec4 deepColor;

in vec3 skyDirection;
out vec4 fragColor;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 5; i++) {
        value += noise(p) * amp;
        p = p * 2.02 + vec2(17.7, 9.2);
        amp *= 0.5;
    }
    return value;
}

vec3 applyTone(vec3 color) {
    float luma = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(luma), color, saturation);
    return color * brightness;
}

void main() {
    vec3 dir = normalize(skyDirection);
    float t = time * max(speed, 0.01);
    float height = clamp(dir.y * 0.5 + 0.5, 0.0, 1.0);
    float horizon = smoothstep(0.02, 0.42, height);

    vec3 base = mix(deepColor.rgb * 0.18, clientColor.rgb * 0.32, height);
    base = mix(base, accentColor.rgb * 0.34, smoothstep(0.38, 1.0, height));

    vec2 skyUv = vec2(
        atan(dir.z, dir.x) * 0.318309886,
        asin(clamp(dir.y, -1.0, 1.0)) * 0.636619772
    );
    vec2 field = vec2(skyUv.x * 1.85, skyUv.y * 1.18);
    field += vec2(dir.x * 0.34 + dir.z * 0.16, dir.z * 0.24);
    field *= mix(1.25, 0.72, height) * max(waveScale, 0.01);
    vec2 nebulaUv = field * 2.2 + vec2(t * 0.07, -t * 0.045);
    float cloud = fbm(nebulaUv);
    float cloud2 = fbm(nebulaUv * 1.85 - vec2(t * 0.032, t * 0.025));
    vec3 nebula = mix(clientColor.rgb, accentColor.rgb, cloud);
    nebula = mix(nebula, deepColor.rgb, cloud2 * 0.45);

    vec3 waves = vec3(0.0);
    float glow = 0.0;
    vec2 wavePos = field;
    for (int i = 0; i < 10; i++) {
        float fi = float(i);
        float lane = wavePos.y;
        lane += sin(wavePos.x * (fi + 1.0) * 2.15 + t * 0.95 + fi * 0.58) * 0.105;
        lane += sin(wavePos.x * 1.35 - t * 0.42 + fi) * 0.028;

        float beam = 0.0085 / max(abs(lane), 0.010);
        beam *= (1.0 - fi / 12.0) * (0.38 + height * 1.2);
        beam = min(beam, 1.4);

        vec3 laneColor = mix(clientColor.rgb, accentColor.rgb, fi / 9.0);
        laneColor = mix(laneColor, deepColor.rgb, 0.25 + 0.25 * sin(t + fi));
        waves += laneColor * beam;
        glow += beam;

        wavePos.y += 0.075;
        wavePos.x += sin(fi + t * 0.12) * 0.018;
    }

    float starNoise = hash(floor((dir.xz / max(dir.y + 1.18, 0.16)) * 230.0));
    float stars = step(0.987, starNoise) * smoothstep(0.35, 0.86, height) * starStrength;
    float starTwinkle = 0.65 + 0.35 * sin(t * 3.0 + starNoise * 24.0);

    vec3 color = base;
    color += nebula * (0.42 + cloud * 0.72) * intensity * horizon;
    color += waves * intensity * 0.62;
    color += accentColor.rgb * pow(clamp(glow, 0.0, 1.0), 1.6) * 0.28;
    color += vec3(1.0, 0.94, 0.82) * stars * starTwinkle;
    color *= 0.58 + height * 0.72;
    color = mix(deepColor.rgb * 0.1, color, smoothstep(0.0, 0.18, height));
    color = applyTone(color);

    fragColor = vec4(color, 1.0);
}
