component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:gold_ingot" });
    component.press(press => {
        press.action("integrated_superheroes:fate_judgment", {
            range: tuned("radius"),
            damage: tuned("damage"),
            focus_multiplier: tuned("focus_multiplier")
        });
        press.animation("force_palm_thrust", { stopAfter: 0.5 });
    });
});
