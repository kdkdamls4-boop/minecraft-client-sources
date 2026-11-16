#version 330 core

in vec2 uv;
in vec4 tint;
out vec4 finalColor;

uniform sampler2D PetSampler;

void main() {
    vec4 sampled = texture(PetSampler, uv) * tint;
    if (sampled.a < 0.10) {
        discard;
    }
    finalColor = vec4(sampled.rgb, 1.0);
}
