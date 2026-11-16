#version 150

in vec3 Position;
in vec2 UV;
in vec4 VertColor;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 FragCoord;
out vec4 VertexColor;

void main() {
    FragCoord = UV;
    VertexColor = VertColor / 255.0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
