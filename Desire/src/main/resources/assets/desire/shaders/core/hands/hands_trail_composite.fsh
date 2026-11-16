#version 330

#moj_import <desire:common.glsl>

#define flameColor (uTopLeftColor)

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float hand = texture(Sampler1, texCoord).a;
    float coverage = texture(Sampler0, texCoord).a;
    float alpha = clamp(coverage * flameColor.a * 1.10, 0.0, 1.0);
    alpha *= 1.0 - smoothstep(0.72, 0.98, hand);
    if (alpha <= 0.001) {
        discard;
    }
    vec3 color = flameColor.rgb * (1.0 + alpha * 0.42);
    fragColor = vec4(color * alpha, alpha);
}
