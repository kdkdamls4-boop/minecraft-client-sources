#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>
#moj_import <skycore:ui_common.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in float LineWidth;

out vec2 quadUv;
out vec2 aspect;
out vec4 primaryColor;
flat out vec4 secondaryColor;
flat out float mode;
flat out float elapsed;
flat out float cornerRadius;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    quadUv = UV0;
    primaryColor = Color;
    secondaryColor = ui_unpackColor(UV1.x, UV1.y);
    elapsed = Position.z;
    mode = float(UV2.x);
    aspect = vec2(max(float(UV2.y) / UI_RADIUS_SCALE, 0.0001), 1.0);
    cornerRadius = LineWidth;
}
