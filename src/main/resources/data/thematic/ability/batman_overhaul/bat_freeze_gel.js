const GEL = "thematic:gadget_ice_grenade";
const FROST = "#BFEFFF";

function chill(target) {
    target.effect("minecraft:slowness", { duration: tuned("slowness_duration") });
    target.effect("thematic:frozen", { duration: tuned("frozen_duration"), amplifier: tuned("frozen_amplifier"), showParticles: false });
    target.particles("minecraft:snowflake", { count: 6, spread: 0.3, speed: 0.02, yOffset: 1 });
}

function shatter(gel) {
    gel.sound("alexscaves:frostmint_spear_hit", { volume: 1.1, world: true });
    gel.sound("minecraft:block.glass.break", { volume: 0.6, pitch: 1.4, world: true });
    gel.vfx("thematic:bizarro_frost_burst", { radius: tuned("radius", 0.5) });
    gel.vfx("thematic:powergirl_frost_mist", { radius: tuned("radius"), duration: tuned("duration") });
    gel.particles("minecraft:snowflake", { count: 60, spread: [1.5, 0.6, 1.5], speed: 0.05, yOffset: 0.4 });
    gel.splash(FROST, { pool: { radius: 1.5 } });
    gel.transformBlock({ "minecraft:water": "minecraft:frosted_ice" }, {
        at: "pos", shape: "circle", radius: tuned("radius"), height: 3, below: 2, only: "still_source"
    });
    gel.burst({ at: "here", radius: tuned("radius"), element: "ice" }, frozen => {
        chill(frozen);
        frozen.markCaught("bat_freeze_gel");
    });
    gel.zone({ floor: true, radius: tuned("radius"), height: 2, every: tuned("zone_every"), for: tuned("duration"), caught: "bat_freeze_gel" }, standing => {
        standing.damage({ damage: tuned("zone_damage"), element: "ice" });
        chill(standing);
    }, chill);
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: GEL });

    component.press(press => {
        press.animation("gadget_throw");
        press.sound("minecraft:entity.snowball.throw", { pitch: 0.7, world: true });
        press.then("throwing");
    });

    component.phase("throwing", throwing => {
        throwing.use("thematic:equip", { item: GEL, slot: "mainhand", displaced: "stash", keep: true });
        throwing.lasts(tuned("release_at"));
        throwing.then("cooldown", thrown => {
            thrown.sound("minecraft:entity.splash_potion.throw", { world: true });
            thrown.projectile({
                body: "thrown",
                throwable: GEL,
                from: { point: "eyes", offset: [0, -0.15, 0] },
                speed: tuned("projectile_speed"),
                pitch: -3,
                fuse: tuned("projectile_fuse"),
                trail: { particle: "minecraft:snowflake", count: 2, spread: 0.05 }
            }, shatter, shatter);
        });
    });
});
