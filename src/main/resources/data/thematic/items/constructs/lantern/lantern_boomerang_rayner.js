item("thematic:melee", rang => {
    rang.use("blunt", { power_cost: 80, durability: 40, attack_damage: 15.0, attack_speed: 0.5, knockback: 1.0 });
    rang.set({ denied_sound: "minecraft:item.shield.block", model: "thematic:boomerang" });
    rang.onUse(use => use.when({ stackTimer: "rang_out" }, out => out.notify("Boomerang is still out"), toss => {
        toss.stackState("rang_out", { op: "start_timer", seconds: 2 });
        toss.swingHand("main");
        toss.projectile({
            from: { point: "eyes", offset: [0, 0, 0.5], relative: "look" },
            speed: 1.4,
            maxDistance: 40,
            size: 0.7,
            gravity: 0,
            directHit: "none",
            pierce: true,
            hitOnce: false,
            return: { mode: "direct", maxDistance: 20 },
            color: "$stack_color",
            look: { style: "feather", model: "thematic:geo/item/boomerang.geo.json", texture: "thematic:textures/item/construct.png", color: "$stack_color", layer: "beam", scale: 1.4 }
        }, struck => struck.when({ target: { hittable: true } }, hit => {
            hit.damage({ damage: 15, source: "player_attack" });
            hit.sound("minecraft:entity.player.attack.strong", { pitch: 1.3 });
        }));
        toss.sound("minecraft:entity.witch.throw", { pitch: 0.8 });
        toss.wear(1);
    }));
    rang.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
});
