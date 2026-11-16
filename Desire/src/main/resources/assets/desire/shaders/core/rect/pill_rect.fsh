#version 330

#moj_import <desire:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

void main() {
    vec2 halfSize = uSize * 0.5;
    float pad = uSmoothness * 0.75;
    vec2 inner = halfSize - vec2(pad);

    float capR = clamp(uRadius.x, 0.0, min(inner.x, inner.y));
    vec2 p = halfSize - FragCoord * uSize;

    float rx = (p.x > 0.0) ? 0.0 : capR;
    vec2 q = vec2(abs(p.x) - inner.x + rx, abs(p.y) - inner.y + rx);
    float dist = length(max(q, vec2(0.0))) + min(max(q.x, q.y), 0.0) - rx;

    float alpha = (1.0 - smoothstep(-uSmoothness * 0.5, uSmoothness * 0.5, dist)) * FragColor.a;

    if (alpha == 0.0) {
        discard;
    }

    fragColor = vec4(FragColor.rgb, alpha);
}
