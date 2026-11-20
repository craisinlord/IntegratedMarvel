const FLARE = "thematic:hunter_flare";
const UNDEAD = { target: { entity: "#minecraft:undead" } };

function scorch(target) {
    target.when(UNDEAD, undead => {
        undead.damage({ element: "fire" });
        undead.ignite({ seconds: tuned("ignite_seconds"), exact: true });
        undead.vfx("thematic:ember_flare", { radius: 0.6 });
        undead.sound("minecraft:entity.generic.burn", { at: "target", volume: 0.5, world: true });
    });
}

function burst(flare) {
    flare.sound("minecraft:item.firecharge.use", { volume: 1.2, world: true });
    flare.sound("minecraft:block.fire.ambient", { volume: 1, pitch: 1.4, world: true });
    flare.vfx("thematic:hunt_flare", { radius: tuned("radius"), color: "#E0FFE7A8" });
    flare.vfx("thematic:ember_flare", { radius: 0.8, height: 2.4 });
    flare.particles("minecraft:end_rod", { count: 50, spread: [2, 1.2, 2], speed: 0.03, yOffset: 0.6 });
    flare.particles("minecraft:flame", { count: 24, spread: [0.6, 0.3, 0.6], speed: 0.04, yOffset: 0.3 });
    flare.lightArea({ at: "pos", face: true, level: 15, shape: "circle", height: 4 });
    flare.zone({ radius: tuned("radius"), height: 4, every: tuned("zone_every"), for: tuned("duration"), hittable: true }, scorch);
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: FLARE });

    component.press(press => {
        press.animation("gadget_throw2");
        press.sound("minecraft:item.flintandsteel.use", { world: true });
        press.then("throwing");
    });

    component.phase("throwing", throwing => {
        throwing.use("thematic:equip", { item: FLARE, slot: "mainhand", displaced: "stash", keep: true });
        throwing.lasts(tuned("release_at"));
        throwing.particles("minecraft:flame", { count: 6, spread: 0.05, speed: 0.01, from: "main_hand" });
        throwing.then("cooldown", thrown => {
            thrown.sound("minecraft:entity.snowball.throw", { pitch: 0.8, world: true });
            thrown.projectile({
                body: "thrown",
                throwable: "thematic:thrown_construct",
                carries: FLARE,
                from: { point: "eyes", offset: [0, -0.15, 0] },
                speed: tuned("projectile_speed"),
                gravity: tuned("projectile_gravity"),
                pitch: -4,
                maxAge: tuned("projectile_max_age"),
                trail: [
                    { particle: "minecraft:flame", count: 2, spread: 0.05 },
                    { particle: "minecraft:end_rod", count: 1, spread: 0.05 }
                ]
            }, burst, burst);
        });
    });
});
