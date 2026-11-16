#version 330 core

in vec2 scan_uv;
out vec4 finalColor;

uniform sampler2D MaskSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform ScanUniforms {
    mat4 InvViewProjection;
    vec4 CenterBand;
    vec4 Motion;
    vec4 Style;
    vec4 ScanColor;
};

const float WAVE_AMP = 0.03;
const float TRAIL_STRENGTH = 0.5;
const float GLOW_LEN = 0.12;
const float GLOW_STRENGTH = 0.6;
const float AMBIENT_LEN = 0.55;
const float AMBIENT_STRENGTH = 0.12;
const float HOT_MIX = 0.7;

void main() {
    float mask = texture(MaskSampler, scan_uv).a;
    if (mask < 0.01) {
        discard;
    }

    float rawDepth = texture(DepthSampler, scan_uv).r;
    float ndcDepth = Style.z > 0.5
        ? rawDepth
        : rawDepth * 2.0 - 1.0;

    vec4 clip = vec4(scan_uv * 2.0 - 1.0, ndcDepth, 1.0);
    vec4 relative = InvViewProjection * clip;
    vec3 pos = relative.xyz / relative.w;

    float bandHalf = 0.02 * Style.x;
    float trailBase = 0.5 * Style.y;

    vec2 radial = pos.xz - CenterBand.xz;
    float theta = atan(radial.y, radial.x);
    float wave = WAVE_AMP * (
        sin(theta * 3.0 + Motion.z * 2.6)
        + 0.45 * sin(theta * 5.0 - Motion.z * 3.4)
    );

    float d = pos.y - (CenterBand.w + wave);
    float dist = abs(d);

    float core = 1.0 - smoothstep(
        bandHalf * 0.7,
        bandHalf,
        dist
    );
    float hot = 1.0 - smoothstep(
        bandHalf * 0.25,
        bandHalf * 0.45,
        dist
    );
    float glow = exp(-dist / GLOW_LEN) * GLOW_STRENGTH;
    float ambient =
        exp(-dist / AMBIENT_LEN) * AMBIENT_STRENGTH;

    float trail = 0.0;
    float behind = -d * Motion.x - bandHalf;
    if (behind > 0.0) {
        float trailLen = trailBase
            * (0.25 + 0.75 * Motion.y);
        trail = (
            1.0 - clamp(
                behind / trailLen,
                0.0,
                1.0
            )
        ) * TRAIL_STRENGTH;
    }

    vec3 hotColor = mix(
        ScanColor.rgb,
        vec3(1.0),
        HOT_MIX
    );
    float intensity = clamp(
        core + glow + ambient + trail + hot,
        0.0,
        1.0
    );

    vec3 result =
        ScanColor.rgb * (core + glow + ambient + trail)
        + hotColor * hot;

    finalColor = vec4(
        result,
        ScanColor.a * Motion.w * mask * intensity
    );
}
