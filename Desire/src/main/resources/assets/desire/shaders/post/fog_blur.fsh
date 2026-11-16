#version 330 core

layout(std140) uniform FogBlurUniforms {
    mat4 uInvProjection;
    vec4 uFog;
};

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

in vec2 uv;
out vec4 fragColor;

bool reverseZ() {
    return uFog.w > 0.5;
}

bool isSky(float depth) {
    return reverseZ() ? depth <= 0.000005 : depth >= 0.99999;
}

float clipDepth(float depth) {
    return reverseZ() ? depth : depth * 2.0 - 1.0;
}

float viewDistance(vec2 t, float depth) {
    vec4 clip = vec4(t * 2.0 - 1.0, clipDepth(depth), 1.0);
    vec4 view = uInvProjection * clip;
    return length(view.xyz / max(abs(view.w), 0.00001));
}

void main() {
    vec4 scene = texture(SceneSampler, uv);
    vec4 blurred = texture(BlurSampler, uv);
    float depth = texture(DepthSampler, uv).r;

    float amount = 1.0;
    if (!isSky(depth)) {
        float dist = viewDistance(uv, depth);
        amount = smoothstep(uFog.x, max(uFog.y, uFog.x + 0.01), dist);
    }
    fragColor = vec4(mix(scene.rgb, blurred.rgb, amount * uFog.z), scene.a);
}
