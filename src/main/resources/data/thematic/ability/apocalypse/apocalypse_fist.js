component(component => {
    component.cooldown();
    component.traits({ wheelIcon: "minecraft:anvil" });
    component.press(press => {
        press.action("integrated_superheroes:apocalypse_fist", {
            range: tuned("radius"),
            damage: tuned("damage")
        });
        press.animation("thunderclap", { stopAfter: 0.7 });
        press.shake({ radius: 10, strength: 0.65, duration: 0.3 });
    });
});
