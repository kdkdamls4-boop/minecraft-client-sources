#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform float Time;
uniform vec3 Accent;
uniform vec2 ItemCenter;
uniform vec4 ColorModulator;

out vec4 OutColor;

float plasmaFlow(vec2 uv, float t) {
    vec2 p = uv * 7.0 - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.006;
    for (int n = 0; n < 6; n++) {
        float tn = t * 0.7 + float(n) * 0.9;
        i = p + vec2(cos(tn - i.x) + sin(tn + i.y * 1.3),
                     sin(tn - i.y) + cos(tn + i.x * 1.3));
        c += 1.0 / length(vec2(p.x / (sin(i.x + tn) / inten),
                               p.y / (cos(i.y + tn) / inten)));
    }
    c /= 6.0;
    c = 1.16 - pow(c, 1.35);
    return clamp(pow(abs(c), 8.0), 0.0, 5.0);
}

void main() {
    vec4 mask = texture(Sampler0, TexCoord);
    if (mask.a < 0.01) discard;

    vec3 accent = max(Accent, vec3(0.05));
    float t = Time;

    vec2 uv = TexCoord - ItemCenter + vec2(0.5);

    float c1 = plasmaFlow(uv * 1.8, t);
    float c2 = plasmaFlow(uv * 2.6 - vec2(0.7, 0.4), t * 1.3) * 0.7;
    float c  = max(c1, c2);

    vec3 deep      = accent * 0.18;
    vec3 mid       = accent;
    vec3 hotShift  = mix(accent, accent.gbr * 1.4, 0.5);
    vec3 bright    = mix(accent, vec3(1.0), 0.55);

    vec3 col = mix(deep, mid, smoothstep(0.0, 0.5, c));
    col = mix(col, hotShift, smoothstep(0.4, 1.0, c));
    col = mix(col, bright * 2.2, smoothstep(0.85, 1.6, c));

    float edge = smoothstep(0.05, 0.35, mask.a);
    OutColor = vec4(col * edge, mask.a) * ColorModulator;
}
