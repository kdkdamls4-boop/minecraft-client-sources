vec4 rockstar_pixel_art(sampler2D image, vec2 uv, float softness) {
    vec2 size = vec2(textureSize(image, 0));
    vec2 texel = uv * size;

    vec2 dx = dFdx(texel);
    vec2 dy = dFdy(texel);
    vec2 width = max(abs(dx), abs(dy));
    vec2 band = clamp(width * softness, vec2(1.0e-5), vec2(1.0));

    vec2 seam = floor(texel + 0.5);
    vec2 sharp = seam + clamp((texel - seam) / band, vec2(-0.5), vec2(0.5));

    return textureGrad(image, sharp / size, dx / size, dy / size);
}

vec4 rockstar_pixel_art(sampler2D image, vec2 uv) {
    return rockstar_pixel_art(image, uv, 1.0);
}
