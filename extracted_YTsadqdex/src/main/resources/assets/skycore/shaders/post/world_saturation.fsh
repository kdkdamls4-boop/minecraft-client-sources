#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D SceneSampler;

layout(std140) uniform SaturationUniforms {
    vec4 SaturationParams;
};

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
    vec3 source = texture(SceneSampler, uv).rgb;
    float sourceLuma = max(dot(source, LUMA), 0.0001);
    float temperature = SaturationParams.y;
    vec3 warmBalance = vec3(1.11, 1.025, 0.88);
    vec3 coldBalance = vec3(0.88, 1.015, 1.12);
    vec3 balance = temperature >= 0.0
            ? mix(vec3(1.0), warmBalance, temperature)
            : mix(vec3(1.0), coldBalance, -temperature);
    vec3 color = source * balance;
    float balancedLuma = max(dot(color, LUMA), 0.0001);
    color *= sourceLuma / balancedLuma;
    float luma = dot(color, LUMA);
    float maximum = max(color.r, max(color.g, color.b));
    float minimum = min(color.r, min(color.g, color.b));
    float pixelSaturation = maximum - minimum;
    float amount = SaturationParams.x;
    float factor = amount >= 1.0
            ? 1.0 + (amount - 1.0) * (1.0 - pixelSaturation * 0.5)
            : amount;
    finalColor = vec4(clamp(mix(vec3(luma), color, factor), 0.0, 1.0), 1.0);
}
