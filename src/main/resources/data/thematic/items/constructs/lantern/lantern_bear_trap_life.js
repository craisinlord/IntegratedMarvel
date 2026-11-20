item("thematic:melee", trap => {
    trap.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:construct",
        durability: 30,
        attack_damage: 6.0,
        attack_speed: -2.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 120, tint: "corps" }
    });
    trap.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    trap.onUse(use => use.when({ stackTimer: "trap_rest" }, rest => rest.notify("Bear Trap recovering", { color: "red" }), toss => {
        toss.meter("life_ringCharge", { cost: 60, failMessage: "Not enough power!" });
        toss.stackState("trap_rest", { op: "start_timer", seconds: 4 });
        toss.projectile({
            from: { point: "eyes", offset: [0, -0.2, 0.5], relative: "look" },
            speed: 1.0,
            maxDistance: 24,
            size: 0.8,
            gravity: 0.06,
            directHit: "none",
            color: "$stack_color",
            look: { style: "feather", model: "thematic:geo/entity/bear_trap.geo.json", texture: "thematic:textures/item/construct.png", color: "$stack_color", layer: "beam", scale: 1.0 }
        }, struck => struck.when({ target: { hittable: true } }, hit => {
            hit.damage({ damage: 18, source: "player_attack" });
            hit.effect("thematic:immobile", { duration: 4, amplifier: 0 });
            hit.effect("thematic:bleed", { duration: 3, amplifier: 0 });
            hit.sound("minecraft:block.iron_trapdoor.close", { pitch: 0.6 });
        }));
        toss.sound("minecraft:entity.witch.throw", { pitch: 0.6 });
        toss.wear(2);
    }));
});
