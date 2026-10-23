#version 330

uniform sampler2D InSampler;

layout(std140) uniform BlurConfig {
    vec2 HalfTexel;
    float Offset;
};

in vec2 texCoord;
out vec4 fragColor;

// Dual Kawase downsample (Marius Bjorge, ARM 2015).
void main() {
    vec2 o = HalfTexel * Offset;
    vec4 sum = texture(InSampler, texCoord) * 4.0;
    sum += texture(InSampler, texCoord - o);
    sum += texture(InSampler, texCoord + o);
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y));
    sum += texture(InSampler, texCoord - vec2(o.x, -o.y));
    fragColor = vec4(sum.rgb / 8.0, 1.0);
}
