#version 330 core
uniform sampler2D SceneSampler;
layout(std140) uniform JumpCircleUniforms {
    vec4 Params;   // progress, max distortion in pixels, per-jump seed, time
    vec4 Accent;   // theme RGB, lifetime opacity
    vec4 Screen;   // framebuffer width, height, inverse width, inverse height
};
in vec2 local;
out vec4 finalColor;

// The unevenness is deliberately angular and hand-drawn, not a clean
// sinusoidal ring or a geometric line mesh. Integer angular frequencies keep
// the contour continuous all the way around the origin (including the seam).
float scribbleRadius(float a, float seed, float time) {
    float wobble = sin(a * 5.0 + seed * 1.13) * 0.047;
    wobble += sin(a * 9.0 - seed * 0.91) * 0.019;
    wobble += sin(a * 17.0 + seed * 2.03) * 0.017;
    wobble += sin(a * 29.0 - seed * 1.31) * 0.009;
    wobble += sin(a * 41.0 + seed * 0.49) * 0.005;
    // The outline is mostly fixed to the ground; only a small living flutter.
    wobble += sin(a * 7.0 + time * 2.8 + seed) * 0.006;
    return 0.818 + wobble;
}

void main() {
    float opacity = clamp(Accent.a, 0.0, 1.0);
    if (opacity < 0.002) discard;
    // Grow the complete contour from the centre without shrinking or moving
    // the terrain-conforming world mesh. The entire outline is present from
    // the very first visible frame, not painted one angular sector at a time.
    float growthT = clamp(Params.x / 0.57, 0.0, 1.0);
    float growth = 0.10 + 0.90 * (1.0 - pow(1.0 - growthT, 3.0));
    vec2 pencil = local / growth;
    float r = max(length(pencil), 0.0001);
    float angle = atan(pencil.y, pencil.x);
    float contour = scribbleRadius(angle, Params.z, Params.w);
    float strokeWidth = 0.020 + sin(angle * 13.0 + Params.z) * 0.005
                      + sin(angle * 31.0 - Params.z * 2.0) * 0.004;
    float aa = max(fwidth(r) * 1.1, 0.005);

    // Main crayon stroke, imperfect secondary pencil pass and a soft tint halo.
    float mainStroke = 1.0 - smoothstep(strokeWidth - aa, strokeWidth + aa,
                                         abs(r - contour));
    float secondContour = contour + 0.035
            + 0.010 * sin(angle * 19.0 + Params.z * 0.7);
    float secondary = (1.0 - smoothstep(0.010, 0.010 + aa * 2.0,
                                       abs(r - secondContour)))
            * (0.34 + 0.23 * sin(angle * 8.0 + Params.z));
    float halo = exp(-pow(abs(r - contour) * 25.0, 2.0)) * 0.23;
    float ink = clamp(mainStroke * 0.90 + secondary + halo, 0.0, 1.0);

    // Real refraction: sample the earlier scene snapshot at perturbed screen
    // coordinates. Keep the mask INSIDE the scribbled contour and taper it at
    // the center and the perimeter so nothing is displaced outside the ring.
    float interior = 1.0 - smoothstep(contour - 0.075, contour - 0.020, r);
    float warpMask = interior * smoothstep(0.03, 0.22, r)
            * (1.0 - smoothstep(contour - 0.19, contour - 0.045, r));
    float phase = angle * 4.0 + r * 22.0 - Params.w * 4.0 + Params.z;
    vec2 direction = vec2(sin(phase) + 0.33 * sin(r * 37.0 - Params.w * 3.0),
                          cos(phase * 0.77) + 0.27 * cos(angle * 6.0 + Params.w * 2.0));
    float uneven = 0.67 + 0.33 * sin(angle * 11.0 + r * 17.0 + Params.z);
    vec2 offsetPixels = direction * (Params.y * warpMask * opacity * uneven);
    vec2 screenUV = gl_FragCoord.xy * Screen.zw;
    vec2 bentUV = clamp(screenUV + offsetPixels * Screen.zw,
                        Screen.zw * 0.5, vec2(1.0) - Screen.zw * 0.5);
    vec3 bentScene = texture(SceneSampler, bentUV).rgb;

    // Interior is faintly tinted like a transparent soap bubble, while the
    // textured world under it remains recognizable and visibly refracted.
    float innerAlpha = interior * opacity * 0.96;
    vec3 innerColor = mix(bentScene, Accent.rgb, 0.065);
    vec3 inkColor = mix(Accent.rgb, vec3(1.0),
                        0.22 + mainStroke * 0.28);
    float ringAlpha = ink * opacity * 0.93;
    float outAlpha = ringAlpha + innerAlpha * (1.0 - ringAlpha);
    if (outAlpha < 0.003) discard;
    vec3 outRgb = (inkColor * ringAlpha
                 + innerColor * innerAlpha * (1.0 - ringAlpha)) / max(outAlpha, 0.001);
    finalColor = vec4(outRgb, clamp(outAlpha, 0.0, 1.0));
}
