item("thematic:melee", swatter => {
    swatter.use("blunt", { power_cost: 249, durability: 40, attack_damage: 22.5, attack_speed: 0.3, knockback: 0.5 });
    swatter.set({ model: "thematic:construct_fly_swatter", denied_sound: "minecraft:item.shield.block", reach: 1.17 });
    swatter.onHit(hit => {
        hit.when({ player: true }, swing => swing.targets("around", { radius: 4 }, each => each.when({ target: { onGround: false } }, flying => {
            flying.push("target", { direction: "away_from_owner", flat: true, strength: 3.5, mode: "set", vertical: -5 });
            flying.damage({ damage: 30, source: "player_attack" });
        }, grounded => grounded.push("target", { direction: "away_from_owner", flat: true, strength: 3.5, mode: "set", vertical: 0.8 }))));
        hit.sound("minecraft:entity.player.attack.sweep", { volume: 1.2, pitch: 1.8 });
        hit.sound("minecraft:entity.generic.explode", { volume: 0.5, pitch: 1.5 });
        hit.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        hit.wear(1);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.25, lift: 0.1 });
        hit.itemCooldown(0.25);
    });
});
