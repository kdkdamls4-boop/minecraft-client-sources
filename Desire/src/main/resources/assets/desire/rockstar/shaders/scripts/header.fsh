#version 150

in vec2 FragCoord;
#define TexCoord FragCoord

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 InvViewProj;
uniform vec3 CamPos;
uniform vec2 Size;
uniform vec2 Resolution;
uniform vec2 MousePos;
uniform float Time;
uniform float GuiScale;
uniform vec4 Color;
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

out vec4 fragColor;
#define OutColor fragColor
