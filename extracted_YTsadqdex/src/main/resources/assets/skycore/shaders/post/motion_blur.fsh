#version 330 core

layout(std140) uniform MotionBlurParams {
    vec4 config;
};

#define strength (config.x)

uniform sampler2D CurrentSampler;
uniform sampler2D HistorySampler;

in vec2 uv;
out vec4 finalColor;

void main() {
    vec4 current = texture(CurrentSampler, uv);
    vec4 previous = texture(HistorySampler, uv);
    float amount = clamp(strength, 0.0, 0.98);
    finalColor = mix(current, previous, amount);
}
