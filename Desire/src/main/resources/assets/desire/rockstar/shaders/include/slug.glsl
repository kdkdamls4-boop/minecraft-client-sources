
const int SLUG_LOG_BAND_WIDTH = 12;
const int SLUG_TABLE_WIDTH = 1024;

uint slugRootCode(float y1, float y2, float y3) {
    uint shift = uint(y1 < 0.0) | (uint(y2 < 0.0) << 1) | (uint(y3 < 0.0) << 2);
    return (0x2E74u >> shift) & 0x0101u;
}

vec2 slugSolve(vec2 a1, vec2 a2, vec2 a3) {
    vec2 a = a1 - a2 * 2.0 + a3;
    vec2 b = a1 - a2;
    float ra = 1.0 / a.x;
    float rb = 0.5 / b.x;

    float d = sqrt(max(b.x * b.x - a.x * a1.x, 0.0));
    float t1 = (b.x - d) * ra;
    float t2 = (b.x + d) * ra;

    if (abs(a.x) < 1.0 / 65536.0) {
        t1 = a1.x * rb;
        t2 = t1;
    }

    return vec2((a.y * t1 - b.y * 2.0) * t1 + a1.y, (a.y * t2 - b.y * 2.0) * t2 + a1.y);
}

ivec2 slugBandLoc(ivec2 glyphLoc, uint offset) {
    ivec2 loc = ivec2(glyphLoc.x + int(offset), glyphLoc.y);
    loc.y += loc.x >> SLUG_LOG_BAND_WIDTH;
    loc.x &= (1 << SLUG_LOG_BAND_WIDTH) - 1;
    return loc;
}

float slugCoverage(sampler2D curveTex, usampler2D bandTex, sampler2D tableTex,
                   int glyphIndex, vec2 renderCoord, float softness, out vec2 pixelsPerEm) {
    ivec2 record = ivec2((glyphIndex % SLUG_TABLE_WIDTH) * 2, glyphIndex / SLUG_TABLE_WIDTH);
    vec4 bandTransform = texelFetch(tableTex, record, 0);
    ivec4 glyphData = ivec4(texelFetch(tableTex, ivec2(record.x + 1, record.y), 0));

    pixelsPerEm = 1.0 / (fwidth(renderCoord) * max(softness, 1.0));

    ivec2 bandMax = glyphData.zw;
    bandMax.y &= 0x00FF;

    ivec2 bandIndex = clamp(ivec2(renderCoord * bandTransform.xy + bandTransform.zw),
                            ivec2(0, 0), bandMax);
    ivec2 glyphLoc = glyphData.xy;

    float xcov = 0.0;
    float xwgt = 0.0;

    uvec2 hband = texelFetch(bandTex, ivec2(glyphLoc.x + bandIndex.y, glyphLoc.y), 0).xy;
    ivec2 hloc = slugBandLoc(glyphLoc, hband.y);

    for (int i = 0; i < int(hband.x); i++) {
        ivec2 curveLoc = ivec2(texelFetch(bandTex, ivec2(hloc.x + i, hloc.y), 0).xy);
        vec4 p12 = texelFetch(curveTex, curveLoc, 0) - vec4(renderCoord, renderCoord);
        vec2 p3 = texelFetch(curveTex, ivec2(curveLoc.x + 1, curveLoc.y), 0).xy - renderCoord;

        if (max(max(p12.x, p12.z), p3.x) * pixelsPerEm.x < -0.5) break;

        uint code = slugRootCode(p12.y, p12.w, p3.y);
        if (code != 0u) {
            vec2 r = slugSolve(p12.yx, p12.wz, p3.yx) * pixelsPerEm.x;

            if ((code & 1u) != 0u) {
                xcov += clamp(r.x + 0.5, 0.0, 1.0);
                xwgt = max(xwgt, clamp(1.0 - abs(r.x) * 2.0, 0.0, 1.0));
            }
            if (code > 1u) {
                xcov -= clamp(r.y + 0.5, 0.0, 1.0);
                xwgt = max(xwgt, clamp(1.0 - abs(r.y) * 2.0, 0.0, 1.0));
            }
        }
    }

    float ycov = 0.0;
    float ywgt = 0.0;

    uvec2 vband = texelFetch(bandTex, ivec2(glyphLoc.x + bandMax.y + 1 + bandIndex.x, glyphLoc.y), 0).xy;
    ivec2 vloc = slugBandLoc(glyphLoc, vband.y);

    for (int i = 0; i < int(vband.x); i++) {
        ivec2 curveLoc = ivec2(texelFetch(bandTex, ivec2(vloc.x + i, vloc.y), 0).xy);
        vec4 p12 = texelFetch(curveTex, curveLoc, 0) - vec4(renderCoord, renderCoord);
        vec2 p3 = texelFetch(curveTex, ivec2(curveLoc.x + 1, curveLoc.y), 0).xy - renderCoord;

        if (max(max(p12.y, p12.w), p3.y) * pixelsPerEm.y < -0.5) break;

        uint code = slugRootCode(p12.x, p12.z, p3.x);
        if (code != 0u) {
            vec2 r = slugSolve(p12.xy, p12.zw, p3.xy) * pixelsPerEm.y;

            if ((code & 1u) != 0u) {
                ycov -= clamp(r.x + 0.5, 0.0, 1.0);
                ywgt = max(ywgt, clamp(1.0 - abs(r.x) * 2.0, 0.0, 1.0));
            }
            if (code > 1u) {
                ycov += clamp(r.y + 0.5, 0.0, 1.0);
                ywgt = max(ywgt, clamp(1.0 - abs(r.y) * 2.0, 0.0, 1.0));
            }
        }
    }

    float coverage = max(abs(xcov * xwgt + ycov * ywgt) / max(xwgt + ywgt, 1.0 / 65536.0),
                         min(abs(xcov), abs(ycov)));
    return clamp(coverage, 0.0, 1.0);
}

float slugWeight(float coverage, float weight) {
    if (weight <= 0.0) return coverage;
    return mix(coverage, sqrt(coverage), clamp(weight, 0.0, 1.0));
}
