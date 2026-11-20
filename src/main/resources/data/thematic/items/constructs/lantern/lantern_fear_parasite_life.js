item("thematic:melee", parasite => {
    parasite.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:fear_effect",
        durability: 30,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 220, tint: "corps" }
    });
    parasite.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    parasite.onUse(use => use.when({ stackTimer: "parasite_rest" }, rest => rest.notify("Fear Parasite recovering", { color: "red" }), latch => {
        latch.meter("life_ringCharge", { cost: 80, failMessage: "Not enough power!" });
        latch.stackState("parasite_rest", { op: "start_timer", seconds: 8 });
        latch.targets("look", { range: 12, width: 1.0, pick: "nearest" }, host => {
            host.effect("thematic:yellow_flashed", { duration: 6, amplifier: 0 });
            host.effect("minecraft:slowness", { duration: 6, amplifier: 1 });
            host.effect("minecraft:weakness", { duration: 6, amplifier: 0 });
            host.push("target", { direction: "away_from_owner", flat: true, strength: 1.2, lift: 0.2 });
            host.particles(dust("$stack_color", 1.0), { count: 20, spread: [0.3, 0.6, 0.3], speed: 0.05, yOffset: 1 });
            host.sound("minecraft:entity.silverfish.ambient", { pitch: 0.6 });
        }, missed => missed.notify("Nothing to latch onto"));
        latch.wear(2);
    }));
});
