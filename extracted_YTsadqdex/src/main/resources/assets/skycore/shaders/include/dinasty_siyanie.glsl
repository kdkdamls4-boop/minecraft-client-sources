 
float siyanieHash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float siyanieNoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    float n000 = siyanieHash13(i);
    float n100 = siyanieHash13(i + vec3(1.0, 0.0, 0.0));
    float n010 = siyanieHash13(i + vec3(0.0, 1.0, 0.0));
    float n110 = siyanieHash13(i + vec3(1.0, 1.0, 0.0));
    float n001 = siyanieHash13(i + vec3(0.0, 0.0, 1.0));
    float n101 = siyanieHash13(i + vec3(1.0, 0.0, 1.0));
    float n011 = siyanieHash13(i + vec3(0.0, 1.0, 1.0));
    float n111 = siyanieHash13(i + vec3(1.0, 1.0, 1.0));
    float c00 = mix(n000, n100, f.x);
    float c10 = mix(n010, n110, f.x);
    float c01 = mix(n001, n101, f.x);
    float c11 = mix(n011, n111, f.x);
    float c0 = mix(c00, c10, f.y);
    float c1 = mix(c01, c11, f.y);
    return mix(c0, c1, f.z);
}

float siyanieFbm(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * siyanieNoise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

vec3 siyanieBackground(in vec3 rd) {
    float sd = dot(normalize(vec3(-0.5, -0.6, 0.9)), rd) * 0.5 + 0.5;
    sd = pow(sd, 5.0);
    vec3 col = mix(vec3(0.05, 0.1, 0.2), vec3(0.1, 0.05, 0.2), sd);
    return col * 0.63;
}

vec3 siyanieSky(vec3 dir, vec3 colorA, vec3 colorB, float time, float intensity, float softness, float coverage) {
     
     
    float v = dir.y;
    vec3 col = siyanieBackground(dir);

    {
        vec2 hd = normalize(dir.xz + vec2(1e-4, 0.0));
        vec2 ring = hd * 2.5;
        float t = time * 0.15;

        float warp = siyanieFbm(vec3(ring, t));
        float rayN = siyanieFbm(vec3(ring * 1.4 + vec2(warp * 1.6, v * 1.4), t * 0.6));
        float rays = clamp(1.0 - abs(2.0 * rayN - 1.0), 0.0, 1.0);
        rays = pow(rays, mix(4.0, 1.2, softness));

        float top = mix(0.55, 1.15, coverage);
        float env = 1.0 - smoothstep(top * 0.5, top, v);
        float m = rays * env;

        float h = clamp(v / top, 0.0, 1.0);
        vec3 aur = mix(colorA, colorB, h * h);

        float glow = 1.0 - smoothstep(0.08, 0.45, v);

        col += aur * m * intensity;
        col += colorA * glow * 0.12 * intensity;
    }

    return col;
}
