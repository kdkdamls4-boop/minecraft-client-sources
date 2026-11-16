#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

layout(std140) uniform HandsData {
    vec4 colorA;
    vec4 colorB;
    vec4 params0;
    vec4 params1;
    vec4 params2;
};

void main() {
    vec4 bloom = texture(Sampler0, texCoord);
    vec4 mask = texture(Sampler1, texCoord);
    float outer = bloom.a * (1.0 - mask.a);
    float intensity = clamp(outer * params0.x, 0.0, 1.0);
    if (intensity <= 0.001) discard;

    vec3 grad;
    if (params0.y > 0.5) {
        vec3 c = bloom.rgb / max(bloom.a, 0.001);
        float m = max(c.r, max(c.g, c.b));
        if (m > 0.001) c /= m;
        float l = dot(c, vec3(0.299, 0.587, 0.114));
        grad = clamp(mix(vec3(l), c, params0.z), 0.0, 1.0);
    } else {
        grad = mix(colorA.rgb, colorB.rgb, texCoord.y);
    }
    fragColor = vec4(grad, intensity);
}
