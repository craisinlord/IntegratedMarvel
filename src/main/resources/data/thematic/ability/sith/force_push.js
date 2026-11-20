function forcePush(id, heavy) {
    component(id, component => {
        component.cooldown();
        component.traits({ wheelIcon: heavy ? "minecraft:wind_charge" : "minecraft:breeze_rod" });

        component.press(press => {
            press.animation("force_palm_thrust", { fadeIn: 0.05 });
            press.sound("minecraft:entity.breeze.wind_burst", { volume: heavy ? 1.4 : 1.1, pitch: heavy ? 0.6 : 0.8, world: true });
            press.vfx("thematic:air_burst_ring", { from: "chest", direction: "look", radius: tuned("radius", 0.4) });
            if (heavy) {
                press.vfx("thematic:punch_pressure_wave", { from: "chest", direction: "look" });
                press.shake({ radius: 10, strength: 0.8, duration: 0.3 });
                press.action("integrated_superheroes:force_redirect", { range: tuned("radius"), angle: tuned("push_arc"), speed: tuned("redirect_speed") });
            }
            press.targets("around", { radius: tuned("radius"), round: true, arc: tuned("push_arc"), lineOfSight: true }, pushed => {
                pushed.damage({ source: "player_attack" });
                pushed.push("target", { direction: "away_from_owner", strength: tuned("push_strength"), lift: tuned("push_lift"), mode: "set" });
                pushed.particles("minecraft:cloud", { anchor: "body", count: 6, spread: 0.3, speed: 0.05 });
                if (heavy) {
                    pushed.action("integrated_superheroes:force_wall_slam", { ticks: tuned("slam_ticks"), damage: tuned("slam_damage"), boss_share: tuned("boss_share") });
                }
            });
        });
    });
}

forcePush("vader_force_push", true);
forcePush("maul_force_push", false);
