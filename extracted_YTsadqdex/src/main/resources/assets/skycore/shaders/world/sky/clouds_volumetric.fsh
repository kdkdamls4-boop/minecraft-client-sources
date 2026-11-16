#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;

#moj_import <skycore:block_outline_common.glsl>
#moj_import <skycore:world_sky_common.glsl>

float cloudNoise(vec3 p) {
    float n = 0.0;
    float a = 0.55;
    for (int i = 0; i < 4; i++) {
        n += valueNoise(p) * a;
        p = p * 2.03 + vec3(13.1, 7.7, 19.3);
        a *= 0.48;
    }
    return n;
}

float cloudDensity(vec3 p, float time) {
    float height = smoothstep(1.05, 1.55, p.y) * (1.0 - smoothstep(3.65, 4.35, p.y));
     
    if (height == 0.0) return 0.0;
    vec3 drift = vec3(time * 0.055, 0.0, time * 0.018);
    float base = cloudNoise(p * vec3(0.42, 0.72, 0.42) + drift);
    float detail = valueNoise(p * 2.7 - drift * 1.7);
    float shape = base * 0.86 + detail * 0.14;
    return max(0.0, (shape - 0.48) * 2.15) * height;
}

void main() {
    if (!skyNearby(DepthSampler, uv)) {
        finalColor = vec4(0.0);
        return;
    }
    vec3 rd = skyDirection(uv);
    float time = SkyParams.x * SkyParams.z * 0.16;
    float horizon = clamp(rd.y * 0.5 + 0.5, 0.0, 1.0);
    vec3 zenith = mix(PrimaryColor.rgb * 0.24, SecondaryColor.rgb * 0.42 + vec3(0.10, 0.18, 0.32), 0.55);
    vec3 horizonColor = mix(vec3(0.62, 0.72, 0.88), SecondaryColor.rgb * 0.48 + vec3(0.28), 0.35);
    vec3 atmosphere = mix(horizonColor, zenith, pow(horizon, 0.55));
    vec3 sunDir = normalize(vec3(0.35, 0.74, 0.57));
    float sunAmount = pow(max(dot(rd, sunDir), 0.0), 128.0);
    atmosphere += vec3(1.0, 0.76, 0.48) * sunAmount * 2.2;

    vec3 origin = vec3(0.0, 1.15, 0.0);
    float startT = 0.2;
    float endT = rd.y > -0.08 ? 12.0 : 5.0;
    float stepSize = (endT - startT) / 24.0;
    float transmittance = 1.0;
    vec3 scattered = vec3(0.0);
    float t = startT + hash21(gl_FragCoord.xy) * stepSize;
    float phase = 0.52 + 0.48 * pow(max(dot(rd, sunDir), 0.0), 4.0);

    for (int i = 0; i < 24; i++) {
        vec3 p = origin + rd * t;
         
        if ((rd.y >= 0.0 && p.y >= 4.35) || (rd.y <= 0.0 && p.y <= 1.05)) break;
        float density = cloudDensity(p, time);
        if (density > 0.002) {
            float lightDensity = cloudDensity(p + sunDir * 0.32, time);
            float light = 0.34 + 0.66 * exp(-lightDensity * 2.4);
            vec3 cloudColor = mix(vec3(0.42, 0.46, 0.52), vec3(1.0, 0.96, 0.90), light);
            cloudColor *= 0.72 + phase * 0.62;
            float alpha = 1.0 - exp(-density * stepSize * 1.85);
            scattered += transmittance * alpha * cloudColor;
            transmittance *= 1.0 - alpha;
            if (transmittance < 0.018) {
                break;
            }
        }
        t += stepSize;
    }

    vec3 color = atmosphere * transmittance + scattered;
    float cloudMask = 1.0 - transmittance;
    color += mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5) * cloudMask * 0.07;
    finalColor = vec4(color, cloudMask);
}
