#version 330 core

uniform sampler2D CurrentSampler;

in vec2 uv;
out vec4 finalColor;

void main() {
    finalColor = texture(CurrentSampler, uv);
}
