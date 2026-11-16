#version 330

#moj_import <desire:common.glsl>

#define glassColor (uTopLeftColor)
#define waveTime (uShape.x)
#define waveSpeed (uShape.y)
#define waveScale (uShape.z)
#define glowStrength (uShape.w)

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p *= 2.0;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    vec2 px = 1.0 / vec2(textureSize(Sampler1, 0));
    float maskAlpha = texture(Sampler1, texCoord).a;
    float mR = texture(Sampler1, texCoord + vec2(px.x, 0.0)).a;
    float mL = texture(Sampler1, texCoord - vec2(px.x, 0.0)).a;
    float mU = texture(Sampler1, texCoord + vec2(0.0, px.y)).a;
    float mD = texture(Sampler1, texCoord - vec2(0.0, px.y)).a;
    maskAlpha = max(maskAlpha, max(max(mR, mL), max(mU, mD)));
    if (maskAlpha < 0.01) {
        discard;
    }

    vec2 res = vec2(textureSize(Sampler0, 0));
    float aspect = res.x / res.y;
    vec2 p = vec2(texCoord.x * aspect, texCoord.y);

    float bands = sin(p.y * (9.0 * waveScale) - waveTime * waveSpeed * 2.2);
    float warp = fbm(p * (2.0 * waveScale) + vec2(waveTime * waveSpeed * 0.4, -waveTime * waveSpeed * 0.3));
    float waveY = (bands * 0.6 + warp * 0.4) * 0.018;
    vec2 sceneUv = clamp(texCoord + vec2(0.0, waveY), 0.0, 1.0);
    vec3 scene = texture(Sampler0, sceneUv).rgb;

    float pattern = clamp(0.5 + 0.5 * bands * 0.6 + warp * 0.35, 0.0, 1.0);

    float pulse = 0.82 + 0.18 * sin(waveTime * waveSpeed * 1.6);
    float proximity = smoothstep(0.0, 0.45, 1.0 - texCoord.y);
    vec3 glow = glassColor.rgb * proximity * (0.7 + pattern * 0.9) * glowStrength * pulse;

    float edge = max(max(mR, mL), max(mU, mD)) - maskAlpha;
    edge = smoothstep(0.05, 0.5, edge);
    vec3 outline = glassColor.rgb * 1.6 * edge;

    float op = clamp(glassColor.a, 0.0, 1.0);
    vec3 tinted = mix(scene, glassColor.rgb * 1.2, op * (0.4 + pattern * 0.6));
    vec3 finalColor = clamp(tinted + glow + outline, 0.0, 1.0);
    float alpha = maskAlpha * (0.3 + 0.7 * op);

    fragColor = vec4(finalColor, alpha);
}