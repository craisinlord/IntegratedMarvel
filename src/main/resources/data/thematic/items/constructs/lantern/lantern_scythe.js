item("thematic:melee", scythe => {
    scythe.use("blade", { power_cost: 350, durability: 28, attack_damage: 34.5, attack_speed: 0.4, knockback: 1.5 });
    scythe.set({ denied_sound: "minecraft:entity.wither.ambient", reach: 0.667, model: "thematic:scythe" });
    scythe.onUse(use => {
        use.itemCooldown(8);
        use.swingHand("main");
        use.targets("around", { radius: 4.5, arc: 120 }, soul => {
            soul.damage({ damage: 30, source: "player_attack", ignoreIframes: true });
            soul.heal(20);
            soul.particles(dust("$stack_color", 1.0), { count: 10, spread: [0.2, 0.5, 0.2], speed: 0.05, yOffset: 1 });
            soul.particles(streak("$stack_color", 1, 2), { count: 8, spread: 0.3, speed: 0.5, yOffset: 1 });
            soul.particles("minecraft:sweep_attack", { count: 1, spread: 0, speed: 0, yOffset: 1 });
            soul.particles(dust("$stack_color", 0.6), { shape: "line", count: 12 });
        }, none => none.notify("No souls to harvest!"));
        use.sound("minecraft:entity.player.attack.sweep", { volume: 1.0, pitch: 0.7 });
        use.sound("minecraft:entity.wither.ambient", { volume: 0.5, pitch: 1.4 });
        use.particleCircle("unfilled_circle", { color: "$stack_color", radius: 4.5, count: 60, height: 0.1, yOffset: 0.1 });
    });
    scythe.onHit(hit => {
        hit.when({ chance: 0.25 }, bleed => {
            bleed.effect("thematic:bleed", { duration: 2, amplifier: 0 });
            bleed.particles("minecraft:damage_indicator", { count: 5, spread: [0.2, 0.3, 0.2], speed: 0.05, yOffset: 1 });
        });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.75, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    scythe.onHeldTick({ every: 20 }, held => held.when({ chance: 0.3 },
        ambient => ambient.particles(dust("$stack_color", 0.8), { count: 1, spread: [0.25, 0.1, 0.25], speed: 0.01, yOffset: 0.5 })));
});
