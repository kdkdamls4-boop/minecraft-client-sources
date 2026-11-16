#version 330
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <skycore:ui_common.glsl>
#moj_import <skycore:ui_fragment.glsl>
uniform sampler2D Sampler0;
in vec2 texCoord;
in vec4 fillColor;
flat in vec4 outlineColor;
flat in float pxRange;
flat in float outlineSdf;
out vec4 fragColor;
void main() {
    vec3 msdf = texture(Sampler0, texCoord).rgb;
    float sd = ui_median3(msdf) - 0.5;
    float screenPxRange = ui_screenPxRange(texCoord, vec2(textureSize(Sampler0, 0)), max(pxRange, 1.0));
    float fillAlpha = clamp(sd * screenPxRange + 0.5, 0.0, 1.0);
    vec4 fill = fillColor * ColorModulator;
    vec4 outline = outlineColor * ColorModulator;
    float outlineAlpha = 0.0;
    if (outlineSdf > 0.0 && outline.a > 0.0) {
        float expandedAlpha = clamp((sd + outlineSdf) * screenPxRange + 0.5, 0.0, 1.0);
        outlineAlpha = outline.a * max(expandedAlpha - fillAlpha, 0.0);
    }
    vec4 composed = ui_compositePremul(vec4(outline.rgb * outlineAlpha, outlineAlpha), vec4(fill.rgb * (fill.a * fillAlpha), fill.a * fillAlpha));
    if (composed.a <= 0.001) discard;
    fragColor = ui_unpremultiply(composed);
}