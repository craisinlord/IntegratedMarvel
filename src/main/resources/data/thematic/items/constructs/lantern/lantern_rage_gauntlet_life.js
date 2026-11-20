item("thematic:melee", construct => {
    construct.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct_boxing_glove",
        durability: 60,
        attack_damage: 22.0,
        attack_speed: -1.0,
        knockback: 1.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 220, tint: "corps" }
    });
    construct.onHit(hit => {
        hit.ignite({ seconds: 2 });
        hit.wear(1);
    });
    construct.onUse(use => use.when({ stackTimer: "rest" }, rest => rest.notify("Recovering", { color: "red" }), go => {
        go.meter("life_ringCharge", { cost: 80, failMessage: "Not enough power!" });
        go.stackState("rest", { op: "start_timer", seconds: 5 });
        go.targets("look", { range: 5, width: 1.0, pick: "nearest" }, struck => {
            struck.damage({ damage: 30, source: "player_attack" });
            struck.push("target", { direction: "away_from_owner", strength: 2.4, lift: 0.5 });
            struck.ignite({ seconds: 4 });
        }, missed => missed.notify("Nothing in reach"));
        go.sound("minecraft:entity.player.attack.strong", { pitch: 0.6 });
        go.wear(2);
    }));
});
