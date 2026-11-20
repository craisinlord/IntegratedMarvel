component(component => {
    component.cooldown({ from: "press" });
    component.traits({ immuneTo: ["minecraft:fall"], wheelIcon: "thematic:gadget_netherite_batarang" });

    component.press(press => {
        press.store("drop", { value: 0 });
        press.sound("minecraft:entity.breeze.jump", { volume: 1.2, pitch: 0.8, world: true });
        press.animation("bat_slam");
        press.vfx("thematic:impact_dust", { radius: 1.2 });
        press.particleCircle("ground_slam", { particle: "minecraft:large_smoke", height: 1, radius: 0.5, count: 30 });
        press.push("self", { lift: tuned("push_lift") });
        press.then("airborne");

        press.wait(1.35);
        press.sound("minecraft:entity.player.attack.sweep", { pitch: 0.5, world: true });
        press.push("self", { lift: tuned("push_lift_2") });
    });

    component.phase("airborne", airborne => {
        airborne.lasts(tuned("lasts"));
        airborne.every(every => every.store("drop", { value: input("fall_distance"), mode: "max" }));
        airborne.on("land", slam => {
            slam.sound("minecraft:item.mace.smash_ground_heavy", { volume: 1.4, world: true });
            slam.sound("minecraft:entity.generic.explode", { volume: 0.7, pitch: 0.6, world: true });
            slam.vfx("thematic:ground_slam", { radius: tuned("radius") });
            slam.vfx("thematic:slam_fracture_large");
            slam.vfx("thematic:impact_dust", { radius: tuned("radius", 0.5) });
            slam.particleDome("atmospheric", { particle: "minecraft:large_smoke", height: 1, count: 96 });
            slam.shake({ radius: 16, strength: 2.2, duration: 0.6 });
            slam.targets("around", target => {
                target.damage({ damage: expr("damage * (1 + min($drop, @drop_cap) * @drop_bonus)"), source: "player_attack" });
                target.effect("minecraft:slowness", { duration: tuned("slowness_duration") });
                target.push("target", { lift: tuned("push_lift_3") });
                target.particles("minecraft:crit", { count: 10, spread: 0.3 });
            });
            slam.then("cooldown");
        });
    });
});
