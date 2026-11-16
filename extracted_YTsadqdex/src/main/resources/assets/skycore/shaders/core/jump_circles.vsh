#version 330 core
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
layout(std140) uniform Projection { mat4 ProjMat; };
out vec2 local;
void main() {
    gl_Position = ProjMat * vec4(Position, 1.0);
    local = UV0; // world-space disk coordinates, -1..1
}
