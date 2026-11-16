#version 330

#moj_import <desire:common.glsl>

uniform sampler2D Sampler0;

in vec2 FragCoord;
in vec2 TexCoord;

out vec4 fragColor;

#define uMouse (TexCoord)
#define uLensZoom (uMix)

void main() {
    float mask = ralpha(uSize, FragCoord, uRadius, uSmoothness);
    if (mask <= 0.0) {
        discard;
    }

    vec2 resolution = vec2(textureSize(Sampler0, 0));
    vec2 uv = gl_FragCoord.xy / resolution;

    vec2 p = FragCoord * 2.0 - 1.0;
    float roundedBox = pow(abs(p.x), 8.0) + pow(abs(p.y), 8.0);
    roundedBox *= 0.0001;

    float rb1 = clamp((1.0 - roundedBox * 10000.0) * 8.0, 0.0, 1.0);
    float rb2 = clamp((0.95 - roundedBox * 9500.0) * 16.0, 0.0, 1.0)
              - clamp((0.9 - roundedBox * 9500.0) * 16.0, 0.0, 1.0);
    float rb3 = clamp((1.5 - roundedBox * 11000.0) * 2.0, 0.0, 1.0)
              - clamp((1.0 - roundedBox * 11000.0) * 2.0, 0.0, 1.0);

    vec2 m2 = uv - uMouse;
    vec2 lens = (uv - uMouse) * (1.0 - roundedBox * 5000.0 * uLensZoom) + uMouse;

    vec4 blurred = vec4(0.0);
    for (int x = -4; x <= 4; x++) {
        for (int y = -4; y <= 4; y++) {
            vec2 offset = vec2(x, y) * 0.5 / resolution;
            blurred += texture(Sampler0, clamp(offset + lens, 0.0, 1.0));
        }
    }
    blurred /= 81.0;

    float gradient = clamp((clamp(m2.y, 0.0, 0.2) + 0.1) / 2.0, 0.0, 1.0)
                   + clamp((clamp(-m2.y, -1000.0, 0.2) * rb3 + 0.1) / 2.0, 0.0, 1.0);
    vec4 lighting = clamp(blurred + vec4(rb1) * gradient + vec4(rb2) * 0.3, 0.0, 1.0);

    float transition = smoothstep(0.0, 1.0, rb1 + rb2);
    vec4 color = mix(texture(Sampler0, uv), lighting, transition);
    color.a = mask * uAlpha;
    fragColor = color;
}
