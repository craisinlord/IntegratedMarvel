item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:scythe",
        durability: 60,
        attack_damage: 30.0,
        attack_speed: -2.6,
        knockback: 1.0,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 340, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.heal(3);
        hit.effect("minecraft:wither", { duration: 3, amplifier: 0 });
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("death_ringCharge", { cost: 140, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 10 });
        go.targets("around", { radius: 4 }, reaped => {
            reaped.damage({ damage: 20, source: "player_attack" });
            reaped.effect("minecraft:wither", { duration: 4, amplifier: 1 });
        });
        go.heal(6);
        go.particles("minecraft:soul", { count: 30, spread: [2.0, 0.5, 2.0], speed: 0.03, yOffset: 1 });
        go.sound("minecraft:particle.soul_escape", { pitch: 0.6 });
        go.wear(2);
    }));
});
