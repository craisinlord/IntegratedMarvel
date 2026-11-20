item("thematic:melee", axe => {
    axe.set({
        model: "thematic:construct_ice_axe",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.17,
        traits: ["construct"],
        look: "energy_construct",
        durability: 80,
        attack_damage: 49.5,
        attack_speed: 0.4,
        knockback: 1.2,
        mineable: "minecraft:mineable/pickaxe",
        mining_speed: 10.0,
        can_mine: "#minecraft:mineable/pickaxe",
        glint: true,
        enchantments: { "minecraft:silk_touch": 1 },
        item_bar: { color: "stack_color" },
        construct: { power_cost: 482, tint: "corps" }
    });
    axe.onHit(hit => {
        hit.when({ target: { player: true } }, shatter => {
            shatter.effect("thematic:bleed", { duration: 5, amplifier: 1 });
            shatter.particles("minecraft:damage_indicator", { count: 30, spread: 0.5, speed: 0.3, yOffset: 1.0 });
            shatter.particles("minecraft:crit", { count: 20, spread: 0.5, speed: 0.2, yOffset: 1.0 });
            shatter.sound("minecraft:block.anvil.destroy", { volume: 1.0, pitch: 1.5 });
            shatter.notify("message.thematic.construct.ice_axe_shattered", { color: "dark_red" });
            shatter.notify("item.thematic.construct.tooltip.heavy_bleeding", { color: "red", who: "target" });
            shatter.wear(80);
        }, other => {
            other.when({ not: { stackTimer: "swing" } }, swing => {
                swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
                swing.wear(2);
                swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
                swing.itemCooldown(0.25);
            });
        });
    });
    axe.onBlockBroken(broken => broken.wear(3));
});
