#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <skycore:ui_common.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;
in float LineWidth;

out vec2 localPos;
out vec4 ringColor;
flat out vec2 halfSize;
flat out float ringThickness;
flat out float healthProgress;
flat out int ringStyle;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    localPos = UV0;
    ringColor = Color;
    halfSize = vec2(UV1) / UI_SIZE_SCALE * 0.5;
    ringThickness = max(LineWidth, 0.0);
    healthProgress = clamp(float(UV2.x) / 32767.0, 0.0, 1.0);
    ringStyle = UV2.y;
}
