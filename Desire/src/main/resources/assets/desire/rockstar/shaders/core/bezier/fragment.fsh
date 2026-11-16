#version 150

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec2 P0, P1, P2, P3;
uniform float Thickness;
uniform vec4 ColorModulator;

out vec4 OutColor;

vec2 cubic(float t) {
    float u = 1.0 - t;
    return u * u * u * P0 + 3.0 * u * u * t * P1 + 3.0 * u * t * t * P2 + t * t * t * P3;
}

float segDist(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / max(dot(ba, ba), 1e-6), 0.0, 1.0);
    return length(pa - ba * h);
}

void main() {
    vec2 p = FragCoord * Size;

    const int N = 48;
    float best = 1e9;
    vec2 prev = P0;
    for (int i = 1; i <= N; i++) {
        vec2 cur = cubic(float(i) / float(N));
        best = min(best, segDist(p, prev, cur));
        prev = cur;
    }

    float halfWidth = Thickness * 0.5;
    float alpha = 1.0 - smoothstep(halfWidth - 0.6, halfWidth + 0.6, best);

    vec4 col = vec4(FragColor.rgb, FragColor.a * alpha);
    if (col.a <= 0.0) {
        discard;
    }
    OutColor = col * ColorModulator;
}
