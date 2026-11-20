item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_spear",
        durability: 60,
        attack_damage: 16.0,
        attack_speed: -1.8,
        knockback: 0.8,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 200, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.heal(2);
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("compassion_ringCharge", { cost: 60, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 6 });
        go.targets("look", { range: 10, width: 1.0, pick: "nearest" }, grabbed => {
            grabbed.push("target", { direction: "toward_owner", strength: 2.0, lift: 0.3 });
            grabbed.effect("minecraft:weakness", { duration: 5, amplifier: 0 });
            grabbed.particles(dust("$stack_color", 1.0), { count: 20, spread: 0.4, speed: 0.05, yOffset: 1 });
        }, missed => missed.notify("Nothing to lash"));
        go.sound("minecraft:entity.fishing_bobber.retrieve", { pitch: 0.7 });
        go.wear(2);
    }));
});
