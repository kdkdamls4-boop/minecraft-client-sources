#version 330

#moj_import <desire:common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec2 halfTexel = uShape.xy;
    float offset = uShape.z;
    float dy = halfTexel.y * 2.0 * offset;

    vec4 color = texture(InSampler, texCoord) * 0.227027;
    color += texture(InSampler, texCoord + vec2(0.0, dy * 1.3846153846)) * 0.3162162162;
    color += texture(InSampler, texCoord - vec2(0.0, dy * 1.3846153846)) * 0.3162162162;
    color += texture(InSampler, texCoord + vec2(0.0, dy * 3.2307692308)) * 0.0702702703;
    color += texture(InSampler, texCoord - vec2(0.0, dy * 3.2307692308)) * 0.0702702703;

    fragColor = vec4(color.rgb, 1.0);
}
