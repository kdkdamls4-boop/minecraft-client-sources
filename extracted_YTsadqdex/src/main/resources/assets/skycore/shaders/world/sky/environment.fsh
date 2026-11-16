#version 330 core
in vec2 uv;
out vec4 finalColor;
layout(std140) uniform SkyEnvironmentUniforms {
    vec4 PrimaryColor;
    vec4 SecondaryColor;
    vec4 SkyParams;  
    vec4 Detail;  
    vec4 SourceShape;  
    vec4 SourceClock;  
};
#moj_import <skycore:block_outline_common.glsl>
#moj_import <skycore:sky_environment_modes.glsl>
#moj_import <skycore:dinasty_siyanie.glsl>
#define color PrimaryColor.rgb
#define color2 (PrimaryColor.rgb * 0.65)
#define time SourceClock.x
#define speed SourceShape.x
#define scale Detail.z
#define outline SourceShape.y
#define glow SourceShape.z
#define fill SourceShape.w
#define alpha SourceClock.y
#moj_import <skycore:source_plasma.glsl>
#undef color
#undef color2
#undef time
#undef speed
#undef scale
#undef outline
#undef glow
#undef fill
#undef alpha
#define u_Color PrimaryColor
#define u_Scale Detail.z
#define u_Time SourceClock.x
#define u_Night Detail.w
#moj_import <skycore:source_summer.glsl>
void main() {
    float theta = uv.x * 6.28318530718;
    float phi = uv.y * 3.14159265359;
    vec3 direction = vec3(sin(phi) * cos(theta), cos(phi), sin(phi) * sin(theta));
    int mode = int(SkyParams.w + 0.5);
    vec3 rgb;
    if (mode == 0) rgb = envSky_deep_space(direction);
    else if (mode == 1) rgb = envSky_nebula(direction);
    else if (mode == 2) rgb = envSky_plasma(direction);
    else if (mode == 3) rgb = envSky_volumetric(direction);
    else if (mode == 4) rgb = siyanieSky(direction, PrimaryColor.rgb, SecondaryColor.rgb,
            SkyParams.x * SkyParams.z, SkyParams.y, Detail.x, Detail.y);
    else if (mode == 5) { vec4 plasma = sourcePlasma(uv, 1.0); rgb = plasma.rgb * plasma.a; }
    else rgb = sourceSummer(uv).rgb;
    finalColor = vec4(rgb, 1.0);
}