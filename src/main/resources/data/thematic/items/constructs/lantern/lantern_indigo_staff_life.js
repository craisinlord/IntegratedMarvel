item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_baton",
        durability: 60,
        attack_damage: 14.0,
        attack_speed: -1.6,
        knockback: 0.6,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 250, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("life_ringCharge", { cost: 100, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 5 });
        go.targets("look", { range: 12, width: 1.2, pick: "nearest" }, mended => {
            mended.heal(8, { target: "hit" });
            mended.particles("minecraft:heart", { count: 6, spread: 0.4, speed: 0.05, yOffset: 1.5 });
        }, alone => alone.heal(4));
        go.sound("minecraft:block.amethyst_block.chime", { pitch: 1.2 });
        go.wear(2);
    }));
});
