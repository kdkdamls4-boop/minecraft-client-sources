#version 330 core
layout(location = 0) in vec3 Position;
layout(location = 1) in vec2 UV0;
out vec2 uv;
#moj_import <skycore:source_overlay.glsl>
void main() {
    gl_Position = Transform * vec4(Position, 1.0);
    uv = UV0;
}
