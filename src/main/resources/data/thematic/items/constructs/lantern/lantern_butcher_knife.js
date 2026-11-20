const ANIMALS = ["axolotl", "bee", "camel", "cat", "chicken", "cow", "donkey", "fox", "frog", "goat", "hoglin", "horse", "llama",
    "mooshroom", "mule", "ocelot", "panda", "parrot", "pig", "polar_bear", "rabbit", "sheep", "skeleton_horse", "sniffer", "strider",
    "trader_llama", "turtle", "wolf", "zombie_horse"];

item("thematic:melee", knife => {
    knife.use("blade", { power_cost: 151, durability: 25, attack_damage: 42.0, attack_speed: -2.0, knockback: 0.1, mining_wear: 4 });
    knife.set({ denied_sound: "minecraft:item.shield.block", model: "thematic:butcher_knife", glint: true });
    knife.onHit(hit => {
        hit.when({ player: true }, swing => {
            swing.when({ not: { stackFlag: "butcher_enchanted" } }, first => {
                first.stackState("butcher_enchanted", { op: "set_flag" });
                first.when({ chance: 2 / 3 }, enchanted => enchanted.when({ chance: 0.5 },
                    one => one.enchantHeld({ enchantments: { "minecraft:looting": 1 }, replace: false }),
                    two => two.enchantHeld({ enchantments: { "minecraft:looting": 2 }, replace: false })));
            });
            swing.when({ target: { entity: ANIMALS } }, animal => {
                animal.particles("minecraft:damage_indicator", { count: 15, spread: 0.5, speed: 0.2, yOffset: 1 });
                animal.notify("Butcher Strike!", { color: "dark_red" });
            });
            swing.when({ stackTimer: "quick_kill", target: { alive: false } }, clean => {
                clean.particles("minecraft:soul", { count: 10, spread: 0.3, speed: 0.1, yOffset: 1 });
                clean.notify("Clean Kill!", { color: "dark_green" });
            });
            swing.when({ target: { entity: ANIMALS } },
                animal => animal.when({ stackTimer: "quick_kill", target: { alive: false } },
                    both => both.damage({ damage: 66, source: "player_attack" }),
                    only => only.damage({ damage: 63, source: "player_attack" })),
                other => other.when({ stackTimer: "quick_kill", target: { alive: false } },
                    clean => clean.damage({ damage: 3, source: "player_attack" })));
            swing.stackState("quick_kill", { op: "start_timer", seconds: 1.05 });
            swing.when({ chance: 0.25 }, critical => {
                critical.damage({ damage: 21, source: "player_attack" });
                critical.particles("minecraft:crit", { count: 20, spread: 0.5, speed: 0.5, anchor: "body" });
                critical.sound("minecraft:entity.player.attack.crit", { pitch: 1.2 });
                critical.notify("Critical Cut!", { color: "gold" });
                critical.effect("thematic:bleed", { duration: 4, amplifier: 2 });
            });
            swing.effect("thematic:bleed", { duration: 4, amplifier: 0 });
            swing.wear(3);
        });
        hit.wear(2);
        hit.push("target", { direction: "away_from_owner", flat: true, strength: 0.05, lift: 0.1 });
        hit.itemCooldown(1.0);
    });
});
