#version 330

// Shared by the moon sky passes (see MoonSky.java for the uniform layout).

layout(std140) uniform MoonConfig {
    vec4 Screen;      // xy = framebuffer size (px), z = time (s), w = master brightness (fade)
    vec4 MoonDisc;    // xy = center (px, origin top-left), z = radius (px), w = longitude rotation (rad)
    vec4 SunDir;      // xyz = direction toward the sun (view space: +x right, +y up, +z toward viewer), w = axial tilt (rad)
    vec4 Look;        // xy = parallax offset (px), z = star brightness, w = glow strength
    vec4 Params;      // x = relief strength, y = moon visibility (0..1), z = exposure, w = nebula strength
    vec4 MeteorA[3];  // xy = head (px), z = travel angle (rad), w = alpha (0 = inactive)
    vec4 MeteorB[3];  // x = trail length (px), y = width (px)
};

const float PI = 3.14159265359;
const float TAU = 6.28318530718;

// ------------------------------------------------------------------ hashing / noise

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

vec2 hash22(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.xx + p3.yz) * p3.zy);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1, 0)), u.x), mix(hash12(i + vec2(0, 1)), hash12(i + vec2(1, 1)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * valueNoise(p);
        p = p * 2.03 + vec2(17.1, 9.7);
        a *= 0.5;
    }
    return v;
}

