const PING = "bat_sonar_ping";
const RAYS = 12;

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:heart_of_the_sea" });
    component.requires({ inWater: true });

    component.press(press => {
        press.animation("sonar_tracer");
        press.sound("alexscaves:submarine_sonar", { volume: 1.4, world: true });
        for (let ray = 0; ray < RAYS; ray++) {
            let angle = ray * Math.PI * 2 / RAYS;
            press.vfx("thematic:sonar_beam", { from: "chest", direction: [Math.cos(angle), 0, Math.sin(angle)], height: tuned("radius", 0.25) });
        }
        press.particleDome("expanding_ring", { particle: "minecraft:bubble_pop", radius: tuned("radius", 0.25), height: 1, count: 120 });
        press.particles("minecraft:nautilus", { count: 40, spread: 1.2, speed: 0.4, yOffset: 1 });
        press.targets("around", { radius: tuned("radius"), round: true, hittable: false }, found => {
            found.when({ target: { inWater: true } }, wet => {
                wet.mark(PING, { for: tuned("duration"), glow: true, seenBy: "owner" });
                wet.particles("minecraft:bubble_column_up", { count: 8, spread: 0.3, speed: 0.05 });
            });
        });
        press.wait(0.6);
        press.sound("alexscaves:submarine_sonar", { volume: 0.6, pitch: 1.3, world: true });
    });
});
