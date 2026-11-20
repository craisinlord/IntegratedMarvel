item("thematic:melee", baton => {
    baton.use("blunt", { power_cost: 136, durability: 24, attack_damage: 34.5, attack_speed: -3.5, knockback: 0.75 });
    baton.set({ model: "thematic:construct_baton", denied_sound: "minecraft:item.shield.block", reach: 1.17 });
    baton.onHit(hit => {
        hit.when({ chance: 1 / 6 }, dazed => dazed.when({ target: { player: true } },
            player => player.effect("thematic:confusion", { duration: 6, amplifier: 1 }),
            mob => mob.effect("minecraft:slowness", { duration: 4, amplifier: 10 })));
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.375, lift: 0.1 });
        hit.itemCooldown(1.0);
    });
});
