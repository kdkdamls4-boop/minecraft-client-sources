#version 150

#moj_import <rockstar:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float FadeStart;
uniform float FadeEnd;
uniform vec2 ClampMin;
uniform vec2 ClampMax;
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    float dist = roundedBoxSDF(center - (FragCoord * Size), center - 1.0, Radius);
    float edge = 1.0 - smoothstep(1.0 - sdfAA(dist, Smoothness), 1.0, dist);

    float vfade = 1.0 - smoothstep(FadeStart, FadeEnd, FragCoord.y);

    float alpha = edge * vfade;
    if (alpha <= 0.001) discard;

    vec2 uv = clamp(TexCoord, ClampMin, ClampMax);

    OutColor = vec4(texture(Sampler0, uv).rgb, alpha) * FragColor * ColorModulator;
}
