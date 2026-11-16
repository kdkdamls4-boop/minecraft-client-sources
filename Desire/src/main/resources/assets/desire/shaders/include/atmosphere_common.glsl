
layout(std140) uniform AtmosphereUniforms {
    mat4 InvViewProjection;
    vec4 FogColorDensity;
    vec4 FogParams;
    vec4 FogExtra;
    vec4 SunDirection;
    vec4 CameraOrigin;
    vec4 WindParams;
    vec4 FlashColor;
    vec4 BoltEnds;
    vec4 BoltColor;
    vec4 BoltParams;
    vec4 RainColor;
    vec4 RainParams;
    vec4 RippleParams;
    vec4 HazeColor;
    vec4 HazeParams;
};

vec3 atmosphereReconstruct(vec2 coords, float depth) {
    float clipZ = FogParams.z > 0.5 ? depth : depth * 2.0 - 1.0;
    vec4 ndc = vec4(coords * 2.0 - 1.0, clipZ, 1.0);
    vec4 position = InvViewProjection * ndc;
    return position.xyz / position.w;
}
