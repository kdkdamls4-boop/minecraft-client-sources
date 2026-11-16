 

layout(std140) uniform WorldSkyUniforms {
    mat4 InvViewProjection;
    vec4 PrimaryColor;
    vec4 SecondaryColor;
    vec4 SkyParams;  
    vec4 SkyTexel;   
};

vec3 skyDirection(vec2 coords) {
    vec2 ndc = coords * 2.0 - 1.0;
    float sampleZ = SkyParams.w > 0.5 ? 0.0 : -1.0;
    vec4 point = InvViewProjection * vec4(ndc, sampleZ, 1.0);
    return normalize(point.xyz / point.w);
}

 
 
bool skyNearby(sampler2D depthSampler, vec2 coords) {
    float depthStride = SkyTexel.z > 0.0 ? SkyTexel.z : 2.0;
    vec2 d = SkyTexel.xy * depthStride;
    if (texture(depthSampler, coords).r <= 0.000001) return true;
    if (texture(depthSampler, coords + vec2( d.x, 0.0)).r <= 0.000001) return true;
    if (texture(depthSampler, coords + vec2(-d.x, 0.0)).r <= 0.000001) return true;
    if (texture(depthSampler, coords + vec2(0.0,  d.y)).r <= 0.000001) return true;
    if (texture(depthSampler, coords + vec2(0.0, -d.y)).r <= 0.000001) return true;
    return false;
}
