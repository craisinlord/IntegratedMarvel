item("thematic:melee", swords => {
    swords.use("blade", { power_cost: 467, durability: 39, attack_damage: 48.0, attack_speed: 2.2, knockback: 0.1 });
    swords.set({ model: "thematic:construct_dual_sword", denied_sound: "minecraft:item.trident.return", reach: 1.17 });
    swords.onUse(use => {
        use.itemCooldown(10);
        use.targets("around", { box: [4, 0.5, 4], within: 4, hittable: false, exceptAllies: false }, target => {
            target.damage({ damage: 9, source: "player_attack" });
            target.effect("minecraft:slowness", { duration: 3, amplifier: 0 });
            target.push("target", { direction: "away_from_owner", strength: 0.5, verticalStrength: 0, lift: 0.2 });
        });
        use.particleCircle("unfilled_circle", { particle: "minecraft:sweep_attack", radius: 2.5, count: 36, height: 0.1, yOffset: 1 });
        use.particleCircle("unfilled_circle", { particle: "minecraft:crit", radius: 2.5, count: 108, height: 0.1, yOffset: 1 });
        use.notify("Whirlwind Strike!");
        use.wear(3);
        use.sound("minecraft:entity.player.attack.sweep", { pitch: 0.8 });
    });
    swords.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ stackTimer: "combo_window" }, going => {
                going.when({ stackCounter: { key: "combo", below: 4 } }, up => up.stackState("combo", { op: "add_counter" }));
                for (let combo = 2; combo <= 4; combo++) {
                    going.when({ stackCounter: { key: "combo", equals: combo } }, bonus => {
                        bonus.damage({ damage: [9.6, 14.4, 19.2][combo - 2], source: "player_attack" });
                        bonus.particles("minecraft:crit", { count: combo * 5, spread: 0.2, speed: 0.1, yOffset: 1 });
                        bonus.sound("minecraft:entity.player.attack.crit", { volume: 0.5, pitch: [1.0, 1.1, 1.2][combo - 2] });
                        if (combo === 3) {
                            bonus.notify("Combo x3");
                        } else if (combo === 4) {
                            bonus.notify("MAX COMBO!");
                        }
                    });
                }
            }, fresh => fresh.stackState("combo", { op: "set_counter", value: 1 }));
            swing.stackState("combo_window", { op: "start_timer", seconds: 2 });
            swing.when({ chance: 0.3 }, bleed => {
                bleed.effect("minecraft:slowness", { duration: 3, amplifier: 0 });
                bleed.particles("minecraft:damage_indicator", { count: 10, spread: 0.1, speed: 0.2, yOffset: 1 });
                bleed.sound("minecraft:entity.player.hurt", { volume: 0.3, pitch: 1.5 });
            });
        });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.05, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    swords.onHeldTick(held => held.when({ stackCounter: { key: "combo", at_least: 2 }, not: { stackTimer: "combo_window" } }, lapsed => {
        lapsed.when({ stackCounter: { key: "combo", at_least: 3 } }, long => long.notify("Combo Reset"));
        lapsed.stackState("combo", { op: "set_counter", value: 1 });
    }));
});
