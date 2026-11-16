#version 150

in vec2 uv;
out vec4 out_color;
uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
layout(std140) uniform JumpData {
    mat4 inv_mvp; mat4 mvp; vec4 wave_params; vec4 first_color; vec4 second_color; vec4 gradient_dir;
    vec4 wave_pos[8]; vec4 wave_fade[8];
};
void main() {
    float rawDepth = texture(DepthSampler, uv).r;
    if (rawDepth >= 0.99999) { out_color = texture(SceneSampler, uv); return; }
    float clipDepth = wave_params.z > 0.5 ? rawDepth : rawDepth * 2.0 - 1.0;
    vec4 wp = inv_mvp * vec4(uv * 2.0 - 1.0, clipDepth, 1.0);
    if (wp.w == 0.0) { out_color = texture(SceneSampler, uv); return; }
    vec3 worldRel = wp.xyz / wp.w;
    float bestStrength=0.0, bestDisplace=0.0; vec2 bestDir=vec2(0.0,1.0);
    for(int i=0;i<8;i++) {
        float fade=wave_fade[i].x, radius=wave_pos[i].w; if(fade<=0.001||radius<=0.05) continue;
        vec3 diff=worldRel-wave_pos[i].xyz; float d=length(diff.xz); float delta=abs(d-radius);
        if (diff.y > 0.30) continue;
        if(delta<wave_params.x) { float ring=smoothstep(0.0,1.0,1.0-delta/wave_params.x); float s=ring*fade*exp(-abs(diff.y)*0.42);
            if(s>bestStrength) { bestStrength=s; bestDir=diff.xz/max(d,0.001); bestDisplace=-sin((d-radius)/wave_params.x*3.14159265)*s; }
        }
    }
    if(bestStrength<=0.001) { out_color=texture(SceneSampler,uv); return; }
    vec2 off=bestDir*bestDisplace*(wave_params.y*0.015); vec2 ca=bestDir*(wave_params.w*0.00025)*bestStrength; vec2 sampleUv=uv-off;
    vec3 scene=vec3(texture(SceneSampler,sampleUv+ca).r,texture(SceneSampler,sampleUv).g,texture(SceneSampler,sampleUv-ca).b);
    vec3 col=mix(first_color.rgb,second_color.rgb,dot(bestDir,gradient_dir.xy)*0.5+0.5); float glow=pow(bestStrength,1.8);
    out_color=vec4(mix(scene,col,glow*0.45)+col*glow*0.7+vec3(1.0)*pow(bestStrength,5.0)*1.6,1.0);
}
