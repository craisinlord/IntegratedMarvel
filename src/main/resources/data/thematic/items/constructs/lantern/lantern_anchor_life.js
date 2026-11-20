item("thematic:melee", anchor => {
    anchor.use("blunt", { power_cost: 300, durability: 40, attack_damage: 33.0, attack_speed: -0.2, knockback: 2.0 });
    anchor.set({ denied_sound: "minecraft:entity.iron_golem.attack", model: "thematic:construct" });
    anchor.onUse(use => use.when({ stackTimer: "anchor_rest" }, rest => rest.notify("Anchor recovering", { color: "red" }), chain => {
        chain.meter("life_ringCharge", { cost: 100, failMessage: "Not enough power!" });
        chain.stackState("anchor_rest", { op: "start_timer", seconds: 12 });
        chain.targets("look", { range: 16, width: 1.5, pick: "nearest" }, caught => {
            caught.effect("thematic:grounded", { duration: 4, amplifier: 0 });
            caught.push("target", { direction: "toward_owner", strength: 0.6, lift: -1.5 });
            caught.particles(streak("$stack_color", 1, 2), { count: 20, spread: 0.3, speed: 0.6, yOffset: 1 });
            caught.sound("minecraft:block.chain.place", { pitch: 0.6 });
        }, missed => missed.notify("Nothing to anchor"));
        chain.wear(2);
    }));
    anchor.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(2);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 1.2, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
});
