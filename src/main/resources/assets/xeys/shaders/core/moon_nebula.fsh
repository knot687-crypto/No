#version 330

#moj_import <xeys:xeys_common.glsl>
#moj_import <xeys:moon_common.glsl>

in vec2 texCoord;
out vec4 fragColor;

// Static backdrop (gradient + faint nebula), rendered once per resize into a cache texture.
vec3 skyColor(vec2 frag) {
    vec2 uv = frag / Screen.xy;
    vec3 col = mix(vec3(0.0060, 0.0068, 0.0120), vec3(0.0105, 0.0115, 0.0190), uv.y);
    // Very faint nebula clouds drifting slowly, tinted deep blue / violet.
    vec2 q = vec2(uv.x * Screen.x / Screen.y, uv.y) * 1.6;
    float n1 = fbm(q);
    float n2 = fbm(q * 1.9 + vec2(3.7, -1.3));
    float neb = smoothstep(0.42, 0.95, n1) * 0.8 + smoothstep(0.55, 1.0, n2) * 0.35;
    col += neb * Params.w * mix(vec3(0.010, 0.013, 0.032), vec3(0.020, 0.012, 0.030), n2);
    return col;
}


void main() {
    // texCoord spans the cache; map it to full-resolution pixel coordinates (origin top-left).
    vec2 frag = vec2(texCoord.x, 1.0 - texCoord.y) * Screen.xy;
    fragColor = vec4(skyColor(frag), 1.0);
}
