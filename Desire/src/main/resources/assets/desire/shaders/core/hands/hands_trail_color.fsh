#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform TrailData {
    vec4 a;
    vec4 b;
    vec4 c;
    vec4 d;
    vec4 e;
};

void main() {
    vec4 bl = texture(Sampler0, texCoord);
    float exposure = c.y;
    float autoColor = c.z;
    float saturation = c.w;

    float alpha = clamp(bl.a * exposure, 0.0, 1.0);
    if (alpha <= 0.001) discard;

    vec3 col;
    if (autoColor > 0.5) {
        vec3 cc = bl.rgb / max(bl.a, 0.001);
        float m = max(cc.r, max(cc.g, cc.b));
        if (m > 0.001) cc /= m;
        float l = dot(cc, vec3(0.299, 0.587, 0.114));
        col = clamp(mix(vec3(l), cc, saturation), 0.0, 1.0);
    } else {
        col = mix(d.rgb, e.rgb, texCoord.y);
    }

    fragColor = vec4(col * alpha, alpha);
}
