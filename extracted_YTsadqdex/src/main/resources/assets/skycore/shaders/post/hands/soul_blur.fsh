#version 330 core

in vec2 uv;
#define texCoord uv 
#define texelSize (1.0 / params.xy)
#define offset params.z  

out vec4 finalColor;

layout(std140) uniform HandBlurData { vec4 params; };
uniform sampler2D Sampler0;

void main() {
    vec2 sampleUv = texCoord;
    vec2 halfpixel = texelSize * 0.5 * offset;

    vec4 sum = texture(Sampler0, sampleUv) * 4.0;
    sum += texture(Sampler0, clamp(sampleUv - halfpixel.xy, vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(sampleUv + halfpixel.xy, vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(sampleUv + vec2(halfpixel.x, -halfpixel.y), vec2(0.005), vec2(0.995)));
    sum += texture(Sampler0, clamp(sampleUv - vec2(halfpixel.x, -halfpixel.y), vec2(0.005), vec2(0.995)));

    finalColor = sum / 8.0;
}
