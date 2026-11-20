component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:golden_apple" });
    component.press(press => {
        press.action("integrated_superheroes:fate_bonds", {
            range: tuned("radius"),
            duration: tuned("duration"),
            damage: tuned("damage"),
            seal_radius: tuned("seal_radius"),
            seal_damage: tuned("seal_damage")
        });
        press.animation("force_palm_thrust", { stopAfter: 0.5 });
    });
});
