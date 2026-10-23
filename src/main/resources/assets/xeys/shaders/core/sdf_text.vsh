#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec4 ParamsA;
in vec4 ParamsC;
in vec4 Clip;

out vec4 vColor;
out vec2 vUv;
flat out vec4 vA;
flat out vec4 vC;
flat out vec4 vClip;
flat out float vFeather;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position.xy, 0.0, 1.0);
    vColor = Color;
    vUv = UV0;
    vA = ParamsA;
    vC = ParamsC;
    vClip = Clip;
    vFeather = Position.z;
}
