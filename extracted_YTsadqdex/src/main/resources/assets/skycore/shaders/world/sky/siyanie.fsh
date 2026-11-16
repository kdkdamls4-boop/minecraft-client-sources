#version 330 core
in vec2 uv;
out vec4 fragColor;
uniform sampler2D DepthSampler;
#moj_import <skycore:world_sky_common.glsl>
#moj_import <skycore:dinasty_siyanie.glsl>
void main() {
    if (texture(DepthSampler, uv).r > 0.000001) discard;
    fragColor = vec4(siyanieSky(skyDirection(uv), PrimaryColor.rgb, SecondaryColor.rgb,
            SkyParams.x * SkyParams.z, SkyParams.y, SkyTexel.z, SkyTexel.w), 1.0);
}