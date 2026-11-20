item("thematic:melee", axe => {
    axe.use("blade", { power_cost: 358, durability: 30, attack_damage: 34.5, attack_speed: 0.7, knockback: 1.8 });
    axe.set({ model: "thematic:construct_battle_axe", denied_sound: "minecraft:item.shield.block", reach: 0.667, mining_speed: 5.0, mineable: "minecraft:mineable/axe" });
    axe.set({ mining: [
        { blocks: ["#minecraft:logs", "#minecraft:planks"], speed: 7.5 },
        { tag: "thematic:mineable/multitool", speed: 5 }
    ] });
    axe.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ chance: 0.5 }, bleed => {
                bleed.effect("thematic:bleed", { duration: 2, amplifier: 0 });
                bleed.particles("minecraft:damage_indicator", { count: 5, spread: [0.2, 0.3, 0.2], speed: 0.05, yOffset: 1 });
            });
            swing.when({ chance: 0.2 }, daze => {
                daze.effect("thematic:confusion", { duration: 3, amplifier: 0 });
                daze.particles("minecraft:effect", { count: 10, spread: [0.2, 0.3, 0.2], speed: 0.05, yOffset: 1.8 });
            });
            swing.when({ criticalSwing: true }, critical => {
                critical.burst({
                    at: "impact",
                    radius: 2.5,
                    damage: 12,
                    knockback: { strength: 0.8, lift: 0.3 },
                    particles: [
                        { particle: dust("$stack_color", 2.0), count: 40, spread: 1.0, speed: 0.1, yOffset: 1 },
                        { particle: streak("$stack_color", 1.4, 2.5), count: 24, spread: 0.5, speed: 0.8, yOffset: 1 },
                        { particle: "minecraft:explosion", count: 1, spread: 0, speed: 0, yOffset: 1 }
                    ],
                    sound: { sound: "minecraft:entity.generic.explode", volume: 0.7, pitch: 1.3 }
                });
                critical.notify("Willpower Blast!", { color: "green" });
            });
        });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.9, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    axe.onUse(use => {
        use.itemCooldown(6);
        use.wear(2);
        use.swingHand("main");
        use.when({ chance: 0.15 }, bleeds => bleeds.stackState("whirl_bleed", { op: "start_timer", seconds: 1 }));
        use.targets("around", { radius: 3.5 }, whirled => {
            whirled.damage({ damage: 18, source: "player_attack" });
            whirled.push("target", { direction: "away_from_owner", strength: 1.1, lift: 0.7 });
            whirled.when({ stackTimer: "whirl_bleed" }, bleed => bleed.effect("thematic:bleed", { duration: 2, amplifier: 0 }));
            whirled.particles("minecraft:sweep_attack", { count: 1, spread: 0, speed: 0, yOffset: 1 });
            whirled.once("pass", first => {
                first.outgoingDamage({ times: 0.1, for: 86400, uses: 1, heldTrait: "construct", key: "construct_empowered" });
                first.notify("Next construct empowered!", { color: "green" });
            });
        });
        use.particleCircle("unfilled_circle", { color: "$stack_color", radius: 3.5, count: 80, height: 0.1, yOffset: 1 });
        use.particles(streak("$stack_color", 1.2, 2.5), { count: 30, spread: [2.5, 0.2, 2.5], speed: 0.7, yOffset: 1 });
        use.particles("minecraft:sweep_attack", { count: 6, spread: [1.5, 0.1, 1.5], speed: 0, yOffset: 1 });
        use.sound("minecraft:entity.player.attack.sweep", { volume: 1.0, pitch: 0.6 });
        use.sound("minecraft:entity.player.attack.strong", { volume: 0.8, pitch: 0.8 });
    });
});
