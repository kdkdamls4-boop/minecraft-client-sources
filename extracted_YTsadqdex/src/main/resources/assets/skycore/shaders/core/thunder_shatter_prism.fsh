#version 330 core
in vec2 uv;
in vec4 tint;
out vec4 finalColor;
layout(std140) uniform ThunderUniforms { vec4 Thunder; };

void main() {
    // UV triangle: tip (0.5,0), base (0,1)-(1,1). An analytic Fresnel-like
    // glass edge, moving refraction and spectral highlight live on the shard.
    float left = 0.5 * (1.0 - uv.y);
    float right = 0.5 * (1.0 + uv.y);
    float edge = min(uv.y * 0.65, min((uv.x - left) * 2.0, (right - uv.x) * 2.0));
    edge = max(edge, 0.0);
    float fresnel = 1.0 - smoothstep(0.005, 0.095, edge);
    float streak = pow(max(0.0, sin(uv.y * 15.0 - uv.x * 17.0 + Thunder.x * 3.4)), 22.0);
    float facets = 0.18 + 0.30 * (1.0 - uv.y) + 0.14 * sin(uv.x * 22.0 + uv.y * 8.0);
    float chroma = sin((uv.x - uv.y * 0.3) * 18.0 + Thunder.x * 1.8) * 0.5 + 0.5;
    vec3 glass = tint.rgb * facets;
    glass += mix(tint.rgb, vec3(0.60, 0.91, 1.0), chroma * 0.46) * fresnel * 0.88;
    glass += vec3(1.0, 0.96, 0.81) * streak * .35;
    float alpha = tint.a * clamp(facets + fresnel * .70 + streak * .30, 0.0, 1.0);
    if (alpha < .003) discard;
    finalColor = vec4(glass, alpha);
}
