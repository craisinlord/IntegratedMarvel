function lunge(actions, ticks) {
    const perfect = ticks >= 20;
    actions.push("self", { direction: "look", strength: perfect ? 2.5 : ticks * 0.125, verticalStrength: 0, lift: 0.1 });
    actions.stackState("lunge", { op: "start_timer", seconds: 0.55 });
    if (perfect) {
        actions.stackState("perfect_lunge", { op: "set_flag" });
        actions.particles("minecraft:soul_fire_flame", { count: 15, spread: 0.3, speed: 0.1, yOffset: 1 });
    } else {
        actions.stackState("perfect_lunge", { op: "clear_flag" });
    }
    actions.sound("minecraft:entity.player.attack.sweep", { pitch: perfect ? 0.9 : 1.1 });
    actions.wear(perfect ? 3 : 1);
    actions.itemCooldown(perfect ? 3 : 2);
    actions.notify(perfect ? "Perfect Lunge!" : "Lunge!", { color: perfect ? "gold" : "yellow" });
}

function partial(actions, next) {
    if (next === 20) {
        lunge(actions, 19);
        return;
    }
    actions.when({ stackTimer: "charge_" + next }, drawn => lunge(drawn, next - 1), longer => partial(longer, next + 1));
}

item("thematic:melee", longsword => {
    longsword.use("blade", { power_cost: 342, durability: 30, attack_damage: 33.0, attack_speed: 0.6, knockback: 1.1 });
    longsword.set({ model: "thematic:construct_longsword", denied_sound: "minecraft:item.shield.block", reach: 1.33, use_action: "bow", max_use_time: 72000 });
    longsword.onUse(use => {
        for (let ticks = 6; ticks <= 20; ticks++) {
            use.stackState("charge_" + ticks, { op: "start_timer", seconds: ticks / 20 });
        }
    });
    longsword.onRelease(release => release.when({ not: { stackTimer: "charge_6" } },
        drawn => drawn.when({ stackTimer: "charge_20" }, part => partial(part, 7), full => lunge(full, 20))));
    longsword.onHit(hit => {
        hit.when({ player: true }, swing => swing.when({ stackTimer: "lunge" }, lunging => {
            lunging.when({ stackFlag: "perfect_lunge" }, perfect => {
                perfect.damage({ damage: 24, source: "player_attack" });
                perfect.knockback({ strength: 1.5, direction: "look" });
                perfect.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
                perfect.effect("minecraft:weakness", { duration: 2, amplifier: 0 });
                perfect.particles("minecraft:crit", { count: 20, spread: 0.5, speed: 0.3, yOffset: 1 });
            }, plain => {
                plain.damage({ damage: 15, source: "player_attack" });
                plain.knockback({ strength: 0.8, direction: "look" });
                plain.effect("minecraft:slowness", { duration: 2, amplifier: 0 });
                plain.particles("minecraft:damage_indicator", { count: 10, spread: 0.5, speed: 0.3, yOffset: 1 });
            });
            lunging.stackState("lunge", { op: "clear" });
        }, normal => normal.when({ chance: 0.2 }, bleed => {
            bleed.effect("minecraft:slowness", { duration: 3, amplifier: 0 });
            bleed.particles("minecraft:damage_indicator", { count: 8, spread: 0.3, speed: 0.2, yOffset: 1 });
        })));
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.55, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
    longsword.onHeldTick(held => held.when({ stackTimer: "lunge" }, lunging => lunging.when({ stackFlag: "perfect_lunge" },
        perfect => perfect.particles("minecraft:soul", { count: 1, spread: 0.2, speed: 0.02, yOffset: 1 }),
        plain => plain.particles("minecraft:cloud", { count: 1, spread: 0.2, speed: 0.02, yOffset: 1 }))));
});
