item("thematic:melee", fist => {
    fist.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:boxing_glove",
        durability: 60,
        attack_damage: 12.0,
        attack_speed: -2.6,
        knockback: 1.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 200, tint: "corps" }
    });
    fist.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    fist.onUse(use => use.when({ stackTimer: "fist_rest" }, rest => rest.notify("Giant Fist recovering", { color: "red" }), wind => {
        wind.stackState("fist_rest", { op: "start_timer", seconds: 8 });
        wind.selfEffect("minecraft:slowness", { duration: 1, amplifier: 3, showParticles: false });
        wind.sound("minecraft:block.beacon.activate", { pitch: 1.6 });
        wind.particleCircle("spinning_ring", { particle: dust("$stack_color", 1.2), radius: 1.0, count: 16 });
        wind.after(1, later => later.when({ heldItem: "thematic:lantern_giant_fist_stewart" }, punch => {
            punch.swingHand("main");
            punch.projectile({
                from: { point: "eyes", offset: [0, -0.3, 0.6], relative: "look" },
                speed: 1.6,
                maxDistance: 16,
                size: 1.4,
                gravity: 0,
                directHit: "none",
                pierce: true,
                color: "$stack_color",
                look: { style: "feather", model: "thematic:geo/item/boxing_glove.geo.json", texture: "thematic:textures/item/construct.png", color: "$stack_color", layer: "beam", scale: 3.0 },
                trail: { particle: "minecraft:end_rod", count: 2, spread: 0.4 }
            }, struck => struck.when({ target: { hittable: true } }, hit => {
                hit.damage({ damage: 30, source: "player_attack" });
                hit.push("target", { direction: "away_from_owner", strength: 2.4, lift: 0.5 });
                hit.sound("minecraft:entity.player.attack.knockback", { pitch: 0.6 });
            }));
            punch.sound("minecraft:entity.firework_rocket.launch", { pitch: 0.6 });
            punch.wear(4);
        }));
    }));
});
