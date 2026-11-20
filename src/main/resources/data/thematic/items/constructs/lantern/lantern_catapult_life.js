item("thematic:melee", catapult => {
    catapult.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct",
        durability: 40,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 180, tint: "corps" }
    });
    catapult.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    catapult.onUse(use => use.when({ stackTimer: "catapult_rest" }, rest => rest.notify("Catapult recovering", { color: "red" }), fling => {
        fling.meter("life_ringCharge", { cost: 50, failMessage: "Not enough power!" });
        fling.stackState("catapult_rest", { op: "start_timer", seconds: 8 });
        fling.when({ sneaking: true }, ally => ally.targets("look", { range: 6, width: 1.0, pick: "nearest" }, launched => {
            launched.push("target", { direction: "look", strength: 2.6, lift: 0.7 });
            launched.effect("minecraft:slow_falling", { duration: 4, amplifier: 0 });
        }, missed => missed.notify("Nobody to launch")), self => {
            self.push("self", { direction: "look", strength: 2.6, lift: 0.6 });
            self.selfEffect("minecraft:slow_falling", { duration: 4, amplifier: 0, showParticles: false });
        });
        fling.sound("minecraft:entity.firework_rocket.launch", { pitch: 0.8 });
        fling.particles(streak("$stack_color", 1, 2), { count: 16, spread: 0.4, speed: 0.6, yOffset: 0.5 });
        fling.wear(2);
    }));
});
