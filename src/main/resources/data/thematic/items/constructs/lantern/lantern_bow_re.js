item("thematic:melee", bow => {
    bow.set({
        model: "thematic:construct_bow",
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        durability: 75,
        attack_damage: 48.0,
        attack_speed: -2.0,
        knockback: 1.0,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 75, tint: "corps" },
        animations: ["bow"],
        use_behaviour: { type: "bow", arrow: "thematic:arrow" }
    });
    bow.onUse(use => use.when({ sneaking: true, not: { stackTimer: "willpower_rest" } }, willpower => {
        willpower.stopUsing();
        willpower.stackState("willpower_rest", { op: "start_timer", seconds: 6 });
        willpower.projectile({
            from: { point: "eyes", offset: [0, 0, 0.5], relative: "look" },
            speed: 3,
            maxDistance: 48,
            size: 0.35,
            directHit: "none",
            pierce: true,
            look: { style: "streak", color: "#33FF66", coreColor: "#FFFFFF" },
            color: "$stack_color",
            trail: { particle: "minecraft:end_rod", count: 1, spread: 0.02 }
        }, pierced => pierced.when({ target: { hittable: true } }, struck => {
            struck.damage({ damage: 36, source: "player_attack" });
            struck.effect("thematic:green_flashed", { duration: 2, amplifier: 0 });
            struck.effect("thematic:vulnerable", { duration: 3, amplifier: 2 });
            struck.particles(streak("$stack_color", 1, 2), { count: 12, spread: 0.3, speed: 0.6, yOffset: 1 });
        }));
        willpower.triggerItemAnim("bow_controller", { clip: "idle" });
        willpower.sound("minecraft:entity.arrow.shoot", { pitch: 0.7 });
        willpower.sound("minecraft:block.beacon.power_select", { volume: 0.6, pitch: 1.8 });
        willpower.wear(2);
    }, draw => {
        draw.triggerItemAnim("bow_controller", { clip: "pull" });
        draw.stackState("drawing", { op: "start_timer", seconds: 1 });
        draw.sound("thematic:bow_draw");
    }));
    bow.onRelease(release => {
        release.triggerItemAnim("bow_controller", { clip: "idle" });
        release.meter("willpower_ringCharge", { consume: 10 });
        release.when({ not: { stackTimer: "drawing" } }, full => {
            full.projectile({
                body: "arrow",
                arrow: "thematic:arrow",
                count: 2,
                spread: 10,
                speed: 3,
                damage: 15
            });
            full.sound("minecraft:entity.arrow.shoot", { pitch: 1.3 });
        });
    });
    bow.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 1 });
        swing.wear(2);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
        swing.itemCooldown(1);
    }));
    bow.onBlockBroken(broken => broken.wear(2));
});
