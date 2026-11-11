#version 150

uniform sampler2D DiffuseSampler;
uniform float Throttle;
uniform float RippleTime;
uniform float Time;
uniform float TakeoffShake;
uniform float ShakeScale;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 baseUv = texCoord;
    vec2 center = vec2(0.5, 0.5);

    float safeThrottle = clamp(Throttle, 0.0, 1.0);

    float throttleShakeIntensity = 0.0;
    if (safeThrottle > 0.0 && safeThrottle < 0.2) {
        throttleShakeIntensity = (0.2 - safeThrottle) * 5.0 * 0.005;
    } else if (safeThrottle > 0.6) {
        throttleShakeIntensity = (safeThrottle - 0.6) * 0.0035;
    }
    throttleShakeIntensity *= ShakeScale;

    float takeoffShake = TakeoffShake * ShakeScale;
    float takeoffShakeOffset = 0.0;
    if (takeoffShake > 0.0) {
        takeoffShakeOffset = sin(Time * 150.0) * 0.02 * takeoffShake;
    }

    if (throttleShakeIntensity > 0.0 || takeoffShake > 0.0) {
        baseUv.x += sin(Time * 60.0) * throttleShakeIntensity;
        baseUv.y += cos(Time * 75.0) * throttleShakeIntensity;
        baseUv.x += takeoffShakeOffset;
        baseUv.y -= takeoffShakeOffset;
    }

    float sonicFactor = max(0.0, (safeThrottle - 0.6) / 0.4);
    vec4 finalColor;

    if (sonicFactor > 0.0 && distance(baseUv, center) > 0.001) {
        float caFade = max(0.0, 1.0 - (RippleTime * 1.0));
        float caOffset = 0.008 * sonicFactor * caFade * distance(baseUv, center);
        vec2 dir = normalize(baseUv - center);

        float r = texture(DiffuseSampler, baseUv + dir * caOffset).r;
        float g = texture(DiffuseSampler, baseUv).g;
        float b = texture(DiffuseSampler, baseUv - dir * caOffset).b;

        finalColor = vec4(r, g, b, 1.0);
    } else {
        finalColor = texture(DiffuseSampler, baseUv);
    }

    finalColor.a = 1.0;
    fragColor = finalColor;
}
