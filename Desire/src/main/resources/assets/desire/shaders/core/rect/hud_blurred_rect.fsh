#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <desire:common.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 screenUv;
in vec4 tintColor;

out vec4 fragColor;

void main() {
    vec2 halfSize = max(uSize * 0.5, vec2(0.5));
    float radius = clamp(uRadius.x, 0.0, min(halfSize.x, halfSize.y));
    vec2 localPos = (FragCoord - 0.5) * uSize;
    vec2 q = abs(localPos) - halfSize + vec2(radius);
    float dist = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
    float aa = max(fwidth(dist), 0.0001);
    float mask = clamp(0.5 - dist / aa, 0.0, 1.0);
    if (mask <= 0.003) {
        discard;
    }

    vec3 blurred = texture(Sampler0, clamp(screenUv, vec2(0.0), vec2(1.0))).rgb;
    vec4 tint = tintColor * ColorModulator;
    vec3 panel = mix(blurred, tint.rgb, clamp(tint.a, 0.0, 1.0));
    panel += vec3((fract(sin(dot(gl_FragCoord.xy, vec2(12.9898, 78.233))) * 43758.5453) - 0.5) / 255.0);

    float alpha = mask * uAlpha;
    if (alpha <= 0.003) {
        discard;
    }
    fragColor = vec4(panel, alpha);
}
