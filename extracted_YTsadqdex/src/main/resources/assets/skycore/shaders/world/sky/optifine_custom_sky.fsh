#version 330 core

in vec2 uv;
out vec4 finalColor;

uniform sampler2D DepthSampler;
uniform sampler2D SkySampler;

layout(std140) uniform CustomSkyUniforms {
    mat4 InvViewProjection;
    vec4 SkyParams; // brightness, rotation, z-zero-to-one, reserved
    vec4 RotationAxis;
};

vec3 rotateAxis(vec3 value, vec3 axis, float angle) {
    float c = cos(angle);
    float s = sin(angle);
    return value * c + cross(axis, value) * s + axis * dot(axis, value) * (1.0 - c);
}

vec2 atlasUv(vec3 d) {
    vec3 a = abs(d);
    int side;
    vec2 face;
    if (a.y >= a.x && a.y >= a.z) {
        if (d.y < 0.0) { side = 0; face = vec2(-d.z, d.x) / a.y; }
        else           { side = 1; face = vec2(-d.z, -d.x) / a.y; }
    } else if (a.x >= a.z) {
        if (d.x > 0.0) { side = 2; face = vec2(d.z, -d.y) / a.x; }
        else           { side = 4; face = vec2(-d.z, -d.y) / a.x; }
    } else {
        if (d.z > 0.0) { side = 3; face = vec2(-d.x, -d.y) / a.z; }
        else           { side = 5; face = vec2(d.x, -d.y) / a.z; }
    }
    vec2 local = face * 0.5 + 0.5;
    return (vec2(float(side % 3), float(side / 3)) + local) / vec2(3.0, 2.0);
}

void main() {
    if (texture(DepthSampler, uv).r > 0.000001 || SkyParams.x <= 0.0001) discard;
    vec2 ndc = uv * 2.0 - 1.0;
    float sampleZ = SkyParams.z > 0.5 ? 0.0 : -1.0;
    vec4 point = InvViewProjection * vec4(ndc, sampleZ, 1.0);
    vec3 direction = normalize(point.xyz / point.w);
    direction = rotateAxis(direction, normalize(RotationAxis.xyz), -SkyParams.y);
    vec4 color = texture(SkySampler, atlasUv(direction));
    finalColor = vec4(color.rgb, color.a * SkyParams.x);
}
