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
            }), pick => pick.when({ not: { flag: "bat_gadget_cooling_batarang" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:item.trident.throw", { pitch: 1.4, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_batarang", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_batarang", { for: 1 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Batarang is not ready", { who: "self", color: "gray" });
            }))));

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_batarang" });
    component.press(press => {
        useGadget(press);
    });
});
