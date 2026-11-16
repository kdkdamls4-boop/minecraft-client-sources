#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform TrailData {
    vec4 a;
    vec4 b;
    vec4 c;
    vec4 d;
    vec4 e;
};

void main() {
    vec4 col = texture(Sampler0, texCoord);
    fragColor = col * e.w;
}
