layout(std140) uniform SourceOverlayUniforms {
    mat4 Transform;
    vec4 Primary;
    vec4 Secondary;
    vec4 Animation;  
    vec4 Strength;  
    vec4 Viewport;  
};

#define color Primary.rgb
#define color2 Secondary.rgb
#define time Animation.x
#define speed Animation.y
#define scale Animation.z
#define outline Animation.w
#define glow Strength.x
#define fill Strength.y
#define alpha Strength.z
#define u_Color Primary
#define u_Scale Animation.z
#define u_Time Animation.x
#define u_Night Strength.w
