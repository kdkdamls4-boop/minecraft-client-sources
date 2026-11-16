uniform sampler2D SkyEnvironmentSampler;
vec3 sampleSkyEnvironment(vec3 direction) {
    vec3 d = normalize(direction);
    vec2 coords = vec2(fract(atan(d.z, d.x) / 6.28318530718), acos(clamp(d.y, -1.0, 1.0)) / 3.14159265359);
     
    float halfTexel = 0.5 / float(textureSize(SkyEnvironmentSampler, 0).x);
    if (coords.x < halfTexel || coords.x > 1.0 - halfTexel) {
        float blend = coords.x < halfTexel ? 0.5 + coords.x / (2.0 * halfTexel)
                : (coords.x - (1.0 - halfTexel)) / (2.0 * halfTexel);
        return mix(texture(SkyEnvironmentSampler, vec2(1.0 - halfTexel, coords.y)).rgb,
                texture(SkyEnvironmentSampler, vec2(halfTexel, coords.y)).rgb, blend);
    }
    return texture(SkyEnvironmentSampler, coords).rgb;
}
