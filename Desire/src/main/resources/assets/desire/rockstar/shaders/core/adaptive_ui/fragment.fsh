#version 150

#moj_import <rockstar:common.glsl>
#moj_import <rockstar:slug.glsl>


in vec2 FragCoord;
in vec2 TexCoord;
flat in int GlyphIndex;
in vec4 FragColor;
in vec2 AnchorUv;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform usampler2D Sampler2;
uniform sampler2D Sampler3;

uniform vec4 LightColor;
uniform vec4 DarkColor;

uniform vec4 Radius;
uniform float RectSmoothness;

uniform float Thickness;

uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    float alpha;

    if (GlyphIndex >= 0) {
        vec2 pixelsPerEm;
        alpha = slugWeight(
                slugCoverage(Sampler0, Sampler2, Sampler3, GlyphIndex, TexCoord, 1.0, pixelsPerEm), Thickness);
    } else {
        vec2 size = TexCoord;
        vec2 center = size * 0.5;
        float dist = roundedBoxSDF(center - (FragCoord * size), center - 1.0, Radius);
        alpha = 1.0 - smoothstep(1.0 - sdfAA(dist, RectSmoothness), 1.0, dist);
    }

    alpha *= FragColor.a;
    if (alpha <= 0.0) {
        discard;
    }

    float state = texture(Sampler1, clamp(AnchorUv, 0.0, 1.0)).r;
    vec4 tint = mix(LightColor, DarkColor, state);

    OutColor = vec4(tint.rgb, alpha * tint.a) * ColorModulator;
}
