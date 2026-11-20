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
            }), pick => pick.when({ flag: "bat_gadget_selected_explosive_batarang" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_explosive_batarang" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.sound("minecraft:item.trident.throw", { pitch: 1.2, world: true });
                pick.projectile({ body: "thrown", throwable: "thematic:gadget_explosive_batarang", byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] } });
                pick.particles("minecraft:crit", { count: 6, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_explosive_batarang", { for: 10 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Explosive Batarang is not ready", { who: "self", color: "gray" });
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
