#version 330

uniform sampler2D InSampler;

layout(std140) uniform BlurConfig {
    vec2 HalfTexel;
    float Offset;
    float Threshold;
};

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

// The part of a color above the threshold, with a soft knee so glows fade in instead of popping.
vec3 bright(vec3 c) {
    float l = dot(c, LUMA);
    float knee = 0.5 * Threshold + 1e-4;
    float s = clamp(l - Threshold + knee, 0.0, 2.0 * knee);
    s = s * s / (4.0 * knee);
    return c * (max(s, l - Threshold) / max(l, 1e-4));
}

// Luminance-weighted (Karis) so a single bright pixel cannot make the whole glow flicker.
vec4 tap(vec2 uv) {
    vec3 c = bright(texture(InSampler, uv).rgb);
    float w = 1.0 / (1.0 + dot(c, LUMA));
    return vec4(c * w, w);
}

// The first dual-Kawase downsample of the bloom chain, keeping only what glows.
void main() {
    vec2 o = HalfTexel * Offset;
    vec4 sum = tap(texCoord) * 4.0;
    sum += tap(texCoord - o);
    sum += tap(texCoord + o);
    sum += tap(texCoord + vec2(o.x, -o.y));
    sum += tap(texCoord - vec2(o.x, -o.y));
    fragColor = vec4(sum.rgb / max(sum.a, 1e-4), 1.0);
}
