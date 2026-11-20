item("thematic:melee", spear => {
    spear.use("blade", { power_cost: 334, durability: 22, attack_damage: 31.5, attack_speed: 0.5, knockback: 1.0 });
    spear.set({
        model: "thematic:construct_spear",
        denied_sound: "minecraft:entity.player.attack.sweep",
        reach: 2.23,
        use_behaviour: {
            type: "trident",
            full_ticks: 20,
            cooldown: 3,
            throw: { throwable: "thematic:construct_spear", velocity: 1.75, velocity_per_tick: 0.175, divergence: 0.5 }
        }
    });
    spear.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ within: 5 }, close => {
                close.when({ chance: 0.3 }, critical => {
                    critical.damage({ damage: 15.75, source: "player_attack" });
                    critical.particles("minecraft:crit", { count: 20, spread: 0.3, speed: 0.3, yOffset: 1 });
                    critical.sound("minecraft:entity.player.attack.crit");
                    critical.notify("Critical Strike!");
                });
                close.effect("minecraft:weakness", { duration: 2, amplifier: 0 });
            });
            swing.when({ chance: 0.2 }, bleed => {
                bleed.effect("thematic:bleed", { duration: 3, amplifier: 0 });
                bleed.particles("minecraft:damage_indicator", { count: 10, spread: [0.2, 0.3, 0.2], speed: 0.1, yOffset: 1 });
            });
        });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(2);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    spear.onHeldTick({ every: 40 }, held => held.particles("minecraft:firework", { count: 1, spread: 0.2, speed: 0.05, yOffset: 1.5 }));
});
