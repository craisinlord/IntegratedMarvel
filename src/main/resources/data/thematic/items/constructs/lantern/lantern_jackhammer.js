item("thematic:melee", drill => {
    drill.set({
        model: "thematic:construct_jackhammer",
        denied_sound: "minecraft:item.shield.block",
        reach: 1.9,
        traits: ["construct"],
        look: "energy_construct",
        durability: 1200,
        attack_damage: 1.5,
        attack_speed: 0.6,
        knockback: 2.0,
        mineable: "minecraft:mineable/pickaxe",
        mining_speed: 10.0,
        mining: [
            { tag: "minecraft:mineable/pickaxe", speed: 10 },
            { tag: "thematic:mineable/multitool", speed: 5, suitable: false }
        ],
        use_action: "bow",
        max_use_time: 72000,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 100, tint: "corps" }
    });
    drill.onHit(hit => {
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 1.0, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    drill.onUse(start => {
        start.sound("minecraft:block.grindstone.use", { volume: 1.0, pitch: 0.8 });
        start.stackState("drill_pound", { op: "start_timer", seconds: 0.25 });
        start.stackState("drill_bore", { op: "start_timer", seconds: 0.4 });
        start.stackState("drill_strain", { op: "start_timer", seconds: 1 });
        start.stackState("drill_hum", { op: "start_timer", seconds: 1.5 });
        start.stackState("drill_sick", { op: "start_timer", seconds: 2 });
    });
    drill.onUseTick(tick => {
        tick.when({ not: { stackTimer: "drill_pound" } }, pound => {
            pound.stackState("drill_pound", { op: "start_timer", seconds: 0.25 });
            pound.targets("around", { radius: 4, arc: 60 }, victim => {
                victim.consumeGauge(5, paid => {
                    paid.particles("minecraft:crit", { anchor: "body", count: 5, spread: 0.3, speed: 0.1 });
                    paid.damage({ damage: 2.25, source: "player_attack" });
                    paid.knockback({ strength: 0.4, direction: "away" });
                    paid.wear(1);
                    paid.once("pass", first => first.sound("minecraft:entity.player.attack.sweep", { volume: 0.9, pitch: 1.6 }));
                });
            });
        });
        tick.when({ not: { stackTimer: "drill_bore" } }, bore => {
            bore.stackState("drill_bore", { op: "start_timer", seconds: 0.4 });
            bore.breakPattern("plane", { at: "look", reach: 5, radius: 2, filter: "#minecraft:mineable/pickaxe", wearChance: 1.0 });
        });
        tick.when({ not: { stackTimer: "drill_strain" } }, strain => {
            strain.stackState("drill_strain", { op: "start_timer", seconds: 1 });
            strain.selfEffect("minecraft:mining_fatigue", { duration: 2, amplifier: 0, showParticles: false });
            strain.selfEffect("minecraft:hunger", { duration: 2, amplifier: 0, showParticles: false });
            strain.consumeGauge(5, paid => { }, broke => {
                broke.stopUsing();
                broke.notify("message.thematic.no_power", { color: "red" });
                broke.sound("minecraft:entity.generic.extinguish_fire", { volume: 1.0, pitch: 1.0 });
            });
        });
        tick.when({ not: { stackTimer: "drill_sick" } }, sick => {
            sick.stackState("drill_sick", { op: "start_timer", seconds: 2 });
            sick.selfEffect("minecraft:nausea", { duration: 3.5, amplifier: 0, showParticles: false });
        });
        tick.when({ not: { stackTimer: "drill_hum" } }, hum => {
            hum.stackState("drill_hum", { op: "start_timer", seconds: 1.5 });
            hum.sound("minecraft:block.grindstone.use", { volume: 0.6, pitch: 1.3 });
        });
        tick.particles("minecraft:smoke", { from: "eyes", forward: 0.5, yOffset: -0.1, count: 1, spread: 0.05, speed: 0.01 });
    });
    drill.onRelease(stop => stop.sound("minecraft:block.metal_pressure_plate.click_off", { volume: 1.0, pitch: 1.0 }));
    drill.onBlockBroken(broken => broken.wear(2));
});
