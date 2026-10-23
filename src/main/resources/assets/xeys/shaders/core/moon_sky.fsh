#version 330

#moj_import <xeys:xeys_common.glsl>
#moj_import <xeys:moon_common.glsl>

uniform sampler2D MoonAlbedo;
uniform sampler2D MoonNormal;
uniform sampler2D SkyCache;

in vec2 texCoord;
out vec4 fragColor;

// ------------------------------------------------------------------ sky

vec3 starLayer(vec2 frag, float cell, float depth, float density, float seed, float sizeScale) {
    vec2 p = (frag + Look.xy * depth) / cell;
    vec2 id = floor(p);
    vec2 f = fract(p);
    float h = hash12(id + seed);
    if (h > density) {
        return vec3(0.0);
    }
    vec2 pos = 0.18 + 0.64 * hash22(id + seed * 1.37);
    float mag = hash12(id + seed * 3.11);
    mag = mag * mag * mag;
    vec2 d = (f - pos) * cell;
    float sigma = mix(0.45, 1.25, mag) * sizeScale;
    float twinkle = 0.72 + 0.28 * sin(Screen.z * (0.6 + 2.4 * h) + h * 91.0);
    float core = exp(-dot(d, d) / (2.0 * sigma * sigma));
    // Halo and spikes fade out before the cell border so no cell edge is ever visible.
    float edge = min(min(f.x, 1.0 - f.x), min(f.y, 1.0 - f.y)) * cell;
    float window = smoothstep(0.0, cell * 0.17, edge);
    // The brightest stars get a soft halo and a faint diffraction cross.
    float halo = mag > 0.5 ? exp(-length(d) / (sigma * 3.0)) * 0.06 * window : 0.0;
    float spikes = mag > 0.9 ? (exp(-abs(d.x) / 0.5) * exp(-d.y * d.y / (sigma * 26.0))
        + exp(-abs(d.y) / 0.5) * exp(-d.x * d.x / (sigma * 26.0))) * 0.09 * window : 0.0;
    vec3 tint = mix(vec3(0.70, 0.80, 1.00), vec3(1.00, 0.88, 0.74), hash12(id + seed * 5.3));
    return tint * (core * (0.18 + 0.95 * mag) + halo + spikes) * twinkle;
}

vec3 meteor(vec2 frag, vec4 a, vec4 b) {
    if (a.w <= 0.001) {
        return vec3(0.0);
    }
    vec2 dir = vec2(cos(a.z), sin(a.z));
    vec2 rel = frag - a.xy;
    float behind = dot(rel, -dir);
    float across = dot(rel, vec2(-dir.y, dir.x));
    vec3 col = vec3(0.0);
    if (behind > -b.y * 6.0 && behind < b.x) {
        float t = clamp(behind / b.x, 0.0, 1.0);
        float width = b.y * mix(0.9, 0.18, t);
        float core = exp(-across * across / (2.0 * width * width));
        float tail = pow(1.0 - t, 2.4) * step(0.0, behind);
        col += vec3(0.80, 0.90, 1.00) * core * tail * 0.95;
    }
    float head = exp(-dot(rel, rel) / (2.0 * pow(b.y * 1.8, 2.0)));
    col += vec3(0.92, 0.96, 1.00) * head * 1.6;
    return col * a.w;
}

// ------------------------------------------------------------------ moon

mat3 rotX(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(1.0, 0.0, 0.0, 0.0, c, s, 0.0, -s, c);
}

mat3 rotY(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat3(c, 0.0, -s, 0.0, 1.0, 0.0, s, 0.0, c);
}

vec3 toLinear(vec3 c) {
    return pow(c, vec3(2.2));
}

// Returns premultiplied moon color (linear) and coverage in alpha.
vec4 moonColor(vec2 frag, out vec2 pOut) {
    float R = MoonDisc.z;
    vec2 p = (frag - MoonDisc.xy) / R;
    pOut = p;
    float r2 = dot(p, p);
    float r = sqrt(r2);
    float cover = clamp((1.0 - r) * R + 0.5, 0.0, 1.0);
    if (cover <= 0.0) {
        return vec4(0.0);
    }
    vec3 nView = vec3(p.x, -p.y, sqrt(max(1.0 - r2, 0.0)));
    mat3 toView = rotX(SunDir.w) * rotY(MoonDisc.w);
    mat3 toMoon = transpose(toView);
    vec3 m = toMoon * nView;

    float lon = atan(m.x, m.z);
    float lat = asin(clamp(m.y, -1.0, 1.0));
    vec2 uv = vec2(lon / TAU + 0.5, 0.5 - lat / PI);
    vec2 dx = dFdx(uv);
    vec2 dy = dFdy(uv);
    dx.x -= round(dx.x);
    dy.x -= round(dy.x);

    vec3 albedo = toLinear(textureGrad(MoonAlbedo, uv, dx, dy).rgb);
    vec3 tn = textureGrad(MoonNormal, uv, dx, dy).rgb * 2.0 - 1.0;
    tn.xy *= Params.x;
    tn = normalize(tn);

    vec3 east = normalize(vec3(m.z, 0.0, -m.x) + vec3(1e-5, 0.0, 0.0));
    vec3 north = cross(m, east);
    vec3 nMoon = normalize(tn.x * east + tn.y * north + tn.z * m);
    vec3 n = toView * nMoon;

    vec3 L = normalize(SunDir.xyz);
    vec3 V = vec3(0.0, 0.0, 1.0);
    float mu0 = max(dot(n, L), 0.0);
    float mu = max(dot(nView, V), 0.05);
    float lommel = 2.0 * mu0 / (mu0 + mu);
    float brdf = mix(mu0, lommel, 0.55);
    // Geometric terminator, softened; relief can still catch light slightly past it (crater rims, peaks).
    float geo = dot(nView, L);
    float term = smoothstep(-0.10, 0.10, geo);
    vec3 sun = vec3(1.0, 0.985, 0.96);
    vec3 lit = albedo * brdf * term * sun * Params.z;
    // Earthshine on the night side: just enough to read the silhouette against the stars.
    lit += albedo * vec3(0.010, 0.012, 0.017) * (1.0 - term);
    // Thin bright limb on the lit side.
    float limb = pow(1.0 - nView.z, 6.0) * max(geo, 0.0);
    lit += vec3(0.05, 0.06, 0.08) * limb;
    float vis = Params.y;
    return vec4(lit, 1.0) * cover * vis;
}

void main() {
    vec2 frag = vec2(gl_FragCoord.x, Screen.y - gl_FragCoord.y);

    // Sky (cached static backdrop, nudged by a fraction of the parallax) and stars, in linear light.
    vec2 skyUv = (frag + Look.xy * 0.08) / Screen.xy;
    vec3 col = toLinear(texture(SkyCache, vec2(skyUv.x, 1.0 - skyUv.y)).rgb);
    float res = Screen.y / 1080.0;
    vec3 stars = starLayer(frag, 95.0 * res, 0.15, 0.55, 11.0, max(res, 0.7))
        + starLayer(frag, 62.0 * res, 0.35, 0.42, 37.0, max(res, 0.7))
        + starLayer(frag, 140.0 * res, 0.7, 0.30, 71.0, max(res * 1.2, 0.8));
    col += stars * Look.z;
    for (int i = 0; i < 3; i++) {
        col += meteor(frag, MeteorA[i], MeteorB[i]);
    }

    // Moon with atmospheric halo.
    vec2 p;
    vec4 moon = moonColor(frag, p);
    float r = length(p);
    vec3 L = normalize(SunDir.xyz);
    vec2 Ls = normalize(vec2(L.x, -L.y) + vec2(1e-5));
    float side = 0.45 + 0.55 * max(dot(r > 1e-4 ? p / r : vec2(0.0), Ls), 0.0);
    float outside = max(r - 1.0, 0.0);
    float halo = (exp(-outside * 5.5) * 0.10 + exp(-outside * 26.0) * 0.20) * side;
    vec3 haloCol = vec3(0.50, 0.62, 1.00) * halo * Look.w * Params.y;
    // Thin scattered-light rim just inside the lit limb.
    float inside = max(1.0 - r, 0.0);
    vec3 rim = vec3(0.55, 0.66, 1.0) * exp(-inside * 40.0) * 0.06 * side * side * Look.w * Params.y;
    // Stars are hidden behind the disc; the halo glows over the sky only.
    col = col * (1.0 - moon.a) + moon.rgb + haloCol * (1.0 - moon.a) + rim * moon.a;

    // Vignette.
    vec2 uv = frag / Screen.xy - 0.5;
    uv.x *= Screen.x / Screen.y;
    float vig = smoothstep(1.25, 0.25, length(uv));
    col *= mix(0.55, 1.0, vig);

    col = pow(max(col, 0.0), vec3(1.0 / 2.2)) * Screen.w;
    fragColor = vec4(xeys_dither(col), 1.0);
}
