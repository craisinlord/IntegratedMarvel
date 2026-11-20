component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:ender_eye" });
    component.press(press => {
        press.action("integrated_superheroes:doom_aegis", {
            duration: tuned("duration"),
            shield: tuned("shield"),
            projectile_cost: tuned("projectile_cost")
        });
        press.then("warded");
    });
    component.phase("warded", warded => {
        warded.lasts("duration");
        warded.on("press", recall => {
            recall.action("integrated_superheroes:doom_recall");
            recall.then("cooldown");
        });
        warded.onExit(exit => exit.action("integrated_superheroes:doom_end_aegis"));
    });
});
