#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D CloudSampler;

#moj_import <skycore:block_outline_common.glsl>
#moj_import <skycore:world_sky_common.glsl>

void main() {
    if (texture(DepthSampler, uv).r > 0.000001) {
        discard;
    }
    vec3 direction = skyDirection(uv);
    vec4 clouds = texture(CloudSampler, uv);
    vec3 sunDir = normalize(vec3(0.35, 0.74, 0.57));
    float sunCore = pow(max(dot(direction, sunDir), 0.0), 850.0);
    float sunGlow = pow(max(dot(direction, sunDir), 0.0), 18.0);
    vec3 color = clouds.rgb;
    color += vec3(1.0, 0.82, 0.58) * sunGlow * 0.24 * (1.0 - clouds.a * 0.72);
    color += vec3(1.0, 0.95, 0.82) * sunCore * 2.8 * (1.0 - clouds.a);
    color *= 0.72 + SkyParams.y * 0.28;
    color = color / (1.0 + color * 0.42);
    color = pow(max(color, vec3(0.0)), vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    finalColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
