#version 150

#moj_import <rockstar:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform vec2 Smoothness;
uniform float Thickness;
uniform float CornerSmoothness;
uniform vec4 ColorModulator;

out vec4 OutColor;

float squircleSDF(vec2 p, vec2 b, vec4 r, float s) {
    r = min(r, vec4(min(b.x, b.y) * s * 0.5));
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    vec2 qc = max(q, 0.0);
    float len = pow(pow(qc.x, s) + pow(qc.y, s), 1.0 / s);
    return min(max(q.x, q.y), 0.0) + len - r.x;
}

void main() {
    vec2 center = Size * 0.5;
    float distance = squircleSDF(center - (FragCoord * Size), center - 1.0, Radius, CornerSmoothness);

    float inner = sdfAA(distance, Smoothness.x);
    float outer = sdfAA(distance, Smoothness.y);

    float alpha = smoothstep(1.0 - Thickness - inner - outer,
        1.0 - Thickness - outer, distance);
    alpha *= 1.0 - smoothstep(1.0 - outer, 1.0, distance);

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) {
        discard;
    }

    OutColor = finalColor * ColorModulator;
}
