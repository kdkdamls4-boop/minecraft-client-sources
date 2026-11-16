#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec2 localPos;
in vec4 ringColor;
flat in vec2 halfSize;
flat in float ringThickness;
flat in float healthProgress;
flat in int ringStyle;

out vec4 fragColor;

void main() {
    vec4 color = ringColor * ColorModulator;
    if (color.a <= 0.001 || ringThickness <= 0.001
            || (ringStyle != 1 && healthProgress <= 0.001)) {
        discard;
    }

    const float TAU = 6.28318530718;
    const float TOP = -1.57079632679;
    float angle = atan(localPos.y, localPos.x);
     
     
    float inset = ringStyle != 0 ? 1.65 : 0.0;
    float outerRadius = max(min(halfSize.x, halfSize.y) - 0.5 - inset, 0.5);
    float innerRadius = max(outerRadius - ringThickness, 0.0);
     
    float amplitude = ringStyle == 2 ? min(0.52, ringThickness * 0.22) : 0.0;
    float wave = amplitude * sin(10.0 * (angle - TOP));
    float radius = length(localPos) - wave;
    float aa = max(fwidth(radius), 0.55);

    float outerCoverage = 1.0 - smoothstep(outerRadius - aa, outerRadius + aa, radius);
    float innerCoverage = 1.0 - smoothstep(innerRadius - aa, innerRadius + aa, radius);
    float ringCoverage = outerCoverage * (1.0 - innerCoverage);

    float arcCoverage = 1.0;
    float trackStart = 0.0;
    float trackEnd = 1.0;
    if (ringStyle == 1 && healthProgress > 0.001) {
        // Keep the unfilled Material-style track separate from both rounded ends
        // of the filled arc. The spacing is measured along the ring in pixels.
        float centerRadius = max(outerRadius - ringThickness * 0.5, 0.5);
        float gapPx = ringThickness + max(1.5, ringThickness * 0.85);
        float gapTurns = gapPx / (TAU * centerRadius);
        trackStart = healthProgress + gapTurns;
        trackEnd = 1.0 - gapTurns;
        if (trackStart >= trackEnd) discard;
        float clockwise = mod(angle - TOP + TAU, TAU) / TAU;
        float angularAa = max(fwidth(clockwise), 0.0015);
        arcCoverage = smoothstep(trackStart - angularAa, trackStart + angularAa, clockwise)
                    * (1.0 - smoothstep(trackEnd - angularAa, trackEnd + angularAa, clockwise));
    } else if (healthProgress < 0.9999 && ringStyle != 1) {
        float ccwFromTop = mod(TOP - angle + TAU, TAU) / TAU;
        float lostProgress = 1.0 - healthProgress;
        float angularAa = max(fwidth(ccwFromTop), 0.0015);
        arcCoverage = smoothstep(lostProgress - angularAa, lostProgress + angularAa, ccwFromTop);
    }

    float coverage = ringCoverage * arcCoverage;
    if (ringStyle == 1 && healthProgress > 0.001) {
        // Round both ends of the background arc without bridging the small gaps.
        float centerRadius = outerRadius - ringThickness * 0.5;
        float startAngle = TOP + TAU * trackStart;
        float endAngle = TOP + TAU * trackEnd;
        vec2 startCenter = vec2(cos(startAngle), sin(startAngle)) * centerRadius;
        vec2 endCenter = vec2(cos(endAngle), sin(endAngle)) * centerRadius;
        float capDistance = min(length(localPos - startCenter), length(localPos - endCenter));
        float capAa = max(fwidth(capDistance), 0.55);
        float capCoverage = 1.0 - smoothstep(ringThickness * 0.5 - capAa,
                                             ringThickness * 0.5 + capAa, capDistance);
        coverage = max(coverage, capCoverage);
    }
    if (ringStyle == 2 && healthProgress < 0.9999) {
         
        float centerRadius = outerRadius - ringThickness * 0.5;
        float endAngle = TOP + TAU * healthProgress;
        float endRadius = centerRadius + amplitude * sin(10.0 * (endAngle - TOP));
        vec2 startCenter = vec2(cos(TOP), sin(TOP)) * centerRadius;
        vec2 endCenter = vec2(cos(endAngle), sin(endAngle)) * endRadius;
        float capDistance = min(length(localPos - startCenter), length(localPos - endCenter));
        float capAa = max(fwidth(capDistance), 0.55);
        float capCoverage = 1.0 - smoothstep(ringThickness * 0.5 - capAa,
                                             ringThickness * 0.5 + capAa, capDistance);
        coverage = max(coverage, capCoverage);
    }
    float alpha = color.a * coverage;
    if (alpha <= 0.001) {
        discard;
    }
    fragColor = vec4(color.rgb, alpha);
}
