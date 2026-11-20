component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:nether_star", stunImmune: true });
    component.press(press => {
        press.action("integrated_superheroes:darkseid_omega_sanction", {
            range: tuned("radius"),
            damage: tuned("damage"),
            max_targets: tuned("max_targets"),
            marked_multiplier: tuned("marked_multiplier")
        });
        press.animation("unleash", { stopAfter: 0.8 });
        press.shake({ radius: 20, strength: 1.5, duration: 0.7 });
    });
});
