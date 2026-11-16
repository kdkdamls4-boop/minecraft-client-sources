#version 330

#moj_import <desire:common.glsl>

#define texelSize (uRadius.xy)

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

in vec2 texCoord;
out vec4 fragColor;

float rawDepth(vec2 uv) {
    float depthBefore = texture(Sampler2, uv).r;
    float depthAfter = texture(Sampler3, uv).r;
    return smoothstep(0.000002, 0.000035, depthAfter - depthBefore);
}

float rawColor(vec2 uv) {
    vec3 afterColor = texture(Sampler0, uv).rgb;
    vec3 beforeColor = texture(Sampler1, uv).rgb;
    vec3 delta = abs(afterColor - beforeColor);
    float colorDiff = max(delta.r, max(delta.g, delta.b));
    return smoothstep(0.045, 0.145, colorDiff);
}

float seededMask(vec2 uv) {
    float depth = rawDepth(uv);
    float nearbyDepth = depth;
    vec2 px = texelSize * 1.5;
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2( px.x, 0.0), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2(-px.x, 0.0), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2(0.0,  px.y), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2(0.0, -px.y), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2( px.x,  px.y), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2(-px.x,  px.y), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2( px.x, -px.y), texelSize, vec2(1.0) - texelSize)));
    nearbyDepth = max(nearbyDepth, rawDepth(clamp(uv + vec2(-px.x, -px.y), texelSize, vec2(1.0) - texelSize)));
    return max(depth, rawColor(uv) * smoothstep(0.04, 0.32, nearbyDepth));
}

void main() {
    vec2 uv = texCoord;
    float center = seededMask(uv);
    float support = seededMask(uv + vec2(texelSize.x, 0.0))
                  + seededMask(uv - vec2(texelSize.x, 0.0))
                  + seededMask(uv + vec2(0.0, texelSize.y))
                  + seededMask(uv - vec2(0.0, texelSize.y));

    float coherent = smoothstep(0.18, 1.15, support);
    float sealed = smoothstep(0.8, 2.2, support);
    float mask = clamp(max(center * coherent, sealed), 0.0, 1.0);

    fragColor = vec4(mask, mask, mask, mask);
}
