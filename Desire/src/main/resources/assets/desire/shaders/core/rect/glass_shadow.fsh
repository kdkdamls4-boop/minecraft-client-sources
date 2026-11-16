#version 330

#moj_import <desire:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

out vec4 fragColor;

void main() {
    float blur = max(uSmoothness, 0.5);
    vec2 expanded = uSize + vec2(blur * 2.0);
    vec2 pixel = FragCoord * expanded;
    vec2 fromCenter = expanded * 0.5 - pixel;
    float dist = rdist(fromCenter, uSize * 0.5, uRadius);
    float x = max(dist, 0.0) / blur;
    float falloff = exp(-4.5 * x * x);
    float alpha = FragColor.a * falloff;
    if (alpha <= 0.002) {
        discard;
    }
    fragColor = vec4(FragColor.rgb, alpha);
}
