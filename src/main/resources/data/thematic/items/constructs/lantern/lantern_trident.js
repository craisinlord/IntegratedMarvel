function frenzyJab(jab, infused) {
    jab.swingHand("main");
    jab.sound("minecraft:item.trident.hit", { volume: 0.6, pitch: 1.4 });
    jab.particles("minecraft:crit", { from: "chest", forward: 1, count: 6, spread: 0.3, speed: 0.1 });
    jab.particles(streak("$stack_color", 0.8, 2), { from: "chest", forward: 1, count: 4, spread: 0.2, speed: 0.5 });
    jab.targets("around", { radius: 1.25, ahead: 1 }, pierced => {
        pierced.damage({ damage: 6, source: "player_attack", ignoreIframes: true });
        pierced.particles("minecraft:crit", { count: 8, spread: 0.2, speed: 0.2, anchor: "body" });
        if (infused) {
            pierced.once("pass", first => {
                first.burst({
                    at: "impact",
                    radius: 1.5,
                    damage: 0,
                    knockback: { strength: 0.4, lift: 0.1 },
                    particles: [
                        { particle: dust("$stack_color", 1.5), count: 25, spread: 0.6, speed: 0.1, yOffset: 1 },
                        { particle: streak("$stack_color", 1, 2), count: 12, spread: 0.3, speed: 0.6, yOffset: 1 },
                        { particle: "minecraft:flash", count: 1, spread: 0, speed: 0, yOffset: 1 }
                    ],
                    sound: { sound: "minecraft:entity.generic.explode", volume: 0.5, pitch: 1.6 }
                });
                first.targets("around", { radius: 1.5, at: "target" }, blasted =>
                    blasted.damage({ damage: 4, source: "player_attack", ignoreIframes: true }));
            });
        }
    });
}

item("thematic:melee", trident => {
    trident.use("blade", { power_cost: 342, durability: 30, attack_damage: 33.0, attack_speed: 0.5, knockback: 1.5 });
    trident.set({ model: "thematic:construct_trident", denied_sound: "minecraft:item.shield.block", reach: 0.334 });
    trident.onUse(use => {
        use.itemCooldown(5);
        use.wear(3);
        use.stackState("frenzy", { op: "start_timer", seconds: 1.2 });
        use.when({ chance: 0.15 }, infused => {
            infused.stackState("infused", { op: "set_flag" });
            infused.notify("Willpower-infused frenzy!", { color: "green" });
            infused.sound("minecraft:block.beacon.power_select", { volume: 0.6, pitch: 1.6 });
        }, plain => plain.stackState("infused", { op: "clear_flag" }));
        use.sound("minecraft:item.trident.riptide_1", { volume: 0.6, pitch: 1.5 });
    });
    trident.onHeldTick({ every: 4 }, held => held.when({ stackTimer: "frenzy" }, jab => jab.when({ stackFlag: "infused" },
        infused => frenzyJab(infused, true),
        plain => frenzyJab(plain, false))));
    trident.onHit(hit => {
        hit.wear(1);
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(2);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.75, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
});
