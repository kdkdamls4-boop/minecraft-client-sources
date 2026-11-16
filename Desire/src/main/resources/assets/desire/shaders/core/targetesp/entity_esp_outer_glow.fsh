#version 330

uniform sampler2D BloomSampler;
uniform sampler2D MaskSampler;

layout(std140) uniform EspOuterGlow {
    vec4 Params;
    vec4 GlowColor1;
    vec4 GlowColor2;
    vec4 OutlineColor;
    vec4 Screen;
};

in vec2 TexCoord;
out vec4 fragColor;

const int OUTLINE_DIRS = 16;
const float TAU = 6.2831853;

void main() {
    vec4 bloom = texture(BloomSampler, TexCoord);
    vec4 maskSample = texture(MaskSampler, TexCoord);
    float entityMask = smoothstep(0.02, 0.55, maskSample.a);
    float bloomMask = clamp(bloom.a, 0.0, 1.0);

    float outline = 0.0;
    if (OutlineColor.a > 0.001 && Screen.z > 0.0) {
        vec2 px = 1.0 / max(Screen.xy, vec2(1.0));
        float ring = 0.0;
        for (int i = 0; i < OUTLINE_DIRS; i++) {
            float angle = TAU * (float(i) + 0.5) / float(OUTLINE_DIRS);
            vec2 dir = vec2(cos(angle), sin(angle)) * px;
            ring = max(ring, texture(MaskSampler, TexCoord + dir * Screen.z).a);
            ring = max(ring, texture(MaskSampler, TexCoord + dir * Screen.z * 0.5).a);
        }
        outline = smoothstep(0.08, 0.6, ring) * (1.0 - entityMask) * OutlineColor.a;
    }

    float outer = pow(bloomMask, 0.8) * (1.0 - entityMask) * Params.x;
    float rim = smoothstep(0.0, 0.65, 1.0 - bloomMask);
    float inner = entityMask * Params.w * mix(0.3, 1.0, rim) * Params.x * 0.55;
    if (max(max(outer, inner), outline) < 0.002) discard;

    vec3 base;
    if (Params.y > 1.5) {
        base = bloomMask > 0.0005 ? bloom.rgb / bloomMask : GlowColor1.rgb;
    } else if (Params.y > 0.5) {
        vec3 sampled = bloomMask > 0.0005 ? bloom.rgb / bloomMask : GlowColor1.rgb;
        if (maskSample.a > 0.02) sampled = maskSample.rgb / maskSample.a;
        float maximum = max(sampled.r, max(sampled.g, sampled.b));
        if (maximum > 0.001) sampled /= maximum;
        float luminance = dot(sampled, vec3(0.299, 0.587, 0.114));
        base = clamp(mix(vec3(luminance), sampled, Params.z), 0.0, 1.0);
    } else {
        float core = pow(clamp(bloomMask, 0.0, 1.0), 0.55);
        base = mix(GlowColor2.rgb, GlowColor1.rgb, core);
    }

    vec3 glowColor = mix(base, vec3(1.0), smoothstep(1.1, 2.4, outer) * 0.35);
    vec3 outlineColor = Params.y > 0.5 ? base : OutlineColor.rgb;

    float glowAlpha = clamp(outer, 0.0, 1.0) * (1.0 - outline);
    vec3 rgb = glowColor * glowAlpha
        + base * clamp(inner, 0.0, 1.0)
        + outlineColor * clamp(outline * 1.25, 0.0, 1.0);
    fragColor = vec4(rgb, 1.0);
}
