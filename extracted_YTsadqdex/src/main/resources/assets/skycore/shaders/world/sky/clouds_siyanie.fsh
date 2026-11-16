#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <skycore:block_outline_common.glsl>
#moj_import <skycore:world_sky_common.glsl>
#moj_import <skycore:siyanie_common.glsl>

 
void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 direction = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z;
    finalColor = siyanieField(direction, time);
}
