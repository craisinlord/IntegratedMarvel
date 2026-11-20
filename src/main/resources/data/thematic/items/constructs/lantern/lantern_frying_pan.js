const PAN = "thematic:lantern_frying_pan";

item("thematic:melee", pan => {
    pan.use("blunt", { power_cost: 124, durability: 40, attack_damage: 30.0, attack_speed: -1.0, knockback: 1.2 });
    pan.set({ model: "thematic:construct_frying_pan", denied_sound: "minecraft:block.anvil.land" });
    pan.onUse({ cooldown: 9 }, use => {
        use.itemCooldown(9);
        use.stackState("Blocking", { op: "set_flag" });
        use.stackState("BlockingEnd", { op: "start_timer", seconds: 3 });
        use.notify("Defensive Stance Activated");
        use.sound("minecraft:item.shield.block", { pitch: 1.2 });
    });
    pan.onHit(hit => {
        hit.stackState("Heat", { op: "add_gauge", value: 15, max: 100 });
        hit.when({ stackGauge: { key: "Heat", at_least: 50 } }, hot => {
            hot.ignite({ seconds: 3, exact: true });
            hot.when({ stackGauge: { key: "Heat", at_least: 75 } }, scorching => {
                scorching.effect("minecraft:weakness", { duration: 3, amplifier: 0 });
            });
            hot.particles("minecraft:flame", { count: 10, spread: 0.5, speed: 0.1, yOffset: 1 });
            hot.sound("minecraft:block.fire.extinguish", { volume: 0.8, pitch: 1.2 });
        });
        hit.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        hit.itemCooldown(1.0);
    });
    pan.onInventoryTick(tick => {
        tick.when({ stackFlag: "Blocking", not: { stackTimer: "BlockingEnd" } }, ended => {
            ended.stackState("Blocking", { op: "clear_flag" });
            ended.when({ player: true, heldItem: PAN }, held => held.sound("minecraft:item.shield.break", { volume: 0.5 }));
        });
        tick.once("beat", { seconds: 0.5, clock: "world" }, cool => cool.when({ stackGauge: { key: "Heat", at_least: 1 } }, warm => {
            warm.when({ stackGauge: { key: "Heat", at_least: 76 }, heldItem: PAN }, sizzling => {
                sizzling.particles("minecraft:smoke", { count: 2, spread: 0.2, speed: 0.05, yOffset: 1 });
                sizzling.when({ chance: 1 / 3 }, hiss => hiss.sound("minecraft:block.fire.ambient", { volume: 0.3, pitch: 1.5 }));
                sizzling.when({ stackGauge: { key: "Heat", at_least: 91 }, chance: 0.2 }, scald => {
                    scald.targets("around", { radius: 1.5, hittable: false }, near => {
                        near.damage({ damage: 3, damageType: "minecraft:on_fire" });
                        near.particles("minecraft:smoke", { count: 5, spread: [0.2, 0.4, 0.2], speed: 0.05, yOffset: 0.5 });
                        near.sound("minecraft:block.fire.extinguish", { volume: 0.3, pitch: 1.5 });
                    });
                });
            });
            warm.stackState("Heat", { op: "add_gauge", value: -1, min: 0 });
        }));
        tick.when({ stackFlag: "Blocking", player: true, heldItem: PAN }, stance => {
            stance.targets("around", { box: { look: 2, y: 1 }, spawnGroups: "monster", arc: 30, hittable: false }, mob => {
                mob.push("target", { direction: "away_from_owner", strength: 0.15, lift: 0.2, mode: "set" });
                mob.sound("minecraft:item.shield.block", { volume: 0.8, pitch: 1.4, at: "self" });
                mob.particles("minecraft:crit", { count: 10, spread: 0.5, speed: 0.2 });
                mob.wear(1);
            });
            stance.once("beat", { seconds: 0.25, clock: "world" }, mark => {
                mark.particles("minecraft:crit", { count: 1, spread: 0.2, speed: 0, forward: 0.7, yOffset: 1.2 });
            });
        });
    });
});
