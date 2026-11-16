#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D PrevSampler;
uniform sampler2D InjectSampler;

layout(std140) uniform HandTrailUniforms {
    vec2 TexelSize;
    float Decay;
    float Rise;
    float Time;
    float EmissionFloor;
    float Turbulence;
    float Chaos;
    float Delta;
};

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int index = 0; index < 4; index++) {
        value += amplitude * vnoise(p);
        p = p * 2.0 + vec2(1.7, 9.2);
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    float delta = clamp(Delta, 0.0, 0.1);
    float turbulenceFreq = mix(1.0, 2.2, clamp(Turbulence / 3.0, 0.0, 1.0));
    vec2 flowUv = uv * vec2(6.0, 4.0) * turbulenceFreq;

    vec2 flow = vec2(
        fbm(flowUv + vec2(0.0, -Time * 1.4)),
        fbm(flowUv + vec2(37.1, -Time * 1.1))
    ) - 0.5;
    vec2 swayVelocity = flow * 0.55 * Turbulence;
    vec2 sway = swayVelocity * delta;

    vec2 sourceUv = uv + sway + vec2(0.0, -Rise * delta);

    float centroidNum = 0.0001 * sourceUv.x;
    float centroidDen = 0.0001;
    for (int index = -1; index <= 1; index++) {
        float offset = float(index) * TexelSize.x * 3.0;
        float weight = texture(PrevSampler, sourceUv + vec2(offset, 0.0)).a;
        centroidNum += (sourceUv.x + offset) * weight;
        centroidDen += weight;
    }
    float taperPull = clamp(Rise * delta * 2.5, 0.0, 0.4);
    sourceUv.x = mix(sourceUv.x, centroidNum / centroidDen, taperPull);

    float previous = texture(PrevSampler, sourceUv).a;

    float lifetime = mix(0.15, 1.2, clamp(Decay, 0.0, 1.0));
    float decayFactor = exp(-delta / max(lifetime, 0.02));

    float coolNoise = fbm(flowUv * 1.7 + vec2(11.0, Time * 1.8));
    float cooling = (0.35 + coolNoise * 0.9) * delta * (2.6 / max(lifetime, 0.02));
    float advectedAlpha = max(previous * decayFactor - cooling, 0.0);

    float coverage = texture(InjectSampler, uv).a;
    float ring = smoothstep(0.16, 0.5, coverage) * (1.0 - smoothstep(0.72, 0.94, coverage));
    float chaosSpread = max(Chaos, 0.001);
    float tongues = smoothstep(
        0.6 - 0.22 * chaosSpread,
        0.6 + 0.24 * chaosSpread,
        fbm(flowUv * 1.6 + vec2(5.0, -Time * 2.2 * mix(0.6, 1.6, clamp(Chaos, 0.0, 1.0))))
    );
    float emission = clamp(
        ring * (EmissionFloor + (1.0 - EmissionFloor) * tongues) * 2.0,
        0.0,
        1.0
    );

    float value = max(emission, advectedAlpha);
    finalColor = vec4(1.0, 1.0, 1.0, clamp(value, 0.0, 1.0));
}
