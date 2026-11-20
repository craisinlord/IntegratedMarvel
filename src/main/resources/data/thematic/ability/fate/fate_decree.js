component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:nether_star", ultimate: { blocksKeys: false }, stunImmune: true });
    component.press(press => {
        press.action("integrated_superheroes:fate_decree", {
            range: tuned("radius"),
            damage: tuned("damage"),
            heal: tuned("heal")
        });
        press.animation("unleash", { stopAfter: 1 });
        press.shake({ radius: 18, strength: 0.8, duration: 0.5 });
    });
});
