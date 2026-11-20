item("thematic:melee", blade => {
    blade.use("blade", { power_cost: 436, durability: 36, attack_damage: 43.5, attack_speed: 1.5, knockback: 0.2 });
    blade.set({ model: "thematic:construct_blade", denied_sound: "minecraft:entity.player.attack.sweep", reach: 1.17 });
    blade.onUse(use => use.targets("around", { box: [8, 4, 8], hittable: false, lineOfSight: true, pick: "nearest", sort: "distance" }, target => {
        target.itemCooldown(4);
        target.push("self", { direction: "toward_target", flat: true, strength: 0.8, lift: 0.2 });
        target.selfEffect("minecraft:speed", { duration: 1.5, amplifier: 1, showParticles: false });
        target.particles(dust([0.8, 0.0, 0.0], 1.0), { system: "streak", aim: "look", from: "chest", spread: 0.1, speed: 0 });
        target.notify("Lunged toward target!");
        target.sound("minecraft:entity.player.attack.sweep", { volume: 0.8, pitch: 1.4, at: "self" });
    }, none => none.notify("No target in range")));
    blade.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ stackTimer: "chain_window" },
                going => going.when({ stackCounter: { key: "chain", below: 5 } }, up => up.stackState("chain", { op: "add_counter" })),
                fresh => fresh.stackState("chain", { op: "set_counter", value: 1 }));
            swing.stackState("chain_window", { op: "start_timer", seconds: 3 });
            for (let links = 1; links <= 5; links++) {
                swing.when({ stackCounter: { key: "chain", equals: links } }, chain => {
                    chain.when({ chance: [0.5, 0.6, 0.7, 0.8, 0.9][links - 1] }, bleed => {
                        bleed.effect("minecraft:slowness", { duration: 4 + links, amplifier: 1 });
                        bleed.effect("thematic:bleed", { duration: [2.5, 3, 3.5, 4, 4.5][links - 1], amplifier: 0 });
                        bleed.particles(dust([0.8, 0.0, 0.0], 1.0), { count: 20, spread: [0.3, 0.5, 0.3], speed: 0.1, yOffset: 1 });
                        bleed.particles("minecraft:damage_indicator", { count: links * 2, spread: [0.1, 0.5, 0.1], speed: 0.05, yOffset: 1 });
                        bleed.sound("minecraft:entity.generic.hurt", { volume: 0.5, pitch: 1.5 });
                        if (links === 2) {
                            bleed.notify("Bleeding inflicted");
                        } else if (links >= 3) {
                            bleed.notify("Bleeding inflicted (x" + links + ")");
                        }
                    });
                    if (links > 1) {
                        chain.particles("minecraft:crit", { count: links * 3, spread: [0.2, 0.5, 0.2], speed: 0.1, yOffset: 1 });
                        chain.sound("minecraft:entity.player.attack.sweep", { volume: 0.5, pitch: [1.2, 1.3, 1.4, 1.5][links - 2] });
                    }
                });
            }
        });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.1, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    blade.onHeldTick({ every: 20 }, held => held.when({ stackCounter: { key: "chain", at_least: 1 } }, chained => chained.when({ stackTimer: "chain_window" },
        live => live.particles(dust([0.8, 0.0, 0.0], 0.8), { count: 1, spread: 0.1, speed: 0, from: "main_hand" }),
        lapsed => {
            lapsed.when({ stackCounter: { key: "chain", at_least: 3 } }, long => long.notify("Combo Reset"));
            lapsed.stackState("chain", { op: "set_counter", value: 0 });
        })));
});
