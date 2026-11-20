item("thematic:melee", hammer => {
    hammer.set({
        model: "thematic:construct_hammer",
        denied_sound: "minecraft:entity.iron_golem.attack",
        reach: 1.9,
        traits: ["construct"],
        look: "energy_construct",
        durability: 200,
        attack_damage: 34.5,
        attack_speed: 0.2,
        knockback: 1.8,
        mineable: "thematic:mineable/multitool",
        mining_speed: 8.0,
        mining: [
            { tag: "thematic:mineable/multitool", speed: 8 },
            { speed: 2, suitable: false }
        ],
        item_bar: { color: "stack_color" },
        construct: { power_cost: 180, tint: "corps" }
    });
    hammer.onHit(hit => {
        hit.when({ chance: 0.3 }, daze => {
            daze.effect("minecraft:mining_fatigue", { duration: 5, amplifier: 0 });
            daze.effect("minecraft:slowness", { duration: 2.5, amplifier: 0 });
            daze.particles("minecraft:crit", { count: 10, spread: [0.3, 0.5, 0.3], speed: 0.1, yOffset: 1.0 });
            daze.sound("minecraft:entity.zombie.attack_wooden_door", { volume: 0.3, pitch: 0.8 });
        });
        hit.particles("block_below", { count: 15, spread: 0.4, speed: 0.1, yOffset: 0.5 });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(2);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.9, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    hammer.onUse(use => use.when({ stackTimer: "slam_rest" }, rest => rest.notify("Ground slam recovering", { color: "red" }), slam => {
        slam.meter("willpower_ringCharge", { cost: 150, failMessage: "Not enough power!" });
        slam.stackState("slam_rest", { op: "start_timer", seconds: 10 });
        slam.swingHand("main");
        slam.targets("around", { radius: 6 }, struck => {
            struck.damage({ damage: 18, source: "player_attack" });
            struck.push("target", { direction: "away_from_owner", strength: 0.8, lift: 0.9 });
        });
        slam.particleCircle("ground_slam", { particle: dust("$stack_color", 1.5), height: 0.5, radius: 6, count: 48 });
        slam.sound("minecraft:entity.generic.explode", { pitch: 0.7 });
        slam.wear(4);
    }));
    hammer.onBlockBroken(broken => {
        broken.when({ sneaking: false }, ring => {
            ring.breakPattern("plane", { radius: 1, harvestableOnly: true, wearChance: 0.3333 });
        });
        broken.sound("minecraft:block.stone.break", { volume: 1.2, pitch: 0.8 });
        broken.wear(2);
    });
});
