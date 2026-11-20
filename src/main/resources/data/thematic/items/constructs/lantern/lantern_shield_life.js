item("thematic:melee", shield => {
    shield.set({
        model: "thematic:construct_shield",
        denied_sound: "minecraft:item.shield.block",
        max_count: 1,
        traits: ["construct"],
        look: "energy_construct",
        durability: 100,
        item_bar: { color: "stack_color" },
        construct: { power_cost: 280, tint: "corps" },
        use_behaviour: "shield",
        attributes: [{ attribute: "minecraft:generic.armor", amount: 4.0, operation: "addition", uuid: "66934663-f980-4fa3-8922-2929ab8a8592" }]
    });
    shield.onUse(use => use.when({ sneaking: true }, bubble => bubble.when({ stackTimer: "bubble_rest" },
        rest => rest.notify("Bubble recovering", { color: "red" }),
        dome => {
            dome.meter("life_ringCharge", { cost: 200, failMessage: "Not enough power!" });
            dome.stackState("bubble", { op: "start_timer", seconds: 4 });
            dome.stackState("bubble_rest", { op: "start_timer", seconds: 12 });
            dome.targets("allies", { radius: 3, within: 3 }, ally => ally.effect("minecraft:resistance", { duration: 4, amplifier: 1 }));
            dome.particleDome("full_dome", { particle: dust("$stack_color", 1.2), radius: 3, count: 80 });
            dome.sound("minecraft:block.beacon.activate", { pitch: 1.4 });
        }), raise => {
        raise.stackState("raised", { op: "start_timer", seconds: 0.25 });
        raise.sound("minecraft:item.shield.block", { volume: 0.8, pitch: 1.0 });
    }));
    shield.onIncomingProjectile({ if: { stackTimer: "bubble" } }, incoming => {
        incoming.cancelEvent();
        incoming.deflectProjectile();
        incoming.sound("minecraft:item.shield.block", { pitch: 1.4 });
    });
    shield.onIncomingProjectile({ if: { stackTimer: "raised" } }, incoming => {
        incoming.cancelEvent();
        incoming.deflectProjectile();
        incoming.sound("minecraft:item.shield.block", { pitch: 1.8 });
        incoming.notify("Perfect block!", { color: "green" });
    });
    shield.onHeldTick({ every: 10 }, held => held.when({ stackTimer: "bubble" },
        up => up.particleDome("dome_only", { particle: dust("$stack_color", 0.8), radius: 3, count: 30 })));
    shield.onHit(hit => hit.when({ not: { stackTimer: "swing" } }, swing => {
        swing.stackState("swing", { op: "start_timer", seconds: 0.25 });
        swing.wear(2);
        swing.push("target", { direction: "away_from_owner", flat: true, strength: 1.0, lift: 0.1 });
        swing.itemCooldown(0.25);
    }));
    shield.onBlockBroken(broken => broken.wear(2));
});
