#version 330 core

in vec2 uv;
out vec4 color;

uniform sampler2D CurrentInput;

layout(std140) uniform GuiKawaseUniforms {
    vec2 Resolution;
    vec2 Padding;
    float Offset;
};

void main() {
    float off = Offset + 0.5;
    vec2 d = Resolution * off * 1.5;

    vec3 sum = texture(CurrentInput, uv + vec2(d.x, d.y)).rgb;
    sum += texture(CurrentInput, uv + vec2(-d.x, d.y)).rgb;
    sum += texture(CurrentInput, uv + vec2(d.x, -d.y)).rgb;
    sum += texture(CurrentInput, uv + vec2(-d.x, -d.y)).rgb;

    color = vec4(sum * 0.25, 1.0);
}