#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform float Time;
uniform float Strength;
uniform float Aspect;

out vec4 OutColor;

const float RISE = 0.22;
const int HEAT_SAMPLES = 6;

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 345.45));
    p += dot(p, p + 34.345);
    return fract(p.x * p.y);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float flameAt(vec2 uv) {
    vec4 c = texture(Sampler1, uv);
    return max(max(c.r, c.g), max(c.b, c.a));
}

float heatAt(vec2 uv) {
    float heat = 0.0;
    for (int s = 1; s < HEAT_SAMPLES; s++) {
        float k = float(s) / float(HEAT_SAMPLES - 1);
        vec2 sp = uv - vec2(0.0, k * RISE);
        heat = max(heat, flameAt(sp) * (1.0 - k));
    }
    return clamp(heat, 0.0, 1.0);
}

void main() {
    vec2 uv = TexCoord;

    float local = flameAt(uv);
    float mask = 1.0 - smoothstep(0.02, 0.20, local);
    float heat = heatAt(uv) * mask;
    if (heat < 0.003 || Strength <= 0.0) {
        OutColor = vec4(texture(Sampler0, uv).rgb, 1.0);
        return;
    }

    vec2 p = uv * vec2(Aspect, 1.0);
    float t = Time;
    float n1 = vnoise(p * 22.0 + vec2( t * 0.60, -t * 1.6));
    float n2 = vnoise(p * 41.0 + vec2(-t * 0.45, -t * 2.7));
    float n3 = vnoise(p * 11.0 + vec2( t * 0.20, -t * 0.9));

    float wobX = (n1 - 0.5) * 2.0 + (n2 - 0.5);
    float wobY = (n3 - 0.5) * 0.6;

    float amp = heat * Strength * 0.012;
    vec2 offset = vec2(wobX, wobY) * amp;

    vec2 duv = clamp(uv + offset, vec2(0.0), vec2(1.0));

    float chroma = amp * 0.25;
    float r = texture(Sampler0, clamp(duv + vec2(0.0, chroma), vec2(0.0), vec2(1.0))).r;
    float g = texture(Sampler0, duv).g;
    float b = texture(Sampler0, clamp(duv - vec2(0.0, chroma), vec2(0.0), vec2(1.0))).b;

    OutColor = vec4(r, g, b, 1.0);
}
