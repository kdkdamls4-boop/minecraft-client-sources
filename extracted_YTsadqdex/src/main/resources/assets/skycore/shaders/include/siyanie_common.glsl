 

float siyanieFbm4(vec3 p) {
    float sum = 0.0;
    float amplitude = 0.5;
    float frequency = 1.0;
    for (int i = 0; i < 4; i++) {
        sum += amplitude * valueNoise(p * frequency);
        frequency *= 2.03;
        amplitude *= 0.5;
    }
    return sum;
}

float siyanieLayer(vec3 direction, float time, float scale, float drift, float phase) {
    vec3 p = direction * scale + vec3(time * drift, time * 0.035, phase);
    float wave = sin(dot(direction.xz, vec2(0.86, 0.51)) * 9.0 + p.y * 6.0 + sin(p.y * 5.0 + phase) * 1.7);
    float noise = siyanieFbm4(p * 2.35 + vec3(phase, time * 0.02, 0.0));
    float body = smoothstep(-0.12, 0.58, wave * 0.5 + 0.5);
    body *= smoothstep(0.26, 0.94, noise);
    float rays = pow(max(0.0, sin(dot(direction, normalize(vec3(0.35, 0.82, 0.45))) * 14.0 + noise * 5.0 + time * 0.38)), 2.8);
    rays *= smoothstep(-0.05, 0.82, direction.y);
    return body * 0.8 + rays * 0.62;
}

vec4 siyanieField(vec3 direction, float time) {
    float dome = smoothstep(-0.28, 0.04, direction.y);
    float zenith = pow(smoothstep(0.0, 1.0, direction.y), 0.5);

    float l1 = siyanieLayer(direction, time, 2.4, 0.11, 0.0);
    float l2 = siyanieLayer(direction, time, 3.1, -0.08, 2.4);
    float l3 = siyanieLayer(direction, time, 3.9, 0.06, 4.8);
    float strength = (l1 * 1.05 + l2 * 0.9 + l3 * 0.72) * dome;
    strength = min(strength * (0.65 + 1.05 * zenith), 3.2);

    float detail = siyanieFbm4(direction * 6.0 + vec3(time * 0.028, time * 0.022, 0.0));
    strength += pow(detail, 2.0) * dome * 0.42;

    vec3 glow = mix(PrimaryColor.rgb, SecondaryColor.rgb, smoothstep(0.08, 0.95, detail));
    vec3 deep = mix(SecondaryColor.rgb * 0.14, PrimaryColor.rgb * 0.2, 0.42);
    vec3 rgb = mix(deep, glow * 2.25, clamp(strength, 0.0, 1.0));
    rgb += glow * pow(strength, 1.85) * 1.55;
    rgb += mix(PrimaryColor.rgb, vec3(1.0), 0.3) * pow(strength, 3.2) * 0.85;

    float alpha = clamp(strength * dome * 1.25 + zenith * 0.12, 0.0, 1.0);
    return vec4(rgb * alpha, alpha);
}
