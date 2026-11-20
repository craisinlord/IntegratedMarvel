item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_katana",
        durability: 60,
        attack_damage: 26.0,
        attack_speed: -2.0,
        knockback: 0.8,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 260, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.when({ chance: 0.25 }, held => held.effect("thematic:immobile", { duration: 1, amplifier: 0 }));
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("life_ringCharge", { cost: 100, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 12 });
        go.selfEffect("minecraft:absorption", { duration: 10, amplifier: 1 });
        go.particles(dust("$stack_color", 1.2), { count: 30, spread: [0.6, 1.0, 0.6], speed: 0.05, yOffset: 1 });
        go.sound("minecraft:block.amethyst_cluster.place", { pitch: 0.9 });
        go.wear(2);
    }));
});
