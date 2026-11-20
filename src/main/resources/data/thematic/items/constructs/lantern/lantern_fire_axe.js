item("thematic:melee", axe => {
    axe.set({
        model: "thematic:construct_fire_axe",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.9,
        traits: ["construct"],
        look: "energy_construct",
        durability: 140,
        attack_damage: 27.0,
        attack_speed: 0.6,
        knockback: 1.5,
        mineable: "thematic:mineable/multitool",
        mining_speed: 4.0,
        mining: [
            { blocks: ["#minecraft:logs", "#minecraft:planks"], speed: 8 }
        ],
        item_bar: { color: "stack_color" },
        construct: { power_cost: 295, tint: "corps" }
    });
    axe.onHit(hit => {
        hit.when({ chance: 0.15 }, concussion => {
            concussion.effect("minecraft:slowness", { duration: 5, amplifier: 3 });
            concussion.effect("minecraft:nausea", { duration: 3, amplifier: 0 });
            concussion.particles("minecraft:cloud", { count: 15, spread: 0.3, speed: 0.2, yOffset: 1.0 });
            concussion.particles("minecraft:crit", { count: 20, spread: 0.5, speed: 0.3, yOffset: 1.0 });
            concussion.sound("minecraft:entity.player.attack.crit", { volume: 1.0, pitch: 0.8 });
            concussion.notify("message.thematic.construct.concussive_hit", { color: "dark_red" });
        });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.75, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    axe.onUseOnBlock({ if: { sneaking: true, block_type: "#minecraft:logs" } }, fell => {
        fell.breakPattern("tree", { filter: "#minecraft:logs", max: 256, leaves: 3, wearChance: 0.125 });
    });
    axe.onBlockBroken(broken => broken.wear(2));
});
