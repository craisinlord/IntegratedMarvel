item("thematic:melee", bat => {
    bat.use("blunt", { power_cost: 319, durability: 18, attack_damage: 30.0, attack_speed: 0.3, knockback: 1.0 });
    bat.set({ model: "thematic:construct_cricket_bat", denied_sound: "minecraft:entity.player.attack.sweep", reach: 1.17 });
    bat.onIncomingProjectile({ if: { player: true } }, incoming => incoming.when({ chance: 0.25 }, deflect => {
        deflect.cancelEvent();
        deflect.deflectProjectile();
        deflect.sound("minecraft:item.shield.block", { pitch: 1.4 });
        deflect.selfEffect("minecraft:speed", { duration: 1.5, amplifier: 0 });
        deflect.notify("Deflected!", { color: "green" });
    }));
    bat.onHit(hit => hit.when({ player: true }, swing => {
        swing.when({ stackTimer: "hit_window" }, streak => {
            streak.stackState("hits", { op: "add_counter" });
            streak.when({ stackCounter: { key: "hits", at_least: 4 } }, boundary => {
                boundary.stackState("hits", { op: "set_counter", value: 0 });
                boundary.sound("minecraft:entity.player.levelup", { pitch: 1.2 });
                boundary.notify("BOUNDARY! 4 consecutive hits!", { color: "gold" });
                boundary.selfEffect("minecraft:strength", { duration: 5, amplifier: 1 });
                boundary.selfEffect("minecraft:resistance", { duration: 5, amplifier: 1 });
                boundary.repairItem({ hands: "main", amount: 3 });
            }, progress => {
                progress.when({ stackCounter: { key: "hits", equals: 1 } },
                    one => one.notify("Consecutive Hit! 1/4", { color: "yellow" }),
                    more => more.when({ stackCounter: { key: "hits", equals: 2 } },
                        two => two.notify("Consecutive Hit! 2/4", { color: "yellow" }),
                        three => three.notify("Consecutive Hit! 3/4", { color: "yellow" })));
            });
        }, fresh => fresh.stackState("hits", { op: "set_counter", value: 1 }));
        swing.stackState("hit_window", { op: "start_timer", seconds: 3 });
        swing.targets("around", { radius: 2.5, except: "struck" }, near => {
            near.damage({ damage: 18, source: "player_attack" });
            near.sound("minecraft:entity.player.attack.sweep", { volume: 0.7, pitch: 1.2 });
        });
        swing.sound("minecraft:entity.player.attack.strong");
        swing.wear(1);
        swing.itemCooldown(0.2);
    }, mob => {
        mob.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        mob.wear(2);
        mob.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
    }));
});
