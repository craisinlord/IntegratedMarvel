item("thematic:melee", anvil => {
    anvil.set({
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
        construct: { power_cost: 200, tint: "corps" }
    });
    anvil.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    anvil.onUse(use => use.when({ stackTimer: "anvil_rest" }, rest => rest.notify("Anvil recovering", { color: "red" }), drop => {
        drop.meter("willpower_ringCharge", { cost: 80, failMessage: "Not enough power!" });
        drop.stackState("anvil_rest", { op: "start_timer", seconds: 6 });
        drop.targets("look", { range: 24, width: 1.0, pick: "nearest" }, target => {
            target.markSpot("lantern_anvil", { anchor: "eyes", offset: [0, 10, 0], offsetFrame: "line", riseOutOfBlocks: true });
            target.projectile({
                from: "spot",
                aim: "target",
                speed: 1.4,
                size: 1.2,
                maxAge: 3,
                directHit: "none",
                color: "$stack_color",
                look: { style: "feather", model: "thematic:geo/entity/anvil_construct.geo.json", texture: "thematic:textures/item/construct.png", color: "$stack_color", layer: "beam", scale: 1.5 }
            }, hit => {
                hit.damage({ damage: 24, source: "player_attack" });
                hit.effect("thematic:stun", { duration: 1, amplifier: 0 });
                hit.sound("minecraft:block.anvil.land", { at: "impact", world: true });
            }, block => block.sound("minecraft:block.anvil.land", { pitch: 0.8, at: "impact", world: true }));
        }, missed => missed.notify("Nothing to drop it on"));
        drop.wear(2);
    }));
});
