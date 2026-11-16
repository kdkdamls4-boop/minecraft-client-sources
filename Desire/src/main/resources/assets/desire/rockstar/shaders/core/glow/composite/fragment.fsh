#version 150

#moj_import <rockstar:common.glsl>

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec2 Resolution;
uniform float Offset;
uniform float OutlineStrength;
uniform float OutlineRadius;
uniform vec2  GlowOffset;

out vec4 OutColor;

void main() {
    vec4 center = texture(Sampler0, TexCoord);

    if (center.a > 0.05) {
        OutColor = vec4(0.0);
        return;
    }

    vec2 px = Resolution * OutlineRadius;
    vec4 s[8];
    s[0] = texture(Sampler0, TexCoord + vec2( px.x, 0.0));
    s[1] = texture(Sampler0, TexCoord + vec2(-px.x, 0.0));
    s[2] = texture(Sampler0, TexCoord + vec2(0.0,  px.y));
    s[3] = texture(Sampler0, TexCoord + vec2(0.0, -px.y));
    s[4] = texture(Sampler0, TexCoord + vec2( px.x,  px.y));
    s[5] = texture(Sampler0, TexCoord + vec2(-px.x,  px.y));
    s[6] = texture(Sampler0, TexCoord + vec2( px.x, -px.y));
    s[7] = texture(Sampler0, TexCoord + vec2(-px.x, -px.y));

    vec3 nColorAcc = vec3(0.0);
    float nAlphaAcc = 0.0;
    float maxA = 0.0;
    for (int i = 0; i < 8; ++i) {
        nColorAcc += s[i].rgb;
        nAlphaAcc += s[i].a;
        maxA = max(maxA, s[i].a);
    }
    vec3 outlineRgb = nColorAcc / max(nAlphaAcc, 0.001);

    vec2 gpx = Resolution * 4.5;
    vec2 gOff = GlowOffset * Resolution;
    vec4 g0 = texture(Sampler1, TexCoord + gOff + vec2( gpx.x,  gpx.y));
    vec4 g1 = texture(Sampler1, TexCoord + gOff + vec2(-gpx.x,  gpx.y));
    vec4 g2 = texture(Sampler1, TexCoord + gOff + vec2( gpx.x, -gpx.y));
    vec4 g3 = texture(Sampler1, TexCoord + gOff + vec2(-gpx.x, -gpx.y));
    vec4 blur = (g0 + g1 + g2 + g3) * 0.25;

    vec3 glow = blur.rgb * Offset;

    vec3 outline = outlineRgb * maxA * OutlineStrength;

    OutColor = vec4(glow + outline, 1.0);
}
