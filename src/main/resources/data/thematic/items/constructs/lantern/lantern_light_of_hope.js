item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_lighter",
        durability: 60,
        attack_damage: 10.0,
        attack_speed: -1.5,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 200, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("hope_ringCharge", { cost: 120, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 12 });
        go.selfEffect("minecraft:regeneration", { duration: 6, amplifier: 1 });
        go.targets("allies", ally => ally.effect("minecraft:regeneration", { duration: 6, amplifier: 1 }));
        go.particles("minecraft:end_rod", { count: 30, spread: [3.0, 1.0, 3.0], speed: 0.02, yOffset: 1 });
        go.sound("minecraft:block.beacon.power_select", { pitch: 1.6 });
        go.wear(2);
    }));
});
