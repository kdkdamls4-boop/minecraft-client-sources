#version 330

#moj_import <desire:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

void main() {
    float alpha;
    if (uMix > 0.0) {
        vec2 center = uSize * 0.5;
        float dist = rdist(center - (FragCoord * uSize), center - 1.0, uRadius);
        alpha = (1.0 - smoothstep(1.0 - uSmoothness, 1.0, dist)) * smoothstep(-uMix - uSmoothness, -uMix, dist);
    } else {
        alpha = ralpha(uSize, FragCoord, uRadius, uSmoothness);
    }
    vec4 color = vec4(FragColor.rgb, FragColor.a * alpha);

    if (color.a == 0.0) {
        discard;
    }

    fragColor = color;
}
