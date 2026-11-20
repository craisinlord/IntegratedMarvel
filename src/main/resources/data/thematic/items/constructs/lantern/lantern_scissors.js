item("thematic:melee", scissors => {
    scissors.set({
        model: "thematic:construct_scissors",
        denied_sound: "minecraft:item.shield.block",
        traits: ["construct"],
        look: "energy_construct",
        durability: 100,
        attack_damage: 16.5,
        attack_speed: 0.5,
        knockback: 0.8,
        mining: [
            { blocks: ["cobweb"], speed: 20 },
            { tag: "minecraft:leaves", speed: 20, suitable: false },
            { tag: "minecraft:wool", speed: 6, suitable: false },
            { blocks: ["vine", "glow_lichen"], speed: 3, suitable: false },
            { blocks: ["redstone_wire", "tripwire"] }
        ],
        item_bar: { color: "stack_color" },
        construct: { power_cost: 202, tint: "corps" }
    });
    scissors.onHit(hit => {
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.4, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    scissors.onUseOnBlock(use => use.trimPlant({ wear: 1 }));
    scissors.onUseOnEntity(use => {
        use.when({ target: { player: true } }, haircut => {
            haircut.effect("thematic:bleed", { duration: 5, amplifier: 1 });
            haircut.damage({ damage: 16.5, source: "player_attack" });
            haircut.particles("minecraft:damage_indicator", { count: 10, spread: 0.3, speed: 0.2, yOffset: 1.8 });
            haircut.sound("minecraft:block.wool.break", { volume: 1.0, pitch: 1.2 });
            haircut.notify("item.thematic.construct.tooltip.player_cut", { color: "red", args: ["target_name"] });
            haircut.notify("item.thematic.construct.tooltip.hair_cut", { color: "dark_red", who: "target", args: ["owner_name"] });
            haircut.wear(2);
        }, other => {
            other.shear({ bonus: [1, 2], wear: 1 });
        });
    });
    scissors.onHeldTick(held => {
        held.when({ sprinting: true, chance: 0.2 }, ouch => {
            ouch.selfEffect("thematic:bleed", { duration: 3, amplifier: 0 });
            ouch.particles("minecraft:damage_indicator", { count: 5, spread: 0.2, speed: 0.1, yOffset: 1.0 });
            ouch.notify("item.thematic.construct.tooltip.scissors_running", { color: "yellow" });
            ouch.sound("minecraft:entity.player.hurt", { volume: 0.5, pitch: 1.5 });
        });
    });
    scissors.onBlockBroken(broken => broken.wear(1));
});
