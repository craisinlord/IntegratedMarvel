item("thematic:melee", club => {
    club.use("blunt", { power_cost: 327, durability: 40, attack_damage: 31.5, attack_speed: 3.0, knockback: 1.2 });
    club.set({ model: "thematic:construct_golf_club", denied_sound: "minecraft:block.wood.hit", reach: 1.33 });
    club.onHit(hit => hit.when({ player: true }, swing => {
        swing.when({ stackTimer: "combo_window", stackCounter: { key: "combo", below: 3 } },
            next => next.stackState("combo", { op: "add_counter" }),
            first => first.stackState("combo", { op: "set_counter", value: 1 }));
        swing.stackState("combo_window", { op: "start_timer", seconds: 2 });
        swing.when({ chance: 0.15 }, perfect => {
            perfect.sound("minecraft:entity.player.attack.crit", { pitch: 1.5 });
            perfect.particles("minecraft:firework", { count: 15, spread: 0.25, speed: 0.05, anchor: "body" });
            perfect.damage({ damage: 56.7, source: "player_attack" });
            perfect.push("target", { direction: "away_from_owner", flat: true, strength: 1.2, lift: 0.1 });
            perfect.notify("Perfect Swing!", { color: "green" });
        }, normal => {
            normal.when({ stackCounter: { key: "combo", equals: 2 } },
                second => second.sound("minecraft:block.wood.hit", { pitch: 1.2 }),
                other => other.when({ stackCounter: { key: "combo", equals: 3 } }, third => {
                    third.sound("minecraft:block.wood.break", { pitch: 1.4 });
                    third.effect("thematic:confusion", { duration: 3, amplifier: 0 });
                }, first => first.sound("minecraft:block.wood.hit", { pitch: 1.0 })));
            normal.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        });
        swing.when({ stackCounter: { key: "combo", at_least: 2 } },
            combo => combo.itemCooldown(0.15),
            single => single.itemCooldown(0.25));
        swing.wear(1);
    }, mob => {
        mob.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        mob.wear(2);
        mob.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
    }));
});
