item("thematic:melee", swarm => {
    swarm.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct",
        durability: 30,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 300, tint: "corps" }
    });
    swarm.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    swarm.onUse(use => use.when({ stackTimer: "swarm_rest" }, rest => rest.notify("Spider Swarm recovering", { color: "red" }), release => {
        release.meter("life_ringCharge", { cost: 120, failMessage: "Not enough power!" });
        release.stackState("swarm_rest", { op: "start_timer", seconds: 15 });
        release.summon("minecraft:cave_spider", {
            count: 4,
            lasts: 10,
            owner: true,
            neverTargetOwner: true,
            tint: "yellow"
        });
        release.sound("minecraft:entity.spider.ambient", { pitch: 0.6 });
        release.wear(3);
    }));
});
