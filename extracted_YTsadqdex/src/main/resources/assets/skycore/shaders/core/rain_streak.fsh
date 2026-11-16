#version 330 core

in vec2 uv;
in vec4 tint;
in float viewDistance;
out vec4 finalColor;

void main() {
    vec2 p = uv + uv - 1.0;
    float column = 1.0 - smoothstep(0.08, 0.92, abs(p.x));
    float tip = 1.0 - smoothstep(0.62, 1.0, abs(p.y));
    float bright = mix(0.28, 0.85, uv.y);
    float alpha = column * column * tip * bright * tint.a;
    alpha *= smoothstep(0.35, 2.2, viewDistance);
    alpha *= 1.0 - smoothstep(26.0, 38.0, viewDistance);
    if (alpha <= 0.002) {
        discard;
    }
    vec3 rgb = mix(tint.rgb, vec3(1.0), 0.35);
    finalColor = vec4(rgb, clamp(alpha, 0.0, 1.0));
}
