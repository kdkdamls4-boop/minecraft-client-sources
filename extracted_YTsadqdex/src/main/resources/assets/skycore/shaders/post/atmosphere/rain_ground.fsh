#version 330 core

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform RainGroundUniforms {
    mat4 ViewProj;
    mat4 InvViewProj;
    vec4 CamPosTime;
    vec4 SkyTint;
    vec4 SunDir;
    vec4 WetReflect;
    vec4 RippleGloss;
    vec4 March;
    vec4 SplashCtrl;
    vec4 SplashColor;
    vec4 ReversePad;
    vec4 SkyReflection;
    vec4 SkyReflectionReserved;
    vec4 SkyReflectionReserved2;
};

#moj_import <skycore:sky_environment_sample.glsl>

in vec2 uv;
out vec4 finalColor;

#define u_CamPos CamPosTime.xyz
#define u_Clock CamPosTime.w
#define u_SkyRgb SkyTint.xyz
#define u_Sun SunDir.xyz
#define u_ReflectAmt WetReflect.x
#define u_SoakAmt WetReflect.y
#define u_FlatOnly WetReflect.z
#define u_FlatThresh WetReflect.w
#define u_WaveAmt RippleGloss.x
#define u_SpecAmt RippleGloss.y
#define u_Px RippleGloss.zw
#define u_StepCount March.x
#define u_Thickness March.y
#define u_TraceFar March.z
#define u_DoWet March.w
#define u_SplashPhase SplashCtrl.x
#define u_SplashDens SplashCtrl.y
#define u_SplashLife SplashCtrl.z
#define u_SplashRad SplashCtrl.w
#define ReverseZ ReversePad.x

const int TRACE_CAP = 8;
const int BISECT_CAP = 1;

float sat(float x) { return clamp(x, 0.0, 1.0); }

float h2(vec2 p) {
    vec3 t = fract(vec3(p.xyx) * vec3(0.1031, 0.11369, 0.13787));
    t += dot(t, t.yzx + 19.19);
    return fract((t.x + t.y) * t.z);
}

vec2 h22(vec2 p) {
    float a = h2(p);
    return vec2(a, h2(p + vec2(a * 17.13, 9.7)));
}

float vn(vec2 p) {
    vec2 c = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(h2(c), h2(c + vec2(1.0, 0.0)), f.x), mix(h2(c + vec2(0.0, 1.0)), h2(c + vec2(1.0, 1.0)), f.x), f.y);
}

vec2 rippleGrad(vec2 p) {
    float eps = 0.28;
    float base = vn(p);
    return vec2(vn(p + vec2(eps, 0.0)) - base, vn(p + vec2(0.0, eps)) - base) / eps;
}

bool skyZ(float z) {
    return ReverseZ > 0.5 ? (z <= 1.0e-5) : (z >= 0.99999);
}

float clipFromDepth(float z) {
    return ReverseZ > 0.5 ? z : (z * 2.0 - 1.0);
}

vec3 worldFromDepth(vec2 tex, float z) {
    vec4 clip = InvViewProj * vec4(tex * 2.0 - 1.0, clipFromDepth(z), 1.0);
    return clip.xyz / max(abs(clip.w), 1e-6);
}

struct ZRing { float left; float right; float down; float up; float spread;     vec4 SkyReflection;
    vec4 SkyReflectionReserved;
    vec4 SkyReflectionReserved2;
};

ZRing fetchZRing(vec2 tex, float z) {
    ZRing r;
    vec2 px = max(u_Px, vec2(1.0 / 1600.0));
    r.left = texture(DepthSampler, tex + vec2(-px.x, 0.0)).r;
    r.right = texture(DepthSampler, tex + vec2(px.x, 0.0)).r;
    r.down = texture(DepthSampler, tex + vec2(0.0, -px.y)).r;
    r.up = texture(DepthSampler, tex + vec2(0.0, px.y)).r;
    r.spread = max(max(abs(r.left - z), abs(r.right - z)), max(abs(r.up - z), abs(r.down - z)));
    return r;
}

float foliageReject(float z, ZRing r) {
    if (skyZ(z) || r.spread < 0.0005) {
        return 0.0;
    }
    float nearest = min(min(r.left, r.right), min(r.up, r.down));
    bool spike = z < nearest - 0.00002;
    bool band = r.spread > 0.002 && r.spread < 0.06;
    return (spike || band) ? 1.0 : 0.0;
}

vec3 rebuildN(vec2 tex, float z, vec3 viewRel, ZRing ring, out float crack) {
    vec2 px = max(u_Px, vec2(1.0 / 1600.0));
    bool preferL = abs(ring.left - z) < abs(ring.right - z);
    bool preferD = abs(ring.down - z) < abs(ring.up - z);
    vec3 sx = preferL ? (viewRel - worldFromDepth(tex + vec2(-px.x, 0.0), ring.left))
                      : (worldFromDepth(tex + vec2(px.x, 0.0), ring.right) - viewRel);
    vec3 sy = preferD ? (viewRel - worldFromDepth(tex + vec2(0.0, -px.y), ring.down))
                      : (worldFromDepth(tex + vec2(0.0, px.y), ring.up) - viewRel);
    crack = max(length(sx), length(sy)) / max(length(viewRel), 1.0);
    vec3 n = cross(sx, sy);
    float nl = length(n);
    if (nl < 1e-8) {
        return normalize(-viewRel);
    }
    n /= nl;
    return dot(n, -viewRel) < 0.0 ? -n : n;
}

float edgeFade(vec2 tex) {
    vec2 e = smoothstep(vec2(0.0), vec2(0.14), tex) * smoothstep(vec2(0.0), vec2(0.14), 1.0 - tex);
    return e.x * e.y;
}

vec3 skyProbe(vec3 origin, vec3 dir) {
    vec3 d = normalize(dir + vec3(0.0, max(0.0, 0.12 - dir.y) * 0.6, 0.0));
    float reach = mix(400.0, 2200.0, sat(d.y));
    vec4 clip = ViewProj * vec4(origin + d * reach, 1.0);
    if (clip.w > 0.001) {
        vec2 sampleUv = clip.xy / clip.w * 0.5 + 0.5;
        bool inFrame = all(greaterThan(sampleUv, vec2(0.06))) && all(lessThan(sampleUv, vec2(0.94)));
        if (inFrame && skyZ(texture(DepthSampler, sampleUv).r)) {
            return mix(u_SkyRgb, texture(SceneSampler, sampleUv).rgb, edgeFade(sampleUv) * 0.85);
        }
    }
    vec2 fallback = vec2(clamp(0.5 + d.x * 0.1, 0.15, 0.85), mix(0.7, 0.9, sat(d.y * 0.5 + 0.5)));
    if (skyZ(texture(DepthSampler, fallback).r)) {
        return mix(u_SkyRgb, texture(SceneSampler, fallback).rgb, 0.7);
    }
    return u_SkyRgb;
}

bool rejectFloorEcho(vec2 tex, float z, float originY) {
    if (skyZ(z)) {
        return false;
    }
    vec3 rel = worldFromDepth(tex, z);
    float dy = abs((u_CamPos.y + rel.y) - originY);
    if (dy >= 2.8) {
        return false;
    }
    vec2 px = max(u_Px * 2.0, vec2(1.0 / 800.0));
    float zr = texture(DepthSampler, tex + vec2(px.x, 0.0)).r;
    float zu = texture(DepthSampler, tex + vec2(0.0, px.y)).r;
    if (skyZ(zr) || skyZ(zu)) {
        return false;
    }
    vec3 pr = worldFromDepth(tex + vec2(px.x, 0.0), zr) - rel;
    vec3 pu = worldFromDepth(tex + vec2(0.0, px.y), zu) - rel;
    vec3 n = cross(pr, pu);
    float nl = length(n);
    if (nl < 1e-8) {
        return false;
    }
    n /= nl;
    if (dot(n, -rel) < 0.0) {
        n = -n;
    }
    return n.y > 0.62;
}

vec3 wetPass(vec3 src, float depth, vec3 rel, ZRing ring, vec2 texCoord) {
    if (u_DoWet < 0.5) {
        return src;
    }
    float viewLen = length(rel);
    float far = max(u_TraceFar, 8.0);
    float distFade = 1.0 - smoothstep(far * 0.72, far * 1.85, viewLen);
    if (distFade < 0.01) {
        return src;
    }
    float crack;
    vec3 n = rebuildN(texCoord, depth, rel, ring, crack);
    if (crack > 0.55) {
        return src;
    }
    float flatGate = mix(1.0, smoothstep(sat(u_FlatThresh), mix(0.82, 0.95, sat(u_FlatThresh)), n.y), u_FlatOnly) * distFade;
    if (flatGate < 0.002) {
        return src;
    }
    float reflectAmt = sat(u_ReflectAmt) * flatGate;
    if (reflectAmt < 0.01) {
        return src;
    }
    float soak = sat(u_SoakAmt) * distFade;
    vec3 world = u_CamPos + rel;
    float wave = max(u_WaveAmt, 0.045);
    if (n.y > 0.1) {
        vec2 flow = world.xz * 0.85 + vec2(u_Clock * 0.31, u_Clock * 0.19);
        vec2 g = rippleGrad(flow);
        n = normalize(n + vec3(g.x, 0.0, g.y) * (u_WaveAmt > 0.001 ? u_WaveAmt * 0.22 : 0.035));
    }
    vec3 viewDir = normalize(rel);
    float luma = dot(src, vec3(0.2126, 0.7152, 0.0722));
    vec3 soaked = mix(mix(vec3(luma), src, 1.0 + 0.4 * soak) * (1.0 - 0.32 * soak), src, 1.0 - flatGate);
    vec3 bounce = reflect(viewDir, n);
    vec3 tip = rel + n * 0.035;
    vec3 env = SkyReflection.w > 0.5
            ? sampleSkyEnvironment(bounce)
            : skyProbe(tip, bounce);
    float originY = world.y;
    int steps = clamp(int(u_StepCount + 0.5), 4, TRACE_CAP);
    float thick = max(u_Thickness, 0.2);
    float stride = far / float(steps);
    float jitter = fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float t = stride * (0.45 + jitter * 0.55);
    float tPrev = t;
    vec2 hitUv = texCoord;
    float hitW = 0.0;
    float skyHit = 0.0;
    float upBias = smoothstep(-0.02, 0.22, bounce.y);
    for (int i = 0; i < TRACE_CAP; i++) {
        if (i >= steps) break;
        vec3 p = tip + bounce * t;
        vec4 clip = ViewProj * vec4(p, 1.0);
        if (clip.w <= 0.001) break;
        vec2 sampleUv = clip.xy / clip.w * 0.5 + 0.5;
        if (sampleUv.x < 0.02 || sampleUv.x > 0.98 || sampleUv.y < 0.02 || sampleUv.y > 0.98) break;
        float sz = texture(DepthSampler, sampleUv).r;
        if (skyZ(sz)) {
            if (t > stride * 1.25 && bounce.y > -0.02) {
                hitUv = sampleUv;
                hitW = edgeFade(sampleUv) * edgeFade(texCoord) * (1.0 - smoothstep(far * 0.55, far, t)) * upBias;
                skyHit = 1.0;
                break;
            }
        } else if (t > stride * 0.85) {
            float behind = length(p) - length(worldFromDepth(sampleUv, sz));
            if (behind > 0.0 && behind < thick + t * 0.08 && !rejectFloorEcho(sampleUv, sz, originY)) {
                float lo = tPrev;
                float hi = t;
                for (int r = 0; r < BISECT_CAP; r++) {
                    float mid = (lo + hi) * 0.5;
                    vec3 q = tip + bounce * mid;
                    vec4 qc = ViewProj * vec4(q, 1.0);
                    vec2 qu = clamp(qc.xy / max(qc.w, 1e-4) * 0.5 + 0.5, 0.0, 1.0);
                    float qz = texture(DepthSampler, qu).r;
                    float qb = length(q) - length(worldFromDepth(qu, qz));
                    if (qb > 0.0 || skyZ(qz)) {
                        hi = mid;
                        sampleUv = qu;
                    } else {
                        lo = mid;
                    }
                }
                hitUv = sampleUv;
                hitW = edgeFade(sampleUv) * edgeFade(texCoord) * (1.0 - smoothstep(far * 0.6, far, hi)) * mix(0.35, 1.0, upBias);
                break;
            }
        }
        tPrev = t;
        t += stride * (1.0 + float(i) * 0.12);
    }
    vec3 reflection = mix(env, texture(SceneSampler, hitUv).rgb, hitW * mix(0.55, 1.0, sat(wave)) * (SkyReflection.w > 0.5 ? 1.0 - skyHit : 1.0));
    float fres = 0.02 + 0.98 * pow(1.0 - sat(dot(n, -viewDir)), 5.0);
    float amt = sat(reflectAmt * (0.12 + 0.88 * fres)) * flatGate * mix(0.92, 1.0, edgeFade(texCoord));
    vec3 halfV = normalize(normalize(u_Sun) - viewDir);
    float spec = pow(max(dot(n, halfV), 0.0), 190.0) * u_SpecAmt * flatGate * max(u_Sun.y, 0.0);
    return mix(soaked, reflection, amt) + spec;
}

vec3 ringPass(vec3 color, vec3 rel) {
    if (u_SplashDens < 0.02) {
        return color;
    }
    float dist = length(rel);
    float splashFar = max(u_TraceFar, 24.0) * 1.6;
    if (dist > splashFar) {
        return color;
    }
    vec3 n = normalize(cross(dFdx(rel), dFdy(rel)));
    if (dot(n, normalize(-rel)) < 0.0) n = -n;
    float up = smoothstep(0.45, 0.85, n.y);
    if (up < 0.001) {
        return color;
    }
    vec3 world = u_CamPos + rel;
    float cell = 0.95;
    vec2 id = floor(world.xz / cell);
    vec2 hid = mod(id, 4096.0);
    float seed = h2(hid + vec2(9.17, 31.73));
    if (seed > u_SplashDens) {
        return color;
    }
    vec2 center = (id + 0.12 + h22(hid + vec2(17.1, 4.7)) * 0.76) * cell;
    float d = length(world.xz - center);
    float dens = sat(u_SplashDens);
    float life = max(u_SplashLife, 0.01);
    float cycle = max(1.0 / max(mix(0.35, 1.05, dens), 0.01), life * 1.35);
    float local = mod(u_SplashPhase + seed * cycle, cycle);
    if (local > life) {
        return color;
    }
    float age = sat(local / life);
    float rad = max(u_SplashRad, 0.01);
    float aa = max(fwidth(d) * 1.5, 0.004);
    float ringR = rad * sqrt(age);
    float ring = 1.0 - smoothstep(aa, aa * 4.0, abs(d - ringR));
    float blob = (1.0 - smoothstep(0.0, rad * mix(0.18, 0.5, age), d)) * exp(-age * 5.5);
    float core = (1.0 - smoothstep(rad * 0.04, rad * 0.2, d)) * exp(-age * 11.0);
    float shape = (blob * 0.55 + ring * 0.28 + core * 0.18) * pow(1.0 - age, 1.35);
    float fade = 1.0 - smoothstep(splashFar * 0.55, splashFar, dist);
    float a = shape * up * fade * mix(0.55, 0.9, dens) * SplashColor.a;
    if (a < 0.002) {
        return color;
    }
    return color + SplashColor.rgb * a;
}

void main() {
    vec4 scene = texture(SceneSampler, uv);
    float z = texture(DepthSampler, uv).r;
    if (skyZ(z)) {
        finalColor = scene;
        return;
    }
    ZRing ring = fetchZRing(uv, z);
    if (foliageReject(z, ring) > 0.5) {
        finalColor = scene;
        return;
    }
    vec3 viewRel = worldFromDepth(uv, z);
    vec3 lit = wetPass(scene.rgb, z, viewRel, ring, uv);
    lit = ringPass(lit, viewRel);
    finalColor = vec4(clamp(lit, 0.0, 1.0), scene.a);
}
