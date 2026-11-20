item("thematic:melee", rod => {
    rod.set({
        model: "thematic:construct_fishing_rod",
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        durability: 32,
        attack_damage: 1.5,
        attack_speed: -4.0,
        knockback: 0.0,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 60, tint: "corps" },
        use_behaviour: "fishing_rod"
    });
    rod.onUse(use => use.when({ sneaking: true }, pull => pull.when({ stackTimer: "grapple_rest" },
        rest => rest.notify("Grapple recovering", { color: "red" }),
        reel => {
            reel.meter("life_ringCharge", { cost: 20, failMessage: "Not enough power!" });
            reel.stopUsing();
            reel.stackState("grapple_rest", { op: "start_timer", seconds: 3 });
            reel.push("self", { direction: "look", strength: 2.0, lift: 0.4 });
            reel.selfEffect("minecraft:slow_falling", { duration: 2, amplifier: 0, showParticles: false });
            reel.sound("minecraft:entity.fishing_bobber.retrieve", { pitch: 0.7 });
        })));
    rod.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 1 });
        swing.wear(1);
        swing.itemCooldown(1);
    }));
    rod.onBlockBroken(broken => broken.wear(2));
});
