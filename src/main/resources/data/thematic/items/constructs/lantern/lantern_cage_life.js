item("thematic:melee", cage => {
    cage.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct",
        durability: 40,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 250, tint: "corps" }
    });
    cage.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    cage.onUse(use => use.when({ stackTimer: "cage_rest" }, rest => rest.notify("Cage recovering", { color: "red" }), lock => {
        lock.meter("life_ringCharge", { cost: 100, failMessage: "Not enough power!" });
        lock.stackState("cage_rest", { op: "start_timer", seconds: 8 });
        lock.targets("look", { range: 16, width: 1.0, pick: "nearest" }, caged => {
            caged.when({ target: { player: true } },
                player => player.effect("thematic:immobile", { duration: 2, amplifier: 0 }),
                mob => mob.effect("thematic:immobile", { duration: 5, amplifier: 0 }));
            caged.particleDome("full_dome", { particle: dust("$stack_color", 1.0), radius: 1.5, count: 48 });
            caged.sound("minecraft:block.beacon.power_select", { pitch: 1.4 });
        }, missed => missed.notify("Nothing to cage"));
        lock.wear(2);
    }));
});
