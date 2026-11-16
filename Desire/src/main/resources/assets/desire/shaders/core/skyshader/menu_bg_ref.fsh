#version 150

layout(std140) uniform SkyData {
    vec4 uColorA;
    vec4 uColorB;
    vec4 uResData;
    vec4 uParams;
    vec4 uCamData;
};

out vec4 fragColor;

#define uTime       (uResData.z)
#define uResolution (uResData.xy)
#define uAccent     (uColorA.rgb)
#define uAlpha      (uColorA.a)
#define uProgress   (uParams.x)

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float noise21(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 6; i++) {
        value += noise21(p) * amplitude;
        p = p * 2.03 + vec2(17.3, 9.2);
        amplitude *= 0.52;
    }
    return value;
}

void main() {
    vec2 uv = gl_FragCoord.xy / uResolution.xy;
    vec2 aspect = vec2(uResolution.x / max(uResolution.y, 1.0), 1.0);
    float time = uTime * 0.28;

    vec2 p = (uv - 0.5) * vec2(aspect.x, 1.0);

    vec2 warpA = vec2(
        fbm(p * 2.5 + vec2(time * 0.90, -time * 0.20)),
        fbm(p * 2.1 + vec2(-time * 0.40,  time * 0.70))
    );
    vec2 warpB = vec2(
        fbm((p + warpA * 0.18) * 4.2 + vec2( time * 1.10,  time * 0.30)),
        fbm((p - warpA * 0.14) * 3.6 + vec2(-time * 0.80,  time * 0.60))
    );
    vec2 flowUv = p + (warpA - 0.5) * 0.22 + (warpB - 0.5) * 0.10;

    float flow   = fbm(flowUv * 3.4 + vec2( time * 1.30, -time * 0.50));
    float swirl  = fbm(flowUv * 5.5 + vec2(-time * 0.75,  time * 1.10));
    float shimmer = smoothstep(0.50, 0.97, flow * 0.70 + swirl * 0.46);

    vec3 accentDark = uAccent * 0.08;

    vec3 baseTop    = vec3(0.008, 0.010, 0.016) + accentDark * 0.30;
    vec3 baseBottom = vec3(0.004, 0.005, 0.010) + accentDark * 0.10;
    vec3 color = mix(baseTop, baseBottom, smoothstep(0.0, 1.0, uv.y));
    color += accentDark * 0.40;
    color += uAccent * flow * 0.018;

    float breathe = 0.68 + 0.32 * sin(time * 2.0);

    vec2 glowCenter = vec2(0.54 + sin(time * 0.72) * 0.012, 0.36 + cos(time * 0.50) * 0.009);
    vec2 glowUv = (uv - glowCenter) * vec2(aspect.x * 1.00, 1.55);
    float glow = exp(-dot(glowUv, glowUv) * 3.4);

    vec2 haloUv = (uv - vec2(0.52, 0.38)) * vec2(aspect.x * 1.40, 2.30);
    float halo = exp(-dot(haloUv, haloUv) * 1.9);

    vec2 softUv = (uv - vec2(0.48, 0.55)) * vec2(aspect.x * 2.20, 3.00);
    float softGlow = exp(-dot(softUv, softUv) * 2.6);

    float sheen = 0.5 + 0.5 * sin((uv.x * 7.0 - uv.y * 5.0) + time * 1.7 + flow * 3.8);
    sheen *= smoothstep(0.20, 0.96, flow);

    color += uAccent * glow  * (0.24 + shimmer * 0.14) * breathe * max(0.35, uProgress);
    color += uAccent * sheen * halo * 0.050;
    color += uAccent * 0.28 * halo  * 0.22;
    color += uAccent * 0.18 * softGlow * 0.18;
    color += uAccent * shimmer * glow * 0.10;

    float vignette = smoothstep(1.10, 0.16, length((uv - 0.5) * vec2(aspect.x, 1.0)));
    color *= 0.70 + vignette * 0.30;

    float grain = fract(sin(dot(floor(gl_FragCoord.xy), vec2(12.9898, 78.233))) * 43758.5453);
    color += (grain - 0.5) * 0.009;

    fragColor = vec4(clamp(color, 0.0, 1.0), uAlpha);
}
