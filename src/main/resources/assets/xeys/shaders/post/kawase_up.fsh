#version 330

uniform sampler2D InSampler;

layout(std140) uniform BlurConfig {
    vec2 HalfTexel;
    float Offset;
};

in vec2 texCoord;
out vec4 fragColor;

// Dual Kawase upsample with a tent filter.
void main() {
    vec2 o = HalfTexel * Offset;
    vec4 sum = texture(InSampler, texCoord + vec2(-o.x * 2.0, 0.0));
    sum += texture(InSampler, texCoord + vec2(-o.x, o.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, o.y * 2.0));
    sum += texture(InSampler, texCoord + vec2(o.x, o.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(o.x * 2.0, 0.0));
    sum += texture(InSampler, texCoord + vec2(o.x, -o.y)) * 2.0;
    sum += texture(InSampler, texCoord + vec2(0.0, -o.y * 2.0));
    sum += texture(InSampler, texCoord + vec2(-o.x, -o.y)) * 2.0;
    fragColor = vec4(sum.rgb / 12.0, 1.0);
}
