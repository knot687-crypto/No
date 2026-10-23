#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec4 ParamsA;
in vec4 ParamsB;
in vec4 ParamsC;
in vec4 ParamsD;
in vec4 Clip;

out vec4 vColor;
out vec2 vLocal;
flat out vec4 vA;
flat out vec4 vB;
flat out vec4 vC;
flat out vec4 vD;
flat out vec4 vClip;
flat out float vFeather;

void main() {
    // Position.z carries the clip feather; GUI geometry always sits on the z = 0 plane.
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    vColor = Color;
    vLocal = UV0;
    vA = ParamsA;
    vB = ParamsB;
    vC = ParamsC;
    vD = ParamsD;
    vClip = Clip;
    vFeather = Position.z;
}
