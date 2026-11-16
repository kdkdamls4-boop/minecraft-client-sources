#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform mat4 InvViewProj;
uniform vec3 CircleCenter;
uniform float WorldRadius;
uniform float Thickness;
uniform vec4 RingColor;
uniform float Strength;

out vec4 OutColor;

const float AMP = 0.026;
const float RIPPLES = 3.0;
const float BLUR = 0.003;
const float OCCLUDE_BIAS = 0.05;
const float PI = 3.14159265;

void main() {
    vec2 uv = TexCoord;
    vec3 base = texture(Sampler0, uv).rgb;

    vec2 ndc = uv * 2.0 - 1.0;

    vec4 farClip = vec4(ndc, 1.0, 1.0);
    vec4 farWorld = InvViewProj * farClip;
    farWorld.xyz /= farWorld.w;
    vec3 rayDir = normalize(farWorld.xyz);

    float b = dot(rayDir, CircleCenter);
    if (b <= 0.0) { OutColor = vec4(base, 1.0); return; }

    float cc = dot(CircleCenter, CircleCenter);
    float R = WorldRadius;
    float dPerp = sqrt(max(0.0, cc - b * b));
    float t = dPerp / R;
    if (R <= 1e-4 || t >= 1.02) { OutColor = vec4(base, 1.0); return; }

    float inside = max(0.0, R * R - dPerp * dPerp);
    float tHit = b - sqrt(inside);
    if (tHit <= 0.0) tHit = b;
    vec3 hit = rayDir * tHit;

    if (hit.y < CircleCenter.y) { OutColor = vec4(base, 1.0); return; }

    float depth = texture(Sampler1, uv).r;
    if (depth < 1.0) {
        vec4 clip = vec4(ndc, depth * 2.0 - 1.0, 1.0);
        vec4 sceneW = InvViewProj * clip;
        sceneW.xyz /= sceneW.w;
        float sceneDist = length(sceneW.xyz);
        if (tHit > sceneDist + max(OCCLUDE_BIAS, sceneDist * 0.005)) { OutColor = vec4(base, 1.0); return; }
    }

    vec2 grad = vec2(dFdx(dPerp), dFdy(dPerp));
    float glen = length(grad);
    if (glen < 1e-6) { OutColor = vec4(base, 1.0); return; }
    vec2 dir = grad / glen;

    float edgeFade = smoothstep(1.02, 0.9, t);
    float ripple = sin(t * PI * RIPPLES);
    float amp = AMP * (0.7 + 0.3 * t) * edgeFade * Strength;
    vec2 off = dir * (ripple * amp);

    vec2 suv = clamp(uv + off, vec2(0.0), vec2(1.0));
    vec3 col = texture(Sampler0, suv).rgb;

    float step = edgeFade * BLUR * Strength;
    if (step > 1e-5) {
        col += texture(Sampler0, clamp(suv + dir * step,       vec2(0.0), vec2(1.0))).rgb;
        col += texture(Sampler0, clamp(suv - dir * step,       vec2(0.0), vec2(1.0))).rgb;
        col += texture(Sampler0, clamp(suv + dir * step * 2.0, vec2(0.0), vec2(1.0))).rgb;
        col += texture(Sampler0, clamp(suv - dir * step * 2.0, vec2(0.0), vec2(1.0))).rgb;
        col /= 5.0;
    }

    OutColor = vec4(col, 1.0);
}
