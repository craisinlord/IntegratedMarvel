function bringDown(held) {
    held.when({ stackFlag: "swinging", not: { stackTimer: "windup" } }, down => {
        down.stackState("swinging", { op: "clear_flag" });
        down.stackState("swing_rest", { op: "start_timer", seconds: 0.25 });
        down.swingHand("main");
        down.targets("around", { radius: 7, arc: 60, lineOfSight: true }, caught => {
            caught.damage({ damage: 56, source: "player_attack" });
            caught.effect("thematic:confusion", { duration: 2, amplifier: 0 });
            caught.push("target", { direction: "away_from_owner", flat: true, strength: 1.5, lift: 0.1 });
        });
        down.particles("minecraft:sweep_attack", { count: 8, spread: [2, 0.2, 2], speed: 0, from: "eyes", forward: 3, yOffset: -0.4 });
        down.particles(streak("$stack_color", 1.2, 2.5), { count: 24, spread: [2.5, 0.3, 2.5], speed: 0.6, from: "eyes", forward: 3, yOffset: -0.4 });
        down.particles(dust("$stack_color", 1.2), { count: 16, spread: [2.5, 0.3, 2.5], speed: 0.05, from: "eyes", forward: 3, yOffset: -0.4 });
        down.sound("minecraft:entity.player.attack.sweep", { pitch: 0.6 });
        down.sound("minecraft:block.anvil.land", { volume: 0.5, pitch: 1.2 });
        down.wear(1);
    });
}

item("thematic:melee", beam => {
    beam.use("blunt", { power_cost: 178, durability: 45, attack_damage: 55.5, attack_speed: -3.2, knockback: 3.0 });
    beam.set({ model: "thematic:construct_steel_beam", denied_sound: "minecraft:entity.iron_golem.hurt", reach: 2.334 });
    beam.onUse(use => use.when({ stackTimer: "slam_cooldown" },
        waiting => waiting.notify("Steel Beam Slam recovering", { color: "red" }),
        slam => {
            slam.stackState("slam_cooldown", { op: "start_timer", seconds: 12 });
            slam.stackState("slam_rise", { op: "start_timer", seconds: 0.5 });
            slam.stackState("slam_window", { op: "start_timer", seconds: 4 });
            slam.stackState("slamming", { op: "set_flag" });
            slam.push("self", { direction: "look_horizontal", strength: 0.3, mode: "set", vertical: 0.7, resetFall: true });
            slam.overshield(25);
            slam.animation("steel_beam_slam");
            slam.sound("minecraft:entity.iron_golem.attack", { pitch: 0.7 });
            slam.particleDome("burst_only", { particle: dust("$stack_color", 1.0), radius: 1.5, count: 12 });
            slam.particles(streak("$stack_color", 1, 2), { count: 14, spread: 0.4, speed: 0.45, yOffset: 0.3 });
            slam.after(0.45, dive => {
                dive.push("self", { direction: "look_horizontal", strength: 1.1, mode: "set", vertical: -1.3, resetFall: true });
                dive.swingHand("main");
                dive.sound("minecraft:entity.player.attack.sweep", { pitch: 0.5 });
            });
        }));
    beam.onAttack(attack => {
        attack.cancelEvent();
        attack.when({ not: { any: [{ stackTimer: "windup" }, { stackTimer: "swing_rest" }] } }, raise => {
            raise.stackState("windup", { op: "start_timer", seconds: 0.5 });
            raise.stackState("swinging", { op: "set_flag" });
            raise.selfEffect("thematic:vulnerable", { duration: 0.5, amplifier: 4, showParticles: false });
            raise.animation("steel_beam_slam");
            raise.sound("minecraft:entity.iron_golem.attack", { pitch: 0.5 });
        });
    });
    beam.onHit(hit => hit.when({ player: false }, mob => {
        mob.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        mob.wear(2);
        mob.push("target", { direction: "away_from_owner", flat: true, strength: 1.5, lift: 0.1 });
    }));
    beam.onHeldTick(held => {
        bringDown(held);
        held.when({ stackFlag: "slamming" }, slamming => {
        slamming.resetFallDistance();
        slamming.when({ onGround: true, not: { stackTimer: "slam_rise" } }, landed => {
            landed.stackState("slamming", { op: "clear_flag" });
            landed.targets("around", { radius: 4.5 }, thrown => {
                thrown.damage({ damage: 33, source: "player_attack" });
                thrown.effect("thematic:confusion", { duration: 2, amplifier: 0 });
                thrown.push("target", { direction: "away_from_owner", flat: true, strength: 2.8, lift: 0.6, launch: true });
            });
            landed.particleCircle("ground_slam", { particle: "minecraft:cloud", height: 0.2, radius: 4.5, count: 40 });
            landed.particleDome("burst_only", { particle: dust("$stack_color", 1.0), radius: 4.5, count: 30 });
                landed.particles(streak("$stack_color", 1.4, 2.5), { count: 40, spread: [1.5, 0.2, 1.5], speed: 0.8, yOffset: 0.3 });
            landed.particles("block_below", { count: 30, spread: 1.5, speed: 0.2, yOffset: 0.2 });
            landed.sound("minecraft:block.anvil.land", { pitch: 0.6 });
            landed.sound("minecraft:entity.generic.explode", { volume: 0.6, pitch: 1.2 });
            landed.wear(2);
        }, airborne => airborne.when({ not: { stackTimer: "slam_window" } },
            lost => lost.stackState("slamming", { op: "clear_flag" })));
    });
    });
});
