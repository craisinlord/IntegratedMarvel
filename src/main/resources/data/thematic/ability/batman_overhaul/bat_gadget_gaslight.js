function lanternSmash(lantern) {
    lantern.sound("minecraft:block.glass.break", { volume: 1, pitch: 0.9, world: true });
    lantern.sound("minecraft:item.firecharge.use", { volume: 0.8, pitch: 1.2, world: true });
    lantern.vfx("thematic:ignition_burst", { radius: 8 });
    lantern.particles("minecraft:flame", { count: 30, spread: [0.8, 0.4, 0.8], speed: 0.06, yOffset: 0.3 });
    lantern.particles("minecraft:lava", { count: 6, spread: 0.3, yOffset: 0.3 });
    lantern.lightArea({ at: "pos", face: true, level: 15, shape: "circle", height: 3 });
    lantern.targets("around", { at: "source", radius: 8, lineOfSight: true }, seen => {
        seen.when({ target: { mob: true } }, mob => {
            mob.effect("minecraft:blindness", { duration: 4 });
            mob.effect("minecraft:slowness", { duration: 4, amplifier: 1 });
            mob.calm({ hostileOnly: true });
        });
    });
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
            }), pick => pick.when({ flag: "bat_gadget_selected_gas_lantern" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_gas_lantern" } }, pick => {
                pick.animation("gadget_throw");
                pick.sound("minecraft:entity.snowball.throw", { pitch: 0.6, world: true });
                pick.projectile({
                    body: "thrown",
                    throwable: "thematic:thrown_construct",
                    carries: "minecraft:lantern",
                    from: { point: "eyes", offset: [0, -0.15, 0] },
                    speed: 1.2,
                    gravity: 0.04,
                    pitch: -4,
                    maxAge: 6,
                    trail: { particle: "minecraft:small_flame", count: 1, spread: 0.03 }
                }, lanternSmash, lanternSmash);
                pick.setFlag("bat_gadget_cooling_gas_lantern", { for: 18 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Gas Lantern is not ready", { who: "self", color: "gray" });
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
