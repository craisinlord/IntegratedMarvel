item("thematic:melee", shovel => {
    shovel.set({
        model: "thematic:construct_shovel",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.33,
        traits: ["construct"],
        look: "energy_construct",
        durability: 200,
        attack_damage: 25.5,
        attack_speed: 0.3,
        knockback: 1.0,
        mineable: "minecraft:mineable/shovel",
        mining_speed: 7.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 280, tint: "corps" }
    });
    shovel.onHit(hit => {
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    shovel.onUseOnBlock(use => {
        use.flatten({ wear: 1 });
        use.extinguish({ at: "hit", shape: "single", blocks: ["campfires"], griefCheck: true });
    });
    shovel.onBlockBroken(broken => {
        broken.breakPattern("plane", { radius: 1, filter: "#minecraft:mineable/shovel", wearChance: 0.3333 });
        broken.wear(2);
    });
});
