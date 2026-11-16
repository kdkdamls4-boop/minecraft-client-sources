#version 330 core

in vec2 uv;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform WetSurfaceUniforms {
    mat4 ViewMatrix;
    mat4 ProjectionMatrix;
    mat4 InverseProjectionMatrix;
    mat4 InverseViewMatrix;
    vec4 SurfaceData0;
    vec4 SurfaceData1;
    vec4 SurfaceData2;
    vec4 SurfaceData3;
};

const int MAX_REFLECTION_STEPS = 16;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.001), vec2(0.999));
}

float luminance(vec3 color) {
    return dot(color, vec3(0.2126, 0.7152, 0.0722));
}

bool reverseZ() {
    return SurfaceData3.x > 0.5;
}

bool isSky(float depth) {
    return reverseZ() ? depth <= 0.000005 : depth >= 0.999995;
}

float clipDepth(float depth) {
    return reverseZ() ? depth : depth * 2.0 - 1.0;
}

float finiteDepth(float depth) {
    return reverseZ() ? max(depth, 0.000001) : min(depth, 0.999999);
}

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float puddleNoise(vec2 p) {
    float value = valueNoise(p) * 0.56;
    value += valueNoise(p * 2.03 + vec2(17.7, 4.3)) * 0.29;
    value += valueNoise(p * 4.11 + vec2(-8.1, 13.2)) * 0.15;
    return value;
}

vec3 reconstructViewPosition(vec2 uv, float depth) {
    vec4 clip = vec4(uv * 2.0 - 1.0, clipDepth(depth), 1.0);
    vec4 view = InverseProjectionMatrix * clip;
    float invW = abs(view.w) > 0.000001 ? 1.0 / view.w : 1.0;
    return view.xyz * invW;
}

vec3 reconstructWorldPosition(vec3 viewPosition) {
    return (InverseViewMatrix * vec4(viewPosition, 1.0)).xyz;
}

vec3 reconstructViewNormal(vec2 uv, vec3 centerPosition) {
    vec2 texel = 1.0 / max(SurfaceData0.xy, vec2(1.0));

    float depthL = texture(DepthSampler, safeUv(uv - vec2(texel.x, 0.0))).r;
    float depthR = texture(DepthSampler, safeUv(uv + vec2(texel.x, 0.0))).r;
    float depthD = texture(DepthSampler, safeUv(uv - vec2(0.0, texel.y))).r;
    float depthU = texture(DepthSampler, safeUv(uv + vec2(0.0, texel.y))).r;

    vec3 posL = reconstructViewPosition(uv - vec2(texel.x, 0.0), finiteDepth(depthL));
    vec3 posR = reconstructViewPosition(uv + vec2(texel.x, 0.0), finiteDepth(depthR));
    vec3 posD = reconstructViewPosition(uv - vec2(0.0, texel.y), finiteDepth(depthD));
    vec3 posU = reconstructViewPosition(uv + vec2(0.0, texel.y), finiteDepth(depthU));

    vec3 rightA = posR - centerPosition;
    vec3 rightB = centerPosition - posL;
    vec3 upA = posU - centerPosition;
    vec3 upB = centerPosition - posD;
    vec3 tangentX = dot(rightA, rightA) < dot(rightB, rightB) ? rightA : rightB;
    vec3 tangentY = dot(upA, upA) < dot(upB, upB) ? upA : upB;

    vec3 normal = cross(tangentX, tangentY);
    float normalLength = length(normal);
    if (normalLength <= 0.000001) {
        return vec3(0.0, 0.0, 1.0);
    }
    normal /= normalLength;
    if (dot(normal, -centerPosition) < 0.0) normal = -normal;
    return normal;
}

float cellRipple(vec2 worldXZ, float scale, float speed, vec2 offset) {
    vec2 grid = worldXZ * scale + offset;
    vec2 cell = floor(grid);
    vec2 local = fract(grid) - 0.5;
    float seed = hash21(cell + offset * 3.7);
    vec2 center = vec2(
        hash21(cell + vec2(3.1, 7.9)),
        hash21(cell + vec2(11.7, -5.3))
    );
    center = (center - 0.5) * 0.54;

    float phase = fract(SurfaceData0.z * speed + seed);
    float radius = phase * 0.72;
    float distanceToDrop = length(local - center);
    float ringDistance = distanceToDrop - radius;
    float envelope = exp(-abs(ringDistance) * 27.0) * (1.0 - phase);
    return sin(ringDistance * 64.0) * envelope * smoothstep(0.02, 0.16, phase);
}

float rippleHeight(vec2 worldXZ) {
    float waves = sin(worldXZ.x * 2.65 + SurfaceData0.z * 1.55)
                 + sin(worldXZ.y * 3.15 - SurfaceData0.z * 1.28)
                 + sin((worldXZ.x + worldXZ.y) * 1.72 + SurfaceData0.z * 0.93);
    waves *= 0.055;

    float rings = cellRipple(worldXZ, 0.44, 0.54, vec2(1.7, 8.2));
    rings += cellRipple(worldXZ, 0.71, 0.67, vec2(-4.3, 2.6)) * 0.58;
    return waves + rings * 0.34;
}

vec3 rippleNormalView(vec2 worldXZ) {
    float epsilon = 0.035;
    float center = rippleHeight(worldXZ);
    float dx = (rippleHeight(worldXZ + vec2(epsilon, 0.0)) - center) / epsilon;
    float dz = (rippleHeight(worldXZ + vec2(0.0, epsilon)) - center) / epsilon;
    float amount = SurfaceData1.w * mix(0.35, 1.0, saturate(SurfaceData2.x));
    vec3 worldNormal = normalize(vec3(-dx * amount, 1.0, -dz * amount));
    return normalize(mat3(ViewMatrix) * worldNormal);
}

vec2 projectDirection(vec3 direction, vec2 vUv, out float valid) {
    vec4 clip = ProjectionMatrix * vec4(direction, 0.0);
    if (clip.w <= 0.00001) {
        valid = 0.0;
        return vUv;
    }
    vec2 projected = clip.xy / clip.w * 0.5 + 0.5;
    vec2 inside = step(vec2(0.002), projected) * step(projected, vec2(0.998));
    valid = inside.x * inside.y;
    return safeUv(projected);
}

vec2 projectPosition(vec3 position, vec2 vUv, out float valid) {
    vec4 clip = ProjectionMatrix * vec4(position, 1.0);
    if (clip.w <= 0.00001) {
        valid = 0.0;
        return vUv;
    }
    vec2 projected = clip.xy / clip.w * 0.5 + 0.5;
    vec2 inside = step(vec2(0.002), projected) * step(projected, vec2(0.998));
    valid = inside.x * inside.y;
    return safeUv(projected);
}

vec2 traceReflection(vec3 origin, vec3 rayDirection, vec3 surfaceNormal, vec2 vUv, out float hitMask) {
    vec3 rayPosition = origin + surfaceNormal * max(0.035, length(origin) * 0.0015);
    float stepLength = max(0.12, length(origin) * 0.014);
    float traveled = 0.0;
    vec2 lastUv = vUv;
    hitMask = 0.0;

    for (int i = 0; i < MAX_REFLECTION_STEPS; i++) {
        if (i >= int(SurfaceData2.y)) break;

        rayPosition += rayDirection * stepLength;
        traveled += stepLength;
        if (traveled > min(30.0, SurfaceData1.z * 0.72) || rayPosition.z > -0.025) break;

        float projected = 0.0;
        vec2 rayUv = projectPosition(rayPosition, vUv, projected);
        if (projected < 0.5) break;
        lastUv = rayUv;

        float sceneDepth = texture(DepthSampler, rayUv).r;
        if (!isSky(sceneDepth)) {
            vec3 scenePosition = reconstructViewPosition(rayUv, sceneDepth);
            float depthDelta = scenePosition.z - rayPosition.z;
            float heightAboveSurface = dot(scenePosition - origin, surfaceNormal);
            float thickness = 0.07 + stepLength * 1.35;

            if (i > 1 && heightAboveSurface > 0.055
                    && depthDelta > 0.0 && depthDelta < thickness) {
                float edge = min(min(rayUv.x, rayUv.y), min(1.0 - rayUv.x, 1.0 - rayUv.y));
                hitMask = smoothstep(0.0, 0.055, edge)
                        * (1.0 - smoothstep(0.0, thickness, depthDelta) * 0.34);
                return rayUv;
            }
        }

        stepLength *= 1.235;
    }

    return lastUv;
}

vec3 blurredReflection(vec2 sampleUv, float blurPixels) {
    vec2 texel = 1.0 / max(SurfaceData0.xy, vec2(1.0));
    vec2 horizontal = vec2(texel.x * blurPixels, 0.0);
    vec2 vertical = vec2(0.0, texel.y * blurPixels * 1.85);

    vec3 color = texture(SceneSampler, safeUv(sampleUv)).rgb * 0.34;
    color += texture(SceneSampler, safeUv(sampleUv + horizontal)).rgb * 0.12;
    color += texture(SceneSampler, safeUv(sampleUv - horizontal)).rgb * 0.12;
    color += texture(SceneSampler, safeUv(sampleUv + vertical)).rgb * 0.16;
    color += texture(SceneSampler, safeUv(sampleUv - vertical)).rgb * 0.16;
    color += texture(SceneSampler, safeUv(sampleUv + vertical * 2.15)).rgb * 0.05;
    color += texture(SceneSampler, safeUv(sampleUv - vertical * 2.15)).rgb * 0.05;
    return color;
}

vec4 saturateColor(vec3 color, float alpha) {
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    return vec4(mix(vec3(gray), color, SurfaceData2.w), alpha);
}

void main() {
    vec4 original = texture(SceneSampler, uv);
    float depth = texture(DepthSampler, uv).r;

    if (isSky(depth)) {
        fragColor = saturateColor(original.rgb, original.a);
        return;
    }

    vec3 viewPosition = reconstructViewPosition(uv, depth);
    float distanceToCamera = length(viewPosition);
    if (distanceToCamera > SurfaceData1.z || distanceToCamera < 0.32) {
        fragColor = saturateColor(original.rgb, original.a);
        return;
    }

    vec3 viewNormal = reconstructViewNormal(uv, viewPosition);
    vec3 worldNormal = normalize(mat3(InverseViewMatrix) * viewNormal);

    float upwardMask = smoothstep(0.58, 0.88, worldNormal.y);
    if (upwardMask <= 0.002) {
        fragColor = saturateColor(original.rgb, original.a);
        return;
    }

    vec3 worldPosition = reconstructWorldPosition(viewPosition);
    float macroNoise = puddleNoise(worldPosition.xz * 0.067);
    float detailNoise = valueNoise(worldPosition.xz * 0.285 + vec2(19.4, -7.8));
    float puddleField = macroNoise * 0.78 + detailNoise * 0.22;
    float threshold = mix(0.73, 0.27, saturate(SurfaceData1.x));
    float puddle = smoothstep(threshold - 0.095, threshold + 0.075, puddleField);

    float rangeFade = 1.0 - smoothstep(SurfaceData1.z * 0.72, SurfaceData1.z, distanceToCamera);
    float nearFade = smoothstep(0.32, 0.82, distanceToCamera);
    float wetFilm = 0.44 + puddle * 0.56;
    float wetMask = saturate(upwardMask * rangeFade * nearFade * wetFilm * SurfaceData0.w);
    if (wetMask <= 0.002) {
        fragColor = saturateColor(original.rgb, original.a);
        return;
    }

    vec3 reflectionNormal = viewNormal;
    float rippleHighlight = 0.0;
    if (int(SurfaceData2.z) == 1) {
        vec3 rippled = rippleNormalView(worldPosition.xz);
        float rippleMix = saturate((0.18 + puddle * 0.52) * SurfaceData1.w);
        reflectionNormal = normalize(mix(viewNormal, rippled, rippleMix));
        rippleHighlight = max(rippleHeight(worldPosition.xz), 0.0);
    }

    vec3 incident = normalize(viewPosition);
    vec3 reflectedDirection = normalize(reflect(incident, reflectionNormal));

    float directionValid = 0.0;
    vec2 directionUv = projectDirection(reflectedDirection, uv, directionValid);

    float rayHit = 0.0;
    vec2 rayUv = traceReflection(viewPosition, reflectedDirection, viewNormal, uv, rayHit);

    vec2 reflectionUv = directionUv;
    float reflectionValid = directionValid * 0.25;
    if (rayHit > 0.001) {
        reflectionUv = rayUv;
        reflectionValid = max(reflectionValid, rayHit * 0.85);
    }

    float border = min(min(reflectionUv.x, reflectionUv.y),
                       min(1.0 - reflectionUv.x, 1.0 - reflectionUv.y));
    reflectionValid *= smoothstep(0.0, 0.035, border);

    float roughness = mix(3.0, 1.2, puddle);
    vec3 reflectedColor = blurredReflection(reflectionUv, roughness);

    vec3 viewToCamera = normalize(-viewPosition);
    float noV = saturate(dot(reflectionNormal, viewToCamera));
    float fresnel = 0.12 + 0.88 * pow(1.0 - noV, 3.0);

    float darkening = saturate(wetMask * (0.28 + puddle * 0.22));
    vec3 wetBase = original.rgb * (1.0 - darkening);

    float reflectivePuddle = smoothstep(0.28, 0.70, puddle);
    float reflectionAmount = wetMask
            * reflectivePuddle
            * (0.20 + fresnel * 0.70)
            * SurfaceData1.y
            * reflectionValid;
    reflectionAmount = min(reflectionAmount, 0.65);

    vec3 color = mix(original.rgb, wetBase, saturate(wetMask * 0.92));
    color = mix(color, reflectedColor, reflectionAmount);

    color += vec3(0.14, 0.18, 0.24) * rippleHighlight * wetMask * 0.12;

    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(vec3(gray), color, SurfaceData2.w);

    fragColor = vec4(clamp(color, 0.0, 1.0), original.a);
}
