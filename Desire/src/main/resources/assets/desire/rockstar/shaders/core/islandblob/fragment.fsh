#version 150

#moj_import <rockstar:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec2 RectCenter;
uniform vec2 RectHalf;
uniform vec4 RectRadius;
uniform vec2 CircleCenter;
uniform float CircleRadius;
uniform float Smooth;
uniform float Outline;
uniform vec4 FillColor;
uniform vec4 OutlineColor;
uniform float GlobalAlpha;

out vec4 OutColor;

float smin(float a, float b, float k) {
    if (k <= 0.0001) return min(a, b);
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

void main() {
    vec2 p = FragCoord * Size;

    float dRect = roundedBoxSDF(p - RectCenter, RectHalf, RectRadius);
    float dCircle = length(p - CircleCenter) - CircleRadius;
    float d = smin(dRect, dCircle, Smooth);

    float aa = max(fwidth(d), 1e-4);

    float fillMask  = 1.0 - smoothstep(0.0, aa, d);
    float outerMask = 1.0 - smoothstep(Outline, Outline + aa, d);
    float ring      = clamp(outerMask - fillMask, 0.0, 1.0);

    float fa = FillColor.a * fillMask;
    float oa = OutlineColor.a * ring;

    float a = fa + oa * (1.0 - fa);
    if (a <= 0.001) discard;

    vec3 rgb = (FillColor.rgb * fa + OutlineColor.rgb * oa * (1.0 - fa)) / max(a, 1e-4);

    a *= GlobalAlpha;
    a += FragColor.a * 1e-7;
    if (a <= 0.001) discard;

    OutColor = vec4(rgb, a);
}
