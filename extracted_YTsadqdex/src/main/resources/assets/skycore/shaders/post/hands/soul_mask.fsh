#version 330 core
// SOULBASE HandShader shader, CC0 1.0. See SOULBASE_HAND_SHADER_LICENSE.txt.

in vec2 uv;
#define texCoord uv

out vec4 finalColor;

uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;
uniform sampler2D DepthBeforeSampler;
uniform sampler2D DepthAfterSampler;

layout(std140) uniform MaskData {
    vec4 params;
};

void main() {
    float depthBefore = texture(DepthBeforeSampler, texCoord).r;
    float depthAfter = texture(DepthAfterSampler, texCoord).r;

    float depthDiff = depthBefore - depthAfter;

    float topDistance = (params.y - gl_FragCoord.y) / max(params.y, 1.0);
    // Depth is the primary signal: an item rendered in front of the scene.
    bool isHand = depthDiff > 0.0001 && depthAfter < 0.9999;

    // Some translucent item layers do not write depth. A color-only fallback must
    // be restricted to the hand area; otherwise sky/exposure changes turn an
    // entire side of the screen into a bright trail when looking upward.
    if (!isHand && topDistance > 0.24) {
        vec3 before = texture(BeforeSampler, texCoord).rgb;
        vec3 after = texture(AfterSampler, texCoord).rgb;
        vec3 colorDiff = abs(after - before);
        float maxColorDiff = max(max(colorDiff.r, colorDiff.g), colorDiff.b);
        isHand = maxColorDiff > 0.018;
    }

    float result = isHand ? 1.0 : 0.0;
    result *= smoothstep(0.20, 0.28, topDistance);
    finalColor = vec4(result, result, result, 1.0);
}
