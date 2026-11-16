#version 150

in vec4 vertexColor;
in vec3 localPos;
in vec2 quadUv;

out vec4 fragColor;

uniform float time;
uniform float alpha;
uniform vec4 baseColor;
uniform vec4 accentColor;
uniform float softness;

#define PI 3.14159265359

float psrdnoise(vec2 x, vec2 period, float rot, out vec2 grad) {
    vec2 uv = vec2(x.x + x.y * 0.5, x.y);
    vec2 i0 = floor(uv);
    vec2 f0 = fract(uv);
    float cmp = step(f0.y, f0.x);
    vec2 o1 = vec2(cmp, 1.0 - cmp);
    vec2 i1 = i0 + o1;
    vec2 i2 = i0 + vec2(1.0, 1.0);
    vec2 v0 = vec2(i0.x - i0.y * 0.5, i0.y);
    vec2 v1 = vec2(v0.x + o1.x - o1.y * 0.5, v0.y + o1.y);
    vec2 v2 = vec2(v0.x + 0.5, v0.y + 1.0);
    vec2 x0 = x - v0;
    vec2 x1 = x - v1;
    vec2 x2 = x - v2;
    vec3 iu, iv;
    vec3 xw, yw;
    if (any(greaterThan(period, vec2(0.0)))) {
        xw = vec3(v0.x, v1.x, v2.x);
        yw = vec3(v0.y, v1.y, v2.y);
        if (period.x > 0.0) xw = mod(vec3(v0.x, v1.x, v2.x), period.x);
        if (period.y > 0.0) yw = mod(vec3(v0.y, v1.y, v2.y), period.y);
        iu = floor(xw + 0.5 * yw + 0.5);
        iv = floor(yw + 0.5);
    } else {
        iu = vec3(i0.x, i1.x, i2.x);
        iv = vec3(i0.y, i1.y, i2.y);
    }
    vec3 hash = mod(iu, 289.0);
    hash = mod((hash * 51.0 + 2.0) * hash + iv, 289.0);
    hash = mod((hash * 34.0 + 10.0) * hash, 289.0);
    vec3 psi = hash * 0.07482 + rot;
    vec3 gx = cos(psi);
    vec3 gy = sin(psi);
    vec2 g0 = vec2(gx.x, gy.x);
    vec2 g1 = vec2(gx.y, gy.y);
    vec2 g2 = vec2(gx.z, gy.z);
    vec3 w = 0.8 - vec3(dot(x0, x0), dot(x1, x1), dot(x2, x2));
    w = max(w, 0.0);
    vec3 w2 = w * w;
    vec3 w4 = w2 * w2;
    vec3 gdotx = vec3(dot(g0, x0), dot(g1, x1), dot(g2, x2));
    float n = dot(w4, gdotx);
    vec3 w3 = w2 * w;
    vec3 dw = -8.0 * w3 * gdotx;
    vec2 dn0 = w4.x * g0 + dw.x * x0;
    vec2 dn1 = w4.y * g1 + dw.y * x1;
    vec2 dn2 = w4.z * g2 + dw.z * x2;
    grad = 10.9 * (dn0 + dn1 + dn2);
    return 10.9 * n;
}

float bounceOut(float t) {
    const float a = 4.0 / 11.0;
    const float b = 8.0 / 11.0;
    const float c = 9.0 / 10.0;
    const float ca = 4356.0 / 361.0;
    const float cb = 35442.0 / 1805.0;
    const float cc = 16061.0 / 1805.0;
    float t2 = t * t;
    return t < a
        ? 7.5625 * t2
        : t < b
        ? 9.075 * t2 - 9.9 * t + 3.4
        : t < c
        ? ca * t2 - cb * t + cc
        : 10.8 * t * t - 20.52 * t + 10.72;
}

float bounceIn(float t) { return 1.0 - bounceOut(1.0 - t); }

vec2 rot(vec2 v, float a) {
    return mat2(cos(a), -sin(a), sin(a), cos(a)) * v;
}

void main() {
    vec2 st = localPos.xz + localPos.y * 0.5;
    st = rot(st, -PI / 8.0);

    vec2 grad;
    float n = psrdnoise(vec2(3.0) * st, vec2(0.0), 1.2 * time, grad);

    float lines = cos((st.x + n * 0.1 + 0.2) * PI);

    float t = bounceIn(lines * 0.5 + 0.5);

    vec3 col = mix(
        accentColor.rgb * 0.7,
        baseColor.rgb * 0.15,
        t
    );

    float edgeDist = min(min(quadUv.x, 1.0 - quadUv.x), min(quadUv.y, 1.0 - quadUv.y));
    float edge = 1.0 - smoothstep(0.0, 0.12 + softness * 0.25, edgeDist);
    col += accentColor.rgb * edge * 0.06;

    float finalAlpha = 0.45 * alpha * vertexColor.a;

    fragColor = vec4(col, clamp(finalAlpha, 0.0, 1.0));
}
