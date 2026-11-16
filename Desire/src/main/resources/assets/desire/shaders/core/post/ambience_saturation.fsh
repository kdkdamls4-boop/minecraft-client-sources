#version 330

#moj_import <desire:common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(InSampler, texCoord);
    float luma = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 saturated = max(mix(vec3(luma), color.rgb, uRadius.x), 0.0);
    fragColor = vec4(saturated, color.a);
}
