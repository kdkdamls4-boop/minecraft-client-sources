#version 330

#moj_import <desire:common.glsl>


#define u_Resolution (uRadius.xy)
#define uResolution  (uRadius.xy)
#define u_CameraDir  (uShape.yz)
#define uCameraDir   (uShape.yz)
#define u_Time       (uShape.x)
#define uTime        (uShape.x)
#define u_Scale      (uShape.w)
#define uScale       (uShape.w)
#define u_Fov        (uBottomRightColor.x)
#define uFov         (uBottomRightColor.x)
#define u_Intensity  (uBottomRightColor.y)
#define uIntensity   (uBottomRightColor.y)
#define uSpeed       (uBottomRightColor.z)
#define u_Night      (uBottomRightColor.w)
#define uNight       (uBottomRightColor.w)
#define uRain        (uTopRightColor.x)
#define uThunder     (uTopRightColor.y)
#define uThemeMix    (uTopRightColor.z)
#define uColor       (uTopLeftColor.rgb)
#define uColor2      (uBottomLeftColor.rgb)

uniform sampler2D DepthSampler;

#define SKY_DEPTH_DISCARD() if (texture(DepthSampler, texCoord).r > 0.0005) discard;

#define uThunderInterval (uText.x)
#define uThunderChance   (uText.y)
#define uThunderGlow     (uText.z)

