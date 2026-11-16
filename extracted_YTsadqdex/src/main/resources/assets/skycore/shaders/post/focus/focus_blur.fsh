#version 330 core

uniform sampler2D SceneSampler;
uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FocusBlurUniforms {
    mat4 InvViewProjection;
    vec4 FocusParams;
    vec4 LensParams;
};

in vec2 uv;
out vec4 finalColor;

bool isSkyDepth(float depth) {
    if (FocusParams.w > 0.5) {
        return depth <= 1.0e-5;
    }
    return depth >= 0.99999;
}

vec3 reconstructPosition(vec2 coords, float depth) {
    float clipZ = FocusParams.w > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 ndc = vec4(coords * 2.0 - 1.0, clipZ, 1.0);
    vec4 position = InvViewProjection * ndc;
    return position.xyz / max(position.w, 1.0e-6);
}

void main() {
    vec3 sceneColor = texture(SceneSampler, uv).rgb;
    vec3 blurredColor = texture(BlurSampler, uv).rgb;
    float depthSample = texture(DepthSampler, uv).r;

    float focusDepth = max(FocusParams.x, 0.1);
    float depthWindow = max(FocusParams.y, 0.05);
    float focusBlend = clamp(FocusParams.z, 0.0, 1.0);
    float radius = max(LensParams.x, 0.02);
    float aspect = max(LensParams.y, 0.1);

    float depthSharpness = 0.0;
    if (!isSkyDepth(depthSample)) {
        float linearDepth = length(reconstructPosition(uv, depthSample));
        float depthDelta = abs(linearDepth - focusDepth);
        depthSharpness = 1.0 - smoothstep(depthWindow, depthWindow * 3.4, depthDelta);
    }

    vec2 centered = uv - vec2(0.5);
    centered.x *= aspect;
    float radialDistance = length(centered);
    float radialSharpness = 1.0 - smoothstep(radius, radius * 1.8, radialDistance);

    float subjectSharpness = depthSharpness * radialSharpness;
    subjectSharpness = smoothstep(0.0, 1.0, subjectSharpness);
    float blurAmount = focusBlend * (1.0 - subjectSharpness);
    blurAmount = smoothstep(0.0, 1.0, blurAmount);

    vec3 color = mix(sceneColor, blurredColor, blurAmount);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
