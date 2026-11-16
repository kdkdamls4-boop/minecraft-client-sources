#version 150

in vec2 TexCoord;
in vec4 FragColor;

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float Floor;

out vec4 OutColor;

void main() {
    vec4 refl = texture(Sampler0, TexCoord);
    vec3 lifted = mix(refl.rgb, vec3(1.0), clamp(Floor, 0.0, 1.0));
    OutColor = vec4(lifted, refl.a) * ColorModulator;
}
