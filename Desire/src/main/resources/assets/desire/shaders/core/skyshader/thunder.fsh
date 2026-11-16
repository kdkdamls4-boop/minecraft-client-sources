#version 330


#moj_import <desire:sky_common.glsl>

#define ParamsTime uTime
#define ParamsSpeed uSpeed
#define ParamsScale uScale
#define ParamsIntensity uIntensity
#define FogColor uTopLeftColor
#define ThunderInterval uText.x
#define ThunderChance uText.y
#define ThunderGlow uText.z

in vec2 texCoord;
out vec4 fragColor;

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

vec3 fogDirection() {
    vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    return normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV);
}

float hash(vec3 p) {
    return fract(sin(dot(p, vec3(127.1, 311.7, 74.7))) * 43758.5453123);
}

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);

    float n000 = hash(i + vec3(0.0, 0.0, 0.0));
    float n100 = hash(i + vec3(1.0, 0.0, 0.0));
    float n010 = hash(i + vec3(0.0, 1.0, 0.0));
    float n110 = hash(i + vec3(1.0, 1.0, 0.0));
    float n001 = hash(i + vec3(0.0, 0.0, 1.0));
    float n101 = hash(i + vec3(1.0, 0.0, 1.0));
    float n011 = hash(i + vec3(0.0, 1.0, 1.0));
    float n111 = hash(i + vec3(1.0, 1.0, 1.0));

    vec4 a = mix(vec4(n000, n010, n001, n011),
                 vec4(n100, n110, n101, n111), f.x);
    vec2 b = mix(a.xz, a.yw, f.y);
    return mix(b.x, b.y, f.z);
}

float fbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    vec3 shift = vec3(100.0);
    for (int i = 0; i < 4; ++i) {
        v += a * noise(p);
        p = p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

void main() {
    vec3 rayW = fogDirection();

    float cloudDensity = 0.0;
    if (rayW.y > -0.15) {
        vec3 p = rayW * ParamsScale;
        p.x += ParamsTime * ParamsSpeed * 0.08;
        p.z += ParamsTime * ParamsSpeed * 0.04;

        cloudDensity = fbm(p);
        cloudDensity *= smoothstep(-0.15, 0.3, rayW.y);
    }

    float interval = max(1.0, ThunderInterval);
    float timeIndex = floor(ParamsTime / interval);
    float timeOffset = fract(ParamsTime / interval);

    float strikeHash = hash(timeIndex * 12.34);
    bool hasStrike = strikeHash < ThunderChance;

    float flash = 0.0;
    float boltGlow = 0.0;

    if (hasStrike) {
        float strikeAngle = hash(timeIndex * 45.67) * 6.28318;

        vec3 boltStart = normalize(vec3(cos(strikeAngle), 1.0, sin(strikeAngle)));
        vec3 boltEnd = normalize(vec3(cos(strikeAngle + (hash(timeIndex * 8.3) - 0.5) * 0.2), -0.2, sin(strikeAngle + (hash(timeIndex * 8.3) - 0.5) * 0.2)));

        float t = timeOffset;
        if (t < 0.55) {
            float flicker = 0.8 + 0.2 * sin(ParamsTime * 95.0) * cos(ParamsTime * 135.0);

            flash = (exp(-t * 22.0) * 1.5 + exp(-abs(t - 0.15) * 25.0) * 1.0 + exp(-abs(t - 0.35) * 15.0) * 0.4) * flicker;

            float boltIntensity = max(exp(-t * 18.0), exp(-abs(t - 0.15) * 22.0));
            if (boltIntensity > 0.02) {
                vec3 segment = boltEnd - boltStart;
                float segLen = length(segment);
                vec3 segDir = segment / segLen;

                float h = dot(rayW - boltStart, segDir);
                h = clamp(h, 0.0, segLen);
                vec3 projection = boltStart + segDir * h;

                float noiseSeed = timeIndex * 73.19;
                float jagged = sin(h * 30.0 + noiseSeed) * 0.035;
                jagged += cos(h * 70.0 - noiseSeed) * 0.015;
                jagged += sin(h * 150.0 + noiseSeed * 1.5) * 0.007;
                jagged += cos(h * 350.0 - noiseSeed * 3.0) * 0.003;

                vec3 offsetProj = projection + vec3(jagged, 0.0, jagged * 0.6);

                float distToBolt = length(rayW - normalize(offsetProj));

                boltGlow = (exp(-distToBolt * 280.0) * 2.5 + exp(-distToBolt * 30.0) * 0.6) * boltIntensity * flicker;

                if (hash(timeIndex * 29.5) > 0.3 && h > segLen * 0.2) {
                    vec3 bStart = projection;
                    vec3 bEnd = normalize(boltEnd + vec3(sin(strikeAngle + 0.8), -0.15, cos(strikeAngle + 0.8)) * 0.4);
                    vec3 bSeg = bEnd - bStart;
                    float bLen = length(bSeg);
                    if (bLen > 0.0) {
                        vec3 bDir = bSeg / bLen;
                        float bh = dot(rayW - bStart, bDir);
                        bh = clamp(bh, 0.0, bLen);
                        vec3 bProj = bStart + bDir * bh;
                        float bJagged = sin(bh * 45.0 + noiseSeed) * 0.015 + cos(bh * 110.0) * 0.006;
                        vec3 bOffsetProj = bProj + vec3(bJagged, 0.0, bJagged);
                        float distToBranch = length(rayW - normalize(bOffsetProj));

                        boltGlow += (exp(-distToBranch * 220.0) * 1.2 + exp(-distToBranch * 30.0) * 0.3) * boltIntensity * (1.0 - bh / bLen) * flicker;
                    }
                }

                if (hash(timeIndex * 83.2) > 0.5 && h > segLen * 0.4) {
                    vec3 bStart = projection;
                    vec3 bEnd = normalize(boltEnd + vec3(sin(strikeAngle - 0.8), -0.2, cos(strikeAngle - 0.8)) * 0.45);
                    vec3 bSeg = bEnd - bStart;
                    float bLen = length(bSeg);
                    if (bLen > 0.0) {
                        vec3 bDir = bSeg / bLen;
                        float bh = dot(rayW - bStart, bDir);
                        bh = clamp(bh, 0.0, bLen);
                        vec3 bProj = bStart + bDir * bh;
                        float bJagged = sin(bh * 50.0 - noiseSeed) * 0.015 + cos(bh * 120.0) * 0.006;
                        vec3 bOffsetProj = bProj + vec3(bJagged, 0.0, bJagged);
                        float distToBranch = length(rayW - normalize(bOffsetProj));

                        boltGlow += (exp(-distToBranch * 220.0) * 1.2 + exp(-distToBranch * 30.0) * 0.3) * boltIntensity * (1.0 - bh / bLen) * flicker;
                    }
                }
            }
        }
    }

    vec3 stormBase = mix(vec3(0.005, 0.005, 0.015), FogColor.rgb * 0.05, 0.5);

    vec3 flashColor = vec3(0.75, 0.82, 1.0) * flash * ThunderGlow;
    vec3 finalSky = stormBase + flashColor * 0.25;

    vec3 cloudBaseColor = mix(vec3(0.01, 0.01, 0.02), vec3(0.06, 0.07, 0.12), cloudDensity);
    vec3 cloudLitColor = mix(vec3(0.02, 0.025, 0.05), vec3(0.8, 0.85, 1.0), cloudDensity * flash * ThunderGlow * 0.7);

    vec3 finalCloud = mix(cloudBaseColor, cloudLitColor, min(flash * ThunderGlow, 1.0));

    float cloudMix = smoothstep(0.18, 0.48, cloudDensity);
    vec3 color = mix(finalSky, finalCloud, cloudMix);

    color += vec3(0.85, 0.92, 1.0) * boltGlow * ThunderGlow;

    color *= (1.0 + ParamsIntensity * 10.0);

    vec4 skyResult = vec4(clamp(color, 0.0, 1.0), FogColor.a);
    SKY_DEPTH_DISCARD();
    fragColor = skyResult;
}
