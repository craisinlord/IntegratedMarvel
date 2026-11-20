component(component => {
    component.traits({ wheelIcon: "minecraft:wither_skeleton_skull" });

    component.on("tick", { every: 1 }, tick => {
        tick.particles("minecraft:smoke", { anchor: "feet", count: 2, spread: [0.35, 0.02, 0.35], speed: 0.005 });
        tick.targets("around", { radius: tuned("radius"), round: true }, foe => {
            foe.when({ target: { hostile: true, player: false } }, dread => {
                dread.effect("minecraft:weakness", { duration: tuned("duration"), amplifier: tuned("amplifier"), showParticles: false });
            });
        });
    });
});
