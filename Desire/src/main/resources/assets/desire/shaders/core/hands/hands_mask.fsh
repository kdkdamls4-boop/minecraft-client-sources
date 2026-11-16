#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D BeforeSampler;
uniform sampler2D AfterSampler;
uniform sampler2D DepthBeforeSampler;
uniform sampler2D DepthAfterSampler;

layout(std140) uniform MaskData {
    vec4 params;
};

void main() {
    vec3 beforeColor = texture(BeforeSampler, texCoord).rgb;
    vec3 itemColor = texture(AfterSampler, texCoord).rgb;
    float colorDelta = max(max(abs(itemColor.r - beforeColor.r), abs(itemColor.g - beforeColor.g)), abs(itemColor.b - beforeColor.b));
    float depthDelta = abs(texture(DepthAfterSampler, texCoord).r - texture(DepthBeforeSampler, texCoord).r);
    float threshold = max(params.z, 0.001);
    float result = smoothstep(threshold, threshold * 3.0, max(colorDelta, depthDelta));
    fragColor = vec4(itemColor * result, result);
}
