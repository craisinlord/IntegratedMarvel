function forcePull(id, disarms) {
    component(id, component => {
        component.cooldown();
        component.traits({ wheelIcon: "minecraft:lead" });

        component.press(press => {
            press.animation("force_reach", { fadeIn: 0.05 });
            press.sound("minecraft:entity.breeze.inhale", { volume: 1.2, pitch: 0.7, world: true });
            press.targets("locked_or_look", {
                range: tuned("radius"), aimAssist: true, lineOfSight: true, pick: "first",
                required: { missCooldown: 0.25 },
            }, pulled => {
                pulled.vfx("thematic:air_burst_column", { anchor: "body", radius: 0.8 });
                pulled.damage({ source: "player_attack" });
                pulled.push("target", {
                    direction: "toward_owner",
                    strength: expr("max(0, distance - @stop_short) * @pull_per_block"),
                    lift: tuned("push_lift"),
                    mode: "set",
                });
                pulled.hitstop({ duration: 0.08 });
                pulled.particles("minecraft:cloud", { anchor: "body", count: 8, spread: 0.3, speed: 0.05 });
                if (disarms) {
                    pulled.when({ target: { player: false } }, mob => mob.disarm());
                }
            });
        });
    });
}

forcePull("vader_force_pull", true);
forcePull("maul_force_pull", false);
