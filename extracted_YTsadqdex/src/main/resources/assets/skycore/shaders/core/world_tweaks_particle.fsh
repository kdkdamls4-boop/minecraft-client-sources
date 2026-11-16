#version 330 core

in vec2 uv;
in vec4 tint;
out vec4 finalColor;

uniform sampler2D ParticleSampler;

void main() {
    vec4 sampled = texture(ParticleSampler, uv) * tint;
    if (sampled.a <= 0.004) {
        discard;
    }
    finalColor = sampled;
}
