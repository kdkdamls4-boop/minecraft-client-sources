#version 150

#moj_import <rockstar:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform vec2 Smoothness;
uniform float Thickness;
uniform float DashLength;
uniform float GapLength;
uniform vec4 Dashed;
uniform vec4 ColorModulator;

out vec4 OutColor;

float dashMask(float along) {
    float period = max(DashLength + GapLength, 0.001);
    float m = mod(along, period);
    float rise = smoothstep(-0.5, 0.5, m);
    float fall = 1.0 - smoothstep(DashLength - 0.5, DashLength + 0.5, m);
    return rise * fall;
}

void main() {
    vec2 p = FragCoord * Size;
    vec2 center = Size * 0.5;

    float distance = roundedBoxSDF(center - p, center - 1.0, Radius);
    float inner = sdfAA(distance, Smoothness.x);
    float outer = sdfAA(distance, Smoothness.y);

    float ring = smoothstep(1.0 - Thickness - inner - outer, 1.0 - Thickness - outer, distance);
    ring *= 1.0 - smoothstep(1.0 - outer, 1.0, distance);

    float distL = p.x, distT = p.y, distR = Size.x - p.x, distB = Size.y - p.y;
    float s;
    float dashed;
    if (distT <= distR && distT <= distB && distT <= distL) { s = p.x;                                  dashed = Dashed.y; }
    else if (distR <= distB && distR <= distL)              { s = Size.x + p.y;                         dashed = Dashed.z; }
    else if (distB <= distL)                                { s = Size.x + Size.y + (Size.x - p.x);     dashed = Dashed.w; }
    else                                                    { s = 2.0 * Size.x + Size.y + (Size.y - p.y); dashed = Dashed.x; }

    float alpha = ring * mix(1.0, dashMask(s), dashed);

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);
    if (finalColor.a <= 0.0) {
        discard;
    }
    OutColor = finalColor * ColorModulator;
}
