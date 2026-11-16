#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

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

float webPattern(vec2 uv, float time, float scale) {
    uv *= max(scale, 0.01) * 7.0;

    vec2 gv = fract(uv) - 0.5;
    vec2 id = floor(uv);

    float lineX = 1.0 - smoothstep(0.015, 0.075, abs(gv.x));
    float lineY = 1.0 - smoothstep(0.015, 0.075, abs(gv.y));

    float diag1 = 1.0 - smoothstep(0.015, 0.075, abs(gv.x - gv.y));
    float diag2 = 1.0 - smoothstep(0.015, 0.075, abs(gv.x + gv.y));

    float pulse = 0.55 + 0.45 * sin(time * 2.2 + dot(id, vec2(0.7, 1.3)));

    return clamp((lineX + lineY) * 0.35 + (diag1 + diag2) * 0.22, 0.0, 1.0) * pulse;
}

float plasmaPattern(vec2 uv, float time, float scale) {
    uv = (uv - 0.5) * max(scale, 0.01) * 5.0;

    float v = 0.0;
    v += sin(uv.x * 1.7 + time);
    v += sin(uv.y * 2.1 - time * 1.25);
    v += sin((uv.x + uv.y) * 1.4 + time * 0.7);
    v += sin(length(uv + vec2(sin(time), cos(time))) * 3.0);

    return 0.5 + 0.5 * sin(v);
}

void main() {
    vec4 s = texture(Sampler0, texCoord);

    float fill = params0.x;
    float mode = params0.y;
    float alpha = colorA.a;

    float inside = smoothstep(0.05, 0.95, s.a);
    float a = clamp(alpha * inside * fill, 0.0, 1.0);

    if (a <= 0.001) discard;

    float time = params1.x * params1.y;
    float scale = params1.z;

    vec3 f;

    if (mode < 0.5) {
        f = colorA.rgb;
    } else if (mode < 1.5) {
        float yN = gl_FragCoord.y / max(params1.w, 1.0);
        float h = fract(yN * scale + time);
        f = hue2rgb(h);
    } else if (mode < 2.5) {
        float p = webPattern(texCoord, time, scale);
        f = mix(colorA.rgb * 0.35, mix(colorA.rgb, colorB.rgb, p), p);
    } else {
        float p = plasmaPattern(texCoord, time, scale);
        f = mix(colorA.rgb, colorB.rgb, p);
    }

    if (params0.z > 0.5) {
        float l = dot(s.rgb, vec3(0.299, 0.587, 0.114));
        f *= mix(1.0, l, params0.w);
    }

    fragColor = vec4(f * a, a);
}