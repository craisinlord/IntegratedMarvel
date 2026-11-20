item("thematic:melee", crowbar => {
    crowbar.use("blunt", { power_cost: 124, durability: 25, attack_damage: 30.0, attack_speed: -2.8, knockback: 0.8 });
    crowbar.set({ denied_sound: "minecraft:entity.player.attack.sweep", reach: 1.17, model: "thematic:crowbar" });
    crowbar.onHit(hit => hit.when({ player: true }, swing => {
        swing.when({ target: { not: { facing: { toward: "wearer", within: 120 } } } }, back => {
            back.when({ chance: 0.2 },
                both => both.damage({ damage: 60, source: "player_attack" }),
                only => only.damage({ damage: 30, source: "player_attack" }));
            back.sound("minecraft:entity.player.attack.crit", { pitch: 0.8 });
            back.notify("Backstab!", { color: "red" });
            back.effect("minecraft:weakness", { duration: 3, amplifier: 0 });
            back.particles("minecraft:crit", { count: 10, spread: 0.25, speed: 0.05, anchor: "body" });
            back.itemCooldown(0.55);
        }, front => front.when({ chance: 0.2 }, critical => {
            critical.damage({ damage: 15, source: "player_attack" });
            critical.sound("minecraft:entity.player.attack.strong", { pitch: 1.2 });
            critical.notify("Critical Hit!", { color: "yellow" });
            critical.effect("minecraft:weakness", { duration: 3, amplifier: 0 });
            critical.particles("minecraft:crit", { count: 10, spread: 0.25, speed: 0.05, anchor: "body" });
            critical.itemCooldown(0.7);
        }, plain => {
            plain.sound("minecraft:entity.player.attack.sweep", { pitch: 1.0 });
            plain.itemCooldown(0.8);
        }));
        swing.wear(1);
    }, mob => {
        mob.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        mob.wear(2);
        mob.push("target", { direction: "away_from_owner", flat: true, strength: 0.4, lift: 0.1 });
    }));
});
