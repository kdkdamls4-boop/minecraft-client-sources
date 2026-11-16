#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform float Time;
uniform vec3 Accent;
uniform vec2 ItemCenter;
uniform vec4 ColorModulator;

out vec4 OutColor;

float lightning(vec2 uv, float t) {
    vec2 p = uv * 5.5 - 250.0;
    vec2 i = p;
    float c = 1.0;
    float inten = 0.0040;
    for (int n = 0; n < 6; n++) {
        float tn = t * 0.55 + float(n);
        i = p + vec2(cos(tn - i.x) + sin(tn + i.y),
                     sin(tn - i.y) + cos(tn + i.x));
        c += 1.0 / length(vec2(p.x / (sin(i.x + tn) / inten),
                               p.y / (cos(i.y + tn) / inten)));
    }
    c /= 6.0;
    c = 1.20 - pow(c, 1.55);
    return clamp(pow(abs(c), 10.0), 0.0, 5.0);
}

void main() {
    vec4 mask = texture(Sampler0, TexCoord);
    if (mask.a < 0.01) discard;

    vec3 accent = max(Accent, vec3(0.05));
    float t = Time;

    vec2 uv = TexCoord - ItemCenter + vec2(0.5);
    float c = lightning(uv * 1.4, t);

    float flash = 0.75 + 0.25 * sin(t * 7.0) * sin(t * 5.3);

    vec3 bolt = accent * (1.8 + flash * 0.5);
    vec3 halo = accent * 0.9;

    float core = smoothstep(0.50, 1.2, c);
    float glow = smoothstep(0.10, 0.50, c) * 0.50;
    float a    = max(core, glow);

    vec3 col = mix(halo, bolt, core);

    float silhouette = smoothstep(0.05, 0.35, mask.a);
    OutColor = vec4(col, a * silhouette) * ColorModulator;
}
