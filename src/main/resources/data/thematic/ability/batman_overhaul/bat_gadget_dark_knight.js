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
            }), pick => pick.when({ flag: "bat_gadget_selected_explosive_gel" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_explosive_gel" } }, pick => {
                pick.animation("web_shoot_right");
                pick.sound("minecraft:entity.slime.squish", { volume: 1, pitch: 0.7, world: true });
                pick.sound("minecraft:block.honey_block.place", { volume: 1, pitch: 1.2, world: true });
                pick.projectile({
                    body: "bolt",
                    from: "main_hand",
                    speed: 2.2,
                    maxDistance: 24,
                    damage: 0,
                    look: { style: "orb", item: "minecraft:light_blue_dye" },
                    trail: { particle: "minecraft:glow", count: 1, spread: 0.05 },
                }, hit => {
                    hit.sound("minecraft:entity.slime.squish_small", { at: "target", volume: 1, pitch: 0.8, world: true });
                }, block => {
                    block.summon("thematic:bat_gel_blob", { at: "face", maxAlive: 3, lasts: 120, owner: true, hittable: false, solid: false });
                    block.sound("minecraft:block.honey_block.slide", { volume: 1, pitch: 1.4, world: true });
                    block.particles("minecraft:glow", { count: 8, spread: 0.2, speed: 0.05 });
                });
                pick.setFlag("bat_gadget_cooling_explosive_gel", { for: 1.5 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Explosive Gel is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_gel_detonate" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_gel_detonate" } }, pick => {
                pick.animation("gadget_throw");
                pick.sound("minecraft:block.lever.click", { volume: 1, pitch: 1.6, world: true });
                pick.action("integrated_superheroes:gel_detonate", { range: 64, radius: 4, damage: 29, push: 1.4, hardness: 3, break_radius: 1.6 });
                pick.setFlag("bat_gadget_cooling_gel_detonate", { for: 12 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Detonate Gel is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_disruptor" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_disruptor" } }, pick => {
                pick.animation("web_shoot_right");
                pick.sound("thematic:laser_charge", { volume: 0.7, pitch: 1.6, world: true });
                pick.particles("minecraft:electric_spark", { from: "main_hand", count: 8, spread: 0.1, speed: 0.08 });
                pick.targets("aimed", { range: 30, maxAngle: 6, assist: 0.5, lineOfSight: true }, target => {
                    target.vfx("thematic:fork_strike_low", { from: "main_hand", to: "target_body" });
                    target.vfx("thematic:fork_burst", { anchor: "body" });
                    target.sound("scguns:item.rusty_gnat.copper_jam", { at: "target", volume: 1.4, pitch: 1, world: true });
                    target.sound("minecraft:block.beacon.deactivate", { at: "target", volume: 0.8, pitch: 1.8, world: true });
                    target.particles("minecraft:electric_spark", { anchor: "body", count: 18, spread: 0.4, speed: 0.15 });
                    target.effect("integrated_superheroes:jammed", { duration: 8, amplifier: 0, showParticles: false });
                    target.effect("thematic:disable", { duration: 8, amplifier: 0, showParticles: false });
                    target.effect("minecraft:slowness", { duration: 3, amplifier: 1, showParticles: false });
                    target.damage({ damage: 5, source: "player_attack" });
                });
                pick.setFlag("bat_gadget_cooling_disruptor", { for: 20 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Disruptor is not ready", { who: "self", color: "gray" });
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
            }), pick => pick.when({ flag: "bat_gadget_selected_electric_batarang" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_electric_batarang" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:block.beacon.power_select", { pitch: 1.8, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_electric_batarang", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_electric_batarang", { for: 8 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Electric Batarang is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ flag: "bat_gadget_selected_flash_grenade" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_flash_grenade" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:entity.snowball.throw", { pitch: 0.7, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_flash_grenade", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_flash_grenade", { for: 15 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Flash Grenade is not ready", { who: "self", color: "gray" });
            }), pick => pick.when({ not: { flag: "bat_gadget_cooling_batarang" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:item.trident.throw", { pitch: 1.4, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_batarang", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_batarang", { for: 1 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Batarang is not ready", { who: "self", color: "gray" });
            })))))))));

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_batarang" });
    component.press(press => {
        useGadget(press);
    });
});
