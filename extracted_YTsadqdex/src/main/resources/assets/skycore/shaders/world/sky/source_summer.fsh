#version 330 core
in vec2 uv;
out vec4 finalColor;
uniform sampler2D DepthSampler;
#moj_import <skycore:source_overlay.glsl>
#moj_import <skycore:source_summer.glsl>
void main() {
    if (texture(DepthSampler, gl_FragCoord.xy * Viewport.xy).r > 0.000001) discard;
    finalColor = sourceSummer(uv);
}
