function launch(actions) {
    actions.push("self", {
        direction: "look",
        strength: expr("@leap_base + min(state_ticks, @charge_ticks) * @leap_per_tick"),
        lift: tuned("leap_lift"),
        mode: "set",
        resetFall: true,
    });
    actions.animation("force_leap", { fadeIn: 0.05 });
    actions.sound("minecraft:entity.breeze.jump", { volume: 1.2, pitch: 0.8, world: true });
    actions.vfx("thematic:slam_dust", { radius: 1 });
    actions.particles("minecraft:cloud", { anchor: "feet", count: 12, spread: [0.4, 0.05, 0.4], speed: 0.05 });
}

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:rabbit_foot" });

    component.hold(hold => {
        hold.sound("opposing_force:laser_blade_swing", { volume: 0.8, pitch: 0.7, world: true });
        hold.then("charging");
    });

    component.phase("charging", phase => {
        phase.lasts(tuned("charge_lasts"));
        phase.animation("feral_leap", { fadeIn: 0.1, hold: "state" });
        phase.every(0.2, every => {
            every.particles("minecraft:large_smoke", { anchor: "feet", count: 3, spread: [0.3, 0.05, 0.3], speed: 0.01 });
        });
        phase.on("release", release => {
            launch(release);
            release.then("leaping");
        });
        phase.then("leaping", lapse => launch(lapse));
        phase.onExit(exit => exit.stopAnimation("feral_leap"));
    });

    component.phase("leaping", phase => {
        phase.lasts(tuned("leap_lasts"));
        phase.traits({ immune: { damage: ["#minecraft:is_fall"], grace: 0.5 } });
        phase.every(every => {
            every.particles("minecraft:large_smoke", { anchor: "body", count: 1, spread: 0.2, speed: 0.01 });
        });
        phase.on("land", slam => {
            slam.sound("opposing_force:laser_blade_swing", { volume: 1.2, pitch: 0.9, world: true });
            slam.sound("minecraft:entity.player.attack.sweep", { volume: 1, pitch: 0.7, world: true });
            slam.vfx("thematic:impact_dust", { radius: tuned("radius", 0.6) });
            slam.particles("minecraft:sweep_attack", { count: 6, spread: [1, 0.2, 1], yOffset: 0.6 });
            slam.targets("around", { radius: tuned("radius"), round: true }, foe => {
                foe.damage({ source: "player_attack" });
                foe.push("target", { direction: "away_from_owner", strength: tuned("push_strength"), lift: tuned("push_lift") });
                foe.particles("minecraft:crit", { anchor: "body", count: 8, spread: 0.3, speed: 0.1 });
            });
            slam.then("cooldown");
        });
    });
});
