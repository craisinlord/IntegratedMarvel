component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:heavy_core" });
    component.press(press => {
        press.action("integrated_superheroes:apocalypse_leap", {
            range: tuned("radius"),
            damage: tuned("damage")
        });
    });
});
