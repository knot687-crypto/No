#version 330

// Shared helpers for XEYS shaders.

// Clip mask: rectangle in framebuffer pixels (origin top-left) with a feather width in pixels.
float xeys_clip(vec4 clip, float feather, vec2 screenSize) {
    vec2 frag = vec2(gl_FragCoord.x, screenSize.y - gl_FragCoord.y);
    vec2 d = min(frag - clip.xy, clip.zw - frag);
    float inside = min(d.x, d.y);
    return clamp((inside + 0.5) / max(feather, 1.0), 0.0, 1.0);
}

// Interleaved gradient noise (Jimenez 2014): stable, cheap, removes 8-bit banding on dark gradients.
float xeys_ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

vec3 xeys_dither(vec3 color) {
    return color + (xeys_ign(gl_FragCoord.xy) - 0.5) / 255.0;
}

// Abramowitz & Stegun style erf approximation, max error ~5e-4.
float xeys_erf(float x) {
    float s = sign(x);
    float a = abs(x);
    float t = 1.0 + (0.278393 + (0.230389 + 0.078108 * (a * a)) * a) * a;
    t *= t;
    return s - s / (t * t);
}
