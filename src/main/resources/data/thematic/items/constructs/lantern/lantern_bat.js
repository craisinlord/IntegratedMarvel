item("thematic:melee", bat => {
    bat.use("blunt", { power_cost: 132, durability: 16, attack_damage: 33.0, attack_speed: -2.8, knockback: 2.85 });
    bat.set({ model: "thematic:construct_bat", denied_sound: "minecraft:item.shield.block", reach: 0.334 });
    bat.onUse(use => use.when({ stackTimer: "home_run_rest" },
        resting => resting.notify("Home run recovering", { color: "red" }),
        windup => {
            windup.stackState("home_run_rest", { op: "start_timer", seconds: 10 });
            windup.stackState("winding_up", { op: "start_timer", seconds: 1 });
            windup.selfEffect("minecraft:slowness", { duration: 1, amplifier: 3, showParticles: false });
            windup.sound("minecraft:item.crossbow.loading_middle", { pitch: 0.6 });
            windup.particleCircle("spinning_ring", { particle: dust("$stack_color", 1.0), radius: 1.2, count: 16 });
            windup.after(1, swing => swing.when({ heldItem: "thematic:lantern_bat" }, homeRun => {
                homeRun.swingHand("main");
                homeRun.targets("around", { radius: 4.5 }, batted => {
                    batted.damage({ damage: 27, source: "player_attack" });
                    batted.push("target", { direction: "away_from_owner", flat: true, strength: 3.2, lift: 0.9, launch: true });
                    batted.particles("minecraft:crit", { count: 12, spread: 0.4, speed: 0.3, yOffset: 1.0 });
                });
                homeRun.particleCircle("ground_slam", { particle: dust("$stack_color", 1.5), height: 1.0, radius: 4.5, count: 36 });
                homeRun.particles(streak("$stack_color", 1.4, 2.5), { count: 36, spread: [1.5, 0.4, 1.5], speed: 0.9, yOffset: 1.0 });
                homeRun.sound("minecraft:entity.player.attack.knockback", { pitch: 0.8 });
                homeRun.sound("minecraft:entity.firework_rocket.blast", { pitch: 1.4 });
                homeRun.wear(2);
            }));
        }));
    bat.onHolderHurt({ if: { stackTimer: "winding_up", not: { source: "#minecraft:bypasses_invulnerability" } } }, braced => {
        braced.cancelEvent();
        braced.sound("minecraft:item.shield.block", { volume: 0.6, pitch: 1.6 });
    });
    bat.onHit(hit => {
        hit.when({ target: { player: true } },
            player => player.effect("minecraft:slowness", { duration: 6, amplifier: 2 }),
            mob => mob.effect("minecraft:slowness", { duration: 5, amplifier: 1 }));
        hit.when({ player: true }, swing => {
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 1.5, lift: 0.4 });
            swing.particleDome("burst_only", { particle: dust("$stack_color", 1.0), radius: 2.5, count: 15 });
            swing.particles(streak("$stack_color", 1, 2), { count: 10, spread: 0.3, speed: 0.5, yOffset: 1.0 });
        });
        hit.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 1.425, lift: 0.1 });
        hit.itemCooldown(1.0);
    });
});
