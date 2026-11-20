const IMPROVISED = [
    { throwable: "thematic:gadget_batarang", sound: "minecraft:item.trident.throw", pitch: 1.4 },
    { throwable: "thematic:gadget_electric_batarang", sound: "minecraft:block.beacon.power_select", pitch: 1.8 },
    { throwable: "thematic:gadget_heated_batarang", sound: "minecraft:item.firecharge.use", pitch: 1.3 },
    { throwable: "thematic:gadget_bola", sound: "minecraft:entity.fishing_bobber.throw", pitch: 0.8 },
    { throwable: "thematic:gadget_flash_grenade", sound: "minecraft:entity.snowball.throw", pitch: 0.7 },
    { throwable: "thematic:gadget_ice_grenade", sound: "alexscaves:frostmint_spear_hit", pitch: 1.2 },
    { throwable: "thematic:gadget_smoke_pellet", sound: "minecraft:entity.splash_potion.throw", pitch: 0.9 }
];

const useGadget = pick => pick.when({ flag: "bat_gadget_selected_grapnel" }, pick => pick.when({ not: { flag: "bat_gadget_cooling_grapnel" } }, pick => {
                pick.animation("web_shoot_right");
                pick.sound("thematic:widow_grapple", { volume: 1, pitch: 1, world: true });
                pick.projectile({ body: "entity", entity: "thematic:bat_grapnel_hook", from: "main_hand", speed: 3, inheritMotion: false });
                pick.resetFallDistance();
                pick.setFlag("bat_gadget_cooling_grapnel", { for: 2 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Grapnel is not ready", { who: "self", color: "gray" });
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
            }), pick => pick.when({ not: { flag: "bat_gadget_cooling_improvised" } }, pick => {
                pick.animation("gadget_throw", { alternate: "gadget_throw2" });
                pick.oneOf(IMPROVISED.map(gadget => grab => {
                    grab.sound(gadget.sound, { pitch: gadget.pitch, world: true });
                    grab.projectile({ body: "thrown", throwable: gadget.throwable, byDefinition: true, from: { point: "eyes", offset: [0, -0.15, 0] }, damage: 19 });
                }));
                pick.particles("minecraft:crit", { count: 8, spread: 0.2, speed: 0.2, from: "main_hand" });
                pick.setFlag("bat_gadget_cooling_improvised", { for: 5 });
            }, busy => {
                busy.sound("minecraft:block.dispenser.fail", { volume: 0.6, pitch: 1.6 });
                busy.notify("Improvised Gadget is not ready", { who: "self", color: "gray" });
            })));

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_batarang" });
    component.press(press => {
        useGadget(press);
    });
});
