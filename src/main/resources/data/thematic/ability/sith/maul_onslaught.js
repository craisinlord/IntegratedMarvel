const BOSS = { target: { entity: "#c:bosses" } };

component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:netherite_sword", stunImmune: true });

    component.press(press => {
        press.sound("minecraft:entity.ravager.roar", { volume: 0.8, pitch: 0.8, world: true });
        press.sound("opposing_force:laser_blade_spin", { volume: 1.2, pitch: 0.8, world: true });
        press.particles("minecraft:large_smoke", { count: 30, spread: [0.6, 1, 0.6], speed: 0.02, yOffset: 1 });
        press.then("onslaught");
    });

    component.phase("onslaught", phase => {
        phase.lasts("duration");
        phase.traits({ stunImmune: true, immune: { damage: ["#minecraft:is_fall"], grace: 1 } });
        phase.every({ clock: "phase", ticks: 8, start: 0.1 }, strike => {
            strike.targets("around", { radius: tuned("radius"), round: true, lineOfSight: true, pick: "nearest" }, foe => {
                foe.particles("minecraft:large_smoke", { anchor: "body", count: 8, spread: 0.3, speed: 0.02 });
                foe.teleport("behind_target", { safe: true, distance: 1.2 });
                foe.lookAt();
                foe.animation("x_slash", { alternate: "slashing_frenzy", stopAfter: 0.4 });
                foe.when(BOSS, boss => boss.damage({ damage: tuned("damage", 0.5), source: "player_attack" }),
                    other => other.damage({ source: "player_attack" }));
                foe.sound("opposing_force:laser_blade_swing", { volume: 1.1, pitch: [0.9, 1.2], world: true });
                foe.particles(dust([0.8, 0.05, 0.05], 1), { anchor: "body", count: 10, spread: 0.3, speed: 0.05 });
                foe.particles("minecraft:sweep_attack", { anchor: "body", count: 1 });
            });
        });
    });
});
