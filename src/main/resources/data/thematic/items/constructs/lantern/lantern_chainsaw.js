item("thematic:melee", saw => {
    saw.set({
        model: "thematic:construct_chainsaw",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.9,
        traits: ["construct"],
        look: "energy_construct",
        durability: 750,
        attack_damage: 1.5,
        attack_speed: 0.8,
        knockback: 2.0,
        mineable: "minecraft:mineable/axe",
        mining_speed: 18.0,
        mining: [
            { blocks: ["#minecraft:logs", "#minecraft:planks", "#minecraft:wooden_fences", "#minecraft:wooden_doors"], speed: 18 },
            { tag: "minecraft:leaves", speed: 3 },
            { tag: "thematic:mineable/multitool", speed: 6, suitable: false }
        ],
        use_action: "bow",
        max_use_time: 72000,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 150, tint: "corps" }
    });
    saw.onHit(hit => {
        hit.effect("thematic:bleed", { duration: 1, amplifier: 0 });
        hit.effect("thematic:grounded", { duration: 1, amplifier: 10, showParticles: false, showIcon: false });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 1.0, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    saw.onUse(start => {
        start.sound("minecraft:block.stone.break", { volume: 1.0, pitch: 2.0 });
        start.stackState("saw_cut", { op: "start_timer", seconds: 0.5 });
        start.stackState("saw_bite", { op: "start_timer", seconds: 0.25 });
        start.stackState("saw_whine", { op: "start_timer", seconds: 1 });
    });
    saw.onUseTick(tick => {
        tick.particles("minecraft:item_snowball", { from: "eyes", forward: 1.0, count: 3, spread: 0.1, speed: 0.05 });
        tick.particles(streak("$stack_color", 0.6, 2), { from: "eyes", forward: 1.0, count: 2, spread: 0.1, speed: 0.4 });
        tick.when({ not: { stackTimer: "saw_cut" } }, cut => {
            cut.stackState("saw_cut", { op: "start_timer", seconds: 0.5 });
            cut.breakPattern("tree", { at: "look", reach: 4, max: 1, leaves: 2, leafChance: 0.3, wearChance: 1.0 });
            cut.breakPattern("single", { at: "look", reach: 4, filter: ["#minecraft:planks", "#minecraft:wooden_fences",
                "#minecraft:wooden_doors", "#minecraft:leaves"], wearChance: 1.0 });
        });
        tick.when({ not: { stackTimer: "saw_bite" } }, bite => {
            bite.stackState("saw_bite", { op: "start_timer", seconds: 0.25 });
            bite.targets("look", { range: 4, pick: "nearest" }, victim => {
                victim.damage({ damage: 0.75, source: "player_attack" });
                victim.effect("thematic:bleed", { duration: 2, amplifier: 0, showParticles: false });
                victim.particles("minecraft:damage_indicator", { anchor: "body", count: 5, spread: 0.1, speed: 0.05 });
                victim.particles("minecraft:crimson_spore", { anchor: "body", count: 10, spread: 0.2, speed: 0.1 });
                victim.wear(1);
            });
        });
        tick.when({ not: { stackTimer: "saw_whine" } }, whine => {
            whine.stackState("saw_whine", { op: "start_timer", seconds: 1 });
            whine.sound("minecraft:item.axe.strip", { volume: 0.8, pitch: 1.5 });
        });
    });
    saw.onRelease(stop => stop.sound("minecraft:item.armor.equip_leather", { volume: 1.0, pitch: 1.0 }));
    saw.onBlockBroken(broken => broken.wear(2));
});
