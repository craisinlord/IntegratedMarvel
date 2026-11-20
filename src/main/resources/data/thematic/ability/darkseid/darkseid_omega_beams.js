component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:redstone" });
    component.press(press => {
        press.action("integrated_superheroes:darkseid_omega_beams", {
            range: tuned("radius"),
            damage: tuned("damage"),
            chain_range: tuned("chain_range"),
            chain_share: tuned("chain_share")
        });
        press.animation("thunderclap", { stopAfter: 0.8 });
    });
});
