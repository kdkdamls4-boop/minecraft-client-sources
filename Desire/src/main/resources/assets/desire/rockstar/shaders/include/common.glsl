
#moj_import <rockstar:rect.glsl>

float sdfAA(float dist, float smoothness) {
    return max(smoothness, fwidth(dist));
}

float ralpha(vec2 size, vec2 coord, vec4 radius, float smoothness) {
    vec2 center = size * 0.5;
    float dist = rdist(center - (coord * size), center - 1.0, radius);
    return 1.0 - smoothstep(1.0 - sdfAA(dist, smoothness), 1.0, dist);
}
