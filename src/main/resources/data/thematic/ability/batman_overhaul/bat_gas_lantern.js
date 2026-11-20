const LANTERN = "minecraft:lantern";

function smash(lantern) {
    lantern.sound("minecraft:block.glass.break", { volume: 1, pitch: 0.9, world: true });
    lantern.sound("minecraft:item.firecharge.use", { volume: 0.8, pitch: 1.2, world: true });
    lantern.vfx("thematic:ignition_burst", { radius: tuned("flash_radius") });
    lantern.particles("minecraft:flame", { count: 30, spread: [0.8, 0.4, 0.8], speed: 0.06, yOffset: 0.3 });
    lantern.particles("minecraft:lava", { count: 6, spread: 0.3, yOffset: 0.3 });
    lantern.lightArea({ at: "pos", face: true, level: 15, shape: "circle", height: 3 });
    lantern.targets("around", { at: "source", radius: tuned("flash_radius"), lineOfSight: true }, seen => {
        seen.when({ target: { mob: true } }, mob => {
            mob.effect("minecraft:blindness", { duration: tuned("blindness_duration") });
            mob.effect("minecraft:slowness", { duration: tuned("blindness_duration"), amplifier: tuned("slowness_amplifier") });
            mob.calm({ hostileOnly: true });
        });
    });
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: LANTERN });

    component.press(press => {
        press.animation("gadget_throw");
        press.sound("minecraft:block.lantern.break", { world: true });
        press.then("throwing");
    });

    component.phase("throwing", throwing => {
        throwing.use("thematic:equip", { item: LANTERN, slot: "mainhand", displaced: "stash", keep: true });
        throwing.lasts(tuned("release_at"));
        throwing.then("cooldown", thrown => {
            thrown.sound("minecraft:entity.snowball.throw", { pitch: 0.6, world: true });
            thrown.projectile({
                body: "thrown",
                throwable: "thematic:thrown_construct",
                carries: LANTERN,
                from: { point: "eyes", offset: [0, -0.15, 0] },
                speed: tuned("projectile_speed"),
                gravity: tuned("projectile_gravity"),
                pitch: -4,
                maxAge: tuned("projectile_max_age"),
                trail: { particle: "minecraft:small_flame", count: 1, spread: 0.03 }
            }, smash, smash);
        });
    });
});
