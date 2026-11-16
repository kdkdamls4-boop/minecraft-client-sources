#version 330 core

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D BlurSampler;

layout(std140) uniform AtmosphereUniforms {
    mat4 InvViewProj;
    mat4 ViewProj;
    vec4 FogColor;
    vec4 SunDirZ;
    vec4 CameraPosTime;
    vec4 TexelPad;
    vec4 AtmosParams;
    vec4 FogRange;
    vec4 Extra;
    vec4 ClipRange;
    vec4 WetParams;
    vec4 WetMarch;
    vec4 SkyReflection;
    vec4 SkyReflectionReserved;
    vec4 SkyReflectionReserved2;
};

#moj_import <skycore:sky_environment_sample.glsl>

in vec2 uv;
out vec4 finalColor;

const int MAX_STEPS = 16;
const int REFINE_STEPS = 2;
const float STEP_GROWTH = 0.12;
const float SURFACE_BIAS = 0.035;
const float EDGE_FADE = 0.12;
const float SILHOUETTE_LIMIT = 0.55;
const float SPEC_POWER = 190.0;

#define ReverseZ SunDirZ.w
#define Texel TexelPad.xy
#define CameraPos CameraPosTime.xyz

bool depthIsSky(float z) {
    return ReverseZ > 0.5 ? (z <= 1.0e-5) : (z >= 0.99999);
}

float clipFromDepth(float z) {
    return ReverseZ > 0.5 ? z : (z * 2.0 - 1.0);
}

float thinOverlayMask(vec2 tex, float z) {
    if (depthIsSky(z)) {
        return 0.0;
    }
    bool revZ = ReverseZ > 0.5;
    float dL = texture(DepthSampler, tex + vec2(-Texel.x, 0.0)).r;
    float dR = texture(DepthSampler, tex + vec2(Texel.x, 0.0)).r;
    float dU = texture(DepthSampler, tex + vec2(0.0, Texel.y)).r;
    float dD = texture(DepthSampler, tex + vec2(0.0, -Texel.y)).r;
    float spread = max(max(abs(dL - z), abs(dR - z)), max(abs(dU - z), abs(dD - z)));
    if (spread < 0.0005) {
        return 0.0;
    }
    float minN = min(min(dL, dR), min(dU, dD));
    float maxN = max(max(dL, dR), max(dU, dD));
    bool isolated = revZ ? (z < minN - 0.00002) : (z > maxN + 0.00002);
    bool crossPlant = spread > 0.002 && spread < 0.06;
    return (isolated || crossPlant) ? 1.0 : 0.0;
}

vec3 camRelFromDepth(vec2 tex, float depth) {
    vec4 clip = vec4(tex * 2.0 - 1.0, clipFromDepth(depth), 1.0);
    vec4 pos = InvViewProj * clip;
    return pos.xyz / max(abs(pos.w), 1e-6);
}

float computeFogFactor(float dist, float skyBit) {
    float dens = AtmosParams.x;
    if (dens < 0.01) {
        return 0.0;
    }
    float farLimit = max(ClipRange.y, 48.0);
    float fogFar = min(max(FogRange.y, FogRange.x + 8.0), farLimit * 0.78);
    float fogNear = min(max(FogRange.x, 1.0), fogFar * 0.4);
    float softPow = max(AtmosParams.y, 0.25);
    float ramp = pow(clamp(smoothstep(fogNear, fogFar, dist), 0.0, 1.0), softPow * 1.05);
    ramp *= mix(0.55, 1.0, 1.0 - smoothstep(fogFar * 0.82, farLimit * 0.98, dist));
    if (skyBit > 0.5) {
        ramp *= mix(0.2, 0.75, uv.y * 0.9 + 0.1);
    } else {
        ramp *= mix(1.0, clamp(Extra.y / 80.0, 0.35, 1.0), 0.15);
    }
    ramp *= mix(1.0, 1.08, FogRange.z);
    return clamp(1.0 - exp(-dens * ramp * 0.95), 0.0, 0.62);
}

float surfaceWetness(vec3 albedo, float upAlign) {
    float luma = dot(albedo, vec3(0.299, 0.587, 0.114));
    float blue = albedo.b - max(albedo.r, albedo.g);
    float flatness = smoothstep(0.78, 0.96, upAlign);
    float waterHue = smoothstep(0.05, 0.2, blue) * smoothstep(0.1, 0.38, albedo.b);
    float notGreen = 1.0 - smoothstep(0.0, 0.14, albedo.g - albedo.r);
    float notBlack = smoothstep(0.03, 0.12, luma);
    return clamp(flatness * waterHue * notGreen * notBlack, 0.0, 1.0);
}

vec3 faceNormal(vec2 tex, float depth, vec3 camRel, out float discontinuity) {
    vec2 uvL = tex - vec2(Texel.x, 0.0);
    vec2 uvR = tex + vec2(Texel.x, 0.0);
    vec2 uvD = tex - vec2(0.0, Texel.y);
    vec2 uvU = tex + vec2(0.0, Texel.y);
    float dL = texture(DepthSampler, uvL).r;
    float dR = texture(DepthSampler, uvR).r;
    float dD = texture(DepthSampler, uvD).r;
    float dU = texture(DepthSampler, uvU).r;
    bool leftCloser = abs(dL - depth) < abs(dR - depth);
    bool downCloser = abs(dD - depth) < abs(dU - depth);
    vec3 ddx = leftCloser ? camRel - camRelFromDepth(uvL, dL) : camRelFromDepth(uvR, dR) - camRel;
    vec3 ddy = downCloser ? camRel - camRelFromDepth(uvD, dD) : camRelFromDepth(uvU, dU) - camRel;
    discontinuity = max(length(ddx), length(ddy)) / max(length(camRel), 1.0);
    vec3 n = cross(ddx, ddy);
    float len = length(n);
    if (len < 1e-8) {
        return normalize(-camRel);
    }
    n /= len;
    return dot(n, -camRel) < 0.0 ? -n : n;
}

float hash12(vec2 p) {
    vec3 q = fract(vec3(p.xyx) * vec3(0.2713, 0.1937, 0.2411));
    q += dot(q, q.yzx + 19.19);
    return fract((q.x + q.y) * q.z * 61.0);
}

float rippleNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), f.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), f.x), f.y);
}

vec2 rippleGradient(vec2 p) {
    const float e = 0.25;
    float base = rippleNoise(p);
    return vec2(rippleNoise(p + vec2(e, 0.0)) - base, rippleNoise(p + vec2(0.0, e)) - base) / e;
}

float edgeFade(vec2 tex) {
    vec2 fade = smoothstep(vec2(0.0), vec2(EDGE_FADE), tex) * smoothstep(vec2(0.0), vec2(EDGE_FADE), 1.0 - tex);
    return fade.x * fade.y;
}

vec3 applyWetWorld(vec2 tex, float depth, vec3 scene, float skyBit) {
    float blockAmt = Extra.x;
    float waterAmt = Extra.z;
    if (skyBit > 0.5 || (blockAmt < 0.01 && waterAmt < 0.01)) {
        return scene;
    }
    if (thinOverlayMask(tex, depth) > 0.5 || depthIsSky(depth)) {
        return scene;
    }
    vec3 camRel = camRelFromDepth(tex, depth);
    float discontinuity;
    vec3 normal = faceNormal(tex, depth, camRel, discontinuity);
    if (discontinuity > SILHOUETTE_LIMIT) {
        return scene;
    }
    float minUp = clamp(Extra.w, 0.2, 0.98);
    float upMask = mix(1.0, smoothstep(minUp, mix(0.82, 0.95, minUp), normal.y), WetParams.w);
    if (upMask <= 0.002) {
        return scene;
    }
    float waterMix = surfaceWetness(scene, clamp(normal.y, 0.0, 1.0));
    float reflectivity = mix(blockAmt, waterAmt, waterMix) * upMask;
    if (reflectivity < 0.01) {
        return scene;
    }
    float wetness = WetParams.x * mix(0.85, 1.15, waterMix);
    float ripple = WetParams.y * mix(0.7, 1.2, waterMix);
    float gloss = WetParams.z * mix(0.85, 1.25, waterMix);
    vec3 world = CameraPos + camRel;
    if (ripple > 0.001 && normal.y > 0.1) {
        vec2 flow = world.xz * 0.85 + vec2(WetMarch.w * 0.31, WetMarch.w * 0.19);
        vec2 gradient = rippleGradient(flow);
        normal = normalize(normal + vec3(gradient.x, 0.0, gradient.y) * ripple * 0.22);
    }
    vec3 incident = normalize(camRel);
    float lum = dot(scene, vec3(0.2126, 0.7152, 0.0722));
    vec3 soaked = mix(vec3(lum), scene, 1.0 + 0.4 * wetness) * (1.0 - 0.32 * wetness);
    soaked = mix(scene, soaked, upMask);
    vec3 reflected = reflect(incident, normal);
    vec3 origin = camRel + normal * SURFACE_BIAS;
    int steps = clamp(int(WetMarch.x), 4, MAX_STEPS);
    float maxDistance = max(WetMarch.z, 8.0);
    float hitThickness = max(WetMarch.y, 0.2);
    float stepLength = maxDistance / float(steps);
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float travelled = stepLength * (0.35 + jitter * 0.65);
    float previous = travelled;
    vec2 hitUv = tex;
    float hit = 0.0;
    for (int i = 0; i < MAX_STEPS; i++) {
        if (i >= steps) {
            break;
        }
        vec3 marchPos = origin + reflected * travelled;
        vec4 clip = ViewProj * vec4(marchPos, 1.0);
        if (clip.w <= 0.001) {
            break;
        }
        vec2 sampleUv = (clip.xy / clip.w) * 0.5 + 0.5;
        if (sampleUv.x < 0.0 || sampleUv.x > 1.0 || sampleUv.y < 0.0 || sampleUv.y > 1.0) {
            break;
        }
        float sceneDepth = texture(DepthSampler, sampleUv).r;
        if (!depthIsSky(sceneDepth)) {
            float behind = length(marchPos) - length(camRelFromDepth(sampleUv, sceneDepth));
            if (behind > 0.0 && behind < hitThickness + travelled * 0.08) {
                float lo = previous;
                float hi = travelled;
                for (int r = 0; r < REFINE_STEPS; r++) {
                    float mid = (lo + hi) * 0.5;
                    vec3 probe = origin + reflected * mid;
                    vec4 probeClip = ViewProj * vec4(probe, 1.0);
                    vec2 probeUv = clamp((probeClip.xy / max(probeClip.w, 1e-4)) * 0.5 + 0.5, 0.0, 1.0);
                    float probeDepth = texture(DepthSampler, probeUv).r;
                    float probeBehind = length(probe) - length(camRelFromDepth(probeUv, probeDepth));
                    if (probeBehind > 0.0) {
                        hi = mid;
                        sampleUv = probeUv;
                    } else {
                        lo = mid;
                    }
                }
                hitUv = sampleUv;
                hit = edgeFade(sampleUv) * (1.0 - smoothstep(maxDistance * 0.6, maxDistance, hi));
                break;
            }
        }
        previous = travelled;
        travelled += stepLength * (1.0 + float(i) * STEP_GROWTH);
    }
    vec3 skyTint = mix(FogColor.rgb * 0.85, vec3(0.35, 0.48, 0.72), 0.35);
    if (SkyReflection.w > 0.5) skyTint = sampleSkyEnvironment(reflected);
    vec3 reflection = mix(skyTint, texture(SceneSampler, hitUv).rgb, hit);
    reflection = mix(reflection, texture(BlurSampler, hitUv).rgb, 0.25 * hit);
    float fresnel = 0.02 + 0.98 * pow(1.0 - clamp(dot(normal, -incident), 0.0, 1.0), 5.0);
    float amount = clamp(reflectivity * (0.12 + 0.88 * fresnel), 0.0, 1.0) * upMask;
    vec3 sunDir = normalize(SunDirZ.xyz);
    vec3 halfway = normalize(sunDir - incident);
    float specular = pow(max(dot(normal, halfway), 0.0), SPEC_POWER) * gloss * upMask * max(sunDir.y, 0.0);
    return clamp(mix(soaked, reflection, amount) + specular, 0.0, 8.0);
}

void main() {
    vec3 scene = max(texture(SceneSampler, uv).rgb, vec3(0.0));
    float z = texture(DepthSampler, uv).r;
    float skyBit = depthIsSky(z) ? 1.0 : 0.0;
    vec3 camRel = camRelFromDepth(uv, z);
    float dist = skyBit > 0.5 ? max(ClipRange.y, 64.0) * 0.92 : length(camRel);
    if (!(dist > 0.0) || dist > 1.0e6) {
        finalColor = vec4(scene, 1.0);
        return;
    }
    scene = applyWetWorld(uv, z, scene, skyBit);
    float overlay = thinOverlayMask(uv, z);
    float fogAmt = computeFogFactor(dist, skyBit) * (1.0 - overlay * 0.85);
    vec3 mist = FogColor.rgb;
    float dayCurve = clamp(SunDirZ.y * 0.5 + 0.5, 0.0, 1.0);
    mist *= mix(vec3(1.1, 0.95, 0.82), vec3(0.85, 0.9, 1.05), dayCurve);
    if (FogRange.z > 0.01) {
        vec3 duskTint = mix(vec3(0.06, 0.09, 0.16), FogColor.rgb * 0.55, 0.35);
        mist = mix(mist, duskTint, FogRange.z * 0.85);
    }
    vec3 color = mix(scene, mist, fogAmt);
    float blurAmt = FogRange.w;
    if (blurAmt > 0.01) {
        float blurT = smoothstep(FogRange.x * 0.75, FogRange.y, dist) * blurAmt;
        if (skyBit > 0.5) {
            blurT *= 0.45;
        }
        color = mix(color, texture(BlurSampler, uv).rgb, clamp(blurT * 0.7, 0.0, 0.65));
    }
    finalColor = vec4(clamp(color, 0.0, 8.0), 1.0);
}
