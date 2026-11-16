#version 330

#moj_import <desire:common.glsl>

#define haloColor (uTopLeftColor)
#define tapScale (uShape.x)

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
out vec4 fragColor;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

void main() {
    float maskAlpha = texture(Sampler1, texCoord).a;
    if (maskAlpha > 0.5) {
        discard;
    }

    float scale = tapScale > 0.001 ? tapScale : 1.5;
    vec2 blurTexel = 1.0 / vec2(textureSize(Sampler0, 0));

    vec2 corner = 2.0 * blurTexel * scale;
    float probe = texture(Sampler0, texCoord).a
            + texture(Sampler0, texCoord + corner).a
            + texture(Sampler0, texCoord - corner).a
            + texture(Sampler0, texCoord + vec2(corner.x, -corner.y)).a
            + texture(Sampler0, texCoord + vec2(-corner.x, corner.y)).a;
    if (probe <= 0.0) {
        discard;
    }

    float alphaSum = 0.0;
    float weightSum = 0.0;
    for (int y = -2; y <= 2; y++) {
        for (int x = -2; x <= 2; x++) {
            float weight = exp(-float(x * x + y * y) / 3.38);
            alphaSum += texture(
                Sampler0,
                texCoord + vec2(float(x), float(y)) * blurTexel * scale
            ).a * weight;
            weightSum += weight;
        }
    }

    float halo = alphaSum / weightSum;
    if (halo < 0.004) {
        discard;
    }
    float tailGate = smoothstep(0.004, 0.025, halo);

    vec3 tint = haloColor.rgb;
    float luma = dot(tint, vec3(0.299, 0.587, 0.114));
    tint = clamp(luma + (tint - luma) * 1.5, 0.0, 1.0);
    float glow = pow(clamp(halo, 0.0, 1.0), 0.85);
    vec3 color = tint * (1.0 + glow * 0.7);
    float intensity = (1.0 - exp(-glow * 2.6)) * haloColor.a * tailGate;

    vec2 pixel = texCoord * vec2(textureSize(Sampler1, 0));
    float dither = (hash21(pixel) + hash21(pixel + 19.7) - 1.0) * (0.5 / 255.0);
    intensity = clamp(
        intensity + dither * smoothstep(0.0, 0.05, intensity),
        0.0,
        1.0
    );
    fragColor = vec4(color, intensity);
}
