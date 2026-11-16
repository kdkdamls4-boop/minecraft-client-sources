#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <desire:common.glsl>

in vec3 Position;
in vec4 Color;

out vec4 FragColor;

void main() {
    FragColor = Color;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
