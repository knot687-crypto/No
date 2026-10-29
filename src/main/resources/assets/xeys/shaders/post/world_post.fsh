#version 330

uniform sampler2D SceneSampler;
uniform sampler2D DepthSampler;
uniform sampler2D DofSampler;
uniform sampler2D BloomSampler;

layout(std140) uniform WorldPost {
    mat4 InvViewProj;
    mat4 PrevViewProj;
    // xyz: how far the camera moved since the previous frame; w: motion blur scale (0 = off).
    vec4 CameraDelta;
    // x: samples, y: longest blur as a fraction of the screen width.
    vec4 Motion;
    // x: distance in focus, y: aperture, z: strength (0 = off).
    vec4 Focus;
    // x: strength (0 = off).
    vec4 Bloom;
    vec4 BloomTint;
    // w, h, 1/w, 1/h
    vec4 Size;
};

in vec2 texCoord;
out vec4 fragColor;

const float GOLDEN_ANGLE = 2.39996323;

// Interleaved gradient noise (Jimenez 2014): per-pixel jitter that turns sample banding into fine grain.
float ign(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

// Camera-relative world position of a pixel, reconstructed from its depth.
vec3 cameraRelative(vec2 uv, float depth) {
    vec4 p = InvViewProj * vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    return p.xyz / p.w;
}

void main() {
    vec2 uv = texCoord;
    vec3 color = texture(SceneSampler, uv).rgb;
    float depth = texture(DepthSampler, uv).r;
    vec3 rel = cameraRelative(uv, depth);
    float jitter = ign(gl_FragCoord.xy);

    // Camera motion blur: where was this point last frame? Smear along the path between.
    if (CameraDelta.w > 0.0) {
        vec4 prev = PrevViewProj * vec4(rel + CameraDelta.xyz, 1.0);
        if (prev.w > 0.0) {
            vec2 prevUv = prev.xy / prev.w * 0.5 + 0.5;
            vec2 v = (uv - prevUv) * CameraDelta.w;
            float len = length(v * Size.xy);
            float maxLen = Motion.y * Size.x;
            if (len > maxLen) {
                v *= maxLen / len;
                len = maxLen;
            }
            if (len > 0.75) {
                int n = int(Motion.x);
                vec3 sum = vec3(0.0);
                for (int i = 0; i < n; i++) {
                    float t = (float(i) + jitter) / float(n) - 0.5;
                    sum += texture(SceneSampler, uv - v * t).rgb;
                }
                color = sum / float(n);
            }
        }
    }

    // Depth of field: a sharp zone around the focus distance that scales with it, blur ramping up
    // quickly in front of it and gradually behind it. Wider apertures shorten the ramps.
    if (Focus.z > 0.0) {
        float dist = length(rel);
        float zone = Focus.x * 0.22;
        float ramp = 6.0 / Focus.y;
        float c = dist < Focus.x
            ? (Focus.x - zone - dist) / (Focus.x * 0.5 * ramp)
            : (dist - Focus.x - zone) / (Focus.x * 2.0 * ramp);
        c = clamp(c, 0.0, 1.0) * Focus.z;
        if (c > 0.004) {
            // A small adaptive disc for mild defocus, the precomputed wide blur for strong defocus.
            float radius = min(c, 0.5) * 14.0;
            float spin = jitter * 6.2831853;
            vec3 sum = color;
            for (int i = 0; i < 12; i++) {
                float a = float(i) * GOLDEN_ANGLE + spin;
                float r = sqrt((float(i) + 0.5) / 12.0) * radius;
                sum += texture(SceneSampler, uv + vec2(cos(a), sin(a)) * r * Size.zw).rgb;
            }
            vec3 disc = sum / 13.0;
            vec3 wide = texture(DofSampler, uv).rgb;
            vec3 blurred = mix(disc, wide, smoothstep(0.4, 1.0, c));
            color = mix(color, blurred, smoothstep(0.0, 0.15, c));
        }
    }

    // Bloom, screen-blended so highlights glow without clipping to flat white.
    if (Bloom.x > 0.0) {
        vec3 glow = clamp(texture(BloomSampler, uv).rgb * Bloom.x * BloomTint.rgb, 0.0, 1.0);
        color = 1.0 - (1.0 - color) * (1.0 - glow);
    }

    fragColor = vec4(color, 1.0);
}
