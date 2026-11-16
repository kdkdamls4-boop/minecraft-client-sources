 

vec4 envCloud_deep_space(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec3 bandNormal = normalize(vec3(0.30, 0.62, 0.72));
    float band = exp(-pow(dot(direction, bandNormal) * 2.2, 2.0));
    float haze = fbm(direction * 3.0 + vec3(time * 0.01, 0.0, 0.0));
    vec3 cloud = vec3(0.006, 0.008, 0.016)
            + mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5) * band * (0.10 + 0.28 * haze);
    return vec4(cloud, band);
}


vec3 envSky_deep_space(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec4 cloud = envCloud_deep_space(direction);
    float stars = starGlow(direction, 300.0, 0.06, time)
            + starGlow(direction * 1.8 + 5.0, 160.0, 0.03, time * 1.3) * 1.3;
    vec3 color = cloud.rgb + vec3(0.90, 0.93, 1.0) * stars * (0.7 + 0.9 * cloud.a);
    color *= SkyParams.y;
    color = color / (1.0 + color * 0.8);
    color = pow(color, vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    return clamp(color, 0.0, 1.0);
}


vec4 envCloud_nebula(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec3 position = direction * 2.2;
    vec3 warp = vec3(
        fbm(position * 0.7 + vec3(time * 0.02, 0.0, 0.0)),
        fbm(position * 0.7 + vec3(5.2, time * 0.018, 1.3)),
        fbm(position * 0.7 + vec3(1.7, 9.2, -time * 0.015))
    );
    position += (warp - 0.5) * 2.0;
    float firstNoise = fbm(position * 1.1);
    float secondNoise = fbm(position * 2.4 + 4.0);
    float density = pow(
        smoothstep(0.32, 0.95, firstNoise * 0.7 + secondNoise * 0.3),
        1.4
    );
    float hue = fbm(position * 0.6 + 9.0);
    vec3 nebula = mix(
        PrimaryColor.rgb,
        SecondaryColor.rgb,
        smoothstep(0.18, 0.85, hue)
    );
    nebula = mix(nebula, nebula * 1.7 + 0.25, density);
    vec3 cloud = vec3(0.010, 0.013, 0.024);
    cloud += nebula * density * 1.35;
    cloud += mix(PrimaryColor.rgb, SecondaryColor.rgb, 0.5)
            * pow(firstNoise, 2.0) * 0.12;
    return vec4(cloud, 0.0);
}


vec3 envSky_nebula(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec3 color = envCloud_nebula(direction).rgb;
    color += vec3(0.85, 0.90, 1.0)
            * starGlow(direction, 240.0, 0.05, time);
    color += vec3(1.0, 0.96, 0.90)
            * starGlow(direction * 1.7 + 31.0, 130.0, 0.025, time * 1.3)
            * 1.4;
    color *= SkyParams.y;
    color = color / (1.0 + color);
    color = pow(color, vec3(0.85)) + ditherRgb(gl_FragCoord.xy);
    return clamp(color, 0.0, 1.0);
}


vec4 envCloud_plasma(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec3 position = direction * 4.2;
    float firstWarp = fbm(
        position * 0.65 + vec3(time * 0.055, -time * 0.07, time * 0.035)
    );
    float secondWarp = fbm(
        position * 1.3
        + vec3(firstWarp * 2.2)
        + vec3(-time * 0.045, time * 0.06, -time * 0.03)
    );
    float value = fbm(
        position * 1.8
        + vec3(secondWarp * 2.4)
        + vec3(time * 0.025, -time * 0.065, time * 0.04)
    );
    float energy = smoothstep(0.15, 0.90, value);
    float vein = pow(energy, 2.5);
    float hue = fbm(position * 0.85 + vec3(secondWarp + time * 0.012));
    vec3 plasmaColor = mix(
        PrimaryColor.rgb,
        SecondaryColor.rgb,
        smoothstep(0.2, 0.85, hue)
    );
    vec3 cloud = mix(plasmaColor * 0.18, plasmaColor * 1.7, energy);
    cloud += plasmaColor * vein * 0.9;
    return vec4(cloud, 0.0);
}


vec3 envSky_plasma(vec3 direction) {

    float time = SkyParams.x * SkyParams.z;
    vec3 color = envCloud_plasma(direction).rgb;
    color += vec3(0.8, 0.9, 1.0)
            * starGlow(direction, 180.0, 0.025, time * 0.2)
            * 0.35;
    color *= SkyParams.y;
    color = color / (1.0 + color * 0.75);
    color = pow(color, vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    return clamp(color, 0.0, 1.0);
}

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


vec4 envCloud_volumetric(vec3 direction) {
    vec3 rd = direction;
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
    return vec4(color, cloudMask);
}


vec3 envSky_volumetric(vec3 direction) {

    vec4 clouds = envCloud_volumetric(direction);
    vec3 sunDir = normalize(vec3(0.35, 0.74, 0.57));
    float sunCore = pow(max(dot(direction, sunDir), 0.0), 850.0);
    float sunGlow = pow(max(dot(direction, sunDir), 0.0), 18.0);
    vec3 color = clouds.rgb;
    color += vec3(1.0, 0.82, 0.58) * sunGlow * 0.24 * (1.0 - clouds.a * 0.72);
    color += vec3(1.0, 0.95, 0.82) * sunCore * 2.8 * (1.0 - clouds.a);
    color *= 0.72 + SkyParams.y * 0.28;
    color = color / (1.0 + color * 0.42);
    color = pow(max(color, vec3(0.0)), vec3(0.88)) + ditherRgb(gl_FragCoord.xy);
    return clamp(color, 0.0, 1.0);
}

