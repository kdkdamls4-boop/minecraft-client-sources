#version 330

#moj_import <desire:common.glsl>

#define fillColor (uTopLeftColor)

uniform sampler2D Sampler0;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float maskAlpha = texture(Sampler0, texCoord).a;
    if (maskAlpha < 0.01) {
        discard;
    }

    float alpha = clamp(maskAlpha, 0.0, 1.0) * fillColor.a;
    if (alpha < 0.002) {
        discard;
    }
    fragColor = vec4(fillColor.rgb, alpha);
}
