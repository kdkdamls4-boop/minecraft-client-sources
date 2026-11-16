#version 330

#moj_import <desire:common.glsl>

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

#define uMouse (uRadius.xy)
#define uLensScale (uRadius.z)
#define uLensZoom (uRadius.w)

void main() {
    vec2 uv = texCoord;
    vec2 resolution = vec2(textureSize(InSampler, 0));

    vec2 m2 = uv - uMouse;

    float roundedBox = pow(abs(m2.x * resolution.x / resolution.y), 8.0) + pow(abs(m2.y), 8.0);
    roundedBox *= uLensScale;

    float rb1 = clamp((1.0 - roundedBox * 10000.0) * 8.0, 0.0, 1.0);
    float rb2 = clamp((0.95 - roundedBox * 9500.0) * 16.0, 0.0, 1.0)
              - clamp((0.9 - roundedBox * 9500.0) * 16.0, 0.0, 1.0);
    float rb3 = clamp((1.5 - roundedBox * 11000.0) * 2.0, 0.0, 1.0)
              - clamp((1.0 - roundedBox * 11000.0) * 2.0, 0.0, 1.0);

    fragColor = vec4(0.0);

    float transition = smoothstep(0.0, 1.0, rb1 + rb2);

    if (transition > 0.0) {
        vec2 lens = (uv - 0.5) * (1.0 - roundedBox * 5000.0 * uLensZoom) + 0.5;

        vec4 blurred = vec4(0.0);
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                vec2 offset = vec2(x, y) * 0.5 / resolution;
                blurred += texture(InSampler, offset + lens);
            }
        }
        blurred /= 81.0;

        float gradient = clamp((clamp(m2.y, 0.0, 0.2) + 0.1) / 2.0, 0.0, 1.0)
                       + clamp((clamp(-m2.y, -1000.0, 0.2) * rb3 + 0.1) / 2.0, 0.0, 1.0);
        vec4 lighting = clamp(blurred + vec4(rb1) * gradient + vec4(rb2) * 0.3, 0.0, 1.0);

        fragColor = mix(texture(InSampler, uv), lighting, transition);
    } else {
        fragColor = texture(InSampler, uv);
    }

    fragColor.a = 1.0;
}
