component(component => {
    component.traits({ wheelIcon: "minecraft:feather" });

    component.on("jump", { sprinting: true }, jump => {
        jump.push("self", { direction: "look_horizontal", strength: tuned("push_strength"), lift: tuned("push_lift"), mode: "add" });
        jump.particles("minecraft:cloud", { anchor: "feet", count: 8, spread: [0.3, 0.05, 0.3], speed: 0.03 });
        jump.sound("minecraft:entity.breeze.jump", { volume: 0.5, pitch: 1.3, world: true });
    });
});
