#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform HandsData {
    vec4 colorA;
    vec4 colorB;
    vec4 params0;
    vec4 params1;
    vec4 params2;
};

vec3 hue2rgb(float h) {
    vec3 p = abs(fract(vec3(h) + vec3(0.0, 2.0/3.0, 1.0/3.0)) * 6.0 - 3.0);
    return clamp(p - 1.0, 0.0, 1.0);
}

void main() {
    vec4 c = texture(Sampler0, texCoord);
    if (c.a > 0.01) discard;

    vec2 texelSize = params0.xy;
    float width = params0.z;
    float colorMode = params0.w;

    float maxA = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            if (x == 0 && y == 0) continue;
            vec2 d = vec2(float(x), float(y)) * texelSize * width;
            maxA = max(maxA, texture(Sampler0, texCoord + d).a);
        }
    }
    if (maxA < 0.01) discard;

    vec3 col;
    if (colorMode > 1.5) {
        vec4 b = texture(Sampler1, texCoord);
        vec3 cb = b.rgb / max(b.a, 0.001);
        float m = max(cb.r, max(cb.g, cb.b));
        if (m > 0.001) cb /= m;
        float l = dot(cb, vec3(0.299, 0.587, 0.114));
        col = clamp(mix(vec3(l), cb, params2.x), 0.0, 1.0);
    } else if (colorMode > 0.5) {
        float yN = gl_FragCoord.y / max(params1.w, 1.0);
        float h = fract(yN * params1.z + params1.x * params1.y);
        col = hue2rgb(h);
    } else {
        col = colorA.rgb;
    }

    fragColor = vec4(col, maxA * colorA.a);
}
