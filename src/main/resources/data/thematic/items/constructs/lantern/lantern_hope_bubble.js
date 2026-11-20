item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_shield",
        durability: 60,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.8,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 250, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("hope_ringCharge", { cost: 200, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 15 });
        go.removeEffect("minecraft:poison");
        go.removeEffect("minecraft:wither");
        go.removeEffect("minecraft:slowness");
        go.removeEffect("minecraft:weakness");
        go.selfEffect("minecraft:resistance", { duration: 5, amplifier: 2 });
        go.targets("allies", ally => ally.effect("minecraft:resistance", { duration: 5, amplifier: 2 }));
        go.particleDome("full_dome", { particle: dust("$stack_color", 1.0), radius: 4, count: 80 });
        go.sound("minecraft:block.beacon.activate", { pitch: 1.4 });
        go.wear(2);
    }));
});
