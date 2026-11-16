#version 330 core

in vec4 tint;
out vec4 finalColor;

void main() {
    if (tint.a <= 0.004) {
        discard;
    }
    finalColor = tint;
}
