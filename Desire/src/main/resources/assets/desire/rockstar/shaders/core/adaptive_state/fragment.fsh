#version 150


in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

uniform vec2 Threshold;
uniform vec2 Probe;
uniform float Rate;

out vec4 OutColor;

float backdropLuma(vec2 uv) {
    return dot(texture(Sampler0, clamp(uv, 0.0, 1.0)).rgb, vec3(0.2126, 0.7152, 0.0722));
}

void main() {
    float luma = backdropLuma(TexCoord) * 2.0
        + backdropLuma(TexCoord + vec2(Probe.x, 0.0)) + backdropLuma(TexCoord - vec2(Probe.x, 0.0))
        + backdropLuma(TexCoord + vec2(0.0, Probe.y)) + backdropLuma(TexCoord - vec2(0.0, Probe.y));
    luma /= 6.0;

    float prev = texture(Sampler1, TexCoord).r;

    float target = prev > 0.5 ? step(Threshold.x, luma) : step(Threshold.y, luma);

    float state = mix(prev, target, Rate);
    if (abs(target - state) < 0.01) state = target;

    OutColor = vec4(state, state, state, 1.0);
}
