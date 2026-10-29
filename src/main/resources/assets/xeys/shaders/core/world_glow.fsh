#version 330

#moj_import <minecraft:fog.glsl>

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec2 texCoord;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
#ifdef GLOW_BEAM
    // texCoord.x: -1..1 across the beam; texCoord.y: base (0) to top (1).
    float across = texCoord.x;
    float y = clamp(texCoord.y, 0.0, 1.0);
    float body = exp(-across * across * 5.0) * 0.55 + exp(-across * across * 60.0) * 0.6;
    float a = body * pow(1.0 - y, 1.5) * smoothstep(0.0, 0.04, y);
    vec3 color = mix(vertexColor.rgb, vec3(1.0), exp(-across * across * 60.0) * 0.5);
#else
    // A soft round glow with a brighter core; texCoord is -1..1 across the sprite.
    float r2 = dot(texCoord, texCoord);
    float core = exp(-r2 * 26.0);
    float a = exp(-r2 * 4.5) * 0.6 + core;
    vec3 color = mix(vertexColor.rgb, vec3(1.0), core * 0.45);
#endif
    // Additive light: fog fades it out rather than tinting it.
    float fog = total_fog_value(sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd,
        FogRenderDistanceStart, FogRenderDistanceEnd);
    a *= vertexColor.a * (1.0 - fog);
    if (a < 0.002) {
        discard;
    }
    fragColor = vec4(color, a);
}
