item("thematic:melee", hoe => {
    hoe.set({
        model: "thematic:construct_hoe",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.33,
        traits: ["construct"],
        look: "energy_construct",
        durability: 100,
        attack_damage: 15.0,
        attack_speed: 0.3,
        knockback: 0.9,
        mineable: "minecraft:mineable/hoe",
        item_bar: { color: "stack_color" },
        construct: { power_cost: 186, tint: "corps" }
    });
    hoe.onHit(hit => hit.wear(1));
    hoe.onUseOnBlock(use => use.till({ wear: 1 }));
    hoe.onBlockBroken(broken => broken.wear(1));
});
