#version 150

#moj_import <rockstar:common.glsl>

in vec2 FragCoord;
in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform float CornerSmoothness;
uniform float GlobalAlpha;

uniform float FresnelPower;
uniform vec3 FresnelColor;
uniform float FresnelAlpha;
uniform float BaseAlpha;
uniform bool FresnelInvert;
uniform float FresnelMix;
uniform float DistortStrength;
uniform float DistortRadius;
uniform float Aberration;
uniform float Saturation;

out vec4 OutColor;

float roundedBoxSDF(vec2 p, vec2 b, vec4 r, float smoothness) {
    r = min(r, vec4(min(b.x, b.y) * smoothness * 0.5));
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 q_clamped = max(q, 0.0);
    float len = pow(pow(q_clamped.x, smoothness) + pow(q_clamped.y, smoothness), 1.0 / smoothness);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

float glassCoverage(vec2 p, vec2 b, vec4 r, float smoothness, float band) {
    return smoothstep(-band, band, -roundedBoxSDF(-p, b, r, smoothness));
}

void main() {
    vec2 center = Size * 0.5;
    vec2 halfSize = center - 1.0;
    vec2 pos = (FragCoord * Size) - center;

    float sdCover = roundedBoxSDF(-pos, halfSize, Radius, CornerSmoothness);
    float alpha = clamp(0.5 - sdCover / max(fwidth(sdCover), 1e-4), 0.0, 1.0);
    if (alpha < 0.001) discard;

    float minSide = min(halfSize.x, halfSize.y);
    float thickness = clamp(DistortRadius, 2.0, minSide);

    float bigStep = thickness * 0.6;
    float c  = glassCoverage(pos,                       halfSize, Radius, CornerSmoothness, thickness);
    float cl = glassCoverage(pos + vec2(-bigStep, 0.0), halfSize, Radius, CornerSmoothness, thickness);
    float cr = glassCoverage(pos + vec2( bigStep, 0.0), halfSize, Radius, CornerSmoothness, thickness);
    float cd = glassCoverage(pos + vec2(0.0, -bigStep), halfSize, Radius, CornerSmoothness, thickness);
    float cu = glassCoverage(pos + vec2(0.0,  bigStep), halfSize, Radius, CornerSmoothness, thickness);

    vec2 gradient = vec2(cr - cl, cu - cd);
    vec2 normal = length(gradient) > 1e-4 ? normalize(gradient) : vec2(0.0);

    float distToEdge = 1.0 - c;

    float responseCurve = smoothstep(0.0, 1.0, distToEdge * 1.2);
    float edgePeak = smoothstep(0.6, 1.0, distToEdge);
    float strength = mix(responseCurve * 0.6, 1.0, edgePeak);

    float refractStrength = strength * DistortStrength * 2.5;

    vec2 fragToTex = fwidth(TexCoord) / max(fwidth(FragCoord), vec2(1e-6));
    vec2 refractDir = normal * refractStrength * fragToTex;

    float ab = Aberration * 0.4;
    vec3 col;
    col.r = texture(Sampler0, clamp(TexCoord + refractDir * (1.0 + ab), 0.0, 1.0)).r;
    col.g = texture(Sampler0, clamp(TexCoord + refractDir,              0.0, 1.0)).g;
    col.b = texture(Sampler0, clamp(TexCoord + refractDir * (1.0 - ab), 0.0, 1.0)).b;

    float luma = dot(col, vec3(0.2126, 0.7152, 0.0722));
    col = mix(vec3(luma), col, Saturation * 1.15);

    col *= 0.45;

    float fresnel = smoothstep(0.0, 1.0, strength) * 0.1;
    col += vec3(fresnel);

    float a = alpha * BaseAlpha * GlobalAlpha;
    a += (Smoothness + FresnelPower + FresnelAlpha + FresnelMix
          + FresnelColor.r + FresnelColor.g + FresnelColor.b
          + (FresnelInvert ? 1.0 : 0.0)) * 1e-7;
    a = clamp(a, 0.0, 1.0);
    if (a < 0.001) discard;

    OutColor = vec4(col, a);
}
