item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_battle_axe",
        durability: 60,
        attack_damage: 30.0,
        attack_speed: -3.0,
        knockback: 1.2,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 300, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.effect("thematic:bleed", { duration: 3, amplifier: 0 });
        hit.ignite({ seconds: 3 });
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("life_ringCharge", { cost: 120, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 8 });
        go.targets("around", { radius: 3.5 }, cut => {
            cut.damage({ damage: 18, source: "player_attack" });
            cut.effect("thematic:bleed", { duration: 3, amplifier: 1 });
            cut.ignite({ seconds: 3 });
        });
        go.particles("minecraft:flame", { count: 40, spread: [2.0, 0.3, 2.0], speed: 0.05, yOffset: 1 });
        go.sound("minecraft:item.firecharge.use", { pitch: 0.8 });
        go.wear(2);
    }));
});
