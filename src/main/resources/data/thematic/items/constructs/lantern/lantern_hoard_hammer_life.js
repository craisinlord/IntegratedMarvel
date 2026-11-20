item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_hammer",
        durability: 60,
        attack_damage: 33.0,
        attack_speed: -3.0,
        knockback: 1.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 320, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("life_ringCharge", { cost: 150, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 10 });
        go.targets("around", { radius: 5 }, crushed => {
            crushed.damage({ damage: 16, source: "player_attack" });
            crushed.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
            crushed.push("target", { direction: "away_from_owner", strength: 0.6, lift: 0.8 });
        });
        go.particles("minecraft:explosion", { count: 3, spread: [2.0, 0.1, 2.0], speed: 0, yOffset: 0.2 });
        go.sound("minecraft:block.anvil.land", { pitch: 0.7 });
        go.wear(2);
    }));
});
