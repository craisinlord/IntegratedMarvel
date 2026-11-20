component(component => {
    component.cooldown();
    component.traits({ ultimate: { blocksKeys: false }, wheelIcon: "minecraft:nether_star", stunImmune: true });
    component.visual("thematic:rising_aura", {
        color: "#0B7034",
        core_color: "#6DFF70",
        tongues: 12,
        alpha: 0.35,
        embers: 4,
        first_person: 0.2
    });
    component.press(press => {
        press.action("integrated_superheroes:doom_ascendant", {
            duration: tuned("duration"),
            damage: tuned("damage"),
            range: tuned("radius"),
            max_targets: tuned("max_targets"),
            beam_multiplier: tuned("beam_multiplier")
        });
        press.animation("unleash", { stopAfter: 1 });
        press.shake({ radius: 16, strength: 1, duration: 0.5 });
        press.then("ascendant");
    });
    component.phase("ascendant", ascendant => {
        ascendant.lasts("duration");
        ascendant.onExit(exit => exit.action("integrated_superheroes:doom_end_ascendant"));
    });
});
