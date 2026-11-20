item("thematic:melee", wrench => {
    wrench.set({
        model: "thematic:construct_wrench",
        denied_sound: "minecraft:block.lever.click",
        traits: ["construct"],
        look: "energy_construct",
        durability: 24,
        attack_damage: 27.0,
        attack_speed: -2.7,
        knockback: 0.6,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 120, tint: "corps" }
    });
    wrench.onHit(hit => {
        hit.when({ chance: 0.5 }, clang => {
            clang.sound("minecraft:block.anvil.place", { volume: 0.5, pitch: 1.4 });
            clang.when({ target: { entity: ["thematic:invisiblejet", "thematic:batmobile", "thematic:skateboard", "thematic:canarybike",
                "thematic:tank", "thematic:tank_construct", "thematic:batwing", "thematic:arrow_plane"] } }, vehicle => {
                vehicle.notify("message.thematic.construct.dismantled", { color: "aqua" });
                vehicle.damage({ damage: 540, source: "mob_attack" });
            });
        });
        hit.effect("minecraft:slowness", { duration: 3, amplifier: 1 });
        hit.when({ not: { stackTimer: "swing" } }, swing => {
            swing.stackState("swing", { op: "start_timer", seconds: 1 });
            swing.wear(1);
            swing.push("target", { direction: "away_from_owner", flat: true, strength: 0.3, lift: 0.1 });
            swing.itemCooldown(1);
        });
    });
    wrench.onUseOnBlock(use => {
        use.cycleBlockProperty("auto", {
            selectionKey: "WrenchProperty",
            except: ["#minecraft:slabs", "thematic:the_flash_experiment"],
            wear: 1
        });
    });
    wrench.onBlockBroken(broken => broken.wear(2));
});
