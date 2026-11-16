#version 330


#moj_import <desire:sky_common.glsl>

#define uColorA uTopLeftColor
#define uColorB vec4(uColor2, uTopLeftColor.a)

in vec2 texCoord;
out vec4 fragColor;

mat2 dMm2(float a) {
    float c = cos(a), s = sin(a);
    return mat2(c, s, -s, c);
}

float dTri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 dTri2(vec2 p) {
    return vec2(dTri(p.x) + dTri(p.y), dTri(p.y + dTri(p.x)));
}

float dAuroraNoise(vec2 p, float time) {
    float z = 1.8;
    float z2 = 2.5;
    float rz = 0.0;
    mat2 m = dMm2(p.x * 0.06);
    p *= m;
    vec2 bp = p;

    for (int i = 0; i < 3; i++) {
        vec2 dg = dTri2(bp * 1.85) * 0.75;
        dg *= dMm2(time * 0.06);
        p -= dg / z2;
        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;
        p *= 1.21 + (rz - 1.0) * 0.02;
        rz += dTri(p.x + dTri(p.y)) * z;
        p *= mat2(-0.95534, 0.29552, -0.29552, -0.95534);
    }
    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

float dHash21(vec2 n) {
    return fract(sin(dot(n, vec2(12.9898, 4.1414))) * 43758.5453);
}

mat3 rotX(float a) {
    float c = cos(a), s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a), s = sin(a);
    return mat3(c, 0.0, s, 0.0, 1.0, 0.0, -s, 0.0, c);
}

vec3 divineRayDir() {
    vec2 uv = gl_FragCoord.xy / max(uResolution, vec2(1.0));
    vec2 sp = uv * 2.0 - 1.0;
    float aspect = uResolution.x / max(uResolution.y, 1.0);
    float tanV = tan(radians(uFov) * 0.5);
    vec3 rayV = normalize(vec3(sp.x * tanV * aspect, sp.y * tanV, 1.0));
    return normalize(rotY(uCameraDir.x) * rotX(uCameraDir.y) * rayV);
}

vec3 dAuroraColor(vec3 ro, vec3 rd, float time, float scale, vec3 colorA, vec3 colorB) {
    vec4 col = vec4(0.0);
    vec4 avgCol = vec4(0.0);

    for (float i = 0.0; i < 15.0; i++) {
        float of = 0.006 * dHash21(gl_FragCoord.xy) * smoothstep(0.0, 8.0, i);
        float pt = ((0.8 + pow(i, 1.4) * 0.002) - ro.y) / (rd.y * 2.0 + 0.4);
        pt -= of;
        vec3 bpos = ro + pt * rd;
        vec2 p = bpos.zx * scale * 0.2;
        float rzt = dAuroraNoise(p, time);

        vec4 col2 = vec4(0.0, 0.0, 0.0, rzt);

        float t = sin(i * 0.043 + time) * 0.5 + 0.5;
        col2.rgb = mix(colorA, colorB, t) * rzt;

        avgCol = mix(avgCol, col2, 0.5);
        col += avgCol * exp2(-i * 0.09 - 2.2) * smoothstep(0.0, 3.0, i);
    }

    col *= clamp(rd.y * 15.0 + 0.4, 0.0, 1.0);
    return col.rgb * col.a * 2.2;
}

vec3 dStars(vec3 p) {
    vec3 c = vec3(0.0);
    float res = uResolution.x * 0.15;

    for (int i = 0; i < 3; i++) {
        vec3 q = fract(p * res) - 0.5;
        vec3 id = floor(p * res);
        float rn = fract(sin(dot(id, vec3(12.9898, 78.233, 56.786))) * 43758.5453);
        float c2 = 1.0 - smoothstep(0.0, 0.6, length(q));
        c2 *= step(rn, 0.0005 + i * i * 0.001);
        c += c2 * vec3(0.9, 0.95, 1.0);
        p *= 1.4;
    }
    return c * 0.5;
}

void main() {
    float animatedTime = uTime * uSpeed;

    vec3 rd = normalize(divineRayDir());
    vec3 ro = vec3(0.0, 0.0, -3.0);

    vec3 col = vec3(0.0);

    float skyGradient = dot(normalize(vec3(-0.5, -0.6, 0.9)), rd) * 0.5 + 0.5;
    skyGradient = pow(skyGradient, 5.0);
    col = mix(vec3(0.02, 0.03, 0.06), vec3(0.05, 0.02, 0.08), skyGradient) * 0.7;

    if (rd.y > 0.0) {
        vec3 aur = dAuroraColor(ro, rd, animatedTime, uScale, uColorA.rgb, uColorB.rgb);
        vec3 starField = dStars(rd) * 0.3;

        col = mix(col, aur + starField, clamp(rd.y * 2.0, 0.0, 1.0));
    } else {
        rd.y = abs(rd.y);
        col *= 0.6;

        vec3 aur = dAuroraColor(ro, rd, animatedTime, uScale * 0.8, uColorA.rgb * 0.7, uColorB.rgb * 0.7);
        vec3 starField = dStars(rd) * 0.1;

        col = mix(col, aur + starField, clamp(rd.y * 1.5, 0.0, 1.0));
    }

    vec4 skyResult = vec4(col, uColorA.a);
    SKY_DEPTH_DISCARD();
    fragColor = skyResult;
}
