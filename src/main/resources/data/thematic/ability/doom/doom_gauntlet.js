component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:emerald" });
    component.press(press => {
        press.action("integrated_superheroes:doom_gauntlet", {
            range: tuned("radius"),
            damage: tuned("damage"),
            sigil_duration: tuned("sigil_duration"),
            sigil_damage: tuned("sigil_damage"),
            sigil_radius: tuned("sigil_radius")
        });
        press.animation("thunderclap", { stopAfter: 0.5 });
    });
});
