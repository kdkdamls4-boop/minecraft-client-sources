#version 150

in vec3 Position;
in vec2 UV0;
in vec4 Color;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 TexCoord;
out vec4 FragColor;
out vec2 GlobalPos;
flat out int GlyphIndex;

void main() {
    TexCoord = UV0;
    FragColor = Color;
    GlobalPos = Position.xy;
    GlyphIndex = UV2.x + UV2.y * 32768 - 1;

    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
