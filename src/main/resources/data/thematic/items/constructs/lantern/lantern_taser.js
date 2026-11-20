item("thematic:melee", taser => {
    taser.set({
        model: "thematic:construct_taser",
        denied_sound: "minecraft:item.shield.block",
        traits: ["construct"],
        look: "energy_construct",
        durability: 20,
        attack_damage: 3.0,
        attack_speed: -4.0,
        knockback: 1.0,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 30, tint: "corps" }
    });
    taser.onHit(hit => {
        hit.effect("minecraft:slowness", { duration: 5, amplifier: 4 });
        hit.effect("minecraft:weakness", { duration: 5, amplifier: 2 });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 1 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
            swing.itemCooldown(1);
        });
    });
    taser.onUse(use => {
        use.when({ not: { stackFlag: "taser_used" } }, fire => {
            fire.sound("minecraft:block.respawn_anchor.charge", { volume: 1.0, pitch: 1.5 });
            fire.targets("look", { range: 4.5, width: 0.5, pick: "nearest" }, zapped => {
                zapped.damage({ damage: 27.75, source: "player_attack" });
                zapped.effect("thematic:shock", { duration: 5, amplifier: 0 });
                zapped.push("target", { lift: 0.1 });
                zapped.stackState("taser_used", { op: "set_flag" });
            });
            fire.sound("minecraft:entity.lightning_bolt.impact", { volume: 0.8, pitch: 1.2 });
            fire.wear(3);
        });
    });
    taser.onBlockBroken(broken => broken.wear(2));
});
