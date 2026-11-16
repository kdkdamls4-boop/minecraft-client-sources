#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D CurrentInput;

layout(std140) uniform KawaseData {
    vec4 params;
};

void main() {
    vec2 uv = texCoord;
    vec2 halfPixel = (0.5 / max(params.xy, vec2(1.0))) * params.z;

    vec4 sum = texture(CurrentInput, uv) * 4.0;
    sum += texture(CurrentInput, uv - halfPixel);
    sum += texture(CurrentInput, uv + halfPixel);
    sum += texture(CurrentInput, uv + vec2(halfPixel.x, -halfPixel.y));
    sum += texture(CurrentInput, uv - vec2(halfPixel.x, -halfPixel.y));

    fragColor = sum / 8.0;
}
