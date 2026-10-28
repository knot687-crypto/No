#version 330

#moj_import <xeys:xeys_common.glsl>

// XEYS screen effects: every 2D post effect in one full-screen pass over a copy of the world image.
// Disabled effects have zero strength, so their branches are skipped uniformly across the screen.

uniform sampler2D InSampler;

layout(std140) uniform ScreenFx {
    vec4 Size;          // width, height, time (s), 0
    vec4 Vignette;      // strength, radius, softness, roundness (0 = fits the screen, 1 = circular)
    vec4 VignetteColor; // rgb, 0
    vec4 Grain;         // amount, size (px), speed, 0
    vec4 Chroma;        // offset at the corners (px), falloff exponent, 0, 0
    vec4 Scan;          // strength, spacing (px), scroll speed (px/s), 0
    vec4 Grade;         // saturation, contrast, brightness, sharpen
    vec4 Tint;          // rgb, amount
    vec4 Mono;          // amount, mode (0 grayscale, 1 sepia, 2 noir), 0, 0
    vec4 Edge;          // rgb, amount: a colored glow creeping in from the screen edges
    vec4 Flash;         // rgb, amount: a brief full-screen wash, stronger at the edges
    vec4 Frame;         // pixel size, letterbox bar height (fraction of height), wave amplitude (px), wave speed
    vec4 Glow;          // rgb, amount: additive light along the edges
};

in vec2 texCoord;
out vec4 fragColor;

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float luma(vec3 c) {
    return dot(c, vec3(0.2126, 0.7152, 0.0722));
}

vec3 sampleScene(vec2 uv, vec2 fromCenter, float radial) {
    if (Chroma.x <= 0.0) {
        return texture(InSampler, uv).rgb;
    }
    // Lateral chromatic aberration: red and blue pulled apart along the radius, growing to the corners.
    vec2 dir = length(fromCenter) > 1e-5 ? normalize(fromCenter) : vec2(0.0);
    vec2 d = dir * Chroma.x * pow(radial, Chroma.y) / Size.xy;
    return vec3(texture(InSampler, uv + d).r, texture(InSampler, uv).g, texture(InSampler, uv - d).b);
}

void main() {
    vec2 res = Size.xy;
    vec2 uv = texCoord;
    vec2 px = uv * res;

    if (Frame.z > 0.0) {
        // Slow refraction ripple (underwater): two crossed sine fields, scaled to the resolution.
        float k = res.y / 1080.0;
        float t = Size.z * Frame.w;
        vec2 wave = vec2(sin(px.y * 0.021 / k + t * 1.7) + 0.5 * sin(px.y * 0.047 / k - t * 2.3),
            cos(px.x * 0.018 / k + t * 1.3) + 0.5 * cos(px.x * 0.041 / k + t * 2.1));
        uv += wave * Frame.z * k / res;
    }

    if (Frame.x > 1.0) {
        vec2 cell = (floor(uv * res / Frame.x) + 0.5) * Frame.x;
        uv = cell / res;
    }

    vec2 fromCenter = uv - 0.5;
    float aspect = res.x / res.y;
    // Radial coordinate: 0 at the center, 1 at the corners; round (aspect-corrected) or screen-fitted.
    float roundR = length(fromCenter * vec2(aspect, 1.0)) / length(vec2(aspect * 0.5, 0.5));
    float fitR = length(fromCenter * 2.0) / sqrt(2.0);
    float radial = mix(fitR, roundR, Vignette.w);

    vec3 col = sampleScene(uv, fromCenter, roundR);

    if (Grade.w > 0.0) {
        vec2 t = 1.0 / res;
        vec3 n = texture(InSampler, uv + vec2(t.x, 0.0)).rgb + texture(InSampler, uv - vec2(t.x, 0.0)).rgb
            + texture(InSampler, uv + vec2(0.0, t.y)).rgb + texture(InSampler, uv - vec2(0.0, t.y)).rgb;
        col = clamp(col + (col - n * 0.25) * Grade.w, 0.0, 1.0);
    }

    col += Grade.z;
    col = (col - 0.5) * Grade.y + 0.5;
    float l = luma(col);
    col = mix(vec3(l), col, Grade.x);

    if (Mono.x > 0.0) {
        vec3 m;
        if (Mono.y < 0.5) {
            m = vec3(l);
        } else if (Mono.y < 1.5) {
            m = vec3(dot(col, vec3(0.393, 0.769, 0.189)), dot(col, vec3(0.349, 0.686, 0.168)), dot(col, vec3(0.272, 0.534, 0.131)));
        } else {
            float k = smoothstep(0.08, 0.92, l);
            m = vec3(k * k * (3.0 - 2.0 * k));
        }
        col = mix(col, m, Mono.x);
    }

    if (Tint.a > 0.0) {
        // Soft-light style color filter: keeps highlights and shadows, shifts the midtones.
        vec3 base = clamp(col, 0.0, 1.0);
        vec3 lo = 2.0 * base * Tint.rgb + base * base * (1.0 - 2.0 * Tint.rgb);
        vec3 hi = 2.0 * base * (1.0 - Tint.rgb) + sqrt(base) * (2.0 * Tint.rgb - 1.0);
        vec3 soft = mix(lo, hi, step(0.5, Tint.rgb));
        col = mix(col, soft, Tint.a);
    }

    if (Vignette.x > 0.0) {
        float v = smoothstep(Vignette.y - Vignette.z, Vignette.y + Vignette.z, radial);
        col = mix(col, VignetteColor.rgb, v * Vignette.x);
    }

    if (Edge.a > 0.0) {
        float e = smoothstep(0.35, 1.05, roundR);
        col = mix(col, Edge.rgb, clamp(e * e * Edge.a, 0.0, 1.0));
    }

    if (Glow.a > 0.0) {
        float gl = smoothstep(0.45, 1.1, roundR);
        col += Glow.rgb * gl * gl * Glow.a;
    }

    if (Flash.a > 0.0) {
        float f = Flash.a * (0.35 + 0.65 * smoothstep(0.1, 1.0, roundR));
        col = mix(col, Flash.rgb, clamp(f, 0.0, 1.0));
    }

    if (Scan.x > 0.0) {
        float s = 0.5 + 0.5 * sin((px.y + Size.z * Scan.z) * 6.2831853 / max(Scan.y, 1.0));
        col *= 1.0 - Scan.x * (1.0 - s) * 0.6;
    }

    if (Grain.x > 0.0) {
        vec2 g = floor(px / max(Grain.y, 1.0));
        float seed = floor(Size.z * 24.0 * Grain.z);
        float n = hash12(g + seed * vec2(17.13, 31.71)) - 0.5;
        // Strongest in the midtones, like film; nearly invisible in deep shadow and bright sky.
        float w = 1.0 - pow(abs(luma(col) * 2.0 - 1.0), 2.0) * 0.6;
        col += n * Grain.x * w;
    }

    if (Frame.y > 0.0) {
        float bar = Frame.y * res.y;
        float edge = min(px.y, res.y - px.y);
        float cover = clamp(bar - edge + 0.5, 0.0, 1.0);
        col = mix(col, vec3(0.0), cover);
    }

    fragColor = vec4(xeys_dither(clamp(col, 0.0, 1.0)), 1.0);
}
