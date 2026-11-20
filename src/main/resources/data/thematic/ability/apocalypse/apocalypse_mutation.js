component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:totem_of_undying", stunImmune: true });
    component.visual("thematic:rising_aura", {
        color: "#153F99",
        core_color: "#8CB7FF",
        tongues: 10,
        alpha: 0.3,
        embers: 4,
        first_person: 0.2
    });
    component.press(press => {
        press.action("integrated_superheroes:apocalypse_mutation", {
            duration: tuned("duration"),
            damage_multiplier: tuned("damage_multiplier")
        });
        press.animation("unleash", { stopAfter: 1.2 });
        press.shake({ radius: 16, strength: 1.2, duration: 0.7 });
        press.then("evolved");
    });
    component.phase("evolved", evolved => {
        evolved.lasts("duration");
        evolved.onExit(exit => exit.action("integrated_superheroes:apocalypse_end_mutation"));
    });
});
