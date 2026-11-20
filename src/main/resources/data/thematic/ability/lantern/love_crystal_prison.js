component("love_crystal_prison", component => {
    component.cooldown();

    component.press(press => {
        press.targets("look", { range: tuned("range"), width: 1.2, pick: "nearest" }, held => {
            held.effect("thematic:immobile", { duration: tuned("duration"), amplifier: 0 });
            held.effect("minecraft:resistance", { duration: tuned("duration"), amplifier: 4 });
            held.particleDome("full_dome", { particle: dust([0.9, 0.4, 0.9], 1.2), radius: 1.5, count: 60 });
            held.sound("minecraft:block.amethyst_block.resonate", { pitch: 1.2 });
        }, missed => missed.notify("Nothing to hold"));
    });
});
