item("thematic:melee", sword => {
    sword.use("blade", { power_cost: 342, durability: 28, attack_damage: 33.0, attack_speed: 0.6, knockback: 1.0 });
    sword.set({ model: "thematic:construct_sword", denied_sound: "minecraft:entity.player.attack.sweep", reach: 1.17, dual_wield: true });
    sword.onUse(use => use.when({ stackFlag: "parry" },
        parrying => parrying.notify("Already in Parry Stance"),
        dash => {
            dash.itemCooldown(6);
            dash.stackState("parry", { op: "set_flag" });
            dash.stackState("parry_window", { op: "start_timer", seconds: 1 });
            dash.wear(4);
            dash.particles("minecraft:sweep_attack", { system: "streak", aim: "look", from: "chest" });
            dash.particles("minecraft:cloud", { system: "streak", aim: "look", from: "chest", count: 5, spread: 0.1, speed: 0.05 });
            dash.markSpot("construct_sword_dash", { along: "look", anchor: "feet", reach: 10, standoff: 1 });
            dash.teleport("spot", { spot: "construct_sword_dash" });
            dash.notify("Dash Strike!");
            dash.sound("minecraft:entity.player.attack.sweep", { pitch: 1.2 });
        }));
    sword.onHit(hit => {
        hit.when({ player: true }, swing => swing.when({ stackFlag: "parry" }, counter => {
            counter.damage({ damage: 66, source: "player_attack" });
            counter.particles("minecraft:crit", { count: 20, spread: 0.3, speed: 0.2, yOffset: 1 });
            counter.particles("minecraft:explosion", { count: 1, spread: 0, speed: 0, yOffset: 0.5 });
            counter.sound("minecraft:entity.player.attack.crit", { pitch: 0.8 });
            counter.notify("Counter Attack!");
            counter.stackState("parry", { op: "clear_flag" });
        }, normal => normal.when({ chance: 0.15 }, bleed => {
            bleed.effect("minecraft:slowness", { duration: 2, amplifier: 0 });
            bleed.particles("minecraft:damage_indicator", { count: 5, spread: 0.1, speed: 0.2, yOffset: 1 });
        })));
        hit.wear(1);
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    sword.onHeldTick({ every: 5 }, held => held.when({ stackFlag: "parry" }, parrying => parrying.when({ stackTimer: "parry_window" },
        open => open.particles("minecraft:end_rod", { count: 1, spread: 0.2, speed: 0.02, yOffset: 1 }),
        over => {
            over.stackState("parry", { op: "clear_flag" });
            over.notify("Parry Stance ended");
        })));
});
