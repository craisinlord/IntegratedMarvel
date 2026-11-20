const STAFF = "thematic:maul_saberstaff";

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:blaze_rod" });
    component.requires({ heldItem: STAFF }, { message: "integrated_superheroes.sith.saber_needed" });

    component.press(press => {
        press.sound("opposing_force:laser_blade_spin", { volume: 1.3, pitch: 1, world: true });
        press.then("whirling");
    });

    component.phase("whirling", phase => {
        phase.lasts("duration");
        phase.animation("spin", { fadeIn: 0.05, priority: 2 });
        phase.traits({ deflect: { projectiles: true, reverse: true, take_ownership: true } });
        phase.every({ clock: "phase", ticks: 10, start: 0.5 }, again => again.animation("spin", { priority: 2 }));
        phase.every({ clock: "phase", ticks: 12, start: 0.6 }, hum => hum.sound("opposing_force:laser_blade_spin", { volume: 1, pitch: [0.95, 1.1], world: true }));
        phase.every({ clock: "phase", ticks: 5 }, pulse => {
            pulse.particleCircle("spinning_ring",{ particle: "minecraft:sweep_attack", height: 0, radius: tuned("radius", 0.8), count: 6, yOffset: 1 });
            pulse.targets("around", { radius: tuned("radius"), round: true }, foe => {
                foe.damage({ source: "player_attack" });
                foe.push("target", { direction: "away_from_owner", strength: tuned("push_strength") });
                foe.particles("minecraft:crit", { anchor: "body", count: 4, spread: 0.3, speed: 0.1 });
            });
        });
        phase.until({ not: { heldItem: STAFF } }, "cooldown");
        phase.onExit(exit => exit.stopAnimation("spin"));
    });
});
