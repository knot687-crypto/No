#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

layout(std140) uniform SkyFx {
    // x: time in seconds, y: intensity, z/w: per effect (stars: size, twinkle speed).
    vec4 Params;
    vec4 ColorA;
    vec4 ColorB;
};

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord;
out vec4 vertexColor;
out float twinkle;

void main() {
#ifdef SKY_STARS
    // Position is the star's center and UV0 its sprite corner (-1..1): expand to face the viewer.
    vec3 n = normalize(Position);
    vec3 right = normalize(cross(n, abs(n.y) > 0.99 ? vec3(1.0, 0.0, 0.0) : vec3(0.0, 1.0, 0.0)));
    vec3 up = cross(right, n);
    float h = fract(sin(dot(Position, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    float size = Params.z * (0.55 + 0.9 * h * h);
    vec3 pos = Position + (right * UV0.x + up * UV0.y) * size;
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);
    twinkle = 0.62 + 0.38 * sin(Params.x * (0.8 + 2.4 * h) * Params.w + h * 61.0);
#else
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    twinkle = 1.0;
#endif
    texCoord = UV0;
    vertexColor = Color;
}
