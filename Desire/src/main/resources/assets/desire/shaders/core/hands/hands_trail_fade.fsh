#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform TrailData {
    vec4 a;
    vec4 b;
    vec4 c;
    vec4 d;
    vec4 e;
};

float wob(float y, float tt) {
    return sin(y * 9.0 + tt * 4.3) * 0.6
         + sin(y * 17.0 - tt * 2.1) * 0.3
         + sin(y * 4.0 + tt * 1.3) * 0.4
         + sin(y * 28.0 + tt * 5.7) * 0.15;
}

vec4 blurSample(vec2 p, vec2 texSize) {
    vec2 px = 1.0 / max(texSize, vec2(1.0));
    vec4 col = texture(Sampler0, p) * 0.36;
    col += texture(Sampler0, p + vec2( px.x, 0.0)) * 0.16;
    col += texture(Sampler0, p + vec2(-px.x, 0.0)) * 0.16;
    col += texture(Sampler0, p + vec2(0.0,  px.y)) * 0.16;
    col += texture(Sampler0, p + vec2(0.0, -px.y)) * 0.16;
    return col;
}

void main() {
    vec2 uv = texCoord;
    vec2 offset = a.xy;
    vec2 texSize = a.zw;
    float fade = b.x;
    float t = b.y;
    float dt = b.z;
    float turb = b.w;
    float flickAmp = c.x;

    float dxTurb = (wob(uv.y, t) - wob(uv.y, t - dt)) * turb;
    float yFactor = 0.7 + uv.y * 0.8;
    vec2 disp = vec2(offset.x + dxTurb, offset.y * yFactor);

    vec4 col = blurSample(uv - disp, texSize);

    float flick = 1.0 - flickAmp + flickAmp * (0.5 + 0.5 * sin(t * 14.0) + 0.25 * sin(t * 9.3));
    float oldA = col.a;
    float newA = max(0.0, oldA * (0.985 * flick) - fade);
    float scale = oldA > 0.001 ? newA / oldA : 0.0;

    fragColor = vec4(col.rgb * scale, newA);
}
