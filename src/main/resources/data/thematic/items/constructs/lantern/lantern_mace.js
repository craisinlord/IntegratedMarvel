const SLAM = "min((min(fall_distance, 12) - 2) * 4.5, 80)";

item("thematic:melee", mace => {
    mace.use("blunt", { power_cost: 132, durability: 25, attack_damage: 33.0, attack_speed: -3.2, knockback: 1.0 });
    mace.set({ model: "thematic:construct_mace", denied_sound: "minecraft:item.shield.block", reach: 1.17 });
    mace.onHit(hit => hit.when({ not: { player: true, fallFlying: true } }, swing => {
        swing.when({ player: true, number: { of: { input: "fall_distance" }, atLeast: 2 } }, slam => {
            slam.damage({ damage: expr(SLAM), source: "player_attack" });
            slam.targets("around", { radius: 3 }, near => {
                near.damage({ damage: expr(SLAM + " * 0.7"), source: "player_attack" });
            });
            slam.sound("minecraft:block.anvil.land", { pitch: 1.5 });
            slam.particleDome("burst_only", { particle: dust([0.0, 0.8, 0.2], 1.0), radius: 3, count: 25 });
            slam.wear(3);
            slam.effect("thematic:immobile", { duration: 3, amplifier: 1 });
            slam.resetFallDistance();
            slam.itemCooldown(4);
        }, strike => {
            strike.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
            strike.wear(2);
            strike.push("target", { direction: "away_from_owner", flat: true, strength: 0.5, lift: 0.1 });
            strike.itemCooldown(1.0);
        });
    }));
});
