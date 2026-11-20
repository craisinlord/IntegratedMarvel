item("thematic:melee", extinguisher => {
    extinguisher.set({
        model: "thematic:construct_extinguisher",
        denied_sound: "minecraft:item.shield.block",
        traits: ["construct"],
        look: "energy_construct",
        durability: 450,
        attack_damage: 19.5,
        attack_speed: 0.4,
        knockback: 1.1,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 233, tint: "corps" }
    });
    extinguisher.use("thematic:sprayer", {
        particle: "minecraft:spit",
        effects: ["thematic:distinguish", "thematic:push"],
        speed: 1.25,
        lifetime: 120,
        damage: 0.15
    });
    extinguisher.onHit(hit => {
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.55, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    extinguisher.onBlockBroken(broken => broken.wear(2));
});
