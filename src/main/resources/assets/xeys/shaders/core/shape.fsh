#version 330

#moj_import <minecraft:globals.glsl>
#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <xeys:xeys_common.glsl>

#if defined(SHAPE_GLASS) || defined(SHAPE_IMAGE)
uniform sampler2D Sampler0;
#endif

in vec4 vColor;
in vec2 vLocal;
flat in vec4 vA;
flat in vec4 vB;
flat in vec4 vC;
flat in vec4 vD;
flat in vec4 vClip;
flat in float vFeather;

out vec4 fragColor;

// Rounded box with per-corner radii. GUI space is y-down: negative y is the top edge.
// r = (top-left, top-right, bottom-right, bottom-left)
float sdRoundBox(vec2 p, vec2 b, vec4 r) {
    vec2 side = (p.x > 0.0) ? r.yz : r.xw;
    float rad = (p.y > 0.0) ? side.y : side.x;
    vec2 q = abs(p) - b + rad;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - rad;
}

// Arc band with round caps. Aperture centered on the +Y axis of the rotated frame.
float sdArc(vec2 p, vec2 sc, float ra, float rb) {
    p.x = abs(p.x);
    return ((sc.y * p.x > sc.x * p.y) ? length(p - sc * ra) : abs(length(p) - ra)) - rb;
}

vec4 premul(vec4 c) {
    return vec4(c.rgb * c.a, c.a);
}

void main() {
    // Local units per screen pixel, robust under any similarity transform of the pose.
    float px = max(length(dFdx(vLocal)), length(dFdy(vLocal)));
    px = max(px, 1e-5);

    vec4 fill = vColor;
    vec4 outP;

#if defined(SHAPE_ARC)
    // vA = (radius, thickness, start angle rad, sweep rad). Angles clockwise from 12 o'clock.
    float radius = vA.x;
    float halfThick = vA.y * 0.5;
    float sweep = vA.w;
    float d;
    float along;
    if (sweep >= 6.28318) {
        d = abs(length(vLocal) - radius) - halfThick;
        float ang = atan(vLocal.x, -vLocal.y);
        along = fract((ang - vA.z) / 6.2831853);
    } else {
        float mid = vA.z + sweep * 0.5;
        float cm = cos(mid);
        float sm = sin(mid);
        // Rotate so the arc mid-direction becomes +Y (pointing up on screen means -y in GUI space).
        vec2 p = vec2(cm * vLocal.x + sm * vLocal.y, sm * vLocal.x - cm * vLocal.y);
        float h = sweep * 0.5;
        d = sdArc(p, vec2(sin(h), cos(h)), radius, halfThick);
        float ang = atan(vLocal.x, -vLocal.y);
        along = clamp(fract((ang - vA.z) / 6.2831853) * 6.2831853 / max(sweep, 1e-4), 0.0, 1.0);
    }
    // Gradient along the arc from the vertex color to ParamsC when ParamsB.x > 0.5.
    if (vB.x > 0.5) {
        fill = mix(fill, vC, along);
    }
    // ParamsB.y > 0: soft glow falloff of that width instead of a crisp edge.
    float arcSoft = vB.y;
    float cov = arcSoft > 0.0 ? 0.5 - 0.5 * xeys_erf(d / (arcSoft * 0.5 * 1.41421356)) : clamp(0.5 - d / px, 0.0, 1.0);
    outP = premul(fill) * cov;
#elif defined(SHAPE_LINE)
    // vA = (half length, half thickness, round caps (1) or square (0), softness)
    vec2 q = vec2(max(abs(vLocal.x) - vA.x, 0.0), vLocal.y);
    float d = (vA.z > 0.5) ? length(q) - vA.y : max(abs(vLocal.x) - vA.x - vA.y, abs(vLocal.y) - vA.y);
    float soft = vA.w;
    float cov = soft > 0.0 ? clamp(0.5 - d / (soft + px), 0.0, 1.0) : clamp(0.5 - d / px, 0.0, 1.0);
    outP = premul(fill) * cov;
#else
    // Rounded rectangle family (fill, border, shadow, glass, image).
    vec2 halfSize = vA.xy;
    float border = vA.z;
    float soft = vA.w;
    float d = sdRoundBox(vLocal, halfSize, vB);

    if (soft > 0.0) {
        // Soft shadow / glow: gaussian falloff of the distance field. soft = blur radius (~2 sigma).
        float sigma = soft * 0.5;
        float a = 0.5 - 0.5 * xeys_erf(d / (sigma * 1.41421356));
        outP = premul(fill) * a;
    } else {
        float outerCov = clamp(0.5 - d / px, 0.0, 1.0);

#if defined(SHAPE_GLASS)
        vec2 suv = gl_FragCoord.xy / ScreenSize;
        vec3 scene = texture(Sampler0, suv).rgb;
        // Gentle luminance-preserving desaturation keeps glass calm over busy scenes.
        float lum = dot(scene, vec3(0.2126, 0.7152, 0.0722));
        scene = mix(vec3(lum), scene, 0.85);
        // vColor = tint color with alpha = tint strength.
        vec3 base = mix(scene, fill.rgb, fill.a);
        fill = vec4(base, 1.0);
#elif defined(SHAPE_IMAGE)
        vec2 t = vLocal / max(2.0 * halfSize, vec2(1e-4)) + 0.5;
        vec2 uv = mix(vC.xy, vC.zw, t);
        vec4 tex = texture(Sampler0, uv);
        fill = tex * fill;
#endif

        vec4 fillP = premul(fill);
        vec4 inner = fillP;

        // Inner glow along the edge using the secondary color (vD.y = size px, vD.z = strength).
#if !defined(SHAPE_IMAGE)
        if (vD.y > 0.0 && vD.z > 0.0) {
            float g = exp(-max(-d, 0.0) / vD.y) * vD.z;
            vec4 glowP = premul(vec4(vC.rgb, vC.a * g));
            inner = glowP + inner * (1.0 - glowP.a);
        }
#endif

        if (border > 0.0) {
#if defined(SHAPE_IMAGE)
            vec4 bc = vec4(0.0);
#else
            vec4 bc = vC;
            // Vertical fade of the border (top = full, bottom = vD.x) for a light-from-above highlight.
            float ty = clamp(vLocal.y / max(halfSize.y, 1e-4) * 0.5 + 0.5, 0.0, 1.0);
            bc.a *= mix(1.0, vD.x, ty);
#endif
            float innerCov = clamp(0.5 - (d + border) / px, 0.0, 1.0);
            vec4 bordP = premul(bc);
            vec4 ringP = bordP + fillP * (1.0 - bordP.a);
            outP = inner * innerCov + ringP * max(outerCov - innerCov, 0.0);
        } else {
            outP = inner * outerCov;
        }
    }
#endif

    float clipA = xeys_clip(vClip, vFeather, ScreenSize);
    outP *= clipA * ColorModulator.a;
    // vD.w: additive amount. Premultiplied output with reduced alpha adds light instead of covering.
    outP.a *= (1.0 - clamp(vD.w, 0.0, 1.0));
    if (outP.a <= 0.0 && dot(outP.rgb, vec3(1.0)) <= 0.0) {
        discard;
    }
    outP.rgb = xeys_dither(outP.rgb);
    fragColor = outP;
}
