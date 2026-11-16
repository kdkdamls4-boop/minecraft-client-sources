#version 330

#moj_import <desire:common.glsl>

in vec4 FragColor;

out vec4 fragColor;

void main() {
    if (FragColor.a == 0.0) {
        discard;
    }
    fragColor = FragColor;
}
