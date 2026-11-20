component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:crying_obsidian" });
    component.press(press => {
        press.action("integrated_superheroes:darkseid_boom_tube", {
            range: tuned("radius"),
            damage: tuned("damage"),
            shock_radius: tuned("shock_radius")
        });
    });
});
