#version 330 core
in vec2 uv;
out vec4 finalColor;
uniform sampler2D DepthSampler;
#moj_import <skycore:source_overlay.glsl>
#moj_import <skycore:source_plasma.glsl>
void main() {
     
    float sceneDepth = texture(DepthSampler, gl_FragCoord.xy * Viewport.xy).r;
    if (sceneDepth > gl_FragCoord.z + 0.000001) discard;
     
    finalColor = sourcePlasma(uv, 1.0);
}
