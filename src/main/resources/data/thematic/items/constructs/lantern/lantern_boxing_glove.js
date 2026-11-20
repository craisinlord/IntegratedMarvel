function atCombo(level) {
    return { stackCounter: { key: "combo", equals: level } };
}

item("thematic:melee", glove => {
    glove.use("blunt", { power_cost: 280, durability: 40, attack_damage: 25.5, attack_speed: 1.5, knockback: 1.5 });
    glove.set({ model: "thematic:construct_boxing_glove", denied_sound: "minecraft:entity.player.attack.weak" });
    glove.onUse(use => use.when({ stackTimer: "combo_window", stackCounter: { key: "combo", at_least: 3 } }, ready => {
        for (let level = 3; level <= 4; level++) {
            ready.when(atCombo(level), combo => combo.targets("around", { radius: 3, arc: 45, pick: "nearest", sort: "distance" }, target => {
                target.damage({ damage: 12 + 2 * level, source: "player_attack" });
                target.push("target", { direction: "look", flat: true, strength: 0.3, lift: [1.1, 1.2, 1.3][level - 3] });
                target.effect("minecraft:slowness", { duration: 2, amplifier: 1 });
                target.effect("minecraft:weakness", { duration: 3, amplifier: 0 });
                target.particles("minecraft:crit", { count: 20, spread: 0.5, speed: 0.2, anchor: "body" });
                target.sound("minecraft:entity.player.attack.crit", { pitch: 0.8, at: "self" });
                target.wear(2);
                target.notify("Uppercut! (" + level + "x Combo)");
            }, missed => {
                missed.sound("minecraft:entity.player.attack.sweep", { volume: 0.8, pitch: 1.2 });
                missed.particles("minecraft:sweep_attack", { count: 1, spread: 0, speed: 0, from: "eyes", forward: 1.5 });
                missed.notify("Uppercut missed!");
            }));
        }
        ready.when(atCombo(5), combo => {
            combo.projectile({
                from: { point: "eyes", offset: [0, -0.2, 0.5], relative: "look" },
                speed: 2.0,
                maxDistance: 12,
                size: 0.9,
                gravity: 0,
                directHit: "none",
                color: "$stack_color",
                look: { style: "feather", model: "thematic:geo/item/boxing_glove.geo.json", texture: "thematic:textures/item/construct.png", color: "$stack_color", layer: "beam", scale: 1.6 },
                trail: { particle: "minecraft:end_rod", count: 1, spread: 0.2 }
            }, struck => struck.when({ target: { hittable: true } }, hit => {
                hit.damage({ damage: 27, source: "player_attack" });
                hit.push("target", { direction: "away_from_owner", strength: 2.6, lift: 0.6 });
                hit.sound("minecraft:entity.player.attack.knockback", { pitch: 0.7 });
            }));
            combo.sound("minecraft:entity.firework_rocket.launch", { pitch: 0.9 });
            combo.notify("Rocket Punch!");
        });
        ready.stackState("combo", { op: "set_counter", value: 0 });
        ready.itemCooldown(10);
    }, low => low.when({ stackTimer: "combo_window" }, counting => {
        for (let level = 0; level <= 2; level++) {
            counting.when(atCombo(level), combo => combo.notify("Combo too low for Uppercut! (" + level + "/3 min)"));
        }
    }, cold => cold.notify("Combo too low for Uppercut! (0/3 min)"))));
    glove.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ stackTimer: "combo_window" },
                going => going.when({ stackCounter: { key: "combo", below: 5 } }, up => up.stackState("combo", { op: "add_counter" })),
                fresh => fresh.stackState("combo", { op: "set_counter", value: 1 }));
            swing.stackState("combo_window", { op: "start_timer", seconds: 3 });
            for (let level = 1; level <= 5; level++) {
                swing.when(atCombo(level), combo => {
                    combo.effect("minecraft:slowness", { duration: 1 + (level - 1) * 0.5, amplifier: 1 });
                    if (level >= 2) {
                        combo.effect("minecraft:mining_fatigue", { duration: 1.5, amplifier: 0 });
                    }
                    if (level >= 3) {
                        combo.effect("minecraft:weakness", { duration: 2, amplifier: 0 });
                    }
                    if (level >= 4) {
                        combo.effect("minecraft:nausea", { duration: 3, amplifier: 0 });
                    }
                    combo.selfEffect("minecraft:speed", { duration: 2, amplifier: 0, ambient: true, showParticles: false });
                    combo.particles("minecraft:crit", { count: Math.min(5 + level * 3, 20), spread: 0.4, speed: 0.1, anchor: "body" });
                    combo.sound("minecraft:entity.player.attack.weak", { volume: 0.8, pitch: [0.9, 1.0, 1.1, 1.2, 1.3][level - 1] });
                });
            }
        });
        hit.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        hit.wear(1);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.75, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    glove.onHeldTick({ every: 20 }, held => {
        held.when({ stackCounter: { key: "combo", at_least: 1 }, not: { stackTimer: "combo_window" } }, expired => {
            expired.stackState("combo", { op: "set_counter", value: 0 });
            expired.notify("Combo Reset!");
        });
        for (let level = 3; level <= 5; level++) {
            held.when({ stackTimer: "combo_window", stackCounter: { key: "combo", equals: level } },
                hot => hot.particles("minecraft:flame", { count: level - 2, spread: [0.2, 0.3, 0.2], speed: 0.01, anchor: 0.8 }));
        }
    });
});
