item("thematic:melee", katana => {
    katana.use("blade", { power_cost: 358, durability: 25, attack_damage: 34.5, attack_speed: 1.0, knockback: 0.2 });
    katana.set({ model: "thematic:construct_katana", denied_sound: "minecraft:item.shield.block", reach: 1.17, dual_wield: true, use_action: "block", max_use_time: 40 });
    katana.onUse(use => use.when({ stackTimer: "deflect_rest" }, resting => {
        resting.stopUsing();
        resting.notify("Deflect recovering", { color: "red" });
    }, raised => {
        raised.stackState("deflecting", { op: "start_timer", seconds: 2 });
        raised.stackState("deflect_rest", { op: "start_timer", seconds: 6 });
        raised.sound("minecraft:item.armor.equip_iron", { pitch: 1.4 });
        raised.particles(dust("$stack_color", 1.0), { count: 8, spread: [0.3, 0.4, 0.3], speed: 0.01, from: "eyes", forward: 0.8, yOffset: -0.4 });
    }));
    katana.onRelease(release => release.stackState("deflecting", { op: "clear" }));
    katana.onIncomingProjectile({ if: { stackTimer: "deflecting", facing: { toward: "attacker", within: 90 } } }, turned => {
        turned.cancelEvent();
        turned.deflectProjectile({ direction: "shooter" });
        turned.heal(5);
        turned.sound("minecraft:item.shield.block", { pitch: 1.6 });
        turned.particles("minecraft:crit", { count: 8, spread: 0.3, speed: 0.2, from: "eyes", forward: 0.8, yOffset: -0.3 });
        turned.particles(streak("$stack_color", 1, 2), { count: 10, spread: 0.2, speed: 0.6, from: "eyes", forward: 0.8, yOffset: -0.3 });
    });
    katana.onHolderHurt({ if: { stackTimer: "deflecting", facing: { toward: "attacker", within: 90 } } }, parried => {
        parried.cancelEvent();
        parried.when({ melee: true }, melee => {
            melee.effect("thematic:bleed", { duration: 5, amplifier: 0 });
            melee.sound("minecraft:item.shield.block", { pitch: 1.2 });
            melee.particles("minecraft:sweep_attack", { count: 2, spread: [0.3, 0.3, 0.3], speed: 0, from: "eyes", forward: 0.8, yOffset: -0.3 });
            melee.particles(streak("$stack_color", 1, 2), { count: 8, spread: 0.2, speed: 0.5, from: "eyes", forward: 0.8, yOffset: -0.3 });
        }, other => {
            other.sound("minecraft:item.shield.block", { pitch: 1.0 });
            other.particles("minecraft:crit", { count: 4, spread: 0.3, speed: 0.1, from: "eyes", forward: 0.8, yOffset: -0.3 });
        });
    });
    katana.onHit(hit => {
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.1, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
});
