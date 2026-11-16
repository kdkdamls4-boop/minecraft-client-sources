#version 330 core

in vec2 uv;
in vec4 tint;
out vec4 finalColor;
uniform sampler2D ParticleSampler;

void main() {
    vec4 texel = texture(ParticleSampler, uv);
    float coverage = texel.a * tint.a;
    if (coverage == 0.0) discard;
    finalColor = vec4(texel.rgb * tint.rgb * coverage * 2.0, coverage);
}
