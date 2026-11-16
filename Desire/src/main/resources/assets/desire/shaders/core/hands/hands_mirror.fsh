#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform HandsData {
    vec4 colorA;
    vec4 colorB;
    vec4 params0;
    vec4 params1;
    vec4 params2;
};

void main() {
    vec4 mask = texture(Sampler0, texCoord);
    if (mask.a < 0.01) discard;

    vec3 blur = texture(Sampler1, texCoord).rgb;
    vec3 tinted = blur * colorA.rgb;
    vec3 col = mix(tinted, mask.rgb, params0.x);
    fragColor = vec4(col, mask.a * colorA.a);
}
