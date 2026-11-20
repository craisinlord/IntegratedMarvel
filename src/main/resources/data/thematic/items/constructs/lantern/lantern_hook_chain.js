item("thematic:melee", hook => {
    hook.set({
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        model: "thematic:billy_club_grapple",
        durability: 30,
        attack_damage: 9.0,
        attack_speed: -1.0,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 180, tint: "corps" }
    });
    hook.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.5 });
        swing.wear(1);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.6, lift: 0.1 });
        swing.itemCooldown(0.5);
    }));
    hook.onUse(use => use.when({ stackTimer: "hook_rest" }, rest => rest.notify("Hook Chain recovering", { color: "red" }), reel => {
        reel.meter("fear_ringCharge", { cost: 60, failMessage: "Not enough power!" });
        reel.stackState("hook_rest", { op: "start_timer", seconds: 6 });
        reel.targets("look", { range: 16, width: 1.0, pick: "nearest" }, hooked => {
            hooked.push("target", { direction: "toward_owner", strength: 2.0, lift: 0.3 });
            hooked.effect("thematic:yellow_flashed", { duration: 3, amplifier: 0 });
            hooked.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
            hooked.sound("minecraft:block.chain.break", { pitch: 0.8 });
        }, missed => missed.notify("Nothing to hook"));
        reel.wear(2);
    }));
});
