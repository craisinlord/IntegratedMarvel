item("thematic:melee", lighter => {
    lighter.set({
        model: "thematic:construct_lighter",
        denied_sound: "minecraft:item.shield.block",
        traits: ["construct"],
        look: "energy_construct",
        durability: 30,
        attack_damage: 7.5,
        attack_speed: 0.5,
        knockback: 0.5,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 8, tint: "corps" }
    });
    lighter.onHit(hit => {
        hit.when({ target: { onFire: false } }, light => {
            light.ignite({ seconds: 5, exact: true });
            light.sound("minecraft:item.flintandsteel.use", { volume: 1.0, pitch: 1.0 });
            light.effect("minecraft:slowness", { duration: 3, amplifier: 0 });
        });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.25, lift: 0.1 });
            swing.itemCooldown(0.25);
        });
    });
    lighter.onUseOnBlock(use => {
        use.placeFire({ face: true });
        use.sound("minecraft:item.flintandsteel.use", { volume: 1.0, pitch: 1.0 });
        use.wear(1);
    });
    lighter.onUseOnEntity(use => {
        use.when({ target: { onFire: false } }, light => {
            light.ignite({ seconds: 5, exact: true });
            light.sound("minecraft:item.flintandsteel.use", { volume: 1.0, pitch: 1.0 });
            light.wear(1);
        });
    });
    lighter.onBlockBroken(broken => broken.wear(2));
});
