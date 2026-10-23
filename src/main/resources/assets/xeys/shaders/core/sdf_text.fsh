#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <xeys:xeys_common.glsl>

uniform sampler2D Sampler0;

in vec4 vColor;
in vec2 vUv;
flat in vec4 vA;   // x = weight (px), y = outline width (px), z = softness (px), w = em size in atlas px
flat in vec4 vC;   // outline / glow color
flat in vec4 vClip;
flat in float vFeather;

out vec4 fragColor;

// Must match FontAtlas: 128 at the outline, 16 levels per atlas pixel.
const float ON_EDGE = 128.0 / 255.0;
const float DIST_SCALE = 16.0 / 255.0;

void main() {
    vec2 size = vec2(textureSize(Sampler0, 0));
    vec2 texel = vUv * size;
    // Atlas pixels per screen pixel (level-0 units), robust under rotation and non-uniform scale.
    float atlasPerPx = max(length(dFdx(texel)), length(dFdy(texel)));
    atlasPerPx = max(atlasPerPx, 1e-4);

    float sample = texture(Sampler0, vUv).r;
    float dist = (sample - ON_EDGE) / DIST_SCALE / atlasPerPx; // signed distance in screen pixels

    // Small text reads lighter than it is with linear blending; nudge it up slightly.
    float emScreen = vA.w / atlasPerPx;
    float boost = clamp((16.0 - emScreen) * 0.012, 0.0, 0.12);
    float weight = vA.x + boost;

    vec4 outP;
    if (vA.z > 0.0) {
        // Soft mode (shadow / glow): gaussian-ish falloff of the field.
        float s = vA.z;
        float a = 0.5 - 0.5 * xeys_erf(-(dist + weight) / (s * 0.70710678));
        outP = vec4(vColor.rgb * vColor.a, vColor.a) * a;
    } else {
        float fillA = clamp(dist + weight + 0.5, 0.0, 1.0);
        vec4 fillP = vec4(vColor.rgb * vColor.a, vColor.a) * fillA;
        if (vA.y > 0.0) {
            float outlineA = clamp(dist + weight + vA.y + 0.5, 0.0, 1.0);
            vec4 lineP = vec4(vC.rgb * vC.a, vC.a) * outlineA;
            outP = fillP + lineP * (1.0 - fillP.a);
        } else {
            outP = fillP;
        }
    }

    outP *= xeys_clip(vClip, vFeather, ScreenSize) * ColorModulator.a;
    if (outP.a <= 0.0) {
        discard;
    }
    fragColor = outP;
}
