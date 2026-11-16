#version 330

#moj_import <desire:common.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;

out vec4 fragColor;

vec4 blend(vec2 uv) {
    vec4 top = mix(uTopLeftColor, uTopRightColor, uv.x);
    vec4 bottom = mix(uBottomLeftColor, uBottomRightColor, uv.x);

    return mix(top, bottom, uv.y);
}

void main() {
    float alpha = ralpha(uSize, FragCoord, uRadius, uSmoothness);

    if (alpha <= 0.0) {
        discard;
    }

    vec2 screenUv = gl_FragCoord.xy / vec2(textureSize(Sampler0, 0));
    vec4 color = mix(texture(Sampler0, screenUv), blend(FragCoord), uMix);
    color.a *= alpha * uAlpha;

    fragColor = color;
}
