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

// The part of a color that glows. Lights and lit surfaces in an LDR image sit only a little above the
// daytime sky, so the ramp is narrow: the sky barely contributes while lanterns, lava and the sun do.
vec3 bright(vec3 c) {
    float l = dot(c, LUMA);
    return c * smoothstep(Threshold - 0.12, Threshold + 0.18, l);
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
