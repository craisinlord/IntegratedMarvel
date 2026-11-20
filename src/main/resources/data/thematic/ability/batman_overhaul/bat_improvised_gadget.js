const GADGETS = [
    { throwable: "thematic:gadget_batarang", sound: "minecraft:item.trident.throw", pitch: 1.4 },
    { throwable: "thematic:gadget_electric_batarang", sound: "minecraft:block.beacon.power_select", pitch: 1.8 },
    { throwable: "thematic:gadget_heated_batarang", sound: "minecraft:item.firecharge.use", pitch: 1.3 },
    { throwable: "thematic:gadget_bola", sound: "minecraft:entity.fishing_bobber.throw", pitch: 0.8 },
    { throwable: "thematic:gadget_flash_grenade", sound: "minecraft:entity.snowball.throw", pitch: 0.7 },
    { throwable: "thematic:gadget_ice_grenade", sound: "alexscaves:frostmint_spear_hit", pitch: 1.2 },
    { throwable: "thematic:gadget_smoke_pellet", sound: "minecraft:entity.splash_potion.throw", pitch: 0.9 }
];

component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "thematic:gadget_batarang" });

    component.press(press => {
        press.animation("gadget_throw", { alternate: "gadget_throw2" });
        press.wait(0.15);
        press.oneOf(GADGETS.map(gadget => pick => {
            pick.sound(gadget.sound, { pitch: gadget.pitch, world: true });
            pick.projectile({
                body: "thrown",
                throwable: gadget.throwable,
                byDefinition: true,
                from: { point: "eyes", offset: [0, -0.15, 0] },
                damage: tuned("damage")
            });
        }));
        press.particles("minecraft:crit", { count: 8, spread: 0.2, speed: 0.2, from: "main_hand" });
    });
});
