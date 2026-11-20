function freezeChill(target) {
    target.effect("minecraft:slowness", { duration: 3 });
    target.effect("thematic:frozen", { duration: 2, amplifier: 0, showParticles: false });
    target.particles("minecraft:snowflake", { count: 6, spread: 0.3, speed: 0.02, yOffset: 1 });
}

function freezeShatter(gel) {
    gel.sound("alexscaves:frostmint_spear_hit", { volume: 1.1, world: true });
    gel.sound("minecraft:block.glass.break", { volume: 0.6, pitch: 1.4, world: true });
    gel.vfx("thematic:bizarro_frost_burst", { radius: 2 });
    gel.vfx("thematic:powergirl_frost_mist", { radius: 4, duration: 6 });
    gel.particles("minecraft:snowflake", { count: 60, spread: [1.5, 0.6, 1.5], speed: 0.05, yOffset: 0.4 });
    gel.splash("#BFEFFF", { pool: { radius: 1.5 } });
    gel.transformBlock({ "minecraft:water": "minecraft:frosted_ice" }, {
        at: "pos", shape: "circle", radius: 4, height: 3, below: 2, only: "still_source"
    });
    gel.targets("around", { at: "source", radius: 4 }, frozen => {
        frozen.damage({ damage: 38, element: "ice" });
        freezeChill(frozen);
    });
    gel.zone({ floor: true, radius: 4, height: 2, every: 1, for: 6, caught: "bat_freeze_gel" }, standing => {
        standing.damage({ damage: 5, element: "ice" });
        freezeChill(standing);
    }, freezeChill);
}

const useGadget = pick => pick.when({ flag: "bat_gadget_selected_grapnel" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_grapnel" } }, pick => {
                pick.animation("web_shoot_right");
                pick.sound("thematic:widow_grapple", { volume: 1, pitch: 1, world: true });
                pick.projectile({ body: "entity", entity: "thematic:bat_grapnel_hook", from: "main_hand", speed: 3, inheritMotion: false });
                pick.resetFallDistance();
                pick.setFlag("bat_gadget_cooling_grapnel", { for: 2 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Grapnel is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_smoke_pellet" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_smoke_pellet" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:entity.splash_potion.throw", { pitch: 0.9, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_smoke_pellet", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_smoke_pellet", { for: 10 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Smoke Pellet is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_freeze_gel" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_freeze_gel" } }, pick => {
                pick.animation("gadget_throw");
                pick.sound("minecraft:entity.splash_potion.throw", { world: true });
                pick.projectile({
                    body: "thrown",
                    throwable: "thematic:gadget_ice_grenade",
                    from: { point: "eyes", offset: [0, -0.15, 0] },
                    speed: 1.4,
                    pitch: -3,
                    fuse: 3,
                    trail: { particle: "minecraft:snowflake", count: 2, spread: 0.05 }
                }, freezeShatter, freezeShatter);
                pick.setFlag("bat_gadget_cooling_freeze_gel", { for: 12 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Freeze Gel is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_rally" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_rally" } }, pick => {
                pick.animation("comms");
                pick.sound("minecraft:block.note_block.bell", { volume: 0.8, pitch: 1.4, world: true });
                pick.selfEffect("minecraft:strength", { duration: 6, amplifier: 1 });
                pick.selfEffect("minecraft:resistance", { duration: 6, amplifier: 1 });
                pick.particles("minecraft:angry_villager", { count: 5, spread: 0.2, speed: 0.1 });
                pick.targets("allies", ally => {
                    ally.effect("minecraft:strength", { duration: 6, amplifier: 1 });
                    ally.effect("minecraft:resistance", { duration: 6, amplifier: 1 });
                });
                pick.setFlag("bat_gadget_cooling_rally", { for: 40 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Rally is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ not: { flag: "bat_gadget_cooling_batarang" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:item.trident.throw", { pitch: 1.4, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_batarang", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_batarang", { for: 1 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Batarang is not ready", { who: "self", color: "gray" });
            })))));

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_batarang" });
    component.press(press => {
        useGadget(press);
    });
});
