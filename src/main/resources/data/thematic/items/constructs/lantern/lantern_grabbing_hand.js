item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct",
        durability: 60,
        attack_damage: 8.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 180, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("avarice_ringCharge", { cost: 60, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 6 });
        go.targets("look", { range: 14, width: 1.0, pick: "nearest" }, grabbed => {
            grabbed.push("target", { direction: "toward_owner", strength: 2.0, lift: 0.3 });
            grabbed.effect("minecraft:weakness", { duration: 4, amplifier: 1 });
            grabbed.particles(dust("$stack_color", 1.0), { count: 20, spread: 0.4, speed: 0.05, yOffset: 1 });
        }, missed => missed.notify("Nothing to grab"));
        go.sound("minecraft:entity.fishing_bobber.retrieve", { pitch: 0.7 });
        go.wear(2);
    }));
});
