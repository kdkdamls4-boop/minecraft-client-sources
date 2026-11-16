#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <desire:common.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec2 FragCoord;
out vec2 screenUv;
out vec4 tintColor;

void main() {
    FragCoord = rvertexcoord(gl_VertexID);
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    screenUv = gl_Position.xy / gl_Position.w * 0.5 + 0.5;
    tintColor = Color;
}
