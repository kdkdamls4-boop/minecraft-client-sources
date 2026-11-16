#version 330 core
in vec2 brushUV;
in vec4 brushColor;
out vec4 finalColor;

void main() {
    // Every strip is actual twisted world geometry. The shader turns that
    // geometry into a cel-painted brush: irregular black boundary, broad
    // white interior and subtle unevenness from real hand-painted ink.
    float u = brushUV.x;
    float side = abs(brushUV.y * 2.0 - 1.0);
    // Broad clean cel-fill: the old high-frequency noise created white striped
    // artefacts, especially where many narrow surfaces overlapped.
    float rough = 0.014 * sin(u * 17.0 + brushUV.y * 3.0)
                + 0.009 * sin(u * 29.0 - brushUV.y * 4.0);
    float innerBoundary = 0.72 + rough;
    float inkWhite = 1.0 - smoothstep(innerBoundary - .025, innerBoundary + .025, side);
    // Pure black accent strokes have vertex RGB=0, white/ink outlined RGB=1.
    vec3 charcoal = vec3(.015, .014, .018);
    vec3 paper = vec3(.995, .991, .975);
    float white = inkWhite * brushColor.r;
    vec3 color = mix(charcoal, paper, white);
    float tipAlpha = smoothstep(0.0, .035, u) * (1.0 - smoothstep(.95, 1.0, u));
    // Defined black ink rim with a clean outer edge, without moire-like hatching.
    float edge = 1.0 - smoothstep(.955, 1.0, side);
    float alpha = brushColor.a * tipAlpha * edge;
    if (alpha < .003) discard;
    finalColor = vec4(color, alpha);
}
