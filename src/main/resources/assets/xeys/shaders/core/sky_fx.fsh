#version 330

layout(std140) uniform SkyFx {
    // x: time in seconds, y: intensity, z/w: per effect.
    vec4 Params;
    vec4 ColorA;
    vec4 ColorB;
};

in vec2 texCoord;
in vec4 vertexColor;
in float twinkle;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float noise(float x) {
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(i), hash(i + 1.0), f);
}

void main() {
#if defined(SKY_AURORA)
    // texCoord.x runs along the curtain, texCoord.y from its lower edge (0) to its top (1).
    float t = Params.x;
    float x = texCoord.x;
    float y = texCoord.y;
    // Vertical rays at several scales, drifting along the curtain.
    float rays = noise(x * 40.0 + t * 0.32) * 0.5 + noise(x * 97.0 - t * 0.55) * 0.3 + noise(x * 9.0 + t * 0.12) * 0.7;
    rays = pow(clamp(rays / 1.5, 0.0, 1.0), 1.7) * 1.6;
    // A crisp lower edge and a long fade upward, with slow brightness waves running along.
    float profile = smoothstep(0.0, 0.07, y) * pow(1.0 - y, 1.6);
    float pulse = 0.65 + 0.35 * sin(x * 6.0 - t * 0.8);
    vec3 color = mix(ColorA.rgb, ColorB.rgb, smoothstep(0.1, 0.9, y));
    float a = rays * profile * pulse * vertexColor.a * Params.y;
    fragColor = vec4(color, clamp(a, 0.0, 1.0));
#elif defined(SKY_METEOR)
    // texCoord.x: tail (0) to head (1); texCoord.y: -1..1 across the streak.
    float along = texCoord.x;
    float across = texCoord.y;
    float core = exp(-across * across * 9.0);
    float tail = pow(along, 2.2) * core;
    float head = exp(-(1.0 - along) * (1.0 - along) * 160.0) * exp(-across * across * 3.0);
    float a = (tail + head * 0.9) * vertexColor.a * Params.y;
    vec3 color = mix(ColorA.rgb, vec3(1.0), clamp(head + core * along * 0.5, 0.0, 1.0));
    fragColor = vec4(color, clamp(a, 0.0, 1.0));
#else
    // Stars: a soft halo around a bright core.
    float r2 = dot(texCoord, texCoord);
    float a = (exp(-r2 * 3.2) * 0.5 + exp(-r2 * 14.0)) * vertexColor.a * twinkle * Params.y;
    fragColor = vec4(vertexColor.rgb, clamp(a, 0.0, 1.0));
#endif
}
